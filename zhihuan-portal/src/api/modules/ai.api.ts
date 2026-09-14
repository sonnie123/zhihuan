import http from '../client'

export interface AIPublishResult {
  brand: string
  model: string
  conditionScore: number
  color: string
  categoryCandidates: { id: number; name: string; confidence: number }[]
  suggestedPrice: number
  priceLow: number
  priceHigh: number
  priceReason: string
  title: string
  description: string
  aiAuditStatus: 'pass' | 'warn' | 'reject'
  auditReason: string
}

export const aiApi = {
  startPublish: (images: string[]) => http.post<{ taskId: string }>('/ai/publish/generate', { images }),
  getPublishResult: (taskId: string) => http.get<AIPublishResult>('/ai/publish/result/' + taskId),
  startBargain: (productId: number, offer: number) => http.post<{ sessionId: string; initialPrice: number; currentOffer: number }>('/ai/bargain/start', { productId, offer }),
}