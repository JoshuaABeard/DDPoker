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
import { PokerTable } from '@/components/game/PokerTable'
import { GameOverlay } from '@/components/game/GameOverlay'
import { GameInfoPanel } from '@/components/game/GameInfoPanel'
import { config } from '@/lib/config'

// ---------------------------------------------------------------------------
// Inner play content — must be inside GameProvider
// ---------------------------------------------------------------------------

function PlayContent({ gameId }: { gameId: string }) {
  const router = useRouter()
  const state = useGameState()
  const actions = useGameActions()

  const [gameName, setGameName] = useState('DD Poker')
  // Local-only UI state
  const [dismissedEliminated, setDismissedEliminated] = useState(false)
  const [infoOpen, setInfoOpen] = useState(false)

  const { gamePhase, lobbyState, gameState, rebuyOffer, addonOffer, eliminatedPosition, error } = state

  // Pick up game name from lobby state
  useEffect(() => {
    if (lobbyState?.name) setGameName(lobbyState.name)
  }, [lobbyState])

  // Toggle game info panel with 'I' key
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'i' || e.key === 'I') {
        // Only toggle if no input/textarea is focused
        if (document.activeElement?.tagName !== 'INPUT' &&
            document.activeElement?.tagName !== 'TEXTAREA') {
          setInfoOpen((v) => !v)
        }
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  // Navigate to results when game completes; persist standings for the results page
  useEffect(() => {
    if (gamePhase === 'complete') {
      try {
        sessionStorage.setItem('ddpoker_results', JSON.stringify({
          standings: state.standings,
          myPlayerId: state.myPlayerId,
        }))
      } catch {
        // sessionStorage unavailable (private browsing etc.) — results page shows fallback
      }
      router.push(`/games/${gameId}/results`)
    }
  }, [gamePhase, gameId, router, state.standings, state.myPlayerId])

  // Handle being kicked
  useEffect(() => {
    if (error === 'You were removed from the game') {
      router.push('/games')
    }
  }, [error, router])

  // Determine which overlay to show (priority order).
  // rebuyOffer/addonOffer/eliminatedPosition come from game state (reducer).
  // Sending a rebuy/addon decision dispatches CLEAR_OFFERS automatically.
  let overlay: React.ReactNode = null
  if (rebuyOffer) {
    overlay = (
      <GameOverlay
        type="rebuy"
        cost={rebuyOffer.cost}
        chips={rebuyOffer.chips}
        timeoutSeconds={rebuyOffer.timeoutSeconds}
        onDecision={actions.sendRebuyDecision}
      />
    )
  } else if (addonOffer) {
    overlay = (
      <GameOverlay
        type="addon"
        cost={addonOffer.cost}
        chips={addonOffer.chips}
        timeoutSeconds={addonOffer.timeoutSeconds}
        onDecision={actions.sendAddonDecision}
      />
    )
  } else if (eliminatedPosition != null && !dismissedEliminated) {
    overlay = (
      <GameOverlay
        type="eliminated"
        finishPosition={eliminatedPosition}
        onClose={() => setDismissedEliminated(true)}
      />
    )
  }

  return (
    /* Full-screen container covers the site nav/footer from the root layout */
    <div className="fixed inset-0 z-40 game-bg">
      {/* Minimal top bar with game name + leave/info buttons */}
      <div className="absolute top-0 left-0 right-0 h-10 flex items-center justify-between px-4 z-50 bg-black bg-opacity-40">
        {/* Game name — text node only (XSS safe) */}
        <span className="text-white text-sm font-semibold truncate max-w-xs">{gameName}</span>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setInfoOpen((v) => !v)}
            title="Game info (I)"
            className="text-gray-300 hover:text-white text-xs"
          >
            Info
          </button>
          <button
            type="button"
            onClick={() => router.push('/games')}
            className="text-gray-300 hover:text-white text-xs underline"
          >
            Leave Game
          </button>
        </div>
      </div>

      {/* Poker table fills remaining space (below the 40px top bar) */}
      <div className="absolute inset-0 top-10">
        <PokerTable gameName={gameName} overlay={overlay} />
      </div>

      {/* Game info panel — toggled by 'I' key or Info button */}
      {infoOpen && gameState && (
        <div className="absolute top-12 right-3 z-50">
          <GameInfoPanel
            gameName={gameName}
            ownerName={lobbyState?.ownerName ?? ''}
            blinds={gameState.blinds}
            currentLevel={gameState.level}
            players={gameState.players.map((p) => ({
              playerId: p.playerId,
              name: p.name,
              chipCount: p.chipCount,
            }))}
            onClose={() => setInfoOpen(false)}
          />
        </div>
      )}
    </div>
  )
}

// ---------------------------------------------------------------------------
// Page — wraps content in GameProvider
// ---------------------------------------------------------------------------

export default function PlayPage() {
  const params = useParams()
  const gameId = params.gameId as string

  return (
    <GameProvider gameId={gameId} serverBaseUrl={config.apiBaseUrl}>
      <PlayContent gameId={gameId} />
    </GameProvider>
  )
}
