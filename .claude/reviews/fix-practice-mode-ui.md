# Review Request

**Branch:** fix-practice-mode-ui
**Worktree:** ../DDPoker-fix-practice-mode-ui
**Plan:** .claude/plans/unified-stirring-porcupine.md
**Requested:** 2026-02-21

## Summary

Fixes 5 visual/protocol bugs discovered during manual play of practice mode. The bugs were
all gaps in the WebSocket client-server protocol or remote UI rendering, with no changes to
game logic.

## Files Changed

Branch was rebased onto main (2026-02-21) after the initial review found that the initial diff
included fixes from 3 newer main commits (`8f2f97d4`, `d222c053`, `ec827838`) that postdated
branch creation. After rebase, diff vs main is exactly these 6 files:

- [ ] `code/pokergameserver/src/main/java/.../websocket/OutboundMessageConverter.java` — Bug 1: `cardToString()` now uses `getRankDisplaySingle()` instead of `getDisplay()` so Ten serializes as "Ts" (not "10s") matching the parser
- [ ] `code/pokergameserver/src/test/java/.../websocket/OutboundMessageConverterTest.java` — Updated "10c"→"Tc" assertion; added `SPADES_T`→"Ts" round-trip test
- [ ] `code/poker/src/main/java/.../online/WebSocketTournamentDirector.java` — Bug 2: `onPlayerEliminated()` now calls `table.clearSeat()` and fires `TYPE_PLAYER_REMOVED` event, matching the pattern of `onPlayerLeft()`
- [ ] `code/poker/src/main/resources/application-embedded.properties` — Bug 3: `ai-action-delay-ms` 400→1000 so AI "think time" is perceptible
- [ ] `code/poker/src/main/java/.../PokerCustomTerritoryDrawer.java` — Bug 4: `drawPot()` falls back to `getTotalPotChipCount()` when history is empty (remote mode), so chip pile renders correctly
- [ ] `code/poker/src/main/java/.../online/RemoteHoldemHand.java` — Bug 5: override `getCommunitySorted()` with lazy `HandSorted` cache so HandStrengthDash and ImproveOdds don't NPE

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** Full `mvn test -P dev` from root: BUILD SUCCESS
- **Coverage:** Not checked (no new production logic, all changes are protocol/rendering fixes)
- **Build:** Clean

## Context & Decisions

**Bug 1 (card serialization):** `Card.getDisplay()` returns "10h" for Ten; `Card.getCard("10h")` parses `charAt(1)='0'` as UNKNOWN suit → BLANK card. `getRankDisplaySingle()` returns "T" matching the parser's `case 'T': return TEN`. One-line fix.

**Bug 2 (eliminated player):** `onPlayerEliminated()` previously set finish position and zeroed chips but never called `table.clearSeat()`, so the player object stayed in `RemotePokerTable.remotePlayers_[]`. Fix reuses identical pattern from `onPlayerLeft()`.

**Bug 3 (AI delay):** Mechanism was already correct (server-side sleep in `ServerPlayerActionProvider.getAction()`); 400ms was just too short to perceive when multiple AIs fold in sequence. Increased to 1000ms.

**Bug 4 (chip pile):** `RemoteHoldemHand.getHistoryCopy()` intentionally returns an empty list to avoid NPE from uninitialized `history_` in the no-arg parent constructor. `drawPot()` iterated that history for blind/ante actions. Added empty-history fallback to use `getTotalPotChipCount()` instead, which IS populated on every `PLAYER_ACTED` WebSocket message.

**Bug 5 (hand strength NPE):** `HoldemHand.communitySorted_` is `private`, initialized only in the normal constructor (not the no-arg constructor used by RemoteHoldemHand). Overrode `getCommunitySorted()` in `RemoteHoldemHand` with a local lazy `HandSorted` cache, matching the exact pattern in `HoldemHand.getCommunitySorted()` line 740.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Opus 4.6 (review agent) -- re-review after rebase onto main
**Date:** 2026-02-21

### Context

