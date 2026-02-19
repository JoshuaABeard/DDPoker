# Review Request

**Branch:** feature-m7-legacy-p2p-removal
**Worktree:** C:\Repos\DDPoker-feature-m7-legacy-p2p-removal
**Plan:** .claude/plans/M7-LEGACY-P2P-REMOVAL.md
**Requested:** 2026-02-18

## Summary

Phase 7.3 of M7 Legacy P2P Removal: delete 11 P2P utility/chat classes
(BetValidator, GameOverChecker, PokerLogicUtils, ChatLobbyManager,
ChatLobbyPanel, OnlineConfiguration, SyncPasswordDialog, ChangeTableDialog,
and their tests), inlining all needed logic into callers before deletion.
Includes expanding the PokerDirector interface and making TournamentDirector
implement it, enabling callers to cast to PokerDirector instead of the
concrete TournamentDirector.

Two commits since Phase 7.2:
- `e20907af` — Phase 7.3 deletions + inlining (main change)
- `09f070e6` — Build fixes: 5 compilation errors from deleted classes still
  referenced in PokerMain, OnlineLobby, Lobby, GameInfoDialog, PokerDirector

## Files Changed

### New/Modified Source
- [x] `online/PokerDirector.java` — Added `doHandAction()` and `setGameOver()` to interface; fixed HandAction import (`engine` → `poker` package)
- [x] `online/WebSocketTournamentDirector.java` — Implemented `doHandAction()` and `setGameOver()`
- [x] `online/TournamentDirector.java` — Added `implements PokerDirector` (already had both required methods)
- [x] `Bet.java` — Inlined BetValidator logic; replaced TournamentDirector cast with PokerDirector; replaced `TournamentDirector.AI_PAUSE_TENTHS` literal
- [x] `CheckEndHand.java` — Inlined GameOverChecker (enum + 4 static methods); changed TD field/cast to PokerDirector; removed dead cleanTables/removeFromWaitList calls
- [x] `PokerUtils.java` — Inlined pow(), roundAmountMinChip(), nChooseK(); removed TDPAUSER(); removed P2P-only sendDealerChatLocal block
- [x] `online/SwingPlayerActionProvider.java` — Inlined BetValidator.determineInputMode(); removed logic.* import
- [x] `dashboard/MyTable.java` — Removed P2P ChangeTableDialog button

### Build Fix Changes (commit 09f070e6)
- [x] `online/PokerDirector.java` — Fixed `HandAction` import (was `engine.HandAction`, is `poker.HandAction`)
- [x] `PokerMain.java` — Removed `getChatServer()`, `shutdownChatServer()`, `TcpChatClientAdapter`; simplified `setChatLobbyHandler()`
- [x] `online/OnlineLobby.java` — Removed `ChatLobbyPanel chat_` field and all usages
- [x] `online/Lobby.java` — Removed `createURLPanel()` method (used deleted OnlineConfiguration)
- [x] `GameInfoDialog.java` — Removed `OnlineTab` inner class and its tab registration

