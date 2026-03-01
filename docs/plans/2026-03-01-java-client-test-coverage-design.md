# Java Client Test Coverage Design

**Status:** ACTIVE
**Date:** 2026-03-01
**Goal:** Raise JUnit test coverage to 80%+ on non-UI code across poker family modules (pokerengine, pokergamecore, poker).

## Scope

**In scope:** poker, pokerengine, pokergamecore modules — all non-UI production classes.

**Out of scope:**
- UI classes (Dialog, Panel, Piece, Display, Renderer, View, Gameboard, ChainPhase subclasses with no extractable logic)
- gamecommon, common, pokerserver, pokergameserver, gameserver, db modules
- E2E scenario tests (already 54 scripts, strong coverage)

## Approach: Bottom-Up by Module

Work in dependency order: pokerengine -> pokergamecore -> poker. Each layer's tests are self-contained and don't need to mock lower layers.

## Current State

| Module | JaCoCo Floor | Estimated Actual | Test:Class Ratio | Target |
|--------|-------------|-----------------|------------------|--------|
| pokerengine | 2% | ~65% | ~1:1 | 80%+ |
| pokergamecore | 80% | ~80% | 0.39:1 (but 13 interfaces) | 80%+ |
| poker (main pkg) | 15% | ~19% | 0.26:1 | 80%+ non-UI |
| poker (ai pkg) | 50% | ~55% | ~1:1 | 80%+ |

## Module 1: pokerengine

**Gaps:**
- `HandInfoFaster` (445 lines) — core hand evaluator, zero tests. Highest-value gap.
- `TournamentTemplate` — trivial POJO, no tests.

**Plan:**
- Write comprehensive `HandInfoFasterTest` covering all hand ranks (royal flush through high card), tie-breaking, 5-card vs 7-card evaluation.
- Write `TournamentTemplateTest` for POJO getter/setter coverage.
- Raise JaCoCo floor from 2% to 50%+.

## Module 2: pokergamecore

**Gaps (11 untested concrete classes):**

Easy (pure logic):
- `SklanksyRanking` — static ranking table lookup
- `PocketScores` — score lookup from pocket+community
- `PocketOdds` — odds calculation from pocket cards
- `PocketRanks` — ranking data structure
- `PocketMatrixFloat/Int/Short` — 2D matrix wrappers (3 classes)
- `SimpleBias` — bias lookup table
- `ActionOptions` — Java record, trivial

Medium:
- `HandInfoFast` (680 lines) — pokergamecore hand evaluator, needs Hand/Card setup
- `PureHandPotential` (456 lines) — draw potential calculator, needs pocket+community setup

**Plan:**
- Write tests for all 11 classes.
- `HandInfoFast` and `PureHandPotential` get comprehensive tests (all hand types, edge cases).
- Lookup tables get oracle tests (known inputs -> expected outputs).
- Maintain 80%+ floor.

## Module 3: poker

### Tier 1 — Pure Logic (no refactoring)

| Class | Lines | Dependencies | Test Strategy |
|-------|-------|-------------|---------------|
| `HandFutures` | 321 | HandInfoFaster, Hand, Deck | Test flush/straight/gutshot draws, improvement odds |
| `HandLadder` | 339 | Hand | Test probability distribution, hand rank counts |
| `PokerStats` | 335 | PokerGame, TournamentProfile | Integration-style: init ConfigManager, run simulations |

### Tier 2 — Extract Logic from Phase Classes

**CheckEndHand** — already well-factored. Change `private static` to package-private on:
- `checkGameOverStatus(game, human, table, neverBrokeCheatActive)` — test all 5 GameOverResult branches
- `calculateNeverBrokeTransfer(chipLeaderAmount, tableMinChip)` — test arithmetic + rounding
- `isHumanBroke(human)` — test predicate combinations
- `shouldOfferRebuy(human, table)` — test predicate combinations

**Showdown** — extract display decision logic:
- Create `ShowdownDisplayInfo` record: resultType, showCards, showHandType, handTypeDesc, bestHandRank, totalWon
- Create `ShowdownCalculator.calculateDisplayInfo(hhand, rabbitHunt, showWinning, showMucked, humanCardsUp, aiFaceUp)` — pure static method
- Test: uncontested pots, multi-way showdown, all-in before river, split pot, cheat flag combinations
- `displayShowdown()` becomes thin rendering shell calling the calculator

**DealCommunity** — extract card visibility logic:
- Create `CommunityCardFlags` record: drawnNormal[5], drawn[5]
- Create `CommunityCardCalculator.calculateVisibility(displayRound, lastBettingRound, numWithCards, rabbitHunt)` — pure static, all primitive params
- Test: pre-flop (no cards), flop (3), turn (4), river (5), rabbit hunt, all-in showdown

### Tier 3 — Deepen Existing Tests

| Test Class | Gaps to Fill |
|-----------|-------------|
| `PokerTableTest` | Button/position tracking (explicitly stubbed as empty section) |
| `PokerPlayerTest` | marshal/demarshal, rebuy/addon ops, `isInHand()` with active hand |
| `PotTest` | Multi-winner split-pot scenarios (Pot + Showdown interaction) |
| `PokerGameTest` | Level transition edge cases, blind structure progression |
| `PokerDatabaseTest` | Hand history query coverage |
| `PokerDataMarshallerTest` | Replace JUnit 3 no-op with real marshal/demarshal tests |

### Tier 4 — Remaining Non-UI Classes

| Class | Strategy |
|-------|----------|
| `HandGroupManager` | Test hand group XML load/save |
| `PlayerProfile` | Test profile data beyond PokerPlayerTest's indirect coverage |
| `GameClock` | Test marshal/demarshal (headless); verify `PureTournamentClock` tests exist in pokergamecore |
| `PokerGameState` / `PokerSaveGame` | Test save/load serialization round-trip |
| `TableDesign` | Test BaseProfile data model getters/setters |
| `PokerContext` | Test context wiring if non-trivial |

### Classes Excluded (pure UI, no extractable logic)

- `ColorUp` — Swing orchestration, `table_.isColoringUp()` state lives on PokerTable
- `Bet` — PlayerActionListener UI phase
- `WaitForDeal`, `PreShowdown`, `NewLevelActions`, `DealButton`, `PokerNight`
- `TournamentOptions` — options screen UI
- All `*Dialog`, `*Panel`, `*Piece`, `*Display`, `*Renderer`, `*View`, `*Gameboard*`

## JaCoCo Floor Progression

| Module | Current Floor | After Phase 1 | After Phase 2 | Final |
|--------|-------------|--------------|--------------|-------|
| pokerengine | 2% | 50% | 60% | 70%+ |
| pokergamecore | 80% | 80% | 80% | 80%+ |
| poker (main) | 15% | 25% | 40% | 60%+ |
| poker (ai) | 50% | 50% | 55% | 60%+ |

Note: The poker main package JaCoCo percentage will be diluted by excluded UI classes. 60%+ instruction coverage on the full package with 80%+ on non-UI code is the realistic target.

## Testing Conventions

- JUnit 5, AssertJ, Mockito (per testing-guide.md)
- Tests in same package as production class for package-private access
- Parameterized tests for lookup tables and hand evaluation
- No PropertyConfig/ConfigManager dependency unless absolutely required (prefer parameter injection)
- Card constants: NEVER modify static Card singletons (per memory.md) — create new instances
