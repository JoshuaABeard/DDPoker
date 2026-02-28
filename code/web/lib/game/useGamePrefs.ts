/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

export interface GamePrefs {
  fourColorDeck: boolean
  checkFold: boolean
  disableShortcuts: boolean
  dealerChat: 'all' | 'actions' | 'none'
}

export const DEFAULT_GAME_PREFS: GamePrefs = {
  fourColorDeck: false,
  checkFold: false,
  disableShortcuts: false,
  dealerChat: 'all',
}

const STORAGE_KEY = 'ddpoker-game-prefs'

function load(): GamePrefs {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      return { ...DEFAULT_GAME_PREFS, ...parsed }
    }
  } catch { /* ignore */ }
  return { ...DEFAULT_GAME_PREFS }
}

export function useGamePrefs() {
  const [prefs, setPrefs] = useState<GamePrefs>(load)

  const setPref = useCallback(<K extends keyof GamePrefs>(key: K, value: GamePrefs[K]) => {
    setPrefs((prev) => {
      const next = { ...prev, [key]: value }
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(next)) } catch { /* ignore */ }
      return next
    })
  }, [])

  return { prefs, setPref }
}
