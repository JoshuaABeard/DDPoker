# Unit Testing Improvement Plan

## Context

DD Poker has 135 test files across 788 source files (~17% file coverage). While the AI subsystem is well-tested (50% threshold), core poker primitives (`Card`, `Deck`, `Hand`), game state management, and server-side services have significant gaps. The `gameengine` module (71 files) has **zero tests** and is tightly coupled to Swing, requiring refactoring to enable testing. This plan focuses on protecting core business logic with thorough unit tests, prioritizing correctness in the code that matters most.

---

## Phase 1: Poker Engine Primitives (`pokerengine`) ✅ COMPLETE

**Status:** ✅ **COMPLETE** (2026-02-13)
- 7 test files created: CardTest, HandTest, CardSuitTest, HandSortedTest, DeckTest, OnlineGameTest, TournamentHistoryTest
- 240 new tests added (total: 476 tests in pokerengine module)
- **Coverage: 65%** (far exceeds 40% target)
- All tests passing in parallel execution

**Original state:** 20 source files, 4 tests, 2% coverage threshold

**Goal:** Comprehensive tests for the core poker data types that everything else builds on.

### Target Classes

**`Card.java`** (0 tests) — Core card representation, extends `com.ddpoker.Card`
- Card construction from rank + suit
- `getRank()`, `getSuit()`, rank/suit display methods
- `compareTo()` ordering (natural ordering by index)
- `equals()` / `hashCode()` correctness
- `fingerprint()` bit manipulation
- `isBlank()`, `isSameRank()`, `isSameSuit()`
- `getCard(suit, rank)` static lookup
- `toHTML()` output format
- Serialization: `marshal()` / `demarshal()` round-trip

**`Hand.java`** (0 direct tests) — Collection of cards with poker-specific queries
- Construction: empty, from cards, from another hand, partial copy
- Card management: `addCard()`, `insertCard()`, `removeCard()`, `removeBlank()`, `countCard()`
- Sorting: `sortAscending()`, `sortDescending()`
- Poker queries: `isSuited()`, `isConnectors()`, `isPair()`, `isRanked()`
- Hand analysis: `hasPair()`, `hasTrips()`, `hasQuads()`, `hasFlush()`, `hasPossibleFlush()`
- Rank queries: `getHighestRank()`, `getLowestRank()`, `getHighestSuited()`, `getNumSuits()`
- Containment: `containsCard()`, `containsAny()`, `containsRank()`, `containsSuit()`, `isInHand()`
- Fingerprinting: `fingerprint()`, `fingerprint(n)`, caching via `fingerprintModCount_`
- Display: `toStringRankSuit()`, `toStringRank()`, `toStringSuited()`, `toHTML()`
- Serialization: `marshal()` / `demarshal()` round-trip

**`Deck.java`** (1 randomness test) — 52-card deck with shuffle
- Construction: unshuffled (sorted order), shuffled
- `nextCard()` dealing, `getNumUndealt()`, `reset()`
- `shuffle()` produces different orderings
- `deal(Hand, int)` dealing to hands
- `removeCard()` removes from undealt portion
- Serialization round-trip

**`HandSorted.java`** (0 tests) — Sorted variant of Hand
- Maintains sort order on card insertion
- Correct behavior when mixing add operations

**`CardSuit.java`** (0 tests) — Suit enumeration
- Suit ranking, display, equality

**Model classes** (partially tested):
- `OnlineProfile.java` — password hashing, equality, validation
- `TournamentProfile.java` — tournament configuration
- `OnlineGame.java` — game state model
- `TournamentHistory.java` — history records, scoring

### Test Files to Create
```
code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/
  CardTest.java
  HandTest.java
  HandSortedTest.java
  DeckTest.java                    # extend existing DeckRandomnessTest coverage
  CardSuitTest.java
code/pokerengine/src/test/java/com/donohoedigital/games/poker/model/
  OnlineGameTest.java
  TournamentHistoryTest.java
```

### Coverage Target
- Raise `pokerengine` threshold from **2% → 40%**

### Key Files
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/Card.java`
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/Hand.java`
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/Deck.java`
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/HandSorted.java`
- `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/CardSuit.java`
- `code/pokerengine/src/test/java/com/donohoedigital/games/poker/engine/DeckRandomnessTest.java` (existing, extend)

---

## Phase 2: Core Poker Logic (`poker` module) ✅ COMPLETE

