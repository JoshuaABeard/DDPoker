/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { HandRankings } from '../HandRankings'

describe('HandRankings', () => {
  it('renders all 10 poker hand rankings', () => {
    render(<HandRankings onClose={() => {}} />)
    expect(screen.getByText('Royal Flush')).toBeDefined()
    expect(screen.getByText('Straight Flush')).toBeDefined()
    expect(screen.getByText('Four of a Kind')).toBeDefined()
    expect(screen.getByText('Full House')).toBeDefined()
    expect(screen.getByText('Flush')).toBeDefined()
    expect(screen.getByText('Straight')).toBeDefined()
    expect(screen.getByText('Three of a Kind')).toBeDefined()
    expect(screen.getByText('Two Pair')).toBeDefined()
    expect(screen.getByText('One Pair')).toBeDefined()
    expect(screen.getByText('High Card')).toBeDefined()
  })

  it('displays example cards for each ranking', () => {
    render(<HandRankings onClose={() => {}} />)
    const images = screen.getAllByRole('img')
    expect(images.length).toBeGreaterThanOrEqual(50) // 10 rankings × 5 cards
  })
})
