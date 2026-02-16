# Phase 7D: ServerAIProvider Integration — Detailed Plan

**Status:** COMPLETE
**Created:** 2026-02-16
**Completed:** 2026-02-16
**Parent Plan:** `.claude/plans/completed/PHASE7-AI-EXTRACTION.md`
**Next Step:** `.claude/plans/M1-SERVER-GAME-ENGINE.md` (Milestone 1: Server Game Engine Foundation — depends on this plan)

---

## Context

V1 and V2 AI algorithms have been fully extracted to `pokergamecore` (Phases 7B, 7C complete). The server has skeleton implementations (`ServerAIProvider`, `ServerAIContext`, `ServerV2AIContext`, `ServerStrategyProvider`) but they are not yet functional:

- **ServerAIProvider** only creates `TournamentAI` for ALL computer players (ignores skill level)
- **ServerAIContext** has ~15 stub methods returning 0/false/null — V1Algorithm is non-functional without them
- **V2Algorithm.wantsRebuy/wantsAddon** return false (TODO stubs)
- Context lifecycle is broken — `currentHand` is final but changes per hand

**Goal:** Make ServerAIProvider route to TournamentAI, V1Algorithm, or V2Algorithm based on player skill level, with all contexts providing real game state data.

---

## Current State Inventory

| File | Status | Key Issues |
|------|--------|------------|
| `ServerAIProvider.java` | Skeleton | Only TournamentAI; single shared AIContext; no skill routing |
| `ServerAIContext.java` | Partial | ~15 stubs (position, pot, cards, betting round); hand eval works |
| `ServerV2AIContext.java` | Mostly done | Many delegations work; opponent model stubbed; some TODOs acceptable |
| `ServerStrategyProvider.java` | Done | Simplified Sklansky rankings (acceptable for now) |
| `StrategyData.java` + `StrategyDataLoader.java` | Done | Loads .dat personality files |
| `V1Algorithm.java` | Done | Needs public rebuy/addon static methods |
| `V2Algorithm.java` | Done | wantsRebuy/wantsAddon return false |
| `TournamentAI.java` | Done | Working |

---

## Implementation Plan

### Step 1: Implement ServerAIContext Stub Methods

**File:** `code/pokerserver/src/main/java/.../server/ServerAIContext.java`

**Goal:** Implement the ~15 stub methods so V1Algorithm functions correctly. Pull up position logic from `ServerV2AIContext`.

**1a. Make `currentHand` mutable:**
```java
private GameHand currentHand;  // Remove 'final'

public void setCurrentHand(GameHand hand) {
    this.currentHand = hand;
}
```

**1b. Pull up position methods from ServerV2AIContext:**

Move `isButton()`, `isSmallBlind()`, `isBigBlind()` implementations and the private helpers `calculateSmallBlindSeat()`, `calculateBigBlindSeat()` from `ServerV2AIContext` up into `ServerAIContext`. Remove the overrides from `ServerV2AIContext`.

**1c. Implement critical stubs (V1 is broken without these):**

| Method | Implementation |
|--------|---------------|
| `getBettingRound()` | `currentHand.getRound()` |
| `getHoleCards(player)` | `currentHand.getPlayerCards(player)` with security: `if (player != aiPlayer) return null` |
| `getCommunityCards()` | `currentHand.getCommunityCards()` |
| `getPotSize()` | `currentHand.getPotSize()` |
| `getAmountToCall(player)` | `currentHand.getAmountToCall(player)` |
| `getNumActivePlayers()` | `currentHand.getNumWithCards()` |

**1d. Implement moderate-priority stubs (improve V1 quality):**

| Method | Implementation |
|--------|---------------|
| `getPosition(player)` | Calculate seat distance from button (0=button through N) |
| `hasBeenBet()` | `currentHand.wasPotAction(currentHand.getRound())` |
| `hasBeenRaised()` | `currentHand.wasRaisedPreFlop()` for round 0; `getLastBettor(round, false) != null` for later rounds |
| `getLastBettor()` | `currentHand.getLastBettor(currentHand.getRound(), true)` |
| `getLastRaiser()` | `currentHand.getLastBettor(currentHand.getRound(), false)` |
| `getNumCallers()` | `currentHand.getNumLimpers()` (close approximation) |
| `getNumPlayersYetToAct(player)` | Count occupied seats after player that haven't acted yet (via `hasActedThisRound`) |
| `getNumPlayersWhoActed(player)` | Count occupied seats that have acted this round |

