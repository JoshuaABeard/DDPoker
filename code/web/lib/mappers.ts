/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Data Mappers
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

/**
 * Map backend OnlineGame to frontend AvailableGame format (mode 0)
 */
export function mapAvailableGame(game: any) {
  return {
    id: game.id,
    name: game.tournamentProfile?.name || 'Unknown',
    host: game.hostPlayer || 'Unknown',
    mode: game.tournamentProfile?.gameType || 'Unknown',
    buyIn: game.tournamentProfile?.buyIn || 0,
    players: game.numPlayers || 0,
    maxPlayers: game.tournamentProfile?.maxPlayers || 0,
    created: game.createDate,
  }
}

/**
 * Map backend OnlineGame to frontend CurrentGame format (mode 1)
 * Extracts player names from the players array
 */
export function mapCurrentGame(game: any) {
  const players = game.players?.map((p: any) => p.name || p.playerName) || []
  return {
    id: game.id,
    name: game.tournamentProfile?.name || 'Unknown',
    host: game.hostPlayer || 'Unknown',
    mode: game.tournamentProfile?.gameType || 'Unknown',
    players,
    blindLevel: game.currentBlindLevel || 0,
    started: game.startDate,
  }
}

/**
 * Map backend OnlineGame to frontend CompletedGame format (mode 2)
 * Finds the winner from the finish table
 */
export function mapCompletedGame(game: any) {
  const winner = game.winner || game.finishTable?.[0]?.playerName || 'Unknown'
  return {
    id: game.id,
    name: game.tournamentProfile?.name || 'Unknown',
    host: game.hostPlayer || 'Unknown',
    mode: game.tournamentProfile?.gameType || 'Unknown',
    winner,
    players: game.numPlayers || 0,
    buyIn: game.tournamentProfile?.buyIn || 0,
    completed: game.endDate,
  }
}

/**
 * Map backend LeaderboardSummary to frontend LeaderboardEntry
 * Calculates DDR1 or ROI based on mode
 */
export function mapLeaderboardEntry(backend: any, mode: 'ddr1' | 'roi') {
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
function calculateROI(entry: any): number {
  const totalBuyin = entry.totalBuyin || entry.totalBuyins || 0
  const totalAddon = entry.totalAddon || entry.totalAddons || 0
  const totalRebuys = entry.totalRebuys || entry.totalRebuy || 0
  const totalPrizes = entry.totalPrizes || entry.totalPrize || 0

  const totalInvested = totalBuyin + totalAddon + totalRebuys
  if (totalInvested === 0) return 0
  return ((totalPrizes - totalInvested) / totalInvested) * 100
}

/**
 * Calculate aggregate statistics from tournament history entries
 * NOTE: This calculates stats from the provided entries array.
 * For accurate full-history stats, ensure all entries are provided (not just paginated subset).
 */
export function calculateTournamentStats(entries: any[]) {
  if (entries.length === 0) {
    return {
      totalGames: 0,
      totalWins: 0,
      totalPrize: 0,
      avgPlacement: 0,
      winRate: 0,
    }
  }

  const totalWins = entries.filter((e) => (e.placement || e.place) === 1).length
  const totalPrize = entries.reduce((sum, e) => sum + (e.prize || e.prizeWon || 0), 0)
  const avgPlacement =
    entries.reduce((sum, e) => sum + (e.placement || e.place || 0), 0) / entries.length

  return {
    totalGames: entries.length,
    totalWins,
    totalPrize,
    avgPlacement,
    winRate: (totalWins / entries.length) * 100,
  }
}

/**
 * Map backend TournamentHistory to frontend format
 */
export function mapTournamentEntry(history: any) {
  return {
    id: history.gameId,
    gameName: history.gameName || 'Unknown',
    placement: history.place,
    totalPlayers: history.numPlayers || 0,
    buyIn: history.buyIn || 0,
    prize: history.prize || 0,
    date: history.endDate,
    mode: history.gameType || 'Unknown',
  }
}
