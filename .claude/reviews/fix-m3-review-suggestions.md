# Review Request

**Branch:** fix-m3-review-suggestions
**Worktree:** ../DDPoker-fix-m3-review-suggestions
**Plan:** (inline plan in user message — addresses M3 non-blocking review suggestions)
**Requested:** 2026-02-16 22:30

## Summary

Addresses all 10 non-blocking suggestions from the M3 WebSocket Game Protocol code review. The most critical fixes are protocol-level: 3 wrong event type mappings in `GameEventBroadcaster` (PotAwarded→PLAYER_ACTED, ShowdownStarted→HAND_COMPLETE, PlayerRebuy/Addon→PLAYER_JOINED were all wrong), plus a conflated PLAYER_LEFT/PLAYER_DISCONNECTED distinction. The remaining fixes cover safety (IOException swallowing), correctness (ALL_IN→INVALID_ACTION, admin kick TOCTOU), documentation (XFF trust comment, wildcard origin comment, stale Javadoc), and tests (+3 new tests).

## Files Changed

- [x] `websocket/message/ServerMessageType.java` — +5 enum values: POT_AWARDED, SHOWDOWN_STARTED, PLAYER_REBUY, PLAYER_ADDON, PLAYER_DISCONNECTED
- [x] `websocket/message/ServerMessageData.java` — +5 new records in sealed interface (permits list updated)
- [x] `websocket/GameEventBroadcaster.java` — Fix 3 wrong mappings; PotAwarded int[]→long[] conversion
- [x] `websocket/OutboundMessageConverter.java` — +createPlayerDisconnectedMessage factory method
- [x] `websocket/GameWebSocketHandler.java` — afterConnectionClosed: PLAYER_DISCONNECTED vs PLAYER_LEFT based on game state
- [x] `websocket/PlayerConnection.java` — Swallow IOException in sendMessage/close; JsonProcessingException remains RuntimeException
- [x] `websocket/InboundMessageRouter.java` — ALL_IN→INVALID_ACTION; fix admin kick double-iteration to single pass
- [x] `auth/LoginRateLimitFilter.java` — XFF trust model comment added
- [x] `websocket/GameWebSocketConfig.java` — Wildcard origin policy comment added
- [x] `GameInstance.java` — Remove stale "Future enhancement (M3)" Javadoc from createSimpleAI()
- [x] `websocket/GameEventBroadcasterTest.java` — +2 type-assertion tests for POT_AWARDED, SHOWDOWN_STARTED
- [x] `websocket/InboundMessageRouterTest.java` — +1 test: ALL_IN returns INVALID_ACTION

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 451/451 passed (was 449 pre-fix; +3 new tests)
- **Build:** Clean (Spotless auto-format applied)
- **Command:** `mvn test -pl pokergameserver -P dev`

## Context & Decisions

**GameEvent field names:** `GameEvent.PlayerRebuy` uses `amount()` (not `addedChips()`), `GameEvent.ShowdownStarted` has `tableId()` (not `handNumber()`), and `GameEvent.PotAwarded.winnerIds()` is `int[]` (not `long[]`). Adjusted GameEventBroadcaster accordingly: `ShowdownStartedData(0)` since no hand number is available in the event, and `int[]→long[]` conversion loop for winner IDs.

**PLAYER_LEFT docs:** The `PlayerLeftData` record doc was updated from "left or disconnected" to "left intentionally" to clarify the distinction now that `PlayerDisconnectedData` exists.

