<template>
  <section class="flight-search-page">
    <section data-testid="dashboard-controls" class="flight-search-page__controls">
      <section
        ref="syncSectionRef"
        class="flight-search-page__card flight-search-page__control-card flight-search-page__control-card--sync"
      >
        <div class="flight-search-page__card-head">
          <div>
            <h2>{{ t('flights.sync.title') }}</h2>
            <p class="flight-search-page__section-note">{{ t('flights.sync.description') }}</p>
          </div>
        </div>

        <el-form
          data-testid="sync-form"
          class="flight-search-page__sync-form"
          label-position="top"
          @submit.prevent="submitSync"
        >
          <el-form-item :label="t('flights.sync.form.airportCode')">
            <el-select
              v-model="syncForm.airportCode"
              data-testid="sync-airport-code"
              @change="onFilterChange"
            >
              <el-option
                v-for="option in airportOptions"
                :key="option.code"
                :label="getAirportLabel(option.code)"
                :value="option.code"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('flights.sync.form.date')">
            <el-date-picker
              v-model="syncForm.date"
              data-testid="sync-date"
              format="YYYY-MM-DD"
              :placeholder="placeholderText.syncDate"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>

          <el-form-item class="flight-search-page__actions flight-search-page__actions--sync">
            <el-button
              data-testid="sync-submit"
              :loading="syncLoading"
              native-type="submit"
              type="primary"
            >
              {{ t('flights.sync.actions.syncDate') }}
            </el-button>
            <el-button
              data-testid="sync-today"
              :loading="syncLoading"
              type="default"
              @click="syncToday"
            >
              {{ t('flights.sync.actions.syncToday') }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="flight-search-page__sync-feedback">
          <p v-if="syncMessage" data-testid="sync-message" class="flight-search-page__sync-message">
            {{ syncMessage }}
          </p>
          <p v-if="syncError" data-testid="sync-error" class="flight-search-page__error">
            {{ syncError }}
          </p>
        </div>
      </section>

      <section
        ref="searchSectionRef"
        class="flight-search-page__card flight-search-page__control-card flight-search-page__control-card--search"
      >
        <div class="flight-search-page__card-head">
          <div>
            <h2>{{ t('flights.search.title') }}</h2>
            <p class="flight-search-page__section-note">{{ t('flights.search.description') }}</p>
          </div>
        </div>

        <el-form
          data-testid="search-form"
          class="flight-search-page__filters"
          label-position="top"
          @submit.prevent="submitSearch"
        >
          <el-form-item :label="t('flights.filters.from')">
            <el-select
              v-model="filters.fromCity"
              data-testid="filter-from-airport"
              :placeholder="t('flights.placeholders.from')"
              @change="onFilterChange"
            >
              <el-option
                v-for="option in airportOptions"
                :key="`from-${option.code}`"
                :label="getAirportLabel(option.code)"
                :value="option.code"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('flights.filters.to')">
            <el-select
              v-model="filters.toCity"
              data-testid="filter-to-airport"
              :placeholder="t('flights.placeholders.to')"
              @change="onFilterChange"
            >
              <el-option
                v-for="option in airportOptions"
                :key="`to-${option.code}`"
                :label="getAirportLabel(option.code)"
                :value="option.code"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('flights.filters.date')">
            <el-date-picker
              v-model="filters.date"
              data-testid="filter-date"
              format="YYYY-MM-DD"
              :placeholder="placeholderText.searchDate"
              type="date"
              value-format="YYYY-MM-DD"
              @change="onFilterChange"
            />
          </el-form-item>

          <el-form-item :label="t('flights.filters.source')">
            <el-select
              v-model="filters.dataSource"
              data-testid="filter-source"
              @change="onFilterChange"
            >
              <el-option :label="filterOptionText.any" value="" />
              <el-option label="aerodatabox" value="aerodatabox" />
            </el-select>
          </el-form-item>

          <el-form-item :label="advancedFilterText.airline">
            <el-select
              v-model="filters.airline"
              data-testid="filter-airline"
              @change="onFilterChange"
            >
              <el-option :label="filterOptionText.any" value="" />
              <el-option
                v-for="airline in airlineOptions"
                :key="airline"
                :label="airline"
                :value="airline"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="advancedFilterText.priceRange">
            <el-select
              v-model="filters.priceRange"
              data-testid="filter-price-range"
              @change="onFilterChange"
            >
              <el-option :label="filterOptionText.priceAny" value="" />
              <el-option :label="filterOptionText.priceLow" value="0-1000" />
              <el-option :label="filterOptionText.priceMid" value="1000-2000" />
              <el-option :label="filterOptionText.priceHigh" value="2000+" />
            </el-select>
          </el-form-item>

          <el-form-item :label="advancedFilterText.status">
            <el-select
              v-model="filters.status"
              data-testid="filter-status"
              @change="onFilterChange"
            >
              <el-option :label="filterOptionText.statusAny" value="" />
              <el-option :label="filterOptionText.statusScheduled" value="Scheduled" />
              <el-option :label="filterOptionText.statusDelayed" value="Delayed" />
              <el-option :label="filterOptionText.statusCancelled" value="Cancelled" />
            </el-select>
          </el-form-item>

          <el-form-item :label="advancedFilterText.departSlot">
            <el-select
              v-model="filters.departSlot"
              data-testid="filter-depart-slot"
              @change="onFilterChange"
            >
              <el-option :label="filterOptionText.slotAny" value="" />
              <el-option :label="filterOptionText.slotOvernight" value="overnight" />
              <el-option :label="filterOptionText.slotMorning" value="morning" />
              <el-option :label="filterOptionText.slotAfternoon" value="afternoon" />
              <el-option :label="filterOptionText.slotEvening" value="evening" />
            </el-select>
          </el-form-item>

          <el-form-item class="flight-search-page__actions flight-search-page__actions--search">
            <el-button :loading="loading" native-type="submit" type="primary">
              {{ t('common.actions.searchFlights') }}
            </el-button>
          </el-form-item>
        </el-form>

        <p v-if="errorMessage" class="flight-search-page__error">{{ errorMessage }}</p>
      </section>
    </section>

    <section
      data-testid="dashboard-sync-strip"
      :class="['flight-search-page__sync-strip', { 'flight-search-page__sync-strip--failed': syncStatus === 'FAILED' }]"
    >
      <div v-if="syncResult" data-testid="sync-result" class="flight-search-page__sync-strip-body">
        <div class="flight-search-page__sync-summary">
          <strong>{{ t('flights.syncResult.title') }}</strong>
          <div :class="['flight-search-page__status-pill', statusToneClass]">
            <span class="flight-search-page__status-dot" />
            <span data-testid="sync-status">{{ syncResult.status || '-' }}</span>
          </div>
        </div>

        <div class="flight-search-page__sync-inline-item">
          <span>{{ t('flights.syncResult.fields.successCount') }}</span>
          <strong data-testid="sync-success-count">{{ syncResult.successCount ?? '-' }}</strong>
        </div>

        <div class="flight-search-page__sync-inline-item">
          <span>{{ t('flights.syncResult.fields.failedCount') }}</span>
          <strong data-testid="sync-failed-count">{{ syncResult.failedCount ?? '-' }}</strong>
        </div>

        <div class="flight-search-page__sync-inline-item">
          <span>{{ t('flights.syncResult.fields.source') }}</span>
          <strong data-testid="sync-source">{{ syncResult.source || '-' }}</strong>
        </div>

        <div class="flight-search-page__sync-inline-item">
          <span>{{ t('flights.filters.date') }}</span>
          <strong>{{ syncForm.date || '-' }}</strong>
        </div>

        <div class="flight-search-page__sync-inline-item">
          <span>{{ t('flights.syncResult.fields.finishedAt') }}</span>
          <strong data-testid="sync-finished-at">{{ formatSyncValue(syncResult.finishedAt) }}</strong>
        </div>

        <button
          type="button"
          data-testid="sync-toggle-details"
          class="flight-search-page__toggle"
          @click="syncDetailsOpen = !syncDetailsOpen"
        >
          {{ syncDetailsOpen ? toggleText.collapseDetails : toggleText.expandDetails }}
        </button>
      </div>

      <div v-else data-testid="sync-result-empty" class="flight-search-page__sync-strip-empty">
        <strong>{{ t('flights.syncResult.empty.title') }}</strong>
        <p>{{ t('flights.syncResult.empty.description') }}</p>
      </div>

      <div
        v-if="syncResult?.errorMessage"
        data-testid="sync-result-error-banner"
        class="flight-search-page__sync-error-block"
      >
        {{ syncResult.errorMessage }}
      </div>

      <dl
        v-if="syncResult && syncDetailsOpen"
        class="flight-search-page__sync-grid flight-search-page__sync-grid--details"
      >
        <div class="flight-search-page__meta-card">
          <dt>{{ t('flights.syncResult.fields.requestParams') }}</dt>
          <dd data-testid="sync-request-params">{{ formatSyncValue(syncResult.requestParams) }}</dd>
        </div>

        <div class="flight-search-page__meta-card">
          <dt>{{ t('flights.syncResult.fields.startedAt') }}</dt>
          <dd data-testid="sync-started-at">{{ formatSyncValue(syncResult.startedAt) }}</dd>
        </div>

        <div class="flight-search-page__meta-card flight-search-page__meta-card--wide">
          <dt>{{ t('flights.syncResult.fields.errorMessage') }}</dt>
          <dd data-testid="sync-error-message">{{ formatSyncValue(syncResult.errorMessage) }}</dd>
        </div>
      </dl>
    </section>

    <section ref="resultsSectionRef" data-testid="dashboard-workspace" class="flight-search-page__workspace flight-search-page__console">
      <section class="flight-search-page__results-pane">
        <section class="flight-search-page__card flight-search-page__results-card">
          <div class="flight-search-page__results-head">
            <div>
              <h2>{{ t('flights.results.title') }}</h2>
              <p class="flight-search-page__results-count">
                {{ resultsMetricText.total(totalCount ? totalCount : 0) }}
              </p>
            </div>

            <div class="flight-search-page__result-controls">
              <span>{{ resultsMetricText.perPage }}</span>
              <el-select
                v-model="pageSize"
                data-testid="pagination-size"
                @change="resetPagination"
              >
                <el-option label="10" :value="10" />
                <el-option label="20" :value="20" />
              </el-select>
            </div>
          </div>

          <div class="flight-search-page__table-shell">
            <FlightTable
              :flights="pagedFlights"
              :loading="loading"
              :selected-flight-id="selectedFlightId"
              @select="selectFlight"
            />
          </div>

          <div v-if="!loading && !totalCount" class="flight-search-page__empty-panel">
            <strong>{{ emptyStateText.title }}</strong>
            <p>{{ emptyStateText.description }}</p>
          </div>

          <div class="flight-search-page__results-footer">
            <div class="flight-search-page__results-summary">
              <strong data-testid="pagination-total">{{ totalCount }}</strong>
              <span>
                {{ resultsMetricText.currentPage(currentPage) }}
              </span>
            </div>

            <span data-testid="pagination-current" class="flight-search-page__page-number">{{ currentPage }}</span>

            <el-pagination
              :current-page="currentPage"
              :page-size="pageSize"
              :total="totalCount"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>
        </section>
      </section>

      <aside class="flight-search-page__inspector-pane">
        <section class="flight-search-page__card flight-search-page__detail-card">
          <div class="flight-search-page__inspector-head">
            <div>
              <h2>{{ detailPanelText.title }}</h2>
            </div>
          </div>

          <div v-if="selectedFlight || historyLoading" class="flight-search-page__detail-grid">
            <div v-if="selectedFlight" class="flight-search-page__detail-summary">
              <strong class="flight-search-page__detail-flight">{{ selectedFlight.flightNo || '-' }}</strong>
              <span class="flight-search-page__detail-route">{{ selectedFlight.routeLabel || '-' }}</span>
              <strong class="flight-search-page__detail-price">￥{{ selectedFlight.price ?? '-' }}</strong>
            </div>

            <FlightDetailCard v-if="selectedFlight" :flight="selectedFlight" />

            <div v-else class="flight-search-page__panel flight-search-page__panel--placeholder">
              {{ t('common.status.loadingFlights') }}
            </div>
          </div>

          <div v-else class="flight-search-page__empty-panel flight-search-page__empty-panel--compact">
            <strong>{{ detailStateText.title }}</strong>
            <p>{{ detailStateText.description }}</p>
          </div>
        </section>

        <section class="flight-search-page__card flight-search-page__history-card">
          <div class="flight-search-page__inspector-head">
            <div>
              <h2>{{ historyText.title }}</h2>
            </div>
          </div>

          <div v-if="selectedFlight || historyLoading" class="flight-search-page__history-panel">
            <PriceHistoryChart :history="priceHistory" :loading="historyLoading" />
          </div>

          <div v-else class="flight-search-page__empty-panel flight-search-page__empty-panel--compact">
            <strong>{{ historyText.title }}</strong>
            <p>{{ historyText.waiting }}</p>
          </div>
        </section>
      </aside>
    </section>

    <section
      ref="aiSectionRef"
      data-testid="dashboard-ai"
      :class="[
        'flight-search-page__ai-drawer',
        { 'flight-search-page__ai-drawer--open': aiPanelOpen }
      ]"
    >
      <div class="flight-search-page__ai-drawer-bar">
        <div class="flight-search-page__ai-drawer-copy">
          <strong>{{ aiText.title }}</strong>
          <p>{{ aiText.description }}</p>
        </div>

        <button
          type="button"
          data-testid="ai-toggle"
          class="flight-search-page__toggle"
          @click="aiPanelOpen = !aiPanelOpen"
        >
          {{ aiPanelOpen ? toggleText.collapseAi : toggleText.expandAi }}
        </button>
      </div>

      <div v-if="aiPanelOpen" class="flight-search-page__ai-content">
        <div class="flight-search-page__ai-form">
          <textarea
            v-model.trim="aiForm.query"
            data-testid="ai-query-input"
            class="flight-search-page__ai-textarea"
            :placeholder="aiText.placeholder"
            rows="3"
          />

          <div class="flight-search-page__ai-actions">
            <el-button
              data-testid="ai-submit"
              :loading="aiLoading"
              type="primary"
              @click="submitAiAdvice"
            >
              {{ aiText.action }}
            </el-button>
          </div>
        </div>

        <p v-if="aiError" data-testid="ai-error" class="flight-search-page__error">
          {{ aiError }}
        </p>

        <div v-if="aiResult" class="flight-search-page__ai-result">
          <div data-testid="ai-intent" class="flight-search-page__ai-panel">
            <strong>{{ aiText.intentTitle }}</strong>
            <p>{{ aiResult.intent?.from || '-' }} -> {{ aiResult.intent?.to || '-' }}</p>
            <p>{{ aiText.date }}: {{ aiResult.intent?.date || '-' }}</p>
            <p>{{ aiText.budget }}: {{ aiResult.intent?.budget ?? '-' }}</p>
            <p>{{ aiText.timePreference }}: {{ aiResult.intent?.timePreference || '-' }}</p>
          </div>

          <div data-testid="ai-candidate-list" class="flight-search-page__ai-panel">
            <strong>{{ aiText.candidatesTitle }}</strong>
            <ul v-if="aiCandidates.length" class="flight-search-page__ai-candidates">
              <li
                v-for="flight in aiCandidates"
                :key="`ai-${flight.id}-${flight.flightNo}`"
                class="flight-search-page__ai-candidate"
              >
                <div>
                  <strong>{{ flight.flightNo }}</strong>
                  <span>{{ flight.airlineLabel || flight.airlineName || '-' }}</span>
                </div>
                <div>{{ flight.routeLabel }}</div>
                <div>{{ flight.departTime }} -> {{ flight.arriveTime }}</div>
                <div>{{ formatAiCandidateMeta(flight) }}</div>
              </li>
            </ul>
            <p v-else>{{ aiText.noCandidates }}</p>
          </div>

          <div data-testid="ai-advice-summary" class="flight-search-page__ai-panel">
            <strong>{{ aiText.adviceTitle }}</strong>
            <p>{{ aiResult.summary || '-' }}</p>
            <p v-if="aiRecommendedFlight">
              {{ aiRecommendedFlight.flightNo }} {{ aiRecommendedFlight.routeLabel }}
            </p>
          </div>
        </div>

        <div v-else-if="!aiError" class="flight-search-page__empty-panel">
          <strong>{{ aiText.title }}</strong>
          <p>{{ aiText.empty }}</p>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchFlight, fetchFlights, fetchPriceHistory, requestAdvice, syncFlights } from '../../../api/flightApi.js'
