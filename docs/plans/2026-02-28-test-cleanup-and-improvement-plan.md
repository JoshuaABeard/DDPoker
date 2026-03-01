# Test Cleanup & Improvement Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove dead code and tests, build comprehensive server engine tests (fuzzer, validation, edge cases), port shell scenarios to JUnit, and optimize the release gate.

**Architecture:** Three rounds — (1) delete dead code/tests, (2) build new server engine tests in `pokergameserver`, (3) optimize shell scripts. All new tests target the live `ServerHand`/`TournamentEngine` classes. The fuzzer uses `ServerHand` directly with `MockServerGameTable` (inner class pattern from existing `ServerHandTest`).

**Tech Stack:** JUnit 5, AssertJ, `ServerHand`, `ServerDeck`, `ServerPlayer`, `ServerPot`, `TournamentEngine`, `ServerPlayerActionProvider`

**Design doc:** `docs/plans/2026-02-28-test-cleanup-and-improvement-design.md`

---

## Round 1: Dead Code & Dead Test Removal

### Task 1: Delete Dead Test Files

Delete 14 test files that test dead legacy game-logic code paths.

**Files to delete:**

```
code/poker/src/test/java/com/donohoedigital/games/poker/integration/AllAITournamentSimulationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/integration/PokerGameIntegrationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/integration/PokerTableIntegrationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/integration/BasePhaseIntegrationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/integration/ChainPhaseIntegrationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/integration/PhaseContractsIntegrationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/integration/PreviousPhaseIntegrationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/HoldemHandPotDistributionTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/HoldemHandPotCalculationTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerBettingTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/PokerGameChipPoolTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerChipTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/PreFlopBiasTest.java
code/poker/src/test/java/com/donohoedigital/games/poker/ai/V2AlgorithmIntegrationTest.java
```

**Step 1: Delete the files**

```bash
cd code
rm poker/src/test/java/com/donohoedigital/games/poker/integration/AllAITournamentSimulationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/integration/PokerGameIntegrationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/integration/PokerTableIntegrationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/integration/BasePhaseIntegrationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/integration/ChainPhaseIntegrationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/integration/PhaseContractsIntegrationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/integration/PreviousPhaseIntegrationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/HoldemHandPotDistributionTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/HoldemHandPotCalculationTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerBettingTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/PokerGameChipPoolTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/PokerPlayerChipTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/PreFlopBiasTest.java
rm poker/src/test/java/com/donohoedigital/games/poker/ai/V2AlgorithmIntegrationTest.java
```

**Step 2: Run tests to verify nothing breaks**

```bash
mvn test -pl poker
```

Expected: All remaining tests pass. The deleted tests had no dependents (verified: `IntegrationTestBase` and mocks stay because AI tests still extend them).

**Step 3: Commit**

```bash
git add -u
git commit -m "test: remove dead legacy game-logic tests

Delete 14 test files that test dead code paths in the legacy desktop
game engine (HoldemHand, PokerPlayer, PokerGame, PokerTable phase
chain). All gameplay now runs through ServerHand/ServerTournamentDirector
in pokergameserver. The desktop AI tests (V2Player, PokerAI, RuleEngine)
are kept as they test live code pending the AI-to-server port milestone."
```

### Task 2: Delete Dead Production Code

**Files:**
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/PreFlopBias.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` (remove `processAction` method, lines 1143-1174)
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java` (remove dead `sDealPlayableHands_` block, field at line 86-87, dead if-block at lines 517-540)

**Step 1: Delete PreFlopBias.java**

```bash
rm code/poker/src/main/java/com/donohoedigital/games/poker/PreFlopBias.java
```

**Step 2: Remove `processAction` method from PokerPlayer.java**

Open `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` and delete the `processAction(HandAction)` method (lines 1143-1174). This method has zero production callers.

**Step 3: Remove dead `sDealPlayableHands_` code from HoldemHand.java**

Open `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java`:
1. Delete the `sDealPlayableHands_` field declaration (line 86) and its commented-out alternative (line 87)
2. Delete the `// noinspection ConstantValue` comment and the entire `if (sDealPlayableHands_ != null)` block (lines 517-540)
3. If `HandSelectionScheme` import becomes unused after removing this block, delete the import too