### Files Deleted (11)
- [x] `SyncPasswordDialog.java`
- [x] `logic/BetValidator.java` + `BetValidatorTest.java`
- [x] `logic/GameOverChecker.java` + `GameOverCheckerTest.java`
- [x] `logic/PokerLogicUtils.java` + `PokerLogicUtilsTest.java`
- [x] `online/ChangeTableDialog.java`
- [x] `online/ChatLobbyManager.java`
- [x] `online/ChatLobbyPanel.java`
- [x] `online/OnlineConfiguration.java`

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** BUILD FAILURE — 1 pre-existing test failure in `pokerserver` module: `HibernateTest.testHibernate` throws `NoClassDefFoundError` for `com/fasterxml/jackson/databind/ser/std/ToEmptyObjectSerializer`. This is a Jackson classpath/version conflict unrelated to Phase 7.3 changes (our changes don't touch pokerserver module or Jackson dependencies).
- **Compilation:** All modules compile cleanly — zero compilation errors.
- **Build:** Clean except for the pre-existing HibernateTest failure.
- **Coverage:** Not measured (build failure short-circuits coverage).

## Context & Decisions

### TournamentDirector Deletion Deferred
Phase 7.5 defers TournamentDirector deletion because it has ~25+ callers in
non-deleted files. Strategy: make TD `implements PokerDirector` (it already
had both required methods), update callers to cast to PokerDirector instead
of TournamentDirector. This keeps Phase 7.3 focused and compilable.

### Deferred P2P Files Not Yet Deleted
OnlineLobby, Lobby, GameInfoDialog, PokerMain, and ~20 other files are
scheduled for Phase 7.3.1/7.5 deletion when TournamentDirector goes. In
the build-fix commit, these files were patched to remove references to
deleted classes (ChatLobbyPanel, ChatLobbyManager, OnlineConfiguration)
rather than deleted outright — their callers still need them.

### nChooseK Inlining
Original `PokerLogicUtils.nChooseK()` used a pre-computed `BigInteger[]`
FACTORIAL array. Inlined version uses an iterative algorithm that avoids
BigInteger entirely (values fit in long). Functionally equivalent.

### Pre-existing HibernateTest failure
`HibernateTest.testHibernate` was failing before Phase 7.3. This is a
Jackson runtime classpath conflict in the `pokerserver` module — our changes
don't touch that module. It should be investigated separately.

---

## Review Results

**Status:** APPROVED

**Reviewed by:** Claude Opus 4.6
**Date:** 2026-02-18

### Findings

#### Strengths

1. **Inlined logic is correct vs. originals.** All three deleted utility classes (BetValidator, GameOverChecker, PokerLogicUtils) had their logic faithfully reproduced at each call site:
   - `BetValidator.determineInputMode()` inlined identically in `Bet.java:174-180` and `SwingPlayerActionProvider.java:222-228` -- same if/else structure, same PokerTableInput mode constants.
   - `BetValidator.validateBetAmount()` inlined in `Bet.java:402-410` -- the rounding logic (modulo, halfway comparison with `(float) nOdd >= (nMinChip / 2.0f)`) matches the original exactly. The simplified "if nNewAmount != nAmount" check replaces the BetValidationResult wrapper cleanly.
   - `GameOverChecker` enum + all 4 static methods inlined into `CheckEndHand.java:67-105` -- enum values, method signatures, and logic bodies are identical to the originals.
   - `PokerLogicUtils.pow()` and `roundAmountMinChip()` inlined identically into `PokerUtils.java`.
   - `PokerLogicUtils.nChooseK()` replaced with an iterative algorithm instead of the BigInteger factorial approach. The iterative version (`result * (n-i) / (i+1)` with `k = min(k, n-k)` optimization) is a well-known correct algorithm for binomial coefficients and is functionally equivalent for poker-range inputs (n <= 52).

2. **PokerDirector interface additions are correct.** `doHandAction(HandAction, boolean)` and `setGameOver()` added to the interface. Both `TournamentDirector` (line 1593 and 1802) and `WebSocketTournamentDirector` (line 764 and 770) have matching implementations. The `HandAction` import was correctly fixed from `engine.HandAction` to `poker.HandAction` in the build-fix commit.

3. **TournamentDirector `implements PokerDirector` is safe.** TD already implemented `GameManager` and `ChatManager` (both supertypes of `PokerDirector`) and already had `setPaused()`, `playerUpdate()`, `doHandAction()`, and `setGameOver()` methods with matching signatures. Adding `implements PokerDirector` requires no code changes to TD itself.

4. **Removed P2P-only code paths are justified:**
   - `cleanTables()` and `removeFromWaitList()` removed from CheckEndHand -- these are TournamentDirector-only methods not on the PokerDirector interface; server handles table management in the WebSocket architecture.
   - `td.isClient()` guard removed from chip verification -- no P2P client concept in new architecture.
   - `sendDealerChatLocal` block removed from PokerUtils -- server handles dealer chat in WebSocket mode; the remaining `displayInformationDialog` path covers both online and offline.
   - `TcpChatClientAdapter` inner class and `getChatServer()`/`shutdownChatServer()` removed from PokerMain -- replaced by `LobbyChatWebSocketClient` in Phase 7.2.
   - `ChatLobbyPanel` field and all usages removed from OnlineLobby -- replaced by WebSocket lobby chat.

5. **Build-fix changes are minimal and correct.** The second commit (`09f070e6`) only touches files that had compilation errors due to references to deleted classes. Each fix is a clean removal of dead code: `OnlineTab` inner class in GameInfoDialog, `createURLPanel()` in Lobby, chat-related fields/methods in OnlineLobby and PokerMain. No accidental functionality removal.

6. **Clean separation of concerns.** Phase 7.3 correctly limits itself to utility/chat class deletion and doesn't attempt the larger TournamentDirector migration (Phase 7.5).

#### Suggestions (Non-blocking)

1. **Orphaned `TournamentDirectorPauser.java`.** The `TDPAUSER()` factory method in PokerUtils was removed, which was the only caller of `TournamentDirectorPauser`. The file itself was not deleted, leaving it as an orphan. Per the plan, it is listed for deletion in Phase 7.3.1 (`TournamentDirectorPauser.java ~80 lines`). Consider deleting it in this phase since its only entry point is now gone, or confirm it will be cleaned up in Phase 7.5. Not blocking since the file compiles fine on its own and is already dead code.

2. **`AI_PAUSE_TENTHS` replaced with magic number `10`.** In `Bet.java:199`, the constant `TournamentDirector.AI_PAUSE_TENTHS` was replaced with the literal `10`. The plan (Phase 7.5.3) calls for moving this constant to `PokerConstants`. A magic number is acceptable as a temporary measure since Phase 7.5 will address it, but the plan should track this.

3. **OnlineLobby.hasFocus() returns hardcoded `false`.** After removing the `ChatLobbyPanel` field, `hasFocus()` was changed to always return `false`. This is functionally correct (lobby chat panel no longer exists to have focus), but callers of this method may be dead code themselves. Worth evaluating during Phase 7.5 cleanup whether `OnlineLobby.hasFocus()` callers can be removed.

4. **OnlineLobby.deliverChatLocal() is now empty.** The method body was removed but the method shell remains. Since OnlineLobby is scheduled for deletion in Phase 7.5, this is fine as-is.

#### Required Changes (Blocking)

None.

### Verification

- **Tests:** All modules compile and pass except the pre-existing `HibernateTest.testHibernate` failure in `pokerserver` (Jackson `NoClassDefFoundError: ToEmptyObjectSerializer`). Confirmed identical failure on `main` branch -- not introduced by Phase 7.3. All poker module tests pass (0 failures, 0 errors).
- **Coverage:** Not measured -- build failure in pokerserver short-circuits coverage aggregation. Poker module coverage unaffected (same test suite passes).
- **Build:** All 17 modules compile cleanly with zero compilation errors. Only pokerserver fails (pre-existing test issue). The 2 skipped modules (gametools, DD Poker REST API) contain no Phase 7.3 changes.
- **Privacy:** No private information (IPs, credentials, personal data) found in any changed files. Deleted files contained no sensitive data.
- **Security:** No security vulnerabilities introduced. Deleted code was all client-side P2P/UI code. No new attack surface.
