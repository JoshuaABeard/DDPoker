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
 * Profile returned by /auth/me (own profile, includes private fields)
 */
export interface ProfileResponse {
  id: number
  username: string
  email: string
  emailVerified: boolean
  admin: boolean
  retired: boolean
  createDate: string
}

/**
 * Response from login/register endpoints
 */
export interface LoginResponse {
  success: boolean
  profile: ProfileResponse | null
  token: string | null
  message: string | null
  retryAfterSeconds: number | null
}

/**
 * Public profile (no email, no admin flag)
 */
export interface PublicProfileResponse {
  id: number
  name: string
  createDate: string
}

/**
 * Tournament statistics summary
 */
export interface TournamentStatsDto {
  totalGames: number
  totalWins: number
  totalPrize: number
  totalBuyIn: number
  profitLoss: number
  bestFinish: number
  avgPlacement: number
  winRate: number
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
 * Backend DTO Interfaces - Match actual API responses from Spring Boot backend
 */

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

export interface TemplateDto {
  id: number
  name: string
  config: string
  createdDate: string
  modifiedDate: string
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
  emailVerified?: boolean
  lockedUntil?: number | null
}

export interface BanDto {
  id: number
  banType: 'PROFILE' | 'EMAIL'
  profileId: number | null
  email: string | null
  reason: string | null
  until: string | null
  createdAt: string
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

