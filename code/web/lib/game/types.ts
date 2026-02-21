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

/**
 * TypeScript types for the DD Poker WebSocket protocol.
 *
 * Mirrors ServerMessageData.java and ClientMessageData.java exactly.
 * Source of truth: pokergameserver websocket/message/*.java
 */

// ============================================================================
// Server message types
// ============================================================================

export type ServerMessageType =
  | 'CONNECTED'
  | 'GAME_STATE'
  | 'HAND_STARTED'
  | 'HOLE_CARDS_DEALT'
  | 'COMMUNITY_CARDS_DEALT'
  | 'ACTION_REQUIRED'
  | 'PLAYER_ACTED'
  | 'ACTION_TIMEOUT'
  | 'HAND_COMPLETE'
  | 'LEVEL_CHANGED'
  | 'PLAYER_ELIMINATED'
  | 'REBUY_OFFERED'
  | 'ADDON_OFFERED'
  | 'GAME_COMPLETE'
  | 'PLAYER_JOINED'
  | 'PLAYER_LEFT'
  | 'PLAYER_DISCONNECTED'
  | 'POT_AWARDED'
  | 'SHOWDOWN_STARTED'
  | 'PLAYER_REBUY'
  | 'PLAYER_ADDON'
  | 'GAME_PAUSED'
  | 'GAME_RESUMED'
  | 'PLAYER_KICKED'
  | 'CHAT_MESSAGE'
  | 'TIMER_UPDATE'
  | 'ERROR'
  | 'LOBBY_STATE'
  | 'LOBBY_PLAYER_JOINED'
  | 'LOBBY_PLAYER_LEFT'
  | 'LOBBY_SETTINGS_CHANGED'
  | 'LOBBY_GAME_STARTING'
  | 'LOBBY_PLAYER_KICKED'
  | 'GAME_CANCELLED'

// ============================================================================
// Client message types
// ============================================================================

export type ClientMessageType =
  | 'PLAYER_ACTION'
  | 'REBUY_DECISION'
  | 'ADDON_DECISION'
  | 'CHAT'
  | 'SIT_OUT'
  | 'COME_BACK'
  | 'ADMIN_KICK'
  | 'ADMIN_PAUSE'
  | 'ADMIN_RESUME'

// ============================================================================
// Server message wrapper
// ============================================================================

export interface ServerMessage<T = unknown> {
  type: ServerMessageType
  gameId: string
  sequenceNumber: number
  data: T
}

// ============================================================================
// Client message wrapper
// ============================================================================

export interface ClientMessage<T = unknown> {
  type: ClientMessageType
  sequenceNumber: number
  data?: T
}

// ============================================================================
// Shared sub-types
// ============================================================================

export interface BlindsData {
  small: number
  big: number
  ante: number
}

export interface BlindsSummary {
  small: number
  big: number
  ante: number
  levels?: BlindLevelConfig[]
}

export interface BlindLevelConfig {
  level: number
  small: number
  big: number
  ante: number
  durationMinutes: number
}

export interface SeatData {
  seatIndex: number
  playerId: number
  playerName: string
  chipCount: number
  /** "ACTIVE" | "FOLDED" | "ALL_IN" */
  status: string
  isDealer: boolean
  isSmallBlind: boolean
  isBigBlind: boolean
  currentBet: number
  /** Card codes like "Ah", "Kd"; empty for face-down */
  holeCards: string[]
  isCurrentActor: boolean
}

export interface PotData {
  amount: number
  eligiblePlayers: number[]
}

export interface TableData {
  tableId: number
  seats: SeatData[]
  communityCards: string[]
  pots: PotData[]
  currentRound: string
  handNumber: number
}

export interface PlayerSummaryData {
  playerId: number
  name: string
  chipCount: number
  tableId: number
  seatIndex: number
  finishPosition: number | null
}

export interface ActionOptionsData {
  canFold: boolean
  canCheck: boolean
  canCall: boolean
  callAmount: number
  canBet: boolean
  minBet: number
  maxBet: number
  canRaise: boolean
  minRaise: number
  maxRaise: number
  canAllIn: boolean
  allInAmount: number
}

export interface WinnerData {
  playerId: number
  amount: number
  hand: string
  cards: string[]
  potIndex: number
}

export interface ShowdownPlayerData {
  playerId: number
  cards: string[]
  handDescription: string
}

export interface StandingData {
  position: number
  playerId: number
  playerName: string
  prize: number
}

export interface BlindPostedData {
  playerId: number
  amount: number
  type: string
}

export interface LobbyPlayerData {
  profileId: number
  name: string
  isOwner: boolean
  isAI: boolean
  aiSkillLevel: string | null
}

// ============================================================================
// Server message data payloads (matching ServerMessageData.java records)
// ============================================================================

export interface ConnectedData {
  playerId: number
  gameState: GameStateData | null
  reconnectToken: string | null
}

export interface GameStateData {
  status: string
  level: number
  blinds: BlindsData
  nextLevelIn: number | null
  tables: TableData[]
  players: PlayerSummaryData[]
}

