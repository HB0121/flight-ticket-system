import { beforeEach, describe, expect, it, vi } from 'vitest'
import router from '../router/index.js'
import { resetSessionState } from '../auth/session.js'

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

describe('api client timeouts', () => {
  it('keeps normal requests short and gives crawler runs enough time', async () => {
    const { runCrawler } = await import('./client.js')
    axiosMock.client.post.mockResolvedValueOnce({ data: { status: 'SUCCESS' } })

    await runCrawler({ source: 'amadeus' })

    expect(axiosMock.create).toHaveBeenCalledWith(expect.objectContaining({
      timeout: 15000
    }))
    expect(axiosMock.client.post).toHaveBeenCalledWith(
      '/api/crawl/run',
      { source: 'amadeus' },
      { timeout: 130000 }
    )
  })
})

describe('router bootstrap', () => {
  function setToken(token) {
    globalThis.localStorage = {
      getItem: vi.fn(key => {
        if (key === 'token') {
          return token
        }

        return null
      }),
      setItem: vi.fn(),
      removeItem: vi.fn()
    }
  }

  function setAuthCheckResult(ok) {
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok,
      status: ok ? 200 : 401
    })
  }

  beforeEach(() => {
    resetSessionState()
    setToken(null)
    setAuthCheckResult(true)
  })

  it('declares user, admin, and auth entry routes', () => {
    const routeNames = router.getRoutes().map(route => route.name)

    expect(routeNames).toContain('auth')
    expect(routeNames).toContain('user-flights')
    expect(routeNames).toContain('admin-crawl-jobs')
  })

  it('redirects authenticated users from root and admin entry paths to their phase-1 landing pages', async () => {
    setToken('valid-token')
    setAuthCheckResult(true)

    await router.push('/')
    expect(router.currentRoute.value.name).toBe('user-flights')

    await router.push('/admin')
    expect(router.currentRoute.value.name).toBe('admin-crawl-jobs')
  })

  it('marks admin entry as authenticated but not role-restricted', () => {
    const adminEntry = router.resolve('/admin')

    expect(adminEntry.meta.requiresAuth).toBe(true)
    expect(adminEntry.meta.requiresAdmin).toBeUndefined()
  })

  it('redirects anonymous users to auth before showing protected pages', async () => {
    setToken(null)

    await router.push('/flights')
    expect(router.currentRoute.value.name).toBe('auth')
    expect(router.currentRoute.value.query.redirect).toBe('/flights')
  })

  it('rejects stale tokens and redirects to auth before showing protected pages', async () => {
    setToken('stale-token')
    setAuthCheckResult(false)

    await router.push('/flights')

    expect(router.currentRoute.value.name).toBe('auth')
    expect(globalThis.localStorage.removeItem).toHaveBeenCalledWith('token')
    expect(globalThis.localStorage.removeItem).toHaveBeenCalledWith('user')
  })
})
