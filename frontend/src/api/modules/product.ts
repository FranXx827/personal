/**
 * 商品相关 API
 */
import { request } from '../http'
import type { PageQuery, PageResult } from '@/types/api'

export interface Product {
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

export interface ProductQuery extends PageQuery {
  categoryId?: number
  minPrice?: number
  maxPrice?: number
  sort?: 'price_asc' | 'price_desc' | 'sales_desc' | 'newest'
}

export const productApi = {
  search: (query: ProductQuery) =>
    request<PageResult<Product>>({
      url: '/products',
      method: 'GET',
      params: query,
    } as never),

  detail: (id: number) => request<Product>(`/products/${id}`),

  hot: (limit = 10) =>
    request<Product[]>({
      url: '/products/hot',
      method: 'GET',
      params: { limit },
    } as never),
}
