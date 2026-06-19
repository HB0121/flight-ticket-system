<template>
  <main class="user-shell">
    <header class="user-shell__header">
      <div class="user-shell__brand">
        <div class="user-shell__logo" aria-hidden="true">
          <span class="user-shell__logo-mark">F</span>
        </div>
        <div class="user-shell__brand-copy">
          <h1>{{ t('flights.console.title') }}</h1>
          <p>{{ t('flights.console.subtitle') }}</p>
        </div>
      </div>

      <nav class="user-shell__nav" aria-label="User Navigation">
        <RouterLink to="/flights">{{ t('layout.nav.flights') }}</RouterLink>
        <RouterLink to="/favorites">{{ t('layout.nav.favorites') }}</RouterLink>
        <RouterLink to="/history">{{ t('layout.nav.history') }}</RouterLink>
      </nav>

      <div class="user-shell__meta">
        <span class="user-shell__badge">{{ badgeText.dataSource }}</span>
        <span class="user-shell__badge user-shell__badge--muted">{{ badgeText.mode }}</span>
        <div class="user-shell__locale-switch">
          <button type="button" @click="switchLocale('zh-CN')">{{ t('common.locales.zhCN') }}</button>
          <button type="button" @click="switchLocale('en-US')">{{ t('common.locales.enUS') }}</button>
        </div>
      </div>
    </header>

    <section class="user-shell__content">
      <RouterView />
    </section>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { setStoredLocale } from '../i18n/locale.js'

const { locale, t } = useI18n()

const badgeText = computed(() => (
  locale.value === 'zh-CN'
    ? {
        dataSource: '数据来源：AeroDataBox',
        mode: '模式：本地 MySQL 查询'
      }
    : {
        dataSource: 'Data Source: AeroDataBox',
        mode: 'Mode: Local MySQL Query'
      }
))

function switchLocale(nextLocale) {
  locale.value = nextLocale
  setStoredLocale(nextLocale)
}
</script>

<style scoped>
.user-shell {
  min-height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr;
  background:
    radial-gradient(circle at top left, rgba(219, 234, 254, 0.5), transparent 28%),
    linear-gradient(180deg, #f7fbff 0%, #eef5fb 100%);
  padding: 10px 12px 12px;
}

.user-shell__header {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) auto auto;
  align-items: center;
  gap: 20px;
  max-width: 1520px;
  width: 100%;
  margin: 0 auto;
  padding: 14px 20px;
  min-height: 78px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #dbe5f0;
  border-radius: 18px;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.05);
}

.user-shell__brand {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.user-shell__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

.user-shell__logo-mark {
  color: #ffffff;
  font-size: 18px;
  font-weight: 800;
  line-height: 1;
}

.user-shell__brand-copy {
  min-width: 0;
}

.user-shell__brand-copy h1 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.2;
}

.user-shell__brand-copy p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.45;
}

.user-shell__nav {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  justify-self: center;
}

.user-shell__nav a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 16px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #dbeafe;
  border-radius: 999px;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.06);
}

.user-shell__nav a.router-link-active {
  color: #ffffff;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  border-color: transparent;
}

.user-shell__meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.user-shell__badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #dbeafe;
  border-radius: 999px;
}

.user-shell__badge--muted {
  color: #334155;
}

.user-shell__locale-switch {
  display: inline-flex;
  align-items: center;
  padding: 2px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #dbeafe;
  border-radius: 999px;
}

.user-shell__locale-switch button {
  min-height: 32px;
  padding: 0 12px;
  color: #334155;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: pointer;
}

.user-shell__locale-switch button:hover {
  color: #1d4ed8;
  background: #eff6ff;
}

.user-shell__content {
  width: 100%;
  max-width: 1520px;
  margin: 0 auto;
  min-height: 0;
  overflow: visible;
  padding: 12px 0 0;
}

@media (max-width: 1180px) {
  .user-shell__header {
    grid-template-columns: 1fr;
    justify-items: start;
  }

  .user-shell__nav,
  .user-shell__meta {
    justify-self: start;
  }
}

@media (max-width: 720px) {
  .user-shell__header {
    gap: 14px;
    padding: 14px 12px 12px;
  }

  .user-shell__content {
    padding: 10px 0 0;
  }

  .user-shell__nav,
  .user-shell__meta {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
