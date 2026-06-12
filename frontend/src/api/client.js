import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

// Request interceptor: attach auth token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle 401
http.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.dispatchEvent(new CustomEvent('auth:logout'))
    }
    return Promise.reject(error)
  }
)

export async function fetchFlights(params = {}) {
  const response = await http.get('/api/flights', { params })
  return response.data
}

export async function fetchFlight(id) {
  const response = await http.get(`/api/flights/${id}`)
  return response.data
}

export async function fetchPriceHistory(id) {
  const response = await http.get(`/api/flights/${id}/price-history`)
  return response.data
}

export async function fetchLatestJob() {
  const response = await http.get('/api/crawl/latest')
  return response.data
}

export async function runCrawler(payload = {}) {
  const response = await http.post('/api/crawl/run', payload)
  return response.data
}

export async function requestAdvice(message) {
  const response = await http.post('/api/ai/advice', { message })
  return response.data
}

export async function requestTiming(message) {
  const response = await http.post('/api/ai/timing', { message })
  return response.data
}

// Conversation APIs
export async function createConversation(title) {
  const response = await http.post('/api/ai/conversations', { title })
  return response.data
}

export async function listConversations() {
  const response = await http.get('/api/ai/conversations')
  return response.data
}

export async function getMessages(sessionId) {
  const response = await http.get(`/api/ai/conversations/${sessionId}/messages`)
  return response.data
}

export async function sendMessage(sessionId, message) {
  const response = await http.post(`/api/ai/conversations/${sessionId}/messages`, { message })
  return response.data
}

export async function deleteConversation(sessionId) {
  await http.delete(`/api/ai/conversations/${sessionId}`)
}

// Auth APIs
export async function login(username, password) {
  const response = await http.post('/api/auth/login', { username, password })
  return response.data
}

export async function register(username, password, nickname) {
  const response = await http.post('/api/auth/register', { username, password, nickname })
  return response.data
}

export async function logout() {
  await http.post('/api/auth/logout')
}

export async function getMe() {
  const response = await http.get('/api/auth/me')
  return response.data
}
