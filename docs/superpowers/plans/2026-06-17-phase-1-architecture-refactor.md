# Phase 1 Architecture Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reshape the current demo-style flight system into a formal role-based architecture by separating user and admin flows, introducing a routed frontend shell, and moving crawl management behind admin APIs without changing the overall crawler execution model.

**Architecture:** Keep the project as a three-part system (`frontend`, `backend`, `crawler`) but reassign responsibilities. The frontend becomes a routed app with distinct user/admin modules, the backend becomes the single business boundary for auth, search, and crawl administration, and the crawler remains a task executor that only receives crawl inputs and writes normalized results.

**Tech Stack:** Vue 3, Vite, Element Plus, Axios, Spring Boot 3, Java 17, JDBC, MySQL, Scrapy/Python

---

## File Structure

### Frontend files
- Create: `frontend/src/router/index.js`  
  Responsibility: declare route table, route metadata, and auth/role guards.
- Create: `frontend/src/layouts/UserLayout.vue`  
  Responsibility: user-side shell for flight search, favorites, and history pages.
- Create: `frontend/src/layouts/AdminLayout.vue`  
  Responsibility: admin-side shell for crawl tasks and data-source status pages.
- Create: `frontend/src/modules/auth/pages/AuthPage.vue`  
  Responsibility: login/register UI currently embedded in `App.vue`.
