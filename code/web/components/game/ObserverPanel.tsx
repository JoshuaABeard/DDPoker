/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'

interface Observer {
  observerId: number
  observerName: string
}

interface ObserverPanelProps {
  observers: Observer[]
}

/**
 * Expandable observer list.
 * Default: shows "{N} watching" counter.
 * On click: expands to show observer names.
 * XSS safety: observer names rendered as text nodes only.
 */
export function ObserverPanel({ observers }: ObserverPanelProps) {
  const [expanded, setExpanded] = useState(false)

  if (observers.length === 0) return null

  return (
    <div className="bg-gray-900 bg-opacity-80 rounded-lg shadow-md">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="px-2 py-1 text-gray-400 text-xs hover:text-gray-200 w-full text-left"
      >
        {observers.length} watching
      </button>
      {expanded && (
        <div className="px-2 pb-2 space-y-0.5">
          {observers.map((o) => (
            <div key={o.observerId} className="text-xs text-gray-300 pl-1">
              {o.observerName}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
