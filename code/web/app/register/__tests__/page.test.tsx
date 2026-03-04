/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import type { ReactNode } from 'react'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'

// Mock api before importing the page
vi.mock('@/lib/api', () => ({
  authApi: {
    register: vi.fn(),
    checkUsername: vi.fn(),
  },
}))

const mockPush = vi.fn()

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: mockPush }),
}))

vi.mock('next/link', () => ({
  default: ({ href, children }: { href: string; children: ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}))

import { authApi } from '@/lib/api'
import RegisterPage from '../page'

describe('RegisterPage', () => {
  beforeEach(() => {
    mockPush.mockReset()
    vi.mocked(authApi.register).mockReset()
    vi.mocked(authApi.checkUsername).mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function fillForm(username = 'testuser', email = 'test@example.com', password = 'password123') {
    fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: username } })
    fireEvent.change(screen.getByLabelText(/^email$/i), { target: { value: email } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: password } })
    fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: password } })
  }

  it('renders the registration form', () => {
    render(<RegisterPage />)
    expect(screen.getByLabelText(/^username$/i)).toBeTruthy()
    expect(screen.getByLabelText(/^email$/i)).toBeTruthy()
    expect(screen.getByLabelText(/^password$/i)).toBeTruthy()
    expect(screen.getByLabelText(/confirm password/i)).toBeTruthy()
  })

  it('redirects to /verify-email-pending after successful registration', async () => {
    vi.mocked(authApi.register).mockResolvedValue({ success: true, message: null, username: 'testuser' })
    render(<RegisterPage />)
    fillForm()
    fireEvent.submit(screen.getByRole('button', { name: /create account/i }).closest('form')!)
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/verify-email-pending')
    })
  })

  it('shows error when passwords do not match', async () => {
    render(<RegisterPage />)
    fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: 'testuser' } })
    fireEvent.change(screen.getByLabelText(/^email$/i), { target: { value: 'test@example.com' } })
    fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'password123' } })
    fireEvent.change(screen.getByLabelText(/confirm password/i), { target: { value: 'different' } })
    fireEvent.submit(screen.getByRole('button', { name: /create account/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain('Passwords do not match')
    })
    expect(vi.mocked(authApi.register)).not.toHaveBeenCalled()
  })

  it('calls checkUsername after debounce when username is typed', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.mocked(authApi.checkUsername).mockResolvedValue(
      new Response(JSON.stringify({ available: true }), { status: 200 })
    )
    render(<RegisterPage />)
    fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: 'newuser' } })
    // Before debounce fires, should not have been called
    expect(vi.mocked(authApi.checkUsername)).not.toHaveBeenCalled()
    // Advance timers past the 400ms debounce
    await act(async () => {
      vi.advanceTimersByTime(400)
    })
    await waitFor(() => {
      expect(vi.mocked(authApi.checkUsername)).toHaveBeenCalledWith('newuser')
    })
  })

  it('shows "Username available" when checkUsername returns available:true', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.mocked(authApi.checkUsername).mockResolvedValue(
      new Response(JSON.stringify({ available: true }), { status: 200 })
    )
    render(<RegisterPage />)
    fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: 'newuser' } })
    await act(async () => {
      vi.advanceTimersByTime(400)
    })
    await waitFor(() => {
      expect(screen.getByText(/username available/i)).toBeTruthy()
    })
  })

  it('shows "Username not available" when checkUsername returns available:false', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    vi.mocked(authApi.checkUsername).mockResolvedValue(
      new Response(JSON.stringify({ available: false }), { status: 200 })
    )
    render(<RegisterPage />)
    fireEvent.change(screen.getByLabelText(/^username$/i), { target: { value: 'takenuser' } })
    await act(async () => {
      vi.advanceTimersByTime(400)
    })
    await waitFor(() => {
      expect(screen.getByText(/username not available/i)).toBeTruthy()
    })
  })

  it('shows server error when registration fails', async () => {
    vi.mocked(authApi.register).mockResolvedValue({ success: false, message: 'Username already taken.' })
    render(<RegisterPage />)
    fillForm()
    fireEvent.submit(screen.getByRole('button', { name: /create account/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain('Username already taken.')
    })
    expect(mockPush).not.toHaveBeenCalled()
  })
})
