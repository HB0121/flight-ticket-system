# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

综合课程设计 III 选题 3.1.7: Flight ticket scraping and auto-update system. Three-layer architecture — Python crawler, Java SpringBoot backend, Vue 3 frontend — connected via MySQL. Designed for local classroom demonstrations.

## Quick Start

```powershell
# Optional: set API keys for real data (system works offline with sample fallback)
$env:AMADEUS_CLIENT_ID="your-amadeus-client-id"
$env:AMADEUS_CLIENT_SECRET="your-amadeus-client-secret"
$env:DEEPSEEK_API_KEY="your-deepseek-api-key"

# 1. Start MySQL
docker compose -f infra/docker-compose.yml up -d mysql

# 2. Start backend (port 8080)
cd backend
mvn spring-boot:run

# 3. Start frontend (port 5173)
cd ../frontend
npm install
npm run dev

# 4. Trigger crawler manually
docker compose -f infra/docker-compose.yml run --rm crawler
```

## Architecture

### Three layers, one database

```
crawler/ (Python Scrapy + Amadeus) ──writes──►  MySQL  ◄──reads── backend/ (SpringBoot) ◄──http── frontend/ (Vue 3)
```

- **crawler** writes flight records, price snapshots, and crawl_job tracking into MySQL.
- **backend** reads/writes the same MySQL for flight queries, price history, AI endpoints, and triggers the crawler as a subprocess.
- **frontend** is a pure SPA that talks only to the backend REST API.

### Graceful degradation (no external API required)

Every optional dependency has a local fallback:

| Dependency | When configured | When missing |
|---|---|---|
| Amadeus API credentials | Real-time flight offers via Amadeus Flight Offers Search | Falls back to static sample HTML flights |
| DeepSeek API key | AI-generated travel advice and timing reports | Falls back to local rule engine (sort by price, trend analysis on price snapshots) |

The system is fully functional and demonstrable with zero external API keys.

### Port interfaces for fallback chains

The AI layer uses an `AiTextClient` functional interface ([AiTextClient.java](backend/src/main/java/com/example/flight/ai/AiTextClient.java)) with two constructors — one taking the real DeepSeek client, one without. If `DEEPSEEK_API_KEY` is empty, `DeepSeekTextClient.generate()` returns `Optional.empty()`, and `AdviceService`/`TimingService` fall back to local text generation.

Similarly, `FlightSearchPort` and `PriceHistoryPort` interfaces ([FlightSearchPort.java](backend/src/main/java/com/example/flight/flight/FlightSearchPort.java), [PriceHistoryPort.java](backend/src/main/java/com/example/flight/flight/PriceHistoryPort.java)) are both implemented by `FlightRepository` for simple JDBC access, keeping the service layer decoupled from the data layer.

### Schema management

Schema is defined in three coordinated places — keep them in sync:

1. [infra/mysql/init.sql](infra/mysql/init.sql) — fresh MySQL container initialization (CREATE TABLE)
2. [DatabaseInitializer.java](backend/src/main/java/com/example/flight/config/DatabaseInitializer.java) — backend startup schema migration (ALTER + CREATE IF NOT EXISTS)
3. [MysqlPipeline._ensure_schema()](crawler/flight_crawler/pipelines.py) — crawler-side schema migration (ALTER + CREATE IF NOT EXISTS)

Test schema for H2 lives in [schema.sql](backend/src/test/resources/schema.sql).

### Crawler execution model

The backend spawns the crawler as a child process via `CrawlService` ([CrawlService.java](backend/src/main/java/com/example/flight/crawl/CrawlService.java)), executing `docker compose run --rm crawler` with configurable timeout. This means the backend's working directory must be where it can resolve the docker-compose path. The crawler Dockerfile defaults to `scrapy crawl sample_flights`, but the `POST /api/crawl/run` endpoint passes arguments to select `amadeus_flights` spider with route/date parameters.

Crawler pipeline ([pipelines.py](crawler/flight_crawler/pipelines.py)) does three things per item: upserts into `flight` (on duplicate key update), inserts into `flight_price_snapshot`, and tracks success/failure counts in `crawl_job`.

## Key source files

### Backend (Java 17, SpringBoot 3.3.5)

| File | Role |
|---|---|
| [FlightController.java](backend/src/main/java/com/example/flight/flight/FlightController.java) | `GET /api/flights` search with optional `fromCity`, `toCity`, `date`, `dataSource` params; `GET /api/flights/{id}`; `GET /api/flights/{id}/price-history` |
| [FlightRepository.java](backend/src/main/java/com/example/flight/flight/FlightRepository.java) | JDBC-based repository; builds dynamic SQL with StringBuilder for search, implements both `FlightSearchPort` and `PriceHistoryPort` |
| [AdviceController.java](backend/src/main/java/com/example/flight/ai/AdviceController.java) | `POST /api/ai/advice` and `POST /api/ai/timing` |
| [AdviceService.java](backend/src/main/java/com/example/flight/ai/AdviceService.java) | Parses natural language with `TravelIntentParser`, queries flights, picks cheapest match, optionally calls AI for summary |
| [TimingService.java](backend/src/main/java/com/example/flight/ai/TimingService.java) | Queries flights + price history, runs local trend analysis (comparing first/last snapshot price), optionally enriches with AI |
| [TravelIntentParser.java](backend/src/main/java/com/example/flight/ai/TravelIntentParser.java) | Regex-based NLU: extracts known Chinese city names, ISO date pattern, and budget from user message |
| [DeepSeekTextClient.java](backend/src/main/java/com/example/flight/ai/DeepSeekTextClient.java) | Calls DeepSeek Chat Completions API via `RestClient`; returns `Optional.empty()` on any failure or missing key |
| [CrawlController.java](backend/src/main/java/com/example/flight/crawl/CrawlController.java) | `POST /api/crawl/run` and `GET /api/crawl/latest` |
| [CrawlService.java](backend/src/main/java/com/example/flight/crawl/CrawlService.java) | Spawns crawler as OS subprocess (shell command), handles timeout/error capture |
| [WebConfig.java](backend/src/main/java/com/example/flight/config/WebConfig.java) | CORS: allows localhost:5173 origins on `/api/**` |
| [application.yml](backend/src/main/resources/application.yml) | MySQL connection, crawler command path, DeepSeek config (all env-var driven) |

