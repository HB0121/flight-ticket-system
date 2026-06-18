<template>
  <section class="flight-search-page">
    <header class="flight-search-page__header">
      <div>
        <p class="flight-search-page__eyebrow">User Flight Search</p>
        <h2>Search current flight snapshots</h2>
        <p class="flight-search-page__subtitle">
          This phase-1 page covers search, detail inspection, and price history without pulling admin or AI concerns into the user flow.
        </p>
      </div>
    </header>

    <el-form class="flight-search-page__filters" label-position="top" @submit.prevent="submitSearch">
      <el-form-item label="From">
        <el-input v-model.trim="filters.fromCity" placeholder="e.g. 上海" />
      </el-form-item>
      <el-form-item label="To">
        <el-input v-model.trim="filters.toCity" placeholder="e.g. 北京" />
      </el-form-item>
      <el-form-item label="Date">
        <el-date-picker
          v-model="filters.date"
          format="YYYY-MM-DD"
          placeholder="Select a departure date"
          type="date"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="Source">
        <el-input v-model.trim="filters.dataSource" placeholder="Optional source filter" />
      </el-form-item>
      <el-form-item class="flight-search-page__actions">
        <el-button :loading="loading" native-type="submit" type="primary">Search Flights</el-button>
      </el-form-item>
    </el-form>

    <p v-if="errorMessage" class="flight-search-page__error">{{ errorMessage }}</p>

    <FlightTable
      :flights="flights"
      :loading="loading"
      :selected-flight-id="selectedFlightId"
      @select="selectFlight"
    />

    <div v-if="selectedFlight || historyLoading" class="flight-search-page__detail-grid">
      <FlightDetailCard v-if="selectedFlight" :flight="selectedFlight" />
      <div v-else class="flight-search-page__panel flight-search-page__panel--placeholder">
        Loading flight details...
      </div>

      <PriceHistoryChart :history="priceHistory" :loading="historyLoading" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { fetchFlight, fetchFlights, fetchPriceHistory } from '../../../api/flightApi.js'
import FlightTable from '../../../shared/components/FlightTable.vue'
import FlightDetailCard from '../../../shared/components/FlightDetailCard.vue'
import PriceHistoryChart from '../../../shared/charts/PriceHistoryChart.vue'

let activeSearchRequestId = 0
let activeSelectionRequestId = 0

const loading = ref(false)
const historyLoading = ref(false)
const errorMessage = ref('')
const flights = ref([])
const selectedFlight = ref(null)
const selectedFlightId = ref(null)
const priceHistory = ref([])

const filters = reactive({
  fromCity: '上海',
  toCity: '北京',
  date: '',
  dataSource: ''
})

onMounted(() => {
  submitSearch()
})

async function submitSearch() {
  const requestId = ++activeSearchRequestId
  loading.value = true
  errorMessage.value = ''

  try {
    const rows = await fetchFlights(buildQueryParams())

    if (requestId !== activeSearchRequestId) {
      return
    }

    flights.value = Array.isArray(rows) ? rows : []

    if (!flights.value.length) {
      selectedFlight.value = null
      selectedFlightId.value = null
      priceHistory.value = []
      return
    }

    await selectFlight(flights.value[0])
  } catch (error) {
    if (requestId !== activeSearchRequestId) {
      return
    }

    flights.value = []
    selectedFlight.value = null
    selectedFlightId.value = null
    priceHistory.value = []
    errorMessage.value = getErrorMessage(error)
  } finally {
    if (requestId === activeSearchRequestId) {
      loading.value = false
    }
  }
}

async function selectFlight(flight) {
  if (!flight?.id) {
    return
  }

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

  if (requestId !== activeSelectionRequestId) {
    return
  }

  if (detailResult.status === 'fulfilled' && detailResult.value) {
    selectedFlight.value = detailResult.value
  }

  if (historyResult.status === 'fulfilled') {
    priceHistory.value = Array.isArray(historyResult.value) ? historyResult.value : []
  } else {
    priceHistory.value = []
  }

  if (detailResult.status === 'rejected') {
    errorMessage.value = getErrorMessage(detailResult.reason)
  } else if (historyResult.status === 'rejected') {
    errorMessage.value = getErrorMessage(
      historyResult.reason,
      'Flight detail loaded partially. Price history is unavailable right now.'
    )
  }

  if (requestId === activeSelectionRequestId) {
    historyLoading.value = false
  }
}

function buildQueryParams() {
  const params = {}

  if (filters.fromCity) {
    params.fromCity = filters.fromCity
  }

  if (filters.toCity) {
    params.toCity = filters.toCity
  }

  if (filters.date) {
    params.date = filters.date
  }

  if (filters.dataSource) {
    params.dataSource = filters.dataSource
  }

  return params
}

function getErrorMessage(error, fallback = 'Unable to load flights. Please try again.') {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}
</script>

<style scoped>
.flight-search-page {
  display: grid;
  gap: 18px;
}

.flight-search-page__header {
  padding: 20px 22px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.flight-search-page__eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.flight-search-page__header h2 {
  margin: 0 0 8px;
  font-size: 24px;
}

.flight-search-page__subtitle {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.flight-search-page__filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
  gap: 14px;
  align-items: end;
  padding: 20px 22px 8px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.flight-search-page__actions {
  align-self: end;
}

.flight-search-page__error {
  margin: 0;
  padding: 12px 14px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
}

.flight-search-page__detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 18px;
}

.flight-search-page__panel {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
}

.flight-search-page__panel--placeholder {
  color: #64748b;
}

@media (max-width: 980px) {
  .flight-search-page__filters,
  .flight-search-page__detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
