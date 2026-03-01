/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { calculateTournamentStats } from '../mappers'
import type { TournamentHistoryDto } from '../types'

const ENTRIES: TournamentHistoryDto[] = [
  { id: 1, name: 'Game 1', placement: 1, buyIn: 100, prize: 500, totalPlayers: 5 },
  { id: 2, name: 'Game 2', placement: 3, buyIn: 100, prize: 50, totalPlayers: 6 },
  { id: 3, name: 'Game 3', placement: 2, buyIn: 200, prize: 300, totalPlayers: 4 },
]

describe('calculateTournamentStats', () => {
  it('computes totalGames, totalWins, totalPrize, avgPlacement, winRate', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.totalGames).toBe(3)
    expect(stats.totalWins).toBe(1)
    expect(stats.totalPrize).toBe(850)
    expect(stats.avgPlacement).toBeCloseTo(2)
    expect(stats.winRate).toBeCloseTo(33.33, 0)
  })

  it('computes totalBuyIn as sum of all buy-ins', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.totalBuyIn).toBe(400)
  })

  it('computes profitLoss as totalPrize minus totalBuyIn', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.profitLoss).toBe(450)
  })

  it('computes bestFinish as lowest placement number', () => {
    const stats = calculateTournamentStats(ENTRIES)
    expect(stats.bestFinish).toBe(1)
  })

  it('returns zero defaults for empty array', () => {
    const stats = calculateTournamentStats([])
    expect(stats.totalGames).toBe(0)
    expect(stats.totalBuyIn).toBe(0)
    expect(stats.profitLoss).toBe(0)
    expect(stats.bestFinish).toBe(0)
  })
})
