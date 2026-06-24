# Flights UX Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade `/flights` into a more user-friendly flight query console with airport dropdowns, localized route display, richer local filters, and client-side pagination without changing backend APIs.

**Architecture:** Keep the current `FlightSearchPage.vue + FlightTable.vue` structure and existing backend calls intact. Add a small frontend airport catalog plus display/filter helpers, then let `FlightSearchPage.vue` own local filtering and pagination state while `FlightTable.vue` remains the presentational result table.

**Tech Stack:** Vue 3 SFCs, Vue I18n, Element Plus, Vitest, Vue Test Utils

---

## File Structure

**Create**
- `frontend/src/shared/constants/airportOptions.js`
- `frontend/src/shared/utils/flightDisplay.js`

**Modify**
- `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`
- `frontend/src/shared/components/FlightTable.vue`
- `frontend/src/i18n/messages/en-US.js`
- `frontend/src/i18n/messages/zh-CN.js`

The new constant file owns the airport directory and locale-aware labels. The utility file owns route display, status normalization, time-slot classification, and frontend-only filter predicates. `FlightSearchPage.vue` keeps sync/search logic plus new local pagination/filter state. `FlightTable.vue` only renders already-processed rows and visual status tags.

### Task 1: Add airport catalog and display/filter helpers

**Files:**
- Create: `frontend/src/shared/constants/airportOptions.js`
- Create: `frontend/src/shared/utils/flightDisplay.js`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Write the failing utility expectations in the page spec**

Add a lightweight import-and-assert block near the existing tests so the new helper contract is pinned before implementation:

```js
it('formats airport labels and time slots for the current locale', async () => {
  const { AIRPORT_OPTIONS, buildAirportOptionLabel } = await import('../../../shared/constants/airportOptions.js')
  const { normalizeFlightForDisplay, matchesTimeSlot } = await import('../../../shared/utils/flightDisplay.js')

  expect(AIRPORT_OPTIONS.some(option => option.code === 'CKG')).toBe(true)
  expect(buildAirportOptionLabel('CKG', 'zh-CN')).toContain('重庆江北')
  expect(buildAirportOptionLabel('PEK', 'en-US')).toContain('Beijing Capital')

  const normalized = normalizeFlightForDisplay({
    id: 1,
    flightNo: 'MU5101',
    fromAirport: 'CKG',
    toAirport: 'PEK',
    departTime: '2026-06-19T08:30:00',
    status: 'Scheduled'
  }, 'zh-CN')

  expect(normalized.routeLabel).toContain('重庆江北 CKG')
  expect(normalized.routeLabel).toContain('北京首都 PEK')
  expect(matchesTimeSlot(normalized, 'morning')).toBe(true)
})
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: FAIL with module-not-found errors for `airportOptions.js` and `flightDisplay.js`.

- [ ] **Step 3: Create the airport catalog**

Add the airport list and locale-aware label builder:

```js
export const AIRPORT_OPTIONS = [
  { code: 'CKG', cityCode: 'CKG', cityNameZh: '重庆', cityNameEn: 'Chongqing', airportNameZh: '重庆江北', airportNameEn: 'Chongqing Jiangbei' },
  { code: 'PEK', cityCode: 'BJS', cityNameZh: '北京', cityNameEn: 'Beijing', airportNameZh: '北京首都', airportNameEn: 'Beijing Capital' },
  { code: 'PKX', cityCode: 'BJS', cityNameZh: '北京', cityNameEn: 'Beijing', airportNameZh: '北京大兴', airportNameEn: 'Beijing Daxing' },
  { code: 'PVG', cityCode: 'SHA', cityNameZh: '上海', cityNameEn: 'Shanghai', airportNameZh: '上海浦东', airportNameEn: 'Shanghai Pudong' },
  { code: 'SHA', cityCode: 'SHA', cityNameZh: '上海', cityNameEn: 'Shanghai', airportNameZh: '上海虹桥', airportNameEn: 'Shanghai Hongqiao' },
  { code: 'CAN', cityCode: 'CAN', cityNameZh: '广州', cityNameEn: 'Guangzhou', airportNameZh: '广州白云', airportNameEn: 'Guangzhou Baiyun' },
  { code: 'SZX', cityCode: 'SZX', cityNameZh: '深圳', cityNameEn: 'Shenzhen', airportNameZh: '深圳宝安', airportNameEn: 'Shenzhen Baoan' },
  { code: 'TFU', cityCode: 'CTU', cityNameZh: '成都', cityNameEn: 'Chengdu', airportNameZh: '成都天府', airportNameEn: 'Chengdu Tianfu' }
]

