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

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

