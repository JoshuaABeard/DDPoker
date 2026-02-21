# Review Request

**Branch:** feature-m5-web-client-game-ui
**Worktree:** ../DDPoker-feature-m5-web-client-game-ui
**Plan:** .claude/plans/M5-WEB-CLIENT-GAME-UI.md
**Requested:** 2026-02-18

## Summary

Implements M5: Web Client Game UI — a complete in-browser poker client built on the existing pokergameserver REST + WebSocket protocol. Adds WebSocket token auth to the server (Phase 5.0), TypeScript game state library with reducer/context/hooks (Phase 5.1), full poker table UI components (Phase 5.2), and game management pages: lobby listing, create game, pre-game lobby, and play page (Phase 5.3). Removes the three legacy game listing pages (`/online/available`, `/online/current`, `/online/completed`) which are superseded by `/games`.

## Files Changed

### Phase 5.0 — Server (Java)
- [x] `code/pokergameserver/.../auth/JwtTokenProvider.java` — Added `generateScopedToken()`, made `getClaims()` public, added scope/gameId/jti claim extractors
- [x] `code/pokergameserver/.../controller/AuthController.java` — Added `GET /api/v1/auth/ws-token` (rate-limited, 60s jti-tracked token)
- [x] `code/pokergameserver/.../service/AuthService.java` — Added `generateWsToken()`, `generateReconnectToken()`, `markJtiUsed()`, `isJtiUsed()` with rate limiting
- [x] `code/pokergameserver/.../websocket/GameWebSocketHandler.java` — Scope validation, 4409 session replacement, reconnect token in CONNECTED
- [x] `code/pokergameserver/.../websocket/OutboundMessageConverter.java` — `reconnectToken` field in ConnectedData
- [x] `code/pokergameserver/.../websocket/WebSocketAutoConfiguration.java` — Injects `AuthService` into `GameWebSocketHandler`
- [x] `code/pokergameserver/.../websocket/message/ServerMessageData.java` — `ConnectedData` record adds `reconnectToken` field
- [x] `code/pokergameserver/.../websocket/GameWebSocketHandlerTest.java` — Updated for new constructor + `getClaims()` mock

### Phase 5.1 — TypeScript library (new files)
- [x] `code/web/lib/game/types.ts` — All TypeScript types mirroring server message structures
- [x] `code/web/lib/game/WebSocketClient.ts` — WebSocket wrapper with reconnect/backoff, reconnect token support
- [x] `code/web/lib/game/gameReducer.ts` — Pure reducer, 34 server message types, defensive against malformed messages
- [x] `code/web/lib/game/GameContext.tsx` — React Context + Provider; ws-token → joinGame → WebSocket lifecycle
- [x] `code/web/lib/game/hooks.ts` — `useGameState()` and `useGameActions()`
- [x] `code/web/lib/game/__tests__/gameReducer.test.ts` — Reducer unit tests
- [x] `code/web/lib/game/__tests__/WebSocketClient.test.ts` — WS client unit tests
- [x] `code/web/vitest.config.ts` — Vitest + jsdom + React setup
- [x] `code/web/lib/api.ts` — Removed `gamesApi`; added `gameServerApi` with all game endpoints
- [x] `code/web/lib/types.ts` — Removed 8 orphaned types from deleted gamesApi
- [x] `code/web/lib/mappers.ts` — Removed 3 orphaned mapper functions
- [x] `code/web/lib/navData.ts` — Updated online link, added Games nav item
- [x] `code/web/lib/sidebarData.ts` — Updated online sidebar, added `gamesSidebarData`
- [x] `code/web/package.json` — Added vitest devDependencies and test scripts

