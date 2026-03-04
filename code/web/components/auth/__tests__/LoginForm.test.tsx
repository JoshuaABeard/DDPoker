/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactNode } from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { LoginForm } from '../LoginForm'

// Mutable mock state
let mockError: string | null = null
let mockIsLoading = false
const mockLogin = vi.fn()
const mockClearError = vi.fn()
const mockPush = vi.fn()
let mockSearchParams = new URLSearchParams()

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => ({
    login: mockLogin,
    error: mockError,
    isLoading: mockIsLoading,
    clearError: mockClearError,
  }),
}))

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
  useSearchParams: () => mockSearchParams,
}))

vi.mock('next/link', () => ({
  default: ({ href, children }: { href: string; children: ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}))

describe('LoginForm', () => {
  beforeEach(() => {
    mockError = null
    mockIsLoading = false
    mockSearchParams = new URLSearchParams()
    mockLogin.mockReset()
    mockClearError.mockReset()
    mockPush.mockReset()
  })

  it('renders username, password, and remember-me fields', () => {
    render(<LoginForm />)
    expect(screen.getByLabelText(/username/i)).toBeTruthy()
    expect(screen.getByLabelText(/^password$/i)).toBeTruthy()
    expect(screen.getByRole('checkbox')).toBeTruthy()
  })

  it('renders submit button', () => {
    render(<LoginForm />)
    expect(screen.getByRole('button', { name: /log in/i })).toBeTruthy()
  })

  it('renders Forgot password link with href /forgot', () => {
    render(<LoginForm />)
    const link = screen.getByRole('link', { name: /forgot your password/i })
    expect(link).toBeTruthy()
    expect((link as HTMLAnchorElement).href).toContain('/forgot')
  })

  it('disables inputs and shows "Logging in..." when isLoading is true', () => {
    mockIsLoading = true
    render(<LoginForm />)
    expect((screen.getByLabelText(/username/i) as HTMLInputElement).disabled).toBe(true)
    expect((screen.getByLabelText(/^password$/i) as HTMLInputElement).disabled).toBe(true)
    expect((screen.getByRole('checkbox') as HTMLInputElement).disabled).toBe(true)
    expect(screen.getByRole('button', { name: /logging in/i })).toBeTruthy()
  })

  it('shows error via role="alert" when error is set', () => {
    mockError = 'Invalid credentials'
    render(<LoginForm />)
    const alert = screen.getByRole('alert')
    expect(alert).toBeTruthy()
    expect(alert.textContent).toContain('Invalid credentials')
  })

  it('redirects to /online on successful login with verified email and no returnUrl', async () => {
    mockLogin.mockResolvedValue({ success: true, emailVerified: true })
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/online')
    })
  })

  it('redirects to valid returnUrl on successful login with verified email', async () => {
    mockLogin.mockResolvedValue({ success: true, emailVerified: true })
    mockSearchParams = new URLSearchParams('returnUrl=/dashboard')
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/dashboard')
    })
  })

  it('redirects to /verify-email-pending on successful login with unverified email', async () => {
    mockLogin.mockResolvedValue({ success: true, emailVerified: false })
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/verify-email-pending')
    })
  })

  it('does not redirect on failed login', async () => {
    mockLogin.mockResolvedValue({ success: false })
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'wrongpass' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalled()
      expect(mockPush).not.toHaveBeenCalled()
    })
  })

  it('calls clearError when username input changes', () => {
    mockError = 'Some error'
    render(<LoginForm />)
    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'newvalue' } })
    expect(mockClearError).toHaveBeenCalled()
  })

  it('blocks open redirect with //evil.com and falls back to /online', async () => {
    mockLogin.mockResolvedValue({ success: true, emailVerified: true })
    mockSearchParams = new URLSearchParams('returnUrl=//evil.com')
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/online')
    })
  })
})
