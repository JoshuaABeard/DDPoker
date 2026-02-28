/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { formatChips, formatHandHistoryForExport } from '../utils'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

describe('formatChips', () => {
  it('formats zero', () => expect(formatChips(0)).toBe('0'))
  it('formats thousands', () => expect(formatChips(1000)).toBe('1,000'))
  it('formats large amounts', () => expect(formatChips(1000000)).toBe('1,000,000'))
  it('formats negative numbers', () => expect(formatChips(-500)).toBe('-500'))
  it('formats decimal input', () => expect(formatChips(1.5)).toBe('1.5'))
})

describe('formatHandHistoryForExport', () => {
  it('formats a complete hand', () => {
    const entries: HandHistoryEntry[] = [
      { id: '1', handNumber: 1, type: 'hand_start', timestamp: 1000 },
      { id: '2', handNumber: 1, type: 'action', playerName: 'Alice', action: 'CALL', amount: 100, timestamp: 1001 },
      { id: '3', handNumber: 1, type: 'action', playerName: 'Bob', action: 'FOLD', amount: 0, timestamp: 1002 },
      { id: '4', handNumber: 1, type: 'community', round: 'FLOP', cards: ['As', 'Kd', 'Qc'], timestamp: 1003 },
      { id: '5', handNumber: 1, type: 'result', winners: [{ playerName: 'Alice', amount: 200, hand: 'Pair of Aces' }], timestamp: 1004 },
    ]
    const output = formatHandHistoryForExport(entries)
    expect(output).toContain('Hand #1')
    expect(output).toContain('Alice: CALL 100')
    expect(output).toContain('Bob: FOLD')
    expect(output).toContain('FLOP: As Kd Qc')
    expect(output).toContain('Alice wins 200 with Pair of Aces')
  })

  it('returns empty string for empty entries', () => {
    const output = formatHandHistoryForExport([])
    expect(output).toBe('')
  })
})
