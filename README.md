# Flight Ticket System

综合课程设计 III 选题 `3.1.7`：基于网络爬虫的机票抓取与自动更新系统设计与实现。

第一版目标是完成稳定可演示的最小闭环：

- Scrapy 模拟爬虫解析本地样例 HTML
- MySQL 保存航班和采集任务
- SpringBoot 提供 REST API
- Vue 3 PC 端展示航班和 AI 出行建议

## 文档

- [第一版框架](./第一版框架.md)
- [GitHub Pages 首页](./docs/index.md)
- [原始设计文档](./2026-06-11-flight-ticket-system-design.md)

## 快速启动

需要先确保 Docker Desktop 或 Docker Engine 正常运行。

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
- `GET /api/flights?fromCity=&toCity=&date=`
- `GET /api/flights/{id}`
- `POST /api/ai/advice`

## 阶段说明

第一版不做小程序、Redis、真实网页爬取、RAG 购票时机预测和公网服务器部署。这些内容放到第二阶段扩展。
