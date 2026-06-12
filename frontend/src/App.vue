<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  Connection,
  DataAnalysis,
  Delete,
  MagicStick,
  Plus,
  Refresh,
  Search
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createConversation,
  deleteConversation,
  fetchFlights,
  fetchLatestJob,
  fetchPriceHistory,
  getMessages,
  listConversations,
  requestAdvice,
  requestTiming,
  runCrawler,
  sendMessage
} from './api/client.js'
import {
  buildCrawlerPayload,
  buildPriceChartOption,
  buildPriceHistoryChartOption,
  formatAdviceSummary,
  formatDateTime,
  formatTimingReport
} from './lib/format.js'

const activeView = ref('dashboard')
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

// Conversation state
const conversations = ref([])
const activeSessionId = ref(null)
const conversationMessages = ref([])
const conversationInput = ref('')
const sendingMessage = ref(false)

const defaultDate = () => {
  const date = new Date()
  date.setDate(date.getDate() + 7)
  return date.toISOString().slice(0, 10)
}

const collectionForm = ref({
  source: 'sample',
  fromCity: '上海',
  toCity: '北京',
  date: defaultDate(),
  adults: 1,
  maxResults: 5
})

const filters = ref({
  fromCity: '上海',
  toCity: '北京',
  date: collectionForm.value.date,
  dataSource: ''
})
const adviceInput = ref(`${collectionForm.value.date} 上海到北京，预算1200元`)
const timingInput = ref(`${collectionForm.value.date} 上海到北京，预算1200元，什么时候买更合适？`)
const adviceResult = ref(null)
const timingResult = ref(null)

const totalFlights = computed(() => flights.value.length)
const lowestPrice = computed(() => {
  if (!flights.value.length) {
    return '-'
  }
  return Math.min(...flights.value.map(flight => Number(flight.price)))
})
const chartOption = computed(() => buildPriceChartOption(flights.value))
const historyOption = computed(() => buildPriceHistoryChartOption(priceHistory.value))
const timingChartOption = computed(() => {
  const history = timingResult.value?.history
  return buildPriceHistoryChartOption(history || [])
})
const adviceText = computed(() => formatAdviceSummary(adviceResult.value))
const timingText = computed(() => formatTimingReport(timingResult.value))

async function loadLatestJob() {
  try {
    latestJob.value = await fetchLatestJob()
    apiOnline.value = true
  } catch {
    apiOnline.value = false
    latestJob.value = {
      status: 'OFFLINE',
      source: '-',
      successCount: 0,
      failedCount: 0,
      rejectedCount: 0,
      errorMessage: '后端未连接'
    }
  }
}

