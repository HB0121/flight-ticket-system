// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import CrawlJobsPage from './CrawlJobsPage.vue'

const mocks = vi.hoisted(() => ({
  createCrawlJob: vi.fn(),
  listCrawlJobs: vi.fn(),
  listDataSourceStatuses: vi.fn()
}))

vi.mock('../../../api/adminCrawlApi.js', () => ({
  createCrawlJob: mocks.createCrawlJob,
  listCrawlJobs: mocks.listCrawlJobs,
  listDataSourceStatuses: mocks.listDataSourceStatuses
}))

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
}

function createWrapper() {
  return mount(CrawlJobsPage, {
    global: {
      plugins: [createTestI18n('en-US')]
    }
  })
}

function createTestI18n(locale = 'en-US') {
  return createI18n({
    legacy: false,
    locale,
    messages: {
      'en-US': {
        admin: {
          crawlJobs: {
            eyebrow: 'Admin Crawl Jobs',
            title: 'Run crawl jobs through the admin boundary',
            subtitle: 'This page keeps phase-1 crawl administration limited to job creation and recent job inspection.',
            form: {
              source: 'Source',
              from: 'From',
              to: 'To',
              date: 'Date',
              adults: 'Adults',
              maxResults: 'Max Results'
            },
            placeholders: {
              from: 'Shanghai',
              to: 'Beijing'
            },
            actions: {
              submit: 'Create Crawl Job',
              submitting: 'Submitting...',
              refresh: 'Refresh',
              refreshing: 'Refreshing...'
            },
            recentJobs: {
              title: 'Recent Jobs',
              empty: 'No crawl jobs yet.',
              columns: {
                id: 'ID',
                source: 'Source',
                status: 'Status',
                success: 'Success',
                failed: 'Failed',
                started: 'Started'
              }
            }
          }
        }
      },
      'zh-CN': {
        admin: {
          crawlJobs: {
            eyebrow: '管理采集任务',
            title: '通过管理边界运行采集任务',
            subtitle: '该页面将 phase-1 的采集管理限制在任务创建和最近任务查看。',
            form: {
              source: '来源',
              from: '出发地',
              to: '目的地',
              date: '日期',
              adults: '乘客数',
              maxResults: '最大结果数'
            },
            placeholders: {
              from: '上海',
              to: '北京'
            },
            actions: {
              submit: '创建采集任务',
              submitting: '提交中...',
              refresh: '刷新',
              refreshing: '刷新中...'
            },
            recentJobs: {
              title: '最近任务',
              empty: '暂时还没有采集任务。',
              columns: {
                id: '编号',
                source: '来源',
                status: '状态',
                success: '成功',
                failed: '失败',
                started: '开始时间'
              }
            }
          }
        }
      }
    }
  })
}

describe('CrawlJobsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads recent jobs and submits a new admin crawl job', async () => {
    mocks.listDataSourceStatuses.mockResolvedValueOnce([
      { code: 'amadeus', label: 'Amadeus', configured: true, mode: 'remote', detail: 'Ready for remote collection' }
    ])
    mocks.listCrawlJobs
      .mockResolvedValueOnce([
        { id: 3, source: 'amadeus', status: 'SUCCESS', successCount: 6, failedCount: 0, startedAt: '2026-06-18T09:00:00' }
      ])
      .mockResolvedValueOnce([
        { id: 4, source: 'amadeus', status: 'RUNNING', successCount: 0, failedCount: 0, startedAt: '2026-06-18T10:00:00' }
      ])
    mocks.createCrawlJob.mockResolvedValueOnce({ id: 4, source: 'amadeus', status: 'RUNNING' })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('amadeus')
    expect(wrapper.text()).toContain('SUCCESS')
    expect(wrapper.findAll('option').map(option => option.element.value)).toEqual(['amadeus'])

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

  it('disables job creation when no real data source is configured', async () => {
    mocks.listDataSourceStatuses.mockResolvedValueOnce([
      { code: 'amadeus', label: 'Amadeus', configured: false, mode: 'remote', detail: 'Amadeus credentials are missing' }
    ])
    mocks.listCrawlJobs.mockResolvedValueOnce([])

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('Amadeus credentials are missing')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('renders localized crawl job copy', async () => {
    mocks.listDataSourceStatuses.mockResolvedValueOnce([])
    mocks.listCrawlJobs.mockResolvedValueOnce([])

    const wrapper = mount(CrawlJobsPage, {
      global: {
        plugins: [createTestI18n('zh-CN')]
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('通过管理边界运行采集任务')
    expect(wrapper.text()).toContain('创建采集任务')
  })
})
