// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import AuthPage from './AuthPage.vue'

const mocks = vi.hoisted(() => ({
  routerReplace: vi.fn(),
  loginMock: vi.fn(),
  successMessageMock: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: mocks.routerReplace
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
      stubs: {
        ElForm: ElFormStub,
        ElFormItem: ElFormItemStub,
        ElInput: ElInputStub,
        ElButton: ElButtonStub
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
})
