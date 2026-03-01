# Server-Side AI Consolidation Implementation Plan

**Status:** COMPLETED (2026-03-01)

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove all game AI and poker business logic from clients so only the server computes AI decisions, advisor data, and equity simulations. Clients become pure UI.

**Architecture:** Embed `pokergameserver`'s engine in desktop practice games so AI runs server-side for both practice and online modes. Advisor data piggybacks on WebSocket game events (no round-trip). Standalone Simulator uses a REST endpoint.

**Tech Stack:** Java 21, Spring Boot 3.5.8, TypeScript/React, WebSocket, REST, Maven

**Design:** See `docs/plans/2026-03-01-server-side-ai-consolidation-design.md`

---

## Task Overview

| # | Task | Depends On |
|---|------|-----------|
| 1 | Fix ServerOpponentTracker position tracking (4→6) | — |
| 2 | Fix ServerOpponentTracker limp detection | 1 |
| 3 | Create AdvisorService in pokergameserver | — |
| 4 | Integrate advisor data into WebSocket events | 3 |
| 5 | Update web client to consume server advisor data | 4 |
| 6 | Create Simulator REST endpoint | — |
| 7 | Update web Simulator to call REST endpoint | 6 |
| 8 | Wire desktop practice games to embedded server | 1, 2 |
| 9 | Remove client-side AI classes and tests | 8 |
| 10 | Clean up remaining references and config | 9 |
| 11 | Remove web client poker calculation libraries | 5, 7 |

---

### Task 1: Fix ServerOpponentTracker Position Tracking (4→6)

**Files:**
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerOpponentTracker.java`
- Modify: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerOpponentTrackerTest.java`

The server currently tracks 4 position categories (`blind=0, early=1, middle=2, late=3`). The client `OpponentModel` tracks 6 (`early=0, middle=1, late=2, small_blind=3, big_blind=4, overall=5`). Blind play is fundamentally different — the AI needs this distinction.

**Step 1: Write failing tests for 6-position tracking**

Add tests that verify small blind and big blind are tracked separately from a generic "blind" bucket. Test that `getPreFlopTightness()` and `getPreFlopAggression()` return distinct values for small blind vs big blind positions.

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl pokerserver -Dtest=ServerOpponentTrackerTest -P fast`
Expected: FAIL — current code uses 4 positions

**Step 3: Expand MutableOpponentStats arrays from [4] to [6]**

In `MutableOpponentStats`:
- Change all `new int[4]` arrays to `new int[6]`: `preFlopRaises`, `preFlopCalls`, `preFlopLimps`, `preFlopFoldsUnraised`, `preFlopHandsByPosition`
- Update position constants to match client's `OpponentModel`:
  - `0 = EARLY`
  - `1 = MIDDLE`
  - `2 = LATE`
  - `3 = SMALL_BLIND`
  - `4 = BIG_BLIND`
  - `5 = OVERALL`
- Update `recordAction()` to accept and use the new position categories
- Update `getPreFlopTightness()` and `getPreFlopAggression()` to handle all 6 positions
- Ensure callers in `ServerOpponentTracker.onPlayerAction()` pass the correct position category

**Step 4: Run tests to verify they pass**

Run: `cd code && mvn test -pl pokerserver -Dtest=ServerOpponentTrackerTest -P fast`
Expected: PASS

**Step 5: Commit**

```bash
git add code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerOpponentTracker.java
git add code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerOpponentTrackerTest.java
git commit -m "fix: expand ServerOpponentTracker to 6 position categories

Match client OpponentModel's position tracking: early, middle, late,
small blind, big blind, and overall. This ensures AI quality doesn't
regress when client-side AI is removed.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: Fix ServerOpponentTracker Limp Detection

**Files:**
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerOpponentTracker.java`
- Modify: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerOpponentTrackerTest.java`

Currently all pre-flop calls are counted as limps. A limp is specifically calling the big blind when no raise has occurred. Calls after a raise are not limps.

**Step 1: Write failing tests for limp vs call-raise distinction**

Test scenarios:
- Player calls BB with no prior raise → counted as limp
- Player calls after a raise → NOT counted as limp (but counted as call)
- Verify `getHandsLimpedPercent()` only reflects true limps

**Step 2: Run tests to verify they fail**

Run: `cd code && mvn test -pl pokerserver -Dtest=ServerOpponentTrackerTest -P fast`
Expected: FAIL — current code counts all calls as limps

**Step 3: Add raise tracking to recordAction()**

In `MutableOpponentStats`:
- Add a transient `boolean raisedPreFlop` field (reset per hand in `startHand()`)
- In `recordAction()` pre-flop handling:
  - On `ACTION_RAISE`: set `raisedPreFlop = true`, increment `preFlopRaises`
  - On `ACTION_CALL`: always increment `preFlopCalls`; only increment `preFlopLimps` when `!raisedPreFlop`
  - On `ACTION_FOLD`: only increment `preFlopFoldsUnraised` when `!raisedPreFlop` (already the current behavior but name implies it)

Note: The `raisedPreFlop` flag should be set by the tracker based on ALL players' actions at the table, not just the recording player. The tracker needs a `onPreFlopRaise()` method or needs to receive all actions at the table, not just the tracked player's. Check how `ServerOpponentTracker.onPlayerAction()` is called — if it receives all players' actions, then the tracker can maintain this state internally.

**Step 4: Run tests to verify they pass**

Run: `cd code && mvn test -pl pokerserver -Dtest=ServerOpponentTrackerTest -P fast`
Expected: PASS

**Step 5: Commit**

```bash
git add code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerOpponentTracker.java
git add code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerOpponentTrackerTest.java
git commit -m "fix: distinguish limps from call-raises in ServerOpponentTracker

Only count pre-flop calls as limps when no raise has occurred.
Previously all pre-flop calls were counted as limps, inflating
getHandsLimpedPercent() and degrading AI quality.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: Create AdvisorService in pokergameserver

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorService.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorResult.java`
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/AdvisorServiceTest.java`

This service computes advisor data (hand evaluation, equity, pot odds, recommendation, starting hand category) for a player given the current game state. It will be called by `GameEventBroadcaster` to include advisor data in WebSocket messages.

**Step 1: Define the AdvisorResult record**

```java
package com.donohoedigital.games.poker.gameserver;

public record AdvisorResult(
    int handRank,           // 0-9 (HIGH_CARD to ROYAL_FLUSH)
    String handDescription, // "One Pair, Aces" or null if < 5 cards
    double equity,          // 0-100 win percentage
    double potOdds,         // 0-100 pot odds percentage
    String recommendation,  // "Raise or Call", "Consider calling", "Consider folding", "Check"
    String startingHandCategory, // "premium", "strong", "playable", "marginal", "fold" or null
    String startingHandNotation  // "AKs", "AA", "72o" or null
) {}
```

**Step 2: Write failing tests for AdvisorService**

Test scenarios:
- Pre-flop with premium hand (AA) → category "premium", high equity
- Post-flop with made hand → correct hand rank and description
- Pot odds calculation: `callAmount / (potSize + callAmount) * 100`
- Recommendation logic:
  - `callAmount == 0` → "Check"
  - `equity - potOdds > 10` → "Raise or Call"
  - `equity - potOdds > 0` → "Consider calling"
  - else → "Consider folding"
- Pre-flop: starting hand category and notation populated
- Post-flop: starting hand fields null

**Step 3: Run tests to verify they fail**

Run: `cd code && mvn test -pl pokergameserver -Dtest=AdvisorServiceTest -P fast`
Expected: FAIL — class doesn't exist yet

**Step 4: Implement AdvisorService**

```java
package com.donohoedigital.games.poker.gameserver;

public class AdvisorService {