**Step 4: Run tests**

```bash
mvn test -pl poker
```

Expected: All tests pass. No production code called these methods/classes.

**Step 5: Commit**

```bash
git add -u
git commit -m "refactor: remove dead production code from legacy engine

- Delete PreFlopBias.java (zero production callers)
- Remove PokerPlayer.processAction() (zero production callers)
- Remove statically-unreachable sDealPlayableHands_ code block in
  HoldemHand.dealCards() (field hardcoded to null)"
```

---

## Round 2: New Server Engine Tests

### Task 3: Port Pot Distribution Scenarios to ServerHandTest

Port the most valuable scenarios from the deleted `HoldemHandPotDistributionTest` that don't already have server-side coverage.

**Files:**
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandTest.java`

**Step 1: Add split-pot test with identical hands**

Add to `ServerHandTest.java` after the existing all-in section (~line 746):

```java
@Test
void testResolve_SplitPot_IdenticalHands() {
    // Two players with identical hands should split the pot evenly
    ServerPlayer alice2 = new ServerPlayer(10, "Alice2", true, 0, 1000);
    ServerPlayer bob2 = new ServerPlayer(11, "Bob2", true, 0, 1000);
    alice2.setSeat(0);
    bob2.setSeat(1);

    MockServerGameTable table2 = new MockServerGameTable(2);
    table2.addPlayer(alice2, 0);
    table2.addPlayer(bob2, 1);

    // Both get A♠K♠ equivalent — same rank cards, different suits
    ServerDeck deck = headsUpDeck(Card.SPADES_A, Card.SPADES_K, Card.HEARTS_A, Card.HEARTS_K);
    ServerHand hand = new ServerHand(table2, 1, 50, 100, 0, 0, 0, 1, deck);
    hand.deal();

    // Both call preflop, check to showdown
    hand.applyPlayerAction(alice2, PlayerAction.call());
    hand.applyPlayerAction(bob2, PlayerAction.check());
    while (hand.getRound() != BettingRound.SHOWDOWN) {
        hand.advanceRound();
    }
    hand.resolve();

    // Split pot: each should have their original 1000
    assertEquals(1000, alice2.getChipCount(), "Split pot: Alice should keep original chips");
    assertEquals(1000, bob2.getChipCount(), "Split pot: Bob should keep original chips");
}
```

**Step 2: Add 4-player all-in test**

```java
@Test
void testAllIn_FourPlayers_StaggeredStacks_PotsCorrectlySplit() {
    // Four players all-in at stacks: 200, 400, 600, 1000
    // Tests 3-way side pot creation with incremental caps
    ServerPlayer p1 = new ServerPlayer(1, "P1", true, 0, 200);
    ServerPlayer p2 = new ServerPlayer(2, "P2", true, 0, 400);
    ServerPlayer p3 = new ServerPlayer(3, "P3", true, 0, 600);
    ServerPlayer p4 = new ServerPlayer(4, "P4", true, 0, 1000);
    p1.setSeat(0); p2.setSeat(1); p3.setSeat(2); p4.setSeat(3);

    MockServerGameTable table4 = new MockServerGameTable(4);
    table4.addPlayer(p1, 0); table4.addPlayer(p2, 1);
    table4.addPlayer(p3, 2); table4.addPlayer(p4, 3);

    // Hand strengths: P1(AA) > P2(KK) > P3(QQ) > P4(JJ)
    // Board is low cards, no straights/flushes
    ServerDeck deck4 = new ServerDeck(List.of(
        Card.SPADES_A, Card.HEARTS_A,   // P1 (seat 0)
        Card.SPADES_K, Card.HEARTS_K,   // P2 (seat 1)
        Card.SPADES_Q, Card.HEARTS_Q,   // P3 (seat 2)
        Card.SPADES_J, Card.HEARTS_J,   // P4 (seat 3)
        Card.DIAMONDS_9,                 // burn
        Card.CLUBS_2, Card.CLUBS_5, Card.DIAMONDS_8, // flop
        Card.HEARTS_9,                   // burn
        Card.HEARTS_3,                   // turn
        Card.CLUBS_9,                    // burn
        Card.CLUBS_6                     // river
    ));

    // Blinds: SB=seat1(P2), BB=seat2(P3), button at seat0
    ServerHand hand = new ServerHand(table4, 1, 10, 20, 0, 0, 1, 2, deck4);
    hand.deal();

    // All go all-in
    hand.applyPlayerAction(p4, PlayerAction.raise(1000)); // UTG (seat 3) raises all-in
    hand.applyPlayerAction(p1, PlayerAction.call());       // Button calls all-in (200)
    hand.applyPlayerAction(p2, PlayerAction.call());       // SB calls all-in (400)
    hand.applyPlayerAction(p3, PlayerAction.call());       // BB calls all-in (600)

    while (hand.getRound() != BettingRound.SHOWDOWN) {
        hand.advanceRound();
    }
    hand.resolve();

    // P1 (AA, shortest) wins main pot: 4×200 = 800
    // P2 (KK) wins 2nd side pot: 3×200 = 600 (eligible: P2, P3, P4 minus P1's cap)
    // P3 (QQ) wins 3rd side pot: 2×200 = 400 (eligible: P3, P4 minus P2's cap)
    // P4 gets excess returned: 400
    assertEquals(800, p1.getChipCount(), "P1 (AA) wins main pot 4×200");
    assertEquals(600, p2.getChipCount(), "P2 (KK) wins 2nd side pot 3×200");
    assertEquals(400, p3.getChipCount(), "P3 (QQ) wins 3rd side pot 2×200");
    assertEquals(400, p4.getChipCount(), "P4 (JJ) gets excess returned");

    // Chip conservation
    assertEquals(2200, p1.getChipCount() + p2.getChipCount() + p3.getChipCount() + p4.getChipCount());
}
```

**Step 3: Add dead-money test (folder contributed to pot)**

```java
@Test
void testResolve_FolderContributedToPot_DeadMoney() {
    // Alice bets, Bob raises, Charlie calls, Alice folds.
    // Alice's contribution is "dead money" in the pot.
    ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
    hand.deal();

    int totalBefore = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();

    hand.applyPlayerAction(alice, PlayerAction.raise(300)); // Alice raises to 300
    hand.applyPlayerAction(bob, PlayerAction.raise(600));   // Bob re-raises to 600
    hand.applyPlayerAction(charlie, PlayerAction.call());   // Charlie calls 600
    hand.applyPlayerAction(alice, PlayerAction.fold());     // Alice folds (lost 300)

    while (hand.getRound() != BettingRound.SHOWDOWN) {
        hand.advanceRound();
    }
    hand.resolve();

    // Alice lost 300, total chips conserved
    int totalAfter = alice.getChipCount() + bob.getChipCount() + charlie.getChipCount();
    assertEquals(totalBefore, totalAfter, "Chip conservation with dead money");
    assertEquals(4700, alice.getChipCount(), "Alice lost her 300 raise");
}
```

**Step 4: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest=ServerHandTest
```

