/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - API Client
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { config, getApiUrl, GAME_SERVER_URL } from './config'
import type {
  ApiError,
  ApiResponse,
  BanDto,
  HostSummary,
  LeaderboardEntry,
  LeaderboardEntryDto,
  LoginRequest,
  LoginResponse,
  OnlineProfileDto,
  PlayerSearchDto,
  ProfileResponse,
  PublicProfileResponse,
  RegisterRequest,
  TemplateDto,
  TournamentHistoryDto,
  TournamentHistoryEntry,
  TournamentStatsDto,
} from './types'
import type {
  GameConfigDto,
  GameJoinResponseDto,
  GameSummaryDto,
  PracticeGameResponseDto,
  WsTokenResponseDto,
} from './game/types'
import type { EquityResult } from './poker/types'

/**
 * Base fetch wrapper with timeout and error handling.
 * Throws on non-2xx responses.
 */
async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = getApiUrl(endpoint)

  const defaultHeaders: HeadersInit = {}
  if (options.body) {
    defaultHeaders['Content-Type'] = 'application/json'
  }

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), config.apiTimeout)

  const signals: AbortSignal[] = [controller.signal]
  if (options.signal) signals.push(options.signal)
  const composedSignal = signals.length > 1 ? AbortSignal.any(signals) : controller.signal

  const fetchOptions: RequestInit = {
    credentials: 'include', // Always send cookies (JWT in HttpOnly cookie)
    ...options,
    signal: composedSignal,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }

  try {
    const response = await fetch(url, fetchOptions)

    if (!response.ok) {
      const errorData: ApiError = await response.json().catch(() => ({
        message: `HTTP error ${response.status}: ${response.statusText}`,
      }))
      throw new Error(errorData.message || `HTTP error ${response.status}`)
    }

    const data = await response.json()
    return {
      data,
      timestamp: new Date().toISOString(),
    }
  } catch (error) {
    console.error('API fetch error:', error)
    throw error
  } finally {
    clearTimeout(timeoutId)
  }
}

/**
 * Like apiFetch but returns the raw Response rather than parsing JSON and
 * throwing on non-2xx. Used for endpoints that return meaningful bodies on
 * 400/429 (e.g., auth endpoints that callers inspect directly).
 * Applies the same timeout and credentials as apiFetch.
 */
async function apiFetchRaw(
  path: string,
  options: RequestInit = {}
): Promise<Response> {
  const url = `${GAME_SERVER_URL}${path}`

  const defaultHeaders: HeadersInit = {}
  if (options.body) {
    defaultHeaders['Content-Type'] = 'application/json'
  }

  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), config.apiTimeout)

  const signals: AbortSignal[] = [controller.signal]
  if (options.signal) signals.push(options.signal)
  const composedSignal = signals.length > 1 ? AbortSignal.any(signals) : controller.signal

  const fetchOptions: RequestInit = {
    credentials: 'include',
    ...options,
    signal: composedSignal,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }

  try {
    return await fetch(url, fetchOptions)
  } finally {
    clearTimeout(timeoutId)
  }
}

/**
 * Authentication API — all calls go to the game server at GAME_SERVER_URL.
 */
export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiFetch<LoginResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    })
    return response.data
  },

  register: async (userData: RegisterRequest): Promise<LoginResponse> => {
    const response = await apiFetch<LoginResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    })
    return response.data
  },

  logout: async (): Promise<void> => {
    await apiFetch<void>('/api/v1/auth/logout', { method: 'POST' })
  },

  getCurrentUser: async (): Promise<ProfileResponse | null> => {
    try {
      const response = await apiFetch<ProfileResponse>('/api/v1/auth/me')
      return response.data
    } catch {
      return null
    }
  },

  changePassword: (oldPassword: string, newPassword: string): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/password', {
      method: 'PUT',
      body: JSON.stringify({ oldPassword, newPassword }),
    }),

  changeEmail: (email: string): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/email', {
      method: 'PUT',
      body: JSON.stringify({ email }),
    }),

  forgotPassword: async (email: string): Promise<{ success: boolean; message: string }> => {
    const response = await apiFetch<{ success: boolean; message: string }>('/api/v1/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    })
    return response.data
  },

  resetPassword: (token: string, newPassword: string): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword }),
    }),

  verifyEmail: (token: string): Promise<Response> =>
    apiFetchRaw(`/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`),

  resendVerification: (): Promise<Response> =>
    apiFetchRaw('/api/v1/auth/resend-verification', { method: 'POST' }),

  checkUsername: (username: string): Promise<Response> =>
    apiFetchRaw(`/api/v1/auth/check-username?username=${encodeURIComponent(username)}`),

  getWsToken: async (): Promise<WsTokenResponseDto> => {
    const response = await apiFetch<WsTokenResponseDto>('/api/v1/auth/ws-token')
    return response.data
  },
}