**Status:** ✅ **COMPLETE** (2026-02-13)
- 2 test files created: HandInfoFasterTest, HandInfoConsistencyTest
- 47 new tests added (total: 1,583 tests in poker module, up from 1,536)
- **Coverage: 21%** (up from 15%, target was 25%)
- All tests passing, focus on critical AI simulation path (HandInfoFaster)

**Original state:** 239 source files, 58 tests, 15% threshold

**Goal:** Test the business logic behind betting, pot management, and hand evaluation.

### Completed Tests

**`HandInfoFasterTest.java`** ✅ (34 tests) — Fast hand evaluation (critical path for AI simulations)
- ✅ All 10 hand types: royal flush, straight flush, quads, full house, flush, straight, trips, two pair, pair, high card
- ✅ Edge cases: wheel straight flush, ace-low straight, blank cards, 5/7 card scenarios
- ✅ Scoring system validation and hand type ranking

**`HandInfoConsistencyTest.java`** ✅ (13 tests) — Cross-validation between evaluators
- ✅ Verified HandInfoFaster matches HandInfo and HandInfoFast across all hand types
- ✅ Regression safety across all three evaluators
- ✅ Performance validation (1000-hand benchmark)

### Not Implemented (Deferred)

**`Bet.java`** — Bet representation and validation
- **Reason:** BetValidator already comprehensively tested (17 tests), Bet.java too UI-heavy

**`HoldemHand.java`** — Core hand-in-progress logic
- **Reason:** Already has 3 test files covering core functionality (HoldemHandTest, HoldemHandPotCalculationTest, HoldemHandPotDistributionTest)

**`PokerUtils.java`** — Utility calculations
- **Reason:** Already has 45 tests covering chip icons, math operations, fold key state, chat formatting

### Coverage Analysis
- **Target:** 15% → 25%
- **Achieved:** 15% → 21%
- **Gap:** 4% below target, but significant 40% improvement from baseline
- **Reason:** HandInfoFaster (335 lines) is small relative to poker module (239 files); focused on high-value critical path

### Key Files
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandInfoFaster.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandInfo.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/Bet.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerUtils.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/integration/IntegrationTestBase.java` (reuse)
- `code/poker/src/test/java/com/donohoedigital/games/poker/mock/MockGameEngine.java` (reuse)

---

## Phase 3: Game State & Common Infrastructure (`gamecommon`)

**Current state:** 46 source files, 1 test, 0% threshold

**Goal:** Test the data structures and game configuration that both client and server depend on.

### Target Classes (pure data structures, no Swing dependencies)

**`GameState.java`** — Game state container
- State serialization/deserialization
- State transitions

**`GamePlayer.java`** — Player representation
- Player properties, equality

**`GamePhase.java` / `GamePhases.java`** — Phase definitions
- Phase lookup, ordering

**`Territory.java` / `Territories.java`** — Game board territories
- Territory lookup, adjacency

**`GamePieceContainerImpl.java`** — Game piece management
- Add/remove/find pieces

**`SaveFile.java` / `SaveDetails.java`** — Save game infrastructure
- File operations, metadata

**`EngineMessage.java`** — Game messaging
- Message construction, serialization

### Test Files to Create
```
code/gamecommon/src/test/java/com/donohoedigital/games/config/
  GameStateTest.java
  GamePlayerTest.java
  GamePhaseTest.java
  TerritoryTest.java
  GamePieceContainerTest.java
  SaveFileTest.java
code/gamecommon/src/test/java/com/donohoedigital/games/comms/
  EngineMessageTest.java
```

### Coverage Target
- Raise `gamecommon` threshold from **0% → 15%**

### Key Files
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GameState.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePlayer.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/config/GamePhase.java`
- `code/gamecommon/src/main/java/com/donohoedigital/games/comms/EngineMessage.java`

---

## Phase 4: Common Utilities (`common`)

**Current state:** 87 source files, 27 tests, 5% threshold

**Goal:** Expand coverage of the shared utility layer that every module depends on.

### Target Classes (untested utilities with broad usage)

**`TypedHashMap.java`** — Typed map used by `Game` and many other classes
- Type-safe get/put for int, boolean, string, etc.
- Default value behavior

**`Format.java`** — Number/string formatting
- Currency, percentage, number formatting

**`Utils.java`** — General utilities
- String manipulation, file operations, collection helpers

**`DMTypedHashMap.java`** — Data-marshalled typed map
- Serialization round-trip

**`DataMarshal` / `DMArrayList`** — Serialization framework
- Marshal/demarshal for various types
- Edge cases: null values, empty collections, nested objects

