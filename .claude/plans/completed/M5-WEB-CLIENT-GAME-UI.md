# M5: Web Client Game UI — Implementation Plan

**Status:** APPROVED
**Parent Plan:** `.claude/plans/SERVER-HOSTED-GAME-ENGINE.md`
**Depends On:** M1 ✅ M2 ✅ M3 ✅ M4 ✅ M6 ✅
**Effort:** XL (4-6 weeks)
**Created:** 2026-02-18

---

## Context

DD Poker has been migrated from P2P to server-authoritative architecture. The server game engine (M1), REST API (M2), WebSocket protocol (M3), desktop client adaptation (M4), and game discovery (M6) are all complete. M7 (legacy P2P removal) is nearly done and does not block this work.

The existing Next.js 16 website (`code/web/`) is a fully functional portal with auth, game listings, leaderboard, profiles, and admin. It has **no WebSocket infrastructure and no gameplay UI**. M5 adds the ability to actually play poker in the browser by consuming the already-locked REST + WebSocket protocol.

**Goal:** Build a fully functional web-based poker game client that connects to the same backend as the desktop client.

**Key Features:**
- **Create games** from the website (`/games/create`) — configure tournament settings, blind structure, AI opponents, password protection
- **Quick Practice** — one-click creation of an AI-filled practice game, straight to playing
- **Game lobby** (`/games`) — browse, filter, search, and join available games
- **Pre-game lobby** (`/games/[id]/lobby`) — real-time player list, lobby chat, owner controls (start/kick/cancel)
- **Full gameplay** (`/games/[id]/play`) — immersive poker table with cards, betting, pot display, action panel
- **In-game chat** — real-time chat during gameplay via WebSocket (`CHAT`/`CHAT_MESSAGE` messages)
- **Lobby chat** — real-time chat while waiting for game to start (same WebSocket protocol)
- **Game results** (`/games/[id]/results`) — final standings, prize distribution, game stats

**Scope additions (from discussion):**
- **User registration page** — `/register` page so new users can create accounts and play immediately
- **Replace `/online/` game listings** — `/games` replaces `/online/available`, `/online/current`, `/online/completed`. `/online/` retains leaderboard, history, profiles, hosts.
- **Full create game form** — Support all `GameConfig` options: blind structure editor, rebuys, addons, payouts, timeouts, game type. Match desktop client capabilities.

