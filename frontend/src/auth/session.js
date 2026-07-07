const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// sessionStatus 缓存本轮页面生命周期中的登录校验结果，避免每次路由跳转都请求 /api/auth/me。
let sessionStatus = 'unknown'
let pendingSessionCheck = null

function getStorage() {
  return typeof globalThis !== 'undefined' ? globalThis.localStorage ?? null : null
}

export function getAuthToken() {
  return getStorage()?.getItem('token') ?? ''
}

export function clearStoredSession() {
  const storage = getStorage()
  storage?.removeItem('token')
  storage?.removeItem('user')
  try { sessionStorage.removeItem('flightSearchPage_v1') } catch { /* 兼容非浏览器环境 */ }
}

export function markSessionAuthenticated() {
  sessionStatus = 'authenticated'
}

export function markSessionAnonymous() {
  sessionStatus = 'anonymous'
}

export function resetSessionState() {
  sessionStatus = 'unknown'
  pendingSessionCheck = null
}

export async function ensureAuthenticatedSession() {
  const token = getAuthToken()

  // 没有 token 时无需请求后端，直接视为未登录。
  if (!token) {
    markSessionAnonymous()
    return false
  }

  if (sessionStatus === 'authenticated') {
    return true
  }

  // 多个路由守卫同时触发时复用同一个校验请求，避免重复访问后端。
  if (pendingSessionCheck) {
    return pendingSessionCheck
  }

  pendingSessionCheck = (async () => {
    try {
      // 用 /api/auth/me 校验 token 是否真实有效，而不是只相信 localStorage。
      const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`
        }
      })

      if (!response.ok) {
        throw new Error(`auth check failed: ${response.status}`)
      }

      markSessionAuthenticated()
      return true
    } catch {
      // token 无效或后端不可达时，清空本地会话，避免继续停留在受保护页面。
      clearStoredSession()
      markSessionAnonymous()
      return false
    } finally {
      pendingSessionCheck = null
    }
  })()

  return pendingSessionCheck
}
