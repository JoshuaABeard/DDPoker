/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'

interface DashboardWidgetProps {
  title: string
  defaultExpanded?: boolean
  children: React.ReactNode
}

export function DashboardWidget({ title, defaultExpanded = true, children }: DashboardWidgetProps) {
  const [expanded, setExpanded] = useState(defaultExpanded)

  return (
    <section className="border-b border-gray-700/50 last:border-b-0">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        className="flex w-full items-center justify-between px-3 py-1.5 text-xs font-semibold text-gray-400 uppercase hover:text-gray-200 transition-colors"
        aria-expanded={expanded}
      >
        <span>{title}</span>
        <span className="text-[10px]">{expanded ? '\u25B2' : '\u25BC'}</span>
      </button>
      {expanded && <div className="px-3 pb-2">{children}</div>}
    </section>
  )
}
