/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - API Client
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { config, getApiUrl } from './config'
import type {
  ApiError,
  ApiResponse,
  AuthResponse,
  BannedKeyDto,
  Game,
  GameDetails,
  GameListResponse,
  HostSummary,
  LeaderboardEntry,
  LeaderboardEntryDto,
  LoginRequest,
  OnlineProfileDto,
  PaginatedResponse,
  PlayerProfile,
  PlayerSearchDto,
  ProfileAlias,
  RegisterRequest,
  TournamentHistoryDto,
  TournamentHistoryEntry,
} from './types'

/**
 * Base fetch wrapper with error handling
 */
async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = getApiUrl(endpoint)

  const defaultHeaders: HeadersInit = {
    'Content-Type': 'application/json',
  }

  // Add timeout for build-time API calls (5 seconds)
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), 5000)

  const fetchOptions: RequestInit = {
    credentials: 'include', // Always send cookies (JWT in HttpOnly cookie)
    ...options,
    signal: controller.signal,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  }

  try {
    const response = await fetch(url, fetchOptions)
    clearTimeout(timeoutId)

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
    clearTimeout(timeoutId)
    console.error('API fetch error:', error)
    throw error
  }
}

/**
 * Authentication API
 */
export const authApi = {
  /**
   * Log in a user
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await apiFetch<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    })
    return response.data
  },

  /**
   * Register a new user
   */
  register: async (userData: RegisterRequest): Promise<PlayerProfile> => {
    const response = await apiFetch<PlayerProfile>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    })
    return response.data
  },

  /**
   * Log out the current user
   */
  logout: async (): Promise<void> => {
    await apiFetch<void>('/api/auth/logout', {
      method: 'POST',
    })
  },

  /**
   * Get the current user's profile
   */
  getCurrentUser: async (): Promise<AuthResponse | null> => {
    try {
      const response = await apiFetch<AuthResponse>('/api/auth/me')
      return response.data
    } catch (error) {
      console.error('Failed to get current user:', error)
      return null
    }
  },

  /**
   * Request password reset - sends password to registered email
   */
  forgotPassword: async (username: string): Promise<{ success: boolean; message: string }> => {
    const response = await apiFetch<{ success: boolean; message: string }>('/api/profile/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ username }),
    })
    return response.data
  },
}

/**
 * Player API
 */
export const playerApi = {
  /**
   * Get a player profile by ID
   */
  getProfile: async (playerId: number): Promise<PlayerProfile> => {
    const response = await apiFetch<PlayerProfile>(`/api/players/${playerId}`)
    return response.data
  },

  /**
   * Get a player profile by name
   */
  getProfileByName: async (playerName: string): Promise<PlayerProfile> => {
    const response = await apiFetch<PlayerProfile>(`/api/players/name/${playerName}`)
    return response.data
  },

  /**
   * Update the current user's profile
   */
  updateProfile: async (updates: Partial<PlayerProfile>): Promise<PlayerProfile> => {
    const response = await apiFetch<PlayerProfile>('/api/players/me', {
      method: 'PUT',
      body: JSON.stringify(updates),
    })
    return response.data
  },

  /**
   * Change password
   */
  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await apiFetch<void>('/api/players/me/password', {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword }),
    })
  },
}

/**
 * Profile API
 */
export const profileApi = {
  /**
   * Get player aliases
   */
  getAliases: async (): Promise<ProfileAlias[]> => {
    const response = await apiFetch<ProfileAlias[]>('/api/profile/aliases')
    return response.data
  },
}

/**
 * Games API
 */
export const gamesApi = {
  /**
   * Get available games (mode 0 - waiting for players)
   */
  getAvailable: async (page = 0, pageSize = 20): Promise<GameListResponse> => {
    const params = new URLSearchParams({
      modes: '0',
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    const response = await apiFetch<GameListResponse>(`/api/games?${params}`)
    return response.data
  },

  /**
   * Get running games (mode 1 - in progress)
   */
  getRunning: async (page = 0, pageSize = 20): Promise<GameListResponse> => {
    const params = new URLSearchParams({
      modes: '1',
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    const response = await apiFetch<GameListResponse>(`/api/games?${params}`)
    return response.data
  },

  /**
   * Get completed games (mode 2 - finished)
   */
  getCompleted: async (
    page = 0,
    pageSize = 20,
    from?: string,
    to?: string
  ): Promise<GameListResponse> => {
    const params = new URLSearchParams({
      modes: '2',
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    if (from) params.append('from', from)
    if (to) params.append('to', to)
    const response = await apiFetch<GameListResponse>(`/api/games?${params}`)
    return response.data
  },

  /**
   * Get game details by ID
   */
  getDetails: async (gameId: number): Promise<GameDetails> => {
    const response = await apiFetch<GameDetails>(`/api/games/${gameId}`)
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
      `/api/leaderboard?${params}`
    )
    return response.data
  },

  /**
   * Get a player's leaderboard rank
   */
  getPlayerRank: async (playerName: string): Promise<LeaderboardEntry> => {
    const response = await apiFetch<LeaderboardEntry>(`/api/leaderboard/player/${playerName}`)
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
  ): Promise<{ history: TournamentHistoryDto[]; total: number }> => {
    const params = new URLSearchParams({
      name: playerName,
      page: page.toString(),
      pageSize: pageSize.toString(),
    })
    if (from) params.append('from', from)
    if (to) params.append('to', to)
    const response = await apiFetch<{ history: TournamentHistoryDto[]; total: number }>(
      `/api/history?${params}`
    )
    return response.data
  },

  /**
   * Get tournament details
   */
  getDetails: async (tournamentId: number): Promise<TournamentHistoryEntry> => {
    const response = await apiFetch<TournamentHistoryEntry>(`/api/tournaments/${tournamentId}`)
    return response.data
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
    const response = await apiFetch<PlayerSearchDto[]>(`/api/search?${params}`)
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
      `/api/games/hosts?${params}`
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
      `/api/admin/profiles?${params}`
    )
    return response.data
  },

  /**
   * Get banned keys list (unpaginated - backend returns all bans)
   */
  getBans: async (): Promise<{ bans: BannedKeyDto[]; total: number }> => {
    const response = await apiFetch<BannedKeyDto[]>('/api/admin/bans')
    return { bans: response.data, total: response.data.length }
  },

  /**
   * Add a new ban
   * Backend expects: { key: string, until?: Date, comment?: string }
   */
  addBan: async (banData: {
    key: string
    comment?: string
    until?: string
  }): Promise<any> => {
    const response = await apiFetch<any>('/api/admin/bans', {
      method: 'POST',
      body: JSON.stringify(banData),
    })
    return response.data
  },

  /**
   * Remove a ban by key string (not numeric ID)
   */
  removeBan: async (key: string): Promise<void> => {
    await apiFetch<void>(`/api/admin/bans/${encodeURIComponent(key)}`, {
      method: 'DELETE',
    })
  },
}

/**
 * Utility function to check if the API is reachable
 */
export async function checkApiHealth(): Promise<boolean> {
  try {
    const response = await fetch(getApiUrl('/api/health'), {
      method: 'GET',
    })
    return response.ok
  } catch (error) {
    console.error('API health check failed:', error)
    return false
  }
}
