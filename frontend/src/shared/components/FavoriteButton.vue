<template>
  <el-button
    :loading="loading"
    :aria-label="isFavorited ? t('common.actions.unfavorite') : t('common.actions.favorite')"
    link
    @click.stop="toggle"
  >
    <el-icon :size="18">
      <StarFilled v-if="isFavorited" style="color: #e6a817;" />
      <Star v-else style="color: #94a3b8;" />
    </el-icon>
  </el-button>
</template>

<script setup>
import { ref } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { addFavorite, removeFavorite } from '../../api/profileApi.js'

const { t } = useI18n()

const props = defineProps({
  flightId: { type: Number, required: true },
  isFavorited: { type: Boolean, default: false },
  favoriteId: { type: Number, default: null }
})

const emit = defineEmits(['toggled'])

const loading = ref(false)

async function toggle() {
  loading.value = true
  try {
    if (props.isFavorited) {
      await removeFavorite(props.favoriteId)
      emit('toggled', false, null)
      ElMessage.success(t('flights.favorite.removed'))
    } else {
      const result = await addFavorite({ flightId: props.flightId })
      emit('toggled', true, result.id)
      ElMessage.success(t('flights.favorite.added'))
    }
  } catch {
    ElMessage.error(t('flights.favorite.failed'))
  } finally {
    loading.value = false
  }
}
</script>
