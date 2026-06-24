# Task 2 Report

## Status: DONE

## What was changed

1. **`Flight.java`** — Added two new fields to the record parameter list:
   - `@JsonInclude(JsonInclude.Include.NON_NULL) Boolean isFavorited`
   - `@JsonInclude(JsonInclude.Include.NON_NULL) Long favoriteId`
   - Added import `com.fasterxml.jackson.annotation.JsonInclude`

2. **`FlightRepository.java`** — Updated the `rowMapper` to map the two new fields using safe getter helpers (`getBooleanOrNull`, `getLongOrNull`) that return null when the column is absent. Added the helper methods and necessary imports (`java.sql.ResultSet`, `java.sql.SQLException`).

3. **`TimingServiceTest.java`** — Updated the `Flight` constructor call to pass `null, null` for the two new trailing parameters.

4. **`AdviceServiceTest.java`** — Updated the `Flight` constructor call to pass `null, null` for the two new trailing parameters.

## Test results

- **Compilation**: Both `mvn compile` and `mvn test-compile` pass with zero errors.
- **Runtime tests**: Both `FlightRepositoryTest` and `FavoriteRepositoryTest` fail at runtime due to a **pre-existing Mockito/bytebuddy JDK compatibility issue** (`Mockito is unable to load the default implementation of class that is a part of Mockito distribution`). This failure is unrelated to the Flight record changes — it occurs in Mockito's static initializer before any test method runs.

## Concerns

- The Mockito/bytebuddy incompatibility with the current JDK (17.0.19) prevents runtime test execution. This needs a separate fix (likely upgrading mockito-core or bytebuddy dependency).
- The `FlightRepository` currently uses `SELECT *` from the `flight` table, which does not include `is_favorited` or `favorite_id` columns. The safe getter methods return `null` for these columns, which matches the `@JsonInclude(NON_NULL)` semantics. A future task (per the design spec) will update the search query to LEFT JOIN with the `favorite` table and include these columns in the result set.
