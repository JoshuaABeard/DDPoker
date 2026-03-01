/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { Dashboard } from '../Dashboard'
import { HandRank } from '@/lib/poker/types'

// Mock poker math functions
vi.mock('@/lib/poker/handEvaluator', () => ({
  evaluateHand: vi.fn(() => ({
    rank: HandRank.ONE_PAIR,
    kickers: [14, 13, 12],
    description: 'One Pair, Aces',
  })),
}))

vi.mock('@/lib/poker/equityCalculator', () => ({
  calculateEquity: vi.fn(() => ({
    win: 55.0,
    tie: 2.0,
    loss: 43.0,
    iterations: 1000,
  })),
}))

vi.mock('../StartingHandsChart', () => ({
  StartingHandsChart: ({ compact }: { compact?: boolean }) => (
    <div data-testid="starting-hands-chart" data-compact={compact ? 'true' : 'false'} />
  ),
}))

const defaultProps = {
  holeCards: ['Ah', 'Kd'],
  communityCards: ['As', '7c', '2h'],
  potSize: 500,
  callAmount: 100,
  numOpponents: 2,
  level: 3,
  blinds: { small: 50, big: 100, ante: 10 },
  nextLevelIn: 720,
  playersRemaining: 6,
  totalPlayers: 10,
  playerRank: 3,
  onClose: vi.fn(),
}

beforeEach(() => {
  localStorage.clear()
})

describe('Dashboard', () => {
  // -------------------------------------------------------------------------
  // Structure
  // -------------------------------------------------------------------------

  it('renders with "Dashboard" heading', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.getByText('Dashboard')).toBeDefined()
  })

  it('close button calls onClose', () => {
    const onClose = vi.fn()
    render(<Dashboard {...defaultProps} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText('Close dashboard'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  // -------------------------------------------------------------------------
  // Widget rendering
  // -------------------------------------------------------------------------

  it('renders hand strength widget with hand rank name', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.getByText('Hand Strength')).toBeDefined()
    expect(screen.getByText('One Pair')).toBeDefined()
  })

  it('renders pot odds widget with call amount', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.getByText('Pot Odds')).toBeDefined()
    expect(screen.getByText(/Call: 100/)).toBeDefined()
  })

  it('shows "Free check" when callAmount is 0', () => {
    render(<Dashboard {...defaultProps} callAmount={0} />)
    expect(screen.getByText('Free check')).toBeDefined()
  })

  it('renders tournament info widget', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.getByText('Tournament Info')).toBeDefined()
    expect(screen.getByText('Level: 3')).toBeDefined()
    expect(screen.getByText(/50\/100/)).toBeDefined()
    expect(screen.getByText(/ante 10/)).toBeDefined()
    expect(screen.getByText(/Players: 6/)).toBeDefined()
  })

  it('renders rank widget with ordinal position', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.getByText('Rank')).toBeDefined()
    expect(screen.getByText(/3rd of 10 players/)).toBeDefined()
  })

  it('shows "Not available" for rank when data is missing', () => {
    render(<Dashboard {...defaultProps} playerRank={undefined} totalPlayers={undefined} />)
    expect(screen.getByText('Not available')).toBeDefined()
  })

  it('renders starting hand chart on preflop', () => {
    render(<Dashboard {...defaultProps} communityCards={[]} />)
    expect(screen.getByTestId('starting-hands-chart')).toBeDefined()
  })

  it('shows "Preflop only" for starting hand post-flop', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.getByText('Preflop only')).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Settings panel
  // -------------------------------------------------------------------------

  it('settings panel is hidden by default', () => {
    render(<Dashboard {...defaultProps} />)
    expect(screen.queryByText('Settings')).toBeNull()
  })

  it('settings gear toggles settings panel', () => {
    render(<Dashboard {...defaultProps} />)
    fireEvent.click(screen.getByLabelText('Dashboard settings'))
    expect(screen.getByText('Settings')).toBeDefined()
    // All 5 default widgets listed with checkboxes
    expect(screen.getByLabelText('Toggle Hand Strength')).toBeDefined()
    expect(screen.getByLabelText('Toggle Pot Odds')).toBeDefined()
    expect(screen.getByLabelText('Toggle Tournament Info')).toBeDefined()
    expect(screen.getByLabelText('Toggle Rank')).toBeDefined()
    expect(screen.getByLabelText('Toggle Starting Hand')).toBeDefined()
  })

  it('toggling a widget checkbox hides that widget', () => {
    render(<Dashboard {...defaultProps} />)
    // Rank widget visible initially
    expect(screen.getByText(/3rd of 10 players/)).toBeDefined()

    // Open settings and uncheck Rank
    fireEvent.click(screen.getByLabelText('Dashboard settings'))
    fireEvent.click(screen.getByLabelText('Toggle Rank'))

    // Rank widget content should be gone
    expect(screen.queryByText(/3rd of 10 players/)).toBeNull()
  })

  it('Reset button restores default widget visibility', () => {
    render(<Dashboard {...defaultProps} />)

    // Open settings and hide Rank
    fireEvent.click(screen.getByLabelText('Dashboard settings'))
    fireEvent.click(screen.getByLabelText('Toggle Rank'))
    expect(screen.queryByText(/3rd of 10 players/)).toBeNull()

    // Reset
    fireEvent.click(screen.getByText('Reset'))
    expect(screen.getByText(/3rd of 10 players/)).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Ordinal suffix edge cases
  // -------------------------------------------------------------------------

  it('shows 1st for rank 1', () => {
    render(<Dashboard {...defaultProps} playerRank={1} />)
    expect(screen.getByText(/1st of 10 players/)).toBeDefined()
  })

  it('shows 11th for rank 11 (not 11st)', () => {
    render(<Dashboard {...defaultProps} playerRank={11} totalPlayers={20} />)
    expect(screen.getByText(/11th of 20 players/)).toBeDefined()
  })
})
