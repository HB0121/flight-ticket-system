import { http } from './http.js'

export async function createConversation(title) {
  const response = await http.post('/api/ai/conversations', { title })
  return response.data
}

export async function listConversations() {
  const response = await http.get('/api/ai/conversations')
  return response.data
}

export async function getMessages(sessionId) {
  const response = await http.get(`/api/ai/conversations/${sessionId}/messages`)
  return response.data
}

export async function sendMessage(sessionId, message) {
  const response = await http.post(`/api/ai/conversations/${sessionId}/messages`, { message })
  return response.data
}

export async function deleteConversation(sessionId) {
  await http.delete(`/api/ai/conversations/${sessionId}`)
}
