# Backend Module Boundaries

The backend remains a single Maven module in this phase. The cleanup goal is to
make package responsibilities explicit without moving repositories or changing
API behavior.

## Package Responsibilities

- `com.example.flight.auth`
  - Owns registration, login, logout, token validation, and current-user lookup.
  - Controllers should not manually assemble new business workflows.
- `com.example.flight.flight`
  - Owns public flight search, flight detail, price history, and search-history
    side effects triggered by flight search.
  - `FlightService` is the service boundary for controller-facing flight reads.
- `com.example.flight.flight.favorite`
  - Owns current-user favorite list, create, and delete operations.
  - `FavoriteService` shields the controller from repository calls.
- `com.example.flight.flight.history`
  - Owns search-history persistence and listing.
- `com.example.flight.crawl`
  - Owns crawler execution and airport/date sync orchestration.
- `com.example.flight.crawl.admin`
  - Owns admin-facing crawl job creation, source normalization, source
    configuration checks, recent job listing, and data-source status reporting.
- `com.example.flight.ai`
  - Owns travel advice, timing analysis, and conversation workflows.
- `com.example.flight.config`
  - Owns web, database initialization, and exception handling infrastructure.

## First-Phase Rules

- Keep all existing URL paths unchanged.
- Keep existing JSON field names unchanged.
- Keep repositories in their current packages.
- Do not introduce mapper packages in this phase.
- Do not modify database schema or configuration files.
- Controllers should validate transport input and delegate to services.
- Services should own business branching, repository orchestration, and side
  effects.
