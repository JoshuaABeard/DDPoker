# Desktop Thin-Client Cleanup Implementation Plan

**Status:** READY

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove all poker logic from the desktop client across three phases: dead code deletion, server API extensions, and UI migration to server data.

**Architecture:** Phase 1 deletes unreferenced dead code. Phase 2 extends the server's `AdvisorService` and `PokerSimulationService` with improvement odds, hand potential, and multi-hand showdown support. Phase 3 rewires desktop UI panels to consume server-provided data and deletes all client-side poker math libraries.

**Tech Stack:** Java 21, Spring Boot 3.5.8, Maven, WebSocket (ADVISOR_UPDATE messages), REST (POST /api/v1/poker/simulate)

**Design:** See `docs/plans/2026-03-01-desktop-thin-client-cleanup-design.md`

---

## Task Overview

| # | Task | Phase | Depends On |
|---|------|-------|-----------|
| 1 | Delete dead AI support classes | 1 | — |
| 2 | Delete dead poker utility classes | 1 | — |
| 3 | Clean up references to deleted classes | 1 | 1, 2 |
| 4 | Add improvement odds to AdvisorService | 2 | — |
| 5 | Add hand potential to AdvisorService | 2 | — |
| 6 | Extend PokerSimulationService for multi-hand showdown | 2 | — |
| 7 | Wire advisor extensions into WebSocket broadcasts | 2 | 4, 5 |
| 8 | Migrate ImproveOdds dashboard to server data | 3 | 7 |
| 9 | Migrate PokerStatsPanel to server data | 3 | 7 |
| 10 | Migrate PokerSimulatorPanel and PokerShowdownPanel to REST | 3 | 6 |
| 11 | Remove WeightGridPanel and PocketWeights | 3 | — |
| 12 | Delete remaining client-side poker math libraries | 3 | 8, 9, 10, 11 |
| 13 | Final cleanup and verification | 3 | 12 |

---

## Phase 1: Dead Code Removal

### Task 1: Delete Dead AI Support Classes

