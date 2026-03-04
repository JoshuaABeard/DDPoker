/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'

// Mock authApi before importing the page
vi.mock('@/lib/api', () => ({
  authApi: {
    resendVerification: vi.fn(),
  },
}))

// Mock useAuth
let mockUser: { email: string } | null = { email: 'test@example.com' }
vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: () => ({ user: mockUser }),
}))

import { authApi } from '@/lib/api'
import VerifyEmailPendingPage from '../page'

describe('VerifyEmailPendingPage', () => {
  beforeEach(() => {
    mockUser = { email: 'test@example.com' }
    vi.mocked(authApi.resendVerification).mockReset()
  })

  it('renders heading and user email', () => {
    render(<VerifyEmailPendingPage />)
    expect(screen.getByRole('heading', { name: /check your email/i })).toBeTruthy()
    expect(screen.getByText(/test@example\.com/)).toBeTruthy()
  })

  it('renders the resend verification button', () => {
    render(<VerifyEmailPendingPage />)
    expect(screen.getByRole('button', { name: /resend verification email/i })).toBeTruthy()
  })

  it('calls resendVerification when button is clicked', async () => {
    vi.mocked(authApi.resendVerification).mockResolvedValue(new Response(null, { status: 200 }))
    render(<VerifyEmailPendingPage />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification email/i }))

    await waitFor(() => {
      expect(authApi.resendVerification).toHaveBeenCalledOnce()
    })
  })

  it('shows confirmation message and disables button on success', async () => {
    vi.mocked(authApi.resendVerification).mockResolvedValue(new Response(null, { status: 200 }))
    render(<VerifyEmailPendingPage />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification email/i }))

    await waitFor(() => {
      expect(screen.getByText(/email resent/i)).toBeTruthy()
    })

    const button = screen.getByRole('button', { name: /resend verification email/i }) as HTMLButtonElement
    expect(button.disabled).toBe(true)
  })

  it('shows error message on failure (non-ok response)', async () => {
    vi.mocked(authApi.resendVerification).mockResolvedValue(new Response(null, { status: 429 }))
    render(<VerifyEmailPendingPage />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification email/i }))

    await waitFor(() => {
      expect(screen.getByText(/could not resend/i)).toBeTruthy()
    })
  })

  it('shows error message when resendVerification throws', async () => {
    vi.mocked(authApi.resendVerification).mockRejectedValue(new Error('Network error'))
    render(<VerifyEmailPendingPage />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification email/i }))

    await waitFor(() => {
      expect(screen.getByText(/could not resend/i)).toBeTruthy()
    })
  })
})
