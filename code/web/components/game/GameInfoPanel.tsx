/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import type { BlindLevelConfig, BlindsSummary } from '@/lib/game/types'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export interface GameInfoPlayer {
  playerId: number
  name: string
  chipCount: number
}

export interface GameInfoPanelProps {
  gameName: string
  ownerName: string
  blinds: BlindsSummary
  /** Current blind level number (1-based). Highlights matching row in structure. */
  currentLevel?: number
  /** Player list for chip counts; sorted descending by chipCount. */
  players?: GameInfoPlayer[]
  /** Called when the close button is clicked. Omit to hide the close button. */
  onClose?: () => void
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}

function formatBlind(level: BlindLevelConfig): string {
  const ante = level.ante > 0 ? ` / ${formatChips(level.ante)}` : ''
  return `${formatChips(level.small)} / ${formatChips(level.big)}${ante}`
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

/**
 * Game information panel showing blind structure and chip counts.
 *
 * Used as a side panel in the play page (keyboard-toggled with I) and as an
 * inline section in the lobby page. XSS-safe: all strings are text nodes.
 */
export function GameInfoPanel({
  gameName,
  ownerName,
  blinds,
  currentLevel,
  players,
  onClose,
}: GameInfoPanelProps) {
  const sortedPlayers = players
    ? [...players].sort((a, b) => b.chipCount - a.chipCount)
    : null

  const levels = blinds.levels ?? []

  return (
    <div className="bg-gray-900 text-white rounded-xl shadow-2xl w-72 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700">
        <div>
          {/* Game name — text node only (XSS safe) */}
          <div className="font-bold text-sm truncate max-w-[180px]">{gameName}</div>
          <div className="text-xs text-gray-400 mt-0.5">Host: {ownerName}</div>
        </div>
        {onClose && (
          <button
            type="button"
            onClick={onClose}
            aria-label="Close game info"
            className="text-gray-400 hover:text-white text-lg leading-none ml-2"
          >
            ×
          </button>
        )}
      </div>

      {/* Current blinds */}
      <div className="px-4 py-3 border-b border-gray-700">
        <div className="text-xs text-gray-400 uppercase tracking-wide mb-1">
          {currentLevel != null ? `Level ${currentLevel} — Current Blinds` : 'Blinds'}
        </div>
        <div className="text-base font-semibold">
          {formatChips(blinds.small)} / {formatChips(blinds.big)}
          {blinds.ante > 0 && (
            <span className="text-sm text-gray-300 font-normal ml-1">
              (ante {formatChips(blinds.ante)})
            </span>
          )}
        </div>
      </div>

      {/* Blind structure */}
      {levels.length > 0 && (
        <div className="px-4 py-2 border-b border-gray-700">
          <div className="text-xs text-gray-400 uppercase tracking-wide mb-2">Blind Structure</div>
          <div className="space-y-0.5 max-h-40 overflow-y-auto pr-1">
            {levels.map((lvl) => {
              const isCurrent = currentLevel != null && lvl.level === currentLevel
              return (
                <div
                  key={lvl.level}
                  className={[
                    'flex items-center justify-between text-xs px-2 py-1 rounded',
                    isCurrent
                      ? 'bg-yellow-500 text-gray-900 font-bold'
                      : 'text-gray-300',
                  ].join(' ')}
                >
                  <span className="text-gray-400 mr-2 w-8">
                    {isCurrent ? '▶' : `L${lvl.level}`}
                  </span>
                  <span className="flex-1">{formatBlind(lvl)}</span>
                  <span className="text-gray-500 ml-2">{lvl.durationMinutes}m</span>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Player chip counts */}
      {sortedPlayers && sortedPlayers.length > 0 && (
        <div className="px-4 py-2 flex-1 overflow-y-auto">
          <div className="text-xs text-gray-400 uppercase tracking-wide mb-2">
            Players ({sortedPlayers.length})
          </div>
          <div className="space-y-0.5">
            {sortedPlayers.map((p, idx) => (
              <div key={p.playerId} className="flex items-center justify-between text-xs">
                <span className="text-gray-500 w-5">{idx + 1}.</span>
                {/* Player name — text node only (XSS safe) */}
                <span className="flex-1 text-gray-200 truncate">{p.name}</span>
                <span className="text-yellow-300 font-semibold ml-2">
                  {formatChips(p.chipCount)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
