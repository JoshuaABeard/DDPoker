/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

export const AVATAR_IDS = [
  'bear', 'eagle', 'fox', 'wolf', 'shark', 'owl',
  'crown', 'diamond', 'spade', 'star', 'flame', 'lightning',
] as const
export type AvatarId = (typeof AVATAR_IDS)[number]

const STORAGE_KEY = 'ddpoker-avatar'
const DEFAULT_ID: AvatarId = 'spade'

function load(): AvatarId {
  try {
    const id = localStorage.getItem(STORAGE_KEY) as AvatarId
    if (AVATAR_IDS.includes(id)) return id
  } catch { /* ignore */ }
  return DEFAULT_ID
}

export function useAvatar() {
  const [avatarId, setId] = useState<AvatarId>(load)

  const setAvatar = useCallback((id: AvatarId) => {
    if (!AVATAR_IDS.includes(id)) return
    setId(id)
    try { localStorage.setItem(STORAGE_KEY, id) } catch { /* ignore */ }
  }, [])

  return { avatarId, setAvatar }
}
