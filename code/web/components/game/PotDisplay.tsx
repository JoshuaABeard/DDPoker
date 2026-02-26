/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { PotData } from '@/lib/game/types'
import { formatChips } from '@/lib/utils'

interface PotDisplayProps {
  pots: PotData[]
}

/**
 * Displays main pot and any side pots.
 */
export function PotDisplay({ pots }: PotDisplayProps) {
  if (pots.length === 0) return null

  const mainPot = pots[0]
  const sidePots = pots.slice(1)

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="text-yellow-300 font-bold text-sm">
        Pot: {formatChips(mainPot.amount)}
      </div>
      {sidePots.map((pot, i) => (
        <div key={i} className="text-yellow-200 text-xs">
          Side Pot {i + 1}: {formatChips(pot.amount)}
        </div>
      ))}
    </div>
  )
}
