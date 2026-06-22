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

describe('aiTravelApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('requests travel advice through the existing AI advice endpoint', async () => {
    const { requestAdvice } = await import('./aiTravelApi.js')
    axiosMock.client.post.mockResolvedValueOnce({ data: { summary: 'ok' } })

    await requestAdvice('help me pick a flight')

    expect(axiosMock.client.post).toHaveBeenCalledWith('/api/ai/advice', {
      message: 'help me pick a flight',
      query: 'help me pick a flight'
    })
  })
})
