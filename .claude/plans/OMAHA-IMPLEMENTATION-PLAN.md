# Omaha Poker Implementation Plan

**Status:** draft (v3 — updated 2026-02-18)

## Context

DDPoker currently supports only Texas Hold'em with three betting structures (No Limit, Pot Limit, Limit). This plan outlines adding **Pot Limit Omaha (PLO)** support as a new game variant while maintaining full backward compatibility with existing Hold'em functionality.

**Why this change**: Pot Limit Omaha is the second most popular poker variant worldwide. Adding PLO support would significantly expand DDPoker's appeal and provide players with variety in tournament formats.

**Current state**:
- Zero Omaha references in codebase — exclusively Hold'em
- Hand evaluation hardcoded for 2 hole cards + 5 community cards
- AI system built around 169 Hold'em starting hand combinations
- UI layout displays 2 hole cards per player
- Two separate hand evaluation paths: client (`poker` module) and server (`pokergameserver` module)

**Goal state**:
- Support Pot Limit Omaha (4 hole cards, must use exactly 2+3 rule)
- Maintain 100% backward compatibility with Hold'em
- Provide competent (not expert) Omaha AI
- PLO works for both local/practice games AND online server-hosted games

---

## Decisions (Resolved)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Online scope | Both local and online | Full PLO support across all play modes |
| Core algorithm placement | `pokerengine` (shared module) | Both `poker` and `pokergameserver` need it; server can't depend on `poker` |
| UI variants | PLO only | Constants for NLO/Limit Omaha defined but hidden in v1 UI |
| Mixed-game tournaments | Single game type per tournament | Infrastructure exists for mixed-game but deferred to v2 |

---

## Architecture Overview

### Module Dependency Constraint

```
pokergameserver ──depends──> pokergamecore ──depends──> pokerengine
poker ──────────depends──────────────────────────────> pokerengine

pokergameserver CANNOT depend on poker (enforced in pom.xml)
```

### Where Omaha Logic Lives

**`pokerengine` module (shared)** — Core algorithm:
```
OmahaScorer (NEW)
    ├── bestScore(Hand holeCards, Hand community) → int
    └── bestHand(Hand holeCards, Hand community) → Hand (best 5 cards)
    Uses HandInfoFaster internally for 5-card scoring
```

**`poker` module (client)** — Client-side wrappers:
```
PokerPlayer.getHandInfo()
    └── For Omaha: calls OmahaScorer.bestHand(), wraps in HandInfo(player, bestFive, null)
    └── For Hold'em: existing HandInfo(player, sorted, community) path
```

**`pokergameserver` module (server)** — Server-side wrappers:
```
ServerHandEvaluator.getScore(holeCards, community, gameType)
    └── For Omaha: calls OmahaScorer.bestScore() via Hand adapter
    └── For Hold'em: existing evaluateHand() path

ServerHand
    └── Deals 4 cards for Omaha, 2 for Hold'em
    └── Passes gameType to evaluateHand()
```

### Game Type Dispatch Pattern

- All game-specific logic checks `PokerConstants.isOmaha(gameType)` vs `isHoldem(gameType)`
- Default behavior preserved for Hold'em (backward compatibility)
- New code paths only activated when gameType == TYPE_POT_LIMIT_OMAHA

### What's Already Generic (No Changes Needed)

- ✅ `Hand` class supports variable card counts
- ✅ `DealDisplay.syncCards()` iterates `hand.size()` (line 67)
- ✅ Hand serialization (marshal/demarshal) handles any card count
- ✅ Pot limit calculation is game-agnostic (`getMaxBet()`, `getMaxRaise()`)
- ✅ Side pot logic is independent of hole card count
- ✅ `TournamentProfile` supports per-level game types
- ✅ `HoldemHand.getGameType()` exists (line 227)
- ✅ `HoldemHand.dealCards(int)` is parameterized (line 496)
- ✅ Card thumbnail mode already handles 4 cards in a 2x2 grid (CardPiece lines 366-411)

### What Needs Building

