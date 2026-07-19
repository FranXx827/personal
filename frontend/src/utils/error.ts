/**
 * API 错误 → 用户可读消息
 */
import { ApiError } from '@/types/api'

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 2001) return '登录已过期，请重新登录'
    if (error.code === 2002) return '您没有权限执行此操作'
    if (error.code === 2003) return '账号已被禁用，请联系管理员'
    if (error.code === 3002) return '库存不足'
    if (error.code === 4001) return '订单状态不允许此操作'
    if (error.code === 10001) return 'AI 服务繁忙，请稍后再试'
    return error.message || '请求失败'
  }
  if (error instanceof Error) {
    if (error.message === 'Network Error') return '网络异常，请检查连接'
    return error.message
  }
  return '未知错误'
}
