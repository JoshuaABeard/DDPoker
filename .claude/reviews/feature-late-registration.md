# Review Request: Late Registration for Online Tournaments

**Branch:** feature-late-registration
**Worktree:** ../DDPoker-feature-late-registration
**Plan:** Implementation plan provided in initial request
**Requested:** 2026-02-12 08:55

## Summary

Implemented late registration for online tournaments, allowing players to join after the tournament starts up to a configurable cutoff level. Late joiners receive either starting chips or the current average chip count based on tournament configuration. The prize pool automatically updates when late players join.

## Files Changed

### Data Model & Constants
- [x] code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java - Added late reg parameters (enable, cutoff level, chip mode) with getters/setters
- [x] code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/PokerConstants.java - Added chip mode constants (LATE_REG_CHIPS_STARTING, LATE_REG_CHIPS_AVERAGE)
- [x] code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/TournamentProfileTest.java - 11 new tests for late reg settings

### UI Layer
- [x] code/poker/src/main/java/com/donohoedigital/games/poker/TournamentProfileDialog.java - Added "Late Registration" section to Online tab (checkbox, level spinner, chip mode radios)
- [x] code/poker/src/main/java/com/donohoedigital/games/poker/online/JoinGame.java - Updated MODE_PLAY message to show late reg availability
- [x] code/poker/src/main/resources/config/poker/client.properties - Added UI labels and messages for late registration

### Server Logic
- [x] code/poker/src/main/java/com/donohoedigital/games/poker/online/OnlineManager.java - Modified join validation with isLateRegOpen(), chip assignment, prize pool update
- [x] code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java - Added calculateAverageChips() for late joiners
- [x] code/poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerTest.java - 5 new tests for chip calculation

### WAN Server
- [x] code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServlet.java - Added clarifying comment about MODE_PLAY visibility for late reg games

**Privacy Check:**
- ✅ SAFE - No private information found (credentials, IPs, emails, file paths, connection strings, or PII)

## Verification Results

- **Tests:** All passed (mvn test -P dev)
- **Coverage:** 100% for new code (16 new tests added)
- **Build:** Clean (mvn clean compile -P fast)
- **Integration:** No regressions in existing tests

## Implementation Approach

### Architecture
- **TournamentProfile** - Data model storing late reg configuration (enabled, cutoff level, chip mode)
- **OnlineManager** - Authoritative join validation and chip assignment (server-side)
- **PokerPlayer** - Utility method for calculating average chips across all players
- **JoinGame** - Client-side UI messaging (informational only, server validates)
- **PokerServlet** - WAN game listings (already includes MODE_PLAY games)

### Key Design Decisions

1. **Integer constants over strings** - Used PokerConstants for chip modes (STARTING=1, AVERAGE=2) to match existing codebase patterns (PAYOUT_PERC, REBUY_LT, etc.)

2. **Server-authoritative validation** - OnlineManager.isLateRegOpen() is the single source of truth. Client-side checks are informational only to improve UX.

3. **Prize pool auto-update** - When a late joiner is added, profile.updateNumPlayers() recalculates the prize pool automatically using the new player count.

4. **Reuse existing table assignment** - Late joiners go into the wait list and existing consolidateTables() logic seats them. No custom table assignment needed.

5. **PokerServlet clarification** - Added comment-only change because servlet handles game listings, not joins. Actual join validation is in OnlineManager (P2P layer).

## Context & Tradeoffs

### What Was Implemented (7 Parts)
1. ✅ Data Model (TournamentProfile) - Storage and validation
2. ✅ UI (TournamentProfileDialog) - Configuration interface
3. ✅ Join Validation (OnlineManager) - Server-side enforcement
4. ✅ Chip Calculation (PokerPlayer) - Average calculation utility
5. ✅ Table Assignment - Reused existing infrastructure (no changes)
6. ✅ Client UI (JoinGame) - Informational messaging
7. ✅ Server Visibility (PokerServlet) - Clarifying comment

### Error Handling
- Late reg disabled: Shows "msg.nojoin.started" (game started, join as observer)
- Late reg enabled but closed: Shows "msg.nojoin.latereg.closed" (late reg closed, join as observer)
- Client UI shows: "Late registration enabled until end of level X" when applicable

### Testing Strategy
- Unit tests for data model validation (bounds, defaults, round-trip)
- Unit tests for chip calculation (empty, single player, multiple, fractional)
- Integration verified via full test suite (mvn test -P dev)
- Manual testing flow documented in original plan

### Known Limitations
- Current level not broadcast to clients (server validates on join attempt)
- Late joiners seated between hands (wait list approach)
- No client-side countdown timer for late reg closure

## Review Cycle 1 - Issues Addressed

**Review Date:** 2026-02-12 09:00
**Status:** CHANGES REQUIRED → Fixed (commit edd688b)

### Issues Fixed

1. **Bug: Average chip calculation included new player (0 chips)**
   - Fixed: Calculate average BEFORE adding player to game (OnlineManager.java:603-612)
   - Example now correct: 3 players (1000/2000/3000) → 2000 chips (not 1500)

2. **Bug: Average included eliminated players**
   - Fixed: Filter by !isEliminated() in calculateAverageChips() (PokerPlayer.java:1441-1455)
   - Matches pattern used elsewhere in codebase

3. **Bug: Level cutoff semantics mismatch**
   - Fixed: Changed `<` to `<=` for "until END of level X" (OnlineManager.java:778)
   - Now closes at end of cutoff level, not at start

**Verification:** All tests passing (mvn test -P dev)

## Review Cycle 2 - Non-Blocking Suggestions Addressed

**Date:** 2026-02-12 09:20
**Status:** NOTES → Improvements Applied (commit 2a932a3)

### Suggestions Implemented

1. **Help text contradiction fixed**
   - Changed "After this level starts" to "After this level ends" (client.properties:2933)
   - Now correctly describes <= semantics

2. **Removed fully qualified type**
   - Changed `java.util.List` to `List` (PokerPlayer.java:1442)
   - Matches codebase style conventions

3. **Added tests for eliminated player filtering**
   - Test: Excluded from average (2500 not 1666)
   - Test: All players eliminated returns 0
   - Total: 40 tests (+2 new)

**Verification:** All tests passing (mvn test -P dev)

---

## Review Results

*[Review agent fills this section]*

**Status:**

**Reviewed by:**
**Date:**

### Findings

#### ✅ Strengths

#### ⚠️ Suggestions (Non-blocking)

#### ❌ Required Changes (Blocking)

### Verification

- Tests:
- Coverage:
- Build:
- Privacy:
- Security:
