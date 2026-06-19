# AeroDataBox Replacement Design

## Scope

This change replaces the existing Amadeus-backed remote flight source with AeroDataBox while preserving the current architecture:

Spring Boot -> CrawlService -> Python crawler -> MySQL -> frontend reads local database

The goal is minimal change. The system must continue to use the existing `flight` and `flight_price_snapshot` tables, keep frontend requests pointed at Spring Boot `/api/...` endpoints, and avoid exposing any RapidAPI credentials to the browser.

Phase 1 covers backend and crawler replacement only. Frontend UI changes are explicitly out of scope for this phase except for compatibility fixes that are required to keep the existing admin workflow operational.

## Non-Goals

- Do not create a new Java `AeroDataBoxService`.
- Do not move external API access from Python into Spring Boot.
- Do not change the primary query flow for `/api/flights`.
- Do not rename `flight` or `flight_price_snapshot`.
- Do not let Vue call RapidAPI directly.

## Current Architecture

The current backend does not call Amadeus directly. Spring Boot launches a Python crawler process through `CrawlService`. The crawler fetches remote flight data and writes normalized rows into MySQL through the existing pipeline.

This means the smallest safe replacement is:

- keep `CrawlService`
- keep the Scrapy-based crawler and pipeline
- replace the Amadeus client and spider implementation with AeroDataBox logic
- add a clearer sync endpoint in Spring Boot that still delegates to the same crawler path

## Phase 1 Deliverables

1. Keep `POST /api/admin/crawl-jobs` working.
2. Add `POST /api/admin/flights/sync?airportCode=CKG&date=2026-06-18`.
3. Normalize old `source=amadeus` usage to the new remote source `aerodatabox`.
4. Replace real Amadeus HTTP calls with AeroDataBox RapidAPI calls.
5. Read `AERODATABOX_KEY` from the environment only.
6. Split a single-date airport sync into two local-time windows:
   - `dateT00:00` to `dateT12:00`
   - `dateT12:00` to `dateT23:59`
7. Merge departures and arrivals, deduplicate, and persist to the existing database tables.
8. Generate local simulated pricing and seat counts when the remote response does not provide them.
9. Return clear error information when remote sync fails.

## API Design

### Existing Crawl Job Endpoint

`POST /api/admin/crawl-jobs`

This endpoint remains available for backward compatibility with the current admin flow.

Request body remains compatible with the current shape and is extended with:

- `airportCode` optional
- `date` optional

`source` compatibility rules:

- `aerodatabox` is the canonical value
- `amadeus` is accepted as a legacy alias and internally normalized to `aerodatabox`
- no path in the system will actually call Amadeus after this change

The airport sync flow uses:

```json
{
  "source": "aerodatabox",
  "airportCode": "CKG",
  "date": "2026-06-18"
}
```

### New Manual Sync Endpoint

`POST /api/admin/flights/sync?airportCode=CKG&date=2026-06-18`

This endpoint exists for direct manual sync and acceptance testing. It does not introduce a second sync implementation. It only validates request parameters, builds a normalized `CrawlRequest`, and delegates to the existing `CrawlService`.

Required query parameters:

- `airportCode`
- `date`

Behavior:

- normalize source to `aerodatabox`
- invoke the existing crawler process path
- return the resulting crawl job or a sync summary compatible with the current backend style

## Java Mapping Design

`CrawlRequest` is extended to support airport-based sync while keeping the existing route-based fields intact.

Fields retained:

- `source`
- `fromCity`
- `toCity`
- `date`
- `adults`
- `maxResults`

Field added:

- `airportCode`

Rules:

- legacy source `amadeus` is normalized to `aerodatabox`
- if `airportCode` is present, the crawler runs in airport sync mode
- `toCrawlerArguments()` passes `source`, `airport_code`, and `date` to the spider

The Java layer remains orchestration-only. No external HTTP logic is added there.

## Python Crawler Design

The crawler becomes AeroDataBox-backed while staying within the existing Scrapy structure.

### Inputs

Primary crawler inputs:

- `source`
- `airport_code`
- `date`

Legacy fields may remain accepted for compatibility but are not used in the new main sync path.

### AeroDataBox Request

Base URL:

- `https://aerodatabox.p.rapidapi.com`

