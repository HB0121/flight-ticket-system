import { createI18n } from 'vue-i18n'
import { resolveInitialLocale } from './locale.js'
import enUS from './messages/en-US.js'
import zhCN from './messages/zh-CN.js'

export const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})