import { AIRPORT_OPTIONS, buildAirportOptionLabel } from '../../../shared/constants/airportOptions.js'
import { matchesTimeSlot, normalizeFlightForDisplay } from '../../../shared/utils/flightDisplay.js'
import FlightTable from '../../../shared/components/FlightTable.vue'
import FlightDetailCard from '../../../shared/components/FlightDetailCard.vue'
import PriceHistoryChart from '../../../shared/charts/PriceHistoryChart.vue'

const { t, locale } = useI18n()

let activeSearchRequestId = 0
let activeSelectionRequestId = 0

const loading = ref(false)
const historyLoading = ref(false)
const errorMessage = ref('')
const flights = ref([])
const selectedFlight = ref(null)
const selectedFlightId = ref(null)
const priceHistory = ref([])
const syncLoading = ref(false)
const syncResult = ref(null)
const syncMessage = ref('')
const syncError = ref('')
const syncDetailsOpen = ref(false)
const aiLoading = ref(false)
const aiResult = ref(null)
const aiError = ref('')
const aiPanelOpen = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const syncSectionRef = ref(null)
const searchSectionRef = ref(null)
const resultsSectionRef = ref(null)
const aiSectionRef = ref(null)

const filters = reactive({
  fromCity: '',
  toCity: '',
  date: '',
  dataSource: '',
  airline: '',
  priceRange: '',
  status: '',
  departSlot: ''
})

