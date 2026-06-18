# Frontend I18n Design

## Goal

Add frontend internationalization for the phase-1 user and admin flows without changing backend APIs or backend-generated messages.

This first scope covers:

- `/auth`
- `/flights`
- `/favorites`
- `/history`
- `/admin/crawl-jobs`
- `/admin/data-sources`

Shared layout and shared flight UI used by those pages are included in scope.

## Non-Goals

- No backend internationalization
- No database schema changes
- No translation of backend error payloads
- No additional languages beyond `zh-CN` and `en-US`
- No full-repo sweep outside the selected phase-1 flow

## Requirements

### Language Selection

- The app must support `zh-CN` and `en-US`
- Manual user selection takes priority
- If no manual selection exists, the app falls back to the browser language
- Unsupported browser languages fall back to `zh-CN`
- The chosen locale must persist in `localStorage`
- Locale switching must update visible UI immediately without a page reload

### UI Coverage

The following UI text must be internationalized in this phase:

- Page titles, subtitles, and labels
- Navigation labels
- Buttons
- Empty states
- Static error fallbacks defined in frontend code
- Shared component headings and table labels

The following may remain unchanged in this phase:

- Backend-provided error strings returned by APIs
- Raw data values such as airline names and data-source codes

## Approach

Use a standard frontend i18n layer based on `vue-i18n`.

This is preferred over a custom translation helper because the project already uses Vue 3 and will benefit from:

- Standard composition API support
- Centralized message catalogs
- Straightforward expansion to additional pages and languages
- Cleaner testing and lower long-term maintenance cost

## Architecture

Add a dedicated i18n module under `frontend/src/i18n/`:

- `index.js`
  - creates and exports the `vue-i18n` instance
- `locale.js`
  - resolves the initial locale
  - reads and writes the persisted locale
  - normalizes browser language values
- `messages/zh-CN.js`
  - Simplified Chinese message catalog
- `messages/en-US.js`
  - English message catalog

Integrate the i18n instance in `frontend/src/main.js`.

## Locale Resolution Rules

Initial locale resolution order:

1. Read persisted locale from `localStorage`
2. If absent, inspect `navigator.language`
3. Normalize browser language:
   - `zh`, `zh-CN`, `zh-SG` -> `zh-CN`
   - `en`, `en-US`, `en-GB` -> `en-US`
4. Fall back to `zh-CN` for anything else

Persisted values outside the supported set must be ignored and treated as absent.

## UI Integration

Add a global language switcher in the main layout header used by the protected routes.

Behavior:

- Shows the current locale
- Allows switching between Chinese and English
- Updates the global locale immediately
- Stores the new value in `localStorage`

The auth page must also expose the same language switch affordance, either directly in the auth card or its surrounding shell, so the user can change language before login.

## Message Key Strategy

Use stable semantic keys grouped by domain, not raw text keys.

Examples:

- `common.actions.search`
- `common.status.loading`
- `auth.login.title`
- `auth.register.submit`
- `layout.nav.flights`
- `flights.search.fromLabel`
- `flights.table.empty`
- `favorites.empty`
- `history.title`
- `admin.crawlJobs.createButton`
- `admin.dataSources.subtitle`

Guidelines:

- Shared text belongs in `common.*`
- Module-specific text belongs under its route or feature namespace
- Avoid duplicating the same English phrase under many unrelated keys when the meaning is the same
- Prefer explicit semantic names over page-position names

## Affected Frontend Areas

Primary implementation targets:

- App bootstrap:
  - `frontend/src/main.js`
- Layouts and navigation:
  - route layout components
- Auth:
  - auth page and related static UI text
- User flow:
  - flights page
  - favorites page
  - history page
- Admin flow:
  - crawl jobs page
  - data-source status page
- Shared UI:
  - flight table
  - flight detail card
  - any shared chart or panel labels inside the scoped pages

## Error Handling

Frontend-defined fallback messages must come from i18n.

Examples:

- generic request failure messages
- empty-state descriptions
- partial-load fallback text

Backend-returned messages stay as-is in this phase. The rendering rule is:

- If the backend returned a message string, show it directly
- Otherwise use the localized frontend fallback

This avoids mixing backend i18n work into the current scope while still internationalizing the static shell.

## Testing Strategy

Add or update frontend tests before implementation changes.

Required test coverage:

- Locale resolution:
  - persisted locale overrides browser language
  - browser Chinese resolves to `zh-CN`
  - browser English resolves to `en-US`
  - unsupported browser locale falls back to `zh-CN`
- Locale switching:
  - switching updates the rendered labels without reload
  - switching writes the locale to `localStorage`
- Route-shell coverage:
  - protected layout navigation labels render in the active locale
- Page coverage:
  - auth page key labels switch
  - flights page headings and controls switch
  - admin crawl page key labels switch

Tests do not need to assert every single string in every page. They should validate representative coverage for each module and the shared switching behavior.

## Rollout Constraints

- Preserve current route structure
- Preserve current auth and data-fetch behavior
- Do not introduce server-side locale negotiation
- Do not refactor unrelated page logic during the i18n pass

## Acceptance Criteria

The work is complete when:

- The frontend supports `zh-CN` and `en-US`
- The user can switch language manually
- The selected language persists across refreshes
- Browser language is used only when no manual selection exists
- All scoped phase-1 pages and their shared UI render localized static text
- Backend messages remain functional and are not broken by the new i18n layer

## Risks

### Existing Hardcoded Text Spread Across Many Files

Mitigation:

- Limit the first pass strictly to scoped phase-1 pages and shared components
- Centralize repeated labels into `common.*`

### Inconsistent Copy Between Shared Components and Pages

Mitigation:

- Use a single message catalog per locale
- Reuse keys for shared concepts such as search, loading, empty, price, and source

### Mixed Language Output Because Backend Messages Stay Raw

Mitigation:

- Treat that as an intentional phase boundary
- Only frontend-owned strings must be localized in this pass

## Implementation Sequence

1. Add i18n infrastructure and locale resolution utilities
2. Add message catalogs for `zh-CN` and `en-US`
3. Add global language switch UI
4. Migrate shared layout and shared flight components
5. Migrate scoped phase-1 pages
6. Update and extend frontend tests
7. Run targeted tests and a production build