export function buildAirportOptionLabel(code, locale = 'zh-CN') {
  const airport = AIRPORT_OPTIONS.find(option => option.code === code)
  if (!airport) return code
  return locale === 'zh-CN'
    ? `${airport.airportNameZh} ${airport.code}`
    : `${airport.airportNameEn} ${airport.code}`
}
```

- [ ] **Step 4: Create the display/filter helper module**

Implement locale-aware normalization plus status/price/time predicates:

```js
import { AIRPORT_OPTIONS, buildAirportOptionLabel } from '../constants/airportOptions.js'

const SUCCESS_STATUSES = new Set(['scheduled', 'success'])
const FAILED_STATUSES = new Set(['cancelled', 'failed'])
const DELAYED_STATUSES = new Set(['delayed'])

export function normalizeStatusTone(status) {
  const normalized = String(status ?? '').trim().toLowerCase()
  if (SUCCESS_STATUSES.has(normalized)) return 'success'
  if (FAILED_STATUSES.has(normalized)) return 'failed'
  if (DELAYED_STATUSES.has(normalized)) return 'warning'
  return 'neutral'
}

export function normalizeFlightForDisplay(flight, locale = 'zh-CN') {
  const fromCode = String(flight.fromAirport ?? '').trim().toUpperCase()
  const toCode = String(flight.toAirport ?? '').trim().toUpperCase()

  return {
    ...flight,
    fromAirportCode: fromCode,
    toAirportCode: toCode,
    fromAirportLabel: buildAirportOptionLabel(fromCode || flight.fromAirport || '-', locale),
    toAirportLabel: buildAirportOptionLabel(toCode || flight.toAirport || '-', locale),
    routeLabel: `${buildAirportOptionLabel(fromCode || flight.fromAirport || '-', locale)} → ${buildAirportOptionLabel(toCode || flight.toAirport || '-', locale)}`,
    statusLabel: flight.status || 'Unknown',
    statusTone: normalizeStatusTone(flight.status),
    airlineLabel: flight.airlineName || 'Unknown Airline'
  }
}

export function matchesTimeSlot(flight, slot) {
  if (!slot) return true
  const hour = new Date(flight.departTime).getHours()
  if (slot === 'overnight') return hour < 6
  if (slot === 'morning') return hour >= 6 && hour < 12
  if (slot === 'afternoon') return hour >= 12 && hour < 18
  if (slot === 'evening') return hour >= 18
  return true
}
```

- [ ] **Step 5: Re-run the page test to verify the helper contract passes**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: helper assertions pass, later UI assertions may still fail.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/shared/constants/airportOptions.js frontend/src/shared/utils/flightDisplay.js frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: add airport catalog and flight display helpers"
```

### Task 2: Convert sync/search airports to dropdowns and add local filter state

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`
- Modify: `frontend/src/i18n/messages/en-US.js`
- Modify: `frontend/src/i18n/messages/zh-CN.js`

- [ ] **Step 1: Write failing page tests for airport dropdown values and new local filters**

Extend the current page spec with Element Plus select stubs and add a test like:

```js
it('submits airport dropdown values as IATA codes and resets pagination on a new search', async () => {
  mocks.fetchFlights.mockResolvedValueOnce(new Array(12).fill(null).map((_, index) => ({
    id: index + 1,
    flightNo: `MU${index + 1000}`,
    fromAirport: 'CKG',
    toAirport: 'PEK',
    departTime: '2026-06-19T08:30:00',
    airlineName: index % 2 === 0 ? 'China Eastern' : 'Air China',
    status: index % 3 === 0 ? 'Scheduled' : 'Delayed',
    price: 900 + index
  })))

  const wrapper = createWrapper('zh-CN')
  await wrapper.find('[data-testid="sync-airport-code"]').setValue('PEK')
  await wrapper.find('[data-testid="filter-from-airport"]').setValue('CKG')
  await wrapper.find('[data-testid="filter-to-airport"]').setValue('PEK')
  await submitSearch(wrapper)

  expect(mocks.fetchFlights).toHaveBeenCalledWith({
    fromCity: 'CKG',
    toCity: 'PEK'
  })
  expect(wrapper.find('[data-testid="pagination-current"]').text()).toBe('1')
})
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: FAIL because the page still uses inputs and has no pagination/filter state.

