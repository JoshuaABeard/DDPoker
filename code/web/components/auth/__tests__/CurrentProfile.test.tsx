/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { CurrentProfile } from '../CurrentProfile'

// Mutable mock state
let mockUser: { username: string; isAdmin: boolean } | null = null
let mockIsAuthenticated = false
const mockLogout = vi.fn()
const mockPush = vi.fn()

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: mockIsAuthenticated,
    logout: mockLogout,
  }),
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

describe('CurrentProfile', () => {
  beforeEach(() => {
    mockUser = null
    mockIsAuthenticated = false
    mockLogout.mockReset()
    mockPush.mockReset()
  })

  it('returns null (empty container) when not authenticated', () => {
    const { container } = render(<CurrentProfile />)
    expect(container.firstChild).toBeNull()
  })

  it('renders username when authenticated', () => {
    mockIsAuthenticated = true
    mockUser = { username: 'JohnDoe', isAdmin: false }
    render(<CurrentProfile />)
    expect(screen.getByText('JohnDoe')).toBeTruthy()
  })

  it('shows Admin badge when isAdmin is true', () => {
    mockIsAuthenticated = true
    mockUser = { username: 'JaneSmith', isAdmin: true }
    render(<CurrentProfile />)
    expect(screen.getByText('Admin')).toBeTruthy()
  })

  it('does not show Admin badge when isAdmin is false', () => {
    mockIsAuthenticated = true
    mockUser = { username: 'RegularUser', isAdmin: false }
    render(<CurrentProfile />)
    expect(screen.queryByText('Admin')).toBeNull()
  })

  it('calls logout() and navigates to / when Logout button is clicked', async () => {
    mockLogout.mockResolvedValue(undefined)
    mockIsAuthenticated = true
    mockUser = { username: 'JohnDoe', isAdmin: false }
    render(<CurrentProfile />)

    fireEvent.click(screen.getByRole('button', { name: /logout/i }))

    await waitFor(() => {
      expect(mockLogout).toHaveBeenCalled()
      expect(mockPush).toHaveBeenCalledWith('/')
    })
  })
})
