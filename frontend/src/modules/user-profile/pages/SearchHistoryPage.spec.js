// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
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

describe('SearchHistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads the current user search history on entry', async () => {
    mocks.fetchSearchHistory.mockResolvedValueOnce([
      { id: 1, fromCity: 'Shanghai', toCity: 'Beijing', travelDate: '2026-06-19', dataSource: 'sample', createdAt: '2026-06-17T09:00:00' }
    ])

    const wrapper = mount(SearchHistoryPage)
    await flushPromises()

    expect(mocks.fetchSearchHistory).toHaveBeenCalledTimes(1)
    expect(wrapper.get('.history-card__route').text()).toBe('Shanghai->Beijing')
    expect(wrapper.text()).toContain('2026-06-19')
  })
})
