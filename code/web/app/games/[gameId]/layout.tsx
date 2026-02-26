/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useParams } from 'next/navigation'
import { GameProvider } from '@/lib/game/GameContext'
import { config } from '@/lib/config'

export default function GameLayout({ children }: { children: React.ReactNode }) {
  const params = useParams()
  const gameId = params.gameId as string

  return (
    <GameProvider gameId={gameId} serverBaseUrl={config.apiBaseUrl}>
      {children}
    </GameProvider>
  )
}
