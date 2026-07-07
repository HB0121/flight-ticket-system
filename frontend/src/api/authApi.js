import { http } from './http.js'

// 登录接口兼容两种调用方式：传对象，或传 username/password 两个参数。
function normalizeLoginPayload(input, password) {
  if (typeof input === 'object' && input !== null) {
    return input
  }

  return {
    username: input,
    password
  }
}

// 注册接口同样兼容对象参数和分散参数，便于页面和测试复用。
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
