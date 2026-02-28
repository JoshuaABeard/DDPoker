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
  AiHoleCardsData,
  ButtonMovedData,
  ChatMessageData,
  ChipsTransferredData,
  CommunityCardsDealtData,
  ConnectedData,
  ConnectionState,
  CurrentPlayerChangedData,
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
  NeverBrokeOfferedData,
  ObserverJoinedData,
  ObserverLeftData,
  PlayerActedData,
  PlayerAddonData,
  PlayerCameBackData,
  PlayerDisconnectedData,
  PlayerEliminatedData,
  PlayerJoinedData,
  PlayerKickedData,
  PlayerLeftData,
  PlayerMovedData,
  PlayerRebuyData,
  PlayerSatOutData,
  PotAwardedData,
  RebuyOfferedData,
  ServerMessage,
  StandingData,
  TableData,
  TimerUpdateData,
} from './types'

/** Maximum chat messages to keep (FIFO — oldest dropped on overflow). */
const MAX_CHAT_MESSAGES = 200

/** Maximum hand history entries to keep (FIFO). */
const MAX_HAND_HISTORY = 200

export interface HandHistoryEntry {
  id: string
  handNumber: number
  type: 'action' | 'community' | 'result' | 'hand_start'
  playerName?: string
  action?: string
  amount?: number
  round?: string
  cards?: string[]
  winners?: { playerName: string; amount: number; hand: string }[]
  timestamp: number
}

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
  isOwner: boolean

  // Tournament stats (from GAME_STATE)
  totalPlayers: number
  playersRemaining: number
  numTables: number
  playerRank: number

  // Hand history
  handHistory: HandHistoryEntry[]

  // Observers
  observers: { observerId: number; observerName: string }[]

  // Color-up
  colorUpInProgress: boolean

  // Overlay prompts
  neverBrokeOffer: { timeoutSeconds: number } | null
  continueRunoutPending: boolean

  // Sequence tracking
  lastSequenceNumber: number | null
  sequenceGapDetected: boolean
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
  isOwner: false,
  totalPlayers: 0,
  playersRemaining: 0,
  numTables: 0,
  playerRank: 0,
  handHistory: [],
  observers: [],
  colorUpInProgress: false,
  neverBrokeOffer: null,
  continueRunoutPending: false,
  lastSequenceNumber: null,
  sequenceGapDetected: false,
}

// ---------------------------------------------------------------------------
// Reducer action types (internal, not to be confused with poker actions)
// ---------------------------------------------------------------------------

