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

import type {
  ActionOptionsData,
  ActionRequiredData,
  AddonOfferedData,
  ChatMessageData,
  CommunityCardsDealtData,
  ConnectedData,
  ConnectionState,
  GameCancelledData,
  GameCompleteData,
  GamePausedData,
  GameResumedData,
  GameStateData,
  HandCompleteData,
  HandStartedData,
  HoleCardsDealtData,
  LevelChangedData,
  LobbyGameStartingData,
  LobbyPlayerData,
  LobbyPlayerJoinedData,
  LobbyPlayerKickedData,
  LobbyPlayerLeftData,
  LobbySettingsChangedData,
  LobbyStateData,
  PlayerActedData,
  PlayerAddonData,
  PlayerDisconnectedData,
  PlayerEliminatedData,
  PlayerJoinedData,
  PlayerKickedData,
  PlayerLeftData,
  PlayerRebuyData,
  PotAwardedData,
  RebuyOfferedData,
  ServerMessage,
  StandingData,
  TableData,
  TimerUpdateData,
} from './types'

/** Maximum chat messages to keep (FIFO — oldest dropped on overflow). */
const MAX_CHAT_MESSAGES = 200

export interface ChatEntry {
  id: string
  playerId: number
  playerName: string
  message: string
  tableChat: boolean
  /** Optimistic: true for locally-sent messages before server echo */
  optimistic?: boolean
}

export interface TimerState {
  playerId: number
  secondsRemaining: number
}

export type GamePhase = 'lobby' | 'playing' | 'complete'

export interface GameState {
  connectionState: ConnectionState
  gamePhase: GamePhase
  gameId: string | null
  /** This client's player ID (from CONNECTED message) */
  myPlayerId: number | null
  /** Reconnect token from CONNECTED — stored in memory only */
  reconnectToken: string | null
  lobbyState: LobbyStateData | null
  gameState: GameStateData | null
  /** The table containing this player */
  currentTable: TableData | null
  /** This player's hole cards */
  holeCards: string[]
  /** Non-null when this client must act */
  actionRequired: ActionOptionsData | null
  /** Timeout for current action */
  actionTimeoutSeconds: number | null
  /** Current actor timer */
  actionTimer: TimerState | null
  chatMessages: ChatEntry[]
  standings: StandingData[]
  /** Pending rebuy offer for this player — cleared on hand start or CLEAR_OFFERS */
  rebuyOffer: RebuyOfferedData | null
  /** Pending addon offer for this player — cleared on hand start or CLEAR_OFFERS */
  addonOffer: AddonOfferedData | null
  /** Set when the current player is eliminated; position shown in overlay */
  eliminatedPosition: number | null
  error: string | null
}

export const initialGameState: GameState = {
  connectionState: 'DISCONNECTED',
  gamePhase: 'lobby',
  gameId: null,
  myPlayerId: null,
  reconnectToken: null,
  lobbyState: null,
  gameState: null,
  currentTable: null,
  holeCards: [],
  actionRequired: null,
  actionTimeoutSeconds: null,
  actionTimer: null,
  chatMessages: [],
  standings: [],
  rebuyOffer: null,
  addonOffer: null,
  eliminatedPosition: null,
  error: null,
}

// ---------------------------------------------------------------------------
// Reducer action types (internal, not to be confused with poker actions)
// ---------------------------------------------------------------------------

export type ReducerAction =
  | { type: 'CONNECTION_STATE_CHANGED'; state: ConnectionState }
  | { type: 'SERVER_MESSAGE'; message: ServerMessage }
  | { type: 'CHAT_OPTIMISTIC'; entry: ChatEntry }
  | { type: 'CLEAR_OFFERS' }
  | { type: 'RESET' }

// ---------------------------------------------------------------------------
// Main reducer
// ---------------------------------------------------------------------------

export function gameReducer(state: GameState, action: ReducerAction): GameState {
  switch (action.type) {
    case 'CONNECTION_STATE_CHANGED':
      // Short-circuit: avoid re-render if state hasn't changed
      if (action.state === state.connectionState) return state
      return { ...state, connectionState: action.state }

    case 'CHAT_OPTIMISTIC':
      return { ...state, chatMessages: appendChat(state.chatMessages, action.entry) }

    case 'CLEAR_OFFERS':
      return { ...state, rebuyOffer: null, addonOffer: null }

    case 'RESET':
      return { ...initialGameState }

    case 'SERVER_MESSAGE':
      return handleServerMessage(state, action.message)

    default:
      return state
  }
}

// ---------------------------------------------------------------------------
// Server message dispatch
// ---------------------------------------------------------------------------

