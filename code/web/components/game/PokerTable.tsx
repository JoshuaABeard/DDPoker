/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useState } from 'react'
import { useGameState, useGameActions } from '@/lib/game/hooks'
import { TournamentInfoBar } from './TournamentInfoBar'
import { TableFelt } from './TableFelt'
import { PlayerSeat } from './PlayerSeat'
import { ActionPanel } from './ActionPanel'
import { ActionTimer } from './ActionTimer'
import { HandHistory } from './HandHistory'
import { ChatPanel } from './ChatPanel'
import { ObserverPanel } from './ObserverPanel'
import { useMutedPlayers } from '@/lib/game/useMutedPlayers'
import { useTheme } from '@/lib/theme/useTheme'
import { useCardBack } from '@/lib/theme/useCardBack'
import { useAvatar } from '@/lib/theme/useAvatar'
import { useSoundEffects } from '@/lib/audio/useSoundEffects'
import { ChipLeaderMini } from './ChipLeaderMini'
import { VolumeControl } from './VolumeControl'
import { ThemePicker } from './ThemePicker'
import { Dialog } from '@/components/ui/Dialog'

/**
 * 10 fixed seat positions around the oval (percentage coordinates).
 * Visual position 0 = bottom center — always the current player's seat.
 * Positions 1–9 are arranged clockwise around the table.
 */
const SEAT_POSITIONS: ReadonlyArray<{ top: string; left: string }> = [
  { top: '88%', left: '50%' }, // 0 — bottom center (me)
  { top: '79%', left: '24%' }, // 1 — bottom-left
  { top: '58%', left: '6%'  }, // 2 — left
  { top: '33%', left: '9%'  }, // 3 — upper-left
  { top: '12%', left: '27%' }, // 4 — top-left
  { top: '7%',  left: '50%' }, // 5 — top-center
  { top: '12%', left: '73%' }, // 6 — top-right
  { top: '33%', left: '91%' }, // 7 — upper-right
  { top: '58%', left: '94%' }, // 8 — right
  { top: '79%', left: '76%' }, // 9 — bottom-right
]

interface PokerTableProps {
  /** Game name rendered in the top info bar (text node only — XSS safe). */
  gameName: string
  /**
   * Optional overlay element rendered on top of the table.
   * The play page is responsible for rebuy/addon/eliminated overlays
   * since those events are not tracked in game state.
   */
  overlay?: React.ReactNode
}

/**
 * Full-screen poker table container.
 *
 * Reads all game state from GameContext (must be wrapped in GameProvider).
 * Rotates seat indices so the current player always appears at visual
 * position 0 (bottom center), with others arranged clockwise.
 *
 * The `overlay` prop slot lets the play page inject rebuy/addon/eliminated
 * modals without coupling this component to those lifecycle events.
 */
