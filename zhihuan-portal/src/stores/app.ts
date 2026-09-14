import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const loading = ref(false)
  const sidebarCollapsed = ref(false)

  function setLoading(v: boolean) {
    loading.value = v
  }
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { loading, sidebarCollapsed, setLoading, toggleSidebar }
})