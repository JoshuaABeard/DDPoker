/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'

interface HandHistoryEntry {
  id: string
  text: string
}

interface HandHistoryProps {
  entries: HandHistoryEntry[]
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
        className="flex items-center justify-between w-full px-3 py-2 text-sm font-semibold text-gray-300 hover:text-white"
      >
        <span>Hand History</span>
        <span>{open ? '▾' : '▸'}</span>
      </button>

      {open && (
        <div
          className="overflow-y-auto max-h-60 px-2 pb-2 space-y-0.5"
          role="log"
          aria-label="Hand history"
        >
          {entries.length === 0 ? (
            <p className="text-gray-500 text-xs text-center py-2">No history yet</p>
          ) : (
            entries.map((entry) => (
              <div key={entry.id} className="text-xs text-gray-300">
                {/* text is a text node — XSS safe */}
                {entry.text}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}
