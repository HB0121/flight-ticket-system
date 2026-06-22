import { describe, expect, it } from 'vitest'
import {
  DEPART_SLOT_OPTIONS,
  FLIGHT_STATUS_OPTIONS,
  PRICE_RANGE_OPTIONS,
  matchesPriceRange,
  normalizeStatusValue
} from './filterOptions.js'

describe('filterOptions', () => {
  it('keeps the existing price range option values', () => {
    expect(PRICE_RANGE_OPTIONS.map(option => option.value)).toEqual(['', '0-1000', '1000-2000', '2000+'])
  })

  it('keeps the existing status and departure slot option values', () => {
    expect(FLIGHT_STATUS_OPTIONS.map(option => option.value)).toEqual(['', 'Scheduled', 'Delayed', 'Cancelled'])
    expect(DEPART_SLOT_OPTIONS.map(option => option.value)).toEqual(['', 'overnight', 'morning', 'afternoon', 'evening'])
  })

  it('matches the existing price range boundaries', () => {
    expect(matchesPriceRange(1000, '0-1000')).toBe(true)
    expect(matchesPriceRange(1000.01, '0-1000')).toBe(false)
    expect(matchesPriceRange(1000.01, '1000-2000')).toBe(true)
    expect(matchesPriceRange(2000, '1000-2000')).toBe(true)
    expect(matchesPriceRange(2000.01, '1000-2000')).toBe(false)
    expect(matchesPriceRange(2000.01, '2000+')).toBe(true)
    expect(matchesPriceRange('not-a-price', '0-1000')).toBe(false)
    expect(matchesPriceRange(500, '')).toBe(true)
  })

  it('normalizes status values without changing comparison behavior', () => {
    expect(normalizeStatusValue(' Scheduled ')).toBe('scheduled')
    expect(normalizeStatusValue(null)).toBe('')
    expect(normalizeStatusValue(undefined)).toBe('')
  })
})
