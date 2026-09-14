import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'

export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'
export const API_BASE = import.meta.env.VITE_API_BASE || '/api'

const http: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截
http.interceptors.request.use(
  (config) => {
    // 从 localStorage 取 token
    const token = localStorage.getItem('zh-token')
    if (token && config.headers) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截
http.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 0) return res.data
      if (res.code === 401) {
        // 未登录
        localStorage.removeItem('zh-token')
        location.href = '/'
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    console.error('[HTTP ERROR]', error.message)
    return Promise.reject(error)
  }
)

export default http
