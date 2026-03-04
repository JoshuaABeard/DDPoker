/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ReactNode } from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'

vi.mock('@/lib/api', () => ({
  authApi: {
    forgotPassword: vi.fn(),
  },
}))

vi.mock('next/link', () => ({
  default: ({ href, children }: { href: string; children: ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}))

import { authApi } from '@/lib/api'
import ForgotPassword from '../page'

describe('ForgotPassword', () => {
  beforeEach(() => {
    vi.mocked(authApi.forgotPassword).mockReset()
  })

  it('renders an email input field (not username)', () => {
    render(<ForgotPassword />)
    const input = screen.getByLabelText(/email/i)
    expect(input).toBeTruthy()
    expect((input as HTMLInputElement).type).toBe('email')
  })

  it('does not render a username input', () => {
    render(<ForgotPassword />)
    expect(screen.queryByLabelText(/^username$/i)).toBeNull()
  })

  it('calls forgotPassword with the entered email', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValue({ success: true, message: 'Email sent.' })
    render(<ForgotPassword />)
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@example.com' } })
    fireEvent.submit(screen.getByRole('button', { name: /send/i }).closest('form')!)
    await waitFor(() => {
      expect(vi.mocked(authApi.forgotPassword)).toHaveBeenCalledWith('user@example.com')
    })
  })

  it('shows success message when forgotPassword returns success:true', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValue({ success: true, message: 'Check your email.' })
    render(<ForgotPassword />)
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@example.com' } })
    fireEvent.submit(screen.getByRole('button', { name: /send/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByRole('status').textContent).toContain('Check your email.')
    })
  })

  it('shows error message when forgotPassword returns success:false', async () => {
    vi.mocked(authApi.forgotPassword).mockResolvedValue({ success: false, message: 'No account found.' })
    render(<ForgotPassword />)
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'unknown@example.com' } })
    fireEvent.submit(screen.getByRole('button', { name: /send/i }).closest('form')!)
    await waitFor(() => {
      expect(screen.getByRole('alert').textContent).toContain('No account found.')
    })
  })
})
