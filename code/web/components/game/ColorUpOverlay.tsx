/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Card } from './Card'
import { formatChips } from '@/lib/utils'
import type { ColorUpStartedData } from '@/lib/game/types'

interface ColorUpOverlayProps {
  data: ColorUpStartedData
  seatNames: Record<number, string>
}

/**
 * Modal overlay showing chip race (color-up) results.
 *
 * XSS safety: all user strings rendered as text nodes.
 */
export function ColorUpOverlay({ data, seatNames }: ColorUpOverlayProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-60"
      role="dialog"
      aria-modal="true"
      aria-label="Chip Race"
    >
      <div className="bg-gray-800 rounded-2xl shadow-2xl p-6 max-w-sm w-full mx-4 text-white text-center">
        <h2 className="text-xl font-bold mb-2">Chip Race</h2>
        <p className="text-gray-300 mb-4">
          New minimum chip: {formatChips(data.newMinChip)}
        </p>
        <div className="space-y-3">
          {data.players.map((player) => {
            const name = seatNames[player.playerId] ?? `Player ${player.playerId}`
            let resultLabel: React.ReactNode
            if (player.won) {
              resultLabel = <span className="text-green-400">Won</span>
            } else if (player.broke) {
              resultLabel = <span className="text-red-400">Broke</span>
            } else {
              resultLabel = <span className="text-gray-400">&mdash;</span>
            }
            return (
              <div key={player.playerId} className="flex items-center justify-between gap-2">
                <span className="font-semibold text-sm truncate">{name}</span>
                <div className="flex items-center gap-1">
                  {player.cards.map((card) => (
                    <Card key={card} card={card} width={28} />
                  ))}
                </div>
                <span className="text-sm">{resultLabel}</span>
                <span className="text-gray-300 text-sm tabular-nums">{formatChips(player.finalChips)}</span>
              </div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
