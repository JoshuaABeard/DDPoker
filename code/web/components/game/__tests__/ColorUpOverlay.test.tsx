/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { ColorUpOverlay } from '../ColorUpOverlay'
import type { ColorUpStartedData } from '@/lib/game/types'

const data: ColorUpStartedData = {
  players: [
    { playerId: 1, cards: ['Ah', 'Kd'], won: true, broke: false, finalChips: 5000 },
    { playerId: 2, cards: ['3c', '2s'], won: false, broke: false, finalChips: 3000 },
    { playerId: 3, cards: ['7h', '5d'], won: false, broke: true, finalChips: 0 },
  ],
  newMinChip: 100,
  tableId: 1,
}

const seatNames: Record<number, string> = { 1: 'Alice', 2: 'Bob', 3: 'Charlie' }

describe('ColorUpOverlay', () => {
  it('shows chip race title and new minimum chip', () => {
    render(<ColorUpOverlay data={data} seatNames={seatNames} />)
    expect(screen.getByText(/chip race/i)).toBeDefined()
    expect(screen.getByText(/100/)).toBeDefined()
  })

  it('shows player results', () => {
    render(<ColorUpOverlay data={data} seatNames={seatNames} />)
    expect(screen.getByText('Alice')).toBeDefined()
    expect(screen.getByText('Bob')).toBeDefined()
    expect(screen.getByText('Charlie')).toBeDefined()
  })

  it('marks winners and broke players', () => {
    render(<ColorUpOverlay data={data} seatNames={seatNames} />)
    expect(screen.getByText('Won')).toBeDefined()
    expect(screen.getByText('Broke')).toBeDefined()
  })
})
