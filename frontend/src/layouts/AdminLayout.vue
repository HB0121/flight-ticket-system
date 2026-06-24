<template>
  <main class="route-layout">
    <header class="route-layout__header">
      <div>
        <p class="route-layout__eyebrow">{{ t('layout.shell.phase') }}</p>
        <h1>{{ t('layout.shell.adminTitle') }}</h1>
      </div>
      <nav class="route-layout__nav">
        <RouterLink to="/admin/crawl-jobs">{{ t('layout.nav.crawlJobs') }}</RouterLink>
        <RouterLink to="/admin/data-sources">{{ t('layout.nav.dataSources') }}</RouterLink>
      </nav>

      <div class="route-layout__locale-switch">
        <button type="button" @click="switchLocale('zh-CN')">{{ t('common.locales.zhCN') }}</button>
        <button type="button" @click="switchLocale('en-US')">{{ t('common.locales.enUS') }}</button>
        <UserMenu />
      </div>
    </header>

    <section class="route-layout__content">
      <RouterView />
    </section>
  </main>
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { setStoredLocale } from '../i18n/locale.js'
import UserMenu from '../shared/components/UserMenu.vue'

const { locale, t } = useI18n()

function switchLocale(nextLocale) {
  locale.value = nextLocale
  setStoredLocale(nextLocale)
}
</script>