export function PokerTable({ gameName, overlay }: PokerTableProps) {
  const state = useGameState()
  const actions = useGameActions()
  const [chatOpen, setChatOpen] = useState(true)
  const { mutedIds, mute, unmute } = useMutedPlayers()
  const { colors: feltColors } = useTheme()
  const { cardBackId } = useCardBack()
  const { avatarId } = useAvatar()
  const [kickTarget, setKickTarget] = useState<{ playerId: number; playerName: string } | null>(null)

  const {
    currentTable,
    holeCards,
    actionRequired,
    actionTimeoutSeconds,
    actionTimer,
    myPlayerId,
    gameState,
    chatMessages,
  } = state

  useSoundEffects(state.handHistory, actionRequired != null)

  if (!currentTable || !gameState) {
    return (
      <div
        className="game-bg flex items-center justify-center w-full h-full"
        role="status"
        aria-label="Connecting to game"
      >
        <p className="text-gray-400 text-lg">Connecting to game...</p>
      </div>
    )
  }

  // Rotate seats so the current player always appears at visual position 0
  const myIndex = currentTable.seats.findIndex((s) => s.playerId === myPlayerId)
  const seatCount = Math.max(currentTable.seats.length, 1)

  function visualPosition(arrayIndex: number): number {
    const pos = myIndex < 0
      ? arrayIndex % SEAT_POSITIONS.length
      : ((arrayIndex - myIndex + seatCount) % seatCount) % SEAT_POSITIONS.length
    return pos >= 0 && pos < SEAT_POSITIONS.length ? pos : 0
  }

  const potTotal = currentTable.pots.reduce((sum, p) => sum + p.amount, 0)
  const showTimer = actionTimeoutSeconds != null

  // Task 6.7: find this player's seat to determine sit-out status
  const mySeat = currentTable.seats.find((s) => s.playerId === myPlayerId)
  const isSatOut = mySeat?.status === 'SAT_OUT'

  return (
    <div
      className="game-bg relative w-full h-full overflow-hidden"
      role="main"
      aria-label="Poker table"
    >
      {/* Tournament info bar — pinned to top */}
      <div className="absolute top-0 left-0 right-0 z-10">
        <TournamentInfoBar
          level={gameState.level}
          blinds={gameState.blinds}
          nextLevelIn={gameState.nextLevelIn}
          playerCount={state.playersRemaining > 0 ? state.playersRemaining : gameState.players.length}
          totalPlayers={state.totalPlayers > 0 ? state.totalPlayers : undefined}
          playerRank={state.playerRank > 0 ? state.playerRank : undefined}
          gameName={gameName}
        />
      </div>

      {/* Observer panel — shown when watchers are present */}
      <div className="absolute top-12 left-3 z-10">
        <ObserverPanel observers={state.observers} />
      </div>

      {/* Admin controls — visible to game owner only */}
      {state.isOwner && (
        <div className="absolute top-12 left-1/2 -translate-x-1/2 z-10 flex gap-2">
          {gameState.status === 'PAUSED' ? (
            <button
              type="button"
              onClick={actions.sendAdminResume}
              className="px-3 py-1 text-xs font-semibold rounded-lg bg-green-700 hover:bg-green-600 text-white"
            >
              Resume Game
            </button>
          ) : (
            <button
              type="button"
              onClick={actions.sendAdminPause}
              className="px-3 py-1 text-xs font-semibold rounded-lg bg-yellow-700 hover:bg-yellow-600 text-white"
            >
              Pause Game
            </button>
          )}
        </div>
      )}

      {/* Oval felt surface (community cards + pots) */}
      <TableFelt table={currentTable} colors={feltColors} />

      {/* Player seats — rotated so my seat is always at position 0 */}
      {currentTable.seats.map((seat, arrayIndex) => (
        <PlayerSeat
          key={seat.playerId ?? arrayIndex}
          seat={{
            ...seat,
            // Reveal hole cards for the current player only
            holeCards: seat.playerId === myPlayerId ? holeCards : [],
          }}
          isMe={seat.playerId === myPlayerId}
          positionStyle={SEAT_POSITIONS[visualPosition(arrayIndex)]}
          isAdmin={state.isOwner}
          onKick={(playerId) => {
            const name = currentTable.seats.find((s) => s.playerId === playerId)?.playerName ?? 'Player'
            setKickTarget({ playerId, playerName: name })
          }}
          cardBackId={cardBackId}
          avatarId={seat.playerId === myPlayerId ? avatarId : undefined}
        />
      ))}

      {/* Action timer — shown when any player has an active countdown */}
      {showTimer && (
        <div className="absolute bottom-[13%] left-1/2 -translate-x-1/2 w-52 z-20">
          <ActionTimer
            totalSeconds={actionTimeoutSeconds}
            remainingSeconds={actionTimer?.secondsRemaining ?? null}
          />
        </div>
      )}

      {/* Action panel — shown only when it's this player's turn */}
      {actionRequired && (
        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 z-20">
          <ActionPanel
            options={actionRequired}
            potSize={potTotal}
            onAction={actions.sendAction}
          />
        </div>
      )}

      {/* Task 6.7: sit-out / come-back toggle — shown when player has a seat */}
      {mySeat != null && (
        <div className="absolute bottom-3 left-3 z-20 flex items-center gap-2">
          <button
            type="button"
            onClick={() => (isSatOut ? actions.sendComeBack() : actions.sendSitOut())}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg transition-colors bg-gray-700 hover:bg-gray-600 text-gray-200"
          >
            {isSatOut ? "I'm Back" : 'Sit Out'}
          </button>
          <VolumeControl />
          <ThemePicker />
        </div>
      )}

      {/* Volume control for observers (no seat) */}
      {mySeat == null && (
        <div className="absolute bottom-3 left-3 z-20 flex items-center gap-2">
          <VolumeControl />
          <ThemePicker />
        </div>
      )}

      {/* Hand history — top-right corner */}
      <div className="absolute top-12 right-3 z-10">
        <HandHistory entries={state.handHistory} />
      </div>

      {/* Mini chip standings — right side, below hand history */}
      {currentTable.seats.length > 0 && (
        <div className="absolute top-72 right-3 z-10">
          <ChipLeaderMini
            players={currentTable.seats.map((s) => ({
              playerId: s.playerId,
              name: s.playerName,
              chipCount: s.chipCount,
            }))}
            myPlayerId={myPlayerId}
          />
        </div>
      )}

      {/* Chat panel — bottom-right corner */}
      <div className="absolute bottom-3 right-3 z-10">
        <ChatPanel
          messages={chatMessages}
          onSend={actions.sendChat}
          open={chatOpen}
          onToggle={() => setChatOpen((v) => !v)}
          mutedIds={mutedIds}
          onMute={mute}
          onUnmute={unmute}
        />
      </div>

      {/* Paused banner — derived from game state status */}
      {gameState.status === 'PAUSED' && (
        <div className="absolute inset-0 flex items-center justify-center z-40 pointer-events-none">
          <div className="bg-black bg-opacity-70 text-white text-2xl font-bold px-8 py-4 rounded-2xl shadow-2xl">
            Game Paused
          </div>
        </div>
      )}

      {/* Overlay modals (rebuy, addon, eliminated — injected by the play page) */}
      {overlay}

      {/* Kick confirmation dialog */}
      {kickTarget && (
        <Dialog
          isOpen={true}
          onClose={() => setKickTarget(null)}
          onConfirm={() => {
            actions.sendAdminKick(kickTarget.playerId)
            setKickTarget(null)
          }}
          type="confirm"
          title="Kick Player"
          message={`Remove ${kickTarget.playerName} from the game?`}
          confirmText="Kick"
          cancelText="Cancel"
        />
      )}
    </div>
  )
}