const syncForm = reactive({
  airportCode: 'CKG',
  date: getTodayDateString()
})

const aiForm = reactive({
  query: ''
})

const syncStatus = computed(() => String(syncResult.value?.status ?? '').toUpperCase())
const airportOptions = AIRPORT_OPTIONS
const airlineOptions = computed(() => {
  const names = flights.value.map(flight => flight?.airlineName).filter(Boolean)
  return [...new Set(names)]
})

const advancedFilterText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        airline: '航空公司',
        priceRange: '价格区间',
        status: '航班状态',
        departSlot: '出发时段'
      }
    : {
        airline: 'Airline',
        priceRange: 'Price Range',
        status: 'Status',
        departSlot: 'Departure Window'
      }
))

const filterOptionText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        any: '请选择',
        priceAny: '全部价格',
        priceLow: '1000以下',
        priceMid: '1000 - 2000',
        priceHigh: '2000以上',
        statusAny: '全部状态',
        statusScheduled: '正常',
        statusDelayed: '延误',
        statusCancelled: '取消',
        slotAny: '全部时段',
        slotOvernight: '凌晨',
        slotMorning: '上午',
        slotAfternoon: '下午',
        slotEvening: '晚上'
      }
    : {
        any: 'Select',
        priceAny: 'Any price',
        priceLow: '0 - 1000',
        priceMid: '1000 - 2000',
        priceHigh: '2000+',
        statusAny: 'Any status',
        statusScheduled: 'Scheduled',
        statusDelayed: 'Delayed',
        statusCancelled: 'Cancelled',
        slotAny: 'Any time',
        slotOvernight: 'Overnight',
        slotMorning: 'Morning',
        slotAfternoon: 'Afternoon',
        slotEvening: 'Evening'
      }
))

