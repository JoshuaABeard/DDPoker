/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'
import { formatChips } from '@/lib/utils'

interface HandHistoryProps {
  entries: HandHistoryEntry[]
}

function renderEntry(entry: HandHistoryEntry): string {
  switch (entry.type) {
    case 'hand_start':
      return `Hand #${entry.handNumber}`
    case 'community':
      return `${entry.round ?? 'Dealt'}: ${(entry.cards ?? []).join(' ')}`
    case 'action': {
      const amountStr = entry.amount && entry.amount > 0 ? ` ${formatChips(entry.amount)}` : ''
      return `${entry.playerName ?? 'Player'}: ${entry.action ?? ''}${amountStr}`
    }
    case 'result': {
      const winner = entry.winners?.[0]
      if (winner) {
        return `${winner.playerName} wins ${formatChips(winner.amount)} with ${winner.hand}`
      }
      return 'Hand complete'
    }
    default:
      return ''
  }
}

/**
 * Collapsible hand history panel.
 *
 * XSS safety: all entry text rendered as text nodes only.
 */
export function HandHistory({ entries }: HandHistoryProps) {
  const [open, setOpen] = useState(false)

  return (
    <div className="bg-gray-900 bg-opacity-90 rounded-xl shadow-xl w-64">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-controls="hand-history-log"
        className="flex items-center justify-between w-full px-3 py-2 text-sm font-semibold text-gray-300 hover:text-white"
      >
        <span>Hand History</span>
        <span>{open ? '▾' : '▸'}</span>
      </button>

      {open && (
        <div
          id="hand-history-log"
          className="overflow-y-auto max-h-60 px-2 pb-2 space-y-0.5"
          role="log"
          aria-label="Hand history"
        >
          {entries.length === 0 ? (
            <p className="text-gray-500 text-xs text-center py-2">No history yet</p>
          ) : (
            entries.map((entry) => (
              <div key={entry.id} className="text-xs text-gray-300">
                {renderEntry(entry)}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}
