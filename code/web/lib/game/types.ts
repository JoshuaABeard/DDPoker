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
  | 'CHIPS_TRANSFERRED'
  | 'COLOR_UP_STARTED'
  | 'AI_HOLE_CARDS'
  | 'NEVER_BROKE_OFFERED'
  | 'CONTINUE_RUNOUT'
  | 'PLAYER_SAT_OUT'
  | 'PLAYER_CAME_BACK'
  | 'OBSERVER_JOINED'
  | 'OBSERVER_LEFT'
  | 'COLOR_UP_COMPLETED'
  | 'BUTTON_MOVED'
  | 'CURRENT_PLAYER_CHANGED'
  | 'TABLE_STATE_CHANGED'
  | 'CLEANING_DONE'
  | 'PLAYER_MOVED'

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
  | 'NEVER_BROKE_DECISION'
  | 'CONTINUE_RUNOUT'
  | 'REQUEST_STATE'

// ============================================================================
// Server message wrapper
// ============================================================================

export interface ServerMessage<T = unknown> {
  type: ServerMessageType
  gameId: string
  sequenceNumber: number
  /** ISO-8601 from Java Instant */
  timestamp: string
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

/** WebSocket protocol blinds (small/big). Do NOT change field names. */
export interface BlindsData {
  small: number
  big: number
  ante: number
}

/** REST DTO blinds (GameSummary.BlindsSummary). Uses smallBlind/bigBlind. */
export interface BlindsSummary {
  smallBlind: number
  bigBlind: number
  ante: number
}

/** Matches server's GameConfig.BlindLevel */
export interface BlindLevelConfig {
  smallBlind: number
  bigBlind: number
  ante: number
  minutes: number
  isBreak: boolean
  gameType: string
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
  chipCount: number | null
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
  totalPlayers: number
  playersRemaining: number
  numTables: number
  playerRank: number
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
  tableId: number
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
  tableId: number
}

export interface ActionTimeoutData {
  playerId: number
  autoAction: string
  tableId: number
}

export interface HandCompleteData {
  handNumber: number
  winners: WinnerData[]
  showdownPlayers: ShowdownPlayerData[]
  tableId: number
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
  tableId: number
  isHuman: boolean
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
  tableId: number
  isReconnect: boolean
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
  tableId: number
}

export interface ShowdownStartedData {
  tableId: number
  showdownPlayers: ShowdownPlayerData[]
}

export interface PlayerRebuyData {
  playerId: number
  playerName: string
  addedChips: number
  chipCount: number | null
}

export interface PlayerAddonData {
  playerId: number
  playerName: string
  addedChips: number
  chipCount: number | null
}

export interface GamePausedData {
  reason: string
  pausedBy: string
  isBreak: boolean
  breakDurationMinutes: number | null
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
  maxPlayers: number
  isPrivate: boolean
  players: LobbyPlayerData[]
  blinds: BlindsData
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
// New server message data payloads (for new message types)
// ============================================================================

export interface ChipsTransferredData {
  fromPlayerId: number
  fromPlayerName: string
  toPlayerId: number
  toPlayerName: string
  amount: number
  fromChipCount: number | null
  toChipCount: number | null
}

export interface ColorUpPlayerData {
  playerId: number
  cards: string[]
  won: boolean
  broke: boolean
  finalChips: number
}

export interface ColorUpStartedData {
  players: ColorUpPlayerData[]
  newMinChip: number
  tableId: number
}

export interface AiPlayerCards {
  playerId: number
  cards: string[]
}

export interface AiHoleCardsData {
  players: AiPlayerCards[]
}

export interface NeverBrokeOfferedData {
  timeoutSeconds: number
}

export type ContinueRunoutData = Record<string, never>

export interface PlayerSatOutData {
  playerId: number
  playerName: string
}

export interface PlayerCameBackData {
  playerId: number
  playerName: string
}

export interface ObserverJoinedData {
  observerId: number
  observerName: string
  tableId: number
}

export interface ObserverLeftData {
  observerId: number
  observerName: string
  tableId: number
}

export interface ColorUpCompletedData {
  tableId: number
}

export interface ButtonMovedData {
  tableId: number
  newSeat: number
}

export interface CurrentPlayerChangedData {
  tableId: number
  playerId: number
  playerName: string
}

export interface TableStateChangedData {
  tableId: number
  oldState: string
  newState: string
}

export interface CleaningDoneData {
  tableId: number
}

export interface PlayerMovedData {
  playerId: number
  playerName: string
  fromTableId: number
  toTableId: number
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

export interface NeverBrokeDecisionData {
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
  hostingType: 'SERVER' | 'COMMUNITY'
  status: 'WAITING_FOR_PLAYERS' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'CANCELLED'
  ownerName: string
  playerCount: number
  maxPlayers: number
  isPrivate: boolean
  wsUrl: string | null
  blinds: BlindsSummary
  createdAt: string | null
  startedAt: string | null
  players: LobbyPlayerData[]
}

export interface RebuyConfigDto {
  enabled: boolean
  cost: number
  chips: number
  chipCount?: number
  maxRebuys: number
  lastLevel?: number
  expressionType?: string
}

export interface AddonConfigDto {
  enabled: boolean
  cost: number
  chips: number
  level?: number
}

export interface PayoutConfigDto {
  type: string
  spots: number
  percent: number
  prizePool: number
  allocationType: string
  spotAllocations?: number[]
}

export interface HouseConfigDto {
  cutType: string
  percent: number
  amount: number
}

export interface BountyConfigDto {
  enabled: boolean
  amount: number
}

export interface TimeoutConfigDto {
  defaultSeconds: number
  preflopSeconds?: number
  flopSeconds?: number
  turnSeconds?: number
  riverSeconds?: number
  thinkBankSeconds?: number
}

export interface BootConfigDto {
  bootSitout: boolean
  bootSitoutCount: number
  bootDisconnect: boolean
  bootDisconnectCount: number
}

export interface LateRegistrationConfigDto {
  enabled: boolean
  untilLevel: number
  chipMode: string
}

export interface ScheduledStartConfigDto {
  enabled: boolean
  startTime: string
  minPlayers: number
}

export interface InviteConfigDto {
  inviteOnly: boolean
  invitees: string[]
  observersPublic: boolean
}

export interface BettingConfigDto {
  maxRaises: number
  raiseCapIgnoredHeadsUp: boolean
}

export interface AIPlayerConfigDto {
  name: string
  skillLevel: number
}

export interface PracticeConfigDto {
  aiActionDelayMs?: number
  handResultPauseMs?: number
  allInRunoutPauseMs?: number
  zipModeEnabled?: boolean
  aiFaceUp?: boolean
  pauseAllinInteractive?: boolean
  autoDeal?: boolean
}

export interface GameConfigDto {
  name: string
  description?: string
  greeting?: string
  maxPlayers: number
  maxOnlinePlayers?: number
  fillComputer: boolean
  buyIn: number
  startingChips: number
  blindStructure: BlindLevelConfig[]
  doubleAfterLastLevel?: boolean
  /** "NO_LIMIT" | "POT_LIMIT" | "LIMIT" */
  defaultGameType: string
  levelAdvanceMode?: 'TIME' | 'HANDS'
  handsPerLevel?: number
  defaultMinutesPerLevel?: number
  rebuys: RebuyConfigDto
  addons: AddonConfigDto
  payout?: PayoutConfigDto
  house?: HouseConfigDto
  bounty?: BountyConfigDto
  timeouts: TimeoutConfigDto
  boot?: BootConfigDto
  lateRegistration?: LateRegistrationConfigDto
  scheduledStart?: ScheduledStartConfigDto
  invite?: InviteConfigDto
  betting?: BettingConfigDto
  allowDash?: boolean
  allowAdvisor?: boolean
  aiPlayers: AIPlayerConfigDto[]
  humanDisplayName?: string
  practiceConfig?: PracticeConfigDto
  password?: string
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
