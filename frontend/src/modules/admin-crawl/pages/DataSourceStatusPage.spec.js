// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import DataSourceStatusPage from './DataSourceStatusPage.vue'

const mocks = vi.hoisted(() => ({
  listDataSourceStatuses: vi.fn()
}))

vi.mock('../../../api/adminCrawlApi.js', () => ({
  listDataSourceStatuses: mocks.listDataSourceStatuses,
  listCrawlJobs: vi.fn(),
  createCrawlJob: vi.fn()
}))

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
}

describe('DataSourceStatusPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads and renders admin data-source statuses', async () => {
    mocks.listDataSourceStatuses.mockResolvedValueOnce([
      { code: 'sample', label: 'Sample', configured: true, mode: 'fallback', detail: 'Built-in fallback spider' },
      { code: 'amadeus', label: 'Amadeus', configured: false, mode: 'remote', detail: 'Crawler command not configured for this source' }
    ])

    const wrapper = mount(DataSourceStatusPage)
    await flushPromises()

    expect(wrapper.text()).toContain('sample')
    expect(wrapper.text()).toContain('fallback')
    expect(wrapper.text()).toContain('Amadeus')
    expect(wrapper.text()).toContain('Not Configured')
    expect(wrapper.get('.data-source-status-page__meta').text()).toBe('sample | fallback')
  })
})
