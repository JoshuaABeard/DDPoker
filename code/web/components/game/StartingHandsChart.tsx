/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { Fragment } from 'react'
import { HAND_LABELS, STARTING_HAND_CATEGORIES, getHandNotation, holeCardsToGrid } from '@/lib/poker/startingHands'
import type { HandCategory } from '@/lib/poker/startingHands'

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
