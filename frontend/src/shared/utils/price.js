export function formatPrice(value) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(0) : String(value)
}
