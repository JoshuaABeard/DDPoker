/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { HandReplay } from '../HandReplay'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

function e(overrides: Partial<HandHistoryEntry>): HandHistoryEntry {
  return { id: String(Math.random()), handNumber: 1, type: 'action', timestamp: Date.now(), ...overrides }
}

const entries: HandHistoryEntry[] = [
  e({ type: 'hand_start', handNumber: 1 }),
  e({ type: 'action', playerName: 'Alice', action: 'CALL', amount: 50 }),
  e({ type: 'community', round: 'Flop', cards: ['Ah', 'Kd', '3c'] }),
  e({ type: 'result', winners: [{ playerName: 'Alice', amount: 100, hand: 'Pair' }] }),
]

describe('HandReplay', () => {
  it('renders playback controls', () => {
    render(<HandReplay entries={entries} onClose={() => {}} />)
    expect(screen.getByLabelText(/next/i)).toBeDefined()
    expect(screen.getByLabelText(/previous/i)).toBeDefined()
  })

  it('shows hand number in title', () => {
    render(<HandReplay entries={entries} onClose={() => {}} />)
    expect(screen.getByText(/hand #1 replay/i)).toBeDefined()
  })

  it('advances step on next click', () => {
    render(<HandReplay entries={entries} onClose={() => {}} />)
    fireEvent.click(screen.getByLabelText(/next/i))
    expect(screen.getByText(/alice/i)).toBeDefined()
  })

  it('calls onClose when close button clicked', () => {
    const onClose = vi.fn()
    render(<HandReplay entries={entries} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText(/close/i))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
