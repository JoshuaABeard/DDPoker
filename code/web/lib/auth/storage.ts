/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Auth Storage Utilities
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export interface StoredAuthUser {
  username: string
}

const AUTH_USER_KEY = 'ddpoker_auth_user'

/**
 * Get the stored auth user from localStorage or sessionStorage
 */
export function getAuthUser(): StoredAuthUser | null {
  if (typeof window === 'undefined') return null

  // Check localStorage first (rememberMe=true)
  const localData = localStorage.getItem(AUTH_USER_KEY)
  if (localData) {
    try {
      return JSON.parse(localData)
    } catch {
      localStorage.removeItem(AUTH_USER_KEY)
    }
  }

  // Check sessionStorage (rememberMe=false)
  const sessionData = sessionStorage.getItem(AUTH_USER_KEY)
  if (sessionData) {
    try {
      return JSON.parse(sessionData)
    } catch {
      sessionStorage.removeItem(AUTH_USER_KEY)
    }
  }

  return null
}

/**
 * Set the auth user in appropriate storage
 */
export function setAuthUser(user: StoredAuthUser, rememberMe: boolean): void {
  if (typeof window === 'undefined') return

  const userData = JSON.stringify(user)

  if (rememberMe) {
    // Store in localStorage (persists across browser sessions)
    localStorage.setItem(AUTH_USER_KEY, userData)
    // Clear sessionStorage if it exists
    sessionStorage.removeItem(AUTH_USER_KEY)
  } else {
    // Store in sessionStorage (clears on browser close)
    sessionStorage.setItem(AUTH_USER_KEY, userData)
    // Clear localStorage if it exists
    localStorage.removeItem(AUTH_USER_KEY)
  }
}

/**
 * Clear the auth user from both storages
 */
export function clearAuthUser(): void {
  if (typeof window === 'undefined') return

  localStorage.removeItem(AUTH_USER_KEY)
  sessionStorage.removeItem(AUTH_USER_KEY)
}