**Admin kick ordering:** The fix serializes removePlayer → broadcastToGame → removeConnection → close, which is the correct order (broadcast happens before removing the kicked player's connection so they receive the PLAYER_KICKED message).

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-16

### Findings

All 10 plan items verified as correctly implemented.

**Item-by-item verification:**

1. **ServerMessageType +5 enum values:** Confirmed POT_AWARDED, SHOWDOWN_STARTED, PLAYER_REBUY, PLAYER_ADDON, PLAYER_DISCONNECTED added with correct Javadoc. (`ServerMessageType.java:76-88`)

2. **ServerMessageData sealed interface:** All 5 new records (PlayerDisconnectedData, PotAwardedData, ShowdownStartedData, PlayerRebuyData, PlayerAddonData) added with Javadoc. Permits list correctly updated from 22 to 27 types. (`ServerMessageData.java:28,103-121`)

3. **GameEventBroadcaster event type mappings fixed:**
   - PotAwarded now maps to POT_AWARDED with PotAwardedData (was incorrectly PLAYER_ACTED). int[]->long[] conversion for winnerIds is correct. (`GameEventBroadcaster.java:87-93`)
   - ShowdownStarted now maps to SHOWDOWN_STARTED with ShowdownStartedData(0) since GameEvent.ShowdownStarted has tableId, not handNumber. (`GameEventBroadcaster.java:94-97`)
   - PlayerRebuy maps to PLAYER_REBUY, PlayerAddon maps to PLAYER_ADDON (were both incorrectly PLAYER_JOINED). Field access uses `e.amount()` which matches `GameEvent.PlayerRebuy(int tableId, int playerId, int amount)`. (`GameEventBroadcaster.java:122-129`)

4. **ALL_IN rejected:** The `ALL_IN` case removed from the switch in `parseAction()`, so it falls through to the `default` which throws `IllegalArgumentException`. The caller catches this and sends an ERROR with code "INVALID_ACTION". (`InboundMessageRouter.java:261-270`)

5. **PLAYER_DISCONNECTED vs PLAYER_LEFT:** `afterConnectionClosed` now checks game state -- if IN_PROGRESS or PAUSED, sends PLAYER_DISCONNECTED; otherwise sends PLAYER_LEFT. The `removePlayer()` implementation only marks the player as disconnected (does not change game state), so the state check after is safe. New `createPlayerDisconnectedMessage` factory added to OutboundMessageConverter. (`GameWebSocketHandler.java:208-218`, `OutboundMessageConverter.java:179-194`)

6. **IOException swallowed in PlayerConnection:** `sendMessage()` catches IOException separately from JsonProcessingException -- IOException returns silently (session closed mid-send), while JsonProcessingException remains a RuntimeException (programming error). `close()` also swallows IOException. Both have clear inline comments explaining the rationale. (`PlayerConnection.java:82-87,103-107`)

7. **Admin kick single pass:** The double-iteration loop replaced with a single pass that finds both `targetUsername` and `targetConnection` simultaneously, eliminating the TOCTOU gap. Correct ordering preserved: removePlayer -> broadcastToGame -> removeConnection -> close. (`InboundMessageRouter.java:212-233`)

8. **LoginRateLimitFilter XFF comment:** 4-line comment added explaining the trust model -- XFF trusted as-is, proxy should strip/overwrite, acceptable risk for community/embedded mode. (`LoginRateLimitFilter.java:103-106`)

9. **GameWebSocketConfig wildcard origin comment:** 2-line comment added explaining JWT auth is the security boundary, not origin-based CSRF. (`GameWebSocketConfig.java:45-46`)

10. **Stale M3 Javadoc removed:** The "Future enhancement (M3)" paragraph removed from `createSimpleAI()`. Replaced with single-line reference "ServerAIProvider integration is a future milestone." (`GameInstance.java:456-462`)

#### Strengths

- All 3 wrong event type mappings (PotAwarded, ShowdownStarted, PlayerRebuy/Addon) are correctly fixed with proper data types. This was the most impactful fix in the branch.
- The int[]->long[] conversion for PotAwarded winnerIds is done correctly with an explicit loop rather than relying on auto-boxing or streams, keeping it simple.
- The ALL_IN rejection is clean -- removing the case from the switch expression rather than adding an explicit throw keeps the code minimal.
- The admin kick single-pass fix is correct and eliminates a real TOCTOU gap where the target connection could disappear between the two loops.
- Test coverage for the new behavior is appropriate: type-assertion tests for the two most critical mapping fixes (POT_AWARDED, SHOWDOWN_STARTED) and a behavioral test for ALL_IN rejection.
- Commit message is thorough, listing all 10 items with clear descriptions of what changed.

#### Suggestions (Non-blocking)

- `ShowdownStartedData(int handNumber)` receives `0` since `GameEvent.ShowdownStarted` only has `tableId()`, not `handNumber()`. The field name in the record is slightly misleading -- it's called `handNumber` but is always 0. Consider renaming to `tableId` or adding a clarifying comment. This is minor and can be addressed in a future cleanup.

#### Required Changes (Blocking)

None.

### Verification

- Tests: 451/451 passed (`mvn test -pl pokergameserver -P dev`), +3 new tests over baseline of 449
- Coverage: Skipped per `-P dev` profile (unit tests only, no coverage aggregation)
- Build: BUILD SUCCESS, clean (Spotless auto-format applied, 0 files changed)
- Privacy: No private information (IPs, credentials, personal data) in any changed files. No config/secrets files modified.
- Security: No new vulnerabilities introduced. XFF trust model and wildcard origin policy are now documented with comments explaining the rationale.
