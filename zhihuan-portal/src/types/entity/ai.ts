// AI 上架产物
export type AIStepStatus = 'pending' | 'running' | 'success' | 'failed'
export type AIStepName = 'image' | 'category' | 'price' | 'description' | 'audit'

export interface AIStepResult {
  name: AIStepName
  label: string
  status: AIStepStatus
  duration?: number
  data?: any
  error?: string
}

export interface AIPublishDraft {
  images: string[]
  // 图像识别
  brand?: string
  model?: string
  conditionScore?: number
  color?: string
  // 类目
  categoryId?: number
  categoryName?: string
  categoryCandidates?: { id: number; name: string; confidence: number }[]
  // 价格
  suggestedPrice?: number
  priceLow?: number
  priceHigh?: number
  priceReason?: string
  // 文案
  title?: string
  description?: string
  // 审核
  aiAuditStatus?: 'pass' | 'warn' | 'reject'
  auditReason?: string
}
