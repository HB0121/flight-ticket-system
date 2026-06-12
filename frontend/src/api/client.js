/**
 * HTTP API 客户端模块。
 *
 * 基于 Axios 封装的统一 HTTP 客户端，集中管理所有后端 API 调用。
 * 包含请求/响应拦截器，自动处理认证 token 注入和 401 未授权响应。
 *
 * ## 认证拦截器设计
 * - **请求拦截器**：每次请求前从 localStorage 读取 token，自动附加到 Authorization 请求头。
 * - **响应拦截器**：收到 401 状态码时，清除本地 token 和用户信息，触发全局 `auth:logout` 事件。
 *
 * ## 导出函数分类
 * | 类别 | 函数 | 说明 |
 * |------|------|------|
 * | 航班 | fetchFlights, fetchFlight, fetchPriceHistory | 航班查询、详情、价格历史 |
 * | 爬虫 | fetchLatestJob, runCrawler | 爬虫任务状态查询和触发 |
 * | AI | requestAdvice, requestTiming | AI 购票建议和时机分析 |
 * | 对话 | createConversation, listConversations, getMessages, sendMessage, deleteConversation | AI 多轮对话管理 |
 * | 认证 | login, register, logout, getMe | 用户注册、登录、登出、获取当前用户 |
 *
 * 设计模式：门面模式（Facade）—— 将 Axios 的底层调用封装为语义清晰的业务 API 函数。
 */

import axios from 'axios'

/**
 * Axios 实例（单例）。
 * 配置了 baseURL（从环境变量 VITE_API_BASE_URL 读取，默认 localhost:8080）
 * 和 15 秒超时。
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

// 请求拦截器：自动附加认证 token
// 在每个请求发出前，从 localStorage 取出 token 并设置到 Authorization 请求头
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    // 使用 Bearer 认证方案（RFC 6750）
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理 401 未授权错误
// 当后端返回 401 时，清除本地存储的认证信息并通知应用层
http.interceptors.response.use(
  response => response,  // 正常响应直接透传
  error => {
    if (error.response && error.response.status === 401) {
      // 清除本地存储的 token 和用户信息
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 通过自定义事件通知应用层（App.vue 监听此事件以更新 UI）
      window.dispatchEvent(new CustomEvent('auth:logout'))
    }
    return Promise.reject(error)  // 继续向后传递错误，让调用方可以进一步处理
  }
)

/**
 * 查询航班列表（支持多条件筛选）。
 * @param {Object} params - 查询参数 {fromCity, toCity, date, dataSource}
 * @returns {Promise<Object>} 后端返回的航班数据
 */
export async function fetchFlights(params = {}) {
  const response = await http.get('/api/flights', { params })
  return response.data
}

/**
 * 查询单个航班详情。
 * @param {number|string} id - 航班 ID
 * @returns {Promise<Object>} 航班详细信息
 */
export async function fetchFlight(id) {
  const response = await http.get(`/api/flights/${id}`)
  return response.data
}

/**
 * 查询航班价格历史（用于绘制价格走势图）。
 * @param {number|string} id - 航班 ID
 * @returns {Promise<Array>} 价格快照数组，按时间排序
 */
export async function fetchPriceHistory(id) {
  const response = await http.get(`/api/flights/${id}/price-history`)
  return response.data
}

/**
 * 获取最近一次爬虫任务的执行状态。
 * @returns {Promise<Object>} 爬虫任务信息（状态、成功/失败数量等）
 */
export async function fetchLatestJob() {
  const response = await http.get('/api/crawl/latest')
  return response.data
}

/**
 * 手动触发爬虫任务。
 * @param {Object} payload - 爬虫参数 {source, fromCity, toCity, date, adults, maxResults}
 * @returns {Promise<Object>} 爬虫任务启动结果
 */
export async function runCrawler(payload = {}) {
  const response = await http.post('/api/crawl/run', payload)
  return response.data
}

/**
 * 请求 AI 购票建议（单轮）。
 * @param {string} message - 用户输入的自然语言消息（如"帮我找上海到北京6月20号的便宜机票"）
 * @returns {Promise<Object>} AI 建议响应（含推荐航班和总结文本）
 */
export async function requestAdvice(message) {
  const response = await http.post('/api/ai/advice', { message })
  return response.data
}

/**
 * 请求 AI 购票时机分析（单轮）。
 * @param {string} message - 用户输入的自然语言消息
 * @returns {Promise<Object>} 时机分析响应（含风险等级和建议购买窗口）
 */
export async function requestTiming(message) {
  const response = await http.post('/api/ai/timing', { message })
  return response.data
}

// 对话管理 API（多轮对话）

/**
 * 创建新的 AI 对话会话。
 * @param {string} title - 对话标题
 * @returns {Promise<Object>} 创建的会话信息（含 sessionId）
 */
export async function createConversation(title) {
  const response = await http.post('/api/ai/conversations', { title })
  return response.data
}

/**
 * 获取所有对话会话列表。
 * @returns {Promise<Array>} 会话列表
 */
export async function listConversations() {
  const response = await http.get('/api/ai/conversations')
  return response.data
}

/**
 * 获取指定会话的历史消息。
 * @param {string} sessionId - 会话 ID
 * @returns {Promise<Array>} 消息列表（含用户消息和 AI 回复）
 */
export async function getMessages(sessionId) {
  const response = await http.get(`/api/ai/conversations/${sessionId}/messages`)
  return response.data
}

/**
 * 向指定会话发送消息并获取 AI 回复。
 * @param {string} sessionId - 会话 ID
 * @param {string} message - 用户消息文本
 * @returns {Promise<Object>} AI 回复
 */
export async function sendMessage(sessionId, message) {
  const response = await http.post(`/api/ai/conversations/${sessionId}/messages`, { message })
  return response.data
}

/**
 * 删除指定会话及其所有消息。
 * @param {string} sessionId - 会话 ID
 */
export async function deleteConversation(sessionId) {
  await http.delete(`/api/ai/conversations/${sessionId}`)
}

// 认证 API

/**
 * 用户登录。
 * @param {string} username - 用户名
 * @param {string} password - 密码（明文，通过 HTTPS 传输）
 * @returns {Promise<Object>} {id, username, nickname, token}
 */
export async function login(username, password) {
  const response = await http.post('/api/auth/login', { username, password })
  return response.data
}

/**
 * 用户注册。
 * @param {string} username - 用户名（2-32 字符，全局唯一）
 * @param {string} password - 密码（4-64 字符）
 * @param {string} nickname - 显示昵称（可选）
 * @returns {Promise<Object>} {id, username, nickname, token}
 */
export async function register(username, password, nickname) {
  const response = await http.post('/api/auth/register', { username, password, nickname })
  return response.data
}

/**
 * 用户登出（清除服务端 token）。
 * 注意：本地 token 由响应拦截器在收到 401 时自动清除。
 */
export async function logout() {
  await http.post('/api/auth/logout')
}

/**
 * 获取当前登录用户信息。
 * 需要在请求头携带有效的 Bearer token。
 * @returns {Promise<Object>} {id, username, nickname}
 */
export async function getMe() {
  const response = await http.get('/api/auth/me')
  return response.data
}
