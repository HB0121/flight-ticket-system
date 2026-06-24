# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Identity

University course project — **Flight Ticket Scraping and Auto-Update System** (机票抓取与自动更新系统). A multi-language, three-tier system: Spring Boot backend, Vue 3 frontend, and Scrapy crawler, all sharing one MySQL database. Currently at Phase 1: a flight data collection and query system. The AI/chat module is retained in code but is not part of the core delivery.

## Build & Run

Requires Docker Desktop, Java 17, Maven, and Node.js.

```powershell
# Start MySQL
docker compose -f infra/docker-compose.yml up -d mysql

# Backend (port 8080)
cd backend
mvn spring-boot:run

# Frontend (port 5173)
cd frontend
npm.cmd install
npm.cmd run dev

# Crawler (one-off)
docker compose -f infra/docker-compose.yml run --rm crawler
```

Environment variables for real external APIs (system works without them but data will be empty):
- `AMADEUS_CLIENT_ID` / `AMADEUS_CLIENT_SECRET` — Amadeus Flight Offers API
- `AERODATABOX_KEY` — AeroDataBox RapidAPI key
- `DEEPSEEK_API_KEY` — DeepSeek AI (optional, falls back to local rule engine)

## Testing

```powershell
cd backend && mvn test                          # JUnit 5
cd frontend && npm.cmd test                     # Vitest + @vue/test-utils
cd crawler && pytest                            # pytest
```

Backend tests use H2 in-memory database (schema at `backend/src/test/resources/schema.sql`). Frontend tests are co-located with source files as `*.spec.js`.

## Architecture

### Shared-database pattern

All three components connect to the same MySQL database (`flight_demo`). The **crawler writes directly to MySQL** via pymysql in its Scrapy pipeline — it does not go through the backend API. The backend reads those tables and exposes them via REST.

### Backend (`com.example.flight`)

- Spring Boot 3.3.5, Java 17, Maven
- **No ORM** — raw JDBC (`spring-boot-starter-jdbc`) with hand-written SQL in `*Repository` classes. No JPA/Hibernate.
- **Custom token auth** — no Spring Security. `LoginInterceptor` extracts a bearer token from the `Authorization` header, resolves the user via `TokenRepository`. Tokens are stored in the `user_token` table.
- Config: `backend/src/main/resources/application.yml`. Schema is re-initialized from `infra/mysql/init.sql` on every startup via `DatabaseInitializer`.
- DDL autocommit mode: none.
- Feature packages: `auth/`, `flight/` (with `favorite/` and `history/` sub-packages), `crawl/` (with `admin/` sub-package), `ai/` (non-primary), `config/`.

### Frontend (`src/`)

- Vue 3 + Vite 5 + Element Plus + ECharts + vue-i18n
- Feature-module layout: `src/modules/<feature>/pages/` for page components
- API client layer at `src/api/` — one module per API area, wrapping axios
- Reusable components in `src/shared/` (`FlightTable`, `FlightDetailCard`, `PriceHistoryChart`)
- Vue Router with auth guards in `src/router/index.js` — unauthenticated users redirect to `/auth`
- Two layout wrappers: `UserLayout.vue` and `AdminLayout.vue`
- i18n: `zh-CN` (default) and `en-US` in `src/i18n/messages/`

### Crawler (`flight_crawler/`)

- Scrapy 2.13, Python 3.11
- Three spiders: `aerodatabox_flights`, `amadeus_flights`, `live_flights`
- API clients: `aerodatabox_client.py`, `amadeus_client.py`
- MySQL pipeline writes directly to the database; connection params from env vars with defaults matching docker-compose
- Runs containerized via Docker; the backend triggers it by shelling out to `docker compose run`
- `sample_pages/flights.html` — local HTML for demo/fallback when external APIs are unavailable

### Database (`infra/mysql/init.sql`)

8 tables: `flight`, `crawl_job`, `flight_price_snapshot`, `flight_validation_failure`, `price_context`, `conversation_session`, `conversation_message`, `app_user`, `user_token`. The `flight` table has a unique constraint on `(flight_no, depart_time, data_source)` — duplicate crawls update price/seats rather than inserting new rows.

## No Linting or CI

There are no ESLint, Prettier, flake8, or checkstyle configs. No CI/CD pipeline is configured.
