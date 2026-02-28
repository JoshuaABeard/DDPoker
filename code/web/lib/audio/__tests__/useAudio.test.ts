/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useAudio } from '../useAudio'
import { resetAudioManager } from '../AudioManager'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
  resetAudioManager()
})

beforeEach(() => {
  vi.stubGlobal('AudioContext', vi.fn(() => ({
    createGain: () => ({ gain: { value: 0 }, connect: vi.fn() }),
    createBufferSource: () => ({ buffer: null, connect: vi.fn(), start: vi.fn() }),
    decodeAudioData: vi.fn().mockResolvedValue({ duration: 1 }),
    destination: {},
  })))
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    arrayBuffer: () => Promise.resolve(new ArrayBuffer(8)),
  }))
})

describe('useAudio', () => {
  it('returns default volume and unmuted state', () => {
    const { result } = renderHook(() => useAudio())
    expect(result.current.volume).toBe(0.7)
    expect(result.current.isMuted).toBe(false)
  })

  it('toggleMute switches muted state', () => {
    const { result } = renderHook(() => useAudio())
    act(() => result.current.toggleMute())
    expect(result.current.isMuted).toBe(true)
    act(() => result.current.toggleMute())
    expect(result.current.isMuted).toBe(false)
  })

  it('setVolume updates volume', () => {
    const { result } = renderHook(() => useAudio())
    act(() => result.current.setVolume(0.4))
    expect(result.current.volume).toBe(0.4)
  })

  it('playSound is callable', () => {
    const { result } = renderHook(() => useAudio())
    expect(() => result.current.playSound('check')).not.toThrow()
  })
})
