import { beforeEach, describe, expect, it, vi } from 'vitest'

const axiosMock = vi.hoisted(() => {
  const client = {
    post: vi.fn(),
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

describe('flightSyncApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('syncs flights through the existing admin flight sync endpoint', async () => {
    const { syncFlights } = await import('./flightSyncApi.js')
    axiosMock.client.post.mockResolvedValueOnce({ data: { status: 'SUCCESS' } })

    await syncFlights({ airportCode: 'CKG', date: '2026-06-22' })

    expect(axiosMock.client.post).toHaveBeenCalledWith('/api/admin/flights/sync', null, {
      params: {
        airportCode: 'CKG',
        date: '2026-06-22'
      }
    })
  })
})
