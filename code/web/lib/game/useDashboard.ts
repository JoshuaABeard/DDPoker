/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

export interface WidgetConfig {
  id: string
  label: string
  visible: boolean
}

export const DEFAULT_WIDGETS: WidgetConfig[] = [
  { id: 'hand-strength', label: 'Hand Strength', visible: true },
  { id: 'pot-odds', label: 'Pot Odds', visible: true },
  { id: 'tournament-info', label: 'Tournament Info', visible: true },
  { id: 'rank', label: 'Rank', visible: true },
  { id: 'starting-hand', label: 'Starting Hand', visible: true },
]

const STORAGE_KEY = 'ddpoker-dashboard'

function load(): WidgetConfig[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed
      }
    }
  } catch { /* ignore */ }
  return DEFAULT_WIDGETS.map((w) => ({ ...w }))
}

export function useDashboard() {
  const [widgets, setWidgets] = useState<WidgetConfig[]>(load)

  const toggleWidget = useCallback((id: string) => {
    setWidgets((prev) => {
      const next = prev.map((w) => (w.id === id ? { ...w, visible: !w.visible } : w))
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)) } catch { /* ignore */ }
      return next
    })
  }, [])

  const moveWidget = useCallback((id: string, direction: 'up' | 'down') => {
    setWidgets((prev) => {
      const idx = prev.findIndex((w) => w.id === id)
      if (idx < 0) return prev
      const swapIdx = direction === 'up' ? idx - 1 : idx + 1
      if (swapIdx < 0 || swapIdx >= prev.length) return prev
      const next = [...prev]
      ;[next[idx], next[swapIdx]] = [next[swapIdx], next[idx]]
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)) } catch { /* ignore */ }
      return next
    })
  }, [])

  const resetToDefaults = useCallback(() => {
    const defaults = DEFAULT_WIDGETS.map((w) => ({ ...w }))
    setWidgets(defaults)
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(defaults)) } catch { /* ignore */ }
  }, [])

  return { widgets, toggleWidget, moveWidget, resetToDefaults }
}
