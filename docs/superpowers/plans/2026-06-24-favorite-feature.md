# 航班收藏功能完善 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在航班搜索页面的表格行和详情卡片中添加收藏/取消收藏按钮，搜索结果中显示已收藏状态。

**Architecture:** 后端 `Flight` 记录新增 `isFavorited`/`favoriteId` 字段，`FlightRepository.search()` 新增带 `userId` 的重载方法通过 LEFT JOIN 一次查询带回收藏状态。前端新建 `FavoriteButton.vue` 可复用组件，修改 `FlightTable`/`FlightDetailCard`/`FlightSearchPage` 接入。

**Tech Stack:** Java 17 (JDBC), Vue 3 (Composition API), Element Plus, Vitest + JUnit 5

## Global Constraints

- 收藏操作基于现有 API：`POST /api/me/favorites` (body: `{flightId}`), `DELETE /api/me/favorites/{id}`
- 前端 API 层 `profileApi.js` 中的 `addFavorite`/`removeFavorite` 已存在，直接复用
- `Flight` 为 Java record，新增字段需处理向后兼容（字段级 `@JsonInclude(NON_NULL)`，仅 null 时省略）
- `FlightSearchPort` 接口被 `TimingService`/`AdviceService` 使用，不可改动签名
- 图标使用 Element Plus 内置 `Star` / `StarFilled`
- 收藏按钮在两处展示：表格行内 + 详情卡片 header

---

### Task 1: 数据库 — init.sql 补入 favorite 表

**Files:**
- Modify: `infra/mysql/init.sql`

**Interfaces:**
- Produces: `favorite` 表在 MySQL 初始化时创建（与 `DatabaseInitializer` 中的定义一致）

- [ ] **Step 1: 在 init.sql 末尾追加 favorite 表定义**

在文件末尾追加：

```sql
create table if not exists favorite (
    id bigint primary key auto_increment,
    user_id bigint not null,
    flight_id bigint not null,
    created_at datetime not null,
    constraint uk_favorite_user_flight unique (user_id, flight_id),
    constraint fk_favorite_user foreign key (user_id) references app_user(id),
    constraint fk_favorite_flight foreign key (flight_id) references flight(id),
    index idx_favorite_user_created (user_id, created_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
```

- [ ] **Step 2: 验证 MySQL 启动后表存在**

```bash
docker compose -f infra/docker-compose.yml up -d mysql
docker compose -f infra/docker-compose.yml exec mysql mysql -u root -proot flight_demo -e "show create table favorite"
```

Expected: 输出 favorite 表结构。

- [ ] **Step 3: Commit**

```bash
git add infra/mysql/init.sql
git commit -m "feat: add favorite table definition to init.sql"
```

---

### Task 2: Backend — Flight 记录新增收藏字段

**Files:**
- Modify: `backend/src/main/java/com/example/flight/flight/Flight.java`

**Interfaces:**
- Consumes: nothing
- Produces: `Flight` record 新增 `Boolean isFavorited` (nullable, `@JsonInclude(NON_NULL)`) 和 `Long favoriteId` (nullable, `@JsonInclude(NON_NULL)`)

- [ ] **Step 1: 修改 Flight.java**

在现有 record 末尾新增两个字段：

```java
package com.example.flight.flight;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Flight(
        Long id,
        String flightNo,
        String airlineName,
        String fromCity,
        String toCity,
        String fromAirport,
        String toAirport,
        LocalDateTime departTime,
        LocalDateTime arriveTime,
        BigDecimal price,
        Integer seatsLeft,
        String dataSource,
        LocalDateTime collectedAt,
        // 新增字段 — 仅当已登录用户搜索时 LEFT JOIN favorite 表填充
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean isFavorited,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long favoriteId
) {}
```

说明：`@JsonInclude(NON_NULL)` 仅标注在两个新字段上。Jackson 2.12+ 支持 record component 级别的注解。序列化时 `null` 值不输出，未登录或旧调用方不受影响。

- [ ] **Step 2: 确认现有测试仍然通过**

