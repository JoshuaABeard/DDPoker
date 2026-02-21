/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Card } from '../Card'

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

describe('Card', () => {
  it('renders a face-down card when no card code is provided', () => {
    render(<Card />)
    const img = screen.getByRole('img')
    expect(img.getAttribute('src')).toBe('/images/cards/card_blank.png')
    expect(img.getAttribute('alt')).toBe('Face-down card')
  })

  it('renders the correct image for a valid card code', () => {
    render(<Card card="Ah" />)
    const img = screen.getByRole('img')
    expect(img.getAttribute('src')).toBe('/images/cards/card_Ah.png')
    expect(img.getAttribute('alt')).toBe('Ace of Hearts')
  })

  it('renders a face-down card for an XSS attempt in card code', () => {
    render(<Card card='<script>alert(1)</script>' />)
    const img = screen.getByRole('img')
    expect(img.getAttribute('src')).toBe('/images/cards/card_blank.png')
    expect(img.getAttribute('alt')).toBe('Face-down card')
  })

  it('renders a face-down card for an empty string', () => {
    render(<Card card="" />)
    expect(screen.getByRole('img').getAttribute('src')).toBe('/images/cards/card_blank.png')
  })

  it('renders a face-down card for a 1-character string', () => {
    render(<Card card="A" />)
    expect(screen.getByRole('img').getAttribute('src')).toBe('/images/cards/card_blank.png')
  })

  it('renders a face-down card for a 3-character string', () => {
    render(<Card card="Ahh" />)
    expect(screen.getByRole('img').getAttribute('src')).toBe('/images/cards/card_blank.png')
  })

  it('formats alt text correctly for all ranks and suits', () => {
    const cases: Array<[string, string]> = [
      ['Kd', 'King of Diamonds'],
      ['Qh', 'Queen of Hearts'],
      ['Jc', 'Jack of Clubs'],
      ['Ts', '10 of Spades'],
      ['2h', '2 of Hearts'],
      ['9c', '9 of Clubs'],
      ['As', 'Ace of Spades'],
    ]
    for (const [code, expectedAlt] of cases) {
      const { unmount } = render(<Card card={code} />)
      expect(screen.getByRole('img').getAttribute('alt')).toBe(expectedAlt)
      unmount()
    }
  })

  it('renders card images with the correct card code in the filename', () => {
    const { unmount: u1 } = render(<Card card="2c" />)
    expect(screen.getByRole('img').getAttribute('src')).toBe('/images/cards/card_2c.png')
    u1()

    const { unmount: u2 } = render(<Card card="Kd" />)
    expect(screen.getByRole('img').getAttribute('src')).toBe('/images/cards/card_Kd.png')
    u2()
  })

  it('applies the default width', () => {
    render(<Card card="Ah" />)
    expect(screen.getByRole('img').getAttribute('width')).toBe('70')
  })

  it('applies a custom width', () => {
    render(<Card card="Ah" width={40} />)
    expect(screen.getByRole('img').getAttribute('width')).toBe('40')
  })
})
