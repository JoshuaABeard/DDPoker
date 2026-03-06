/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community Contributors
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import React from 'react'
import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { GameProvider } from '../GameContext'
import { useGameState, useGameActions } from '../hooks'
import type { ServerMessage } from '../types'

// ---------------------------------------------------------------------------
// Mocks — vi.hoisted ensures these are available inside hoisted vi.mock calls
// ---------------------------------------------------------------------------

interface MockWsInstance {
  connect: ReturnType<typeof vi.fn>
  disconnect: ReturnType<typeof vi.fn>
  send: ReturnType<typeof vi.fn>
  setReconnectToken: ReturnType<typeof vi.fn>
  onMessage?: (msg: ServerMessage) => void
  onStateChange?: (state: string) => void
  onError?: (error: unknown) => void
  tokenRefreshFn?: () => Promise<string>
}

const { mockGetWsToken, mockJoinGame, mockWsInstanceRef } = vi.hoisted(() => {
  const mockGetWsToken = vi.fn()
  const mockJoinGame = vi.fn()
  const mockWsInstanceRef: { current: MockWsInstance | null } = { current: null }
  return { mockGetWsToken, mockJoinGame, mockWsInstanceRef }
})

vi.mock('@/lib/api', () => ({
  authApi: {
    getWsToken: (...args: unknown[]) => mockGetWsToken(...args),
  },
  gameServerApi: {
    joinGame: (...args: unknown[]) => mockJoinGame(...args),
  },
}))

vi.mock('../WebSocketClient', () => ({
  WebSocketClient: vi.fn().mockImplementation((opts: Record<string, unknown>) => {
    const instance: MockWsInstance = {
      connect: vi.fn(),
      disconnect: vi.fn(),
      send: vi.fn(),
      setReconnectToken: vi.fn(),
      onMessage: opts.onMessage as (msg: ServerMessage) => void,
      onStateChange: opts.onStateChange as (state: string) => void,
      onError: opts.onError as (error: unknown) => void,
      tokenRefreshFn: opts.tokenRefreshFn as (() => Promise<string>) | undefined,
    }
    mockWsInstanceRef.current = instance
    return instance
  }),
  deriveWsProtocol: () => 'ws:',
}))

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

function StateInspector() {
  const state = useGameState()
  return (
    <div>
      <span data-testid="connection-state">{state.connectionState}</span>
      <span data-testid="game-phase">{state.gamePhase}</span>
      <span data-testid="game-id">{state.gameId ?? ''}</span>
      <span data-testid="my-player-id">{state.myPlayerId ?? ''}</span>
      <span data-testid="action-required">{state.actionRequired ? 'yes' : 'no'}</span>
      <span data-testid="chat-count">{state.chatMessages.length}</span>
      <span data-testid="rebuy-offer">{state.rebuyOffer ? 'yes' : 'no'}</span>
      <span data-testid="addon-offer">{state.addonOffer ? 'yes' : 'no'}</span>
      <span data-testid="never-broke-offer">{state.neverBrokeOffer ? 'yes' : 'no'}</span>
      <span data-testid="continue-runout">{state.continueRunoutPending ? 'yes' : 'no'}</span>
    </div>
  )
}

function ActionInvoker({ action, args }: { action: string; args?: Record<string, unknown> }) {
  const actions = useGameActions()
  const handleClick = () => {
    switch (action) {
      case 'sendAction':
        actions.sendAction(args as { action: 'FOLD'; amount: number } ?? { action: 'FOLD', amount: 0 })
        break
      case 'sendChat':
        actions.sendChat(
          (args?.message as string) ?? 'hello',
          args?.tableChat as boolean | undefined,
        )
        break
      case 'sendAdminKick':
        actions.sendAdminKick((args?.playerId as number) ?? 42)
        break
      case 'sendAdminPause':
        actions.sendAdminPause()
        break
      case 'sendAdminResume':
        actions.sendAdminResume()
        break
      case 'sendRebuyDecision':
        actions.sendRebuyDecision((args?.accept as boolean) ?? true)
        break
      case 'sendAddonDecision':
        actions.sendAddonDecision((args?.accept as boolean) ?? false)
        break
      case 'sendSitOut':
        actions.sendSitOut()
        break
      case 'sendComeBack':
        actions.sendComeBack()
        break
      case 'sendNeverBrokeDecision':
        actions.sendNeverBrokeDecision((args?.accept as boolean) ?? true)
        break
      case 'sendContinueRunout':
        actions.sendContinueRunout()
        break
      case 'sendRequestState':
        actions.sendRequestState()
        break
    }
  }
  return (
    <button data-testid="invoke" onClick={handleClick}>
      {action}
    </button>
  )
}

