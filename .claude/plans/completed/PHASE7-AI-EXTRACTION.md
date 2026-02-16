# Phase 7: Extract AI to pokergamecore

**Status:** active — V1/V2 extraction complete, ServerAIProvider next

**Last Updated:** 2026-02-16

### ✅ Completed
- **Phase 7A: Core AI Infrastructure** - PurePokerAI and AIContext interfaces
- **Phase 7B: V1 Algorithm Extraction** - V1Algorithm in pokergamecore
- **Phase 7C: V2 Algorithm Extraction** - V2Algorithm + PureRuleEngine in pokergamecore
- **Phase 7E: TournamentAI** - Production implementation with M-ratio strategy

### 📋 Remaining
- **Phase 7D: ServerAIProvider Integration** — Detailed plan: [PHASE7D-SERVER-AI-PROVIDER.md](../PHASE7D-SERVER-AI-PROVIDER.md)
  - Steps 1-4: Core integration (skill routing, context implementations, rebuy/addon, tests)
  - Step 5: AI quality improvements (opponent modeling, HandPotential, PocketRanks, HandSelectionScheme)

### ➡️ Next
- **Milestone 1: Server Game Engine Foundation** — Detailed plan: [M1-SERVER-GAME-ENGINE.md](../M1-SERVER-GAME-ENGINE.md)
  - Master plan: [SERVER-HOSTED-GAME-ENGINE.md](../SERVER-HOSTED-GAME-ENGINE.md)
  - Depends on Phase 7D completion

## Context

Currently, all poker AI logic (V1Player, V2Player) lives in the `poker` module and depends on Swing classes (PokerPlayer, PokerTable, HoldemHand). This prevents server-hosted games from using intelligent AI.

**The Problem:**
```
poker module (Swing-based)
├── PokerAI (abstract base)
│   ├── Depends on: PokerPlayer, PokerTable, HoldemHand
│   ├── Depends on: PokerGame, GameEngine
│   └── Cannot run in headless server environment
├── V1Player extends PokerAI (~800 lines)
└── V2Player extends PokerAI (~1200 lines)
```

**The Goal:**
```
pokergamecore (Swing-free)
├── PurePokerAI interface
│   ├── PlayerAction getAction(GamePlayerInfo, ActionOptions)
│   ├── boolean wantsRebuy(GamePlayerInfo)
│   └── boolean wantsAddon(GamePlayerInfo)
├── V1Algorithm implements PurePokerAI
├── V2Algorithm implements PurePokerAI
└── TournamentAI implements PurePokerAI (simple, already implemented in tests)

poker module (Swing client - backward compatible)
├── PokerAI wrapper (delegates to pokergamecore)
├── V1Player (wraps V1Algorithm)
└── V2Player (wraps V2Algorithm)
```

---

## Prerequisites

**Must complete first:**
- ✅ Phase 0-4: pokergamecore module created with interfaces
- ✅ Phase 5: Server integration proof-of-concept
- ⏳ Phase 3: Player action integration (SwingPlayerActionProvider)
- ⏳ TournamentContext has getBigBlind/getSmallBlind/getAnte methods (✅ DONE)

**Proof of Concept Complete:**
- ✅ Tournament-aware AI implemented in HeadlessGameRunnerTest
- ✅ Demonstrates 10-50x faster gameplay than random AI
- ✅ Works with pure pokergamecore interfaces
- ✅ No Swing dependencies required

---

## Completed Prerequisite Work

### 2026-02-15: Tournament AI POC & Interface Extensions

**Session:** `.claude/sessions/2026-02-15-tournament-ai-poc.md`
**Review:** `.claude/reviews/main-tournament-ai-poc.md`

#### 1. ✅ Extended TournamentContext Interface
**Files Modified:**
- `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/TournamentContext.java`
  - Added `int getSmallBlind(int level)` - Query small blind for any level
  - Added `int getBigBlind(int level)` - Query big blind for any level
  - Added `int getAnte(int level)` - Query ante for any level

