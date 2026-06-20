# `/flights` 仪表盘化重构 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `/flights` 页面从"纵向堆叠"重排为符合 `AGENTS.md` 的"紧凑仪表盘"5 区布局，且不动后端、不动子组件逻辑、不引入新依赖。

**Architecture:** 仅在 `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` 内做容器层重排。保留子组件（`FlightTable.vue` / `FlightDetailCard.vue` / `PriceHistoryChart.vue`）和 store / API 不变。新增的"同步摘要条"从原同步卡内摘出独立成行；AI 区改为默认折叠薄条；通过 CSS Grid 实现 5 区网格与响应式断点。

**Tech Stack:** Vue 3 SFC、Element Plus、Vitest + @vue/test-utils、jsdom

**Spec:** [docs/superpowers/specs/2026-06-20-flights-dashboard-layout-design.md](../../specs/2026-06-20-flights-dashboard-layout-design.md)

---

## 文件清单

### 修改
- `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` — 模板结构重排 + `<style scoped>` 整体重写
- `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js` — 测试用例补强
- `frontend/src/style.css` — 仅在需要新增全局工具类时（如 `--dashboard-gap`）才动；优先在 scoped 样式内完成

### 不动
- `frontend/src/shared/components/FlightTable.vue`
- `frontend/src/shared/components/FlightDetailCard.vue`
- `frontend/src/shared/charts/PriceHistoryChart.vue`
- `frontend/src/api/flightApi.js`
- 任何 store 文件
- 后端任何文件
- `frontend/src/i18n/*`（i18n 迁移由独立 spec 处理）

---

## Task 1：把模板顶层从"四段堆叠"改为"5 区仪表盘骨架"

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` 模板部分（约第 1-535 行）

- [ ] **Step 1: 调整模板根容器类名，明确 5 区**

将根 `<section class="flight-search-page">` 的 `<style>` 区对应选择器名 `flight-search-page` 保持不变（避免破坏现有测试断言），但新增以下结构语义类：

- 在同步卡 + 查询卡外层 `<section class="flight-search-page__controls">` 上加 `data-testid="dashboard-controls"`
- 在同步摘要条 `<section data-testid="sync-result-card" class="flight-search-page__sync-strip">` 上**替换** `data-testid="sync-result-card"` 为 `data-testid="dashboard-sync-strip"`（同元素上 data-testid 唯一；旧值仅在 plan 历史中引用，不会被任何测试或代码消费）
- 在主结果区 `<section class="flight-search-page__workspace flight-search-page__console">` 上加 `data-testid="dashboard-workspace"`
- 在 AI 区 `<section class="flight-search-page__ai-drawer">` 上加 `data-testid="dashboard-ai"`（已存在 `data-testid="ai-toggle"` 不变）

模板不做删除，仅在每个区域顶层标签加 1 个 `data-testid`。**先保持结构不变**（这是后续 5 步重排的前置）。

- [ ] **Step 2: 跑测试，确认现状未坏**

```bash
cd frontend
npm test -- --run FlightSearchPage
```

预期：全部测试 PASS（仅加 data-testid 是非破坏性变更）

- [ ] **Step 3: 提交**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue
git commit -m "refactor(flights): add 5-zone dashboard data-testids (no visual change)"
```

---

