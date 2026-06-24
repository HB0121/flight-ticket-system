// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import FavoriteButton from './FavoriteButton.vue'

const mocks = vi.hoisted(() => ({
  addFavorite: vi.fn(),
  removeFavorite: vi.fn()
}))

vi.mock('../../api/profileApi.js', () => ({
  addFavorite: mocks.addFavorite,
  removeFavorite: mocks.removeFavorite
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

const ElButtonStub = {
  template: '<button :disabled="loading"><slot /></button>',
  props: ['loading', 'link', 'ariaLabel']
}

const ElIconStub = {
  template: '<span><slot /></span>',
  props: ['size']
}

const StarStub = {
  template: '<i class="star-icon"></i>'
}

const StarFilledStub = {
  template: '<i class="star-filled-icon"></i>'
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
            favorite: 'Favorite',
            unfavorite: 'Unfavorite'
          }
        },
        flights: {
          favorite: {
            added: 'Favorited',
            removed: 'Removed from favorites',
            failed: 'Operation failed, please retry'
          }
        }
      },
      'zh-CN': {
        common: {
          actions: {
            favorite: '收藏',
            unfavorite: '取消收藏'
          }
        },
        flights: {
          favorite: {
            added: '已收藏',
            removed: '已取消收藏',
            failed: '操作失败，请重试'
          }
        }
      }
    }
  })
}

function createWrapper(props = {}) {
  return mount(FavoriteButton, {
    props: {
      flightId: 1,
      isFavorited: false,
      favoriteId: null,
      ...props
    },
    global: {
      plugins: [createTestI18n()],
      stubs: {
        ElButton: ElButtonStub,
        ElIcon: ElIconStub,
        Star: StarStub,
        StarFilled: StarFilledStub
      }
    }
  })
}

describe('FavoriteButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders favorite button when not favorited', () => {
    const wrapper = createWrapper({ isFavorited: false, favoriteId: null })
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.html()).not.toContain('star-filled')
  })

  it('renders favorited state correctly', () => {
    const wrapper = createWrapper({ isFavorited: true, favoriteId: 10 })
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.html()).toContain('star-filled')
  })

  it('clicking unfavorited star calls addFavorite and emits toggled(true, 99)', async () => {
    mocks.addFavorite.mockResolvedValue({ id: 99, flightId: 1 })
    const wrapper = createWrapper({ isFavorited: false, favoriteId: null })

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(mocks.addFavorite).toHaveBeenCalledWith({ flightId: 1 })
    expect(wrapper.emitted('toggled')).toBeTruthy()
    expect(wrapper.emitted('toggled')[0]).toEqual([true, 99])
  })

  it('clicking favorited star calls removeFavorite(10) and emits toggled(false, null)', async () => {
    mocks.removeFavorite.mockResolvedValue()
    const wrapper = createWrapper({ isFavorited: true, favoriteId: 10 })

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(mocks.removeFavorite).toHaveBeenCalledWith(10)
    expect(wrapper.emitted('toggled')).toBeTruthy()
    expect(wrapper.emitted('toggled')[0]).toEqual([false, null])
  })
})
