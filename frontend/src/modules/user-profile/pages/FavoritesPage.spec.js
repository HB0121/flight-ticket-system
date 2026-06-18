// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
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

function createWrapper() {
  return mount(FavoritesPage, {
    global: {
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
})
