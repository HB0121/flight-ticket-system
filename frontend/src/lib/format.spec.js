import { describe, expect, it } from 'vitest'
import {
  buildCrawlerPayload,
  buildPriceChartOption,
  buildPriceHistoryChartOption,
  formatAdviceSummary,
  formatTimingReport
} from './format.js'

describe('formatAdviceSummary', () => {
  it('uses summary and recommended flight information from backend response', () => {
    const text = formatAdviceSummary({
      summary: '推荐 MU5101，价格 980 元，属于预算内选择。',
      recommendedFlight: {
        flightNo: 'MU5101',
        fromCity: '上海',
        toCity: '北京',
        price: 980
      }
    })

    expect(text).toContain('MU5101')
    expect(text).toContain('上海')
    expect(text).toContain('北京')
    expect(text).toContain('980')
  })
})

describe('buildPriceChartOption', () => {
  it('builds bar chart labels and values from flights', () => {
    const option = buildPriceChartOption([
      { flightNo: 'MU5101', price: 980 },
      { flightNo: 'CA1502', price: 1280 }
    ])

    expect(option.xAxis.data).toEqual(['MU5101', 'CA1502'])
    expect(option.series[0].data).toEqual([980, 1280])
  })
})

describe('buildCrawlerPayload', () => {
  it('normalizes collection form values for backend request', () => {
    expect(buildCrawlerPayload({
      source: 'amadeus',
      fromCity: '上海',
      toCity: '北京',
      date: '2026-06-19',
      adults: 1,
      maxResults: 3
    })).toEqual({
      source: 'amadeus',
      fromCity: '上海',
      toCity: '北京',
      date: '2026-06-19',
      adults: 1,
      maxResults: 3
    })
  })
})

describe('buildPriceHistoryChartOption', () => {
  it('builds line chart labels and values from price snapshots', () => {
    const option = buildPriceHistoryChartOption([
      { observedAt: '2026-06-11T10:00:00', price: 1020 },
      { observedAt: '2026-06-11T11:00:00', price: 980 }
    ])

    expect(option.xAxis.data).toEqual(['2026-06-11 10:00', '2026-06-11 11:00'])
    expect(option.series[0].data).toEqual([1020, 980])
  })
})

describe('formatTimingReport', () => {
  it('formats timing report summary and risk level', () => {
    const text = formatTimingReport({
      summary: '本地分析：价格正在下降。',
      riskLevel: 'LOW',
      buyWindow: '建议 3 天内关注'
    })

    expect(text).toContain('LOW')
    expect(text).toContain('建议 3 天内关注')
    expect(text).toContain('价格正在下降')
  })
})