/**
 * Profile API
 */
export const profileApi = {
  getProfile: async (id: number): Promise<PublicProfileResponse> => {
    const response = await apiFetch<PublicProfileResponse>(`/api/v1/profiles/${id}`)
    return response.data
  },

  getProfileByName: async (name: string): Promise<PublicProfileResponse> => {
    const response = await apiFetch<PublicProfileResponse>(`/api/v1/profiles/name/${name}`)
    return response.data
  },

  getAliases: async (): Promise<PublicProfileResponse[]> => {
    const response = await apiFetch<PublicProfileResponse[]>('/api/v1/profiles/aliases')
    return response.data
  },

  retireProfile: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/profiles/${id}/retire`, { method: 'POST' })
  },
}

/**
 * Game Server API — calls /api/v1/* endpoints on the pokergameserver.
 */
export const gameServerApi = {
  /**
   * List games with optional status filter.
   * @param status Filter: 'WAITING_FOR_PLAYERS' | 'IN_PROGRESS' | 'COMPLETED' (omit for all)
   */
  listGames: async (params?: {
    status?: string
    search?: string
    page?: number
    pageSize?: number
  }): Promise<{ games: GameSummaryDto[]; total: number }> => {
    const qs = new URLSearchParams()
    if (params?.status) qs.append('status', params.status)
    if (params?.search) qs.append('search', params.search)
    if (params?.page !== undefined) qs.append('page', params.page.toString())
    if (params?.pageSize) qs.append('pageSize', params.pageSize.toString())
    const response = await apiFetch<{ games: GameSummaryDto[]; total: number }>(
      `/api/v1/games${qs.toString() ? `?${qs}` : ''}`
    )
    return response.data
  },

  /**
   * Get a single game summary by ID.
   */
  getGame: async (gameId: string): Promise<GameSummaryDto> => {
    const response = await apiFetch<GameSummaryDto>(`/api/v1/games/${gameId}`)
    return response.data
  },

  /**
   * Create a new game.
   * Returns the created game summary (including gameId and wsUrl).
   */
  createGame: async (config: GameConfigDto): Promise<GameSummaryDto> => {
    const response = await apiFetch<GameSummaryDto>('/api/v1/games', {
      method: 'POST',
      body: JSON.stringify(config),
    })
    return response.data
  },

  /**
   * Create a practice game (pre-filled with AI, auto-starts).
   * Returns { gameId } — no separate join/start needed.
   */
  createPracticeGame: async (config?: Partial<GameConfigDto>): Promise<PracticeGameResponseDto> => {
    const response = await apiFetch<PracticeGameResponseDto>('/api/v1/games/practice', {
      method: 'POST',
      body: JSON.stringify(config ?? {}),
    })
    return response.data
  },

  /**
   * Join a game. Returns the WebSocket URL and gameId.
   * @param password Required for private games. Sent in POST body only (never in URL).
   */
  joinGame: async (gameId: string, password?: string): Promise<GameJoinResponseDto> => {
    const response = await apiFetch<GameJoinResponseDto>(`/api/v1/games/${gameId}/join`, {
      method: 'POST',
      body: JSON.stringify(password !== undefined ? { password } : {}),
    })
    return response.data
  },

  /**
   * Start a game (owner only).
   */
  startGame: async (gameId: string): Promise<void> => {
    await apiFetch<void>(`/api/v1/games/${gameId}/start`, { method: 'POST' })
  },

  /**
   * Update game settings (owner only, lobby phase only).
   */
  updateSettings: async (gameId: string, settings: Partial<GameConfigDto>): Promise<GameSummaryDto> => {
    const response = await apiFetch<GameSummaryDto>(`/api/v1/games/${gameId}/settings`, {
      method: 'PUT',
      body: JSON.stringify(settings),
    })
    return response.data
  },

  /**
   * Kick a player (owner only).
   */
  kickPlayer: async (gameId: string, profileId: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/games/${gameId}/kick`, {
      method: 'POST',
      body: JSON.stringify({ profileId }),
    })
  },

  /**
   * Cancel a game (owner only).
   */
  cancelGame: async (gameId: string): Promise<void> => {
    await apiFetch<void>(`/api/v1/games/${gameId}`, { method: 'DELETE' })
  },

  /**
   * Join a game as observer (spectator). Returns WebSocket URL and token.
   */
  observeGame: async (gameId: string): Promise<GameJoinResponseDto> => {
    const response = await apiFetch<GameJoinResponseDto>(`/api/v1/games/${gameId}/observe`, {
      method: 'POST',
    })
    return response.data
  },

  /**
   * Run a Monte Carlo equity simulation.
   */
  simulate: async (params: {
    holeCards: string[]
    communityCards: string[]
    numOpponents: number
    iterations: number
    knownOpponentHands?: string[][]
  }): Promise<EquityResult> => {
    const response = await apiFetch<EquityResult>('/api/v1/poker/simulate', {
      method: 'POST',
      body: JSON.stringify(params),
    })
    return response.data
  },
}

