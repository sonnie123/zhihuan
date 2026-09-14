// IM 消息
export type MessageType = 'text' | 'image' | 'product' | 'ai_bargain' | 'system'
export type MessageStatus = 'sending' | 'sent' | 'read' | 'failed'

export interface ChatMessage {
  id: number
  conversationId: string
  senderId: number
  receiverId: number
  type: MessageType
  content: string
  status?: MessageStatus
  sendTime: string
  productId?: number
  // AI 议价专用
  bargainData?: BargainData
}

export interface BargainOffer {
  by: 'buyer' | 'seller' | 'ai'
  price: number
  round: number
  message?: string
  time: string
}

export interface BargainData {
  productId: number
  initialPrice: number
  targetPrice: number
  currentOffer: number
  rounds: BargainOffer[]
  status: 'negotiating' | 'agreed' | 'rejected' | 'expired'
  finalPrice?: number
}

export interface Conversation {
  id: string
  targetUser: import('./user').UserDTO
  productId?: number
  productTitle?: string
  productImage?: string
  productPrice?: number
  lastMessage: string
  lastTime: string
  unreadCount: number
  pinned?: boolean
  muted?: boolean
}