**1e. Stubs that remain as neutral defaults (no server-side data available):**

| Method | Value | Reason |
|--------|-------|--------|
| `getAmountBetThisRound(player)` | 0 | No per-player per-round bet tracking in GameHand |
| `getLastBetAmount()` | 0 | No direct method on GameHand |
| `getLastActionInRound(player, round)` | ACTION_NONE | No per-round action history tracking |
| `getOpponentRaiseFrequency(opponent, round)` | 50 | No opponent modeling on server |
| `getOpponentBetFrequency(opponent, round)` | 50 | No opponent modeling on server |

These are acceptable — V1 works with neutral defaults for opponent modeling (it uses them as hints, not gates). `getAmountBetThisRound` and `getLastBetAmount` are used in bet-sizing logic where 0 produces conservative behavior (not crashes).

**Also modify:** `ServerV2AIContext.java` — remove the overridden `isButton()`, `isSmallBlind()`, `isBigBlind()` methods and the private `calculateSmallBlindSeat()`/`calculateBigBlindSeat()` helpers (now inherited from parent).

---

### Step 2: Fix V2Algorithm Rebuy/Addon

**Files:**
- `code/pokergamecore/src/main/java/.../core/ai/V1Algorithm.java`
- `code/pokergamecore/src/main/java/.../core/ai/V2Algorithm.java`

**Goal:** V2Algorithm should make personality-based rebuy/addon decisions instead of always returning false.

**2a. Make V1Algorithm's static rebuy/addon methods package-private:**

Change visibility of the two static helper methods from `private` to package-private (default):
```java
// Was: private static boolean wantsRebuy(GamePlayerInfo player, int rebuyPropensity)
static boolean wantsRebuy(GamePlayerInfo player, int rebuyPropensity) { ... }
static boolean wantsAddon(GamePlayerInfo player, int addonPropensity, int buyinChips) { ... }
```

**2b. Add propensity fields and implement in V2Algorithm:**
```java
// Add fields
private final int rebuyPropensity;
private final int addonPropensity;

// Add constructor that accepts propensity values
public V2Algorithm(int rebuyPropensity, int addonPropensity) {
    this(null, false, rebuyPropensity, addonPropensity);
}

public V2Algorithm(Consumer<String> debugOutput, boolean enableDebug,
                   int rebuyPropensity, int addonPropensity) {
    this.ruleEngine = new PureRuleEngine();
    this.debugOutput = debugOutput != null ? debugOutput : (s -> {});
    this.debug = enableDebug;
    this.rebuyPropensity = rebuyPropensity;
    this.addonPropensity = addonPropensity;
}

// Keep existing no-arg constructor with reasonable defaults
public V2Algorithm() {
    this(null, false, 50, 50);  // Moderate propensity
}

// Implement
@Override
public boolean wantsRebuy(GamePlayerInfo player, AIContext context) {
    return V1Algorithm.wantsRebuy(player, rebuyPropensity);
}

@Override
public boolean wantsAddon(GamePlayerInfo player, AIContext context) {
    TournamentContext tournament = context.getTournament();
    int buyinChips = tournament != null ? tournament.getStartingChips() : 0;
    return V1Algorithm.wantsAddon(player, addonPropensity, buyinChips);
}
```

**Note:** V1Algorithm and V2Algorithm are in the same package (`core.ai`), so package-private visibility works.

---

### Step 3: Refactor ServerAIProvider for Skill Routing

**File:** `code/pokerserver/src/main/java/.../server/ServerAIProvider.java`

**Goal:** Create the right AI type per player based on skill level. Manage per-player contexts.

**3a. Accept skill level mapping:**

