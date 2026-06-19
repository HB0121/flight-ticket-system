import { buildAirportOptionLabel } from '../constants/airportOptions.js'

const SUCCESS_STATUSES = new Set(['scheduled', 'success'])
const FAILED_STATUSES = new Set(['cancelled', 'failed'])
const DELAYED_STATUSES = new Set(['delayed'])

const AIRLINE_NAME_MAP = {
  'china southern': {
    zh: '中国南方航空',
    en: 'China Southern'
  },
  'china southern airlines': {
    zh: '中国南方航空',
    en: 'China Southern'
  },
  'air china': {
    zh: '中国国际航空',
    en: 'Air China'
  },
  'china eastern': {
    zh: '中国东方航空',
    en: 'China Eastern'
  },
  'china eastern airlines': {
    zh: '中国东方航空',
    en: 'China Eastern'
  },
  'sichuan airlines': {
    zh: '四川航空',
    en: 'Sichuan Airlines'
  },
  'hainan airlines': {
    zh: '海南航空',
    en: 'Hainan Airlines'
  },
  'shenzhen airlines': {
    zh: '深圳航空',
    en: 'Shenzhen Airlines'
  },
  'xiamen airlines': {
    zh: '厦门航空',
    en: 'Xiamen Airlines'
  },
  'spring airlines': {
    zh: '春秋航空',
    en: 'Spring Airlines'
  },
  'juneyao airlines': {
    zh: '吉祥航空',
    en: 'Juneyao Airlines'
  },
  'ok airways': {
    zh: '奥凯航空',
    en: 'OK Airways'
  },
  'tianjin airlines': {
    zh: '天津航空',
    en: 'Tianjin Airlines'
  },
  'shandong airlines': {
    zh: '山东航空',
    en: 'Shandong Airlines'
  }
}

export function normalizeStatusTone(status) {
  const normalizedStatus = String(status ?? '').trim().toLowerCase()

  if (SUCCESS_STATUSES.has(normalizedStatus)) {
    return 'success'
  }

  if (FAILED_STATUSES.has(normalizedStatus)) {
    return 'failed'
  }

  if (DELAYED_STATUSES.has(normalizedStatus)) {
    return 'warning'
  }

  return 'neutral'
}

export function buildAirlineDisplayLabel(airlineName, locale = 'zh-CN') {
  const rawName = String(airlineName ?? '').trim()

  if (!rawName) {
    return locale === 'zh-CN' ? '未知航司' : 'Unknown Airline'
  }

  const mapped = AIRLINE_NAME_MAP[rawName.toLowerCase()]
  if (!mapped) {
    return rawName
  }

  return locale === 'zh-CN' ? mapped.zh : mapped.en
}

export function normalizeFlightForDisplay(flight, locale = 'zh-CN') {
  const fromCode = String(flight?.fromAirport ?? '').trim().toUpperCase()
  const toCode = String(flight?.toAirport ?? '').trim().toUpperCase()
  const fromAirportLabel = buildAirportOptionLabel(fromCode || flight?.fromAirport || '-', locale)
  const toAirportLabel = buildAirportOptionLabel(toCode || flight?.toAirport || '-', locale)
  const airlineRawName = String(flight?.airlineName ?? '').trim()
  const airlineLabel = buildAirlineDisplayLabel(airlineRawName, locale)

  return {
    ...flight,
    airlineRawName,
    airlineName: airlineLabel,
    airlineLabel,
    fromAirportCode: fromCode,
    toAirportCode: toCode,
    fromAirportLabel,
    toAirportLabel,
    routeLabel: `${fromAirportLabel} -> ${toAirportLabel}`,
    statusLabel: flight?.status || 'Unknown',
    statusTone: normalizeStatusTone(flight?.status)
  }
}

export function matchesTimeSlot(flight, slot) {
  if (!slot) {
    return true
  }

  const departTime = flight?.departTime ? new Date(flight.departTime) : null
  const hour = Number.isNaN(departTime?.getTime?.()) ? null : departTime.getHours()

  if (hour === null) {
    return false
  }

  if (slot === 'overnight') {
    return hour < 6
  }

  if (slot === 'morning') {
    return hour >= 6 && hour < 12
  }

  if (slot === 'afternoon') {
    return hour >= 12 && hour < 18
  }

  if (slot === 'evening') {
    return hour >= 18
  }

  return true
}
