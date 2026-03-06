# Poker Module (Thin Client) Test Coverage Expansion

**Status:** APPROVED (2026-03-06)

## Goal

Push unit test coverage in the `poker` module aggressively:
- Root package (`com.donohoedigital.games.poker`): 13% -> 40%+
- AI package (`com.donohoedigital.games.poker.ai`): 43% -> 55%+

## Context

The poker module is the Java Swing thin client. It has 65 existing test classes with 1,249 test methods, but coverage is heavily concentrated in display types, online/network clients, and data classes. The ~80 Swing UI classes are largely untested.

**Constraints:**
- AssertJ Swing does not work in this project — no direct Swing component testing
- `pokerengine` and `pokergamecore` are banned at compile time (Maven Enforcer)
- UI code paths can only be exercised via E2E tests through the GameControlServer HTTP API
- Actual AI decision-making is server-side; `poker.ai` package is client-side configuration UI

## Phase 1: Dead Code Cleanup

Remove dead AI code to reduce the coverage denominator and clean up the codebase.

### Delete

| File | Reason |
|------|--------|
| `ai/AIStrategy.java` | Empty class, zero references anywhere |
| `ai/AIConstants.java` | Zero poker-module consumers; duplicate of `pokergamecore` copy |
| `ai/phase/CreateTestCase.java` | Dead placeholder dialog ("not available, server-driven AI") |

### Related Changes

- Remove `CreateTestCase` phase definition from `gamedef.xml`
- Remove `CreateTestCase` trigger from `ShowTournamentTable.java`

## Phase 2: Unit Tests for Untested Pure-Logic Classes

New test files — no production code changes needed.

| Class | Lines | What to Test |
|-------|-------|-------------|
| `TournamentProfileFormatter` | 450 | HTML generation for blind levels, payouts, online settings |
| `HandSelectionScheme` | 310 | `getHandStrength()`, group parsing, serialization round-trip |
| `ImpExp` / `ImpExpParadise` / `ImpExpUB` | ~400 | Hand import/export parsing for each format |

Also: review existing `StatResultsTest` for thin coverage and expand if needed.

## Phase 3: Extract Logic from UI Classes

Small refactorings to pull testable logic out of Swing-coupled classes. Pattern follows the existing `ShowdownCalculator` extraction.

| Source Class | Logic to Extract | Approach |
|--------------|-----------------|----------|
| `Bet.java` | Input mode selection, bet rounding to chip denomination, action type determination | New `BetCalculator` class or static methods |
| `ChipLeaderPanel.java` | Player ranking algorithm, average stack / blind multiple calculations | New `ChipLeaderCalculator` |
| `GameOver.java` | Game outcome classification (win/money/busted/observer) | Simple helper or enum |
| `SidePotsDialog.java` | Pot classification (main/side/overbet) + eligible player sorting | `PotClassifier` helper |
| `PlayerInfo.java` (dashboard) | Rebuy availability calculation | Extract to method on existing class |

Estimated effort: ~2-3 hours total refactoring + tests.

## Phase 4: Expand E2E Tests via GameControlServer

Cover UI code paths indirectly through the HTTP control API. These are slower but exercise actual Swing rendering paths.

Scenarios to add:
- Betting scenarios: check, call, raise, fold, all-in
- Tournament progression: blind level changes, player eliminations, final table
- Edge cases: side pots, showdowns, split pots
- Game save/restore lifecycle

## Phase 5: Integration Tests via MockGameEngine

Test classes that need game infrastructure but not actual rendering, using the existing `IntegrationTestBase` / `MockGameEngine` / `MockPokerMain` infrastructure.

Candidates:
- `PokerGameState` / `PokerGameStateDelegate` — game state machine
- `PokerGameboard` / `PokerGameboardDelegate` — gameboard state delegation
- `WebSocketOpponentTracker` — opponent tracking state
- `SwingEventBus` — event dispatch (mock subscribers)

## Expected Coverage Impact

| Package | Current | After Ph 1-3 | After Ph 4-5 |
|---------|---------|-------------|-------------|
| Root (`poker`) | 13% | ~25% | ~40%+ |
| AI (`poker.ai`) | 43% | ~55% | ~60% |

## Decisions

- **No AssertJ Swing**: Confirmed non-functional in this project. UI testing is via GameControlServer E2E only.
- **AI code stays client-side**: `poker.ai` package is configuration UI for AI opponents (player types, hand selection, strategy sliders), not AI decision-making. Legitimately client-side.
- **Extract-and-test pattern**: Follow `ShowdownCalculator` as the reference for extracting logic from UI classes.
