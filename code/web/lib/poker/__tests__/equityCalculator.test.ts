/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { calculateEquity } from '../equityCalculator'

describe('calculateEquity', () => {
  it('returns percentages that sum to ~100', () => {
    const result = calculateEquity(['Ah', 'Kh'], [], 1, 1000)
    const total = result.win + result.tie + result.loss
    expect(total).toBeGreaterThan(99)
    expect(total).toBeLessThan(101)
  })

  it('AA vs 1 opponent has ~80%+ equity', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 1, 5000)
    expect(result.win).toBeGreaterThan(75)
  })

  it('known board gives deterministic results with enough iterations', () => {
    // AA with board AAKK2 — quads, nearly unbeatable
    const result = calculateEquity(['Ah', 'Ad'], ['Ac', 'As', 'Kh', 'Kd', '2c'], 1, 1000)
    expect(result.win).toBeGreaterThan(95)
  })

  it('respects iteration count', () => {
    const result = calculateEquity(['Ah', 'Kh'], [], 1, 100)
    expect(result.iterations).toBe(100)
  })

  it('handles multiple opponents', () => {
    const result = calculateEquity(['Ah', 'Ad'], [], 3, 1000)
    expect(result.win).toBeGreaterThan(50)
    expect(result.loss).toBeGreaterThan(0)
  })
})
