/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Configuration
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

/**
 * API configuration
 *
 * The API_BASE_URL is read from environment variables:
 * - NEXT_PUBLIC_API_BASE_URL: Set via .env.local for development
 *
 * Production default is empty string (same-origin relative URLs).
 * In Docker, the frontend and API are served from the same Spring Boot server,
 * so relative URLs like /api/games work without specifying a host.
 */
export const config = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL || '',
  apiTimeout: 30000, // 30 seconds
  environment: process.env.NODE_ENV || 'development',
  isDevelopment: process.env.NODE_ENV === 'development',
  isProduction: process.env.NODE_ENV === 'production',
}

/**
 * Base URL of the game server (pokergameserver).
 *
 * Set NEXT_PUBLIC_GAME_SERVER_URL in .env.local (development) or the
 * deployment environment. Example:
 *   NEXT_PUBLIC_GAME_SERVER_URL=http://poker.yourdomain.com:8877
 *
 * Defaults to localhost for local development.
 */
export const GAME_SERVER_URL = process.env.NEXT_PUBLIC_GAME_SERVER_URL ?? 'http://localhost:8877'

/**
 * Get the full API URL for an endpoint
 * @param endpoint - The API endpoint path (e.g., '/api/players')
 * @returns The full URL to the API endpoint
 */
export function getApiUrl(endpoint: string): string {
  return `${config.apiBaseUrl}${endpoint}`
}
