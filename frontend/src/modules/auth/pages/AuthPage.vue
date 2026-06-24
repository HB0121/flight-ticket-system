<template>
  <section class="auth-page">
    <div class="auth-card auth-module-card">
      <div class="auth-locale-switch">
        <button type="button" @click="switchLocale('zh-CN')">{{ t('common.locales.zhCN') }}</button>
        <button type="button" @click="switchLocale('en-US')">{{ t('common.locales.enUS') }}</button>
      </div>

      <p class="auth-eyebrow">{{ t('auth.brand') }}</p>
      <h1>{{ t('auth.login.title') }}</h1>
      <p class="auth-subtitle">{{ t('auth.login.subtitle') }}</p>

      <div class="auth-switcher">
        <button
          type="button"
          :class="['auth-switcher__item', { active: mode === 'login' }]"
          @click="switchMode('login')"
        >
          {{ t('auth.switcher.login') }}
        </button>
        <button
          type="button"
          :class="['auth-switcher__item', { active: mode === 'register' }]"
          @click="switchMode('register')"
        >
          {{ t('auth.switcher.register') }}
        </button>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item :label="t('auth.fields.username')" class="auth-input">
          <el-input
            v-model.trim="form.username"
            autocomplete="username"
            :placeholder="t('auth.placeholders.username')"
          />
        </el-form-item>

        <el-form-item :label="t('auth.fields.password')" class="auth-input">
          <el-input
            v-model.trim="form.password"
            autocomplete="current-password"
            :placeholder="t('auth.placeholders.password')"
            show-password
            type="password"
          />
        </el-form-item>

        <el-form-item v-if="mode === 'register'" :label="t('auth.fields.nickname')" class="auth-input">
          <el-input
            v-model.trim="form.nickname"
            autocomplete="nickname"
            :placeholder="t('auth.placeholders.nickname')"
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
          {{ mode === 'login' ? t('auth.login.submit') : t('auth.register.submit') }}
        </el-button>
      </el-form>

      <p class="auth-footnote">
        {{ mode === 'login' ? t('auth.footnote.loginPrompt') : t('auth.footnote.registerPrompt') }}
        <button class="auth-inline-link" type="button" @click="switchMode(mode === 'login' ? 'register' : 'login')">
          {{ mode === 'login' ? t('auth.footnote.loginAction') : t('auth.footnote.registerAction') }}
        </button>
      </p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../../../api/authApi.js'
import { markSessionAuthenticated } from '../../../auth/session.js'
import { setStoredLocale } from '../../../i18n/locale.js'

const router = useRouter()
const route = useRoute()
const { locale, t } = useI18n()

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

function switchLocale(nextLocale) {
  locale.value = nextLocale
  setStoredLocale(nextLocale)
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
  markSessionAuthenticated()
}

function getErrorMessage(error, fallback) {
  return error?.response?.data?.message
    ?? error?.response?.data?.error
    ?? error?.message
    ?? fallback
}

function getPostAuthTarget() {
  return typeof route.query.redirect === 'string' && route.query.redirect
    ? route.query.redirect
    : { name: 'user-flights' }
}

async function submit() {
  if (!form.username || !form.password) {
    errorMessage.value = t('auth.validation.missingCredentials')
    return
  }

  if (mode.value === 'register' && form.password.length < 4) {
    errorMessage.value = t('auth.validation.shortPassword')
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
        ? t('auth.login.invalidSession')
        : t('auth.login.registerInvalidSession')
      return
    }

    persistSession(payload)
    successMessage.value = mode.value === 'login' ? t('auth.login.success') : t('auth.register.success')
    ElMessage.success(successMessage.value)
    await router.replace(getPostAuthTarget())
  } catch (error) {
    errorMessage.value = getErrorMessage(
      error,
      mode.value === 'login' ? t('auth.login.error') : t('auth.register.error')
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

.auth-locale-switch {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 12px;
}

.auth-locale-switch button {
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: #ffffff;
  cursor: pointer;
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
