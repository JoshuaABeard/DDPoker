/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Image from 'next/image'
import { CardBack } from './cardBacks'
import type { CardBackId } from '@/lib/theme/useCardBack'

interface CardProps {
  /** Card code like "Ah", "Kd", "2c". Empty string or undefined = face-down. */
  card?: string
  /** Display width in pixels (height auto-proportioned). Default 70. */
  width?: number
  className?: string
  /** Card back design to use when face-down. Default 'classic-red'. */
  cardBackId?: CardBackId
  /** When true, diamonds render blue and clubs render green. */
  fourColorDeck?: boolean
}

/** CSS filters to recolor suits for four-color deck mode. */
const FOUR_COLOR_FILTERS: Record<string, string> = {
  d: 'hue-rotate(240deg) saturate(1.5)',         // red diamonds → blue
  c: 'hue-rotate(120deg) saturate(2) brightness(1.3)', // black clubs → green
}

/**
 * Renders a single playing card image.
 *
 * Card codes use the format: rank (A,2-9,T,J,Q,K) + suit (h,d,c,s).
 * Maps to `public/images/cards/card_{code}.png`.
 * Face-down cards render an SVG card back design.
 *
 * XSS safety: card code is validated before use in the image path.
 * Only alphanumeric ASCII characters are accepted.
 */
export function Card({ card, width = 70, className = '', cardBackId = 'classic-red', fourColorDeck }: CardProps) {
  const height = Math.round(width * (280 / 200)) // maintain 200x280 aspect ratio

  if (!isValidCardCode(card)) {
    return (
      <span className={`inline-block select-none ${className}`} role="img" aria-label="Face-down card">
        <CardBack id={cardBackId} width={width} height={height} />
      </span>
    )
  }

  const suit = card[1]
  const filter = fourColorDeck ? FOUR_COLOR_FILTERS[suit] : undefined

  return (
    <Image
      src={`/images/cards/card_${card}.png`}
      alt={formatCardAlt(card)}
      width={width}
      height={height}
      className={`select-none ${className}`}
      style={{ imageRendering: 'crisp-edges', ...(filter ? { filter } : undefined) }}
    />
  )
}

/** Returns true only if card code is safe to use in a file path. */
function isValidCardCode(code?: string): code is string {
  return typeof code === 'string' && /^[A-Za-z0-9]{2}$/.test(code)
}

/** Human-readable alt text for a card code, e.g. "Ah" → "Ace of Hearts". */
function formatCardAlt(code: string): string {
  const rankMap: Record<string, string> = {
    A: 'Ace', '2': '2', '3': '3', '4': '4', '5': '5',
    '6': '6', '7': '7', '8': '8', '9': '9', T: '10',
    J: 'Jack', Q: 'Queen', K: 'King',
  }
  const suitMap: Record<string, string> = {
    h: 'Hearts', d: 'Diamonds', c: 'Clubs', s: 'Spades',
  }
  const rank = rankMap[code[0]] ?? code[0]
  const suit = suitMap[code[1]] ?? code[1]
  return `${rank} of ${suit}`
}