- ❌ `OmahaScorer` in `pokerengine` — core 60-combination algorithm (BLOCKER)
- ❌ `PokerPlayer.getHandInfo()` dispatch by game type (BLOCKER)
- ❌ `ServerHandEvaluator` Omaha support (BLOCKER for online)
- ❌ `ServerHand` dealing and showdown for Omaha (BLOCKER for online)
- ❌ AI pre-flop strategy for 4-card hands — both client (`V1Player`) and server (`V1Algorithm`)
- ❌ UI card spacing for 4 cards in normal play mode
- ❌ Omaha game type constants in `PokerConstants`

---

## TDD Methodology

**Following CLAUDE.md guidelines**: All implementation MUST follow strict Test-Driven Development.

### TDD Workflow (RED-GREEN-REFACTOR)

1. **RED**: Write a failing test FIRST — test defines expected behavior
2. **GREEN**: Write minimal code to pass — no extra features
3. **REFACTOR**: Clean up while keeping tests green
4. **REPEAT**: Next requirement, back to RED

### Test Coverage Requirements

| Phase | Minimum Coverage | Focus |
|-------|-----------------|-------|
| Phase 1 | 100% | Simple utility methods |
| Phase 2 | 95% | Hand evaluation logic (CRITICAL) |
| Phase 3 | 85% | Client game flow |
| Phase 4 | 85% | Server game flow |
| Phase 5 | Visual verification | UI layout correctness |
| Phase 6 | 80% | AI decision making |
| Phase 7 | Full E2E | Complete game simulation |

### Test Conventions

- **File naming**: `*Test.java` (unit), `*IntegrationTest.java` (integration)
- **Method naming**: `should_<ExpectedBehavior>_When_<Condition>()`
- **Structure**: Given-When-Then comments
- **Card instances**: Always create fresh Card instances (see learnings.md)
- **PropertyConfig**: Ensure initialized before tests that need it

---

## Implementation Phases

### Phase 1: Foundation — Game Type Infrastructure (1-2 days, LOW risk)

**Goal**: Add Omaha constants and utility methods without breaking existing functionality.

**Module**: `pokerengine`

#### Steps

**Step 1.1: RED/GREEN — PokerConstants utility methods**

Add to `PokerConstants.java`:
```java
// Game type constants (existing: 1=NLH, 2=PLH, 3=LH)
public static final int TYPE_POT_LIMIT_OMAHA = 4;
public static final int TYPE_NO_LIMIT_OMAHA = 5;
public static final int TYPE_LIMIT_OMAHA = 6;

public static final String DE_POT_LIMIT_OMAHA = "potlimitomaha";
public static final String DE_NO_LIMIT_OMAHA = "nolimitomaha";
public static final String DE_LIMIT_OMAHA = "limitomaha";

public static boolean isOmaha(int gameType) {
    return gameType == TYPE_POT_LIMIT_OMAHA ||
           gameType == TYPE_NO_LIMIT_OMAHA ||
           gameType == TYPE_LIMIT_OMAHA;
}

public static boolean isHoldem(int gameType) {
    return gameType == TYPE_NO_LIMIT_HOLDEM ||
           gameType == TYPE_POT_LIMIT_HOLDEM ||
           gameType == TYPE_LIMIT_HOLDEM;
}

public static int getHoleCardCount(int gameType) {
    return isOmaha(gameType) ? 4 : 2;
}
```

Tests: `PokerConstantsTest.java` — cover all constants, utility methods, edge cases.

**Step 1.2: RED/GREEN — TournamentProfile string mapping**

Update `TournamentProfile.getGameType(int nLevel)` to map `"potlimitomaha"` → `TYPE_POT_LIMIT_OMAHA` (and similar for NLO, LO). The per-level storage infrastructure already exists.

**Phase 1 Verification**:
- ✅ All new tests pass (100% coverage)
- ✅ All existing tests pass (zero regression)
- ✅ `mvn test -P dev` succeeds

---

### Phase 2: Hand Evaluation Core — OmahaScorer (6-8 days, HIGH risk)

**Goal**: Implement Omaha hand evaluation in the shared `pokerengine` module.

**Module**: `pokerengine` (accessible by both `poker` and `pokergameserver`)

**Critical Algorithm**: Evaluate C(4,2) × C(5,3) = 6 × 10 = 60 combinations, select best.

**TDD CRITICAL**: This is the highest-risk phase. Every test must be written BEFORE implementation.

#### Architecture

`OmahaScorer` lives in `pokerengine` and uses `HandInfoFaster` for 5-card scoring:

