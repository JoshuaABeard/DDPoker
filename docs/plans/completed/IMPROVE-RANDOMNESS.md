# Improve Deck Shuffling Randomness (Option B: Full Cleanup)

**Status:** ✅ Completed (2026-02-11)
**Selected approach:** Option B — Separate concerns clearly
**Scope:** ~50 lines changed in 2 files + 200 lines new tests

## Context

Review of how randomness/shuffling works in DD Poker identified several weaknesses in the custom seed generator and its interaction with `SecureRandom`. The core `SecureRandom` is solid, but custom seeding weakens it unnecessarily.

## Current Architecture

The app uses a **layered approach** with three RNG sources:

### 1. `SecureRandom` (Deck.java:57) — used for initial deck construction shuffle
```java
private static SecureRandom random = new SecureRandom();
```
- Used in `new Deck(true, seed)` via `Collections.shuffle(this, random)`
- **Critical issue:** When a seed is passed (which is always the case for real gameplay), it calls `random.setSeed(seed)` — this **supplements** the SecureRandom's internal entropy with that seed, but the seed itself is weak (see below)

### 2. `MersenneTwisterFast` (Deck.java:60) — used for mid-hand re-shuffles
```java
private static MersenneTwisterFast qrandom = new MersenneTwisterFast();
```
- Used in `shuffle()`, `qshuffle()`, and `addRandom()` methods
- Seeded once at startup from `System.currentTimeMillis()`
- **Not cryptographically secure**, designed purely for speed
- Used by the simulator/calc tool (appropriate there) but also by `addRandom()` during gameplay

### 3. Custom seed generator (HoldemHand.java:145-174) — feeds into the `Deck` constructor
```java
private static int lastSEED = (int) System.currentTimeMillis();
private static int SEEDADJ = 2;
```
- `NEXT_SEED()`: multiplies `lastSEED * SEEDADJ`, mods by `Integer.MAX_VALUE`
- `ADJUST_SEED()`: called on every player action (bet/fold/raise), adds elapsed milliseconds to `SEEDADJ`
- Timing of player actions provides the main entropy source

## Issues Found (Ranked by Severity)

### Issue 1: `SecureRandom.setSeed()` weakens entropy (HIGH)
**File:** `Deck.java:141`

When `seed > 0` (which is always true during gameplay), `random.setSeed(seed)` is called on the **static** `SecureRandom`. Per the Java docs, `setSeed()` on `SecureRandom` *supplements* existing entropy rather than replacing it, so this isn't catastrophic. However, it means the seed generation quality matters — and the custom seed generator is weak (see Issue 2). More importantly, because the `SecureRandom` is **static and shared**, calling `setSeed()` on it from one hand can affect the entropy state for subsequent hands across all tables.

### Issue 2: Custom seed generator has low entropy (HIGH)
**File:** `HoldemHand.java:153-162`

The `NEXT_SEED()` function is essentially a linear congruential generator:
- Starts from `System.currentTimeMillis()` (predictable to within seconds)
- Multiplies by `SEEDADJ` (starts at 2, grows with elapsed time between actions)
- The multiplication can produce short cycles or degenerate to fallback values
- Only 32 bits of seed space (int), limiting the number of possible shuffles to ~2 billion — far fewer than the 52! (~2^226) possible deck orderings

### Issue 3: `MersenneTwisterFast` used in gameplay path (MEDIUM)
**File:** `Deck.java:153-170`

The `shuffle()` and `qshuffle()` methods use `MersenneTwisterFast` which:
- Is not cryptographically secure (internal state can be reconstructed from ~624 outputs)
- Is seeded once from `System.currentTimeMillis()` at class load time
- Is **static** — shared across all decks, not thread-safe (the class docs say so explicitly)
- Used by `addRandom()` which is called during simulation re-deals

For single-player offline poker this is mostly fine since these methods are primarily used by the calculator/simulator. But it's worth noting.

### Issue 4: `qshuffle()` only shuffles 26 cards (LOW)
**File:** `Deck.java:164-170`

The quick shuffle only shuffles the first 26 cards of the deck. Cards in positions 27-52 remain in their original order. This is intentional for performance in the simulator but creates a non-uniform distribution if used elsewhere.

