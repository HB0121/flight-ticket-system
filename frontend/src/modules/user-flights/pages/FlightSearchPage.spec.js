// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import FlightSearchPage from './FlightSearchPage.vue'

const mocks = vi.hoisted(() => ({
  fetchFlights: vi.fn(),
  fetchFlight: vi.fn(),
  fetchPriceHistory: vi.fn(),
  syncFlights: vi.fn(),
  requestAdvice: vi.fn()
}))

vi.mock('../../../api/flightApi.js', () => ({
  fetchFlights: mocks.fetchFlights,
  fetchFlight: mocks.fetchFlight,
  fetchPriceHistory: mocks.fetchPriceHistory,
  syncFlights: mocks.syncFlights,
  requestAdvice: mocks.requestAdvice
}))

const ElFormStub = {
  template: '<form v-bind="$attrs" @submit.prevent="$emit(\'submit\', $event)"><slot /></form>',
  emits: ['submit']
}

const ElFormItemStub = {
  template: '<label><span>{{ label }}</span><slot /></label>',
  props: ['label']
}

const ElInputStub = {
  template: `
    <input
      v-bind="$attrs"
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
      v-bind="$attrs"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue']
}

const ElSelectStub = {
  template: `
    <select
      v-bind="$attrs"
      :value="modelValue"
      @change="$emit('update:modelValue', $event.target.value)"
    >
      <slot />
    </select>
  `,
  props: ['modelValue'],
  emits: ['update:modelValue', 'change']
}

const ElOptionStub = {
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value']
}

const ElButtonStub = {
  template: '<button v-bind="$attrs" :type="nativeType || \'button\'" :disabled="loading"><slot /></button>',
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
        <span>{{ flight.flightNo }}</span>
        <span class="flight-route">{{ flight.routeLabel || '' }}</span>
        <span
          :data-testid="\`status-tag-\${flight.id}\`"
          :class="['flight-table__status', \`flight-table__status--\${flight.statusTone || 'neutral'}\`]"
        >
          {{ flight.statusLabel || '' }}
        </span>
      </button>
      <span class="flight-count">{{ flights.length }}</span>
      <span class="selected-flight-id">{{ selectedFlightId ?? '' }}</span>
      <span class="loading-flag">{{ loading ? 'loading' : 'idle' }}</span>
    </div>
  `,
  props: ['flights', 'selectedFlightId', 'loading'],
  emits: ['select']
}