- [ ] **Step 3: Replace free-text airport inputs with dropdown-backed fields**

In `FlightSearchPage.vue`, import the airport catalog and add the new search filter fields:

```js
import { AIRPORT_OPTIONS } from '../../../shared/constants/airportOptions.js'

const filters = reactive({
  fromCity: '',
  toCity: '',
  date: '',
  dataSource: '',
  airline: '',
  priceRange: '',
  status: '',
  departSlot: ''
})

const syncForm = reactive({
  airportCode: 'CKG',
  date: getTodayDateString()
})
```

Then change the template fields:

```vue
<el-select v-model="syncForm.airportCode" data-testid="sync-airport-code">
  <el-option
    v-for="option in airportOptions"
    :key="option.code"
    :label="getAirportLabel(option.code)"
    :value="option.code"
  />
</el-select>
```

And likewise for `filters.fromCity` / `filters.toCity`.

- [ ] **Step 4: Add frontend-only local filter options and reset behavior**

Add computed options plus a shared reset helper:

```js
const currentPage = ref(1)
const pageSize = ref(10)

function resetPagination() {
  currentPage.value = 1
}

function onFilterChange() {
  resetPagination()
}
```

Wire `@change="onFilterChange"` on dropdowns/date controls and call `resetPagination()` at the start of `submitSearch()` and after successful sync-driven refresh.

- [ ] **Step 5: Update i18n labels for new filter fields**

Add strings for:

```js
filters: {
  from: 'From',
  to: 'To',
  date: 'Date',
  source: 'Source',
  airline: 'Airline',
  priceRange: 'Price Range',
  status: 'Status',
  departSlot: 'Departure Window'
}
```

And Chinese equivalents:

```js
filters: {
  from: '出发机场',
  to: '到达机场',
  date: '日期',
  source: '来源',
  airline: '航空公司',
  priceRange: '价格区间',
  status: '航班状态',
  departSlot: '出发时段'
}
```

- [ ] **Step 6: Re-run the page test to verify dropdown submission and pagination reset pass**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: dropdown submission assertions pass; table-display assertions may still fail.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js frontend/src/i18n/messages/en-US.js frontend/src/i18n/messages/zh-CN.js
git commit -m "feat: add airport dropdowns and local filter state"
```

### Task 3: Add local filtering and client-side pagination to the flights page

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Write failing pagination and filter tests**

Add tests that assert:

```js
it('paginates flights locally with a default page size of 10', async () => {
  mocks.fetchFlights.mockResolvedValueOnce(makeFlights(23))
  mocks.fetchFlight.mockResolvedValueOnce({ id: 1, flightNo: 'MU1001' })
  mocks.fetchPriceHistory.mockResolvedValueOnce([])

  const wrapper = createWrapper('en-US')
  await submitSearch(wrapper)

  expect(wrapper.findAll('.flight-row')).toHaveLength(10)
  expect(wrapper.find('[data-testid="pagination-total"]').text()).toBe('23')
})
```

and:

```js
it('filters by airline, status, price range, and departure slot on the client', async () => {
  mocks.fetchFlights.mockResolvedValueOnce(makeMixedFlights())
  const wrapper = createWrapper('zh-CN')
  await submitSearch(wrapper)

  await wrapper.find('[data-testid="filter-airline"]').setValue('China Eastern')
  await wrapper.find('[data-testid="filter-status"]').setValue('Scheduled')
  await wrapper.find('[data-testid="filter-price-range"]').setValue('0-1000')
  await wrapper.find('[data-testid="filter-depart-slot"]').setValue('morning')
  await flushPromises()

  expect(wrapper.find('[data-testid="pagination-total"]').text()).toBe('1')
  expect(wrapper.find('.flight-row').text()).toContain('MU')
})
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: FAIL because the page still renders all rows and has no local filter pipeline.

