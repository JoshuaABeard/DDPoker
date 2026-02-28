/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { RANKS, SUITS } from '@/lib/poker/types'

interface CardPickerProps {
  /** Cards already in use (disabled in picker) */
  usedCards: string[]
  /** Called when a card is selected */
  onSelect: (card: string) => void
  onClose: () => void
}

const SUIT_SYMBOLS: Record<string, string> = {
  h: '\u2665',
  d: '\u2666',
  c: '\u2663',
  s: '\u2660',
}

const SUIT_COLORS: Record<string, string> = {
  h: 'text-red-500',
  d: 'text-red-500',
  c: 'text-gray-200',
  s: 'text-gray-200',
}

/** Ranks displayed high-to-low: A, K, Q, ..., 2 */
const DISPLAY_RANKS = [...RANKS].reverse()

export function CardPicker({ usedCards, onSelect, onClose }: CardPickerProps) {
  const usedSet = new Set(usedCards)

  return (
    <div className="flex flex-col gap-2 bg-gray-800 rounded-lg p-3">
      <div className="grid grid-cols-[auto_repeat(13,1fr)] gap-px text-xs">
        {SUITS.map((suit) => {
          const color = SUIT_COLORS[suit]
          return (
            <div key={suit} className="contents">
              {/* Row header: suit symbol */}
              <div className={`flex items-center justify-center pr-1 text-sm ${color}`}>
                {SUIT_SYMBOLS[suit]}
              </div>
              {/* Card cells */}
              {DISPLAY_RANKS.map((rank) => {
                const card = `${rank}${suit}`
                const isUsed = usedSet.has(card)
                return (
                  <button
                    key={card}
                    type="button"
                    disabled={isUsed}
                    onClick={() => onSelect(card)}
                    className={`font-mono text-center py-1 px-0.5 rounded-sm ${color} ${
                      isUsed
                        ? 'opacity-30 cursor-not-allowed'
                        : 'hover:bg-gray-600 cursor-pointer'
                    }`}
                    data-testid={`card-${card}`}
                  >
                    {rank}
                  </button>
                )
              })}
            </div>
          )
        })}
      </div>

      <button
        onClick={onClose}
        className="text-gray-400 hover:text-white text-sm self-center mt-1"
      >
        Close
      </button>
    </div>
  )
}
