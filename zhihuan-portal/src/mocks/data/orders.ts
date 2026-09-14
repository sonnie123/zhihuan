import type { OrderDTO } from '@/types/entity/order'
import { getProductById } from './products'

const statusMap: Record<number, string> = {
  1: '待支付', 2: '已支付', 3: '已发货', 4: '已完成', 5: '已评价', 91: '退款中', 92: '已退款', 99: '已取消',
}

export const orders: OrderDTO[] = [
  { id: 1, orderNo: 'ZH202409140001', buyerId: 1, sellerId: 1001, productId: 1, productTitle: getProductById(1)!.title, productImage: getProductById(1)!.coverImage, productPrice: 6299, shippingFee: 0, totalAmount: 6299, payAmount: 6299, status: 1, createTime: '2024-09-14 10:30:00' },
  { id: 2, orderNo: 'ZH202409130002', buyerId: 1, sellerId: 1006, productId: 8, productTitle: getProductById(8)!.title, productImage: getProductById(8)!.coverImage, productPrice: 820, shippingFee: 12, totalAmount: 832, payAmount: 832, status: 2, createTime: '2024-09-13 16:20:00', payTime: '2024-09-13 16:25:00' },
  { id: 3, orderNo: 'ZH202409120003', buyerId: 1, sellerId: 1003, productId: 10, productTitle: getProductById(10)!.title, productImage: getProductById(10)!.coverImage, productPrice: 99, shippingFee: 8, totalAmount: 107, payAmount: 107, status: 3, createTime: '2024-09-12 09:15:00', payTime: '2024-09-12 09:20:00', shipTime: '2024-09-12 14:00:00' },
  { id: 4, orderNo: 'ZH202409050004', buyerId: 1, sellerId: 1002, productId: 6, productTitle: getProductById(6)!.title, productImage: getProductById(6)!.coverImage, productPrice: 168, shippingFee: 8, totalAmount: 176, payAmount: 176, status: 4, createTime: '2024-09-05 11:00:00', payTime: '2024-09-05 11:05:00', shipTime: '2024-09-06 09:00:00', confirmTime: '2024-09-08 16:30:00' },
]

export const orderStatusText = statusMap
