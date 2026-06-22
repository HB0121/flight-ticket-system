import { http } from './http.js'

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

// Compatibility exports. New code should import these from their domain APIs.
export async function syncFlights({ airportCode, date }) {
  const response = await http.post('/api/admin/flights/sync', null, {
    params: {
      airportCode,
      date
    }
  })
  return response.data
}

export async function requestAdvice(message) {
  const response = await http.post('/api/ai/advice', {
    message,
    query: message
  })
  return response.data
}
