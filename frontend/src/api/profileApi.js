import { http } from './http.js'

export async function fetchFavorites() {
  const response = await http.get('/api/me/favorites')
  return response.data
}

export async function addFavorite(payload) {
  const response = await http.post('/api/me/favorites', payload)
  return response.data
}

export async function removeFavorite(favoriteId) {
  await http.delete(`/api/me/favorites/${favoriteId}`)
}

export async function fetchSearchHistory() {
  const response = await http.get('/api/me/search-history')
  return response.data
}
