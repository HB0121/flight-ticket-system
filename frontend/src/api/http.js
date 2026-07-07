import axios from 'axios'
import { clearStoredSession, markSessionAnonymous } from '../auth/session.js'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const REQUEST_TIMEOUT_MS = 15000

function getStorage() {
  return typeof globalThis !== 'undefined' ? globalThis.localStorage ?? null : null
}

function dispatchAuthLogout() {
  const target = typeof window !== 'undefined' ? window : globalThis
  const EventCtor = target?.CustomEvent ?? globalThis?.CustomEvent

  if (typeof target?.dispatchEvent === 'function' && typeof EventCtor === 'function') {
    target.dispatchEvent(new EventCtor('auth:logout'))
  }
}

// 全项目共用的 axios 实例，所有业务 API 都通过它访问后端。
export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS
})

// 请求发出前自动把本地 token 放到 Authorization 头里。
http.interceptors.request.use(config => {
  const token = getStorage()?.getItem('token')

  if (!token) {
    return config
  }

  if (config.headers && typeof config.headers.set === 'function') {
    config.headers.set('Authorization', `Bearer ${token}`)
  } else {
    config.headers = {
      ...(config.headers ?? {}),
      Authorization: `Bearer ${token}`
    }
  }

  return config
})

http.interceptors.response.use(
  response => response,
  error => {
    // 后端返回 401 说明登录态已失效，前端需要清空本地会话并通知界面更新。
    if (error.response?.status === 401) {
      clearStoredSession()
      markSessionAnonymous()
      dispatchAuthLogout()
    }

    return Promise.reject(error)
  }
)

export default http
