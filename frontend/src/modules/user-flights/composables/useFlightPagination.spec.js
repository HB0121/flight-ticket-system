import { computed, nextTick, ref } from 'vue'
import { describe, expect, it } from 'vitest'
import { useFlightPagination } from './useFlightPagination.js'

function createRows(count) {
  return Array.from({ length: count }, (_, index) => ({ id: index + 1 }))
}

describe('useFlightPagination', () => {
  it('paginates rows with the existing default page size and visible page window', () => {
    const rows = ref(createRows(23))
    const pagination = useFlightPagination(rows)

    expect(pagination.pageSize.value).toBe(10)
    expect(pagination.totalCount.value).toBe(23)
    expect(pagination.totalPages.value).toBe(3)
    expect(pagination.paginationRangeStart.value).toBe(1)
    expect(pagination.paginationRangeEnd.value).toBe(10)
    expect(pagination.visiblePageNumbers.value).toEqual([1, 2, 3])
    expect(pagination.pagedFlights.value.map(row => row.id)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
  })

  it('clamps requested pages to the available page range', () => {
    const rows = ref(createRows(23))
    const pagination = useFlightPagination(rows)

    pagination.handlePageChange(99)

    expect(pagination.currentPage.value).toBe(3)
    expect(pagination.paginationRangeStart.value).toBe(21)
    expect(pagination.paginationRangeEnd.value).toBe(23)

    pagination.handlePageChange(0)

    expect(pagination.currentPage.value).toBe(1)
  })

  it('resets to the first page when page size changes', async () => {
    const rows = ref(createRows(23))
    const pagination = useFlightPagination(rows)

    pagination.goToLastPage()
    expect(pagination.currentPage.value).toBe(3)

    pagination.pageSize.value = 20
    await nextTick()

    expect(pagination.currentPage.value).toBe(1)
    expect(pagination.totalPages.value).toBe(2)
    expect(pagination.pagedFlights.value).toHaveLength(20)
  })

  it('keeps the current page inside range when the filtered source shrinks', async () => {
    const rows = ref(createRows(23))
    const filteredRows = computed(() => rows.value)
    const pagination = useFlightPagination(filteredRows)

    pagination.goToLastPage()
    rows.value = createRows(11)
    await nextTick()

    expect(pagination.currentPage.value).toBe(2)
    expect(pagination.paginationRangeStart.value).toBe(11)
    expect(pagination.paginationRangeEnd.value).toBe(11)
  })
})
