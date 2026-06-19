import { beforeEach, describe, expect, it, vi } from 'vitest'

describe('locale resolution', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.unstubAllGlobals()
    globalThis.localStorage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn()
    }
  })

  it('prefers persisted locale over browser language', async () => {
    globalThis.localStorage.getItem.mockReturnValueOnce('en-US')
    vi.stubGlobal('navigator', { language: 'zh-CN' })

    const { resolveInitialLocale } = await import('./locale.js')

    expect(resolveInitialLocale()).toBe('en-US')
  })

  it('maps browser english variants to en-US', async () => {
    globalThis.localStorage.getItem.mockReturnValueOnce(null)
    vi.stubGlobal('navigator', { language: 'en-GB' })

    const { resolveInitialLocale } = await import('./locale.js')

    expect(resolveInitialLocale()).toBe('en-US')
  })

  it('falls back to zh-CN for unsupported browser locales', async () => {
    globalThis.localStorage.getItem.mockReturnValueOnce(null)
    vi.stubGlobal('navigator', { language: 'fr-FR' })

    const { resolveInitialLocale } = await import('./locale.js')

    expect(resolveInitialLocale()).toBe('zh-CN')
  })

  it('persists supported locales only', async () => {
    const { setStoredLocale, LOCALE_STORAGE_KEY } = await import('./locale.js')

    setStoredLocale('en-US')

    expect(globalThis.localStorage.setItem).toHaveBeenCalledWith(LOCALE_STORAGE_KEY, 'en-US')
  })
})
