export const SUPPORTED_LOCALES = ['zh-CN', 'en-US']
export const DEFAULT_LOCALE = 'zh-CN'
export const LOCALE_STORAGE_KEY = 'locale'

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

export function getStoredLocale() {
  const normalized = normalizeLocale(globalThis.localStorage?.getItem?.(LOCALE_STORAGE_KEY))
  return SUPPORTED_LOCALES.includes(normalized) ? normalized : null
}

export function setStoredLocale(locale) {
  const normalized = normalizeLocale(locale)
  if (!SUPPORTED_LOCALES.includes(normalized)) {
    return
  }

  globalThis.localStorage?.setItem?.(LOCALE_STORAGE_KEY, normalized)
}

export function resolveInitialLocale() {
  return (
    getStoredLocale()
    || normalizeLocale(globalThis.navigator?.language)
    || DEFAULT_LOCALE
  )
}
