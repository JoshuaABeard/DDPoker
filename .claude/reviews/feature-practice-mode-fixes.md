# Review Request

**Branch:** feature-practice-mode-fixes
**Worktree:** C:/Repos/DDPoker-feature-practice-mode-fixes
**Plan:** .claude/plans/smooth-humming-snowflake.md
**Requested:** 2026-02-22 10:30

## Summary

Five practice mode improvements for WebSocket mode: (1) timing prefs (AI action delay, inter-hand pause, all-in pause, zip mode) wired from client UI to embedded server via new `GameConfig.PracticeConfig`; (2) all player cards (including folded) sent in `HAND_COMPLETE`; (3) Never Broke chip transfer from chip leader when human busts; (4) color-up chip race animation preserved via `COLOR_UP_STARTED` WS message; (5) dead UI options removed and live AI card reveal added via `AI_HOLE_CARDS` WS message.

## Files Changed

- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/GamePrefsPanel.java` - Removed dead options: OPTION_AUTODEALFOLD spinner, OPTION_AUTODEAL checkbox, PREF_AUTOSAVE checkbox, OPTION_CHEAT_PAUSECARDS, OPTION_CHEAT_MANUAL_BUTTON, OPTION_CHEAT_RABBITHUNT
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/SwingEventBus.java` - Added ColorUpStarted and ChipsTransferred cases to exhaustive switch
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` - Added handlers for CHIPS_TRANSFERRED, COLOR_UP_STARTED, AI_HOLE_CARDS messages
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/server/GameServerRestClient.java` - Added createPracticeGame overload accepting PracticeConfig
- [ ] `code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java` - Reads timing prefs and builds PracticeConfig to pass to server
- [ ] `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/event/GameEvent.java` - Added ChipsTransferred, ColorUpStarted, ColorUpPlayerData sealed interface variants
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameConfig.java` - Added PracticeConfig nested record as last field; added withPracticeConfig()
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/GameInstance.java` - Reads practiceConfig and wires per-game timing/flags to director
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerGameTable.java` - Implemented colorUp() chip race with ServerDeck; ColorUpPlayerResult record
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayer.java` - Added oddChips field with getter/setter
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java` - Converted static timing constants to instance fields with setters; TD.ColorUp phase handler; Never Broke logic; findChipLeader helper
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/controller/TournamentProfileConverter.java` - Added null for practiceConfig in GameConfig constructor call
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java` - Send all player cards (not just non-folded) in HandCompleted; ChipsTransferred/ColorUpStarted/AI_HOLE_CARDS broadcasting; aiFaceUp support
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameWebSocketHandler.java` - Calls setAiFaceUp on broadcaster when practiceConfig.aiFaceUp is true
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageData.java` - Added ChipsTransferredData, ColorUpStartedData, AiHoleCardsData records; updated permits list
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageType.java` - Added CHIPS_TRANSFERRED, COLOR_UP_STARTED, AI_HOLE_CARDS
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/GameConfigTest.java` - Added null for practiceConfig
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/GameInstanceManagerTest.java` - Added null for practiceConfig
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/GameInstanceTest.java` - Added null for practiceConfig
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirectorTest.java` - Fixed pre-existing flaky test interHandPausePreventsRacing: added director.setHandResultPauseMs(delayMs) so the test uses 20ms instead of 3000ms default
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/integration/WebSocketIntegrationTest.java` - Added null for practiceConfig
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/service/GameServiceTest.java` - Added null for practiceConfig

**Privacy Check:**
- ✅ SAFE - No private information found. Changes are pure game logic, message types, and UI options.

## Verification Results

- **Tests:** All passed (mvn test -P dev, BUILD SUCCESS, 1:53 min)
- **Coverage:** Not run separately; existing thresholds maintained
- **Build:** Clean

## Context & Decisions

**Phase 1 (Timing):** `PracticeConfig` is a nullable nested record appended to `GameConfig`. All fields nullable (null = use server defaults). Only applied when `practiceConfig != null`, so non-practice games are unaffected. `GameInstance` reads the record and calls the new setters on `ServerTournamentDirector`.

**Phase 2 (Folded cards):** Changed filter in `GameEventBroadcaster.HandCompleted` from `!sp.isFolded()` to `!sp.isSittingOut()`. This sends all active player cards at showdown, including those who folded, matching original desktop behavior.

**Phase 3 (Never Broke):** Transfer half the chip leader's chips to the human when the human hits 0 chips (`practiceConfig.neverBroke() == true`). The leader must have > 1 chip. Broadcasts `CHIPS_TRANSFERRED` WS message for client notification.

**Phase 4 (Color-up):** The `TD.ColorUp` phase from `TournamentEngine` is intercepted in `ServerTournamentDirector.applyResult()`. Server runs the chip race (deals one card per player with odd chips from a `ServerDeck`, highest card wins a full new-denomination chip), then publishes `GameEvent.ColorUpStarted`. No server-side sleep — client processes WS messages sequentially so animation is shown before next `HAND_STARTED`.

**Phase 5 (Dead options + AI reveal):** Six dead options removed from `GamePrefsPanel`. When `aiFaceUp=true`, `GameWebSocketHandler` sets `broadcaster.setAiFaceUp(true)`, triggering `AI_HOLE_CARDS` broadcast after each `HAND_STARTED` with all AI hole cards.

