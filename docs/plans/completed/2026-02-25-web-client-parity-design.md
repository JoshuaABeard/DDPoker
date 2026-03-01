# Web Client Protocol & Core UI Parity

**Status:** COMPLETED (2026-03-01)
**Scope:** Fix all protocol mismatches, handle all server message types, fix bugs, add missing core gameplay features.
**Approach:** Bottom-up — types → reducer → WebSocket → components → routing → features.

## Background

A code review of the web client (`code/web/`) against the Java desktop client and server (`pokergameserver`) revealed:
- 4 critical issues (runtime failures: BlindsSummary field names, DTO mismatches, no auth guard)
- 9 high-severity issues (functionally broken: 15 unhandled message types, chat duplication, stale reconnect token, etc.)
- 11 medium-severity issues (incorrect behavior in specific scenarios)
- 8 low-severity issues (cosmetic, UX, maintainability)
- Significant missing features vs desktop client

## Design Decisions

- **Web client adapts to server** — Server DTOs are the source of truth. TypeScript types change to match Java records. No server-side changes except where genuinely broken.
- **Bottom-up fix order** — Protocol types first, then reducer, then WebSocket client, then components, then routing, then new features. Each phase independently testable.
- **Shared GameProvider** — Single `[gameId]/layout.tsx` wraps lobby and play pages to eliminate WebSocket reconnection gap.
- **Deferred to future plans:** Analysis tools (hand equity simulator, statistics), deck/table customization, dashboard editor, game save/resume.

## Phase 1: Protocol Alignment (types.ts)

### Server message types — add 15 missing
`CHIPS_TRANSFERRED`, `COLOR_UP_STARTED`, `COLOR_UP_COMPLETED`, `AI_HOLE_CARDS`, `NEVER_BROKE_OFFERED`, `CONTINUE_RUNOUT`, `PLAYER_SAT_OUT`, `PLAYER_CAME_BACK`, `OBSERVER_JOINED`, `OBSERVER_LEFT`, `BUTTON_MOVED`, `CURRENT_PLAYER_CHANGED`, `TABLE_STATE_CHANGED`, `CLEANING_DONE`, `PLAYER_MOVED`

### Client message types — add 3 missing
`NEVER_BROKE_DECISION`, `CONTINUE_RUNOUT`, `REQUEST_STATE`

### Data interface fixes

| Interface | Change |
|-----------|--------|
| `BlindsSummary` | Rename `small`/`big` → `smallBlind`/`bigBlind` |
| `GameStateData` | Add `totalPlayers`, `playersRemaining`, `numTables`, `playerRank` |
| `GameSummaryDto` | Remove `ownerProfileId`, `buyIn`, `startingChips`; add `createdAt`, `startedAt`, `players` |
| `ServerMessage` | Add `timestamp` field |
| `PlayerActedData` | Add `tableId` |
| `CommunityCardsDealtData` | Add `tableId` |
| `HandCompleteData` | Add `tableId` |
| `ActionTimeoutData` | Add `tableId` |
| `PotAwardedData` | Add `tableId` |
| `PlayerJoinedData` | Add `tableId`, `isReconnect` |
| `PlayerEliminatedData` | Add `tableId`, `isHuman` |
| `WinnerData` | Add `chipCount` |
| `PlayerRebuyData` | Add `chipCount` |
| `PlayerAddonData` | Add `chipCount` |
| `GamePausedData` | Add `isBreak`, `breakDurationMinutes` |

### New data interfaces
Create TypeScript interfaces for all 15 new message types (e.g., `ChipsTransferredData`, `ColorUpStartedData`, `PlayerSatOutData`, `PlayerMovedData`, etc.).

### GameConfigDto restructure
Match server's nested `GameConfig`: `RebuyConfig`, `AddonConfig`, `TimeoutConfig` sub-objects, `aiPlayers` list instead of flat `aiCount`, game type field, payout config, bounty config, boot config, late registration, invite config, betting config.

## Phase 2: State Management (gameReducer.ts)

### New case handlers for 15 missing message types
- `CHIPS_TRANSFERRED` — Update chip counts for source and target players
- `COLOR_UP_STARTED` / `COLOR_UP_COMPLETED` — Set `colorUpInProgress` flag; on completed, update chip counts
- `AI_HOLE_CARDS` — Store AI hole cards in seat for face-up rendering in practice mode
- `NEVER_BROKE_OFFERED` — Set overlay prompt
- `CONTINUE_RUNOUT` — Set "continue" prompt for all-in runouts
- `PLAYER_SAT_OUT` / `PLAYER_CAME_BACK` — Update seat status
- `OBSERVER_JOINED` / `OBSERVER_LEFT` — Maintain observers list
- `BUTTON_MOVED` — Update dealer seat index
- `CURRENT_PLAYER_CHANGED` — Update `isCurrentActor` flags
- `TABLE_STATE_CHANGED` — Update table state field
- `CLEANING_DONE` — Clear per-hand visual state
- `PLAYER_MOVED` — Handle multi-table seat reassignment