```bash
cd backend && mvn test -pl . -Dtest="FlightRepositoryTest,FavoriteRepositoryTest"
```

Expected: 所有测试 PASS。现有 `FlightRepository` 的 `rowMapper` 未改动（仍用 `select * from flight`），新字段不会出现在查询结果中，RowMapper 不会尝试读取它们。`FavoriteRepository` 的 `rowMapper` 同样不受影响。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/example/flight/flight/Flight.java
git commit -m "feat: add isFavorited and favoriteId fields to Flight record"
```

---

### Task 3: Backend — FlightRepository 新增带收藏状态的搜索方法

**Files:**
- Modify: `backend/src/main/java/com/example/flight/flight/FlightRepository.java`

**Interfaces:**
- Consumes: `Flight` record with `isFavorited`/`favoriteId` (Task 2)
- Produces: `search(FlightSearchCriteria criteria, Long userId): List<Flight>` — 新重载方法

- [ ] **Step 1: 新增 RowMapper 和重载方法**

在 `FlightRepository` 中新增：

```java
private final RowMapper<Flight> rowMapperWithFavorite = (rs, rowNum) -> new Flight(
        rs.getLong("id"),
        rs.getString("flight_no"),
        rs.getString("airline_name"),
        rs.getString("from_city"),
        rs.getString("to_city"),
        rs.getString("from_airport"),
        rs.getString("to_airport"),
        rs.getTimestamp("depart_time").toLocalDateTime(),
        rs.getTimestamp("arrive_time").toLocalDateTime(),
        rs.getBigDecimal("price"),
        rs.getInt("seats_left"),
        rs.getString("data_source"),
        rs.getTimestamp("collected_at").toLocalDateTime(),
        getBooleanOrNull(rs, "is_favorited"),
        getLongOrNull(rs, "favorite_id")
);

private Boolean getBooleanOrNull(ResultSet rs, String column) throws SQLException {
    try {
        boolean val = rs.getBoolean(column);
        return rs.wasNull() ? null : val;
    } catch (SQLException e) {
        return null;
    }
}

private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
    try {
        long val = rs.getLong(column);
        return rs.wasNull() ? null : val;
    } catch (SQLException e) {
        return null;
    }
}

