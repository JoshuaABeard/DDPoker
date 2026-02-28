/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAvatar, AVATAR_IDS } from '../useAvatar'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

describe('useAvatar', () => {
  it('defaults to spade', () => {
    const { result } = renderHook(() => useAvatar())
    expect(result.current.avatarId).toBe('spade')
  })

  it('switches avatar and persists', () => {
    const { result } = renderHook(() => useAvatar())
    act(() => result.current.setAvatar('crown'))
    expect(result.current.avatarId).toBe('crown')
    expect(store['ddpoker-avatar']).toBe('crown')
  })

  it('has 12 avatar options', () => {
    expect(AVATAR_IDS.length).toBe(12)
  })
})
