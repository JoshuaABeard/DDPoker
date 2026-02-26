import { describe, it, expect } from 'vitest'
import { gameReducer, initialGameState } from '../gameReducer'
import type { GameState } from '../gameReducer'
import type { ServerMessage, SeatData, TableData } from '../types'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function msg<T>(type: string, data: T, gameId = 'game-1'): ServerMessage {
  return { type: type as ServerMessage['type'], gameId, sequenceNumber: 1, timestamp: '2026-01-01T00:00:00Z', data }
}

function connected(playerId = 42, reconnectToken: string | null = 'rt-token') {
  return msg('CONNECTED', { playerId, gameState: null, reconnectToken })
}

/** Build a minimal TableData with two seats. */
function makeTable(overrides?: Partial<TableData>): TableData {
  return {
    tableId: 1,
    seats: [
      makeSeat(0, 1, 'Alice', 1000),
      makeSeat(1, 2, 'Bob', 800),
    ],
    communityCards: [],
    pots: [],
    currentRound: 'PREFLOP',
    handNumber: 1,
    ...overrides,
  }
}

function makeSeat(seatIndex: number, playerId: number, playerName: string, chipCount: number, status = 'ACTIVE'): SeatData {
  return {
    seatIndex,
    playerId,
    playerName,
    chipCount,
    status,
    isDealer: false,
    isSmallBlind: false,
    isBigBlind: false,
    currentBet: 0,
    holeCards: [],
    isCurrentActor: false,
  }
}

