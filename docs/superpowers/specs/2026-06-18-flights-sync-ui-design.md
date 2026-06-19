# Flights Sync UI Design

## Goal

Upgrade `/flights` into a single-page demonstration console for course-defense use without changing the backend architecture.

The page must make the system flow obvious:

1. sync external flight data through the backend
2. persist synced data into local MySQL
3. query local flight data through Spring Boot APIs
4. position the page as part of the AI travel advice platform

## Scope

This design only changes the frontend presentation and frontend calls to existing backend APIs.

The page must not:

- call RapidAPI or AeroDataBox directly
- expose any AeroDataBox key
- change the Spring Boot -> CrawlService -> Python crawler -> MySQL architecture
- replace the existing `/api/flights` query flow

## Page Structure

`/flights` becomes a top-to-bottom single-page console:

1. brand header card
2. highlighted sync card
3. flight query card
4. sync result card
5. flight results table and detail area

## Brand Header

The top card introduces the system:

- title: `机票查询系统`
- subtitle: `基于 AeroDataBox 的航班数据查询与 AI 出行建议平台`
- info badges:
  - `Data Source: AeroDataBox`
  - `Mode: Local MySQL Query`

Visual direction:

- blue-white palette
- wide card with gentle gradient
- strong title hierarchy
- suitable for defense presentation rather than consumer product marketing

## Sync Card

The sync card is the most prominent operational area on the page.

### Fields

- `airportCode`, default `CKG`
- `date`, default current date

### Actions

- primary button: `同步指定日期航班`
- secondary button: `同步今日航班`

### Behavior

Both buttons call the backend only:

- `POST /api/admin/flights/sync?airportCode=...&date=...`

The request must use the existing frontend `http` client so the login token is automatically attached through the current `Authorization: Bearer <token>` interceptor.

### Messaging

The card includes short explanatory copy showing that sync goes through the backend and writes to local MySQL.

## Query Card

The existing flight query card remains, with these fields preserved:

- departure city
- destination city
- date
- source
- search button

The query card continues to call the local backend query API only:

- `GET /api/flights`

No remote sync logic belongs in the query card.

## Sync Result Card

A dedicated result card displays the backend sync response directly on the page.

### Displayed fields

- `status`
- `successCount`
- `failedCount`
- `errorMessage`
- `source`
- `requestParams`
- `startedAt`
- `finishedAt`

### Status handling

- `SUCCESS`: show green success state
- `FAILED`: show red error state
- `RUNNING`: show blue loading/running state
- no sync yet: show neutral empty-state guidance

### Error display

When sync fails, the backend `errorMessage` must be displayed verbatim in the sync result area. It must not be hidden behind console output or a generic toast.

## Flight Results Area

The existing results area remains but is presented as a more explicit local-database result section.

### Table columns

- flight number
- airline
- departure city / airport
- arrival city / airport
- departure time
- arrival time
- price
- seats left
- data source

### Follow-up detail

Keep the existing detail card and price history chart behavior. Do not redesign that flow unless required by layout adjustments.

## State Design

The page keeps existing search state and adds separate sync state.

### Existing search state kept

- `loading`
- `errorMessage`
- `flights`
- `selectedFlight`
- `selectedFlightId`
- `priceHistory`
- `historyLoading`
- `filters`

### New sync state

- `syncForm`
  - `airportCode`
  - `date`
- `syncLoading`
- `syncResult`
- `syncMessage`
- `syncError`

This separation prevents sync UI behavior from breaking existing query behavior.

## Frontend API Design

Add a new frontend method:

- `syncFlights({ airportCode, date })`

Implementation:

- use existing `http` client
- send `POST /api/admin/flights/sync`
- pass `airportCode` and `date` as query parameters

Because `http` already injects the stored token, no extra token wiring should be introduced in the page component.

## Interaction Flow

### Sync success

When sync succeeds:

1. show green success feedback
2. store the returned crawl-job payload in `syncResult`
3. set `filters.dataSource = "aerodatabox"`
4. if a sync date exists, set `filters.date = syncForm.date`
5. automatically call existing `submitSearch()`

### Sync failure

When sync fails:

1. show red failure feedback
2. store backend error information in `syncError`
3. render backend `errorMessage` in the sync result card
4. do not crash the page
5. keep query functionality usable

### Sync empty-but-successful

If the backend returns a success-like result with no flights fetched, treat it as a successful sync state with an explanatory message rather than a page error.

## Empty and Error States

### Empty states

- before any sync: show a neutral sync guidance card
- query returns no rows: show a local-data empty state encouraging the user to run sync first

### Error states

- sync errors: shown in sync result card
- query errors: continue using the page-level query error banner

## Files To Modify

Primary frontend files:

- `frontend/src/api/flightApi.js`
- `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

Likely i18n files:

- `frontend/src/i18n/messages/zh-CN.js`
- `frontend/src/i18n/messages/en-US.js`

No frontend file may contain the AeroDataBox key or any RapidAPI direct-call logic.

## Implementation Steps

1. Extend `flightApi.js` with `syncFlights({ airportCode, date })`.
2. Add sync-related state and actions to `FlightSearchPage.vue`.
3. Add the new page layout sections:
   - brand header
   - sync card
   - query card
   - sync result card
   - existing results area
4. Hook sync buttons to the backend sync API through the shared `http` client.
5. On success:
   - set `filters.dataSource` to `aerodatabox`
   - sync `filters.date`
   - call `submitSearch()`
6. On failure:
   - show backend `errorMessage` in the sync result area
7. Update tests for sync success, sync failure, and auto-refresh behavior.
8. Update i18n strings if the page still uses translated copy.

## Non-Goals

- no backend API changes
- no frontend direct RapidAPI calls
- no new Java service layer
- no advanced animations
- no redesign of unrelated pages
