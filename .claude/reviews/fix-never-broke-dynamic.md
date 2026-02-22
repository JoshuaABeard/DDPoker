# Review Request

## Review Request

**Branch:** fix-never-broke-dynamic
**Worktree:** ../DDPoker-fix-never-broke-dynamic
**Plan:** .claude/plans/rippling-crunching-tarjan.md
**Requested:** 2026-02-22 14:35

## Summary

Replaces the static `PracticeConfig.neverBroke` startup check with an offer-based server→client→server round-trip (mirroring the rebuy pattern). When the human busts, the server publishes `NeverBrokeOffered`, the client reads `PokerUtils.isOptionOn(OPTION_CHEAT_NEVERBROKE)` at that moment and responds with `NEVER_BROKE_DECISION`, so the preference is read dynamically rather than locked in at game creation.

Also fixes a heads-up bug where `!tournament.isGameOver()` was `false` when only one player had chips (game-over condition), causing neverBroke to be skipped even in normal play. Fixed by replacing with an `inHandleGameOverContext` parameter that only suppresses neverBroke during `handleGameOver` cleanup.

## Files Changed

**Core event:**
- [x] `code/pokergamecore/src/main/java/.../core/event/GameEvent.java` — Added `NeverBrokeOffered(tableId, playerId, timeoutSeconds)` record

**Server — messages:**
- [x] `code/pokergameserver/src/main/java/.../websocket/message/ServerMessageType.java` — Added `NEVER_BROKE_OFFERED`
- [x] `code/pokergameserver/src/main/java/.../websocket/message/ServerMessageData.java` — Added `NeverBrokeOfferedData`
- [x] `code/pokergameserver/src/main/java/.../websocket/message/ClientMessageType.java` — Added `NEVER_BROKE_DECISION`
- [x] `code/pokergameserver/src/main/java/.../websocket/message/ClientMessageData.java` — Added `NeverBrokeDecisionData`

**Server — logic:**
- [x] `code/pokergameserver/src/main/java/.../GameConfig.java` — Removed `neverBroke` from `PracticeConfig` record (now dynamic)
- [x] `code/pokergameserver/src/main/java/.../ServerTournamentDirector.java` — Replaced `practiceConfig.neverBroke()` with `neverBrokeCallback: BiPredicate<Integer,Integer>`; added `inHandleGameOverContext` param to `eliminateZeroChipPlayers`
- [x] `code/pokergameserver/src/main/java/.../GameInstance.java` — Added `offerNeverBroke`/`submitNeverBrokeDecision` with `pendingNeverBroke` future map; wired callback to director
- [x] `code/pokergameserver/src/main/java/.../websocket/GameEventBroadcaster.java` — Handle `NeverBrokeOffered` → send to player
- [x] `code/pokergameserver/src/main/java/.../websocket/InboundMessageRouter.java` — Handle `NEVER_BROKE_DECISION` → call `game.submitNeverBrokeDecision`
- [x] `code/pokergameserver/src/main/java/.../controller/TournamentProfileConverter.java` — Remove `neverBroke` mapping (spotless reformat only)

**Client:**
- [x] `code/poker/src/main/java/.../online/WebSocketGameClient.java` — Added `sendNeverBrokeDecision(boolean accept)`
- [x] `code/poker/src/main/java/.../online/WebSocketTournamentDirector.java` — Handle `NEVER_BROKE_OFFERED`, auto-respond based on live option
- [x] `code/poker/src/main/java/.../online/SwingEventBus.java` — Added `NeverBrokeOffered` cases
- [x] `code/poker/src/main/java/.../server/PracticeGameLauncher.java` — Removed `neverBroke` from `PracticeConfig` construction
- [x] `code/poker/src/main/java/.../server/GameServerRestClient.java` — Spotless reformat only

**Tests:**
- [x] `code/pokergameserver/src/test/.../ServerTournamentDirectorTest.java` — Updated 3 existing tests to use `setNeverBrokeCallback`; added 3 new tests: `neverBrokeSkippedWhenCallbackReturnsFalse`, `neverBrokeSkippedInHandleGameOverContext`, `neverBrokeFiresInHeadsUp`
- [x] `code/pokergameserver/src/test/.../GameInstanceTest.java` — Added 2 new tests: `offerNeverBrokePublishesEventAndBlocksUntilDecision`, `offerNeverBrokeReturnsFalseOnTimeout`
- [x] `code/pokergameserver/src/test/.../GameConfigTest.java` — Spotless reformat only
- [x] `code/pokergameserver/src/test/.../GameInstanceManagerTest.java` — Spotless reformat only
- [x] `code/pokergameserver/src/test/.../integration/WebSocketIntegrationTest.java` — Spotless reformat only

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 1530/1530 passed (pokergamecore + pokergameserver + poker modules)
- **New tests added:** 5 (3 in ServerTournamentDirectorTest, 2 in GameInstanceTest)
- **Build:** Clean

## Context & Decisions

- **Offer pattern** mirrors the existing rebuy round-trip exactly: server publishes event, blocks on `CompletableFuture`, client auto-responds without a dialog.
- **`inHandleGameOverContext`**: The old code guarded with `!tournament.isGameOver()`. In heads-up, when the human busts, `isGameOver()` becomes true (only one non-zero-chip player remains), so neverBroke was skipped even in the normal CLEAN path. The new `inHandleGameOverContext` boolean is `false` for normal hand processing and `true` only inside `handleGameOver` cleanup.
- **No dialog needed on client**: neverBroke auto-accepts/declines based on current preference, consistent with the "cheat option reads live at bust-time" requirement.
- **`neverBroke` default still `false`** in `client.properties` — this is intentional, it's a cheat. Users opt in explicitly.

