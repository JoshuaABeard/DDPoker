/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useCardBack, CARD_BACK_IDS } from '../useCardBack'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useCardBack', () => {
  it('defaults to classic-red', () => {
    const { result } = renderHook(() => useCardBack())
    expect(result.current.cardBackId).toBe('classic-red')
  })

  it('switches card back and persists', () => {
    const { result } = renderHook(() => useCardBack())
    act(() => result.current.setCardBack('blue-diamond'))
    expect(result.current.cardBackId).toBe('blue-diamond')
    expect(store['ddpoker-card-back']).toBe('blue-diamond')
  })

  it('has 4 card back options', () => {
    expect(CARD_BACK_IDS.length).toBe(4)
  })
})
