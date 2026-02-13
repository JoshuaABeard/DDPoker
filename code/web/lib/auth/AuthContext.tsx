'use client'

/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Authentication Context
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { createContext, useCallback, useEffect, useState } from 'react'
import { authApi } from '../api'
import type { AuthResponse } from '../types'
import { clearAuthUser, getAuthUser, setAuthUser, type StoredAuthUser } from './storage'

interface AuthUser {
  username: string
  isAdmin: boolean
}

interface AuthState {
  user: AuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

interface AuthContextValue extends AuthState {
  login: (username: string, password: string, rememberMe: boolean) => Promise<boolean>
  logout: () => Promise<void>
  checkAuthStatus: () => Promise<void>
  clearError: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

interface AuthProviderProps {
  children: React.ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
  })

  /**
   * Check auth status on mount by verifying stored user data
   */
  const checkAuthStatus = useCallback(async () => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    try {
      const storedUser = getAuthUser()
      if (storedUser) {
        // Verify the session is still valid and get current admin status from API
        const authResponse = await authApi.getCurrentUser()
        if (authResponse && authResponse.success && authResponse.username) {
          setState({
            user: {
              username: authResponse.username,
              isAdmin: authResponse.admin || false
            },
            isAuthenticated: true,
            isLoading: false,
            error: null,
          })
          return
        }
      }

      // No valid session
      clearAuthUser()
      setState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      })
    } catch (error) {
      clearAuthUser()
      setState({
        user: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      })
    }
  }, [])

  /**
   * Log in a user
   * @returns true if login successful, false otherwise
   */
  const login = useCallback(async (username: string, password: string, rememberMe: boolean): Promise<boolean> => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    try {
      const response = await authApi.login({
        username,
        password,
        rememberMe,
      })

      if (response.success && response.username) {
        // Store only username in localStorage/sessionStorage (not isAdmin)
        const storedUser: StoredAuthUser = {
          username: response.username,
        }
        setAuthUser(storedUser, rememberMe)

        // Set full user info in state (including isAdmin from API response)
        const user: AuthUser = {
          username: response.username,
          isAdmin: response.admin || false,
        }

        setState({
          user,
          isAuthenticated: true,
          isLoading: false,
          error: null,
        })
        return true
      } else {
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: response.message || 'Login failed',
        }))
        return false
      }
    } catch (error) {
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Login failed',
      }))
      return false
    }
  }, [])

  /**
   * Log out the current user
   */
  const logout = useCallback(async () => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    try {
      await authApi.logout()
    } catch (error) {
      console.error('Logout API call failed:', error)
      // Continue with logout even if API call fails
    }

    clearAuthUser()
    setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    })
  }, [])

  /**
   * Clear error message
   */
  const clearError = useCallback(() => {
    setState((prev) => ({ ...prev, error: null }))
  }, [])

  // Check auth status on mount
  useEffect(() => {
    checkAuthStatus()
  }, [checkAuthStatus])

  const value: AuthContextValue = {
    ...state,
    login,
    logout,
    checkAuthStatus,
    clearError,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
