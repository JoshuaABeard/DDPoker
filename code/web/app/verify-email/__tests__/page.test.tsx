/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'

// Mock authApi before importing the page
vi.mock('@/lib/api', () => ({
  authApi: {
    verifyEmail: vi.fn(),
  },
}))

// Mutable mock state for navigation
const mockReplace = vi.fn()
let mockToken: string | null = 'test-token-123'

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: () => ({ get: (key: string) => (key === 'token' ? mockToken : null) }),
}))

import { authApi } from '@/lib/api'
import VerifyEmailPage from '../page'

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    mockToken = 'test-token-123'
    mockReplace.mockReset()
    vi.mocked(authApi.verifyEmail).mockReset()
  })

  it('shows verifying indicator initially before the async call resolves', async () => {
    // Never resolves during this test
    vi.mocked(authApi.verifyEmail).mockReturnValue(new Promise(() => {}))
    render(<VerifyEmailPage />)
    expect(screen.getByText(/verifying/i)).toBeTruthy()
  })

  it('redirects to /games on successful verification', async () => {
    vi.mocked(authApi.verifyEmail).mockResolvedValue(new Response(null, { status: 200 }))
    render(<VerifyEmailPage />)

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith('/games')
    })
  })

  it('shows error message and resend link on failure', async () => {
    vi.mocked(authApi.verifyEmail).mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify({ message: 'Token expired.' }), {
          status: 400,
          headers: { 'Content-Type': 'application/json' },
        })
      )
    )
    render(<VerifyEmailPage />)

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /verification failed/i })).toBeTruthy()
    })
    expect(screen.getByText(/token expired/i)).toBeTruthy()
    expect(screen.getByRole('link', { name: /request a new link/i })).toBeTruthy()
  })

  it('shows error immediately when token is missing', async () => {
    mockToken = null
    render(<VerifyEmailPage />)

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /verification failed/i })).toBeTruthy()
    })
    expect(screen.getByText(/missing token/i)).toBeTruthy()
    expect(authApi.verifyEmail).not.toHaveBeenCalled()
  })

  it('shows fallback error message when failure response has no JSON body', async () => {
    vi.mocked(authApi.verifyEmail).mockResolvedValue(new Response(null, { status: 400 }))
    render(<VerifyEmailPage />)

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /verification failed/i })).toBeTruthy()
    })
    expect(screen.getByText(/invalid or expired link/i)).toBeTruthy()
  })
})