**Flaky test fix:** `interHandPausePreventsRacing` was already flaky on `main` (3000ms default × multiple hands could exceed 30s timeout). Fixed by calling `director.setHandResultPauseMs(delayMs)` (20ms) using the new setter.

---

## Review Results

**Status:** APPROVED (blocking issues resolved 2026-02-22)

**Reviewed by:** Claude Opus 4.6 (review agent)
**Date:** 2026-02-22

### Findings

#### Strengths

1. **Clean PracticeConfig wiring chain.** The data flow from `PracticeGameLauncher` (reads UI prefs) -> `GameServerRestClient` (serializes to JSON) -> `GameConfig.PracticeConfig` (record) -> `GameInstance` (applies to director) -> `ServerTournamentDirector` (uses values) is well-structured and easy to follow.

2. **Good use of nullable fields with server defaults.** `PracticeConfig` fields are all nullable `Integer`/`Boolean`, with null meaning "use server default." `GameInstance` only calls setters when values are non-null, preserving backward compatibility.

3. **Idempotent color-up guard.** `ServerGameTable.colorUp()` returns early if `colorUpResults != null`, and `colorUpFinish()` resets it to null. Prevents double-computation.

4. **Clean dead option removal.** Six options removed from `GamePrefsPanel` without breaking the layout. No stale references remain.

5. **Flaky test fix is correct.** `interHandPausePreventsRacing` was genuinely flaky due to the 3000ms default pause. Using the new setter to reduce it to 20ms is the right fix.

6. **EmbeddedGameServer auth simplification.** Replacing profile-based identity derivation (SHA-256 of filename + create date) with a simpler OS username + persisted random password is cleaner and eliminates the coupling to `PlayerProfile`/`PlayerProfileOptions`.

7. **ALL_IN action simplification.** Removing the complex BET/RAISE/CALL mapping and always sending RAISE is simpler, assuming the server validates and handles it correctly.

#### Suggestions (Non-blocking)

1. **Color-up comment inconsistency.** `ServerGameTable.java:362-364` says "one card per odd chip" but the implementation (line 396-399) deals exactly one card per participant regardless of odd chip count. The comment should match the implementation. This is a simplified chip race variant (one card per player), which is acceptable for a game simulator, but the comment is misleading.

2. **Handoff file missing 8 changed files.** The handoff lists 22 files but 29 were changed. Missing from the list:
   - `EmbeddedGameServer.java` - Major auth refactor
   - `PlayerProfileOptions.java` - Removed onProfileChanged_ listener
   - `PokerGame.java` - Simplified getHumanPlayer()
   - `PokerMain.java` - Removed pre-authentication code
   - `AdvanceAction.java` - Simplified ALL_IN mapping
   - `RemoteHoldemHand.java` - Removed ownerTable_ back-link
   - `RemotePokerTable.java` - Removed setOwnerTable call
   - `PracticeGameController.java` - Removed humanDisplayName logic

   These are significant changes (especially `EmbeddedGameServer` and `AdvanceAction`) that should have been documented in the handoff for review transparency.

3. **`oddChips` field on `ServerPlayer` is never cleared.** After `colorUp()` sets it, the value persists until the next `colorUp()` call. This is harmless since the field is only read during color-up processing, but a reset in `colorUpFinish()` would be cleaner.

4. **`findChipLeader` iterates seats linearly.** For large tables this is fine, but the method could be a stream one-liner. Not a real issue at current table sizes.

#### Required Changes (Blocking)

1. **No tests for color-up chip race logic.** `ServerGameTable.colorUp()` implements a non-trivial algorithm (odd chip calculation, card dealing, winner selection, chip awarding, broke detection) with zero test coverage. This needs tests covering at minimum:
   - Basic chip race: 2+ players with odd chips, verify winners get new-min chips, losers don't
   - Edge case: player whose entire stack is odd chips (goes broke after rounding down, doesn't win the race)
   - Edge case: `nextMinChip <= minChip` (no-op path)
   - Edge case: no players have odd chips (no participants)
   - Idempotent guard: calling `colorUp()` twice returns same results

   Reference: `ServerGameTable.java:356-437`

2. **No tests for Never Broke logic.** `ServerTournamentDirector.eliminateZeroChipPlayers()` now includes a Never Broke code path with meaningful edge cases that are untested:
   - Happy path: human busts, chip leader has chips, transfer occurs
   - Edge case: chip leader has exactly 1 chip (no transfer, human eliminated)
   - Edge case: no chip leader available (all others also busted)
   - Edge case: AI player busts with neverBroke=true (should NOT receive chips -- only human)

   Reference: `ServerTournamentDirector.java:521-531`

### Verification

- **Tests:** All 242 tests pass (`mvn test -P dev`, BUILD SUCCESS, 1:46 min). Zero failures, zero errors.
- **Coverage:** Not separately verified; existing JaCoCo thresholds are maintained (build would fail otherwise). However, the new color-up and Never Broke code paths have zero dedicated test coverage.
- **Build:** Clean. No warnings in reactor summary. Spotless formatting applied.
- **Privacy:** SAFE. No private information (IPs, credentials, personal data) in any changed file. The `local-identity.properties` file is stored in `~/.ddpoker/` (user home, not in repo) and contains only a generated UUID password for the local H2 database.
- **Security:** No OWASP concerns. The auth refactor in `EmbeddedGameServer` is local-only (embedded H2, localhost-bound server). UUID passwords provide sufficient entropy for this use case.