### Bug fixes in existing handlers
- `handleHandStarted` — Reset `ALL_IN` seats to `ACTIVE`
- `handlePlayerKicked` — Remove kicked player's seat
- `handlePlayerChipChange` — Use absolute `chipCount` when available
- `handleHandComplete` — Update winner chip counts from `WinnerData.chipCount`
- `handleChatMessage` — Deduplicate against optimistic entries
- `handlePotAwarded` — Update winner chip counts

### New state fields
- `handHistory: HandHistoryEntry[]` — Populated from game events
- `observers: { playerId: number; playerName: string }[]`
- `colorUpInProgress: boolean`
- `totalPlayers`, `playersRemaining`, `numTables`, `playerRank`
- `neverBrokeOffer` / `continueRunout` — Overlay prompt state

### Sequence number tracking
Track last received sequence number. On gap detection, send `REQUEST_STATE`.

## Phase 3: WebSocket & API Hardening

### WebSocket Client
- Accept `tokenRefreshFn` callback for reconnect token refresh
- Guard against double-connect (close old socket first)
- Reset sequence number per connection
- Clean disconnect (remove handlers before close)

### API wrapper
- Content-Type only when body exists
- Compose AbortSignals with `AbortSignal.any()`
- `checkApiHealth` uses `apiFetch`
- `getCurrentUser` distinguishes 401 from 500

### GameContext
- Optimistic `actionRequired` clearing on `sendAction`
- New methods: `sendSitOut()`, `sendComeBack()`, `sendNeverBrokeDecision()`, `sendContinueRunout()`, `sendRequestState()`
- Inject `getWsToken` into WebSocketClient for reconnect

## Phase 4: Component Fixes

### PlayerSeat
- Fix `visualPosition()` — use array index consistently
- Active-hand check before showing face-down cards
- `SAT_OUT` and `DISCONNECTED` visual indicators

### ActionPanel
- Fix stale closure (ref for `onAction`)
- Disabled state after action sent

### BetSlider
- Numeric text input for precise amounts
- Smart step size (big blind based)
- Hide misleading presets

### HandHistory
- Wire to `handHistory` state field
- Render entries as they accumulate

### GameOverlay
- `neverBroke` and `continueRunout` variants
- Countdown timer for timed decisions
- ARIA attributes and focus trapping

### ActionTimer
- 500ms buffer (matching JSDoc)
- Sync from TIMER_UPDATE

### CommunityCards
- Hide empty slots before flop

### Code quality
- Extract `formatChips()` to `lib/utils.ts` (used in 7 files)
- Import `PotData` from `types.ts` in PotDisplay

## Phase 5: Routing & Auth

### Auth middleware
`code/web/middleware.ts` — redirect unauthenticated users to `/login?returnUrl=...` for `/games/*` routes.

### Shared GameProvider layout
`app/games/[gameId]/layout.tsx` — single GameProvider wrapping lobby and play pages.

### Page fixes
- Register page: check `success` flag before redirect
- Results page: REST API fallback for refresh resilience
- Watch link: error handling for observer/spectator scenario

## Phase 6: Missing Core Features

### Create game settings expansion
- Game type selector (No Limit / Pot Limit / Limit)
- Custom blind structure editor (add/remove/edit levels, breaks)
- Level advance mode (time vs hands)
- Payout structure (spots, percentages)
- Bounty toggle + amount
- Per-street timeouts
- Boot settings
- Late registration toggle
- Invite/observer settings

### Sit-out / Come-back
- Button in player action area
- `SIT_OUT` / `COME_BACK` client messages
- Visual indicator on seat

### Observer mode
- Track observers in state
- Display observer count
- Observer chat channel

### Hand history tracking
- Build entries from game events in reducer
- Scrollable, filterable display

## Out of Scope (Future Plans)
- Hand equity simulator / analysis tools
- Deck and table design customization
- Dashboard editor
- Game save/resume
- Export features (chat, hand history, screenshots)
- Poker Clock / Home Game mode
- Community game WebSocket proxy (see `2026-02-18-community-game-ws-proxy.md`)
