import { http } from './http.js'

const CRAWLER_TIMEOUT_MS = 130000

export async function createCrawlJob(payload = {}) {
  const response = await http.post('/api/admin/crawl-jobs', payload, { timeout: CRAWLER_TIMEOUT_MS })
  return response.data
}

export async function listCrawlJobs() {
  const response = await http.get('/api/admin/crawl-jobs')
  return response.data
}

export async function listDataSourceStatuses() {
  const response = await http.get('/api/admin/data-sources/status')
  return response.data
}
