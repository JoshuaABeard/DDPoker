/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { Fragment } from 'react'

type HandCategory = 'premium' | 'strong' | 'playable' | 'marginal' | 'fold'

/** Rank labels from highest to lowest, used as row/column headers */
const HAND_LABELS = ['A', 'K', 'Q', 'J', 'T', '9', '8', '7', '6', '5', '4', '3', '2'] as const

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
const STARTING_HAND_CATEGORIES: HandCategory[][] = [
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
function getHandNotation(row: number, col: number): string {
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
function holeCardsToGrid(cards: string[]): { row: number; col: number } {
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

interface StartingHandsChartProps {
  currentHand?: string[]
  compact?: boolean
  onClose?: () => void
}

const CATEGORY_COLORS: Record<HandCategory, string> = {
  premium: 'bg-green-600',
  strong: 'bg-yellow-600',
  playable: 'bg-orange-600',
  marginal: 'bg-orange-900',
  fold: 'bg-gray-700',
}

const LEGEND_ITEMS: { category: HandCategory; label: string }[] = [
  { category: 'premium', label: 'Premium' },
  { category: 'strong', label: 'Strong' },
  { category: 'playable', label: 'Playable' },
  { category: 'marginal', label: 'Marginal' },
  { category: 'fold', label: 'Fold' },
]

export function StartingHandsChart({ currentHand, compact, onClose }: StartingHandsChartProps) {
  const highlight = currentHand && currentHand.length === 2 ? holeCardsToGrid(currentHand) : null

  return (
    <div className="flex flex-col gap-2">
      {/* Grid */}
      <div className="grid grid-cols-[auto_repeat(13,1fr)] gap-px text-xs">
        {/* Empty top-left corner */}
        <div />
        {/* Column headers */}
        {HAND_LABELS.map((label) => (
          <div key={`col-${label}`} className="text-center text-gray-400 font-mono text-[10px] pb-0.5">
            {label}
          </div>
        ))}

        {/* Rows */}
        {STARTING_HAND_CATEGORIES.map((row, rowIdx) => (
          <Fragment key={`row-${rowIdx}`}>
            {/* Row header */}
            <div className="text-right text-gray-400 font-mono text-[10px] pr-1 flex items-center justify-end">
              {HAND_LABELS[rowIdx]}
            </div>
            {/* Cells */}
            {row.map((category, colIdx) => {
              const isHighlighted = highlight !== null && highlight.row === rowIdx && highlight.col === colIdx
              const notation = getHandNotation(rowIdx, colIdx)
              return (
                <div
                  key={`${rowIdx}-${colIdx}`}
                  className={`${CATEGORY_COLORS[category]} aspect-square flex items-center justify-center rounded-sm ${isHighlighted ? 'ring-2 ring-white' : ''}`}
                  title={notation}
                  data-testid="hand-cell"
                >
                  {!compact && (
                    <span className="text-white text-[8px] leading-none font-mono">{notation}</span>
                  )}
                </div>
              )
            })}
          </Fragment>
        ))}
      </div>

      {/* Legend (non-compact only) */}
      {!compact && (
        <div className="flex gap-3 justify-center text-xs text-gray-300">
          {LEGEND_ITEMS.map(({ category, label }) => (
            <div key={category} className="flex items-center gap-1">
              <div className={`w-3 h-3 rounded-sm ${CATEGORY_COLORS[category]}`} />
              <span>{label}</span>
            </div>
          ))}
        </div>
      )}

      {/* Close button (non-compact only) */}
      {!compact && onClose && (
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white text-sm self-center mt-1"
        >
          Close
        </button>
      )}
    </div>
  )
}
