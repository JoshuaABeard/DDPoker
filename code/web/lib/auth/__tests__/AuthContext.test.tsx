/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor, act } from '@testing-library/react'
import React, { useContext } from 'react'
import { AuthProvider, AuthContext } from '../AuthContext'

// --- Module mocks ---

vi.mock('@/lib/api', () => ({
  authApi: {
    getCurrentUser: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('../storage', () => ({
  getAuthUser: vi.fn(),
  setAuthUser: vi.fn(),
  clearAuthUser: vi.fn(),
}))

import { authApi } from '@/lib/api'
import { getAuthUser, setAuthUser, clearAuthUser } from '../storage'

const mockGetCurrentUser = vi.mocked(authApi.getCurrentUser)
const mockLogin = vi.mocked(authApi.login)
const mockLogout = vi.mocked(authApi.logout)
const mockGetAuthUser = vi.mocked(getAuthUser)
const mockSetAuthUser = vi.mocked(setAuthUser)
const mockClearAuthUser = vi.mocked(clearAuthUser)

// --- Consumer component ---

function AuthConsumer() {
  const ctx = useContext(AuthContext)!
  return (
    <div>
      <span data-testid="username">{ctx.user?.username ?? ''}</span>
      <span data-testid="isAuthenticated">{String(ctx.isAuthenticated)}</span>
      <span data-testid="isLoading">{String(ctx.isLoading)}</span>
      <span data-testid="isAdmin">{String(ctx.user?.isAdmin ?? '')}</span>
      <span data-testid="error">{ctx.error ?? ''}</span>
      <button onClick={() => ctx.login('user1', 'pass1', false)}>login</button>
      <button onClick={() => ctx.logout()}>logout</button>
      <button onClick={() => ctx.clearError()}>clearError</button>
    </div>
  )
}

function renderWithProvider() {
  return render(
    <AuthProvider>
      <AuthConsumer />
    </AuthProvider>
  )
}

describe('AuthContext / AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- checkAuthStatus on mount ---

  describe('mount with stored user and valid session', () => {
    it('becomes authenticated with admin status from API', async () => {
      mockGetAuthUser.mockReturnValue({ username: 'alice' })
      mockGetCurrentUser.mockResolvedValue({
        success: true,
        message: 'ok',
        username: 'alice',
        admin: true,
      })

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isAuthenticated').textContent).toBe('true')
      )
      expect(screen.getByTestId('username').textContent).toBe('alice')
      expect(screen.getByTestId('isAdmin').textContent).toBe('true')
      expect(screen.getByTestId('isLoading').textContent).toBe('false')
    })
  })

  describe('mount with no stored user', () => {
    it('remains unauthenticated and does not call getCurrentUser', async () => {
      mockGetAuthUser.mockReturnValue(null)

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
      expect(mockGetCurrentUser).not.toHaveBeenCalled()
    })
  })

  describe('mount with expired session (API rejects)', () => {
    it('clears storage and becomes unauthenticated', async () => {
      mockGetAuthUser.mockReturnValue({ username: 'bob' })
      mockGetCurrentUser.mockRejectedValue(new Error('session expired'))

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
      expect(mockClearAuthUser).toHaveBeenCalled()
    })
  })

  describe('mount with API returning success:false', () => {
    it('clears storage and becomes unauthenticated', async () => {
      mockGetAuthUser.mockReturnValue({ username: 'carol' })
      mockGetCurrentUser.mockResolvedValue({
        success: false,
        message: 'invalid session',
        username: undefined,
      })

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
      expect(mockClearAuthUser).toHaveBeenCalled()
    })
  })

  // --- login ---

  describe('login success', () => {
    it('stores user in storage and updates authenticated state', async () => {
      mockGetAuthUser.mockReturnValue(null)
      mockLogin.mockResolvedValue({
        success: true,
        message: 'ok',
        username: 'user1',
        admin: false,
      })

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )

      await act(async () => {
        screen.getByRole('button', { name: /login/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('isAuthenticated').textContent).toBe('true')
      )
      expect(screen.getByTestId('username').textContent).toBe('user1')
      expect(mockSetAuthUser).toHaveBeenCalledWith({ username: 'user1' }, false)
    })
  })

  describe('login failure (API returns success:false)', () => {
    it('sets error from response.message', async () => {
      mockGetAuthUser.mockReturnValue(null)
      mockLogin.mockResolvedValue({
        success: false,
        message: 'Invalid credentials',
      })

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )

      await act(async () => {
        screen.getByRole('button', { name: /login/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('error').textContent).toBe('Invalid credentials')
      )
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
    })
  })

  describe('login exception', () => {
    it('sets error from error.message', async () => {
      mockGetAuthUser.mockReturnValue(null)
      mockLogin.mockRejectedValue(new Error('Network error'))

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )

      await act(async () => {
        screen.getByRole('button', { name: /login/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('error').textContent).toBe('Network error')
      )
      expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
    })
  })

  // --- logout ---

  describe('logout', () => {
    it('clears storage and resets state', async () => {
      mockGetAuthUser.mockReturnValue({ username: 'alice' })
      mockGetCurrentUser.mockResolvedValue({
        success: true,
        message: 'ok',
        username: 'alice',
        admin: false,
      })
      mockLogout.mockResolvedValue(undefined)

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isAuthenticated').textContent).toBe('true')
      )

      await act(async () => {
        screen.getByRole('button', { name: /logout/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
      )
      expect(screen.getByTestId('username').textContent).toBe('')
      expect(mockClearAuthUser).toHaveBeenCalled()
    })

    it('continues with logout even when API call fails', async () => {
      mockGetAuthUser.mockReturnValue({ username: 'alice' })
      mockGetCurrentUser.mockResolvedValue({
        success: true,
        message: 'ok',
        username: 'alice',
        admin: false,
      })
      mockLogout.mockRejectedValue(new Error('API down'))

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isAuthenticated').textContent).toBe('true')
      )

      await act(async () => {
        screen.getByRole('button', { name: /logout/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('isAuthenticated').textContent).toBe('false')
      )
      expect(mockClearAuthUser).toHaveBeenCalled()
    })
  })

  // --- clearError ---

  describe('clearError', () => {
    it('nulls the error', async () => {
      mockGetAuthUser.mockReturnValue(null)
      mockLogin.mockResolvedValue({ success: false, message: 'Bad login' })

      renderWithProvider()

      await waitFor(() =>
        expect(screen.getByTestId('isLoading').textContent).toBe('false')
      )

      await act(async () => {
        screen.getByRole('button', { name: /login/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('error').textContent).toBe('Bad login')
      )

      await act(async () => {
        screen.getByRole('button', { name: /clearError/i }).click()
      })

      await waitFor(() =>
        expect(screen.getByTestId('error').textContent).toBe('')
      )
    })
  })
})