const ElPaginationStub = {
  template: `
    <div v-bind="$attrs">
      <button type="button" data-testid="pagination-prev" @click="$emit('current-change', Math.max(currentPage - 1, 1))">
        Prev
      </button>
      <button type="button" data-testid="pagination-next" @click="$emit('current-change', currentPage + 1)">
        Next
      </button>
    </div>
  `,
  props: ['currentPage', 'pageSize', 'total'],
  emits: ['current-change']
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

function createTestI18n(locale = 'en-US') {
  return createI18n({
    legacy: false,
    locale,
    messages: {
      'en-US': {
        common: {
          actions: { searchFlights: 'Search Flights' },
          status: { loadingFlights: 'Loading flights...' }
        },
        flights: {
          console: {
            eyebrow: 'Phase 1',
            title: 'Flight Query System',
            subtitle: 'Flight sync, local MySQL query, and AI travel advice demo console.',
            badges: {
              dataSource: 'Data Source: AeroDataBox',
              mode: 'Mode: Local MySQL Query'
            }
          },
          sync: {
            eyebrow: 'Flight Sync',
            title: 'Sync flight data',
            description: 'Trigger backend sync, let the crawler write MySQL, then query local data only.',
            form: {
              airportCode: 'Airport Code',
              date: 'Sync Date'
            },
            placeholders: {
              airportCode: 'CKG',
              date: 'YYYY-MM-DD'
            },
            actions: {
              syncDate: 'Sync Selected Date',
              syncToday: 'Sync Today'
            },
            messages: {
              success: 'Flight sync completed.',
              failed: 'Flight sync failed.'
            }
          },
          syncResult: {
            eyebrow: 'Sync Result',
            title: 'Latest sync status',
            empty: {
              title: 'No sync result yet',
              description: 'Run a sync first to inspect backend job output.',
              summary: 'Waiting for the first backend sync run.'
            },
            summary: {
              success: 'Backend sync completed and local query results have been refreshed.',
              failed: 'Backend sync failed. Review the returned error message below.',
              empty: 'Sync completed but no flight rows were returned.'
            },
            fields: {
              successCount: 'Success Count',
              failedCount: 'Failed Count',
              source: 'Source',
              requestParams: 'Request Params',
              startedAt: 'Started At',
              finishedAt: 'Finished At',
              errorMessage: 'Error Message'
            }
          },
          search: {
            eyebrow: 'Flight Search',
            title: 'Query local flight snapshots',
            description: 'Search flights from the local MySQL data written by the backend crawler.'
          },
          results: {
            eyebrow: 'Flight Results',
            title: 'Flight query results',
            description: 'The table below is served from the local database through Spring Boot APIs.',
            emptyHint: 'No local rows yet. Run a sync first, then search again.',
            emptyTitle: 'No local flights found',
            emptyDescription: 'Try syncing the selected airport and date first, then query the local database again.'
          },
          eyebrow: 'User Flight Search',
          title: 'Search current flight snapshots',
          subtitle: 'Search and inspect flight snapshots.',
          filters: { from: 'From', to: 'To', date: 'Date', source: 'Source' },
          placeholders: {
            from: 'Departure city',
            to: 'Arrival city',
            date: 'Select a departure date',
            source: 'Optional source filter'
          },
          errors: {
            loadFailed: 'Unable to load flights. Please try again.',
            partialHistory: 'Flight detail loaded partially. Price history is unavailable right now.'
          }
        }
      },
      'zh-CN': {
        common: {
          actions: { searchFlights: '查询航班' },
          status: { loadingFlights: '正在加载航班...' }
        },
        flights: {
          console: {
            eyebrow: '阶段 1',
            title: '机票查询系统',
            subtitle: '基于 AeroDataBox 的航班数据查询与 AI 出行建议平台',
            badges: {
              dataSource: 'Data Source: AeroDataBox',
              mode: 'Mode: Local MySQL Query'
            }
          },
          sync: {
            eyebrow: '航班同步',
            title: '同步航班数据',
            description: '通过后端触发同步，由 crawler 写入 MySQL，再基于本地数据查询展示。',
            form: {
              airportCode: '机场代码',
              date: '同步日期'
            },
            placeholders: {
              airportCode: 'CKG',
              date: 'YYYY-MM-DD'
            },
            actions: {
              syncDate: '同步指定日期航班',
              syncToday: '同步今日航班'
            },
            messages: {
              success: '航班同步完成。',
              failed: '航班同步失败。'
            }
          },
          syncResult: {
            eyebrow: '同步结果',
            title: '最近一次同步状态',
            empty: {
              title: '暂未执行同步',
              description: '先执行一次同步，这里会展示后端返回的任务结果。',
              summary: '等待第一次后端同步执行。'
            },
            summary: {
              success: '后端同步已完成，并已刷新本地航班查询结果。',
              failed: '后端同步失败，请查看下方错误信息。',
              empty: '同步已完成，但当前未返回航班数据。'
            },
            fields: {
              successCount: '成功数量',
              failedCount: '失败数量',
              source: '数据来源',
              requestParams: '请求参数',
              startedAt: '开始时间',
              finishedAt: '结束时间',
              errorMessage: '错误信息'
            }
          },
          search: {
            eyebrow: '航班查询',
            title: '查询本地航班快照',
            description: '保留原有查询流程，只查询 Spring Boot 提供的本地 MySQL 航班数据。'
          },
          results: {
            eyebrow: '航班结果',
            title: '航班查询结果',
            description: '下方结果表展示的是本地数据库中的航班数据。',
            emptyHint: '当前还没有本地数据，建议先执行同步。',
            emptyTitle: '未查询到本地航班',
            emptyDescription: '请先同步指定机场和日期的数据，再重新查询本地航班列表。'
          },
          eyebrow: '用户航班查询',
          title: '查询当前航班快照',
          subtitle: '查询并查看航班快照。',
          filters: { from: '出发地', to: '目的地', date: '日期', source: '来源' },
          placeholders: {
            from: '出发城市',
            to: '到达城市',
            date: '选择出发日期',
            source: '可选来源筛选'
          },
          errors: {
            loadFailed: '加载航班失败，请稍后重试。',
            partialHistory: '航班详情已加载，但价格历史暂时不可用。'
          }
        }
      }
    }
  })
}

function createWrapper(locale = 'en-US') {
  return mount(FlightSearchPage, {
    global: {
      plugins: [createTestI18n(locale)],
      stubs: {
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElDatePicker: ElDatePickerStub,
        ElSelect: ElSelectStub,
        ElOption: ElOptionStub,
        ElPagination: ElPaginationStub,
        ElButton: ElButtonStub,
        FlightTable: FlightTableStub,
        FlightDetailCard: FlightDetailCardStub,
        PriceHistoryChart: PriceHistoryChartStub
      }
    }
  })
}

async function submitSearch(wrapper) {
  await wrapper.find('[data-testid="search-form"]').trigger('submit')
  await flushPromises()
}

async function submitSync(wrapper) {
  await wrapper.find('[data-testid="sync-form"]').trigger('submit')
  await flushPromises()
}

async function expandSyncDetails(wrapper) {
  await wrapper.find('[data-testid="sync-toggle-details"]').trigger('click')
  await flushPromises()
}

async function expandAiPanel(wrapper) {
  await wrapper.find('[data-testid="ai-toggle"]').trigger('click')
  await flushPromises()
}

describe('FlightSearchPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('submits airport dropdown values as IATA codes and resets pagination on a new search', async () => {
    mocks.fetchFlights.mockResolvedValueOnce(
      Array.from({ length: 12 }, (_, index) => ({
        id: index + 1,
        flightNo: `MU${index + 1000}`,
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T08:30:00',
        airlineName: index % 2 === 0 ? 'China Eastern' : 'Air China',
        status: index % 3 === 0 ? 'Scheduled' : 'Delayed',
        price: 900 + index
      }))
    )
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU1000',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockResolvedValueOnce([])

    const wrapper = createWrapper('zh-CN')
    await wrapper.find('[data-testid="sync-airport-code"]').setValue('PEK')
    await wrapper.find('[data-testid="filter-from-airport"]').setValue('CKG')
    await wrapper.find('[data-testid="filter-to-airport"]').setValue('PEK')
    await wrapper.find('[data-testid="filter-airline"]').setValue('China Eastern')
    await wrapper.find('[data-testid="filter-status"]').setValue('Scheduled')
    await wrapper.find('[data-testid="filter-price-range"]').setValue('0-1000')
    await wrapper.find('[data-testid="filter-depart-slot"]').setValue('morning')
    await submitSearch(wrapper)

    expect(mocks.fetchFlights).toHaveBeenCalledWith({
      fromCity: 'CKG',
      toCity: 'PEK'
    })
    expect(wrapper.find('[data-testid="pagination-current"]').text()).toBe('1')
  })

  it('paginates flights locally with a default page size of 10', async () => {
    mocks.fetchFlights.mockResolvedValueOnce(
      Array.from({ length: 23 }, (_, index) => ({
        id: index + 1,
        flightNo: `MU${index + 1000}`,
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T08:30:00',
        airlineName: index % 2 === 0 ? 'China Eastern' : 'Air China',
        status: 'Scheduled',
        price: 900 + index
      }))
    )
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU1000',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockResolvedValueOnce([])

    const wrapper = createWrapper('en-US')
    await submitSearch(wrapper)

    expect(wrapper.findAll('.flight-row')).toHaveLength(10)
    expect(wrapper.find('[data-testid="pagination-total"]').text()).toBe('23')
    expect(wrapper.find('[data-testid="pagination-current"]').text()).toBe('1')
  })

  it('filters by airline, status, price range, and departure slot on the client', async () => {
    mocks.fetchFlights.mockResolvedValueOnce([
      {
        id: 1,
        flightNo: 'MU5101',
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T08:30:00',
        airlineName: 'China Eastern',
        status: 'Scheduled',
        price: 980
      },
      {
        id: 2,
        flightNo: 'CA1502',
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T20:30:00',
        airlineName: 'Air China',
        status: 'Delayed',
        price: 1680
      }
    ])
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU5101',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockResolvedValueOnce([])

    const wrapper = createWrapper('zh-CN')
    await submitSearch(wrapper)

    await wrapper.find('[data-testid="filter-airline"]').setValue('China Eastern')
    await wrapper.find('[data-testid="filter-status"]').setValue('Scheduled')
    await wrapper.find('[data-testid="filter-price-range"]').setValue('0-1000')
    await wrapper.find('[data-testid="filter-depart-slot"]').setValue('morning')
    await flushPromises()

    expect(wrapper.find('[data-testid="pagination-total"]').text()).toBe('1')
    expect(wrapper.findAll('.flight-row')).toHaveLength(1)
    expect(wrapper.find('.flight-row').text()).toContain('MU5101')
  })

  it('renders localized route labels and success status tags in the table rows', async () => {
    mocks.fetchFlights.mockResolvedValueOnce([
      {
        id: 1,
        flightNo: 'MU5101',
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T08:30:00',
        airlineName: 'China Eastern',
        status: 'Scheduled',
        price: 980
      }
    ])
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU5101',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockResolvedValueOnce([])

    const wrapper = createWrapper('zh-CN')
    await submitSearch(wrapper)

    expect(wrapper.text()).toContain('重庆江北 CKG')
    expect(wrapper.text()).toContain('北京首都 PEK')
    expect(wrapper.text()).toContain('Scheduled')
    expect(wrapper.find('[data-testid="status-tag-1"]').classes()).toContain('flight-table__status--success')
  })

  it('switches page size and renders english airport labels for en-US locale', async () => {
    mocks.fetchFlights.mockResolvedValueOnce(
      Array.from({ length: 21 }, (_, index) => ({
        id: index + 1,
        flightNo: `MU${index + 1000}`,
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T08:30:00',
        airlineName: 'China Eastern',
        status: 'Scheduled',
        price: 980
      }))
    )
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU1000',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockResolvedValueOnce([])

    const wrapper = createWrapper('en-US')
    await submitSearch(wrapper)
    await wrapper.find('[data-testid="pagination-size"]').setValue('20')
    await flushPromises()

    expect(wrapper.findAll('.flight-row')).toHaveLength(20)
    expect(wrapper.text()).toContain('Chongqing Jiangbei CKG')
    expect(wrapper.text()).toContain('Beijing Capital PEK')
  })

  it('renders localized chrome copy and chinese date placeholders', async () => {
    const wrapper = createWrapper('zh-CN')

    expect(wrapper.text()).toContain('每页显示')
    expect(wrapper.text()).toContain('已选航班详情')
    expect(wrapper.find('[data-testid="sync-date"]').attributes('placeholder')).toBe('请选择同步日期')
    expect(wrapper.find('[data-testid="filter-date"]').attributes('placeholder')).toBe('选择出发日期')
  })

  it('formats airport labels and time slots for the current locale', async () => {
    const { AIRPORT_OPTIONS, buildAirportOptionLabel } = await import('../../../shared/constants/airportOptions.js')
    const { buildAirlineDisplayLabel, normalizeFlightForDisplay, matchesTimeSlot } = await import('../../../shared/utils/flightDisplay.js')

    expect(AIRPORT_OPTIONS.some(option => option.code === 'CKG')).toBe(true)
    expect(AIRPORT_OPTIONS.some(option => option.code === 'SHE')).toBe(true)
    expect(AIRPORT_OPTIONS.some(option => option.code === 'UYN')).toBe(true)
    expect(buildAirportOptionLabel('CKG', 'zh-CN')).toContain('重庆江北')
    expect(buildAirportOptionLabel('SHE', 'zh-CN')).toContain('沈阳桃仙')
    expect(buildAirportOptionLabel('PEK', 'en-US')).toContain('Beijing Capital')
    expect(buildAirportOptionLabel('NNG', 'en-US')).toContain('Nanning Wuxu')
    expect(buildAirlineDisplayLabel('China Southern', 'zh-CN')).toBe('中国南方航空')
    expect(buildAirlineDisplayLabel('Sichuan Airlines', 'en-US')).toBe('Sichuan Airlines')
    expect(buildAirlineDisplayLabel('Unknown Demo Air', 'zh-CN')).toBe('Unknown Demo Air')

    const normalized = normalizeFlightForDisplay({
      id: 1,
      flightNo: 'MU5101',
      fromAirport: 'CKG',
      toAirport: 'PEK',
      departTime: '2026-06-19T08:30:00',
      airlineName: 'China Eastern',
      status: 'Scheduled'
    }, 'zh-CN')

    expect(normalized.routeLabel).toContain('重庆江北 CKG')
    expect(normalized.routeLabel).toContain('北京首都 PEK')
    expect(normalized.airlineLabel).toBe('中国东方航空')
    expect(matchesTimeSlot(normalized, 'morning')).toBe(true)
  })

  it('exposes the syncFlights API symbol through the page mock surface', async () => {
    const flightApi = await import('../../../api/flightApi.js')

    expect(flightApi.syncFlights).toBe(mocks.syncFlights)

    await flightApi.syncFlights({ airportCode: 'CKG', date: '2026-06-18' })

    expect(mocks.syncFlights).toHaveBeenCalledWith({
      airportCode: 'CKG',
      date: '2026-06-18'
    })
  })

  it('loads flights after search submission and lets the user switch selection', async () => {
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
    await submitSearch(wrapper)

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
    await submitSearch(wrapper)

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
    await submitSearch(wrapper)

    expect(wrapper.find('.detail-card').text()).toContain('MU1001|China Eastern')
    expect(wrapper.text()).toContain('History service unavailable')
    expect(wrapper.find('.history-chart').text()).toContain('0|idle')
  })

  it('syncs flights successfully, switches source to aerodatabox, and refreshes the list', async () => {
    mocks.syncFlights.mockResolvedValueOnce({
      status: 'SUCCESS',
      successCount: 2,
      failedCount: 0,
      errorMessage: null,
      source: 'aerodatabox',
      requestParams: '{"airportCode":"CKG","date":"2026-06-18"}',
      startedAt: '2026-06-18T08:00:00',
      finishedAt: '2026-06-18T08:00:05'
    })
    mocks.fetchFlights.mockResolvedValueOnce([
      { id: 1, flightNo: 'MU1001' }
    ])
    mocks.fetchFlight.mockResolvedValueOnce({
      id: 1,
      flightNo: 'MU1001',
      airlineName: 'China Eastern'
    })
    mocks.fetchPriceHistory.mockResolvedValueOnce([])

    const wrapper = createWrapper()
    await wrapper.find('[data-testid="sync-airport-code"]').setValue('CKG')
    await wrapper.find('[data-testid="sync-date"]').setValue('2026-06-18')
    await submitSync(wrapper)

    expect(mocks.syncFlights).toHaveBeenCalledWith({
      airportCode: 'CKG',
      date: '2026-06-18'
    })
    expect(mocks.fetchFlights).toHaveBeenCalledWith({
      date: '2026-06-18',
      dataSource: 'aerodatabox'
    })
    expect(wrapper.find('[data-testid="sync-message"]').text()).toContain('Flight sync completed.')
    expect(wrapper.find('[data-testid="sync-status"]').text()).toBe('SUCCESS')
    expect(wrapper.find('[data-testid="sync-success-count"]').text()).toBe('2')
    expect(wrapper.find('[data-testid="sync-failed-count"]').text()).toBe('0')
    expect(wrapper.find('[data-testid="sync-source"]').text()).toBe('aerodatabox')
    expect(wrapper.find('[data-testid="sync-finished-at"]').text()).toBe('2026-06-18T08:00:05')
    expect(wrapper.find('[data-testid="sync-error-message"]').exists()).toBe(false)
    await expandSyncDetails(wrapper)
    expect(wrapper.find('[data-testid="sync-request-params"]').text()).toBe('{"airportCode":"CKG","date":"2026-06-18"}')
    expect(wrapper.find('[data-testid="sync-started-at"]').text()).toBe('2026-06-18T08:00:00')
    expect(wrapper.find('[data-testid="sync-error-message"]').text()).toBe('-')
    expect(wrapper.find('[data-testid="sync-error"]').exists()).toBe(false)
  })

  it('shows backend sync errorMessage without breaking the page', async () => {
    mocks.syncFlights.mockRejectedValueOnce({
      response: {
        data: {
          status: 'FAILED',
          successCount: 0,
          failedCount: 1,
          errorMessage: "'str' object has no attribute 'get'",
          source: 'aerodatabox',
          requestParams: 'airportCode=CKG&date=2026-06-18',
          startedAt: '2026-06-18T08:10:00',
          finishedAt: '2026-06-18T08:10:02'
        }
      }
    })

    const wrapper = createWrapper()
    await wrapper.find('[data-testid="sync-date"]').setValue('2026-06-18')
    await submitSync(wrapper)

    expect(mocks.syncFlights).toHaveBeenCalledWith({
      airportCode: 'CKG',
      date: '2026-06-18'
    })
    expect(wrapper.find('[data-testid="sync-error"]').text()).toContain("'str' object has no attribute 'get'")
    expect(wrapper.find('[data-testid="sync-status"]').text()).toBe('FAILED')
    expect(wrapper.find('[data-testid="sync-success-count"]').text()).toBe('0')
    expect(wrapper.find('[data-testid="sync-failed-count"]').text()).toBe('1')
    expect(wrapper.find('[data-testid="sync-source"]').text()).toBe('aerodatabox')
    expect(wrapper.find('[data-testid="sync-finished-at"]').text()).toBe('2026-06-18T08:10:02')
    await expandSyncDetails(wrapper)
    expect(wrapper.find('[data-testid="sync-request-params"]').text()).toBe('airportCode=CKG&date=2026-06-18')
    expect(wrapper.find('[data-testid="sync-started-at"]').text()).toBe('2026-06-18T08:10:00')
    expect(wrapper.find('[data-testid="sync-error-message"]').text()).toContain("'str' object has no attribute 'get'")
    expect(wrapper.find('[data-testid="sync-result-error-banner"]').text()).toContain("'str' object has no attribute 'get'")
    expect(wrapper.find('.flight-count').text()).toBe('0')
    expect(mocks.fetchFlights).not.toHaveBeenCalled()
  })

  it('renders localized flight search labels', () => {
    const wrapper = createWrapper('zh-CN')

    expect(wrapper.text()).toContain('同步航班数据')
    expect(wrapper.text()).toContain('航班查询结果')
    expect(wrapper.text()).toContain('查询航班')
  })
  it('submits an ai advice query and renders parsed intent, candidates, and summary', async () => {
    mocks.requestAdvice.mockResolvedValueOnce({
      summary: '推荐 MU2201，价格在预算内，上午到达更符合出差安排。',
      intent: {
        from: '重庆',
        to: '北京',
        date: '2026-06-19',
        budget: 1200,
        timePreference: '上午'
      },
      recommendedFlight: {
        id: 11,
        flightNo: 'MU2201',
        airlineName: 'China Eastern',
        fromAirport: 'CKG',
        toAirport: 'PEK',
        departTime: '2026-06-19T07:20:00',
        arriveTime: '2026-06-19T09:40:00',
        price: 980,
        seatsLeft: 4,
        dataSource: 'aerodatabox'
      },
      candidates: [
        {
          id: 11,
          flightNo: 'MU2201',
          airlineName: 'China Eastern',
          fromAirport: 'CKG',
          toAirport: 'PEK',
          departTime: '2026-06-19T07:20:00',
          arriveTime: '2026-06-19T09:40:00',
          price: 980,
          seatsLeft: 4,
          dataSource: 'aerodatabox'
        }
      ]
    })

    const wrapper = createWrapper('zh-CN')
    await expandAiPanel(wrapper)
    await wrapper.find('[data-testid="ai-query-input"]').setValue('下周五去北京出差，预算1200元，希望上午到')
    await wrapper.find('[data-testid="ai-submit"]').trigger('click')
    await flushPromises()

    expect(mocks.requestAdvice).toHaveBeenCalledWith('下周五去北京出差，预算1200元，希望上午到')
    expect(wrapper.find('[data-testid="ai-advice-summary"]').text()).toContain('MU2201')
    expect(wrapper.find('[data-testid="ai-intent"]').text()).toContain('重庆')
    expect(wrapper.find('[data-testid="ai-intent"]').text()).toContain('北京')
    expect(wrapper.find('[data-testid="ai-candidate-list"]').text()).toContain('MU2201')
    expect(wrapper.find('[data-testid="ai-candidate-list"]').text()).toContain('aerodatabox')
  })

  it('shows backend ai error message without breaking the page', async () => {
    mocks.requestAdvice.mockRejectedValueOnce({
      response: {
        data: {
          message: '当前本地数据库暂无符合条件的航班，请先同步航班数据。'
        }
      }
    })

    const wrapper = createWrapper('zh-CN')
    await expandAiPanel(wrapper)
    await wrapper.find('[data-testid="ai-query-input"]').setValue('下周五去北京出差，预算1200元，希望上午到')
    await wrapper.find('[data-testid="ai-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="ai-error"]').text()).toContain('当前本地数据库暂无符合条件的航班')
    expect(wrapper.find('[data-testid="ai-advice-summary"]').exists()).toBe(false)
  })

  it('starts with collapsed sync details and ai panel', async () => {
    const wrapper = createWrapper('zh-CN')

    expect(wrapper.find('[data-testid="sync-toggle-details"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="ai-query-input"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="ai-toggle"]').text()).toContain('展开 AI 出行建议')
  })

  it('renders the dashboard workspace structure', () => {
    const wrapper = createWrapper('en-US')

    expect(wrapper.find('.flight-search-page__header').exists()).toBe(false)
    expect(wrapper.find('.flight-search-page__tabs').exists()).toBe(true)
    expect(wrapper.find('.flight-search-page__controls').exists()).toBe(true)
    expect(wrapper.find('.flight-search-page__sync-strip').exists()).toBe(true)
    expect(wrapper.find('.flight-search-page__console').exists()).toBe(true)
    expect(wrapper.find('.flight-search-page__results-pane').exists()).toBe(true)
    expect(wrapper.find('.flight-search-page__inspector-pane').exists()).toBe(true)
    expect(wrapper.find('.flight-search-page__table-shell').exists()).toBe(true)
  })
  it('expands the ai drawer inline without removing the console workspace', async () => {
    const wrapper = createWrapper('en-US')

    await expandAiPanel(wrapper)

    expect(wrapper.find('.flight-search-page__ai-drawer').classes()).toContain('flight-search-page__ai-drawer--open')
    expect(wrapper.find('.flight-search-page__console').exists()).toBe(true)
    expect(wrapper.find('[data-testid="ai-query-input"]').exists()).toBe(true)
  })
})
