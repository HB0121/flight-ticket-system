import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

export async function fetchFlights(params = {}) {
  const response = await http.get('/api/flights', { params })
  return response.data
}

export async function fetchLatestJob() {
  const response = await http.get('/api/crawl/latest')
  return response.data
}

export async function runCrawler() {
  const response = await http.post('/api/crawl/run')
  return response.data
}

export async function requestAdvice(message) {
  const response = await http.post('/api/ai/advice', { message })
  return response.data
}

