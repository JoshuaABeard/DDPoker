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
 * Get the full API URL for an endpoint
 * @param endpoint - The API endpoint path (e.g., '/api/players')
 * @returns The full URL to the API endpoint
 */
export function getApiUrl(endpoint: string): string {
  return `${config.apiBaseUrl}${endpoint}`
}