/**
 * Leaderboard API
 */
export const leaderboardApi = {
  /**
   * Get the leaderboard
   */
  getLeaderboard: async (
    mode: 'ddr1' | 'roi',
    page = 0,
    pageSize = 50,
    filters?: {
      name?: string
      from?: string
      to?: string
      games?: number
    }
  ): Promise<{ entries: LeaderboardEntryDto[]; total: number }> => {
    const params = new URLSearchParams({
      mode,
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    if (filters?.name) params.append('name', filters.name)
    if (filters?.from) params.append('from', filters.from)
    if (filters?.to) params.append('to', filters.to)
    if (filters?.games) params.append('gamesLimit', filters.games.toString())
    const response = await apiFetch<{ entries: LeaderboardEntryDto[]; total: number }>(
      `/api/v1/leaderboard?${params}`
    )
    return response.data
  },

  /**
   * Get a player's leaderboard rank
   */
  getPlayerRank: async (playerName: string): Promise<LeaderboardEntry> => {
    const response = await apiFetch<LeaderboardEntry>(`/api/v1/leaderboard/player/${playerName}`)
    return response.data
  },
}

/**
 * Tournament History API
 */
export const tournamentApi = {
  /**
   * Get tournament history for a player
   */
  getHistory: async (
    playerName: string,
    page = 0,
    pageSize = 50,
    from?: string,
    to?: string
  ): Promise<{ content: TournamentHistoryDto[]; totalElements: number; totalPages: number; stats: TournamentStatsDto }> => {
    const params = new URLSearchParams({
      name: playerName,
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    if (from) params.append('from', from)
    if (to) params.append('to', to)
    const response = await apiFetch<{ content: TournamentHistoryDto[]; totalElements: number; totalPages: number; stats: TournamentStatsDto }>(
      `/api/v1/history?${params}`
    )
    return response.data
  },

  /**
   * Get tournament details
   */
  getDetails: async (tournamentId: number): Promise<TournamentHistoryEntry> => {
    const response = await apiFetch<TournamentHistoryEntry>(`/api/v1/tournaments/${tournamentId}`)
    return response.data
  },
}

/**
 * Tournament Template API
 */
export const templateApi = {
  list: async (): Promise<TemplateDto[]> => {
    const response = await apiFetch<TemplateDto[]>('/api/v1/profiles/templates')
    return response.data
  },

  create: async (name: string, config: object): Promise<TemplateDto> => {
    const response = await apiFetch<TemplateDto>('/api/v1/profiles/templates', {
      method: 'POST',
      body: JSON.stringify({ name, config: JSON.stringify(config) }),
    })
    return response.data
  },

  update: async (id: number, name: string, config: object): Promise<TemplateDto> => {
    const response = await apiFetch<TemplateDto>(`/api/v1/profiles/templates/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ name, config: JSON.stringify(config) }),
    })
    return response.data
  },

  delete: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/profiles/templates/${id}`, { method: 'DELETE' })
  },
}

/**
 * Search API
 */
export const searchApi = {
  /**
   * Search for players by name
   */
  searchPlayers: async (name: string, page = 0, pageSize = 50): Promise<PlayerSearchDto[]> => {
    const params = new URLSearchParams({
      name,
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    const response = await apiFetch<PlayerSearchDto[]>(`/api/v1/search?${params}`)
    return response.data
  },
}

/**
 * Host API
 */
export const hostApi = {
  /**
   * Get host statistics
   */
  getHosts: async (
    search?: string,
    from?: string,
    to?: string,
    page = 0,
    pageSize = 50
  ): Promise<{ hosts: HostSummary[]; total: number }> => {
    const params = new URLSearchParams({ page: page.toString(), pageSize: pageSize.toString() })
    if (search) params.append('search', search)
    if (from) params.append('from', from)
    if (to) params.append('to', to)
    const response = await apiFetch<{ hosts: HostSummary[]; total: number }>(
      `/api/v1/games/hosts?${params}`
    )
    return response.data
  },
}

/**
 * Admin API
 */
export const adminApi = {
  /**
   * Search online profiles
   */
  searchProfiles: async (filters?: {
    name?: string
    email?: string
    page?: number
    pageSize?: number
  }): Promise<{ profiles: OnlineProfileDto[]; total: number }> => {
    const params = new URLSearchParams()
    if (filters?.name) params.append('name', filters.name)
    if (filters?.email) params.append('email', filters.email)
    if (filters?.page !== undefined) params.append('page', filters.page.toString())
    if (filters?.pageSize) params.append('pageSize', filters.pageSize.toString())

    const response = await apiFetch<{ profiles: OnlineProfileDto[]; total: number }>(
      `/api/v1/admin/profiles?${params}`
    )
    return response.data
  },

  getBans: async (): Promise<{ bans: BanDto[]; total: number }> => {
    const response = await apiFetch<BanDto[]>('/api/v1/admin/bans')
    return { bans: response.data, total: response.data.length }
  },

  addBan: async (banData: {
    banType: 'PROFILE' | 'EMAIL'
    profileId?: number
    email?: string
    reason?: string
    until?: string
  }): Promise<BanDto> => {
    const response = await apiFetch<BanDto>('/api/v1/admin/bans', {
      method: 'POST',
      body: JSON.stringify(banData),
    })
    return response.data
  },

  removeBan: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/admin/bans/${id}`, { method: 'DELETE' })
  },

  /**
   * Manually mark a profile as email-verified and clear any pending token.
   */
  verifyProfile: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/admin/profiles/${id}/verify`, { method: 'POST' })
  },

  /**
   * Clear account lockout for a profile (reset failed attempts and lockout fields).
   */
  unlockProfile: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/admin/profiles/${id}/unlock`, { method: 'POST' })
  },

  /**
   * Generate a new verification token and send the verification email.
   * Returns a rejected promise if the profile is already verified (400).
   */
  resendVerification: async (id: number): Promise<void> => {
    await apiFetch<void>(`/api/v1/admin/profiles/${id}/resend-verification`, { method: 'POST' })
  },
}

/**
 * Utility function to check if the API is reachable
 */
export async function checkApiHealth(): Promise<boolean> {
  try {
    await apiFetch<{ status: string }>('/api/v1/health')
    return true
  } catch {
    return false
  }
}
