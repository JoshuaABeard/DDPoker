# Plan: Extract `pokergamecore` Module — Shared Game Logic for Server & Web

## Context

DDPoker's core game classes (PokerGame, PokerTable, HoldemHand, PokerPlayer) live in the `poker` module, which depends on `gameengine` (Swing). This makes it impossible for the server or a future web client to run game logic. We need to extract pure game logic into a new `pokergamecore` module with zero Swing dependencies, enabling:
- **Server-hosted games** (server runs the game engine, clients just connect)
- **Future web version** (reuses same game logic via WebSocket)
- **Single source of truth** for poker rules (no duplication)

**Parent plan:** `.claude/plans/SERVER-HOSTED-GAMES.md` (Option C: Native Server Game Engine)
**Design doc:** `.claude/plans/OPTION-C-SHARED-CORE-DESIGN.md`

---

## Current Coupling Analysis

| Class | Module | Swing? | GameEngine? | Can Move Directly? |
|-------|--------|--------|-------------|-------------------|
| HoldemHand (3252 lines) | poker | None | 1 call (line 149) | Yes, after removing 1 line |
| PokerTable (1969 lines) | poker | None | None | Yes, but refs PokerPlayer/PokerGame |
| PokerGame (2169 lines) | poker | Via `extends Game` | 2 calls | No — inheritance chain |
| PokerPlayer | poker | Via `extends GamePlayer` | 1 call | No — inheritance chain |
| HandAction, Pot | poker | None | None | Yes |
| PokerTableEvent/Listener | poker | None | None | Yes |
| logic/*.java (11 classes) | poker | None | None | Yes |

**Key insight:** TournamentDirector's `_processTable()` already separates pure game logic from UI/network dispatch via the `TDreturn` inner class. This is the natural extraction boundary.

---

## Strategy: Interface + Delegation Hybrid

1. **Move** classes that are already pure (HoldemHand, events, logic/, HandAction, Pot)
2. **Interface** the contracts for PokerGame/PokerPlayer/PokerTable (so core code doesn't need Swing classes)
3. **Extract** TournamentEngine from TD's `_processTable()` state machine
4. **Delegate** from existing Swing classes to core interfaces where needed

---

## Target Module Structure

```
pokerengine (Card, Hand, Deck, TournamentProfile) — NO Swing
    ↓
pokergamecore (NEW) — NO Swing
  ├── TournamentEngine          — extracted from TD._processTable()
  ├── TableProcessResult        — extracted from TDreturn
  ├── HoldemHand                — moved from poker
  ├── HandAction, Pot           — moved from poker
  ├── PokerTableEvent/Listener  — moved from poker
  ├── PokerTableInput           — moved from poker
  ├── PlayerIdentityProvider    — replaces GameEngine.getPlayerId()
  ├── logic/*                   — moved from poker/logic/
  └── interfaces: GameState, TableState, PlayerState
    ↓                               ↓
poker (Swing client)            pokerserver (Spring server)
  ├── PokerGame extends Game      ├── ServerTournamentDirector
  ├── PokerPlayer extends GP      ├── ServerGameRegistry
  ├── TournamentDirector          └── (future) ServerOnlineManager
  │   delegates to Engine
  └── OnlineManager
```

---

## Phase 0: Create Module + Core Interfaces (Est: 0.5 day)

**Goal:** Empty module compiles, all tests pass.

### Create
- `code/pokergamecore/pom.xml` — depends on `pokerengine` only
- `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/`

### Core Interfaces
```java
// Replaces GameEngine.getPlayerId() singleton calls
public interface PlayerIdentityProvider {
    String getPlayerId();
}
```

### Modify
- `code/pom.xml` — add `pokergamecore` module (after `pokerengine`, before `pokernetwork`)
- `code/poker/pom.xml` — add dependency on `pokergamecore`
- `code/pokernetwork/pom.xml` — add dependency on `pokergamecore`

### Verify
- `mvn compile` — all modules build
- `mvn test -P dev` — all tests pass

---

## Phase 1: Move Pure Event & Data Classes (Est: 1–2 days)

**Goal:** Move classes with zero Swing dependencies into `pokergamecore`.

### Move (change package to `c.d.g.poker.core`)
1. `PokerTableListener.java` — pure interface
2. `PokerTableEvent.java` — only refs PokerTable/PokerPlayer/HandAction (by parameter)
3. `PlayerActionListener.java` — pure interface
4. `PokerTableInput.java` — pure interface (mode constants + method)
5. `HandAction.java` — refs PokerPlayer only for name/chipCount (introduce small interface if needed)
6. `Pot.java` — refs PokerPlayer in winner lists
7. `HandInfo.java`, `HandInfoFast.java`, `HandInfoFaster.java` — only depend on `pokerengine` types
8. `HandStrength.java` — pure computation

### Backward Compatibility
- Keep re-export classes in original packages: `public class HandAction extends c.d.g.poker.core.HandAction {}`
- Or use `@Deprecated` imports pointing to new location
- Update all internal references in `poker` module

### Verify
- `mvn test -P dev` — all tests pass
- No Swing imports in `pokergamecore` (grep verification)

---

## Phase 2: Move Logic Classes + HoldemHand (Est: 2–3 days)

**Goal:** Move already-extracted logic and the mostly-pure HoldemHand to `pokergamecore`.

### Move Logic Classes (all already pure — zero Swing/GameEngine deps confirmed)
- `BetValidator.java`
- `ColorUpLogic.java`
- `DealingRules.java`
- `GameOverChecker.java`
- `HandOrchestrator.java`
- `LevelTransitionLogic.java`
- `PokerLogicUtils.java`
- `ShowdownCalculator.java`
- `TableManager.java`
- `TournamentClock.java` (static utility)

**Keep in poker:** `OnlineCoordinator.java` — coordinates online-specific behavior

### Move HoldemHand
- Remove single `GameEngine.getGameEngine()` call (line 149) — replace with `PlayerIdentityProvider` parameter or remove if only used for debug
- Move to `pokergamecore`
- Move associated tests

### Verify
- All existing tests pass
- HoldemHand unit tests pass from `pokergamecore`
- `mvn test -P dev` green

---

## Phase 3: Extract TournamentEngine (Est: 3–4 days)

**Goal:** Extract the core state machine from `TournamentDirector._processTable()` into a public, testable `TournamentEngine` class in `pokergamecore`. This is the highest-value phase — it enables server-hosted games.

### Key Files
- **Source:** `code/poker/src/main/java/com/donohoedigital/games/poker/online/TournamentDirector.java`
- **Target:** `code/pokergamecore/src/main/java/com/donohoedigital/games/poker/core/TournamentEngine.java`

### New Classes in `pokergamecore`

**`TableProcessResult.java`** — public version of `TDreturn`:
```java
public class TableProcessResult {
    private int tableState = -1;
    private int pendingTableState = -1;
    private String phaseToRun;
    private TypedHashMap phaseParams;
    private boolean runOnClient;
    private boolean addAllHumans;
    private boolean onlySendToWaitList;
    private boolean save, autoSave, sleep;
    private List<PokerTableEvent> events;
}
```

**`TournamentEngine.java`** — extracted state machine:
```java
public class TournamentEngine {
    public TableProcessResult processTable(PokerTable table, PokerGame game,
                                           boolean isHost, boolean isOnline) {
        // Extracted from TD._processTable()
        // Pure game logic — no UI, no network
        // Returns what SHOULD happen; caller decides HOW
    }
}
```

### Migration Approach
1. Copy `_processTable()` and all `doXxx()` methods to `TournamentEngine`
2. Replace `this.game_`, `this.mgr_`, `this.bHost_` with method parameters
3. Replace `this.ret_` with local `TableProcessResult`
4. Remove any Swing calls (there should be almost none in `_processTable()`)
5. Modify `TournamentDirector.processTable()` to delegate to `TournamentEngine`:
```java
// In TournamentDirector.processTable():
TableProcessResult result = engine.processTable(table, game_, bHost_, bOnline_);
// ... existing UI/network dispatch using result fields ...
```

### Testing
- **Unit tests for every state transition** in `TournamentEngine`
- **Comparison tests**: Run old TD path and new Engine path, verify identical `TableProcessResult`
- All existing tests pass

---

## Phase 4: Pure Tournament Clock (Est: 0.5 day)

**Goal:** Replace `GameClock extends javax.swing.Timer` with a pure clock for server use.

### New Class in `pokergamecore`
**`PureTournamentClock.java`** — uses `System.currentTimeMillis()`, no Swing Timer:
```java
public class PureTournamentClock {
    private long millisRemaining;
    private boolean running;
    private long lastTickTime;

    public void tick() { /* update based on wall clock */ }
    public int getSecondsRemaining() { ... }
    public boolean isExpired() { ... }
}
```

### Modify
- `GameClock.java` in `poker` — delegate time tracking to `PureTournamentClock`, keep Swing `Timer` for UI ticking

### Verify
- `PureTournamentClock` unit tests pass
- `GameClock` behavior unchanged
- All existing tests pass

---

## Phase 5: Server Integration Proof-of-Concept (Est: 1–2 days)

**Goal:** Demonstrate that `pokerserver` can run a complete tournament using `pokergamecore` alone, with zero Swing on the classpath.

### Modify
- `code/pokerserver/pom.xml` — add dependency on `pokergamecore`

### New Test in `pokerserver`
**`HeadlessGameRunner.java`** — integration test:
```java
@Test
void runCompleteTournament() {
    TournamentProfile profile = createTestProfile();
    TournamentEngine engine = new TournamentEngine();
    // Create game, tables, players using core classes
    // Run game loop calling engine.processTable() repeatedly
    // Verify tournament completes with winner
}
```

### Maven Enforcer
- Add rule to `pokergamecore/pom.xml` to ban `javax.swing.*` and `java.awt.*` imports

### Verify
- `HeadlessGameRunner` completes successfully
- No Swing on `pokergamecore` classpath
- All tests pass

---

## Phase 6: Server Game Hosting Infrastructure (Est: 2–3 days)

**Goal:** Build the actual server infrastructure for hosting concurrent games.

### New Classes in `pokerserver`
- **`ServerTournamentDirector.java`** — game loop using `TournamentEngine`, sends events via network
- **`ServerGameRegistry.java`** — manages `Map<String, ServerGame>` for concurrent games
- **`ServerPlayerActionProvider.java`** — receives player actions from network messages

### Architecture
```java
public class ServerTournamentDirector implements Runnable {
    private final TournamentEngine engine;
    private final OnlineMessageSender messageSender;

