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

describe('conversationApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('uses the existing conversation endpoints', async () => {
    const conversationApi = await import('./conversationApi.js')
    axiosMock.client.post.mockResolvedValueOnce({ data: { id: 's1' } })
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ id: 's1' }] })
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ id: 1, role: 'user' }] })
    axiosMock.client.post.mockResolvedValueOnce({ data: { summary: 'ok' } })
    axiosMock.client.delete.mockResolvedValueOnce({})

    await conversationApi.createConversation('Trip plan')
    await conversationApi.listConversations()
    await conversationApi.getMessages('s1')
    await conversationApi.sendMessage('s1', 'hello')
    await conversationApi.deleteConversation('s1')

    expect(axiosMock.client.post).toHaveBeenNthCalledWith(1, '/api/ai/conversations', { title: 'Trip plan' })
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(1, '/api/ai/conversations')
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(2, '/api/ai/conversations/s1/messages')
    expect(axiosMock.client.post).toHaveBeenNthCalledWith(2, '/api/ai/conversations/s1/messages', { message: 'hello' })
    expect(axiosMock.client.delete).toHaveBeenCalledWith('/api/ai/conversations/s1')
  })
})
