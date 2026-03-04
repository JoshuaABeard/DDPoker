/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'

vi.mock('@/lib/api', () => ({
  authApi: {
    changePassword: vi.fn(),
    changeEmail: vi.fn(),
    resendVerification: vi.fn(),
  },
}))

vi.mock('@/lib/auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

import { authApi } from '@/lib/api'
import { useAuth } from '@/lib/auth/useAuth'
import AccountPage from '../page'

const mockUseAuth = vi.mocked(useAuth)

function renderWithUser(emailVerified = true) {
  mockUseAuth.mockReturnValue({
    user: { username: 'testuser', email: 'test@example.com', emailVerified, isAdmin: false },
    isLoading: false,
    isAuthenticated: true,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    checkAuthStatus: vi.fn(),
    clearError: vi.fn(),
  })
  return render(<AccountPage />)
}

describe('AccountPage', () => {
  beforeEach(() => {
    vi.mocked(authApi.changePassword).mockReset()
    vi.mocked(authApi.changeEmail).mockReset()
    vi.mocked(authApi.resendVerification).mockReset()
  })

  it('renders headings for each section', () => {
    renderWithUser()
    expect(screen.getByRole('heading', { name: /account settings/i })).toBeTruthy()
    expect(screen.getByRole('heading', { name: /change password/i })).toBeTruthy()
    expect(screen.getByRole('heading', { name: /change email/i })).toBeTruthy()
  })

  it('shows validation error when passwords do not match', async () => {
    renderWithUser()
    fireEvent.change(screen.getByPlaceholderText(/current password/i), { target: { value: 'oldpass' } })
    fireEvent.change(screen.getByPlaceholderText(/new password \(min/i), { target: { value: 'newpass123' } })
    fireEvent.change(screen.getByPlaceholderText(/confirm new password/i), { target: { value: 'different' } })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByText(/passwords do not match/i)).toBeTruthy()
    })
    expect(vi.mocked(authApi.changePassword)).not.toHaveBeenCalled()
  })

  it('shows validation error when new password is too short', async () => {
    renderWithUser()
    fireEvent.change(screen.getByPlaceholderText(/current password/i), { target: { value: 'oldpass' } })
    fireEvent.change(screen.getByPlaceholderText(/new password \(min/i), { target: { value: 'short' } })
    fireEvent.change(screen.getByPlaceholderText(/confirm new password/i), { target: { value: 'short' } })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByText(/at least 8 characters/i)).toBeTruthy()
    })
    expect(vi.mocked(authApi.changePassword)).not.toHaveBeenCalled()
  })

  it('calls changePassword API and shows success', async () => {
    vi.mocked(authApi.changePassword).mockResolvedValue(new Response(null, { status: 200 }))
    renderWithUser()
    fireEvent.change(screen.getByPlaceholderText(/current password/i), { target: { value: 'oldpass' } })
    fireEvent.change(screen.getByPlaceholderText(/new password \(min/i), { target: { value: 'newpass123' } })
    fireEvent.change(screen.getByPlaceholderText(/confirm new password/i), { target: { value: 'newpass123' } })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByText(/password changed successfully/i)).toBeTruthy()
    })
    expect(vi.mocked(authApi.changePassword)).toHaveBeenCalledWith('oldpass', 'newpass123')
  })

  it('calls changeEmail API and shows confirmation message', async () => {
    vi.mocked(authApi.changeEmail).mockResolvedValue(new Response(null, { status: 200 }))
    renderWithUser()
    fireEvent.change(screen.getByPlaceholderText(/new email address/i), { target: { value: 'new@example.com' } })
    fireEvent.submit(screen.getByRole('button', { name: /update email/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByText(/confirmation email sent/i)).toBeTruthy()
    })
    expect(vi.mocked(authApi.changeEmail)).toHaveBeenCalledWith('new@example.com')
  })

  it('shows Resend Verification section when emailVerified is false', () => {
    renderWithUser(false)
    expect(screen.getByRole('heading', { name: /email verification/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /resend verification email/i })).toBeTruthy()
  })

  it('does NOT show Resend Verification section when emailVerified is true', () => {
    renderWithUser(true)
    expect(screen.queryByRole('heading', { name: /email verification/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /resend verification email/i })).toBeNull()
  })

  it('calls resendVerification and shows sent message', async () => {
    vi.mocked(authApi.resendVerification).mockResolvedValue(new Response(null, { status: 200 }))
    renderWithUser(false)
    fireEvent.click(screen.getByRole('button', { name: /resend verification email/i }))
    await waitFor(() => {
      expect(screen.getByText(/verification email resent/i)).toBeTruthy()
    })
    expect(vi.mocked(authApi.resendVerification)).toHaveBeenCalled()
  })
})
