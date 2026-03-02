/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { PlayerList } from '../PlayerList'

describe('PlayerList', () => {
  it('renders a dash when player list is empty', () => {
    render(<PlayerList players={[]} />)
    expect(screen.getByText('-')).toBeTruthy()
  })

  it('renders each player as a link', () => {
    render(<PlayerList players={['alice', 'bob']} />)
    expect(screen.getByRole('link', { name: 'alice' })).toBeTruthy()
    expect(screen.getByRole('link', { name: 'bob' })).toBeTruthy()
  })

  it('uses default comma separator between players', () => {
    const { container } = render(<PlayerList players={['alice', 'bob']} />)
    expect(container.textContent).toContain(', ')
  })

  it('uses a custom separator when provided', () => {
    const { container } = render(<PlayerList players={['alice', 'bob']} separator=" | " />)
    expect(container.textContent).toContain(' | ')
  })

  it('renders a single player without a separator', () => {
    const { container } = render(<PlayerList players={['alice']} />)
    expect(container.textContent).not.toContain(', ')
  })
})
