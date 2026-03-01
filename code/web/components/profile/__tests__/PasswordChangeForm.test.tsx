/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { PasswordChangeForm } from '../PasswordChangeForm'

vi.mock('@/lib/api', () => ({
  playerApi: { changePassword: vi.fn() },
}))

import { playerApi } from '@/lib/api'

describe('PasswordChangeForm', () => {
  beforeEach(() => {
    vi.mocked(playerApi.changePassword).mockReset()
  })

  it('renders 3 password fields', () => {
    render(<PasswordChangeForm />)
    expect(screen.getByLabelText(/current password/i)).toBeTruthy()
    expect(screen.getByLabelText(/^new password$/i)).toBeTruthy()
    expect(screen.getByLabelText(/confirm new password/i)).toBeTruthy()
  })

  it('shows error and does not call API when passwords do not match', async () => {
    render(<PasswordChangeForm />)

    fireEvent.change(screen.getByLabelText(/current password/i), {
      target: { value: 'currentPass1' },
    })
    fireEvent.change(screen.getByLabelText(/^new password$/i), {
      target: { value: 'newpassword1' },
    })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'differentPass1' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeTruthy()
    })
    expect(screen.getByRole('alert').textContent).toContain('do not match')
    expect(playerApi.changePassword).not.toHaveBeenCalled()
  })

  it('shows error and does not call API when new password is too short', async () => {
    render(<PasswordChangeForm />)

    fireEvent.change(screen.getByLabelText(/current password/i), {
      target: { value: 'currentPass1' },
    })
    fireEvent.change(screen.getByLabelText(/^new password$/i), {
      target: { value: 'short' },
    })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'short' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeTruthy()
    })
    expect(screen.getByRole('alert').textContent).toContain('at least 8 characters')
    expect(playerApi.changePassword).not.toHaveBeenCalled()
  })

  it('calls API with correct args on valid submit', async () => {
    vi.mocked(playerApi.changePassword).mockResolvedValue(undefined)
    render(<PasswordChangeForm />)

    fireEvent.change(screen.getByLabelText(/current password/i), {
      target: { value: 'myOldPass1' },
    })
    fireEvent.change(screen.getByLabelText(/^new password$/i), {
      target: { value: 'myNewPass1' },
    })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'myNewPass1' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)

    await waitFor(() => {
      expect(playerApi.changePassword).toHaveBeenCalledWith('myOldPass1', 'myNewPass1')
    })
  })

  it('shows success message and clears fields on success', async () => {
    vi.mocked(playerApi.changePassword).mockResolvedValue(undefined)
    render(<PasswordChangeForm />)

    fireEvent.change(screen.getByLabelText(/current password/i), {
      target: { value: 'myOldPass1' },
    })
    fireEvent.change(screen.getByLabelText(/^new password$/i), {
      target: { value: 'myNewPass1' },
    })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'myNewPass1' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByRole('status')).toBeTruthy()
    })
    expect(screen.getByRole('status').textContent).toContain('Password changed successfully')

    // Fields should be cleared
    expect((screen.getByLabelText(/current password/i) as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText(/^new password$/i) as HTMLInputElement).value).toBe('')
    expect((screen.getByLabelText(/confirm new password/i) as HTMLInputElement).value).toBe('')
  })

  it('shows error message when API throws', async () => {
    vi.mocked(playerApi.changePassword).mockRejectedValue(new Error('Current password is incorrect'))
    render(<PasswordChangeForm />)

    fireEvent.change(screen.getByLabelText(/current password/i), {
      target: { value: 'wrongOldPass' },
    })
    fireEvent.change(screen.getByLabelText(/^new password$/i), {
      target: { value: 'myNewPass1' },
    })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
      target: { value: 'myNewPass1' },
    })
    fireEvent.submit(screen.getByRole('button', { name: /change password/i }).closest('form')!)

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeTruthy()
    })
    expect(screen.getByRole('alert').textContent).toContain('Current password is incorrect')
  })
})
