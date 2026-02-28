/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Card } from './Card'
import { DealerButton } from './DealerButton'
import type { SeatData } from '@/lib/game/types'
import { formatChips } from '@/lib/utils'

interface PlayerSeatProps {
  seat: SeatData
  isMe: boolean
  /** CSS position override for oval layout (e.g. { top: '10%', left: '50%' }) */
  positionStyle: React.CSSProperties
  /** Whether the local player is the game owner/admin */
  isAdmin?: boolean
  /** Called when admin clicks kick on this seat */
  onKick?: (playerId: number) => void
}

/**
 * Renders a single player seat around the poker table oval.
 *
 * XSS safety: playerName and status strings are rendered as React text nodes only.
 */
export function PlayerSeat({ seat, isMe, positionStyle, isAdmin, onKick }: PlayerSeatProps) {
  const { playerName, chipCount, status, isDealer, isSmallBlind, isBigBlind,
          currentBet, holeCards, isCurrentActor } = seat

  const isFolded = status === 'FOLDED'
  const isAllIn = status === 'ALL_IN'
  const isEmpty = !playerName

  if (isEmpty) return null

  return (
    <div
      className="absolute flex flex-col items-center gap-0.5"
      style={{ transform: 'translate(-50%, -50%)', ...positionStyle }}
      role="region"
      aria-label={`${playerName}'s seat`}
    >
      {/* Hole cards — hidden for folded/sat-out/eliminated players; face-down for active others */}
      <div className="flex gap-0.5">
        {isMe ? (
          holeCards.map((card, i) => <Card key={i} card={card} width={40} />)
        ) : !isFolded && status !== 'SAT_OUT' ? (
          <>
            <Card width={40} />
            <Card width={40} />
          </>
        ) : null}
      </div>

      {/* Player info box */}
      <div
        className={[
          'rounded-lg px-2 py-1 text-center text-xs min-w-[80px] shadow-md transition-all',
          isFolded ? 'opacity-40 bg-gray-700' : 'bg-gray-800',
          isCurrentActor ? 'ring-2 ring-yellow-400 animate-pulse' : '',
          isMe ? 'border border-blue-400' : '',
        ].join(' ')}
      >
        {/* Name — text node only, no dangerouslySetInnerHTML */}
        <div className="font-semibold text-white truncate max-w-[80px]">{playerName}</div>
        <div className="text-gray-300">{formatChips(chipCount)}</div>

        {/* Status badges */}
        <div className="flex justify-center gap-1 mt-0.5">
          {isDealer && <DealerButton type="dealer" />}
          {isSmallBlind && <DealerButton type="small-blind" />}
          {isBigBlind && <DealerButton type="big-blind" />}
          {isAllIn && (
            <span className="text-[9px] bg-red-600 text-white rounded px-1">ALL IN</span>
          )}
          {isFolded && (
            <span className="text-[9px] bg-gray-600 text-gray-300 rounded px-1">FOLDED</span>
          )}
          {status === 'SAT_OUT' && (
            <span className="text-[9px] text-yellow-400 italic rounded px-1">Sitting Out</span>
          )}
          {status === 'DISCONNECTED' && (
            <span className="text-[9px] text-red-400 italic rounded px-1">Disconnected</span>
          )}
        </div>
        {isAdmin && !isMe && onKick && (
          <button
            type="button"
            onClick={(e) => { e.stopPropagation(); onKick(seat.playerId) }}
            aria-label={`Kick ${playerName}`}
            className="text-[9px] text-red-400 hover:text-red-300 mt-0.5"
          >
            Kick
          </button>
        )}
      </div>

      {/* Current bet */}
      {currentBet > 0 && (
        <div className="text-yellow-300 text-xs font-bold">{formatChips(currentBet)}</div>
      )}
    </div>
  )
}
