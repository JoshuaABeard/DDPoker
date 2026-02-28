/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ThemePicker } from '../ThemePicker'

vi.mock('@/lib/theme/useTheme', () => ({
  useTheme: () => ({ themeId: 'classic-green', colors: { center: '#2d5a1b', mid: '#1e3d12', edge: '#152d0d', border: '#1a2e0f' }, setTheme: vi.fn() }),
}))
vi.mock('@/lib/theme/useCardBack', () => ({
  useCardBack: () => ({ cardBackId: 'classic-red', setCardBack: vi.fn() }),
  CARD_BACK_IDS: ['classic-red', 'blue-diamond', 'green-celtic', 'gold-royal'],
}))
vi.mock('@/lib/theme/useAvatar', () => ({
  useAvatar: () => ({ avatarId: 'spade', setAvatar: vi.fn() }),
  AVATAR_IDS: ['bear', 'eagle', 'fox', 'wolf', 'shark', 'owl', 'crown', 'diamond', 'spade', 'star', 'flame', 'lightning'],
}))

describe('ThemePicker', () => {
  it('renders a settings button', () => {
    render(<ThemePicker />)
    expect(screen.getByRole('button', { name: /settings/i })).toBeDefined()
  })

  it('opens popover on click', () => {
    render(<ThemePicker />)
    fireEvent.click(screen.getByRole('button', { name: /settings/i }))
    expect(screen.getByText('Table')).toBeDefined()
  })

  it('shows theme options when Table tab active', () => {
    render(<ThemePicker />)
    fireEvent.click(screen.getByRole('button', { name: /settings/i }))
    expect(screen.getByLabelText(/classic green/i)).toBeDefined()
  })
})
