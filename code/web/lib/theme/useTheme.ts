/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'
import { THEMES, DEFAULT_THEME_ID } from './themes'
import type { ThemeColors } from './themes'

const STORAGE_KEY = 'ddpoker-theme'

function loadThemeId(): string {
  try {
    const id = localStorage.getItem(STORAGE_KEY)
    if (id && THEMES[id]) return id
  } catch { /* ignore */ }
  return DEFAULT_THEME_ID
}

export function useTheme(): {
  themeId: string
  colors: ThemeColors
  setTheme: (id: string) => void
} {
  const [themeId, setThemeId] = useState(loadThemeId)

  const setTheme = useCallback((id: string) => {
    if (!THEMES[id]) return
    setThemeId(id)
    try { localStorage.setItem(STORAGE_KEY, id) } catch { /* ignore */ }
  }, [])

  return { themeId, colors: THEMES[themeId]?.colors ?? THEMES[DEFAULT_THEME_ID].colors, setTheme }
}
