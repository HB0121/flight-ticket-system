<template>
  <div class="flight-table">
    <div class="flight-table__header">
      <div>
        <h3>{{ t('flights.table.title') }}</h3>
        <p>{{ t('flights.table.results', { count: flights.length }) }}</p>
      </div>
    </div>

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
          <div class="flight-table__secondary">{{ row.fromAirportLabel || row.fromAirport || '-' }} -> {{ row.toAirportLabel || row.toAirport || '-' }}</div>
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.departure')" min-width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.departTime) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('flights.table.columns.arrival')" min-width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.arriveTime) }}
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
    </el-table>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '../../lib/format.js'
import { formatPrice } from '../utils/price.js'

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
  }
})

defineEmits(['select'])

const statusColumnLabel = computed(() => {
  if (te('flights.table.columns.status')) {
    return t('flights.table.columns.status')
  }

  return locale.value === 'zh-CN' ? '状态' : 'Status'
})

const unknownStatusLabel = computed(() => {
  if (te('flights.table.unknownStatus')) {
    return t('flights.table.unknownStatus')
  }

  return locale.value === 'zh-CN' ? '未知' : 'Unknown'
})

function rowClassName({ row }) {
  return row.id === props.selectedFlightId ? 'flight-table__row--selected' : ''
}
</script>

<style scoped>
.flight-table {
  display: grid;
  gap: 10px;
}

.flight-table__header h3 {
  margin: 0 0 4px;
  font-size: 17px;
}

.flight-table__header p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.flight-table__primary {
  color: #0f172a;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flight-table__secondary {
  margin-top: 2px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.25;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.flight-table__price {
  color: #0f766e;
  font-weight: 700;
  font-size: 14px;
}

.flight-table__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 11px;
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
  padding: 8px 0;
}

:deep(.el-table td.el-table__cell) {
  padding: 6px 0;
}

:deep(.el-table .cell) {
  line-height: 1.3;
}

:deep(.el-table th.el-table__cell .cell) {
  font-size: 12px;
  font-weight: 700;
  color: #475569;
}
</style>

<style>
.flight-table__row--selected {
  --el-table-tr-bg-color: #eff6ff;
}
</style>
