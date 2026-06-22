# Backend API Contract

This document freezes the current backend API surface for the first phase of
module-boundary cleanup. Paths, methods, request shapes, and response JSON
structures must stay compatible while controller logic is moved into services.

## Auth

- `POST /api/auth/register`
  - Body: `username`, `password`
  - Response: `id`, `username`, `nickname`, `token`
- `POST /api/auth/login`
  - Body: `username`, `password`
  - Response: `id`, `username`, `nickname`, `token`
- `POST /api/auth/logout`
  - Header: optional `Authorization: Bearer {token}`
  - Response: `204 No Content`
- `GET /api/auth/me`
  - Response: `id`, `username`, `nickname`

## Flights

- `GET /api/flights`
  - Query: optional `fromCity`, `toCity`, `date`, `dataSource`
  - Response: array of `Flight`
  - Side effect: records search history only after a successful non-empty search.
- `GET /api/flights/{id}`
  - Response: `Flight`, or `404` when missing.
- `GET /api/flights/{id}/price-history`
  - Response: array of `FlightPriceSnapshot`, or `404` when the flight is missing.

## Crawl And Admin

- `POST /api/crawl/run`
  - Body: `CrawlRequest`
  - Response: `CrawlJob`
- `GET /api/crawl/latest`
  - Response: latest `CrawlJob`
- `POST /api/admin/crawl-jobs`
  - Body: `source`, optional route/date/adult/result/airport fields.
  - Response: `CrawlJob`
  - Compatibility: `amadeus` remains accepted as an alias for `aerodatabox`.
- `GET /api/admin/crawl-jobs`
  - Response: recent `CrawlJob` array, currently limited to 20.
- `POST /api/admin/flights/sync`
  - Query: `airportCode`, `date`
  - Response: `CrawlJob`
- `GET /api/admin/data-sources/status`
  - Response: data source status array.

## Current User Resources

- `GET /api/me/favorites`
  - Response: array of `FavoriteRecord`.
- `POST /api/me/favorites`
  - Body: `flightId`
  - Response: created or existing `FavoriteRecord`.
- `DELETE /api/me/favorites/{favoriteId}`
  - Response: `204 No Content`.
- `GET /api/me/search-history`
  - Response: array of `SearchHistoryRecord`.

## AI

- `POST /api/ai/advice`
  - Body: `message`, optional `query`
  - Response: `AdviceResponse`
- `POST /api/ai/timing`
  - Body: `message`
  - Response: `TimingResponse`
- `POST /api/ai/conversations`
  - Body: optional `title`
  - Response: `ConversationSession`
- `GET /api/ai/conversations`
  - Response: recent conversation sessions.
- `GET /api/ai/conversations/{sessionId}/messages`
  - Response: array of `ConversationMessage`.
- `POST /api/ai/conversations/{sessionId}/messages`
  - Body: `message`
  - Response: `AdviceResponse`
- `DELETE /api/ai/conversations/{sessionId}`
  - Response: `204 No Content`.
