# AeroDataBox Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the existing Amadeus-backed remote crawler path with AeroDataBox while preserving the current Spring Boot -> crawler -> MySQL flow and adding a direct sync endpoint.

**Architecture:** Spring Boot remains an orchestration layer only. Java normalizes sync requests and invokes the existing Python crawler path. The Python crawler becomes AeroDataBox-backed, splits one-day airport syncs into two 12-hour requests, deduplicates merged results, generates stable simulated price/seats, and persists rows through the existing pipeline.

**Tech Stack:** Spring Boot, JUnit 5, Scrapy, Python requests, MySQL, PowerShell

---

### Task 1: Extend backend request normalization and sync endpoints

**Files:**
- Modify: `backend/src/main/java/com/example/flight/crawl/CrawlRequest.java`
- Modify: `backend/src/main/java/com/example/flight/crawl/admin/AdminCrawlController.java`
- Create: `backend/src/main/java/com/example/flight/crawl/admin/AdminFlightSyncController.java`
- Test: `backend/src/test/java/com/example/flight/crawl/CrawlRequestTest.java`
- Test: `backend/src/test/java/com/example/flight/crawl/admin/AdminCrawlControllerTest.java`
- Test: `backend/src/test/java/com/example/flight/crawl/admin/AdminFlightSyncControllerTest.java`

- [ ] **Step 1: Write failing backend tests for source normalization and airport sync**

Add tests that assert:
- `source=amadeus` normalizes to `aerodatabox`
- airport sync requests build crawler args for `aerodatabox_flights`
- `POST /api/admin/crawl-jobs` accepts `airportCode` and delegates to `CrawlService`
- `POST /api/admin/flights/sync` requires `airportCode` and `date`

- [ ] **Step 2: Run backend crawl/admin tests to verify failures**

Run: `.\mvnw.cmd -q -Dtest=CrawlRequestTest,AdminCrawlControllerTest,AdminFlightSyncControllerTest test`
Expected: FAIL because airport sync fields and controller do not exist yet.

- [ ] **Step 3: Implement minimal Java request and controller changes**

Update `CrawlRequest` to add `airportCode`, normalize `amadeus` to `aerodatabox`, route airport-mode requests to `scrapy crawl aerodatabox_flights`, and preserve existing live source handling. Update `AdminCrawlController` to accept `airportCode` in the request body and allow `aerodatabox` plus the legacy alias. Add `AdminFlightSyncController` that builds a `CrawlRequest("aerodatabox", null, null, date, null, null, airportCode)` and delegates to `CrawlService`.

- [ ] **Step 4: Run backend crawl/admin tests to verify they pass**

Run: `.\mvnw.cmd -q -Dtest=CrawlRequestTest,AdminCrawlControllerTest,AdminFlightSyncControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add backend/src/main/java/com/example/flight/crawl/CrawlRequest.java backend/src/main/java/com/example/flight/crawl/admin/AdminCrawlController.java backend/src/main/java/com/example/flight/crawl/admin/AdminFlightSyncController.java backend/src/test/java/com/example/flight/crawl/CrawlRequestTest.java backend/src/test/java/com/example/flight/crawl/admin/AdminCrawlControllerTest.java backend/src/test/java/com/example/flight/crawl/admin/AdminFlightSyncControllerTest.java
git commit -m "feat: add AeroDataBox sync request flow"
```

### Task 2: Replace Amadeus configuration and status reporting

**Files:**
- Modify: `backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusService.java`
- Modify: `backend/src/main/resources/application.yml`
- Test: `backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusServiceTest.java`
- Test: `backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusControllerTest.java`

- [ ] **Step 1: Write failing status/config tests**

Add tests that assert:
- configured source code is `aerodatabox`
- configuration is based on crawler command plus `AERODATABOX_KEY`
- returned detail text mentions AeroDataBox instead of Amadeus

- [ ] **Step 2: Run status tests to verify failures**

Run: `.\mvnw.cmd -q -Dtest=DataSourceStatusServiceTest,DataSourceStatusControllerTest test`
Expected: FAIL because the service still reports Amadeus.

- [ ] **Step 3: Implement status/config updates**

Update `DataSourceStatusService` to read `AERODATABOX_KEY`, report code/label `aerodatabox`/`AeroDataBox`, and mark configured only when crawler command and key exist. Add non-secret config placeholders in `application.yml` if needed for base URL/host defaults, but keep the API key in environment only.

- [ ] **Step 4: Run status tests to verify they pass**

Run: `.\mvnw.cmd -q -Dtest=DataSourceStatusServiceTest,DataSourceStatusControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusService.java backend/src/main/resources/application.yml backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusServiceTest.java backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusControllerTest.java
git commit -m "feat: switch data source status to AeroDataBox"
```

### Task 3: Replace crawler remote client and spider with AeroDataBox

