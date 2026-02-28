/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useHandReplayState } from '../useHandReplayState'
import type { HandHistoryEntry } from '../gameReducer'

function e(overrides: Partial<HandHistoryEntry>): HandHistoryEntry {
  return { id: String(Math.random()), handNumber: 1, type: 'action', timestamp: Date.now(), ...overrides }
}

const sampleEntries: HandHistoryEntry[] = [
  e({ type: 'hand_start', handNumber: 1 }),
  e({ type: 'action', playerName: 'Alice', action: 'CALL', amount: 50 }),
  e({ type: 'action', playerName: 'Bob', action: 'RAISE', amount: 150 }),
  e({ type: 'community', round: 'Flop', cards: ['Ah', 'Kd', '3c'] }),
  e({ type: 'action', playerName: 'Alice', action: 'CHECK' }),
  e({ type: 'action', playerName: 'Bob', action: 'BET', amount: 200 }),
  e({ type: 'community', round: 'Turn', cards: ['Ah', 'Kd', '3c', '7s'] }),
  e({ type: 'result', winners: [{ playerName: 'Bob', amount: 600, hand: 'Pair of Kings' }] }),
]

describe('useHandReplayState', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('initializes at step 0', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    expect(result.current.currentStep).toBe(0)
    expect(result.current.totalSteps).toBe(sampleEntries.length)
  })

  it('advances to next step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.next())
    expect(result.current.currentStep).toBe(1)
  })

  it('goes back to previous step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.next())
    act(() => result.current.next())
    act(() => result.current.prev())
    expect(result.current.currentStep).toBe(1)
  })

  it('does not go below step 0', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.prev())
    expect(result.current.currentStep).toBe(0)
  })

  it('does not go past last step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    for (let i = 0; i < 20; i++) act(() => result.current.next())
    expect(result.current.currentStep).toBe(sampleEntries.length - 1)
  })

  it('returns visible entries up to current step', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.next()) // step 1
    act(() => result.current.next()) // step 2
    expect(result.current.visibleEntries).toHaveLength(3) // steps 0,1,2
  })

  it('derives community cards from visible entries', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    // Advance to the flop (step 3)
    act(() => { result.current.next(); result.current.next(); result.current.next(); result.current.next() })
    expect(result.current.communityCards).toEqual(['Ah', 'Kd', '3c'])
  })

  it('auto-plays at set speed', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.play())
    expect(result.current.isPlaying).toBe(true)
    act(() => { vi.advanceTimersByTime(1000) })
    expect(result.current.currentStep).toBe(1)
  })

  it('pauses auto-play', () => {
    const { result } = renderHook(() => useHandReplayState(sampleEntries))
    act(() => result.current.play())
    act(() => result.current.pause())
    expect(result.current.isPlaying).toBe(false)
  })
})
