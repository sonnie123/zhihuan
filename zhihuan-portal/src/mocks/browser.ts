import axios, { type AxiosRequestConfig } from 'axios'
import { matchHandler } from './handlers'

/**
 * Mock 拦截器 - 通过 axios adapter 拦截请求
 * - 不污染 window.fetch (生产环境无需担心副作用)
 * - 如果检测到 window.fetch 被旧版 mock 破坏,自动从 window.__zhOrigFetch 恢复
 */
export function setupMock() {
  if (!import.meta.env.DEV) return
  if (import.meta.env.VITE_USE_MOCK !== 'true') return

  // 恢复 window.fetch (如果 index.html 的 guard 脚本保存了原 fetch)
  try {
    if (typeof window !== 'undefined') {
      if (typeof window.fetch !== 'function' && (window as any).__zhOrigFetch) {
        window.fetch = (window as any).__zhOrigFetch
        console.log('%c[智焕] 检测到 fetch 被破坏,已自动恢复', 'color: #F59E0B;')
      }
    }
  } catch { /* noop */ }

  const realAdapter = axios.defaults.adapter

  const mockAdapter = async (config: AxiosRequestConfig): Promise<any> => {
    const baseURL = (config.baseURL || '').replace(/^https?:\/\/[^/]+/, '')
    const url = (config.url || '').replace(/^https?:\/\/[^/]+/, '')
    const fullPath = (baseURL + (url.startsWith('/') ? url : '/' + url)).replace(/^\/+/, '/')
    const path = fullPath.replace(/^\/api/, '')
    const method = (config.method || 'get').toUpperCase()

    const match = matchHandler(path, method)

    if (match) {
      await new Promise(r => setTimeout(r, 200 + Math.random() * 300))
      let body: any = config.data
      if (typeof body === 'string') {
        try { body = JSON.parse(body) } catch { /* keep as string */ }
      }
      const result = await match.handler(match.params, body)
      return {
        data: result,
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
        request: {},
      }
    }

    return realAdapter!(config)
  }

  axios.defaults.adapter = mockAdapter

  console.log(
    '%c[智焕] Mock 已启用 (axios adapter) %c 共 22 个端点',
    'color: #7C3AED; font-weight: bold;',
    'color: #06B6D4; font-weight: bold;'
  )
}
