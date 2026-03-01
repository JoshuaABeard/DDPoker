/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TournamentInfoBar } from '../TournamentInfoBar'
import type { BlindsData, BlindLevelConfig } from '@/lib/game/types'

const defaultBlinds: BlindsData = { small: 100, big: 200, ante: 0 }

const defaultProps = {
  level: 3,
  blinds: defaultBlinds,
  nextLevelIn: null,
  playerCount: 7,
  gameName: 'Saturday Night Game',
}

describe('TournamentInfoBar', () => {
  it('renders game name', () => {
    render(<TournamentInfoBar {...defaultProps} />)
    expect(screen.getByText('Saturday Night Game')).toBeTruthy()
  })

  it('renders level number', () => {
    render(<TournamentInfoBar {...defaultProps} />)
    expect(screen.getByText('3')).toBeTruthy()
  })

  it('renders small and big blinds formatted', () => {
    render(<TournamentInfoBar {...defaultProps} />)
    // formatChips uses Intl.NumberFormat so 100 -> "100", 200 -> "200"
    expect(screen.getByText(/100\/200/)).toBeTruthy()
  })

  it('shows ante when non-zero', () => {
    const blindsWithAnte: BlindsData = { small: 100, big: 200, ante: 25 }
    render(<TournamentInfoBar {...defaultProps} blinds={blindsWithAnte} />)
    expect(screen.getByText(/25/)).toBeTruthy()
  })

  it('does not show ante when zero', () => {
    render(<TournamentInfoBar {...defaultProps} />)
    // The ante section "(25)" should not appear when ante is 0
    expect(screen.queryByText(/\(0\)/)).toBeNull()
  })

  it('shows timer when nextLevelIn is a number', () => {
    render(<TournamentInfoBar {...defaultProps} nextLevelIn={90} />)
    // formatTime(90) = "1:30"
    expect(screen.getByText('1:30')).toBeTruthy()
  })

  it('hides timer when nextLevelIn is null', () => {
    render(<TournamentInfoBar {...defaultProps} nextLevelIn={null} />)
    expect(screen.queryByText(/Next/)).toBeNull()
  })

  it('formats timer as m:ss for values under a minute', () => {
    render(<TournamentInfoBar {...defaultProps} nextLevelIn={45} />)
    expect(screen.getByText('0:45')).toBeTruthy()
  })

  it('shows playerCount alone when totalPlayers is not provided', () => {
    render(<TournamentInfoBar {...defaultProps} playerCount={7} />)
    expect(screen.getByText('7')).toBeTruthy()
  })

  it('shows playerCount/totalPlayers when totalPlayers is provided', () => {
    render(<TournamentInfoBar {...defaultProps} playerCount={7} totalPlayers={100} />)
    expect(screen.getByText('7/100')).toBeTruthy()
  })

  it('shows player rank when provided', () => {
    render(<TournamentInfoBar {...defaultProps} playerRank={2} />)
    expect(screen.getByText('2')).toBeTruthy()
  })

  it('hides rank when playerRank is not provided', () => {
    render(<TournamentInfoBar {...defaultProps} />)
    expect(screen.queryByText(/Rank/)).toBeNull()
  })

  it('shows break hint when next level in blindStructure is a break', () => {
    const blindStructure: BlindLevelConfig[] = [
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
      { smallBlind: 100, bigBlind: 200, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
      { smallBlind: 0, bigBlind: 0, ante: 0, minutes: 10, isBreak: true, gameType: 'BREAK' },
    ]
    render(
      <TournamentInfoBar
        {...defaultProps}
        blindStructure={blindStructure}
        currentLevel={1}
      />,
    )
    expect(screen.getByText('Break in 1 level')).toBeTruthy()
  })

  it('shows break hint with plural "levels" when break is more than one level away', () => {
    const blindStructure: BlindLevelConfig[] = [
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
      { smallBlind: 100, bigBlind: 200, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
      { smallBlind: 150, bigBlind: 300, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
      { smallBlind: 0, bigBlind: 0, ante: 0, minutes: 10, isBreak: true, gameType: 'BREAK' },
    ]
    render(
      <TournamentInfoBar
        {...defaultProps}
        blindStructure={blindStructure}
        currentLevel={0}
      />,
    )
    expect(screen.getByText('Break in 3 levels')).toBeTruthy()
  })

  it('hides break hint when no break in blindStructure', () => {
    const blindStructure: BlindLevelConfig[] = [
      { smallBlind: 50, bigBlind: 100, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
      { smallBlind: 100, bigBlind: 200, ante: 0, minutes: 20, isBreak: false, gameType: 'NL' },
    ]
    render(
      <TournamentInfoBar
        {...defaultProps}
        blindStructure={blindStructure}
        currentLevel={0}
      />,
    )
    expect(screen.queryByText(/Break in/)).toBeNull()
  })

  it('hides break hint when blindStructure is not provided', () => {
    render(<TournamentInfoBar {...defaultProps} />)
    expect(screen.queryByText(/Break in/)).toBeNull()
  })
})
