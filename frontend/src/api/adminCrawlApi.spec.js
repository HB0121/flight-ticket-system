import { beforeEach, describe, expect, it, vi } from 'vitest'

const axiosMock = vi.hoisted(() => {
  const client = {
    get: vi.fn(),
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

describe('adminCrawlApi', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
  })

  it('uses admin crawl job and data-source endpoints', async () => {
    const adminCrawlApi = await import('./adminCrawlApi.js')
    axiosMock.client.post.mockResolvedValueOnce({ data: { id: 5 } })
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ id: 5 }] })
    axiosMock.client.get.mockResolvedValueOnce({ data: [{ code: 'amadeus' }] })

    await adminCrawlApi.createCrawlJob({ source: 'amadeus' })
    await adminCrawlApi.listCrawlJobs()
    await adminCrawlApi.listDataSourceStatuses()

    expect(axiosMock.client.post).toHaveBeenCalledWith(
      '/api/admin/crawl-jobs',
      { source: 'amadeus' },
      { timeout: 130000 }
    )
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(1, '/api/admin/crawl-jobs')
    expect(axiosMock.client.get).toHaveBeenNthCalledWith(2, '/api/admin/data-sources/status')
  })
})