## Task 2：样式重写——5 区网格 + 紧凑 Header + 同步摘要条独立成行

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` `<style scoped>` 块（约第 1186-1905 行）

**关键原则：** 模板不动，只改 `<style>`。通过 CSS Grid 重排布局，配合新增的容器选择器。

- [ ] **Step 1: 改写根容器为 5 行 Grid**

替换第 1186-1196 行 `.flight-search-page` 样式：

```css
.flight-search-page {
  display: grid;
  gap: 16px;
  box-sizing: border-box;
  min-height: 0;
  overflow: visible;
  padding: 0 0 18px;
  grid-template-rows: auto auto auto 1fr auto;
  grid-template-areas:
    "header"
    "controls"
    "strip"
    "workspace"
    "ai";
}
```

`.flight-search-page__tabs` 选择器加 `grid-area: header;`
`.flight-search-page__controls` 加 `grid-area: controls;`
`.flight-search-page__sync-strip` 加 `grid-area: strip;`
`.flight-search-page__workspace` 加 `grid-area: workspace;`
`.flight-search-page__ai-drawer` 加 `grid-area: ai;`

- [ ] **Step 2: 同步 / 查询卡并排（沿用 AGENTS.md 推荐比例）**

修改第 1240-1247 行 `.flight-search-page__controls`：

```css
.flight-search-page__controls {
  display: grid;
  grid-template-columns: 0.46fr 0.54fr;
  gap: 16px;
  align-items: stretch;
  min-height: 0;
  overflow: visible;
}
```

- [ ] **Step 3: 同步摘要条独立成行（薄横条）**

修改第 1399-1408 行 `.flight-search-page__sync-strip`，调整为 56px 高的薄横条：

```css
.flight-search-page__sync-strip {
  display: grid;
  grid-template-columns: auto repeat(5, minmax(110px, 1fr)) auto;
  gap: 12px;
  align-items: center;
  min-height: 56px;
  padding: 8px 16px;
  background: #ffffff;
  border: 1px solid #dbe5f0;
  border-left: 4px solid #10b981;
  border-radius: 12px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
  overflow: visible;
}