```java
public class ServerAIProvider implements PlayerActionProvider {
    private final Map<Integer, PurePokerAI> playerAIs = new ConcurrentHashMap<>();
    private final Map<Integer, AIContext> playerContexts = new ConcurrentHashMap<>();
    private final GameTable table;
    private final TournamentContext tournament;
    private GameHand currentHand; // Updated per hand

    public ServerAIProvider(List<GamePlayerInfo> players,
                            Map<Integer, Integer> skillLevels,
                            GameTable table,
                            TournamentContext tournament) {
        this.table = table;
        this.tournament = tournament;
        initializeAIs(players, skillLevels);
    }
}
```

**3b. Skill-level AI routing:**

| Skill | AI Type | Context Type |
|-------|---------|-------------|
| 1-2 | TournamentAI | ServerAIContext |
| 3-4 | V1Algorithm | ServerAIContext |
| 5-7 | V2Algorithm | ServerV2AIContext + ServerStrategyProvider |

```java
private void initializeAIs(List<GamePlayerInfo> players, Map<Integer, Integer> skillLevels) {
    for (GamePlayerInfo player : players) {
        if (!player.isHuman()) {
            int skill = skillLevels.getOrDefault(player.getID(), 3); // Default: V1
            PurePokerAI ai = createAI(skill, player);
            AIContext ctx = createContext(skill, player);
            playerAIs.put(player.getID(), ai);
            playerContexts.put(player.getID(), ctx);
        }
    }
}

private PurePokerAI createAI(int skillLevel, GamePlayerInfo player) {
    long seed = player.getID() * 31L + System.nanoTime();
    return switch (skillLevel) {
        case 1, 2 -> new TournamentAI();
        case 3, 4 -> new V1Algorithm(seed, mapToV1Skill(skillLevel));
        case 5, 6, 7 -> new V2Algorithm();
        default -> new V1Algorithm(seed, V1Algorithm.AI_MEDIUM);
    };
}

private AIContext createContext(int skillLevel, GamePlayerInfo player) {
    if (skillLevel >= 5) {
        ServerStrategyProvider strategy = new ServerStrategyProvider(
            String.valueOf(player.getID()));
        return new ServerV2AIContext(table, currentHand, tournament, player, strategy);
    }
    return new ServerAIContext(table, currentHand, tournament, player);
}
```

**3c. Hand lifecycle update:**

```java
public void onNewHand(GameHand hand) {
    this.currentHand = hand;
    for (AIContext ctx : playerContexts.values()) {
        if (ctx instanceof ServerAIContext sac) {
            sac.setCurrentHand(hand);
        }
    }
}
```

**3d. Action routing with per-player context:**

```java
@Override
public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
    if (player.isHuman()) return null;

    PurePokerAI ai = playerAIs.get(player.getID());
    AIContext ctx = playerContexts.get(player.getID());
    if (ai == null || ctx == null) return PlayerAction.fold();

    try {
        return ai.getAction(player, options, ctx);
    } catch (Exception e) {
        // Log + fallback to fold
        return PlayerAction.fold();
    }
}
```

**3e. Expose rebuy/addon decisions:**

```java
public boolean wantsRebuy(GamePlayerInfo player) {
    PurePokerAI ai = playerAIs.get(player.getID());
    AIContext ctx = playerContexts.get(player.getID());
    if (ai == null || ctx == null) return false;
    return ai.wantsRebuy(player, ctx);
}

public boolean wantsAddon(GamePlayerInfo player) {
    PurePokerAI ai = playerAIs.get(player.getID());
    AIContext ctx = playerContexts.get(player.getID());
    if (ai == null || ctx == null) return false;
    return ai.wantsAddon(player, ctx);
}
```

---

### Step 4: Update Tests

**4a. Update V2AlgorithmTest** (`code/pokergamecore/src/test/java/.../core/ai/V2AlgorithmTest.java`):
- Update tests for new V2Algorithm constructor (propensity fields)
- Update wantsRebuy/wantsAddon tests to verify delegation to V1 logic
- Test: low propensity → rebuy/addon, high propensity → no rebuy/addon, max rebuys → no rebuy

