export function formatPrice(n: number | string): string {
  const num = typeof n === 'string' ? parseFloat(n) : n
  if (isNaN(num)) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toLocaleString('en-US')
}

export function formatTimeAgo(date: string | Date): string {
  const d = typeof date === 'string' ? new Date(date.replace(/-/g, '/')) : date
  const now = Date.now()
  const diff = Math.floor((now - d.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  if (diff < 604800) return Math.floor(diff / 86400) + ' 天前'
  return d.toLocaleDateString('zh-CN')
}

export function formatCountdown(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return [h, m, s].map(n => n.toString().padStart(2, '0')).join(':')
}