### Issue 5: Demo mode uses deterministic seeds (INFO)
**File:** `HoldemHand.java:195`

`seed = 9183349 + (nNum * 129L)` — completely predictable. This is by design for demo mode but worth flagging to ensure demo mode can never activate in production/online play.

## Implementation Plan (Option B)

### Step 1: Modify `Deck.java` — Remove `setSeed()`, make `SecureRandom` non-static

**File:** `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/Deck.java`

1. Remove the `setSeed()` call at line 141. The seeded-shuffle constructor should only be used for demo/test mode — when a seed is provided, use a separate `Random` seeded with that value instead of contaminating the `SecureRandom`.
2. Change `private static SecureRandom random` to a `ThreadLocal<SecureRandom>` so multi-table play gets independent RNG state per thread.
3. Add a Javadoc comment on `shuffle()` and `qshuffle()` clarifying they use `MersenneTwisterFast` and are intended for simulation/calc only, not for dealing to players.

```java
// Before:
private static SecureRandom random = new SecureRandom();

// After:
private static final ThreadLocal<SecureRandom> secureRandom =
    ThreadLocal.withInitial(SecureRandom::new);
```

```java
// Before (constructor):
if (bShuffle) {
    if (seed > 0) random.setSeed(seed);
    Collections.shuffle(this, random);
}

// After (constructor):
if (bShuffle) {
    if (seed > 0) {
        // Deterministic shuffle for demo/test mode
        Random seeded = new Random(seed);
        Collections.shuffle(this, seeded);
    } else {
        // Production shuffle using OS entropy
        Collections.shuffle(this, secureRandom.get());
    }
}
```

### Step 2: Modify `HoldemHand.java` — Scope seed logic to demo mode only

**File:** `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java`

1. In the constructor, only compute a seed when in demo mode. In production, pass seed=0 (which triggers the SecureRandom path in the updated Deck constructor).
2. Remove the `ADJUST_SEED()` call from `addHistory()` — it's no longer needed since the seed generator isn't used in production.
3. Keep `NEXT_SEED()`, `ADJUST_SEED()`, and their state variables for demo mode compatibility, but they only execute when `isDemo()` is true.

```java
// Before (constructor):
long seed = NEXT_SEED();
GameEngine engine = GameEngine.getGameEngine();
if (engine != null && engine.isDemo()) {
    PokerGame game = table.getGame();
    if (game != null && !game.isClockMode()) {
        PokerPlayer player = game.getHumanPlayer();
        int nNum = (player.isObserver()) ? table.getHandNum() : player.getHandsPlayed();
        seed = 9183349 + (nNum * 129L);
    }
}
deck_ = new Deck(true, seed);

// After (constructor):
long seed = 0;
GameEngine engine = GameEngine.getGameEngine();
if (engine != null && engine.isDemo()) {
    PokerGame game = table.getGame();
    if (game != null && !game.isClockMode()) {
        PokerPlayer player = game.getHumanPlayer();
        int nNum = (player.isObserver()) ? table.getHandNum() : player.getHandsPlayed();
        seed = 9183349 + (nNum * 129L);
    }
}
deck_ = new Deck(true, seed);
```

```java
// Before (addHistory):
ADJUST_SEED();

// After (addHistory):
// Remove the ADJUST_SEED() call entirely
```

### Step 3: Clean up unused seed code in `HoldemHand.java`

Since `NEXT_SEED()` is no longer called from the constructor in production mode and `ADJUST_SEED()` is removed from `addHistory()`, these methods and their static fields (`lastSEED`, `SEEDADJ`, `lastADJ`) become unused and can be removed.

Note: If demo mode still needs seed generation (verify this), keep the methods but mark them as demo-only.

## Call sites to verify (no changes expected)

These already correctly only pass a seed in demo mode:
- `PokerTable.java:989` — `setButtonHighCard()` — seed=0 unless `isDemo()`
- `PokerTable.java:1186` — color-up logic — seed=0 unless `isDemo()`
- `HoldemSimulator.java:85,541` — uses `new Deck(true)` (no seed) — correct

## Files to Modify

