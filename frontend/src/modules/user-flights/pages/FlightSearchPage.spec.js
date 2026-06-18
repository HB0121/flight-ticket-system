// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import FlightSearchPage from './FlightSearchPage.vue'

const mocks = vi.hoisted(() => ({
  fetchFlights: vi.fn(),
  fetchFlight: vi.fn(),
  fetchPriceHistory: vi.fn()
}))

vi.mock('../../../api/flightApi.js', () => ({
  fetchFlights: mocks.fetchFlights,
  fetchFlight: mocks.fetchFlight,
  fetchPriceHistory: mocks.fetchPriceHistory
}))

const ElFormStub = {
  template: '<form @submit.prevent="$emit(\'submit\', $event)"><slot /></form>',
  emits: ['submit']
}

const ElFormItemStub = {
  template: '<label><span>{{ label }}</span><slot /></label>',
  props: ['label']
}

const ElInputStub = {
  template: `
    <input
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue']
}

const ElDatePickerStub = {
  template: `
    <input
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue']
}

const ElButtonStub = {
  template: '<button :type="nativeType || \'button\'" :disabled="loading"><slot /></button>',
  props: ['loading', 'nativeType']
}

const FlightTableStub = {
  template: `
    <div>
      <button
        v-for="flight in flights"
        :key="flight.id"
        class="flight-row"
        type="button"
        @click="$emit('select', flight)"
      >
        {{ flight.flightNo }}
      </button>
      <span class="flight-count">{{ flights.length }}</span>
      <span class="selected-flight-id">{{ selectedFlightId ?? '' }}</span>
      <span class="loading-flag">{{ loading ? 'loading' : 'idle' }}</span>
    </div>
  `,
  props: ['flights', 'selectedFlightId', 'loading'],
  emits: ['select']
}

const FlightDetailCardStub = {
  template: '<div class="detail-card">{{ flight.flightNo }}|{{ flight.airlineName || "none" }}</div>',
  props: ['flight']
}

const PriceHistoryChartStub = {
  template: '<div class="history-chart">{{ history.length }}|{{ loading ? "loading" : "idle" }}</div>',
  props: ['history', 'loading']
}

function createDeferred() {
  let resolve
  let reject
  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
}

function createWrapper() {
  return mount(FlightSearchPage, {
    global: {
      stubs: {
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElDatePicker: ElDatePickerStub,
        ElButton: ElButtonStub,
        FlightTable: FlightTableStub,
        FlightDetailCard: FlightDetailCardStub,
        PriceHistoryChart: PriceHistoryChartStub
      }
    }
  })
}

describe('FlightSearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads flights on entry and lets the user switch selection', async () => {
    mocks.fetchFlights.mockResolvedValueOnce([
      { id: 1, flightNo: 'MU1001' },
      { id: 2, flightNo: 'CA2002' }
    ])
    mocks.fetchFlight
      .mockResolvedValueOnce({ id: 1, flightNo: 'MU1001', airlineName: 'China Eastern' })
      .mockResolvedValueOnce({ id: 2, flightNo: 'CA2002', airlineName: 'Air China' })
    mocks.fetchPriceHistory
      .mockResolvedValueOnce([{ observedAt: '2026-06-17T08:00:00', price: 960 }])
      .mockResolvedValueOnce([
        { observedAt: '2026-06-17T08:00:00', price: 1200 },
        { observedAt: '2026-06-17T12:00:00', price: 1180 }
      ])

    const wrapper = createWrapper()
    await flushPromises()

    expect(mocks.fetchFlights).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.detail-card').text()).toContain('MU1001|China Eastern')
    expect(wrapper.find('.history-chart').text()).toContain('1|idle')

    await wrapper.findAll('.flight-row')[1].trigger('click')
    await flushPromises()

    expect(wrapper.find('.detail-card').text()).toContain('CA2002|Air China')
    expect(wrapper.find('.history-chart').text()).toContain('2|idle')
  })

  it('keeps the latest selected flight when an older selection resolves later', async () => {
    const staleDetail = createDeferred()
    const staleHistory = createDeferred()

    mocks.fetchFlights.mockResolvedValueOnce([
      { id: 1, flightNo: 'MU1001' },
      { id: 2, flightNo: 'CA2002' }
    ])
    mocks.fetchFlight.mockImplementation(id => {
      if (id === 1 && mocks.fetchFlight.mock.calls.length === 1) {
        return Promise.resolve({ id: 1, flightNo: 'MU1001', airlineName: 'China Eastern' })
      }

      if (id === 1) {
        return Promise.resolve({ id: 1, flightNo: 'MU1001', airlineName: 'China Eastern Fresh' })
      }

      return staleDetail.promise
    })
    mocks.fetchPriceHistory.mockImplementation(id => {
      if (id === 1 && mocks.fetchPriceHistory.mock.calls.length === 1) {
        return Promise.resolve([{ observedAt: '2026-06-17T08:00:00', price: 960 }])
      }

      if (id === 1) {
        return Promise.resolve([{ observedAt: '2026-06-17T09:00:00', price: 940 }])
      }

      return staleHistory.promise
    })

    const wrapper = createWrapper()
    await flushPromises()

    const rows = wrapper.findAll('.flight-row')
    await rows[1].trigger('click')
    await rows[0].trigger('click')
    await flushPromises()

    expect(wrapper.find('.detail-card').text()).toContain('MU1001|China Eastern Fresh')

    staleDetail.resolve({ id: 2, flightNo: 'CA2002', airlineName: 'Air China Stale' })
    staleHistory.resolve([{ observedAt: '2026-06-17T10:00:00', price: 1180 }])
    await flushPromises()

    expect(wrapper.find('.detail-card').text()).toContain('MU1001|China Eastern Fresh')
    expect(wrapper.find('.detail-card').text()).not.toContain('CA2002|Air China Stale')
  })

  it('preserves detailed flight data when history fails to load', async () => {
    mocks.fetchFlights.mockResolvedValueOnce([
      { id: 1, flightNo: 'MU1001' }
    ])
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU1001',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockRejectedValueOnce(new Error('History service unavailable'))

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.find('.detail-card').text()).toContain('MU1001|China Eastern')
    expect(wrapper.text()).toContain('History service unavailable')
    expect(wrapper.find('.history-chart').text()).toContain('0|idle')
  })
})
