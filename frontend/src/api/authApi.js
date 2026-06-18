import { http } from './http.js'

function normalizeLoginPayload(input, password) {
  if (typeof input === 'object' && input !== null) {
    return input
  }

  return {
    username: input,
    password
  }
}

function normalizeRegisterPayload(input, password, nickname) {
  if (typeof input === 'object' && input !== null) {
    return input
  }

  return {
    username: input,
    password,
    nickname
  }
}

export async function login(input, password) {
  const response = await http.post('/api/auth/login', normalizeLoginPayload(input, password))
  return response.data
}

export async function register(input, password, nickname) {
  const response = await http.post('/api/auth/register', normalizeRegisterPayload(input, password, nickname))
  return response.data
}

export async function logout() {
  await http.post('/api/auth/logout')
}

export async function getMe() {
  const response = await http.get('/api/auth/me')
  return response.data
}
