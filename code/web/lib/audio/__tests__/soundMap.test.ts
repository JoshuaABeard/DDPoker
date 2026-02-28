/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { soundMap, ALL_SOUND_FILES } from '../soundMap'

describe('soundMap', () => {
  it('maps all expected sound names', () => {
    const expected = ['bet', 'check', 'raise', 'shuffle', 'shuffleShort', 'cheers', 'bell', 'attention', 'click', 'camera']
    for (const name of expected) {
      expect(soundMap[name]).toBeDefined()
      expect(soundMap[name].length).toBeGreaterThan(0)
    }
  })

  it('has 10 bet variants', () => {
    expect(soundMap.bet).toHaveLength(10)
  })

  it('exports a flat list of all unique file names', () => {
    expect(ALL_SOUND_FILES.length).toBeGreaterThan(0)
    expect(new Set(ALL_SOUND_FILES).size).toBe(ALL_SOUND_FILES.length)
  })
})
