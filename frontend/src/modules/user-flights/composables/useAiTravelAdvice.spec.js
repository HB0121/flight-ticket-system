import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAiTravelAdvice } from './useAiTravelAdvice.js'

function createAdvice(localeValue = 'en-US') {
  const requestAdvice = vi.fn()
  const normalizeFlightForDisplay = vi.fn(flight => ({
    ...flight,
    normalized: true
  }))
  const getErrorMessage = vi.fn((error, fallback) => error?.message ?? fallback)
  const t = vi.fn(key => key)

  return {
    requestAdvice,
    normalizeFlightForDisplay,
    getErrorMessage,
    t,
    advice: useAiTravelAdvice({
      requestAdvice,
      normalizeFlightForDisplay,
      locale: ref(localeValue),
      getErrorMessage,
      t
    })
  }
}

describe('useAiTravelAdvice', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('keeps the existing empty query behavior in English', async () => {
    const { requestAdvice, advice } = createAdvice('en-US')

    await advice.submitAiAdvice()

    expect(requestAdvice).not.toHaveBeenCalled()
    expect(advice.aiResult.value).toBeNull()
    expect(advice.aiError.value).toBe('Please enter a travel request.')
    expect(advice.aiLoading.value).toBe(false)
  })

  it('keeps the existing empty query behavior in Chinese', async () => {
    const { advice } = createAdvice('zh-CN')

    await advice.submitAiAdvice()

    expect(advice.aiError.value).toBe('请输入出行需求。')
  })

  it('requests advice and stores the result on success', async () => {
    const { requestAdvice, advice } = createAdvice('en-US')
    const result = {
      summary: 'Take MU5101',
      candidates: [{ id: 1, flightNo: 'MU5101' }],
      recommendedFlight: { id: 2, flightNo: 'CA1201' }
    }
    requestAdvice.mockResolvedValueOnce(result)
    advice.aiForm.query = 'Find a morning flight'

    const submitPromise = advice.submitAiAdvice()

    expect(advice.aiLoading.value).toBe(true)
    expect(advice.aiError.value).toBe('')

    await submitPromise

    expect(requestAdvice).toHaveBeenCalledWith('Find a morning flight')
    expect(advice.aiResult.value).toEqual(result)
    expect(advice.aiLoading.value).toBe(false)
  })

  it('clears the result and reports the fallback error when request fails', async () => {
    const { getErrorMessage, requestAdvice, advice } = createAdvice('en-US')
    requestAdvice.mockRejectedValueOnce(new Error('service unavailable'))
    advice.aiResult.value = { summary: 'previous' }
    advice.aiForm.query = 'Find a flight'

    await advice.submitAiAdvice()

    expect(advice.aiResult.value).toBeNull()
    expect(getErrorMessage).toHaveBeenCalledWith(
      expect.any(Error),
      'Unable to generate travel advice right now.'
    )
    expect(advice.aiError.value).toBe('service unavailable')
    expect(advice.aiLoading.value).toBe(false)
  })

  it('normalizes AI candidates and recommended flight with the current locale', () => {
    const { normalizeFlightForDisplay, advice } = createAdvice('en-US')
    advice.aiResult.value = {
      candidates: [{ id: 1, flightNo: 'MU5101' }],
      recommendedFlight: { id: 2, flightNo: 'CA1201' }
    }

    expect(advice.aiCandidates.value).toEqual([{ id: 1, flightNo: 'MU5101', normalized: true }])
    expect(advice.aiRecommendedFlight.value).toEqual({ id: 2, flightNo: 'CA1201', normalized: true })
    expect(normalizeFlightForDisplay).toHaveBeenCalledWith({ id: 1, flightNo: 'MU5101' }, 'en-US')
    expect(normalizeFlightForDisplay).toHaveBeenCalledWith({ id: 2, flightNo: 'CA1201' }, 'en-US')
  })

  it('keeps AI auto-sync status text behavior unchanged', () => {
    const { advice } = createAdvice('en-US')

    advice.aiLoading.value = true
    expect(advice.aiSyncStatusText.value).toBe('If local data is missing, the backend may auto-sync flight data. Please wait...')

    advice.aiLoading.value = false
    advice.aiResult.value = null
    expect(advice.aiSyncStatusText.value).toBe('')

    advice.aiResult.value = {
      syncAttempted: true,
      syncStatus: 'SUCCESS',
      autoSynced: true,
      syncMessage: ''
    }
    expect(advice.aiSyncStatusText.value).toBe('Auto sync completed. Advice is based on local data.')

    advice.aiResult.value = {
      syncAttempted: true,
      syncStatus: 'FAILED',
      syncMessage: ''
    }
    expect(advice.aiSyncStatusText.value).toBe('Auto sync failed: Try again later or sync this date manually.')

    advice.aiResult.value = {
      syncAttempted: true,
      syncStatus: 'SKIPPED',
      syncMessage: 'Using existing local data.'
    }
    expect(advice.aiSyncStatusText.value).toBe('Using existing local data.')
  })

  it('formats AI candidate metadata with existing fallback values', () => {
    const { advice } = createAdvice('en-US')

    expect(advice.formatAiCandidateMeta({ price: 1200, seatsLeft: 3, dataSource: 'aerodatabox' })).toBe('¥1200 / 3 seats / aerodatabox')
    expect(advice.formatAiCandidateMeta({})).toBe('¥- / - seats / -')
  })
})
