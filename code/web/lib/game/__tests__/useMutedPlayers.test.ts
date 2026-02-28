/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useMutedPlayers } from '../useMutedPlayers'

// Mock localStorage
const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useMutedPlayers', () => {
  it('starts with empty set', () => {
    const { result } = renderHook(() => useMutedPlayers())
    expect(result.current.isMuted(1)).toBe(false)
  })

  it('mutes a player', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(42))
    expect(result.current.isMuted(42)).toBe(true)
  })

  it('unmutes a player', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(42))
    act(() => result.current.unmute(42))
    expect(result.current.isMuted(42)).toBe(false)
  })

  it('persists to localStorage', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(1))
    act(() => result.current.mute(2))
    expect(store['ddpoker-muted-players']).toBe('[1,2]')
  })

  it('loads from localStorage', () => {
    store['ddpoker-muted-players'] = '[10,20]'
    const { result } = renderHook(() => useMutedPlayers())
    expect(result.current.isMuted(10)).toBe(true)
    expect(result.current.isMuted(20)).toBe(true)
    expect(result.current.isMuted(30)).toBe(false)
  })

  it('returns mutedIds set', () => {
    const { result } = renderHook(() => useMutedPlayers())
    act(() => result.current.mute(5))
    expect(result.current.mutedIds.has(5)).toBe(true)
  })
})