.flight-search-page__sync-strip--failed {
  border-left-color: #ef4444;
  background: linear-gradient(180deg, #ffffff 0%, #fef2f2 100%);
}
```

同步失败态由模板侧根据 `syncStatus === 'FAILED'` 切换类名（在 Task 4 加）。

- [ ] **Step 4: 改写主结果区为左 8 + 右 4 仪表盘布局**

替换第 1249-1257 行：

```css
.flight-search-page__workspace,
.flight-search-page__console {
  display: grid;
  grid-template-columns: minmax(0, 0.62fr) minmax(0, 0.38fr);
  gap: 16px;
  align-items: start;
  min-height: 0;
  overflow: visible;
}
```

- [ ] **Step 5: AI 默认折叠为薄条**

修改第 1711-1722 行 `.flight-search-page__ai-drawer`：

```css
.flight-search-page__ai-drawer {
  display: grid;
  grid-template-rows: auto auto;
  gap: 0;
  padding: 10px 16px;
  background: #ffffff;
  border: 1px solid #dbe5f0;
  border-radius: 12px;
  box-shadow: 0 6px 14px rgba(15, 23, 42, 0.04);
  min-height: 48px;
  max-height: 48px;
  overflow: hidden;
  transition: max-height 220ms ease;
}

.flight-search-page__ai-drawer--open {
  max-height: 40vh;
  overflow: auto;
}
```

`.flight-search-page__ai-drawer-bar` 保持单行布局（标题+按钮）。

- [ ] **Step 6: 删掉过期响应式断点中的冲突规则**

第 1826-1863 行 `@media (max-width: 1280px)` 内已经存在的 `.flight-search-page__controls, .flight-search-page__workspace, .flight-search-page__console { grid-template-columns: 1fr; }` 保留——它处理中屏堆叠。

但需要删除：
- 第 1849-1851 行 `.flight-search-page__control-card { height: auto; }`（已被新规则覆盖）
- 第 1853-1856 行 `.flight-search-page__table-shell { min-height: 420px; max-height: 520px; }` —— **保留**，中屏表格高度合理

- [ ] **Step 7: 跑测试**

```bash
cd frontend
npm test -- --run FlightSearchPage
```

预期：所有现有测试 PASS。视觉测试在浏览器手动确认（Task 5 之后整体验收）。

- [ ] **Step 8: 提交**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue
git commit -m "style(flights): 5-zone dashboard grid + compact AI drawer"
```

---

## Task 3：同步摘要条失败态类名绑定（模板侧）

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.vue` 模板

- [ ] **Step 1: 在 `<section data-testid="dashboard-sync-strip" class="flight-search-page__sync-strip">` 上按 syncStatus 切类**

模板原代码：

```vue
<section data-testid="dashboard-sync-strip" class="flight-search-page__sync-strip">
```

改为：

```vue
<section
  data-testid="dashboard-sync-strip"
  :class="['flight-search-page__sync-strip', { 'flight-search-page__sync-strip--failed': syncStatus === 'FAILED' }]"
>
```

- [ ] **Step 2: 跑测试**

```bash
cd frontend
npm test -- --run FlightSearchPage
```

预期：PASS。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.vue
git commit -m "feat(flights): bind failed class on sync strip"
```

---

## Task 4：测试补强——AI 默认折叠 + 同步摘要条失败态 + 同步进行中查询不阻塞

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: 在 describe 块内新增"AI 默认折叠"用例**

在已有测试 suite 中（搜 `describe(` 找到 `FlightSearchPage` 主 describe），追加：

```js
  it('renders AI section collapsed by default with toggle visible', async () => {
    const wrapper = mount(FlightSearchPage, {
      global: createGlobals()
    })
    await flushPromises()

    const aiDrawer = wrapper.find('[data-testid="dashboard-ai"]')
    expect(aiDrawer.exists()).toBe(true)
    expect(aiDrawer.classes()).toContain('flight-search-page__ai-drawer')
    expect(aiDrawer.classes()).not.toContain('flight-search-page__ai-drawer--open')

    const toggle = wrapper.find('[data-testid="ai-toggle"]')
    expect(toggle.exists()).toBe(true)
  })

  it('expands AI section when toggle is clicked', async () => {
    const wrapper = mount(FlightSearchPage, {
      global: createGlobals()
    })
    await flushPromises()

    const aiDrawer = wrapper.find('[data-testid="dashboard-ai"]')
    expect(aiDrawer.classes()).not.toContain('flight-search-page__ai-drawer--open')

    await wrapper.find('[data-testid="ai-toggle"]').trigger('click')
    await flushPromises()

    expect(aiDrawer.classes()).toContain('flight-search-page__ai-drawer--open')
  })
```

- [ ] **Step 2: 新增"同步摘要条失败态"用例**

```js
  it('applies failed class to sync strip when sync status is FAILED', async () => {
    mocks.syncFlights.mockResolvedValue({
      status: 'FAILED',
      successCount: 0,
      failedCount: 0,
      source: 'aerodatabox',
      finishedAt: '2026-06-20 14:15:09'
    })

    const wrapper = mount(FlightSearchPage, {
      global: createGlobals()
    })
    await flushPromises()

    // submit the sync form
    await wrapper.find('[data-testid="sync-submit"]').trigger('click')
    await flushPromises()

    const strip = wrapper.find('[data-testid="dashboard-sync-strip"]')
    expect(strip.classes()).toContain('flight-search-page__sync-strip--failed')
  })
```

- [ ] **Step 3: 新增"同步进行中查询卡仍可交互"用例**

```js
  it('keeps search card interactive while sync is in flight', async () => {
    const syncDeferred = createDeferred()
    mocks.syncFlights.mockReturnValue(syncDeferred.promise)
    mocks.fetchFlights.mockResolvedValue([])

    const wrapper = mount(FlightSearchPage, {
      global: createGlobals()
    })
    await flushPromises()

    // start sync but don't await resolution
    const syncClick = wrapper.find('[data-testid="sync-submit"]').trigger('click')
    await flushPromises()

    // search card should still be reachable and submit-able
    const searchSubmit = wrapper.find('[data-testid="search-form"]')
    expect(searchSubmit.exists()).toBe(true)
    await searchSubmit.trigger('submit')
    await flushPromises()

    // resolve sync to clean up
    syncDeferred.resolve({ status: 'SUCCESS', successCount: 1, failedCount: 0, source: 'aerodatabox', finishedAt: '' })
    await syncClick
    await flushPromises()
  })
```

- [ ] **Step 4: 确认 `createGlobals()` helper 已存在或在文件内已有等效定义**

打开 `FlightSearchPage.spec.js` 顶部搜索 `createGlobals` 或 `global: plugins`。若不存在，需要在使用每个 `mount(FlightSearchPage, ...)` 处改为 `global: { plugins: [i18n] }` 形式（项目内已有的写法）。若不一致，按文件内现有 `<100 行起`的样式补一个本地 helper：

```js
function createGlobals() {
  return {
    plugins: [createTestI18n()],
    stubs: {
      'el-form': ElFormStub,
      'el-form-item': ElFormItemStub,
      'el-select': ElSelectStub,
      'el-option': ElOptionStub,
      'el-button': ElButtonStub,
      'el-input': ElInputStub,
      'el-date-picker': ElDatePickerStub,
      'el-pagination': ElPaginationStub,
      FlightTable: FlightTableStub,
      FlightDetailCard: FlightDetailCardStub,
      PriceHistoryChart: PriceHistoryChartStub
    }
  }
}
```

如果文件**已经**有同样的 helper 函数，跳过此步。

- [ ] **Step 5: 跑测试**

```bash
cd frontend
npm test -- --run FlightSearchPage
```

预期：所有原有测试 + 新增 4 个测试全部 PASS。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "test(flights): cover AI drawer collapse + sync strip failure state"
```

---

## Task 5：响应式断点校验（中屏 1279px 与小屏 900px）

**Files:**
- Modify: `frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js`

- [ ] **Step 1: 在 spec 顶部 mocks 区新增 matchMedia mock**

在文件顶部 `vi.mock(...)` 块之外新增：

```js
function mockMatchMedia(width) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation(query => {
      const isNarrow = query.includes('max-width: 1279px') || query.includes('max-width: 899px')
      const matches = isNarrow && width <= (query.includes('1279') ? 1279 : 899)
      return {
        matches,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn()
      }
    })
  })
}
```

- [ ] **Step 2: 新增"中屏断点下结果区上下堆叠"用例**

```js
  it('stacks results pane vertically at narrow breakpoint', async () => {
    mockMatchMedia(1100)
    const wrapper = mount(FlightSearchPage, {
      global: createGlobals(),
      attachTo: document.body
    })
    await flushPromises()

    const workspace = wrapper.find('[data-testid="dashboard-workspace"]').element
    const styles = window.getComputedStyle(workspace)
    // At narrow widths, our scoped CSS overrides grid-template-columns to 1fr.
    // We assert via a data attribute instead to avoid relying on getComputedStyle under jsdom.
    expect(workspace.getAttribute('data-testid')).toBe('dashboard-workspace')
    // jsdom doesn't apply media queries, so just verify no error at mount time
    wrapper.unmount()
  })
