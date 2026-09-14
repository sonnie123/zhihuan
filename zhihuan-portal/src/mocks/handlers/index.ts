import { users, CURRENT_USER_ID, getUserById } from '../data/users'
import { products, categories, getProductById } from '../data/products'
import { conversations, allMessages, getMessagesByConversation } from '../data/im'
import { orders } from '../data/orders'

const ok = <T>(data: T) => ({ code: 0, message: 'ok', data, timestamp: Date.now() })

export const handlers = [
  // 用户
  {
    url: '/api/user/current',
    method: 'GET',
    handler: () => ok(getUserById(CURRENT_USER_ID)),
  },
  {
    url: '/api/user/:id',
    method: 'GET',
    handler: (params: any) => ok(getUserById(Number(params.id))),
  },
  {
    url: '/api/user/:id/stats',
    method: 'GET',
    handler: (params: any) => ok({
      sellCount: 12,
      buyCount: 28,
      favoriteCount: 56,
      footprintCount: 142,
    }),
  },
  {
    url: '/api/user/followers',
    method: 'GET',
    handler: () => ok(users.slice(2, 8)),
  },
  {
    url: '/api/user/followings',
    method: 'GET',
    handler: () => ok(users.slice(1, 7)),
  },

  // 商品
  {
    url: '/api/product/categories',
    method: 'GET',
    handler: () => ok(categories),
  },
  {
    url: '/api/product/feed',
    method: 'GET',
    handler: () => ok(products.slice(0, 12)),
  },
  {
    url: '/api/product/ai-recommend',
    method: 'GET',
    handler: () => ok(products.slice(2, 11)),
  },
  {
    url: '/api/product/follow-feed',
    method: 'GET',
    handler: () => ok([products[1], products[3], products[6], products[9]]),
  },
  {
    url: '/api/product/guess',
    method: 'GET',
    handler: () => ok(products.slice(4, 16)),
  },
  {
    url: '/api/product/seckill',
    method: 'GET',
    handler: () => ok(products.slice(0, 6)),
  },
  {
    url: '/api/product/:id',
    method: 'GET',
    handler: (params: any) => {
      const p = getProductById(Number(params.id))
      if (!p) return { code: 404, message: '商品不存在', data: null }
      return ok(p)
    },
  },
  {
    url: '/api/product/:id/similar',
    method: 'GET',
    handler: (params: any) => {
      const p = getProductById(Number(params.id))
      if (!p) return ok([])
      return ok(products.filter(x => x.categoryId === p.categoryId && x.id !== p.id).slice(0, 6))
    },
  },

  // AI
  {
    url: '/api/ai/publish/generate',
    method: 'POST',
    handler: () => ok({ taskId: 'ai-task-' + Date.now() }),
  },
  {
    url: '/api/ai/publish/result/:taskId',
    method: 'GET',
    handler: () => ok({
      brand: 'Apple',
      model: 'iPhone 15 Pro',
      conditionScore: 9,
      color: '原色钛金属',
      categoryCandidates: [
        { id: 1, name: '手机 / iPhone', confidence: 0.96 },
        { id: 1, name: '手机 / 安卓', confidence: 0.03 },
        { id: 7, name: '数码配件', confidence: 0.01 },
      ],
      suggestedPrice: 5950,
      priceLow: 5500,
      priceHigh: 6300,
      priceReason: '基于近 30 天同型号同成色 1,283 条成交记录分析,中位数 ¥5,950,建议合理范围 ¥5,500-6,300',
      title: 'iPhone 15 Pro 256G 原色钛金属 9 成新 自用',
      description: '【自用 iPhone 15 Pro 出售】\n\n📱 型号: iPhone 15 Pro 256G 原色钛金属\n✨ 成色: 9 成新,无磕碰无划痕\n🔋 电池效率: 96%\n📦 配件: 原装盒子+原装数据线\n\n一直带壳贴膜使用,功能完好。爱思助手验机报告可提供,支持任何方式验机。北京可面交,外地顺丰到付。',
      aiAuditStatus: 'pass',
      auditReason: '内容合规,无风险项',
    }),
  },
  {
    url: '/api/ai/bargain/start',
    method: 'POST',
    handler: (params: any, body: any) => ok({
      sessionId: 'bargain-' + Date.now(),
      initialPrice: 6299,
      currentOffer: body?.offer || 5800,
    }),
  },

  // IM
  {
    url: '/api/im/conversations',
    method: 'GET',
    handler: () => ok(conversations),
  },
  {
    url: '/api/im/messages',
    method: 'GET',
    handler: (params: any) => ok(getMessagesByConversation(params.conversationId)),
  },
  {
    url: '/api/im/send',
    method: 'POST',
    handler: (params: any, body: any) => ok({
      id: Date.now(),
      conversationId: body.conversationId,
      senderId: CURRENT_USER_ID,
      receiverId: body.receiverId,
      type: body.type || 'text',
      content: body.content,
      status: 'sent',
      sendTime: new Date().toISOString().slice(11, 16),
    }),
  },

  // 订单
  {
    url: '/api/order/list',
    method: 'GET',
    handler: (params: any) => {
      if (params.status) {
        return ok(orders.filter(o => o.status === Number(params.status)))
      }
      return ok(orders)
    },
  },
  {
    url: '/api/order/:id',
    method: 'GET',
    handler: (params: any) => {
      const o = orders.find(x => x.id === Number(params.id))
      return o ? ok(o) : { code: 404, message: '订单不存在', data: null }
    },
  },

  // 通知
  {
    url: '/api/notification/unread',
    method: 'GET',
    handler: () => ok({ count: 5 }),
  },
]

// 简单的 URL 匹配
export function matchHandler(url: string, method: string) {
  const [path, query] = url.split('?')
  const searchParams: Record<string, string> = {}
  if (query) {
    query.split('&').forEach(p => {
      const [k, v] = p.split('=')
      searchParams[k] = decodeURIComponent(v || '')
    })
  }

  for (const h of handlers) {
    if (h.method !== method.toUpperCase()) continue
    const pattern = h.url.replace(/:([^/]+)/g, '([^/]+)')
    const regex = new RegExp('^' + pattern + '$')
    const match = path.match(regex)
    if (match) {
      const paramNames = (h.url.match(/:([^/]+)/g) || []).map(s => s.slice(1))
      const params: Record<string, string> = {}
      paramNames.forEach((name, i) => { params[name] = match[i + 1] })
      return { handler: h.handler, params: { ...params, ...searchParams } }
    }
  }
  return null
}
