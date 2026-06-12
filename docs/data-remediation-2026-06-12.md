# 数据端整改记录

日期：2026-06-12

## 背景

本次排查确认，当前航班查询接口不是实时请求外部航班接口，而是读取 MySQL 中已采集的数据。采集链路支持两类来源：

- `sample`：读取本地样例 HTML，作为演示兜底数据。
- `amadeus`：调用 Amadeus Flight Offers API 获取真实航班报价；接口不可用、未配置密钥或返回空数据时，会回退到 `sample`。

整改前的问题是：系统只记录用户请求的数据源，无法明确区分“请求的是 Amadeus”与“实际写入的是 sample”。因此当 Amadeus 回退 sample 后，前端仍按 `amadeus` 过滤，容易出现“采集完成但列表为空”的现象。

## 整改内容

### 1. 采集任务增加实际数据源字段

`crawl_job` 新增字段：

- `actual_source`：实际写入的数据源，例如 `amadeus`、`sample` 或 `mixed`。
- `fallback_reason`：发生回退时的原因说明。

相关位置：

- `infra/mysql/init.sql`
- `backend/src/main/java/com/example/flight/config/DatabaseInitializer.java`
- `backend/src/test/resources/schema.sql`

### 2. 爬虫记录 Amadeus 回退情况

Amadeus 爬虫现在会在以下情况下标记实际来源为 `sample`：

- 未配置 Amadeus API Key 或 Secret。
- Amadeus 返回空结果。
- Amadeus 请求异常。

同时会记录回退原因，方便后端和前端展示。

相关位置：

- `crawler/flight_crawler/spiders/amadeus_flights.py`
- `crawler/flight_crawler/spiders/sample_flights.py`
- `crawler/flight_crawler/pipelines.py`

### 3. 后端 API 返回实际来源和回退原因

`CrawlJob` 返回结构增加：

- `actualSource`
- `fallbackReason`

`CrawlRepository` 会从 `crawl_job` 中读取这些字段，采集启动失败时也会填充实际来源，避免前端拿到不完整状态。

相关位置：

- `backend/src/main/java/com/example/flight/crawl/CrawlJob.java`
- `backend/src/main/java/com/example/flight/crawl/CrawlRepository.java`
- `backend/src/main/java/com/example/flight/crawl/CrawlController.java`
- `backend/src/main/java/com/example/flight/crawl/CrawlService.java`

### 4. 前端按实际来源刷新查询

前端执行采集后，不再只根据用户选择的数据源设置筛选条件，而是优先使用后端返回的 `actualSource`。

例如用户选择 `amadeus`，但接口失败并回退到 `sample` 时，前端会按 `sample` 查询，从而展示实际写入的数据。

同时，前端会展示 `fallbackReason`，让用户知道本次采集为什么回退。

相关位置：

- `frontend/src/App.vue`
- `frontend/src/lib/format.js`
- `frontend/src/lib/format.spec.js`

## 新增测试

新增或更新了以下测试：

- 爬虫测试：验证 Amadeus 回退 sample 时会记录 `actual_source` 和回退原因。
- 后端测试：验证 `CrawlRepository` 能正确读取 `actualSource` 和 `fallbackReason`。
- 前端测试：验证采集完成后筛选条件会使用后端返回的实际数据源。

相关位置：

- `crawler/tests/test_parser.py`
- `backend/src/test/java/com/example/flight/crawl/CrawlRepositoryTest.java`
- `frontend/src/lib/format.spec.js`

## 验证结果

已执行以下验证：

```powershell
cd backend
mvn.cmd test
```

结果：10 个后端测试通过。

```powershell
cd crawler
.\.venv\Scripts\python.exe -m pytest
```

结果：4 个爬虫测试通过。

```powershell
cd frontend
npm.cmd test -- --run
```

结果：6 个前端测试通过。

```powershell
cd frontend
npm.cmd run build
```

结果：前端构建成功。构建过程中存在依赖包 Rolldown 注解和 chunk size 警告，但退出码为 0。

## 运行注意事项

本次修改涉及 crawler 镜像内的 Python 代码。代码提交后，如果本地继续通过 Docker Compose 运行采集器，需要重新构建 crawler 镜像：

```powershell
docker compose -f infra\docker-compose.yml build crawler
```

如果构建时卡在 `python:3.11-slim`，说明 Docker Hub 访问或代理配置有问题，需要先解决基础镜像拉取问题，再重新构建。

