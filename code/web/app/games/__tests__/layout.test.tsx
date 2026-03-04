/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

const mockReplace = vi.fn()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
}))

vi.mock('@/components/layout/SidebarLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="sidebar-layout">{children}</div>
  ),
}))

vi.mock('@/lib/sidebarData', () => ({
  gamesSidebarData: [],
}))

import { useAuth } from '@/lib/auth/useAuth'
import GamesLayout from '../layout'

const mockUseAuth = vi.mocked(useAuth)

describe('GamesLayout', () => {
  beforeEach(() => {
    mockReplace.mockReset()
  })

  it('redirects to /login when not authenticated', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isLoading: false,
      isAuthenticated: false,
      error: null,
      login: vi.fn(),
      logout: vi.fn(),
      checkAuthStatus: vi.fn(),
      clearError: vi.fn(),
    })
    render(<GamesLayout><div>content</div></GamesLayout>)
    expect(mockReplace).toHaveBeenCalledWith('/login')
  })

  it('redirects to /verify-email-pending when authenticated but email not verified', () => {
    mockUseAuth.mockReturnValue({
      user: { username: 'testuser', email: 'test@example.com', emailVerified: false, isAdmin: false },
      isLoading: false,
      isAuthenticated: true,
      error: null,
      login: vi.fn(),
      logout: vi.fn(),
      checkAuthStatus: vi.fn(),
      clearError: vi.fn(),
    })
    render(<GamesLayout><div>content</div></GamesLayout>)
    expect(mockReplace).toHaveBeenCalledWith('/verify-email-pending')
  })

  it('renders children when authenticated and email is verified', () => {
    mockUseAuth.mockReturnValue({
      user: { username: 'testuser', email: 'test@example.com', emailVerified: true, isAdmin: false },
      isLoading: false,
      isAuthenticated: true,
      error: null,
      login: vi.fn(),
      logout: vi.fn(),
      checkAuthStatus: vi.fn(),
      clearError: vi.fn(),
    })
    render(<GamesLayout><div>game content</div></GamesLayout>)
    expect(screen.getByText('game content')).toBeTruthy()
    expect(mockReplace).not.toHaveBeenCalled()
  })

  it('does not redirect while loading', () => {
    mockUseAuth.mockReturnValue({
      user: null,
      isLoading: true,
      isAuthenticated: false,
      error: null,
      login: vi.fn(),
      logout: vi.fn(),
      checkAuthStatus: vi.fn(),
      clearError: vi.fn(),
    })
    render(<GamesLayout><div>content</div></GamesLayout>)
    expect(mockReplace).not.toHaveBeenCalled()
  })
})
