# Web Client Protocol & Core UI Parity — Implementation Plan

**Status:** COMPLETED (2026-03-01)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix all protocol mismatches between the web client and Java server, handle all 47 server message types, fix all reducer/WebSocket/component bugs, restructure routing, and add missing core gameplay features.

**Architecture:** Bottom-up approach working from TypeScript protocol types → state reducer → WebSocket client → React components → Next.js routing → new features. Each phase builds on the previous, and every phase is independently testable.

**Tech Stack:** TypeScript, React 19, Next.js 16, Vitest, Tailwind CSS 4, browser WebSocket API.

**Design doc:** `docs/plans/2026-02-25-web-client-parity-design.md`

---

## Phase 1: Protocol Alignment (types.ts)

### Task 1.1: Add Missing Server Message Types

**Files:**
- Modify: `code/web/lib/game/types.ts:24-58`

**Step 1: Add the 9 missing types to ServerMessageType union**

The server's `ServerMessageType.java` defines 42 types. The client has 33. Add these 9:

```typescript
export type ServerMessageType =
  // ... existing 33 types ...
  | 'GAME_CANCELLED'
  // Add these:
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
```

**Step 2: Run tests to verify no compile errors**

Run: `cd code/web && npx tsc --noEmit`
Expected: PASS (no type errors)

### Task 1.2: Add Missing Client Message Types

**Files:**
- Modify: `code/web/lib/game/types.ts:64-73`

**Step 1: Add 3 missing types to ClientMessageType union**

```typescript
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
  // Add these:
  | 'NEVER_BROKE_DECISION'
  | 'CONTINUE_RUNOUT'
  | 'REQUEST_STATE'
```

**Step 2: Run type check**

Run: `cd code/web && npx tsc --noEmit`
Expected: PASS

### Task 1.3: Add timestamp to ServerMessage

**Files:**
- Modify: `code/web/lib/game/types.ts:79-84`

**Step 1: Add timestamp field**

```typescript
export interface ServerMessage<T = unknown> {
  type: ServerMessageType
  gameId: string
  sequenceNumber: number
  timestamp: string  // ISO-8601 from Java Instant
  data: T
}
```

**Step 2: Run type check — expect failures in code that destructures ServerMessage**

