import { computed, reactive, ref } from 'vue'

export function useFlightSync({
  filters,
  resetPagination,
  submitSearch,
  getTodayDateString,
  t,
  syncFlights
} = {}) {
  const syncLoading = ref(false)
  const syncResult = ref(null)
  const syncMessage = ref('')
  const syncError = ref('')
  const syncDetailsOpen = ref(false)

  const syncForm = reactive({
    airportCode: 'CKG',
    date: getTodayDateString?.() ?? ''
  })

  const syncStatus = computed(() => String(syncResult.value?.status ?? '').toUpperCase())
  const statusToneClass = computed(() => {
    if (syncStatus.value === 'SUCCESS') return 'flight-search-page__status-pill--success'
    if (syncStatus.value === 'FAILED') return 'flight-search-page__status-pill--failed'
    if (syncStatus.value === 'RUNNING') return 'flight-search-page__status-pill--running'
    return 'flight-search-page__status-pill--neutral'
  })

  function getSyncErrorMessage(error, fallback = t?.('flights.sync.messages.failed')) {
    return error?.response?.data?.errorMessage
      ?? error?.response?.data?.message
      ?? error?.response?.data?.error
      ?? error?.message
      ?? fallback
  }

  function isFailedSyncResult(result) {
    return String(result?.status ?? '').toUpperCase() === 'FAILED'
  }

  function formatSyncValue(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value)
  }

  async function applySuccessfulSync(date) {
    syncError.value = ''
    syncMessage.value = t?.('flights.sync.messages.success') ?? ''
    syncDetailsOpen.value = false
    if (filters) {
      filters.dataSource = 'aerodatabox'
      if (date) filters.date = date
    }
    resetPagination?.()
    await submitSearch?.()
  }

  async function submitSync() {
    const airportCode = syncForm.airportCode?.trim()
    const date = syncForm.date || getTodayDateString?.()

    syncLoading.value = true
    syncMessage.value = ''
    syncError.value = ''

    try {
      const result = await syncFlights({ airportCode, date })
      syncResult.value = result ?? null
      if (isFailedSyncResult(result)) {
        syncError.value = getSyncErrorMessage({ response: { data: result } })
        return
      }
      await applySuccessfulSync(date)
    } catch (error) {
      syncResult.value = error?.response?.data ?? null
      syncError.value = getSyncErrorMessage(error)
    } finally {
      syncLoading.value = false
    }
  }

  async function syncToday() {
    syncForm.date = getTodayDateString?.() ?? ''
    await submitSync()
  }

  return {
    syncForm,
    syncLoading,
    syncResult,
    syncMessage,
    syncError,
    syncDetailsOpen,
    syncStatus,
    statusToneClass,
    submitSync,
    syncToday,
    formatSyncValue,
    getSyncErrorMessage,
    isFailedSyncResult
  }
}
