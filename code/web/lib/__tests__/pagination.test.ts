/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import {
  toBackendPage,
  toFrontendPage,
  calculateTotalPages,
  buildPaginationResult,
} from '../pagination'

describe('toBackendPage', () => {
  it('converts page 1 to backend page 0', () => {
    expect(toBackendPage(1)).toBe(0)
  })

  it('converts page 5 to backend page 4', () => {
    expect(toBackendPage(5)).toBe(4)
  })

  it('clamps negative frontend pages to 0', () => {
    expect(toBackendPage(0)).toBe(0)
    expect(toBackendPage(-1)).toBe(0)
  })
})

describe('toFrontendPage', () => {
  it('converts backend page 0 to frontend page 1', () => {
    expect(toFrontendPage(0)).toBe(1)
  })

  it('converts backend page 4 to frontend page 5', () => {
    expect(toFrontendPage(4)).toBe(5)
  })

  it('clamps negative backend pages to 1', () => {
    expect(toFrontendPage(-1)).toBe(1)
  })
})

describe('calculateTotalPages', () => {
  it('calculates pages correctly', () => {
    expect(calculateTotalPages(10, 5)).toBe(2)
    expect(calculateTotalPages(11, 5)).toBe(3)
    expect(calculateTotalPages(5, 5)).toBe(1)
  })

  it('returns 0 for zero or negative total items', () => {
    expect(calculateTotalPages(0, 10)).toBe(0)
    expect(calculateTotalPages(-1, 10)).toBe(0)
  })

  it('returns 0 for zero or negative page size', () => {
    expect(calculateTotalPages(10, 0)).toBe(0)
    expect(calculateTotalPages(10, -1)).toBe(0)
  })
})

describe('buildPaginationResult', () => {
  it('builds result with correct fields', () => {
    const items = ['a', 'b', 'c']
    const result = buildPaginationResult(items, 30, 2, 10)
    expect(result.data).toEqual(['a', 'b', 'c'])
    expect(result.totalPages).toBe(3)
    expect(result.totalItems).toBe(30)
    expect(result.currentPage).toBe(2)
  })

  it('handles empty items array', () => {
    const result = buildPaginationResult([], 0, 1, 10)
    expect(result.data).toEqual([])
    expect(result.totalPages).toBe(0)
    expect(result.totalItems).toBe(0)
    expect(result.currentPage).toBe(1)
  })
})