    /**
     * Compute advisor data for the given player's current situation.
     *
     * @param holeCards      player's 2 hole cards
     * @param communityCards current community cards (0-5)
     * @param potSize        current pot size in chips
     * @param callAmount     amount to call (0 if can check)
     * @param numOpponents   number of active opponents
     * @param iterations     Monte Carlo iterations (typically 2000)
     * @return advisor result with hand eval, equity, pot odds, recommendation
     */
    public AdvisorResult compute(Card[] holeCards, Card[] communityCards,
                                  int potSize, int callAmount,
                                  int numOpponents, int iterations) { ... }
}
```

Key implementation details:
- Use `HandInfoFaster` from pokergamecore for hand evaluation (same as server game engine)
- Implement Monte Carlo equity calculation: deal random cards to opponents, evaluate all hands, tally wins/ties/losses
- Use `ServerStrategyProvider`'s embedded hand strength tables for starting hand categorization, or embed the same 13x13 grid from the web client's `startingHands.ts`
- Pot odds: `callAmount / (potSize + callAmount) * 100` (handle `callAmount == 0` → 0.0)
- Keep it stateless — pure function with no side effects

**Step 5: Run tests to verify they pass**

Run: `cd code && mvn test -pl pokergameserver -Dtest=AdvisorServiceTest -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorService.java
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/AdvisorResult.java
git add code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/AdvisorServiceTest.java
git commit -m "feat: add AdvisorService for server-side hand analysis

Computes hand evaluation, Monte Carlo equity, pot odds, starting
hand category, and recommendation text. Will be integrated into
WebSocket game events to replace client-side computation.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: Integrate Advisor Data into WebSocket Events

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcaster.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/message/ServerMessageData.java`
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/websocket/OutboundMessageConverter.java`
- Modify: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/websocket/GameEventBroadcasterTest.java`
- Modify: `code/web/lib/game/types.ts` — add `AdvisorData` TypeScript interface

**Step 1: Add AdvisorData record to ServerMessageData**

```java
record AdvisorData(
    Integer handRank,
    String handDescription,
    double equity,
    double potOdds,
    String recommendation,
    String startingHandCategory,
    String startingHandNotation
) implements ServerMessageData {}
```

**Step 2: Write failing tests**

Test that when `HOLE_CARDS` is sent to a human player, advisor data is included. Test that after `COMMUNITY_CARDS_DEALT`, updated advisor data is sent privately to the human player.

**Step 3: Integrate AdvisorService into GameEventBroadcaster**

Key integration points:
- After sending `HOLE_CARDS` message (line ~203-208 in `GameEventBroadcaster`): compute advisor data and send as a separate `ADVISOR_UPDATE` private message to the human player
- On `COMMUNITY_CARDS_DEALT` event: compute updated advisor data and send `ADVISOR_UPDATE` to each human player
- On `PLAYER_ACTED` event: recompute pot odds and recommendation, send `ADVISOR_UPDATE`

Add a new `ServerMessageType.ADVISOR_UPDATE` and corresponding `AdvisorData` record.

The `GameEventBroadcaster` needs:
- An `AdvisorService` field (injected via constructor)
- Access to the current hand's game state (hole cards, community cards, pot, etc.) to compute advisor data
- Logic to only send advisor data to human players who are still in the hand

**Step 4: Add TypeScript types**

In `code/web/lib/game/types.ts`, add:
```typescript
export interface AdvisorData {
  handRank: number | null
  handDescription: string | null
  equity: number
  potOdds: number
  recommendation: string
  startingHandCategory: string | null
  startingHandNotation: string | null
}
```

Add `'ADVISOR_UPDATE'` to the message type union.

**Step 5: Run tests**

Run: `cd code && mvn test -pl pokergameserver -Dtest=GameEventBroadcasterTest -P fast`
Expected: PASS

**Step 6: Commit**

```bash
git commit -m "feat: include advisor data in WebSocket game events

Add ADVISOR_UPDATE message type sent to human players after hole cards,
community cards, and player actions. Eliminates client-side equity
calculation round-trip.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: Update Web Client to Consume Server Advisor Data

**Files:**
- Modify: `code/web/lib/game/gameReducer.ts` — handle `ADVISOR_UPDATE` messages
- Modify: `code/web/components/game/AdvisorPanel.tsx` — read from game state instead of computing
- Modify: `code/web/components/game/Dashboard.tsx` — read from game state instead of computing
- Create: `code/web/components/game/__tests__/AdvisorPanel.server.test.tsx` — test with server data

**Step 1: Add advisor state to gameReducer**