const placeholderText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        syncDate: '请选择同步日期',
        searchDate: '选择出发日期'
      }
    : {
        syncDate: 'Choose sync date',
        searchDate: 'Choose departure date'
      }
))

const displayFlights = computed(() =>
  flights.value.map(flight => normalizeFlightForDisplay(flight, locale.value))
)

const filteredFlights = computed(() =>
  displayFlights.value.filter(flight => {
    if (filters.airline && flight.airlineRawName !== filters.airline && flight.airlineLabel !== filters.airline) {
      return false
    }
    if (filters.status && normalizeStatusValue(flight.statusLabel) !== normalizeStatusValue(filters.status)) {
      return false
    }
    if (filters.priceRange && !matchesPriceRange(flight.price, filters.priceRange)) {
      return false
    }
    if (filters.departSlot && !matchesTimeSlot(flight, filters.departSlot)) {
      return false
    }
    return true
  })
)

const totalCount = computed(() => filteredFlights.value.length)
const pagedFlights = computed(() => {
  const startIndex = (currentPage.value - 1) * Number(pageSize.value || 10)
  return filteredFlights.value.slice(startIndex, startIndex + Number(pageSize.value || 10))
})

const statusToneClass = computed(() => {
  if (syncStatus.value === 'SUCCESS') return 'flight-search-page__status-pill--success'
  if (syncStatus.value === 'FAILED') return 'flight-search-page__status-pill--failed'
  if (syncStatus.value === 'RUNNING') return 'flight-search-page__status-pill--running'
  return 'flight-search-page__status-pill--neutral'
})

