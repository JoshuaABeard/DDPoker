/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AdvisorPanel } from '../AdvisorPanel'
import type { AdvisorData } from '@/lib/game/types'

vi.mock('../StartingHandsChart', () => ({
  StartingHandsChart: ({ compact }: { compact?: boolean }) => (
    <div data-testid="starting-hands-chart" data-compact={compact ? 'true' : 'false'} />
  ),
}))

const defaultAdvisorData: AdvisorData = {
  handRank: 2, // TWO_PAIR
  handDescription: 'Two Pair, Aces and Kings',
  equity: 73.8,
  potOdds: 16.7,
  recommendation: 'Raise or Call',
  startingHandCategory: null,
  startingHandNotation: null,
}

const defaultProps = {
  advisorData: defaultAdvisorData,
  holeCards: ['Ah', 'Kd'],
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
  // Null advisorData (waiting state)
  // -------------------------------------------------------------------------

  it('shows "Waiting for data..." when advisorData is null', () => {
    render(<AdvisorPanel {...defaultProps} advisorData={null} />)
    expect(screen.getByText('Waiting for data...')).toBeDefined()
  })

  it('close button works when advisorData is null', () => {
    const onClose = vi.fn()
    render(<AdvisorPanel {...defaultProps} advisorData={null} onClose={onClose} />)
    fireEvent.click(screen.getByLabelText('Close advisor'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  // -------------------------------------------------------------------------
  // Hand Strength section
  // -------------------------------------------------------------------------

  it('shows hand description from server data', () => {
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.getByText('Two Pair, Aces and Kings')).toBeDefined()
  })

  it('shows "Waiting for flop..." when handRank is null', () => {
    const data: AdvisorData = { ...defaultAdvisorData, handRank: null, handDescription: null }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    expect(screen.getByText('Waiting for flop...')).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Equity section
  // -------------------------------------------------------------------------

  it('shows equity percentage from server data', () => {
    render(<AdvisorPanel {...defaultProps} />)
    const matches = screen.getAllByText(/73\.8%/)
    expect(matches.length).toBeGreaterThanOrEqual(1)
  })

  it('shows "No equity data" when equity is 0', () => {
    const data: AdvisorData = { ...defaultAdvisorData, equity: 0 }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    expect(screen.getByText('No equity data')).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Pot Odds section
  // -------------------------------------------------------------------------

  it('shows pot odds and +EV indicator when equity > pot odds', () => {
    render(<AdvisorPanel {...defaultProps} />)
    const potOddsMatches = screen.getAllByText(/Pot Odds/)
    expect(potOddsMatches.length).toBeGreaterThanOrEqual(1)
    expect(screen.getByText(/\+EV/)).toBeDefined()
  })

  it('shows "Free check" when potOdds is 0', () => {
    const data: AdvisorData = { ...defaultAdvisorData, potOdds: 0, recommendation: 'Check — no cost to see more cards' }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    expect(screen.getByText('Free check')).toBeDefined()
  })

  it('shows -EV indicator when equity < pot odds', () => {
    const data: AdvisorData = { ...defaultAdvisorData, equity: 10, potOdds: 25 }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    expect(screen.getByText(/-EV/)).toBeDefined()
  })

  // -------------------------------------------------------------------------
  // Starting Hand section (preflop only)
  // -------------------------------------------------------------------------

  it('shows starting hand chart when server provides starting hand data', () => {
    const data: AdvisorData = {
      ...defaultAdvisorData,
      handRank: null,
      handDescription: null,
      startingHandNotation: 'AKo',
      startingHandCategory: 'premium',
    }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    expect(screen.getByTestId('starting-hands-chart')).toBeDefined()
    expect(screen.getByText(/AKo/)).toBeDefined()
    expect(screen.getByText(/premium/i)).toBeDefined()
  })

  it('does not show starting hand chart when server provides no starting hand data', () => {
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.queryByTestId('starting-hands-chart')).toBeNull()
  })

  // -------------------------------------------------------------------------
  // Recommendation section
  // -------------------------------------------------------------------------

  it('shows recommendation from server data', () => {
    render(<AdvisorPanel {...defaultProps} />)
    expect(screen.getByText('Raise or Call')).toBeDefined()
  })

  it('shows "Consider folding" recommendation with red styling', () => {
    const data: AdvisorData = { ...defaultAdvisorData, recommendation: 'Consider folding' }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    const el = screen.getByText('Consider folding')
    expect(el.className).toContain('text-red-400')
  })

  it('shows "Consider calling" recommendation with yellow styling', () => {
    const data: AdvisorData = { ...defaultAdvisorData, recommendation: 'Consider calling' }
    render(<AdvisorPanel {...defaultProps} advisorData={data} />)
    const el = screen.getByText('Consider calling')
    expect(el.className).toContain('text-yellow-400')
  })
})
