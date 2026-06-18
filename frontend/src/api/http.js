import axios from 'axios'

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

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT_MS
})

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
    if (error.response?.status === 401) {
      const storage = getStorage()
      storage?.removeItem('token')
      storage?.removeItem('user')
      dispatchAuthLogout()
    }

    return Promise.reject(error)
  }
)

export default http
