import { computed, ref, watch } from 'vue'

const DEFAULT_PAGE_SIZE = 10

function resolvePageSize(pageSize) {
  return Number(pageSize || DEFAULT_PAGE_SIZE)
}

export function useFlightPagination(sourceRows, options = {}) {
  const currentPage = ref(1)
  const pageSize = ref(options.pageSize ?? DEFAULT_PAGE_SIZE)

  const rows = computed(() => sourceRows.value ?? [])
  const totalCount = computed(() => rows.value.length)
  const totalPages = computed(() => Math.max(Math.ceil(totalCount.value / resolvePageSize(pageSize.value)), 1))
  const paginationRangeStart = computed(() => (
    totalCount.value ? (currentPage.value - 1) * resolvePageSize(pageSize.value) + 1 : 0
  ))
  const paginationRangeEnd = computed(() => Math.min(currentPage.value * resolvePageSize(pageSize.value), totalCount.value))
  const visiblePageNumbers = computed(() => {
    const pages = new Set([1, totalPages.value])
    for (let page = currentPage.value - 1; page <= currentPage.value + 1; page += 1) {
      if (page >= 1 && page <= totalPages.value) pages.add(page)
    }
    return [...pages].sort((a, b) => a - b)
  })
  const pagedFlights = computed(() => {
    const size = resolvePageSize(pageSize.value)
    const startIndex = (currentPage.value - 1) * size
    return rows.value.slice(startIndex, startIndex + size)
  })

  function resetPagination() {
    currentPage.value = 1
  }

  function handlePageChange(page) {
    const nextPage = Number(page) || 1
    const maxPage = Math.max(Math.ceil(totalCount.value / resolvePageSize(pageSize.value)), 1)
    currentPage.value = Math.min(Math.max(nextPage, 1), maxPage)
  }

  function goToFirstPage() {
    handlePageChange(1)
  }

  function goToLastPage() {
    handlePageChange(totalPages.value)
  }

  watch([rows, pageSize], () => {
    const maxPage = Math.max(Math.ceil(totalCount.value / resolvePageSize(pageSize.value)), 1)
    if (currentPage.value > maxPage) currentPage.value = maxPage
  })

  watch(pageSize, () => {
    resetPagination()
  })

  return {
    currentPage,
    pageSize,
    totalCount,
    totalPages,
    paginationRangeStart,
    paginationRangeEnd,
    visiblePageNumbers,
    pagedFlights,
    resetPagination,
    handlePageChange,
    goToFirstPage,
    goToLastPage
  }
}
