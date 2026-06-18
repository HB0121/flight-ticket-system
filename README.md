# Flight System

当前仓库已按 phase-1 目标重构为“航班数据采集与查询系统”，主线不再以 AI 交互为中心。

## Phase 1 架构

- `frontend`：Vue 3 路由化前端，区分普通用户与管理员视图
- `backend`：Spring Boot 统一业务边界，负责认证、航班查询、收藏、搜索历史、管理端爬取接口
- `crawler`：独立爬虫执行器，接收采集参数并写入规范化结果

当前主流程：

- 普通用户：登录、航班查询、价格历史、收藏、搜索历史
- 管理员：爬取任务、数据源状态
- AI：保留为后续扩展能力，不属于当前一级导航，也不是 phase-1 的核心交付

## 前端路由

- `/auth`：登录 / 注册
- `/flights`：航班查询
- `/favorites`：我的收藏
- `/history`：搜索历史
- `/admin/crawl-jobs`：爬取任务
- `/admin/data-sources`：数据源状态

## 主要接口

认证：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `POST /api/auth/logout`

用户侧：

- `GET /api/flights`
- `GET /api/flights/{id}`
- `GET /api/flights/{id}/price-history`
- `GET /api/me/favorites`
- `POST /api/me/favorites`
- `DELETE /api/me/favorites/{favoriteId}`
- `GET /api/me/search-history`

管理侧：

- `POST /api/admin/crawl-jobs`
- `GET /api/admin/crawl-jobs`
- `GET /api/admin/data-sources/status`

兼容接口仍可能保留在代码中，但新前端主流程应优先使用以上边界。

## 快速启动

先确保 Docker Desktop 或 Docker Engine 可用。

如果只演示本地 fallback 流程，可以不配置外部数据源。若需要远程数据源，可按需设置：

```powershell
$env:AMADEUS_CLIENT_ID="your-amadeus-client-id"
$env:AMADEUS_CLIENT_SECRET="your-amadeus-client-secret"
```

启动数据库、后端与前端：

```powershell
docker compose -f infra/docker-compose.yml up -d mysql

cd backend
mvn spring-boot:run

cd ../frontend
npm.cmd install
npm.cmd run dev
```

- 前端默认地址：`http://localhost:5173`
- 后端默认地址：`http://localhost:8080`

手动运行爬虫：

```powershell
docker compose -f infra/docker-compose.yml run --rm crawler
```

## 数据源说明

- `sample`：内置 fallback 数据源，适合本地演示
- `amadeus`：作为远程扩展数据源保留在 phase-1 范围内

## 说明

- 当前重构目标是先把“查询系统 + 管理采集边界”做干净
- AI 相关代码可以继续保留，但不应影响当前导航、接口边界和页面叙事
- 旧 README 中关于 AI 主流程的描述已不再代表当前架构
