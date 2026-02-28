/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useGamePrefs, DEFAULT_GAME_PREFS } from '../useGamePrefs'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useGamePrefs', () => {
  it('returns defaults when nothing stored', () => {
    const { result } = renderHook(() => useGamePrefs())
    expect(result.current.prefs).toEqual(DEFAULT_GAME_PREFS)
  })

  it('persists changes to localStorage', () => {
    const { result } = renderHook(() => useGamePrefs())
    act(() => result.current.setPref('fourColorDeck', true))
    expect(result.current.prefs.fourColorDeck).toBe(true)
    expect(JSON.parse(store['ddpoker-game-prefs']).fourColorDeck).toBe(true)
  })

  it('loads stored prefs on mount', () => {
    store['ddpoker-game-prefs'] = JSON.stringify({ ...DEFAULT_GAME_PREFS, checkFold: true })
    const { result } = renderHook(() => useGamePrefs())
    expect(result.current.prefs.checkFold).toBe(true)
  })

  it('ignores invalid stored data', () => {
    store['ddpoker-game-prefs'] = 'not-json'
    const { result } = renderHook(() => useGamePrefs())
    expect(result.current.prefs).toEqual(DEFAULT_GAME_PREFS)
  })
})
