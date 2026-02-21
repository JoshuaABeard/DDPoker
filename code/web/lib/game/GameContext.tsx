/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

'use client'

import React, { createContext, useCallback, useEffect, useMemo, useReducer, useRef } from 'react'
import { gameServerApi } from '@/lib/api'
import { WebSocketClient, deriveWsProtocol } from './WebSocketClient'
import { gameReducer, initialGameState } from './gameReducer'
import type { GameState } from './gameReducer'
import type { PlayerActionData, ChatData, AdminKickData } from './types'

// ============================================================================
// Context value types
// ============================================================================

export interface GameActions {
  /** Send a poker action (fold/check/call/bet/raise). Disables panel until PLAYER_ACTED. */
  sendAction: (action: PlayerActionData) => void
  /** Send a chat message (optimistic). */
  sendChat: (message: string, tableChat?: boolean) => void
  /** Kick a player (owner only). */
  sendAdminKick: (playerId: number) => void
  /** Pause the game (owner only). */
  sendAdminPause: () => void
  /** Resume the game (owner only). */
  sendAdminResume: () => void
  /** Accept or decline a rebuy offer. */
  sendRebuyDecision: (accept: boolean) => void
  /** Accept or decline an addon offer. */
  sendAddonDecision: (accept: boolean) => void
}

interface GameContextValue {
  state: GameState
  actions: GameActions
}

// ============================================================================
// Context
// ============================================================================

const GameContext = createContext<GameContextValue | null>(null)

// ============================================================================
// Provider
// ============================================================================

export interface GameProviderProps {
  /** Game ID to connect to */
  gameId: string
  /** Base server URL for WebSocket connection (e.g. "http://localhost:8080") */
  serverBaseUrl: string
  children: React.ReactNode
}

/**
 * Manages the WebSocket lifecycle and game state for a single game session.
 *
 * Place this high in the tree (the play page) so all game components
 * can access state via `useGameState()` and actions via `useGameActions()`.
 */
export function GameProvider({ gameId, serverBaseUrl, children }: GameProviderProps) {
  const [state, dispatch] = useReducer(gameReducer, initialGameState)
  const wsClientRef = useRef<WebSocketClient | null>(null)
  const reconnectTokenRef = useRef<string | null>(null)
  // Ref to latest state so actions can access current values without stale closures
  const stateRef = useRef(state)
  stateRef.current = state

  // Build the WebSocket URL from the server base URL + ws protocol
  const buildWsUrl = useCallback(
    (path: string) => {
      const wsProtocol = deriveWsProtocol()
      // Strip http(s):// from serverBaseUrl, keep host:port
      const hostPart = serverBaseUrl.replace(/^https?:\/\//, '')
      return `${wsProtocol}//${hostPart}${path}`
    },
    [serverBaseUrl],
  )

  // ---------------------------------------------------------------------------
  // Connect on mount, disconnect on unmount
  // ---------------------------------------------------------------------------

  useEffect(() => {
    let cancelled = false

    async function openConnection() {
      try {
        // 1. Fetch a short-lived ws-connect token
        const { token } = await gameServerApi.getWsToken()
        if (cancelled) return

        // 2. Join the game to get the wsUrl
        const { wsUrl } = await gameServerApi.joinGame(gameId)
        if (cancelled) return

        // 3. Derive WS URL (use server-provided wsUrl if absolute, else build from base)
        const fullWsUrl = wsUrl.startsWith('ws') ? wsUrl : buildWsUrl(wsUrl)

        // 4. Build client
        const client = new WebSocketClient({
          onMessage(message) {
            dispatch({ type: 'SERVER_MESSAGE', message })

            // Extract reconnect token from CONNECTED message
            if (message.type === 'CONNECTED' && message.data) {
              const data = message.data as { reconnectToken?: string }
              if (data.reconnectToken) {
                reconnectTokenRef.current = data.reconnectToken
                client.setReconnectToken(data.reconnectToken)
              }
            }

            // Navigate on LOBBY_GAME_STARTING or GAME_COMPLETE is handled by page
          },
          onStateChange(connectionState) {
            dispatch({ type: 'CONNECTION_STATE_CHANGED', state: connectionState })
          },
          onError(error) {
            console.error('[GameProvider] WebSocket error:', error)
          },
        })

        wsClientRef.current = client
        client.connect(fullWsUrl, token)
      } catch (err) {
        console.error('[GameProvider] Failed to open connection:', err)
        dispatch({ type: 'CONNECTION_STATE_CHANGED', state: 'DISCONNECTED' })
      }
    }

    openConnection()

    return () => {
      cancelled = true
      wsClientRef.current?.disconnect()
      wsClientRef.current = null
      dispatch({ type: 'RESET' })
    }
  }, [gameId, buildWsUrl])

  // ---------------------------------------------------------------------------
  // Actions — memoized so the object reference is stable across re-renders.
  // State values are accessed via stateRef to avoid stale closures.
  // dispatch from useReducer is guaranteed stable by React.
  // ---------------------------------------------------------------------------

  const actions = useMemo<GameActions>(() => ({
    sendAction(data: PlayerActionData) {
      wsClientRef.current?.send('PLAYER_ACTION', data)
    },

    sendChat(message: string, tableChat = true) {
      const data: ChatData = { message, tableChat }
      // Optimistic: show message immediately using latest playerId via ref
      dispatch({
        type: 'CHAT_OPTIMISTIC',
        entry: {
          id: `optimistic-${Date.now()}`,
          playerId: stateRef.current.myPlayerId ?? 0,
          playerName: 'You',
          message,
          tableChat,
          optimistic: true,
        },
      })
      wsClientRef.current?.send('CHAT', data)
    },

    sendAdminKick(playerId: number) {
      const data: AdminKickData = { playerId }
      wsClientRef.current?.send('ADMIN_KICK', data)
    },

    sendAdminPause() {
      wsClientRef.current?.send('ADMIN_PAUSE')
    },

    sendAdminResume() {
      wsClientRef.current?.send('ADMIN_RESUME')
    },

    sendRebuyDecision(accept: boolean) {
      wsClientRef.current?.send('REBUY_DECISION', { accept })
      dispatch({ type: 'CLEAR_OFFERS' })
    },

    sendAddonDecision(accept: boolean) {
      wsClientRef.current?.send('ADDON_DECISION', { accept })
      dispatch({ type: 'CLEAR_OFFERS' })
    },
  }), [dispatch])

  return <GameContext.Provider value={{ state, actions }}>{children}</GameContext.Provider>
}

// ============================================================================
// Exported context value — accessed via hooks in hooks.ts
// ============================================================================

export { GameContext }
