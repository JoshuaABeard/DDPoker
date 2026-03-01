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
import { renderHook } from '@testing-library/react'
import { useRequireAuth } from '../useRequireAuth'

// --- Module mocks ---

const mockRouterPush = vi.fn()

vi.mock('../useAuth', () => ({
  useAuth: vi.fn(),
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockRouterPush }),
  usePathname: () => '/admin/dashboard',
}))

import { useAuth } from '../useAuth'

const mockUseAuth = vi.mocked(useAuth)

function buildAuthState(overrides: {
  user?: { username: string; isAdmin: boolean } | null
  isAuthenticated?: boolean
  isLoading?: boolean
} = {}) {
  return {
    user: null,
    isAuthenticated: false,
    isLoading: false,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    checkAuthStatus: vi.fn(),
    clearError: vi.fn(),
    ...overrides,
  }
}

describe('useRequireAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // --- loading state ---

  it('does not redirect while still loading', () => {
    mockUseAuth.mockReturnValue(buildAuthState({ isLoading: true }))

    renderHook(() => useRequireAuth())

    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  // --- not authenticated ---

  it('redirects to /login with encoded returnUrl when not authenticated', () => {
    mockUseAuth.mockReturnValue(buildAuthState({ isAuthenticated: false, isLoading: false }))

    renderHook(() => useRequireAuth())

    expect(mockRouterPush).toHaveBeenCalledWith(
      `/login?returnUrl=${encodeURIComponent('/admin/dashboard')}`
    )
  })

  // --- authenticated, no admin requirement ---

  it('does not redirect when authenticated and no admin requirement', () => {
    mockUseAuth.mockReturnValue(
      buildAuthState({
        user: { username: 'alice', isAdmin: false },
        isAuthenticated: true,
        isLoading: false,
      })
    )

    renderHook(() => useRequireAuth())

    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  // --- authenticated but not admin, admin required ---

  it('redirects to / when authenticated but not admin and requireAdmin=true', () => {
    mockUseAuth.mockReturnValue(
      buildAuthState({
        user: { username: 'bob', isAdmin: false },
        isAuthenticated: true,
        isLoading: false,
      })
    )

    renderHook(() => useRequireAuth({ requireAdmin: true }))

    expect(mockRouterPush).toHaveBeenCalledWith('/')
  })

  // --- authenticated admin, admin required ---

  it('does not redirect when authenticated admin and requireAdmin=true', () => {
    mockUseAuth.mockReturnValue(
      buildAuthState({
        user: { username: 'carol', isAdmin: true },
        isAuthenticated: true,
        isLoading: false,
      })
    )

    renderHook(() => useRequireAuth({ requireAdmin: true }))

    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  // --- return value ---

  it('returns { user, isLoading, isAuthenticated }', () => {
    const user = { username: 'dave', isAdmin: false }
    mockUseAuth.mockReturnValue(
      buildAuthState({ user, isAuthenticated: true, isLoading: false })
    )

    const { result } = renderHook(() => useRequireAuth())

    expect(result.current).toEqual({ user, isLoading: false, isAuthenticated: true })
  })
})
