import http from '../client'
import type { ProductDTO, CategoryDTO } from '@/types/entity/product'

export const productApi = {
  getFeed: () => http.get<ProductDTO[]>('/product/feed'),
  getAIRecommend: () => http.get<ProductDTO[]>('/product/ai-recommend'),
  getFollowFeed: () => http.get<ProductDTO[]>('/product/follow-feed'),
  getGuess: () => http.get<ProductDTO[]>('/product/guess'),
  getSeckill: () => http.get<ProductDTO[]>('/product/seckill'),
  getById: (id: number) => http.get<ProductDTO>('/product/' + id),
  getSimilar: (id: number) => http.get<ProductDTO[]>('/product/' + id + '/similar'),
  getCategories: () => http.get<CategoryDTO[]>('/product/categories'),
}