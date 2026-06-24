<template>
  <section class="flight-detail-card">
    <header class="flight-detail-card__header">
      <div>
        <p class="flight-detail-card__eyebrow">{{ t('flights.detail.eyebrow') }}</p>
        <h3>
          {{ flight.flightNo || '-' }}
          <FavoriteButton
            :flight-id="flight.id"
            :is-favorited="isFavorited"
            :favorite-id="favoriteId"
            @toggled="(fav, id) => emit('favorite-toggled', { flightId: flight.id, isFavorited: fav, favoriteId: id })"
          />
        </h3>
      </div>
      <strong class="flight-detail-card__price">¥{{ formatPrice(flight.price) }}</strong>
    </header>

    <dl class="flight-detail-card__grid">
      <div>
        <dt>{{ t('flights.detail.airline') }}</dt>
        <dd>{{ flight.airlineLabel || flight.airlineName || '-' }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.route') }}</dt>
        <dd>{{ flight.routeLabel || `${flight.fromCity || '-'} -> ${flight.toCity || '-'}` }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.airports') }}</dt>
        <dd>{{ flight.fromAirportLabel || flight.fromAirport || '-' }} -> {{ flight.toAirportLabel || flight.toAirport || '-' }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.departure') }}</dt>
        <dd>{{ formatDateTime(flight.departTime) }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.arrival') }}</dt>
        <dd>{{ formatDateTime(flight.arriveTime) }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.seatsLeft') }}</dt>
        <dd>{{ flight.seatsLeft ?? '-' }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.source') }}</dt>
        <dd>{{ flight.dataSource || '-' }}</dd>
      </div>
      <div>
        <dt>{{ t('flights.detail.collected') }}</dt>
        <dd>{{ formatDateTime(flight.collectedAt) }}</dd>
      </div>
    </dl>
  </section>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { formatDateTime } from '../../lib/format.js'
import { formatPrice } from '../utils/price.js'
import FavoriteButton from './FavoriteButton.vue'

const { t } = useI18n()

const props = defineProps({
  flight: { type: Object, required: true },
  isFavorited: { type: Boolean, default: false },
  favoriteId: { type: Number, default: null }
})

const emit = defineEmits(['favorite-toggled'])
</script>

<style scoped>
.flight-detail-card {
  padding: 10px;
  background: #ffffff;
  border: 1px solid #d9e2ef;
  border-radius: 9px;
  box-shadow: none;
}

.flight-detail-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.flight-detail-card__eyebrow {
  margin: 0 0 3px;
  color: #2563eb;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.flight-detail-card__header h3 {
  margin: 0;
  font-size: 18px;
}

.flight-detail-card__price {
  color: #0f766e;
  font-size: 18px;
}

.flight-detail-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  margin: 0;
}

.flight-detail-card__grid div {
  min-width: 0;
  padding: 7px 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.flight-detail-card__grid dt {
  color: #64748b;
  font-size: 10px;
}

.flight-detail-card__grid dd {
  margin: 3px 0 0;
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
