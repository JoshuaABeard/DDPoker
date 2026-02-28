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
    style,
  }: {
    src: string
    alt: string
    width: number
    style?: React.CSSProperties
  }) => (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={src} alt={alt} width={width} style={style} />
  ),
}))

describe('Card', () => {
  it('renders a face-down card when no card code is provided', () => {
    render(<Card />)
    const el = screen.getByRole('img')
    expect(el.getAttribute('aria-label')).toBe('Face-down card')
    expect(el.querySelector('svg')).toBeTruthy()
  })

  it('renders the correct image for a valid card code', () => {
    render(<Card card="Ah" />)
    const img = screen.getByRole('img')
    expect(img.getAttribute('src')).toBe('/images/cards/card_Ah.png')
    expect(img.getAttribute('alt')).toBe('Ace of Hearts')
  })

  it('renders a face-down card for an XSS attempt in card code', () => {
    render(<Card card='<script>alert(1)</script>' />)
    const el = screen.getByRole('img')
    expect(el.getAttribute('aria-label')).toBe('Face-down card')
    expect(el.querySelector('svg')).toBeTruthy()
  })

  it('renders a face-down card for an empty string', () => {
    render(<Card card="" />)
    expect(screen.getByRole('img').getAttribute('aria-label')).toBe('Face-down card')
  })

  it('renders a face-down card for a 1-character string', () => {
    render(<Card card="A" />)
    expect(screen.getByRole('img').getAttribute('aria-label')).toBe('Face-down card')
  })

  it('renders a face-down card for a 3-character string', () => {
    render(<Card card="Ahh" />)
    expect(screen.getByRole('img').getAttribute('aria-label')).toBe('Face-down card')
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

  describe('fourColorDeck', () => {
    it('does not apply a filter when fourColorDeck is false', () => {
      render(<Card card="Kd" />)
      const img = screen.getByRole('img') as HTMLImageElement
      expect(img.style.filter).toBe('')
    })

    it('applies hue-rotate filter to diamonds when fourColorDeck is true', () => {
      render(<Card card="Kd" fourColorDeck />)
      const img = screen.getByRole('img') as HTMLImageElement
      expect(img.style.filter).toBe('hue-rotate(240deg) saturate(1.5)')
    })

    it('applies hue-rotate filter to clubs when fourColorDeck is true', () => {
      render(<Card card="9c" fourColorDeck />)
      const img = screen.getByRole('img') as HTMLImageElement
      expect(img.style.filter).toBe('hue-rotate(120deg) saturate(2) brightness(1.3)')
    })

    it('does not apply a filter to hearts when fourColorDeck is true', () => {
      render(<Card card="Ah" fourColorDeck />)
      const img = screen.getByRole('img') as HTMLImageElement
      expect(img.style.filter).toBe('')
    })

    it('does not apply a filter to spades when fourColorDeck is true', () => {
      render(<Card card="Qs" fourColorDeck />)
      const img = screen.getByRole('img') as HTMLImageElement
      expect(img.style.filter).toBe('')
    })
  })
})
