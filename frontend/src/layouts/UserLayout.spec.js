// @vitest-environment jsdom
import { it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import UserLayout from './UserLayout.vue'

vi.mock('../i18n/locale.js', () => ({
  setStoredLocale: vi.fn()
}))

it('renders a compact localized shell header without the old phase card copy', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'zh-CN',
    messages: {
      'zh-CN': {
        layout: {
          nav: { flights: '航班', favorites: '收藏', history: '历史' }
        },
        common: { locales: { zhCN: '中文', enUS: 'English' } },
        flights: {
          console: {
            title: '机票查询系统',
            subtitle: '基于 AeroDataBox 的航班数据查询与 AI 出行建议平台',
            badges: {
              dataSource: '数据来源：AeroDataBox',
              mode: '模式：本地 MySQL 查询'
            }
          }
        }
      }
    }
  })

  const wrapper = mount(UserLayout, {
    global: {
      plugins: [i18n],
      stubs: {
        RouterView: true,
        RouterLink: { template: '<a><slot /></a>' }
      }
    }
  })

  expect(wrapper.text()).toContain('机票查询系统')
  expect(wrapper.text()).toContain('航班')
  expect(wrapper.text()).toContain('收藏')
  expect(wrapper.text()).toContain('历史')
  expect(wrapper.text()).toContain('中文')
  expect(wrapper.text()).toContain('数据来源：AeroDataBox')
  expect(wrapper.text()).toContain('模式：本地 MySQL 查询')
  expect(wrapper.text()).not.toContain('阶段 1')
  expect(wrapper.text()).not.toContain('用户航班')
})
