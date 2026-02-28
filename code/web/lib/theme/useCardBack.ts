/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useCallback, useState } from 'react'

export const CARD_BACK_IDS = ['classic-red', 'blue-diamond', 'green-celtic', 'gold-royal'] as const
export type CardBackId = (typeof CARD_BACK_IDS)[number]

const STORAGE_KEY = 'ddpoker-card-back'
const DEFAULT_ID: CardBackId = 'classic-red'

function load(): CardBackId {
  try {
    const id = localStorage.getItem(STORAGE_KEY) as CardBackId
    if (CARD_BACK_IDS.includes(id)) return id
  } catch { /* ignore */ }
  return DEFAULT_ID
}

export function useCardBack() {
  const [cardBackId, setId] = useState<CardBackId>(load)

  const setCardBack = useCallback((id: CardBackId) => {
    if (!CARD_BACK_IDS.includes(id)) return
    setId(id)
    try { localStorage.setItem(STORAGE_KEY, id) } catch { /* ignore */ }
  }, [])

  return { cardBackId, setCardBack }
}
