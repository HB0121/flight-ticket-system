# Flight Ticket System

综合课程设计 III 选题 `3.1.7`：基于网络爬虫的机票抓取与自动更新系统设计与实现。

第一版目标是完成稳定可演示的最小闭环：

- Scrapy 模拟爬虫解析本地样例 HTML
- MySQL 保存航班和采集任务
- SpringBoot 提供 REST API
- Vue 3 PC 端展示航班和 AI 出行建议

第二版目标是在第一版基础上增强课堂演示能力：

- Amadeus API 真实航班报价采集，保留 sample 兜底
- MySQL 保存航班当前状态和价格快照
- DeepSeek API 生成 AI 出行建议和购票时机分析，保留本地规则兜底
- Vue 3 PC 端展示采集配置、航班详情、价格趋势和 AI 报告

## 文档

- [第一版框架](./第一版框架.md)
- [第二版框架](./第二版框架.md)
- [GitHub Pages 首页](./docs/index.md)
- [原始设计文档](./2026-06-11-flight-ticket-system-design.md)

## 快速启动

需要先确保 Docker Desktop 或 Docker Engine 正常运行。

第二版外部 API 都是可选配置。未配置时系统会回退到 sample 数据和本地规则，仍可完成本地演示。

```powershell
$env:AMADEUS_CLIENT_ID="your-amadeus-client-id"
$env:AMADEUS_CLIENT_SECRET="your-amadeus-client-secret"
$env:DEEPSEEK_API_KEY="your-deepseek-api-key"
```

```powershell
docker compose -f infra/docker-compose.yml up -d mysql

cd backend
mvn spring-boot:run

cd ../frontend
npm.cmd install
npm.cmd run dev
```

前端默认访问 `http://localhost:5173`，后端默认访问 `http://localhost:8080`。

手动运行样例爬虫：

```powershell
docker compose -f infra/docker-compose.yml run --rm crawler
```

GitHub Pages：

https://hb0121.github.io/flight-ticket-system/

## 主要接口

- `POST /api/crawl/run`
- `GET /api/crawl/latest`
- `GET /api/flights?fromCity=&toCity=&date=&dataSource=`
- `GET /api/flights/{id}`
- `GET /api/flights/{id}/price-history`
- `POST /api/ai/advice`
- `POST /api/ai/timing`

## 阶段说明

第一版不做小程序、Redis、真实网页爬取、RAG 购票时机预测和公网服务器部署。

第二版加入 Amadeus 真实航班报价采集、DeepSeek AI 和轻量购票时机分析，但仍不做小程序、公网部署、Redis 和完整向量库。