Expected: All tests pass including the new ones.

**Step 5: Commit**

```bash
git add -u
git commit -m "test: port pot distribution scenarios to ServerHandTest

Add split-pot (identical hands), 4-player staggered all-in, and
dead-money (folder contribution) tests. These cover the most
valuable scenarios from the deleted HoldemHandPotDistributionTest,
now tested against the live ServerHand engine."
```

### Task 4: Create Random Scenario Fuzzer Infrastructure

Create the fuzzer infrastructure and first batch of tests.

**Files:**
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandFuzzTest.java`

**Step 1: Create the fuzzer test class with infrastructure**

Create `ServerHandFuzzTest.java` with:
- `FuzzGameSetup` record (playerCount, stacks, smallBlind, bigBlind, ante)
- `generateRandomSetup(Random)` method
- `createTable(FuzzGameSetup)` method
- `playRandomHand(ServerHand, Random)` method that generates random-but-legal actions
- `assertInvariants(ServerHand, List<ServerPlayer>, int totalChipsBefore)` method
- Inner `MockServerGameTable` class (same pattern as `ServerHandTest`)

The fuzzer generates legal actions by checking what's valid at each decision point:
- If no bet is facing: randomly check or bet (random amount between minBet and maxBet)
- If facing a bet: randomly fold, call, or raise (random amount between minRaise and maxRaise)
- 20% chance of all-in (bet/raise to max)

Invariants checked after every hand:
1. Chip conservation: `sum(player.getChipCount()) == totalChipsBefore`
2. No negative chips: `player.getChipCount() >= 0` for all players
3. Hand marked done: `hand.isDone() == true`
4. Pot fully distributed: `hand.getPotSize() == 0` (after resolve)

**Step 2: Add the first set of fuzzer test methods**

```java
@RepeatedTest(200)
void fuzz_randomGames_chipConservation(RepetitionInfo info) {
    Random rng = new Random(10000 + info.getCurrentRepetition());
    FuzzGameSetup setup = generateRandomSetup(rng);
    // Play 5 hands, assert invariants after each
}

