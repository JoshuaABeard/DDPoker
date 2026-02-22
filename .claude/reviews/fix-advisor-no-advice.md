# Review Request

**Branch:** fix-advisor-no-advice
**Worktree:** ../DDPoker-fix-advisor-no-advice
**Plan:** N/A (2-line bug fix)
**Requested:** 2026-02-22

## Summary

The advisor dashboard panel showed "no advice" on the player's very first action
turn in an online session, even though advice should be available. Subsequent
turns in the same session worked correctly.

**Root cause:** `PokerPlayer.getPokerAI()` lazily creates the advisor AI for
the local human player in online mode (when `playerType_ == null`). The method
called `setPokerAI(PokerAI.createPokerAI(playerType_))` to store the AI, but
never assigned the result to the local `ai` variable — so the method returned
`null` on that first call.

`DashboardAdvisor.updateInfo()` guards on `(ai != null) && (ai instanceof V2Player)`.
With `ai == null`, the guard fails and `NOADVICE` is shown. On the second call
`getGameAI()` returns the stored advisor AI and everything works.

The `else` branch (dummy AI for non-human-controlled online players) correctly
did `ai = new PokerAI(); setPokerAI(ai);`. The human-player branch now follows
the same pattern.

## Files Changed

- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` — Split
  `setPokerAI(PokerAI.createPokerAI(playerType_))` into two statements so the
  local `ai` variable is updated before `return ai`.

**Privacy Check:**
- ✅ SAFE — No private information

## Verification Results

- **Tests:** Pre-existing compilation errors in `WebSocketTournamentDirector.java` and
  `GameServerRestClient.java` from other in-progress branches prevent full build. The
  change to `PokerPlayer.java` itself is syntactically clean (single-expression split).
- **Coverage:** N/A — no new test class; lazy-init path requires a live online session.
- **Build:** `PokerPlayer.java` produces no new errors.

## Context & Decisions

**Why only online mode?** In practice mode the human player has `playerType_` set
during table setup (via `createPokerAI()`), so `getGameAI()` returns the existing
V2Player and the lazy-init block is never entered. Only online-game guests start
with `playerType_ == null`.

**No change to AI behaviour:** The advisor AI created is identical to what was
stored before — `PokerAI.createPokerAI(PlayerType.getAdvisor())`. The only
difference is that the caller now receives it on the first call instead of null.

**Symmetry with else branch:** The fix makes both branches of the if/else read
`ai = <create AI>; setPokerAI(ai);`, which is the obviously correct pattern.

## Review Results

**Verdict: APPROVED**

**Reviewer:** Claude Sonnet 4.6
**Date:** 2026-02-22

### Root Cause Diagnosis

Correct. The diff confirms the original code was:

```java
setPokerAI(PokerAI.createPokerAI(playerType_));
```

The created AI was stored via `setPokerAI` but the local `ai` variable was never
assigned, so `return ai` returned the `null` it had at entry. On the very next
call, `getGameAI()` (the base-class getter) would return the now-stored AI, which
is why only the first call exhibited the bug.

### Fix Correctness

The fix is correct. The two-line replacement:

```java
ai = PokerAI.createPokerAI(playerType_);
setPokerAI(ai);
```

assigns the newly created AI to both the local variable and the field, so
`return ai` returns the real object on the first call. This is exactly the same
pattern used by the `else` branch that was already working.

### Guard Condition Confirmation

`DashboardAdvisor.updateInfo()` (line 164) requires all of: `hh != null`,
`pp != null`, `h != null`, `ai != null`, and `ai instanceof V2Player`. The lazy
path creates the AI via `PokerAI.createPokerAI(PlayerType.getAdvisor())`, which
returns a `V2Player` instance. Both conditions — non-null and correct type — are
satisfied after the fix. The `NOADVICE` fallback path is no longer incorrectly
taken on the first call.

### Both Branches Now Correct

- **Human locally-controlled branch (fixed):** `ai = PokerAI.createPokerAI(playerType_); setPokerAI(ai);` — local variable populated, non-null returned.
- **Else branch (was already correct):** `ai = new PokerAI(); ai.init(); setPokerAI(ai);` — same pattern, unchanged.

Both branches now assign `ai` before calling `setPokerAI(ai)` and both return
the real object on the first call.

### Edge Cases and Unintended Consequences

None identified.

- **Thread safety:** No regression — the lazy-init block was never thread-safe
  to begin with and is only called on the Swing EDT; the fix does not change that.
- **`setPokerAI` side-effects:** `setPokerAI` reads the old AI via `getGameAI()`
  (not `getPokerAI()`) to avoid recursion, so passing the freshly created `ai`
  before it is stored carries no risk.
- **`PokerAI.createPokerAI` returning null:** If it returned null the behaviour
  would be identical to before the fix (null stored and returned), so no new
  failure mode is introduced.
- **Scope of change:** Only one file, only two lines changed. No adjacent logic
  is touched.

### Minor Observations (non-blocking)

The `else` branch calls `ai.init()` after construction but the `if` branch
(which calls `PokerAI.createPokerAI`) does not. This pre-existed the fix and is
outside its scope; `createPokerAI` presumably handles its own initialization
internally. Worth a follow-up look, but not a blocker for this fix.
