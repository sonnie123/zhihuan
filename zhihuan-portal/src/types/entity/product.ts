// 商品实体 (对齐 zhihuan-product ProductDTO)
export type ProductCondition = 1 | 2 | 3 | 4  // 1全新 2几乎全新 3轻微使用 4明显使用
export type ProductStatus = 1 | 2 | 3 | 4 | 5 | 6  // 草稿/审核中/在售/已售/下架/违规
export type AuditStatus = 0 | 1 | 2  // 0未审核 1通过 2拒绝

export interface ProductDTO {
  id: number
  sellerId: number
  seller?: import('./user').UserDTO
  categoryId: number
  categoryName?: string
  title: string
  description: string
  coverImage: string
  images: string[]
  price: number
  originalPrice?: number
  condition: ProductCondition
  conditionText?: string
  status: ProductStatus
  viewCount: number
  favoriteCount: number
  publishTime: string
  location?: string
  tags?: string[]
  aiAuditStatus?: AuditStatus
  aiPriceRange?: { low: number; high: number; reason?: string }
}

export interface CategoryDTO {
  id: number
  parentId: number
  name: string
  level: 1 | 2 | 3
  icon: string
  sort: number
  children?: CategoryDTO[]
}
