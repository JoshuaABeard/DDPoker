/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import Sidebar from '../Sidebar'

let mockPathname = '/'

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
}))

vi.mock('next/link', () => ({
  default: ({
    href,
    className,
    onClick,
    children,
  }: {
    href: string
    className?: string
    onClick?: () => void
    children: React.ReactNode
  }) => (
    <a href={href} className={className} onClick={onClick}>
      {children}
    </a>
  ),
}))

const sections = [
  {
    title: 'Games',
    items: [
      { title: 'Game Lobby', link: '/games', icon: 'X', exactMatch: true },
      { title: 'Hosts', link: '/online/hosts' },
    ],
  },
  {
    items: [{ title: 'Leaderboard', link: '/online/leaderboard' }],
  },
]

describe('Sidebar', () => {
  beforeEach(() => {
    mockPathname = '/'
  })

  it('renders section titles and item links', () => {
    render(<Sidebar sections={sections} />)
    expect(screen.getByText('Games')).toBeTruthy()
    expect(screen.getByRole('link', { name: /Game Lobby/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Hosts/i })).toBeTruthy()
    expect(screen.getByRole('link', { name: /Leaderboard/i })).toBeTruthy()
  })

  it('applies active class when pathname starts with link (non-exactMatch)', () => {
    mockPathname = '/online/hosts/detail'
    render(<Sidebar sections={sections} />)
    const hostsLink = screen.getByRole('link', { name: /Hosts/i })
    expect(hostsLink.className).toContain('active')
  })

  it('does NOT apply active class to exactMatch item when pathname is a sub-path', () => {
    mockPathname = '/games/lobby'
    render(<Sidebar sections={sections} />)
    const gameLobbyLink = screen.getByRole('link', { name: /Game Lobby/i })
    expect(gameLobbyLink.className).not.toContain('active')
  })

  it('applies active class to exactMatch item on exact path match', () => {
    mockPathname = '/games'
    render(<Sidebar sections={sections} />)
    const gameLobbyLink = screen.getByRole('link', { name: /Game Lobby/i })
    expect(gameLobbyLink.className).toContain('active')
  })

  it('hamburger button toggles aria-expanded on click', () => {
    render(<Sidebar sections={sections} />)
    const hamburger = screen.getByLabelText(/Toggle sidebar menu/i)
    expect(hamburger.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(hamburger)
    expect(hamburger.getAttribute('aria-expanded')).toBe('true')
    fireEvent.click(hamburger)
    expect(hamburger.getAttribute('aria-expanded')).toBe('false')
  })

  it('Escape key closes the open mobile sidebar', () => {
    render(<Sidebar sections={sections} />)
    const hamburger = screen.getByLabelText(/Toggle sidebar menu/i)
    fireEvent.click(hamburger)
    expect(hamburger.getAttribute('aria-expanded')).toBe('true')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(hamburger.getAttribute('aria-expanded')).toBe('false')
  })
})
