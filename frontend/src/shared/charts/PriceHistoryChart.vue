<template>
  <section class="price-history-chart">
    <header class="price-history-chart__header">
      <div>
        <h3>Price History</h3>
        <p>{{ summary }}</p>
      </div>
    </header>

    <div v-if="loading" class="price-history-chart__empty">
      Loading price history...
    </div>
    <div v-else-if="!history.length" class="price-history-chart__empty">
      No price history available for this flight yet.
    </div>
    <div v-else class="price-history-chart__bars">
      <article
        v-for="entry in normalizedHistory"
        :key="entry.key"
        class="price-history-chart__bar-card"
      >
        <div class="price-history-chart__bar-meta">
          <span>{{ entry.label }}</span>
          <strong>¥{{ entry.price }}</strong>
        </div>
        <div class="price-history-chart__track">
          <div class="price-history-chart__bar" :style="{ width: `${entry.width}%` }" />
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { buildPriceHistoryChartOption } from '../../lib/format.js'

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

const option = computed(() => buildPriceHistoryChartOption(props.history))

const normalizedHistory = computed(() => {
  const labels = option.value.xAxis?.data ?? []
  const prices = option.value.series?.[0]?.data ?? []
  const maxPrice = Math.max(...prices.map(price => Number(price) || 0), 1)

  return props.history.map((entry, index) => {
    const numericPrice = Number(prices[index]) || 0
    return {
      key: `${entry.id ?? 'row'}-${entry.observedAt ?? index}`,
      label: labels[index] ?? 'Unknown time',
      price: numericPrice,
      width: Math.max((numericPrice / maxPrice) * 100, 8)
    }
  })
})

const summary = computed(() => {
  if (!props.history.length) {
    return 'History becomes available after repeated crawls capture the same flight.'
  }

  const prices = normalizedHistory.value.map(entry => entry.price)
  const lowest = Math.min(...prices)
  const highest = Math.max(...prices)
  return `${props.history.length} capture${props.history.length === 1 ? '' : 's'} recorded, from ¥${lowest} to ¥${highest}.`
})
</script>

<style scoped>
.price-history-chart {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.price-history-chart__header h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.price-history-chart__header p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.price-history-chart__empty {
  padding: 18px;
  color: #64748b;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
}

.price-history-chart__bars {
  display: grid;
  gap: 12px;
}

.price-history-chart__bar-card {
  display: grid;
  gap: 8px;
}

.price-history-chart__bar-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #334155;
  font-size: 13px;
}

.price-history-chart__bar-meta strong {
  color: #0f172a;
  font-size: 14px;
}

.price-history-chart__track {
  width: 100%;
  height: 12px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
}

.price-history-chart__bar {
  height: 100%;
  background: linear-gradient(90deg, #0f766e 0%, #14b8a6 100%);
  border-radius: 999px;
}
</style>
