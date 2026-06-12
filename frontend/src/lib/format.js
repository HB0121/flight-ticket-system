/**
 * 格式化与图表配置工具模块。
 *
 * 纯函数集合，负责将后端 API 返回的原始数据转换为前端展示所需的格式。
 * 包括：AI 建议文本格式化、ECharts 图表配置生成、爬虫参数构造、日期格式化等。
 *
 * 设计模式：工具函数模块（Utility Module）—— 所有函数均为无副作用的纯函数，
 * 输入数据 → 输出格式化结果，便于单元测试（参见 format.spec.js）。
 */

/**
 * 格式化 AI 购票建议的展示文本。
 *
 * 将后端返回的 AI 建议响应拼接为可读的摘要字符串：
 * - 包含 AI 生成的总结文本
 * - 附上推荐航班的核心信息（航线、航班号、票价）
 * - 若无推荐航班，仅展示总结或默认提示
 *
 * @param {Object|null} response - 后端 /api/ai/advice 的响应体
 * @param {string} [response.summary] - AI 生成的总结文本
 * @param {Object} [response.recommendedFlight] - 推荐的航班对象
 * @param {string} response.recommendedFlight.flightNo - 航班号（如 MU5101）
 * @param {string} response.recommendedFlight.fromCity - 出发城市（中文）
 * @param {string} response.recommendedFlight.toCity - 到达城市（中文）
 * @param {number} response.recommendedFlight.price - 票价（元）
 * @returns {string} 格式化后的展示文本
 */
export function formatAdviceSummary(response) {
  if (!response) {
    return ''
  }
  const flight = response.recommendedFlight
  if (!flight) {
    return response.summary || '暂无推荐结果'
  }
  // 拼接总结文本 + 推荐航班关键信息
  return `${response.summary} 推荐航线：${flight.fromCity} → ${flight.toCity}，航班 ${flight.flightNo}，票价 ${flight.price} 元。`
}

/**
 * 构建航班价格柱状图的 ECharts 配置对象。
 *
 * 每个航班生成一根柱子，x 轴为航班号，y 轴为票价（元）。
 * 使用蓝色 (#2563eb) 柱状图，顶部带圆角。
 *
 * @param {Array} flights - 航班数组 [{flightNo, price}, ...]
 * @returns {Object} ECharts option 配置对象，可直接传给 ECharts 实例的 setOption()
 */
export function buildPriceChartOption(flights) {
  const rows = Array.isArray(flights) ? flights : []
  return {
    tooltip: {
      trigger: 'axis',                         // 坐标轴触发：hover 整条轴显示所有系列数据
      valueFormatter: value => `${value} 元`    // 提示框中数值后追加"元"
    },
    grid: {
      top: 24,     // 图表上边距（留出 tooltip 空间）
      right: 20,
      bottom: 36,  // 图表下边距（留出 x 轴标签空间）
      left: 48     // 图表左边距（留出 y 轴标签空间）
    },
    xAxis: {
      type: 'category',                        // 类目轴：离散的航班号
      data: rows.map(flight => flight.flightNo),
      axisTick: { alignWithLabel: true }       // 刻度线对齐标签中心
    },
    yAxis: {
      type: 'value',                           // 数值轴：连续的票价
      name: '价格',
      axisLabel: {
        formatter: '{value} 元'                 // y 轴标签追加"元"
      }
    },
    series: [
      {
        name: '票价',
        type: 'bar',                           // 柱状图系列
        data: rows.map(flight => Number(flight.price)),
        barMaxWidth: 42,                       // 柱子最大宽度（防止柱子过宽）
        itemStyle: {
          color: '#2563eb',                    // 柱状图填充色（蓝色）
          borderRadius: [4, 4, 0, 0]          // 顶部圆角 4px
        }
      }
    ]
  }
}

/**
 * 构建价格历史折线图的 ECharts 配置对象。
 *
 * x 轴为观察时间，y 轴为票价，展示航班价格随时间的变化趋势。
 * 使用青色 (#0f766e) 折线，下方带半透明面积填充。
 *
 * @param {Array} history - 价格快照数组 [{observedAt, price}, ...]
 * @returns {Object} ECharts option 配置对象
 */
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
      // 将 ISO 时间格式转换为 "yyyy-MM-dd HH:mm" 格式
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
        type: 'line',                          // 折线图系列
        smooth: true,                          // 平滑曲线（贝塞尔插值）
        data: rows.map(row => Number(row.price)),
        itemStyle: { color: '#0f766e' },       // 折线和数据点颜色（青色）
        areaStyle: { color: 'rgba(15, 118, 110, 0.12)' }  // 折线下方半透明面积填充
      }
    ]
  }
}

/**
 * 构建爬虫触发参数。
 *
 * 根据前端表单数据构造后端 POST /api/crawl/run 所需的 payload。
 * - 数据源为 'sample' 时仅传 source（使用本地示例 HTML）
 * - 数据源为 'amadeus' 时传入完整参数（城市、日期、人数等），未填的字段使用默认值
 *
 * @param {Object} form - 前端采集配置表单数据
 * @param {string} [form.source='sample'] - 数据源（'sample' 或 'amadeus'）
 * @param {string} [form.fromCity='上海'] - 出发城市（中文名）
 * @param {string} [form.toCity='北京'] - 到达城市（中文名）
 * @param {string} [form.date] - 出发日期（ISO 格式 yyyy-MM-dd）
 * @param {number} [form.adults=1] - 成人乘客数量
 * @param {number} [form.maxResults=5] - 最大返回结果数
 * @returns {Object} 后端 /api/crawl/run 所需的请求体
 */
export function buildCrawlerPayload(form) {
  const source = form?.source || 'sample'
  if (source !== 'amadeus') {
    // 非 Amadeus 数据源：使用静态示例数据，无需额外参数
    return { source: 'sample' }
  }
  // Amadeus 数据源：传入完整路线参数，未填字段使用默认值
  return {
    source,
    fromCity: form.fromCity || '上海',
    toCity: form.toCity || '北京',
    date: form.date,
    adults: Number(form.adults || 1),
    maxResults: Number(form.maxResults || 5)
  }
}

/**
 * 格式化购票时机分析报告的展示文本。
 *
 * 拼接风险等级、分析总结和建议购买窗口为一行文本。
 *
 * @param {Object|null} response - 后端 /api/ai/timing 的响应体
 * @param {string} [response.riskLevel] - 风险等级（LOW / MEDIUM / HIGH）
 * @param {string} [response.summary] - 分析总结文本
 * @param {string} [response.buyWindow] - 建议购买时间窗口
 * @returns {string} 格式化后的展示文本
 */
export function formatTimingReport(response) {
  if (!response) {
    return ''
  }
  // 拼接：风险等级 + 分析总结 + 购买窗口建议
  return `风险等级：${response.riskLevel || 'UNKNOWN'}。${response.summary || '暂无分析结果'} ${response.buyWindow || ''}`.trim()
}

/**
 * 格式化日期时间字符串为展示格式 "yyyy-MM-dd HH:mm"。
 *
 * 处理逻辑：
 * 1. 将 ISO 格式中的 'T' 替换为空格
 * 2. 截取前 16 个字符（即 "yyyy-MM-dd HH:mm" 部分）
 * 3. 空值返回 "-"
 *
 * @param {string|null} value - ISO 日期时间字符串（如 "2026-06-11T10:00:00"）
 * @returns {string} 格式化后的字符串（如 "2026-06-11 10:00"）
 */
export function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}
