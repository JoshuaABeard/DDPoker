/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useContext } from 'react'
import { GameContext } from './GameContext'
import type { GameActions } from './GameContext'
import type { GameState } from './gameReducer'

/**
 * Returns the full game state. Must be used inside a `<GameProvider>`.
 */
export function useGameState(): GameState {
  const ctx = useContext(GameContext)
  if (!ctx) {
    throw new Error('useGameState must be used inside a GameProvider')
  }
  return ctx.state
}

/**
 * Returns the game action dispatchers. Must be used inside a `<GameProvider>`.
 */
export function useGameActions(): GameActions {
  const ctx = useContext(GameContext)
  if (!ctx) {
    throw new Error('useGameActions must be used inside a GameProvider')
  }
  return ctx.actions
}
