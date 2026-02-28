/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { PotData, SeatData } from '@/lib/game/types'
import { formatChips } from '@/lib/utils'

interface PotDisplayProps {
  pots: PotData[]
  seats?: SeatData[]
}

/**
 * Displays main pot and any side pots.
 */
export function PotDisplay({ pots, seats }: PotDisplayProps) {
  if (pots.length === 0) return null

  const mainPot = pots[0]
  const sidePots = pots.slice(1)

  function playerCount(pot: PotData): string {
    if (!seats || pot.eligiblePlayers.length === 0) return ''
    return ` (${pot.eligiblePlayers.length} player${pot.eligiblePlayers.length !== 1 ? 's' : ''})`
  }

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="text-yellow-300 font-bold text-sm">
        Pot: {formatChips(mainPot.amount)}{playerCount(mainPot)}
      </div>
      {sidePots.map((pot, i) => (
        <div key={i} className="text-yellow-200 text-xs">
          Side Pot {i + 1}: {formatChips(pot.amount)}{playerCount(pot)}
        </div>
      ))}
    </div>
  )
}
