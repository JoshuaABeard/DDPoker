# Phase 7C: V2 AI Algorithm Extraction to pokergamecore

**Created:** 2026-02-15
**Completed:** 2026-02-15
**Status:** COMPLETE
**Depends on:** Phase 7B (V1 Extraction) — COMPLETE

## Context

V2Player is the default AI in DD Poker, used by all standard player profiles. It was built on top of V1Player (inheritance: `V2Player → V1Player → PokerAI`) and uses a sophisticated rule-based engine with 15 outcomes, 46 decision factors, opponent modeling, and hand range weighting. Currently V2Player lives in the `poker` module, which depends on Swing/AWT through `GameEngine`/`GameContext`/`BasePhase`. This blocks server-side use.

Phase 7B successfully extracted V1Algorithm (~1,100 lines) to pokergamecore. V2 extraction follows the same pattern (PurePokerAI interface + AIContext abstraction) but is significantly more complex:

- **V2Player:** 1,715 lines (stateful: tilt, steal suspicion, pot raise tracking, hand strength caching)
- **RuleEngine:** 2,874 lines (46 factors, 15 outcomes, direct opponent model access)
- **Support classes:** ~3,100 lines total (PocketWeights, PocketRanks/Scores/Odds, OpponentModel, AIOutcome, BetRange, HandPotential, SimpleBias, trackers)
- **Total scope:** ~7,700 lines to analyze; ~5,500 lines of new/modified code across 8 phases

**Key finding:** None of these classes import Swing/AWT directly. The coupling is indirect — through `PokerPlayer`, `HoldemHand`, and `PlayerType` parameters. This means extraction is a matter of abstracting these interfaces, not rewriting algorithms.

---

## Architecture Overview

### Current Coupling (V2Player → Swing)

```
RuleEngine.execute(V2Player)
  ├── V2Player.getPokerPlayer() → PokerPlayer  [poker module]
  ├── PokerPlayer.getHoldemHand() → HoldemHand  [poker module]
  ├── V2Player.getStratFactor() → PlayerType    [poker module, uses GameEngine]
  ├── PocketWeights.getInstance(HoldemHand)     [singleton keyed by HoldemHand]
  └── opponent.getOpponentModel() → OpponentModel [accessed via PokerPlayer]
```

### Target Architecture (Swing-free)

```
PureRuleEngine.execute(V2Context)
  ├── V2Context extends AIContext               [pokergamecore]
  │   ├── Strategy factors via StrategyProvider [pokergamecore interface]
  │   ├── Opponent data via V2OpponentModel     [pokergamecore interface]
  │   ├── Hand strength via HandEvaluationContext [pokergamecore]
  │   └── All game state queries               [pokergamecore]
  ├── V2Algorithm implements PurePokerAI        [pokergamecore]
  │   ├── Stateful: tilt, steal suspicion, pot raise history
  │   └── Delegates to PureRuleEngine
  └── ServerV2Context / ClientV2Context         [pokerserver / poker]
```

### Dead Code Elimination

RuleEngine has two flags that are always constant:
- `NEWCODE = true` (always) — ~400 lines of `!NEWCODE` branches are dead code
- `USE_CONFIDENCE = false` (always) — additional dead code

During extraction, dead code branches will be **removed** to simplify the extracted code. This does not change behavior since these paths never execute. The original RuleEngine in `poker` module remains unchanged.

---

## Progress Tracking

### Phase 1: Foundation — Pure Utility Classes → pokergamecore
- [ ] Copy BooleanTracker.java to pokergamecore/core/ai/
- [ ] Copy FloatTracker.java to pokergamecore/core/ai/
- [ ] Copy PocketMatrixFloat.java to pokergamecore/core/ai/
- [ ] Copy PocketMatrixShort.java to pokergamecore/core/ai/
- [ ] Copy AIConstants.java to pokergamecore/core/ai/
- [ ] Copy SimpleBias.java to pokergamecore/core/ai/
- [ ] Update package declarations and imports
- [ ] Write unit tests for BooleanTracker and FloatTracker
- [ ] Verify: `mvn test-compile -P fast -pl pokergamecore -am`
- [ ] Commit

### Phase 2: Opponent Modeling → pokergamecore
- [ ] Create V2OpponentModel.java interface in pokergamecore
- [ ] Modify OpponentModel.java to implement V2OpponentModel
- [ ] Write tests for V2OpponentModel
- [ ] Verify build
- [ ] Commit

### Phase 3: Hand Evaluation — PocketScores/Ranks/Odds → pokergamecore
- [ ] Analyze HandInfoFaster.java and HandInfoFast.java dependencies
- [ ] Move HandInfoFaster.java to pokerengine (if Swing-free)
- [ ] Move/extract HandInfoFast.java constants to pokerengine
- [ ] Copy PocketScores.java to pokergamecore/core/ai/
- [ ] Copy PocketRanks.java to pokergamecore/core/ai/
- [ ] Copy PocketOdds.java to pokergamecore/core/ai/
- [ ] Write tests for PocketScores, PocketRanks, PocketOdds
- [ ] Verify build
- [ ] Commit

