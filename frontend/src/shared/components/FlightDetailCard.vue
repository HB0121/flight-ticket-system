<template>
  <section class="flight-detail-card">
    <header class="flight-detail-card__header">
      <div>
        <p class="flight-detail-card__eyebrow">Selected Flight</p>
        <h3>{{ flight.flightNo || '-' }}</h3>
      </div>
      <strong class="flight-detail-card__price">¥{{ formatPrice(flight.price) }}</strong>
    </header>

    <dl class="flight-detail-card__grid">
      <div>
        <dt>Airline</dt>
        <dd>{{ flight.airlineName || '-' }}</dd>
      </div>
      <div>
        <dt>Route</dt>
        <dd>{{ flight.fromCity || '-' }} → {{ flight.toCity || '-' }}</dd>
      </div>
      <div>
        <dt>Airports</dt>
        <dd>{{ flight.fromAirport || '-' }} → {{ flight.toAirport || '-' }}</dd>
      </div>
      <div>
        <dt>Departure</dt>
        <dd>{{ formatDateTime(flight.departTime) }}</dd>
      </div>
      <div>
        <dt>Arrival</dt>
        <dd>{{ formatDateTime(flight.arriveTime) }}</dd>
      </div>
      <div>
        <dt>Seats Left</dt>
        <dd>{{ flight.seatsLeft ?? '-' }}</dd>
      </div>
      <div>
        <dt>Source</dt>
        <dd>{{ flight.dataSource || '-' }}</dd>
      </div>
      <div>
        <dt>Collected</dt>
        <dd>{{ formatDateTime(flight.collectedAt) }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup>
import { formatDateTime } from '../../lib/format.js'
import { formatPrice } from '../utils/price.js'

defineProps({
  flight: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.flight-detail-card {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.flight-detail-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.flight-detail-card__eyebrow {
  margin: 0 0 6px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.flight-detail-card__header h3 {
  margin: 0;
  font-size: 22px;
}

.flight-detail-card__price {
  color: #0f766e;
  font-size: 24px;
}

.flight-detail-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
}

.flight-detail-card__grid div {
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.flight-detail-card__grid dt {
  color: #64748b;
  font-size: 12px;
}

.flight-detail-card__grid dd {
  margin: 6px 0 0;
  color: #0f172a;
  font-weight: 600;
}

@media (max-width: 720px) {
  .flight-detail-card__header {
    flex-direction: column;
  }

  .flight-detail-card__grid {
    grid-template-columns: 1fr;
  }
}
</style>