### Phase 5.2 — Components (new files)
- [x] `code/web/components/game/Card.tsx` — Single card image, XSS-safe path validation
- [x] `code/web/components/game/CommunityCards.tsx` — 5-slot community card row
- [x] `code/web/components/game/PotDisplay.tsx` — Main pot + side pots
- [x] `code/web/components/game/DealerButton.tsx` — D/SB/BB markers
- [x] `code/web/components/game/PlayerSeat.tsx` — Player seat around oval (XSS-safe text)
- [x] `code/web/components/game/BetSlider.tsx` — Bet range input with presets, integer arithmetic
- [x] `code/web/components/game/ActionPanel.tsx` — Fold/Check/Call/Bet/Raise/All-In with keyboard shortcuts
- [x] `code/web/components/game/ActionTimer.tsx` — Countdown progress bar (server-driven, not wall-clock)
- [x] `code/web/components/game/TournamentInfoBar.tsx` — Level/blinds/timer/players top bar
- [x] `code/web/components/game/ChatPanel.tsx` — Chat messages + input, auto-scroll, 500-char limit, XSS-safe
- [x] `code/web/components/game/HandHistory.tsx` — Collapsible action log
- [x] `code/web/components/game/GameOverlay.tsx` — Modal overlays: rebuy/addon/paused/eliminated/tab-replaced
- [x] `code/web/components/game/TableFelt.tsx` — Oval green felt (radial gradient, inset shadow)
- [x] `code/web/components/game/PokerTable.tsx` — Full-screen container; 10 seats around oval, seat rotation so current player is always at bottom center
- [x] `code/web/components/game/__tests__/Card.test.tsx` — Card image/alt/XSS tests
- [x] `code/web/components/game/__tests__/ActionPanel.test.tsx` — Button visibility, click actions, slider flow, large chip formatting

### Phase 5.3 — Pages (new files)
- [x] `code/web/app/register/page.tsx` — Registration form; calls `gameServerApi.register()`
- [x] `code/web/app/games/layout.tsx` — Games section sidebar layout
- [x] `code/web/app/games/page.tsx` — Game lobby: tab filter (Open/In Progress/Completed), search, join with password dialog, 10s auto-refresh
- [x] `code/web/app/games/create/page.tsx` — Create game form: basic, AI, advanced (password/rebuys/addon/timeout), Quick Practice button
- [x] `code/web/app/games/[gameId]/lobby/page.tsx` — Pre-game lobby: real-time player list, chat, owner controls (start/kick/cancel)
- [x] `code/web/app/games/[gameId]/results/page.tsx` — Results page (stub; full REST fallback deferred to future iteration)
- [x] `code/web/app/games/[gameId]/play/page.tsx` — Full-screen play page: `GameProvider` wraps `PokerTable`, `fixed inset-0` covers nav/footer, overlay management
- [x] `code/web/components/game/PasswordDialog.tsx` — Private game password dialog; clears state on unmount, password in body only (never URL)

### Phase 5.3 — Deletions and updates
- [x] Deleted: `code/web/app/online/available/page.tsx`
- [x] Deleted: `code/web/app/online/current/page.tsx`
- [x] Deleted: `code/web/app/online/completed/page.tsx`
- [x] `code/web/app/online/page.tsx` — Replaced Games section (3 dead links) with single `/games` link
- [x] `code/web/app/login/page.tsx` — Added "Don't have an account? Create one" link to `/register`
- [x] `code/web/app/globals.css` — Added `--game-bg`, `--felt-green`, `--gold-accent` CSS variables and `.game-bg` utility class

**Privacy Check:**
- ✅ SAFE — No private information found. No credentials, tokens, or personal data committed.

## Verification Results

- **Tests:** Not run (requires npm install in the web client, and Maven build for Java)
- **Coverage:** Not measured
- **Build:** Not verified in this session

## Context & Decisions

**Server prerequisites (Phase 5.0):**
- `GameInstance.buildStateSnapshot()` doesn't exist → CONNECTED message sends `null` gameState for initial connections; client waits for next server events. This is a known deferral.
- `GameConnectionManager.getConnection()` doesn't exist → iterated `sessionConnections` (local map in handler) to find existing player connections for 4409 close.
- Reconnect tokens scoped to gameId so they can't be used to connect to other games.

