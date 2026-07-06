# 机票抓取与自动更新系统

这是一个课程项目，用于演示航班数据采集、自动更新、查询展示和用户收藏管理。当前项目主线是“航班数据采集与查询系统”，AI/chat 相关代码仍保留在仓库中，但不是当前核心交付功能。

## 项目简介

系统由前端、后端、爬虫和数据库四部分组成：

- 前端提供用户登录、航班查询、收藏、搜索历史和管理端页面。
- 后端提供认证、航班、收藏、搜索历史、采集任务和数据源状态等 REST API。
- 爬虫负责从外部数据源采集航班数据，并写入 MySQL。
- MySQL 作为共享数据库，保存航班、采集任务、用户、收藏和价格快照等数据。

## 技术架构

- `frontend`：Vue 3 + Vite + Element Plus + ECharts + vue-router + vue-i18n
- `backend`：Spring Boot 3.3.5 + Java 17 + Maven + JDBC
- `crawler`：Scrapy + Python + PyMySQL
- `infra`：Docker Compose + MySQL 8.4

后端没有使用 JPA/Hibernate，主要通过手写 SQL 和 JDBC Repository 访问数据库。认证使用自定义 token 机制，不依赖 Spring Security 的完整认证链路。

## 目录结构

```text
backend/    Spring Boot 后端服务
frontend/   Vue 3 前端应用
crawler/    Scrapy 航班数据采集程序
infra/      MySQL Docker Compose 和初始化 SQL
```

## 核心业务流程

1. 使用 Docker Compose 启动 MySQL。
2. 后端启动时连接 MySQL，并根据 `infra/mysql/init.sql` 初始化表结构。
3. 管理端或命令行触发爬虫任务。
4. 爬虫采集航班数据并直接写入 MySQL。
5. 后端读取数据库并向前端提供航班查询、收藏、历史记录等接口。
6. 前端通过 Axios 调用后端接口，完成页面展示和用户操作。

## 功能模块

用户侧：

- 注册、登录、退出
- 航班查询
- 航班详情
- 价格历史
- 航班收藏
- 搜索历史

管理侧：

- 创建采集任务
- 查看采集任务记录
- 查看数据源状态
- 手动同步指定机场和日期的航班数据

保留模块：

- AI/chat 相关接口和数据库表仍在代码中，但不是当前主业务流程。

## 快速启动

环境要求：

- Docker Desktop 或 Docker Engine
- Java 17
- Maven
- Node.js
- Python 3.11 或可用的 Docker 爬虫环境

启动 MySQL：

```powershell
docker compose -f infra/docker-compose.yml up -d mysql
```

启动后端：

```powershell
cd backend
mvn spring-boot:run
```

启动前端：

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

默认访问地址：

- 前端：http://localhost:5173
- 后端：http://localhost:8080
- MySQL：localhost:3306

手动运行爬虫：

```powershell
docker compose -f infra/docker-compose.yml run --rm crawler
```

## 环境变量

航班数据源：

- `AERODATABOX_KEY`：AeroDataBox API key
- `AERODATABOX_BASE_URL`：AeroDataBox API 地址，默认 `https://aerodatabox.p.rapidapi.com`
- `AERODATABOX_HOST`：AeroDataBox RapidAPI host，默认 `aerodatabox.p.rapidapi.com`

保留 AI 模块：

- `DEEPSEEK_API_KEY`：DeepSeek API key
- `DEEPSEEK_BASE_URL`：DeepSeek API 地址
- `DEEPSEEK_MODEL`：DeepSeek 模型名称

没有配置真实外部 API key 时，采集任务可能失败或没有有效数据。

## 常用接口

认证：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`

航班：

- `GET /api/flights`
- `GET /api/flights/{id}`
- `GET /api/flights/{id}/price-history`

用户：

- `GET /api/me/favorites`
- `POST /api/me/favorites`
- `DELETE /api/me/favorites/{favoriteId}`
- `GET /api/me/search-history`

管理：

- `POST /api/admin/crawl-jobs`
- `GET /api/admin/crawl-jobs`
- `GET /api/admin/data-sources/status`
- `POST /api/admin/flights/sync`

兼容/保留接口：

- `POST /api/crawl/run`
- `GET /api/crawl/latest`
- `/api/ai/**`

## 测试命令

后端：

```powershell
cd backend
mvn test
```

前端：

```powershell
cd frontend
npm.cmd test
```

爬虫：

```powershell
cd crawler
pytest
```

## 清理与重新生成

以下内容不是源码，清理后可以重新生成：

- `frontend/node_modules/`：运行 `npm.cmd install` 重新安装。
- `backend/target/`：运行 Maven 命令后重新生成。
- 运行日志目录或日志文件：重新启动服务后会再次生成。

注意：不要删除 `.git`、`.gitignore`、`backend/`、`frontend/`、`crawler/`、`infra/` 等项目必要文件。

## 说明

当前项目重点是航班采集、查询、收藏和管理端采集任务。AI/chat 相关代码属于保留能力，后续可以继续清理或独立整理，但不应影响当前主业务流程。