```java
package com.donohoedigital.games.poker.engine;

public class OmahaScorer {
    /**
     * Find the best score from all valid Omaha combinations.
     * Evaluates C(4,2) × C(N,3) combos where N = community.size().
     */
    public static int bestScore(Hand holeCards, Hand community) { ... }

    /**
     * Find the best 5-card hand from all valid Omaha combinations.
     */
    public static Hand bestHand(Hand holeCards, Hand community) { ... }
}
```

#### Steps

**Step 2.1: RED — Critical Omaha rule tests (write ALL before implementation)**

**NEW FILE**: `OmahaScorerTest.java` in `pokerengine/src/test/`

```java
// CRITICAL TEST 1: Board-only flush must NOT count
@Test
void should_NotBeFlush_When_PlayerHasOnlyOneCardOfFlushSuit() {
    // Player has 1 spade; board has 4 spades
    // Cannot make flush — needs exactly 2 suited hole cards
}

// CRITICAL TEST 2: Must use exactly 2 hole cards
@Test
void should_FindRoyalFlush_When_TwoSuitedHoleCardsPlusBoardMakeIt() {
    // A♠ K♠ in hole + Q♠ J♠ T♠ on board
}

// CRITICAL TEST 3: Board-only straight must NOT count
@Test
void should_NotBeStraight_When_NoTwoHoleCardsContribute() {
    // Board: 9-8-7-6-5; Hole: A-K-2-3 → high card only
}

// CRITICAL TEST 4: Four of same rank in hole
@Test
void should_BeTwoPair_When_FourAcesInHoleAndPairOnBoard() {
    // AAAA hole, KK on board → AA + KK (can only use 2 aces)
}

// CRITICAL TEST 5: All hand types work
// Royal flush, straight flush, quads, full house, flush,
// straight, trips, two pair, pair, high card

// CRITICAL TEST 6: Pre-flop (3 community) and turn (4 community) work
@Test
void should_EvaluateCorrectly_When_OnlyThreeCommunityCards()
@Test
void should_EvaluateCorrectly_When_FourCommunityCards()

// CRITICAL TEST 7: Performance
@Test
void should_Complete60Evaluations_InUnder5ms()

// CRITICAL TEST 8: Score consistency
@Test
void should_ProduceSameScoreAs_HandInfoFaster_When_GivenSameFiveCards()
```

**Step 2.2: GREEN — Implement OmahaScorer**

```java
public static int bestScore(Hand holeCards, Hand community) {
    ApplicationError.assertTrue(holeCards.size() == 4, "Omaha requires 4 hole cards");
    ApplicationError.assertTrue(community.size() >= 3, "Need at least 3 community cards");

    int best = 0;
    HandInfoFaster evaluator = new HandInfoFaster();

    // C(4,2) hole card combos
    for (int h1 = 0; h1 < 4; h1++) {
        for (int h2 = h1 + 1; h2 < 4; h2++) {
            // C(N,3) community card combos
            for (int c1 = 0; c1 < community.size(); c1++) {
                for (int c2 = c1 + 1; c2 < community.size(); c2++) {
                    for (int c3 = c2 + 1; c3 < community.size(); c3++) {
                        HandSorted candidate = new HandSorted();
                        candidate.addCard(holeCards.getCard(h1));
                        candidate.addCard(holeCards.getCard(h2));
                        candidate.addCard(community.getCard(c1));
                        candidate.addCard(community.getCard(c2));
                        candidate.addCard(community.getCard(c3));

                        int score = evaluator.getScore(candidate);
                        if (score > best) {
                            best = score;
                        }
                    }
                }
            }
        }
    }
    return best;
}
```

**Step 2.3: Integration — PokerPlayer.getHandInfo() (client-side)**

**MODIFY**: `PokerPlayer.java` line 1399

```java
public HandInfo getHandInfo() {
    if (handInfo_ == null) {
        HandSorted sorted = getHandSorted();
        HoldemHand hhand = getHoldemHand();
        if (sorted != null && hhand != null) {
            int gameType = hhand.getGameType();
            if (PokerConstants.isOmaha(gameType)) {
                Hand bestFive = OmahaScorer.bestHand(getHand(), hhand.getCommunity());
                handInfo_ = new HandInfo(this, new HandSorted(bestFive), null);
            } else {
                handInfo_ = new HandInfo(this, sorted, hhand.getCommunitySorted());
            }
        }
    }
    return handInfo_;
}
```