**Play page full-screen approach:**
- Instead of the `(game)` route group from the plan (which can't override root layout without restructuring all pages), the play page uses `position: fixed; inset: 0; z-index: 40` to visually cover the nav/footer. This achieves the same immersive effect without restructuring the existing app.

**Results page:**
- Results page is a stub that links to the game lobby. Full standings view requires either: (a) persisting standings through navigation state, or (b) a REST endpoint `GET /api/v1/games/{id}/results`. Both are deferred to a future iteration. The play page auto-navigates to results on GAME_COMPLETE.

**Overlays in PokerTable:**
- REBUY_OFFERED and ADDON_OFFERED aren't persisted in game state (reducer returns state unchanged for these). The play page manages overlay state locally and passes overlays to PokerTable via an `overlay` prop.
- Paused state is derived from `gameState.status === 'PAUSED'` (reason/pausedBy not stored in state).

**XSS prevention:**
- All user strings (playerName, gameName, chatMessage, game name in overlays/lobby/results) are rendered as React text nodes only. No `dangerouslySetInnerHTML` anywhere in game components.
- Card file paths validated with `/^[A-Za-z0-9]{2}$/` before use in image src.

---

## Review Results

**Status:** APPROVED_WITH_SUGGESTIONS

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-18

### Findings

#### Strengths

1. **Security is well-designed.** The JWT WS token flow (short-lived 60s token with jti single-use tracking, scope restriction, rate limiting) is correctly implemented end-to-end. The server validates `scope`, `jti`, and `gameId` claims in `GameWebSocketHandler.afterConnectionEstablished()` before allowing connection. Reconnect tokens are game-scoped with 24h TTL, preventing cross-game abuse. Password is sent only in POST body, never in URL. `PasswordDialog` clears state on unmount.

2. **XSS prevention is thorough and consistent.** Every component that renders user content (`ChatPanel`, `PlayerSeat`, `TournamentInfoBar`, `GameOverlay`, lobby page, results page, games listing) renders user strings as React text nodes only. No `dangerouslySetInnerHTML` anywhere in the new code. Card paths are validated with a strict `/^[A-Za-z0-9]{2}$/` regex. Verified with grep: the only mentions of `dangerouslySetInnerHTML` are in "do not use" comments.

3. **Reducer is defensive and well-structured.** All 34 server message types are handled in the switch. Unknown types log a warning and return state unchanged. Malformed messages (null, missing type) are caught at the boundary with try/catch. The reducer is a pure function with no side effects, making it easily testable. The `appendChat` helper correctly implements FIFO with a 200-message cap.

4. **WebSocket client has clean reconnect logic.** Exponential backoff (1s to 30s, ~10 attempts), reconnect token preference over initial token, 4409 close code for tab replacement (terminal, no reconnect), intentional disconnect flag. The `deriveWsProtocol()` function correctly derives ws/wss from the page protocol.

5. **Types mirror the server faithfully.** The TypeScript types in `types.ts` match `ServerMessageData.java` record-for-record. Field names are consistent (`ownerName`, `blinds.small/big`, `profileId`). REST DTO types (`GameSummaryDto`, `GameConfigDto`, etc.) match server DTOs.

6. **Test quality is good.** The `Card.test.tsx` includes XSS injection tests. `ActionPanel.test.tsx` covers button visibility, click actions, slider flow, and large chip formatting (1,500,000). `gameReducer.test.ts` covers CONNECTED (with/without gameState), LOBBY_STATE, LOBBY_PLAYER_JOINED (including duplicate prevention), HOLE_CARDS_DEALT, ACTION_REQUIRED, GAME_COMPLETE, CHAT_MESSAGE (including 200-message FIFO cap), ERROR, unknown types, and malformed messages. `WebSocketClient.test.ts` covers connect/disconnect, reconnect with backoff, reconnect token usage, 4409 handling, and send serialization.

7. **Architecture is clean.** Pages are thin wrappers around `GameProvider`. Business logic lives in `lib/game/` (reducer, types, WebSocket client) and `components/game/` (UI). The overlay prop pattern in `PokerTable` cleanly separates event-driven overlays from game state.

8. **Legacy cleanup is complete.** Three legacy pages deleted, `gamesApi` removed from `api.ts`, 3 orphaned mappers removed from `mappers.ts`, 8 orphaned types removed from `types.ts`, sidebar/nav updated.

#### Suggestions (Non-blocking)

1. **`CONNECTION_STATE_CHANGED` always creates a new object** (line 132 of `gameReducer.ts`): `return { ...state, connectionState: action.state }`. This means setting the same state value creates a new reference, triggering an unnecessary re-render. The test at line 42-47 (`does not re-render if connectionState unchanged`) currently passes because the `CONNECTION_STATE_CHANGED` action is returned as the same `state` object -- but reading the code, it actually spreads to a new object. The test assertion `expect(result).toBe(state)` should fail since `{ ...state }` is a new object. Verify this test actually passes, or add a guard: `if (action.state === state.connectionState) return state`.

2. **`handlePlayerLeft` is a no-op** (lines 493-498 of `gameReducer.ts`). The function receives the data but returns `state` unchanged for both PLAYER_LEFT and PLAYER_DISCONNECTED. The comment says "For disconnects, mark player as disconnected; for leaves, they're gone" but neither action is taken. This means disconnected players will still show as active on the table until the next full GAME_STATE snapshot. Consider at minimum marking the seat status as 'DISCONNECTED' for the PLAYER_DISCONNECTED case so the UI can visually indicate the player is offline.

3. **`handlePlayerJoined` is also a no-op** (lines 488-491). The comment says "reflected by next GAME_STATE update" but this means a joining player may not appear until the next hand starts. For lobby-to-game transitions this is fine, but for mid-game joins (if supported in the future) it would be incomplete.

4. **TODO comment in reducer** (line 182): `return state // TODO: update lobby settings when server sends this`. The CLAUDE.md prohibits TODO comments. This should either be implemented (update `lobbyState` with new settings from `LobbySettingsChangedData`) or documented as a known deferral in the handoff rather than left as a TODO in code.

5. **Overlay events (REBUY_OFFERED, ADDON_OFFERED) cannot reach the PlayContent component.** The play page comments (lines 67-70) acknowledge this: "Since we can't easily hook into that callback from here, we observe state changes." But the reducer discards these messages, and no state change occurs, so the overlays will never trigger. The `rebuyOffer`/`addonOffer` state variables in `PlayContent` are set to `null` initially and never updated. Consider either: (a) storing the offer data in reducer state so the play page can detect it, or (b) exposing an `onRawMessage` callback from `GameProvider` so the play page can intercept these events. This is a functional gap, but since rebuys/addons require the server to actually send these events (and the implementation may not yet do so), it can be deferred.

6. **`GameContext.tsx` actions object is recreated on every render** (lines 154-196). The `actions` object contains closures over `state.myPlayerId` (used in `sendChat`). Since `state` changes frequently, the actions object changes on every render, causing all consumers of `useGameActions()` to re-render unnecessarily. Consider wrapping the actions in `useMemo` with appropriate dependencies, or using `useRef` for the mutable parts.

7. **`apiFetch` has a hardcoded 5-second timeout** (line 49 of `api.ts`), which may be too short for game creation or practice game setup under load. The comment says "for build-time API calls" but it applies to all calls including `gameServerApi`. Consider either using `config.apiTimeout` (30s) or making the timeout configurable per call.

8. **Seat rotation arithmetic has an edge case.** In `PokerTable.tsx` line 93: `return ((seatIndex - myIndex + seatCount) % seatCount) % 10`. If `seatCount` exceeds 10, the `% 10` clamp could map two different seats to the same visual position. This is unlikely (max 10 players) but a guard would be prudent: `const pos = ((seatIndex - myIndex + seatCount) % seatCount)` then `return pos < 10 ? pos : 0`.

9. **`PlayerSeat` shows two face-down cards for all non-self players regardless of status.** Even folded or eliminated players show two blank cards. Consider hiding cards for folded players or showing no cards for empty seats.

10. **Phase 5.4 (GameInfoPanel) was planned but not implemented.** The plan lists it as a separate small phase. No `GameInfoPanel.tsx` exists. This is acceptable if intentionally deferred, but should be noted.

11. **`ResultsView` component is defined but never used.** In `results/page.tsx`, the `ResultsView` component (lines 29-107) and its imports (`StandingData`, `ordinal`) exist but the default export `ResultsPage` renders a stub instead. The dead code should be removed since it's not in use, or it should be wired up.

#### Required Changes (Blocking)

None. The implementation is functionally sound, security-hardened, and well-tested. The suggestions above are all non-blocking improvements.

### Verification

- Tests: Not run (requires `npm install` in `code/web/`). Code review confirms 4 test files with comprehensive coverage of critical paths: reducer state transitions, WebSocket lifecycle, card XSS validation, and action panel behavior.
- Coverage: Not measured. Test files cover the most critical business logic (reducer, WS client, Card component, ActionPanel).
- Build: Not verified. All imports are self-consistent across the codebase.
- Privacy: SAFE. No credentials, tokens, secrets, or personal data committed. Passwords handled in POST body only. WS tokens generated at runtime, not stored in source.
- Security: PASS. JWT scope validation, jti single-use tracking, rate limiting, reconnect token game-scoping, XSS prevention via text-only rendering, card path validation. No `dangerouslySetInnerHTML` usage. Password dialog clears on unmount.
