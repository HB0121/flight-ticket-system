# Flights Sync UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `/flights` into a single-page demonstration console that can trigger backend sync, display sync results, and refresh local flight results without exposing RapidAPI details.

**Architecture:** The frontend stays as a thin UI layer over existing Spring Boot APIs. `flightApi.js` gains one sync method for `POST /api/admin/flights/sync`, while `FlightSearchPage.vue` adds a separate sync state machine and new layout sections. Existing `/api/flights` search, detail, and history flows remain intact and are reused after successful sync.

**Tech Stack:** Vue 3 SFCs, Vue I18n, Axios, Vitest, Vue Test Utils, Element Plus

---

### Task 1: Add frontend sync API wrapper

**Files:**
- Modify: `frontend/src/api/flightApi.js`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Write the failing sync API expectation in the page test**

Extend the existing `flightApi.js` mock in `FlightSearchPage.spec.js` to include `syncFlights`, then add one test that expects a sync call to happen with `airportCode` and `date` when the new sync form is submitted.

Use a mock shape like:

```js
const mocks = vi.hoisted(() => ({
  fetchFlights: vi.fn(),
  fetchFlight: vi.fn(),
  fetchPriceHistory: vi.fn(),
  syncFlights: vi.fn()
}))
```

Expected failing assertion:

```js
expect(mocks.syncFlights).toHaveBeenCalledWith({
  airportCode: 'CKG',
  date: '2026-06-18'
})
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: FAIL because `syncFlights` does not exist in `flightApi.js` and is not called by the page.

- [ ] **Step 3: Implement the minimal frontend API method**

Update `frontend/src/api/flightApi.js` to add:

```js
export async function syncFlights({ airportCode, date }) {
  const response = await http.post('/api/admin/flights/sync', null, {
    params: { airportCode, date }
  })
  return response.data
}
```

Do not add token code here. Reuse the existing `http` instance only.

- [ ] **Step 4: Re-run the page test and verify the API symbol is now available**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: the test still fails, but now due to missing page wiring instead of missing API support.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/flightApi.js frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: add frontend flight sync api wrapper"
```

### Task 2: Add sync state and behavior to `FlightSearchPage.vue`

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Write failing tests for sync success and sync failure**

Add two tests:

1. sync success:
   - calls `syncFlights`
   - stores sync result on screen
   - sets `filters.dataSource` to `aerodatabox`
   - sets `filters.date` to the synced date
   - re-runs `fetchFlights`

2. sync failure:
   - shows the backend `errorMessage`
   - does not crash the page

Use mocked backend responses like:

```js
mocks.syncFlights.mockResolvedValueOnce({
  status: 'SUCCESS',
  successCount: 8,
  failedCount: 0,
  errorMessage: null,
  source: 'aerodatabox',
  requestParams: 'source=aerodatabox, airportCode=CKG, date=2026-06-18',
  startedAt: '2026-06-18T10:00:00',
  finishedAt: '2026-06-18T10:01:00'
})
```

and

```js
mocks.syncFlights.mockRejectedValueOnce({
  response: {
    data: {
      errorMessage: "'str' object has no attribute 'get'"
    }
  }
})
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: FAIL because the page has no sync state, no sync form, and no sync result output.

- [ ] **Step 3: Implement sync state and handlers**

In `FlightSearchPage.vue`, add:

- `syncForm = reactive({ airportCode: 'CKG', date: todayString() })`
- `syncLoading = ref(false)`
- `syncResult = ref(null)`
- `syncMessage = ref('')`
- `syncError = ref('')`

Add handlers:

- `submitSync()`
- `syncToday()`
- `applySuccessfulSync(syncPayload)`
- `getSyncErrorMessage(error)`

Behavior:

- call `syncFlights({ airportCode, date })`
- on success:
  - store `syncResult`
  - set `syncMessage`
  - clear `syncError`
  - set `filters.dataSource = 'aerodatabox'`
  - set `filters.date = syncForm.date`
  - `await submitSearch()`
- on failure:
  - store `syncError`
  - keep `syncResult` when available or clear it
  - do not touch the backend architecture or any token logic

- [ ] **Step 4: Re-run the page test to verify logic passes**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: sync behavior tests pass, layout-related tests may still fail until markup is added.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: add flights page sync state and auto refresh"
```

