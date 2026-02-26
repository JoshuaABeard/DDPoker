/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { BlindsData } from '@/lib/game/types'
import { formatChips } from '@/lib/utils'

interface TournamentInfoBarProps {
  level: number
  blinds: BlindsData
  /** Seconds until next level change, or null. */
  nextLevelIn: number | null
  /** Total player count remaining. */
  playerCount: number
  totalPlayers?: number
  playerRank?: number
  /** Game name — rendered as text, never HTML. */
  gameName: string
}

function formatTime(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

/**
 * Top info bar for the game table: level, blinds, next-level timer, players remaining.
 *
 * XSS safety: gameName rendered as text node only.
 */
export function TournamentInfoBar({ level, blinds, nextLevelIn, playerCount, totalPlayers, playerRank, gameName }: TournamentInfoBarProps) {
  return (
    <div className="flex items-center justify-between px-4 py-2 bg-gray-900 bg-opacity-80 text-white text-sm">
      <div className="font-semibold text-gray-300 truncate max-w-[200px]">{gameName}</div>

      <div className="flex gap-4 items-center">
        <div>
          <span className="text-gray-400 text-xs">Level </span>
          <span className="font-bold">{level}</span>
        </div>

        <div>
          <span className="text-gray-400 text-xs">Blinds </span>
          <span className="font-bold">
            {formatChips(blinds.small)}/{formatChips(blinds.big)}
            {blinds.ante > 0 && ` (${formatChips(blinds.ante)})`}
          </span>
        </div>

        {nextLevelIn != null && (
          <div>
            <span className="text-gray-400 text-xs">Next </span>
            <span className="font-bold text-yellow-400">{formatTime(nextLevelIn)}</span>
          </div>
        )}

        {totalPlayers != null ? (
          <div>
            <span className="text-gray-400 text-xs">Players </span>
            <span className="font-bold">{playerCount}/{totalPlayers}</span>
          </div>
        ) : (
          <div>
            <span className="text-gray-400 text-xs">Players </span>
            <span className="font-bold">{playerCount}</span>
          </div>
        )}

        {playerRank != null && (
          <div>
            <span className="text-gray-400 text-xs">Rank </span>
            <span className="font-bold">{playerRank}</span>
          </div>
        )}
      </div>
    </div>
  )
}
