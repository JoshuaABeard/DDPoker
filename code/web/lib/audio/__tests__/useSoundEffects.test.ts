/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook } from '@testing-library/react'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useSoundEffects } from '../useSoundEffects'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

const mockPlaySound = vi.fn()
vi.mock('../useAudio', () => ({
  useAudio: () => ({
    playSound: mockPlaySound,
    volume: 0.7,
    setVolume: vi.fn(),
    isMuted: false,
    toggleMute: vi.fn(),
  }),
}))

function entry(overrides: Partial<HandHistoryEntry>): HandHistoryEntry {
  return {
    id: String(Math.random()),
    handNumber: 1,
    type: 'action',
    timestamp: Date.now(),
    ...overrides,
  }
}

describe('useSoundEffects', () => {
  beforeEach(() => {
    mockPlaySound.mockClear()
  })

  it('plays shuffle on hand_start', () => {
    const entries = [entry({ type: 'hand_start' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('shuffle')
  })

  it('plays bet on BET action', () => {
    const entries = [entry({ type: 'action', action: 'BET' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('bet')
  })

  it('plays check on CHECK action', () => {
    const entries = [entry({ type: 'action', action: 'CHECK' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('check')
  })

  it('plays raise on RAISE action', () => {
    const entries = [entry({ type: 'action', action: 'RAISE' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('raise')
  })

  it('plays click on FOLD action', () => {
    const entries = [entry({ type: 'action', action: 'FOLD' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('click')
  })

  it('plays shuffleShort on community cards', () => {
    const entries = [entry({ type: 'community', round: 'Flop', cards: ['Ah', 'Kd', '3c'] })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('shuffleShort')
  })

  it('plays cheers on result', () => {
    const entries = [entry({ type: 'result', winners: [{ playerName: 'P1', amount: 100, hand: 'Pair' }] })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    expect(mockPlaySound).toHaveBeenCalledWith('cheers')
  })

  it('plays bell when it becomes your turn', () => {
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: [], a: true })
    expect(mockPlaySound).toHaveBeenCalledWith('bell')
  })

  it('does not play bell if already your turn', () => {
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: true } },
    )
    rerender({ h: [], a: true })
    expect(mockPlaySound).not.toHaveBeenCalledWith('bell')
  })

  it('does not replay sounds for old entries', () => {
    const entries = [entry({ type: 'action', action: 'CHECK' })]
    const { rerender } = renderHook(
      ({ h, a }) => useSoundEffects(h, a),
      { initialProps: { h: [] as HandHistoryEntry[], a: false } },
    )
    rerender({ h: entries, a: false })
    mockPlaySound.mockClear()
    rerender({ h: entries, a: false })
    expect(mockPlaySound).not.toHaveBeenCalled()
  })
})
