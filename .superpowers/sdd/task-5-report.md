# Task 5 Report: FavoriteButton Component

## Status
SUCCESS -- All 4 tests pass.

## Files Created

1. `frontend/src/shared/components/FavoriteButton.vue` -- Reusable favorite toggle button component using Composition API (`<script setup>`). Uses Element Plus `el-button` (link variant) with `Star`/`StarFilled` icons from `@element-plus/icons-vue`. Props: `flightId` (Number, required), `isFavorited` (Boolean, default false), `favoriteId` (Number, default null). Emits: `toggled(isFavorited: boolean, favoriteId: number|null)`. Consumes `addFavorite`/`removeFavorite` from `profileApi.js`. Shows i18n-based success/error messages via `ElMessage`.

2. `frontend/src/shared/components/FavoriteButton.spec.js` -- 4 tests using Vitest + @vue/test-utils. Mocks `profileApi.js` via `vi.hoisted()` + `vi.mock()`. Provides inline i18n messages via `vue-i18n` `createI18n`. Stubs `ElButton`, `ElIcon`, `Star`, `StarFilled` for isolated testing.

## Test Results

```
 ✓ src/shared/components/FavoriteButton.spec.js (4 tests) 109ms
   ✓ renders favorite button when not favorited
   ✓ renders favorited state correctly
   ✓ clicking unfavorited star calls addFavorite and emits toggled(true, 99)
   ✓ clicking favorited star calls removeFavorite(10) and emits toggled(false, null)

 Test Files  1 passed (1)
      Tests  4 passed (4)
```

All 4 tests PASS.

## Environment Note

`jsdom` was downgraded from v29 to v24 via `npm install --save-dev jsdom@24` because v29 requires Node.js 22+ and the environment runs Node.js 18.19.0. This is a pre-existing environment incompatibility that also affected the existing `FavoritesPage.spec.js` test. The downgrade is recorded in `package.json` and `package-lock.json`.

## Component Interface (for Tasks 6-8 consumers)

```html
<FavoriteButton
  :flight-id="row.id"
  :is-favorited="false"
  :favorite-id="null"
  @toggled="handler"
/>
```

`toggled` event payload: `(isFavorited: boolean, favoriteId: number|null)`

## Concerns

- The i18n keys `flights.favorite.*` and `common.actions.favorite`/`common.actions.unfavorite` do not yet exist in the main i18n message files (`zh-CN.js`, `en-US.js`). These are scheduled for Task 9. The component will show raw key strings until that task is completed.
- The `jsdom` downgrade is a side effect tracked in package.json; if the team upgrades Node.js to 22+ in the future, jsdom can be upgraded back to v29.