**Implementations:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java` - Delegates to TournamentProfile
- `code/pokergamecore/src/test/java/.../TournamentEngineTest.java` - Added to StubTournamentContext
- `code/pokerserver/src/test/java/.../HeadlessGameRunnerTest.java` - Implemented in test contexts

**Why:** AI needs to query blind structure at any level to calculate M-ratio (stack pressure) for tournament decisions.

#### 2. ✅ Tournament-Aware AI Implementation (Proof of Concept)
**File:** `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/HeadlessGameRunnerTest.java:66-149`

**Features:**
- M-ratio based decision making: `stack / (smallBlind + bigBlind + 10*ante)`
- Three strategic zones:
  - Critical (M < 5): Push/fold strategy (70% all-in or fold)
  - Danger (5 ≤ M < 10): Aggressive play (frequent raises)
  - Comfortable (M ≥ 10): Balanced play (weighted actions)
- Zero Swing dependencies
- 10-50x faster than random AI

**Performance:**
- 10-player tournament: ~912 iterations (vs 5000+ random)
- 6-player rapid blinds: ~10k iterations (vs 500k+ random)
- Deep stack: ~381 iterations (vs massive timeout with random)

#### 3. ✅ Build Configuration Updates
**pokergamecore/pom.xml:**
- Added maven-enforcer-plugin (34 lines)
- Bans Swing/AWT imports in compile scope
- Prevents accidental Swing dependencies

**pokerserver/pom.xml:**
- Added pokergamecore dependency (5 lines)
- Enables headless game runner tests

#### 4. ✅ Code Review Completed
**Review Status:** APPROVED (blocking + non-blocking issues resolved)

**Blocking Issue Fixed:**
- StubTournamentContext missing new interface methods → Added 3 stub implementations

**Non-Blocking Issues Fixed:**
- Emoji usage in test output → Replaced with plain text ("[OK]", "[INFO]")
- M-ratio approximation → Improved from `bigBlind * 1.5` to accurate `smallBlind + bigBlind + 10*ante`
- Handoff documentation → Corrected pom.xml change descriptions
- Workflow violation → Acknowledged (work done on main instead of worktree)

**Verification:**
- pokergamecore: 202/202 tests passing
- HeadlessGameRunnerTest: 9/13 passing, 4 disabled (3 stress tests for Phase 7, 1 for Phase 3)
- All builds successful

#### 5. ✅ Stress Tests Status
**Disabled (will re-enable with Phase 7 full AI):**
- `deepStackTournament` - 100 BB starting stacks exceed iteration limits
- `rapidBlindProgressionTournament` - Rapid blind increases create complex scenarios
- `frequentAllInSituations` - Shallow stacks with all-in pressure

**Note:** These are valid stress tests that demonstrate architectural limits. Tournament AI is 10-50x faster than random, but deep stacks still require full V1/V2 AI for optimal performance.

---

## Architecture Overview

### Current AI Flow (Phase 6 - Swing-dependent)
```
Player needs to act
  ↓
TournamentDirector calls player.getPokerAI()
  ↓
PokerAI.getAction() → inspects PokerPlayer, PokerTable, HoldemHand (Swing objects)
  ↓
V1Player/V2Player logic (hand evaluation, position, pot odds, opponent modeling)
  ↓
Returns HandAction
```

### Target AI Flow (Phase 7 - Swing-free)
```
Player needs to act
  ↓
TournamentEngine calls actionProvider.getAction(player, options)
  ↓
ServerAIProvider (or SwingAIProvider) gets PurePokerAI instance
  ↓
V1Algorithm/V2Algorithm.getAction(GamePlayerInfo, ActionOptions)
  ↓
Uses only pokergamecore interfaces (no Swing!)
  ↓
