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
