/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'

vi.mock('@/lib/api', () => ({
  adminApi: {
    verifyProfile: vi.fn(),
    unlockProfile: vi.fn(),
    resendVerification: vi.fn(),
  },
}))

import { adminApi } from '@/lib/api'
import { ProfileActionsCell } from '../ProfileActionsCell'

const mockVerify = vi.mocked(adminApi.verifyProfile)
const mockUnlock = vi.mocked(adminApi.unlockProfile)
const mockResend = vi.mocked(adminApi.resendVerification)

const FUTURE_LOCK = Date.now() + 60 * 60 * 1000

beforeEach(() => {
  mockVerify.mockReset()
  mockUnlock.mockReset()
  mockResend.mockReset()
})

describe('ProfileActionsCell — verification badge visibility', () => {
  it('shows Verify and Resend Verification buttons when not verified', () => {
    render(<ProfileActionsCell id={1} emailVerified={false} lockedUntil={null} />)
    expect(screen.getByRole('button', { name: /^verify$/i })).toBeTruthy()
    expect(screen.getByRole('button', { name: /resend verification/i })).toBeTruthy()
  })

  it('hides Verify and Resend Verification buttons when already verified', () => {
    render(<ProfileActionsCell id={1} emailVerified={true} lockedUntil={null} />)
    expect(screen.queryByRole('button', { name: /^verify$/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /resend verification/i })).toBeNull()
  })

  it('shows Unlock button when account is locked', () => {
    render(<ProfileActionsCell id={1} emailVerified={true} lockedUntil={FUTURE_LOCK} />)
    expect(screen.getByRole('button', { name: /^unlock$/i })).toBeTruthy()
  })

  it('hides Unlock button when account is not locked', () => {
    render(<ProfileActionsCell id={1} emailVerified={true} lockedUntil={null} />)
    expect(screen.queryByRole('button', { name: /^unlock$/i })).toBeNull()
  })

  it('hides Unlock button when lockedUntil is in the past', () => {
    const pastLock = Date.now() - 1000
    render(<ProfileActionsCell id={1} emailVerified={true} lockedUntil={pastLock} />)
    expect(screen.queryByRole('button', { name: /^unlock$/i })).toBeNull()
  })
})

describe('ProfileActionsCell — Verify button', () => {
  it('calls adminApi.verifyProfile with the profile id', async () => {
    mockVerify.mockResolvedValue(undefined)
    render(<ProfileActionsCell id={42} emailVerified={false} lockedUntil={null} />)

    fireEvent.click(screen.getByRole('button', { name: /^verify$/i }))

    await waitFor(() => {
      expect(mockVerify).toHaveBeenCalledWith(42)
    })
  })

  it('shows success message after verify succeeds', async () => {
    mockVerify.mockResolvedValue(undefined)
    render(<ProfileActionsCell id={1} emailVerified={false} lockedUntil={null} />)

    fireEvent.click(screen.getByRole('button', { name: /^verify$/i }))

    await waitFor(() => {
      expect(screen.getByRole('status')).toBeTruthy()
      expect(screen.getByText(/verified successfully/i)).toBeTruthy()
    })
  })

  it('shows error message when verify fails', async () => {
    mockVerify.mockRejectedValue(new Error('Network error'))
    render(<ProfileActionsCell id={1} emailVerified={false} lockedUntil={null} />)

    fireEvent.click(screen.getByRole('button', { name: /^verify$/i }))

    await waitFor(() => {
      expect(screen.getByText(/action failed/i)).toBeTruthy()
    })
  })
})

describe('ProfileActionsCell — Unlock button', () => {
  it('calls adminApi.unlockProfile with the profile id', async () => {
    mockUnlock.mockResolvedValue(undefined)
    render(<ProfileActionsCell id={7} emailVerified={true} lockedUntil={FUTURE_LOCK} />)

    fireEvent.click(screen.getByRole('button', { name: /^unlock$/i }))

    await waitFor(() => {
      expect(mockUnlock).toHaveBeenCalledWith(7)
    })
  })

  it('shows success message after unlock succeeds', async () => {
    mockUnlock.mockResolvedValue(undefined)
    render(<ProfileActionsCell id={1} emailVerified={true} lockedUntil={FUTURE_LOCK} />)

    fireEvent.click(screen.getByRole('button', { name: /^unlock$/i }))

    await waitFor(() => {
      expect(screen.getByText(/account unlocked/i)).toBeTruthy()
    })
  })

  it('shows error message when unlock fails', async () => {
    mockUnlock.mockRejectedValue(new Error('Network error'))
    render(<ProfileActionsCell id={1} emailVerified={true} lockedUntil={FUTURE_LOCK} />)

    fireEvent.click(screen.getByRole('button', { name: /^unlock$/i }))

    await waitFor(() => {
      expect(screen.getByText(/action failed/i)).toBeTruthy()
    })
  })
})

describe('ProfileActionsCell — Resend Verification button', () => {
  it('calls adminApi.resendVerification with the profile id', async () => {
    mockResend.mockResolvedValue(undefined)
    render(<ProfileActionsCell id={99} emailVerified={false} lockedUntil={null} />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification/i }))

    await waitFor(() => {
      expect(mockResend).toHaveBeenCalledWith(99)
    })
  })

  it('shows success message after resend succeeds', async () => {
    mockResend.mockResolvedValue(undefined)
    render(<ProfileActionsCell id={1} emailVerified={false} lockedUntil={null} />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification/i }))

    await waitFor(() => {
      expect(screen.getByText(/verification email sent/i)).toBeTruthy()
    })
  })

  it('shows error message when resend fails', async () => {
    mockResend.mockRejectedValue(new Error('Already verified'))
    render(<ProfileActionsCell id={1} emailVerified={false} lockedUntil={null} />)

    fireEvent.click(screen.getByRole('button', { name: /resend verification/i }))

    await waitFor(() => {
      expect(screen.getByText(/action failed/i)).toBeTruthy()
    })
  })
})
