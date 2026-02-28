/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

export interface ThemeColors {
  center: string
  mid: string
  edge: string
  border: string
}

export interface Theme {
  id: string
  name: string
  colors: ThemeColors
}

export const THEMES: Record<string, Theme> = {
  'classic-green': {
    id: 'classic-green',
    name: 'Classic Green',
    colors: { center: '#2d5a1b', mid: '#1e3d12', edge: '#152d0d', border: '#1a2e0f' },
  },
  'royal-blue': {
    id: 'royal-blue',
    name: 'Royal Blue',
    colors: { center: '#1b3d5a', mid: '#122d3e', edge: '#0d1f2d', border: '#0f1e2e' },
  },
  'casino-red': {
    id: 'casino-red',
    name: 'Casino Red',
    colors: { center: '#5a1b1b', mid: '#3d1212', edge: '#2d0d0d', border: '#2e0f0f' },
  },
  'dark-night': {
    id: 'dark-night',
    name: 'Dark Night',
    colors: { center: '#2a2a3a', mid: '#1e1e2e', edge: '#141420', border: '#1a1a28' },
  },
  'wooden': {
    id: 'wooden',
    name: 'Wooden',
    colors: { center: '#5a3d1b', mid: '#3d2a12', edge: '#2d1f0d', border: '#2e1a0f' },
  },
}

export const DEFAULT_THEME_ID = 'classic-green'
