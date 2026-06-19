<template>
  <section class="profile-page">
    <header class="profile-page__header">
      <div>
        <p class="profile-page__eyebrow">{{ t('history.eyebrow') }}</p>
        <h2>{{ t('history.title') }}</h2>
        <p class="profile-page__subtitle">
          {{ t('history.subtitle') }}
        </p>
      </div>
    </header>

    <p v-if="errorMessage" class="profile-page__error">{{ errorMessage }}</p>

    <div v-if="loading && !rows.length" class="profile-page__empty">{{ t('common.status.loadingHistory') }}</div>
    <div v-else-if="!rows.length" class="profile-page__empty">{{ t('history.empty') }}</div>

    <div v-else class="history-list">
      <article v-for="row in rows" :key="row.id" class="history-card">
        <div class="history-card__route">
          <strong>{{ row.fromCity || '-' }}</strong>
          <span aria-hidden="true">-></span>
          <strong>{{ row.toCity || '-' }}</strong>
        </div>

        <dl class="history-card__meta">
          <div>
            <dt>{{ t('common.labels.travelDate') }}</dt>
            <dd>{{ row.travelDate || '-' }}</dd>
          </div>
          <div>
            <dt>{{ t('common.labels.source') }}</dt>
            <dd>{{ row.dataSource || '-' }}</dd>
          </div>
          <div>
            <dt>{{ t('common.labels.searchedAt') }}</dt>
            <dd>{{ formatDateTime(row.createdAt) }}</dd>
          </div>
        </dl>
      </article>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchSearchHistory } from '../../../api/profileApi.js'
import { formatDateTime } from '../../../lib/format.js'

const { t } = useI18n()

const rows = ref([])
const loading = ref(false)
const errorMessage = ref('')

onMounted(() => {
  loadHistory()
})

async function loadHistory() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetchSearchHistory()
    rows.value = Array.isArray(response) ? response : []
  } catch (error) {
    rows.value = []
    errorMessage.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function getErrorMessage(error) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? t('history.errors.loadFailed')
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
}

.profile-page__header {
  padding: 20px 22px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.profile-page__eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-page__header h2 {
  margin: 0 0 8px;
  font-size: 24px;
}

.profile-page__subtitle {
  margin: 0;
  color: #64748b;
}

.profile-page__error {
  margin: 0;
  padding: 12px 14px;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
}

.profile-page__empty {
  padding: 18px;
  color: #64748b;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
}

.history-list {
  display: grid;
  gap: 14px;
}

.history-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.history-card__route {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #0f172a;
  font-size: 18px;
}

.history-card__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.history-card__meta div {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.history-card__meta dt {
  color: #64748b;
  font-size: 12px;
}

.history-card__meta dd {
  margin: 6px 0 0;
  color: #0f172a;
  font-weight: 600;
}

@media (max-width: 820px) {
  .history-card__meta {
    grid-template-columns: 1fr;
  }
}
</style>
