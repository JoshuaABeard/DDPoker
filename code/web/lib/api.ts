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
  Game,
  GameDetails,
  LeaderboardEntry,
  LoginRequest,
  PaginatedResponse,
  PlayerProfile,
  RegisterRequest,
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

  const fetchOptions: RequestInit = {
    credentials: 'include', // Always send cookies (JWT in HttpOnly cookie)
    ...options,
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
  getCurrentUser: async (): Promise<PlayerProfile | null> => {
    try {
      const response = await apiFetch<PlayerProfile>('/api/auth/me')
      return response.data
    } catch (error) {
      console.error('Failed to get current user:', error)
      return null
    }
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
 * Games API
 */
export const gamesApi = {
  /**
   * Get all games
   */
  getAll: async (status?: 'waiting' | 'in_progress' | 'completed'): Promise<Game[]> => {
    const query = status ? `?status=${status}` : ''
    const response = await apiFetch<Game[]>(`/api/games${query}`)
    return response.data
  },

  /**
   * Get current games (in progress)
   */
  getCurrent: async (): Promise<Game[]> => {
    return gamesApi.getAll('in_progress')
  },

  /**
   * Get completed games
   */
  getCompleted: async (page = 0, pageSize = 20): Promise<PaginatedResponse<Game>> => {
    const response = await apiFetch<PaginatedResponse<Game>>(
      `/api/games/completed?page=${page}&pageSize=${pageSize}`
    )
    return response.data
  },

  /**
   * Get game details by ID
   */
  getDetails: async (gameId: number): Promise<GameDetails> => {
    const response = await apiFetch<GameDetails>(`/api/games/${gameId}`)
    return response.data
  },

  /**
   * Search games
   */
  search: async (query: string): Promise<Game[]> => {
    const response = await apiFetch<Game[]>(`/api/games/search?q=${encodeURIComponent(query)}`)
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
  getLeaderboard: async (limit = 100): Promise<LeaderboardEntry[]> => {
    const response = await apiFetch<LeaderboardEntry[]>(`/api/leaderboard?limit=${limit}`)
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
    pageSize = 20
  ): Promise<PaginatedResponse<TournamentHistoryEntry>> => {
    const response = await apiFetch<PaginatedResponse<TournamentHistoryEntry>>(
      `/api/tournaments/player/${playerName}?page=${page}&pageSize=${pageSize}`
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