- [ ] **Step 3: Build the frontend-only result pipeline**

Inside `FlightSearchPage.vue`, add computed layers:

```js
const locale = computed(() => i18nLocale.value)

const displayFlights = computed(() =>
  flights.value.map(flight => normalizeFlightForDisplay(flight, locale.value))
)

const filteredFlights = computed(() =>
  displayFlights.value.filter(flight => {
    if (filters.airline && flight.airlineLabel !== filters.airline) return false
    if (filters.status && normalizeStatusValue(flight.statusLabel) !== filters.status) return false
    if (filters.priceRange && !matchesPriceRange(flight.price, filters.priceRange)) return false
    if (filters.departSlot && !matchesTimeSlot(flight, filters.departSlot)) return false
    return true
  })
)

const totalCount = computed(() => filteredFlights.value.length)
const pagedFlights = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredFlights.value.slice(start, start + pageSize.value)
})
```

- [ ] **Step 4: Add pagination controls and page-size switching**

Render a small summary bar plus pagination:

```vue
<div class="flight-search-page__result-toolbar">
  <span data-testid="pagination-total">{{ totalCount }}</span>
  <span data-testid="pagination-current">{{ currentPage }}</span>
  <el-select v-model="pageSize" data-testid="pagination-size" @change="resetPagination">
    <el-option :value="10" label="10 / page" />
    <el-option :value="20" label="20 / page" />
  </el-select>
  <el-pagination
    :current-page="currentPage"
    :page-size="pageSize"
    :total="totalCount"
    layout="prev, pager, next"
    @current-change="currentPage = $event"
  />
</div>
```

Pass `pagedFlights` instead of `flights` into `FlightTable`.

- [ ] **Step 5: Ensure selection stays valid after filtering/pagination**

Add a small watcher-style guard:

```js
watch(pagedFlights, rows => {
  if (!rows.length) {
    selectedFlight.value = null
    selectedFlightId.value = null
    priceHistory.value = []
    return
  }
  if (!rows.some(row => row.id === selectedFlightId.value)) {
    void selectFlight(rows[0])
  }
}, { immediate: false })
```

- [ ] **Step 6: Re-run the page test to verify pagination and filters pass**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: PASS for pagination/filter cases, with existing sync cases still green.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: add local filtering and pagination to flights page"
```

### Task 4: Upgrade the result table for localized route display and status tags

**Files:**
- Modify: `frontend/src/shared/components/FlightTable.vue`
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`
- Modify: `frontend/src/i18n/messages/en-US.js`
- Modify: `frontend/src/i18n/messages/zh-CN.js`

- [ ] **Step 1: Write failing assertions for localized route display and status tags**

Add a test that asserts the rendered table rows show:

```js
expect(wrapper.text()).toContain('重庆江北 CKG')
expect(wrapper.text()).toContain('北京首都 PEK')
expect(wrapper.text()).toContain('Scheduled')
expect(wrapper.find('[data-testid="status-tag-1"]').classes()).toContain('flight-table__status--success')
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: FAIL because the table still renders raw city/airport fields and no status column.

- [ ] **Step 3: Update the table columns to use normalized display fields**

Change the route and status rendering in `FlightTable.vue`:

```vue
<el-table-column :label="t('flights.table.columns.route')" min-width="220">
  <template #default="{ row }">
    <div class="flight-table__primary">{{ row.routeLabel }}</div>
    <div class="flight-table__secondary">{{ row.fromAirportLabel }} → {{ row.toAirportLabel }}</div>
  </template>
</el-table-column>