In `GameState` interface, add:
```typescript
advisorData: AdvisorData | null
```

Add reducer case for `ADVISOR_UPDATE`:
```typescript
case 'ADVISOR_UPDATE':
  return { ...state, advisorData: message.data as AdvisorData, ...seqState }
```

Clear advisor data on `HAND_STARTED` (reset for new hand).

**Step 2: Refactor AdvisorPanel to use server data**

Remove all local computation:
- Remove `calculateEquity()` call
- Remove `evaluateHand()` call
- Remove pot odds calculation
- Remove recommendation logic
- Remove imports of `equityCalculator`, `handEvaluator`, `startingHands`

Instead, read `advisorData` from game state props. Display:
- Hand rank + description from `advisorData.handRank` / `advisorData.handDescription`
- Equity from `advisorData.equity`
- Pot odds from `advisorData.potOdds`
- Recommendation from `advisorData.recommendation`
- Starting hand from `advisorData.startingHandCategory` / `advisorData.startingHandNotation`

Handle `advisorData === null` gracefully (show "Waiting for data..." or similar).

**Step 3: Refactor Dashboard similarly**

Remove local equity calculation from `Dashboard.tsx` (line 63 area). Use `advisorData` from game state.

**Step 4: Update tests**

Update `AdvisorPanel.test.tsx` to test with server-provided data instead of mocking local calculation functions.

**Step 5: Run tests**

Run: `cd code/web && npm test -- --testPathPattern=AdvisorPanel`
Expected: PASS

**Step 6: Commit**

```bash
git commit -m "refactor(web): consume advisor data from server via WebSocket

AdvisorPanel and Dashboard now read server-computed advisor data from
game state instead of calculating equity/hand-strength client-side.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: Create Simulator REST Endpoint

**Files:**
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/PokerSimulationService.java`
- Create: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/SimulationResult.java`
- Create: `code/api/src/main/java/com/donohoedigital/poker/api/controller/SimulationController.java`
- Create: `code/api/src/main/java/com/donohoedigital/poker/api/dto/SimulationRequest.java`
- Create: `code/pokergameserver/src/test/java/com/donohoedigital/games/poker/gameserver/PokerSimulationServiceTest.java`
- Create: `code/api/src/test/java/com/donohoedigital/poker/api/controller/SimulationControllerTest.java`

**Step 1: Define SimulationResult record**

```java
package com.donohoedigital.games.poker.gameserver;

import java.util.List;

public record SimulationResult(
    double win,       // 0-100
    double tie,       // 0-100
    double loss,      // 0-100
    int iterations,
    List<OpponentResult> opponentResults  // null if no known opponent hands
) {
    public record OpponentResult(double win, double tie, double loss) {}
}
```

**Step 2: Write failing tests for PokerSimulationService**

Test scenarios:
- AA vs 1 random opponent → win% > 80
- AA vs KK (known) → win% ~80, with per-opponent result
- Pre-flop with 2 opponents → win + tie + loss ≈ 100
- Full board (5 community cards) → deterministic result
- Edge: 0 community cards, 5 community cards
- Input validation: duplicate cards rejected, invalid card format rejected

**Step 3: Implement PokerSimulationService**

Port the Monte Carlo algorithm from `equityCalculator.ts`:
1. Remove known cards (hole + community + known opponent hands) from deck
2. For each iteration: shuffle remaining, deal remaining community, deal unknown opponents, evaluate all hands, tally
3. Return percentages

Use `HandInfoFaster` for hand evaluation (same as `AdvisorService`).

Card format conversion: accept string cards ("Ah", "Kd") and convert to internal `Card` objects.

**Step 4: Define SimulationRequest DTO**

```java
package com.donohoedigital.poker.api.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record SimulationRequest(
    @NotNull @Size(min = 2, max = 2) List<String> holeCards,
    @Size(max = 5) List<String> communityCards,
    @Min(1) @Max(9) int numOpponents,
    @Min(100) @Max(100000) int iterations,
    List<List<String>> knownOpponentHands  // optional
) {}
```

**Step 5: Implement SimulationController**

```java
@RestController
@RequestMapping("/api/v1/poker")
public class SimulationController {

