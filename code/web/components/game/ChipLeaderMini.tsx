/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { formatChips } from '@/lib/utils'

interface Player {
  playerId: number
  name: string
  chipCount: number
}

interface ChipLeaderMiniProps {
  players: Player[]
  myPlayerId: number | null
}

function ordinal(n: number): string {
  const s = ['th', 'st', 'nd', 'rd']
  const v = n % 100
  return n + (s[(v - 20) % 10] || s[v] || s[0])
}

/**
 * Compact always-visible chip standings.
 * Shows top 3 (or all if <=5 remain), with local player rank if not shown.
 * XSS safety: all names rendered as text nodes only.
 */
export function ChipLeaderMini({ players, myPlayerId }: ChipLeaderMiniProps) {
  if (players.length === 0) return null

  const sorted = [...players].sort((a, b) => b.chipCount - a.chipCount)
  const showAll = sorted.length <= 5
  const displayed = showAll ? sorted : sorted.slice(0, 3)
  const myRank = sorted.findIndex((p) => p.playerId === myPlayerId) + 1
  const myInDisplayed = displayed.some((p) => p.playerId === myPlayerId)

  return (
    <div className="bg-gray-900 bg-opacity-80 rounded-lg px-2 py-1.5 text-xs shadow-md min-w-[120px]">
      {displayed.map((p, i) => (
        <div
          key={p.playerId}
          className={`flex items-center gap-1 ${p.playerId === myPlayerId ? 'text-blue-300' : 'text-gray-300'}`}
        >
          <span className="text-gray-500 w-4">{i + 1}.</span>
          <span className="flex-1 truncate max-w-[70px]">{p.name}</span>
          <span className="text-yellow-300 font-semibold">{formatChips(p.chipCount)}</span>
        </div>
      ))}
      {!myInDisplayed && myRank > 0 && (
        <div className="text-blue-300 border-t border-gray-700 mt-1 pt-1 flex items-center gap-1">
          <span className="text-gray-500 w-4">{ordinal(myRank)}</span>
          <span className="flex-1 truncate max-w-[70px]">You</span>
          <span className="text-yellow-300 font-semibold">
            {formatChips(sorted[myRank - 1].chipCount)}
          </span>
        </div>
      )}
    </div>
  )
}
