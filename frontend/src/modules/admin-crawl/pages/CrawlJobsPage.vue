<template>
  <section class="admin-crawl-page">
    <header class="admin-crawl-page__header">
      <div>
        <p class="admin-crawl-page__eyebrow">Admin Crawl Jobs</p>
        <h2>Run crawl jobs through the admin boundary</h2>
        <p class="admin-crawl-page__subtitle">
          This page keeps phase-1 crawl administration limited to job creation and recent job inspection.
        </p>
      </div>
    </header>

    <form class="admin-crawl-page__form" @submit.prevent="submitJob">
      <label>
        <span>Source</span>
        <select v-model="form.source">
          <option value="sample">sample</option>
          <option value="amadeus">amadeus</option>
        </select>
      </label>

      <label>
        <span>From</span>
        <input v-model.trim="form.fromCity" placeholder="Shanghai" type="text">
      </label>

      <label>
        <span>To</span>
        <input v-model.trim="form.toCity" placeholder="Beijing" type="text">
      </label>

      <label>
        <span>Date</span>
        <input v-model="form.date" type="date">
      </label>

      <label>
        <span>Adults</span>
        <input v-model.number="form.adults" min="1" type="number">
      </label>

      <label>
        <span>Max Results</span>
        <input v-model.number="form.maxResults" min="1" type="number">
      </label>

      <button :disabled="submitting" type="submit">
        {{ submitting ? 'Submitting...' : 'Create Crawl Job' }}
      </button>
    </form>

    <p v-if="errorMessage" class="admin-crawl-page__error">{{ errorMessage }}</p>

    <section class="admin-crawl-page__panel">
      <div class="admin-crawl-page__panel-header">
        <h3>Recent Jobs</h3>
        <button :disabled="loadingJobs" type="button" @click="loadJobs">
          {{ loadingJobs ? 'Refreshing...' : 'Refresh' }}
        </button>
      </div>

      <p v-if="!jobs.length && !loadingJobs" class="admin-crawl-page__empty">No crawl jobs yet.</p>

      <table v-else class="admin-crawl-page__table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Source</th>
            <th>Status</th>
            <th>Success</th>
            <th>Failed</th>
            <th>Started</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="job in jobs" :key="job.id ?? `${job.source}-${job.startedAt}`">
            <td>{{ job.id ?? '-' }}</td>
            <td>{{ job.source ?? 'sample' }}</td>
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
import { onMounted, reactive, ref } from 'vue'
import { createCrawlJob, listCrawlJobs } from '../../../api/adminCrawlApi.js'

const loadingJobs = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const jobs = ref([])

const form = reactive({
  source: 'sample',
  fromCity: '',
  toCity: '',
  date: '',
  adults: 1,
  maxResults: 5
})

onMounted(() => {
  loadJobs()
})

async function loadJobs() {
  loadingJobs.value = true
  errorMessage.value = ''

  try {
    const rows = await listCrawlJobs()
    jobs.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    jobs.value = []
    errorMessage.value = getErrorMessage(error, 'Unable to load crawl jobs right now.')
  } finally {
    loadingJobs.value = false
  }
}

async function submitJob() {
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
    errorMessage.value = getErrorMessage(error, 'Unable to create the crawl job.')
  } finally {
    submitting.value = false
  }
}

function normalizeOptional(value) {
  return typeof value === 'string' && value.trim() ? value.trim() : null
}

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
