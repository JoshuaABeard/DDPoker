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
  const prevRef = useRef({ lastEntryId: handHistory.at(-1)?.id ?? null, isMyTurn })

  useEffect(() => {
    const prev = prevRef.current

    // Find new entries since last seen (tracks by ID, not length,
    // because the reducer trims handHistory to 200 entries via FIFO)
    let startIdx = 0
    if (prev.lastEntryId) {
      const idx = handHistory.findIndex((e) => e.id === prev.lastEntryId)
      startIdx = idx >= 0 ? idx + 1 : 0
    }
    const newEntries = handHistory.slice(startIdx)
    if (newEntries.length > 0) {
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

    prevRef.current = { lastEntryId: handHistory.at(-1)?.id ?? null, isMyTurn }
  }, [handHistory, isMyTurn, playSound])
}
