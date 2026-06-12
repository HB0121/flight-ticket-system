<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  DataAnalysis,
  MagicStick,
  Search,
  User,
  SwitchButton,
  Key,
  UserFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  fetchFlights,
  fetchLatestJob,
  fetchPriceHistory,
  getMe,
  login,
  logout,
  register,
  requestAdvice,
  requestTiming,
  runCrawler
} from './api/client.js'
import {
  buildCrawlerPayload,
  buildPriceChartOption,
  buildPriceHistoryChartOption,
  formatAdviceSummary,
  formatDateTime,
  formatTimingReport
} from './lib/format.js'

// === Auth state ===
const isLoggedIn = ref(false)
const currentUser = ref(null)
const authMode = ref('login') // 'login' | 'register'
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '' })
const authLoading = ref(false)

// === App state ===
const activeView = ref('flights')
const flights = ref([])
const latestJob = ref(null)
const selectedFlight = ref(null)
const priceHistory = ref([])
const loadingFlights = ref(false)
const loadingHistory = ref(false)
const runningCrawler = ref(false)
const askingAdvice = ref(false)
const askingTiming = ref(false)
const apiOnline = ref(true)
const priceChartEl = ref(null)
const historyChartEl = ref(null)
const timingChartEl = ref(null)
let priceChart = null
let historyChart = null
let timingChart = null

const defaultDate = () => {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  return date.toISOString().slice(0, 10)
}

// 航班查询表单（合并采集+查询为一个表单）
const searchForm = ref({
  source: 'sample',
  fromCity: '上海',
  toCity: '北京',
  date: defaultDate(),
})
const adviceInput = ref(`${searchForm.value.date} 上海到北京，预算1200元`)
const timingInput = ref(`${searchForm.value.date} 上海到北京，预算1200元，什么时候买更合适？`)
const adviceResult = ref(null)
const timingResult = ref(null)

const totalFlights = computed(() => flights.value.length)
const lowestPrice = computed(() => {
  if (!flights.value.length) return '-'
  return Math.min(...flights.value.map(flight => Number(flight.price)))
})
const chartOption = computed(() => buildPriceChartOption(flights.value))
const historyOption = computed(() => buildPriceHistoryChartOption(priceHistory.value))
const timingChartOption = computed(() => buildPriceHistoryChartOption(timingResult.value?.history || []))
const adviceText = computed(() => formatAdviceSummary(adviceResult.value))
const timingText = computed(() => formatTimingReport(timingResult.value))

// === Auth functions ===
async function checkAuth() {
  const token = localStorage.getItem('token')
  if (!token) return
  try {
    currentUser.value = await getMe()
    isLoggedIn.value = true
  } catch {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }
}

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  authLoading.value = true
  try {
    const data = await login(loginForm.value.username, loginForm.value.password)
    localStorage.setItem('token', data.token)
    currentUser.value = data
    isLoggedIn.value = true
    ElMessage.success('登录成功')
    await initApp()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    authLoading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.value.username || !registerForm.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  authLoading.value = true
  try {
    const data = await register(
      registerForm.value.username,
      registerForm.value.password
    )
    localStorage.setItem('token', data.token)
    currentUser.value = data
    isLoggedIn.value = true
    ElMessage.success('注册成功')
    await initApp()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '注册失败')
  } finally {
    authLoading.value = false
  }
}

async function handleLogout() {
  try { await logout() } catch { /* ignore */ }
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  isLoggedIn.value = false
  currentUser.value = null
}

function switchAuthMode() {
  authMode.value = authMode.value === 'login' ? 'register' : 'login'
}

// === Data functions ===
async function initApp() {
  await Promise.all([loadLatestJob(), loadFlights()])
}

async function loadLatestJob() {
  try {
    latestJob.value = await fetchLatestJob()
    apiOnline.value = true
  } catch {
    apiOnline.value = false
    latestJob.value = { status: 'OFFLINE', source: '-', successCount: 0, failedCount: 0, rejectedCount: 0, errorMessage: '后端未连接' }
  }
}