const resultsMetricText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        total: count => `共 ${count} 条结果`,
        perPage: '每页显示',
        currentPage: page => `当前第 ${page} 页`
      }
    : {
        total: count => `${count} results`,
        perPage: 'Per page',
        currentPage: page => `Page ${page}`
      }
))

const toggleText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        expandDetails: '展开详情',
        collapseDetails: '收起详情',
        expandAi: '展开 AI 出行建议',
        collapseAi: '收起 AI 出行建议'
      }
    : {
        expandDetails: 'View Details',
        collapseDetails: 'Hide Details',
        expandAi: 'Expand AI Advice',
        collapseAi: 'Collapse AI Advice'
      }
))

const emptyStateText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        title: '当前筛选条件下没有航班',
        description: '可以先同步指定机场和日期的数据，或调整出发地、目的地、日期与筛选条件后重试。'
      }
    : {
        title: 'No flights match the current filters',
        description: 'Try syncing the selected airport/date first, or adjust the route, date, and filters.'
      }
))

const detailStateText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        title: '尚未选中航班',
        description: '查询到本地航班后，点击中栏列表中的任意航班查看详情和价格历史。'
      }
    : {
        title: 'No flight selected',
        description: 'After local results load, select a flight in the center list to inspect details and price history.'
      }
))

const detailPanelText = computed(() => (
  locale.value === 'zh-CN'
    ? { title: '已选航班详情' }
    : { title: 'Selected Flight Detail' }
))

const historyText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        title: '价格历史',
        waiting: '完成多次同步后，这里会展示当前航班的价格快照变化。'
      }
    : {
        title: 'Price History',
        waiting: 'Price snapshots will appear here after repeated sync runs capture the same flight.'
      }
))

const aiText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        title: 'AI 出行建议助手',
        description: '输入自然语言需求，系统会先查询本地 MySQL 航班，再基于候选航班生成建议。',
        placeholder: '例如：下周五去北京出差，预算1200元，希望上午到',
        action: '生成出行建议',
        intentTitle: '解析出的出行需求',
        candidatesTitle: '候选航班',
        adviceTitle: '建议结果',
        empty: '输入出行需求后，这里会显示解析结果、候选航班和建议文案。',
        budget: '预算',
        date: '日期',
        timePreference: '时间偏好',
        noCandidates: '当前没有符合条件的候选航班。'
      }
    : {
        title: 'AI Travel Advice Assistant',
        description: 'Describe your trip in natural language. The system will query local MySQL flights first, then generate advice from local candidates only.',
        placeholder: 'Example: Business trip to Beijing next Friday, budget 1200 RMB, prefer to arrive in the morning',
        action: 'Generate Advice',
        intentTitle: 'Parsed Intent',
        candidatesTitle: 'Candidate Flights',
        adviceTitle: 'Advice',
        empty: 'Enter a travel request to see the parsed intent, local flight candidates, and advice.',
        budget: 'Budget',
        date: 'Date',
        timePreference: 'Time Preference',
        noCandidates: 'No matching local flight candidates yet.'
      }
))

const aiCandidates = computed(() =>
  Array.isArray(aiResult.value?.candidates)
    ? aiResult.value.candidates.map(flight => normalizeFlightForDisplay(flight, locale.value))
    : []
)

const aiRecommendedFlight = computed(() =>
  aiResult.value?.recommendedFlight
    ? normalizeFlightForDisplay(aiResult.value.recommendedFlight, locale.value)
    : null
)

function formatAiCandidateMeta(flight) {
  const price = flight?.price ?? '-'
  const seats = flight?.seatsLeft ?? '-'
  const source = flight?.dataSource || '-'

  return locale.value === 'zh-CN'
    ? `￥${price} / ${seats} 座 / ${source}`
    : `¥${price} / ${seats} seats / ${source}`
}

async function submitSearch() {
  const requestId = ++activeSearchRequestId
  loading.value = true
  errorMessage.value = ''
  resetPagination()

  try {
    const rows = await fetchFlights(buildQueryParams())
    if (requestId !== activeSearchRequestId) return

    flights.value = Array.isArray(rows) ? rows : []

    if (!flights.value.length || !pagedFlights.value.length) {
      selectedFlight.value = null
      selectedFlightId.value = null
      priceHistory.value = []
      return
    }

    await selectFlight(pagedFlights.value[0])
  } catch (error) {
    if (requestId !== activeSearchRequestId) return
    flights.value = []
    selectedFlight.value = null
    selectedFlightId.value = null
    priceHistory.value = []
    errorMessage.value = getErrorMessage(error)
  } finally {
    if (requestId === activeSearchRequestId) loading.value = false
  }
}