    public void run() {
        while (!game.isComplete()) {
            TableProcessResult result = engine.processTable(table, game, true, true);
            dispatchResult(result);  // Send network messages instead of UI
        }
    }
}
```

### Verify
- Server can host a game with AI players
- Multiple concurrent games work
- Network protocol compatible with existing clients

---

## What We're NOT Doing (Explicitly Deferred)

- **Moving PokerGame/PokerPlayer** — too risky to break `Game`/`GamePlayer` inheritance chain now. Instead we use interfaces and delegation.
- **Refactoring OnlineManager** — stays in `poker` module. Server gets its own `ServerOnlineManager` later.
- **Client UI changes** — no visible changes to the desktop client.
- **Web client** — foundation only; actual web client is a separate future project.
- **Breaking online protocol** — existing multiplayer must continue working.

---

## Risk Summary

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking save/load format | High | Preserve `DataMarshal` token format exactly |
| Breaking online multiplayer | High | Online code stays in `poker` module |
| Subtle state bugs during extraction | High | Extract one state at a time, comparison tests |
| Circular dependencies | Medium | pokergamecore depends ONLY on pokerengine |
| Build time increase | Low | Module is small, parallel build |

---

## Estimated Total Effort

| Phase | Days | Risk |
|-------|------|------|
| 0: Module + interfaces | 0.5 | Low |
| 1: Move pure event/data classes | 1–2 | Medium |
| 2: Move logic/ + HoldemHand | 2–3 | Medium |
| 3: Extract TournamentEngine | 3–4 | High |
| 4: Pure TournamentClock | 0.5 | Low |
| 5: Server proof-of-concept | 1–2 | Low |
| 6: Server hosting infrastructure | 2–3 | Medium |
| **Total** | **~10–15 days** | |

---

## Critical Files

- `code/poker/src/main/java/com/donohoedigital/games/poker/online/TournamentDirector.java` — extraction source for TournamentEngine
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerGame.java` — tournament orchestrator (interface only, not moved)
- `code/poker/src/main/java/com/donohoedigital/games/poker/HoldemHand.java` — moves to pokergamecore
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerTable.java` — interface + potential move
- `code/poker/src/main/java/com/donohoedigital/games/poker/PokerPlayer.java` — interface only
- `code/poker/src/main/java/com/donohoedigital/games/poker/logic/*.java` — all 10 classes move
- `code/poker/src/main/java/com/donohoedigital/games/poker/GameClock.java` — delegates to pure clock
- `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/PokerServer.java` — server integration point

## Verification

After all phases:
1. `mvn test -P dev` — all tests pass
2. `mvn verify -P coverage` — coverage thresholds met
3. `grep -r "javax.swing\|java.awt" code/pokergamecore/` — zero results
4. `HeadlessGameRunner` test completes full tournament on server
5. Desktop client behavior unchanged (manual verification)
6. Online multiplayer still works (manual verification)
