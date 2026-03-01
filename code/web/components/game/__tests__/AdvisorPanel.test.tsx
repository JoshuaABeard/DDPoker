/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AdvisorPanel } from '../AdvisorPanel'
import { HandRank } from '@/lib/poker/types'

// Mock poker math functions to avoid slow Monte Carlo in tests
vi.mock('@/lib/poker/handEvaluator', () => ({
  evaluateHand: vi.fn(() => ({
    rank: HandRank.TWO_PAIR,
    kickers: [14, 13, 12],
    description: 'Two Pair, Aces and Kings',
  })),
  compareHands: vi.fn(() => 1),
}))

vi.mock('@/lib/poker/equityCalculator', () => ({
  calculateEquity: vi.fn(() => ({
    win: 72.3,
    tie: 1.5,
    loss: 26.2,
    iterations: 2000,
  })),
}))

vi.mock('../StartingHandsChart', () => ({
  StartingHandsChart: ({ compact }: { compact?: boolean }) => (
    <div data-testid="starting-hands-chart" data-compact={compact ? 'true' : 'false'} />
  ),
}))

const defaultProps = {
  holeCards: ['Ah', 'Kd'],
  communityCards: ['As', 'Kc', '2h'],
  potSize: 1000,
  callAmount: 200,
  numOpponents: 3,
  onClose: vi.fn(),
}

describe('AdvisorPanel', () => {
  it('renders with "AI Advisor" heading', () => {
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.getByText('AI Advisor')).toBeDefined()
  })

  it('close button calls onClose', () => {
    const onClose = vi.fn()
    render(<AdvisorPanel {...defaultProps} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText('Close advisor'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  // -------------------------------------------------------------------------
  // Hand Strength section
  // -------------------------------------------------------------------------

  it('shows hand description when 5+ cards available', () => {
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.getByText('Two Pair, Aces and Kings')).toBeDefined()
  })

  it('shows "Waiting for flop..." when fewer than 5 cards', () => {
    render(<AdvisorPanel {...defaultProps} communityCards={['As']} />)
    expect(screen.getByText('Waiting for flop...')).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Equity section
  // -------------------------------------------------------------------------

  it('shows equity percentage with opponent count', () => {
    render(<AdvisorPanel {...defaultProps} />)
    // 72.3 + 1.5 = 73.8% — appears in both Equity and Pot Odds sections
    const matches = screen.getAllByText(/73\.8%/)
    expect(matches.length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/3 opponents/)).toBeDefined()
  })

  it('shows fallback when no opponents', () => {
    render(<AdvisorPanel {...defaultProps} numOpponents={0} />)
    expect(screen.getByText('Need hole cards and opponents')).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Pot Odds section
  // -------------------------------------------------------------------------

  it('shows pot odds and +EV indicator when equity > pot odds', () => {
    // potOdds = 200 / (1000+200) = 16.7%, equity = 73.8% => +EV
    render(<AdvisorPanel {...defaultProps} />)
    // "Pot Odds" appears as both section heading and inline text
    const potOddsMatches = screen.getAllByText(/Pot Odds/)
    expect(potOddsMatches.length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/\+EV/)).toBeDefined()
  })

  it('shows "Free check" when callAmount is 0', () => {
    render(<AdvisorPanel {...defaultProps} callAmount={0} />)
    expect(screen.getByText('Free check')).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Starting Hand section (preflop only)
  // -------------------------------------------------------------------------

  it('shows starting hand chart when on preflop', () => {
    render(<AdvisorPanel {...defaultProps} communityCards={[]} />)
    expect(screen.getByTestId('starting-hands-chart')).toBeDefined()
  })

  it('does not show starting hand chart post-flop', () => {
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.queryByTestId('starting-hands-chart')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // Recommendation section
  // -------------------------------------------------------------------------

  it('shows recommendation when equity data is available', () => {
    // equity (73.8%) > potOdds (16.7%) by >10, so "Raise or Call"
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.getByText('Raise or Call')).toBeDefined()
  })

  it('shows "Check" recommendation when callAmount is 0', () => {
    render(<AdvisorPanel {...defaultProps} callAmount={0} />)
    expect(screen.getByText(/Check/)).toBeDefined()
  })

  it('renders with singular "opponent" for 1 opponent', () => {
    render(<AdvisorPanel {...defaultProps} numOpponents={1} />)
    expect(screen.getByText(/1 opponent\b/)).toBeDefined()
  })
})
