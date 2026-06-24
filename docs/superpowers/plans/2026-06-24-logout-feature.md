# Logout Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a visible logout button with current-user display to both UserLayout and AdminLayout.

**Architecture:** Create a shared `UserMenu.vue` component placed inside the existing header of each layout. The component reads the logged-in user from localStorage, shows their display name, and handles the full logout flow: API call → local session clear → redirect to `/auth`. No backend changes needed.

**Tech Stack:** Vue 3 (Composition API, `<script setup>`), Element Plus (`ElMessageBox`, `ElMessage`), vue-i18n, vue-router

## Global Constraints

- Follow existing project patterns: shared components go under `src/shared/components/`, use `<script setup>`, use scoped CSS
- i18n keys in `zh-CN.js` (default) and `en-US.js` with identical structure
- Backend logout API already exists at `POST /api/auth/logout` — no backend work
- Logout must clear localStorage even if the API call fails

---

### Task 1: Add i18n keys for logout

**Files:**
- Modify: `frontend/src/i18n/messages/zh-CN.js` — add `auth.logout` block
- Modify: `frontend/src/i18n/messages/en-US.js` — add `auth.logout` block

**Interfaces:**
- Produces: `t('auth.logout.button')`, `t('auth.logout.confirm')`, `t('auth.logout.success')`, `t('auth.logout.error')` — usable in any component via `useI18n()`

- [ ] **Step 1: Add logout keys to zh-CN.js**

Insert after `auth.validation` block (after line 80, before the closing `},` of the `auth` object):

```js
    logout: {
      button: '退出登录',
      confirm: '确定要退出登录吗？',
      success: '已退出登录',
      error: '退出请求失败，但本地会话已清除'
    }
```

- [ ] **Step 2: Add logout keys to en-US.js**

Insert after `auth.validation` block (after line 80, before the closing `},` of the `auth` object):

```js
    logout: {
      button: 'Sign out',
      confirm: 'Are you sure you want to sign out?',
      success: 'Signed out',
      error: 'Sign-out request failed, but session was cleared locally'
    }
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/i18n/messages/zh-CN.js frontend/src/i18n/messages/en-US.js
git commit -m "feat: add logout i18n keys for zh-CN and en-US"
```

---

### Task 2: Create UserMenu shared component

**Files:**
- Create: `frontend/src/shared/components/UserMenu.vue`

**Interfaces:**
- Consumes: `logout()` from `@/api/authApi`, `clearStoredSession` / `markSessionAnonymous` from `@/auth/session`, `useRouter` from `vue-router`, `useI18n` from `vue-i18n`, `ElMessageBox` / `ElMessage` from `element-plus`
- Produces: `<UserMenu />` — a self-contained Vue component with no props, no emits. Can be dropped into any layout.

- [ ] **Step 1: Create the component file**

Write `frontend/src/shared/components/UserMenu.vue`:

```vue
<template>
  <div v-if="displayName" class="user-menu">
    <span class="user-menu__greeting" :title="displayName">
      <span class="user-menu__icon" aria-hidden="true">👤</span>
      {{ displayName }}
    </span>
    <el-button
      class="user-menu__logout"
      type="text"
      size="small"
      @click="handleLogout"
    >
      {{ t('auth.logout.button') }}
    </el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessageBox, ElMessage } from 'element-plus'
import { logout } from '@/api/authApi'
import { clearStoredSession, markSessionAnonymous } from '@/auth/session'

const { t } = useI18n()
const router = useRouter()
const displayName = ref('')

onMounted(() => {
  try {
    const raw = localStorage.getItem('user')
    if (raw) {
      const user = JSON.parse(raw)
      displayName.value = user.nickname || user.username || ''
    }
  } catch {
    displayName.value = ''
  }
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm(
      t('auth.logout.confirm'),
      t('auth.logout.button'),
      { confirmButtonText: t('auth.logout.button'), cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    await logout()
  } catch (err) {
    console.error('Logout API call failed', err)
    ElMessage.warning(t('auth.logout.error'))
  }

  clearStoredSession()
  markSessionAnonymous()
  router.push({ name: 'auth' })

  if (displayName.value) {
    ElMessage.success(t('auth.logout.success'))
  }
}
</script>

<style scoped>
.user-menu {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.user-menu__greeting {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-menu__icon {
  font-size: 14px;
  flex-shrink: 0;
}

.user-menu__logout {
  padding: 0 10px;
  min-height: 30px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
}

.user-menu__logout:hover {
  color: #dc2626;
  border-color: #fecaca;
  background: #fef2f2;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/shared/components/UserMenu.vue
git commit -m "feat: add UserMenu shared component with logout flow"
```

---

### Task 3: Integrate UserMenu into UserLayout

**Files:**
- Modify: `frontend/src/layouts/UserLayout.vue`

**Interfaces:**
- Consumes: `UserMenu` from `@/shared/components/UserMenu.vue`

- [ ] **Step 1: Add import and template insertion in UserLayout.vue**

**Import** — add after the existing `import { setStoredLocale } from '../i18n/locale.js'` line:

```js
import UserMenu from '@/shared/components/UserMenu.vue'
```

**Template** — insert `<UserMenu />` inside `<div class="user-shell__meta">`, before the `<span class="user-shell__badge">` elements:

```vue
      <div class="user-shell__meta">
        <UserMenu />
        <span class="user-shell__badge">{{ badgeText.dataSource }}</span>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/layouts/UserLayout.vue
git commit -m "feat: add UserMenu to UserLayout header"
```

---

### Task 4: Integrate UserMenu into AdminLayout

**Files:**
- Modify: `frontend/src/layouts/AdminLayout.vue`

**Interfaces:**
- Consumes: `UserMenu` from `@/shared/components/UserMenu.vue`

- [ ] **Step 1: Add import and template insertion in AdminLayout.vue**

**Import** — add after the existing `import { setStoredLocale } from '../i18n/locale.js'` line:

```js
import UserMenu from '@/shared/components/UserMenu.vue'
```

**Template** — insert `<UserMenu />` inside `<div class="route-layout__locale-switch">`, before the first `<button>`:

```vue
      <div class="route-layout__locale-switch">
        <UserMenu />
        <button type="button" @click="switchLocale('zh-CN')">{{ t('common.locales.zhCN') }}</button>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/layouts/AdminLayout.vue
git commit -m "feat: add UserMenu to AdminLayout header"
```

---

## Verification

After all tasks are complete, start the frontend and verify:

1. Log in as `admin` / `admin123`
2. Confirm the username appears in the header with a logout button
3. Click logout → confirm dialog appears → click confirm
4. Verify redirect to `/auth` (login page)
5. Verify localStorage `token` and `user` keys are removed
6. Try navigating to `/flights` directly → should redirect to `/auth`
7. Repeat login and verify switching locales works alongside the UserMenu
