<template>
  <div class="flight-table">
    <!-- 航班表只负责展示和抛出事件，查询、分页、收藏状态由父页面维护。 -->
    <el-table
      :data="flights"
      :empty-text="loading ? t('common.status.loadingFlights') : t('flights.table.empty')"
      :row-class-name="rowClassName"
      @row-click="row => $emit('select', row)"
    >
      <el-table-column :label="t('flights.table.columns.flight')" min-width="140">
        <template #default="{ row }">
          <div class="flight-table__primary">{{ row.flightNo || '-' }}</div>
          <div class="flight-table__secondary">{{ row.airlineLabel || row.airlineName || t('flights.table.unknownAirline') }}</div>
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.route')" min-width="220">
        <template #default="{ row }">
          <div class="flight-table__primary">{{ row.routeLabel || `${row.fromAirport || '-'} -> ${row.toAirport || '-'}` }}</div>
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.departure')" min-width="150">
        <template #default="{ row }">
          {{ formatTimeOnly(row.departTime) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.arrival')" min-width="150">
        <template #default="{ row }">
          {{ formatTimeOnly(row.arriveTime) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.price')" min-width="100" align="right">
        <template #default="{ row }">
          <span class="flight-table__price">￥{{ formatPrice(row.price) }}</span>
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.seats')" min-width="90" align="center">
        <template #default="{ row }">
          {{ row.seatsLeft ?? '-' }}
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.source')" min-width="110">
        <template #default="{ row }">
          {{ row.dataSource || '-' }}
        </template>
      </el-table-column>

      <el-table-column :label="statusColumnLabel" min-width="120" align="center">
        <template #default="{ row }">
          <span
            :data-testid="`status-tag-${row.id}`"
            :class="['flight-table__status', `flight-table__status--${row.statusTone || 'neutral'}`]"
          >
            {{ row.statusLabel || unknownStatusLabel }}
          </span>
        </template>
      </el-table-column>

      <el-table-column :label="t('common.actions.favorite')" width="54" align="center">
        <template #default="{ row }">
          <!-- 收藏按钮的结果会一路回传到父页面，更新 favoriteStatusMap。 -->
          <FavoriteButton
            :flight-id="row.id"
            :is-favorited="favoriteState(row.id).isFavorited"
            :favorite-id="favoriteState(row.id).favoriteId"
            @toggled="(fav, id) => onFavoriteToggled(row.id, fav, id)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatPrice } from '../utils/price.js'
import FavoriteButton from './FavoriteButton.vue'

const { locale, t, te } = useI18n()

const props = defineProps({
  flights: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  selectedFlightId: {
    type: [Number, String],
    default: null
  },
  favoriteStatusMap: {
    type: Map,
    default: () => new Map()
  }
})

const emit = defineEmits(['select', 'favorite-toggled'])

// 收藏状态由父页面统一维护，表格只负责按 flightId 读取当前行状态。
function favoriteState(flightId) {
  return props.favoriteStatusMap.get(flightId) || { isFavorited: false, favoriteId: null }
}

// 子组件 FavoriteButton 只知道收藏结果，表格补上 flightId 后继续抛给父页面。
function onFavoriteToggled(flightId, isFavorited, favoriteId) {
  emit('favorite-toggled', { flightId, isFavorited, favoriteId })
}

// i18n 文案缺失时提供兜底，避免表头直接显示 key。
const statusColumnLabel = computed(() => {
  if (te('flights.table.columns.status')) {
    return t('flights.table.columns.status')
  }

  return locale.value === 'zh-CN' ? '状态' : 'Status'
})

// 后端状态为空或未翻译时，统一显示可读的未知状态。
const unknownStatusLabel = computed(() => {
  if (te('flights.table.unknownStatus')) {
    return t('flights.table.unknownStatus')
  }

  return locale.value === 'zh-CN' ? '未知' : 'Unknown'
})

// Element Plus 表格通过行 class 标出当前选中的航班。
function rowClassName({ row }) {
  return row.id === props.selectedFlightId ? 'flight-table__row--selected' : ''
}

// 后端返回完整日期时间，表格中只展示 HH:mm，减少列宽占用。
function formatTimeOnly(value) {
  if (!value) return '-'
  const text = String(value).replace('T', ' ')
  const timePart = text.includes(' ') ? text.split(' ')[1] : text
  return timePart.slice(0, 5) || '-'
}
</script>

<style scoped>
.flight-table {
  display: grid;
  gap: 0;
  min-width: 0;
  height: 100%;
}

.flight-table__primary {
  color: #0f172a;
  font-weight: 600;
  font-size: 13px;
  line-height: 1.15;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flight-table__secondary {
  margin-top: 2px;
  color: #64748b;
  font-size: 10px;
  line-height: 1.1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flight-table__price {
  color: #0f766e;
  font-weight: 700;
  font-size: 13px;
}

.flight-table__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
}

.flight-table__status--success {
  color: #166534;
  background: #f0fdf4;
}

.flight-table__status--failed {
  color: #b91c1c;
  background: #fef2f2;
}

.flight-table__status--warning {
  color: #b45309;
  background: #fff7ed;
}

.flight-table__status--neutral {
  color: #475569;
  background: #f1f5f9;
}

:deep(.el-table th.el-table__cell) {
  height: 30px;
  padding: 3px 0;
}

:deep(.el-table td.el-table__cell) {
  height: 34px;
  padding: 3px 0;
}

:deep(.el-table .cell) {
  line-height: 1.15;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.el-table th.el-table__cell .cell) {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}

:deep(.el-table),
:deep(.el-table__inner-wrapper) {
  height: 100%;
}

:deep(.el-table__header-wrapper) {
  position: sticky;
  top: 0;
  z-index: 2;
}
</style>

<style>
.flight-table__row--selected {
  --el-table-tr-bg-color: #eff6ff;
}
</style>