export interface HandStartedData {
  handNumber: number
  dealerSeat: number
  smallBlindSeat: number
  bigBlindSeat: number
  blindsPosted: BlindPostedData[]
}

export interface HoleCardsDealtData {
  cards: string[]
}

export interface CommunityCardsDealtData {
  round: string
  cards: string[]
  allCommunityCards: string[]
}

export interface ActionRequiredData {
  timeoutSeconds: number
  options: ActionOptionsData
}

export interface PlayerActedData {
  playerId: number
  playerName: string
  action: string
  amount: number
  totalBet: number
  chipCount: number
  potTotal: number
}

export interface ActionTimeoutData {
  playerId: number
  autoAction: string
}

export interface HandCompleteData {
  handNumber: number
  winners: WinnerData[]
  showdownPlayers: ShowdownPlayerData[]
}

export interface LevelChangedData {
  level: number
  smallBlind: number
  bigBlind: number
  ante: number
  nextLevelIn: number | null
}

export interface PlayerEliminatedData {
  playerId: number
  playerName: string
  finishPosition: number
  handsPlayed: number
}

export interface RebuyOfferedData {
  cost: number
  chips: number
  timeoutSeconds: number
}

export interface AddonOfferedData {
  cost: number
  chips: number
  timeoutSeconds: number
}

export interface GameCompleteData {
  standings: StandingData[]
  totalHands: number
  duration: number
}

export interface PlayerJoinedData {
  playerId: number
  playerName: string
  seatIndex: number
}

export interface PlayerLeftData {
  playerId: number
  playerName: string
}

export interface PlayerDisconnectedData {
  playerId: number
  playerName: string
}

export interface PotAwardedData {
  winnerIds: number[]
  amount: number
  potIndex: number
}

export interface ShowdownStartedData {
  tableId: number
  showdownPlayers: ShowdownPlayerData[]
}

export interface PlayerRebuyData {
  playerId: number
  playerName: string
  addedChips: number
}

export interface PlayerAddonData {
  playerId: number
  playerName: string
  addedChips: number
}

export interface GamePausedData {
  reason: string
  pausedBy: string
}

export interface GameResumedData {
  resumedBy: string
}

export interface PlayerKickedData {
  playerId: number
  playerName: string
  reason: string
}

export interface ChatMessageData {
  playerId: number
  playerName: string
  message: string
  tableChat: boolean
}

export interface TimerUpdateData {
  playerId: number
  secondsRemaining: number
}

export interface ErrorData {
  code: string
  message: string
}

export interface LobbyStateData {
  gameId: string
  name: string
  hostingType: string
  ownerName: string
  ownerProfileId: number
  maxPlayers: number
  isPrivate: boolean
  players: LobbyPlayerData[]
  blinds: BlindsSummary
}

export interface LobbyPlayerJoinedData {
  player: LobbyPlayerData
}

export interface LobbyPlayerLeftData {
  player: LobbyPlayerData
}

export interface LobbyPlayerKickedData {
  player: LobbyPlayerData
}

export interface LobbySettingsChangedData {
  updatedSettings: GameSummaryDto
}

export interface LobbyGameStartingData {
  startingInSeconds: number
}

export interface GameCancelledData {
  reason: string
}

// ============================================================================
// Client message data payloads (matching ClientMessageData.java records)
// ============================================================================

export interface PlayerActionData {
  action: 'FOLD' | 'CHECK' | 'CALL' | 'BET' | 'RAISE' | 'ALL_IN'
  amount: number
}

export interface RebuyDecisionData {
  accept: boolean
}

export interface AddonDecisionData {
  accept: boolean
}

export interface ChatData {
  message: string
  tableChat: boolean
}

export interface AdminKickData {
  playerId: number
}

// ============================================================================
// REST DTO types (matching server DTOs)
// ============================================================================

export interface GameSummaryDto {
  gameId: string
  name: string
  status: 'WAITING_FOR_PLAYERS' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'CANCELLED'
  hostingType: 'SERVER' | 'COMMUNITY'
  ownerName: string
  ownerProfileId: number
  playerCount: number
  maxPlayers: number
  isPrivate: boolean
  buyIn: number
  startingChips: number
  blinds: BlindsSummary
  wsUrl: string | null
}

export interface GameConfigDto {
  name: string
  maxPlayers: number
  buyIn: number
  startingChips: number
  blindStructure: BlindLevelConfig[]
  fillWithAI: boolean
  aiSkillLevel?: number
  aiCount?: number
  password?: string
  allowRebuys: boolean
  rebuyLimit: number
  rebuyCost: number
  rebuyChips: number
  allowAddon: boolean
  addonCost: number
  addonChips: number
  actionTimeoutSeconds: number
}

export interface GameJoinResponseDto {
  wsUrl: string
  gameId: string
}

export interface PracticeGameResponseDto {
  gameId: string
}

export interface WsTokenResponseDto {
  token: string
}

// ============================================================================
// WebSocket connection state
// ============================================================================

export type ConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING'
