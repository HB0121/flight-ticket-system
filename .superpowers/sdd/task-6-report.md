# Task 6 Report — Add Favorite Column to FlightTable

## Status: COMPLETE

## What Changed

Modified `frontend/src/shared/components/FlightTable.vue`:

1. **Import**: Added `import FavoriteButton from './FavoriteButton.vue'`
2. **Props**: Added `favoriteStatusMap: { type: Map, default: () => new Map() }` to `defineProps`
3. **Emits**: Changed `defineEmits(['select'])` to `const emit = defineEmits(['select', 'favorite-toggled'])`
4. **Helper functions**: Added `favoriteState(flightId)` and `onFavoriteToggled(flightId, isFavorited, favoriteId)`
5. **Template**: Added a new `<el-table-column>` for favorites (width 54, centered) after the status column, rendering `<FavoriteButton>` for each row

## Build Result

PASS — `npx vite build --mode development` completed successfully (built in 7.64s). No errors.

## Interfaces Produced (for Task 7-8 consumers)

- `<FlightTable :favorite-status-map="map" @favorite-toggled="handler" />`
- `favoriteStatusMap`: `Map<flightId, { isFavorited: boolean, favoriteId: number|null }>`
- `favorite-toggled` payload: `{ flightId, isFavorited, favoriteId }`

## Concerns

None. The FavoriteButton already has `@click.stop` which prevents row-click event bubbling from the favorite column.