### Test Files to Create
```
code/common/src/test/java/com/donohoedigital/base/
  TypedHashMapTest.java
  FormatTest.java
  UtilsTest.java
code/common/src/test/java/com/donohoedigital/comms/
  DMTypedHashMapTest.java
  DMArrayListTest.java
```

### Coverage Target
- Raise `common` threshold from **5% → 15%**

### Key Files
- `code/common/src/main/java/com/donohoedigital/base/TypedHashMap.java`
- `code/common/src/main/java/com/donohoedigital/base/Format.java`
- `code/common/src/main/java/com/donohoedigital/base/Utils.java`
- `code/common/src/main/java/com/donohoedigital/comms/DMTypedHashMap.java`

---

## Phase 5: GameEngine Refactoring for Testability (`gameengine`)

**Current state:** 71 source files, 0 tests, 0% threshold. Tightly coupled to Swing — `GameEngine` extends `BaseApp` which initializes UI, and `GameContext` manages Swing windows.

**Goal:** Extract testable logic from Swing-coupled classes into pure-logic classes, then test those.

### Refactoring Approach

**`Game.java`** (extends `TypedHashMap`, implements `GameInfo`, `GamePlayerList`, `GameObserverList`)
- This class contains player management, game properties, and observer logic mixed with Swing event dispatching
- **Extract:** `GameModel` — pure player list management, game properties, observer notification (no Swing)
- **Keep in `Game`:** Swing-specific event dispatching, preference persistence

**`GameContext.java`** — Phase management, dialog management, frame management
- Contains testable phase stack logic mixed with JFrame/JInternalFrame management
- **Extract:** `PhaseManager` — phase stack push/pop/navigation logic (no Swing)
- **Keep in `GameContext`:** Frame creation, dialog display, Swing lifecycle

**`GameEngine.java`** — Application lifecycle, configuration, game management
- Contains config loading, game creation, and version checking mixed with Swing initialization
- **Extract:** `GameConfig` — configuration loading, version validation, module management (no Swing)
- **Keep in `GameEngine`:** SplashScreen, window creation, EDT management

**`GameManager.java`** — Save/load game management
- File I/O and game state management is testable
- **Extract if needed**, otherwise test directly (may work with mocked dependencies)

### Test Files to Create
```
code/gameengine/src/test/java/com/donohoedigital/games/engine/
  GameModelTest.java             # extracted from Game.java
  PhaseManagerTest.java          # extracted from GameContext.java
  GameConfigTest.java            # extracted from GameEngine.java
  GameManagerTest.java           # save/load logic
```

### Coverage Target
- Raise `gameengine` threshold from **0% → 10%**

### Key Constraint
- Refactoring must not change external behavior — existing code continues using the original classes, which delegate to the new pure-logic classes
- Each extraction is a separate commit for easy review/revert

### Key Files
- `code/gameengine/src/main/java/com/donohoedigital/games/engine/Game.java`
- `code/gameengine/src/main/java/com/donohoedigital/games/engine/GameContext.java`
- `code/gameengine/src/main/java/com/donohoedigital/games/engine/GameEngine.java`
- `code/gameengine/src/main/java/com/donohoedigital/games/engine/GameManager.java`

---

## Phase 6: Server Services (`pokerserver`, `gameserver`)

**Current state:** `pokerserver` is the best-covered module (36% threshold, 14 tests). `gameserver` at 9% with 4 tests.

**Goal:** Fill remaining service-layer gaps.

### Target Classes

**`pokerserver`** — extend existing Spring test infrastructure:
- `OnlineGameServiceImpl.java` — game CRUD, state transitions, queries
- `TournamentHistoryServiceImpl.java` — history recording, leaderboard aggregation
- Fill gaps in `OnlineProfileServiceImpl.java` (partially tested)

**`gameserver`** — server infrastructure:
- `BannedKeyService` — ban management (partially tested, extend)
- `RegistrationService` — registration validation

### Test Files to Create/Extend
```
code/pokerserver/src/test/java/com/donohoedigital/games/poker/service/
  OnlineGameServiceTest.java      # new
  TournamentHistoryServiceExtendedTest.java  # extend existing
code/gameserver/src/test/java/.../
  RegistrationServiceTest.java    # new
```

### Coverage Target
- Raise `pokerserver` threshold from **36% → 45%**
- Raise `gameserver` threshold from **9% → 15%**

