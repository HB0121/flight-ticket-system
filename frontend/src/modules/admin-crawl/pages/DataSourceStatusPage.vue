<template>
  <section class="data-source-status-page">
    <header class="data-source-status-page__header">
      <div>
        <p class="data-source-status-page__eyebrow">Admin Data Sources</p>
        <h2>Inspect basic crawl source configuration</h2>
        <p class="data-source-status-page__subtitle">
          Phase 1 keeps this view intentionally small: source code, mode, configuration readiness, and a short detail.
        </p>
      </div>
      <button :disabled="loading" type="button" @click="loadStatuses">
        {{ loading ? 'Refreshing...' : 'Refresh' }}
      </button>
    </header>

    <p v-if="errorMessage" class="data-source-status-page__error">{{ errorMessage }}</p>

    <section class="data-source-status-page__grid">
      <article v-for="status in statuses" :key="status.code" class="data-source-status-page__card">
        <div class="data-source-status-page__row">
          <h3>{{ status.label }}</h3>
          <span :class="['data-source-status-page__badge', status.configured ? 'is-configured' : 'is-not-configured']">
            {{ status.configured ? 'Configured' : 'Not Configured' }}
          </span>
        </div>
        <p class="data-source-status-page__meta">{{ status.code }} | {{ status.mode }}</p>
        <p class="data-source-status-page__detail">{{ status.detail }}</p>
      </article>
    </section>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { listDataSourceStatuses } from '../../../api/adminCrawlApi.js'

const loading = ref(false)
const errorMessage = ref('')
const statuses = ref([])

onMounted(() => {
  loadStatuses()
})

async function loadStatuses() {
  loading.value = true
  errorMessage.value = ''

  try {
    const rows = await listDataSourceStatuses()
    statuses.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    statuses.value = []
    errorMessage.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function getErrorMessage(error) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? 'Unable to load data-source status right now.'
}
</script>

<style scoped>
.data-source-status-page {
  display: grid;
  gap: 18px;
}

.data-source-status-page__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.data-source-status-page__eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.data-source-status-page__header h2 {
  margin: 0 0 8px;
}

.data-source-status-page__subtitle {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.data-source-status-page__header button {
  min-height: 40px;
  padding: 0 14px;
  color: #ffffff;
  background: #0f766e;
  border: 1px solid #0f766e;
  border-radius: 10px;
  font: inherit;
  cursor: pointer;
}

.data-source-status-page__error {
  margin: 0;
  padding: 12px 14px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
}

.data-source-status-page__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.data-source-status-page__card {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.data-source-status-page__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.data-source-status-page__row h3 {
  margin: 0;
}

.data-source-status-page__badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.data-source-status-page__badge.is-configured {
  color: #166534;
  background: #dcfce7;
}

.data-source-status-page__badge.is-not-configured {
  color: #991b1b;
  background: #fee2e2;
}

.data-source-status-page__meta,
.data-source-status-page__detail {
  margin: 10px 0 0;
  color: #475569;
}

@media (max-width: 720px) {
  .data-source-status-page__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