function handleServerMessage(state: GameState, message: ServerMessage): GameState {
  // Validate basic message shape
  if (!message || typeof message.type !== 'string') {
    console.warn('[gameReducer] Malformed message (missing type):', message)
    return state
  }

  try {
    switch (message.type) {
      // Connection lifecycle
      case 'CONNECTED':
        return handleConnected(state, message.gameId, message.data as ConnectedData)

      case 'ERROR':
        return { ...state, error: (message.data as { code: string; message: string }).message }

      // Lobby messages
      case 'LOBBY_STATE':
        return handleLobbyState(state, message.data as LobbyStateData)

      case 'LOBBY_PLAYER_JOINED':
        return handleLobbyPlayerJoined(state, (message.data as LobbyPlayerJoinedData).player)

      case 'LOBBY_PLAYER_LEFT':
        return handleLobbyPlayerLeft(state, (message.data as LobbyPlayerLeftData).player)

      case 'LOBBY_PLAYER_KICKED':
        return handleLobbyPlayerLeft(state, (message.data as LobbyPlayerKickedData).player)

      case 'LOBBY_SETTINGS_CHANGED':
        return handleLobbySettingsChanged(state, message.data as LobbySettingsChangedData)

      case 'LOBBY_GAME_STARTING':
        return handleLobbyGameStarting(state, message.data as LobbyGameStartingData)

      case 'GAME_CANCELLED':
        return handleGameCancelled(state, message.data as GameCancelledData)

      // Game state
      case 'GAME_STATE':
        return handleGameState(state, message.data as GameStateData)

      case 'HAND_STARTED':
        return handleHandStarted(state, message.data as HandStartedData)

      case 'HOLE_CARDS_DEALT':
        return handleHoleCardsDealt(state, message.data as HoleCardsDealtData)

      case 'COMMUNITY_CARDS_DEALT':
        return handleCommunityCardsDealt(state, message.data as CommunityCardsDealtData)

      case 'ACTION_REQUIRED':
        return handleActionRequired(state, message.data as ActionRequiredData)

      case 'PLAYER_ACTED':
        return handlePlayerActed(state, message.data as PlayerActedData)

      case 'ACTION_TIMEOUT':
        return handleActionTimeout(state)

      case 'HAND_COMPLETE':
        return handleHandComplete(state, message.data as HandCompleteData)

      case 'LEVEL_CHANGED':
        return handleLevelChanged(state, message.data as LevelChangedData)

      case 'PLAYER_ELIMINATED':
        return handlePlayerEliminated(state, message.data as PlayerEliminatedData)

      case 'REBUY_OFFERED':
        return { ...state, rebuyOffer: message.data as RebuyOfferedData }

      case 'ADDON_OFFERED':
        return { ...state, addonOffer: message.data as AddonOfferedData }

      case 'GAME_COMPLETE':
        return handleGameComplete(state, message.data as GameCompleteData)

      // Player events
      case 'PLAYER_JOINED':
        return handlePlayerJoined(state, message.data as PlayerJoinedData)

      case 'PLAYER_LEFT':
        return handlePlayerLeft(state, message.data as PlayerLeftData)

      case 'PLAYER_DISCONNECTED':
        return handlePlayerDisconnected(state, message.data as PlayerDisconnectedData)

      case 'PLAYER_KICKED':
        return handlePlayerKicked(state, message.data as PlayerKickedData)

      case 'PLAYER_REBUY':
        return handlePlayerChipChange(state, (message.data as PlayerRebuyData).playerId,
          (message.data as PlayerRebuyData).addedChips, true)

      case 'PLAYER_ADDON':
        return handlePlayerChipChange(state, (message.data as PlayerAddonData).playerId,
          (message.data as PlayerAddonData).addedChips, true)

      case 'POT_AWARDED':
        return handlePotAwarded(state, message.data as PotAwardedData)

      case 'SHOWDOWN_STARTED':
        return state // Visual effect only — no state change

      // Admin events
      case 'GAME_PAUSED':
        return handleGamePaused(state, message.data as GamePausedData)

      case 'GAME_RESUMED':
        return handleGameResumed(state, message.data as GameResumedData)

      // Chat
      case 'CHAT_MESSAGE':
        return handleChatMessage(state, message.data as ChatMessageData)

      case 'TIMER_UPDATE':
        return handleTimerUpdate(state, message.data as TimerUpdateData)

      default:
        console.warn('[gameReducer] Unknown server message type:', message.type)
        return state
    }
  } catch (err) {
    console.warn('[gameReducer] Error processing message:', message.type, err)
    return state
  }
}

// ---------------------------------------------------------------------------
// Individual message handlers
// ---------------------------------------------------------------------------