---

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| State management | **React Context + useReducer** | Matches existing `AuthContext` pattern. Only one game active at a time — no need for Redux/Zustand. |
| WebSocket library | **Browser `WebSocket` API** | Server uses plain JSON text frames. No socket.io needed. Thin wrapper for reconnect/auth. |
| Game page layout | **Full-screen (no nav/sidebar)** via Next.js route groups | Immersive game experience. Use `(game)` route group with minimal layout. |
| Card rendering | **PNG images** (`public/images/cards/`) | 52 card images + `card_blank.png` already exist. CSS transitions for animations. |
| New dependencies | **Vitest + @testing-library/react** | Vitest is the modern choice: native ESM/TS support, faster than Jest, no transform config. No other new deps. |
| WS token approach | **New `GET /api/v1/auth/ws-token` endpoint** | JWT is in HttpOnly cookie (JS can't read it). Server needs a new endpoint that returns a short-lived WS token. See Phase 5.0. |

---

## Page Architecture Principle

**Pages must be thin.** All business logic belongs in `lib/` or `components/` where Vitest can reach it without the Next.js framework.

| Lives in | Contains |
|----------|----------|
| `lib/game/` | WebSocket client, game state reducer, pure derived-value functions, type definitions |
| `lib/api.ts` | All API calls (`gameServerApi.*`) |
| `components/game/` | Stateful UI components (receive props, dispatch actions) |
| `app/**/page.tsx` | Route entry, layout composition, `GameProvider` wiring — **nothing else** |

Any logic that could be a pure function or custom hook must be extracted, even if used once. The test for a good page: if you deleted it and recreated it from imports alone, no business logic would be lost.

---

## Phase 5.0: Server-Side Prerequisites

**Effort:** Small (< 1 day, server-side changes)

### 5.0.1 WebSocket Token Endpoint

The WebSocket handler (`GameWebSocketHandler`) authenticates via `?token=JWT` in the query string. The web app's JWT is in an HttpOnly cookie (inaccessible to JavaScript). We need a bridge.

**Add to `AuthController.java` (`/api/v1/auth`):**
```
GET /api/v1/auth/ws-token → { "token": "short-lived-jwt" }
```

- Requires authenticated request (cookie-based auth)
- Returns a short-lived JWT (60 seconds expiry) usable only for WebSocket connection
- The web client calls this endpoint, gets the token, appends `?token=xxx` to the WebSocket URL

**Security hardening:**
- **Single-use:** Token includes a `jti` (unique ID) claim. Server tracks used `jti` values in a TTL cache (60 seconds, same as token lifetime). Reject replayed tokens.
- **Rate limiting:** Max 5 requests/minute per authenticated user (prevents token farming).
- **Scope restriction:** Token includes `scope: "ws-connect"` claim. `GameWebSocketHandler` validates that `scope == "ws-connect"` or `scope == "reconnect"` (see 5.0.4). The ws-token cannot be used for REST API authentication.

**Files to modify:**
- `code/pokergameserver/.../controller/AuthController.java` — Add `wsToken()` endpoint with rate limiting
- `code/pokergameserver/.../service/AuthService.java` — Add `generateWsToken()` with `jti` + `scope` claims
- `code/pokergameserver/.../auth/JwtTokenProvider.java` — Add `generateScopedToken(username, profileId, scope, gameId, ttlMs)` method; update `validateToken()` to extract scope/gameId claims
- `code/pokergameserver/.../websocket/GameWebSocketHandler.java` — Validate `scope` claim on connection; track used `jti` values

### 5.0.2 Game State Snapshot on Reconnect

Currently `GameWebSocketHandler.afterConnectionEstablished()` sends `CONNECTED` with `null` gameState (line 188-189). For in-progress games, the reconnecting player receives no state snapshot — they only get future events. This means the UI would be blank until the next event arrives.

**Fix:** After sending `CONNECTED`, when game is `IN_PROGRESS` or `PAUSED`, build and send a `GAME_STATE` snapshot:
- Build `GameStateData` from the `GameInstance`'s current `ServerTournamentContext` / `PokerGame` / `PokerTable` state
- Include per-player hole card filtering (only send the reconnecting player's cards)
- Send as a separate `GAME_STATE` message after `CONNECTED`

**Files to modify:**
- `code/pokergameserver/.../websocket/GameWebSocketHandler.java` — Add state snapshot send after reconnect
- `code/pokergameserver/.../websocket/OutboundMessageConverter.java` — Add `createGameStateSnapshot()` method (may already exist partially)

### 5.0.3 Close Replaced WebSocket Sessions (Multiple Tabs Fix)

Currently, when a player opens the game in a second tab, `GameWebSocketHandler` registers the new connection but **never closes the old WebSocket session**. The old tab stops receiving server messages (connection manager now points at the new one) but remains open, can still send actions, and has no way to distinguish "replaced" from a network failure — so it loops on reconnect.

**Fix:** Before registering the new `PlayerConnection`, check if the player already has an active connection. If so, close the old session with close code **4409** and then register the new one.

- Close code 4409 is application-defined: "Connection replaced by new session"
- The client (`WebSocketClient.ts`) treats 4409 as terminal — does not reconnect, instead shows "This game was opened in another tab"
- Only applies when game is IN_PROGRESS or PAUSED (reconnect path). WAITING_FOR_PLAYERS allows multiple lobby connections from the same player without conflict (spectating lobby is fine).

**Files to modify:**
- `code/pokergameserver/.../websocket/GameWebSocketHandler.java` — Before `connectionManager.addConnection()`, retrieve existing connection for `profileId` and close its session with `CloseStatus(4409, "Connection replaced by new session")`

### 5.0.4 Reconnect Token (Session Expiry Protection)

A regular JWT has a 1-hour TTL (7 days with remember-me). If the auth cookie expires mid-game and the WebSocket drops, the player can't obtain a new ws-token — they'd be locked out of their own game. The reconnect token solves this by decoupling game reconnection from cookie session state.

**How it works:**

1. When the server sends the `CONNECTED` message to a player, it includes a `reconnectToken` — a game-scoped JWT with:
   - `profileId` and `username` (same as normal JWT)
   - `gameId` claim (scopes token to one specific game)
   - `scope: "reconnect"` claim (prevents use as a general auth or ws-connect token)
   - **24-hour TTL** (outlives any reasonable game session)

2. The web client stores the reconnect token **in memory only** (not sessionStorage/localStorage — cleared on tab close is acceptable since the WebSocket also closes).

3. On WebSocket reconnect, the client uses the reconnect token directly in `?token=reconnectToken` — **bypasses the cookie-based ws-token endpoint entirely**.

4. Server validation in `GameWebSocketHandler.afterConnectionEstablished()`:
   - If token has `scope == "reconnect"`: verify `gameId` claim matches the URL gameId, verify player is already in the game (`hasPlayer(profileId)`). Reject if mismatched.
   - If token has `scope == "ws-connect"`: existing validation (no gameId check needed, single-use jti check applies).

5. The ws-token endpoint (5.0.1) is still used for the **initial** connection. Reconnection uses the reconnect token — no cookie required.

**Files to modify:**
- `code/pokergameserver/.../auth/JwtTokenProvider.java` — `generateScopedToken()` already added in 5.0.1; used here with `scope: "reconnect"`, `gameId` claim, 24h TTL
- `code/pokergameserver/.../websocket/GameWebSocketHandler.java` — Generate and include reconnect token in CONNECTED message; validate `scope`+`gameId` claims on reconnect
- `code/pokergameserver/.../websocket/OutboundMessageConverter.java` — Include `reconnectToken` field in CONNECTED message payload

---

## Phase 5.1: WebSocket Client Library & Game Types

**Effort:** Medium (1–1.5 weeks)

### 5.1.1 TypeScript Types — `code/web/lib/game/types.ts`

Mirror the server's `ServerMessageData.java` and `ClientMessageData.java` exactly:
- 34 server message types (`ServerMessageType` union)
- 9 client message types (`ClientMessageType` union)
- All data interfaces: `GameStateData`, `TableData`, `SeatData`, `ActionOptionsData`, `LobbyStateData`, etc.
- REST DTO types: `GameSummaryDto`, `GameConfigDto`, `GameJoinResponseDto`, `BlindLevelConfig`, etc.

**Source of truth:** `code/pokergameserver/.../websocket/message/ServerMessageData.java` and `ClientMessageData.java`

### 5.1.2 WebSocket Client — `code/web/lib/game/WebSocketClient.ts`

Thin wrapper around browser `WebSocket`:
- `connect(wsUrl: string, token: string)` — opens connection to `{wsUrl}?token={token}`
- `disconnect()` — clean close
- `send(type: ClientMessageType, data?)` — serialize + send with auto-incrementing `sequenceNumber`
- Reconnection: exponential backoff (1s → 2s → 4s → ... → 30s max, 10 attempts)
- Connection states: `DISCONNECTED | CONNECTING | CONNECTED | RECONNECTING`
- Event callbacks: `onMessage`, `onStateChange`, `onError`

**WebSocket protocol derivation:**
The client derives the WS protocol from the page protocol — never hardcoded:
```typescript
const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
```
- **Site on HTTPS:** Only `wss://` connections. Community-hosted games (no TLS) are not reachable — shown as "Desktop client required" in the lobby.
- **Site on HTTP:** `ws://` connections allowed. Community-hosted games are directly joinable.

See `.claude/plans/COMMUNITY-GAME-WS-PROXY.md` for the future plan to proxy community game connections from HTTPS sites.

**Token flow:**
1. Call `GET /api/v1/auth/ws-token` → get `{ token }`
2. Call `POST /api/v1/games/{id}/join` → get `{ wsUrl, gameId }`
3. Connect to `{wsProtocol}//{wsHost}/ws/games/{gameId}?token={token}`
4. On reconnect: use stored reconnect token (from CONNECTED message, see 5.0.4), reconnect to same URL — no cookie required

### 5.1.3 Game State Reducer — `code/web/lib/game/gameReducer.ts`

Pure reducer function: `(state: GameState, message: ServerMessage) => GameState`

State shape:
- `connectionState`: connection status
- `gamePhase`: `'lobby' | 'playing' | 'complete'`
- `myPlayerId`: set from CONNECTED message
- `lobbyState`: pre-game lobby data
- `gameState`: full game state snapshot
- `currentTable`: the table where current player sits
- `holeCards`: current player's private cards
- `actionRequired`: non-null when it's my turn
- `actionTimer`: countdown data
- `chatMessages`: capped array (200, FIFO — drop oldest on overflow)
- `standings`: final results
- `error`: error state

Each of the 34 server message types maps to a specific state transition.

**Message validation:** The reducer validates incoming message shape with TypeScript type guards before processing. Unknown message types are logged to `console.warn` and discarded. Malformed messages (missing required fields) are discarded with a warning. No message should ever crash the UI or corrupt state — the reducer must be defensive at this boundary.

**Optimistic updates policy:**
- **Game actions (fold/call/raise/bet/check):** Not optimistic. After sending `PLAYER_ACTION`, disable the action panel immediately and wait for `PLAYER_ACTED` before updating game state. This prevents stale UI if the server rejects the action.
- **Chat messages:** Optimistic. Show message immediately in the chat panel; no server confirmation event is needed.
- **Admin actions (kick/pause/resume):** Not optimistic. Wait for the corresponding server event before updating UI.

### 5.1.4 Game Context — `code/web/lib/game/GameContext.tsx`

React Context + Provider wrapping the reducer and WebSocket client:
- `GameProvider` manages WebSocket lifecycle and dispatches messages to reducer
- Children access state via `useGameState()` hook
- Actions via `useGameActions()` hook: `sendAction`, `sendChat`, `sendAdminKick`, etc.

### 5.1.5 REST API Extensions — `code/web/lib/api.ts` (modify)

Add `gameServerApi` calling `/api/v1/*` endpoints:
- `register(userData)` — `POST /api/v1/auth/register` (**use this for the /register page**, not `authApi.register` which calls the legacy `/api/auth/register`)
- `getWsToken()` — `GET /api/v1/auth/ws-token`
- `listGames(params)` — `GET /api/v1/games`
- `getGame(id)` — `GET /api/v1/games/{id}`
- `createGame(config)` — `POST /api/v1/games`
- `createPracticeGame(config)` — `POST /api/v1/games/practice` (creates, auto-joins caller, auto-starts; returns `{ gameId }` — no separate join/start needed)
- `joinGame(id, password?)` — `POST /api/v1/games/{id}/join`
- `startGame(id)` — `POST /api/v1/games/{id}/start`
- `updateSettings(id, settings)` — `PUT /api/v1/games/{id}/settings`
- `kickPlayer(id, profileId)` — `POST /api/v1/games/{id}/kick`
- `cancelGame(id)` — `DELETE /api/v1/games/{id}`

Uses existing `apiFetch` pattern with `credentials: 'include'`.

### 5.1.6 Testing Setup & Tests

**New config:** `code/web/vitest.config.ts`
```typescript
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/', 'coverage/', '.next/', '**/*.config.ts'],
    },
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './') },
  },
})
```

**New devDependencies:** `vitest`, `@vitejs/plugin-react`, `@testing-library/react`, `@testing-library/user-event`, `jsdom`, `@vitest/coverage-v8`

**New `package.json` scripts:**
```json
"test": "vitest run",
"test:watch": "vitest",
"test:coverage": "vitest run --coverage"
```

Vitest scope: client components and pure lib functions only. Server components and Next.js routing require E2E tooling (out of M5 scope).

Tests:
- `lib/game/__tests__/WebSocketClient.test.ts` — mock `WebSocket`, test connect/reconnect/send
- `lib/game/__tests__/gameReducer.test.ts` — test each message type's state transition

---

## Phase 5.2: Poker Table Components

**Effort:** Large (1.5–2 weeks)

### Component Hierarchy

```
PokerTable (full-screen container, dark background)
├── TournamentInfoBar (top: level, blinds, next-level timer, player count)
├── TableFelt (oval green surface)
│   ├── CommunityCards (5 slots, center)
│   └── PotDisplay (below cards)
├── PlayerSeat ×10 (positioned around oval via CSS absolute %)
│   ├── Card ×2 (hole cards: face-up for self, face-down for others)
│   ├── DealerButton / BlindMarker
│   └── StatusBadge (FOLDED, ALL_IN, DISCONNECTED)
├── ActionPanel (bottom center, visible when it's my turn)
│   └── BetSlider (range input with presets: Min, ½ Pot, Pot, All-In)
├── ActionTimer (countdown bar for current actor)
├── HandHistory (collapsible right panel)
├── ChatPanel (collapsible bottom-right)
└── GameOverlay (modal: rebuy/addon offer, pause, eliminated)
```

### Files to Create

| File | Purpose |
|------|---------|
| `components/game/PokerTable.tsx` | Main container, dark background, positions children |
| `components/game/TableFelt.tsx` | Green oval with CSS gradient |
| `components/game/PlayerSeat.tsx` | Player info: name, chips, cards, status, bet |
| `components/game/Card.tsx` | Single card image (`card_{rank}{suit}.png` or `card_blank.png`) |
| `components/game/CommunityCards.tsx` | 5-slot card row |
| `components/game/PotDisplay.tsx` | Main pot + side pots |
| `components/game/ActionPanel.tsx` | Fold/Check/Call/Raise buttons |
| `components/game/BetSlider.tsx` | Range input for bet/raise amounts |
| `components/game/ActionTimer.tsx` | Countdown progress bar |
| `components/game/TournamentInfoBar.tsx` | Level, blinds, timer, player count |
| `components/game/HandHistory.tsx` | Scrollable action log |
| `components/game/ChatPanel.tsx` | Chat messages + input **(never use `dangerouslySetInnerHTML` for message content — render as text only)** |
| `components/game/DealerButton.tsx` | "D" chip indicator |
| `components/game/GameOverlay.tsx` | Modal overlays (rebuy, addon, pause, eliminated) |

### Key Design Details

**Seat positioning:** 10 fixed positions around the oval (CSS `position: absolute` with percentage coordinates). Current player always at bottom center (seat indices rotated in UI).

**Action panel:** Shown only when `actionRequired` is non-null. Buttons derive from `ActionOptionsData`:
- Fold (canFold), Check (canCheck), Call $X (canCall), Raise (canRaise), All-In
- BetSlider: `minRaise..maxRaise` range with presets (Min, ½ Pot, Pot, All-In)
- BetSlider uses **integer arithmetic only** (no floating-point). Format chip counts with `Intl.NumberFormat`. Test with chip counts > 1,000,000.
- Keyboard shortcuts: F=Fold, C=Check/Call, R=Raise, Enter=Confirm **(suppressed when chat input or any text field is focused)**

**Card component:** Maps server card notation (e.g., `"Ah"`) to existing images at `public/images/cards/card_Ah.png`. Face-down = `card_blank.png`.

**Action timer:** Server sends `timeRemainingMs` (not `startedAt`) so client doesn't need clock synchronization. Client adds a 500ms visual buffer before displaying "timed out" to account for network latency.

**XSS prevention:** All user-supplied strings — usernames, game names, chat messages — are rendered as **React text nodes only**. Never `dangerouslySetInnerHTML`. This applies to every component that renders user content: `ChatPanel`, `PlayerSeat`, `HandHistory`, `TournamentInfoBar`, `GameInfoPanel`, lobby pages.

**Animations (CSS only):**
- Card dealing: `transition: transform 0.3s`
- Current actor highlight: CSS pulse animation
- Timer bar: `transition: width` synchronized to timer updates
- Pot changes: `transition: opacity 0.3s`

**Dark theme:** CSS variables in `globals.css`:
- Table background: `#1a1a2e`, felt green: `#2d5a1b`, gold accent: `#FFD700`

### Tests
- `components/game/__tests__/Card.test.tsx` — correct images for card codes
- `components/game/__tests__/ActionPanel.test.tsx` — buttons enabled/disabled from options

---

## Phase 5.3: Game Management Pages

**Effort:** Large (1.5–2 weeks)

### Route Structure

```
app/
├── (default)/              ← existing layout (nav + sidebar + footer)
│   ├── register/
│   │   └── page.tsx        ← /register — new user registration
│   ├── games/
│   │   ├── layout.tsx      ← games section sidebar layout
│   │   ├── page.tsx        ← /games — game lobby listing (replaces /online/available+current+completed)
│   │   ├── create/
│   │   │   └── page.tsx    ← /games/create — create game form
│   │   └── [gameId]/
│   │       ├── lobby/
│   │       │   └── page.tsx ← /games/[gameId]/lobby — pre-game lobby
│   │       └── results/
│   │           └── page.tsx ← /games/[gameId]/results — post-game results
└── (game)/                 ← full-screen layout (no nav/sidebar/footer)
    └── games/
        └── [gameId]/
            └── play/
                └── page.tsx ← /games/[gameId]/play — game play (immersive)
```

**Route groups** allow the play page to have its own minimal layout while other game pages use the standard layout. **Verify** that Next.js resolves `(default)/games/[gameId]/lobby` vs `(game)/games/[gameId]/play` unambiguously — test both routes during Phase 5.3 integration.

### Page Details

**`/register` — User Registration**
- Registration form: username, password, confirm password, email
- Calls `POST /api/v1/auth/register`
- On success: auto-login (cookie set by server), redirect to `/games`
- Link from login page ("Don't have an account? Register")
- Validation: username uniqueness (server-side), password strength, email format

**`/games` — Game Lobby** (replaces `/online/available`, `/online/current`, `/online/completed`)
- Tab filters: Open / In Progress / Completed
- Search by game name or host
- DataTable: Name, Host, Players, Blinds, Type (SERVER/COMMUNITY), Private (lock), Actions (Join/View)
- Community-hosted games: if site is HTTPS, show "Desktop client required" badge and disable Join button. If site is HTTP, Join works directly via `ws://`.
- Join: if private → password dialog → `joinGame()` → navigate to lobby. Password sent in POST body only (never in URL/query params). `PasswordDialog` clears input state on unmount.
- Create button → `/games/create`
- Auto-refresh: poll every 10s
- Auth required

**`/games/create` — Create Game**
- Form building `GameConfigDto`:
  - Basic: name, max players, buy-in, starting chips
  - Blind structure: preset templates (Turbo/Standard/Slow) + custom editor
  - AI: toggle fill-with-AI, skill level, count
  - Advanced (collapsible): game type, password, rebuys, addons, timeouts
- Submit: `createGame()` → navigate to `/games/{id}/lobby`
- Quick Practice: `createPracticeGame()` → navigate to `/games/{id}/play`

**`/games/[gameId]/lobby` — Pre-Game Lobby**
- WebSocket connection for live updates (LOBBY_STATE, LOBBY_PLAYER_JOINED, etc.)
- Player list with owner badge, AI badge, kick button (owner only)
- Game settings summary
- Chat panel
- Owner controls: Start Game, Cancel, Edit Settings
- Non-owner: Leave Game
- On LOBBY_GAME_STARTING: countdown → navigate to `/games/{id}/play`

**`/games/[gameId]/play` — Game Play** (full-screen, `(game)` route group)
- `GameProvider` wraps page, opens WebSocket
- Renders `PokerTable` component full-screen
- Minimal top bar: game name + leave button
- On GAME_COMPLETE: navigate to `/games/{id}/results`

**`/games/[gameId]/results` — Game Results**
- Standings table (position, name, prize)
- Game stats (total hands, duration)
- Buttons: Play Again, Back to Lobby
- Data from context (if navigated from play) or REST fallback

### `/online/` Page Removal & Navigation Update

The site is not in production. The three legacy game listing pages and all the code they exclusively used are **deleted outright** — no redirects.

**Delete these page files entirely:**
- `app/online/available/page.tsx` — legacy `GET /api/games?modes=0` listing
- `app/online/current/page.tsx` — legacy `GET /api/games?modes=1` listing
- `app/online/completed/page.tsx` — legacy `GET /api/games?modes=2` listing

**`lib/api.ts` — Delete `gamesApi` export entirely:**
- `getAvailable`, `getRunning`, `getCompleted` — each used by one of the deleted pages
- `getDetails` — `GET /api/games/{id}` — already dead (no page ever called it)

**`lib/mappers.ts` — Delete three mapper functions:**
- `mapAvailableGame`, `mapCurrentGame`, `mapCompletedGame` — used only by the deleted pages

**`lib/types.ts` — Delete 8 types orphaned by the above:**
- `Game`, `GameDetails`, `GamePlayer`, `BlindLevel` — used only by `gamesApi`
- `GameListResponse` — return type of deleted `gamesApi` functions
- `OnlineGameDto`, `TournamentProfileDto`, `GamePlayerDto` — used only by deleted mappers

**`lib/navData.ts` — Update `online` nav entry:**
```
// Before
online: { title: 'Online', link: '/online/available' }

// After — nav top-level points to the portal landing, not a deleted page
online: { title: 'Online', link: '/online' }
```
Also add the new top-level `games` nav item: `{ title: 'Games', link: '/games' }`

**`lib/sidebarData.ts` — Rewrite `onlineSidebarData` Games section:**
Remove the three deleted page links (`/online/available`, `/online/current`, `/online/completed`) and replace with a link to `/games`. The Statistics section keeps Leaderboard, History; the Profile section keeps My Profile, Search Players.

```typescript
// Before (Games section):
{ title: 'Available Games', link: '/online/available' },
{ title: 'Current Games', link: '/online/current' },
{ title: 'Hosts', link: '/online/hosts' },
// Statistics section:
{ title: 'Completed Games', link: '/online/completed' },

// After (Games section):
{ title: 'Game Lobby', link: '/games' },
{ title: 'Hosts', link: '/online/hosts' },
// Statistics section: remove Completed Games entry (now in /games)
```

Add new `gamesSidebarData` export for the `/games` section layout:
```typescript
export const gamesSidebarData: SidebarSection[] = [
  {
    title: 'Games',
    items: [
      { title: 'Game Lobby', link: '/games', exactMatch: true },
      { title: 'Create Game', link: '/games/create' },
      { title: 'Quick Practice', link: '/games/create?practice=true' },
    ],
  },
]
```

**`app/online/page.tsx` — Remove dead game links, add Games link:**
The portal landing page has a "Games" section with links to the three deleted pages and RSS feeds. Replace that entire section with a single link to `/games`:
```
// Remove: Available Games (/online/available + RSS), Current Games (/online/current + RSS),
//         Completed Games (/online/completed + RSS)
// Add: Game Lobby (/games)
```

**`/register` page — use `gameServerApi`, not `authApi`:**
`authApi.register` calls the old `/api/auth/register` (legacy WAN path). The new `/register` page must call `/api/v1/auth/register` (pokergameserver JWT-based profiles). Add `register` to `gameServerApi` in Phase 5.1.5 — do not reuse `authApi.register`.

**Pre-existing issues (do NOT fix in M5 — note only):**
- `online/history/page.tsx` links to `/online/game?id=...` — no such page exists. Pre-existing dead link.

### Navigation Integration

Modify existing files:
- `lib/navData.ts` — Add "Games" nav item pointing to `/games`, add "Register" link on login page
- `lib/sidebarData.ts` — Add games sidebar: Lobby, Create Game. Update online sidebar to replace game listing links with `/games`

### Files to Create/Modify

New files: ~8 page/layout files + 1 password dialog component + 1 game CSS file
Modified: `api.ts`, `navData.ts`, `sidebarData.ts`, `globals.css`

---

## Phase 5.4: Game Info Panel

**Effort:** Small (2–3 days)

**File:** `components/game/GameInfoPanel.tsx`

Reusable panel showing game details. Used in:
- Lobby page sidebar
- In-game overlay (toggle with `i` key)
- Results page header

Sections: game name, host, status, buy-in, starting chips, blind structure (current level highlighted), player list with chip counts, tournament clock.

---

## Execution Order

```
5.0 Server prerequisite (ws-token endpoint)
 ↓
5.1.1 Types
 ↓
5.1.5 REST API extensions ──→ 5.1.2 WebSocket client
                               ↓
                              5.1.3 Game state reducer
                               ↓
                              5.1.4 Game context + hooks
                               ↓
                              5.1.6 Tests + Jest setup
 ↓
5.2 Table components (Card → ActionPanel → PlayerSeat → PokerTable → supporting)
 ↓         ↘
 ↓          5.4 GameInfoPanel
 ↓         ↙
5.3 Pages (Layout → Lobby → Create → Pre-game lobby → Play → Results)
```

---

## Files Summary

### New Files (~35)

**Phase 5.0 (5 server files modified)**
- `AuthController.java` — add `wsToken()` endpoint with rate limiting
- `AuthService.java` — add `generateWsToken()` with `jti` + `scope` claims
- `JwtTokenProvider.java` — add `generateScopedToken()` for ws-connect and reconnect tokens; update `validateToken()` to extract scope/gameId claims
- `GameWebSocketHandler.java` — validate token scope; send GAME_STATE snapshot on reconnect; close replaced sessions with code 4409; include reconnect token in CONNECTED message; track used `jti` values
- `OutboundMessageConverter.java` — add `createGameStateSnapshot()`; include `reconnectToken` field in CONNECTED message

**Phase 5.1 — Library (8 files)**
- `code/web/lib/game/types.ts`
- `code/web/lib/game/WebSocketClient.ts`
- `code/web/lib/game/gameReducer.ts`
- `code/web/lib/game/GameContext.tsx`
- `code/web/lib/game/hooks.ts`
- `code/web/lib/game/__tests__/WebSocketClient.test.ts`
- `code/web/lib/game/__tests__/gameReducer.test.ts`
- `code/web/vitest.config.ts`

**Phase 5.2 — Components (16 files)**
- `code/web/components/game/PokerTable.tsx`
- `code/web/components/game/TableFelt.tsx`
- `code/web/components/game/PlayerSeat.tsx`
- `code/web/components/game/Card.tsx`
- `code/web/components/game/CommunityCards.tsx`
- `code/web/components/game/PotDisplay.tsx`
- `code/web/components/game/ActionPanel.tsx`
- `code/web/components/game/BetSlider.tsx`
- `code/web/components/game/ActionTimer.tsx`
- `code/web/components/game/TournamentInfoBar.tsx`
- `code/web/components/game/HandHistory.tsx`
- `code/web/components/game/ChatPanel.tsx`
- `code/web/components/game/DealerButton.tsx`
- `code/web/components/game/GameOverlay.tsx`
- `code/web/components/game/__tests__/Card.test.tsx`
- `code/web/components/game/__tests__/ActionPanel.test.tsx`

**Phase 5.3 — Pages (13 files)**
- `code/web/app/(default)/register/page.tsx`
- `code/web/app/(default)/games/layout.tsx`
- `code/web/app/(default)/games/page.tsx`
- `code/web/app/(default)/games/create/page.tsx`
- `code/web/app/(default)/games/[gameId]/lobby/page.tsx`
- `code/web/app/(default)/games/[gameId]/results/page.tsx`
- `code/web/app/(game)/games/[gameId]/play/page.tsx`
- `code/web/app/(game)/layout.tsx`
- `code/web/app/games/games.css`
- `code/web/components/game/PasswordDialog.tsx`
- `code/web/app/online/available/page.tsx` (modify: redirect to /games?tab=open)
- `code/web/app/online/current/page.tsx` (modify: redirect to /games?tab=in-progress)
- `code/web/app/online/completed/page.tsx` (modify: redirect to /games?tab=completed)

**Phase 5.4 — Info Panel (2 files)**
- `code/web/components/game/GameInfoPanel.tsx`
- `code/web/components/game/__tests__/GameInfoPanel.test.tsx`

### Modified Files (8)
- `code/web/lib/api.ts` — Add `gameServerApi`; delete `gamesApi` and its types
- `code/web/lib/mappers.ts` — Delete `mapAvailableGame`, `mapCurrentGame`, `mapCompletedGame`
- `code/web/lib/types.ts` — Delete 8 orphaned types (`Game`, `GameDetails`, `GamePlayer`, `BlindLevel`, `GameListResponse`, `OnlineGameDto`, `TournamentProfileDto`, `GamePlayerDto`)
- `code/web/lib/navData.ts` — Point `online` nav to `/online`; add `games` nav item
- `code/web/lib/sidebarData.ts` — Rewrite `onlineSidebarData` Games section; add `gamesSidebarData`
- `code/web/app/online/page.tsx` — Remove dead game listing links; add link to `/games`
- `code/web/app/globals.css` — Add game dark theme variables
- `code/web/package.json` — Add vitest + testing-library devDependencies; add test/test:watch/test:coverage scripts

### Deleted Files (3)
- `code/web/app/online/available/page.tsx` — Legacy game listing page (replaced by `/games`)
- `code/web/app/online/current/page.tsx` — Legacy game listing page (replaced by `/games`)
- `code/web/app/online/completed/page.tsx` — Legacy game listing page (replaced by `/games`)

---

## Verification

### Automated Tests
- Jest unit tests for WebSocket client, game reducer, and key components
- Run: `cd code/web && npm test`

### Manual Testing (against running server)
1. **Game creation:** Create a game via `/games/create`, verify it appears in `/games` list
2. **Join flow:** Join from lobby → pre-game lobby → see player list update in real-time
3. **Full game:** Start game → play hands (fold/check/call/raise) → see cards deal, actions broadcast, pots update → game completes → results show
4. **Practice game:** Quick practice with AI → full game through to completion
5. **Private game:** Create with password → verify password prompt on join
6. **Reconnection:** Disconnect (close tab) → reopen → verify state restored
7. **Owner controls:** Start, pause, kick player from lobby and in-game
8. **Responsive:** Test on desktop (1920px) and tablet (768px) widths

### Integration with Server
- Start standalone server: `cd code && mvn spring-boot:run -pl pokergameserver` (or via Docker)
- Start web dev server: `cd code/web && npm run dev`
- Configure `NEXT_PUBLIC_API_BASE_URL` to point at the server

---

## Risks

| # | Risk | Impact | Mitigation |
|---|------|--------|------------|
| R1 | WS token endpoint needed (server change) | Blocks WebSocket auth | Phase 5.0 — small addition to existing AuthController. <1 day effort. |
| R2 | Route groups require restructuring existing routes | Medium | Only new game pages use route groups. Existing `/online/` pages redirected. |
| R3 | Card images may not look right at small sizes | Low | Card PNGs are already in the project; test at different sizes, add CSS quality hints. |
| R4 | Reconnection state recovery | Medium | **Resolved in Phase 5.0.2** — Server currently sends null gameState on reconnect. Fix: send GAME_STATE snapshot after CONNECTED for in-progress games. |
| R5 | Full-screen layout conflicts with root layout | Low | Route groups cleanly separate layouts. `(game)` group has its own layout without Navigation/Footer. |
| R6 | WS token expiry between fetch and connect | Low | Token has 60s TTL. On auth failure during connect, auto-refetch token and retry once. |
| R7 | Session cookie expiry during gameplay | Low | **Resolved in Phase 5.0.4.** Reconnect token (24h TTL, game-scoped) is issued in the CONNECTED message. Client uses it for reconnection — no cookie required. Cookie expiry has zero impact on active games. |
| R8 | Multiple browser tabs opening same game | Low | **Resolved in Phase 5.0.3.** Server closes old WebSocket with code 4409 when new connection registered. Client treats 4409 as terminal — shows "Opened in another tab", does not reconnect. |
| R9 | Game state null on reconnect (lobby vs in-progress) | Medium | Client handles both paths: if CONNECTED has gameState → playing mode; if LOBBY_STATE follows → lobby mode. |
| R10 | WS token replay attack | Low | **Resolved in Phase 5.0.1.** Tokens are single-use (`jti` claim tracked in TTL cache). Endpoint rate-limited to 5/min per user. |
| R11 | Action timer client/server clock drift | Low | **Resolved.** Server sends `timeRemainingMs` (countdown) not `startedAt` (wall clock). Client adds 500ms visual buffer. |
| R12 | Community games unreachable from HTTPS site | Low | **Accepted limitation.** Community games shown as "Desktop client required" when site is HTTPS. See `.claude/plans/COMMUNITY-GAME-WS-PROXY.md` for future proxy solution. |

---

## Not In Scope

- Mobile-optimized layout (desktop + tablet only)
- Sound effects
- Spectator/observer mode
- Multi-table tournament table switching
- Server-side changes beyond Phase 5.0 (ws-token endpoint)
- PWA / offline support
- Storybook / visual regression testing
- E2E tests (Cypress/Playwright)
- **Community-hosted game WebSocket proxy** — When the site is served over HTTPS, browsers block `ws://` connections to community hosts (mixed content). Community games are shown as "Desktop client required" in the lobby. When the site is HTTP, community games are directly joinable via `ws://`. A future server-side WebSocket proxy will allow community game connections from HTTPS sites — see `.claude/plans/COMMUNITY-GAME-WS-PROXY.md`.
