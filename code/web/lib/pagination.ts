/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Pagination Utilities
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

/**
 * Convert frontend 1-based page number to backend 0-based page number
 */
export function toBackendPage(frontendPage: number): number {
  return Math.max(0, frontendPage - 1)
}

/**
 * Convert backend 0-based page number to frontend 1-based page number
 */
export function toFrontendPage(backendPage: number): number {
  return backendPage + 1
}

/**
 * Calculate total pages from total items and page size
 */
export function calculateTotalPages(totalItems: number, pageSize: number): number {
  return Math.ceil(totalItems / pageSize)
}

/**
 * Build a standardized pagination result object
 */
export function buildPaginationResult<T>(
  items: T[],
  totalItems: number,
  frontendPage: number,
  pageSize: number
) {
  return {
    data: items,
    totalPages: calculateTotalPages(totalItems, pageSize),
    totalItems,
    currentPage: frontendPage,
  }
}
