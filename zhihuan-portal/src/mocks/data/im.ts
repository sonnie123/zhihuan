import type { Conversation, ChatMessage } from '@/types/entity/im'
import { getUserById, CURRENT_USER_ID } from './users'
import { getProductById } from './products'

// 会话列表
export const conversations: Conversation[] = [
  {
    id: 'c1',
    targetUser: getUserById(1001)!,
    productId: 1,
    productTitle: 'iPhone 15 Pro 256G 原色钛金属',
    productImage: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400&h=400&fit=crop',
    productPrice: 6299,
    lastMessage: '价格可以小刀吗?',
    lastTime: '14:32',
    unreadCount: 2,
    pinned: true,
  },
  {
    id: 'c2',
    targetUser: getUserById(1006)!,
    productId: 8,
    productTitle: 'COACH 女士手提单肩包',
    productImage: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=400&h=400&fit=crop',
    productPrice: 899,
    lastMessage: '[AI 议价] 双方 Agent 已就 ¥820 达成一致',
    lastTime: '13:18',
    unreadCount: 0,
  },
  {
    id: 'c3',
    targetUser: getUserById(1002)!,
    productId: 6,
    productTitle: '《三体》全集三本 大刘签名版',
    productImage: 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400&h=400&fit=crop',
    productPrice: 168,
    lastMessage: '收到啦,书保存得真好!',
    lastTime: '昨天',
    unreadCount: 0,
    muted: true,
  },
  {
    id: 'c4',
    targetUser: getUserById(1003)!,
    productId: 10,
    productTitle: '优衣库摇粒绒外套',
    productImage: 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400&h=400&fit=crop',
    productPrice: 99,
    lastMessage: '已下单,请尽快发货哦~',
    lastTime: '昨天',
    unreadCount: 1,
  },
  {
    id: 'c5',
    targetUser: getUserById(1005)!,
    productId: 11,
    productTitle: '德龙 EC685 半自动咖啡机',
    productImage: 'https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=400&h=400&fit=crop',
    productPrice: 580,
    lastMessage: '能便宜点吗?',
    lastTime: '前天',
    unreadCount: 0,
  },
  {
    id: 'c6',
    targetUser: getUserById(1007)!,
    productId: 13,
    productTitle: 'Switch OLED 白色 港版',
    productImage: 'https://images.unsplash.com/photo-1612036782180-6f0822045d23?w=400&h=400&fit=crop',
    productPrice: 1880,
    lastMessage: '游戏都包含哪些?',
    lastTime: '前天',
    unreadCount: 0,
  },
]

// 议价剧本 - iPhone 15 Pro
const iphoneProduct = getProductById(1)!
const iphoneMessages: ChatMessage[] = [
  {
    id: 1001, conversationId: 'c1', senderId: CURRENT_USER_ID, receiverId: 1001, type: 'text',
    content: '你好,iPhone 这台还在吗?成色怎么样?',
    status: 'read', sendTime: '14:20',
  },
  {
    id: 1002, conversationId: 'c1', senderId: 1001, receiverId: CURRENT_USER_ID, type: 'text',
    content: '在的,9 成新,带原盒,电池效率 96%。无磕碰无划痕。',
    status: 'read', sendTime: '14:21',
  },
  {
    id: 1003, conversationId: 'c1', senderId: CURRENT_USER_ID, receiverId: 1001, type: 'text',
    content: '6200 能出吗?',
    status: 'read', sendTime: '14:25',
  },
  {
    id: 1004, conversationId: 'c1', senderId: 1001, receiverId: CURRENT_USER_ID, type: 'text',
    content: '最低 6100,不能再少了',
    status: 'read', sendTime: '14:26',
  },
  {
    id: 1005, conversationId: 'c1', senderId: CURRENT_USER_ID, receiverId: 1001, type: 'text',
    content: '价格可以小刀吗?',
    status: 'read', sendTime: '14:32',
  },
  {
    id: 1006, conversationId: 'c1', senderId: 0, receiverId: CURRENT_USER_ID, type: 'system',
    content: '智换 AI 议价助手已介入,正在为你们协商价格',
    status: 'read', sendTime: '14:32:30',
  },
  {
    id: 1007, conversationId: 'c1', senderId: 0, receiverId: CURRENT_USER_ID, type: 'ai_bargain',
    content: 'AI 议价进行中',
    status: 'read', sendTime: '14:33',
    productId: 1,
    bargainData: {
      productId: 1,
      initialPrice: 6299,
      targetPrice: 5800,
      currentOffer: 5950,
      status: 'negotiating',
      rounds: [
        { by: 'buyer', price: 5800, round: 1, message: '我预算有限,5800 行不行?', time: '14:33:05' },
        { by: 'ai', price: 5800, round: 1, message: '已分析:卖家心理价位 6000,建议出价 ¥5,950', time: '14:33:08' },
        { by: 'buyer', price: 5950, round: 2, message: '调整为 ¥5,950', time: '14:33:12' },
        { by: 'seller', price: 6100, round: 2, message: '我最低 6100', time: '14:33:20' },
        { by: 'ai', price: 5950, round: 2, message: '检测到差距 ¥150,建议让步到 ¥6,050 试探', time: '14:33:25' },
        { by: 'buyer', price: 6050, round: 3, message: '出价 ¥6,050', time: '14:33:32' },
      ],
    },
  },
]

