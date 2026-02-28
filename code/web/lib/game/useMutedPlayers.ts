/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

const STORAGE_KEY = 'ddpoker-muted-players'

function loadMuted(): Set<number> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return new Set(JSON.parse(raw) as number[])
  } catch {
    /* ignore corrupt data */
  }
  return new Set()
}

function saveMuted(ids: Set<number>): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...ids]))
  } catch {
    /* localStorage unavailable */
  }
}

export function useMutedPlayers() {
  const [mutedIds, setMutedIds] = useState<Set<number>>(() => loadMuted())

  const mute = useCallback((playerId: number) => {
    setMutedIds((prev) => {
      const next = new Set(prev)
      next.add(playerId)
      saveMuted(next)
      return next
    })
  }, [])

  const unmute = useCallback((playerId: number) => {
    setMutedIds((prev) => {
      const next = new Set(prev)
      next.delete(playerId)
      saveMuted(next)
      return next
    })
  }, [])

  const isMuted = useCallback((playerId: number) => mutedIds.has(playerId), [mutedIds])

  return { mutedIds, mute, unmute, isMuted }
}
