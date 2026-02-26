# Thin Client Audit Fixes

**Status:** COMPLETED (2026-02-26)
**Goal:** Fix 4 bugs discovered during systematic audit of the thin client WebSocket system.

---

## Bug 1: `canAllIn` ignored in `_getAdvanceActionWS`

**Severity:** Medium (gameplay impact — wrong action sent to server)

**Root cause:** `getAdvanceActionWS` 7-param overload accepts `canAllIn` and `allInAmount` but never forwards them to `_getAdvanceActionWS`. When the all-in advance button is pre-selected and `canAllIn=false` (short-stack scenario), sends `"ALL_IN"` when the server expects `"CALL"`.

**Fix:** Forward `canAllIn` to `_getAdvanceActionWS` and guard the `allin_` branch. When `canAllIn=false`, return `{"CALL", "0"}` instead of `{"ALL_IN", "0"}`.

**Files:** `AdvanceAction.java` — `_getAdvanceActionWS` signature + `allin_` branch

**Also:** Remove dead `"BLIND_BET"` entry from the opponent tracking filter in `onPlayerActed` (server `ActionType` enum has no `BLIND_BET`; server sends `BLIND_SM`/`BLIND_BIG` only).

## Bug 2: `isPreFlop_` race condition in `onPlayerActed`

**Severity:** Low (opponent tracking stats miscategorization)

**Root cause:** `isPreFlop_` is written on the WS listener thread (`onHandStarted`, `onCommunityCardsDealt`) but read inside EDT lambdas (`onPlayerActed`). When the EDT is backlogged, `onCommunityCardsDealt` can set `isPreFlop_=false` before the queued `onPlayerActed` lambda executes, miscategorizing pre-flop actions as post-flop.

**Fix:** Capture `isPreFlop_` into a `final` local variable before `invokeLater` in `onPlayerActed`.

**Files:** `WebSocketTournamentDirector.java` — `onPlayerActed` method

## Bug 3: Community cards not cleared in `onHandStarted`

**Severity:** Low (stale community cards visible after reconnect)

**Root cause:** `onHandStarted` reuses the previous `RemoteHoldemHand` but only calls `clearWins()`. Community cards from the previous hand persist. In the reconnect-path broadcaster (no per-player GAME_STATE before HAND_STARTED), old community cards remain visible at the start of the new hand.

**Fix:** Add `hand.updateCommunity(new Hand())` after `clearWins()`.

**Files:** `WebSocketTournamentDirector.java` — `onHandStarted` method

## Bug 4: Sitting-out players get blank hole cards

**Severity:** Low (incorrect face-down card images for non-participating players)

**Root cause:** `applyTableData` adds blank cards for all non-local, non-`"FOLDED"` players. The server sends `"SITTING_OUT"` as the status for sitting-out players (confirmed in `OutboundMessageConverter.java:297`), so they pass the condition and get face-down card images despite not being in the hand.

**Fix:** Add `&& !"SITTING_OUT".equals(sd.status())` to the blank-card condition.

**Files:** `WebSocketTournamentDirector.java` — `applyTableData` method