All downstream code (`HoldemHand.preResolvePot()`, `resolvePot()`, `Showdown`, `PokerStats`) automatically gets correct Omaha evaluation without changes.

**Phase 2 Verification**:
- ✅ All 60 combinations evaluated correctly for every test case
- ✅ "Exactly 2+3" rule enforced (board-only hands rejected)
- ✅ No regression in Hold'em evaluation
- ✅ Performance: <5ms per Omaha hand evaluation
- ✅ Score consistency with HandInfoFaster for same 5 cards
- ✅ `mvn test -P dev` succeeds

---

### Phase 3: Client Game Logic — Deal 4 Cards (2-3 days, LOW risk)

**Goal**: Modify client-side dealing logic to deal 4 hole cards for Omaha.

**Module**: `poker`

#### Steps

**Step 3.1: RED — Test dealing behavior**

```java
@Test
void should_DealFourCards_When_GameTypeIsOmaha()
@Test
void should_DealTwoCards_When_GameTypeIsHoldem()  // regression
```

**Step 3.2: GREEN — Modify dealing logic**

**MODIFY**: `HoldemHand.java` line 260

Currently: `dealCards(2);`

Change to:
```java
int numHoleCards = PokerConstants.getHoleCardCount(getGameType());
dealCards(numHoleCards);
```

**Deck capacity check**: 10 players × 4 + 5 community + 3 burn = 48 of 52. Safe.

**Phase 3 Verification**:
- ✅ Omaha deals 4 cards per player
- ✅ Hold'em still deals 2 cards
- ✅ All existing tests pass

---

### Phase 4: Server Game Logic — ServerHand Omaha Support (3-5 days, MEDIUM risk)

**Goal**: Enable Omaha for online server-hosted games.

**Modules**: `pokergameserver`, `pokergamecore`

#### Steps

**Step 4.1: ServerHandEvaluator — Omaha scoring**

**MODIFY**: `ServerHandEvaluator.java`

Current `getScore()` (line 73) combines all cards into `new Card[7]` and evaluates best-5-from-7. This is wrong for Omaha.

Option A (recommended): Add game-type-aware method:
```java
public int getScore(List<Card> holeCards, List<Card> communityCards, int gameType) {
    if (PokerConstants.isOmaha(gameType)) {
        // Convert List<Card> to Hand for OmahaScorer
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);
        return OmahaScorer.bestScore(hole, community);
    }
    return getScore(holeCards, communityCards); // existing Hold'em path
}
```

Keep the existing `getScore(List<Card>, List<Card>)` for backward compatibility.

**Step 4.2: ServerHand — Deal 4 cards and pass game type**

**MODIFY**: `ServerHand.java`

- Add `gameType` field (passed from tournament config)
- Update dealing logic to deal `PokerConstants.getHoleCardCount(gameType)` cards
- Fix `Card[7]` hardcoding if present elsewhere
- Pass `gameType` to `ServerHandEvaluator.getScore()` at showdown (line 750)

**Step 4.3: ServerTournamentDirector — Game type propagation**

Verify that game type flows from `TournamentProfile` → `ServerTournamentDirector` → `ServerHand`. May need to thread `gameType` through the construction chain.

**Step 4.4: pokergamecore AI awareness**

Check if `V1Algorithm` and `V2Algorithm` in `pokergamecore` need Omaha pre-flop dispatch. If they use `HandInfoFast`/`PureHandPotential`, they likely need the same kind of game-type dispatch as client-side V1Player.

**Phase 4 Verification**:
- ✅ `ServerHandEvaluator` returns correct scores for Omaha hands
- ✅ `ServerHand` deals 4 cards for Omaha
- ✅ Showdown uses 2+3 rule on server
- ✅ No regression for Hold'em server games
- ✅ `ServerHandEvaluatorTest` covers Omaha cases

---

### Phase 5: UI Layout — Display 4 Hole Cards (3-4 days, MEDIUM risk)

**Goal**: Update card display to show 4 hole cards for Omaha.

**Module**: `poker`

#### How Card Display Works

