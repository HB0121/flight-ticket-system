// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import CrawlJobsPage from './CrawlJobsPage.vue'

const mocks = vi.hoisted(() => ({
  createCrawlJob: vi.fn(),
  listCrawlJobs: vi.fn()
}))

vi.mock('../../../api/adminCrawlApi.js', () => ({
  createCrawlJob: mocks.createCrawlJob,
  listCrawlJobs: mocks.listCrawlJobs,
  listDataSourceStatuses: vi.fn()
}))

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
}

function createWrapper() {
  return mount(CrawlJobsPage)
}

describe('CrawlJobsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads recent jobs and submits a new admin crawl job', async () => {
    mocks.listCrawlJobs
      .mockResolvedValueOnce([
        { id: 3, source: 'sample', status: 'SUCCESS', successCount: 6, failedCount: 0, startedAt: '2026-06-18T09:00:00' }
      ])
      .mockResolvedValueOnce([
        { id: 4, source: 'amadeus', status: 'RUNNING', successCount: 0, failedCount: 0, startedAt: '2026-06-18T10:00:00' }
      ])
    mocks.createCrawlJob.mockResolvedValueOnce({ id: 4, source: 'amadeus', status: 'RUNNING' })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('sample')
    expect(wrapper.text()).toContain('SUCCESS')
    expect(wrapper.findAll('option').map(option => option.element.value)).toEqual(['sample', 'amadeus'])

    await wrapper.get('select').setValue('amadeus')
    const inputs = wrapper.findAll('input')
    await inputs[0].setValue('Shanghai')
    await inputs[1].setValue('Beijing')
    await inputs[2].setValue('2026-06-21')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.createCrawlJob).toHaveBeenCalledWith({
      source: 'amadeus',
      fromCity: 'Shanghai',
      toCity: 'Beijing',
      date: '2026-06-21',
      adults: 1,
      maxResults: 5
    })
    expect(wrapper.text()).toContain('RUNNING')
  })
})
