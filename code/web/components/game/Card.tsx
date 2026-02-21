/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import Image from 'next/image'

interface CardProps {
  /** Card code like "Ah", "Kd", "2c". Empty string or undefined = face-down. */
  card?: string
  /** Display width in pixels (height auto-proportioned). Default 70. */
  width?: number
  className?: string
}

/**
 * Renders a single playing card image.
 *
 * Card codes use the format: rank (A,2-9,T,J,Q,K) + suit (h,d,c,s).
 * Maps to `public/images/cards/card_{code}.png`.
 * Face-down cards use `card_blank.png`.
 *
 * XSS safety: card code is validated before use in the image path.
 * Only alphanumeric ASCII characters are accepted.
 */
export function Card({ card, width = 70, className = '' }: CardProps) {
  const imageFile = isValidCardCode(card) ? `card_${card}` : 'card_blank'
  const height = Math.round(width * (280 / 200)) // maintain 200x280 aspect ratio

  return (
    <Image
      src={`/images/cards/${imageFile}.png`}
      alt={isValidCardCode(card) ? formatCardAlt(card!) : 'Face-down card'}
      width={width}
      height={height}
      className={`select-none ${className}`}
      style={{ imageRendering: 'crisp-edges' }}
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
