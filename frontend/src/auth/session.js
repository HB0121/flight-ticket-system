const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

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
  try { sessionStorage.removeItem('flightSearchPage_v1') } catch { /* SSR guard */ }
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

  if (!token) {
    markSessionAnonymous()
    return false
  }

  if (sessionStatus === 'authenticated') {
    return true
  }

  if (pendingSessionCheck) {
    return pendingSessionCheck
  }

  pendingSessionCheck = (async () => {
    try {
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
      clearStoredSession()
      markSessionAnonymous()
      return false
    } finally {
      pendingSessionCheck = null
    }
  })()

  return pendingSessionCheck
}