**Files to DELETE:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/AIOutcome.java` (365 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/BetRange.java` (269 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/BooleanTracker.java` (214 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/FloatTracker.java` (196 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/HandProbabilityMatrix.java` (118 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/AIOutcomeIntegrationTest.java` (364 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/BetRangeTest.java` (518 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/BooleanTrackerTest.java` (749 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/FloatTrackerTest.java` (762 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/HandProbabilityMatrixTest.java` (802 lines)

**Context:** These classes were previously used by `PokerAI`, `V1Player`, `V2Player`, `RuleEngine`, and `ClientStrategyProvider` — all removed in the AI consolidation (2026-03-01). Equivalent classes exist in `pokergamecore` for server-side use. The test files only test the dead production classes.

**Step 1: Delete production files**

```bash
cd code
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/AIOutcome.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/BetRange.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/BooleanTracker.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/FloatTracker.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/HandProbabilityMatrix.java
```

**Step 2: Delete test files**

```bash
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/AIOutcomeIntegrationTest.java
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/BetRangeTest.java
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/BooleanTrackerTest.java
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/FloatTrackerTest.java
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/HandProbabilityMatrixTest.java
```

**Step 3: Verify build compiles**

Run: `cd code && mvn compile -pl poker -P fast`
Expected: BUILD SUCCESS (no other files import these classes)

**Step 4: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All tests pass

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove dead AI support classes from desktop client

Delete AIOutcome, BetRange, BooleanTracker, FloatTracker,
HandProbabilityMatrix and their tests. These were orphaned when
PokerAI/V1Player/V2Player/RuleEngine were removed in the AI
consolidation. Equivalent classes exist in pokergamecore for
server-side use."
```

---

### Task 2: Delete Dead Poker Utility Classes

**Files to DELETE:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemExpert.java` (245 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/OtherTables.java` (433 lines)

**Context:** `HoldemExpert` is only referenced in commented-out debug code in `HoldemHand.java:2387-2388`. `OtherTables` is only referenced in comments in `ServerTournamentDirector.java`, `ShowTournamentTable.java`, and `TournamentEngine.java`. The server has `SklankskyRanking` in pokergamecore as a replacement for `HoldemExpert`. Table consolidation is handled server-side by `ServerTournamentDirector`.

**Step 1: Delete production files**

```bash
cd code
git rm poker/src/main/java/com/donohoedigital/games/poker/HoldemExpert.java
git rm poker/src/main/java/com/donohoedigital/games/poker/OtherTables.java
```

**Step 2: Verify build compiles**

Run: `cd code && mvn compile -pl poker -P fast`
Expected: BUILD SUCCESS

**Step 3: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All tests pass

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove dead HoldemExpert and OtherTables classes

HoldemExpert only referenced in commented-out debug code. Server has
SklankskyRanking in pokergamecore. OtherTables only referenced in
comments; table consolidation handled server-side."
```

---

### Task 3: Clean Up References to Deleted Classes

**Files to MODIFY:**
- `docs/memory.md:36` — Remove entry referencing `AIOutcome`, `BetRange`, `ClientStrategyProvider`
- `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java:2387-2388` — Remove commented-out `HoldemExpert` references
- `code/poker/src/main/java/com/donohoedigital/games/poker/ShowTournamentTable.java:943-945` — Update comments referencing `OtherTables`
- `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java:713` — Update comment referencing `OtherTables`
- `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/TournamentEngine.java:321` — Update comment referencing `OtherTables`

**Step 1: Update docs/memory.md**

Remove line 36 which reads:
```
- [poker] AI code (AIOutcome, BetRange, ClientStrategyProvider) uses Math.random()...
```
This entry references deleted classes and is no longer relevant.

**Step 2: Remove commented-out HoldemExpert references**

In `HoldemHand.java`, remove the two commented-out lines at ~2387-2388:
```java
// int nRank = HoldemExpert.getSklanskyRank(player.getHandSorted());
// int nGroup = HoldemExpert.getGroupFromRank(nRank);
```

**Step 3: Update OtherTables comments**

In `ShowTournamentTable.java:943-945`, update the comment to remove `OtherTables` reference — describe the behavior directly instead of referencing a deleted class.

In `ServerTournamentDirector.java:713`, update the comment to remove `OtherTables` reference.

In `TournamentEngine.java:321`, update the comment to remove `OtherTables` reference.

**Step 4: Search for any remaining references**

```bash
cd code && grep -rn "AIOutcome\|BetRange\|BooleanTracker\|FloatTracker\|HandProbabilityMatrix\|HoldemExpert\|OtherTables" --include="*.java" --include="*.xml" --include="*.md" | grep -v "pokergamecore" | grep -v "Binary"
```

Expected: Zero results (all references cleaned)

**Step 5: Verify build**

Run: `cd code && mvn test -P dev`
Expected: All tests pass across all modules

**Step 6: Commit**

```bash
git add -A
git commit -m "chore: clean up references to deleted dead code classes

Remove commented-out HoldemExpert references in HoldemHand.java.
Update comments in ServerTournamentDirector, ShowTournamentTable,
and TournamentEngine that referenced deleted OtherTables class.
Remove obsolete docs/memory.md entry about AIOutcome/BetRange."
```

---

## Phase 2: Extend Server APIs

### Task 4: Add Improvement Odds to AdvisorService

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorService.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorResult.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/AdvisorServiceTest.java`

**Context:** The desktop `ImproveOdds` dashboard uses `HandFutures` to compute the probability of improving to specific hand types (flush, straight, trips, etc.) on flop/turn. Port this logic into `AdvisorService` so it's included in `ADVISOR_UPDATE` messages.

`HandFutures` works by: (1) evaluating current hand with `HandInfoFaster`, (2) enumerating all remaining single-card draws (turn or river), (3) re-evaluating with each possible card, (4) counting how many improve to each hand type.

**Step 1: Write failing tests**

Add tests to `AdvisorServiceTest.java`:

```java
@Test
void improvementOdds_flopWithFlushDraw_includesFlushOdds() {
    // Ah 2h on a board of Kh 7h 3c = flush draw (9 outs)
    Card[] hole = {Card.getCard("Ah"), Card.getCard("2h")};
    Card[] community = {Card.getCard("Kh"), Card.getCard("7h"), Card.getCard("3c")};
    AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500);
    assertThat(result.improvementOdds()).isNotNull();
    assertThat(result.improvementOdds().get("FLUSH")).isGreaterThan(15.0); // ~19.1%
}

@Test
void improvementOdds_river_returnsNull() {
    // 5 community cards = river, no improvement possible
    Card[] hole = {Card.getCard("Ah"), Card.getCard("Kh")};
    Card[] community = {Card.getCard("2c"), Card.getCard("7d"), Card.getCard("Js"),
                        Card.getCard("3h"), Card.getCard("9c")};
    AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500);
    assertThat(result.improvementOdds()).isNull();
}

@Test
void improvementOdds_preflop_returnsNull() {
    Card[] hole = {Card.getCard("Ah"), Card.getCard("Kh")};
    Card[] community = {};
    AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500);
    assertThat(result.improvementOdds()).isNull();
}
```

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl pokergameserver -Dtest=AdvisorServiceTest -P fast`
Expected: FAIL — `improvementOdds()` method doesn't exist on `AdvisorResult`

**Step 3: Add `improvementOdds` field to AdvisorResult**

Add a `Map<String, Double> improvementOdds` field to the `AdvisorResult` record. Null when not applicable (preflop, river).

**Step 4: Implement improvement odds calculation in AdvisorService**

Port the core logic from `HandFutures`:
1. Only compute when community has 3 or 4 cards (flop/turn)
2. Get current hand type via `HandInfoFaster`
3. For each remaining card in the deck (47 or 46 cards):
   - Add card to community, evaluate with `HandInfoFaster`
   - If new hand type > current hand type, increment count for that type
4. Convert counts to percentages
5. Return map of hand type name -> improvement percentage (only entries > 0)

Hand type names: `"ONE_PAIR"`, `"TWO_PAIR"`, `"TRIPS"`, `"STRAIGHT"`, `"FLUSH"`, `"FULL_HOUSE"`, `"FOUR_OF_A_KIND"`, `"STRAIGHT_FLUSH"`, `"ROYAL_FLUSH"`

**Step 5: Run tests to verify they pass**

Run: `cd code && mvn test -pl pokergameserver -Dtest=AdvisorServiceTest -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git add -A
git commit -m "feat: add improvement odds to AdvisorService

Calculate probability of improving to each hand type on flop/turn.
Enumerates remaining deck cards and evaluates hand improvement.
Returns null on preflop/river where improvement odds are meaningless."
```

---

### Task 5: Add Hand Potential to AdvisorService

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorService.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorResult.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/AdvisorServiceTest.java`

**Context:** The desktop `PokerStatsPanel` uses `HandPotential` to show positive/negative potential (probability of hand improving/worsening relative to opponents). Port a simplified version into `AdvisorService`.

`HandPotential` works by: (1) evaluating current hand strength vs all possible opponent hands, (2) enumerating future board cards, (3) re-evaluating strength after each, (4) computing what percentage of currently-ahead hands fall behind (negative) and currently-behind hands get ahead (positive).

**Step 1: Write failing tests**

```java
@Test
void handPotential_flopWithDraws_hasPositivePotential() {
    // Flush draw = high positive potential
    Card[] hole = {Card.getCard("Ah"), Card.getCard("Kh")};
    Card[] community = {Card.getCard("7h"), Card.getCard("3h"), Card.getCard("9c")};
    AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500);
    assertThat(result.positivePotential()).isGreaterThan(10.0);
}

@Test
void handPotential_river_returnsNull() {
    Card[] hole = {Card.getCard("Ah"), Card.getCard("Kh")};
    Card[] community = {Card.getCard("2c"), Card.getCard("7d"), Card.getCard("Js"),
                        Card.getCard("3h"), Card.getCard("9c")};
    AdvisorResult result = service.compute(hole, community, 0, 0, 1, 500);
    assertThat(result.positivePotential()).isNull();
    assertThat(result.negativePotential()).isNull();
}
```

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl pokergameserver -Dtest=AdvisorServiceTest -P fast`
Expected: FAIL

**Step 3: Add potential fields to AdvisorResult**

Add `Double positivePotential` and `Double negativePotential` fields. Null when not applicable (preflop with < 3 community, river).

**Step 4: Implement hand potential calculation**

Simplified version of `HandPotential`:
1. Only compute when community has 3 or 4 cards
2. Sample N random opponent hands (use 200 samples for performance)
3. For each opponent hand:
   - Evaluate current: are we ahead, behind, or tied?
   - Enumerate remaining community cards
   - Re-evaluate: are we now ahead, behind, or tied?
   - Track transitions: behind->ahead (positive), ahead->behind (negative)
4. Positive potential = behind->ahead / total behind
5. Negative potential = ahead->behind / total ahead
6. Return as percentages (0-100)

**Step 5: Run tests to verify they pass**

Run: `cd code && mvn test -pl pokergameserver -Dtest=AdvisorServiceTest -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git add -A
git commit -m "feat: add hand potential to AdvisorService

Calculate positive potential (behind->ahead probability) and negative
potential (ahead->behind probability) on flop/turn. Uses sampling of
opponent hands for performance."
```

---

### Task 6: Extend PokerSimulationService for Multi-Hand Showdown

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/PokerSimulationService.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/SimulationResult.java`
- Modify: `code/api/src/main/java/com/donohoedigital/poker/api/dto/SimulationRequest.java`
- Modify: `code/api/src/main/java/com/donohoedigital/poker/api/controller/SimulationController.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/PokerSimulationServiceTest.java`

**Context:** The desktop `PokerShowdownPanel` computes equity for multiple known hands simultaneously (e.g., AA vs KK vs QQ with a specific board). The current `PokerSimulationService` only supports one player hand vs N opponents. Extend it to support multiple known hands with per-hand results.

**Step 1: Write failing tests**

```java
@Test
void multiHand_AAvsKK_returnsPerHandEquity() {
    List<List<String>> hands = List.of(
        List.of("Ah", "Ad"),
        List.of("Kh", "Kd")
    );
    SimulationResult result = service.simulateMultiHand(hands, List.of(), 10000);
    assertThat(result.handResults()).hasSize(2);
    assertThat(result.handResults().get(0).win()).isGreaterThan(70.0); // AA ~81%
    assertThat(result.handResults().get(1).win()).isGreaterThan(10.0); // KK ~19%
}

@Test
void multiHand_withCommunity_deterministicOnRiver() {
    List<List<String>> hands = List.of(
        List.of("Ah", "Kh"),
        List.of("2c", "7d")
    );
    List<String> community = List.of("Ac", "Kc", "3s", "8d", "Jh");
    SimulationResult result = service.simulateMultiHand(hands, community, 1000);
    assertThat(result.handResults().get(0).win()).isEqualTo(100.0); // AK has two pair
}
```

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl pokergameserver -Dtest=PokerSimulationServiceTest -P fast`
Expected: FAIL — `simulateMultiHand()` doesn't exist

**Step 3: Add `simulateMultiHand` method and update result types**

Add to `SimulationResult`:
```java
public record HandResult(double win, double tie, double loss) {}
```
Add `List<HandResult> handResults` field (null for single-hand mode).

Add `simulateMultiHand(List<List<String>> hands, List<String> communityCards, int iterations)`:
1. Parse all card strings
2. Validate no duplicates across all hands + community
3. Build remaining deck
4. For each iteration:
   - Shuffle remaining deck, deal missing community cards
   - Evaluate each hand with `ServerHandEvaluator`
   - Determine winner(s) by highest score; handle ties
   - Increment win/tie/loss counters per hand
5. Convert to percentages

**Step 4: Add REST endpoint support**

Extend `SimulationRequest` with optional `List<List<String>> allHands` field. When present, use `simulateMultiHand` instead of `simulate`.

**Step 5: Run tests to verify they pass**

Run: `cd code && mvn test -pl pokergameserver -Dtest=PokerSimulationServiceTest -P fast`
Run: `cd code && mvn test -pl api -Dtest=SimulationControllerTest -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git add -A
git commit -m "feat: add multi-hand showdown simulation

Extend PokerSimulationService with simulateMultiHand() for comparing
multiple known hands simultaneously. Returns per-hand win/tie/loss
percentages. Used by desktop PokerShowdownPanel."
```

---

### Task 7: Wire Advisor Extensions into WebSocket Broadcasts

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageData.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcasterTest.java`

**Context:** The `ADVISOR_UPDATE` WebSocket message currently sends `handRank`, `handDescription`, `equity`, `potOdds`, `recommendation`, `startingHandCategory`, `startingHandNotation`. Extend it to also include the new `improvementOdds`, `positivePotential`, and `negativePotential` fields from Tasks 4-5.

**Step 1: Write failing tests**

Test that `ADVISOR_UPDATE` messages on flop include improvement odds and potential data.

**Step 2: Extend AdvisorData record in ServerMessageData**

Add fields to `ServerMessageData.AdvisorData`:
```java
record AdvisorData(
    Integer handRank, String handDescription, double equity,
    double potOdds, String recommendation,
    String startingHandCategory, String startingHandNotation,
    Map<String, Double> improvementOdds,    // NEW — null on preflop/river
    Double positivePotential,                // NEW — null on preflop/river
    Double negativePotential                 // NEW — null on preflop/river
) implements ServerMessageData {}
```

**Step 3: Update GameEventBroadcaster.sendAdvisorUpdates()**

Pass the new fields from `AdvisorResult` through to `AdvisorData` when constructing the message (~line 757-760).

**Step 4: Run tests**

Run: `cd code && mvn test -pl pokergameserver -Dtest=GameEventBroadcasterTest -P fast`
Expected: PASS

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: include improvement odds and hand potential in ADVISOR_UPDATE

Extend WebSocket ADVISOR_UPDATE messages with improvementOdds map,
positivePotential, and negativePotential. Sent on flop/turn only."
```

---

## Phase 3: Migrate UI Panels + Delete Client Math

### Task 8: Migrate ImproveOdds Dashboard to Server Data

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/ImproveOdds.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (if advisor data not already cached)
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGameState.java` (store advisor data)

**Context:** `ImproveOdds` currently creates a `HandFutures` object and calls `getOddsImproveTo()` for each hand type. After Task 7, the server sends `improvementOdds` in `ADVISOR_UPDATE`. The dashboard should read this pre-computed data instead.

**Step 1: Ensure desktop client stores advisor data from WebSocket**

Check if `WebSocketTournamentDirector` already handles `ADVISOR_UPDATE` messages and stores the data in game state. If not, add a handler that:
1. Parses the `ADVISOR_UPDATE` message
2. Stores `improvementOdds`, `positivePotential`, `negativePotential` on `PokerGameState` or a new `AdvisorState` object
3. Fires a property change event so dashboard widgets refresh

**Step 2: Rewrite ImproveOdds to read server data**

Replace the `HandFutures`-based calculation block (~lines 91-120) with:
1. Read `improvementOdds` map from game state
2. If null (preflop/river), show empty/N/A
3. Otherwise, iterate through the map entries and build the same HTML table

Remove imports of `HandFutures`, `HandInfoFaster`, `HandInfo`.

**Step 3: Verify build**

Run: `cd code && mvn compile -pl poker -P fast`
Expected: BUILD SUCCESS

**Step 4: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All tests pass

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: migrate ImproveOdds dashboard to server-provided data

ImproveOdds now reads improvementOdds from ADVISOR_UPDATE WebSocket
messages instead of computing locally with HandFutures."
```

---

### Task 9: Migrate PokerStatsPanel to Server Data

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerStatsPanel.java`

**Context:** `PokerStatsPanel` has 5 tabs using `HandPotential` (FLOP/TURN/RIVER), `HandLadder` (LADDER), and `HandStrength` (STRENGTH). After Tasks 4-5 and 7, the server sends hand potential and equity in `ADVISOR_UPDATE`.

**Step 1: Replace HandPotential tabs**

In `UpdateThread.run()`, replace `new HandPotential(pocket_, community_)` with reading `positivePotential`/`negativePotential` from game state. Simplify the HTML output to show positive/negative potential percentages.

**Step 2: Replace HandStrength tab**

Replace `new HandStrength()` and `strength_.toHTML(pocket_, community_, 9)` with reading `equity` from the existing `ADVISOR_UPDATE` data. The server already provides equity against N opponents.

**Step 3: Replace HandLadder tab**

The ladder view is a detailed breakdown of how many possible opponent hands beat/tie/lose to yours. This is specialized — for the initial migration, either:
- Remove the LADDER tab (simplest — it's a rarely-used feature)
- Or show a simplified version using the equity percentage

Recommendation: remove the LADDER tab. It's a niche analysis tool.

**Step 4: Remove imports**

Remove imports of `HandPotential`, `HandStrength`, `HandLadder`, `HandInfo`, `HandInfoFast`.

**Step 5: Verify build and run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: migrate PokerStatsPanel to server-provided advisor data

Replace local HandPotential/HandStrength computation with data from
ADVISOR_UPDATE WebSocket messages. Remove LADDER tab (niche feature
without server equivalent)."
```

---

### Task 10: Migrate PokerSimulatorPanel and PokerShowdownPanel to REST

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerSimulatorPanel.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerShowdownPanel.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/server/GameServerRestClient.java` (add simulate method)

**Context:** Both panels run `HoldemSimulator` in background threads. Replace with REST calls to `POST /api/v1/poker/simulate` on the embedded server.

**Step 1: Add simulation REST method to GameServerRestClient**

Add a method that calls the embedded server's simulate endpoint:
```java
public SimulationResult simulate(SimulationRequest request) {
    // POST to http://localhost:{port}/api/v1/poker/simulate
    // Parse JSON response to SimulationResult
}
```

Use the existing HTTP client infrastructure in `GameServerRestClient`.

**Step 2: Rewrite PokerShowdownPanel.UpdateThread**

Replace `HoldemSimulator.simulate(hands, community, numSims, progress)` with:
1. Build `SimulationRequest` from the UI's selected hands + community cards
2. Call `gameServerRestClient.simulate(request)`
3. Map `SimulationResult.handResults()` to the existing `StatResult[]` display format
4. Progress callback: since REST is fire-and-forget, show indeterminate progress bar during the call

**Step 3: Rewrite PokerSimulatorPanel.UpdateThread**

Replace `HoldemSimulator.simulate(pocket, community, progress)` with a REST call. The server's simulation doesn't group by hand category — simplify to show overall win/tie/loss equity instead.

**Step 4: Remove HoldemSimulator imports**

**Step 5: Verify build and run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: migrate simulator panels to server REST API

PokerSimulatorPanel and PokerShowdownPanel now call the embedded
server's POST /api/v1/poker/simulate instead of running Monte Carlo
simulations locally via HoldemSimulator."
```

---

### Task 11: Remove WeightGridPanel and PocketWeights

**Files to MODIFY:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/gui/WeightGridPanel.java` — DELETE
- `code/poker/src/main/java/com/donohoedigital/games/poker/SimulatorDialog.java` — Remove WeightGridPanel tab

**Context:** `WeightGridPanel` is the sole consumer of `PocketWeights`. It's an AI debugging visualization showing inferred opponent hand distributions. This is a development tool, not a user-facing feature. Remove it rather than migrating to server.

**Step 1: Remove WeightGridPanel tab from SimulatorDialog**

In `SimulatorDialog.java`, find where `WeightGridPanel` is created and added as a tab. Remove the tab creation code.

**Step 2: Delete WeightGridPanel.java**

```bash
git rm code/poker/src/main/java/com/donohoedigital/games/poker/ai/gui/WeightGridPanel.java
```

**Step 3: Verify build**

Run: `cd code && mvn compile -pl poker -P fast`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove WeightGridPanel AI debugging visualization

Remove the AI hand weight heatmap from the calculator dialog. This
was a development/debugging tool showing PocketWeights internals,
not a user-facing feature."
```

---

### Task 12: Delete Remaining Client-Side Poker Math Libraries

**Status: PARTIALLY COMPLETED (2026-03-01)**

**Completed (commits 62a17303, 26e83f80):**
- Deleted: `HandLadder.java`, `ai/PocketOdds.java`, `ai/PocketRanks.java`, `ai/PocketScores.java`, `ai/SimpleBias.java`, `ai/PocketMatrixFloat.java`, `ai/PocketMatrixInt.java`, `ai/PocketMatrixShort.java`
- Deleted their test files
- Cleaned up `PokerStats.java`: removed dead DEBUG block that used `HandInfo`
- Cleaned up `UiDashboardWidgetsHandler.java`: replaced local `HandFutures`/`HandInfo`-based `computeImproveOdds()` with server-provided data from `AdvisorState`

**Blocked — non-trivial callers prevent deletion:**

`HandInfo.java`, `HandInfoFast.java`, `HandStrength.java`, `HandFutures.java`, `HandPotential.java` are used by `ServerAIContext.java` in `pokerserver` to implement the server-side V1 AI algorithm. That class uses:
- `HandInfoFast.getTypeFromScore()`, `HandInfoFast.getCards()`, `new HandInfoFast()` — hand type and rank extraction
- `HandInfo.isOurHandInvolved()`, `HandInfo.isNutFlush()` — static utility methods
- `HandStrength` — hand strength Monte Carlo simulation
- `HandFutures` — improvement odds Monte Carlo simulation
- `HandInfo.FLUSH`, `HandInfo.TRIPS` — hand type constants

These cannot be removed without replacing the V1 server AI's hand evaluation with pokergamecore equivalents (`HandInfoFaster`, `PureHandPotential`, etc.). That is a separate engineering task.

Additionally:
- `HoldemHand.java` uses `HandInfo` for core pot resolution (`preResolvePot`, `resolvePot`) — can only replace with `HandInfoFaster` if `getBest()` replacement is found
- `Showdown.java`, `WebSocketTournamentDirector.java`, `MyHand.java` use `HandInfo.getBest()` for display — `HandInfoFaster` has no `getBest()` equivalent
- `ImpExpParadise.java`, `PokerDatabase.java`, `HandHistoryPanel.java` use `HandInfoFast` for hand export/display
- `PokerPlayer.java` uses `HandStrength` and `HandPotential` for local hand evaluation used in `HandStrengthDash` display
- `SimulatorHandler.java` (dev) uses `HoldemSimulator` — plan said to delegate to server REST API, but desktop doesn't have that client

`PocketMatrixByte.java` and `PocketMatrixString.java` are kept because `AdvisorGridPanel.java` (a UI panel) uses them.

**Files to DELETE (production):**
- `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemSimulator.java` (820 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandStrength.java` (289 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandPotential.java` (778 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandFutures.java` (321 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandInfo.java` (1,039 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandInfoFast.java` (835 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HandLadder.java` (339 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketWeights.java` (711 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketOdds.java` (208 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketRanks.java` (185 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketScores.java` (163 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/SimpleBias.java` (219 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixByte.java` (106 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixFloat.java` (114 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixInt.java` (106 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixShort.java` (106 lines)
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixString.java` (106 lines)

**Files to DELETE (tests):**
- `code/poker/src/test/java/com/donohoedigital/games/poker/HandStrengthTest.java` (297 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/PocketRanksTest.java` (411 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/PocketScoresTest.java` (404 lines)
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/PocketOddsTest.java` (if exists)
- Any other test files for the above classes

**Files to MODIFY:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` — Remove `getHandStrength()` method that uses `HandStrength` (replace with cached value from server)
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerAIContext.java` — Remove `HandStrength` usage; use `AdvisorService.compute()` or pokergamecore's hand evaluation instead
- `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/ImproveOdds.java` — Verify no remaining HandFutures imports
- `code/poker/src/dev/java/com/donohoedigital/games/poker/control/UiDashboardWidgetsHandler.java` — Remove `HandFutures`, `HandInfo`, `HandInfoFaster` imports; use server data
- `code/poker/src/dev/java/com/donohoedigital/games/poker/control/HandResultHandler.java` — Remove `HandInfoFast` import; use server's pre-computed ranking
- `code/poker/src/dev/java/com/donohoedigital/games/poker/control/SimulatorHandler.java` — Remove `HoldemSimulator` import; delegate to server

**Step 1: Delete all production files**

```bash
cd code
git rm poker/src/main/java/com/donohoedigital/games/poker/HoldemSimulator.java
git rm poker/src/main/java/com/donohoedigital/games/poker/HandStrength.java
git rm poker/src/main/java/com/donohoedigital/games/poker/HandPotential.java
git rm poker/src/main/java/com/donohoedigital/games/poker/HandFutures.java
git rm poker/src/main/java/com/donohoedigital/games/poker/HandInfo.java
git rm poker/src/main/java/com/donohoedigital/games/poker/HandInfoFast.java
git rm poker/src/main/java/com/donohoedigital/games/poker/HandLadder.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketWeights.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketOdds.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketRanks.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketScores.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/SimpleBias.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixByte.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixFloat.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixInt.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixShort.java
git rm poker/src/main/java/com/donohoedigital/games/poker/ai/PocketMatrixString.java
```

**Step 2: Delete test files**

```bash
git rm poker/src/test/java/com/donohoedigital/games/poker/HandStrengthTest.java
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/PocketRanksTest.java
git rm poker/src/test/java/com/donohoedigital/games/poker/ai/PocketScoresTest.java
# Delete PocketOddsTest.java if it exists
```

**Step 3: Fix compilation errors**

Fix all files listed in "Files to MODIFY" above. For each:
- `PokerPlayer.getHandStrength()`: Return the cached `handStrength_` field that's set by server data, instead of computing locally
- `ServerAIContext`: Use pokergamecore's hand evaluation directly (it already has `HandInfoFaster` available from the `pokerengine` dependency)
- Dev control handlers: Delegate to server REST APIs instead of computing locally

**Step 4: Verify build compiles**

Run: `cd code && mvn compile -P fast`
Expected: BUILD SUCCESS across all modules

**Step 5: Run all tests**

Run: `cd code && mvn test -P dev`
Expected: All tests pass

**Step 6: Commit**

```bash
git add -A
git commit -m "refactor: delete all client-side poker math libraries

Remove HoldemSimulator, HandStrength, HandPotential, HandFutures,
HandInfo, HandInfoFast, HandLadder, PocketWeights, PocketOdds,
PocketRanks, PocketScores, SimpleBias, and all PocketMatrix classes
from the desktop client. All poker calculations now performed
server-side via WebSocket (advisor) and REST (simulator).

~6,500 lines of poker math removed from the desktop module."
```

---

### Task 13: Final Cleanup and Verification

**Step 1: Search for any remaining poker math references**

```bash
cd code && grep -rn "HandStrength\|HandPotential\|HandFutures\|HandInfo\b\|HandInfoFast\b\|HandLadder\|HoldemSimulator\|HoldemExpert\|PocketWeights\|PocketOdds\|PocketRanks\|PocketScores\|SimpleBias\|PocketMatrix\|OtherTables\|AIOutcome\|BetRange\|BooleanTracker\|FloatTracker\|HandProbabilityMatrix" --include="*.java" poker/src/ | grep -v "test/" | grep -v "Binary"
```

Expected: Zero matches (or only references to `pokergamecore`/`pokerengine` equivalents)

**Step 2: Run full test suite**

Run: `cd code && mvn test -P dev`
Expected: All tests pass

**Step 3: Start desktop client and verify**

```bash
cd code && mvn clean package -DskipTests -P dev
java -jar poker/target/DDPokerCE-3.3.0.jar
```

Manual verification:
- Start a practice game — AI players make decisions, human can play
- Open the Calculator Tool (Simulator dialog) — panels load without error
- Check the dashboard — HandStrength and ImproveOdds display values
- Play through a complete hand — all phases animate correctly

**Step 4: Update design doc status**

Change status in `docs/plans/2026-03-01-desktop-thin-client-cleanup-design.md` to `COMPLETED (YYYY-MM-DD)`.

**Step 5: Commit**

```bash
git add -A
git commit -m "docs: mark desktop thin-client cleanup as COMPLETED"
```

---

## Verification Checklist

After all tasks:
- [ ] `cd code && mvn test -P dev` passes
- [ ] Desktop practice game works (AI makes decisions, human can play)
- [ ] Dashboard HandStrength displays server-provided equity
- [ ] Dashboard ImproveOdds displays server-provided improvement odds
- [ ] Calculator Tool Simulator panel runs simulations via REST
- [ ] Calculator Tool Showdown panel computes equity via REST
- [ ] Calculator Tool Stats panel shows hand potential from server
- [ ] No `HandStrength`, `HandPotential`, `HandFutures`, `HandInfo`, `HandInfoFast`, `HoldemSimulator`, `PocketWeights`, or other poker math classes remain in `code/poker/src/main/`
- [ ] No poker calculation imports in the desktop client (only `pokerengine` model types like `Card`, `Hand`, `Deck`)
