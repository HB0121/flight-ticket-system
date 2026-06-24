# 航班收藏功能完善 — 设计方案

日期: 2026-06-24 | 状态: 待审批

## 背景

后端收藏 API 已完整实现（`POST/GET/DELETE /api/me/favorites`），前端收藏列表页可查看和移除收藏。但前端 **缺少"添加收藏"的入口** — `addFavorite()` 在 `profileApi.js` 中已定义却从未被任何组件调用。用户无法从航班搜索结果中收藏航班。

## 设计决策（已确认）

| 决策 | 选择 |
|------|------|
| 收藏按钮位置 | **两处都要**：表格每行 + 详情卡片 |
| 搜索结果显示收藏状态 | **需要**：已收藏航班显示实心星 |
| 图标样式 | **星形 ⭐**（Element Plus `Star` / `StarFilled`）|
| 数据持久性 | 收藏存 MySQL，重启/重登不丢失 |

---

## 一、数据库：补充 init.sql

`infra/mysql/init.sql` 缺少 `favorite` 表（目前仅靠 `DatabaseInitializer` 运行时创建），补入以保持规范一致。

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

---

## 二、后端：航班查询返回收藏状态

### 修改 Flight 记录

新增两个字段，Jackson 序列化时 `null` 值不输出（向后兼容）：

```java
public record Flight(
    // ... 现有 13 个字段不变 ...
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean isFavorited,    // 新增：当前用户是否已收藏
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Long favoriteId         // 新增：收藏记录 ID（取消时用）
) {}
```

### 修改 FlightRepository.search()

新增重载方法，接受 `userId` 参数，使用 LEFT JOIN 一次查询完成：

```sql
SELECT f.*,
       fav.id AS favorite_id,
       CASE WHEN fav.id IS NOT NULL THEN true ELSE false END AS is_favorited
FROM flight f
LEFT JOIN favorite fav ON fav.flight_id = f.id AND fav.user_id = ?
WHERE ...
ORDER BY price ASC, depart_time ASC
```

RowMapper 将 `favorite_id` 和 `is_favorited` 映射到 `Flight` 记录的新字段。

### 修改 FlightController.search()

将 `user.id()` 传给 `FlightRepository.search()`，搜索结果自动带收藏状态。

---

## 三、前端：新增 FavoriteButton 组件

**文件:** `frontend/src/shared/components/FavoriteButton.vue`

```
Props:
  flightId     Number (必填)
  isFavorited  Boolean (默认 false)
  favoriteId   Number|null (默认 null)

Emits:
  toggled(isFavorited: boolean, favoriteId: number|null)
```

**外观:**
- 未收藏→ `el-button(link)` + 空心灰色 `Star`
- 已收藏→ `el-button(link)` + 实心金色 `StarFilled`
- 点击中→ loading spinner

**交互（组件内自治，直接调 API）：**
1. 未收藏点击 → `addFavorite({flightId})` → 成功 emit `toggled(true, favId)` + 提示"已收藏"
2. 已收藏点击 → `removeFavorite(favoriteId)` → 成功 emit `toggled(false, null)` + 提示"已取消收藏"
3. 请求中按钮 loading，避免连点
4. 失败 → ElMessage error，状态不回滚

---

## 四、前端：修改 FlightTable.vue

### 新增 Props

```js
favoriteStatusMap: Map<flightId, { isFavorited, favoriteId }>
```

### 新增操作列

在表格最右侧增加一列（width: 50, align: center），每行渲染 `FavoriteButton`，从 `favoriteStatusMap` 获取初始状态。

### 新增 Emit

```js
emit('favorite-toggled', { flightId, isFavorited, favoriteId })
```

父组件收到后更新 `favoriteStatusMap`。

---

## 五、前端：修改 FlightDetailCard.vue

### 新增 Props

```js
isFavorited: Boolean (默认 false)
favoriteId: Number|null (默认 null)
```

### 新增收藏按钮

在卡片 header 区域，航班号右侧放置 `FavoriteButton`。

### 新增 Emit

```js
emit('favorite-toggled', { flightId, isFavorited, favoriteId })
```

---

## 六、前端：修改 FlightSearchPage.vue

### 状态管理

新增响应式数据：

```js
const favoriteStatusMap = ref(new Map())
```

### 搜索响应处理

收到航班列表后，遍历提取收藏状态填充 `favoriteStatusMap`：

```js
flights.value.forEach(f => {
  if (f.isFavorited) {
    favoriteStatusMap.value.set(f.id, { isFavorited: true, favoriteId: f.favoriteId })
  }
})
```

### 事件处理

```js
function onFavoriteToggled({ flightId, isFavorited, favoriteId }) {
  if (isFavorited) {
    favoriteStatusMap.value.set(flightId, { isFavorited: true, favoriteId })
  } else {
    favoriteStatusMap.value.delete(flightId)
  }
}
```

### 模板

- `FlightTable` 传入 `favoriteStatusMap`，监听 `@favorite-toggled`
- `FlightDetailCard` 传入当前选中航班的收藏状态，监听 `@favorite-toggled`

---

## 七、i18n 文案补充

新增键值对：

| Key | zh-CN | en-US |
|-----|-------|-------|
| `common.actions.favorite` | 收藏 | Favorite |
| `common.actions.unfavorite` | 取消收藏 | Unfavorite |
| `flights.favorite.added` | 已收藏 | Favorited |
| `flights.favorite.removed` | 已取消收藏 | Removed from favorites |
| `flights.favorite.failed` | 操作失败，请重试 | Operation failed, please retry |

---

## 八、涉及文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `infra/mysql/init.sql` | 修改 | 补入 `favorite` 表定义 |
| `backend/.../flight/Flight.java` | 修改 | 新增 `isFavorited`、`favoriteId` |
| `backend/.../flight/FlightRepository.java` | 修改 | 新增带 userId 的 LEFT JOIN 查询 |
| `backend/.../flight/FlightController.java` | 修改 | 传递 user.id() 给 Repository |
| `frontend/src/shared/components/FavoriteButton.vue` | **新建** | 可复用收藏按钮 |
| `frontend/src/shared/components/FlightTable.vue` | 修改 | 新增收藏列 |
| `frontend/src/shared/components/FlightDetailCard.vue` | 修改 | 新增收藏按钮 |
| `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` | 修改 | 集成收藏状态管理 |
| `frontend/src/i18n/messages/zh-CN.js` | 修改 | 补充文案 |
| `frontend/src/i18n/messages/en-US.js` | 修改 | 补充文案 |

---

## 九、后端备选方案（如不想改 Flight 记录）

如果不想修改 `Flight` 记录（考虑到它是核心领域模型），可采用 **独立 API 方案**：

- 新增 `GET /api/me/favorites/check?flightIds=1,2,3` 批量查询接口
- 前端搜索完成后，单独调此接口获取收藏状态
- 优点：`Flight` 记录不变，关注点分离
- 缺点：多一次网络请求，状态同步更复杂

**推荐仍采用主方案（LEFT JOIN）**，因为一次查询完成，无额外网络开销，且 `Flight` 记录本质上已是与前端绑定的 DTO。
