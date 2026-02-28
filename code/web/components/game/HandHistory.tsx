/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'
import { formatChips, formatHandHistoryForExport } from '@/lib/utils'

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

  function handleExport() {
    const text = formatHandHistoryForExport(entries)
    if (!text) return
    const blob = new Blob([text], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `hand-history-${Date.now()}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="bg-gray-900 bg-opacity-90 rounded-xl shadow-xl w-64">
      <div className="flex items-center justify-between w-full px-3 py-2">
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          aria-controls="hand-history-log"
          className="flex items-center gap-1 text-sm font-semibold text-gray-300 hover:text-white"
        >
          <span>Hand History</span>
          <span>{open ? '▾' : '▸'}</span>
        </button>
        {entries.length > 0 && (
          <button
            type="button"
            onClick={handleExport}
            aria-label="Export hand history"
            className="text-gray-500 hover:text-gray-300 text-xs"
            title="Export hand history"
          >
            Export
          </button>
        )}
      </div>

      <div
        id="hand-history-log"
        className={`overflow-y-auto max-h-60 px-2 pb-2 space-y-0.5${open ? '' : ' hidden'}`}
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
    </div>
  )
}