All hole cards are placed at a single `POINT_HOLE1` territory point. Multiple `CardPiece` instances are spaced automatically via `getXAdjust()` (55px normal, 82px large). No "hole2/3/4" territory points exist or are needed.

- 2-card width: ~155px (card + 55px offset)
- 4-card width: ~265px (card + 3 × 55px)

#### Steps

**Step 5.1: Visual test with 4 cards**

Before changing any code, deal 4 cards in a test game and observe. The system may already work since `DealDisplay.syncCards()` iterates `hand.size()`.

**Step 5.2: Adjust card spacing if needed**

If 4 cards overflow at edge seats, modify `CardPiece.getXAdjust()`:

```java
@Override
public int getXAdjust() {
    int numCards = getNumCards();
    if (isLarge()) {
        return numCards > 2 ? 60 : 82;
    }
    return numCards > 2 ? 40 : 55;
}
```

**Step 5.3: Verify all display modes**

- Normal mode at all 10 seats
- Large card mode
- Thumbnail/chip-race mode (already handles 4 cards in 2x2 grid)
- Card mouse-over and selection

**Phase 5 Verification**:
- ✅ 4 cards display at all seats without overlap
- ✅ Hold'em 2-card display unchanged
- ✅ All display modes work

---

### Phase 6: AI Foundation — Omaha Pre-Flop (6-8 days, HIGH risk)

**Goal**: Implement competent Omaha AI for both client and server.

**Modules**: `poker` (V1Player), `pokergamecore` (V1Algorithm, V2Algorithm)

#### Steps

**Step 6.1: OmahaHandStrength scoring system**

**NEW FILE**: `OmahaHandStrength.java` in `pokerengine` (shared, so both client and server AI can use it)

Hand Classes (Hutchison Point Count):
1. **Premium**: AAxx with suited/connected cards (top 5%)
2. **Strong**: AKQJ, double-suited (top 15%)
3. **Rundowns**: 9876, JT98 with suit potential (top 25%)
4. **Medium Pairs**: QQxx, JJxx with support (top 35%)
5. **Speculative**: Two >T with flush draw potential (top 50%)
6. **Trash**: Everything else (fold pre-flop)

```java
public static int getOmahaHandStrength(Hand fourCardHole) {
    int score = 0;
    // +points for pairs, high cards, suitedness, connectedness
    // -points for danglers (low unconnected cards)
    return Math.min(100, score);
}
```

**Step 6.2: Client AI dispatch — V1Player**

**MODIFY**: `V1Player.java` line 433

```java
int handStrength;
if (PokerConstants.isOmaha(getGameType())) {
    handStrength = OmahaHandStrength.getOmahaHandStrength(_hole);
} else {
    int sklanskyRank = HoldemExpert.getSklanskyRank(_hole);
    handStrength = convertSklanskyToStrength(sklanskyRank);
}
```

**Step 6.3: Server AI dispatch — V1Algorithm / V2Algorithm**

Similar dispatch in `pokergamecore` AI classes. Since `OmahaHandStrength` is in `pokerengine`, both client and server AI can access it.

**Step 6.4: Post-flop AI (simplified)**

Reuse existing post-flop logic. Made hand evaluation is already correct via `PokerPlayer.getHandInfo()` (client) and `ServerHandEvaluator` (server) dispatches from Phase 2/4.

**Known limitation**: Outs/drawing calculations in `HandPotential` / `PureHandPotential` assume Hold'em. AI draw decisions will be approximate for Omaha v1.

**Tests**:
```java
should_RankAAKKDoubleSuited_AsStrongest()
should_RankDanglers_AsWeakHands()
should_FoldTrashHands_PreFlop()
should_RaisePremiumHands_PreFlop()
should_NotChaseBoardOnlyFlush_PostFlop()
```

**Phase 6 Verification**:
- ✅ AI plays reasonable pre-flop
- ✅ Correct post-flop evaluation
- ✅ No regression in Hold'em AI
- ✅ Both client and server AI work

---

### Phase 7: Integration & Polish (3-4 days, LOW risk)

**Goal**: Wire everything together, add UI controls, comprehensive testing.

#### Steps

**Step 7.1: Tournament Setup UI**

Add PLO to game type dropdown (single game type for entire tournament):
```
Game Type: [No Limit Hold'em ▼]
           [Pot Limit Hold'em]
           [Limit Hold'em]
           [Pot Limit Omaha]    ← NEW
```

