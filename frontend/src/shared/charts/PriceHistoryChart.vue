<template>
  <section class="price-history-chart">
    <header class="price-history-chart__header">
      <div>
        <h3>{{ t('flights.history.title') }}</h3>
        <p>{{ summary }}</p>
      </div>
      <dl v-if="!loading && chartEntries.length" class="price-history-chart__stats">
        <div>
          <dt>{{ currentLabel }}</dt>
          <dd>￥{{ currentPrice }}</dd>
        </div>
        <div>
          <dt>{{ lowLabel }}</dt>
          <dd>￥{{ minPrice }}</dd>
        </div>
        <div>
          <dt>{{ highLabel }}</dt>
          <dd>￥{{ maxPrice }}</dd>
        </div>
      </dl>
    </header>

    <div v-if="loading" class="price-history-chart__empty">
      {{ t('flights.history.loading') }}
    </div>
    <div v-else-if="!chartEntries.length" class="price-history-chart__empty">
      {{ t('flights.history.empty') }}
    </div>
    <div v-else class="price-history-chart__body">
      <div class="price-history-chart__plot">
        <svg viewBox="0 0 320 170" preserveAspectRatio="none" class="price-history-chart__svg" aria-hidden="true">
          <g class="price-history-chart__grid">
            <line
              v-for="line in gridLines"
              :key="line.key"
              :x1="line.x1"
              :x2="line.x2"
              :y1="line.y1"
              :y2="line.y2"
            />
          </g>
          <polyline class="price-history-chart__line-shadow" :points="polylinePoints" />
          <polyline class="price-history-chart__line" :points="polylinePoints" />
          <g v-for="point in chartPoints" :key="point.key" class="price-history-chart__point">
            <circle :cx="point.x" :cy="point.y" r="4.5" />
          </g>
        </svg>
        <div class="price-history-chart__x-axis">
          <span v-for="entry in chartEntries" :key="entry.key">{{ entry.shortLabel }}</span>
        </div>
      </div>

      <div class="price-history-chart__list">
        <article
          v-for="entry in chartEntries"
          :key="entry.key"
          class="price-history-chart__list-row"
        >
          <span>{{ entry.label }}</span>
          <strong>￥{{ entry.price }}</strong>
        </article>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()

const props = defineProps({
  history: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const chartEntries = computed(() => {
  const rows = Array.isArray(props.history) ? [...props.history].slice(-10) : []
  return rows.map((entry, index) => {
    const price = Number(entry?.price ?? entry?.snapshotPrice ?? 0) || 0
    const observedAt = entry?.observedAt || entry?.capturedAt || entry?.createdAt || ''
    const date = observedAt ? new Date(observedAt) : null
    const label = date && !Number.isNaN(date.getTime())
      ? formatDateLabel(date)
      : t('flights.history.unknownTime')
    const shortLabel = date && !Number.isNaN(date.getTime())
      ? formatShortTime(date)
      : `${index + 1}`

    return {
      key: `${entry?.id ?? 'row'}-${observedAt || index}`,
      price,
      label,
      shortLabel
    }
  })
})

const prices = computed(() => chartEntries.value.map(entry => entry.price))
const minPrice = computed(() => (prices.value.length ? Math.min(...prices.value) : 0))
const maxPrice = computed(() => (prices.value.length ? Math.max(...prices.value) : 0))
const currentPrice = computed(() => (prices.value.length ? prices.value[prices.value.length - 1] : 0))

const chartPoints = computed(() => {
  const entries = chartEntries.value
  if (!entries.length) return []

  const left = 20
  const right = 300
  const top = 18
  const bottom = 130
  const range = Math.max(maxPrice.value - minPrice.value, 1)
  const step = entries.length > 1 ? (right - left) / (entries.length - 1) : 0

  return entries.map((entry, index) => {
    const x = entries.length > 1 ? left + step * index : (left + right) / 2
    const ratio = (entry.price - minPrice.value) / range
    const y = bottom - ratio * (bottom - top)

    return {
      key: entry.key,
      x: Number(x.toFixed(2)),
      y: Number(y.toFixed(2))
    }
  })
})

const polylinePoints = computed(() =>
  chartPoints.value.map(point => `${point.x},${point.y}`).join(' ')
)

const gridLines = computed(() => {
  const top = 18
  const bottom = 130
  const left = 20
  const right = 300
  const lines = 4
  return Array.from({ length: lines }, (_, index) => {
    const y = top + ((bottom - top) / (lines - 1)) * index
    return {
      key: `line-${index}`,
      x1: left,
      x2: right,
      y1: y,
      y2: y
    }
  })
})

const summary = computed(() => {
  if (!chartEntries.value.length) {
    return t('flights.history.waiting')
  }

  return locale.value === 'zh-CN'
    ? `当前价 ￥${currentPrice.value} / 最低 ￥${minPrice.value} / 最高 ￥${maxPrice.value}`
    : `Current ￥${currentPrice.value} / Low ￥${minPrice.value} / High ￥${maxPrice.value}`
})

const currentLabel = computed(() => (locale.value === 'zh-CN' ? '当前价' : 'Current'))
const lowLabel = computed(() => (locale.value === 'zh-CN' ? '最低价' : 'Low'))
const highLabel = computed(() => (locale.value === 'zh-CN' ? '最高价' : 'High'))

function formatDateLabel(date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function formatShortTime(date) {
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hours = `${date.getHours()}`.padStart(2, '0')
  const minutes = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}
</script>

<style scoped>
.price-history-chart {
  display: grid;
  gap: 8px;
  min-height: 0;
}

.price-history-chart__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.price-history-chart__header h3 {
  margin: 0 0 2px;
  font-size: 15px;
  color: #0f172a;
}

.price-history-chart__header p {
  margin: 0;
  color: #64748b;
  font-size: 11px;
  line-height: 1.3;
}

.price-history-chart__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin: 0;
}

.price-history-chart__stats dt {
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
}

.price-history-chart__stats dd {
  margin: 2px 0 0;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
}

.price-history-chart__empty {
  padding: 12px;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
}

.price-history-chart__body {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(118px, 0.8fr);
  gap: 8px;
  min-height: 0;
}

.price-history-chart__plot {
  display: grid;
  gap: 6px;
  min-height: 0;
}

.price-history-chart__svg {
  width: 100%;
  height: 138px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border: 1px solid #dbeafe;
  border-radius: 8px;
}

.price-history-chart__grid line {
  stroke: #dbeafe;
  stroke-width: 1;
}

.price-history-chart__line-shadow {
  fill: none;
  stroke: rgba(37, 99, 235, 0.16);
  stroke-width: 6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.price-history-chart__line {
  fill: none;
  stroke: #2563eb;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.price-history-chart__point circle {
  fill: #ffffff;
  stroke: #2563eb;
  stroke-width: 3;
}

.price-history-chart__x-axis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(44px, 1fr));
  gap: 4px;
  color: #64748b;
  font-size: 10px;
}

.price-history-chart__list {
  display: grid;
  gap: 6px;
  max-height: 178px;
  overflow: auto;
  padding-right: 4px;
}

.price-history-chart__list-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  font-size: 11px;
  color: #334155;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 7px;
}

.price-history-chart__list-row strong {
  color: #0f172a;
  font-size: 12px;
}

@media (max-width: 900px) {
  .price-history-chart__header,
  .price-history-chart__body {
    grid-template-columns: 1fr;
  }
}
</style>