**4b. Create ServerAIContextTest** (`code/pokerserver/src/test/java/.../server/ServerAIContextTest.java`):
- Test all newly implemented methods:
  - `getBettingRound()` returns hand round
  - `getHoleCards()` returns cards for own player, null for others (security)
  - `getCommunityCards()` returns community cards
  - `getPotSize()` returns pot
  - `getAmountToCall()` returns call amount
  - `getNumActivePlayers()` returns players with cards
  - `isButton/isSmallBlind/isBigBlind` correctly identify positions
  - `getPosition()` returns correct relative position
  - `setCurrentHand()` updates hand reference
- Test null safety (null hand, null table)

**4c. Create ServerAIProviderTest** (`code/pokerserver/src/test/java/.../server/ServerAIProviderTest.java`):
- Test skill routing: verify correct AI type created per skill level
- Test per-player context isolation (V2 player gets V2AIContext, V1 gets base)
- Test `onNewHand()` propagates to all contexts
- Test `wantsRebuy()` / `wantsAddon()` delegation
- Test human player returns null
- Test missing AI falls back to fold
- Test AI exception falls back to fold

**4d. Integration test** — Add to `HeadlessGameRunnerTest.java` or new test:
- Run a complete tournament with mixed AI types (TournamentAI + V1 + V2)
- Verify game completes without errors
- Verify all AI types produce actions (not just fold)

---

## Files Changed Summary (Steps 1-4)

| File | Change Type | Description |
|------|-------------|-------------|
| `pokerserver/.../ServerAIContext.java` | Modify | Implement ~12 stub methods; make currentHand mutable; pull up position logic |
| `pokerserver/.../ServerV2AIContext.java` | Modify | Remove overridden position methods (now inherited) |
| `pokerserver/.../ServerAIProvider.java` | Rewrite | Skill routing, per-player contexts, hand lifecycle, rebuy/addon |
| `pokergamecore/.../V1Algorithm.java` | Modify | Make 2 static methods package-private (private → default) |
| `pokergamecore/.../V2Algorithm.java` | Modify | Add propensity fields, constructors, implement wantsRebuy/wantsAddon |
| `pokergamecore/.../V2AlgorithmTest.java` | Modify | Update constructor tests, rebuy/addon tests |
| `pokerserver/.../ServerAIContextTest.java` | **New** | Unit tests for context implementations |
| `pokerserver/.../ServerAIProviderTest.java` | **New** | Unit tests for skill routing and lifecycle |

---

## Verification

```bash
# After Steps 1-4 (core integration):
cd code && mvn test -pl pokergamecore -Dtest=V2AlgorithmTest
cd code && mvn test -pl pokerserver
cd code && mvn test -P dev
cd code && mvn verify -pl pokergamecore  # enforcer plugin checks

# After Step 5 (quality improvements):
cd code && mvn test -pl pokerserver  # all new tests
cd code && mvn test -P dev           # full regression
```

---

### Step 5: AI Quality Improvements

After Steps 1-4, the AI is functional but plays suboptimally because several context methods return neutral defaults. This step implements them for higher-quality play.

**Key dependency note:** `pokerserver` depends on `poker` module, so `HandPotential`, `HandSelectionScheme` etc. are directly usable. `PocketRanks`, `SimpleBias`, `PocketOdds` are already in `pokergamecore`.

#### 5a. Implement remaining ServerAIContext stubs

**File:** `ServerAIContext.java`

| Method | Implementation | Notes |
|--------|---------------|-------|
| `getAmountBetThisRound(player)` | Track via `onPlayerAction()` callback | Need to add action tracking to ServerAIContext |
| `getLastBetAmount()` | Track via `onPlayerAction()` callback | Set on each bet/raise action |
| `getLastActionInRound(player, round)` | Track via `onPlayerAction()` callback per player per round | Enables limper detection in V1 |

**Approach:** Add `onPlayerAction(GamePlayerInfo player, int action, int amount, int round)` method to ServerAIContext. ServerAIProvider (or TournamentEngine) calls this after each player acts. Context stores `Map<Integer, int[]> lastActionPerRound` (playerID → action-per-round) and per-round bet amounts.