async function loadFlights() {
  loadingFlights.value = true
  try {
    flights.value = await fetchFlights({
      fromCity: filters.value.fromCity || undefined,
      toCity: filters.value.toCity || undefined,
      date: filters.value.date || undefined,
      dataSource: filters.value.dataSource || undefined
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

async function selectFlight(row) {
  selectedFlight.value = row
  await loadPriceHistory(row)
}

async function loadPriceHistory(row = selectedFlight.value) {
  if (!row?.id) {
    priceHistory.value = []
    return
  }
  loadingHistory.value = true
  try {
    priceHistory.value = await fetchPriceHistory(row.id)
    await nextTick()
    renderHistoryChart()
  } catch {
    priceHistory.value = []
  } finally {
    loadingHistory.value = false
  }
}

async function handleRunCrawler() {
  runningCrawler.value = true
  try {
    const payload = buildCrawlerPayload(collectionForm.value)
    latestJob.value = await runCrawler(payload)
    apiOnline.value = true
    filters.value.fromCity = collectionForm.value.fromCity
    filters.value.toCity = collectionForm.value.toCity
    filters.value.date = collectionForm.value.date
    filters.value.dataSource = payload.source === 'amadeus' ? 'amadeus' : ''
    await loadFlights()
    ElMessage.success('采集任务已完成')
  } catch (error) {
    if (!error.response) apiOnline.value = false
    ElMessage.error(error?.message || '采集任务失败')
  } finally {
    runningCrawler.value = false
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
  } finally {
    askingAdvice.value = false
  }
}

async function handleTiming() {
  askingTiming.value = true
  try {
    timingResult.value = await requestTiming(timingInput.value)
    apiOnline.value = true
    await nextTick()
    renderTimingChart()
  } catch (error) {
    if (!error.response) apiOnline.value = false
    ElMessage.error(error?.message || '购票时机分析失败')
  } finally {
    askingTiming.value = false
  }
}

// Conversation functions
async function loadConversations() {
  try {
    conversations.value = await listConversations()
    apiOnline.value = true
  } catch {
    conversations.value = []
  }
}

async function createNewSession() {
  try {
    const session = await createConversation('出行咨询')
    conversations.value.unshift(session)
    activeSessionId.value = session.id
    conversationMessages.value = []
    conversationInput.value = ''
  } catch {
    ElMessage.error('创建对话失败')
  }
}

async function selectSession(sessionId) {
  activeSessionId.value = sessionId
  try {
    conversationMessages.value = await getMessages(sessionId)
  } catch {
    conversationMessages.value = []
  }
}

async function sendConversationMessage() {
  const msg = conversationInput.value.trim()
  if (!msg || !activeSessionId.value) return
  sendingMessage.value = true
  try {
    const response = await sendMessage(activeSessionId.value, msg)
    conversationMessages.value = await getMessages(activeSessionId.value)
    conversationInput.value = ''
  } catch (error) {
    ElMessage.error(error?.message || '发送消息失败')
  } finally {
    sendingMessage.value = false
  }
}

async function deleteCurrentSession() {
  if (!activeSessionId.value) return
  try {
    await ElMessageBox.confirm('确认删除此对话？', '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteConversation(activeSessionId.value)
    activeSessionId.value = null
    conversationMessages.value = []
    await loadConversations()
    ElMessage.success('对话已删除')
  } catch {
    // user cancelled
  }
}

function renderPriceChart() {
  if (!priceChartEl.value) return
  if (!priceChart) {
    priceChart = echarts.init(priceChartEl.value)
  }
  priceChart.setOption(chartOption.value, true)
}

function renderHistoryChart() {
  if (!historyChartEl.value) return
  if (!historyChart) {
    historyChart = echarts.init(historyChartEl.value)
  }
  historyChart.setOption(historyOption.value, true)
}

function renderTimingChart() {
  if (!timingChartEl.value) return
  if (!timingChart) {
    timingChart = echarts.init(timingChartEl.value)
  }
  timingChart.setOption(timingChartOption.value, true)
}

function switchView(view) {
  activeView.value = view
  nextTick(() => {
    renderPriceChart()
    renderHistoryChart()
    if (view === 'ai') {
      renderTimingChart()
      loadConversations()
    }
  })
}

function resizeCharts() {
  priceChart?.resize()
  historyChart?.resize()
  timingChart?.resize()
}

watch(chartOption, () => nextTick(renderPriceChart))
watch(historyOption, () => nextTick(renderHistoryChart))
watch(timingChartOption, () => nextTick(renderTimingChart))

onMounted(async () => {
  await Promise.all([loadLatestJob(), loadFlights()])
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  priceChart?.dispose()
  historyChart?.dispose()
  timingChart?.dispose()
})
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">综合课程设计 III · 3.1.7 · 第二版</p>
        <h1>机票抓取与自动更新系统</h1>
      </div>
      <el-button :loading="runningCrawler" type="primary" :icon="Refresh" @click="handleRunCrawler">
        采集
      </el-button>
    </header>

    <el-alert
      v-if="!apiOnline"
      title="后端服务未连接"
      type="error"
      description="请确认后端已启动 (localhost:8080) 并重试"
      show-icon
      closable
      @close="apiOnline = true"
    />

    <nav class="nav-tabs" aria-label="主功能">
      <button :class="{ active: activeView === 'dashboard' }" @click="switchView('dashboard')">
        <el-icon><DataAnalysis /></el-icon>
        总览
      </button>
      <button :class="{ active: activeView === 'flights' }" @click="switchView('flights')">
        <el-icon><Search /></el-icon>
        航班
      </button>
      <button :class="{ active: activeView === 'ai' }" @click="switchView('ai')">
        <el-icon><MagicStick /></el-icon>
        AI
      </button>
    </nav>

    <section v-if="activeView === 'dashboard'" class="view-grid">
      <article class="metric-panel">
        <span>航班数量</span>
        <strong>{{ totalFlights }}</strong>
      </article>
      <article class="metric-panel accent">
        <span>最低票价</span>
        <strong>{{ lowestPrice === '-' ? '-' : `${lowestPrice} 元` }}</strong>
      </article>
      <article class="metric-panel">
        <span>采集状态</span>
        <strong>{{ latestJob?.status || 'EMPTY' }}</strong>
      </article>
      <article class="metric-panel">
        <span>采集来源</span>
        <strong>{{ latestJob?.source || '-' }}</strong>
      </article>
      <section class="wide-panel">
        <div class="panel-title">
          <el-icon><Connection /></el-icon>
          最近采集
        </div>
        <dl class="status-list">
          <div>
            <dt>开始时间</dt>
            <dd>{{ formatDateTime(latestJob?.startedAt) }}</dd>
          </div>
          <div>
            <dt>结束时间</dt>
            <dd>{{ formatDateTime(latestJob?.finishedAt) }}</dd>
          </div>
          <div>
            <dt>成功数量</dt>
            <dd>{{ latestJob?.successCount ?? 0 }}</dd>
          </div>
          <div>
            <dt>失败数量</dt>
            <dd>{{ latestJob?.failedCount ?? 0 }}</dd>
          </div>
          <div v-if="latestJob?.rejectedCount !== undefined">
            <dt>校验拒绝</dt>
            <dd>{{ latestJob?.rejectedCount ?? 0 }}</dd>
          </div>
        </dl>
        <p class="job-params">{{ latestJob?.requestParams || latestJob?.errorMessage || '暂无采集参数' }}</p>
      </section>
    </section>

    <section v-show="activeView === 'flights'" class="work-panel">
      <div class="panel-title">采集配置</div>
      <div class="collect-grid">
        <el-select v-model="collectionForm.source" placeholder="数据源">
          <el-option label="样例兜底" value="sample" />
          <el-option label="Amadeus" value="amadeus" />
        </el-select>
        <el-input v-model="collectionForm.fromCity" placeholder="出发城市" clearable />
        <el-input v-model="collectionForm.toCity" placeholder="到达城市" clearable />
        <el-date-picker v-model="collectionForm.date" value-format="YYYY-MM-DD" type="date" placeholder="出发日期" />
        <el-input-number v-model="collectionForm.adults" :min="1" :max="9" controls-position="right" />
        <el-input-number v-model="collectionForm.maxResults" :min="1" :max="20" controls-position="right" />
        <el-button :loading="runningCrawler" type="primary" :icon="Refresh" @click="handleRunCrawler">
          执行采集
        </el-button>
      </div>

      <div class="panel-title section-title">航班查询</div>
      <div class="filters">
        <el-input v-model="filters.fromCity" placeholder="出发城市" clearable />
        <el-input v-model="filters.toCity" placeholder="到达城市" clearable />
        <el-date-picker v-model="filters.date" value-format="YYYY-MM-DD" type="date" placeholder="出发日期" />
        <el-select v-model="filters.dataSource" placeholder="数据源" clearable>
          <el-option label="全部" value="" />
          <el-option label="样例" value="sample" />
          <el-option label="Amadeus" value="amadeus" />
        </el-select>
        <el-button :loading="loadingFlights" type="primary" :icon="Search" @click="loadFlights">
          查询
        </el-button>
      </div>

      <div ref="priceChartEl" class="price-chart"></div>
      <el-table
        :data="flights"
        v-loading="loadingFlights"
        height="360"
        empty-text="暂无航班数据"
        highlight-current-row
        @row-click="selectFlight"
      >
        <el-table-column prop="flightNo" label="航班号" width="110" sortable />
        <el-table-column prop="airlineName" label="航司" width="120" sortable />
        <el-table-column label="航线" min-width="150">
          <template #default="{ row }">{{ row.fromCity }} → {{ row.toCity }}</template>
        </el-table-column>
        <el-table-column label="机场" min-width="190">
          <template #default="{ row }">{{ row.fromAirport }} → {{ row.toAirport }}</template>
        </el-table-column>
        <el-table-column label="起飞" min-width="150" sortable sort-by="departTime">
          <template #default="{ row }">{{ formatDateTime(row.departTime) }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100" sortable>
          <template #default="{ row }">{{ row.price }} 元</template>
        </el-table-column>
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

    <section v-if="activeView === 'ai'" class="work-panel ai-panel">
      <!-- Conversation -->
      <section class="ai-block conversation-block">
        <div class="panel-title">
          <el-icon><Connection /></el-icon>
          智能对话
        </div>
        <div class="conversation-layout">
          <aside class="conversation-sidebar">
            <el-button :icon="Plus" type="primary" size="small" @click="createNewSession" style="width:100%;margin-bottom:8px">
              新对话
            </el-button>
            <div
              v-for="session in conversations"
              :key="session.id"
              :class="['session-item', { active: session.id === activeSessionId }]"
              @click="selectSession(session.id)"
            >
              {{ session.title }}
            </div>
            <p v-if="!conversations.length" class="empty-copy" style="font-size:12px">暂无对话</p>
          </aside>
          <div class="conversation-main">
            <div class="message-list" v-if="activeSessionId">
              <div
                v-for="msg in conversationMessages"
                :key="msg.id"
                :class="['message-bubble', msg.role === 'user' ? 'user' : 'assistant']"
              >
                <div class="bubble-content">{{ msg.content }}</div>
                <div class="bubble-time">{{ formatDateTime(msg.createdAt) }}</div>
              </div>
              <p v-if="!conversationMessages.length" class="empty-copy">发送第一条消息开始对话</p>
            </div>
            <p v-else class="empty-copy" style="padding: 40px">选择或创建一个对话开始</p>
            <div class="message-input" v-if="activeSessionId">
              <el-input
                v-model="conversationInput"
                placeholder="输入出行需求..."
                @keyup.enter="sendConversationMessage"
                clearable
              />
              <el-button
                :loading="sendingMessage"
                type="primary"
                :icon="MagicStick"
                @click="sendConversationMessage"
                :disabled="!conversationInput.trim()"
              >
                发送
              </el-button>
              <el-button :icon="Delete" @click="deleteCurrentSession" type="danger" text>
                删除对话
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <el-divider />

      <!-- AI Advice -->
      <section class="ai-block">
        <div class="panel-title">
          <el-icon><MagicStick /></el-icon>
          AI 出行建议
        </div>
        <el-input
          v-model="adviceInput"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请输入出行需求"
        />
        <div class="actions">
          <el-button :loading="askingAdvice" type="primary" :icon="MagicStick" @click="handleAdvice">
            生成建议
          </el-button>
        </div>
        <section v-if="adviceResult" class="advice-output">
          <h2>推荐结果</h2>
          <p>{{ adviceText }}</p>
        </section>
      </section>

      <el-divider />

      <!-- AI Timing -->
      <section class="ai-block">
        <div class="panel-title">
          <el-icon><DataAnalysis /></el-icon>
          购票时机分析
        </div>
        <el-input
          v-model="timingInput"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请输入购票时机问题"
        />
        <div class="actions">
          <el-button :loading="askingTiming" type="primary" :icon="DataAnalysis" @click="handleTiming">
            分析时机
          </el-button>
        </div>
        <section v-if="timingResult" class="advice-output">
          <h2>分析报告</h2>
          <p>{{ timingText }}</p>
          <div v-if="timingResult.history && timingResult.history.length" class="timing-chart-area">
            <div class="panel-title" style="margin-top:12px">价格趋势</div>
            <div ref="timingChartEl" class="history-chart"></div>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>