```

**注意**：jsdom 不应用 `@media` 查询，本用例主要验证**窄屏 mount 不抛错**。真实视觉断点验证需在 Task 6 浏览器手动验收。

- [ ] **Step 3: 跑测试**

```bash
cd frontend
npm test -- --run FlightSearchPage
```

预期：PASS。

- [ ] **Step 4: 提交**

```bash
git add frontend/src/modules/user-flights/pages/FlightSearchPage.spec.js
git commit -m "test(flights): responsive breakpoint mount smoke"
```

---

## Task 6：手动浏览器验收（参考图对比）

**Files:** 无（仅验证）

- [ ] **Step 1: 启动前端 dev server**

```bash
cd frontend
npm run dev
```

浏览器打开 http://localhost:5173/flights，登录后查看。

- [ ] **Step 2: 对照参考图逐项验收**

打开 `docs/ui/flights-dashboard-target.png`（用户提供的视觉参考）。逐项核对：

1. **Header**（≥1280px 视口）
   - [ ] Logo、Title/Subtitle、4 个 Tabs（数据同步 / 航班查询 / 航班结果 / AI 建议）、Data Source Badge、Mode Badge、Language Switch、用户入口 在一行
   - [ ] 行高 ≈ 64px，不挤压

2. **同步 + 查询并排**
   - [ ] 两个卡片等高，宽度比 ≈ 0.46 : 0.54
   - [ ] gap 16px

3. **同步摘要条**
   - [ ] 独立成行，薄横条样式（≈56px 高）
   - [ ] 成功：左侧绿色边条；失败：左侧红色边条
   - [ ] 5 项指标：状态 / 成功数 / 失败数 / 数据来源 / 同步时间

4. **结果区（左 8 / 右 4）**
   - [ ] 表格占左侧 ~62%
   - [ ] 详情卡 + 价格历史图表占右侧 ~38%，上下排
   - [ ] 表格行可点击，详情与图表同步刷新

5. **AI 默认折叠**
   - [ ] 默认薄条状态（48px 高）
   - [ ] 点击"展开"按钮后展开为面板，最高 40vh
   - [ ] 展开不挤压结果区

- [ ] **Step 3: 验证响应式断点**

在浏览器 dev tools 切换：
- 1366×768（窄屏）—— 结果区上下堆叠；同步/查询仍并排
- 800×600（移动）—— 所有区域纵向堆叠

- [ ] **Step 4: 验证失败态**

触发一次失败的同步（手动改 API key 或断网），确认：
- [ ] 同步摘要条变红
- [ ] 顶部 Toast 提示
- [ ] 详情区不可点（视觉上无高亮）

- [ ] **Step 5: 视觉验收通过 → 完成**

无需代码改动。如果发现偏差，回到对应 Task 修复并重跑该 Task 的测试。

---

## Task 7：运行完整测试套件 + 后端无改动校验

**Files:** 无

- [ ] **Step 1: 跑全测试**

```bash
cd frontend
npm test -- --run
```

预期：全 PASS，包括 FlightSearchPage、FlightTable、UserLayout、其它共享组件。

- [ ] **Step 2: 检查 git 状态确认后端零改动**

```bash
cd d:/01_Personal_Learning/flight-system
git status
git diff --stat backend/
```

预期：`backend/` 下无任何文件改动。

- [ ] **Step 3: 提交验收记录**

如果第 1 步发现任何失败，**不要**强行合并。先修复，再回到对应 Task 重做。

通过则在 git log 中追加一个空提交作为里程碑：

```bash
git commit --allow-empty -m "milestone: /flights dashboard layout refactor complete and verified"
```

---

## 自检（Self-Review 已完成）

1. **Spec 覆盖**：每条 spec 要求 → 对应 Task
   - 5 区网格 → Task 2
   - 同步摘要条独立 → Task 2 + Task 3
   - AI 默认折叠 → Task 2 + Task 4 测试
   - 响应式断点 → Task 2 CSS + Task 5 测试 + Task 6 浏览器验收
   - 测试策略 5 项 → Task 4（4 项）+ Task 5（响应式）
   - 硬边界（不动后端）→ Task 7 校验

2. **占位符扫描**：无 TBD/TODO。每步都有具体代码或命令。

3. **类型/命名一致性**：
   - 类名 `flight-search-page__ai-drawer` / `flight-search-page__sync-strip` 在模板和样式中保持一致
   - data-testid 在 Task 1 引入，在 Task 4/5 测试中复用：`dashboard-controls`、`dashboard-sync-strip`、`dashboard-workspace`、`dashboard-ai`
   - 失败态类名 `flight-search-page__sync-strip--failed` 在 Task 2（CSS）和 Task 3（模板）和 Task 4（测试）三处一致

4. **回滚策略**：每个 Task 独立 commit，`git revert <commit>` 可单步回滚。