// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import FavoritesPage from './FavoritesPage.vue'

const mocks = vi.hoisted(() => ({
  fetchFavorites: vi.fn(),
  removeFavorite: vi.fn()
}))

vi.mock('../../../api/profileApi.js', () => ({
  fetchFavorites: mocks.fetchFavorites,
  removeFavorite: mocks.removeFavorite,
  fetchSearchHistory: vi.fn(),
  addFavorite: vi.fn()
}))

const ElButtonStub = {
  template: '<button :disabled="loading" @click="$emit(\'click\')"><slot /></button>',
  props: ['loading'],
  emits: ['click']
}

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
          actions: {
            refresh: 'Refresh',
            remove: 'Remove'
          },
          labels: {
            departure: 'Departure',
            airports: 'Airports',
            source: 'Source'
          },
          status: {
            loadingFavorites: 'Loading favorites...'
          }
        },
        favorites: {
          eyebrow: 'User Profile',
          title: 'Favorites',
          subtitle: 'Review the flights you have already pinned from search results.',
          empty: 'No saved flights yet.',
          errors: {
            loadFailed: 'Unable to load favorites right now.',
            removeFailed: 'Unable to remove this favorite right now.'
          }
        }
      },
      'zh-CN': {
        common: {
          actions: {
            refresh: '刷新',
            remove: '移除'
          },
          labels: {
            departure: '出发',
            airports: '机场',
            source: '来源'
          },
          status: {
            loadingFavorites: '正在加载收藏...'
          }
        },
        favorites: {
          eyebrow: '用户资料',
          title: '收藏',
          subtitle: '查看你已经从搜索结果中收藏的航班。',
          empty: '还没有收藏航班。',
          errors: {
            loadFailed: '当前无法加载收藏列表。',
            removeFailed: '当前无法移除这条收藏。'
          }
        }
      }
    }
  })
}

function createWrapper(locale = 'en-US') {
  return mount(FavoritesPage, {
    global: {
      plugins: [createTestI18n(locale)],
      stubs: {
        ElButton: ElButtonStub
      }
    }
  })
}

describe('FavoritesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads favorites and removes one entry when requested', async () => {
    mocks.fetchFavorites
      .mockResolvedValueOnce([
        { id: 1, flightId: 7, flightNo: 'MU1001', fromCity: 'Shanghai', toCity: 'Beijing', fromAirport: 'PVG', toAirport: 'PEK', departTime: '2026-06-19T08:00:00', price: 880, dataSource: 'sample' }
      ])
      .mockResolvedValueOnce([])
    mocks.removeFavorite.mockResolvedValueOnce()

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('MU1001')
    expect(wrapper.get('.favorites-card__route').text()).toBe('Shanghai -> Beijing')
    expect(wrapper.get('.favorites-card__price').text()).toBe('CNY 880')
    expect(wrapper.text()).toContain('PVG -> PEK')
    await wrapper.findAll('button')[1].trigger('click')
    await flushPromises()

    expect(mocks.removeFavorite).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('No saved flights yet.')
  })

  it('renders localized favorites copy', () => {
    const wrapper = createWrapper('zh-CN')

    expect(wrapper.text()).toContain('收藏')
  })
})
