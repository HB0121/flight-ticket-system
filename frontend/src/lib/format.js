export function formatAdviceSummary(response) {
  if (!response) {
    return ''
  }
  const flight = response.recommendedFlight
  if (!flight) {
    return response.summary || '暂无推荐结果'
  }
  return `${response.summary} 推荐航线：${flight.fromCity} → ${flight.toCity}，航班 ${flight.flightNo}，票价 ${flight.price} 元。`
}

export function buildPriceChartOption(flights) {
  const rows = Array.isArray(flights) ? flights : []
  return {
    tooltip: {
      trigger: 'axis',
      valueFormatter: value => `${value} 元`
    },
    grid: {
      top: 24,
      right: 20,
      bottom: 36,
      left: 48
    },
    xAxis: {
      type: 'category',
      data: rows.map(flight => flight.flightNo),
      axisTick: { alignWithLabel: true }
    },
    yAxis: {
      type: 'value',
      name: '价格',
      axisLabel: {
        formatter: '{value} 元'
      }
    },
    series: [
      {
        name: '票价',
        type: 'bar',
        data: rows.map(flight => Number(flight.price)),
        barMaxWidth: 42,
        itemStyle: {
          color: '#2563eb',
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }
}

export function buildPriceHistoryChartOption(history) {
  const rows = Array.isArray(history) ? history : []
  return {
    tooltip: {
      trigger: 'axis',
      valueFormatter: value => `${value} 元`
    },
    grid: {
      top: 24,
      right: 20,
      bottom: 36,
      left: 48
    },
    xAxis: {
      type: 'category',
      data: rows.map(row => formatDateTime(row.observedAt))
    },
    yAxis: {
      type: 'value',
      name: '价格',
      axisLabel: {
        formatter: '{value} 元'
      }
    },
    series: [
      {
        name: '价格历史',
        type: 'line',
        smooth: true,
        data: rows.map(row => Number(row.price)),
        itemStyle: { color: '#0f766e' },
        areaStyle: { color: 'rgba(15, 118, 110, 0.12)' }
      }
    ]
  }
}

const LIVE_CRAWLER_SOURCES = new Set(['ctrip_live', 'fliggy_live', 'qunar_live'])

export function isLiveCrawlerSource(source) {
  return LIVE_CRAWLER_SOURCES.has(source)
}

export function buildCrawlerPayload(form) {
  const source = form?.source || 'amadeus'
  if (source !== 'amadeus' && !isLiveCrawlerSource(source)) {
    throw new Error(`unsupported source: ${source}`)
  }
  return {
    source,
    fromCity: form.fromCity || '上海',
    toCity: form.toCity || '北京',
    date: form.date,
    adults: Number(form.adults || 1),
    maxResults: Number(form.maxResults || 5)
  }
}

export function formatTimingReport(response) {
  if (!response) {
    return ''
  }
  return `风险等级：${response.riskLevel || 'UNKNOWN'}。${response.summary || '暂无分析结果'} ${response.buyWindow || ''}`.trim()
}

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}