export type ReducerAction =
  | { type: 'CONNECTION_STATE_CHANGED'; state: ConnectionState }
  | { type: 'SERVER_MESSAGE'; message: ServerMessage }
  | { type: 'CHAT_OPTIMISTIC'; entry: ChatEntry }
  | { type: 'CLEAR_OFFERS' }
  | { type: 'CLEAR_NEVER_BROKE' }
  | { type: 'CLEAR_CONTINUE_RUNOUT' }
  | { type: 'ACTION_SENT' }
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

    case 'CLEAR_NEVER_BROKE':
      return { ...state, neverBrokeOffer: null }

    case 'CLEAR_CONTINUE_RUNOUT':
      return { ...state, continueRunoutPending: false }

    case 'ACTION_SENT':
      return { ...state, actionRequired: null }

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

  // Sequence gap detection
  const seq = message.sequenceNumber
  let sequenceGapDetected = state.sequenceGapDetected
  if (seq != null && state.lastSequenceNumber != null && seq > state.lastSequenceNumber + 1) {
    console.warn(`[gameReducer] Sequence gap detected: expected ${state.lastSequenceNumber + 1}, got ${seq}`)
    sequenceGapDetected = true
  } else if (seq != null) {
    sequenceGapDetected = false
  }

  const seqState = { lastSequenceNumber: seq ?? state.lastSequenceNumber, sequenceGapDetected }

  try {
    switch (message.type) {
      // Connection lifecycle
      case 'CONNECTED':
        return { ...handleConnected(state, message.gameId, message.data as ConnectedData), ...seqState }

      case 'ERROR':
        return { ...state, error: (message.data as { code: string; message: string }).message, ...seqState }

      // Lobby messages
      case 'LOBBY_STATE':
        return { ...handleLobbyState(state, message.data as LobbyStateData), ...seqState }

      case 'LOBBY_PLAYER_JOINED':
        return { ...handleLobbyPlayerJoined(state, (message.data as LobbyPlayerJoinedData).player), ...seqState }

      case 'LOBBY_PLAYER_LEFT':
        return { ...handleLobbyPlayerLeft(state, (message.data as LobbyPlayerLeftData).player), ...seqState }

      case 'LOBBY_PLAYER_KICKED':
        return { ...handleLobbyPlayerLeft(state, (message.data as LobbyPlayerKickedData).player), ...seqState }

      case 'LOBBY_SETTINGS_CHANGED':
        return { ...handleLobbySettingsChanged(state, message.data as LobbySettingsChangedData), ...seqState }

      case 'LOBBY_GAME_STARTING':
        return { ...handleLobbyGameStarting(state, message.data as LobbyGameStartingData), ...seqState }

      case 'GAME_CANCELLED':
        return { ...handleGameCancelled(state, message.data as GameCancelledData), ...seqState }

      // Game state
      case 'GAME_STATE':
        return { ...handleGameState(state, message.data as GameStateData), ...seqState }

      case 'HAND_STARTED':
        return { ...handleHandStarted(state, message.data as HandStartedData), ...seqState }

      case 'HOLE_CARDS_DEALT':
        return { ...handleHoleCardsDealt(state, message.data as HoleCardsDealtData), ...seqState }

      case 'COMMUNITY_CARDS_DEALT':
        return { ...handleCommunityCardsDealt(state, message.data as CommunityCardsDealtData), ...seqState }

      case 'ACTION_REQUIRED':
        return { ...handleActionRequired(state, message.data as ActionRequiredData), ...seqState }

      case 'PLAYER_ACTED':
        return { ...handlePlayerActed(state, message.data as PlayerActedData), ...seqState }

      case 'ACTION_TIMEOUT':
        return { ...handleActionTimeout(state), ...seqState }

      case 'HAND_COMPLETE':
        return { ...handleHandComplete(state, message.data as HandCompleteData), ...seqState }

      case 'LEVEL_CHANGED':
        return { ...handleLevelChanged(state, message.data as LevelChangedData), ...seqState }

      case 'PLAYER_ELIMINATED':
        return { ...handlePlayerEliminated(state, message.data as PlayerEliminatedData), ...seqState }

      case 'REBUY_OFFERED':
        return { ...state, rebuyOffer: message.data as RebuyOfferedData, ...seqState }

      case 'ADDON_OFFERED':
        return { ...state, addonOffer: message.data as AddonOfferedData, ...seqState }

      case 'GAME_COMPLETE':
        return { ...handleGameComplete(state, message.data as GameCompleteData), ...seqState }

      // Player events
      case 'PLAYER_JOINED':
        return { ...handlePlayerJoined(state, message.data as PlayerJoinedData), ...seqState }

      case 'PLAYER_LEFT':
        return { ...handlePlayerLeft(state, message.data as PlayerLeftData), ...seqState }

      case 'PLAYER_DISCONNECTED':
        return { ...handlePlayerDisconnected(state, message.data as PlayerDisconnectedData), ...seqState }

      case 'PLAYER_KICKED':
        return { ...handlePlayerKicked(state, message.data as PlayerKickedData), ...seqState }

      case 'PLAYER_REBUY': {
        const d = message.data as PlayerRebuyData
        return { ...handlePlayerChipChange(state, d.playerId, d.addedChips, d.chipCount), ...seqState }
      }

      case 'PLAYER_ADDON': {
        const d = message.data as PlayerAddonData
        return { ...handlePlayerChipChange(state, d.playerId, d.addedChips, d.chipCount), ...seqState }
      }

      case 'POT_AWARDED':
        return { ...handlePotAwarded(state, message.data as PotAwardedData), ...seqState }

      case 'SHOWDOWN_STARTED':
        return { ...state, ...seqState } // Visual effect only — no state change

      // Admin events
      case 'GAME_PAUSED':
        return { ...handleGamePaused(state, message.data as GamePausedData), ...seqState }

      case 'GAME_RESUMED':
        return { ...handleGameResumed(state, message.data as GameResumedData), ...seqState }

      // Chat
      case 'CHAT_MESSAGE':
        return { ...handleChatMessage(state, message.data as ChatMessageData), ...seqState }

      case 'TIMER_UPDATE':
        return { ...handleTimerUpdate(state, message.data as TimerUpdateData), ...seqState }

      // New message types (Phase 2)
      case 'CHIPS_TRANSFERRED':
        return { ...handleChipsTransferred(state, message.data as ChipsTransferredData), ...seqState }

      case 'COLOR_UP_STARTED':
        return { ...state, colorUpInProgress: true, ...seqState }

      case 'COLOR_UP_COMPLETED':
        return { ...state, colorUpInProgress: false, ...seqState }

      case 'AI_HOLE_CARDS':
        return { ...handleAiHoleCards(state, message.data as AiHoleCardsData), ...seqState }

      case 'NEVER_BROKE_OFFERED': {
        const d = message.data as NeverBrokeOfferedData
        return { ...state, neverBrokeOffer: { timeoutSeconds: d.timeoutSeconds }, ...seqState }
      }

      case 'CONTINUE_RUNOUT':
        return { ...state, continueRunoutPending: true, ...seqState }

      case 'PLAYER_SAT_OUT':
        return { ...handlePlayerSatOut(state, message.data as PlayerSatOutData), ...seqState }

      case 'PLAYER_CAME_BACK':
        return { ...handlePlayerCameBack(state, message.data as PlayerCameBackData), ...seqState }

      case 'OBSERVER_JOINED': {
        const d = message.data as ObserverJoinedData
        return {
          ...state,
          observers: [...state.observers, { observerId: d.observerId, observerName: d.observerName }],
          ...seqState,
        }
      }

      case 'OBSERVER_LEFT': {
        const d = message.data as ObserverLeftData
        return {
          ...state,
          observers: state.observers.filter((o) => o.observerId !== d.observerId),
          ...seqState,
        }
      }

      case 'BUTTON_MOVED':
        return { ...handleButtonMoved(state, message.data as ButtonMovedData), ...seqState }

      case 'CURRENT_PLAYER_CHANGED':
        return { ...handleCurrentPlayerChanged(state, message.data as CurrentPlayerChangedData), ...seqState }

      case 'TABLE_STATE_CHANGED':
        // Informational only — no UI state change needed
        return { ...state, ...seqState }

      case 'CLEANING_DONE':
        return { ...handleCleaningDone(state), ...seqState }

      case 'PLAYER_MOVED':
        return { ...handlePlayerMoved(state, message.data as PlayerMovedData), ...seqState }

      default:
        console.warn('[gameReducer] Unknown server message type:', message.type)
        return { ...state, ...seqState }
    }
  } catch (err) {
    console.warn('[gameReducer] Error processing message:', message.type, err)
    return { ...state, ...seqState }
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
    isOwner: state.myPlayerId != null && state.myPlayerId === data.ownerProfileId,
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
      ownerName: s.ownerName,
      blinds: s.blinds ?? state.lobbyState.blinds,
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
    // Tournament stats
    totalPlayers: data.totalPlayers,
    playersRemaining: data.playersRemaining,
    numTables: data.numTables,
    playerRank: data.playerRank,
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
          status: seat.status === 'FOLDED' || seat.status === 'ALL_IN' ? 'ACTIVE' : seat.status,
          isCurrentActor: false,
        })),
        communityCards: [],
        pots: [],
      }
    : state.currentTable
  const handStartEntry: HandHistoryEntry = {
    id: `${data.handNumber}-hand_start-${Date.now()}`,
    handNumber: data.handNumber,
    type: 'hand_start',
    timestamp: Date.now(),
  }
  return {
    ...state,
    holeCards: [],
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    rebuyOffer: null,
    addonOffer: null,
    currentTable: updatedTable,
    handHistory: [...state.handHistory, handStartEntry].slice(-MAX_HAND_HISTORY),
  }
}

