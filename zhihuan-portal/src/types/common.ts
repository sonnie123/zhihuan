// 通用 API 响应
export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface PageQuery {
  page?: number
  size?: number
  sort?: string
  order?: 'asc' | 'desc'
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
  hasMore: boolean
}