---

## Review Results

**Status:** NOTES

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

1. **Correct offer pattern**: The `pendingNeverBroke.put(playerId, future)` happens BEFORE `eventBus.publish(...)`, which is correct. Since `GameEventBus.publish()` is synchronous and the client response arrives on a separate WebSocket inbound thread, there is no race condition — the future is always in the map before any response can arrive.

2. **Accurate heads-up fix**: The `inHandleGameOverContext` parameter correctly replaces `!tournament.isGameOver()`. In heads-up, when the human busts, `isGameOver()` becomes `true` immediately (only the AI has chips), causing the old guard to skip neverBroke even though we are not in the `handleGameOver` cleanup path. The new explicit boolean resolves this cleanly and precisely.

3. **Thread safety is acceptable**: `offerNeverBroke` blocks the director game thread — the same approach used by `offerRebuy` and `offerAddon`. This is intentional, consistent, and tolerable for a single-human practice game where the client responds automatically (usually sub-100ms). Blocking is not a correctness concern.

4. **Race between `submitNeverBrokeDecision` and `offerNeverBroke`**: The question raised in the review request is: what if `submitNeverBrokeDecision` is called before `offerNeverBroke` puts the future? This cannot happen in practice because: the future is in the map before `publish()` fires, and client responses arrive on a separate thread only after the WS message is sent. The `null` check in `submitNeverBrokeDecision` handles any late/spurious messages safely.

5. **Mirrors rebuy precisely**: The new code is structurally identical to `offerRebuy`/`submitRebuyDecision`. Same map type, same timeout, same `finally`-remove, same `InterruptedException` handling (which silently returns `false`, consistent with the existing pattern). No new patterns introduced.

6. **Clean removal of static field**: `PracticeConfig.neverBroke` is completely removed — no stale references in server code. `CheckEndHand.java` (offline practice mode) correctly remains untouched since it uses a separate code path.

7. **Five tests are adequate**: The 3 director-level tests (`callbackReturnsFalse`, `inHandleGameOverContext`, `neverBrokeFiresInHeadsUp`) cover the three key guard conditions for neverBroke. The 2 `GameInstanceTest` tests verify the async offer/timeout mechanics. Coverage of the critical code paths is solid.

8. **Surgical precision**: The change touches exactly what is needed. `CheckEndHand.java` (offline path), `GameConfigTest`, `GameInstanceManagerTest`, `WebSocketIntegrationTest`, `GameServerRestClient.java` — all are spotless-only reformats with zero logic changes.

#### ⚠️ Suggestions (Non-blocking)

1. **`NeverBrokeOfferedData.playerId` is unused on the client** (`GameInstance.java:671`, `ServerMessageData.java:271`): The `playerId` field is included in the server-sent `NeverBrokeOfferedData` but the client handler (`WebSocketTournamentDirector.java:1222-1227`) never reads it. The message is already sent point-to-point to the player, so `playerId` is redundant. Compare: `RebuyOfferedData` and `AddonOfferedData` do not include `playerId` for the same reason. This is harmless but inconsistent. Consider removing the field to match the `RebuyOfferedData`/`AddonOfferedData` pattern, or adding a comment explaining why it is included if there's a future use case in mind (e.g., future web client that needs to know which player the offer is for).

2. **Callback always wired for all practice games** (`GameInstance.java:269`): `setNeverBrokeCallback(this::offerNeverBroke)` is called for every practice game regardless of whether the player has ever enabled the neverBroke option. This means every practice bust event now incurs a server→client→server WebSocket round-trip (typically <100ms), even when the option is permanently off. In normal play this is imperceptible, but it is a behavioral change from the old "skip if not configured" approach. This is intentional by design (reads preference at bust-time), but worth documenting as a known trade-off.

3. **`neverBrokeCallback` is a mutable setter while `rebuyOfferCallback`/`addonOfferCallback` are final constructor parameters** (`ServerTournamentDirector.java:89-93` vs. `102-103`): This asymmetry is defensible (neverBroke is always wired for practice games, not conditionally on config like rebuy/addon), but it is inconsistent. A comment noting the rationale would help future maintainers.

#### ❌ Required Changes (Blocking)

None.

### Verification

- **Tests:** 1481/1481 passed across `pokergamecore` + `pokergameserver` + `poker` modules (confirmed by reviewer). The `pokerserver` failures (123 errors) are pre-existing Spring context failures unrelated to this change — confirmed identical on `main` branch.
- **Coverage:** Not explicitly measured, but the 5 new tests directly cover: the event-publish-and-block path, the timeout path, callback-returns-false elimination, in-game-over-context suppression, and heads-up rescue. All meaningful branches in `offerNeverBroke` and `eliminateZeroChipPlayers`'s neverBroke block are covered.
- **Build:** Clean (spotless, javadoc, no warnings in affected modules).
- **Privacy:** No private information. The `NeverBrokeOfferedData` contains only `playerId` (an integer) and `timeoutSeconds`. No credentials, IPs, or personal data.
- **Security:** No security concerns. The `NEVER_BROKE_DECISION` message is routed via the same authenticated WebSocket connection as all other player messages. The `submitNeverBrokeDecision` call uses `connection.getProfileId()` (authenticated profile), matching the rebuy/addon pattern exactly.
