// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createI18n } from 'vue-i18n'
import AuthPage from './AuthPage.vue'
import { resetSessionState } from '../../../auth/session.js'

const mocks = vi.hoisted(() => ({
  routerReplace: vi.fn(),
  loginMock: vi.fn(),
  successMessageMock: vi.fn(),
  routeQuery: {}
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplace
  }),
  useRoute: () => ({
    query: mocks.routeQuery
  })
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    success: mocks.successMessageMock
  }
}))

vi.mock('../../../api/authApi.js', () => ({
  login: mocks.loginMock,
  register: vi.fn()
}))

const ElFormStub = {
  template: '<form @submit.prevent="$emit(\'submit\', $event)"><slot /></form>',
  emits: ['submit']
}

const ElFormItemStub = {
  template: '<label><span>{{ label }}</span><slot /></label>',
  props: ['label']
}

const ElInputStub = {
  template: `
    <input
      :type="type || 'text'"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    />
  `,
  props: ['modelValue', 'placeholder', 'type'],
  emits: ['update:modelValue']
}

const ElButtonStub = {
  template: '<button :type="nativeType || \'button\'" :disabled="loading"><slot /></button>',
  props: ['loading', 'nativeType']
}

function createWrapper() {
  return mount(AuthPage, {
    global: {
      plugins: [createTestI18n('en-US')],
      stubs: {
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElButton: ElButtonStub
      }
    }
  })
}

function createTestI18n(locale = 'en-US') {
  return createI18n({
    legacy: false,
    locale,
    messages: {
      'en-US': {
        auth: {
          brand: 'Flight Query Platform',
          login: {
            title: 'Sign in',
            subtitle: 'Access the phase-1 flight system',
            submit: 'Sign in',
            invalidSession: 'Login succeeded but the session payload was invalid.',
            registerInvalidSession: 'Registration succeeded but the session payload was invalid.',
            success: 'Signed in. Redirecting to flight search.',
            error: 'Login failed. Please try again.'
          },
          register: {
            submit: 'Create account',
            success: 'Registration successful. You are now signed in.',
            error: 'Registration failed. Please try again.'
          },
          fields: {
            username: 'Username',
            password: 'Password',
            nickname: 'Nickname (optional)'
          },
          placeholders: {
            username: 'Enter your username',
            password: 'Enter your password',
            nickname: 'Defaults to the username if omitted'
          },
          switcher: {
            login: 'Login',
            register: 'Register'
          },
          footnote: {
            loginPrompt: 'Need an account?',
            loginAction: 'Register here',
            registerPrompt: 'Already have an account?',
            registerAction: 'Back to login'
          },
          validation: {
            missingCredentials: 'Enter both username and password.',
            shortPassword: 'Registration passwords must be at least 4 characters.'
          }
        },
        common: {
          locales: {
            zhCN: 'Chinese',
            enUS: 'English'
          }
        }
      },
      'zh-CN': {
        auth: {
          brand: '航班查询平台',
          login: {
            title: '登录',
            subtitle: '进入 phase-1 航班系统',
            submit: '登录',
            invalidSession: '登录成功，但会话数据无效。',
            registerInvalidSession: '注册成功，但会话数据无效。',
            success: '登录成功，正在跳转。',
            error: '登录失败，请稍后重试。'
          },
          register: {
            submit: '创建账号',
            success: '注册成功，已自动登录。',
            error: '注册失败，请稍后重试。'
          },
          fields: {
            username: '用户名',
            password: '密码',
            nickname: '昵称（可选）'
          },
          placeholders: {
            username: '请输入用户名',
            password: '请输入密码',
            nickname: '留空时默认使用用户名'
          },
          switcher: {
            login: '登录',
            register: '注册'
          },
          footnote: {
            loginPrompt: '还没有账号？',
            loginAction: '去注册',
            registerPrompt: '已有账号？',
            registerAction: '返回登录'
          },
          validation: {
            missingCredentials: '请输入用户名和密码。',
            shortPassword: '注册密码至少 4 位。'
          }
        },
        common: {
          locales: {
            zhCN: '中文',
            enUS: 'English'
          }
        }
      }
    }
  })
}

async function fillAndSubmit(wrapper, username, password) {
  const inputs = wrapper.findAll('input')
  await inputs[0].setValue(username)
  await inputs[1].setValue(password)
  await wrapper.find('form').trigger('submit')
  await nextTick()
}

describe('AuthPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetSessionState()
    mocks.routeQuery = {}
    globalThis.localStorage = {
      setItem: vi.fn(),
      getItem: vi.fn(),
      removeItem: vi.fn()
    }
  })

  it('persists a valid session and redirects to user flights after login', async () => {
    mocks.loginMock.mockResolvedValueOnce({
      id: 7,
      username: 'demo',
      token: 'valid-token'
    })

    const wrapper = createWrapper()
    await fillAndSubmit(wrapper, 'demo', 'secret')

    expect(globalThis.localStorage.setItem).toHaveBeenCalledWith('token', 'valid-token')
    expect(globalThis.localStorage.setItem).toHaveBeenCalledWith(
      'user',
      JSON.stringify({ id: 7, username: 'demo', nickname: 'demo' })
    )
    expect(mocks.routerReplace).toHaveBeenCalledWith({ name: 'user-flights' })
    expect(mocks.successMessageMock).toHaveBeenCalled()
  })

  it('shows an auth error and skips persistence when the login payload is malformed', async () => {
    mocks.loginMock.mockResolvedValueOnce({
      username: 'demo'
    })

    const wrapper = createWrapper()
    await fillAndSubmit(wrapper, 'demo', 'secret')

    expect(globalThis.localStorage.setItem).not.toHaveBeenCalled()
    expect(mocks.routerReplace).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Login succeeded but the session payload was invalid.')
  })

  it('redirects back to the requested page after login when a redirect query is present', async () => {
    mocks.routeQuery = { redirect: '/admin/data-sources' }
    mocks.loginMock.mockResolvedValueOnce({
      id: 7,
      username: 'demo',
      token: 'valid-token'
    })

    const wrapper = createWrapper()
    await fillAndSubmit(wrapper, 'demo', 'secret')

    expect(mocks.routerReplace).toHaveBeenCalledWith('/admin/data-sources')
  })

  it('renders auth copy from the active locale', () => {
    const wrapper = mount(AuthPage, {
      global: {
        plugins: [createTestI18n('zh-CN')],
        stubs: {
          ElForm: ElFormStub,
          ElFormItem: ElFormItemStub,
          ElInput: ElInputStub,
          ElButton: ElButtonStub
        }
      }
    })

    expect(wrapper.text()).toContain('登录')
    expect(wrapper.text()).toContain('中文')
  })
})
