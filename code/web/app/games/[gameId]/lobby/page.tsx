/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { GameProvider } from '@/lib/game/GameContext'
import { useGameState, useGameActions } from '@/lib/game/hooks'
import { ChatPanel } from '@/components/game/ChatPanel'
import { GameInfoPanel } from '@/components/game/GameInfoPanel'
import { gameServerApi } from '@/lib/api'
import { config } from '@/lib/config'

// ---------------------------------------------------------------------------
// Inner lobby UI — must be inside GameProvider
// ---------------------------------------------------------------------------

function LobbyContent({ gameId }: { gameId: string }) {
  const router = useRouter()
  const state = useGameState()
  const actions = useGameActions()
  const [chatOpen, setChatOpen] = useState(true)
  const [starting, setStarting] = useState(false)
  const [startError, setStartError] = useState<string | null>(null)

  const { lobbyState, gamePhase, chatMessages, myPlayerId, error } = state

  // Navigate to play page when game starts
  useEffect(() => {
    if (gamePhase === 'playing') {
      router.push(`/games/${gameId}/play`)
    }
  }, [gamePhase, gameId, router])

  // Handle GAME_CANCELLED
  useEffect(() => {
    if (error === 'Game was cancelled') {
      router.push('/games')
    }
  }, [error, router])

  async function handleStart() {
    setStartError(null)
    setStarting(true)
    try {
      await gameServerApi.startGame(gameId)
    } catch (err: unknown) {
      setStartError(err instanceof Error ? err.message : 'Failed to start game.')
      setStarting(false)
    }
  }

  async function handleCancel() {
    try {
      await gameServerApi.cancelGame(gameId)
      router.push('/games')
    } catch {
      // Ignore — game may already be cancelled
      router.push('/games')
    }
  }

  if (!lobbyState) {
    return (
      <div className="flex items-center justify-center h-40">
        <p className="text-gray-500">Connecting to lobby…</p>
      </div>
    )
  }

  const isOwner = myPlayerId != null &&
    lobbyState.players.some((p) => p.profileId === myPlayerId && p.isOwner)

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        {/* Game name — text node only (XSS safe) */}
        <h1 className="text-2xl font-bold">{lobbyState.name}</h1>
        <span className="text-sm text-gray-500">
          {lobbyState.players.length}/{lobbyState.maxPlayers} players
        </span>
      </div>

      {/* Game settings — blind structure and host info */}
      <div className="flex flex-wrap gap-4 items-start">
        <GameInfoPanel
          gameName={lobbyState.name}
          ownerName={lobbyState.ownerName}
          blinds={lobbyState.blinds}
        />
        <div className="text-sm text-gray-600 space-y-1 pt-1">
          <div>
            <span className="font-medium">Private:</span>{' '}
            {lobbyState.isPrivate ? 'Yes' : 'No'}
          </div>
          <div>
            <span className="font-medium">Type:</span> {lobbyState.hostingType}
          </div>
        </div>
      </div>

      {/* Player list */}
      <div>
        <h2 className="text-lg font-semibold mb-3">Players</h2>
        <div className="rounded-lg border border-gray-200 divide-y divide-gray-100">
          {lobbyState.players.map((player) => (
            <div
              key={player.profileId}
              className="flex items-center justify-between px-4 py-3"
            >
              <div className="flex items-center gap-2">
                {/* Player name — text node only (XSS safe) */}
                <span className="font-medium">{player.name}</span>
                {player.isOwner && (
                  <span className="text-xs bg-yellow-100 text-yellow-800 border border-yellow-300 rounded px-1.5 py-0.5">
                    Owner
                  </span>
                )}
                {player.isAI && (
                  <span className="text-xs bg-blue-100 text-blue-800 border border-blue-300 rounded px-1.5 py-0.5">
                    AI
                  </span>
                )}
              </div>
              {isOwner && !player.isOwner && !player.isAI && (
                <button
                  type="button"
                  onClick={() => actions.sendAdminKick(player.profileId)}
                  className="text-xs text-red-600 hover:text-red-800 hover:underline"
                >
                  Kick
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Owner controls */}
      {isOwner && (
        <div className="flex gap-3">
          <button
            type="button"
            onClick={handleStart}
            disabled={starting || lobbyState.players.length < 2}
            className="px-6 py-2 bg-green-700 hover:bg-green-600 disabled:opacity-50 text-white font-bold rounded-lg transition-colors"
          >
            {starting ? 'Starting…' : 'Start Game'}
          </button>
          <button
            type="button"
            onClick={handleCancel}
            className="px-4 py-2 bg-red-100 hover:bg-red-200 text-red-700 font-semibold rounded-lg transition-colors text-sm"
          >
            Cancel Game
          </button>
        </div>
      )}

      {!isOwner && (
        <div className="text-sm text-gray-500">
          Waiting for the game owner to start the game…
        </div>
      )}

      {startError && (
        <p className="text-red-600 text-sm" role="alert">
          {startError}
        </p>
      )}

      {/* Chat */}
      <div>
        <h2 className="text-lg font-semibold mb-2">Chat</h2>
        <ChatPanel
          messages={chatMessages}
          onSend={actions.sendChat}
          open={chatOpen}
          onToggle={() => setChatOpen((v) => !v)}
        />
      </div>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page — wraps content in GameProvider
// ---------------------------------------------------------------------------

export default function LobbyPage() {
  const params = useParams()
  const gameId = params.gameId as string
  const serverBaseUrl = config.apiBaseUrl

  return (
    <GameProvider gameId={gameId} serverBaseUrl={serverBaseUrl}>
      <LobbyContent gameId={gameId} />
    </GameProvider>
  )
}