The initial review (2026-02-21) returned CHANGES REQUIRED, citing 6 apparent regressions and 4
undocumented file changes. Investigation revealed these were false positives: the branch predated
3 main commits (`8f2f97d4`, `d222c053`, `ec827838`) that added those features. The branch was
rebased onto main, which absorbed those commits. The diff vs main is now exactly 6 files
(confirmed: `git diff main --stat` shows 6 files, +45/-22 lines). All prior "regression" findings
are resolved -- those features now exist on main and are not touched by this branch.

### Findings

#### File 1: OutboundMessageConverter.java (Bug 1 -- card serialization)

**Change:** `cardToString()` switches from `card.getDisplay()` to `card.getRankDisplaySingle() + card.getCardSuit().getAbbr()`.

**Analysis:** Correct and surgical. `Card.getDisplay()` returns "10s" for Ten of Spades. The parser `Card.getCard(String)` reads `charAt(0)` for rank and `charAt(1)` for suit. With "10s", `charAt(1)` is `'0'`, which matches no suit case in the switch statement (lines 454-471 of Card.java), yielding `UNKNOWN_RANK` and ultimately a BLANK card. With the fix, "Ts" is produced: `charAt(0)='T'` maps to TEN via `case 'T': return TEN` (Card.java line 247), and `charAt(1)='s'` maps to SPADES. Round-trip is correct.

Both `getRankDisplaySingle()` and `getCardSuit().getAbbr()` are well-established in the codebase (used in `CardImageCreator`, `Hand.toString()`, `PokerDatabase`, `ImpExpParadise`, and `Card.toStringSingle()`). No side effects.

**Verdict:** Correct. No issues.

#### File 2: OutboundMessageConverterTest.java (Bug 1 -- test updates)

**Changes:** (a) Updated existing assertion from `"10c"` to `"Tc"` for `Card.CLUBS_T` in the `createGameStateMessage` test. (b) Added explicit `assertEquals("Ts", OutboundMessageConverter.cardToString(Card.SPADES_T))` assertion with explanatory comment.

**Analysis:** Both changes are minimal and correct. The new assertion directly validates the bug fix. The existing "As" assertion still covers the non-Ten case. Coverage is adequate for a one-line serialization change.

**Verdict:** Correct. No issues.

#### File 3: WebSocketTournamentDirector.java (Bug 2 -- player elimination)

**Change:** `onPlayerEliminated()` rewritten from a flat lookup-and-update to a loop-over-tables pattern that: (a) finds the player's seat via `findSeat(table, d.playerId())`, (b) sets finish position and zeros chips, (c) calls `table.clearSeat(seat)`, (d) fires `TYPE_PLAYER_REMOVED` with the full `PokerTableEvent(type, table, player, seat)` constructor, and (e) breaks after the first match.

**Analysis:** This exactly mirrors the existing `onPlayerLeft()` method (lines 838-850), which uses the same loop-find-clear-fire-break pattern. The previous implementation set player metadata but never called `clearSeat()`, leaving the eliminated player's object in the table's seat array. This caused the player to remain visually seated. The fix is structurally identical to the proven `onPlayerLeft()` pattern. The `SwingUtilities.invokeLater()` wrapper is preserved.

One subtle difference from `onPlayerLeft()`: the elimination handler also sets `p.setPlace(d.finishPosition())` and `p.setChipCount(0)` before clearing the seat. This is correct -- finish position must be recorded before the player is removed from the table, and zeroing chips is cosmetically appropriate (eliminated = no chips). These calls operate on the player object, which remains referenced even after `clearSeat()` removes it from the table array.

**Verdict:** Correct. No issues.

#### File 4: application-embedded.properties (Bug 3 -- AI delay)

**Change:** `game.server.ai-action-delay-ms` changed from `400` to `1000`.

**Analysis:** A config-only change. The delay mechanism (server-side sleep in `ServerPlayerActionProvider.getAction()`) is unchanged. 1000ms is reasonable for human perception when watching AI actions. The comment on the preceding line already documents the purpose ("Delay between AI player actions so humans can observe the game").

