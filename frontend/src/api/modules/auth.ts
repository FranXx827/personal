import { request } from '../http'

export type UserType = 'MERCHANT' | 'BUYER'

export interface LoginRequest {
  username: string
  password: string
  type: UserType
}

export interface RegisterMerchantRequest {
  merchantName: string
  username: string
  password: string
  phone?: string
}

export interface RegisterBuyerRequest {
  name: string
  username: string
  password: string
  phone?: string
}

export interface TokenResponse {
  accessToken: string
  refreshToken?: string
  expiresIn: number
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  roles: UserType[]
}

export const authApi = {
  login: (data: LoginRequest) =>
    request<TokenResponse>({
      url: '/auth/login',
      method: 'POST',
      data,
    } as never),

  registerMerchant: (data: RegisterMerchantRequest) =>
    request<void>({
      url: '/auth/register/MERCHANT',
      method: 'POST',
      data: { ...data, type: 'MERCHANT' },
    } as never),

  registerBuyer: (data: RegisterBuyerRequest) =>
    request<void>({
      url: '/auth/register/BUYER',
      method: 'POST',
      data: { ...data, type: 'BUYER' },
    } as never),

  refresh: (refreshToken: string) =>
    request<TokenResponse>({
      url: '/auth/refresh',
      method: 'POST',
      data: { refreshToken },
    } as never),

  logout: () =>
    request<void>({
      url: '/auth/logout',
      method: 'POST',
    } as never),

  getProfile: () => request<UserInfo>('/auth/me'),
}