Headers:

- `X-RapidAPI-Key: <from AERODATABOX_KEY>`
- `X-RapidAPI-Host: aerodatabox.p.rapidapi.com`

Path format:

- `/flights/airports/iata/{airportCode}/{fromLocal}/{toLocal}`

Query string:

- `withLeg=true`
- `direction=Both`
- `withCancelled=true`
- `withCodeshared=true`
- `withCargo=false`
- `withPrivate=false`
- `withLocation=false`

### Time Window Split

For a requested `date`, the crawler must always issue two requests:

1. `dateT00:00` to `dateT12:00`
2. `dateT12:00` to `dateT23:59`

The crawler merges the returned `departures` and `arrivals` arrays from both windows.

### Deduplication

Rows are deduplicated before yielding items into the pipeline.

Recommended dedupe key:

- flight number
- scheduled departure time
- departure airport
- arrival airport

This keeps the existing DB unique behavior stable and avoids obvious duplicate writes from overlapping response content.

## Data Mapping

The existing database schema is preserved.

### Target Table: `flight`

Mapped fields:

- `flight_no`
  - from remote flight number, with fallback to codeshare or call sign if needed
- `airline_name`
  - from airline name, fallback to airline code or `"Unknown Airline"`
- `from_city`
  - city name if available, otherwise airport IATA code
- `to_city`
  - city name if available, otherwise airport IATA code
- `from_airport`
  - departure airport IATA or airport display name
- `to_airport`
  - arrival airport IATA or airport display name
- `depart_time`
  - scheduled local departure time
- `arrive_time`
  - scheduled local arrival time
- `price`
  - simulated local price
- `seats_left`
  - simulated local remaining seats
- `data_source`
  - `aerodatabox`
- `collected_at`
  - current crawler timestamp

### Additional Remote Fields

These fields must be extracted in the crawler even though Phase 1 does not add new DB columns:

- terminal
- status
- aircraft model

They may be kept in the crawler item payload, logs, or future extension points, but schema expansion is not part of this phase.

### Target Table: `flight_price_snapshot`

The pipeline continues to create snapshots using the same normalized `price`, `seats_left`, route, departure time, and data source.

## Simulated Price and Seat Rules

Because AeroDataBox does not provide a complete sellable fare payload for this use case, the crawler generates local values.

Price:

- deterministic simulated economy-style display price
- range: 500 to 1500
- based on a stable hash of route, flight number, and departure time so repeated syncs do not fluctuate unnecessarily

Seats:

- deterministic simulated value
- reasonable range such as 3 to 30

This preserves the existing UI and AI recommendation logic, both of which expect a local `price` and `seats_left`.

## Error Handling

Remote failures must not crash the backend process silently.

Expected handling:

- missing `AERODATABOX_KEY` produces a clear configuration error
- remote non-200 responses include status details
- empty responses return a clear "no flights found" style message
- crawler failures propagate back through the existing crawl job error fields
- Spring Boot endpoints return clear error information instead of an unhandled server crash

## Files Expected to Change in Phase 1

Java:

- `backend/src/main/java/com/example/flight/crawl/CrawlRequest.java`
- `backend/src/main/java/com/example/flight/crawl/admin/AdminCrawlController.java`
- `backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusService.java`
- `backend/src/main/java/com/example/flight/crawl/admin/AdminFlightSyncController.java`
- `backend/src/main/resources/application.yml`

Python crawler:

- `crawler/flight_crawler/amadeus_client.py` or replacement file
- `crawler/flight_crawler/spiders/amadeus_flights.py` or replacement file
- `crawler/flight_crawler/pipelines.py` only if item compatibility requires it

Tests:

- targeted backend tests for request normalization and sync endpoint behavior
- targeted crawler tests if the existing test setup supports them

## Phase Boundary

Phase 1 ends when:

- backend starts successfully
- `AERODATABOX_KEY` can be supplied through environment configuration
- `POST /api/admin/flights/sync` triggers the crawler path
- synced rows land in `flight` and `flight_price_snapshot`
- `/api/flights` can read those rows from MySQL
- no code path still performs a real Amadeus request

Phase 1 does not require frontend UX changes beyond what is strictly necessary to avoid breaking existing compatibility.
