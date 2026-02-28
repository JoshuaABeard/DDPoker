/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { FULL_DECK, shuffle, removeCards } from '../deck'

describe('deck', () => {
  it('has 52 cards', () => {
    expect(FULL_DECK.length).toBe(52)
  })

  it('has no duplicates', () => {
    expect(new Set(FULL_DECK).size).toBe(52)
  })

  it('shuffle returns same length', () => {
    const deck = [...FULL_DECK]
    shuffle(deck)
    expect(deck.length).toBe(52)
    expect(new Set(deck).size).toBe(52)
  })

  it('removeCards removes specified cards', () => {
    const result = removeCards(FULL_DECK, ['Ah', 'Kd'])
    expect(result.length).toBe(50)
    expect(result).not.toContain('Ah')
    expect(result).not.toContain('Kd')
  })

  it('removeCards is case-insensitive', () => {
    const result = removeCards(FULL_DECK, ['ah', 'KD'])
    expect(result.length).toBe(50)
  })
})