Returns PlayerAction
```

---

## Phase 7A: Extract Core AI Infrastructure (Est: 2-3 days)

### Goal
Move AI decision logic into pokergamecore without breaking existing Swing client.

### New Interfaces in pokergamecore

**`PurePokerAI.java`** - Swing-free AI interface:
```java
package com.donohoedigital.games.poker.core;

/**
 * Pure poker AI interface with no Swing dependencies.
 * Suitable for both server-hosted games and desktop client.
 */
public interface PurePokerAI {
    /**
     * Decide what action to take.
     *
     * @param player Player making the decision
     * @param options Available actions and constraints
     * @return The action to take
     */
    PlayerAction getAction(GamePlayerInfo player, ActionOptions options);

    /**
     * Decide whether to rebuy when eliminated.
     *
     * @param player Player considering rebuy
     * @return true if player wants to rebuy
     */
    boolean wantsRebuy(GamePlayerInfo player);

    /**
     * Decide whether to take addon chips.
     *
     * @param player Player considering addon
     * @return true if player wants addon
     */
    boolean wantsAddon(GamePlayerInfo player);
}
```

**`AIContext.java`** - Game state for AI decisions:
```java
package com.donohoedigital.games.poker.core;

/**
 * Provides AI with read-only access to game state for decision making.
 */
public interface AIContext {
    GameTable getTable();
    GameHand getCurrentHand();
    TournamentContext getTournament();

    // Hand evaluation
    int evaluateHand(Card[] pocket, Card[] community);

    // Position queries
    boolean isButton(GamePlayerInfo player);
    boolean isSmallBlind(GamePlayerInfo player);
    boolean isBigBlind(GamePlayerInfo player);

    // Pot queries
    int getPotSize();
    int getAmountToCall(GamePlayerInfo player);

    // Player queries
    int getNumActivePlayers();
    int getNumPlayersYetToAct();
}
```

---

## Phase 7B: Extract V1 AI Algorithm (Est: 2-3 days)

### Goal
Move V1Player logic into V1Algorithm (pokergamecore), keep V1Player as wrapper.

### Current V1Player Structure
```
V1Player (~800 lines) in poker module
├── Uses SklankskyGroup for preflop hand strength
├── Uses position-based aggression
├── Uses pot odds calculations
├── Uses opponent modeling (OpponentModel class)
├── Depends on: PokerPlayer, PokerTable, HoldemHand
```

### Target V1Algorithm Structure
```java
// pokergamecore
public class V1Algorithm implements PurePokerAI {
    private final OpponentModel opponentModel;

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        AIContext context = ... // passed in or built from player

        // Extract V1Player decision logic here
        // Use SklankskyGroup, position, pot odds
        // Access game state via AIContext (not Swing objects)

        return decideAction(player, options, context);
    }
}

// poker module (wrapper for backward compatibility)
public class V1Player extends PokerAI {
    private final V1Algorithm algorithm = new V1Algorithm();

    @Override
    public PlayerAction getAction(boolean quick) {
        // Convert PokerPlayer → GamePlayerInfo
        GamePlayerInfo playerInfo = adaptPlayer(getPokerPlayer());
        ActionOptions options = buildOptions();

        // Delegate to pure algorithm
        return algorithm.getAction(playerInfo, options);
    }
}
```

### Migration Strategy
1. **Copy** V1Player logic to V1Algorithm in pokergamecore
2. **Replace** Swing dependencies with interface calls:
   - `PokerPlayer` → `GamePlayerInfo`
   - `PokerTable` → `GameTable`
   - `HoldemHand` → `GameHand`
3. **Extract** helper methods (hand evaluation, position logic) into AIContext
4. **Modify** V1Player to delegate to V1Algorithm
5. **Test** that existing desktop client still works identically

---

## Phase 7C: Extract V2 AI Algorithm (Est: 2-3 days)

### Goal
Move V2Player logic into V2Algorithm (pokergamecore).

### Current V2Player Structure
```
V2Player (~1200 lines) in poker module
├── All V1 features PLUS:
├── Advanced opponent modeling
├── Bluffing strategy
├── Bet sizing optimization
├── Position-aware continuation betting
├── Depends on: PokerPlayer, PokerTable, HoldemHand
```

### Target V2Algorithm
```java
// pokergamecore
public class V2Algorithm implements PurePokerAI {
    private final OpponentModel opponentModel;

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        // Extract V2Player decision logic
        // Advanced features: bluffing, bet sizing, c-betting

