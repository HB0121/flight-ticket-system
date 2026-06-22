import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useFlightSelection } from './useFlightSelection.js'

const mocks = vi.hoisted(() => ({
  fetchFlight: vi.fn(),
  fetchPriceHistory: vi.fn()
}))

vi.mock('../../../api/flightApi.js', () => ({
  fetchFlight: mocks.fetchFlight,
  fetchPriceHistory: mocks.fetchPriceHistory
}))

function createDeferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

function createSelection() {
  const errorMessage = ref('previous error')

  return {
    errorMessage,
    selection: useFlightSelection({
      locale: ref('en-US'),
      getErrorMessage: (error, fallback = 'Load failed') => error?.message ?? fallback,
      getPartialHistoryMessage: () => 'Partial history failed',
      setErrorMessage: message => {
        errorMessage.value = message
      }
    })
  }
}

describe('useFlightSelection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('selects a flight immediately, then loads detail and price history', async () => {
    const { errorMessage, selection } = createSelection()
    const row = { id: 7, flightNo: 'MU5101', airlineName: 'China Eastern' }
    const detail = { id: 7, flightNo: 'MU5101-D', airlineName: 'China Eastern' }
    const history = [{ id: 1, price: 900 }]
    mocks.fetchFlight.mockResolvedValueOnce(detail)
    mocks.fetchPriceHistory.mockResolvedValueOnce(history)

    const selectionPromise = selection.selectFlight(row)

    expect(selection.selectedFlightId.value).toBe(7)
    expect(selection.selectedFlight.value).toEqual(row)
    expect(selection.priceHistory.value).toEqual([])
    expect(selection.historyLoading.value).toBe(true)
    expect(errorMessage.value).toBe('')

    await selectionPromise

    expect(mocks.fetchFlight).toHaveBeenCalledWith(7)
    expect(mocks.fetchPriceHistory).toHaveBeenCalledWith(7)
    expect(selection.selectedFlight.value.flightNo).toBe('MU5101-D')
    expect(selection.priceHistory.value).toEqual(history)
    expect(selection.historyLoading.value).toBe(false)
  })

  it('keeps the selected row and reports an error when history loading fails', async () => {
    const { errorMessage, selection } = createSelection()
    const row = { id: 8, flightNo: 'CA1201', airlineName: 'Air China' }
    mocks.fetchFlight.mockResolvedValueOnce({ id: 8, flightNo: 'CA1201', airlineName: 'Air China' })
    mocks.fetchPriceHistory.mockRejectedValueOnce(new Error('history unavailable'))

    await selection.selectFlight(row)

    expect(selection.selectedFlight.value.flightNo).toBe('CA1201')
    expect(selection.priceHistory.value).toEqual([])
    expect(errorMessage.value).toBe('history unavailable')
    expect(selection.historyLoading.value).toBe(false)
  })

  it('keeps the later selection when an earlier request finishes last', async () => {
    const { selection } = createSelection()
    const firstDetail = createDeferred()
    const firstHistory = createDeferred()
    const secondDetail = createDeferred()
    const secondHistory = createDeferred()
    mocks.fetchFlight
      .mockReturnValueOnce(firstDetail.promise)
      .mockReturnValueOnce(secondDetail.promise)
    mocks.fetchPriceHistory
      .mockReturnValueOnce(firstHistory.promise)
      .mockReturnValueOnce(secondHistory.promise)

    const firstPromise = selection.selectFlight({ id: 1, flightNo: 'MU1001' })
    const secondPromise = selection.selectFlight({ id: 2, flightNo: 'MU1002' })

    secondDetail.resolve({ id: 2, flightNo: 'MU1002-D' })
    secondHistory.resolve([{ id: 'second-history' }])
    await secondPromise

    expect(selection.selectedFlightId.value).toBe(2)
    expect(selection.selectedFlight.value.flightNo).toBe('MU1002-D')
    expect(selection.priceHistory.value).toEqual([{ id: 'second-history' }])

    firstDetail.resolve({ id: 1, flightNo: 'MU1001-D' })
    firstHistory.resolve([{ id: 'first-history' }])
    await firstPromise

    expect(selection.selectedFlightId.value).toBe(2)
    expect(selection.selectedFlight.value.flightNo).toBe('MU1002-D')
    expect(selection.priceHistory.value).toEqual([{ id: 'second-history' }])
  })

  it('clears selected flight state without touching request APIs', () => {
    const { selection } = createSelection()
    selection.selectedFlight.value = { id: 3 }
    selection.selectedFlightId.value = 3
    selection.priceHistory.value = [{ id: 1 }]

    selection.clearSelectedFlight()

    expect(selection.selectedFlight.value).toBeNull()
    expect(selection.selectedFlightId.value).toBeNull()
    expect(selection.priceHistory.value).toEqual([])
    expect(mocks.fetchFlight).not.toHaveBeenCalled()
    expect(mocks.fetchPriceHistory).not.toHaveBeenCalled()
  })
})
