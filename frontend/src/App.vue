<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  Connection,
  DataAnalysis,
  MagicStick,
  Refresh,
  Search
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { fetchFlights, fetchLatestJob, requestAdvice, runCrawler } from './api/client.js'
import { buildPriceChartOption, formatAdviceSummary, formatDateTime } from './lib/format.js'

const activeView = ref('dashboard')
const flights = ref([])
const latestJob = ref(null)
const loadingFlights = ref(false)
const runningCrawler = ref(false)
const askingAdvice = ref(false)
const chartEl = ref(null)
let chart = null

const filters = ref({
  fromCity: '上海',
  toCity: '北京',
  date: '2026-06-19'
})
const adviceInput = ref('2026-06-19 上海到北京，预算1200元')
const adviceResult = ref(null)

const totalFlights = computed(() => flights.value.length)
const lowestPrice = computed(() => {
  if (!flights.value.length) {
    return '-'
  }
  return Math.min(...flights.value.map(flight => Number(flight.price)))
})
const chartOption = computed(() => buildPriceChartOption(flights.value))
const adviceText = computed(() => formatAdviceSummary(adviceResult.value))

async function loadLatestJob() {
  try {
    latestJob.value = await fetchLatestJob()
  } catch {
    latestJob.value = {
      status: 'OFFLINE',
      successCount: 0,
      failedCount: 0,
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
      date: filters.value.date || undefined
    })
    await nextTick()
    renderChart()
  } catch {
    flights.value = []
    ElMessage.warning('暂时无法连接后端服务')
  } finally {
    loadingFlights.value = false
  }
}

async function handleRunCrawler() {
  runningCrawler.value = true
  try {
    latestJob.value = await runCrawler()
    await loadFlights()
    ElMessage.success('采集任务已完成')
  } catch (error) {
    ElMessage.error(error?.message || '采集任务失败')
  } finally {
    runningCrawler.value = false
  }
}

async function handleAdvice() {
  askingAdvice.value = true
  try {
    adviceResult.value = await requestAdvice(adviceInput.value)
  } catch (error) {
    ElMessage.error(error?.message || 'AI 建议请求失败')
  } finally {
    askingAdvice.value = false
  }
}

function renderChart() {
  if (!chartEl.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartEl.value)
  }
  chart.setOption(chartOption.value, true)
}

function switchView(view) {
  activeView.value = view
  nextTick(renderChart)
}

watch(chartOption, () => nextTick(renderChart))

onMounted(async () => {
  await Promise.all([loadLatestJob(), loadFlights()])
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">综合课程设计 III · 3.1.7</p>
        <h1>机票抓取与自动更新系统</h1>
      </div>
      <el-button :loading="runningCrawler" type="primary" :icon="Refresh" @click="handleRunCrawler">
        采集
      </el-button>
    </header>

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
        </dl>
      </section>
    </section>

    <section v-show="activeView === 'flights'" class="work-panel">
      <div class="filters">
        <el-input v-model="filters.fromCity" placeholder="出发城市" clearable />
        <el-input v-model="filters.toCity" placeholder="到达城市" clearable />
        <el-date-picker v-model="filters.date" value-format="YYYY-MM-DD" type="date" placeholder="出发日期" />
        <el-button :loading="loadingFlights" type="primary" :icon="Search" @click="loadFlights">
          查询
        </el-button>
      </div>
      <div ref="chartEl" class="price-chart"></div>
      <el-table :data="flights" v-loading="loadingFlights" height="420" empty-text="暂无航班数据">
        <el-table-column prop="flightNo" label="航班号" width="110" />
        <el-table-column prop="airlineName" label="航司" width="120" />
        <el-table-column label="航线" min-width="150">
          <template #default="{ row }">{{ row.fromCity }} → {{ row.toCity }}</template>
        </el-table-column>
        <el-table-column label="机场" min-width="190">
          <template #default="{ row }">{{ row.fromAirport }} → {{ row.toAirport }}</template>
        </el-table-column>
        <el-table-column label="起飞" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.departTime) }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">{{ row.price }} 元</template>
        </el-table-column>
        <el-table-column prop="seatsLeft" label="余票" width="80" />
      </el-table>
    </section>

    <section v-if="activeView === 'ai'" class="work-panel ai-panel">
      <el-input
        v-model="adviceInput"
        type="textarea"
        :rows="4"
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
  </main>
</template>
