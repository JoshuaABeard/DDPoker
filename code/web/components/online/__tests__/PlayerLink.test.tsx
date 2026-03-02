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
import { PlayerLink } from '../PlayerLink'

describe('PlayerLink', () => {
  it('renders the player name as link text', () => {
    render(<PlayerLink playerName="alice" />)
    expect(screen.getByRole('link', { name: 'alice' })).toBeTruthy()
  })

  it('links to the history page with the player name encoded', () => {
    render(<PlayerLink playerName="alice smith" />)
    const link = screen.getByRole('link')
    expect(link.getAttribute('href')).toBe('/online/history?name=alice%20smith')
  })

  it('applies extra className when provided', () => {
    render(<PlayerLink playerName="bob" className="text-red-500" />)
    const link = screen.getByRole('link')
    expect(link.className).toContain('text-red-500')
  })
})