```java
// Add tracking fields
private final Map<Integer, int[]> playerActionsPerRound = new HashMap<>();
private int lastBetAmount = 0;
private final Map<Integer, int[]> playerBetsPerRound = new HashMap<>();

public void onPlayerAction(GamePlayerInfo player, int action, int amount, int round) {
    playerActionsPerRound
        .computeIfAbsent(player.getID(), k -> new int[4])
        [round] = action;
    if (action == HandAction.ACTION_BET || action == HandAction.ACTION_RAISE) {
        lastBetAmount = amount;
    }
    playerBetsPerRound
        .computeIfAbsent(player.getID(), k -> new int[4])
        [round] += amount;
}

// Reset per hand
public void setCurrentHand(GameHand hand) {
    this.currentHand = hand;
    playerActionsPerRound.clear();
    playerBetsPerRound.clear();
    lastBetAmount = 0;
}
```

**Also requires:** ServerAIProvider to call `context.onPlayerAction()` when notified of player actions by the game engine. This hooks into whatever callback mechanism TournamentEngine provides (GameEventBus or direct call).

#### 5b. HandSelectionScheme in ServerStrategyProvider

**Status:** ✅ COMPLETE (using embedded .dat file data)

**Decision:** Extract and embed hand strength data from HandSelectionScheme .dat files.

**Rationale:**
- Provides Doug Donohoe's exact hand rankings without file I/O
- Avoids HandSelectionScheme's desktop framework dependencies (BaseProfile, ConfigManager)
- No runtime file access or resource path configuration needed
- Maintains fidelity to original hand strength calculations

**Implementation:**
- Created `EmbeddedHandStrength` nested class in `ServerStrategyProvider.java`
- Extracted data from all four .dat files:
  - `handselection.0994.dat` - Heads-up (2 players)
  - `handselection.1000.dat` - Very short (3-4 players)
  - `handselection.0995.dat` - Short-handed (5-6 players)
  - `handselection.0996.dat` - Full table (7-10 players)
- Implemented parser for hand notation (pairs, ranges, suited/offsuit)
- `getHandStrength()` now uses embedded data with Sklansky fallback
- All 15 ServerStrategyProviderTest tests pass

#### 5c. HandPotential draw counts in ServerV2AIContext

**File:** `ServerV2AIContext.java`

Uses: `poker/.../HandPotential.java` (accessible from pokerserver)

**Implement these 4 methods:**

| Method | Implementation |
|--------|---------------|
| `getNutFlushCount(Hand pocket, Hand community)` | Create HandPotential, call relevant method |
| `getNonNutFlushCount(Hand pocket, Hand community)` | Create HandPotential, call relevant method |
| `getNutStraightCount(Hand pocket, Hand community)` | Create HandPotential, call relevant method |
| `getNonNutStraightCount(Hand pocket, Hand community)` | Create HandPotential, call relevant method |

**Need to check:** HandPotential's API to see exactly which methods provide flush/straight draw counts. May need to read HandPotential.java during implementation.

#### 5d. PocketRanks-based hand strength in ServerV2AIContext

**File:** `ServerV2AIContext.java`

Uses: `pokergamecore/.../core/ai/PocketRanks.java`, `SimpleBias.java`, `PocketOdds.java`

**Implement:**

| Method | Implementation |
|--------|---------------|
| `getRawHandStrength(Hand pocket, Hand community)` | `PocketRanks.getRawHandStrength(pocket, community, numOpponents)` |
| `getBiasedRawHandStrength(int seat, Hand community)` | `SimpleBias.getBiasedStrength(pocket, community, opponentModel)` |
| `getBiasedEffectiveHandStrength(int seat, Hand community)` | `PocketOdds.getEHS(pocket, community, opponentModel)` |
| `getApparentStrength(int seat, Hand community)` | Derive from biased EHS and board texture |

**Need to check:** PocketRanks/SimpleBias/PocketOdds API signatures and required initialization during implementation.

#### 5e. Opponent Modeling

**New file:** `code/pokerserver/src/main/java/.../server/ServerOpponentTracker.java`
**Modify:** `ServerV2AIContext.java` — wire in real opponent models

**Goal:** Track per-player statistics across hands to implement `V2OpponentModel` with real data.

**Tracked stats:**
- Hands played count
- Pre-flop: raise %, fold %, call %, limp % (per position category)
- Post-flop per round: act %, check-fold %, open %, raise %
- Bet-fold frequency, overbet frequency