function renderProvider(children?: React.ReactNode) {
  return render(
    <GameProvider gameId="game-abc" serverBaseUrl="http://localhost:8080">
      <StateInspector />
      {children}
    </GameProvider>,
  )
}

// Wait for the async openConnection() to complete (getWsToken + joinGame)
async function waitForConnection() {
  await act(async () => {
    await vi.waitFor(() => {
      expect(mockWsInstanceRef.current).toBeDefined()
    })
  })
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('GameProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockWsInstanceRef.current = null
    mockGetWsToken.mockResolvedValue({ token: 'test-token' })
    mockJoinGame.mockResolvedValue({ wsUrl: '/ws/game/123' })
  })

  // =========================================================================
  // Provider lifecycle
  // =========================================================================

  describe('lifecycle', () => {
    it('fetches token, joins game, connects WebSocket with correct URL on mount', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      expect(mockGetWsToken).toHaveBeenCalledTimes(1)
      expect(mockJoinGame).toHaveBeenCalledWith('game-abc')
      expect(mockWsInstanceRef.current!.connect).toHaveBeenCalledWith(
        'ws://localhost:8080/ws/game/123',
        'test-token',
      )
    })

    it('uses absolute wsUrl when server provides one starting with ws', async () => {
      mockJoinGame.mockResolvedValue({ wsUrl: 'wss://remote.server/ws/game/456' })

      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      expect(mockWsInstanceRef.current!.connect).toHaveBeenCalledWith(
        'wss://remote.server/ws/game/456',
        'test-token',
      )
    })

    it('disconnects WebSocket and resets on unmount', async () => {
      let unmount: () => void
      await act(async () => {
        const result = renderProvider()
        unmount = result.unmount
      })
      await waitForConnection()

      await act(async () => {
        unmount()
      })

      expect(mockWsInstanceRef.current!.disconnect).toHaveBeenCalled()
    })

    it('dispatches DISCONNECTED when connection fails (getWsToken rejects)', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      mockGetWsToken.mockRejectedValue(new Error('auth failed'))

      await act(async () => {
        renderProvider()
      })

      // Wait for the async error to propagate
      await act(async () => {
        await vi.waitFor(() => {
          expect(screen.getByTestId('connection-state').textContent).toBe('DISCONNECTED')
        })
      })

      consoleSpy.mockRestore()
    })

    it('dispatches server messages to reducer via onMessage callback', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      // Simulate a CONNECTED message from the server
      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'CONNECTED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: { playerId: 7, gameState: null, reconnectToken: null },
        })
      })

      expect(screen.getByTestId('my-player-id').textContent).toBe('7')
      expect(screen.getByTestId('game-id').textContent).toBe('game-abc')
    })

    it('dispatches connection state changes via onStateChange callback', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      await act(async () => {
        mockWsInstanceRef.current!.onStateChange!('CONNECTED')
      })

      expect(screen.getByTestId('connection-state').textContent).toBe('CONNECTED')
    })

    it('stores reconnect token from CONNECTED message with reconnectToken', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'CONNECTED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: { playerId: 7, gameState: null, reconnectToken: 'recon-tok-123' },
        })
      })

      expect(mockWsInstanceRef.current!.setReconnectToken).toHaveBeenCalledWith('recon-tok-123')
    })

    it('does NOT set reconnect token when CONNECTED has no reconnectToken', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'CONNECTED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: { playerId: 7, gameState: null, reconnectToken: null },
        })
      })

      expect(mockWsInstanceRef.current!.setReconnectToken).not.toHaveBeenCalled()
    })

    it('does NOT set reconnect token when CONNECTED data is missing (message.data is falsy)', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'CONNECTED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: null as unknown,
        })
      })

      expect(mockWsInstanceRef.current!.setReconnectToken).not.toHaveBeenCalled()
    })

    it('calls onError without crashing (console.error)', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      await act(async () => {
        mockWsInstanceRef.current!.onError!(new Error('ws error'))
      })

      expect(consoleSpy).toHaveBeenCalledWith(
        '[GameProvider] WebSocket error:',
        expect.any(Error),
      )
      consoleSpy.mockRestore()
    })

    it('tokenRefreshFn fetches a new token', async () => {
      await act(async () => {
        renderProvider()
      })
      await waitForConnection()

      mockGetWsToken.mockResolvedValue({ token: 'refreshed-token' })
      const result = await mockWsInstanceRef.current!.tokenRefreshFn!()

      expect(result).toBe('refreshed-token')
      // Once for initial connect + once for refresh
      expect(mockGetWsToken).toHaveBeenCalledTimes(2)
    })
  })

  // =========================================================================
  // Actions
  // =========================================================================

  describe('actions', () => {
    async function renderWithAction(action: string, args?: Record<string, unknown>) {
      await act(async () => {
        renderProvider(<ActionInvoker action={action} args={args} />)
      })
      await waitForConnection()
    }

    async function clickInvoke() {
      const user = userEvent.setup()
      await user.click(screen.getByTestId('invoke'))
    }

    it('sendAction sends PLAYER_ACTION and dispatches ACTION_SENT', async () => {
      await renderWithAction('sendAction', { action: 'FOLD', amount: 0 })

      // First put actionRequired into state so we can see it get cleared
      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'ACTION_REQUIRED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: {
            timeoutSeconds: 30,
            options: {
              canFold: true, canCheck: false, canCall: true, callAmount: 100,
              canBet: false, minBet: 0, maxBet: 0,
              canRaise: false, minRaise: 0, maxRaise: 0,
              canAllIn: true, allInAmount: 500,
            },
          },
        })
      })
      expect(screen.getByTestId('action-required').textContent).toBe('yes')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('PLAYER_ACTION', { action: 'FOLD', amount: 0 })
      expect(screen.getByTestId('action-required').textContent).toBe('no')
    })

    it('sendChat sends CHAT with optimistic dispatch', async () => {
      await renderWithAction('sendChat', { message: 'gg wp', tableChat: true })

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('CHAT', { message: 'gg wp', tableChat: true })
      expect(screen.getByTestId('chat-count').textContent).toBe('1')
    })

    it('sendAdminKick sends ADMIN_KICK with playerId', async () => {
      await renderWithAction('sendAdminKick', { playerId: 42 })

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('ADMIN_KICK', { playerId: 42 })
    })

    it('sendAdminPause sends ADMIN_PAUSE', async () => {
      await renderWithAction('sendAdminPause')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('ADMIN_PAUSE')
    })

    it('sendAdminResume sends ADMIN_RESUME', async () => {
      await renderWithAction('sendAdminResume')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('ADMIN_RESUME')
    })

    it('sendRebuyDecision sends REBUY_DECISION and dispatches CLEAR_OFFERS', async () => {
      await renderWithAction('sendRebuyDecision', { accept: true })

      // Inject a rebuy offer first
      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'REBUY_OFFERED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: { cost: 100, chips: 1000, timeoutSeconds: 30 },
        })
      })
      expect(screen.getByTestId('rebuy-offer').textContent).toBe('yes')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('REBUY_DECISION', { accept: true })
      expect(screen.getByTestId('rebuy-offer').textContent).toBe('no')
    })

    it('sendAddonDecision sends ADDON_DECISION and dispatches CLEAR_OFFERS', async () => {
      await renderWithAction('sendAddonDecision', { accept: false })

      // Inject an addon offer first
      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'ADDON_OFFERED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: { cost: 200, chips: 2000, timeoutSeconds: 30 },
        })
      })
      expect(screen.getByTestId('addon-offer').textContent).toBe('yes')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('ADDON_DECISION', { accept: false })
      expect(screen.getByTestId('addon-offer').textContent).toBe('no')
    })

    it('sendSitOut sends SIT_OUT', async () => {
      await renderWithAction('sendSitOut')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('SIT_OUT')
    })

    it('sendComeBack sends COME_BACK', async () => {
      await renderWithAction('sendComeBack')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('COME_BACK')
    })

    it('sendNeverBrokeDecision sends NEVER_BROKE_DECISION and dispatches CLEAR_NEVER_BROKE', async () => {
      await renderWithAction('sendNeverBrokeDecision', { accept: true })

      // Inject a never broke offer
      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'NEVER_BROKE_OFFERED',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: { timeoutSeconds: 15 },
        })
      })
      expect(screen.getByTestId('never-broke-offer').textContent).toBe('yes')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('NEVER_BROKE_DECISION', { accept: true })
      expect(screen.getByTestId('never-broke-offer').textContent).toBe('no')
    })

    it('sendContinueRunout sends CONTINUE_RUNOUT and dispatches CLEAR_CONTINUE_RUNOUT', async () => {
      await renderWithAction('sendContinueRunout')

      // Inject a continue runout prompt
      await act(async () => {
        mockWsInstanceRef.current!.onMessage!({
          type: 'CONTINUE_RUNOUT',
          gameId: 'game-abc',
          sequenceNumber: 1,
          timestamp: '2026-01-01T00:00:00Z',
          data: null as unknown,
        })
      })
      expect(screen.getByTestId('continue-runout').textContent).toBe('yes')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('CONTINUE_RUNOUT')
      expect(screen.getByTestId('continue-runout').textContent).toBe('no')
    })

    it('sendRequestState sends REQUEST_STATE', async () => {
      await renderWithAction('sendRequestState')

      await clickInvoke()

      expect(mockWsInstanceRef.current!.send).toHaveBeenCalledWith('REQUEST_STATE')
    })
  })
})
