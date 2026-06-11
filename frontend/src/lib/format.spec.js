import { describe, expect, it } from 'vitest'
import { buildPriceChartOption, formatAdviceSummary } from './format.js'

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

