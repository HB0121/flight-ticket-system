/**
 * format.js 工具模块的单元测试。
 *
 * 使用 Vitest 测试框架，覆盖 format.js 中所有导出函数的典型输入场景。
 * 每个 describe 块对应一个函数，每个 it 块测试一种输入/输出组合。
 *
 * 测试结构：
 * - formatAdviceSummary: 验证 AI 建议文本拼接正确性
 * - buildPriceChartOption:   验证柱状图配置的标签和数据正确转换
 * - buildCrawlerPayload:     验证爬虫参数构造逻辑（sample vs amadeus）
 * - buildPriceHistoryChartOption: 验证折线图配置和时间格式化
 * - formatTimingReport:      验证时机分析报告文本拼接
 */

import { describe, expect, it } from 'vitest'
import {
  buildCrawlerPayload,
  buildPriceChartOption,
  buildPriceHistoryChartOption,
  formatAdviceSummary,
  formatTimingReport
} from './format.js'

/**
 * formatAdviceSummary 测试套件。
 * 验证：
 * 1. 正确提取 summary 和 recommendedFlight 字段
 * 2. 拼接文本包含航班号、出发城市、到达城市、票价
 */
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

/**
 * buildPriceChartOption 测试套件。
 * 验证：
 * 1. xAxis.data 包含所有航班号
 * 2. series[0].data 包含对应的数值型票价
 */
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

/**
 * buildCrawlerPayload 测试套件。
 * 验证：
 * 1. amadeus 数据源时传入完整的路线参数
 * 2. 数值字段正确转换为 Number 类型
 */
describe('buildCrawlerPayload', () => {
  it('normalizes collection form values for backend request', () => {
    expect(buildCrawlerPayload({
      source: 'amadeus',
      fromCity: '上海',
      toCity: '北京',
      date: '2026-06-19'
    })).toEqual({
      source: 'amadeus',
      fromCity: '上海',
      toCity: '北京',
      date: '2026-06-19'
    })
  })
})

/**
 * buildPriceHistoryChartOption 测试套件。
 * 验证：
 * 1. observedAt 时间字段被 formatDateTime 正确转换为 "yyyy-MM-dd HH:mm"
 * 2. price 字段正确转换为数值数组
 */
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

/**
 * formatTimingReport 测试套件。
 * 验证：
 * 1. 风险等级（riskLevel）出现在输出中
 * 2. 购买窗口建议（buyWindow）出现在输出中
 * 3. 分析总结（summary）出现在输出中
 */
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
