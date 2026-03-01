/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
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
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
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

  it('redirects to /online on successful login without returnUrl', async () => {
    mockLogin.mockResolvedValue(true)
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/online')
    })
  })

  it('redirects to valid returnUrl on successful login', async () => {
    mockLogin.mockResolvedValue(true)
    mockSearchParams = new URLSearchParams('returnUrl=/dashboard')
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/dashboard')
    })
  })

  it('does not redirect on failed login', async () => {
    mockLogin.mockResolvedValue(false)
    render(<LoginForm />)

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'wrongpass' } })
    fireEvent.submit(screen.getByRole('button', { name: /log in/i }).closest('form')!)

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalled()
    })
    expect(mockPush).not.toHaveBeenCalled()
  })

  it('blocks open redirect with //evil.com and falls back to /online', async () => {
    mockLogin.mockResolvedValue(true)
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