NLO and Limit Omaha constants defined but not exposed in v1 UI.

**Step 7.2: Display Labels**

- Tournament info panel: "Pot Limit Omaha"
- Hand history: Show 4 hole cards

**Step 7.3: Network Protocol**

Verify Omaha games work over WebSocket:
- Game type transmitted in game state
- 4-card hands serialize/deserialize correctly
- Server evaluates with OmahaScorer, client displays correctly

**Step 7.4: Save/Load Compatibility**

- Old Hold'em saves load correctly (default game type = Hold'em)
- New Omaha saves load correctly

**Step 7.5: End-to-End Tests**

```java
should_CompleteFullOmahaHand_LocalGame()
should_CompleteFullOmahaHand_ServerHostedGame()
should_MaintainHoldemBackwardCompatibility()
```

**Step 7.6: Backward Compatibility Suite**

- All existing Hold'em tests pass
- `mvn test -P dev` succeeds
- `mvn verify -P coverage` meets thresholds

---

## Edge Cases & Risk Analysis

### CRITICAL: Hand Evaluation Correctness

**Risk**: Omaha's "exactly 2+3" rule is the #1 source of bugs.

**Mitigations**:
- Write "known bad" tests BEFORE implementation
- Cross-validate with online Omaha calculators
- Property-based testing with random hands
- 95% test coverage for Phase 2
- Single algorithm (`OmahaScorer`) used by both client and server — no dual-implementation drift

### CRITICAL: Client/Server Evaluation Consistency

**Risk**: Client and server use different evaluation paths. If they produce different scores, game state becomes inconsistent.

**Mitigation**: Both use `OmahaScorer` from `pokerengine`. The core algorithm is in one place. `PokerPlayer.getHandInfo()` and `ServerHandEvaluator` are thin wrappers around the same `OmahaScorer.bestScore()`.

### CRITICAL: ServerHandEvaluator Card Array Size

**Risk**: `ServerHandEvaluator.getScore()` allocates `new Card[7]` (line 77). Passing 4 hole cards would overflow.

**Mitigation**: The new game-type-aware overload dispatches to `OmahaScorer` before reaching the Card[7] allocation. The existing Hold'em path is unchanged.

### MEDIUM: PokerPlayer.getHandInfo() Integration

**Risk**: `getHandInfo()` is called from many places. If the Omaha dispatch is wrong, incorrect hands could win.

**Callers identified**:
- `HoldemHand.preResolvePot()` line 2653
- `HoldemHand.resolvePot()` lines 2691, 2729
- `Showdown.java` line 162
- `PokerStats.java` line 230

**Mitigation**: Single integration point means all callers automatically get correct evaluation.

### MEDIUM: HandPotential / Outs Calculation

**Risk**: `HandPotential` and `PureHandPotential` calculate drawing odds assuming Hold'em rules. Omaha outs calculation is fundamentally different (6 two-card combos × remaining cards).

**Mitigation**: Accept approximate outs for v1. AI still plays reasonably because made-hand evaluation is correct. Follow-up task for Omaha-specific outs.

### MEDIUM: HandInfo.isOurHandInvolved()

**Risk**: Used for display highlighting (which cards contribute to best hand). Assumes 2 hole cards.

**Mitigation**: May produce incorrect highlights for Omaha but won't affect game correctness. Defer to v2.

### MEDIUM: UI Card Overflow

**Risk**: 4 cards at 55px spacing may overflow at edge seats.

**Mitigations**:
- Visual test first — may already fit
- Reduce `getXAdjust()` for 4-card hands
- Thumbnail mode already handles 4 cards

### LOW: Deck Exhaustion

10 × 4 + 5 + 3 burn = 48 from 52. Safe.

### LOW: Pot Limit / Side Pots

Verified game-agnostic. No changes needed.

### LOW: Serialization

`Hand.marshal()` handles any card count. Verified safe.

---

## Critical Files Summary

### Phase 1 (Foundation) — `pokerengine`
- `PokerConstants.java` — Add Omaha constants and utility methods
- `TournamentProfile.java` — Add Omaha string→constant mapping