@RepeatedTest(200)
void fuzz_headsUp_allVariations(RepetitionInfo info) {
    Random rng = new Random(20000 + info.getCurrentRepetition());
    // 2-player only, stack ratios from 1:1 to 100:1
}

@RepeatedTest(100)
void fuzz_allSameStack_allIns(RepetitionInfo info) {
    Random rng = new Random(30000 + info.getCurrentRepetition());
    // All equal stacks, force at least one all-in per hand
}
```

**Step 3: Run the fuzzer**

```bash
cd code && mvn test -pl pokergameserver -Dtest=ServerHandFuzzTest
```

Expected: All 500 iterations pass. If any fail, the seed is deterministic — reproduce with the specific repetition number.

**Step 4: Commit**

```bash
git add pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandFuzzTest.java
git commit -m "test: add random scenario fuzzer for ServerHand

Comprehensive fuzz testing that generates random-but-legal game
setups (2-10 players, varied stacks/blinds) and random-but-legal
action sequences. Asserts chip conservation, no negative chips,
and clean hand termination after every hand. 500 iterations across
random games, heads-up variations, and same-stack all-ins."
```

### Task 5: Add Remaining Fuzzer Test Methods

Expand the fuzzer with edge-case-specific generators.

**Files:**
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandFuzzTest.java`

**Step 1: Add micro/deep stack fuzzer**

```java
@RepeatedTest(100)
void fuzz_microStacks_vs_deepStacks(RepetitionInfo info) {
    Random rng = new Random(40000 + info.getCurrentRepetition());
    // Mix of stacks: some at 1-2 BB, others at 50-100 BB
    // Tests short-stack all-in against deep-stack scenarios
}
```

**Step 2: Add rapid blind escalation fuzzer**

```java
@RepeatedTest(100)
void fuzz_rapidBlindEscalation(RepetitionInfo info) {
    Random rng = new Random(50000 + info.getCurrentRepetition());
    // Start with low blinds, increase blinds every hand
    // Players quickly become short-stacked
}
```

**Step 3: Add play-to-bust fuzzer**

```java
@RepeatedTest(100)
void fuzz_playToBust_headsUp(RepetitionInfo info) {
    Random rng = new Random(60000 + info.getCurrentRepetition());
    // 2 players, play hands until one is busted (chips == 0)
    // Max 200 hands safety limit
}
```

**Step 4: Add full-table and antes fuzzers**

```java
@RepeatedTest(100)
void fuzz_maxPlayers_fullTable(RepetitionInfo info) {
    Random rng = new Random(70000 + info.getCurrentRepetition());
    // 9-10 players, varied stacks, play 3 hands each
}

@RepeatedTest(100)
void fuzz_antesWithBlinds(RepetitionInfo info) {
    Random rng = new Random(80000 + info.getCurrentRepetition());
    // Blinds + antes at various ratios
}

@RepeatedTest(100)
void fuzz_allCheckToShowdown(RepetitionInfo info) {
    Random rng = new Random(90000 + info.getCurrentRepetition());
    // All players check/call only — never fold, never raise
    // Stress tests pot distribution with maximum showdowns
}
```

