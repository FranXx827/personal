/**
 * 订单 / 购物车 API
 */
import { request } from '../http'
import type { PageQuery, PageResult } from '@/types/api'

export interface CartItem {
  skuId: number
  productId: number
  title: string
  cover: string
  price: number
  quantity: number
  specs: string
  selected: boolean
}

export interface Order {
  id: number
  orderNo: string
  status: 'PENDING_PAY' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELED' | 'REFUNDING'
  totalAmount: number
  items: OrderItem[]
  createdAt: string
}

export interface OrderItem {
  skuId: number
  title: string
  price: number
  quantity: number
  cover: string
}

export interface CreateOrderRequest {
  items: { skuId: number; quantity: number }[]
  addressId: number
  couponId?: number
}

export const cartApi = {
  list: () => request<CartItem[]>('/cart'),
  add: (skuId: number, quantity: number) =>
    request<void>({ url: '/cart/items', method: 'POST', data: { skuId, quantity } } as never),
  update: (skuId: number, quantity: number) =>
    request<void>({ url: `/cart/items/${skuId}`, method: 'PATCH', data: { quantity } } as never),
  remove: (skuId: number) =>
    request<void>({ url: `/cart/items/${skuId}`, method: 'DELETE' } as never),
  selectAll: (selected: boolean) =>
    request<void>({ url: '/cart/select-all', method: 'POST', data: { selected } } as never),
}

export const orderApi = {
  create: (data: CreateOrderRequest) =>
    request<{ orderNo: string }>({ url: '/orders', method: 'POST', data } as never),

  detail: (orderNo: string) => request<Order>(`/orders/${orderNo}`),

  list: (query: PageQuery) =>
    request<PageResult<Order>>({ url: '/orders', method: 'GET', params: query } as never),

  cancel: (orderNo: string, reason?: string) =>
    request<void>({
      url: `/orders/${orderNo}/cancel`,
      method: 'POST',
      data: { reason },
    } as never),

  pay: (orderNo: string) =>
    request<{ payUrl: string }>({
      url: `/orders/${orderNo}/pay`,
      method: 'POST',
    } as never),
}

export const seckillApi = {
  /** 秒杀下单 (高并发：Redis+Lua 原子扣减) */
  seckill: (skuId: number) =>
    request<{ orderNo: string }>({
      url: `/seckill/${skuId}`,
      method: 'POST',
    } as never),
}
