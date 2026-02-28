/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { motion, AnimatePresence } from 'framer-motion'
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
      <AnimatePresence>
        {cards.map((card, i) => (
          <motion.div
            key={card}
            initial={{ opacity: 0, y: -20, scale: 0.8 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 0.3, delay: i < 3 ? i * 0.15 : 0 }}
          >
            <Card card={card} width={cardWidth} />
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  )
}
