import { computed, reactive, ref } from 'vue'

export function useAiTravelAdvice({
  requestAdvice,
  normalizeFlightForDisplay,
  locale,
  getErrorMessage
} = {}) {
  const aiLoading = ref(false)
  const aiResult = ref(null)
  const aiError = ref('')

  const aiForm = reactive({
    query: ''
  })

  const aiCandidates = computed(() =>
    Array.isArray(aiResult.value?.candidates)
      ? aiResult.value.candidates.map(flight => normalizeFlightForDisplay(flight, locale?.value))
      : []
  )

  const aiRecommendedFlight = computed(() =>
    aiResult.value?.recommendedFlight
      ? normalizeFlightForDisplay(aiResult.value.recommendedFlight, locale?.value)
      : null
  )

  const aiSyncStatusText = computed(() => {
    if (aiLoading.value) {
      return locale?.value === 'zh-CN'
        ? '本地暂无该日期航班时，系统会自动同步航班数据，请稍候...'
        : 'If local data is missing, the backend may auto-sync flight data. Please wait...'
    }

    if (!aiResult.value?.syncAttempted) return ''

    const message = aiResult.value.syncMessage || ''
    if (aiResult.value.syncStatus === 'SUCCESS' && aiResult.value.autoSynced) {
      return locale?.value === 'zh-CN'
        ? (message || '自动同步成功，正在基于本地数据生成建议。')
        : (message || 'Auto sync completed. Advice is based on local data.')
    }

    if (aiResult.value.syncStatus === 'FAILED') {
      return locale?.value === 'zh-CN'
        ? `自动同步失败：${message || '请稍后重试或手动同步该日期航班。'}`
        : `Auto sync failed: ${message || 'Try again later or sync this date manually.'}`
    }

    return message
  })

  function formatAiCandidateMeta(flight) {
    const price = flight?.price ?? '-'
    const seats = flight?.seatsLeft ?? '-'
    const source = flight?.dataSource || '-'

    return locale?.value === 'zh-CN'
      ? `￥${price} / ${seats} 座 / ${source}`
      : `¥${price} / ${seats} seats / ${source}`
  }

  async function submitAiAdvice() {
    const query = aiForm.query.trim()
    if (!query) {
      aiError.value = locale?.value === 'zh-CN' ? '请输入出行需求。' : 'Please enter a travel request.'
      aiResult.value = null
      return
    }

    aiLoading.value = true
    aiError.value = ''

    try {
      aiResult.value = await requestAdvice(query)
    } catch (error) {
      aiResult.value = null
      aiError.value = getErrorMessage(
        error,
        locale?.value === 'zh-CN'
          ? '生成出行建议失败，请稍后重试。'
          : 'Unable to generate travel advice right now.'
      )
    } finally {
      aiLoading.value = false
    }
  }

  return {
    aiForm,
    aiLoading,
    aiResult,
    aiError,
    aiCandidates,
    aiRecommendedFlight,
    aiSyncStatusText,
    submitAiAdvice,
    formatAiCandidateMeta
  }
}
