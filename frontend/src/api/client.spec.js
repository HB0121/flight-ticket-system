import { describe, expect, it, vi } from 'vitest'
import router from '../router/index.js'

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

    await runCrawler({ source: 'sample' })

    expect(axiosMock.create).toHaveBeenCalledWith(expect.objectContaining({
      timeout: 15000
    }))
    expect(axiosMock.client.post).toHaveBeenCalledWith(
      '/api/crawl/run',
      { source: 'sample' },
      { timeout: 130000 }
    )
  })
})

describe('router bootstrap', () => {
  it('declares user, admin, and auth entry routes', () => {
    const routeNames = router.getRoutes().map(route => route.name)

    expect(routeNames).toContain('auth')
    expect(routeNames).toContain('user-flights')
    expect(routeNames).toContain('admin-crawl-jobs')
  })

  it('redirects root and admin entry paths to their phase-1 landing pages', async () => {
    await router.push('/')
    expect(router.currentRoute.value.name).toBe('user-flights')

    await router.push('/admin')
    expect(router.currentRoute.value.name).toBe('admin-crawl-jobs')
  })

  it('does not advertise admin protection before a real guard exists', () => {
    const adminEntry = router.resolve('/admin')

    expect(adminEntry.meta.requiresAdmin).toBeUndefined()
  })
})
