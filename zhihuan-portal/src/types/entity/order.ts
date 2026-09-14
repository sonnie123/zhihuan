// 订单实体 (对齐 zhihuan-trade OrderDTO)
export type OrderStatus =
  | 1   // 待支付
  | 2   // 已支付
  | 3   // 已发货
  | 4   // 已完成
  | 5   // 已评价
  | 91  // 退款中
  | 92  // 已退款
  | 99  // 取消

export interface OrderDTO {
  id: number
  orderNo: string
  buyerId: number
  sellerId: number
  productId: number
  productTitle: string
  productImage: string
  productPrice: number
  shippingFee: number
  totalAmount: number
  payAmount: number
  status: OrderStatus
  createTime: string
  payTime?: string
  shipTime?: string
  confirmTime?: string
}
