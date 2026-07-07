import { http } from './http.js'

// 航班业务接口：页面只关心函数语义，不直接拼接 axios 请求细节。
export async function fetchFlights(params = {}) {
  const response = await http.get('/api/flights', { params })
  return response.data
}

export async function fetchFlight(id) {
  const response = await http.get(`/api/flights/${id}`)
  return response.data
}

export async function fetchPriceHistory(id) {
  const response = await http.get(`/api/flights/${id}/price-history`)
  return response.data
}

// 同步航班数据时，前端只发起管理请求；真正的爬虫执行和数据库写入由后端完成。
export async function syncFlights({ airportCode, date }) {
  const response = await http.post('/api/admin/flights/sync', null, {
    params: {
      airportCode,
      date
    }
  })
  return response.data
}

// AI 建议模块保留 message 和 query 两个字段，兼容后端不同阶段的请求格式。
export async function requestAdvice(message) {
  const response = await http.post('/api/ai/advice', {
    message,
    query: message
  })
  return response.data
}
