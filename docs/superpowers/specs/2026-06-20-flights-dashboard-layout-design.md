# `/flights` 仪表盘化重构设计

- **日期**: 2026-06-20
- **作者**: 设计协作产出
- **状态**: 已批准，待落计划
- **关联**: `AGENTS.md`（硬边界）、`docs/ui/flights-dashboard-target.png`（视觉参考）、`docs/superpowers/plans/2026-06-19-flights-ux-enhancement.md`（既有 UX 增强计划）

## 1. 背景与目标

`/flights` 是用户的主查询页。`AGENTS.md` 已经给出明确目标——把它从"纵向堆叠页面"重排为"紧凑仪表盘"。当前实现与该目标存在差距：

1. 顶部 Header 较松散，未形成仪表盘头部
2. 同步卡 + 查询卡上下堆叠，未并排
3. 同步结果未抽离为摘要条，与卡片混在一起
4. 表格 / 详情 / 图表三个区域之间的视觉层次和密度未达仪表盘标准
5. AI 建议区默认展开，占位过大，挤压主结果区

本次重构**仅重排 `FlightSearchPage.vue` 及其相关 CSS**，目标是把现有子组件（`FlightTable.vue` / `FlightDetailCard.vue` / `PriceHistoryChart.vue`）的容器改造成符合 AGENTS.md 的 6 区仪表盘布局。

## 2. 硬边界（来自 `AGENTS.md`）

以下行为**禁止**出现在本次实现中：

- 不改后端 API（Spring Boot 任何 controller / service / repository）
- 不改 crawler、AeroDataBox 同步逻辑
- 不改 AI 推荐的业务逻辑
- 不暴露 / 硬编码 RapidAPI、DeepSeek 密钥
- 不以 mock-only 替换任何已有功能（登录、收藏、历史、同步、查询、详情、价格历史、AI 建议都必须保留）
- 不删除登录、收藏、历史、同步、查询、详情、价格历史、AI 建议功能

## 3. 范围

### 3.1 In-Scope

- `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` 的顶层布局与样式
- `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js` 测试用例补强
- `frontend/src/style.css` 与 `frontend/src/layouts/UserLayout.vue` 中**仅与本次布局相关**的工具类（如确需新增）
- `frontend/src/shared/components/FlightTable.spec.js`（如需补强选中行 → 详情同步）

### 3.2 Out-of-Scope

- `FlightTable.vue` / `FlightDetailCard.vue` / `PriceHistoryChart.vue` 内部 props 与渲染逻辑
- 国际化（i18n）迁移——本次保持现状中文，文案不动；i18n 迁移由独立 spec 处理
- 新增功能（如深色模式、新筛选维度等）
- 其它页面（`/favorites` / `/history` / Admin）

## 4. 设计

### 4.1 整体网格

页面外层使用 12 列响应式网格。每个区域按 `AGENTS.md` 推荐比例与参考图实际密度确定：

| 区域 | 桌面列宽 | 桌面行高 | 内容 |
|------|---------|---------|------|
| ① Header | 12 | 64px | Logo · 标题/副标题 · 导航 Tabs · Data Source Badge · Mode Badge · Language Switch · 用户入口 |
| ② 同步卡 / 查询卡 | 同步 0.46fr / 查询 0.54fr | auto | 两卡片并排，gap=16px |
| ③ 同步摘要条 | 12 | 56px | 最近一次同步状态/成功数/失败数/数据来源/同步时间 |
| ④ 主结果区 | 表格 8 / 详情+图表 4 | auto | 左 FlightTable；右 FlightDetailCard（顶）+ PriceHistoryChart（底） |
| ⑤ AI 建议 | 12 | 默认折叠，展开约 40vh | 默认折叠为薄条，展开为面板 |

### 4.2 关键改动点

**(1) 顶部 Header 紧凑化**
- 单行高 64px；Logo、Title/Subtitle、导航 Tabs、Data Source / Mode Badge、Language、User 全部在同一行
- 删除参考图中不存在的独立大标题 banner
- 复用 `UserLayout.vue` 中已存在的导航 Tabs 与品牌区，仅调整密度

