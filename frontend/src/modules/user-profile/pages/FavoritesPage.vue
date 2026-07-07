<template>
  <section class="profile-page">
    <!-- 收藏页头部提供手动刷新入口，便于从其他页面修改收藏后重新同步。 -->
    <header class="profile-page__header">
      <div>
        <p class="profile-page__eyebrow">{{ t('favorites.eyebrow') }}</p>
        <h2>{{ t('favorites.title') }}</h2>
        <p class="profile-page__subtitle">
          {{ t('favorites.subtitle') }}
        </p>
      </div>
      <el-button :loading="loading" type="primary" @click="loadFavorites">{{ t('common.actions.refresh') }}</el-button>
    </header>

    <p v-if="errorMessage" class="profile-page__error">{{ errorMessage }}</p>

    <div v-if="loading && !favorites.length" class="profile-page__empty">{{ t('common.status.loadingFavorites') }}</div>
    <div v-else-if="!favorites.length" class="profile-page__empty">{{ t('favorites.empty') }}</div>

    <!-- 收藏列表由当前登录用户决定，删除按钮会调用个人中心接口。 -->
    <div v-else class="favorites-list">
      <article v-for="favorite in displayFavorites" :key="favorite.id" class="favorites-card">
        <div class="favorites-card__summary">
          <div>
            <p class="favorites-card__flight">{{ favorite.flightNo || '-' }}</p>
            <p class="favorites-card__route">
              {{ favorite.fromCity || '-' }} -> {{ favorite.toCity || '-' }}
            </p>
          </div>
          <strong class="favorites-card__price">CNY {{ formatPrice(favorite.price) }}</strong>
        </div>

        <dl class="favorites-card__meta">
          <div>
            <dt>{{ t('common.labels.departure') }}</dt>
            <dd>{{ formatDateTime(favorite.departTime) }}</dd>
          </div>
          <div>
            <dt>{{ t('common.labels.airports') }}</dt>
            <dd>{{ favorite.fromAirportLabel || favorite.fromAirport || '-' }} -> {{ favorite.toAirportLabel || favorite.toAirport || '-' }}</dd>
          </div>
          <div>
            <dt>{{ t('common.labels.source') }}</dt>
            <dd>{{ favorite.dataSource || '-' }}</dd>
          </div>
        </dl>

        <div class="favorites-card__actions">
          <el-button :loading="removingId === favorite.id" @click="remove(favorite.id)">{{ t('common.actions.remove') }}</el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onActivated, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchFavorites, removeFavorite } from '../../../api/profileApi.js'

defineOptions({ name: 'FavoritesPage' })
import { formatDateTime } from '../../../lib/format.js'
import { normalizeFlightForDisplay } from '../../../shared/utils/flightDisplay.js'
import { formatPrice } from '../../../shared/utils/price.js'

const { t, locale } = useI18n()

const favorites = ref([])
const loading = ref(false)
const removingId = ref(null)
const errorMessage = ref('')

const displayFavorites = computed(() =>
  favorites.value.map(favorite => normalizeFlightForDisplay(favorite, locale.value))
)

onMounted(() => {
  loadFavorites()
})
onActivated(() => {
  loadFavorites()
})

// 页面挂载或从其他 KeepAlive 页面返回时，重新读取当前用户收藏。
async function loadFavorites() {
  loading.value = true
  errorMessage.value = ''

  try {
    const rows = await fetchFavorites()
    favorites.value = Array.isArray(rows) ? rows : []
  } catch (error) {
    favorites.value = []
    errorMessage.value = getErrorMessage(error, t('favorites.errors.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 删除收藏后重新拉取列表，确保页面和后端状态一致。
async function remove(favoriteId) {
  removingId.value = favoriteId
  errorMessage.value = ''

  try {
    await removeFavorite(favoriteId)
    await loadFavorites()
  } catch (error) {
    errorMessage.value = getErrorMessage(error, t('favorites.errors.removeFailed'))
  } finally {
    removingId.value = null
  }
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
}

.profile-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.favorites-list {
  display: grid;
  gap: 16px;
}

.favorites-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.favorites-card__summary {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.favorites-card__flight {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
}

.favorites-card__route {
  margin: 0;
  color: #475569;
}

.favorites-card__price {
  color: #0f766e;
  font-size: 24px;
}

.favorites-card__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.favorites-card__meta div {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.favorites-card__meta dt {
  color: #64748b;
  font-size: 12px;
}

.favorites-card__meta dd {
  margin: 6px 0 0;
  color: #0f172a;
  font-weight: 600;
}

.favorites-card__actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 820px) {
  .profile-page__header,
  .favorites-card__summary {
    flex-direction: column;
  }

  .favorites-card__meta {
    grid-template-columns: 1fr;
  }
}
</style>