async function selectFlight(flight) {
  if (!flight?.id) return

  const requestId = ++activeSelectionRequestId
  selectedFlightId.value = flight.id
  selectedFlight.value = flight
  priceHistory.value = []
  historyLoading.value = true
  errorMessage.value = ''

  const [detailResult, historyResult] = await Promise.allSettled([
    fetchFlight(flight.id),
    fetchPriceHistory(flight.id)
  ])

  if (requestId !== activeSelectionRequestId) return

  if (detailResult.status === 'fulfilled' && detailResult.value) {
    selectedFlight.value = normalizeFlightForDisplay(detailResult.value, locale.value)
  }
  if (historyResult.status === 'fulfilled') {
    priceHistory.value = Array.isArray(historyResult.value) ? historyResult.value : []
  } else {
    priceHistory.value = []
  }

  if (detailResult.status === 'rejected') {
    errorMessage.value = getErrorMessage(detailResult.reason)
  } else if (historyResult.status === 'rejected') {
    errorMessage.value = getErrorMessage(historyResult.reason, t('flights.errors.partialHistory'))
  }

  if (requestId === activeSelectionRequestId) historyLoading.value = false
}

function buildQueryParams() {
  const params = {}
  if (filters.fromCity) params.fromCity = filters.fromCity
  if (filters.toCity) params.toCity = filters.toCity
  if (filters.date) params.date = filters.date
  if (filters.dataSource) params.dataSource = filters.dataSource
  return params
}

function getErrorMessage(error, fallback = t('flights.errors.loadFailed')) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}

async function submitSync() {
  const airportCode = syncForm.airportCode?.trim()
  const date = syncForm.date || getTodayDateString()

  syncLoading.value = true
  syncMessage.value = ''
  syncError.value = ''

  try {
    const result = await syncFlights({ airportCode, date })
    syncResult.value = result ?? null
    if (isFailedSyncResult(result)) {
      syncError.value = getSyncErrorMessage({ response: { data: result } })
      return
    }
    await applySuccessfulSync(date)
  } catch (error) {
    syncResult.value = error?.response?.data ?? null
    syncError.value = getSyncErrorMessage(error)
  } finally {
    syncLoading.value = false
  }
}

async function syncToday() {
  syncForm.date = getTodayDateString()
  await submitSync()
}

async function submitAiAdvice() {
  const query = aiForm.query.trim()
  if (!query) {
    aiError.value = locale.value === 'zh-CN' ? '请输入出行需求。' : 'Please enter a travel request.'
    aiResult.value = null
    return
  }

  aiLoading.value = true
  aiError.value = ''

  try {
    aiResult.value = await requestAdvice(query)
  } catch (error) {
    aiResult.value = null
    aiError.value = getErrorMessage(
      error,
      locale.value === 'zh-CN'
        ? '生成出行建议失败，请稍后重试。'
        : 'Unable to generate travel advice right now.'
    )
  } finally {
    aiLoading.value = false
  }
}

async function applySuccessfulSync(date) {
  syncError.value = ''
  syncMessage.value = t('flights.sync.messages.success')
  syncDetailsOpen.value = false
  filters.dataSource = 'aerodatabox'
  resetPagination()
  if (date) filters.date = date
  await submitSearch()
}

function getSyncErrorMessage(error, fallback = t('flights.sync.messages.failed')) {
  return error?.response?.data?.errorMessage
    ?? error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}

function getTodayDateString() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function isFailedSyncResult(result) {
  return String(result?.status ?? '').toUpperCase() === 'FAILED'
}

function formatSyncValue(value) {
  return value === null || value === undefined || value === '' ? '-' : String(value)
}

function getAirportLabel(code) {
  return buildAirportOptionLabel(code, locale.value)
}

function onFilterChange() {
  resetPagination()
}

function resetPagination() {
  currentPage.value = 1
}

function handlePageChange(page) {
  const nextPage = Number(page) || 1
  const maxPage = Math.max(Math.ceil(totalCount.value / Number(pageSize.value || 10)), 1)
  currentPage.value = Math.min(Math.max(nextPage, 1), maxPage)
}

function matchesPriceRange(price, range) {
  const numericPrice = Number(price)
  if (!Number.isFinite(numericPrice)) return false
  if (range === '0-1000') return numericPrice <= 1000
  if (range === '1000-2000') return numericPrice > 1000 && numericPrice <= 2000
  if (range === '2000+') return numericPrice > 2000
  return true
}

function normalizeStatusValue(status) {
  return String(status ?? '').trim().toLowerCase()
}

watch([filteredFlights, pageSize], () => {
  const maxPage = Math.max(Math.ceil(totalCount.value / Number(pageSize.value || 10)), 1)
  if (currentPage.value > maxPage) currentPage.value = maxPage
})

watch(pagedFlights, rows => {
  if (!rows.length) {
    selectedFlight.value = null
    selectedFlightId.value = null
    priceHistory.value = []
    return
  }
  if (!rows.some(row => row.id === selectedFlightId.value)) {
    void selectFlight(rows[0])
  }
})
</script>

<style scoped>
.flight-search-page {
  display: grid;
  gap: 16px;
  box-sizing: border-box;
  min-height: 0;
  overflow: visible;
  padding: 0 0 18px;
  grid-template-rows: auto auto 1fr auto;
  grid-template-areas:
    "controls"
    "strip"
    "workspace"
    "ai";
}

