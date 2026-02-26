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
 * Displays community cards (flop, turn, river).
 * Returns null when no cards have been dealt — no empty placeholders.
 */
export function CommunityCards({ cards, cardWidth = 65 }: CommunityCardsProps) {
  if (cards.length === 0) return null

  return (
    <div className="flex gap-1 items-center justify-center" role="region" aria-label="Community cards">
      {cards.map((card, i) => (
        <div key={i} className="transition-transform duration-300">
          <Card card={card} width={cardWidth} />
        </div>
      ))}
    </div>
  )
}
