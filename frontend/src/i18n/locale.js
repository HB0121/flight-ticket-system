export const SUPPORTED_LOCALES = ['zh-CN', 'en-US']
export const DEFAULT_LOCALE = 'zh-CN'
export const LOCALE_STORAGE_KEY = 'locale'

// 把浏览器语言或用户输入统一归一化成项目支持的 locale key。
export function normalizeLocale(input) {
  const value = String(input || '').trim().toLowerCase()

  if (!value) {
    return null
  }

  if (value.startsWith('zh')) {
    return 'zh-CN'
  }

  if (value.startsWith('en')) {
    return 'en-US'
  }

  return null
}

// 优先使用用户手动选择过的语言。
export function getStoredLocale() {
  const normalized = normalizeLocale(globalThis.localStorage?.getItem?.(LOCALE_STORAGE_KEY))
  return SUPPORTED_LOCALES.includes(normalized) ? normalized : null
}

// 只保存项目支持的语言，避免写入无效值。
export function setStoredLocale(locale) {
  const normalized = normalizeLocale(locale)
  if (!SUPPORTED_LOCALES.includes(normalized)) {
    return
  }

  globalThis.localStorage?.setItem?.(LOCALE_STORAGE_KEY, normalized)
}

// 首次进入页面时：本地设置优先，其次浏览器语言，最后回退中文。
export function resolveInitialLocale() {
  return (
    getStoredLocale()
    || normalizeLocale(globalThis.navigator?.language)
    || DEFAULT_LOCALE
  )
}