function stateWithTable(myPlayerId: number, table: TableData): GameState {
  return { ...initialGameState, myPlayerId, currentTable: table }
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
      blinds: { smallBlind: 10, bigBlind: 20, ante: 0 },
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
          blinds: { smallBlind: 10, bigBlind: 20, ante: 0 },
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
          blinds: { smallBlind: 10, bigBlind: 20, ante: 0 },
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
      blinds: { smallBlind: 10, bigBlind: 20, ante: 0 },
    }

    it('updates lobby settings from LOBBY_SETTINGS_CHANGED', () => {
      const state = { ...initialGameState, lobbyState: baseLobby }
      const updatedSettings = {
        gameId: 'game-1',
        name: 'New Name',
        status: 'WAITING_FOR_PLAYERS' as const,
        hostingType: 'SERVER' as const,
        ownerName: 'Alice',
        playerCount: 1,
        maxPlayers: 6,
        isPrivate: true,
        blinds: { smallBlind: 25, bigBlind: 50, ante: 5 },
        wsUrl: null,
        createdAt: null,
        startedAt: null,
        players: [],
      }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_SETTINGS_CHANGED', { updatedSettings }),
      })
      expect(result.lobbyState?.name).toBe('New Name')
      expect(result.lobbyState?.maxPlayers).toBe(6)
      expect(result.lobbyState?.isPrivate).toBe(true)
      // Blinds must be updated from the new settings
      expect(result.lobbyState?.blinds).toEqual({ smallBlind: 25, bigBlind: 50, ante: 5 })
      // Players list must be preserved (not wiped by settings change)
      expect(result.lobbyState?.players).toHaveLength(1)
    })

    it('does not change lobbyState when lobbyState is null', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('LOBBY_SETTINGS_CHANGED', { updatedSettings: {} }),
      })
      expect(result.lobbyState).toBeNull()
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

  // -------------------------------------------------------------------------
  // Part A: New GameState fields present in initialState
  // -------------------------------------------------------------------------

  describe('initialState new fields', () => {
    it('has totalPlayers, playersRemaining, numTables, playerRank defaulting to 0', () => {
      expect(initialGameState.totalPlayers).toBe(0)
      expect(initialGameState.playersRemaining).toBe(0)
      expect(initialGameState.numTables).toBe(0)
      expect(initialGameState.playerRank).toBe(0)
    })

    it('has handHistory defaulting to empty array', () => {
      expect(initialGameState.handHistory).toEqual([])
    })

    it('has observers defaulting to empty array', () => {
      expect(initialGameState.observers).toEqual([])
    })

    it('has colorUpInProgress defaulting to false', () => {
      expect(initialGameState.colorUpInProgress).toBe(false)
    })

    it('has neverBrokeOffer defaulting to null', () => {
      expect(initialGameState.neverBrokeOffer).toBeNull()
    })

    it('has continueRunoutPending defaulting to false', () => {
      expect(initialGameState.continueRunoutPending).toBe(false)
    })

    it('has lastSequenceNumber defaulting to null', () => {
      expect(initialGameState.lastSequenceNumber).toBeNull()
    })

    it('has sequenceGapDetected defaulting to false', () => {
      expect(initialGameState.sequenceGapDetected).toBe(false)
    })
  })

  // -------------------------------------------------------------------------
  // Part B: Bug fix — handleHandStarted resets ALL_IN status
  // -------------------------------------------------------------------------

  describe('HAND_STARTED resets ALL_IN seats', () => {
    it('resets ALL_IN seats to ACTIVE on hand start', () => {
      const table = makeTable({
        seats: [
          makeSeat(0, 1, 'Alice', 1000, 'FOLDED'),
          makeSeat(1, 2, 'Bob', 800, 'ALL_IN'),
          makeSeat(2, 3, 'Carol', 600, 'ACTIVE'),
        ],
      })
      const state = stateWithTable(1, table)
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
      const seats = result.currentTable!.seats
      expect(seats.find((s) => s.playerId === 1)?.status).toBe('ACTIVE')
      expect(seats.find((s) => s.playerId === 2)?.status).toBe('ACTIVE')
      expect(seats.find((s) => s.playerId === 3)?.status).toBe('ACTIVE')
    })
  })

  // -------------------------------------------------------------------------
  // Part C: Bug fix — handlePlayerKicked removes the kicked seat
  // -------------------------------------------------------------------------

  describe('PLAYER_KICKED removes the kicked seat', () => {
    it('removes kicked player seat when another player is kicked', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_KICKED', { playerId: 2, playerName: 'Bob', reason: 'AFK' }),
      })
      expect(result.currentTable?.seats).toHaveLength(1)
      expect(result.currentTable?.seats[0].playerId).toBe(1)
    })

    it('sets error and lobby phase when the current player is kicked', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_KICKED', { playerId: 1, playerName: 'Alice', reason: 'kicked' }),
      })
      expect(result.error).toBe('You were removed from the game')
      expect(result.gamePhase).toBe('lobby')
    })
  })

  // -------------------------------------------------------------------------
  // Part D: Bug fix — handlePlayerChipChange uses absolute chipCount
  // -------------------------------------------------------------------------

  describe('PLAYER_REBUY uses absolute chipCount', () => {
    it('uses absolute chipCount from rebuy when available', () => {
      const table = makeTable({
        seats: [makeSeat(0, 1, 'Alice', 500)],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_REBUY', { playerId: 1, playerName: 'Alice', addedChips: 1000, chipCount: 1500 }),
      })
      expect(result.currentTable?.seats[0].chipCount).toBe(1500)
    })

    it('falls back to addition when chipCount is null', () => {
      const table = makeTable({
        seats: [makeSeat(0, 1, 'Alice', 500)],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_REBUY', { playerId: 1, playerName: 'Alice', addedChips: 1000, chipCount: null }),
      })
      expect(result.currentTable?.seats[0].chipCount).toBe(1500)
    })
  })

  describe('PLAYER_ADDON uses absolute chipCount', () => {
    it('uses absolute chipCount from addon when available', () => {
      const table = makeTable({
        seats: [makeSeat(0, 1, 'Alice', 500)],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_ADDON', { playerId: 1, playerName: 'Alice', addedChips: 2000, chipCount: 2500 }),
      })
      expect(result.currentTable?.seats[0].chipCount).toBe(2500)
    })
  })

  // -------------------------------------------------------------------------
  // Part E: Bug fix — handleHandComplete updates winner chip counts
  // -------------------------------------------------------------------------

  describe('HAND_COMPLETE updates winner chip counts', () => {
    it('updates chip counts for winners when chipCount is non-null', () => {
      const table = makeTable({
        seats: [
          makeSeat(0, 1, 'Alice', 500),
          makeSeat(1, 2, 'Bob', 800),
        ],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('HAND_COMPLETE', {
          handNumber: 1,
          tableId: 1,
          showdownPlayers: [],
          winners: [
            { playerId: 1, amount: 300, hand: 'Pair', cards: [], potIndex: 0, chipCount: 900 },
          ],
        }),
      })
      expect(result.currentTable?.seats.find((s) => s.playerId === 1)?.chipCount).toBe(900)
      // Bob's chip count should be unchanged
      expect(result.currentTable?.seats.find((s) => s.playerId === 2)?.chipCount).toBe(800)
    })

    it('does not update chip count when winner chipCount is null', () => {
      const table = makeTable({
        seats: [makeSeat(0, 1, 'Alice', 500)],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('HAND_COMPLETE', {
          handNumber: 1,
          tableId: 1,
          showdownPlayers: [],
          winners: [
            { playerId: 1, amount: 300, hand: 'Pair', cards: [], potIndex: 0, chipCount: null },
          ],
        }),
      })
      expect(result.currentTable?.seats[0].chipCount).toBe(500)
    })
  })

  // -------------------------------------------------------------------------
  // Part F: Bug fix — chat deduplication
  // -------------------------------------------------------------------------

  describe('chat deduplication', () => {
    it('deduplicates server echo of optimistic chat message', () => {
      const optimisticEntry = {
        id: 'opt-1',
        playerId: 42,
        playerName: 'Alice',
        message: 'Hello',
        tableChat: true,
        optimistic: true as const,
      }
      const state = { ...initialGameState, myPlayerId: 42, chatMessages: [optimisticEntry] }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('CHAT_MESSAGE', {
          playerId: 42,
          playerName: 'Alice',
          message: 'Hello',
          tableChat: true,
        }),
      })
      expect(result.chatMessages).toHaveLength(1)
      expect(result.chatMessages[0].optimistic).toBeFalsy()
    })

    it('appends non-duplicate chat messages normally', () => {
      const optimisticEntry = {
        id: 'opt-1',
        playerId: 42,
        playerName: 'Alice',
        message: 'Hello',
        tableChat: true,
        optimistic: true as const,
      }
      const state = { ...initialGameState, myPlayerId: 42, chatMessages: [optimisticEntry] }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('CHAT_MESSAGE', {
          playerId: 42,
          playerName: 'Alice',
          message: 'Different message',
          tableChat: true,
        }),
      })
      expect(result.chatMessages).toHaveLength(2)
    })
  })

  // -------------------------------------------------------------------------
  // Part G: GAME_STATE populates tournament fields
  // -------------------------------------------------------------------------

  describe('GAME_STATE tournament fields', () => {
    it('maps totalPlayers, playersRemaining, numTables, playerRank from GAME_STATE', () => {
      const gameStateData = {
        status: 'IN_PROGRESS',
        level: 2,
        blinds: { small: 50, big: 100, ante: 10 },
        nextLevelIn: 300,
        tables: [],
        players: [],
        totalPlayers: 100,
        playersRemaining: 45,
        numTables: 5,
        playerRank: 12,
      }
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('GAME_STATE', gameStateData),
      })
      expect(result.totalPlayers).toBe(100)
      expect(result.playersRemaining).toBe(45)
      expect(result.numTables).toBe(5)
      expect(result.playerRank).toBe(12)
    })
  })

  // -------------------------------------------------------------------------
  // Part H: New message type handlers
  // -------------------------------------------------------------------------

  describe('CHIPS_TRANSFERRED', () => {
    it('updates chip counts for both from and to players', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('CHIPS_TRANSFERRED', {
          fromPlayerId: 1,
          fromPlayerName: 'Alice',
          toPlayerId: 2,
          toPlayerName: 'Bob',
          amount: 200,
          fromChipCount: 800,
          toChipCount: 1000,
        }),
      })
      expect(result.currentTable?.seats.find((s) => s.playerId === 1)?.chipCount).toBe(800)
      expect(result.currentTable?.seats.find((s) => s.playerId === 2)?.chipCount).toBe(1000)
    })

    it('is a no-op when currentTable is null', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('CHIPS_TRANSFERRED', {
          fromPlayerId: 1, fromPlayerName: 'Alice',
          toPlayerId: 2, toPlayerName: 'Bob',
          amount: 200, fromChipCount: 800, toChipCount: 1000,
        }),
      })
      expect(result.currentTable).toBeNull()
    })
  })

  describe('COLOR_UP_STARTED / COLOR_UP_COMPLETED', () => {
    it('sets colorUpInProgress to true on COLOR_UP_STARTED', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('COLOR_UP_STARTED', { players: [], newMinChip: 25, tableId: 1 }),
      })
      expect(result.colorUpInProgress).toBe(true)
    })

    it('sets colorUpInProgress to false on COLOR_UP_COMPLETED', () => {
      const state = { ...initialGameState, colorUpInProgress: true }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('COLOR_UP_COMPLETED', { tableId: 1 }),
      })
      expect(result.colorUpInProgress).toBe(false)
    })
  })

  describe('AI_HOLE_CARDS', () => {
    it('sets hole cards for AI players in the table', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('AI_HOLE_CARDS', {
          players: [{ playerId: 2, cards: ['Ah', 'Kd'] }],
        }),
      })
      expect(result.currentTable?.seats.find((s) => s.playerId === 2)?.holeCards).toEqual(['Ah', 'Kd'])
    })

    it('is a no-op when currentTable is null', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('AI_HOLE_CARDS', { players: [{ playerId: 2, cards: ['Ah', 'Kd'] }] }),
      })
      expect(result.currentTable).toBeNull()
    })
  })

  describe('NEVER_BROKE_OFFERED', () => {
    it('sets neverBrokeOffer with timeoutSeconds', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('NEVER_BROKE_OFFERED', { timeoutSeconds: 30 }),
      })
      expect(result.neverBrokeOffer).toEqual({ timeoutSeconds: 30 })
    })
  })

  describe('CONTINUE_RUNOUT', () => {
    it('sets continueRunoutPending to true', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('CONTINUE_RUNOUT', null),
      })
      expect(result.continueRunoutPending).toBe(true)
    })
  })

  describe('PLAYER_SAT_OUT', () => {
    it('updates player status to SAT_OUT', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_SAT_OUT', { playerId: 2, playerName: 'Bob' }),
      })
      expect(result.currentTable?.seats.find((s) => s.playerId === 2)?.status).toBe('SAT_OUT')
    })
  })

  describe('PLAYER_CAME_BACK', () => {
    it('updates player status to ACTIVE', () => {
      const table = makeTable({
        seats: [
          makeSeat(0, 1, 'Alice', 1000, 'ACTIVE'),
          makeSeat(1, 2, 'Bob', 800, 'SAT_OUT'),
        ],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_CAME_BACK', { playerId: 2, playerName: 'Bob' }),
      })
      expect(result.currentTable?.seats.find((s) => s.playerId === 2)?.status).toBe('ACTIVE')
    })
  })

  describe('OBSERVER_JOINED / OBSERVER_LEFT', () => {
    it('adds observer on OBSERVER_JOINED', () => {
      const result = gameReducer(initialGameState, {
        type: 'SERVER_MESSAGE',
        message: msg('OBSERVER_JOINED', { observerId: 10, observerName: 'Watcher', tableId: 1 }),
      })
      expect(result.observers).toHaveLength(1)
      expect(result.observers[0]).toEqual({ observerId: 10, observerName: 'Watcher' })
    })

    it('removes observer on OBSERVER_LEFT', () => {
      const state = { ...initialGameState, observers: [{ observerId: 10, observerName: 'Watcher' }] }
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('OBSERVER_LEFT', { observerId: 10, observerName: 'Watcher', tableId: 1 }),
      })
      expect(result.observers).toHaveLength(0)
    })
  })

  describe('BUTTON_MOVED', () => {
    it('sets isDealer on the new dealer seat', () => {
      const table = makeTable({
        seats: [
          { ...makeSeat(0, 1, 'Alice', 1000), isDealer: true },
          makeSeat(1, 2, 'Bob', 800),
        ],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('BUTTON_MOVED', { tableId: 1, newSeat: 1 }),
      })
      expect(result.currentTable?.seats.find((s) => s.seatIndex === 0)?.isDealer).toBe(false)
      expect(result.currentTable?.seats.find((s) => s.seatIndex === 1)?.isDealer).toBe(true)
    })
  })

  describe('CURRENT_PLAYER_CHANGED', () => {
    it('marks the new current actor', () => {
      const table = makeTable({
        seats: [
          { ...makeSeat(0, 1, 'Alice', 1000), isCurrentActor: true },
          makeSeat(1, 2, 'Bob', 800),
        ],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('CURRENT_PLAYER_CHANGED', { tableId: 1, playerId: 2, playerName: 'Bob' }),
      })
      expect(result.currentTable?.seats.find((s) => s.playerId === 1)?.isCurrentActor).toBe(false)
      expect(result.currentTable?.seats.find((s) => s.playerId === 2)?.isCurrentActor).toBe(true)
    })
  })

  describe('TABLE_STATE_CHANGED', () => {
    it('is informational — only updates lastSequenceNumber', () => {
      const message = { ...msg('TABLE_STATE_CHANGED', { tableId: 1, oldState: 'WAITING', newState: 'PLAYING' }), sequenceNumber: 7 }
      const result = gameReducer(initialGameState, { type: 'SERVER_MESSAGE', message })
      expect(result.lastSequenceNumber).toBe(7)
    })
  })

  describe('CLEANING_DONE', () => {
    it('clears community cards, pots, currentBet, holeCards, isCurrentActor from seats', () => {
      const table = makeTable({
        communityCards: ['Ah', 'Kd', '2c'],
        pots: [{ amount: 500, eligiblePlayers: [1, 2] }],
        seats: [
          { ...makeSeat(0, 1, 'Alice', 1000), currentBet: 100, holeCards: ['As', 'Ks'], isCurrentActor: true },
          { ...makeSeat(1, 2, 'Bob', 800), currentBet: 50, holeCards: ['2h', '3h'], isCurrentActor: false },
        ],
      })
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('CLEANING_DONE', { tableId: 1 }),
      })
      expect(result.currentTable?.communityCards).toEqual([])
      expect(result.currentTable?.pots).toEqual([])
      expect(result.currentTable?.seats.every((s) => s.currentBet === 0)).toBe(true)
      expect(result.currentTable?.seats.every((s) => s.holeCards.length === 0)).toBe(true)
      expect(result.currentTable?.seats.every((s) => s.isCurrentActor === false)).toBe(true)
    })
  })

  describe('PLAYER_MOVED', () => {
    it('removes moved player from our table when they are not us', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_MOVED', {
          playerId: 2,
          playerName: 'Bob',
          fromTableId: 1,
          toTableId: 2,
        }),
      })
      expect(result.currentTable?.seats).toHaveLength(1)
      expect(result.currentTable?.seats[0].playerId).toBe(1)
    })

    it('is a no-op when moved player is the current player (server will send GAME_STATE)', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_MOVED', {
          playerId: 1,
          playerName: 'Alice',
          fromTableId: 1,
          toTableId: 2,
        }),
      })
      // Table stays intact — server will follow up with GAME_STATE
      expect(result.currentTable?.seats).toHaveLength(2)
    })
  })

  // -------------------------------------------------------------------------
  // Part H: New reducer actions (CLEAR_NEVER_BROKE, CLEAR_CONTINUE_RUNOUT, ACTION_SENT)
  // -------------------------------------------------------------------------

  describe('CLEAR_NEVER_BROKE', () => {
    it('clears neverBrokeOffer', () => {
      const state = { ...initialGameState, neverBrokeOffer: { timeoutSeconds: 30 } }
      const result = gameReducer(state, { type: 'CLEAR_NEVER_BROKE' })
      expect(result.neverBrokeOffer).toBeNull()
    })
  })

  describe('CLEAR_CONTINUE_RUNOUT', () => {
    it('clears continueRunoutPending', () => {
      const state = { ...initialGameState, continueRunoutPending: true }
      const result = gameReducer(state, { type: 'CLEAR_CONTINUE_RUNOUT' })
      expect(result.continueRunoutPending).toBe(false)
    })
  })

  describe('ACTION_SENT', () => {
    it('clears actionRequired', () => {
      const options = {
        canFold: true, canCheck: false, canCall: true, callAmount: 100,
        canBet: false, minBet: 0, maxBet: 0,
        canRaise: false, minRaise: 0, maxRaise: 0,
        canAllIn: true, allInAmount: 500,
      }
      const state = { ...initialGameState, actionRequired: options }
      const result = gameReducer(state, { type: 'ACTION_SENT' })
      expect(result.actionRequired).toBeNull()
    })
  })

  // -------------------------------------------------------------------------
  // Part I: Hand history tracking
  // -------------------------------------------------------------------------

  describe('hand history tracking', () => {
    it('appends action entry to handHistory on PLAYER_ACTED', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('PLAYER_ACTED', {
          playerId: 2,
          playerName: 'Bob',
          action: 'CALL',
          amount: 100,
          totalBet: 100,
          chipCount: 700,
          potTotal: 200,
          tableId: 1,
        }),
      })
      expect(result.handHistory).toHaveLength(1)
      expect(result.handHistory[0].type).toBe('action')
      expect(result.handHistory[0].playerName).toBe('Bob')
      expect(result.handHistory[0].action).toBe('CALL')
    })

    it('appends hand_start entry to handHistory on HAND_STARTED', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
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
      expect(result.handHistory.some((e) => e.type === 'hand_start')).toBe(true)
    })

    it('appends community entry to handHistory on COMMUNITY_CARDS_DEALT', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('COMMUNITY_CARDS_DEALT', {
          round: 'FLOP',
          cards: ['Ah', 'Kd', '2c'],
          allCommunityCards: ['Ah', 'Kd', '2c'],
          tableId: 1,
        }),
      })
      expect(result.handHistory.some((e) => e.type === 'community')).toBe(true)
      const communityEntry = result.handHistory.find((e) => e.type === 'community')
      expect(communityEntry?.round).toBe('FLOP')
      expect(communityEntry?.cards).toEqual(['Ah', 'Kd', '2c'])
    })

    it('appends result entry to handHistory on HAND_COMPLETE', () => {
      const table = makeTable()
      const state = stateWithTable(1, table)
      const result = gameReducer(state, {
        type: 'SERVER_MESSAGE',
        message: msg('HAND_COMPLETE', {
          handNumber: 1,
          tableId: 1,
          showdownPlayers: [],
          winners: [
            { playerId: 1, amount: 300, hand: 'Pair', cards: [], potIndex: 0, chipCount: 900 },
          ],
        }),
      })
      expect(result.handHistory.some((e) => e.type === 'result')).toBe(true)
    })

    it('caps handHistory at 200 entries', () => {
      let state: GameState = stateWithTable(1, makeTable())
      for (let i = 0; i < 205; i++) {
        state = gameReducer(state, {
          type: 'SERVER_MESSAGE',
          message: msg('PLAYER_ACTED', {
            playerId: 2,
            playerName: 'Bob',
            action: 'CALL',
            amount: 0,
            totalBet: 0,
            chipCount: 800,
            potTotal: 0,
            tableId: 1,
          }),
        })
      }
      expect(state.handHistory.length).toBeLessThanOrEqual(200)
    })
  })

  // -------------------------------------------------------------------------
  // Part J: Sequence number tracking
  // -------------------------------------------------------------------------

  describe('sequence number tracking', () => {
    it('updates lastSequenceNumber from server messages', () => {
      const message = { ...msg('HOLE_CARDS_DEALT', { cards: ['Ah', 'Kd'] }), sequenceNumber: 5 }
      const result = gameReducer(initialGameState, { type: 'SERVER_MESSAGE', message })
      expect(result.lastSequenceNumber).toBe(5)
    })

    it('detects sequence gaps and sets sequenceGapDetected', () => {
      const state = { ...initialGameState, lastSequenceNumber: 3 }
      // Sequence jumps from 3 to 5 — gap detected
      const message = { ...msg('HOLE_CARDS_DEALT', { cards: ['Ah', 'Kd'] }), sequenceNumber: 5 }
      const result = gameReducer(state, { type: 'SERVER_MESSAGE', message })
      expect(result.sequenceGapDetected).toBe(true)
    })

    it('does not set sequenceGapDetected when sequence is consecutive', () => {
      const state = { ...initialGameState, lastSequenceNumber: 4 }
      const message = { ...msg('HOLE_CARDS_DEALT', { cards: ['Ah', 'Kd'] }), sequenceNumber: 5 }
      const result = gameReducer(state, { type: 'SERVER_MESSAGE', message })
      expect(result.sequenceGapDetected).toBe(false)
    })
  })
})
