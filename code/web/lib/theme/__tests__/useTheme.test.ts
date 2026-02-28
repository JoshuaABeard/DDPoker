/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useTheme } from '../useTheme'
import { THEMES } from '../themes'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useTheme', () => {
  it('defaults to classic-green', () => {
    const { result } = renderHook(() => useTheme())
    expect(result.current.themeId).toBe('classic-green')
    expect(result.current.colors.center).toBe('#2d5a1b')
  })

  it('switches theme and persists', () => {
    const { result } = renderHook(() => useTheme())
    act(() => result.current.setTheme('royal-blue'))
    expect(result.current.themeId).toBe('royal-blue')
    expect(store['ddpoker-theme']).toBe('royal-blue')
  })

  it('loads saved theme from localStorage', () => {
    store['ddpoker-theme'] = 'casino-red'
    const { result } = renderHook(() => useTheme())
    expect(result.current.themeId).toBe('casino-red')
  })

  it('falls back to default for invalid saved theme', () => {
    store['ddpoker-theme'] = 'nonexistent'
    const { result } = renderHook(() => useTheme())
    expect(result.current.themeId).toBe('classic-green')
  })

  it('exports all available themes', () => {
    expect(Object.keys(THEMES).length).toBe(5)
  })
})