function handleConnected(state: GameState, gameId: string, data: ConnectedData): GameState {
  return {
    ...state,
    gameId,
    myPlayerId: data.playerId,
    reconnectToken: data.reconnectToken,
    // gameState from CONNECTED is non-null only for reconnects to in-progress games
    gamePhase: data.gameState ? 'playing' : state.gamePhase,
    gameState: data.gameState ?? state.gameState,
    currentTable: data.gameState
      ? findMyTable(data.gameState, data.playerId)
      : state.currentTable,
    error: null,
  }
}

function handleLobbyState(state: GameState, data: LobbyStateData): GameState {
  return {
    ...state,
    gamePhase: 'lobby',
    lobbyState: data,
    error: null,
  }
}

function handleLobbyPlayerJoined(state: GameState, player: LobbyPlayerData): GameState {
  if (!state.lobbyState) return state
  const exists = state.lobbyState.players.some((p) => p.profileId === player.profileId)
  if (exists) return state
  return {
    ...state,
    lobbyState: {
      ...state.lobbyState,
      players: [...state.lobbyState.players, player],
    },
  }
}

function handleLobbyPlayerLeft(state: GameState, player: LobbyPlayerData): GameState {
  if (!state.lobbyState) return state
  return {
    ...state,
    lobbyState: {
      ...state.lobbyState,
      players: state.lobbyState.players.filter((p) => p.profileId !== player.profileId),
    },
  }
}

function handleLobbySettingsChanged(state: GameState, data: LobbySettingsChangedData): GameState {
  if (!state.lobbyState) return state
  const s = data.updatedSettings
  return {
    ...state,
    lobbyState: {
      ...state.lobbyState,
      name: s.name,
      maxPlayers: s.maxPlayers,
      isPrivate: s.isPrivate,
      blinds: s.blinds,
      ownerName: s.ownerName,
      ownerProfileId: s.ownerProfileId,
    },
  }
}

function handleLobbyGameStarting(state: GameState, _data: LobbyGameStartingData): GameState {
  // Page navigation is handled by the context; reducer just notes the transition
  return { ...state, gamePhase: 'playing' }
}

function handleGameCancelled(state: GameState, _data: GameCancelledData): GameState {
  return { ...state, error: 'Game was cancelled', gamePhase: 'lobby' }
}

function handleGameState(state: GameState, data: GameStateData): GameState {
  const myPlayerId = state.myPlayerId
  return {
    ...state,
    gamePhase: 'playing',
    gameState: data,
    currentTable: myPlayerId != null ? findMyTable(data, myPlayerId) : null,
    // Clear action state on full state refresh
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
  }
}

function handleHandStarted(state: GameState, data: HandStartedData): GameState {
  // Update dealer/blind seat markers in the current table
  const updatedTable = state.currentTable
    ? {
        ...state.currentTable,
        handNumber: data.handNumber,
        seats: state.currentTable.seats.map((seat) => ({
          ...seat,
          isDealer: seat.seatIndex === data.dealerSeat,
          isSmallBlind: seat.seatIndex === data.smallBlindSeat,
          isBigBlind: seat.seatIndex === data.bigBlindSeat,
          // Reset per-hand state
          holeCards: [],
          currentBet: 0,
          status: seat.status === 'FOLDED' ? 'ACTIVE' : seat.status,
          isCurrentActor: false,
        })),
        communityCards: [],
        pots: [],
      }
    : state.currentTable
  return {
    ...state,
    holeCards: [],
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    rebuyOffer: null,
    addonOffer: null,
    currentTable: updatedTable,
  }
}

function handleHoleCardsDealt(state: GameState, data: HoleCardsDealtData): GameState {
  return { ...state, holeCards: data.cards }
}

function handleCommunityCardsDealt(state: GameState, data: CommunityCardsDealtData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      communityCards: data.allCommunityCards,
      currentRound: data.round,
    },
  }
}

function handleActionRequired(state: GameState, data: ActionRequiredData): GameState {
  return {
    ...state,
    actionRequired: data.options,
    actionTimeoutSeconds: data.timeoutSeconds,
    actionTimer: null,
  }
}

function handlePlayerActed(state: GameState, data: PlayerActedData): GameState {
  let newState = state
  // Clear my action panel if I was the one who acted
  if (data.playerId === state.myPlayerId) {
    newState = { ...newState, actionRequired: null, actionTimeoutSeconds: null }
  }
  // Update seat state in current table
  if (newState.currentTable) {
    const updatedSeats = newState.currentTable.seats.map((seat) => {
      if (seat.playerId !== data.playerId) return seat
      const action = data.action.toUpperCase()
      return {
        ...seat,
        chipCount: data.chipCount,
        currentBet: data.totalBet,
        status: action === 'FOLD' ? 'FOLDED' : action === 'ALL_IN' ? 'ALL_IN' : 'ACTIVE',
        isCurrentActor: false,
      }
    })
    newState = {
      ...newState,
      currentTable: { ...newState.currentTable, seats: updatedSeats },
      actionTimer: null,
    }
  }
  return newState
}

