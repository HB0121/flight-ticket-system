import { http } from './http.js'
export { login, register, logout, getMe } from './authApi.js'
export { fetchFlights, fetchFlight, fetchPriceHistory } from './flightApi.js'
export { syncFlights } from './flightSyncApi.js'
export { requestAdvice } from './aiTravelApi.js'
export {
  createConversation,
  listConversations,
  getMessages,
  sendMessage,
  deleteConversation
} from './conversationApi.js'

// Compatibility barrel only. New domain APIs should live in dedicated modules.
const CRAWLER_TIMEOUT_MS = 130000

export async function fetchLatestJob() {
  const response = await http.get('/api/crawl/latest')
  return response.data
}

export async function runCrawler(payload = {}) {
  const response = await http.post('/api/crawl/run', payload, { timeout: CRAWLER_TIMEOUT_MS })
  return response.data
}

export async function requestTiming(message) {
  const response = await http.post('/api/ai/timing', { message })
  return response.data
}