**(2) 同步 + 查询并排**
- CSS：`grid-template-columns: 0.46fr 0.54fr; gap: 16px;`（沿用 `AGENTS.md` 推荐值）
- 两个卡片同高：标题行 + 表单行 + 操作行
- 同步卡片内"上次同步"提示从主标题下方移到摘要条，避免重复

**(3) 同步摘要条（从同步卡片中拆出）**
- 5 列指标：状态徽章 / 成功数 / 失败数 / 数据来源 / 同步时间
- 失败时整条左边框变红 + 浅红背景；成功时薄绿条
- 数据来自 `syncStatus`/`lastSync` store state，仅做展现，不改 store 结构

**(4) 结果区：左 8 + 右 4**
- 左侧：`<FlightTable>`，列宽固定，sticky header
- 右侧：`<FlightDetailCard>` 顶部 + `<PriceHistoryChart>` 底部，二者上下排
- 选中行：左侧高亮，右侧详情与图表同步刷新（已有行为，仅视觉确认）

**(5) AI 建议折叠**
- 默认收起为一个薄条：图标 + 标题 + "展开"按钮
- 展开时高度上限约 40vh，超出滚动
- 不再挤压结果区——展开后结果区不缩小，仅 AI 区向下推

### 4.3 响应式

| 断点 | 行为 |
|------|------|
| ≥1280px | 完整 12 列布局，详情区与图表区上下排（保持桌面仪表盘） |
| 960–1279px | 同步/查询仍并排，结果区改为上下堆叠（详情在上，图在下） |
| <960px | 所有区域纵向堆叠，Header Tabs 改为横向滚动 |

### 4.4 状态：空 / 加载 / 错误

- **同步进行中**：同步卡片内出现进度环，不阻塞查询卡片
- **查询无结果**：表格区域显示空态插画 + "试试调整筛选条件"
- **加载中**：表格骨架屏（6 行），详情区显示占位卡
- **同步失败**：摘要条变红 + Toast，详情区不可点
- **AI 折叠条**：加载时按钮显示 spinner
- **网络错误**：表格上方出现轻量错误条，保留旧数据可见

### 4.5 数据流与状态

本次仅做展现重排，**不动 store / API**。所需的响应式状态：

- `selectedFlight` — 来自 `FlightSearchPage.vue` 已有 ref，驱动详情/图表
- `lastSync` / `syncStatus` — 来自现有 store
- `aiOpen` — 新增 ref，控制 AI 折叠条状态
- `windowWidth` — 用 `window.matchMedia('(max-width: 1279px)')` 监听断点，控制响应式类（不引入额外依赖）

## 5. 测试策略

在 `FlightSearchPage.spec.js` 与 `FlightTable.spec.js` 已有的基础上补强：

1. 同步摘要条根据同步结果切换 success/fail 样式
2. AI 建议折叠/展开切换只影响 AI 区高度，不挤压结果区
3. 响应式断点切换时 DOM 顺序正确（CSS-only，验证类名即可）
4. 表格选中行与详情/图表同步刷新（已有断言可加强）
5. 同步进行中查询卡片仍可交互（不被阻塞）

不引入新依赖，不动 store / API。

## 6. 风险与回滚

- **风险面**：仅 `FlightSearchPage.vue` + 其 CSS；外加可能的 `style.css` 工具类
- **回滚**：`git revert` 单个 commit 即可，不影响其它模块
- **测试覆盖**：spec 文件先于代码，确保重构前后行为一致

## 7. 不做的事（YAGNI）

- 不抽 PageHeader / SectionCard / MetricStrip 等布局组件（未来如 `/favorites` 也需要类似骨架，再升级到 Layout 层）
- 不动 i18n、不引入新依赖
- 不调整颜色 / 主题 token（沿用现有 `style.css`）
- 不增加新的筛选维度 / 列
- 不修改后端任何代码

## 8. 验收清单

- [ ] `/flights` 在 1280px+ 视口下呈现仪表盘布局：5 个区域比例与 `AGENTS.md` 一致
- [ ] 同步摘要条独立成行，失败态明显
- [ ] AI 建议默认折叠，展开不挤压结果区
- [ ] 响应式断点切换时无横向滚动条
- [ ] 所有原有功能仍可用：登录态、同步、查询、详情、价格历史、AI 建议
- [ ] `FlightSearchPage.spec.js` 与 `FlightTable.spec.js` 全绿
- [ ] 后端无任何改动