function handleActionTimeout(state: GameState): GameState {
  return { ...state, actionRequired: null, actionTimeoutSeconds: null, actionTimer: null }
}

function handleHandComplete(state: GameState, _data: HandCompleteData): GameState {
  return {
    ...state,
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    holeCards: [],
  }
}

function handleLevelChanged(state: GameState, data: LevelChangedData): GameState {
  if (!state.gameState) return state
  return {
    ...state,
    gameState: {
      ...state.gameState,
      level: data.level,
      blinds: { small: data.smallBlind, big: data.bigBlind, ante: data.ante },
      nextLevelIn: data.nextLevelIn,
    },
  }
}

function handlePlayerEliminated(state: GameState, data: PlayerEliminatedData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.filter((s) => s.playerId !== data.playerId),
    },
    // Record finish position so the play page can show an eliminated overlay
    eliminatedPosition: data.playerId === state.myPlayerId
      ? data.finishPosition
      : state.eliminatedPosition,
  }
}

function handleGameComplete(state: GameState, data: GameCompleteData): GameState {
  return {
    ...state,
    gamePhase: 'complete',
    standings: data.standings,
    actionRequired: null,
    actionTimeoutSeconds: null,
  }
}

function handlePlayerJoined(state: GameState, _data: PlayerJoinedData): GameState {
  // Intentional no-op: mid-game joins happen when tables rebalance in multi-table
  // tournaments. The server sends GAME_STATE immediately after PLAYER_JOINED with
  // accurate chip counts. Patching here would require fabricating unknown fields.
  return state
}

function handlePlayerLeft(state: GameState, data: PlayerLeftData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.filter((s) => s.playerId !== data.playerId),
    },
  }
}

function handlePlayerDisconnected(state: GameState, data: PlayerDisconnectedData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) =>
        s.playerId === data.playerId ? { ...s, status: 'DISCONNECTED' } : s,
      ),
    },
  }
}

function handlePlayerKicked(state: GameState, data: PlayerKickedData): GameState {
  // If this player was kicked, error state is set; parent handles redirect
  if (data.playerId === state.myPlayerId) {
    return { ...state, error: 'You were removed from the game', gamePhase: 'lobby' }
  }
  return state
}

function handlePlayerChipChange(
  state: GameState,
  playerId: number,
  addedChips: number,
  add: boolean,
): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((seat) =>
        seat.playerId === playerId
          ? { ...seat, chipCount: add ? seat.chipCount + addedChips : addedChips }
          : seat,
      ),
    },
  }
}

function handlePotAwarded(state: GameState, _data: PotAwardedData): GameState {
  // Pot changes are reflected in the next GAME_STATE or HAND_STARTED event
  return state
}

function handleGamePaused(state: GameState, _data: GamePausedData): GameState {
  if (!state.gameState) return state
  return { ...state, gameState: { ...state.gameState, status: 'PAUSED' } }
}

function handleGameResumed(state: GameState, _data: GameResumedData): GameState {
  if (!state.gameState) return state
  return { ...state, gameState: { ...state.gameState, status: 'IN_PROGRESS' } }
}

function handleChatMessage(state: GameState, data: ChatMessageData): GameState {
  const entry: ChatEntry = {
    id: `${data.playerId}-${Date.now()}-${Math.random()}`,
    playerId: data.playerId,
    playerName: data.playerName,
    message: data.message,
    tableChat: data.tableChat,
  }
  return { ...state, chatMessages: appendChat(state.chatMessages, entry) }
}

function handleTimerUpdate(state: GameState, data: TimerUpdateData): GameState {
  return {
    ...state,
    actionTimer: { playerId: data.playerId, secondsRemaining: data.secondsRemaining },
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function findMyTable(gameState: GameStateData, myPlayerId: number) {
  const mySummary = gameState.players.find((p) => p.playerId === myPlayerId)
  if (!mySummary) return gameState.tables[0] ?? null
  return gameState.tables.find((t) => t.tableId === mySummary.tableId) ?? gameState.tables[0] ?? null
}

function appendChat(messages: ChatEntry[], entry: ChatEntry): ChatEntry[] {
  const next = [...messages, entry]
  if (next.length > MAX_CHAT_MESSAGES) {
    return next.slice(next.length - MAX_CHAT_MESSAGES)
  }
  return next
}
