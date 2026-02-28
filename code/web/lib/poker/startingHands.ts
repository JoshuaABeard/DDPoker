/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export type HandCategory = 'premium' | 'strong' | 'playable' | 'marginal' | 'fold'

/** Rank labels from highest to lowest, used as row/column headers */
export const HAND_LABELS = ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3', '2'] as const

/**
 * 13x13 grid of starting hand categories for standard 9-player full ring.
 *
 * Grid layout:
 * - Row = first rank (A=0, K=1, ..., 2=12)
 * - Col = second rank
 * - Upper-right triangle (col > row) = suited hands
 * - Lower-left triangle (row > col) = offsuit hands
 * - Diagonal (row === col) = pocket pairs
 */
// prettier-ignore
export const STARTING_HAND_CATEGORIES: HandCategory[][] = [
  //        A          K          Q          J          T          9          8          7          6          5          4          3          2
  /* A */ ['premium', 'premium', 'strong',  'strong',  'playable','playable','playable','playable','playable','playable','playable','playable','playable'],
  /* K */ ['strong',  'premium', 'strong',  'playable','playable','marginal','fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold'    ],
  /* Q */ ['playable','playable','premium', 'playable','playable','marginal','fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold'    ],
  /* J */ ['playable','marginal','marginal','strong',  'playable','marginal','fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold'    ],
  /* T */ ['marginal','marginal','marginal','marginal','strong',  'playable','marginal','fold',    'fold',    'fold',    'fold',    'fold',    'fold'    ],
  /* 9 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'playable','playable','marginal','fold',    'fold',    'fold',    'fold',    'fold'    ],
  /* 8 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'playable','playable','marginal','fold',    'fold',    'fold',    'fold'    ],
  /* 7 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'playable','playable','marginal','fold',    'fold',    'fold'    ],
  /* 6 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'marginal','marginal','fold',    'fold',    'fold'    ],
  /* 5 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'marginal','marginal','fold',    'fold'    ],
  /* 4 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'marginal','fold',    'fold'    ],
  /* 3 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'marginal','fold'    ],
  /* 2 */ ['fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'fold',    'marginal'],
]

/** Returns notation like "AKs", "AKo", or "AA" for a grid position */
export function getHandNotation(row: number, col: number): string {
  const r1 = HAND_LABELS[row]
  const r2 = HAND_LABELS[col]
  if (row === col) return `${r1}${r2}`
  if (col > row) return `${r1}${r2}s`
  return `${r2}${r1}o`
}

const RANK_TO_INDEX: Record<string, number> = {
  A: 0, K: 1, Q: 2, J: 3, T: 4,
  '9': 5, '8': 6, '7': 7, '6': 8, '5': 9, '4': 10, '3': 11, '2': 12,
}

/** Convert 2 hole cards (e.g. ["Ah", "Ks"]) to {row, col} grid position */
export function holeCardsToGrid(cards: string[]): { row: number; col: number } {
  const rank1 = cards[0][0].toUpperCase()
  const rank2 = cards[1][0].toUpperCase()
  const suit1 = cards[0][1]
  const suit2 = cards[1][1]
  const suited = suit1 === suit2

  const idx1 = RANK_TO_INDEX[rank1]
  const idx2 = RANK_TO_INDEX[rank2]

  // Higher rank (lower index) comes first
  const highIdx = Math.min(idx1, idx2)
  const lowIdx = Math.max(idx1, idx2)

  if (highIdx === lowIdx) {
    // Pocket pair — on the diagonal
    return { row: highIdx, col: highIdx }
  }

  if (suited) {
    // Suited — upper-right triangle (row < col)
    return { row: highIdx, col: lowIdx }
  }

  // Offsuit — lower-left triangle (row > col)
  return { row: lowIdx, col: highIdx }
}
