# Logout Feature Design

**Date:** 2026-06-24
**Status:** Approved

## Overview

Add a logout (sign-out) feature to the flight ticket system. The backend `POST /api/auth/logout` API already exists and deletes the bearer token from `user_token` table. The frontend already has the `logout()` API function and `clearStoredSession()` utility. This work adds the missing UI: a user menu showing the current username and a logout button, placed in both layouts.

## Approach

**Scheme B — Shared component `UserMenu`.** Create a reusable component in `src/shared/`, following existing project patterns (`FlightTable`, `FlightDetailCard`, `PriceHistoryChart`). Both `UserLayout` and `AdminLayout` import and use it.

## Implementation Plan

### 1. New file: `frontend/src/shared/UserMenu.vue`

Self-contained component with no props. Reads user info from `localStorage` on mount.

**Template:**
- A `span` or text node showing the user's display name (`nickname || username`, with a user icon).
- An Element Plus `el-button` (text type) labeled "退出登录" / "Sign out" via i18n.

**Script:**
- `displayName` ref — parsed from `localStorage.getItem('user')` on `mounted`.
- `handleLogout()` method:
  1. `ElMessageBox.confirm('确定要退出登录吗？')` — confirmation dialog (optional but recommended).
  2. Call `logout()` from `@/api/authApi` (best-effort, catch and log errors).
  3. Call `clearStoredSession()` from `@/auth/session` (always, even if API fails).
  4. Call `markSessionAnonymous()` from `@/auth/session`.
  5. `router.push({ name: 'auth' })` redirect to login page.
  6. `ElMessage.success(t('auth.logout.success'))` feedback.

**Error handling:** API failure does NOT block local session cleanup or redirect. The user always exits successfully on the client side. Errors are logged with `console.error`.

**Style (scoped):**
- Flex container, right-aligned.
- Gap between username and logout button.
- Subtle text color for username.

### 2. i18n additions

Add to `frontend/src/i18n/messages/zh-CN.js` and `frontend/src/i18n/messages/en-US.js`:

| Key | zh-CN | en-US |
|---|---|---|
| `auth.logout.button` | 退出登录 | Sign out |
| `auth.logout.confirm` | 确定要退出登录吗？ | Are you sure you want to sign out? |
| `auth.logout.success` | 已退出登录 | Signed out |
| `auth.logout.error` | 退出请求失败，但本地会话已清除 | Sign-out request failed, but session was cleared locally |

### 3. Modify: `UserLayout.vue`

- Import `UserMenu` from `@/shared/UserMenu.vue`.
- Insert `<UserMenu />` in the header area, to the left of the locale switcher buttons.

### 4. Modify: `AdminLayout.vue`

- Same changes as `UserLayout.vue`.

### 5. (Optional) Wire up the `auth:logout` event

The Axios 401 interceptor in `http.js` already dispatches `auth:logout` on `window`. Adding a listener in `App.vue` (or the top-level router setup) would enable automatic redirect on 401, but this is tracked separately as it's not core to the logout feature.

## Logout Flow (end-to-end)

```
User clicks "Sign out"
  → [Optional: confirmation dialog]
  → POST /api/auth/logout (Authorization: Bearer <token>)
      → LoginInterceptor validates token
      → AuthController.logout() → AuthService.logout() → TokenRepository.deleteByToken()
      → Response: 204 No Content
  → clearStoredSession()  (removes 'token' and 'user' from localStorage)
  → markSessionAnonymous() (sets in-memory sessionStatus = 'anonymous')
  → router.push({ name: 'auth' })
  → ElMessage.success('Signed out')
```

On API failure, steps after the API call still execute — the user is never locked in.

## Files Changed

| File | Action |
|---|---|
| `frontend/src/shared/UserMenu.vue` | **New** |
| `frontend/src/layouts/UserLayout.vue` | Modify (add import + template insertion) |
| `frontend/src/layouts/AdminLayout.vue` | Modify (add import + template insertion) |
| `frontend/src/i18n/messages/zh-CN.js` | Modify (add keys) |
| `frontend/src/i18n/messages/en-US.js` | Modify (add keys) |

No backend changes needed.