**Verdict:** Correct. No issues.

#### File 5: PokerCustomTerritoryDrawer.java (Bug 4 -- pot chip rendering)

**Change:** `drawPot()` now checks `hist.isEmpty()` before iterating. When empty (remote mode), it draws chips from `hhand.getTotalPotChipCount()` instead. The original history-iteration path is preserved in an `else` block.

**Analysis:** `RemoteHoldemHand.getHistoryCopy()` always returns `Collections.emptyList()` (the no-arg parent constructor does not initialize `history_`, and the override avoids accessing it). Previously, the loop over an empty list simply drew nothing -- no chips were ever rendered in remote mode. The fallback uses `getTotalPotChipCount()`, which is populated by `RemoteHoldemHand.updatePot()` on every `PLAYER_ACTED` WebSocket message, so it reflects the current pot.

The `while (potTotal > 0)` loop is safe: `drawAmount()` always subtracts at least 1 (the minimum chip denomination), guaranteeing termination. The `else` block preserves the existing local-game behavior untouched -- the only code change for local games is the addition of the `if/else` branch, which is a no-op since local games always have non-empty history.

**Verdict:** Correct. No issues.

#### File 6: RemoteHoldemHand.java (Bug 5 -- hand strength NPE)

**Change:** Added a `remoteCommunitySorted_` field (initialized to `new HandSorted(5)`) and overrode `getCommunitySorted()` with a lazy-cache pattern that rebuilds when `remoteCommunity_` changes.

**Analysis:** The parent `HoldemHand.getCommunitySorted()` (line 739-744) uses the same pattern: compare `communitySorted_.fingerprint()` with `community_.fingerprint()`, rebuild if different. However, the parent's `communitySorted_` is `private` and initialized only in the normal constructor -- the no-arg constructor (used by `RemoteHoldemHand`) leaves it `null`, causing NPE when `HandStrengthDash` or `ImproveOdds` panels call `getCommunitySorted()`.

The override uses the local `remoteCommunitySorted_` and `remoteCommunity_` fields, which are always non-null (initialized in field declarations). The `HandSorted(5)` constructor creates an empty hand with fingerprint 0, matching the initial empty `remoteCommunity_` fingerprint (also 0), so no unnecessary rebuilds occur. When `updateCommunity()` sets a new `Hand`, the fingerprints diverge and the cache rebuilds correctly via `new HandSorted(remoteCommunity_)`, which sorts the community cards.

**Verdict:** Correct. No issues.

### Suggestions (Non-blocking)

1. **Stale class-level Javadoc in OutboundMessageConverter (lines 35-36):** The Javadoc still says "All card serialization uses Card.getDisplay()" but the code now uses `getRankDisplaySingle() + getCardSuit().getAbbr()`. This should be updated to avoid misleading future readers. For example: "Card serialization uses getRankDisplaySingle() + getCardSuit().getAbbr() to produce two-character notation (e.g. 'Ah', 'Ts') compatible with Card.getCard() parsing."

2. **AI delay tuning note:** 1000ms is reasonable for a 3-player table, but at a 9-player table with 8 AIs folding in sequence, the round takes 8 seconds of pure delay. This is a UX judgment call and acceptable for now, but documenting that this value is tunable (or exposing it in a settings UI in a future iteration) would be beneficial.

### Verification

- **Diff scope:** Confirmed via `git diff main --stat`: exactly 6 files changed, +45/-22 lines. No undocumented files.
- **Tests:** BUILD SUCCESS reported. The `OutboundMessageConverterTest` updates directly cover the card serialization fix. The other 4 bugs (elimination, AI delay, pot rendering, hand strength NPE) are in Swing UI / rendering code where unit testing is impractical -- they were verified through manual play (per the plan).
- **Coverage:** Not checked; acceptable. No new production logic requiring coverage -- all changes are rendering/protocol fixes. No test deletions in this diff.
- **Build:** Clean.
- **Privacy:** No private information in any changed file. SAFE.
- **Security:** No security concerns. No credential handling, no user input processing, no new attack surface.
