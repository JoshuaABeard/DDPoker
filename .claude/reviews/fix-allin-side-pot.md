# Review Request

**Branch:** fix-allin-side-pot
**Worktree:** ../DDPoker-fix-allin-side-pot
**Plan:** N/A
**Requested:** 2026-02-22 14:15

## Summary

Fixed a bug in `ServerHand.calcPots()` where the chip leader lost all their chips in a heads-up all-in situation when the short-stacked opponent won. Two bugs in the server-side pot distribution logic caused this: incorrect distribution order (main pot processed first, taking all chips) and cumulative rather than incremental side pot caps (mismatching the client's algorithm).

## Files Changed

- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerHand.java` - Fixed `calcPots()`: incremental sideBet values + two-pass distribution (side pots first, main pot last); added package-private test constructor
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerDeck.java` - Added package-private constructor for test-injecting a specific card order
- [ ] `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPot.java` - Updated javadoc to reflect incremental cap semantics
- [ ] `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandTest.java` - Added two deterministic all-in tests: short stack wins (chip leader gets excess back), chip leader wins (gets everything)

**Privacy Check:**
- ✅ SAFE - No private information found

## Verification Results

- **Tests:** 587/587 passed
- **Coverage:** not measured (unit tests only via `-P dev`)
- **Build:** Clean

## Context & Decisions

### Root Cause

`ServerHand.calcPots()` had the pot list ordered `[mainPot(sideBet=0), sidePot(sideBet=X)]`. The distribution loop processed them in list order. Since `sideBet==0` means "take all remaining chips", the main pot swept everything before the side pot got any. This left the chip leader's excess in a pot where both players were eligible, so the short-stack winner took all 1600 chips instead of their rightful 1200.

The client's `HoldemHand.calcPots()` (correct) works the opposite way: the "capped" pot (sideBet set) is added first, and the "take-all" pot (NO_SIDE) is added last, so distribution processes the cap first.

### Fix Strategy

Two-pass distribution: pass 1 handles all `sideBet > 0` pots (capped), pass 2 handles `sideBet == 0` (main pot, takes remainder). Side pots now also store incremental caps (`potInfo.bet - lastSideBet`) rather than cumulative thresholds, which is required for correct multi-all-in scenarios where a player's remaining bet after earlier side pots is smaller than the cumulative threshold.

### Test Design

Tests use a package-private `ServerDeck(List<Card>)` constructor and `ServerHand(..., ServerDeck)` constructor to inject deterministic hole cards (A♠A♥ vs 2♦3♦) so we can assert the specific winner without random variance.

---

## Review Results

**Status:** APPROVED WITH SUGGESTIONS

**Reviewed by:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Findings

#### ✅ Strengths

1. **Root cause analysis is accurate.** The bug is precisely diagnosed: the pre-fix server code iterated pots in list order `[mainPot(sideBet=0), sidePot(sideBet=X)]`, so the main pot's "take all remaining" sweep consumed all chips before the side pot got any. The client's `HoldemHand.calcPots()` avoids this by structural list ordering (the side pot with its cap gets a `setSideBet` call and the new `Pot(round, i+1)` is appended last with `NO_SIDE=-1`), so a single-pass loop works. The server cannot rely on list order because the main pot is always created first, so the two-pass approach is the correct fix.

2. **Incremental vs cumulative cap fix is correct.** The old code stored `potInfo.bet` (the cumulative all-in level) as the side pot cap. For multi-all-in scenarios this is wrong: if player A is all-in for 300 and player B for 600, the side pot for A should cap at 300-per-player, and after that side pot is satisfied, the next side pot should cap at 300 more (not 600). The fix stores `potInfo.bet - lastSideBet` (incremental), matching what the client does at `pot.setSideBet(potinfo.nBet - nLastSideBet)` (HoldemHand.java line 1576).

3. **Manual trace of the primary bug scenario confirms correct outcomes:**
   - Alice (chip leader, 1000) vs Bob (short stack, 600), both all-in, Bob wins.
   - After betting: `playerBets[Alice]=1000`, `playerBets[Bob]=600`, `currentBet=1000`.
   - `PotInfo`: Bob (needsSide=true, bet=600), Alice (needsSide=false, bet=1000).
   - Pass 0 - side pot (sideBet=600): Bob contributes 600, Alice contributes 600. sidePot = 1200, both eligible.
   - Pass 1 - main pot (sideBet=0): only Alice has remaining 400. mainPot = 400, Alice only eligible → `isOverbet()` = true.
   - Resolve: Bob wins sidePot (1200). Alice gets mainPot returned (400). Total: Bob=1200, Alice=400. Correct.

4. **Chip leader wins case is also correct:** When Alice wins, she wins the sidePot (1200) and gets the overbet mainPot returned (400) = 1600 total. Correct.

5. **Deterministic test design is solid.** Using `ServerDeck(List<Card>)` and the package-private `ServerHand(..., ServerDeck)` constructor is the right approach for deterministic tests. The `headsUpDeck` helper carefully accounts for deal order (seat0 first), burn cards, and the full 13-card sequence through river. A♠A♥ vs 2♦3♦ is an excellent dominating hand choice — community cards (7, 8, 9, J, K) give no straights, flushes, or pairs to the 2♦3♦, so the result is never in doubt.

6. **Both parity tests (short-stack wins, chip-leader wins) are included.** This covers the actual bug regression plus the symmetric happy path.

7. **Package-private visibility on test constructors is appropriate.** `ServerDeck(List<Card>)` and `ServerHand(..., ServerDeck)` are in the same package as the tests, no reflection needed, and the `public` production API is untouched.

8. **Surgical change.** Only the minimum code is modified: the two bugs in `calcPots()`, the two test constructors, and the javadoc update. Nothing else is touched.

#### ⚠️ Suggestions (Non-blocking)

1. **Missing test case: 3-player multi-all-in with different stack sizes.** The two new tests cover heads-up all-in scenarios. A case with 3 players at different stack sizes (e.g., 400/600/1000) going all-in would validate the incremental cap logic more rigorously — this is exactly the scenario where the old cumulative-cap bug could surface in a non-obvious way. The current tests would pass even with a cumulative cap when there is only one side pot (because `potInfo.bet - lastSideBet` = `potInfo.bet - 0` = `potInfo.bet`). Consider adding a test such as:
   - Alice=1000, Bob=600, Carol=400, all-in.
   - Expected: Carol's side pot (3x400=1200, all three eligible), Bob's side pot (2x200=400, Bob+Alice), Alice's overbet pot (400, Alice only returned).

2. **The `addToPot()` method (lines 279-300) is dead code.** It is defined but never called in the current betting flow (all bets go through `playerBets` → `calcPots()`). This is pre-existing and not introduced by this fix, but noting it as a maintainability concern. The method has different eligibility semantics from `calcPots()` and could cause confusion if someone accidentally wires it back in.

3. **Minor: the `headsUpDeck` helper comment says "seat0 gets deck[0..1], seat1 gets deck[2..3]"** but `dealHoleCards()` iterates seats in order, so it actually deals to the lowest-numbered seat first. The comment is correct, but it would be even clearer to state that the deal order is determined by seat index (0, 1, 2...) independent of player order.

#### ❌ Required Changes (Blocking)

None.

### Verification

- Tests: 587/587 passed (as reported by author; not independently re-run)
- Coverage: Not measured (unit tests only via `-P dev`)
- Build: Clean (as reported by author)
- Privacy: No private data found
- Security: No security concerns; test constructors are package-private, not accessible externally
