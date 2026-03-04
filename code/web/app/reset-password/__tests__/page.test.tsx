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
    resetPassword: vi.fn(),
  },
}))

// Mutable mock state
const mockReplace = vi.fn()
let mockToken: string | null = 'reset-token-abc'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: () => ({ get: (key: string) => (key === 'token' ? mockToken : null) }),
}))

import { authApi } from '@/lib/api'
import ResetPasswordPage from '../page'

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    mockToken = 'reset-token-abc'
    mockReplace.mockReset()
    vi.mocked(authApi.resetPassword).mockReset()
  })

  it('renders password and confirm password inputs and submit button', () => {
    render(<ResetPasswordPage />)
    expect(screen.getByPlaceholderText(/new password/i)).toBeTruthy()
    expect(screen.getByPlaceholderText(/confirm password/i)).toBeTruthy()
    expect(screen.getByRole('button', { name: /reset password/i })).toBeTruthy()
  })

  it('shows validation error when passwords do not match', async () => {
    render(<ResetPasswordPage />)

    fireEvent.change(screen.getByPlaceholderText(/new password/i), {
      target: { value: 'password123' },
    })
    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: 'different123' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /reset password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByText(/passwords do not match/i)).toBeTruthy()
    })
    expect(authApi.resetPassword).not.toHaveBeenCalled()
  })

  it('shows validation error when password is too short', async () => {
    render(<ResetPasswordPage />)

    fireEvent.change(screen.getByPlaceholderText(/new password/i), {
      target: { value: 'short' },
    })
    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: 'short' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /reset password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByText(/at least 8 characters/i)).toBeTruthy()
    })
    expect(authApi.resetPassword).not.toHaveBeenCalled()
  })

  it('shows confirmation message on success', async () => {
    vi.mocked(authApi.resetPassword).mockResolvedValue(new Response(null, { status: 200 }))
    render(<ResetPasswordPage />)

    fireEvent.change(screen.getByPlaceholderText(/new password/i), {
      target: { value: 'newpassword123' },
    })
    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: 'newpassword123' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /reset password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByText(/password reset/i)).toBeTruthy()
    })
  })

  it('shows server error message on failure', async () => {
    vi.mocked(authApi.resetPassword).mockResolvedValue(
      new Response(JSON.stringify({ message: 'Reset token has expired.' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      })
    )
    render(<ResetPasswordPage />)

    fireEvent.change(screen.getByPlaceholderText(/new password/i), {
      target: { value: 'newpassword123' },
    })
    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: 'newpassword123' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /reset password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByText(/reset token has expired/i)).toBeTruthy()
    })
  })

  it('calls resetPassword with token and password on valid submit', async () => {
    vi.mocked(authApi.resetPassword).mockResolvedValue(new Response(null, { status: 200 }))
    render(<ResetPasswordPage />)

    fireEvent.change(screen.getByPlaceholderText(/new password/i), {
      target: { value: 'securepass99' },
    })
    fireEvent.change(screen.getByPlaceholderText(/confirm password/i), {
      target: { value: 'securepass99' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /reset password/i }).closest('form')!)

    await waitFor(() => {
      expect(authApi.resetPassword).toHaveBeenCalledWith('reset-token-abc', 'securepass99')
    })
  })
})