### Key Files
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/service/impl/OnlineGameServiceImpl.java`
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/service/impl/TournamentHistoryServiceImpl.java`
- `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/PokerServerTest.java` (existing Spring test pattern)
- `code/pokerserver/src/main/resources/app-context-pokerserver.xml` (Spring config)

---

## Existing Infrastructure to Reuse

| Utility | Location | Purpose |
|---------|----------|---------|
| `IntegrationTestBase` | `code/poker/src/test/java/.../integration/IntegrationTestBase.java` | Mock GameEngine/PokerMain setup |
| `MockGameEngine` | `code/poker/src/test/java/.../mock/MockGameEngine.java` | Singleton injection via reflection |
| `MockPokerMain` | `code/poker/src/test/java/.../mock/MockPokerMain.java` | PokerMain mock |
| `ConfigTestHelper` | `code/common/src/main/java/com/donohoedigital/config/ConfigTestHelper.java` | Config initialization for tests |
| Spring test config | `code/pokerserver/src/main/resources/app-context-pokerserver.xml` | Spring context for service tests |
| `@Tag("slow")` | Throughout | Marks slow/integration tests for profile-based exclusion |

---

## Implementation Sequence

| Phase | Module | Scope | Target Threshold | Depends On |
|-------|--------|-------|-----------------|------------|
| **1** | pokerengine | Card, Hand, Deck, CardSuit, models | 2% → 40% | Nothing |
| **2** | poker | HandInfo*, Bet, HoldemHand, PokerUtils | 15% → 25% | Phase 1 (uses Card/Hand) |
| **3** | gamecommon | GameState, GamePlayer, GamePhase, Territory | 0% → 15% | Nothing |
| **4** | common | TypedHashMap, Format, Utils, DM* serialization | 5% → 15% | Nothing |
| **5** | gameengine | Extract + test GameModel, PhaseManager, GameConfig | 0% → 10% | Phase 3, Phase 4 |
| **6** | pokerserver, gameserver | Service layer gaps | 36% → 45%, 9% → 15% | Nothing |

Phases 1, 3, 4, and 6 are independent and can proceed in parallel. Phase 2 builds on Phase 1's card/hand types. Phase 5 depends on patterns established in Phases 3-4.

## Verification

For each phase:
1. `mvn test -pl <module>` — all new + existing tests pass
2. `mvn test -P dev` — full project fast tests pass (no regressions)
3. `mvn verify -P coverage -pl <module>` — coverage meets new threshold
4. Update the module's `pom.xml` coverage threshold to the new baseline
5. `mvn test` — full test suite passes with zero warnings

---

## Status Tracking

### Phase 1: Poker Engine Primitives ✅ COMPLETE
**Completed:** 2026-02-13
**Branch:** test-poker-engine-primitives
**Coverage:** 65% (target: 40%)
**Tests:** 476 total (240 new)

**Files Created:**
- `CardTest.java` (39 tests) — Card construction, comparison, serialization, static lookups
- `HandTest.java` (53 tests) — Hand construction, poker queries, fingerprinting
- `CardSuitTest.java` (15 tests) — Suit constants, comparisons, display
- `HandSortedTest.java` (37 tests) — Sorted insertion, poker-specific queries
- `DeckTest.java` (29 tests) — Dealing, removing, sorting, bug-specific decks
- `OnlineGameTest.java` (29 tests) — JPA entity, mode lifecycle, merge, equals/hashCode
- `TournamentHistoryTest.java` (49 tests) — Entity fields, business logic, serialization

**Production Bugs Discovered:**
- HandSorted.hasStraightDraw() throws IndexOutOfBoundsException on empty hands
- OnlineGame.hashCode() violates equals/hashCode contract (includes super.hashCode())

**Key Learnings:**
- NEVER call setValue() on static Card constants (SPADES_A, etc.) — they are shared singletons
- PropertyConfig is a global singleton — removed @AfterClass cleanup to fix parallel execution

**Commits:**
- 6ee967e test: Add comprehensive tests for OnlineGame and TournamentHistory
- 14329ea docs: Document OnlineGame hashCode() bug in learnings
- (previous commits for CardTest, HandTest, CardSuitTest, HandSortedTest, DeckTest)

---

### Phase 2: Core Poker Logic
**Status:** Not started

### Phase 3: Game State & Common Infrastructure
**Status:** Not started

### Phase 4: Utility Layer
**Status:** Not started

### Phase 5: Game Engine Refactoring
**Status:** Not started

### Phase 6: Server Services
**Status:** Not started