Run: `cd code/web && npx tsc --noEmit`
Fix any downstream type errors (the timestamp field is optional in practice since existing code doesn't use it, but adding it is correct).

### Task 1.4: Fix BlindsSummary to Match Server DTO

**Files:**
- Modify: `code/web/lib/game/types.ts:106-111`

**Important context:** The server has TWO blind-related types:
- `ServerMessageData.BlindsData(int small, int big, int ante)` — used in WebSocket messages. The client's existing `BlindsData` interface is CORRECT.
- `GameSummary.BlindsSummary(int smallBlind, int bigBlind, int ante)` — used in REST DTOs. The client's `BlindsSummary` is WRONG (uses `small`/`big` instead of `smallBlind`/`bigBlind`).

**Step 1: Fix BlindsSummary**

```typescript
export interface BlindsSummary {
  smallBlind: number
  bigBlind: number
  ante: number
}
```

Remove the `levels` field — server's `BlindsSummary` doesn't have it.

**Step 2: Fix BlindLevelConfig to match server's GameConfig.BlindLevel**

```typescript
export interface BlindLevelConfig {
  smallBlind: number
  bigBlind: number
  ante: number
  minutes: number
  isBreak: boolean
  gameType: string
}
```

**Step 3: Fix all downstream references**

Search for `blinds.small`, `blinds.big`, `.small`, `.big` in components that use `BlindsSummary` (NOT `BlindsData`). Update to `blinds.smallBlind`, `blinds.bigBlind`. Key files:
- `code/web/components/game/TournamentInfoBar.tsx`
- `code/web/app/games/page.tsx`
- `code/web/app/games/create/page.tsx`
- `code/web/app/games/[gameId]/lobby/page.tsx`

**Step 4: Run type check**

Run: `cd code/web && npx tsc --noEmit`
Expected: PASS after fixing all references

### Task 1.5: Fix Data Interfaces — Add Missing Fields

**Files:**
- Modify: `code/web/lib/game/types.ts` — multiple interfaces

**Step 1: Add missing fields to existing interfaces**

Apply ALL of these changes:

```typescript
// GameStateData — add 4 tournament fields
export interface GameStateData {
  status: string
  level: number
  blinds: BlindsData
  nextLevelIn: number | null
  tables: TableData[]
  players: PlayerSummaryData[]
  totalPlayers: number       // ADD
  playersRemaining: number   // ADD
  numTables: number          // ADD
  playerRank: number         // ADD
}

// CommunityCardsDealtData — add tableId
export interface CommunityCardsDealtData {
  round: string
  cards: string[]
  allCommunityCards: string[]
  tableId: number            // ADD
}

// PlayerActedData — add tableId
export interface PlayerActedData {
  playerId: number
  playerName: string
  action: string
  amount: number
  totalBet: number
  chipCount: number
  potTotal: number
  tableId: number            // ADD
}

// ActionTimeoutData — add tableId
export interface ActionTimeoutData {
  playerId: number
  autoAction: string
  tableId: number            // ADD
}

// HandCompleteData — add tableId
export interface HandCompleteData {
  handNumber: number
  winners: WinnerData[]
  showdownPlayers: ShowdownPlayerData[]
  tableId: number            // ADD
}

// PlayerEliminatedData — add tableId, isHuman
export interface PlayerEliminatedData {
  playerId: number
  playerName: string
  finishPosition: number
  handsPlayed: number
  tableId: number            // ADD
  isHuman: boolean           // ADD
}

// PotAwardedData — add tableId
export interface PotAwardedData {
  winnerIds: number[]
  amount: number
  potIndex: number
  tableId: number            // ADD
}

// PlayerJoinedData — add tableId, isReconnect
export interface PlayerJoinedData {
  playerId: number
  playerName: string
  seatIndex: number
  tableId: number            // ADD
  isReconnect: boolean       // ADD
}

// WinnerData — add chipCount
export interface WinnerData {
  playerId: number
  amount: number
  hand: string
  cards: string[]
  potIndex: number
  chipCount: number | null   // ADD (Integer in Java = nullable)
}

// PlayerRebuyData — add chipCount
export interface PlayerRebuyData {
  playerId: number
  playerName: string
  addedChips: number
  chipCount: number | null   // ADD
}

// PlayerAddonData — add chipCount
export interface PlayerAddonData {
  playerId: number
  playerName: string
  addedChips: number
  chipCount: number | null   // ADD
}

// GamePausedData — add isBreak, breakDurationMinutes
export interface GamePausedData {
  reason: string
  pausedBy: string
  isBreak: boolean                    // ADD
  breakDurationMinutes: number | null // ADD (Integer in Java)
}
```

**Step 2: Run type check and fix any downstream errors**

Run: `cd code/web && npx tsc --noEmit`
Expected: May need to update reducer/component code that destructures these types.

### Task 1.6: Add New Data Interfaces for Missing Message Types

**Files:**
- Modify: `code/web/lib/game/types.ts` — add after existing server data interfaces

**Step 1: Add all new data interfaces**

```typescript
// ============================================================================
// New server message data payloads (added for protocol parity)
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

/** No data payload — presence of message type is the signal */
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

// New client message data payloads
export interface NeverBrokeDecisionData {
  accept: boolean
}
```

**Step 2: Run type check**

Run: `cd code/web && npx tsc --noEmit`
Expected: PASS

### Task 1.7: Fix GameSummaryDto to Match Server

**Files:**
- Modify: `code/web/lib/game/types.ts:443-457`

**Step 1: Update GameSummaryDto to match server's GameSummary record**

```typescript
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
  createdAt: string | null  // ISO-8601 from Java Instant
  startedAt: string | null  // ISO-8601 from Java Instant
  players: LobbyPlayerData[]
}
```

Removed: `ownerProfileId`, `buyIn`, `startingChips` (not in server DTO).
Added: `createdAt`, `startedAt`, `players` (in server DTO).

**Step 2: Fix all code that references removed fields**

Search for `ownerProfileId`, `buyIn`, `startingChips` in `code/web/` and remove/update. Key files:
- `code/web/app/games/page.tsx` — game list cards
- `code/web/app/games/create/page.tsx` — remove from form submission

**Step 3: Run type check**

Run: `cd code/web && npx tsc --noEmit`

### Task 1.8: Restructure GameConfigDto to Match Server's GameConfig

**Files:**
- Modify: `code/web/lib/game/types.ts:459-477`

**Step 1: Replace flat GameConfigDto with nested structure matching GameConfig.java**

```typescript
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
  defaultGameType: string  // "NO_LIMIT" | "POT_LIMIT" | "LIMIT"
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
  password?: string  // Not in GameConfig — handled at API layer
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
  startTime: string  // ISO-8601
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
```

**Step 2: Fix create game page and API calls**

The `code/web/app/games/create/page.tsx` form submission and `code/web/lib/api.ts` create/practice game calls must build the new nested structure.

**Step 3: Run type check**

Run: `cd code/web && npx tsc --noEmit`

### Task 1.9: Commit Phase 1

**Step 1: Run all tests**

Run: `cd code/web && npm test`
Expected: PASS

**Step 2: Commit**

```bash
cd code/web
git add lib/game/types.ts
git commit -m "fix(web): align TypeScript types with server protocol

Add 9 missing server message types, 3 missing client message types.
Fix BlindsSummary field names (smallBlind/bigBlind).
Add missing fields to 14 data interfaces (tableId, chipCount, etc.).
Add 17 new data interfaces for previously unhandled message types.
Restructure GameConfigDto to match server's nested GameConfig.
Fix GameSummaryDto to match server's GameSummary record."
```

---

## Phase 2: State Management (gameReducer.ts)

### Task 2.1: Add New State Fields

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — GameState interface and initialState

**Step 1: Add new fields to GameState type and initial state**

Add to the `GameState` interface:

```typescript
// Tournament stats from GAME_STATE
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
lastSequenceNumber: number
```

Add the `HandHistoryEntry` type:

```typescript
export interface HandHistoryEntry {
  id: string
  handNumber: number
  type: 'action' | 'deal' | 'community' | 'result'
  playerName?: string
  action?: string
  amount?: number
  round?: string
  cards?: string[]
  winners?: { playerName: string; amount: number; hand: string }[]
  timestamp: number
}
```

Initialize all new fields with defaults in `initialState`.

**Step 2: Run type check**

Run: `cd code/web && npx tsc --noEmit`

### Task 2.2: Fix handleHandStarted — Reset ALL_IN Status

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handleHandStarted function

**Step 1: Write a failing test**

Test file: `code/web/lib/game/__tests__/gameReducer.test.ts`

```typescript
it('resets ALL_IN seats to ACTIVE on hand start', () => {
  const state = {
    ...baseState,
    currentTable: {
      ...baseTable,
      seats: [
        { ...baseSeat, seatIndex: 0, status: 'ALL_IN' },
        { ...baseSeat, seatIndex: 1, status: 'FOLDED' },
        { ...baseSeat, seatIndex: 2, status: 'ACTIVE' },
      ],
    },
  }
  const result = gameReducer(state, {
    type: 'SERVER_MESSAGE',
    message: {
      type: 'HAND_STARTED',
      gameId: 'g1',
      sequenceNumber: 1,
      timestamp: new Date().toISOString(),
      data: { handNumber: 2, dealerSeat: 0, smallBlindSeat: 1, bigBlindSeat: 2, blindsPosted: [] },
    },
  })
  expect(result.currentTable!.seats[0].status).toBe('ACTIVE')
  expect(result.currentTable!.seats[1].status).toBe('ACTIVE')
  expect(result.currentTable!.seats[2].status).toBe('ACTIVE')
})
```

**Step 2: Run test to verify it fails**

Run: `cd code/web && npx vitest run lib/game/__tests__/gameReducer.test.ts`
Expected: FAIL — seat 0 is still 'ALL_IN'

**Step 3: Fix the status reset in handleHandStarted**

Change the seat mapping from:
```typescript
status: seat.status === 'FOLDED' ? 'ACTIVE' : seat.status,
```
To:
```typescript
status: seat.status === 'FOLDED' || seat.status === 'ALL_IN' ? 'ACTIVE' : seat.status,
```

**Step 4: Run test to verify it passes**

Run: `cd code/web && npx vitest run lib/game/__tests__/gameReducer.test.ts`
Expected: PASS

### Task 2.3: Fix handlePlayerKicked — Remove Kicked Player

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handlePlayerKicked function

**Step 1: Write a failing test**

```typescript
it('removes kicked player seat when another player is kicked', () => {
  const state = {
    ...baseState,
    myPlayerId: 1,
    currentTable: {
      ...baseTable,
      seats: [
        { ...baseSeat, seatIndex: 0, playerId: 1 },
        { ...baseSeat, seatIndex: 1, playerId: 2 },
      ],
    },
  }
  const result = gameReducer(state, {
    type: 'SERVER_MESSAGE',
    message: {
      type: 'PLAYER_KICKED',
      gameId: 'g1',
      sequenceNumber: 1,
      timestamp: new Date().toISOString(),
      data: { playerId: 2, playerName: 'Bob', reason: 'AFK' },
    },
  })
  expect(result.currentTable!.seats).toHaveLength(1)
  expect(result.currentTable!.seats[0].playerId).toBe(1)
})
```

**Step 2: Run test to verify it fails**

**Step 3: Fix handlePlayerKicked**

```typescript
function handlePlayerKicked(state: GameState, data: PlayerKickedData): GameState {
  if (data.playerId === state.myPlayerId) {
    return { ...state, error: 'You were removed from the game', gamePhase: 'lobby' }
  }
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.filter((s) => s.playerId !== data.playerId),
    },
  }
}
```

**Step 4: Run test to verify it passes**

### Task 2.4: Fix handlePlayerChipChange — Use Absolute chipCount

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handlePlayerChipChange

**Step 1: Write a failing test**

```typescript
it('uses absolute chipCount from rebuy when available', () => {
  const state = stateWithSeat({ playerId: 1, chipCount: 500 })
  const result = gameReducer(state, {
    type: 'SERVER_MESSAGE',
    message: {
      type: 'PLAYER_REBUY',
      gameId: 'g1',
      sequenceNumber: 1,
      timestamp: new Date().toISOString(),
      data: { playerId: 1, playerName: 'Alice', addedChips: 1000, chipCount: 1500 },
    },
  })
  expect(seatFor(result, 1).chipCount).toBe(1500)
})
```

**Step 2: Fix handlePlayerChipChange to prefer absolute chipCount**

```typescript
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
          ? { ...seat, chipCount: absoluteChipCount ?? seat.chipCount + addedChips }
          : seat,
      ),
    },
  }
}
```

Update callers:
```typescript
case 'PLAYER_REBUY': {
  const d = message.data as PlayerRebuyData
  return handlePlayerChipChange(state, d.playerId, d.addedChips, d.chipCount)
}
case 'PLAYER_ADDON': {
  const d = message.data as PlayerAddonData
  return handlePlayerChipChange(state, d.playerId, d.addedChips, d.chipCount)
}
```

**Step 3: Run test to verify it passes**

### Task 2.5: Fix handleHandComplete — Update Winner Chip Counts

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handleHandComplete

**Step 1: Update handleHandComplete to apply winner chipCounts**

```typescript
function handleHandComplete(state: GameState, data: HandCompleteData): GameState {
  let seats = state.currentTable?.seats ?? []
  // Update winner chip counts if provided
  for (const winner of data.winners) {
    if (winner.chipCount != null) {
      seats = seats.map((s) =>
        s.playerId === winner.playerId ? { ...s, chipCount: winner.chipCount! } : s,
      )
    }
  }
  return {
    ...state,
    currentTable: state.currentTable
      ? { ...state.currentTable, seats }
      : state.currentTable,
    actionRequired: null,
    actionTimeoutSeconds: null,
    actionTimer: null,
    holeCards: [],
  }
}
```

### Task 2.6: Fix Chat Deduplication

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handleChatMessage

**Step 1: Write a failing test**

```typescript
it('deduplicates server echo of optimistic chat message', () => {
  const state = {
    ...baseState,
    myPlayerId: 1,
    chatMessages: [{
      id: 'optimistic-123',
      playerId: 1,
      playerName: 'Me',
      message: 'Hello',
      tableChat: true,
      optimistic: true,
    }],
  }
  const result = gameReducer(state, {
    type: 'SERVER_MESSAGE',
    message: {
      type: 'CHAT_MESSAGE',
      gameId: 'g1',
      sequenceNumber: 1,
      timestamp: new Date().toISOString(),
      data: { playerId: 1, playerName: 'Me', message: 'Hello', tableChat: true },
    },
  })
  // Should replace optimistic entry, not add a duplicate
  expect(result.chatMessages).toHaveLength(1)
  expect(result.chatMessages[0].optimistic).toBeUndefined()
})
```

**Step 2: Fix handleChatMessage**

```typescript
function handleChatMessage(state: GameState, data: ChatMessageData): GameState {
  // Check if this is an echo of an optimistic message
  const optimisticIndex = state.chatMessages.findIndex(
    (m) => m.optimistic && m.playerId === data.playerId && m.message === data.message,
  )
  if (optimisticIndex >= 0) {
    // Replace the optimistic entry with the server-confirmed one
    const confirmed: ChatEntry = {
      id: `${data.playerId}-${Date.now()}-${Math.random()}`,
      playerId: data.playerId,
      playerName: data.playerName,
      message: data.message,
      tableChat: data.tableChat,
    }
    const messages = [...state.chatMessages]
    messages[optimisticIndex] = confirmed
    return { ...state, chatMessages: messages }
  }
  // New message from another player
  const entry: ChatEntry = {
    id: `${data.playerId}-${Date.now()}-${Math.random()}`,
    playerId: data.playerId,
    playerName: data.playerName,
    message: data.message,
    tableChat: data.tableChat,
  }
  return { ...state, chatMessages: appendChat(state.chatMessages, entry) }
}
```

**Step 3: Run test to verify it passes**

### Task 2.7: Add Handlers for All 15 New Message Types

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handleServerMessage switch

**Step 1: Add case handlers for each new message type**

Add to the switch statement in `handleServerMessage`:

```typescript
case 'CHIPS_TRANSFERRED': {
  const d = message.data as ChipsTransferredData
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => {
        if (s.playerId === d.fromPlayerId && d.fromChipCount != null) return { ...s, chipCount: d.fromChipCount }
        if (s.playerId === d.toPlayerId && d.toChipCount != null) return { ...s, chipCount: d.toChipCount }
        return s
      }),
    },
  }
}

case 'COLOR_UP_STARTED':
  return { ...state, colorUpInProgress: true }

case 'COLOR_UP_COMPLETED': {
  // Chip counts will be updated via the next GAME_STATE
  return { ...state, colorUpInProgress: false }
}

case 'AI_HOLE_CARDS': {
  const d = message.data as AiHoleCardsData
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => {
        const aiCards = d.players.find((p) => p.playerId === s.playerId)
        return aiCards ? { ...s, holeCards: aiCards.cards } : s
      }),
    },
  }
}

case 'NEVER_BROKE_OFFERED': {
  const d = message.data as NeverBrokeOfferedData
  return { ...state, neverBrokeOffer: { timeoutSeconds: d.timeoutSeconds } }
}

case 'CONTINUE_RUNOUT':
  return { ...state, continueRunoutPending: true }

case 'PLAYER_SAT_OUT': {
  const d = message.data as PlayerSatOutData
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) =>
        s.playerId === d.playerId ? { ...s, status: 'SAT_OUT' } : s,
      ),
    },
  }
}

case 'PLAYER_CAME_BACK': {
  const d = message.data as PlayerCameBackData
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) =>
        s.playerId === d.playerId ? { ...s, status: 'ACTIVE' } : s,
      ),
    },
  }
}

case 'OBSERVER_JOINED': {
  const d = message.data as ObserverJoinedData
  return {
    ...state,
    observers: [...state.observers, { observerId: d.observerId, observerName: d.observerName }],
  }
}

case 'OBSERVER_LEFT': {
  const d = message.data as ObserverLeftData
  return {
    ...state,
    observers: state.observers.filter((o) => o.observerId !== d.observerId),
  }
}

case 'BUTTON_MOVED': {
  const d = message.data as ButtonMovedData
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => ({
        ...s,
        isDealer: s.seatIndex === d.newSeat,
      })),
    },
  }
}

case 'CURRENT_PLAYER_CHANGED': {
  const d = message.data as CurrentPlayerChangedData
  if (!state.currentTable) return state
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.map((s) => ({
        ...s,
        isCurrentActor: s.playerId === d.playerId,
      })),
    },
  }
}

case 'TABLE_STATE_CHANGED': {
  // Informational — no UI state change needed
  return state
}

case 'CLEANING_DONE': {
  // Clear per-hand visual state
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

case 'PLAYER_MOVED': {
  const d = message.data as PlayerMovedData
  if (!state.currentTable) return state
  // If the moved player is us, we need a new table — request full state
  if (d.playerId === state.myPlayerId) {
    // The server will send a new GAME_STATE with our new table
    return state
  }
  // Remove the moved player from our table
  return {
    ...state,
    currentTable: {
      ...state.currentTable,
      seats: state.currentTable.seats.filter((s) => s.playerId !== d.playerId),
    },
  }
}
```

**Step 2: Remove the default warning log for known types**

Update the `default` case to only warn for truly unknown types.

**Step 3: Write tests for critical new handlers (NEVER_BROKE_OFFERED, CONTINUE_RUNOUT, PLAYER_SAT_OUT, AI_HOLE_CARDS)**

**Step 4: Run all tests**

Run: `cd code/web && npm test`
Expected: PASS

### Task 2.8: Add Hand History Tracking

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts`

**Step 1: Add hand history entries in relevant handlers**

In `handleHandStarted`, `handlePlayerActed`, `handleCommunityCardsDealt`, and `handleHandComplete`, append a `HandHistoryEntry` to `state.handHistory`. Cap at 200 entries.

Example for `handlePlayerActed`:
```typescript
const historyEntry: HandHistoryEntry = {
  id: `${state.currentTable?.handNumber}-${data.playerName}-${Date.now()}`,
  handNumber: state.currentTable?.handNumber ?? 0,
  type: 'action',
  playerName: data.playerName,
  action: data.action,
  amount: data.amount,
  timestamp: Date.now(),
}
const handHistory = [...state.handHistory, historyEntry].slice(-200)
```

### Task 2.9: Add Sequence Number Tracking

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts`

**Step 1: Track sequence numbers in the reducer**

In `handleServerMessage`, before the switch, check for gaps:

```typescript
if (message.sequenceNumber > 0 && state.lastSequenceNumber > 0) {
  if (message.sequenceNumber > state.lastSequenceNumber + 1) {
    console.warn(`Sequence gap: expected ${state.lastSequenceNumber + 1}, got ${message.sequenceNumber}`)
    // Signal that a REQUEST_STATE should be sent (handled in GameContext)
    return { ...state, lastSequenceNumber: message.sequenceNumber, sequenceGapDetected: true }
  }
}
return { /* ... normal handling ... */, lastSequenceNumber: message.sequenceNumber }
```

### Task 2.10: Update GAME_STATE Handler for New Fields

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handleGameState

**Step 1: Extract new tournament fields from GameStateData**

```typescript
totalPlayers: data.totalPlayers,
playersRemaining: data.playersRemaining,
numTables: data.numTables,
playerRank: data.playerRank,
```

### Task 2.11: Commit Phase 2

Run: `cd code/web && npm test`
Expected: PASS

```bash
git add lib/game/gameReducer.ts lib/game/__tests__/
git commit -m "fix(web): handle all 47 server message types in reducer

Add handlers for 15 previously unhandled message types.
Fix handleHandStarted to reset ALL_IN seats.
Fix handlePlayerKicked to remove kicked player's seat.
Fix handlePlayerChipChange to use absolute chipCount.
Fix handleHandComplete to update winner chip counts.
Fix chat deduplication for optimistic messages.
Add hand history tracking from game events.
Add sequence number gap detection."
```

---

## Phase 3: WebSocket & API Hardening

### Task 3.1: Add Token Refresh to WebSocketClient

**Files:**
- Modify: `code/web/lib/game/WebSocketClient.ts`

**Step 1: Accept tokenRefreshFn in constructor**

```typescript
export class WebSocketClient {
  private tokenRefreshFn: (() => Promise<string>) | null = null

  constructor(
    private onMessage: (message: ServerMessage) => void,
    private onStateChange: (state: ConnectionState) => void,
    tokenRefreshFn?: () => Promise<string>,
  ) {
    this.tokenRefreshFn = tokenRefreshFn ?? null
  }
```

**Step 2: Use tokenRefreshFn in reconnect logic**

Replace the reconnect logic that falls back to `this.initialToken`:

```typescript
private async attemptReconnect(): Promise<void> {
  // ... delay calculation ...
  let token = this.reconnectToken
  if (!token && this.tokenRefreshFn) {
    try {
      token = await this.tokenRefreshFn()
    } catch {
      this.setState('DISCONNECTED')
      return
    }
  }
  if (!token) {
    this.setState('DISCONNECTED')
    return
  }
  this.openSocket(token)
}
```

### Task 3.2: Guard Against Double-Connect

**Files:**
- Modify: `code/web/lib/game/WebSocketClient.ts` — connect()

**Step 1: Add cleanup in connect()**

```typescript
connect(wsUrl: string, token: string): void {
  // Clean up any existing connection
  if (this.ws) {
    this.ws.onopen = null
    this.ws.onmessage = null
    this.ws.onerror = null
    this.ws.onclose = null
    this.ws.close(1000, 'Reconnecting')
    this.ws = null
  }
  this.clearReconnectTimer()
  this.wsUrl = wsUrl
  this.initialToken = token
  this.intentionalClose = false
  this.reconnectAttempt = 0
  this.openSocket(token)
}
```

### Task 3.3: Fix Disconnect Cleanup

**Files:**
- Modify: `code/web/lib/game/WebSocketClient.ts` — disconnect()

**Step 1: Remove handlers before closing**

```typescript
disconnect(): void {
  this.intentionalClose = true
  this.clearReconnectTimer()
  if (this.ws) {
    this.ws.onopen = null
    this.ws.onmessage = null
    this.ws.onerror = null
    this.ws.onclose = null
    this.ws.close(1000, 'Client disconnect')
    this.ws = null
  }
  this.setState('DISCONNECTED')
}
```

### Task 3.4: Reset Sequence Number Per Connection

**Files:**
- Modify: `code/web/lib/game/WebSocketClient.ts` — openSocket()

**Step 1: Reset sequenceNumber at start of openSocket()**

```typescript
private openSocket(token: string): void {
  this.sequenceNumber = 0
  // ... rest of existing code
}
```

### Task 3.5: Fix API Wrapper Issues

**Files:**
- Modify: `code/web/lib/api.ts`

**Step 1: Content-Type only on bodies**

Change `apiFetch` to only set Content-Type when body exists:

```typescript
const defaultHeaders: HeadersInit = {}
if (options.body) {
  defaultHeaders['Content-Type'] = 'application/json'
}
```

**Step 2: Fix checkApiHealth to use apiFetch**

```typescript
export async function checkApiHealth(): Promise<boolean> {
  try {
    const response = await apiFetch<{ status: string }>('/api/health')
    return !!response.data
  } catch {
    return false
  }
}
```

**Step 3: Compose AbortSignals**

```typescript
const signals = [controller.signal]
if (options.signal) signals.push(options.signal)
const composedSignal = signals.length > 1 ? AbortSignal.any(signals) : controller.signal
```

### Task 3.6: Add Missing GameContext Actions

**Files:**
- Modify: `code/web/lib/game/GameContext.tsx`

**Step 1: Add new action methods to GameActions interface and implementation**

```typescript
sendSitOut() {
  wsClientRef.current?.send('SIT_OUT', {})
},
sendComeBack() {
  wsClientRef.current?.send('COME_BACK', {})
},
sendNeverBrokeDecision(accept: boolean) {
  wsClientRef.current?.send('NEVER_BROKE_DECISION', { accept })
  dispatch({ type: 'CLEAR_NEVER_BROKE' })
},
sendContinueRunout() {
  wsClientRef.current?.send('CONTINUE_RUNOUT', {})
  dispatch({ type: 'CLEAR_CONTINUE_RUNOUT' })
},
sendRequestState() {
  wsClientRef.current?.send('REQUEST_STATE', {})
},
```

**Step 2: Add optimistic clearing for sendAction**

```typescript
sendAction(data: PlayerActionData) {
  wsClientRef.current?.send('PLAYER_ACTION', data)
  dispatch({ type: 'ACTION_SENT' })
},
```

Add `ACTION_SENT` case to reducer that clears `actionRequired`.

**Step 3: Inject token refresh into WebSocketClient**

```typescript
const wsClient = new WebSocketClient(
  handleMessage,
  handleStateChange,
  async () => {
    const resp = await gameServerApi.getWsToken()
    return resp.token
  },
)
```

**Step 4: Handle sequence gap detection**

After dispatch, check if `sequenceGapDetected` is true and send REQUEST_STATE.

### Task 3.7: Commit Phase 3

Run: `cd code/web && npm test`

```bash
git add lib/game/WebSocketClient.ts lib/api.ts lib/game/GameContext.tsx lib/game/gameReducer.ts
git commit -m "fix(web): harden WebSocket reconnect, API wrapper, and game actions

Add token refresh callback for reconnect after expired initial token.
Guard against double-connect and clean disconnect race.
Reset sequence numbers per connection.
Fix Content-Type header on GET requests.
Add sendSitOut, sendComeBack, sendNeverBrokeDecision, sendContinueRunout.
Add optimistic action clearing to prevent double-clicks.
Handle sequence gap detection with REQUEST_STATE."
```

---

## Phase 4: Component Fixes

### Task 4.1: Extract Shared formatChips Utility

**Files:**
- Create: `code/web/lib/utils.ts`
- Modify: `code/web/components/game/PlayerSeat.tsx`, `ActionPanel.tsx`, `BetSlider.tsx`, `PotDisplay.tsx`, `GameOverlay.tsx`, `TournamentInfoBar.tsx`, `GameInfoPanel.tsx`

**Step 1: Create utils.ts**

```typescript
export function formatChips(n: number): string {
  return new Intl.NumberFormat('en-US').format(n)
}
```

**Step 2: Replace all local formatChips with import**

In each of the 7 files, remove the local `function formatChips` and add:
```typescript
import { formatChips } from '@/lib/utils'
```

**Step 3: Fix PotDisplay to import PotData from types.ts**

Remove local `PotData` interface, add:
```typescript
import type { PotData } from '@/lib/game/types'
```

### Task 4.2: Fix PlayerSeat visualPosition

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`

**Step 1: Fix visualPosition to use consistent indexing**

The fix: use the **array index** for rotation, not `seat.seatIndex`:

```typescript
{currentTable.seats.map((seat, arrayIndex) => (
  <PlayerSeat
    key={seat.playerId}
    {...seatProps}
    positionStyle={SEAT_POSITIONS[visualPosition(arrayIndex)]}
  />
))}
```

And update `visualPosition` to work with array indices:

```typescript
function visualPosition(arrayIndex: number): number {
  const pos = myIndex < 0
    ? arrayIndex % SEAT_POSITIONS.length
    : ((arrayIndex - myIndex + seatCount) % seatCount) % SEAT_POSITIONS.length
  return pos >= 0 && pos < SEAT_POSITIONS.length ? pos : 0
}
```

### Task 4.3: Fix PlayerSeat — SAT_OUT and DISCONNECTED Indicators

**Files:**
- Modify: `code/web/components/game/PlayerSeat.tsx`

**Step 1: Add visual indicators for SAT_OUT and DISCONNECTED status**

```tsx
{status === 'SAT_OUT' && (
  <span className="text-xs text-yellow-400 italic">Sitting Out</span>
)}
{status === 'DISCONNECTED' && (
  <span className="text-xs text-red-400 italic">Disconnected</span>
)}
```

**Step 2: Only show face-down cards during active hand**

Add a prop or check: only render face-down cards if `handNumber > 0` and the seat status is not `ELIMINATED` or `SAT_OUT`.

### Task 4.4: Fix ActionPanel — Stale Closure and Disabled State

**Files:**
- Modify: `code/web/components/game/ActionPanel.tsx`

**Step 1: Fix the stale closure with a ref**

```typescript
const onActionRef = useRef(onAction)
onActionRef.current = onAction

useEffect(() => {
  const handler = (e: KeyboardEvent) => {
    switch (e.key.toLowerCase()) {
      case 'f':
        if (options.canFold) onActionRef.current({ action: 'FOLD', amount: 0 })
        break
      // ... etc
    }
  }
  window.addEventListener('keydown', handler)
  return () => window.removeEventListener('keydown', handler)
}, [options])
```

Remove the eslint-disable comment.

### Task 4.5: Improve BetSlider — Text Input and Smart Steps

**Files:**
- Modify: `code/web/components/game/BetSlider.tsx`

**Step 1: Add numeric text input**

Add an `<input type="number">` next to the slider:

```tsx
<input
  type="number"
  min={min}
  max={max}
  value={value}
  onChange={(e) => onChange(clamp(parseInt(e.target.value, 10) || min))}
  className="w-20 text-center bg-gray-700 text-white rounded px-2 py-1"
/>
```

**Step 2: Hide misleading presets**

Only show "1/2 Pot" if `halfPot > min`, and "Pot" if `pot > min && pot < max`.

### Task 4.6: Wire HandHistory to State

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx`
- Modify: `code/web/components/game/HandHistory.tsx`

**Step 1: Pass handHistory from state**

In PokerTable, replace `entries={[]}` with:

```tsx
<HandHistory entries={gameState.handHistory} />
```

**Step 2: Update HandHistory component to render HandHistoryEntry type**

Update the component to accept and render `HandHistoryEntry[]` with action descriptions.

### Task 4.7: Add GameOverlay Variants — NeverBroke and ContinueRunout

**Files:**
- Modify: `code/web/components/game/GameOverlay.tsx`

**Step 1: Add neverBroke overlay variant**

```tsx
interface NeverBrokeProps {
  type: 'neverBroke'
  timeoutSeconds: number
  onDecision: (accept: boolean) => void
}
```

**Step 2: Add continueRunout overlay variant**

```tsx
interface ContinueRunoutProps {
  type: 'continueRunout'
  onContinue: () => void
}
```

**Step 3: Add countdown timer to rebuy/addon/neverBroke overlays**

Use a simple countdown display based on `timeoutSeconds`:

```tsx
function CountdownTimer({ seconds }: { seconds: number }) {
  const [remaining, setRemaining] = useState(seconds)
  useEffect(() => {
    const interval = setInterval(() => setRemaining((p) => Math.max(0, p - 1)), 1000)
    return () => clearInterval(interval)
  }, [seconds])
  return <p className="text-yellow-400 text-sm mt-2">Time remaining: {remaining}s</p>
}
```

**Step 4: Add ARIA attributes**

```tsx
<div role="dialog" aria-modal="true" aria-label="Game overlay">
```

### Task 4.8: Fix CommunityCards — Hide Empty Slots Before Flop

**Files:**
- Modify: `code/web/components/game/CommunityCards.tsx`

**Step 1: Only render card slots when cards exist**

```tsx
if (cards.length === 0) return null

const slots = Array.from({ length: 5 }, (_, i) => cards[i])
```

### Task 4.9: Fix TournamentInfoBar — Use BlindsSummary Field Names

**Files:**
- Modify: `code/web/components/game/TournamentInfoBar.tsx`

**Step 1: Update blind references**

This component receives `BlindsData` (from WebSocket), which uses `small`/`big` — so it should be correct already. Verify and fix if needed. Also add tournament stats display (players remaining, rank).

### Task 4.10: Commit Phase 4

Run: `cd code/web && npm test`

```bash
git add lib/utils.ts components/game/ lib/game/gameReducer.ts
git commit -m "fix(web): fix component bugs, extract utilities, add overlays

Extract formatChips to shared utility, remove 7 duplicates.
Fix seat position calculation (array index vs seatIndex).
Add SAT_OUT and DISCONNECTED visual indicators.
Fix ActionPanel stale closure with ref.
Add text input to BetSlider.
Wire HandHistory to reducer state.
Add NeverBroke and ContinueRunout overlay variants.
Add countdown timer to timed overlays.
Hide empty community card slots before flop."
```

---

## Phase 5: Routing & Auth

### Task 5.1: Add Auth Middleware

**Files:**
- Create: `code/web/middleware.ts`

**Step 1: Create Next.js middleware**

```typescript
import { NextRequest, NextResponse } from 'next/server'

const PUBLIC_PATHS = ['/', '/login', '/register', '/forgot', '/support', '/download']

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Allow public paths and static assets
  if (PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(p + '/'))
    || pathname.startsWith('/_next')
    || pathname.startsWith('/images')
    || pathname.startsWith('/api')) {
    return NextResponse.next()
  }

  // Check for auth cookie
  const authToken = request.cookies.get('auth_token')
  if (!authToken) {
    const loginUrl = new URL('/login', request.url)
    loginUrl.searchParams.set('returnUrl', pathname)
    return NextResponse.redirect(loginUrl)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
```

### Task 5.2: Create Shared GameProvider Layout

**Files:**
- Create: `code/web/app/games/[gameId]/layout.tsx`
- Modify: `code/web/app/games/[gameId]/lobby/page.tsx`
- Modify: `code/web/app/games/[gameId]/play/page.tsx`

**Step 1: Create [gameId]/layout.tsx**

```tsx
'use client'

import { useParams } from 'next/navigation'
import { GameProvider } from '@/lib/game/GameContext'

export default function GameLayout({ children }: { children: React.ReactNode }) {
  const params = useParams()
  const gameId = params.gameId as string

  return (
    <GameProvider gameId={gameId}>
      {children}
    </GameProvider>
  )
}
```

**Step 2: Remove GameProvider wrapping from lobby and play pages**

Both pages should now just use `useGameState()` and `useGameActions()` directly, since the parent layout provides the context.

### Task 5.3: Fix Register Page — Check Success Flag

**Files:**
- Modify: `code/web/app/register/page.tsx`

**Step 1: Check response before redirect**

```typescript
const response = await gameServerApi.register(username, password, email)
if (!response.success) {
  setError(response.message || 'Registration failed')
  return
}
router.push('/games')
```

### Task 5.4: Fix Results Page — REST Fallback

**Files:**
- Modify: `code/web/app/games/[gameId]/results/page.tsx`

**Step 1: Add REST API fallback when sessionStorage is empty**

```typescript
useEffect(() => {
  const raw = sessionStorage.getItem('ddpoker_results')
  if (raw) {
    const parsed = JSON.parse(raw)
    setStandings(parsed.standings)
    setMyPlayerId(parsed.myPlayerId)
    sessionStorage.removeItem('ddpoker_results')
  } else {
    // Fallback: fetch from API
    gameServerApi.getGame(gameId).then((game) => {
      if (game?.standings) {
        setStandings(game.standings)
      }
    }).catch(() => {
      // Game data unavailable
    })
  }
}, [gameId])
```

### Task 5.5: Fix Watch Link Error Handling

**Files:**
- Modify: `code/web/app/games/page.tsx`

**Step 1: Add error handling for Watch action**

When clicking Watch on an in-progress game, attempt to join as observer via `gameServerApi.observeGame(gameId)`. If it fails, show an error toast.

### Task 5.6: Fix Import in Results Page

**Files:**
- Modify: `code/web/app/games/[gameId]/results/page.tsx`

**Step 1: Merge duplicate imports**

```typescript
import { useParams, useRouter } from 'next/navigation'
```

### Task 5.7: Commit Phase 5

Run: `cd code/web && npm test`

```bash
git add middleware.ts app/games/ lib/game/GameContext.tsx
git commit -m "feat(web): add auth middleware, shared GameProvider layout, page fixes

Add Next.js middleware for authentication guards on /games/* routes.
Create shared [gameId]/layout.tsx with single GameProvider.
Fix register page to check success flag.
Add REST fallback for results page refresh.
Add error handling for Watch link on in-progress games."
```

---

## Phase 6: Missing Core Features

### Task 6.1: Expand Create Game Form — Game Type and Blind Structure

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Add game type selector**

```tsx
<select value={gameType} onChange={(e) => setGameType(e.target.value)}>
  <option value="NO_LIMIT">No Limit</option>
  <option value="POT_LIMIT">Pot Limit</option>
  <option value="LIMIT">Limit</option>
</select>
```

**Step 2: Add blind structure editor**

Build a table where each row is a blind level with editable small/big/ante/minutes/isBreak fields. Include Add Level and Remove Level buttons. Default: the existing `DEFAULT_BLIND_STRUCTURE` but now editable.

**Step 3: Add level advance mode**

```tsx
<select value={levelAdvanceMode} onChange={(e) => setLevelAdvanceMode(e.target.value)}>
  <option value="TIME">Time-based</option>
  <option value="HANDS">Hands per level</option>
</select>
{levelAdvanceMode === 'HANDS' && (
  <input type="number" value={handsPerLevel} onChange={...} />
)}
```

### Task 6.2: Expand Create Game Form — Payout and Bounty

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Add payout configuration**

Fields: type (PERCENTAGE/FIXED), spots, percent, allocationType.

**Step 2: Add bounty toggle**

```tsx
<label>
  <input type="checkbox" checked={bountyEnabled} onChange={...} />
  Enable Bounties
</label>
{bountyEnabled && <input type="number" value={bountyAmount} onChange={...} />}
```

### Task 6.3: Expand Create Game Form — Timeouts, Boot, Late Registration

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Add per-street timeout fields**

Show advanced timeout settings (preflop, flop, turn, river, think bank) in a collapsible section.

**Step 2: Add boot settings**

Checkboxes for bootSitout and bootDisconnect with count inputs.

**Step 3: Add late registration toggle**

Enable/disable with "until level" input.

### Task 6.4: Expand Create Game Form — Invite and AI Settings

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Add invite settings**

Toggle for invite-only, text area for invitees, observers-public toggle.

**Step 2: Add AI player configuration**

Instead of just `aiCount`, show a list of AI players with name and skill level (1-10) inputs. Add/remove buttons.

### Task 6.5: Fix Practice Game to Use Form Settings

**Files:**
- Modify: `code/web/app/games/create/page.tsx`

**Step 1: Update handlePractice to use form state**

Replace hardcoded values with form state variables for maxPlayers, AI settings, rebuy/addon settings, etc.

### Task 6.6: Update Form Submission to Build Nested GameConfigDto

**Files:**
- Modify: `code/web/app/games/create/page.tsx`
- Modify: `code/web/lib/api.ts`

**Step 1: Build nested DTO from form state**

```typescript
const config: GameConfigDto = {
  name,
  maxPlayers,
  fillComputer: fillWithAI,
  buyIn,
  startingChips,
  blindStructure,
  defaultGameType: gameType,
  levelAdvanceMode,
  rebuys: { enabled: allowRebuys, cost: rebuyCost, chips: rebuyChips, maxRebuys: rebuyLimit },
  addons: { enabled: allowAddon, cost: addonCost, chips: addonChips },
  timeouts: { defaultSeconds: actionTimeoutSeconds },
  aiPlayers: aiPlayerList,
  // ... other settings
}
```

**Step 2: Update API methods to send new DTO structure**

### Task 6.7: Add Sit-Out / Come-Back UI

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx` or create `code/web/components/game/SitOutButton.tsx`

**Step 1: Add sit-out toggle button**

Show a "Sit Out" / "I'm Back" button near the action panel. When clicked, call `sendSitOut()` or `sendComeBack()` from game actions.

### Task 6.8: Show Observer Count

**Files:**
- Modify: `code/web/components/game/PokerTable.tsx` or `TournamentInfoBar.tsx`

**Step 1: Display observer count**

```tsx
{observers.length > 0 && (
  <span className="text-gray-400 text-sm">{observers.length} watching</span>
)}
```

### Task 6.9: Show Tournament Stats (Players Remaining, Rank)

**Files:**
- Modify: `code/web/components/game/TournamentInfoBar.tsx`

**Step 1: Add tournament stats display**

```tsx
<span>Players: {playersRemaining}/{totalPlayers}</span>
{playerRank > 0 && <span>Rank: #{playerRank}</span>}
```

### Task 6.10: Commit Phase 6

Run: `cd code/web && npm test`

```bash
git add app/games/create/ components/game/ lib/api.ts lib/game/
git commit -m "feat(web): add create-game settings, sit-out, observer count

Expand create game form: game type, blind structure editor,
payout config, bounty, per-street timeouts, boot settings,
late registration, invite settings, AI player configuration.
Fix practice game to use form settings.
Build nested GameConfigDto for server compatibility.
Add sit-out/come-back button.
Show observer count and tournament stats."
```

---

## Final Verification

### Task 7.1: Full Test Suite

Run: `cd code/web && npm test`
Expected: All tests pass

### Task 7.2: Type Check

Run: `cd code/web && npx tsc --noEmit`
Expected: No type errors

### Task 7.3: Build

Run: `cd code/web && npm run build`
Expected: Build succeeds

### Task 7.4: Manual Smoke Test Checklist

Before considering this complete, verify:
- [ ] Auth middleware redirects unauthenticated users to /login
- [ ] Game listing page loads and displays games
- [ ] Create game form shows all settings
- [ ] Practice game starts with correct settings
- [ ] WebSocket connects and receives GAME_STATE
- [ ] Cards render correctly (community and hole cards)
- [ ] Player actions (fold/check/call/raise) work
- [ ] Chat messages appear once (no duplication)
- [ ] Sit-out/come-back toggles work
- [ ] Rebuy/addon overlays show with countdown
- [ ] Hand history populates during play
- [ ] Lobby-to-play transition is seamless (no reconnect gap)
- [ ] Results page survives refresh