### Phase 2 (Hand Evaluation) — `pokerengine` + `poker`
- **`OmahaScorer.java`** — **NEW** in `pokerengine`: Core 60-combination algorithm
- `PokerPlayer.java` line 1399 — Modify `getHandInfo()` to dispatch by game type

### Phase 3 (Client Dealing) — `poker`
- `HoldemHand.java` line 260 — Change `dealCards(2)` to use `getHoleCardCount()`

### Phase 4 (Server Logic) — `pokergameserver` + `pokergamecore`
- `ServerHandEvaluator.java` — Add game-type-aware scoring method
- `ServerHand.java` — Deal 4 cards, pass game type to evaluation
- `ServerTournamentDirector.java` — Thread game type through

### Phase 5 (UI) — `poker`
- `CardPiece.java` — Potentially adjust `getXAdjust()` for 4-card hands

### Phase 6 (AI) — `pokerengine` + `poker` + `pokergamecore`
- **`OmahaHandStrength.java`** — **NEW** in `pokerengine`: Hutchison-based scoring
- `V1Player.java` line 433 — Client AI pre-flop dispatch
- `V1Algorithm.java` / `V2Algorithm.java` — Server AI pre-flop dispatch

### Phase 7 (Integration) — all modules
- Tournament creation dialog — Add PLO option
- UI labels — Show game variant name
- E2E tests — Both local and online

---

## Effort Estimation

| Phase | Complexity | Days | Dependencies |
|-------|------------|------|--------------|
| Phase 1: Foundation | LOW | 1-2 | None |
| Phase 2: Hand Evaluation | HIGH | 6-8 | Phase 1 |
| Phase 3: Client Dealing | LOW | 2-3 | Phase 1 |
| Phase 4: Server Logic | MEDIUM | 3-5 | Phase 1, 2 |
| Phase 5: UI Layout | MEDIUM | 3-4 | Phase 3 |
| Phase 6: AI | HIGH | 6-8 | Phase 2 |
| Phase 7: Integration | LOW | 3-4 | All |
| **Total** | | **24-34** | **(4-6 weeks)** |

**Critical Path**: Phase 1 → Phase 2 → Phase 4 → Phase 7

**Parallel Options**:
- Phase 3 (client dealing) can start after Phase 1 (parallel with Phase 2)
- Phase 5 (UI) can start after Phase 3 (parallel with Phase 4, 6)
- Phase 6 (AI) can start after Phase 2 (parallel with Phase 4, 5)

```
Phase 1 ──> Phase 2 ──> Phase 4 ──> Phase 7
         └──> Phase 3 ──> Phase 5 ──┘
              Phase 2 ──> Phase 6 ──┘
```

---

## Success Criteria

**Functional**:
- ✅ PLO tournament playable from start to finish (local AND online)
- ✅ Correct 4-card dealing
- ✅ Correct hand evaluation (exactly 2+3 rule enforced)
- ✅ Competent AI (doesn't make egregious mistakes)

**Quality**:
- ✅ Zero regression in Hold'em
- ✅ All existing tests pass
- ✅ 95%+ coverage for Phase 2 (OmahaScorer)
- ✅ Performance: <5ms hand evaluation
- ✅ Client and server produce identical scores for same hands

**User Experience**:
- ✅ 4 cards display clearly at all seats
- ✅ PLO selectable in tournament setup
- ✅ Works for both practice and online games

**Maintainability**:
- ✅ Core algorithm (`OmahaScorer`) in shared `pokerengine` module
- ✅ Strategy pattern allows future variants (Omaha Hi-Lo, 5-Card Omaha)
- ✅ No code duplication between client and server evaluation

---

## Known Limitations (v1)

Intentionally deferred:

1. **Omaha Hi-Lo**: Not supported. Architecture allows it (add `evaluateLow()` to `OmahaScorer`).
2. **5-Card Omaha (PLO5)**: Not supported. Change `getHoleCardCount()` to 5 when ready.
3. **NLO / Limit Omaha**: Constants defined but not exposed in UI. Just a UI toggle away.
4. **Mixed-game tournaments**: Infrastructure exists in `TournamentProfile` but UI not exposed.
5. **Omaha-specific outs calculation**: `HandPotential` / `PureHandPotential` use Hold'em logic. AI draws approximate.
6. **Hand highlight accuracy**: `isOurHandInvolved()` may highlight wrong hole cards in Omaha.
