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
import { logout } from '../../api/authApi'
import { clearStoredSession, markSessionAnonymous } from '../../auth/session'

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
  sessionStorage.removeItem('flightSearchPage_v1')
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