### Crawler (Python 3.11, Scrapy 2.13+)

| File | Role |
|---|---|
| [amadeus_flights.py](crawler/flight_crawler/spiders/amadeus_flights.py) | Reads args `from_city/to_city/date/adults/max_results`, calls Amadeus API, falls back to sample HTML on failure |
| [sample_flights.py](crawler/flight_crawler/spiders/sample_flights.py) | Parses local `sample_pages/flights.html` via `scrapy.Request` with `file://` URI |
| [amadeus_client.py](crawler/flight_crawler/amadeus_client.py) | OAuth2 token fetch, Flight Offers Search API call, response normalization with city IATA + airport name dictionaries |
| [parser.py](crawler/flight_crawler/parser.py) | BeautifulSoup parser for sample HTML — selects `.flight-card` elements, extracts typed fields |
| [pipelines.py](crawler/flight_crawler/pipelines.py) | MySQL pipeline: creates crawl_job on open, upserts flight + inserts snapshot per item, updates crawl_job on close |
| [settings.py](crawler/flight_crawler/settings.py) | Enables MysqlPipeline, disables robots.txt obedience, uses asyncio reactor |
| [Dockerfile](crawler/Dockerfile) | Python 3.11-slim, installs requirements, default CMD `scrapy crawl sample_flights` |

### Frontend (Vue 3, Vite, Element Plus, ECharts)

| File | Role |
|---|---|
| [App.vue](frontend/src/App.vue) | Single-file SPA: three views (dashboard/flights/AI), collection config, flight table with detail panel, price charts, AI advice/timing sections |
| [client.js](frontend/src/api/client.js) | Axios wrapper: all 7 API endpoints, base URL from `VITE_API_BASE_URL` env var |
| [format.js](frontend/src/lib/format.js) | Chart options (bar for prices, line for history), date formatter, crawler payload builder, response text formatters |
| [main.js](frontend/src/main.js) | App bootstrap: creates Vue app, installs ElementPlus |

## Commands

```bash
# Backend
cd backend && mvn spring-boot:run        # Start (port 8080)
cd backend && mvn test                   # Run all tests (JUnit5 + H2)
cd backend && mvn test -Dtest=FlightRepositoryTest  # Run single test class

# Frontend
cd frontend && npm run dev               # Start dev server (port 5173)
cd frontend && npm run build             # Production build
cd frontend && npm test                  # Run Vitest tests

# Crawler (via Docker)
docker compose -f infra/docker-compose.yml run --rm crawler                # Default: sample_flights
docker compose -f infra/docker-compose.yml run --rm crawler \
  scrapy crawl amadeus_flights -a from_city=上海 -a to_city=北京            # Amadeus real data

# Crawler (local Python, for debugging)
cd crawler && pip install -r requirements.txt
cd crawler && scrapy crawl sample_flights
cd crawler && pytest                     # Run parser/client tests

# Infrastructure
docker compose -f infra/docker-compose.yml up -d mysql    # Start MySQL
docker compose -f infra/docker-compose.yml down           # Stop everything
```

## Data model

Three MySQL tables, all InnoDB/utf8mb4:

- **flight** — current flight snapshot. Unique key on `(flight_no, depart_time, data_source)`. Upserted on each crawl.
- **flight_price_snapshot** — append-only price history. One row per crawl per flight. Used for trend charts and timing analysis.
- **crawl_job** — audit log. Each crawl creates one row (RUNNING → SUCCESS/FAILED), tracks `source`, `request_params`, success/failure counts.

The `Flight` Java type is a [Java record](backend/src/main/java/com/example/flight/flight/Flight.java) — immutable, with `BigDecimal` for price.

## Testing

- **Backend**: `@JdbcTest` with H2 in-memory database. Tests use `@Import(FlightRepository.class)` to slice-test the repository. Insert fixtures via JdbcTemplate, then exercise search/pricing queries.
- **Crawler**: pytest tests for parser (sample HTML → dict) and amadeus_client (API response normalization, credential detection).
- **Frontend**: Vitest config in package.json (`vitest` v4), tests in `src/lib/format.spec.js`.

## Design constraints

- No Redis, no FAISS, no WeChat mini-program, no public deployment in v2. These are reserved for later phases.
- All external API calls (Amadeus, DeepSeek) must fail gracefully with local fallbacks — never crash the system.
- Crawler is one-shot (not daemon) — triggered manually or via API, runs inside a Docker container, exits when done.
- Chinese city names are the domain language: user input and API params use Chinese city names (上海, 北京), which are mapped to IATA codes (SHA, BJS) only at the Amadeus client boundary.
