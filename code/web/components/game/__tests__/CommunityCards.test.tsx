/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { CommunityCards } from '../CommunityCards'

// next/image doesn't work in jsdom — replace with a plain <img>
vi.mock('next/image', () => ({
  default: ({
    src,
    alt,
    width,
  }: {
    src: string
    alt: string
    width: number
  }) => (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} width={width} />
  ),
}))

describe('CommunityCards', () => {
  it('returns null when no cards', () => {
    const { container } = render(<CommunityCards cards={[]} />)
    expect(container.firstChild).toBeNull()
  })

  it('shows only actual cards (no empty slots) for 3 cards', () => {
    render(<CommunityCards cards={['Ah', 'Kh', 'Qh']} />)
    const imgs = screen.getAllByRole('img')
    expect(imgs).toHaveLength(3)
  })

  it('shows 5 cards when all community cards are present', () => {
    render(<CommunityCards cards={['Ah', 'Kh', 'Qh', 'Jh', 'Th']} />)
    const imgs = screen.getAllByRole('img')
    expect(imgs).toHaveLength(5)
  })

  it('shows 1 card for a single card', () => {
    render(<CommunityCards cards={['Ah']} />)
    const imgs = screen.getAllByRole('img')
    expect(imgs).toHaveLength(1)
  })

  it('renders with correct ARIA label', () => {
    render(<CommunityCards cards={['Ah', 'Kh']} />)
    expect(screen.getByRole('region', { name: 'Community cards' })).toBeTruthy()
  })
})