async function loadFlights() {
  loadingFlights.value = true
  try {
    flights.value = await fetchFlights({
      fromCity: searchForm.value.fromCity || undefined,
      toCity: searchForm.value.toCity || undefined,
      date: searchForm.value.date || undefined
    })
    apiOnline.value = true
    await nextTick()
    renderPriceChart()
    if (flights.value.length) {
      const stillSelected = flights.value.find(flight => flight.id === selectedFlight.value?.id)
      await selectFlight(stillSelected || flights.value[0])
    } else {
      selectedFlight.value = null
      priceHistory.value = []
      renderHistoryChart()
    }
  } catch {
    apiOnline.value = false
    flights.value = []
    selectedFlight.value = null
    priceHistory.value = []
  } finally {
    loadingFlights.value = false
  }
}

async function selectFlight(row) { selectedFlight.value = row; await loadPriceHistory(row) }

async function loadPriceHistory(row = selectedFlight.value) {
  if (!row?.id) { priceHistory.value = []; return }
  loadingHistory.value = true
  try {
    priceHistory.value = await fetchPriceHistory(row.id)
    await nextTick(); renderHistoryChart()
  } catch {
    priceHistory.value = []
  } finally { loadingHistory.value = false }
}

// 点击"查询航班"：先触发爬虫采集 → 再查询数据库刷新页面
async function handleSearch() {
  const source = searchForm.value.source
  runningCrawler.value = true
  let crawlSuccess = false
  try {
    const payload = buildCrawlerPayload(searchForm.value)
    latestJob.value = await runCrawler(payload)       // ① 触发爬虫
    apiOnline.value = true
    // 即使 HTTP 200，status 可能仍是 FAILED（爬虫命令执行失败）
    crawlSuccess = latestJob.value?.status === 'SUCCESS'
  } catch (error) {
    if (!error.response) apiOnline.value = false
  } finally { runningCrawler.value = false }

  await loadFlights()               // ② 从数据库查询并渲染

  // ③ 根据采集结果和已有数据显示反馈
  if (crawlSuccess) {
    ElMessage.success(source === 'amadeus'
      ? 'Amadeus 真实票价采集成功，数据已更新'
      : '本地样例数据采集成功')
  } else if (flights.value.length > 0) {
    ElMessage.warning(source === 'amadeus'
      ? 'Amadeus 采集失败（请检查 API 密钥和 Docker 环境），正在显示已有数据'
      : '本地样例采集未成功（需 Docker 环境），正在显示已缓存数据')
  } else {
    ElMessage.error(source === 'amadeus'
      ? 'Amadeus 采集失败且数据库无数据，请检查 API 密钥和网络连接'
      : '本地样例采集失败且数据库无数据，请确认后端和 Docker 已启动')
  }
}

async function handleAdvice() {
  askingAdvice.value = true
  try {
    adviceResult.value = await requestAdvice(adviceInput.value)
    apiOnline.value = true
  } catch (error) {
    if (!error.response) apiOnline.value = false
    ElMessage.error(error?.message || 'AI 建议请求失败')
  } finally { askingAdvice.value = false }
}

async function handleTiming() {
  askingTiming.value = true
  try {
    timingResult.value = await requestTiming(timingInput.value)
    apiOnline.value = true
    // v-if 渲染需要时间，延迟确保 DOM 元素已创建
    setTimeout(() => renderTimingChart(), 150)
  } catch (error) {
    if (!error.response) apiOnline.value = false
    ElMessage.error(error?.message || '购票时机分析失败')
  } finally { askingTiming.value = false }
}

// Chart functions
function renderPriceChart() {
  if (!priceChartEl.value) return
  if (!priceChart) priceChart = echarts.init(priceChartEl.value)
  priceChart.setOption(chartOption.value, true)
  priceChart.resize()  // 强制重绘，避免容器尺寸变化导致挤压
}

function renderHistoryChart() {
  if (!historyChartEl.value) return
  if (!historyChart) historyChart = echarts.init(historyChartEl.value)
  historyChart.setOption(historyOption.value, true)
  historyChart.resize()
}

function renderTimingChart() {
  if (!timingChartEl.value) return
  if (!timingChart) timingChart = echarts.init(timingChartEl.value)
  timingChart.setOption(timingChartOption.value, true)
  timingChart.resize()
}

