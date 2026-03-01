/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { PasswordDialog } from '../PasswordDialog'

describe('PasswordDialog', () => {
  it('renders the game name', () => {
    render(<PasswordDialog gameName="Sunday Night Poker" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.getByText('Sunday Night Poker')).toBeTruthy()
  })

  it('has role=dialog', () => {
    render(<PasswordDialog gameName="Test Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.getByRole('dialog')).toBeTruthy()
  })

  it('Submit button is disabled when password is empty', () => {
    render(<PasswordDialog gameName="Test Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    const submitBtn = screen.getByRole('button', { name: /^join$/i })
    expect((submitBtn as HTMLButtonElement).disabled).toBe(true)
  })

  it('Submit button is enabled when password is filled', () => {
    render(<PasswordDialog gameName="Test Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    const input = screen.getByLabelText('Password')
    fireEvent.change(input, { target: { value: 'secret' } })
    const submitBtn = screen.getByRole('button', { name: /^join$/i })
    expect((submitBtn as HTMLButtonElement).disabled).toBe(false)
  })

  it('calls onSubmit with the entered password on form submit', () => {
    const onSubmit = vi.fn()
    render(<PasswordDialog gameName="Test Game" onSubmit={onSubmit} onCancel={vi.fn()} />)
    const input = screen.getByLabelText('Password')
    fireEvent.change(input, { target: { value: 'mypassword' } })
    fireEvent.click(screen.getByRole('button', { name: /^join$/i }))
    expect(onSubmit).toHaveBeenCalledWith('mypassword')
  })

  it('Escape key fires onCancel', () => {
    const onCancel = vi.fn()
    render(<PasswordDialog gameName="Test Game" onSubmit={vi.fn()} onCancel={onCancel} />)
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(onCancel).toHaveBeenCalled()
  })

  it('Cancel button fires onCancel', () => {
    const onCancel = vi.fn()
    render(<PasswordDialog gameName="Test Game" onSubmit={vi.fn()} onCancel={onCancel} />)
    fireEvent.click(screen.getByRole('button', { name: /^cancel$/i }))
    expect(onCancel).toHaveBeenCalled()
  })

  it('shows error message when error prop is provided', () => {
    render(
      <PasswordDialog
        gameName="Test Game"
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
        error="Invalid password"
      />,
    )
    const alert = screen.getByRole('alert')
    expect(alert).toBeTruthy()
    expect(alert.textContent).toContain('Invalid password')
  })

  it('does not show error element when error prop is absent', () => {
    render(<PasswordDialog gameName="Test Game" onSubmit={vi.fn()} onCancel={vi.fn()} />)
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
