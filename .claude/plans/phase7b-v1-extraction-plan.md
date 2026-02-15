# Phase 7B: V1 Algorithm Extraction Plan

**Created:** 2026-02-15
**Status:** Foundation Ready - Full Extraction Pending
**Estimated Effort:** 8-12 hours (dedicated session)

---

## Discovery Summary

**V1Player.java Analysis:**
- **Total Lines:** 1,614 lines (much larger than initial estimate!)
- **Core AI Logic:** ~800 lines
- **Support Methods:** ~800 lines
- **Location:** `code/poker/src/main/java/.../ai/V1Player.java`

**Key Dependencies Identified:**
1. **HoldemExpert** - Sklansky rank calculations
2. **HandInfoFaster** - Fast hand evaluation
3. **OpponentModel** - Player modeling/tracking
4. **PokerPlayer** - Swing-based player class
5. **HoldemHand** - Swing-based hand class
6. **PokerGame** - Swing-based game class
7. **TournamentProfile** - Tournament configuration
8. **HandAction** - Action constants
9. **DiceRoller** - Random number generation

---

## Architecture Overview

### Current V1Player Structure

```java
V1Player extends PokerAI {
    // Personality traits (set per player)
    - nTightFactor_     // 0-100, 100 = very tight
    - nBluffFactor_     // 0-100, 100 = lots of bluffing
    - nRebuyPropensity_ // 0-100
    - nAddonPropensity_ // 0-100

    // Transient state (set per action)
    - _hhand            // Current hand (HoldemHand)
    - _nToCall          // Amount to call
    - _hole             // Player's hole cards
    - _comm             // Community cards
    - _bettor, _raiser  // Who bet/raised
    - _potOdds          // Current pot odds
    - _skill            // AI difficulty (EASY/MEDIUM/HARD)

    // Main decision logic
    + getAction(boolean quick) -> PlayerAction
      ├─ getPreFlop() -> PlayerAction
      └─ getPostFlop() -> PlayerAction
          ├─ getFlop() -> PlayerAction
          ├─ getTurn() -> PlayerAction
          └─ getRiver() -> PlayerAction

    // Support logic
    + wantsRebuy() -> boolean
    + wantsAddon() -> boolean
    - getTightFactor() -> int
    - getBluffFactor() -> int
    - evaluateHand() -> various
}
```

### Target V1Algorithm Structure

```java
V1Algorithm implements PurePokerAI {
    // Constructor-injected dependencies
    private final Random random;
    private final int skillLevel;

    // Personality traits (immutable)
    private final int tightFactor;
    private final int bluffFactor;
    private final int rebuyPropensity;
    private final int addonPropensity;

    // Main interface methods
    + getAction(player, options, context) -> PlayerAction
      ├─ getPreFlopAction(...) -> PlayerAction
      └─ getPostFlopAction(...) -> PlayerAction
          ├─ getFlopAction(...) -> PlayerAction
          ├─ getTurnAction(...) -> PlayerAction
          └─ getRiverAction(...) -> PlayerAction

    + wantsRebuy(player, context) -> boolean
    + wantsAddon(player, context) -> boolean

    // Helper methods (all private)
    - getTightFactor(context) -> int
    - getBluffFactor(context) -> int
    - calculatePotOdds(...) -> float
    - evaluateHandStrength(...) -> int
    - getSklanskyRank(cards) -> int
}
```

---

## Extraction Strategy

### Phase 1: Setup & Dependencies (2-3 hours)

**1.1: Extract Sklansky Rankings**
- Create `SklankskyRanking.java` in pokergamecore
- Extract from `HoldemExpert.getSklanskyRank()`
- Pure static method: `int getRank(Card c1, Card c2)`

**1.2: Extract Hand Evaluation**
- Review `HandInfoFaster.java` (already in poker module)
- Determine if it can move to pokerengine or needs wrapper
- Create Swing-free hand evaluator

**1.3: Extract Opponent Modeling**
- Review `OpponentModel.java` (~200-300 lines estimated)
- Extract to pokergamecore if Swing-free
- If Swing-dependent, create `PureOpponentModel` interface

**1.4: Create Helper Classes**
- `PotOddsCalculator` - Pot odds calculations
- `PositionAnalyzer` - Position-based strategy adjustments

### Phase 2: Core Algorithm (4-6 hours)

**2.1: Create V1Algorithm Skeleton**
- ✅ DONE - Basic structure created
- Constructor with personality traits
- Stub implementations of PurePokerAI methods

**2.2: Extract Pre-Flop Logic** (~300 lines)
- `getPreFlop()` method
- Sklansky-based starting hand selection
- Position adjustments
- Stack size considerations
- All-in system for tight players

**2.3: Extract Post-Flop Logic** (~500 lines)
- `getPostFlop()` → dispatch to specific street
- `getFlop()` - Flop strategy
- `getTurn()` - Turn strategy
- `getRiver()` - River strategy
- Hand strength evaluation
- Drawing hand logic
- Pot odds calculations

**2.4: Extract Support Methods**
- Personality adjustments (tight/bluff factors)
- Bet sizing logic
- Check-raise detection
- Bluffing logic

### Phase 3: Integration (2-3 hours)

**3.1: Update V1Player (Wrapper)**
```java
public class V1Player extends PokerAI {
    private V1Algorithm algorithm;

    public void setPokerPlayer(PokerPlayer player) {
        super.setPokerPlayer(player);
        // Initialize algorithm with personality traits
        this.algorithm = new V1Algorithm(
            player.getName().hashCode(), // seed
            skillLevel
        );
    }

    public PlayerAction getAction(boolean quick) {
        // Convert Swing objects to pure interfaces
        GamePlayerInfo playerInfo = adaptPlayer(getPokerPlayer());
        ActionOptions options = buildOptions();
        AIContext context = buildContext();

        // Delegate to pure algorithm
        return algorithm.getAction(playerInfo, options, context);
    }
}
```