function switchView(view) {
  activeView.value = view
  nextTick(() => {
    // 切换到航班视图时，延迟一下确保 DOM 渲染完成再初始化图表
    setTimeout(() => {
      renderPriceChart(); renderHistoryChart()
    }, 100)
    if (view === 'ai') { renderTimingChart() }
  })
}

function resizeCharts() { priceChart?.resize(); historyChart?.resize(); timingChart?.resize() }

watch(chartOption, () => nextTick(renderPriceChart))
watch(historyOption, () => nextTick(renderHistoryChart))
watch(timingChartOption, () => nextTick(renderTimingChart))

// Listen for forced logout from interceptor
function onAuthLogout() { isLoggedIn.value = false; currentUser.value = null }
onMounted(async () => {
  window.addEventListener('auth:logout', onAuthLogout)
  await checkAuth()
  if (isLoggedIn.value) {
    await initApp()
    window.addEventListener('resize', resizeCharts)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('auth:logout', onAuthLogout)
  window.removeEventListener('resize', resizeCharts)
  priceChart?.dispose(); historyChart?.dispose(); timingChart?.dispose()
})
</script>

<template>
  <!-- ============== LOGIN VIEW ============== -->
  <main v-if="!isLoggedIn" class="auth-page">
    <div class="auth-card">
      <h1>机票抓取与自动更新系统</h1>

      <!-- Login Form -->
      <template v-if="authMode === 'login'">
        <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" size="large" class="auth-input" @keyup.enter="handleLogin" />
        <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Key" size="large" class="auth-input" show-password @keyup.enter="handleLogin" />
        <el-button :loading="authLoading" type="primary" size="large" class="auth-btn" @click="handleLogin">
          登录
        </el-button>
        <el-button text type="primary" @click="switchAuthMode">还没有账号？立即注册</el-button>
      </template>

      <!-- Register Form -->
      <template v-else>
        <el-input v-model="registerForm.username" placeholder="用户名" :prefix-icon="UserFilled" size="large" class="auth-input" @keyup.enter="handleRegister" />
        <el-input v-model="registerForm.password" type="password" placeholder="密码" :prefix-icon="Key" size="large" class="auth-input" show-password @keyup.enter="handleRegister" />
        <el-button :loading="authLoading" type="primary" size="large" class="auth-btn" @click="handleRegister">
          注册
        </el-button>
        <el-button text type="primary" @click="switchAuthMode">已有账号？立即登录</el-button>
      </template>
    </div>
  </main>

  <!-- ============== APP VIEW ============== -->
  <main v-else class="app-shell">
    <header class="topbar">
      <div>
        <h1>机票抓取与自动更新系统</h1>
      </div>
      <div class="topbar-actions">
        <span class="user-info">
          <el-icon><User /></el-icon>
          {{ currentUser?.nickname || currentUser?.username }}
        </span>
        <el-button :icon="SwitchButton" @click="handleLogout">登出</el-button>
      </div>
    </header>

    <el-alert v-if="!apiOnline" title="后端服务未连接" type="error" description="请确认后端已启动 (localhost:8080) 并重试" show-icon closable @close="apiOnline = true" />

    <nav class="nav-tabs" aria-label="主功能">
      <button :class="{ active: activeView === 'flights' }" @click="switchView('flights')">
        <el-icon><Search /></el-icon> 航班
      </button>
      <button :class="{ active: activeView === 'ai' }" @click="switchView('ai')">
        <el-icon><MagicStick /></el-icon> AI
      </button>
    </nav>

    <!-- Dashboard -->
    <!-- Flights -->
    <section v-show="activeView === 'flights'" class="work-panel">
      <div class="panel-title">航班查询</div>
      <div class="search-grid">
        <div class="field-with-label">
          <span class="field-label">数据来源</span>
          <el-select v-model="searchForm.source">
            <el-option label="本地样例" value="sample" />
            <el-option label="真实票价" value="amadeus" />
          </el-select>
        </div>
        <div class="field-with-label">
          <span class="field-label">出发城市</span>
          <el-input v-model="searchForm.fromCity" placeholder="如：上海" clearable />
        </div>
        <div class="field-with-label">
          <span class="field-label">到达城市</span>
          <el-input v-model="searchForm.toCity" placeholder="如：北京" clearable />
        </div>
        <div class="field-with-label">
          <span class="field-label">出发日期</span>
          <el-date-picker v-model="searchForm.date" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" />
        </div>
        <div class="field-with-label" style="justify-content:flex-end">
          <el-button :loading="runningCrawler || loadingFlights" type="primary" :icon="Search" @click="handleSearch">查询航班</el-button>
        </div>
      </div>

      <div ref="priceChartEl" class="price-chart"></div>
      <el-table :data="flights" v-loading="loadingFlights" height="360" empty-text="暂无航班数据" highlight-current-row @row-click="selectFlight">
        <el-table-column prop="flightNo" label="航班号" width="110" sortable />
        <el-table-column prop="airlineName" label="航司" width="120" sortable />
        <el-table-column label="航线" min-width="150"><template #default="{ row }">{{ row.fromCity }} → {{ row.toCity }}</template></el-table-column>
        <el-table-column label="机场" min-width="190"><template #default="{ row }">{{ row.fromAirport }} → {{ row.toAirport }}</template></el-table-column>
        <el-table-column label="起飞" min-width="150" sortable sort-by="departTime"><template #default="{ row }">{{ formatDateTime(row.departTime) }}</template></el-table-column>
        <el-table-column prop="price" label="价格" width="100" sortable><template #default="{ row }">{{ row.price }} 元</template></el-table-column>
        <el-table-column prop="seatsLeft" label="余票" width="80" sortable />
        <el-table-column prop="dataSource" label="来源" width="100" />
      </el-table>

      <section class="detail-panel">
        <div>
          <div class="panel-title">航班详情</div>
          <dl v-if="selectedFlight" class="detail-list">
            <div><dt>航班号</dt><dd>{{ selectedFlight.flightNo }}</dd></div>
            <div><dt>航司</dt><dd>{{ selectedFlight.airlineName }}</dd></div>
            <div><dt>航线</dt><dd>{{ selectedFlight.fromCity }} → {{ selectedFlight.toCity }}</dd></div>
            <div><dt>当前价格</dt><dd>{{ selectedFlight.price }} 元</dd></div>
            <div><dt>余票</dt><dd>{{ selectedFlight.seatsLeft }}</dd></div>
            <div><dt>最近采集</dt><dd>{{ formatDateTime(selectedFlight.collectedAt) }}</dd></div>
          </dl>
          <p v-else class="empty-copy">选择一条航班查看详情。</p>
        </div>
        <div v-loading="loadingHistory">
          <div class="panel-title">价格趋势</div>
          <div ref="historyChartEl" class="history-chart"></div>
        </div>
      </section>
    </section>

    <!-- AI -->
    <section v-if="activeView === 'ai'" class="work-panel ai-panel">
      <section class="ai-block">
        <div class="panel-title"><el-icon><MagicStick /></el-icon> AI 出行建议</div>
        <el-input v-model="adviceInput" type="textarea" :rows="3" resize="none" placeholder="请输入出行需求" />
        <div class="actions"><el-button :loading="askingAdvice" type="primary" :icon="MagicStick" @click="handleAdvice">生成建议</el-button></div>
        <section v-if="adviceResult" class="advice-output"><h2>推荐结果</h2><p>{{ adviceText }}</p></section>
      </section>

      <el-divider />
      <section class="ai-block">
        <div class="panel-title"><el-icon><DataAnalysis /></el-icon> 购票时机分析</div>
        <el-input v-model="timingInput" type="textarea" :rows="3" resize="none" placeholder="请输入购票时机问题" />
        <div class="actions"><el-button :loading="askingTiming" type="primary" :icon="DataAnalysis" @click="handleTiming">分析时机</el-button></div>
        <section v-if="timingResult" class="advice-output">
          <h2>分析报告</h2><p>{{ timingText }}</p>
          <div v-if="timingResult.history && timingResult.history.length" class="timing-chart-area">
            <div class="panel-title" style="margin-top:12px">价格趋势</div>
            <div ref="timingChartEl" class="history-chart"></div>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>
