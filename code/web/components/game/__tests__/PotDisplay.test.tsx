/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { PotDisplay } from '../PotDisplay'
import type { PotData, SeatData } from '@/lib/game/types'

const seats: SeatData[] = [
  { seatIndex: 0, playerId: 1, playerName: 'Alice', chipCount: 5000, status: 'ACTIVE', isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
  { seatIndex: 1, playerId: 2, playerName: 'Bob', chipCount: 3000, status: 'ALL_IN', isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
  { seatIndex: 2, playerId: 3, playerName: 'Charlie', chipCount: 8000, status: 'ACTIVE', isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
]

describe('PotDisplay', () => {
  it('shows nothing when no pots', () => {
    const { container } = render(<PotDisplay pots={[]} />)
    expect(container.innerHTML).toBe('')
  })

  it('shows main pot with player count', () => {
    const pots: PotData[] = [{ amount: 1500, eligiblePlayers: [1, 2, 3] }]
    render(<PotDisplay pots={pots} seats={seats} />)
    expect(screen.getByText(/1,500/)).toBeDefined()
    expect(screen.getByText(/3 players/)).toBeDefined()
  })

  it('shows side pots with player count', () => {
    const pots: PotData[] = [
      { amount: 1500, eligiblePlayers: [1, 2, 3] },
      { amount: 800, eligiblePlayers: [1, 3] },
    ]
    render(<PotDisplay pots={pots} seats={seats} />)
    expect(screen.getByText(/Side Pot 1/)).toBeDefined()
    expect(screen.getByText(/2 players/)).toBeDefined()
  })

  it('works without seats prop', () => {
    const pots: PotData[] = [{ amount: 1000, eligiblePlayers: [1, 2] }]
    render(<PotDisplay pots={pots} />)
    expect(screen.getByText(/1,000/)).toBeDefined()
  })
})
