/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { GameInfoPanel } from '../GameInfoPanel'

const defaultProps = {
  gameName: 'Test Game',
  ownerName: 'alice',
  blinds: { small: 25, big: 50, ante: 0 },
}

describe('GameInfoPanel', () => {
  it('renders game name and owner', () => {
    render(<GameInfoPanel {...defaultProps} />)
    expect(screen.getByText('Test Game')).toBeTruthy()
    expect(screen.getByText(/alice/)).toBeTruthy()
  })

  it('shows current blinds', () => {
    render(<GameInfoPanel {...defaultProps} />)
    expect(screen.getByText(/25/)).toBeTruthy()
    expect(screen.getByText(/50/)).toBeTruthy()
  })

  it('accepts BlindsSummary format (smallBlind/bigBlind)', () => {
    render(
      <GameInfoPanel
        {...defaultProps}
        blinds={{ smallBlind: 100, bigBlind: 200, ante: 25 }}
      />,
    )
    expect(screen.getByText(/100/)).toBeTruthy()
    expect(screen.getByText(/200/)).toBeTruthy()
  })

  it('shows ante when ante > 0', () => {
    render(
      <GameInfoPanel {...defaultProps} blinds={{ small: 50, big: 100, ante: 10 }} />,
    )
    expect(screen.getByText(/ante/i)).toBeTruthy()
  })

  it('does not show ante section when ante is 0', () => {
    render(<GameInfoPanel {...defaultProps} />)
    expect(screen.queryByText(/ante/i)).toBeNull()
  })

  it('shows close button when onClose provided', () => {
    const onClose = vi.fn()
    render(<GameInfoPanel {...defaultProps} onClose={onClose} />)
    expect(screen.getByRole('button', { name: /close/i })).toBeTruthy()
  })

  it('calls onClose when close button clicked', async () => {
    const onClose = vi.fn()
    render(<GameInfoPanel {...defaultProps} onClose={onClose} />)
    await userEvent.click(screen.getByRole('button', { name: /close/i }))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('does not show close button when onClose not provided', () => {
    render(<GameInfoPanel {...defaultProps} />)
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('shows greeting when provided', () => {
    render(<GameInfoPanel {...defaultProps} greeting="Welcome to the table!" />)
    expect(screen.getByText('Welcome to the table!')).toBeTruthy()
  })

  it('does not show greeting section when not provided', () => {
    render(<GameInfoPanel {...defaultProps} />)
    expect(screen.queryByText('Welcome')).toBeNull()
  })

  it('shows level indicator when currentLevel provided', () => {
    render(<GameInfoPanel {...defaultProps} currentLevel={3} />)
    expect(screen.getByText(/Level 3/)).toBeTruthy()
  })

  it('shows blind structure when provided', () => {
    const structure = [
      { smallBlind: 25, bigBlind: 50, ante: 0, minutes: 15 },
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 15 },
    ]
    render(<GameInfoPanel {...defaultProps} blindStructure={structure} />)
    expect(screen.getByText(/Blind Structure/i)).toBeTruthy()
  })

  it('shows players list when provided', () => {
    const players = [
      { playerId: 1, name: 'alice', chipCount: 1000 },
      { playerId: 2, name: 'bob', chipCount: 500 },
    ]
    render(<GameInfoPanel {...defaultProps} players={players} />)
    expect(screen.getByText('alice')).toBeTruthy()
    expect(screen.getByText('bob')).toBeTruthy()
  })

  it('sorts players by chip count descending', () => {
    const players = [
      { playerId: 1, name: 'xAlice', chipCount: 500 },
      { playerId: 2, name: 'xBob', chipCount: 1000 },
    ]
    render(<GameInfoPanel {...defaultProps} players={players} />)
    // xBob has more chips and should appear first in the players list
    const playerList = document.querySelector('.space-y-0\\.5 ~ .space-y-0\\.5') ?? document.querySelector('[class*="space-y"]')
    const playerNames = screen.getAllByText(/xBob|xAlice/)
    expect(playerNames[0].textContent).toBe('xBob')
    expect(playerNames[1].textContent).toBe('xAlice')
  })

  it('shows rebuy and addon info when provided', () => {
    render(
      <GameInfoPanel
        {...defaultProps}
        rebuyInfo="3 allowed, 1000 chips"
        addonInfo="1 allowed, 500 chips"
      />,
    )
    expect(screen.getByText(/Rebuys:/)).toBeTruthy()
    expect(screen.getByText(/Add-ons:/)).toBeTruthy()
  })

  it('shows minChip when minChip > 0', () => {
    render(<GameInfoPanel {...defaultProps} minChip={25} />)
    expect(screen.getByText(/Min chip/i)).toBeTruthy()
  })

  it('does not show minChip when minChip is 0', () => {
    render(<GameInfoPanel {...defaultProps} minChip={0} />)
    expect(screen.queryByText(/Min chip/i)).toBeNull()
  })
})
