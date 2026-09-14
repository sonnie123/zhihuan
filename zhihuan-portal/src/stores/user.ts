import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserDTO, UserStats } from '@/types/entity/user'
import { userApi } from '@/api/modules/user.api'

export const useUserStore = defineStore('user', () => {
  const current = ref<UserDTO | null>(null)
  const stats = ref<UserStats>({ sellCount: 0, buyCount: 0, favoriteCount: 0, footprintCount: 0 })
  const followers = ref<UserDTO[]>([])
  const followings = ref<UserDTO[]>([])
  const isLogged = computed(() => current.value !== null)

  async function loadCurrent() {
    try {
      current.value = await userApi.getCurrent()
      if (current.value) {
        stats.value = await userApi.getStats(current.value.id)
      }
    } catch (e) {
      console.warn('[user] 未登录或加载失败', e)
    }
  }

  async function loadFollowers() {
    followers.value = await userApi.getFollowers()
  }
  async function loadFollowings() {
    followings.value = await userApi.getFollowings()
  }

  return { current, stats, followers, followings, isLogged, loadCurrent, loadFollowers, loadFollowings }
})