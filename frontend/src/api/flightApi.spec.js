import { beforeEach, describe, expect, it, vi } from 'vitest'
import router from '../router/index.js'

const axiosMock = vi.hoisted(() => {
  const client = {
    get: vi.fn(),
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

describe('flightApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('uses dedicated flight search endpoints', async () => {
    const flightApi = await import('./flightApi.js')
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ id: 1 }] })
    axiosMock.client.get.mockResolvedValueOnce({ data: { id: 1 } })
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ observedAt: '2026-06-17T08:00:00', price: 960 }] })

    await flightApi.fetchFlights({ fromCity: '上海', toCity: '北京' })
    await flightApi.fetchFlight(1)
    await flightApi.fetchPriceHistory(1)

    expect(axiosMock.client.get).toHaveBeenNthCalledWith(1, '/api/flights', {
      params: {
        fromCity: '上海',
        toCity: '北京'
      }
    })
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(2, '/api/flights/1')
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(3, '/api/flights/1/price-history')
  })
})

describe('primary navigation architecture', () => {
  it('does not expose AI as a primary phase-1 route', async () => {
    const routeNames = router.getRoutes().map(route => route.name).filter(Boolean)

    expect(routeNames).not.toContain('ai')
  })
})