    @PostMapping("/simulate")
    public SimulationResult simulate(@Valid @RequestBody SimulationRequest request) {
        // Validate no duplicate cards across all inputs
        // Convert string cards to Card objects
        // Call PokerSimulationService
        // Return result
    }
}
```

**Step 6: Run tests**

Run: `cd code && mvn test -pl pokergameserver -Dtest=PokerSimulationServiceTest -P fast`
Run: `cd code && mvn test -pl api -Dtest=SimulationControllerTest -P fast`
Expected: PASS

**Step 7: Commit**

```bash
git commit -m "feat: add poker simulation REST endpoint

POST /api/v1/poker/simulate runs Monte Carlo equity simulations
server-side. Supports known opponent hands and per-opponent result
breakdown. Replaces client-side equity calculator.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: Update Web Simulator to Call REST Endpoint

**Files:**
- Modify: `code/web/components/game/Simulator.tsx`
- Modify: `code/web/components/game/__tests__/Simulator.test.tsx`

**Step 1: Replace local calculateEquity() with fetch call**

In `Simulator.tsx`, replace the `runSimulation()` function:

```typescript
const runSimulation = async () => {
  setIsCalculating(true)
  try {
    const response = await fetch('/api/v1/poker/simulate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        holeCards: hole,
        communityCards: community,
        numOpponents: opponents,
        iterations: 10000,
        knownOpponentHands: known.length > 0 ? known : undefined,
      }),
    })
    if (!response.ok) throw new Error('Simulation failed')
    const result = await response.json()
    setResults(result)
  } catch (error) {
    console.error('Simulation error:', error)
  } finally {
    setIsCalculating(false)
  }
}
```

Remove import of `calculateEquity` from `equityCalculator.ts`.

**Step 2: Update tests**

Mock the `fetch` call instead of the `calculateEquity` function. Test loading states and error handling.

**Step 3: Run tests**

Run: `cd code/web && npm test -- --testPathPattern=Simulator`
Expected: PASS

**Step 4: Commit**

```bash
git commit -m "refactor(web): call server REST endpoint for simulator

Simulator component now sends POST to /api/v1/poker/simulate instead
of running Monte Carlo simulation client-side.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 8: Wire Desktop Practice Games to Embedded Server

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/server/PracticeGameLauncher.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/Bet.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java`
- Modify: `code/poker/pom.xml`

This is the most complex task. The goal is to make practice games use the server-side AI via the embedded server engine, rather than calling local AI classes.

**Step 1: Add pokergameserver dependency to poker/pom.xml**

Add the `pokergameserver` module as a dependency so the desktop client can embed the server engine.

**Step 2: Modify PracticeGameLauncher**

Currently `PracticeGameLauncher.launch()` creates a practice game on the embedded REST server. Verify it properly starts `ServerTournamentDirector` with `ServerAIProvider` so AI runs server-side.

Ensure the embedded server:
- Creates `ServerAIProvider` with the correct skill levels for each AI player
- Uses `ServerPlayerActionProvider` to route human actions back to the Swing UI
- Broadcasts game events that the desktop UI can consume

**Step 3: Modify Bet.java**

In `Bet.process()` (lines 191-234), the AI branch calls `doAI()` which calls `player_.getAction(false)`. For embedded server mode:
- AI players: the embedded server handles their actions via `ServerAIProvider`. The desktop UI receives `PLAYER_ACTED` events and renders them. `Bet` should not call `doAI()` at all — the server drives the game loop.
- Human players: the embedded server's `ServerPlayerActionProvider` blocks on a `CompletableFuture`. The Swing UI completes this future when the human clicks an action button.

The key change: when using the embedded server, `Bet.process()` for AI players should simply wait for the server to broadcast the action rather than computing it locally.

**Step 4: Modify PokerPlayer.java**

Remove or bypass `createPokerAI()` (lines 244-279) and `getPokerAI()` (lines 297-318) for embedded server mode. The server manages AI instances, not the client.

For the transition:
- `getPokerAI()` can return null when the embedded server is active
- `getAction()` should not be called for AI players (server handles it)
- `setPlayerType()` still works but doesn't trigger AI creation