**Data structure:**
```java
public class ServerOpponentTracker {
    private final Map<Integer, MutableOpponentStats> playerStats = new ConcurrentHashMap<>();

    public void onPlayerAction(GamePlayerInfo player, int action, int amount,
                               int round, int positionCategory) {
        playerStats.computeIfAbsent(player.getID(), k -> new MutableOpponentStats())
                   .recordAction(action, amount, round, positionCategory);
    }

    public V2OpponentModel getModel(int playerId) {
        MutableOpponentStats stats = playerStats.get(playerId);
        return stats != null ? stats : new StubV2OpponentModel();
    }
}
```

**Integration:**
- ServerAIProvider creates one `ServerOpponentTracker` shared across all player contexts
- Pass tracker to ServerV2AIContext constructor
- `getOpponentModel(player)` returns `tracker.getModel(player.getID())`
- Also update ServerAIContext for V1's `getOpponentRaiseFrequency` / `getOpponentBetFrequency`

**Also implement in ServerV2AIContext:**
- `getChipCountAtStart(player)` — tracker records chip counts at hand start
- `getHandsBeforeBigBlind(player)` — calculate from seat and button position
- `getConsecutiveHandsUnpaid(player)` — tracker records payment history

#### 5f. Additional ServerV2AIContext improvements

| Method | Current | Improved |
|--------|---------|----------|
| `getRemainingAverageHohM()` | Returns table average | Query tournament for total remaining players/chips |
| `getStartingOrder(player)` | Returns seat number | Calculate actual pre-flop betting order |
| `getPostFlopPositionCategory(player)` | Delegates to starting position | Calculate post-flop order (relative to dealer) |
| `isLimit()` | Returns false | Check tournament profile for game type |

---

### Step 5 Testing

**5g. Tests for Step 5 implementations:**

- **ServerOpponentTrackerTest** — Unit test stat accumulation and model retrieval
- **ServerStrategyProviderTest** — Test HandSelectionScheme loading (if data files accessible in test)
- **ServerV2AIContextTest** — Expand to test PocketRanks/SimpleBias/PocketOdds integration
- **ServerAIContextTest** — Test action tracking (onPlayerAction, getLastActionInRound, getAmountBetThisRound)
- **Integration test** — Full tournament with V2 players using real opponent models, verify improved play quality

---

### Step 5 Files Summary

| File | Change Type | Description |
|------|-------------|-------------|
| `pokerserver/.../ServerAIContext.java` | Modify | Add onPlayerAction(), action/bet tracking |
| `pokerserver/.../ServerV2AIContext.java` | Modify | Implement HandPotential, PocketRanks, SimpleBias, PocketOdds methods |
| `pokerserver/.../ServerStrategyProvider.java` | Modify | Replace Sklansky with HandSelectionScheme |
| `pokerserver/.../ServerOpponentTracker.java` | **New** | Per-player stat tracking and V2OpponentModel implementation |
| `pokerserver/.../ServerAIProvider.java` | Modify | Create and wire ServerOpponentTracker |
| `pokerserver/.../ServerOpponentTrackerTest.java` | **New** | Unit tests |
| `pokerserver/.../ServerStrategyProviderTest.java` | **New** | Unit tests |

---

## Implementation Order

| Step | Description | Blocks | Est. Effort |
|------|-------------|--------|-------------|
| **1** | ServerAIContext stub implementations | None | M |
| **2** | V2Algorithm rebuy/addon | None (parallel with Step 1) | S |
| **3** | ServerAIProvider skill routing | Steps 1, 2 | M |
| **4** | Tests for Steps 1-3 | Step 3 | M |
| **5a** | Action/bet tracking | Step 3 | S |
| **5b** | HandSelectionScheme loading | None | S |
| **5c** | HandPotential draw counts | None | S |
| **5d** | PocketRanks-based hand strength | None | M |
| **5e** | Opponent modeling | Step 3 | L |
| **5f** | Remaining V2 context improvements | Step 5e | S |
| **5g** | Tests for Step 5 | Steps 5a-5f | M |

Steps 1 and 2 can be done in parallel. Steps 5a-5d can be done in parallel. Step 5e is the largest single item.