### Phase 4: Strategy Provider + V2AIContext Interface
- [ ] Create StrategyProvider.java interface in pokergamecore
- [ ] Create V2AIContext.java interface (extends AIContext) in pokergamecore
- [ ] Move + adapt AIOutcome.java to pokergamecore (remove Swing types)
- [ ] Move + adapt BetRange.java to pokergamecore (remove PokerPlayer)
- [ ] Write tests
- [ ] Verify build
- [ ] Commit

### Phase 5: PureRuleEngine → pokergamecore
- [ ] Copy RuleEngine.java → PureRuleEngine.java in pokergamecore
- [ ] Replace V2Player/PokerPlayer/HoldemHand with V2AIContext/GamePlayerInfo
- [ ] Replace OpponentModel with V2OpponentModel
- [ ] Replace PocketWeights with context methods
- [ ] Remove NEWCODE=false dead code branches
- [ ] Remove USE_CONFIDENCE=false dead code
- [ ] Add debug callback (Consumer<String>)
- [ ] Move HandPotential to pokergamecore (if Swing-free)
- [ ] Verify build
- [ ] Commit

### Phase 6: V2Algorithm → pokergamecore
- [ ] Create V2Algorithm.java implementing PurePokerAI
- [ ] Implement stateful lifecycle detection (fingerprinting)
- [ ] Extract computeOdds() from V2Player
- [ ] Extract getBiasedHandStrength() / updateFieldMatrix()
- [ ] Implement playerActed() for steal suspicion / overbet tracking
- [ ] Handle wantsRebuy() / wantsAddon() delegation
- [ ] Write V2AlgorithmTest
- [ ] Verify build
- [ ] Commit

### Phase 7: Server + Desktop Integration
- [ ] Create ServerV2AIContext (extends ServerAIContext, implements V2AIContext)
- [ ] Create ServerStrategyProvider (implements StrategyProvider)
- [ ] Extract PlayerTypeData for Swing-free strategy loading
- [ ] Modify V2Player to become thin wrapper around V2Algorithm
- [ ] Write ServerV2AIContextTest
- [ ] Verify all tests pass
- [ ] Commit

### Phase 8: Behavioral Parity Verification
- [ ] Write PureRuleEngineTest (known inputs → expected outcomes)
- [ ] Write V2ComparisonTest (V2Player vs V2Algorithm decision comparison)
- [ ] Run 100-hand tournament simulation with V2Algorithm
- [ ] Verify ≥50% code coverage on new code
- [ ] Final full test suite verification
- [ ] Commit

---

## Known Concerns

### 1. Strategy Factor Data Loading
PlayerType loads from `.dat` files using `SaveFile`/`BaseProfile` which depends on `GameEngine`. Server-side needs an alternative loading mechanism. This is the highest-risk item.

### 2. HandSelectionScheme
V2Player uses `HandSelectionScheme` (loaded per table size) for pre-flop hand strength. Needs to be available in pokergamecore or its data loaded separately.

### 3. PocketWeights Complexity
PocketWeights (715 lines) is a singleton keyed by `HoldemHand` with complex hand history processing. **Recommendation:** Make it a V2AIContext responsibility.

### 4. Bad Beat Scoring / Steam
`computeBadBeatScore()` uses `HoldemSimulator` with full Monte Carlo. **Recommend deferring** — server AI uses 0 steam initially.

### 5. Serialization (marshal/demarshal)
V2Player serializes potRaised_, stealSuspicion_, dSteam_ via `DMTypedHashMap`. Server-side needs equivalent state persistence.

### 6. HandPotential class
Used for draw detection (nut flush/straight counts). Must verify it's Swing-free before deciding to move vs. abstract.

### 7. loadReputation / saveReputation
V2Player loads/saves opponent model data from `PlayerProfile`. Server-side needs separate implementation.

---

## Estimated Effort

| Phase | New/Modified Lines | Test Lines | Effort |
|---|---|---|---|
| 1: Utility classes | 900 | 200 | 2-3h |
| 2: Opponent modeling | 230 | 100 | 1-2h |
| 3: Hand evaluation | 800 | 300 | 3-4h |
| 4: V2AIContext + support | 830 | 150 | 3-4h |
| 5: PureRuleEngine | 2,550 | 300 | 6-8h |
| 6: V2Algorithm | 1,200 | 400 | 4-6h |
| 7: Integration | 950 | 150 | 3-4h |
| 8: Parity tests | 0 | 1,050 | 3-4h |
| **Total** | **~7,460** | **~2,650** | **25-35h** |

---

## Success Criteria

1. V2Algorithm compiles in pokergamecore with zero Swing dependencies
2. PureRuleEngine compiles in pokergamecore with zero Swing dependencies
3. All support classes (trackers, matrices, SimpleBias, PocketRanks/Scores/Odds) in pokergamecore
4. ServerV2AIContext implements V2AIContext with full functionality
5. Decision comparison tests show identical outcomes for 90%+ of scenarios
6. Desktop V2Player wrapper produces identical behavior to original
7. All existing tests pass (V1Algorithm, V2Player, RuleEngine)
8. Code coverage ≥ 50% for new code
