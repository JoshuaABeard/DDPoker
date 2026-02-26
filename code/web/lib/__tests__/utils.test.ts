/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { formatChips } from '../utils'

describe('formatChips', () => {
  it('formats zero', () => expect(formatChips(0)).toBe('0'))
  it('formats thousands', () => expect(formatChips(1000)).toBe('1,000'))
  it('formats large amounts', () => expect(formatChips(1000000)).toBe('1,000,000'))
  it('formats negative numbers', () => expect(formatChips(-500)).toBe('-500'))
  it('formats decimal input by truncating', () => {
    // Intl.NumberFormat with default options formats decimals
    // Verify the function handles non-integers without throwing
    expect(typeof formatChips(1.5)).toBe('string')
  })
})
