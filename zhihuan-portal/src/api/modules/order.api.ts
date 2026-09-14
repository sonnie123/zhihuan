import http from '../client'
import type { OrderDTO } from '@/types/entity/order'

export const orderApi = {
  getList: (status?: number) => http.get<OrderDTO[]>('/order/list', { params: { status } }),
  getById: (id: number) => http.get<OrderDTO>('/order/' + id),
}