        return decideAction(player, options, context);
    }
}
```

**Same migration strategy as V1**, plus:
- Extract bluffing logic
- Extract bet sizing calculations
- Extract continuation bet logic

---

## Phase 7D: Integrate with Server (Est: 1-2 days)

### Goal
Enable server-hosted games to use V1/V2 AI without Swing dependencies.

### New Class in pokerserver

**`ServerAIProvider.java`** - AI provider for server games:
```java
public class ServerAIProvider implements PlayerActionProvider {
    private final Map<Integer, PurePokerAI> playerAIs = new HashMap<>();

    public ServerAIProvider(List<GamePlayerInfo> players) {
        for (GamePlayerInfo player : players) {
            if (player.isComputer()) {
                // Choose AI based on skill level
                PurePokerAI ai = createAI(player.getSkillLevel());
                playerAIs.put(player.getID(), ai);
            }
        }
    }

    private PurePokerAI createAI(int skillLevel) {
        return switch (skillLevel) {
            case 1, 2 -> new TournamentAI(skillLevel); // Simple AI
            case 3, 4 -> new V1Algorithm();             // Moderate AI
            case 5, 6, 7 -> new V2Algorithm();          // Advanced AI
            default -> new V1Algorithm();
        };
    }

    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        PurePokerAI ai = playerAIs.get(player.getID());
        return ai != null ? ai.getAction(player, options) : PlayerAction.fold();
    }
}
```

### Update ServerTournamentDirector
```java
public class ServerTournamentDirector implements Runnable {
    private final TournamentEngine engine;
    private final ServerAIProvider aiProvider;  // NEW

    public ServerTournamentDirector(TournamentContext tournament) {
        GameEventBus eventBus = new GameEventBus();
        this.aiProvider = new ServerAIProvider(tournament.getPlayers());  // NEW
        this.engine = new TournamentEngine(eventBus, aiProvider);
    }

    @Override
    public void run() {
        while (!game.isComplete()) {
            TableProcessResult result = engine.processTable(table, game, true, true);
            // AI decisions now handled by ServerAIProvider!
            dispatchResult(result);
        }
    }
}
```

---

## Phase 7E: Add Simple AI Variants (Est: 1 day)

### Goal
Provide multiple AI difficulty levels for variety.

### AI Hierarchy
```
PurePokerAI (interface)
├── TournamentAI (simple, M-ratio based) ← Already implemented in tests!
├── AggressiveAI (high betting frequency)
├── ConservativeAI (tight play, high folding)
├── V1Algorithm (moderate skill)
└── V2Algorithm (advanced skill)
```

**TournamentAI** - Promote from test code to production:
```java
// pokergamecore/src/main/java/.../core/ai/TournamentAI.java
public class TournamentAI implements PurePokerAI {
    private final Random random;