**Step 5: Run full fuzzer suite**

```bash
cd code && mvn test -pl pokergameserver -Dtest=ServerHandFuzzTest
```

Expected: All 1100 iterations pass.

**Step 6: Commit**

```bash
git add -u
git commit -m "test: expand fuzzer with edge-case generators

Add micro/deep stack, rapid blind escalation, play-to-bust,
full-table, antes, and all-check-to-showdown fuzzers. Total
1100 iterations covering a wide range of game configurations."
```

### Task 6: Create Action Validation Tests

Test that `ServerPlayerActionProvider.validateAction()` correctly handles illegal inputs.

**Files:**
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerPlayerActionProviderValidationTest.java`

**Step 1: Create the validation test class**

The `validateAction` method is private in `ServerPlayerActionProvider` (line 323). The tests should construct `ActionOptions` records with specific constraints and verify the validated action output.

Options for testing private `validateAction`:
- Option A: Make it package-private (test is in same package)
- Option B: Test through the public `submitAction` flow

Use Option A — change `private` to package-private on `validateAction` (minimal change, test is in same package).

**Step 2: Write parameterized tests**

```java
class ServerPlayerActionProviderValidationTest {

    @Test
    void validate_betExceedsMax_clampsToMax() {
        ActionOptions opts = new ActionOptions(false, false, true, false, true,
            0, 100, 500, 0, 0, 0); // canBet, minBet=100, maxBet=500
        PlayerAction result = validateAction(PlayerAction.bet(999), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(500);
    }

    @Test
    void validate_betBelowMin_clampsToMin() {
        ActionOptions opts = new ActionOptions(false, false, true, false, true,
            0, 100, 500, 0, 0, 0);
        PlayerAction result = validateAction(PlayerAction.bet(50), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(100);
    }

    @Test
    void validate_checkWhenCannotCheck_folds() {
        ActionOptions opts = new ActionOptions(false, true, false, false, true,
            100, 0, 0, 0, 0, 0); // canCall but not canCheck
        PlayerAction result = validateAction(PlayerAction.check(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void validate_raiseWhenCannotRaise_folds() {
        ActionOptions opts = new ActionOptions(false, true, false, false, true,
            100, 0, 0, 0, 0, 0); // canCall only
        PlayerAction result = validateAction(PlayerAction.raise(500), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void validate_raiseBelowMin_clampsToMin() {
        ActionOptions opts = new ActionOptions(false, true, false, true, true,
            100, 0, 0, 200, 1000, 0); // canRaise, minRaise=200, maxRaise=1000
        PlayerAction result = validateAction(PlayerAction.raise(50), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(result.amount()).isEqualTo(200);
    }

    @Test
    void validate_callWhenValid_unchanged() {
        ActionOptions opts = new ActionOptions(false, true, false, false, true,
            100, 0, 0, 0, 0, 0);
        PlayerAction result = validateAction(PlayerAction.call(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.CALL);
    }

    @Test
    void validate_foldAlwaysValid() {
        ActionOptions opts = new ActionOptions(true, true, true, true, true,
            0, 100, 500, 200, 1000, 0);
        PlayerAction result = validateAction(PlayerAction.fold(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void validate_unknownAction_folds() {
        ActionOptions opts = new ActionOptions(true, true, true, true, true,
            0, 100, 500, 200, 1000, 0);
        // ActionType enum may not have an "unknown" value, but the default branch
        // in validateAction handles any unexpected type by folding
    }
}
```

**Step 3: Change `validateAction` to package-private**

In `ServerPlayerActionProvider.java` line 323, change:
```java
private PlayerAction validateAction(PlayerAction action, ActionOptions options) {
```
to:
```java
PlayerAction validateAction(PlayerAction action, ActionOptions options) {
```

**Step 4: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest=ServerPlayerActionProviderValidationTest
```

Expected: All tests pass.

**Step 5: Commit**

```bash
git add -u
git add pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerPlayerActionProviderValidationTest.java
git commit -m "test: add action validation tests for ServerPlayerActionProvider

Parameterized tests covering: bet/raise clamping to min/max,
check-when-cannot-check folds, raise-when-cannot-raise folds,
valid actions pass through unchanged. Makes validateAction
package-private for direct testing."
```

### Task 7: Add Heads-Up Edge Case Tests

Test heads-up poker-specific rules in `ServerHand`.

**Files:**
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandTest.java`

**Step 1: Add heads-up blind posting test**

```java
@Test
void testHeadsUp_ButtonPostsSB_OpponentPostsBB() {
    // In heads-up, button posts SB and opponent posts BB
    ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 1000);
    ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
    btn.setSeat(0);
    bb.setSeat(1);

    MockServerGameTable t = new MockServerGameTable(2);
    t.addPlayer(btn, 0);
    t.addPlayer(bb, 1);

    // button=0, sbSeat=0 (button posts SB), bbSeat=1
    ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
    hand.deal();

    assertEquals(950, btn.getChipCount(), "Button should post SB (50)");
    assertEquals(900, bb.getChipCount(), "Opponent should post BB (100)");
    assertEquals(150, hand.getPotSize(), "Pot should have SB+BB");
}
```

**Step 2: Add heads-up preflop action order test**

```java
@Test
void testHeadsUp_PreflopActionOrder_ButtonActsFirst() {
    ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 1000);
    ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
    btn.setSeat(0);
    bb.setSeat(1);

    MockServerGameTable t = new MockServerGameTable(2);
    t.addPlayer(btn, 0);
    t.addPlayer(bb, 1);

    ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
    hand.deal();

    // Preflop: button/SB acts first in heads-up
    assertEquals(btn, hand.getCurrentPlayerWithInit(),
        "Button/SB should act first preflop in heads-up");
}
```

**Step 3: Add heads-up postflop action order test**

```java
@Test
void testHeadsUp_PostflopActionOrder_BBActsFirst() {
    ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 1000);
    ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
    btn.setSeat(0);
    bb.setSeat(1);

    MockServerGameTable t = new MockServerGameTable(2);
    t.addPlayer(btn, 0);
    t.addPlayer(bb, 1);

    ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
    hand.deal();

    // Complete preflop
    hand.applyPlayerAction(btn, PlayerAction.call());
    hand.applyPlayerAction(bb, PlayerAction.check());
    hand.advanceRound(); // → FLOP

    // Postflop: BB acts first (non-button player)
    assertEquals(bb, hand.getCurrentPlayerWithInit(),
        "BB should act first postflop in heads-up");
}
```

**Step 4: Add heads-up partial blind test**

```java
@Test
void testHeadsUp_PartialBlind_ShortStackedSB() {
    // Button/SB has only 30 chips, SB is 50. Should post partial blind.
    ServerPlayer btn = new ServerPlayer(1, "Button", true, 0, 30);
    ServerPlayer bb = new ServerPlayer(2, "BB", true, 0, 1000);
    btn.setSeat(0);
    bb.setSeat(1);

    MockServerGameTable t = new MockServerGameTable(2);
    t.addPlayer(btn, 0);
    t.addPlayer(bb, 1);

    ServerHand hand = new ServerHand(t, 1, 50, 100, 0, 0, 0, 1);
    hand.deal();

    // Button should post all 30 as partial SB
    assertEquals(0, btn.getChipCount(), "Short-stacked SB should be all-in");
    assertTrue(btn.isAllIn(), "Button should be all-in after partial blind");
    assertEquals(30, hand.getActualSmallBlindPosted(), "Actual SB posted should be 30");

    // Chip conservation
    assertEquals(1030, btn.getChipCount() + bb.getChipCount() + hand.getPotSize());
}
```

**Step 5: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest=ServerHandTest
```

**Step 6: Commit**

```bash
git add -u
git commit -m "test: add heads-up edge case tests to ServerHandTest

Cover heads-up blind posting (button=SB), preflop action order
(button acts first), postflop action order (BB acts first), and
partial blind (short-stacked SB goes all-in)."
```

### Task 8: Add Timeout/Disconnection Tests

Test that the action provider handles timeouts correctly.

**Files:**
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerPlayerActionProviderTest.java`

**Step 1: Read the existing test file to understand patterns**

Read `ServerPlayerActionProviderTest.java` to understand how it sets up the provider and submits actions. Match the existing patterns.

**Step 2: Add timeout test**

The `ServerPlayerActionProvider` has a `humanTimeoutSeconds` parameter. When a human player doesn't act within the timeout, it should auto-fold. Add a test that:
1. Creates a provider with a short timeout (1 second)
2. Requests an action for a human player
3. Does NOT submit an action
4. Verifies the returned action is FOLD after the timeout expires

**Step 3: Add all-in-player-skipped test**

If the current player is already all-in, the action provider should not be consulted (the hand should skip them). This is tested at the `ServerHand` level — verify `getCurrentPlayerWithInit()` skips all-in players.

Add to `ServerHandTest`:

```java
@Test
void testAllInPlayer_SkippedForAction() {
    ServerHand hand = new ServerHand(table, 1, 50, 100, 0, 0, 1, 2);
    hand.deal();

    // Alice goes all-in
    hand.applyPlayerAction(alice, PlayerAction.raise(5000));

    // Next player to act should NOT be alice
    ServerPlayer next = hand.getCurrentPlayerWithInit();
    assertNotEquals(alice, next, "All-in player should be skipped for action");
}
```

**Step 4: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest="ServerPlayerActionProviderTest,ServerHandTest"
```

**Step 5: Commit**

```bash
git add -u
git commit -m "test: add timeout and all-in-skip tests

Test that human players auto-fold on timeout and that all-in
players are skipped when determining the next player to act."
```

### Task 9: Port Shell Scenarios to JUnit

Create fast JUnit equivalents of the highest-value shell scenario scripts.

**Files:**
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandScenarioTest.java`

**Step 1: Create scenario test class**

This class runs deterministic multi-hand scenarios against `ServerHand` directly, mirroring what the shell scripts test but executing in milliseconds instead of 30+ seconds.

```java
/**
 * Deterministic scenario tests mirroring the highest-value shell scripts.
 * These run directly against ServerHand (no HTTP, no Swing, no JVM launch).
 *
 * Shell script equivalents (still run in release gate for E2E validation):
 * - test-chip-conservation.sh
 * - test-all-actions.sh
 * - test-hand-flow.sh
 * - test-blind-posting.sh
 * - test-allin-side-pot.sh
 */
class ServerHandScenarioTest {
    // Uses same MockServerGameTable inner class pattern as ServerHandTest
}
```

**Step 2: Add chip conservation scenario (mirrors test-chip-conservation.sh)**

Play N hands with a call-everything strategy. Assert chip conservation after every hand.

```java
@Test
void scenario_chipConservation_10Hands_callStrategy() {
    // 3 players, 5000 chips each, play 10 hands
    // All players call every bet, check when possible
    // Assert: total chips constant after every hand
}
```

**Step 3: Add all-actions scenario (mirrors test-all-actions.sh)**

Exercise every action type in a controlled sequence.

```java
@Test
void scenario_allActions_exerciseEveryActionType() {
    // Single hand that exercises: FOLD, CHECK, CALL, BET, RAISE, ALL_IN
    // Use card injection for deterministic outcome
}
```

**Step 4: Add hand-flow scenario (mirrors test-hand-flow.sh)**

Verify community cards appear at correct streets.

```java
@Test
void scenario_handFlow_communityCardsAtCorrectStreets() {
    // Inject specific cards, verify:
    // - After deal: 0 community cards
    // - After flop: 3 community cards (correct values)
    // - After turn: 4 community cards
    // - After river: 5 community cards
}
```

**Step 5: Add blind-posting scenario (mirrors test-blind-posting.sh)**

Verify blind/ante deductions at various player counts.

```java
@ParameterizedTest
@ValueSource(ints = {2, 3, 4, 6, 9})
void scenario_blindPosting_variousPlayerCounts(int numPlayers) {
    // Create table with numPlayers
    // Deal a hand, verify SB and BB are correctly posted
    // Verify pot equals SB + BB (+ antes if configured)
}
```

**Step 6: Run tests**

```bash
cd code && mvn test -pl pokergameserver -Dtest=ServerHandScenarioTest
```

**Step 7: Commit**

```bash
git add pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/ServerHandScenarioTest.java
git commit -m "test: port shell scenarios to fast JUnit tests

Mirror highest-value shell scripts (chip conservation, all actions,
hand flow, blind posting) as fast JUnit tests against ServerHand.
Run in <100ms vs 30+s for shell equivalents. Shell scripts stay
in release gate for full E2E validation."
```

---

## Round 3: Shell Script Optimization

### Task 10: Optimize Release Gate to Single-Launch

**Files:**
- Modify: `tests/scenarios/run-release-gate.sh`

**Step 1: Read the current release gate script**

Read `tests/scenarios/run-release-gate.sh` to understand the current flow.

**Step 2: Add build-once and launch-once logic**

Modify `run-release-gate.sh` to:
1. Source `lib.sh`
2. Call `lib_parse_args "$@"`
3. Build once (unless `--skip-build`)
4. Launch the game JVM once (unless `--skip-launch`)
5. Pass `--skip-build --skip-launch` to each script
6. Add a `restart_game()` function that calls `POST /game/start` between scripts to reset state (rather than restarting the JVM)

```bash
#!/usr/bin/env bash
# run-release-gate.sh — Execute release-gate scenario scripts sequentially.
# Builds once, launches once, runs all scripts against the same JVM.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib.sh"

lib_parse_args "$@"

# Build once
if [[ "$SKIP_BUILD" != "true" ]]; then
    log "Building JAR (once for all scripts)..."
    (cd "$CODE_DIR" && mvn clean package -DskipTests -P dev -q)
fi

# Launch once
if [[ "$SKIP_LAUNCH" != "true" ]]; then
    log "Launching game (once for all scripts)..."
    launch_game
    wait_health
fi

SCRIPTS=(
    "test-app-launch.sh"
    # ... all 16 scripts
)

total=${#SCRIPTS[@]}
passed=0
failed=0

for script in "${SCRIPTS[@]}"; do
    echo
    echo "--- Running: $script ---"
    if bash "$SCRIPT_DIR/$script" --skip-build --skip-launch "$@"; then
        echo "PASS: $script"
        passed=$((passed + 1))
    else
        echo "FAIL: $script"
        failed=$((failed + 1))
    fi
done

# Summary and cleanup
```

**Step 3: Verify all 16 release gate scripts support --skip-build --skip-launch**

Check each script in the SCRIPTS array and verify it properly handles these flags via `lib_parse_args`. Fix any that don't.

**Step 4: Test the optimized release gate**

```bash
bash tests/scenarios/run-release-gate.sh
```

Expected: All 16 scripts pass. Total time should be roughly 50% less than before.

**Step 5: Commit**

```bash
git add tests/scenarios/run-release-gate.sh
git commit -m "perf: optimize release gate to single build and launch

Build JAR once and launch game JVM once for all 16 release gate
scripts. Each script runs with --skip-build --skip-launch against
the shared JVM. Eliminates 15 redundant JVM startups."
```

---

## Verification

After all tasks are complete:

**Step 1: Run full test suite**

```bash
cd code && mvn test
```

Expected: All tests pass (fewer total tests due to deleted dead tests, but same or better coverage of live code).

**Step 2: Run dev profile**

```bash
cd code && mvn test -P dev
```

Expected: All non-slow/non-integration tests pass, including new fuzzer and scenario tests (they have no tags, so they run in dev profile).

**Step 3: Run release gate**

```bash
bash tests/scenarios/run-release-gate.sh
```

Expected: All 16 scripts pass with the single-launch optimization.
