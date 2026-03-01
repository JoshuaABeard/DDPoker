/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { TableFelt } from '../TableFelt'
import type { TableData } from '@/lib/game/types'

vi.mock('../CommunityCards', () => ({
  CommunityCards: ({ cards, fourColorDeck }: { cards: string[]; fourColorDeck?: boolean }) => (
    <div data-testid="community-cards" data-four-color={String(!!fourColorDeck)}>
      {cards.join(',')}
    </div>
  ),
}))

vi.mock('../PotDisplay', () => ({
  PotDisplay: ({ pots }: { pots: { amount: number; eligiblePlayers: number[] }[] }) => (
    <div data-testid="pot-display">{pots.map((p) => p.amount).join(',')}</div>
  ),
}))

const minimalTable: TableData = {
  tableId: 1,
  seats: [],
  communityCards: ['Ah'],
  pots: [{ amount: 100, eligiblePlayers: [1] }],
  currentRound: 'FLOP',
  handNumber: 1,
}

describe('TableFelt', () => {
  it('renders the community cards mock', () => {
    render(<TableFelt table={minimalTable} />)
    expect(screen.getByTestId('community-cards')).toBeTruthy()
  })

  it('renders the pot display mock', () => {
    render(<TableFelt table={minimalTable} />)
    expect(screen.getByTestId('pot-display')).toBeTruthy()
  })

  it('has role=region with aria-label "Poker table felt"', () => {
    render(<TableFelt table={minimalTable} />)
    expect(screen.getByRole('region', { name: 'Poker table felt' })).toBeTruthy()
  })

  it('applies custom colors to the background gradient', () => {
    const customColors = {
      center: '#ff0000',
      mid: '#00ff00',
      edge: '#0000ff',
      border: '#123456',
    }
    render(<TableFelt table={minimalTable} colors={customColors} />)
    const region = screen.getByRole('region', { name: 'Poker table felt' })
    const style = region.getAttribute('style') ?? ''
    // The radial-gradient preserves hex color strings as-is
    expect(style).toContain('#ff0000')
    expect(style).toContain('#00ff00')
    expect(style).toContain('#0000ff')
    // jsdom normalizes hex border color to rgb — rgb(18, 52, 86) is #123456
    expect(style).toContain('rgb(18, 52, 86)')
  })

  it('passes fourColorDeck=true through to CommunityCards', () => {
    render(<TableFelt table={minimalTable} fourColorDeck={true} />)
    const cards = screen.getByTestId('community-cards')
    expect(cards.getAttribute('data-four-color')).toBe('true')
  })

  it('passes fourColorDeck=false (default) through to CommunityCards', () => {
    render(<TableFelt table={minimalTable} />)
    const cards = screen.getByTestId('community-cards')
    expect(cards.getAttribute('data-four-color')).toBe('false')
  })
})