**Files:**
- Create: `crawler/flight_crawler/aerodatabox_client.py`
- Create: `crawler/flight_crawler/spiders/aerodatabox_flights.py`
- Modify: `crawler/tests/test_parser.py`
- Optional compatibility wrapper: `crawler/flight_crawler/amadeus_client.py`
- Optional compatibility wrapper: `crawler/flight_crawler/spiders/amadeus_flights.py`

- [ ] **Step 1: Write failing crawler normalization tests**

Add tests that assert:
- one-day airport sync expands into two AeroDataBox windows
- departures and arrivals map route direction correctly
- dedupe key is `flight_no + depart_time + from_airport + to_airport`
- simulated prices/seats are stable across repeated normalization
- empty remote arrays normalize to an empty list instead of raising

- [ ] **Step 2: Run crawler tests to verify failures**

Run: `python -m pytest crawler/tests/test_parser.py -q`
Expected: FAIL because AeroDataBox helpers do not exist yet.

- [ ] **Step 3: Implement AeroDataBox client and spider**

Create `aerodatabox_client.py` with:
- env var lookup for `AERODATABOX_KEY`
- base URL and host constants
- request window builder for `00:00-12:00` and `12:00-23:59`
- remote GET helper with RapidAPI headers
- normalization for departures and arrivals
- deterministic price/seat generation
- dedupe helper

Create `aerodatabox_flights.py` with:
- spider args `source`, `airport_code`, `date`
- request summary string for crawl jobs
- remote fetch + normalize + empty-result handling
- error propagation with readable messages

If compatibility is needed, keep `amadeus_flights.py` as a thin wrapper or leave it unused once Java points to the new spider.

- [ ] **Step 4: Run crawler tests to verify they pass**

Run: `python -m pytest crawler/tests/test_parser.py -q`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add crawler/flight_crawler/aerodatabox_client.py crawler/flight_crawler/spiders/aerodatabox_flights.py crawler/tests/test_parser.py
git commit -m "feat: replace Amadeus crawler with AeroDataBox"
```

### Task 4: Preserve pipeline behavior and verify end-to-end sync reporting

**Files:**
- Modify: `crawler/flight_crawler/pipelines.py` only if item compatibility or empty-result job handling requires it
- Modify: `crawler/Dockerfile` if the default spider entry point must change
- Test: `backend/src/test/java/com/example/flight/crawl/CrawlServiceTest.java`

- [ ] **Step 1: Add failing test or assertion coverage for sync result handling**

Add or adjust tests to verify:
- crawler errors surface into job failure reporting
- empty flight lists can still finish as success when the spider decides that no flights were found

- [ ] **Step 2: Run focused tests to verify failures**

Run: `.\mvnw.cmd -q -Dtest=CrawlServiceTest test`
Expected: FAIL only if the new sync behavior changes current assumptions.

- [ ] **Step 3: Implement minimal pipeline or service adjustments**

Only change `pipelines.py` or `CrawlService` if required to preserve:
- `crawl_job.error_message` for remote failures
- `SUCCESS` when no rows are returned but the sync completed normally
- existing `flight` and `flight_price_snapshot` writes for non-empty results

- [ ] **Step 4: Run focused tests to verify they pass**

Run: `.\mvnw.cmd -q -Dtest=CrawlServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

Run:
```bash
git add crawler/flight_crawler/pipelines.py crawler/Dockerfile backend/src/test/java/com/example/flight/crawl/CrawlServiceTest.java
git commit -m "fix: preserve crawl job reporting for AeroDataBox sync"
```

### Task 5: Verify Phase 1 manually and document operator steps

**Files:**
- Modify: `docs/superpowers/specs/2026-06-18-aerodatabox-design.md` only if implementation reality requires a small correction

- [ ] **Step 1: Run backend test suite for touched areas**

Run: `.\mvnw.cmd -q -Dtest=CrawlRequestTest,AdminCrawlControllerTest,AdminFlightSyncControllerTest,DataSourceStatusServiceTest,DataSourceStatusControllerTest,CrawlServiceTest test`
Expected: PASS

- [ ] **Step 2: Run crawler tests**

Run: `python -m pytest crawler/tests/test_parser.py -q`
Expected: PASS

- [ ] **Step 3: Start backend and execute one manual sync**

Run backend with `AERODATABOX_KEY` set, then call:
```bash
curl -X POST "http://localhost:8080/api/admin/flights/sync?airportCode=CKG&date=2026-06-18"
```
Expected: HTTP 200 with a crawl job or success payload. Empty flights is acceptable if the response indicates sync completed without results.

- [ ] **Step 4: Verify MySQL persistence**

Run:
```sql
select id, flight_no, from_airport, to_airport, depart_time, price, data_source
from flight
where data_source = 'aerodatabox'
order by id desc
limit 20;

select id, flight_no, depart_time, price, seats_left, data_source, observed_at
from flight_price_snapshot
where data_source = 'aerodatabox'
order by id desc
limit 20;
```
Expected: synced rows and snapshots are present when the remote API returns flights.

- [ ] **Step 5: Commit**

Run:
```bash
git add .
git commit -m "test: verify AeroDataBox phase 1 sync flow"
```
