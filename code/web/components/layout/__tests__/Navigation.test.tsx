/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Navigation from '../Navigation'

let mockPathname = '/'
let mockAuthState = { user: null as { isAdmin?: boolean } | null, isAuthenticated: false }

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}))

vi.mock('next/link', () => ({
  default: ({
    href,
    className,
    children,
  }: {
    href: string
    className?: string
    children: React.ReactNode
  }) => (
    <a href={href} className={className}>
      {children}
    </a>
  ),
}))

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => mockAuthState,
}))

vi.mock('@/components/auth/CurrentProfile', () => ({
  CurrentProfile: () => <div data-testid="current-profile" />,
}))

describe('Navigation', () => {
  beforeEach(() => {
    mockPathname = '/'
    mockAuthState = { user: null, isAuthenticated: false }
  })

  it('renders nav items (Home, About, Download, etc.)', () => {
    render(<Navigation />)
    expect(screen.getByRole('link', { name: /Home/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /About/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Download/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Support/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Games/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Online/i })).toBeTruthy()
  })

  it('does NOT render admin items for non-admin user', () => {
    mockAuthState = { user: { isAdmin: false }, isAuthenticated: true }
    render(<Navigation />)
    expect(screen.queryByText('Ban List')).toBeNull()
  })

  it('renders admin items for admin user', () => {
    mockAuthState = { user: { isAdmin: true }, isAuthenticated: true }
    render(<Navigation />)
    expect(screen.getByRole('link', { name: /Admin/i })).toBeTruthy()
  })

  it('renders Login link when not authenticated', () => {
    mockAuthState = { user: null, isAuthenticated: false }
    render(<Navigation />)
    expect(screen.getByRole('link', { name: /Log In/i })).toBeTruthy()
    expect(screen.queryByTestId('current-profile')).toBeNull()
  })

  it('renders CurrentProfile when authenticated', () => {
    mockAuthState = { user: { isAdmin: false }, isAuthenticated: true }
    render(<Navigation />)
    expect(screen.getByTestId('current-profile')).toBeTruthy()
    expect(screen.queryByRole('link', { name: /Log In/i })).toBeNull()
  })
})