**Step 5: Integration test**

Create a test that starts a practice game via `PracticeGameLauncher`, verifies AI players make actions via the server engine, and the human player can submit actions.

**Step 6: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: PASS (existing tests may need updates if they relied on local AI)

**Step 7: Commit**

```bash
git commit -m "feat: wire desktop practice games to embedded server engine

Practice games now use ServerAIProvider for AI decisions instead of
client-side PokerAI classes. AI runs server-side for both practice
and online modes, sharing the same code path.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 9: Remove Client-Side AI Classes and Tests

**Files to DELETE:**
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/PokerAI.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/V1Player.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/V2Player.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/RuleEngine.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/ClientV2AIContext.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/ClientStrategyProvider.java`
- `code/poker/src/main/java/com/donohoedigital/games/poker/ai/OpponentModel.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/OpponentModelTest.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/RuleEngineIntegrationTest.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/RuleEngineOutcomeAdjustmentTest.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/V1PlayerStaticTest.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/V2PlayerHandStrengthTest.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/V2PlayerHarringtonTest.java`
- `code/poker/src/test/java/com/donohoedigital/games/poker/ai/PokerAIPositionTest.java`

**Note on PlayerType:** `PlayerType.java` is closely tied to the removed AI classes but may still be needed for player profile persistence and AI class name lookups (used in binary `.dat` files). Evaluate whether `PlayerType` can be simplified to a data class that doesn't reference AI class names, or if it needs to remain for backward compatibility. The server uses `ServerStrategyProvider` + `StrategyDataLoader` instead.

**Note on AI GUI classes:** The following GUI classes in `com.donohoedigital.games.poker.ai.gui` and `com.donohoedigital.games.poker.ai.phase` are referenced in `gamedef.xml`. These handle the PlayerType editor UI in the desktop client. Evaluate whether these still work without `PokerAI`/`V1Player`/`V2Player` — they may need `PlayerType` but not the AI implementations.

**Step 1: Delete the files listed above**

**Step 2: Fix compilation errors**

Files that will break and need updating:
- `PokerPlayer.java`: Remove `createPokerAI()`, `getPokerAI()`, `setPokerAI()` methods (or stub them if still needed by framework). Remove `playerType_` field if no longer needed.
- `Bet.java`: Already updated in Task 8 — verify `doAI()` is removed/no-op
- `PokerTable.java`: Remove `getPokerAI()` calls at lines 1470, 1748, 1787
- `ShowTournamentTable.java`: Remove `player.getPokerAI().getPlayerType().getName()` calls (lines 2157, 2206) — get player name from server state instead
- `HoldemHand.java`: Remove wildcard import `com.donohoedigital.games.poker.ai.*` if no longer needed
- `DashboardAdvisor.java`: Remove `pp.getPokerAI()` calls (lines 116, 190) — advisor data now comes from server
- `PocketWeights.java`: Remove `player.getPokerAI().getStartingOrder()` and `getNumPlayers()` calls (lines 395, 397)
- `AITest.java`: Remove or update
- `WebSocketOpponentTracker.java`: Review — may need updates

**Step 3: Verify build**

Run: `cd code && mvn compile -pl poker -P fast`
Expected: BUILD SUCCESS

**Step 4: Run tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: PASS (some tests may need deletion if they only tested removed classes)

**Step 5: Commit**

