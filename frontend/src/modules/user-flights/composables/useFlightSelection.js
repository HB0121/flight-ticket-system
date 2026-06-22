import { ref } from 'vue'
import { fetchFlight, fetchPriceHistory } from '../../../api/flightApi.js'
import { normalizeFlightForDisplay } from '../../../shared/utils/flightDisplay.js'

export function useFlightSelection({
  locale,
  getErrorMessage,
  getPartialHistoryMessage,
  setErrorMessage
} = {}) {
  let activeSelectionRequestId = 0

  const historyLoading = ref(false)
  const selectedFlight = ref(null)
  const selectedFlightId = ref(null)
  const priceHistory = ref([])

  function clearSelectedFlight() {
    selectedFlight.value = null
    selectedFlightId.value = null
    priceHistory.value = []
  }

  function reportError(error, fallback) {
    if (!setErrorMessage) return
    const message = getErrorMessage
      ? getErrorMessage(error, fallback)
      : (error?.message ?? fallback)
    setErrorMessage(message)
  }

  async function selectFlight(flight) {
    if (!flight?.id) return

    const requestId = ++activeSelectionRequestId
    selectedFlightId.value = flight.id
    selectedFlight.value = flight
    priceHistory.value = []
    historyLoading.value = true
    setErrorMessage?.('')

    const [detailResult, historyResult] = await Promise.allSettled([
      fetchFlight(flight.id),
      fetchPriceHistory(flight.id)
    ])

    if (requestId !== activeSelectionRequestId) return

    if (detailResult.status === 'fulfilled' && detailResult.value) {
      selectedFlight.value = normalizeFlightForDisplay(detailResult.value, locale?.value)
    }
    if (historyResult.status === 'fulfilled') {
      priceHistory.value = Array.isArray(historyResult.value) ? historyResult.value : []
    } else {
      priceHistory.value = []
    }

    if (detailResult.status === 'rejected') {
      reportError(detailResult.reason)
    } else if (historyResult.status === 'rejected') {
      reportError(historyResult.reason, getPartialHistoryMessage?.())
    }

    if (requestId === activeSelectionRequestId) historyLoading.value = false
  }

  return {
    historyLoading,
    selectedFlight,
    selectedFlightId,
    priceHistory,
    selectFlight,
    clearSelectedFlight
  }
}
