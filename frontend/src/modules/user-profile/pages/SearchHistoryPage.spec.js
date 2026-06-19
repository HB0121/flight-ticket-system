// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import SearchHistoryPage from './SearchHistoryPage.vue'

const mocks = vi.hoisted(() => ({
  fetchSearchHistory: vi.fn()
}))

vi.mock('../../../api/profileApi.js', () => ({
  fetchFavorites: vi.fn(),
  addFavorite: vi.fn(),
  removeFavorite: vi.fn(),
  fetchSearchHistory: mocks.fetchSearchHistory
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
        common: {
          labels: {
            travelDate: 'Travel Date',
            source: 'Source',
            searchedAt: 'Searched At'
          },
          status: {
            loadingHistory: 'Loading search history...'
          }
        },
        history: {
          eyebrow: 'User Profile',
          title: 'Search History',
          subtitle: 'Recent flight searches are recorded automatically from the current query flow.',
          empty: 'No search history yet.',
          errors: {
            loadFailed: 'Unable to load search history right now.'
          }
        }
      },
      'zh-CN': {
        common: {
          labels: {
            travelDate: '出行日期',
            source: '来源',
            searchedAt: '搜索时间'
          },
          status: {
            loadingHistory: '正在加载搜索历史...'
          }
        },
        history: {
          eyebrow: '用户资料',
          title: '搜索历史',
          subtitle: '最近的航班搜索会从当前查询流程中自动记录。',
          empty: '还没有搜索历史。',
          errors: {
            loadFailed: '当前无法加载搜索历史。'
          }
        }
      }
    }
  })
}

function createWrapper(locale = 'en-US') {
  return mount(SearchHistoryPage, {
    global: {
      plugins: [createTestI18n(locale)]
    }
  })
}

describe('SearchHistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads the current user search history on entry', async () => {
    mocks.fetchSearchHistory.mockResolvedValueOnce([
      { id: 1, fromCity: 'Shanghai', toCity: 'Beijing', travelDate: '2026-06-19', dataSource: 'sample', createdAt: '2026-06-17T09:00:00' }
    ])

    const wrapper = createWrapper()
    await flushPromises()

    expect(mocks.fetchSearchHistory).toHaveBeenCalledTimes(1)
    expect(wrapper.get('.history-card__route').text()).toBe('Shanghai->Beijing')
    expect(wrapper.text()).toContain('2026-06-19')
  })

  it('renders localized history copy', () => {
    const wrapper = createWrapper('zh-CN')

    expect(wrapper.text()).toContain('搜索历史')
  })
})
