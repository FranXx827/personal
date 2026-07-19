/**
 * Axios HTTP 客户端
 * - 统一前缀 / 自动注入 token
 * - 401 自动刷新
 * - 统一错误处理 (后端 code != 0 抛 ApiError)
 */
import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import { ApiError, type ApiResponse } from '@/types/api'
import { useUserStore } from '@/stores/user'
import { getErrorMessage } from '@/utils/error'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL
const AI_BASE_URL = import.meta.env.VITE_AI_BASE_URL

/** 主后端客户端 */
export const http: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  withCredentials: true, // 允许携带 refresh token cookie
})

/** AI 服务客户端 */
export const aiHttp: AxiosInstance = axios.create({
  baseURL: AI_BASE_URL,
  timeout: 60000, // AI 请求更长
  adapter: 'fetch', // 使用 fetch adapter 以支持 responseType: 'stream'（XHR 不支持 stream）
})

let isRefreshing = false
let pendingQueue: Array<(token: string | null) => void> = []

function processQueue(token: string | null) {
  pendingQueue.forEach((cb) => cb(token))
  pendingQueue = []
}

http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

aiHttp.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse
    if (body.code === 0) return body.data as AxiosResponse
    // 业务错误（如用户名已存在，后端返回 HTTP 200 + code != 0）：
    // 成功分支抛出的 ApiError 不会经过错误拦截器，因此这里必须主动弹窗，否则前端无提示。
    const err = new ApiError(body.code, body.message, body.traceId, body.data)
    ElMessage.error(getErrorMessage(err))
    throw err
  },
  async (error) => {
    const original = error.config
    const userStore = useUserStore()

    if (error.response?.status === 401 && !(original as Record<string, unknown>)._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push((token) => {
            if (token) {
              original.headers.Authorization = `Bearer ${token}`
              resolve(http(original))
            } else {
              reject(error)
            }
          })
        })
      }
      ;(original as Record<string, unknown>)._retry = true
      isRefreshing = true
      try {
        const newToken = await userStore.refresh()
        processQueue(newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return http(original)
      } catch (refreshErr) {
        processQueue(null)
        userStore.logout()
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    const apiErr =
      error.response?.data?.code !== undefined
        ? new ApiError(
            error.response.data.code,
            error.response.data.message ?? error.message,
            error.response.data.traceId,
          )
        : error

    ElMessage.error(getErrorMessage(apiErr))
    return Promise.reject(apiErr)
  },
)

aiHttp.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const userStore = useUserStore()

    if (error.response?.status === 401 && !(original as Record<string, unknown>)._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push((token) => {
            if (token) {
              original.headers.Authorization = `Bearer ${token}`
              resolve(aiHttp(original))
            } else {
              reject(error)
            }
          })
        })
      }
      ;(original as Record<string, unknown>)._retry = true
      isRefreshing = true
      try {
        const newToken = await userStore.refresh()
        processQueue(newToken)
        original.headers.Authorization = `Bearer ${newToken}`
        return aiHttp(original)
      } catch (refreshErr) {
        processQueue(null)
        userStore.logout()
        return Promise.reject(refreshErr)
      } finally {
        isRefreshing = false
      }
    }

    ElMessage.error(getErrorMessage(error))
    return Promise.reject(error)
  },
)

/*
 * request 兼容两种历史写法：
 * 既可直接当函数调用 request<T>({ url, method, data })，
 * 也可当对象调用 request.post<T>(url, data)。
 */
type RequestConfig = AxiosRequestConfig & { url: string; method?: string; data?: unknown }

function exec<T = unknown>(config: RequestConfig | string): Promise<T> {
  // 兼容历史写法：request<UserInfo>('/auth/me') 直接传字符串 URL（默认 GET）。
  // 否则字符串会被当成 RequestConfig 对象，config.url 为 undefined，
  // 请求会打到 baseURL('/api') 本身，导致 NoResourceFoundException: No static resource api.
  if (typeof config === 'string') {
    return http.get<unknown, T>(config)
  }
  const method = (config.method ?? 'GET').toUpperCase()
  const url = config.url
  const data = config.data
  const map: Record<string, () => Promise<unknown>> = {
    POST: () => http.post(url, data, config),
    PUT: () => http.put(url, data, config),
    PATCH: () => http.patch(url, data, config),
    DELETE: () => http.delete(url, config),
  }
  const fn = map[method] ?? (() => http.get(url, config))
  return fn() as unknown as Promise<T>
}

export const request = Object.assign(exec, {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    http.get<unknown, T>(url, config),
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    http.post<unknown, T>(url, data, config),
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    http.put<unknown, T>(url, data, config),
  patch: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    http.patch<unknown, T>(url, data, config),
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    http.delete<unknown, T>(url, config),
})
