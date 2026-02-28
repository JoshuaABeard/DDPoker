/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { GameConfigDto } from './types'

type BlindLevel = GameConfigDto['blindStructure'][number]

export interface BlindPreset {
  id: string
  name: string
  description: string
  levels: BlindLevel[]
}

function level(sb: number, bb: number, ante: number, minutes: number): BlindLevel {
  return { smallBlind: sb, bigBlind: bb, ante, minutes, isBreak: false, gameType: 'NOLIMIT_HOLDEM' }
}

export const BLIND_PRESETS: BlindPreset[] = [
  {
    id: 'turbo',
    name: 'Turbo',
    description: '8 levels, 3 min each — fast-paced games',
    levels: [
      level(25, 50, 0, 3), level(50, 100, 0, 3), level(100, 200, 25, 3), level(150, 300, 50, 3),
      level(200, 400, 50, 3), level(300, 600, 75, 3), level(500, 1000, 100, 3), level(1000, 2000, 200, 3),
    ],
  },
  {
    id: 'standard',
    name: 'Standard',
    description: '10 levels, 5 min each — balanced play',
    levels: [
      level(25, 50, 0, 5), level(50, 100, 0, 5), level(75, 150, 25, 5), level(100, 200, 25, 5),
      level(150, 300, 50, 5), level(200, 400, 50, 5), level(300, 600, 75, 5), level(400, 800, 100, 5),
      level(600, 1200, 200, 5), level(800, 1600, 200, 5),
    ],
  },
  {
    id: 'deep-stack',
    name: 'Deep Stack',
    description: '12 levels, 8 min each — slower, deeper strategy',
    levels: [
      level(10, 20, 0, 8), level(15, 30, 0, 8), level(25, 50, 0, 8), level(50, 100, 10, 8),
      level(75, 150, 20, 8), level(100, 200, 25, 8), level(150, 300, 40, 8), level(200, 400, 50, 8),
      level(250, 500, 75, 8), level(300, 600, 100, 8), level(400, 800, 100, 8), level(500, 1000, 150, 8),
    ],
  },
]
