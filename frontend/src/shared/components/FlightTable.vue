<template>
  <div class="flight-table">
    <div class="flight-table__header">
      <div>
        <h3>Matching Flights</h3>
        <p>{{ flights.length }} result{{ flights.length === 1 ? '' : 's' }} in the current query.</p>
      </div>
    </div>

    <el-table
      :data="flights"
      :empty-text="loading ? 'Loading flights...' : 'No flights found for the current filters.'"
      :row-class-name="rowClassName"
      @row-click="row => $emit('select', row)"
    >
      <el-table-column label="Flight" min-width="140">
        <template #default="{ row }">
          <div class="flight-table__primary">{{ row.flightNo || '-' }}</div>
          <div class="flight-table__secondary">{{ row.airlineName || 'Unknown airline' }}</div>
        </template>
      </el-table-column>

      <el-table-column label="Route" min-width="180">
        <template #default="{ row }">
          <div class="flight-table__primary">{{ row.fromCity || '-' }} → {{ row.toCity || '-' }}</div>
          <div class="flight-table__secondary">{{ row.fromAirport || '-' }} → {{ row.toAirport || '-' }}</div>
        </template>
      </el-table-column>

      <el-table-column label="Departure" min-width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.departTime) }}
        </template>
      </el-table-column>

      <el-table-column label="Arrival" min-width="150">
        <template #default="{ row }">
          {{ formatDateTime(row.arriveTime) }}
        </template>
      </el-table-column>

      <el-table-column label="Price" min-width="100" align="right">
        <template #default="{ row }">
          <span class="flight-table__price">¥{{ formatPrice(row.price) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="Seats" min-width="90" align="center">
        <template #default="{ row }">
          {{ row.seatsLeft ?? '-' }}
        </template>
      </el-table-column>

      <el-table-column label="Source" min-width="110">
        <template #default="{ row }">
          {{ row.dataSource || '-' }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { formatDateTime } from '../../lib/format.js'
import { formatPrice } from '../utils/price.js'

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

function rowClassName({ row }) {
  return row.id === props.selectedFlightId ? 'flight-table__row--selected' : ''
}
</script>

<style scoped>
.flight-table {
  display: grid;
  gap: 14px;
}

.flight-table__header h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.flight-table__header p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.flight-table__primary {
  color: #0f172a;
  font-weight: 600;
}

.flight-table__secondary {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.flight-table__price {
  color: #0f766e;
  font-weight: 700;
}
</style>

<style>
.flight-table__row--selected {
  --el-table-tr-bg-color: #eff6ff;
}
</style>
