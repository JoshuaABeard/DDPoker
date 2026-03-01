/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AliasManagement } from '../AliasManagement'

vi.mock('@/components/ui/Dialog', () => ({
  Dialog: ({
    isOpen,
    onClose,
    onConfirm,
    title,
  }: {
    isOpen: boolean
    onClose: () => void
    onConfirm: () => void
    title: string
    children?: React.ReactNode
  }) =>
    isOpen ? (
      <div role="dialog">
        <p>{title}</p>
        <button onClick={onConfirm}>Confirm</button>
        <button onClick={onClose}>Cancel</button>
      </div>
    ) : null,
}))

const ACTIVE_ALIASES = [
  { name: 'AcePlayer', createdDate: '2026-01-01' },
  { name: 'RiverKing', createdDate: '2026-02-01' },
]

const RETIRED_ALIASES = [
  { name: 'OldNick', createdDate: '2025-01-01', retiredDate: '2025-06-01' },
]

describe('AliasManagement', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders active aliases', () => {
    render(<AliasManagement aliases={ACTIVE_ALIASES} />)
    expect(screen.getByText('AcePlayer')).toBeTruthy()
    expect(screen.getByText('RiverKing')).toBeTruthy()
  })

  it('renders retired aliases in a separate section', () => {
    render(<AliasManagement aliases={[...ACTIVE_ALIASES, ...RETIRED_ALIASES]} />)
    expect(screen.getByText('OldNick')).toBeTruthy()
    expect(screen.getByText(/Retired Aliases/i)).toBeTruthy()
  })

  it('shows "No active aliases" when there are none', () => {
    render(<AliasManagement aliases={RETIRED_ALIASES} />)
    expect(screen.getByText(/No active aliases/i)).toBeTruthy()
  })

  it('opens dialog when Retire button is clicked', () => {
    render(<AliasManagement aliases={ACTIVE_ALIASES} />)
    expect(screen.queryByRole('dialog')).toBeNull()

    const retireButtons = screen.getAllByRole('button', { name: /retire/i })
    fireEvent.click(retireButtons[0])

    expect(screen.getByRole('dialog')).toBeTruthy()
  })

  it('closes dialog when Cancel is clicked', async () => {
    render(<AliasManagement aliases={ACTIVE_ALIASES} />)

    const retireButtons = screen.getAllByRole('button', { name: /retire/i })
    fireEvent.click(retireButtons[0])
    expect(screen.getByRole('dialog')).toBeTruthy()

    fireEvent.click(screen.getByRole('button', { name: /cancel/i }))

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).toBeNull()
    })
  })

  it('shows success message (role="status") after confirming retire', async () => {
    render(<AliasManagement aliases={ACTIVE_ALIASES} />)

    const retireButtons = screen.getAllByRole('button', { name: /retire/i })
    fireEvent.click(retireButtons[0])

    fireEvent.click(screen.getByRole('button', { name: /confirm/i }))

    await waitFor(() => {
      expect(screen.getByRole('status')).toBeTruthy()
    })
    expect(screen.getByRole('status').textContent).toContain('retired successfully')
  })
})