### Task 3: Restructure the page layout into the single-page demonstration console

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Write failing layout assertions**

Add tests that assert the page renders:

- the new top brand title
- the new sync section heading
- the new sync result block
- the existing search button and result area

Assertions should look for the visible copy rather than implementation details, for example:

```js
expect(wrapper.text()).toContain('机票查询系统')
expect(wrapper.text()).toContain('同步航班数据')
expect(wrapper.text()).toContain('航班查询')
expect(wrapper.text()).toContain('航班查询结果')
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: FAIL because the page still has the old layout.

- [ ] **Step 3: Implement the new markup and styles**

Update `FlightSearchPage.vue` template and scoped CSS to add:

- blue-white hero header with title, subtitle, and two badges
- prominent sync card above the query form
- query card label and wrapper
- sync result card below the query card
- existing `FlightTable`, `FlightDetailCard`, and `PriceHistoryChart` beneath that

Do not rewrite the existing result-selection logic. Reuse current components and current search flow.

Style constraints:

- blue-white palette
- card layout
- clear, stable presentation
- no complex animation requirements

- [ ] **Step 4: Re-run the page test to verify the layout passes**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: PASS for the new copy/layout assertions.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: redesign flights page as sync demo console"
```

### Task 4: Show sync result fields and explicit error output

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Write failing tests for result-field rendering**

Add tests that assert the sync result card shows:

- `status`
- `successCount`
- `failedCount`
- `errorMessage`
- `source`
- `requestParams`
- `startedAt`
- `finishedAt`

Also assert that a backend failure message is rendered verbatim when sync fails.

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: FAIL because the card does not yet render the full result payload.

- [ ] **Step 3: Implement sync result rendering**

In the sync result section of `FlightSearchPage.vue`, render a field grid and dedicated `errorMessage` block. Use a state class such as:

- `is-success`
- `is-failed`
- `is-idle`

Keep the backend `errorMessage` unchanged in the UI text.

- [ ] **Step 4: Re-run the page test to verify it passes**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: show sync job result and backend errors on flights page"
```

### Task 5: Update localized copy if the page still relies on i18n keys

**Files:**
- Modify: `frontend/src/i18n/messages/zh-CN.js`
- Modify: `frontend/src/i18n/messages/en-US.js`
- Test: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: Add failing localization assertions if new copy uses i18n**

If the page uses translation keys for the new sync/header/result sections, add assertions that both locales render the new terms, for example:

```js
expect(wrapper.text()).toContain('同步航班数据')
expect(wrapper.text()).toContain('Sync Flight Data')
```

- [ ] **Step 2: Run the page test to verify it fails**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: FAIL only if the page references missing translation keys.

- [ ] **Step 3: Add the new translation keys**

Add only the keys required by the new sync console:

- page brand title/subtitle
- badges
- sync card labels
- sync result labels
- success and failure helper text
- empty-state copy

Do not rewrite unrelated message groups.

- [ ] **Step 4: Re-run the page test to verify localization passes**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/i18n/messages/zh-CN.js frontend/src/i18n/messages/en-US.js frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "feat: localize flights sync console copy"
```

### Task 6: Final verification

**Files:**
- Modify: `docs/superpowers/specs/2026-06-18-flights-sync-ui-design.md` only if implementation reveals a necessary wording correction

- [ ] **Step 1: Run the focused frontend page tests**

Run:

```bash
cd frontend
npm run test -- FlightSearchPage.spec.js
```

Expected: PASS

- [ ] **Step 2: Run the broader frontend test suite if practical**

Run:

```bash
cd frontend
npm run test
```

Expected: PASS, or document any unrelated pre-existing failures clearly.

- [ ] **Step 3: Start the frontend and verify manual behavior**

Run:

```bash
cd frontend
npm run dev
```

Manual check:

- log in
- open `/flights`
- use the top sync card
- verify sync result card updates
- verify successful sync sets source to `aerodatabox`
- verify the page re-runs flight search
- verify failed sync shows backend `errorMessage` on page

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/flightApi.js frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js frontend/src/i18n/messages/zh-CN.js frontend/src/i18n/messages/en-US.js
git commit -m "feat: add sync-driven flights demo console"
```