function handleHoleCardsDealt(state: GameState, data: HoleCardsDealtData): GameState {
  return { ...state, holeCards: data.cards }
}

function handleCommunityCardsDealt(state: GameState, data: CommunityCardsDealtData): GameState {
  if (!state.currentTable) return state
  const communityEntry: HandHistoryEntry = {
    id: `${state.currentTable.handNumber}-community-${data.round}-${Date.now()}`,
    handNumber: state.currentTable.handNumber,
    type: 'community',
    round: data.round,
    cards: data.cards,
    timestamp: Date.now(),
  }
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      communityCards: data.allCommunityCards,
      currentRound: data.round,
    },
    handHistory: [...state.handHistory, communityEntry].slice(-MAX_HAND_HISTORY),
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
  const actionEntry: HandHistoryEntry = {
    id: `${newState.currentTable?.handNumber ?? 0}-${data.playerName}-${Date.now()}`,
    handNumber: newState.currentTable?.handNumber ?? 0,
    type: 'action',
    playerName: data.playerName,
    action: data.action,
    amount: data.amount,
    timestamp: Date.now(),
  }
  return {
    ...newState,
    handHistory: [...newState.handHistory, actionEntry].slice(-MAX_HAND_HISTORY),
  }
}

function handleActionTimeout(state: GameState): GameState {
  return { ...state, actionRequired: null, actionTimeoutSeconds: null, actionTimer: null }
}

