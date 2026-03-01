/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import Footer from '../Footer'

describe('Footer', () => {
  it('renders community edition text', () => {
    render(<Footer />)
    expect(screen.getByText('DD Poker Community Edition')).toBeTruthy()
  })

  it('has a link to ddpoker.com with correct href', () => {
    render(<Footer />)
    const link = screen.getByRole('link', { name: /Doug Donohoe/i })
    expect(link.getAttribute('href')).toBe('https://www.ddpoker.com')
  })

  it('opens the ddpoker.com link in a new tab', () => {
    render(<Footer />)
    const link = screen.getByRole('link', { name: /Doug Donohoe/i })
    expect(link.getAttribute('target')).toBe('_blank')
  })

  it('has rel="noopener noreferrer" on the ddpoker.com link', () => {
    render(<Footer />)
    const link = screen.getByRole('link', { name: /Doug Donohoe/i })
    expect(link.getAttribute('rel')).toBe('noopener noreferrer')
  })

  it('renders trademark notice', () => {
    render(<Footer />)
    expect(screen.getByText(/DD Poker™ is a trademark of Donohoe Digital LLC/)).toBeTruthy()
  })
})
