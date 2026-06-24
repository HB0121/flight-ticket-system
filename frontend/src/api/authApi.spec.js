import { beforeEach, describe, expect, it, vi } from 'vitest'

const axiosMock = vi.hoisted(() => {
  const client = {
    get: vi.fn(),
    post: vi.fn(),
    defaults: { headers: { common: {} } },
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

describe('shared auth HTTP client', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    globalThis.localStorage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn()
    }
    globalThis.window = {
      dispatchEvent: vi.fn()
    }
    globalThis.CustomEvent = class CustomEvent {
      constructor(type) {
        this.type = type
      }
    }
  })

  it('creates one axios client with auth interceptors and handles token lifecycle', async () => {
    const { http } = await import('./http.js')

    expect(http).toBe(axiosMock.client)
    expect(axiosMock.create).toHaveBeenCalledWith(expect.objectContaining({
      baseURL: 'http://localhost:8080',
      timeout: 15000
    }))
    expect(axiosMock.client.interceptors.request.use).toHaveBeenCalledTimes(1)
    expect(axiosMock.client.interceptors.response.use).toHaveBeenCalledTimes(1)

    const requestInterceptor = axiosMock.client.interceptors.request.use.mock.calls[0][0]
    globalThis.localStorage.getItem.mockReturnValueOnce('demo-token')

    const config = await requestInterceptor({ headers: {} })
    expect(config.headers.Authorization).toBe('Bearer demo-token')

    const responseErrorInterceptor = axiosMock.client.interceptors.response.use.mock.calls[0][1]
    const error = { response: { status: 401 } }

    await expect(responseErrorInterceptor(error)).rejects.toBe(error)
    expect(globalThis.localStorage.removeItem).toHaveBeenCalledWith('token')
    expect(globalThis.localStorage.removeItem).toHaveBeenCalledWith('user')
    expect(globalThis.window.dispatchEvent).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'auth:logout' })
    )
  })
})

describe('authApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('uses dedicated auth endpoints', async () => {
    const authApi = await import('./authApi.js')
    axiosMock.client.post.mockResolvedValueOnce({ data: { token: 't' } })
    axiosMock.client.post.mockResolvedValueOnce({ data: { id: 1 } })
    axiosMock.client.post.mockResolvedValueOnce({ data: null })
    axiosMock.client.get.mockResolvedValueOnce({ data: { username: 'demo' } })

    await authApi.login({ username: 'demo', password: 'secret' })
    await authApi.register({ username: 'demo', password: 'secret', nickname: 'Demo' })
    await authApi.logout()
    await authApi.getMe()

    expect(axiosMock.client.post).toHaveBeenNthCalledWith(1, '/api/auth/login', {
      username: 'demo',
      password: 'secret'
    })
    expect(axiosMock.client.post).toHaveBeenNthCalledWith(2, '/api/auth/register', {
      username: 'demo',
      password: 'secret',
      nickname: 'Demo'
    })
    expect(axiosMock.client.post).toHaveBeenNthCalledWith(3, '/api/auth/logout')
    expect(axiosMock.client.get).toHaveBeenCalledWith('/api/auth/me')
  })
})
