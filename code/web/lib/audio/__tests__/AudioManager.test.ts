/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { AudioManager } from '../AudioManager'

const store: Record<string, string> = {}
beforeEach(() => {
  Object.keys(store).forEach((k) => delete store[k])
  vi.spyOn(Storage.prototype, 'getItem').mockImplementation((key) => store[key] ?? null)
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation((key, value) => {
    store[key] = value
  })
})

const mockStart = vi.fn()
const mockSourceConnect = vi.fn()
const mockGainConnect = vi.fn()
const mockGain = { value: 0 }
const mockDecodeAudioData = vi.fn().mockResolvedValue({ duration: 1 })

beforeEach(() => {
  mockStart.mockClear()
  mockSourceConnect.mockClear()
  mockGainConnect.mockClear()
  mockDecodeAudioData.mockClear()
  mockGain.value = 0

  vi.stubGlobal('AudioContext', vi.fn(() => ({
    createGain: () => ({ gain: mockGain, connect: mockGainConnect }),
    createBufferSource: () => ({ buffer: null, connect: mockSourceConnect, start: mockStart }),
    decodeAudioData: mockDecodeAudioData,
    destination: {},
  })))

  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    arrayBuffer: () => Promise.resolve(new ArrayBuffer(8)),
  }))
})

describe('AudioManager', () => {
  it('starts with default volume 0.7 and unmuted', () => {
    const mgr = new AudioManager()
    expect(mgr.getVolume()).toBe(0.7)
    expect(mgr.isMuted()).toBe(false)
  })

  it('loads saved settings from localStorage', () => {
    store['ddpoker-audio'] = JSON.stringify({ volume: 0.3, muted: true })
    const mgr = new AudioManager()
    expect(mgr.getVolume()).toBe(0.3)
    expect(mgr.isMuted()).toBe(true)
  })

  it('persists volume changes to localStorage', () => {
    const mgr = new AudioManager()
    mgr.setVolume(0.5)
    expect(store['ddpoker-audio']).toContain('"volume":0.5')
  })

  it('persists mute changes to localStorage', () => {
    const mgr = new AudioManager()
    mgr.setMuted(true)
    expect(store['ddpoker-audio']).toContain('"muted":true')
  })

  it('clamps volume to 0-1 range', () => {
    const mgr = new AudioManager()
    mgr.setVolume(1.5)
    expect(mgr.getVolume()).toBe(1)
    mgr.setVolume(-0.5)
    expect(mgr.getVolume()).toBe(0)
  })

  it('sets gain to 0 when muted after init', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.setMuted(true)
    expect(mockGain.value).toBe(0)
  })

  it('restores gain when unmuted after init', async () => {
    const mgr = new AudioManager()
    mgr.setVolume(0.6)
    await mgr.init()
    mgr.setMuted(true)
    mgr.setMuted(false)
    expect(mockGain.value).toBe(0.6)
  })

  it('plays a sound by creating a buffer source', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.play('check')
    expect(mockSourceConnect).toHaveBeenCalled()
    expect(mockStart).toHaveBeenCalled()
  })

  it('does not play when muted', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.setMuted(true)
    mgr.play('check')
    expect(mockStart).not.toHaveBeenCalled()
  })

  it('does not play unknown sound names', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    mgr.play('nonexistent')
    expect(mockStart).not.toHaveBeenCalled()
  })

  it('init is idempotent', async () => {
    const mgr = new AudioManager()
    await mgr.init()
    await mgr.init()
    expect(AudioContext).toHaveBeenCalledTimes(1)
  })
})
