/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ChipLeaderMini } from '../ChipLeaderMini'

describe('ChipLeaderMini', () => {
  it('renders nothing when no players', () => {
    const { container } = render(<ChipLeaderMini players={[]} myPlayerId={1} />)
    expect(container.textContent).toBe('')
  })

  it('shows top 3 players sorted by chips', () => {
    const players = [
      { playerId: 1, name: 'Alice', chipCount: 500 },
      { playerId: 2, name: 'Bob', chipCount: 1500 },
      { playerId: 3, name: 'Charlie', chipCount: 1000 },
      { playerId: 4, name: 'Dave', chipCount: 200 },
    ]
    render(<ChipLeaderMini players={players} myPlayerId={4} />)
    expect(screen.getByText('Bob')).toBeTruthy()
    expect(screen.getByText('Charlie')).toBeTruthy()
    expect(screen.getByText('Alice')).toBeTruthy()
  })

  it('shows my position when not in top 3', () => {
    const players = [
      { playerId: 1, name: 'Alice', chipCount: 5000 },
      { playerId: 2, name: 'Bob', chipCount: 4000 },
      { playerId: 3, name: 'Charlie', chipCount: 3000 },
      { playerId: 4, name: 'Dave', chipCount: 2000 },
      { playerId: 5, name: 'Eve', chipCount: 1000 },
      { playerId: 6, name: 'Me', chipCount: 100 },
    ]
    render(<ChipLeaderMini players={players} myPlayerId={6} />)
    expect(screen.getByText(/6th/)).toBeTruthy()
  })

  it('shows all players when 5 or fewer', () => {
    const players = [
      { playerId: 1, name: 'Alice', chipCount: 500 },
      { playerId: 2, name: 'Bob', chipCount: 300 },
    ]
    render(<ChipLeaderMini players={players} myPlayerId={1} />)
    expect(screen.getByText('Alice')).toBeTruthy()
    expect(screen.getByText('Bob')).toBeTruthy()
  })
})
