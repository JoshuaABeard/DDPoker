/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { RANKS, SUITS } from './types'

/** Full 52-card deck as 2-char card codes */
export const FULL_DECK: string[] = RANKS.flatMap((r) => SUITS.map((s) => `${r}${s}`))

/** Fisher-Yates shuffle (mutates array) */
export function shuffle(deck: string[]): string[] {
  for (let i = deck.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[deck[i], deck[j]] = [deck[j], deck[i]]
  }
  return deck
}

/** Return a new deck with the given cards removed */
export function removeCards(deck: string[], cards: string[]): string[] {
  const set = new Set(cards.map((c) => c.toLowerCase()))
  return deck.filter((c) => !set.has(c.toLowerCase()))
}
