/**
 * 商家相关 API（看板 / 店铺商品）
 */
import { request } from '../http'
import type { PageResult } from '@/types/api'

export interface MerchantProduct {
  id: number
  title: string
  description: string
  price: number
  cover: string
  merchantId: number
  merchantName: string
  sales: number
  rating: number
  /** 0=下架 1=上架 */
  status: number
}

export interface MerchantDashboard {
  merchantId: number
  merchantName: string
  productCount: number
  ongoingOrders: number
  revenue: number
  products: MerchantProduct[]
}

export const merchantApi = {
  dashboard: () => request<MerchantDashboard>('/merchant/dashboard'),

  products: (page = 1, pageSize = 20) =>
    request<PageResult<MerchantProduct>>({
      url: '/merchant/products',
      method: 'GET',
      params: { page, pageSize },
    } as never),
}
