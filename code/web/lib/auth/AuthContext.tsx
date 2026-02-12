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

interface AuthState {
  user: StoredAuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
  error: string | null
}

interface AuthContextValue extends AuthState {
  login: (username: string, password: string, rememberMe: boolean) => Promise<void>
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
        // Verify the session is still valid by calling the API
        const currentUser = await authApi.getCurrentUser()
        if (currentUser) {
          setState({
            user: { username: storedUser.username, isAdmin: storedUser.isAdmin },
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
   */
  const login = useCallback(async (username: string, password: string, rememberMe: boolean) => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }))

    try {
      const response = (await authApi.login({
        username,
        password,
        rememberMe,
      })) as unknown as AuthResponse

      if (response.success && response.username) {
        const user: StoredAuthUser = {
          username: response.username,
          isAdmin: response.admin || false,
        }

        setAuthUser(user, rememberMe)

        setState({
          user,
          isAuthenticated: true,
          isLoading: false,
          error: null,
        })
      } else {
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: response.message || 'Login failed',
        }))
      }
    } catch (error) {
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: error instanceof Error ? error.message : 'Login failed',
      }))
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