.flight-search-page__controls {
  display: grid;
  grid-template-columns: 0.32fr 0.68fr;
  gap: 16px;
  align-items: start;
  min-height: 0;
  overflow: visible;
  grid-area: controls;
}

.flight-search-page__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 16px;
  color: #1d4ed8;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  background: #ffffff;
  border: 1px solid #d9e7fb;
  border-radius: 999px;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.04);
}

.flight-search-page__workspace,
.flight-search-page__console {
  display: grid;
  grid-template-columns: minmax(0, 0.72fr) minmax(300px, 0.28fr);
  gap: 16px;
  align-items: start;
  min-height: 0;
  overflow: visible;
  grid-area: workspace;
}

.flight-search-page__card {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 16px 16px 14px;
  background: #ffffff;
  border: 1px solid #dbe5f0;
  border-radius: 16px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
}

.flight-search-page__control-card {
  min-height: 168px;
  height: auto;
  align-content: start;
  overflow: visible;
}

.flight-search-page__control-card--search {
  gap: 8px;
}

.flight-search-page__card-head,
.flight-search-page__results-head,
.flight-search-page__inspector-head,
.flight-search-page__ai-drawer-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.flight-search-page__card-head h2,
.flight-search-page__results-head h2,
.flight-search-page__inspector-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  line-height: 1.25;
}

.flight-search-page__section-note {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

.flight-search-page__sync-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  align-items: end;
  min-height: 0;
}

.flight-search-page__filters {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 6px 10px;
  align-items: end;
  min-height: 0;
}

.flight-search-page__sync-form :deep(.el-form-item),
.flight-search-page__filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.flight-search-page__sync-form :deep(.el-form-item__label),
.flight-search-page__filters :deep(.el-form-item__label) {
  padding-bottom: 4px;
  color: #475569;
  font-size: 12px;
  line-height: 1.2;
}

.flight-search-page__sync-form :deep(.el-input__wrapper),
.flight-search-page__sync-form :deep(.el-select__wrapper),
.flight-search-page__sync-form :deep(.el-date-editor.el-input),
.flight-search-page__filters :deep(.el-input__wrapper),
.flight-search-page__filters :deep(.el-select__wrapper),
.flight-search-page__filters :deep(.el-date-editor.el-input) {
  min-height: 36px;
}

.flight-search-page__actions {
  align-self: end;
}

.flight-search-page__actions--sync {
  grid-column: 1 / -1;
}