    // Current implementation from HeadlessGameRunnerTest.createTournamentAI()
    // Works well, 10-50x faster than random play
}
```

---

## Testing Strategy

### Unit Tests
- **V1AlgorithmTest** - Test all V1 decision paths without Swing
- **V2AlgorithmTest** - Test all V2 decision paths without Swing
- **TournamentAITest** - Test M-ratio calculations and decision zones
- **AIContextTest** - Test game state queries

### Integration Tests
- **ServerAIIntegrationTest** - Run headless tournament with all AI types
- **PokerGameAITest** - Verify desktop client AI still works (backward compatibility)

### Comparison Tests
- **V1Player vs V1Algorithm** - Verify identical decisions
- **V2Player vs V2Algorithm** - Verify identical decisions

### Stress Tests (re-enable from HeadlessGameRunnerTest)
- ✅ deepStackTournament
- ✅ rapidBlindProgressionTournament
- ✅ frequentAllInSituations

**Expected Results:**
- All stress tests should pass with V1/V2 algorithms
- Completion times should be 50-100x faster than random AI
- No iteration limit failures

---

## Dependencies to Extract

### Must Move to pokergamecore
1. **SklankskyGroup** - Preflop hand strength (currently in poker module)
2. **OpponentModel** - Player modeling (currently in poker module)
3. **HandEvaluator** - Already in pokerengine (reuse)
4. **PotOddsCalculator** - Extract from AI logic

### Can Stay in poker module
- **GUI-related AI features** (AdvisorInfoDialog, DashboardAdvisor)
- **Human player helpers** (hint system, analysis tools)

---

## Backward Compatibility

### Must Preserve
- ✅ Existing save/load format (DataMarshal)
- ✅ Online multiplayer protocol
- ✅ Desktop client AI behavior (identical decisions)
- ✅ Tournament profile compatibility

### Testing Strategy
1. Run full regression suite before and after
2. Compare AI decisions in identical scenarios
3. Verify online games work with mixed Swing/pure clients

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| AI behavior changes subtly | HIGH | Comparison tests, record/replay scenarios |
| Performance degradation | MEDIUM | Benchmark before/after, optimize hot paths |
| Breaking desktop client | HIGH | Keep wrappers, thorough integration testing |
| Complex OpponentModel extraction | MEDIUM | Extract incrementally, test at each step |

---

## Success Criteria

✅ Server can host games with V1/V2 AI (no Swing)
✅ Desktop client AI works identically (backward compatible)
✅ All 3 stress tests pass (deepStack, rapidBlind, frequentAllIn)
✅ AI makes identical decisions in comparison tests
✅ TournamentAI available as simple fallback option
✅ All existing tests pass

---

## Estimated Total Effort

| Phase | Days | Risk |
|-------|------|------|
| 7A: Core infrastructure | 2-3 | Medium |
| 7B: Extract V1 | 2-3 | Medium |
| 7C: Extract V2 | 2-3 | Medium-High |
| 7D: Server integration | 1-2 | Low |
| 7E: Simple AI variants | 1 | Low |
| **TOTAL** | **8-13 days** | **Medium** |

---

## Notes

- All prerequisite work complete (see "Completed Prerequisite Work" section above)
- Architecture validated through headless tests
- TournamentAI POC demonstrates feasibility
- Clear path from current state to goal
- Ready to start Phase 7A once Phase 3 completes

---

## Related Files

**Already Completed (see "Completed Prerequisite Work"):**
- `code/pokerserver/src/test/java/.../HeadlessGameRunnerTest.java` - Tournament AI POC
- `code/pokergamecore/src/main/java/.../TournamentContext.java` - Blind query methods
- `code/poker/src/main/java/.../PokerGame.java` - Blind query implementations
- `.claude/sessions/2026-02-15-tournament-ai-poc.md` - Session documentation
- `.claude/reviews/main-tournament-ai-poc.md` - Code review (approved)

**To Be Created in Phase 7:**
- `pokergamecore/src/main/java/.../core/PurePokerAI.java`
- `pokergamecore/src/main/java/.../core/AIContext.java`
- `pokergamecore/src/main/java/.../core/ai/TournamentAI.java`
- `pokergamecore/src/main/java/.../core/ai/V1Algorithm.java`
- `pokergamecore/src/main/java/.../core/ai/V2Algorithm.java`
- `pokerserver/src/main/java/.../server/ServerAIProvider.java`