| File | Change |
|------|--------|
| `code/pokerengine/src/main/java/.../engine/Deck.java` | ThreadLocal SecureRandom, separate seeded vs unseeded shuffle paths, document simulation methods |
| `code/poker/src/main/java/.../poker/HoldemHand.java` | Remove NEXT_SEED/ADJUST_SEED from production path, seed only in demo mode |

## Verification

1. ✅ Run existing poker engine tests to ensure no regressions
2. ✅ Run `DealTester` (proto module) to verify distribution uniformity is maintained
3. ✅ Verify demo mode still produces deterministic hands (same seed = same deal)
4. ✅ Build completes with zero warnings

## Implementation Summary

**Completed:** 2026-02-11

### Changes Made

1. **Deck.java** (~30 lines changed):
   - Replaced `static SecureRandom` with `ThreadLocal<SecureRandom>` for thread isolation
   - Split shuffle logic: seed=0 uses `SecureRandom`, seed>0 uses `Random(seed)` for deterministic demo mode
   - Removed `MersenneTwisterFast` entirely
   - **Constructor shuffle:** Uses `SecureRandom` (production) or `Random(seed)` (demo)
   - **shuffle()/qshuffle()/addRandom():** Use `ThreadLocalRandom` for fast simulation/calculator path
   - Added comprehensive javadoc comments

2. **HoldemHand.java** (~30 lines changed):
   - Changed constructor to default seed=0 (production mode uses SecureRandom)
   - Kept deterministic seed formula (9183349 + nNum*129L) for demo mode only
   - Removed `ADJUST_SEED()` call from `addHistory()`
   - Deleted unused methods: `NEXT_SEED()`, `ADJUST_SEED()`
   - Deleted unused fields: `lastSEED`, `SEEDADJ`, `lastADJ`

3. **DeckRandomnessTest.java** (new file, ~230 lines):
   - 8 comprehensive tests for randomness behavior
   - Tests production mode non-determinism
   - Tests demo mode determinism
   - Tests ThreadLocal isolation
   - All tests pass ✅

### Verification Results

- **DeckRandomnessTest:** All 8 tests pass
- **DealTester (10M shuffles):**
  - Min: 7,262 | Max: 7,795 | Avg: 7,541.48
  - Expected avg: 10,000,000 / 1,326 ≈ 7,541.48 ✅
  - Variance: ~7.1% - excellent uniformity
- **Compiler warnings:** Zero ✅
- **Code coverage:** Improved with new tests

### Benefits

- **Security:** Production deck construction uses pure `SecureRandom` without weak custom seeding
- **Performance:** Simulation/calculator methods (`shuffle()`, `qshuffle()`, `addRandom()`) use fast `ThreadLocalRandom`
- **Thread Safety:** Both `SecureRandom` and `ThreadLocalRandom` use ThreadLocal for independent per-thread state
- **Simplicity:** Removed 30+ lines of complex, low-entropy seed generator code
- **Testability:** Comprehensive test suite ensures correctness
- **Demo Mode:** Still works with deterministic seeds for debugging/testing

### Architecture

**Two shuffle paths for different use cases:**

1. **Constructor shuffle (production dealing):**
   - `new Deck(true, 0)` → Uses `SecureRandom` (cryptographically secure)
   - Used for dealing cards to players in actual games
   - Maximum security, ensures unpredictable shuffles

2. **Runtime shuffle methods (simulation/calculator):**
   - `shuffle()`, `qshuffle()`, `addRandom()` → Use `ThreadLocalRandom` (fast)
   - Used by HoldemSimulator for millions of iterations
   - High quality randomness with excellent performance

### Known Limitations / Future Considerations

**ThreadLocal Cleanup:**
- `ThreadLocal<SecureRandom>` instances are never explicitly removed via `.remove()`
- **Impact:** In DD Poker's desktop architecture (long-lived threads), this is fine
- **Risk:** If used in thread-pooled environments (servlet containers, executors, app servers):
  - ThreadLocal values persist across task executions
  - Prevents garbage collection of SecureRandom instances
  - Can cause memory leaks over time
- **Mitigation (if needed):**
  - Call `secureRandom.remove()` after deck construction in pooled environments
  - Or refactor to pass RNG instances explicitly instead of using ThreadLocal
  - `ThreadLocalRandom` doesn't have this issue (JVM-managed, no cleanup needed)
