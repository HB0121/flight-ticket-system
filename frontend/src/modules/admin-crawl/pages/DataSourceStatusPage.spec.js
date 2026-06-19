// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
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

function createTestI18n(locale = 'en-US') {
  return createI18n({
    legacy: false,
    locale,
    messages: {
      'en-US': {
        admin: {
          dataSources: {
            eyebrow: 'Admin Data Sources',
            title: 'Data source status',
            subtitle: 'This view reports whether the real remote data source is actually configured. No fallback source is exposed.',
            actions: {
              refresh: 'Refresh',
              refreshing: 'Refreshing...'
            },
            badges: {
              configured: 'Configured',
              notConfigured: 'Not Configured'
            }
          }
        }
      },
      'zh-CN': {
        admin: {
          dataSources: {
            eyebrow: '管理数据源',
            title: '数据源状态',
            subtitle: '该页面显示真实远程数据源是否已正确配置，不再暴露任何兜底数据源。',
            actions: {
              refresh: '刷新',
              refreshing: '刷新中...'
            },
            badges: {
              configured: '已配置',
              notConfigured: '未配置'
            }
          }
        }
      }
    }
  })
}

describe('DataSourceStatusPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads and renders admin data-source statuses', async () => {
    mocks.listDataSourceStatuses.mockResolvedValueOnce([
      { code: 'amadeus', label: 'Amadeus', configured: false, mode: 'remote', detail: 'Amadeus credentials are missing' }
    ])

    const wrapper = mount(DataSourceStatusPage, {
      global: {
        plugins: [createTestI18n('en-US')]
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Amadeus')
    expect(wrapper.text()).toContain('Not Configured')
    expect(wrapper.get('.data-source-status-page__meta').text()).toBe('amadeus | remote')
  })

  it('renders localized data source copy', async () => {
    mocks.listDataSourceStatuses.mockResolvedValueOnce([])

    const wrapper = mount(DataSourceStatusPage, {
      global: {
        plugins: [createTestI18n('zh-CN')]
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('数据源状态')
    expect(wrapper.text()).toContain('不再暴露任何兜底数据源')
  })
})
