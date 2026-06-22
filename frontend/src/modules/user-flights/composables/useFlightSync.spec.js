import { reactive } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useFlightSync } from './useFlightSync.js'

function createSync(options = {}) {
  const filters = reactive({
    date: '',
    dataSource: ''
  })
  const resetPagination = vi.fn()
  const submitSearch = vi.fn().mockResolvedValue(undefined)
  const syncFlights = vi.fn()
  const t = vi.fn(key => ({
    'flights.sync.messages.success': 'Flight sync completed.',
    'flights.sync.messages.failed': 'Flight sync failed.'
  })[key] ?? key)
  const getTodayDateString = vi.fn(() => options.today ?? '2026-06-22')

  return {
    filters,
    resetPagination,
    submitSearch,
    syncFlights,
    t,
    getTodayDateString,
    sync: useFlightSync({
      filters,
      resetPagination,
      submitSearch,
      getTodayDateString,
      t,
      syncFlights
    })
  }
}

describe('useFlightSync', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('uses the existing default form values', () => {
    const { sync } = createSync()

    expect(sync.syncForm.airportCode).toBe('CKG')
    expect(sync.syncForm.date).toBe('2026-06-22')
  })

  it('syncs selected date and refreshes search results on success', async () => {
    const { filters, resetPagination, submitSearch, syncFlights, sync } = createSync()
    const result = {
      status: 'SUCCESS',
      successCount: 12,
      source: 'aerodatabox'
    }
    syncFlights.mockResolvedValueOnce(result)
    sync.syncForm.airportCode = ' CKG '
    sync.syncForm.date = '2026-06-23'
    sync.syncDetailsOpen.value = true

    await sync.submitSync()

    expect(syncFlights).toHaveBeenCalledWith({ airportCode: 'CKG', date: '2026-06-23' })
    expect(sync.syncResult.value).toEqual(result)
    expect(sync.syncMessage.value).toBe('Flight sync completed.')
    expect(sync.syncError.value).toBe('')
    expect(sync.syncDetailsOpen.value).toBe(false)
    expect(filters.dataSource).toBe('aerodatabox')
    expect(filters.date).toBe('2026-06-23')
    expect(resetPagination).toHaveBeenCalledTimes(1)
    expect(submitSearch).toHaveBeenCalledTimes(1)
    expect(sync.syncLoading.value).toBe(false)
  })

  it('shows backend business failure without refreshing search results', async () => {
    const { resetPagination, submitSearch, syncFlights, sync } = createSync()
    const result = {
      status: 'FAILED',
      errorMessage: 'Crawler failed'
    }
    syncFlights.mockResolvedValueOnce(result)

    await sync.submitSync()

    expect(sync.syncResult.value).toEqual(result)
    expect(sync.syncError.value).toBe('Crawler failed')
    expect(sync.syncMessage.value).toBe('')
    expect(resetPagination).not.toHaveBeenCalled()
    expect(submitSearch).not.toHaveBeenCalled()
    expect(sync.syncLoading.value).toBe(false)
  })

  it('shows request errors without refreshing search results', async () => {
    const { resetPagination, submitSearch, syncFlights, sync } = createSync()
    syncFlights.mockRejectedValueOnce({
      response: {
        data: {
          message: 'Network failed'
        }
      }
    })

    await sync.submitSync()

    expect(sync.syncResult.value).toEqual({ message: 'Network failed' })
    expect(sync.syncError.value).toBe('Network failed')
    expect(resetPagination).not.toHaveBeenCalled()
    expect(submitSearch).not.toHaveBeenCalled()
    expect(sync.syncLoading.value).toBe(false)
  })

  it('syncs today by setting the date from getTodayDateString first', async () => {
    const { syncFlights, sync } = createSync({ today: '2026-06-24' })
    syncFlights.mockResolvedValueOnce({ status: 'SUCCESS' })
    sync.syncForm.date = '2026-06-20'

    await sync.syncToday()

    expect(sync.syncForm.date).toBe('2026-06-24')
    expect(syncFlights).toHaveBeenCalledWith({ airportCode: 'CKG', date: '2026-06-24' })
  })

  it('keeps sync display helpers behavior unchanged', () => {
    const { sync } = createSync()

    expect(sync.formatSyncValue(null)).toBe('-')
    expect(sync.formatSyncValue(undefined)).toBe('-')
    expect(sync.formatSyncValue('')).toBe('-')
    expect(sync.formatSyncValue(12)).toBe('12')

    sync.syncResult.value = { status: 'SUCCESS' }
    expect(sync.syncStatus.value).toBe('SUCCESS')
    expect(sync.statusToneClass.value).toBe('flight-search-page__status-pill--success')

    sync.syncResult.value = { status: 'FAILED' }
    expect(sync.statusToneClass.value).toBe('flight-search-page__status-pill--failed')

    sync.syncResult.value = { status: 'RUNNING' }
    expect(sync.statusToneClass.value).toBe('flight-search-page__status-pill--running')

    sync.syncResult.value = { status: '' }
    expect(sync.statusToneClass.value).toBe('flight-search-page__status-pill--neutral')
  })
})