// COACH 包会话
const coachMessages: ChatMessage[] = [
  {
    id: 2001, conversationId: 'c2', senderId: CURRENT_USER_ID, receiverId: 1006, type: 'text',
    content: '这款包是正品吗?有购买凭证吗?',
    status: 'read', sendTime: '11:30',
  },
  {
    id: 2002, conversationId: 'c2', senderId: 1006, receiverId: CURRENT_USER_ID, type: 'text',
    content: '正品,日本中古店购入,有收据。支持验货宝。',
    status: 'read', sendTime: '11:35',
  },
  {
    id: 2003, conversationId: 'c2', senderId: CURRENT_USER_ID, receiverId: 1006, type: 'text',
    content: '750 出吗?',
    status: 'read', sendTime: '11:40',
  },
  {
    id: 2004, conversationId: 'c2', senderId: 1006, receiverId: CURRENT_USER_ID, type: 'text',
    content: '最低 850',
    status: 'read', sendTime: '11:45',
  },
  {
    id: 2005, conversationId: 'c2', senderId: 0, receiverId: CURRENT_USER_ID, type: 'system',
    content: '智换 AI 议价助手介入',
    status: 'read', sendTime: '11:46',
  },
  {
    id: 2006, conversationId: 'c2', senderId: 0, receiverId: CURRENT_USER_ID, type: 'ai_bargain',
    content: 'AI 议价',
    status: 'read', sendTime: '11:47',
    bargainData: {
      productId: 8,
      initialPrice: 899,
      targetPrice: 750,
      currentOffer: 820,
      status: 'agreed',
      finalPrice: 820,
      rounds: [
        { by: 'buyer', price: 750, round: 1, message: '750', time: '11:47:01' },
        { by: 'seller', price: 850, round: 1, message: '850 最低', time: '11:47:30' },
        { by: 'ai', price: 800, round: 1, message: '建议中位数 ¥800', time: '11:47:45' },
        { by: 'buyer', price: 800, round: 2, message: '800', time: '11:48:00' },
        { by: 'seller', price: 840, round: 2, message: '820 行不行?', time: '11:48:20' },
        { by: 'ai', price: 820, round: 2, message: '双方接受 ¥820 成交', time: '11:48:30' },
      ],
    },
  },
  {
    id: 2007, conversationId: 'c2', senderId: 0, receiverId: CURRENT_USER_ID, type: 'system',
    content: '🎉 双方已就 ¥820 达成一致,请尽快完成支付',
    status: 'read', sendTime: '11:50',
  },
]

// 普通消息
const otherMessages: ChatMessage[] = [
  {
    id: 3001, conversationId: 'c3', senderId: 1002, receiverId: CURRENT_USER_ID, type: 'text',
    content: '收到啦,书保存得真好!',
    status: 'read', sendTime: '昨天 21:30',
  },
]

export const allMessages: Record<string, ChatMessage[]> = {
  c1: iphoneMessages,
  c2: coachMessages,
  c3: otherMessages,
}

export function getMessagesByConversation(conversationId: string) {
  return allMessages[conversationId] || []
}
