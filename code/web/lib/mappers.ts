/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Data Mappers
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type {
  LeaderboardEntryDto,
  TournamentHistoryDto,
} from './types'

/**
 * Map backend LeaderboardSummary to frontend LeaderboardEntry
 * Calculates DDR1 or ROI based on mode
 */
export function mapLeaderboardEntry(backend: LeaderboardEntryDto, mode: 'ddr1' | 'roi') {
  const score = mode === 'ddr1' ? (backend.ddr1 || 0) : calculateROI(backend)
  return {
    rank: backend.rank || 0,
    playerName: backend.playerName || 'Unknown',
    score,
    gamesPlayed: backend.gamesPlayed || 0,
    wins: backend.wins || 0,
    winRate: backend.gamesPlayed > 0 ? ((backend.wins || 0) / backend.gamesPlayed) * 100 : 0,
  }
}

/**
 * Calculate ROI percentage from backend stats
 */
function calculateROI(entry: LeaderboardEntryDto): number {
  const totalBuyin = entry.totalBuyin || 0
  const totalAddon = entry.totalAddon || 0
  const totalRebuys = entry.totalRebuys || 0
  const totalPrizes = entry.totalPrizes || 0

  const totalInvested = totalBuyin + totalAddon + totalRebuys
  if (totalInvested === 0) return 0
  return ((totalPrizes - totalInvested) / totalInvested) * 100
}

/**
 * Map backend TournamentHistory to frontend format
 */
export function mapTournamentEntry(history: TournamentHistoryDto) {
  return {
    id: history.id,
    gameName: history.name || 'Unknown',
    placement: history.placement || history.place || 0,
    totalPlayers: history.totalPlayers || 0,
    buyIn: history.buyIn || 0,
    prize: history.prize || 0,
    date: history.date || history.endDate || new Date().toISOString(),
    mode: history.gameType || 'Unknown',
  }
}