**3.2: Create Adapters**
- `PokerPlayer` → `GamePlayerInfo`
- `HoldemHand` → Action context
- `PokerGame` → `TournamentContext`

**3.3: Implement AIContext Methods**
- Complete ServerAIContext stub methods
- Position queries (button, blinds, early/mid/late)
- Pot queries (size, to call, last bet)
- Hand evaluation (rank, score, outs)

### Phase 4: Testing (2-3 hours)

**4.1: Unit Tests**
- V1AlgorithmTest - Test decision logic
- SklanskyRankingTest - Test hand rankings
- PotOddsCalculatorTest - Test calculations

**4.2: Comparison Tests**
- Run 1000 hands with V1Player
- Run same 1000 hands with V1Algorithm (via wrapper)
- Compare decisions - should be IDENTICAL
- Any differences are bugs

**4.3: Integration Tests**
- Server-hosted game with V1Algorithm
- Desktop client with V1Player (wrapper)
- Verify both work correctly

---

## Key Challenges & Solutions

### Challenge 1: HoldemHand Dependencies
**Problem:** V1Player heavily uses HoldemHand (Swing class)
**Solution:**
- Extract only the data needed (pot size, to call, etc.)
- Pass via AIContext instead of direct object access
- AIContext methods map to HoldemHand queries

### Challenge 2: State Management
**Problem:** V1Player uses 20+ instance variables prefixed with `_`
**Solution:**
- Pass as method parameters instead of instance state
- Create `DecisionContext` struct to group related values
- Keeps V1Algorithm stateless (thread-safe)

### Challenge 3: Personality Traits
**Problem:** Tight/bluff factors set per player, adjusted per action
**Solution:**
- Store base values in constructor
- Adjustment methods take context, return adjusted value
- Keeps algorithm immutable after construction

### Challenge 4: HandInfoFaster
**Problem:** Used for fast hand evaluation, may have Swing deps
**Solution:**
- Review HandInfoFaster implementation
- If Swing-free, move to pokerengine
- If Swing-dependent, extract pure logic to new class
- Use Hand evaluator already in pokerengine

### Challenge 5: Sklansky Rankings
**Problem:** Currently in HoldemExpert static method
**Solution:**
- Extract to standalone class in pokergamecore
- Simple 2-card lookup table (169 combinations)
- Pure function: no state, no dependencies

---

## File Structure

### New Files in pokergamecore
```
code/pokergamecore/src/main/java/.../core/ai/
├── V1Algorithm.java           [NEW] - Main AI implementation
├── SklankskyRanking.java      [NEW] - Hand ranking system
├── PotOddsCalculator.java     [NEW] - Pot odds utilities
└── PositionAnalyzer.java      [NEW] - Position-based adjustments

code/pokergamecore/src/main/java/.../core/ai/model/
└── OpponentModel.java         [MOVE] - Player modeling (if Swing-free)
```

### Modified Files in poker
```
code/poker/src/main/java/.../ai/
└── V1Player.java              [MODIFY] - Becomes wrapper, delegates to V1Algorithm
```

### Test Files
```
code/pokergamecore/src/test/java/.../ai/
├── V1AlgorithmTest.java       [NEW] - Unit tests
├── SklankskyRankingTest.java  [NEW] - Ranking tests
└── V1ComparisonTest.java      [NEW] - V1Player vs V1Algorithm comparison
```

---

## Current Status

### ✅ Completed
- Analysis of V1Player structure (1614 lines identified)
- Dependency identification (9 major dependencies)
- Architecture design (V1Algorithm + adapters)
- Extraction strategy documented
- V1Algorithm skeleton created

### ⏳ Next Steps (Dedicated Session)
1. Extract Sklansky rankings
2. Review/extract HandInfoFaster
3. Extract pre-flop logic
4. Extract post-flop logic
5. Create V1Player wrapper
6. Write comparison tests
7. Verify identical behavior

### 📊 Estimated Breakdown
- **Setup & Dependencies:** 2-3 hours
- **Core Algorithm:** 4-6 hours
- **Integration:** 2-3 hours
- **Testing:** 2-3 hours
- **Total:** 10-15 hours (full focused session or 2-3 partial sessions)

---

## Notes for Implementation

### DO
- ✅ Extract logic incrementally (one method at a time)
- ✅ Test each extraction (unit test before moving on)
- ✅ Keep V1Player working throughout (wrapper approach)
- ✅ Maintain exact same decision behavior
- ✅ Document any differences found

### DON'T
- ❌ Try to improve the AI while extracting
- ❌ Change decision logic "while we're at it"
- ❌ Skip testing (comparison tests are critical)
- ❌ Delete V1Player (becomes wrapper for backward compat)
- ❌ Rush the extraction (precision > speed)

### Testing Strategy
1. **Unit tests** - Test individual methods in isolation
2. **Comparison tests** - Same seed → same decisions
3. **Integration tests** - Both versions play full games
4. **Regression tests** - Existing tests still pass

---

## Success Criteria

✅ V1Algorithm compiles in pokergamecore (zero Swing dependencies)
✅ V1Player wrapper compiles and delegates correctly
✅ Comparison tests show IDENTICAL decisions (1000+ hands)
✅ Desktop client works with wrapped V1Player
✅ Server works with pure V1Algorithm
✅ All existing tests pass
✅ Code coverage ≥ 80% for new code

---

**Ready for dedicated extraction session.**
**Estimated: 10-15 hours of focused work.**
