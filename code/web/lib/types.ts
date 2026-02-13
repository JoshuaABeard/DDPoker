/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - TypeScript Types
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

/**
 * Base API response wrapper
 */
export interface ApiResponse<T> {
  data: T
  error?: string
  timestamp?: string
}

/**
 * Player profile information
 */
export interface PlayerProfile {
  id: number
  name: string
  email?: string
  createdAt: string
  lastLogin?: string
  isActive: boolean
  stats?: PlayerStats
}

/**
 * Player statistics
 */
export interface PlayerStats {
  gamesPlayed: number
  gamesWon: number
  totalChips: number
  biggestWin?: number
  tournamentPoints?: number
}

/**
 * Game/Tournament information
 */
export interface Game {
  id: number
  name: string
  host: string
  status: 'waiting' | 'in_progress' | 'completed'
  gameType: 'no_limit' | 'pot_limit' | 'limit'
  buyIn: number
  currentPlayers: number
  maxPlayers: number
  startTime?: string
  endTime?: string
  blindLevel?: number
}

/**
 * Game details with player list
 */
export interface GameDetails extends Game {
  players: GamePlayer[]
  blindStructure?: BlindLevel[]
  chatEnabled: boolean
}

/**
 * Player in a game
 */
export interface GamePlayer {
  playerName: string
  chips: number
  position: number
  status: 'active' | 'sitting_out' | 'eliminated'
}

/**
 * Blind level structure
 */
export interface BlindLevel {
  level: number
  smallBlind: number
  bigBlind: number
  ante?: number
  duration: number
}

/**
 * Leaderboard entry
 */
export interface LeaderboardEntry {
  rank: number
  playerName: string
  points: number
  gamesPlayed: number
  wins: number
  winRate: number
}

/**
 * Tournament history entry
 */
export interface TournamentHistoryEntry {
  id: number
  gameName: string
  placement: number
  totalPlayers: number
  prizeWon: number
  date: string
}

/**
 * Authentication types (for Phase 3)
 */
export interface LoginRequest {
  username: string
  password: string
  rememberMe: boolean
}

export interface AuthResponse {
  success: boolean
  message: string
  username?: string
  admin?: boolean
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

/**
 * Pagination types
 */
export interface PaginatedResponse<T> {
  data: T[]
  page: number
  pageSize: number
  totalItems: number
  totalPages: number
}

/**
 * Error response
 */
export interface ApiError {
  message: string
  code?: string
  details?: Record<string, unknown>
}

/**
 * Host summary for game hosts
 */
export interface HostSummary {
  playerName: string
  gamesHosted: number
  lastHosted?: string
}

/**
 * Player alias information
 */
export interface ProfileAlias {
  name: string
  createdDate: string
  retiredDate?: string
}

/**
 * Tournament statistics
 */
export interface TournamentStats {
  totalGames: number
  totalWins: number
  totalPrize: number
  avgPlacement: number
  winRate: number
}

/**
 * Backend DTO Interfaces - Match actual API responses from Spring Boot backend
 */

export interface TournamentProfileDto {
  name: string
  gameType: string
  buyIn: number
  maxPlayers: number
}

export interface GamePlayerDto {
  name?: string
  playerName?: string
}

export interface OnlineGameDto {
  id: number
  tournamentProfile?: TournamentProfileDto
  hostPlayer?: string
  numPlayers?: number
  createDate?: string
  startDate?: string
  endDate?: string
  currentBlindLevel?: number
  players?: GamePlayerDto[]
  winner?: string
  finishTable?: Array<{ playerName?: string }>
}

export interface LeaderboardEntryDto {
  rank: number
  playerName: string
  ddr1?: number
  gamesPlayed: number
  wins?: number
  totalBuyin?: number
  totalAddon?: number
  totalRebuys?: number
  totalPrizes?: number
}

export interface TournamentHistoryDto {
  id: number
  name: string
  placement: number
  place?: number
  totalPlayers?: number
  buyIn?: number
  prize?: number
  date?: string
  endDate?: string
  gameType?: string
}

export interface OnlineProfileDto {
  id: number
  name?: string
  playerName?: string
  email?: string
  createdAt?: string
  createDate?: string
  lastLogin?: string
  isActive?: boolean
}

export interface BannedKeyDto {
  id: number
  key: string
  comment?: string
  createDate: string
  until?: string
}

export interface HostSummaryDto {
  name?: string
  playerName?: string
  lastHosted?: string
  lastSeen?: string
  totalGamesHosted: number
}

export interface PlayerSearchDto {
  name?: string
  playerName?: string
  gamesPlayed?: number
  lastPlayed?: string
  lastSeen?: string
  rank?: number
}

export interface ProfileAliasDto {
  name: string
  createDate: string
  retireDate?: string
}

/**
 * Game list response from backend
 */
export interface GameListResponse {
  games: OnlineGameDto[]
  total: number
}
