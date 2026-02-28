/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useRef } from 'react'
import { useAudio } from './useAudio'
import type { HandHistoryEntry } from '@/lib/game/gameReducer'

/**
 * Watches game state changes and plays appropriate sound effects.
 *
 * @param handHistory - Current hand history entries array
 * @param isMyTurn - Whether the local player must act now
 */
export function useSoundEffects(
  handHistory: HandHistoryEntry[],
  isMyTurn: boolean,
): void {
  const { playSound } = useAudio()
  const prevRef = useRef({ historyLength: handHistory.length, isMyTurn })

  useEffect(() => {
    const prev = prevRef.current

    // Process new hand history entries
    if (handHistory.length > prev.historyLength) {
      const newEntries = handHistory.slice(prev.historyLength)
      for (const e of newEntries) {
        switch (e.type) {
          case 'hand_start':
            playSound('shuffle')
            break
          case 'action':
            if (e.action === 'CHECK') playSound('check')
            else if (e.action === 'RAISE') playSound('raise')
            else if (e.action === 'FOLD') playSound('click')
            else playSound('bet') // BET, CALL, ALL_IN
            break
          case 'community':
            playSound('shuffleShort')
            break
          case 'result':
            playSound('cheers')
            break
        }
      }
    }

    // Your turn notification
    if (isMyTurn && !prev.isMyTurn) {
      playSound('bell')
    }

    prevRef.current = { historyLength: handHistory.length, isMyTurn }
  }, [handHistory, isMyTurn, playSound])
}