<el-table-column :label="t('flights.table.columns.status')" min-width="120" align="center">
  <template #default="{ row }">
    <span
      :data-testid="`status-tag-${row.id}`"
      :class="['flight-table__status', `flight-table__status--${row.statusTone}`]"
    >
      {{ row.statusLabel || t('flights.table.unknownStatus') }}
    </span>
  </template>
</el-table-column>
```

- [ ] **Step 4: Add status-tag styles and clearer value emphasis**

Add scoped CSS:

```css
.flight-table__status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.flight-table__status--success { color: #166534; background: #f0fdf4; }
.flight-table__status--failed { color: #b91c1c; background: #fef2f2; }
.flight-table__status--warning { color: #b45309; background: #fff7ed; }
.flight-table__status--neutral { color: #475569; background: #f1f5f9; }
```

- [ ] **Step 5: Add i18n keys for the new table/status copy**

Add:

```js
table: {
  unknownStatus: 'Unknown',
  columns: {
    flight: 'Flight',
    route: 'Route',
    departure: 'Departure',
    arrival: 'Arrival',
    price: 'Price',
    seats: 'Seats',
    source: 'Source',
    status: 'Status'
  }
}
```

And Chinese:

```js
table: {
  unknownStatus: '未知',
  columns: {
    status: '状态'
  }
}
```

- [ ] **Step 6: Re-run the page test to verify route localization and status tags pass**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add frontend/src/shared/components/FlightTable.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js frontend/src/i18n/messages/en-US.js frontend/src/i18n/messages/zh-CN.js
git commit -m "feat: localize flight table routes and status tags"
```

### Task 5: Final verification and polish

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Modify: `frontend/src/shared/components/FlightTable.vue`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Add final assertions for page-size switching and locale-sensitive labels**

Add one final test:

```js
it('switches page size and renders english airport labels for en-US locale', async () => {
  mocks.fetchFlights.mockResolvedValueOnce(makeFlights(21, { fromAirport: 'CKG', toAirport: 'PEK' }))
  const wrapper = createWrapper('en-US')
  await submitSearch(wrapper)

  await wrapper.find('[data-testid="pagination-size"]').setValue('20')
  await flushPromises()

  expect(wrapper.findAll('.flight-row')).toHaveLength(20)
  expect(wrapper.text()).toContain('Chongqing Jiangbei CKG')
  expect(wrapper.text()).toContain('Beijing Capital PEK')
})
```

- [ ] **Step 2: Run the page test to verify it fails if any locale/pagination wiring is missing**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
```

Expected: either PASS immediately or fail on the remaining missing wiring.

- [ ] **Step 3: Make the minimal polish fixes**

If needed, adjust:

```js
watch([() => filters.airline, () => filters.priceRange, () => filters.status, () => filters.departSlot], () => {
  resetPagination()
})
```

and ensure the locale passed to `normalizeFlightForDisplay` comes from `useI18n()`:

```js
const { t, locale } = useI18n()
```

- [ ] **Step 4: Run the focused tests and production build**

Run:

```bash
cd frontend
npx.cmd vitest run src/modules/user-flights/pages/FlightSearchPage.spec.js
npm.cmd run build
```

Expected:
- `FlightSearchPage.spec.js` passes
- Vite build succeeds

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/shared/components/FlightTable.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: polish flights query experience"
```

## Self-Review

**Spec coverage:** This plan covers all approved requirements: client-side pagination, airport dropdowns, airport mapping, localized route display, airline/price/status/time-slot filtering, clearer result-table presentation, and unchanged backend boundaries.

**Placeholder scan:** No `TODO`/`TBD` placeholders remain. Each task includes concrete files, code shape, commands, and expected outcomes.

**Type consistency:** The plan uses one shared naming set throughout:
- airport options: `AIRPORT_OPTIONS`
- normalization helper: `normalizeFlightForDisplay`
- page state: `currentPage`, `pageSize`, `filteredFlights`, `pagedFlights`
- table fields: `routeLabel`, `fromAirportLabel`, `toAirportLabel`, `statusLabel`, `statusTone`

