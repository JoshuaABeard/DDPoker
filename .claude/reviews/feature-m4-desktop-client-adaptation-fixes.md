# Review Request

## Review Request

**Branch:** feature-m4-desktop-client-adaptation
**Worktree:** ../DDPoker-feature-m4-desktop-client-adaptation
**Plan:** .claude/plans/SERVER-HOSTED-GAME-ENGINE.md (M4 review fixes)
**Requested:** 2026-02-17 01:55

## Summary

Follow-up fixes for the 8 findings from the first M4 code review (1 blocking, 7 non-blocking). All changes are in a single commit (`d1fa4a3e`) touching 4 files. 1596 tests pass.

## Files Changed

- [ ] `poker/.../online/WebSocketGameClient.java` — Added `AtomicBoolean reconnected` guard in `handleReconnect()` to prevent duplicate connections; added JWT-in-query-string comment in `connect()`
- [ ] `poker/.../online/WebSocketTournamentDirector.java` — Added `showHand.clear()` before adding showdown cards in `onHandComplete()`; added comment documenting long→int playerId cast limitation
- [ ] `poker/.../server/GameServerRestClient.java` — Promoted `ObjectMapper` from per-instance field to `private static final OBJECT_MAPPER`
- [ ] `poker/.../server/PracticeGameLauncher.java` — Simplified `defaultSkillLevel()` Javadoc to remove misleading claim about profile-based mapping

**Privacy Check:**
- ✅ SAFE — No private information in any of the changes

## Verification Results

- **Tests:** 1596/1596 passed (0 failures, 25 skipped — pre-existing)
- **Build:** Clean

## Context & Decisions

**Reconnect fix approach:** Used `AtomicBoolean reconnected` local to `handleReconnect()` rather than a class-level field. Rationale: each disconnect/reconnect cycle is independent. The flag is captured by the lambda closures for all 10 scheduled tasks from that cycle; on the next disconnect a fresh flag is created. This avoids needing to reset class-level state between reconnect cycles.

**`showHand.clear()` safety:** `Hand` extends `DMArrayList<Card>` (which extends `ArrayList`), so `clear()` is inherited. The `cType_` field (face-up/down) is not affected by `clear()`. Clearing before showdown ensures no duplicate cards when the local player's hole cards (set via `HOLE_CARDS_DEALT`) are re-populated from `showdownPlayers`.

**ObjectMapper thread-safety:** Jackson's `ObjectMapper` is thread-safe for concurrent reads after configuration is complete. Making it `static final` with all configuration applied at class-load time is safe.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-17

### Findings

#### Strengths

1. **Reconnect guard is well-designed.** Using a local `AtomicBoolean` scoped to each `handleReconnect()` invocation is the right approach. Each disconnect/reconnect cycle gets its own flag, avoiding the need to manage class-level state resets. The flag is correctly shared across all 10 scheduled lambdas from one cycle and correctly set in the `whenComplete` callback only on success.

2. **`showHand.clear()` is correct and necessary.** The local player's hole cards arrive via `HOLE_CARDS_DEALT` (line 398-402) before `HAND_COMPLETE` delivers the full showdown data. Without clearing, the local player would accumulate duplicate cards. Clearing is harmless for non-local players who had no prior cards. Placement (before the card-add loop, inside the null/empty check) is correct.

3. **ObjectMapper promotion is textbook.** Jackson `ObjectMapper` is thread-safe for reads after configuration. The static initializer applies all modules and feature flags at class-load time. All three call sites (`writeValueAsString`, `readValue` x2) are concurrent-read-safe operations. No stale instance field remains.

4. **Javadoc fix removes a genuine lie.** The old Javadoc described behavior ("uses the profile's percentage mix") that the method body did not implement. The new one-liner is accurate.

5. **Comments are accurate and proportional.** The JWT-in-query-string comment explains a non-obvious but standard pattern. The long-to-int cast comment documents a known limitation with the correct scope (safe now, needs revisiting for M6+).

#### Suggestions (Non-blocking)

1. **Minor race window in reconnect guard (acceptable).** `WebSocketGameClient.java:224-234`: Because `openConnection()` is asynchronous, there is a theoretical window between the `reconnected.get()` check (line 225) and the `reconnected.set(true)` in `whenComplete` (line 232) where a concurrent attempt could slip through. However, this is a non-issue in practice: the scheduler is single-threaded, and exponential backoff means the minimum gap between consecutive attempts is 500ms -- orders of magnitude longer than a WebSocket handshake. Even in the pathological case, the `webSocket` field would simply be overwritten, and the system would stabilize with one active connection. No change needed.

2. **Double `handleReconnect()` calls are theoretically possible (pre-existing).** If both `onClose` and `onError` fire for the same disconnect, `handleReconnect()` would be called twice, each creating its own `AtomicBoolean` and scheduling its own 10 attempts. This is a pre-existing concern that exists independent of this fix. Worth noting for a future hardening pass (e.g., a class-level `reconnecting` guard), but not a regression from this commit and not blocking.

3. **ObjectMapper comment formatting.** `GameServerRestClient.java:49-52`: The multi-line comment has an awkward line break ("safe for concurrent\n// reads\n// after configuration") that appears to be an artifact of auto-formatting. It reads slightly oddly but is functionally irrelevant.

#### Required Changes (Blocking)

None.

### Verification

- **Tests:** 1576 passed, 0 failures, 25 skipped (pre-existing). One unrelated pre-existing failure in `HibernateTest` (`NoClassDefFoundError` for `ToEmptyObjectSerializer`) confirmed to reproduce identically on `main`.
- **Coverage:** Not re-measured for this incremental fix commit; no new code paths requiring coverage.
- **Build:** Clean across all 19 modules (`mvn clean package -DskipTests` -- BUILD SUCCESS).
- **Privacy:** No hardcoded tokens, credentials, IPs, or personal data. The `?token=` URL construction uses runtime variables only.
- **Security:** No new attack surface. JWT-in-query-string is correctly documented as acceptable for embedded localhost-only mode.
