/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { HandRank, HAND_RANK_NAMES, RANK_VALUES, type HandResult } from './types'

/** Reverse lookup: rank number to display name */
const RANK_NAMES: Record<number, string> = {
  14: 'Ace',
  13: 'King',
  12: 'Queen',
  11: 'Jack',
  10: 'Ten',
  9: 'Nine',
  8: 'Eight',
  7: 'Seven',
  6: 'Six',
  5: 'Five',
  4: 'Four',
  3: 'Three',
  2: 'Two',
}

/** Plural form for rank names (used in some descriptions) */
const RANK_NAMES_PLURAL: Record<number, string> = {
  14: 'Aces',
  13: 'Kings',
  12: 'Queens',
  11: 'Jacks',
  10: 'Tens',
  9: 'Nines',
  8: 'Eights',
  7: 'Sevens',
  6: 'Sixes',
  5: 'Fives',
  4: 'Fours',
  3: 'Threes',
  2: 'Twos',
}

/** Parse a 2-character card code into rank value and suit */
function parseCard(card: string): { rank: number; suit: string } {
  const rankChar = card[0]
  const suit = card[1]
  const rank = RANK_VALUES[rankChar]
  return { rank, suit }
}

/** Generate all C(n, k) combinations of indices */
function combinations<T>(arr: T[], k: number): T[][] {
  const results: T[][] = []
  const combo: T[] = []

  function recurse(start: number): void {
    if (combo.length === k) {
      results.push([...combo])
      return
    }
    for (let i = start; i < arr.length; i++) {
      combo.push(arr[i])
      recurse(i + 1)
      combo.pop()
    }
  }

  recurse(0)
  return results
}

/** Evaluate a 5-card hand and return the result */
function evaluate5(cards: string[]): HandResult {
  const parsed = cards.map(parseCard)
  const ranks = parsed.map((c) => c.rank).sort((a, b) => b - a) // descending
  const suits = parsed.map((c) => c.suit)

  // Check flush: all same suit
  const isFlush = suits.every((s) => s === suits[0])

  // Check straight: 5 consecutive ranks
  let isStraight = false
  let straightHigh = 0

  // Normal straight check (sorted descending)
  if (ranks[0] - ranks[4] === 4 && new Set(ranks).size === 5) {
    isStraight = true
    straightHigh = ranks[0]
  }

  // Ace-low straight (wheel): A-5-4-3-2
  if (!isStraight && ranks[0] === 14 && ranks[1] === 5 && ranks[2] === 4 && ranks[3] === 3 && ranks[4] === 2) {
    isStraight = true
    straightHigh = 5 // Ace counts as low, so high card is 5
  }

  // Straight flush / Royal flush
  if (isFlush && isStraight) {
    if (straightHigh === 14) {
      return {
        rank: HandRank.ROYAL_FLUSH,
        kickers: [14],
        description: 'Royal Flush',
      }
    }
    return {
      rank: HandRank.STRAIGHT_FLUSH,
      kickers: [straightHigh],
      description: `Straight Flush, ${RANK_NAMES[straightHigh]} High`,
    }
  }

  // Count rank occurrences
  const counts = new Map<number, number>()
  for (const r of ranks) {
    counts.set(r, (counts.get(r) || 0) + 1)
  }

  // Group by count
  const quads: number[] = []
  const trips: number[] = []
  const pairs: number[] = []
  const singles: number[] = []

  for (const [rank, count] of counts.entries()) {
    if (count === 4) quads.push(rank)
    else if (count === 3) trips.push(rank)
    else if (count === 2) pairs.push(rank)
    else singles.push(rank)
  }

  // Sort each group descending
  quads.sort((a, b) => b - a)
  trips.sort((a, b) => b - a)
  pairs.sort((a, b) => b - a)
  singles.sort((a, b) => b - a)

  // Four of a kind
  if (quads.length > 0) {
    const kickers = [quads[0], singles[0]]
    return {
      rank: HandRank.FOUR_OF_A_KIND,
      kickers,
      description: `Four of a Kind, ${RANK_NAMES_PLURAL[quads[0]]}`,
    }
  }

  // Full house
  if (trips.length > 0 && pairs.length > 0) {
    const kickers = [trips[0], pairs[0]]
    return {
      rank: HandRank.FULL_HOUSE,
      kickers,
      description: `Full House, ${RANK_NAMES_PLURAL[trips[0]]} over ${RANK_NAMES_PLURAL[pairs[0]]}`,
    }
  }

  // Flush
  if (isFlush) {
    return {
      rank: HandRank.FLUSH,
      kickers: ranks,
      description: `Flush, ${RANK_NAMES[ranks[0]]} High`,
    }
  }

  // Straight
  if (isStraight) {
    return {
      rank: HandRank.STRAIGHT,
      kickers: [straightHigh],
      description: `Straight, ${RANK_NAMES[straightHigh]} High`,
    }
  }

  // Three of a kind
  if (trips.length > 0) {
    const kickers = [trips[0], ...singles]
    return {
      rank: HandRank.THREE_OF_A_KIND,
      kickers,
      description: `Three of a Kind, ${RANK_NAMES_PLURAL[trips[0]]}`,
    }
  }

  // Two pair
  if (pairs.length === 2) {
    const kickers = [pairs[0], pairs[1], singles[0]]
    return {
      rank: HandRank.TWO_PAIR,
      kickers,
      description: `Two Pair, ${RANK_NAMES_PLURAL[pairs[0]]} and ${RANK_NAMES_PLURAL[pairs[1]]}`,
    }
  }

  // One pair
  if (pairs.length === 1) {
    const kickers = [pairs[0], ...singles]
    return {
      rank: HandRank.ONE_PAIR,
      kickers,
      description: `One Pair, ${RANK_NAMES_PLURAL[pairs[0]]}`,
    }
  }

  // High card
  return {
    rank: HandRank.HIGH_CARD,
    kickers: ranks,
    description: `High Card, ${RANK_NAMES[ranks[0]]}`,
  }
}

/**
 * Evaluate a poker hand of 5-7 cards and return the best possible 5-card hand result.
 * For 6-7 card hands, all C(n,5) combinations are evaluated.
 */
export function evaluateHand(cards: string[]): HandResult {
  if (cards.length === 5) {
    return evaluate5(cards)
  }

  // For 6-7 cards, try all 5-card combinations and keep the best
  const combos = combinations(cards, 5)
  let best: HandResult | null = null

  for (const combo of combos) {
    const result = evaluate5(combo)
    if (best === null || compareHands(result, best) > 0) {
      best = result
    }
  }

  return best!
}

/**
 * Compare two hand results.
 * Returns positive if a wins, negative if b wins, 0 if tie.
 */
export function compareHands(a: HandResult, b: HandResult): number {
  // Compare ranks first
  if (a.rank !== b.rank) {
    return a.rank - b.rank
  }

  // Compare kickers element-wise
  const len = Math.min(a.kickers.length, b.kickers.length)
  for (let i = 0; i < len; i++) {
    if (a.kickers[i] !== b.kickers[i]) {
      return a.kickers[i] - b.kickers[i]
    }
  }

  return 0
}
