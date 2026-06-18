<template>
  <section class="auth-page">
    <div class="auth-card auth-module-card">
      <p class="auth-eyebrow">Flight Query Platform</p>
      <h1>Account Access</h1>
      <p class="auth-subtitle">Sign in to continue to flight search, or create a new demo account.</p>

      <div class="auth-switcher">
        <button
          type="button"
          :class="['auth-switcher__item', { active: mode === 'login' }]"
          @click="switchMode('login')"
        >
          Login
        </button>
        <button
          type="button"
          :class="['auth-switcher__item', { active: mode === 'register' }]"
          @click="switchMode('register')"
        >
          Register
        </button>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="Username" class="auth-input">
          <el-input
            v-model.trim="form.username"
            autocomplete="username"
            placeholder="Enter your username"
          />
        </el-form-item>

        <el-form-item label="Password" class="auth-input">
          <el-input
            v-model.trim="form.password"
            autocomplete="current-password"
            placeholder="Enter your password"
            show-password
            type="password"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" label="Nickname (optional)" class="auth-input">
          <el-input
            v-model.trim="form.nickname"
            autocomplete="nickname"
            placeholder="Defaults to the username if omitted"
          />
        </el-form-item>

        <p v-if="errorMessage" class="auth-feedback auth-feedback--error">{{ errorMessage }}</p>
        <p v-else-if="successMessage" class="auth-feedback auth-feedback--success">{{ successMessage }}</p>

        <el-button
          :loading="submitting"
          class="auth-btn"
          native-type="submit"
          size="large"
          type="primary"
        >
          {{ mode === 'login' ? 'Sign in' : 'Create account' }}
        </el-button>
      </el-form>

      <p class="auth-footnote">
        {{ mode === 'login' ? 'Need an account?' : 'Already have an account?' }}
        <button class="auth-inline-link" type="button" @click="switchMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? 'Register here' : 'Back to login' }}
        </button>
      </p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../../../api/authApi.js'

const router = useRouter()

const mode = ref('login')
const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

function switchMode(nextMode) {
  mode.value = nextMode
  errorMessage.value = ''
  successMessage.value = ''
}

function isValidSessionPayload(payload) {
  return Boolean(
    payload
    && payload.token
    && payload.id !== undefined
    && payload.id !== null
    && payload.username
  )
}

function persistSession(payload) {
  const safeNickname = payload.nickname ?? payload.username ?? ''

  localStorage.setItem('token', payload.token)
  localStorage.setItem('user', JSON.stringify({
    id: payload.id,
    username: payload.username,
    nickname: safeNickname
  }))
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}

async function submit() {
  if (!form.username || !form.password) {
    errorMessage.value = 'Enter both username and password.'
    return
  }

  if (mode.value === 'register' && form.password.length < 4) {
    errorMessage.value = 'Registration passwords must be at least 4 characters.'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const payload = mode.value === 'login'
      ? await login({ username: form.username, password: form.password })
      : await register({
          username: form.username,
          password: form.password,
          nickname: form.nickname || undefined
        })

    if (!isValidSessionPayload(payload)) {
      errorMessage.value = mode.value === 'login'
        ? 'Login succeeded but the session payload was invalid.'
        : 'Registration succeeded but the session payload was invalid.'
      return
    }

    persistSession(payload)
    successMessage.value = mode.value === 'login' ? 'Signed in. Redirecting to flight search.' : 'Registration successful. You are now signed in.'
    ElMessage.success(successMessage.value)
    await router.replace({ name: 'user-flights' })
  } catch (error) {
    errorMessage.value = getErrorMessage(
      error,
      mode.value === 'login' ? 'Login failed. Please try again.' : 'Registration failed. Please try again.'
    )
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-module-card {
  text-align: left;
}

.auth-eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-switcher {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 18px;
  padding: 4px;
  background: #eff6ff;
  border-radius: 10px;
}

.auth-switcher__item {
  height: 40px;
  color: #475569;
  background: transparent;
  border: 0;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.auth-switcher__item.active {
  color: #0f172a;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.15);
}

.auth-feedback {
  margin: 4px 0 14px;
  font-size: 13px;
  line-height: 1.5;
}

.auth-feedback--error {
  color: #b91c1c;
}

.auth-feedback--success {
  color: #047857;
}

.auth-footnote {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

.auth-inline-link {
  padding: 0;
  color: #2563eb;
  background: transparent;
  border: 0;
  cursor: pointer;
}
</style>
