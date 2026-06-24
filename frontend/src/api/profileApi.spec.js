import { beforeEach, describe, expect, it, vi } from 'vitest'

const axiosMock = vi.hoisted(() => {
  const client = {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() }
    }
  }

  return {
    client,
    create: vi.fn(() => client)
  }
})

vi.mock('axios', () => ({
  default: {
    create: axiosMock.create
  }
}))

describe('profileApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('uses dedicated favorites and search history endpoints', async () => {
    const profileApi = await import('./profileApi.js')
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ id: 1 }] })
    axiosMock.client.post.mockResolvedValueOnce({ data: { id: 8 } })
    axiosMock.client.delete.mockResolvedValueOnce({ data: null })
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ id: 11 }] })

    await profileApi.fetchFavorites()
    await profileApi.addFavorite({ flightId: 8 })
    await profileApi.removeFavorite(8)
    await profileApi.fetchSearchHistory()

    expect(axiosMock.client.get).toHaveBeenNthCalledWith(1, '/api/me/favorites')
    expect(axiosMock.client.post).toHaveBeenCalledWith('/api/me/favorites', { flightId: 8 })
    expect(axiosMock.client.delete).toHaveBeenCalledWith('/api/me/favorites/8')
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(2, '/api/me/search-history')
  })
})