```bash
git commit -m "refactor: remove client-side AI classes from desktop module

Remove PokerAI, V1Player, V2Player, RuleEngine, ClientV2AIContext,
ClientStrategyProvider, OpponentModel and their tests. AI decisions
now exclusively handled by the embedded server engine.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 10: Clean Up Remaining References and Config

**Files:**
- Modify: Binary `.dat` files in `code/poker/src/main/resources/save/poker/playertypes/`
- Modify: `code/poker/src/main/resources/config/poker/gamedef.xml` (if AI GUI phases need updating)
- Review: AI GUI classes in `com.donohoedigital.games.poker.ai.gui` and `com.donohoedigital.games.poker.ai.phase`

**Step 1: Handle binary .dat files**

The `playertype.*.dat` files contain embedded class name strings like `com.donohoedigital.games.poker.ai.V2Player`. These are used by `PlayerType.getAIClassName()` for reflection-based AI instantiation.

Since AI is now server-side:
- If `PlayerType` is kept: update `getAIClassName()` to return a server-recognized identifier instead of a client class name
- If `PlayerType` is removed: the `.dat` files in the `poker` module are no longer needed (server has its own copies in `pokerserver/src/main/resources/`)
- If `.dat` files are kept for other profile data (player names, personality settings): the AI class name field can be ignored or stripped

**Step 2: Review gamedef.xml**

Check references to:
- `com.donohoedigital.games.poker.ai.phase.CreateTestCase` (line 403)
- `com.donohoedigital.games.poker.ai.gui.PlayerTypeDialog` (line 493)
- `com.donohoedigital.games.poker.ai.gui.HandSelectionDialog` (line 498)
- `com.donohoedigital.games.poker.ai.gui.PlayerTypeRosterDialog` (line 592)
- `com.donohoedigital.games.poker.ai.gui.AdvisorInfoDialog` (line 956)

These GUI classes manage the player type editor. If they still compile and work without the removed AI classes, they can stay. If not, they need updating or removal.

**Step 3: Run full build**

Run: `cd code && mvn test -P fast`
Expected: BUILD SUCCESS, all tests pass

**Step 4: Commit**

```bash
git commit -m "chore: clean up AI class references in config and data files

Update player type configuration and gamedef.xml to reflect
server-side AI architecture.

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 11: Remove Web Client Poker Calculation Libraries

**Files to DELETE:**
- `code/web/lib/poker/handEvaluator.ts`
- `code/web/lib/poker/equityCalculator.ts`
- `code/web/lib/poker/startingHands.ts`
- `code/web/lib/poker/deck.ts`
- `code/web/lib/poker/__tests__/handEvaluator.test.ts` (if exists)
- `code/web/lib/poker/__tests__/equityCalculator.test.ts` (if exists)

**Files to KEEP:**
- `code/web/lib/poker/types.ts` — still needed for type definitions (HandRank enum, EquityResult interface used by server response types)
- `code/web/components/game/StartingHandsChart.tsx` — visual chart component. If it imports from `startingHands.ts`, inline the grid data or receive it from the server.

**Step 1: Check for remaining imports**

Search for any remaining imports of the deleted files across the web codebase:
```bash
cd code/web && grep -r "from.*poker/handEvaluator\|from.*poker/equityCalculator\|from.*poker/startingHands\|from.*poker/deck" --include="*.ts" --include="*.tsx"
```

Fix any remaining references.

**Step 2: Handle StartingHandsChart**

`StartingHandsChart.tsx` imports from `startingHands.ts`. Options:
- Inline the 13x13 grid data directly in the component (it's static reference data)
- Or keep `startingHands.ts` if it's only used for the visual chart (it's just a data grid, not business logic)

Recommendation: inline the grid data since it's a small constant.

**Step 3: Delete the files**

**Step 4: Verify build**

Run: `cd code/web && npm run build`
Expected: BUILD SUCCESS

**Step 5: Run tests**

Run: `cd code/web && npm test`
Expected: PASS

**Step 6: Commit**

```bash
git commit -m "refactor(web): remove client-side poker calculation libraries

Delete handEvaluator.ts, equityCalculator.ts, startingHands.ts, and
deck.ts. All poker calculations now performed server-side via
WebSocket (advisor) and REST (simulator).

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Verification Checklist

After all tasks are complete:

1. **Desktop practice game works:** Start a practice game, verify AI players make decisions, human can play
2. **Desktop online game works:** Connect to server, verify game plays correctly
3. **Web client advisor works:** Join a game, verify AdvisorPanel shows server-computed data
4. **Web client simulator works:** Open simulator, run a simulation, verify results come from server
5. **No client-side poker logic:** Verify no hand evaluation, equity calculation, or AI decision code remains on any client
6. **Full build passes:** `cd code && mvn test` and `cd code/web && npm test` both pass
7. **No regression in AI quality:** AI players make reasonable decisions (not just folding everything)
