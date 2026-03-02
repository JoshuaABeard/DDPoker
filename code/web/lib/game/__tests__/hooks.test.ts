/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { renderHook } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { useGameState, useGameActions } from '../hooks'

describe('useGameState', () => {
  it('throws when used outside GameProvider', () => {
    expect(() => renderHook(() => useGameState())).toThrow(
      'useGameState must be used inside a GameProvider',
    )
  })
})

describe('useGameActions', () => {
  it('throws when used outside GameProvider', () => {
    expect(() => renderHook(() => useGameActions())).toThrow(
      'useGameActions must be used inside a GameProvider',
    )
  })
})
