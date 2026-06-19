<template>
  <section class="admin-crawl-page">
    <header class="admin-crawl-page__header">
      <div>
        <p class="admin-crawl-page__eyebrow">{{ t('admin.crawlJobs.eyebrow') }}</p>
        <h2>{{ t('admin.crawlJobs.title') }}</h2>
        <p class="admin-crawl-page__subtitle">
          {{ t('admin.crawlJobs.subtitle') }}
        </p>
      </div>
    </header>

    <form class="admin-crawl-page__form" @submit.prevent="submitJob">
      <label>
        <span>{{ t('admin.crawlJobs.form.source') }}</span>
        <select v-model="form.source" :disabled="loadingStatuses || !availableSources.length">
          <option v-for="source in availableSources" :key="source" :value="source">{{ source }}</option>
        </select>
      </label>

      <label>
        <span>{{ t('admin.crawlJobs.form.from') }}</span>
        <input v-model.trim="form.fromCity" :placeholder="t('admin.crawlJobs.placeholders.from')" type="text">
      </label>

      <label>
        <span>{{ t('admin.crawlJobs.form.to') }}</span>
        <input v-model.trim="form.toCity" :placeholder="t('admin.crawlJobs.placeholders.to')" type="text">
      </label>

      <label>
        <span>{{ t('admin.crawlJobs.form.date') }}</span>
        <input v-model="form.date" type="date">
      </label>

      <label>
        <span>{{ t('admin.crawlJobs.form.adults') }}</span>
        <input v-model.number="form.adults" min="1" type="number">
      </label>

      <label>
        <span>{{ t('admin.crawlJobs.form.maxResults') }}</span>
        <input v-model.number="form.maxResults" min="1" type="number">
      </label>

      <button :disabled="submitting || loadingStatuses || !canSubmit" type="submit">
        {{ submitting ? t('admin.crawlJobs.actions.submitting') : t('admin.crawlJobs.actions.submit') }}
      </button>
    </form>

    <p v-if="errorMessage" class="admin-crawl-page__error">{{ errorMessage }}</p>
    <p v-else-if="!canSubmit && unavailableReason" class="admin-crawl-page__error">{{ unavailableReason }}</p>

    <section class="admin-crawl-page__panel">
      <div class="admin-crawl-page__panel-header">
        <h3>{{ t('admin.crawlJobs.recentJobs.title') }}</h3>
        <button :disabled="loadingJobs" type="button" @click="loadJobs">
          {{ loadingJobs ? t('admin.crawlJobs.actions.refreshing') : t('admin.crawlJobs.actions.refresh') }}
        </button>
      </div>

      <p v-if="!jobs.length && !loadingJobs" class="admin-crawl-page__empty">{{ t('admin.crawlJobs.recentJobs.empty') }}</p>

      <table v-else class="admin-crawl-page__table">
        <thead>
          <tr>
            <th>{{ t('admin.crawlJobs.recentJobs.columns.id') }}</th>
            <th>{{ t('admin.crawlJobs.recentJobs.columns.source') }}</th>
            <th>{{ t('admin.crawlJobs.recentJobs.columns.status') }}</th>
            <th>{{ t('admin.crawlJobs.recentJobs.columns.success') }}</th>
            <th>{{ t('admin.crawlJobs.recentJobs.columns.failed') }}</th>
            <th>{{ t('admin.crawlJobs.recentJobs.columns.started') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in jobs" :key="job.id ?? `${job.source}-${job.startedAt}`">
            <td>{{ job.id ?? '-' }}</td>
            <td>{{ job.source ?? '-' }}</td>
            <td>{{ job.status ?? 'UNKNOWN' }}</td>
            <td>{{ job.successCount ?? 0 }}</td>
            <td>{{ job.failedCount ?? 0 }}</td>
            <td>{{ formatTimestamp(job.startedAt) }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { createCrawlJob, listCrawlJobs, listDataSourceStatuses } from '../../../api/adminCrawlApi.js'

const { t } = useI18n()

const loadingJobs = ref(false)
const loadingStatuses = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const jobs = ref([])
const configuredStatuses = ref([])

const form = reactive({
  source: 'amadeus',
  fromCity: '',
  toCity: '',
  date: '',
  adults: 1,
  maxResults: 5
})

onMounted(() => {
  loadStatuses()
  loadJobs()
})

async function loadStatuses() {
  loadingStatuses.value = true

  try {
    const rows = await listDataSourceStatuses()
    configuredStatuses.value = Array.isArray(rows) ? rows : []
    if (!availableSources.value.includes(form.source)) {
      form.source = availableSources.value[0] ?? ''
    }
  } catch (error) {
    configuredStatuses.value = []
    errorMessage.value = getErrorMessage(error, t('admin.crawlJobs.errors.loadStatusesFailed'))
  } finally {
    loadingStatuses.value = false
  }
}

async function loadJobs() {
  loadingJobs.value = true
  errorMessage.value = ''

  try {
    const rows = await listCrawlJobs()
    jobs.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    jobs.value = []
    errorMessage.value = getErrorMessage(error, t('admin.crawlJobs.errors.loadJobsFailed'))
  } finally {
    loadingJobs.value = false
  }
}

async function submitJob() {
  if (!canSubmit.value) {
    errorMessage.value = unavailableReason.value || t('admin.crawlJobs.errors.noConfiguredSource')
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await createCrawlJob({
      source: form.source,
      fromCity: normalizeOptional(form.fromCity),
      toCity: normalizeOptional(form.toCity),
      date: normalizeOptional(form.date),
      adults: normalizePositiveInt(form.adults, 1),
      maxResults: normalizePositiveInt(form.maxResults, 5)
    })
    await loadJobs()
  } catch (error) {
    errorMessage.value = getErrorMessage(error, t('admin.crawlJobs.errors.createFailed'))
  } finally {
    submitting.value = false
  }
}

function normalizeOptional(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

const availableSources = computed(() => configuredStatuses.value.map(status => status.code))
const canSubmit = computed(() => configuredStatuses.value.some(status => status.code === form.source && status.configured))
const unavailableReason = computed(() => (
  configuredStatuses.value.find(status => status.code === form.source && !status.configured)?.detail ?? ''
))

function normalizePositiveInt(value, fallback) {
  return Number.isInteger(value) && value > 0 ? value : fallback
}

function formatTimestamp(value) {
  if (!value) {
    return '-'
  }

  return String(value).replace('T', ' ')
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}
</script>

<style scoped>
.admin-crawl-page {
  display: grid;
  gap: 18px;
}

.admin-crawl-page__header,
.admin-crawl-page__form,
.admin-crawl-page__panel {
  padding: 20px 22px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.admin-crawl-page__eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-crawl-page__header h2,
.admin-crawl-page__panel h3 {
  margin: 0 0 8px;
}

.admin-crawl-page__subtitle {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.admin-crawl-page__form {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.admin-crawl-page__form label {
  display: grid;
  gap: 6px;
  font-size: 14px;
  color: #0f172a;
}

.admin-crawl-page__form input,
.admin-crawl-page__form select,
.admin-crawl-page__form button,
.admin-crawl-page__panel button {
  min-height: 40px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 0 12px;
  font: inherit;
}

.admin-crawl-page__form button,
.admin-crawl-page__panel button {
  color: #ffffff;
  background: #0f766e;
  border-color: #0f766e;
  cursor: pointer;
}

.admin-crawl-page__panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.admin-crawl-page__error {
  margin: 0;
  padding: 12px 14px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
}

.admin-crawl-page__empty {
  margin: 0;
  color: #64748b;
}

.admin-crawl-page__table {
  width: 100%;
  border-collapse: collapse;
}

.admin-crawl-page__table th,
.admin-crawl-page__table td {
  padding: 10px 12px;
  border-bottom: 1px solid #e2e8f0;
  text-align: left;
  font-size: 14px;
}

@media (max-width: 1100px) {
  .admin-crawl-page__form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .admin-crawl-page__form {
    grid-template-columns: 1fr;
  }

  .admin-crawl-page__panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
