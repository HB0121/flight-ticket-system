import { http } from './http.js'
export { login, register, logout, getMe } from './authApi.js'

// Compatibility barrel only. New domain APIs should live in dedicated modules.
const CRAWLER_TIMEOUT_MS = 130000

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
  const response = await http.post('/api/crawl/run', payload, { timeout: CRAWLER_TIMEOUT_MS })
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
