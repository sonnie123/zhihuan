import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ProductDTO, CategoryDTO } from '@/types/entity/product'
import { productApi } from '@/api/modules/product.api'

export const useProductStore = defineStore('product', () => {
  const feed = ref<ProductDTO[]>([])
  const aiRecommend = ref<ProductDTO[]>([])
  const followFeed = ref<ProductDTO[]>([])
  const guess = ref<ProductDTO[]>([])
  const seckill = ref<ProductDTO[]>([])
  const categories = ref<CategoryDTO[]>([])

  async function loadHome() {
    const [f, ai, ff, g, s, c] = await Promise.all([
      productApi.getFeed(),
      productApi.getAIRecommend(),
      productApi.getFollowFeed(),
      productApi.getGuess(),
      productApi.getSeckill(),
      productApi.getCategories(),
    ])
    feed.value = f
    aiRecommend.value = ai
    followFeed.value = ff
    guess.value = g
    seckill.value = s
    categories.value = c
  }

  return { feed, aiRecommend, followFeed, guess, seckill, categories, loadHome }
})