.flight-search-page__actions--sync :deep(.el-form-item__content) {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.flight-search-page__actions--search {
  grid-column: 4 / 6;
}

.flight-search-page__actions--search :deep(.el-form-item__content) {
  display: flex;
  justify-content: flex-end;
}

.flight-search-page__sync-feedback {
  display: grid;
  gap: 6px;
  align-self: end;
}

.flight-search-page__sync-message {
  margin: 0;
  min-height: 36px;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  color: #166534;
  font-size: 12px;
  font-weight: 600;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 12px;
}

.flight-search-page__error {
  margin: 0;
  padding: 8px 12px;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 600;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

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
  grid-area: strip;
}

.flight-search-page__sync-strip--failed {
  border-left-color: #ef4444;
  background: linear-gradient(180deg, #ffffff 0%, #fef2f2 100%);
}

.flight-search-page__sync-strip-body {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) repeat(4, minmax(110px, 0.8fr)) minmax(160px, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.flight-search-page__sync-summary {
  display: grid;
  gap: 6px;
}

.flight-search-page__sync-summary strong {
  color: #0f172a;
  font-size: 15px;
}

.flight-search-page__status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}

.flight-search-page__status-pill--success {
  color: #166534;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.flight-search-page__status-pill--failed {
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.flight-search-page__status-pill--running,
.flight-search-page__status-pill--neutral {
  color: #1d4ed8;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.flight-search-page__status-dot {
  width: 8px;
  height: 8px;
  background: currentColor;
  border-radius: 999px;
}

.flight-search-page__sync-inline-item {
  display: grid;
  gap: 3px;
}

.flight-search-page__sync-inline-item span {
  color: #64748b;
  font-size: 10px;
  font-weight: 600;
}

.flight-search-page__sync-inline-item strong {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.flight-search-page__sync-strip-empty {
  display: grid;
  gap: 6px;
}

.flight-search-page__sync-strip-empty strong {
  color: #0f172a;
  font-size: 15px;
}

.flight-search-page__sync-strip-empty p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.flight-search-page__sync-grid {
  display: grid;
  gap: 8px;
  margin: 0;
}

.flight-search-page__sync-grid--details {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.flight-search-page__meta-card {
  padding: 10px 12px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
}

.flight-search-page__meta-card--wide {
  grid-column: 1 / -1;
}

.flight-search-page__meta-card dt {
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
}

.flight-search-page__meta-card dd {
  margin: 6px 0 0;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.flight-search-page__sync-error-block {
  padding: 8px 12px;
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
}

.flight-search-page__results-pane,
.flight-search-page__inspector-pane {
  min-width: 0;
  min-height: 0;
}

.flight-search-page__results-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: visible;
  gap: 12px;
}

.flight-search-page__results-head {
  align-items: center;
}

.flight-search-page__results-count {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.flight-search-page__result-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.flight-search-page__table-shell {
  flex: 1;
  min-height: 460px;
  max-height: 560px;
  overflow: auto;
}

.flight-search-page__results-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  flex-shrink: 0;
}

.flight-search-page__results-summary {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.flight-search-page__results-summary strong {
  color: #0f172a;
  font-size: 18px;
}

.flight-search-page__page-number {
  display: none;
}

.flight-search-page__inspector-pane {
  display: grid;
  gap: 14px;
  align-self: start;
  overflow: auto;
  padding-right: 4px;
  grid-auto-rows: max-content;
  max-height: 860px;
}

.flight-search-page__detail-card,
.flight-search-page__history-card {
  align-content: start;
  flex-shrink: 0;
}

.flight-search-page__detail-card {
  min-height: 300px;
}

.flight-search-page__history-card {
  min-height: 280px;
}

.flight-search-page__detail-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: linear-gradient(180deg, #f9fbff 0%, #eef5ff 100%);
  border: 1px solid #d9e7fb;
  border-radius: 14px;
}

.flight-search-page__detail-flight {
  color: #2563eb;
  font-size: 22px;
  font-weight: 800;
}

.flight-search-page__detail-route {
  min-width: 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
}

.flight-search-page__detail-price {
  color: #0f766e;
  font-size: 18px;
  font-weight: 800;
}

.flight-search-page__detail-grid,
.flight-search-page__history-panel {
  display: grid;
  gap: 12px;
  min-height: 0;
  max-height: 320px;
}

.flight-search-page__panel {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe5f0;
  border-radius: 12px;
}

.flight-search-page__panel--placeholder {
  color: #64748b;
}

.flight-search-page__empty-panel {
  padding: 16px 18px;
  color: #475569;
  background: linear-gradient(180deg, #f8fbff 0%, #f8fafc 100%);
  border: 1px dashed #bfd7ff;
  border-radius: 14px;
}

.flight-search-page__empty-panel strong {
  display: block;
  margin-bottom: 8px;
  color: #0f172a;
}

.flight-search-page__empty-panel p {
  margin: 0;
  line-height: 1.5;
}

.flight-search-page__empty-panel--compact {
  min-height: 140px;
  align-content: center;
}

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
  grid-area: ai;
}

.flight-search-page__ai-drawer--open {
  max-height: 40vh;
  overflow: auto;
}

.flight-search-page__ai-drawer-copy strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
}

.flight-search-page__ai-drawer-copy p {
  margin: 3px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

.flight-search-page__ai-content {
  display: grid;
  gap: 14px;
  padding-top: 4px;
}

.flight-search-page__ai-form {
  display: grid;
  gap: 12px;
}

.flight-search-page__ai-textarea {
  width: 100%;
  min-height: 100px;
  padding: 12px 14px;
  color: #0f172a;
  font: inherit;
  line-height: 1.5;
  resize: vertical;
  background: #f8fbff;
  border: 1px solid #cbdcf7;
  border-radius: 12px;
  outline: none;
}

.flight-search-page__ai-textarea:focus {
  border-color: #60a5fa;
  box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.18);
}

.flight-search-page__ai-actions {
  display: flex;
  justify-content: flex-end;
}

.flight-search-page__ai-result {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.flight-search-page__ai-panel {
  display: grid;
  gap: 8px;
  padding: 14px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
}

.flight-search-page__ai-panel strong {
  color: #0f172a;
}

.flight-search-page__ai-panel p {
  margin: 0;
  color: #334155;
  line-height: 1.5;
}

.flight-search-page__ai-candidates {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flight-search-page__ai-candidate {
  display: grid;
  gap: 6px;
  padding: 10px;
  background: #ffffff;
  border: 1px solid #dbeafe;
  border-radius: 10px;
}

.flight-search-page__ai-candidate div {
  color: #334155;
  line-height: 1.45;
}

@media (max-width: 1280px) {
  .flight-search-page__controls,
  .flight-search-page__workspace,
  .flight-search-page__console {
    grid-template-columns: 1fr;
  }

  .flight-search-page__filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flight-search-page__actions--search {
    grid-column: 1 / -1;
  }

  .flight-search-page__actions--search :deep(.el-form-item__content) {
    justify-content: flex-start;
  }

  .flight-search-page__sync-strip-body {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .flight-search-page__control-card {
    height: auto;
  }

  .flight-search-page__table-shell {
    min-height: 420px;
    max-height: 520px;
  }

  .flight-search-page__inspector-pane {
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 900px) {
  .flight-search-page__card-head,
  .flight-search-page__results-head,
  .flight-search-page__results-footer,
  .flight-search-page__ai-drawer-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .flight-search-page__filters,
  .flight-search-page__sync-form,
  .flight-search-page__sync-grid--details,
  .flight-search-page__ai-result,
  .flight-search-page__sync-strip-body {
    grid-template-columns: 1fr;
  }

  .flight-search-page__detail-summary {
    grid-template-columns: 1fr;
  }

  .flight-search-page__inspector-pane {
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 720px) {
  .flight-search-page {
    padding-bottom: 14px;
  }

  .flight-search-page__card,
  .flight-search-page__sync-strip,
  .flight-search-page__ai-drawer {
    padding: 14px;
    border-radius: 14px;
  }
}
</style>
