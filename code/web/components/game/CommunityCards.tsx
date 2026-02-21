/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Card } from './Card'

interface CommunityCardsProps {
  cards: string[]
  cardWidth?: number
}

/**
 * Displays up to 5 community cards (flop, turn, river).
 * Empty slots render face-down placeholders.
 */
export function CommunityCards({ cards, cardWidth = 65 }: CommunityCardsProps) {
  const slots = Array.from({ length: 5 }, (_, i) => cards[i])

  return (
    <div className="flex gap-1 items-center justify-center" role="region" aria-label="Community cards">
      {slots.map((card, i) => (
        <div
          key={i}
          className="transition-transform duration-300"
          style={{ transform: card ? 'scale(1)' : 'scale(0.95)' }}
        >
          <Card card={card} width={cardWidth} />
        </div>
      ))}
    </div>
  )
}
