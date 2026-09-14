export const storage = {
  get<T = any>(key: string): T | null {
    try {
      const v = localStorage.getItem(key)
      return v ? JSON.parse(v) : null
    } catch { return null }
  },
  set(key: string, value: any) {
    try { localStorage.setItem(key, JSON.stringify(value)) } catch {}
  },
  remove(key: string) {
    localStorage.removeItem(key)
  },
}
