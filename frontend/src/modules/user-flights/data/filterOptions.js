export const PRICE_RANGE_OPTIONS = [
  { value: '', labelKey: 'priceAny' },
  { value: '0-1000', labelKey: 'priceLow' },
  { value: '1000-2000', labelKey: 'priceMid' },
  { value: '2000+', labelKey: 'priceHigh' }
]

export const FLIGHT_STATUS_OPTIONS = [
  { value: '', labelKey: 'statusAny' },
  { value: 'Scheduled', labelKey: 'statusScheduled' },
  { value: 'Delayed', labelKey: 'statusDelayed' },
  { value: 'Cancelled', labelKey: 'statusCancelled' }
]

export const DEPART_SLOT_OPTIONS = [
  { value: '', labelKey: 'slotAny' },
  { value: 'overnight', labelKey: 'slotOvernight' },
  { value: 'morning', labelKey: 'slotMorning' },
  { value: 'afternoon', labelKey: 'slotAfternoon' },
  { value: 'evening', labelKey: 'slotEvening' }
]

export function matchesPriceRange(price, range) {
  const numericPrice = Number(price)
  if (!Number.isFinite(numericPrice)) return false
  if (range === '0-1000') return numericPrice <= 1000
  if (range === '1000-2000') return numericPrice > 1000 && numericPrice <= 2000
  if (range === '2000+') return numericPrice > 2000
  return true
}

export function normalizeStatusValue(status) {
  return String(status ?? '').trim().toLowerCase()
}