- Create: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`  
  Responsibility: user flight search, list, selected flight details, and price trend.
- Create: `frontend/src/modules/user-profile/pages/FavoritesPage.vue`  
  Responsibility: favorite flights view.
- Create: `frontend/src/modules/user-profile/pages/SearchHistoryPage.vue`  
  Responsibility: stored user searches view.
- Create: `frontend/src/modules/admin-crawl/pages/CrawlJobsPage.vue`  
  Responsibility: admin crawl form and crawl job list.
- Create: `frontend/src/modules/admin-crawl/pages/DataSourceStatusPage.vue`  
  Responsibility: admin data source status view.
- Create: `frontend/src/shared/charts/PriceHistoryChart.vue`  
  Responsibility: shared ECharts wrapper for flight/suggestion trend visuals.
- Create: `frontend/src/shared/components/FlightTable.vue`  
  Responsibility: reusable flight result table.
- Create: `frontend/src/shared/components/FlightDetailCard.vue`  
  Responsibility: reusable selected-flight detail card.
- Create: `frontend/src/api/http.js`  
  Responsibility: shared Axios client with auth/token interceptors.
- Create: `frontend/src/api/authApi.js`  
  Responsibility: auth-only requests.
- Create: `frontend/src/api/flightApi.js`  
  Responsibility: flight query/detail/history requests.
- Create: `frontend/src/api/profileApi.js`  
  Responsibility: favorites and search history requests.
- Create: `frontend/src/api/adminCrawlApi.js`  
  Responsibility: admin crawl job and source status requests.
- Modify: `frontend/src/App.vue`  
  Responsibility: reduce to `<router-view />` app shell only.
- Modify: `frontend/src/main.js`  
  Responsibility: register router before mounting.
- Modify: `frontend/src/style.css`  
  Responsibility: keep only shared app-level styling and remove page-specific coupling.
- Keep temporarily: `frontend/src/lib/format.js`  
  Responsibility: existing formatting helpers, moved later only when all consumers are migrated.

### Backend files
- Create: `backend/src/main/java/com/example/flight/crawl/admin/AdminCrawlController.java`  
  Responsibility: admin-only crawl endpoints under `/api/admin/crawl-jobs`.
- Create: `backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusController.java`  
  Responsibility: `/api/admin/data-sources/status` API.
- Create: `backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusService.java`  
  Responsibility: compute crawler/data-source availability summaries.
- Create: `backend/src/main/java/com/example/flight/flight/favorite/FavoriteController.java`  
  Responsibility: `/api/me/favorites` CRUD endpoints.
- Create: `backend/src/main/java/com/example/flight/flight/favorite/FavoriteRepository.java`  
  Responsibility: JDBC persistence for user favorites.
- Create: `backend/src/main/java/com/example/flight/flight/history/SearchHistoryController.java`  
  Responsibility: `/api/me/search-history` read endpoint.
- Create: `backend/src/main/java/com/example/flight/flight/history/SearchHistoryRepository.java`  
  Responsibility: JDBC persistence for search history.
- Create: `backend/src/main/java/com/example/flight/flight/history/SearchHistoryService.java`  
  Responsibility: record search requests from flight queries and read them for the current user.
- Modify: `backend/src/main/java/com/example/flight/flight/FlightController.java`  
  Responsibility: call history recording logic after successful flight searches.
- Modify: `backend/src/main/java/com/example/flight/crawl/CrawlService.java`  
  Responsibility: stay reusable behind admin controller, not user-facing.
- Modify: `backend/src/main/java/com/example/flight/config/DatabaseInitializer.java`  
  Responsibility: create any new `favorite` and `search_history` tables.
- Modify: `backend/src/main/resources/application.yml`  
  Responsibility: keep crawler config stable; no AI-driven routing in phase 1.
- Keep temporarily: `backend/src/main/java/com/example/flight/ai/*`  
  Responsibility: preserved but no longer part of the primary navigation target.

### Crawler files
- Keep: `crawler/flight_crawler/pipelines.py`  
  Responsibility: continue writing `flight`, `flight_price_snapshot`, and `crawl_job`.
- Keep: `crawler/flight_crawler/spiders/*`  
  Responsibility: no phase-1 functional rewrite; only confirm that the admin-triggered path still works.

### Test files
- Create: `frontend/src/api/authApi.spec.js`
- Create: `frontend/src/api/flightApi.spec.js`
- Create: `frontend/src/api/profileApi.spec.js`
- Create: `frontend/src/api/adminCrawlApi.spec.js`
- Create: `backend/src/test/java/com/example/flight/flight/favorite/FavoriteRepositoryTest.java`
- Create: `backend/src/test/java/com/example/flight/flight/history/SearchHistoryRepositoryTest.java`
- Create: `backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusServiceTest.java`
- Modify: `backend/src/test/java/com/example/flight/flight/FlightRepositoryTest.java`

---

### Task 1: Replace the monolithic frontend entry with a routed app shell

**Files:**
- Create: `frontend/src/router/index.js`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/main.js`
- Test: `frontend/src/api/client.spec.js`

- [ ] **Step 1: Write the failing route bootstrap test**

```js
import { describe, expect, it } from 'vitest'
import router from '../router/index.js'

describe('router bootstrap', () => {
  it('declares user, admin, and auth entry routes', () => {
    const routeNames = router.getRoutes().map(route => route.name)
    expect(routeNames).toContain('auth')
    expect(routeNames).toContain('user-flights')
    expect(routeNames).toContain('admin-crawl-jobs')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/api/client.spec.js`
Expected: FAIL because `../router/index.js` does not exist and the app has no routed shell.

- [ ] **Step 3: Write the minimal routed shell implementation**

```js
// frontend/src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import AuthPage from '../modules/auth/pages/AuthPage.vue'
import UserLayout from '../layouts/UserLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import FlightSearchPage from '../modules/user-flights/pages/FlightSearchPage.vue'
import FavoritesPage from '../modules/user-profile/pages/FavoritesPage.vue'
import SearchHistoryPage from '../modules/user-profile/pages/SearchHistoryPage.vue'
import CrawlJobsPage from '../modules/admin-crawl/pages/CrawlJobsPage.vue'
import DataSourceStatusPage from '../modules/admin-crawl/pages/DataSourceStatusPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/auth', name: 'auth', component: AuthPage, meta: { public: true } },
    {
      path: '/',
      component: UserLayout,
      children: [
        { path: '', redirect: '/flights' },
        { path: 'flights', name: 'user-flights', component: FlightSearchPage },
        { path: 'favorites', name: 'user-favorites', component: FavoritesPage },
        { path: 'history', name: 'user-history', component: SearchHistoryPage }
      ]
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAdmin: true },
      children: [
        { path: 'crawl-jobs', name: 'admin-crawl-jobs', component: CrawlJobsPage },
        { path: 'data-sources', name: 'admin-data-sources', component: DataSourceStatusPage }
      ]
    }
  ]
})

export default router
```

```vue
<!-- frontend/src/App.vue -->
<template>
  <router-view />
</template>
```

```js
// frontend/src/main.js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'
import router from './router/index.js'

createApp(App).use(router).use(ElementPlus).mount('#app')
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/api/client.spec.js`
Expected: PASS for the route bootstrap assertions.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/App.vue frontend/src/main.js frontend/src/router/index.js
git commit -m "refactor: add routed frontend shell"
```

### Task 2: Extract auth into its own frontend module and shared HTTP client

**Files:**
- Create: `frontend/src/modules/auth/pages/AuthPage.vue`
- Create: `frontend/src/api/http.js`
- Create: `frontend/src/api/authApi.js`
- Modify: `frontend/src/api/client.js`
- Test: `frontend/src/api/authApi.spec.js`

- [ ] **Step 1: Write the failing auth API test**

```js
import { describe, expect, it } from 'vitest'
import { login, register, getMe, logout } from './authApi.js'

describe('auth api module', () => {
  it('exports the dedicated auth actions', () => {
    expect(typeof login).toBe('function')
    expect(typeof register).toBe('function')
    expect(typeof getMe).toBe('function')
    expect(typeof logout).toBe('function')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/api/authApi.spec.js`
Expected: FAIL because `authApi.js` does not exist.

- [ ] **Step 3: Write the minimal dedicated auth client**

```js
// frontend/src/api/http.js
import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.dispatchEvent(new CustomEvent('auth:logout'))
    }
    return Promise.reject(error)
  }
)

export default http
```

```js
// frontend/src/api/authApi.js
import http from './http.js'

export async function login(username, password) {
  const response = await http.post('/api/auth/login', { username, password })
  return response.data
}

export async function register(username, password, nickname) {
  const response = await http.post('/api/auth/register', { username, password, nickname })
  return response.data
}

export async function getMe() {
  const response = await http.get('/api/auth/me')
  return response.data
}

export async function logout() {
  await http.post('/api/auth/logout')
}
```

```js
// frontend/src/api/client.js
export * from './authApi.js'
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/api/authApi.spec.js`
Expected: PASS and the auth API module is now independent of the general client barrel.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/auth/pages/AuthPage.vue frontend/src/api/http.js frontend/src/api/authApi.js frontend/src/api/client.js frontend/src/api/authApi.spec.js
git commit -m "refactor: extract auth frontend module"
```

### Task 3: Split user flight search into module pages and dedicated APIs

**Files:**
- Create: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`
- Create: `frontend/src/shared/components/FlightTable.vue`
- Create: `frontend/src/shared/components/FlightDetailCard.vue`
- Create: `frontend/src/shared/charts/PriceHistoryChart.vue`
- Create: `frontend/src/api/flightApi.js`
- Test: `frontend/src/api/flightApi.spec.js`

- [ ] **Step 1: Write the failing flight API test**

```js
import { describe, expect, it } from 'vitest'
import { fetchFlights, fetchFlight, fetchPriceHistory } from './flightApi.js'

describe('flight api module', () => {
  it('exports flight query functions', () => {
    expect(typeof fetchFlights).toBe('function')
    expect(typeof fetchFlight).toBe('function')
    expect(typeof fetchPriceHistory).toBe('function')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/api/flightApi.spec.js`
Expected: FAIL because `flightApi.js` does not exist.

- [ ] **Step 3: Write the minimal user flight module split**

```js
// frontend/src/api/flightApi.js
import http from './http.js'

export async function fetchFlights(params = {}) {
  const response = await http.get('/api/flights', { params })
  return response.data
}

export async function fetchFlight(id) {
  const response = await http.get(`/api/flights/${id}`)
  return response.data
}

export async function fetchPriceHistory(id) {
  const response = await http.get(`/api/flights/${id}/price-history`)
  return response.data
}
```

```vue
<!-- frontend/src/modules/user-flights/pages/FlightSearchPage.vue -->
<template>
  <section class="user-flight-page">
    <h1>航班搜索</h1>
    <FlightTable :rows="flights" @select="selectFlight" />
    <FlightDetailCard :flight="selectedFlight" />
    <PriceHistoryChart :history="priceHistory" />
  </section>
</template>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npm test -- src/api/flightApi.spec.js`
Expected: PASS and the search functionality is no longer anchored to `App.vue`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue frontend/src/shared/components/FlightTable.vue frontend/src/shared/components/FlightDetailCard.vue frontend/src/shared/charts/PriceHistoryChart.vue frontend/src/api/flightApi.js frontend/src/api/flightApi.spec.js
git commit -m "refactor: split user flight frontend module"
```

### Task 4: Add user profile modules for favorites and search history

**Files:**
- Create: `frontend/src/modules/user-profile/pages/FavoritesPage.vue`
- Create: `frontend/src/modules/user-profile/pages/SearchHistoryPage.vue`
- Create: `frontend/src/api/profileApi.js`
- Create: `backend/src/main/java/com/example/flight/flight/favorite/FavoriteController.java`
- Create: `backend/src/main/java/com/example/flight/flight/favorite/FavoriteRepository.java`
- Create: `backend/src/main/java/com/example/flight/flight/history/SearchHistoryController.java`
- Create: `backend/src/main/java/com/example/flight/flight/history/SearchHistoryRepository.java`
- Create: `backend/src/main/java/com/example/flight/flight/history/SearchHistoryService.java`
- Modify: `backend/src/main/java/com/example/flight/flight/FlightController.java`
- Modify: `backend/src/main/java/com/example/flight/config/DatabaseInitializer.java`
- Test: `backend/src/test/java/com/example/flight/flight/favorite/FavoriteRepositoryTest.java`
- Test: `backend/src/test/java/com/example/flight/flight/history/SearchHistoryRepositoryTest.java`

- [ ] **Step 1: Write the failing repository tests**

```java
@JdbcTest
@Import(FavoriteRepository.class)
class FavoriteRepositoryTest {
    @Autowired FavoriteRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void savesAndListsFavoritesByUser() {
        Long userId = jdbcTemplate.queryForObject(
                "insert into user(username, password, nickname) values ('alice', 'x', 'Alice')",
                Long.class
        );
        repository.save(userId, 1L);
        assertThat(repository.findByUserId(userId)).hasSize(1);
    }
}
```

```java
@JdbcTest
@Import(SearchHistoryRepository.class)
class SearchHistoryRepositoryTest {
    @Autowired SearchHistoryRepository repository;

    @Test
    void recordsUserSearchesInReverseChronologicalOrder() {
        repository.save(1L, "上海", "北京", LocalDate.of(2026, 6, 20), "sample");
        assertThat(repository.findByUserId(1L)).hasSize(1);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -Dtest=FavoriteRepositoryTest,SearchHistoryRepositoryTest`
Expected: FAIL because the repositories and schema support do not exist.

- [ ] **Step 3: Write the minimal favorite/history implementation**

```java
// backend/src/main/java/com/example/flight/flight/favorite/FavoriteController.java
@RestController
@RequestMapping("/api/me/favorites")
public class FavoriteController {
    private final FavoriteRepository favoriteRepository;

    public FavoriteController(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestAttribute("user") User user) {
        return favoriteRepository.findByUserId(user.id());
    }
}
```

```java
// backend/src/main/java/com/example/flight/flight/history/SearchHistoryService.java
@Service
public class SearchHistoryService {
    private final SearchHistoryRepository repository;

    public SearchHistoryService(SearchHistoryRepository repository) {
        this.repository = repository;
    }

    public void record(Long userId, String fromCity, String toCity, LocalDate date, String dataSource) {
        if (userId == null) return;
        repository.save(userId, fromCity, toCity, date, dataSource);
    }
}
```

```java
// backend/src/main/java/com/example/flight/flight/FlightController.java
private final SearchHistoryService searchHistoryService;

@GetMapping
public List<Flight> search(..., @RequestAttribute(value = "user", required = false) User user) {
    List<Flight> result = flightRepository.search(new FlightSearchCriteria(fromCity, toCity, date, dataSource));
    if (!result.isEmpty() && user != null) {
        searchHistoryService.record(user.id(), fromCity, toCity, date, dataSource);
    }
    return result;
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=FavoriteRepositoryTest,SearchHistoryRepositoryTest`
Expected: PASS and the user profile capabilities now exist at the backend boundary.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/flight/flight/favorite backend/src/main/java/com/example/flight/flight/history backend/src/main/java/com/example/flight/flight/FlightController.java backend/src/main/java/com/example/flight/config/DatabaseInitializer.java backend/src/test/java/com/example/flight/flight/favorite/FavoriteRepositoryTest.java backend/src/test/java/com/example/flight/flight/history/SearchHistoryRepositoryTest.java frontend/src/modules/user-profile/pages/FavoritesPage.vue frontend/src/modules/user-profile/pages/SearchHistoryPage.vue frontend/src/api/profileApi.js
git commit -m "feat: add favorites and search history"
```

### Task 5: Move crawl operations behind admin-specific APIs and UI

**Files:**
- Create: `backend/src/main/java/com/example/flight/crawl/admin/AdminCrawlController.java`
- Create: `backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusController.java`
- Create: `backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusService.java`
- Create: `frontend/src/modules/admin-crawl/pages/CrawlJobsPage.vue`
- Create: `frontend/src/modules/admin-crawl/pages/DataSourceStatusPage.vue`
- Create: `frontend/src/api/adminCrawlApi.js`
- Modify: `backend/src/main/java/com/example/flight/crawl/CrawlController.java`
- Test: `backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusServiceTest.java`
- Test: `frontend/src/api/adminCrawlApi.spec.js`

- [ ] **Step 1: Write the failing admin crawl tests**

```java
class DataSourceStatusServiceTest {
    @Test
    void exposesSampleAndConfiguredExternalSources() {
        DataSourceStatusService service = new DataSourceStatusService("id", "secret");
        assertThat(service.listStatuses()).extracting("code").contains("sample", "amadeus");
    }
}
```

```js
import { describe, expect, it } from 'vitest'
import { createCrawlJob, listCrawlJobs, listDataSourceStatuses } from './adminCrawlApi.js'

describe('admin crawl api module', () => {
  it('exports admin-only crawl operations', () => {
    expect(typeof createCrawlJob).toBe('function')
    expect(typeof listCrawlJobs).toBe('function')
    expect(typeof listDataSourceStatuses).toBe('function')
  })
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -Dtest=DataSourceStatusServiceTest`
Expected: FAIL because the admin status service does not exist.

Run: `cd frontend && npm test -- src/api/adminCrawlApi.spec.js`
Expected: FAIL because `adminCrawlApi.js` does not exist.

- [ ] **Step 3: Write the minimal admin crawl boundary**

```java
// backend/src/main/java/com/example/flight/crawl/admin/AdminCrawlController.java
@RestController
@RequestMapping("/api/admin/crawl-jobs")
public class AdminCrawlController {
    private final CrawlService crawlService;
    private final CrawlRepository crawlRepository;

    @PostMapping
    public CrawlJob create(@RequestBody(required = false) CrawlRequest request) {
        return crawlService.runCrawler(request == null ? new CrawlRequest(null, null, null, null, null, null) : request);
    }

    @GetMapping
    public List<CrawlJob> list() {
        return crawlRepository.findRecent(20);
    }
}
```

```java
// backend/src/main/java/com/example/flight/crawl/admin/DataSourceStatusService.java
@Service
public class DataSourceStatusService {
    public List<Map<String, Object>> listStatuses() {
        return List.of(
                Map.of("code", "sample", "available", true, "mode", "fallback"),
                Map.of("code", "amadeus", "available", true, "mode", "remote")
        );
    }
}
```

```js
// frontend/src/api/adminCrawlApi.js
import http from './http.js'

export async function createCrawlJob(payload = {}) {
  const response = await http.post('/api/admin/crawl-jobs', payload, { timeout: 130000 })
  return response.data
}

export async function listCrawlJobs() {
  const response = await http.get('/api/admin/crawl-jobs')
  return response.data
}

export async function listDataSourceStatuses() {
  const response = await http.get('/api/admin/data-sources/status')
  return response.data
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=DataSourceStatusServiceTest`
Expected: PASS and the backend exposes admin crawl boundaries.

Run: `cd frontend && npm test -- src/api/adminCrawlApi.spec.js`
Expected: PASS and the frontend no longer uses crawl APIs from a mixed general client.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/example/flight/crawl/admin backend/src/test/java/com/example/flight/crawl/admin/DataSourceStatusServiceTest.java frontend/src/modules/admin-crawl/pages/CrawlJobsPage.vue frontend/src/modules/admin-crawl/pages/DataSourceStatusPage.vue frontend/src/api/adminCrawlApi.js frontend/src/api/adminCrawlApi.spec.js
git commit -m "refactor: move crawl flow behind admin boundary"
```

### Task 6: Remove AI from the primary navigation and stabilize the phase-1 architecture

**Files:**
- Modify: `frontend/src/layouts/UserLayout.vue`
- Modify: `frontend/src/layouts/AdminLayout.vue`
- Modify: `frontend/src/style.css`
- Modify: `README.md`
- Test: `frontend/src/api/flightApi.spec.js`
- Test: `backend/src/test/java/com/example/flight/flight/FlightRepositoryTest.java`

- [ ] **Step 1: Write the failing architectural smoke check**

```js
import { describe, expect, it } from 'vitest'
import router from '../router/index.js'

describe('primary navigation architecture', () => {
  it('does not expose AI as a primary route in phase 1', () => {
    const routeNames = router.getRoutes().map(route => route.name)
    expect(routeNames).not.toContain('ai')
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm test -- src/api/flightApi.spec.js`
Expected: FAIL while the old AI-first route/navigation assumptions still remain.

- [ ] **Step 3: Write the minimal stabilization changes**

```md
# README excerpt

## Phase 1 architecture

- User side: flight search, favorites, search history
- Admin side: crawl jobs, data source status
- AI remains in the codebase as a reserved extension, not a primary navigation surface
```

```vue
<!-- frontend/src/layouts/UserLayout.vue -->
<template>
  <main class="user-layout">
    <nav>
      <RouterLink to="/flights">航班搜索</RouterLink>
      <RouterLink to="/favorites">我的收藏</RouterLink>
      <RouterLink to="/history">搜索历史</RouterLink>
    </nav>
    <router-view />
  </main>
</template>
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd frontend && npm test -- src/api/flightApi.spec.js`
Expected: PASS with no AI primary route remaining.

Run: `cd backend && mvn test -Dtest=FlightRepositoryTest`
Expected: PASS and existing query behavior remains intact.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/layouts/UserLayout.vue frontend/src/layouts/AdminLayout.vue frontend/src/style.css README.md
git commit -m "refactor: align primary navigation with phase 1 architecture"
```

---

## Self-Review

### Spec coverage
- User/admin split: covered by Tasks 1, 3, 4, 5, and 6.
- Frontend routed shell and module split: covered by Tasks 1, 2, and 3.
- Admin-only crawl boundary: covered by Task 5.
- Favorites and search history: covered by Task 4.
- AI moved out of current core flow: covered by Task 6.
- Crawler model preserved: explicitly preserved in the file-structure section and not rewritten in phase 1 tasks.

### Placeholder scan
- No placeholder markers remain in executable steps.
- Each task names exact files, commands, and a minimal code target.
- The plan intentionally avoids broader phase-2/phase-3 work such as crawler source refactors and AI re-entry.

### Type consistency
- Frontend API modules are consistently named `authApi`, `flightApi`, `profileApi`, and `adminCrawlApi`.
- Admin crawl routes consistently use `/api/admin/crawl-jobs` and `/api/admin/data-sources/status`.
- Search history consistently records `(userId, fromCity, toCity, date, dataSource)`.
