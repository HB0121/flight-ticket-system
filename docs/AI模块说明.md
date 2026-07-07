# AI 模块说明

## 模块定位

`backend/src/main/java/com/example/flight/ai` 是系统的辅助推荐模块，不是航班抓取、入库、查询、收藏等核心流程的前置依赖。

当前系统即使不配置外部大模型 API，也可以通过本地规则生成出行建议。配置 `DEEPSEEK_API_KEY` 后，系统会优先调用 DeepSeek 生成更自然的中文建议；调用失败或未配置时，自动回退到本地规则。

## 当前建议讲法

答辩时可以把 AI 模块定位为：

> 在已有航班数据的基础上，系统支持自然语言出行需求解析，并结合本地航班候选结果生成购票建议。AI 能力是增强项，核心数据仍来自本地 MySQL 航班表。

重点讲 `/api/ai/advice` 这一条主线即可。

## 核心调用链

前端当前主要使用：

```text
FlightSearchPage.vue
  -> requestAdvice(message)
  -> POST /api/ai/advice
  -> AdviceController.advice()
  -> AdviceService.generate()
  -> TravelIntentParser.parse()
  -> FlightSearchPort.search()
  -> AiTextClient.generate()
  -> AdviceResponse
```

处理过程：

1. 用户输入自然语言，例如“下周五从重庆去北京出差，预算1200元，希望上午到”。
2. `TravelIntentParser` 解析出出发城市、目的城市、日期、预算和时间偏好。
3. `AdviceService` 根据解析结果查询本地航班数据。
4. 系统过滤无票、超预算、不匹配路线的航班。
5. 如果有时间偏好，优先选择满足出发或到达时间窗口的航班。
6. 从候选航班中选择价格最低且时间更早的航班作为推荐。
7. 如果 DeepSeek 可用，用候选航班和用户需求生成中文建议。
8. 如果 DeepSeek 不可用，使用本地规则生成兜底建议。

## 类职责

| 类 | 作用 |
| --- | --- |
| `AdviceController` | AI 模块 REST 入口，提供建议、购票时机和会话接口。 |
| `AdviceService` | 出行建议核心业务，负责查本地航班、筛选候选、生成推荐和自动同步兜底。 |
| `TravelIntentParser` | 将自然语言解析为结构化出行意图。 |
| `ParsedTravelIntent` | 内部解析结果，包含城市、日期、预算、时间偏好。 |
| `TimePreference` | 时间偏好枚举：凌晨、上午、下午、晚上。 |
| `AdviceRequest` | `/api/ai/advice` 请求体，兼容 `message` 和 `query` 字段。 |
| `AdviceResponse` | 出行建议返回体，包含摘要、解析意图、推荐航班、候选航班和同步状态。 |
| `AdviceIntentView` | 面向前端展示的解析意图视图。 |
| `AiTextClient` | AI 文本生成接口。 |
| `DeepSeekTextClient` | DeepSeek API 实现，未配置 key 或调用失败时返回空结果。 |
| `TimingService` | 购票时机分析扩展能力，结合价格快照、历史规律和节假日生成时机建议。 |
| `TimingRequest` / `TimingResponse` | `/api/ai/timing` 的请求和返回结构。 |
| `PriceContextRepository` | 读写 `price_context` 表，提供价格规律上下文。 |
| `PriceContextSeedService` | 应用启动时为 `price_context` 表补充演示用规则数据。 |
| `HolidayProximityCalculator` | 判断出发日期和主要节假日的距离，用于购票时机提示。 |
| `ConversationRepository` | 多轮对话会话和消息的 JDBC 存储。 |
| `ConversationRequest` / `ConversationSession` / `ConversationMessage` / `SendMessageRequest` | 多轮对话接口的数据结构。 |

## 接口使用状态

| 接口 | 当前状态 | 说明 |
| --- | --- | --- |
| `POST /api/ai/advice` | 已接入前端主页面 | 当前最适合展示和答辩的 AI 能力。 |
| `POST /api/ai/timing` | 后端已实现，前端未重点接入 | 可作为扩展能力说明。 |
| `/api/ai/conversations/**` | 后端已实现，前端未重点接入 | 支持多轮对话存储，可作为预留能力说明。 |

## 和主系统的关系

AI 模块不会直接写入航班主数据。它主要读取：

- `flight`：查询候选航班。
- `flight_price_snapshot`：购票时机分析时读取历史价格。
- `price_context`：读取历史规律文本。
- `conversation_session` / `conversation_message`：保存多轮对话。

`AdviceService` 在本地没有候选航班时，会尝试通过 `FlightSyncPort` 自动同步出发机场和日期对应的航班，然后重新查询本地数据。这仍然遵循“先采集入库，再基于本地数据推荐”的思路。

## 运行条件

不配置 DeepSeek 时：

```text
用户输入 -> 本地解析 -> 本地航班查询 -> 本地规则建议
```

配置 DeepSeek 时：

```text
用户输入 -> 本地解析 -> 本地航班查询 -> DeepSeek 生成建议
```

需要的环境变量：

```text
DEEPSEEK_API_KEY
```

如果没有配置或调用失败，系统会自动回退，不影响主流程。

## 推荐答辩重点

建议重点讲：

1. AI 模块不是凭空编造航班，而是基于本地数据库的真实候选航班。
2. 自然语言解析是本地规则实现，能提取城市、日期、预算和时间偏好。
3. DeepSeek 是增强文本表达能力，不是核心数据来源。
4. 未配置 DeepSeek 时系统仍可运行，说明模块具备降级能力。
5. 会话和购票时机分析属于扩展能力，当前核心展示以出行建议为主。
