# Task 3 Report — FlightRepository: Add Favorite-Aware Search

**Status:** DONE_WITH_CONCERNS

## What Was Changed

### File Modified
`backend/src/main/java/com/example/flight/flight/FlightRepository.java`

### Changes Made

1. **Added `rowMapperWithFavorite` field** (after `snapshotRowMapper`): A new `RowMapper<Flight>` that maps all 15 Flight record fields, using `getBooleanOrNull` and `getLongOrNull` helper methods for the `is_favorited` and `favorite_id` columns. This mirrors the existing `rowMapper` but is semantically dedicated to the LEFT JOIN query.

2. **Added `search(FlightSearchCriteria criteria, Long userId)` overload**: A new public method that:
   - Uses `SELECT f.*, fav.id AS favorite_id, CASE WHEN fav.id IS NOT NULL THEN true ELSE false END AS is_favorited`
   - Uses `FROM flight f LEFT JOIN favorite fav ON fav.flight_id = f.id AND fav.user_id = ?`
   - All WHERE clauses use table alias `f.` (e.g., `f.from_city`, `f.price`)
   - Uses `rowMapperWithFavorite` for result mapping
   - Dynamic WHERE clause construction mirrors the existing `search(FlightSearchCriteria)` method

3. **Existing code preserved**:
   - `rowMapper` field **not** reverted (needs 15 args to compile with the updated Flight record from Task 2; the `getBooleanOrNull`/`getLongOrNull` helpers gracefully return null when columns are missing from `SELECT * FROM flight`)
   - `search(FlightSearchCriteria criteria)` method completely unchanged (same `SELECT * FROM flight` query, still uses original `rowMapper`)
   - `FlightSearchPort` interface unchanged
   - Helper methods `getBooleanOrNull` and `getLongOrNull` already existed (added earlier, reused)
   - `import java.sql.ResultSet` and `import java.sql.SQLException` already existed

## Test Results

**Compilation:** `mvn compile test-compile` — BUILD SUCCESS. No compilation errors.

**Test execution:** `mvn test -Dtest="FlightRepositoryTest,FavoriteRepositoryTest"` — BUILD FAILURE, but **all 7 failures are due to a single pre-existing environment issue**: `java.lang.IllegalStateException: Could not initialize plugin: interface org.mockito.plugins.MockMaker (alternate: null)`.

This error occurs in `ResetMocksTestExecutionListener.beforeTestMethod()`, which is Spring Boot's test infrastructure (not the test assertions themselves). It happens before any test method body executes. The root cause is that Mockito's byte-buddy agent cannot self-attach on this JVM (Java 17.0.19 on Windows 11). Neither `mock-maker-inline` system property nor `mockito-extensions` configuration file resolved the issue.

**All 7 test errors (0 assertions failed):**
```
FlightRepositoryTest.searchesFlightsByRouteAndDateOrderedByPrice    ERROR (MockMaker)
FlightRepositoryTest.searchesFlightsByIataAirportCodesAndDate       ERROR (MockMaker)
FlightRepositoryTest.searchesFlightsByDataSourceWhenProvided        ERROR (MockMaker)
FlightRepositoryTest.returnsPriceHistoryForFlightOrderedByObservationTime ERROR (MockMaker)
FavoriteRepositoryTest.createsListsAndDeletesFavoritesByUser       ERROR (MockMaker)
FavoriteRepositoryTest.rejectsUnknownFlightBeforeInsert             ERROR (MockMaker)
FavoriteRepositoryTest.ignoresDuplicateFavoriteForSameUserAndFlight ERROR (MockMaker)
```

## Concerns

1. **Mockito MockMaker environment issue**: Tests cannot run in this environment due to a Mockito/byte-buddy compatibility issue with the JVM. This is pre-existing and not caused by the Task 3 changes. The code compiles cleanly and follows the plan exactly. The tests should pass once the Mockito issue is resolved (likely by adding `-javaagent` pointing to byte-buddy-agent.jar, or upgrading mockito-core, or switching to a JDK without attach API restrictions).

2. **Existing `rowMapper` modified from Task 2**: The plan states the existing `rowMapper` must stay unchanged, but after Task 2's Flight record gained `isFavorited` and `favoriteId` fields, the `rowMapper` constructor call must provide 15 arguments to compile. The current approach (using the defensive helpers) is correct and safe — when columns are absent from `SELECT * FROM flight`, the helpers catch SQLException and return null.
