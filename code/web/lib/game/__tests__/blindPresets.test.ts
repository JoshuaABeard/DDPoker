/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { describe, it, expect } from 'vitest'
import { BLIND_PRESETS } from '../blindPresets'

describe('blindPresets', () => {
  it('has 3 presets: turbo, standard, deep-stack', () => {
    expect(BLIND_PRESETS).toHaveLength(3)
    expect(BLIND_PRESETS.map((p) => p.id)).toEqual(['turbo', 'standard', 'deep-stack'])
  })

  it('turbo has 8 levels with 3 min each', () => {
    const turbo = BLIND_PRESETS.find((p) => p.id === 'turbo')!
    expect(turbo.levels).toHaveLength(8)
    expect(turbo.levels[0].minutes).toBe(3)
  })

  it('standard has 10 levels with 5 min each', () => {
    const standard = BLIND_PRESETS.find((p) => p.id === 'standard')!
    expect(standard.levels).toHaveLength(10)
    expect(standard.levels[0].minutes).toBe(5)
  })

  it('deep-stack has 12 levels with 8 min each', () => {
    const deep = BLIND_PRESETS.find((p) => p.id === 'deep-stack')!
    expect(deep.levels).toHaveLength(12)
    expect(deep.levels[0].minutes).toBe(8)
  })

  it('all presets have increasing blinds', () => {
    for (const preset of BLIND_PRESETS) {
      for (let i = 1; i < preset.levels.length; i++) {
        expect(preset.levels[i].bigBlind).toBeGreaterThanOrEqual(preset.levels[i - 1].bigBlind)
      }
    }
  })
})