public List<Flight> search(FlightSearchCriteria criteria, Long userId) {
    var sql = new StringBuilder(
        "SELECT f.*, " +
        "fav.id AS favorite_id, " +
        "CASE WHEN fav.id IS NOT NULL THEN true ELSE false END AS is_favorited " +
        "FROM flight f " +
        "LEFT JOIN favorite fav ON fav.flight_id = f.id AND fav.user_id = ? " +
        "WHERE 1=1");
    var args = new ArrayList<Object>();
    args.add(userId);

    if (StringUtils.hasText(criteria.fromCity())) {
        sql.append(" and (f.from_city = ? or upper(f.from_airport) = upper(?))");
        args.add(criteria.fromCity());
        args.add(criteria.fromCity());
    }
    if (StringUtils.hasText(criteria.toCity())) {
        sql.append(" and (f.to_city = ? or upper(f.to_airport) = upper(?))");
        args.add(criteria.toCity());
        args.add(criteria.toCity());
    }
    if (criteria.date() != null) {
        LocalDate date = criteria.date();
        sql.append(" and f.depart_time >= ? and f.depart_time < ?");
        args.add(Timestamp.valueOf(date.atStartOfDay()));
        args.add(Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
    }
    if (StringUtils.hasText(criteria.dataSource())) {
        sql.append(" and f.data_source = ?");
        args.add(criteria.dataSource());
    }

    sql.append(" order by f.price asc, f.depart_time asc");
    return jdbcTemplate.query(sql.toString(), rowMapperWithFavorite, args.toArray());
}
```

**关键点：**
- 现有 `rowMapper` 和 `search(FlightSearchCriteria)` 方法**保持不变**（`FlightSearchPort` 接口不可改）
- 新增 `search(FlightSearchCriteria, Long userId)` 重载方法
- LEFT JOIN 确保未收藏的航班也会返回（`isFavorited=false, favoriteId=null`）
- `getBooleanOrNull`/`getLongOrNull` 对不存在的列做防御

- [ ] **Step 2: 运行现有测试确认不破坏旧行为**

```bash
cd backend && mvn test -Dtest="FlightRepositoryTest"
```

Expected: PASS。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/example/flight/flight/FlightRepository.java
git commit -m "feat: add search with userId to FlightRepository for favorite status"
```

---

### Task 4: Backend — FlightController 传递 userId 给新查询

**Files:**
- Modify: `backend/src/main/java/com/example/flight/flight/FlightController.java`

**Interfaces:**
- Consumes: `FlightRepository.search(criteria, userId)` (Task 3)
- Produces: `GET /api/flights` 返回的 `Flight` JSON 含 `isFavorited`/`favoriteId` 字段（已登录时）

- [ ] **Step 1: 修改 search() 方法**

```java
@GetMapping
public List<Flight> search(@RequestParam(required = false) String fromCity,
                           @RequestParam(required = false) String toCity,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @RequestParam(required = false) String dataSource,
                           @RequestAttribute("user") User user) {
    log.debug("flight search: fromCity={}, toCity={}, date={}, dataSource={}", fromCity, toCity, date, dataSource);
    var criteria = new FlightSearchCriteria(fromCity, toCity, date, dataSource);
    List<Flight> flights = flightRepository.search(criteria, user.id());  // 改为调用带 userId 的重载
    log.debug("flight search result count={}", flights.size());
    if (shouldRecordSearch(fromCity, toCity, date, dataSource)) {
        searchHistoryService.record(user.id(), fromCity, toCity, date, dataSource);
    }
    return flights;
}
```

改动仅一行：`flightRepository.search(criteria)` → `flightRepository.search(criteria, user.id())`

- [ ] **Step 2: 运行测试验证**

```bash
cd backend && mvn test
```

Expected: ALL PASS。

- [ ] **Step 3: 手动验证 API 返回**

启动后端，带 token 调用：

```bash
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/flights?fromCity=Shanghai"
```

Expected: 返回的 JSON 每个 flight 对象含 `isFavorited` (boolean) 和 `favoriteId` (null 或 number)。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/example/flight/flight/FlightController.java
git commit -m "feat: pass userId to flight search for favorite status enrichment"
```

---

### Task 5: Frontend — 新建 FavoriteButton.vue 可复用组件

**Files:**
- Create: `frontend/src/shared/components/FavoriteButton.vue`
- Test: `frontend/src/shared/components/FavoriteButton.spec.js`

**Interfaces:**
- Consumes: `addFavorite(payload)`, `removeFavorite(favoriteId)` from `profileApi.js`
- Produces: `<FavoriteButton>` — props: `flightId` (Number), `isFavorited` (Boolean), `favoriteId` (Number|null); emits: `toggled(isFavorited, favoriteId)`

- [ ] **Step 1: 写测试文件 FavoriteButton.spec.js**

```js
// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import FavoriteButton from './FavoriteButton.vue'

const mocks = vi.hoisted(() => ({
  addFavorite: vi.fn(),
  removeFavorite: vi.fn()
}))

vi.mock('../../api/profileApi.js', () => ({
  addFavorite: mocks.addFavorite,
  removeFavorite: mocks.removeFavorite
}))

async function flushPromises() {
  await Promise.resolve()
  await Promise.resolve()
  await new Promise(resolve => setTimeout(resolve, 0))
  await nextTick()
}

describe('FavoriteButton', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders unfavorited star when isFavorited is false', () => {
    const wrapper = mount(FavoriteButton, {
      props: { flightId: 1, isFavorited: false, favoriteId: null }
    })
    // 应该渲染空心星（未收藏状态）
    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.html()).not.toContain('star-filled')
  })

  it('renders favorited star when isFavorited is true', () => {
    const wrapper = mount(FavoriteButton, {
      props: { flightId: 1, isFavorited: true, favoriteId: 10 }
    })
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('calls addFavorite and emits toggled when clicking unfavorited star', async () => {
    mocks.addFavorite.mockResolvedValue({ id: 99, flightId: 1 })
    const wrapper = mount(FavoriteButton, {
      props: { flightId: 1, isFavorited: false, favoriteId: null }
    })

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(mocks.addFavorite).toHaveBeenCalledWith({ flightId: 1 })
    expect(wrapper.emitted('toggled')).toBeTruthy()
    expect(wrapper.emitted('toggled')[0]).toEqual([true, 99])
  })

  it('calls removeFavorite and emits toggled when clicking favorited star', async () => {
    mocks.removeFavorite.mockResolvedValue()
    const wrapper = mount(FavoriteButton, {
      props: { flightId: 1, isFavorited: true, favoriteId: 10 }
    })

    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(mocks.removeFavorite).toHaveBeenCalledWith(10)
    expect(wrapper.emitted('toggled')).toBeTruthy()
    expect(wrapper.emitted('toggled')[0]).toEqual([false, null])
  })

  it('disables button while request is in flight', async () => {
    // addFavorite 不 resolve，保持 loading 状态
    mocks.addFavorite.mockReturnValue(new Promise(() => {}))
    const wrapper = mount(FavoriteButton, {
      props: { flightId: 1, isFavorited: false, favoriteId: null }
    })

    await wrapper.find('button').trigger('click')
    await nextTick()

    expect(wrapper.find('button').attributes('disabled')).toBeDefined()
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd frontend && npx vitest run src/shared/components/FavoriteButton.spec.js
```

Expected: FAIL（组件文件不存在）

- [ ] **Step 3: 实现 FavoriteButton.vue**

```vue
<template>
  <el-button
    :loading="loading"
    :aria-label="isFavorited ? t('common.actions.unfavorite') : t('common.actions.favorite')"
    link
    @click.stop="toggle"
  >
    <el-icon :size="18">
      <StarFilled v-if="isFavorited" style="color: #e6a817;" />
      <Star v-else style="color: #94a3b8;" />
    </el-icon>
  </el-button>
</template>

<script setup>
import { ref } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { addFavorite, removeFavorite } from '../../api/profileApi.js'

const { t } = useI18n()

const props = defineProps({
  flightId: { type: Number, required: true },
  isFavorited: { type: Boolean, default: false },
  favoriteId: { type: Number, default: null }
})

const emit = defineEmits(['toggled'])

const loading = ref(false)

async function toggle() {
  loading.value = true
  try {
    if (props.isFavorited) {
      await removeFavorite(props.favoriteId)
      emit('toggled', false, null)
      ElMessage.success(t('flights.favorite.removed'))
    } else {
      const result = await addFavorite({ flightId: props.flightId })
      emit('toggled', true, result.id)
      ElMessage.success(t('flights.favorite.added'))
    }
  } catch {
    ElMessage.error(t('flights.favorite.failed'))
  } finally {
    loading.value = false
  }
}
</script>
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd frontend && npx vitest run src/shared/components/FavoriteButton.spec.js
```

Expected: 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/components/FavoriteButton.vue frontend/src/shared/components/FavoriteButton.spec.js
git commit -m "feat: add FavoriteButton component"
```

---

### Task 6: Frontend — 修改 FlightTable.vue 添加收藏列

**Files:**
- Modify: `frontend/src/shared/components/FlightTable.vue`

**Interfaces:**
- Consumes: `FavoriteButton` (Task 5)
- New prop: `favoriteStatusMap` — `Map<number, { isFavorited: boolean, favoriteId: number|null }>`
- New emit: `favorite-toggled` — payload `{ flightId, isFavorited, favoriteId }`

- [ ] **Step 1: 修改 FlightTable.vue**

在 `<script setup>` 中新增：

```js
import FavoriteButton from './FavoriteButton.vue'

const props = defineProps({
  flights: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  selectedFlightId: { type: [Number, String], default: null },
  // 新增
  favoriteStatusMap: { type: Map, default: () => new Map() }
})

const emit = defineEmits(['select', 'favorite-toggled'])

function favoriteState(flightId) {
  return props.favoriteStatusMap.get(flightId) || { isFavorited: false, favoriteId: null }
}

function onFavoriteToggled(flightId, isFavorited, favoriteId) {
  emit('favorite-toggled', { flightId, isFavorited, favoriteId })
}
```

在 `<template>` 中，`<el-table>` 最后一个 `<el-table-column>`（status 列）**之后**新增收藏列：

```html
<el-table-column :label="t('common.actions.favorite')" width="50" align="center">
  <template #default="{ row }">
    <FavoriteButton
      :flight-id="row.id"
      :is-favorited="favoriteState(row.id).isFavorited"
      :favorite-id="favoriteState(row.id).favoriteId"
      @toggled="(fav, id) => onFavoriteToggled(row.id, fav, id)"
    />
  </template>
</el-table-column>
```

同时阻止收藏按钮的点击事件冒泡导致行选中：

在 `FavoriteButton` 组件中已有 `@click.stop`（Task 5 已包含），无需额外处理。

- [ ] **Step 2: 验证现有测试不破坏**

查看是否有 FlightTable 测试文件。若无，手动验证构建：

```bash
cd frontend && npx vitest run
```

Expected: 现有测试不受影响（FlightTable 无测试文件，FavoriteButton 测试 PASS）

- [ ] **Step 3: Commit**

```bash
git add frontend/src/shared/components/FlightTable.vue
git commit -m "feat: add favorite column to FlightTable"
```

---

### Task 7: Frontend — 修改 FlightDetailCard.vue 添加收藏按钮

**Files:**
- Modify: `frontend/src/shared/components/FlightDetailCard.vue`

**Interfaces:**
- Consumes: `FavoriteButton` (Task 5)
- New props: `isFavorited` (Boolean), `favoriteId` (Number|null)
- New emit: `favorite-toggled`

- [ ] **Step 1: 修改 FlightDetailCard.vue**

在 `<script setup>` 中：

```js
import FavoriteButton from './FavoriteButton.vue'

const props = defineProps({
  flight: { type: Object, required: true },
  // 新增
  isFavorited: { type: Boolean, default: false },
  favoriteId: { type: Number, default: null }
})

const emit = defineEmits(['favorite-toggled'])
```

在 `<template>` 的 header 区域，价格旁边添加：

```html
<header class="flight-detail-card__header">
  <div>
    <p class="flight-detail-card__eyebrow">{{ t('flights.detail.eyebrow') }}</p>
    <h3>
      {{ flight.flightNo || '-' }}
      <FavoriteButton
        :flight-id="flight.id"
        :is-favorited="isFavorited"
        :favorite-id="favoriteId"
        @toggled="(fav, id) => $emit('favorite-toggled', { flightId: flight.id, isFavorited: fav, favoriteId: id })"
      />
    </h3>
  </div>
  <strong class="flight-detail-card__price">¥{{ formatPrice(flight.price) }}</strong>
</header>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/shared/components/FlightDetailCard.vue
git commit -m "feat: add favorite button to FlightDetailCard header"
```

---

### Task 8: Frontend — 修改 FlightSearchPage.vue 集成收藏状态

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue`

**Interfaces:**
- Consumes: `FlightTable` (Task 6), `FlightDetailCard` (Task 7), `FavoriteButton` (Task 5)
- Manages: `favoriteStatusMap` (reactive Map)

- [ ] **Step 1: 新增收藏状态管理逻辑**

在 `<script setup>` 中新增（位置：在现有 `flights`/`selectedFlight` 等状态定义之后）：

```js
// 收藏状态 Map: flightId -> { isFavorited, favoriteId }
const favoriteStatusMap = ref(new Map())

function updateFavoriteStatus(flightsList) {
  flightsList.forEach(f => {
    if (f.isFavorited) {
      favoriteStatusMap.value.set(f.id, { isFavorited: true, favoriteId: f.favoriteId })
    }
  })
}

function onFavoriteToggled({ flightId, isFavorited, favoriteId }) {
  if (isFavorited) {
    favoriteStatusMap.value.set(flightId, { isFavorited: true, favoriteId })
  } else {
    favoriteStatusMap.value.delete(flightId)
  }
}

// 获取当前选中航班的收藏状态（计算属性）
const selectedFavoriteState = computed(() => {
  const flight = selectedFlight.value
  if (!flight) return { isFavorited: false, favoriteId: null }
  return favoriteStatusMap.value.get(flight.id) || { isFavorited: false, favoriteId: null }
})
```

在搜索成功后更新状态（在设置 `flights.value = ...` 之后）：

```js
// 在 handleSearch 函数中，赋值 flights.value 之后加：
updateFavoriteStatus(flights.value)
```

- [ ] **Step 2: 修改模板传入收藏 props 和事件**

`FlightTable` 部分新增 props 和事件：

```html
<FlightTable
  :flights="flights"
  :loading="loading"
  :selected-flight-id="selectedFlight?.id"
  :favorite-status-map="favoriteStatusMap"
  @select="onSelectFlight"
  @favorite-toggled="onFavoriteToggled"
/>
```

`FlightDetailCard` 部分新增 props 和事件：

```html
<FlightDetailCard
  :flight="selectedFlight"
  :is-favorited="selectedFavoriteState.isFavorited"
  :favorite-id="selectedFavoriteState.favoriteId"
  @favorite-toggled="onFavoriteToggled"
/>
```

- [ ] **Step 3: 验证构建**

```bash
cd frontend && npx vite build --mode development 2>&1 | tail -5
```

Expected: build 成功，无报错。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue
git commit -m "feat: integrate favorite state into FlightSearchPage"
```

---

### Task 9: Frontend — 补充 i18n 文案

**Files:**
- Modify: `frontend/src/i18n/messages/zh-CN.js`
- Modify: `frontend/src/i18n/messages/en-US.js`

**Interfaces:**
- Consumes: `FavoriteButton` (Task 5) — 使用 `common.actions.favorite`/`unfavorite`, `flights.favorite.*`

- [ ] **Step 1: 修改 zh-CN.js**

在 `common.actions` 中添加：

```js
favorite: '收藏',
unfavorite: '取消收藏',
```

在 `flights` 下新增 `favorite` 块：

```js
favorite: {
  added: '已收藏',
  removed: '已取消收藏',
  failed: '操作失败，请重试'
}
```

- [ ] **Step 2: 修改 en-US.js**

在 `common.actions` 中添加：

```js
favorite: 'Favorite',
unfavorite: 'Unfavorite',
```

在 `flights` 下新增 `favorite` 块：

```js
favorite: {
  added: 'Favorited',
  removed: 'Removed from favorites',
  failed: 'Operation failed, please retry'
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/i18n/messages/zh-CN.js frontend/src/i18n/messages/en-US.js
git commit -m "feat: add favorite-related i18n keys"
```

---

### Task 10: 端到端验证

**Files:**
- 无新建文件

- [ ] **Step 1: 启动全栈**

```bash
docker compose -f infra/docker-compose.yml up -d mysql
cd backend && mvn spring-boot:run &
cd frontend && npm run dev &
```

- [ ] **Step 2: 手动验证完整流程**

1. 登录 → 搜索航班 → 确认表格每行有收藏星形图标
2. 点击某航班的空心星 → 确认图标变实心金色 → 提示"已收藏"
3. 点击该行 → 详情卡片出现 → 确认卡片也有收藏按钮且为实心
4. 点击详情卡片中的收藏按钮 → 确认取消收藏 → 提示"已取消收藏"
5. 导航到 `/favorites` → 确认刚刚收藏的航班在列表中
6. 在收藏列表页移除该收藏 → 返回搜索页 → 确认图标为空心
7. 重新搜索 → 确认之前收藏的航班状态正确显示
8. 退出登录 → 重新登录 → 搜索 → 确认收藏状态仍然保持

- [ ] **Step 3: 记录验证结果**

在 plan 文件末尾标记每个检查点通过/失败。
