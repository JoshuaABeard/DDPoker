/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect } from 'vitest'
import { DashboardWidget } from '../DashboardWidget'

describe('DashboardWidget', () => {
  it('shows children by default', () => {
    render(
      <DashboardWidget title="Stats">
        <span>child content</span>
      </DashboardWidget>,
    )
    expect(screen.getByText('child content')).toBeTruthy()
  })

  it('has aria-expanded=true when expanded by default', () => {
    render(
      <DashboardWidget title="Stats">
        <span>child content</span>
      </DashboardWidget>,
    )
    const button = screen.getByRole('button', { name: /Stats/i })
    expect(button.getAttribute('aria-expanded')).toBe('true')
  })

  it('hides children when defaultExpanded is false', () => {
    render(
      <DashboardWidget title="Stats" defaultExpanded={false}>
        <span>child content</span>
      </DashboardWidget>,
    )
    expect(screen.queryByText('child content')).toBeNull()
  })

  it('has aria-expanded=false when defaultExpanded is false', () => {
    render(
      <DashboardWidget title="Stats" defaultExpanded={false}>
        <span>child content</span>
      </DashboardWidget>,
    )
    const button = screen.getByRole('button', { name: /Stats/i })
    expect(button.getAttribute('aria-expanded')).toBe('false')
  })

  it('toggle click hides children when initially expanded', async () => {
    const user = userEvent.setup()
    render(
      <DashboardWidget title="Stats">
        <span>child content</span>
      </DashboardWidget>,
    )
    const button = screen.getByRole('button', { name: /Stats/i })
    await user.click(button)
    expect(screen.queryByText('child content')).toBeNull()
  })

  it('toggle click shows children when initially collapsed', async () => {
    const user = userEvent.setup()
    render(
      <DashboardWidget title="Stats" defaultExpanded={false}>
        <span>child content</span>
      </DashboardWidget>,
    )
    const button = screen.getByRole('button', { name: /Stats/i })
    await user.click(button)
    expect(screen.getByText('child content')).toBeTruthy()
  })
})
