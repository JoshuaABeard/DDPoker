import { describe, it, expect } from 'vitest'
import { gameReducer, initialGameState } from '../gameReducer'
import type { ServerMessage } from '../types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function msg<T>(type: string, data: T, gameId = 'game-1'): ServerMessage {
  return { type: type as ServerMessage['type'], gameId, sequenceNumber: 1, data }
}

function connected(playerId = 42, reconnectToken: string | null = 'rt-token') {
  return msg('CONNECTED', { playerId, gameState: null, reconnectToken })
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('gameReducer', () => {
  it('returns initial state unchanged for unknown actions', () => {
    // @ts-expect-error - testing unknown action
    const result = gameReducer(initialGameState, { type: 'UNKNOWN' })
    expect(result).toBe(initialGameState)
  })

  it('RESET returns initial state', () => {
    const modified = { ...initialGameState, myPlayerId: 42 }
    const result = gameReducer(modified, { type: 'RESET' })
    expect(result).toEqual(initialGameState)
  })

  it('CONNECTION_STATE_CHANGED updates connectionState', () => {
    const result = gameReducer(initialGameState, {
      type: 'CONNECTION_STATE_CHANGED',
      state: 'CONNECTED',
    })
    expect(result.connectionState).toBe('CONNECTED')
  })

  it('does not re-render if connectionState unchanged', () => {
    const state = { ...initialGameState, connectionState: 'CONNECTED' as const }
    const result = gameReducer(state, { type: 'CONNECTION_STATE_CHANGED', state: 'CONNECTED' })
    // Same object reference — no state update
    expect(result).toBe(state)
  })

  // -------------------------------------------------------------------------
  // CONNECTED
  // -------------------------------------------------------------------------

  describe('CONNECTED', () => {
    it('sets myPlayerId and gameId from CONNECTED', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: connected(42),
      })
      expect(result.myPlayerId).toBe(42)
      expect(result.gameId).toBe('game-1')
    })

    it('stores reconnectToken from CONNECTED', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: connected(42, 'my-reconnect-token'),
      })
      expect(result.reconnectToken).toBe('my-reconnect-token')
    })

    it('clears error on CONNECTED', () => {
      const state = { ...initialGameState, error: 'previous error' }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: connected(42),
      })
      expect(result.error).toBeNull()
    })

    it('sets gamePhase to playing when CONNECTED includes gameState', () => {
      const gameState = {
        status: 'IN_PROGRESS',
        level: 1,
        blinds: { small: 10, big: 20, ante: 0 },
        nextLevelIn: null,
        tables: [{ tableId: 1, seats: [], communityCards: [], pots: [], currentRound: 'PREFLOP', handNumber: 5 }],
        players: [{ playerId: 42, name: 'Alice', chipCount: 1000, tableId: 1, seatIndex: 0, finishPosition: null }],
      }
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('CONNECTED', { playerId: 42, gameState, reconnectToken: null }),
      })
      expect(result.gamePhase).toBe('playing')
      expect(result.gameState).toEqual(gameState)
    })
  })

  // -------------------------------------------------------------------------
  // LOBBY_STATE
  // -------------------------------------------------------------------------

  describe('LOBBY_STATE', () => {
    const lobbyState = {
      gameId: 'game-1',
      name: 'Test Game',
      hostingType: 'SERVER',
      ownerName: 'Alice',
      ownerProfileId: 1,
      maxPlayers: 9,
      isPrivate: false,
      players: [{ profileId: 1, name: 'Alice', isOwner: true, isAI: false, aiSkillLevel: null }],
      blinds: { small: 10, big: 20, ante: 0 },
    }

    it('sets lobby state and phase', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_STATE', lobbyState),
      })
      expect(result.gamePhase).toBe('lobby')
      expect(result.lobbyState?.name).toBe('Test Game')
      expect(result.lobbyState?.players).toHaveLength(1)
    })
  })

  // -------------------------------------------------------------------------
  // LOBBY_PLAYER_JOINED
  // -------------------------------------------------------------------------

  describe('LOBBY_PLAYER_JOINED', () => {
    it('adds a player to the lobby', () => {
      const initialWithLobby = {
        ...initialGameState,
        lobbyState: {
          gameId: 'game-1',
          name: 'Test',
          hostingType: 'SERVER',
          ownerName: 'Alice',
          ownerProfileId: 1,
          maxPlayers: 9,
          isPrivate: false,
          players: [{ profileId: 1, name: 'Alice', isOwner: true, isAI: false, aiSkillLevel: null }],
          blinds: { small: 10, big: 20, ante: 0 },
        },
      }
      const result = gameReducer(initialWithLobby, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_PLAYER_JOINED', {
          player: { profileId: 2, name: 'Bob', isOwner: false, isAI: false, aiSkillLevel: null },
        }),
      })
      expect(result.lobbyState?.players).toHaveLength(2)
      expect(result.lobbyState?.players[1].name).toBe('Bob')
    })

    it('does not add duplicate players', () => {
      const initialWithLobby = {
        ...initialGameState,
        lobbyState: {
          gameId: 'game-1',
          name: 'Test',
          hostingType: 'SERVER',
          ownerName: 'Alice',
          ownerProfileId: 1,
          maxPlayers: 9,
          isPrivate: false,
          players: [{ profileId: 1, name: 'Alice', isOwner: true, isAI: false, aiSkillLevel: null }],
          blinds: { small: 10, big: 20, ante: 0 },
        },
      }
      const result = gameReducer(initialWithLobby, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_PLAYER_JOINED', {
          player: { profileId: 1, name: 'Alice', isOwner: true, isAI: false, aiSkillLevel: null },
        }),
      })
      expect(result.lobbyState?.players).toHaveLength(1)
    })
  })

  // -------------------------------------------------------------------------
  // HOLE_CARDS_DEALT
  // -------------------------------------------------------------------------

  describe('HOLE_CARDS_DEALT', () => {
    it('sets hole cards', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('HOLE_CARDS_DEALT', { cards: ['Ah', 'Kd'] }),
      })
      expect(result.holeCards).toEqual(['Ah', 'Kd'])
    })
  })

  // -------------------------------------------------------------------------
  // ACTION_REQUIRED
  // -------------------------------------------------------------------------

  describe('ACTION_REQUIRED', () => {
    it('sets actionRequired and timeout', () => {
      const options = {
        canFold: true, canCheck: false, canCall: true, callAmount: 100,
        canBet: false, minBet: 0, maxBet: 0,
        canRaise: true, minRaise: 200, maxRaise: 1000,
        canAllIn: true, allInAmount: 1000,
      }
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('ACTION_REQUIRED', { timeoutSeconds: 30, options }),
      })
      expect(result.actionRequired).toEqual(options)
      expect(result.actionTimeoutSeconds).toBe(30)
    })
  })

  // -------------------------------------------------------------------------
  // GAME_COMPLETE
  // -------------------------------------------------------------------------

  describe('GAME_COMPLETE', () => {
    it('sets gamePhase to complete and standings', () => {
      const standings = [
        { position: 1, playerId: 42, playerName: 'Alice', prize: 1000 },
        { position: 2, playerId: 43, playerName: 'Bob', prize: 500 },
      ]
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('GAME_COMPLETE', { standings, totalHands: 50, duration: 3600 }),
      })
      expect(result.gamePhase).toBe('complete')
      expect(result.standings).toEqual(standings)
      expect(result.actionRequired).toBeNull()
    })
  })

  // -------------------------------------------------------------------------
  // CHAT_MESSAGE
  // -------------------------------------------------------------------------

  describe('CHAT_MESSAGE', () => {
    it('appends chat message', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('CHAT_MESSAGE', {
          playerId: 1, playerName: 'Alice', message: 'Hello!', tableChat: true,
        }),
      })
      expect(result.chatMessages).toHaveLength(1)
      expect(result.chatMessages[0].message).toBe('Hello!')
    })

    it('caps chat at 200 messages (FIFO)', () => {
      let state = initialGameState
      for (let i = 0; i < 205; i++) {
        state = gameReducer(state, {
          type: 'SERVER_MESSAGE',
          message: msg('CHAT_MESSAGE', {
            playerId: 1, playerName: 'Alice', message: `msg-${i}`, tableChat: true,
          }),
        })
      }
      expect(state.chatMessages).toHaveLength(200)
      // First message should be msg-5 (oldest 5 dropped)
      expect(state.chatMessages[0].message).toBe('msg-5')
    })
  })

  // -------------------------------------------------------------------------
  // CHAT_OPTIMISTIC
  // -------------------------------------------------------------------------

  describe('CHAT_OPTIMISTIC', () => {
    it('adds optimistic chat entry', () => {
      const entry = {
        id: 'opt-1', playerId: 42, playerName: 'You',
        message: 'test', tableChat: true, optimistic: true,
      }
      const result = gameReducer(initialGameState, { type: 'CHAT_OPTIMISTIC', entry })
      expect(result.chatMessages[0]).toEqual(entry)
    })
  })

  // -------------------------------------------------------------------------
  // ERROR
  // -------------------------------------------------------------------------

  describe('ERROR', () => {
    it('sets error from ERROR message', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('ERROR', { code: 'GAME_FULL', message: 'Game is full' }),
      })
      expect(result.error).toBe('Game is full')
    })
  })

  // -------------------------------------------------------------------------
  // LOBBY_SETTINGS_CHANGED
  // -------------------------------------------------------------------------

  describe('LOBBY_SETTINGS_CHANGED', () => {
    const baseLobby = {
      gameId: 'game-1',
      name: 'Old Name',
      hostingType: 'SERVER',
      ownerName: 'Alice',
      ownerProfileId: 1,
      maxPlayers: 9,
      isPrivate: false,
      players: [{ profileId: 1, name: 'Alice', isOwner: true, isAI: false, aiSkillLevel: null }],
      blinds: { small: 10, big: 20, ante: 0 },
    }

    it('updates lobby settings from LOBBY_SETTINGS_CHANGED', () => {
      const state = { ...initialGameState, lobbyState: baseLobby }
      const updatedSettings = {
        gameId: 'game-1',
        name: 'New Name',
        status: 'WAITING_FOR_PLAYERS' as const,
        hostingType: 'SERVER' as const,
        ownerName: 'Alice',
        ownerProfileId: 1,
        playerCount: 1,
        maxPlayers: 6,
        isPrivate: true,
        buyIn: 1000,
        startingChips: 5000,
        blinds: { small: 25, big: 50, ante: 5 },
        wsUrl: null,
      }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_SETTINGS_CHANGED', { updatedSettings }),
      })
      expect(result.lobbyState?.name).toBe('New Name')
      expect(result.lobbyState?.maxPlayers).toBe(6)
      expect(result.lobbyState?.isPrivate).toBe(true)
      expect(result.lobbyState?.blinds.small).toBe(25)
      // Players list must be preserved (not wiped by settings change)
      expect(result.lobbyState?.players).toHaveLength(1)
    })

    it('is a no-op when lobbyState is null', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_SETTINGS_CHANGED', { updatedSettings: {} }),
      })
      expect(result).toBe(initialGameState)
    })
  })

  // -------------------------------------------------------------------------
  // PLAYER_LEFT / PLAYER_DISCONNECTED
  // -------------------------------------------------------------------------

  describe('PLAYER_LEFT', () => {
    const tableWithPlayers = {
      tableId: 1,
      seats: [
        { seatIndex: 0, playerId: 42, playerName: 'Alice', chipCount: 1000, status: 'ACTIVE',
          isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
        { seatIndex: 1, playerId: 43, playerName: 'Bob', chipCount: 800, status: 'ACTIVE',
          isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
      ],
      communityCards: [],
      pots: [],
      currentRound: 'PREFLOP',
      handNumber: 1,
    }

    it('removes the seat for PLAYER_LEFT', () => {
      const state = { ...initialGameState, currentTable: tableWithPlayers }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_LEFT', { playerId: 43, playerName: 'Bob' }),
      })
      expect(result.currentTable?.seats).toHaveLength(1)
      expect(result.currentTable?.seats[0].playerId).toBe(42)
    })

    it('marks seat as DISCONNECTED for PLAYER_DISCONNECTED', () => {
      const state = { ...initialGameState, currentTable: tableWithPlayers }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_DISCONNECTED', { playerId: 43, playerName: 'Bob' }),
      })
      expect(result.currentTable?.seats).toHaveLength(2)
      const bobSeat = result.currentTable?.seats.find((s) => s.playerId === 43)
      expect(bobSeat?.status).toBe('DISCONNECTED')
    })
  })

  // -------------------------------------------------------------------------
  // REBUY_OFFERED / ADDON_OFFERED / CLEAR_OFFERS
  // -------------------------------------------------------------------------

  describe('REBUY_OFFERED / ADDON_OFFERED / CLEAR_OFFERS', () => {
    it('stores rebuy offer in state', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('REBUY_OFFERED', { cost: 100, chips: 500, timeoutSeconds: 30 }),
      })
      expect(result.rebuyOffer).toEqual({ cost: 100, chips: 500, timeoutSeconds: 30 })
    })

    it('stores addon offer in state', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('ADDON_OFFERED', { cost: 200, chips: 1000, timeoutSeconds: 60 }),
      })
      expect(result.addonOffer).toEqual({ cost: 200, chips: 1000, timeoutSeconds: 60 })
    })

    it('CLEAR_OFFERS clears both offers', () => {
      const state = {
        ...initialGameState,
        rebuyOffer: { cost: 100, chips: 500, timeoutSeconds: 30 },
        addonOffer: { cost: 200, chips: 1000, timeoutSeconds: 60 },
      }
      const result = gameReducer(state, { type: 'CLEAR_OFFERS' })
      expect(result.rebuyOffer).toBeNull()
      expect(result.addonOffer).toBeNull()
    })

    it('HAND_STARTED clears pending offers', () => {
      const state = {
        ...initialGameState,
        rebuyOffer: { cost: 100, chips: 500, timeoutSeconds: 30 },
        addonOffer: { cost: 200, chips: 1000, timeoutSeconds: 60 },
      }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('HAND_STARTED', {
          handNumber: 2,
          dealerSeat: 0,
          smallBlindSeat: 1,
          bigBlindSeat: 2,
          blindsPosted: [],
        }),
      })
      expect(result.rebuyOffer).toBeNull()
      expect(result.addonOffer).toBeNull()
    })
  })

  // -------------------------------------------------------------------------
  // PLAYER_ELIMINATED — eliminatedPosition
  // -------------------------------------------------------------------------

  describe('PLAYER_ELIMINATED', () => {
    const tableWithSeat = {
      tableId: 1,
      seats: [
        { seatIndex: 0, playerId: 42, playerName: 'Alice', chipCount: 1000, status: 'ACTIVE',
          isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
      ],
      communityCards: [],
      pots: [],
      currentRound: 'PREFLOP',
      handNumber: 1,
    }

    it('sets eliminatedPosition when current player is eliminated', () => {
      const state = { ...initialGameState, myPlayerId: 42, currentTable: tableWithSeat }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_ELIMINATED', {
          playerId: 42, playerName: 'Alice', finishPosition: 5, handsPlayed: 20,
        }),
      })
      expect(result.eliminatedPosition).toBe(5)
      expect(result.currentTable?.seats).toHaveLength(0)
    })

    it('does not set eliminatedPosition when other player is eliminated', () => {
      const tableWith2 = {
        ...tableWithSeat,
        seats: [
          ...tableWithSeat.seats,
          { seatIndex: 1, playerId: 99, playerName: 'Bob', chipCount: 0, status: 'ACTIVE',
            isDealer: false, isSmallBlind: false, isBigBlind: false, currentBet: 0, holeCards: [], isCurrentActor: false },
        ],
      }
      const state = { ...initialGameState, myPlayerId: 42, currentTable: tableWith2 }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_ELIMINATED', {
          playerId: 99, playerName: 'Bob', finishPosition: 6, handsPlayed: 15,
        }),
      })
      expect(result.eliminatedPosition).toBeNull()
    })
  })

  // -------------------------------------------------------------------------
  // Unknown message type
  // -------------------------------------------------------------------------

  it('ignores unknown server message types (no crash)', () => {
    const result = gameReducer(initialGameState, {
      type: 'SERVER_MESSAGE',
      message: msg('TOTALLY_UNKNOWN_TYPE', {}),
    })
    expect(result).toEqual(initialGameState)
  })

  it('ignores malformed messages (no crash)', () => {
    const result = gameReducer(initialGameState, {
      type: 'SERVER_MESSAGE',
      message: null as unknown as ServerMessage,
    })
    expect(result).toEqual(initialGameState)
  })
})