function handleHandComplete(state: GameState, data: HandCompleteData): GameState {
  // Update winner chip counts in the table
  let updatedTable = state.currentTable
  if (updatedTable && data.winners.length > 0) {
    updatedTable = {
      ...updatedTable,
      seats: updatedTable.seats.map((seat) => {
        const winner = data.winners.find((w) => w.playerId === seat.playerId)
        if (winner && winner.chipCount != null) {
          return { ...seat, chipCount: winner.chipCount }
        }
        return seat
      }),
    }
  }
  const resultEntry: HandHistoryEntry = {
    id: `${data.handNumber}-result-${Date.now()}`,
    handNumber: data.handNumber,
    type: 'result',
    winners: data.winners.map((w) => {
      const seat = state.currentTable?.seats.find((s) => s.playerId === w.playerId)
      return { playerName: seat?.playerName ?? String(w.playerId), amount: w.amount, hand: w.hand }
    }),
    timestamp: Date.now(),
  }
  return {
    ...state,
    currentTable: updatedTable,
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    holeCards: [],
    handHistory: [...state.handHistory, resultEntry].slice(-MAX_HAND_HISTORY),
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
  // Remove the kicked player's seat from the table
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.filter((s) => s.playerId !== data.playerId),
    },
  }
}

function handlePlayerChipChange(
  state: GameState,
  playerId: number,
  addedChips: number,
  absoluteChipCount: number | null,
): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((seat) =>
        seat.playerId === playerId
          ? { ...seat, chipCount: absoluteChipCount != null ? absoluteChipCount : seat.chipCount + addedChips }
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
  // Deduplicate: if there is an optimistic entry with the same playerId + message, replace it
  const optimisticIndex = state.chatMessages.findIndex(
    (m) => m.optimistic && m.playerId === data.playerId && m.message === data.message,
  )
  if (optimisticIndex !== -1) {
    const updated = [...state.chatMessages]
    updated[optimisticIndex] = entry
    return { ...state, chatMessages: updated }
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
// New message type handlers (Phase 2)
// ---------------------------------------------------------------------------

function handleChipsTransferred(state: GameState, data: ChipsTransferredData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => {
        if (s.playerId === data.fromPlayerId && data.fromChipCount != null) return { ...s, chipCount: data.fromChipCount }
        if (s.playerId === data.toPlayerId && data.toChipCount != null) return { ...s, chipCount: data.toChipCount }
        return s
      }),
    },
  }
}

function handleAiHoleCards(state: GameState, data: AiHoleCardsData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => {
        const aiCards = data.players.find((p) => p.playerId === s.playerId)
        return aiCards ? { ...s, holeCards: aiCards.cards } : s
      }),
    },
  }
}

function handlePlayerSatOut(state: GameState, data: PlayerSatOutData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) =>
        s.playerId === data.playerId ? { ...s, status: 'SAT_OUT' } : s,
      ),
    },
  }
}

function handlePlayerCameBack(state: GameState, data: PlayerCameBackData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) =>
        s.playerId === data.playerId ? { ...s, status: 'ACTIVE' } : s,
      ),
    },
  }
}

function handleButtonMoved(state: GameState, data: ButtonMovedData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => ({ ...s, isDealer: s.seatIndex === data.newSeat })),
    },
  }
}

function handleCurrentPlayerChanged(state: GameState, data: CurrentPlayerChangedData): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => ({ ...s, isCurrentActor: s.playerId === data.playerId })),
    },
  }
}

function handleCleaningDone(state: GameState): GameState {
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      communityCards: [],
      pots: [],
      seats: state.currentTable.seats.map((s) => ({
        ...s,
        currentBet: 0,
        holeCards: [],
        isCurrentActor: false,
      })),
    },
  }
}

function handlePlayerMoved(state: GameState, data: PlayerMovedData): GameState {
  if (!state.currentTable) return state
  // If moved player is us, server will send a new GAME_STATE — just wait
  if (data.playerId === state.myPlayerId) return state
  // Remove moved player from our table
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.filter((s) => s.playerId !== data.playerId),
    },
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
