# Design: Poker Desktop Client Thinning

**Status:** COMPLETED (2026-03-02) — implementation plan written and executed (see 2026-03-02-poker-client-thinning-plan.md)
**Created:** 2026-03-02
**Scope:** `code/poker` (desktop client), `code/pokerengine` (shared engine), `code/pokerserver` (backend)

---

## Goal

Thin the `poker` desktop client module so it contains only:

- Swing UI components (panels, dialogs, dashboard items)
- WebSocket/REST client infrastructure (`online/` package)
- Embedded server launcher (`server/` package)
- Local file I/O (save game, stats database, hand history, profiles)
- Display-side view model interfaces and their `Remote*` implementations
- AI strategy UI config (opponent mix dialogs — read-only config data, no execution)

The `poker` module should **not** define game engine classes that other modules need to import.

---

## Current State

### Module Dependency Problem

`pokerserver` (the backend server) currently declares `poker` (the desktop client) as a compile dependency:

```
pokerserver → poker   ← backwards dependency
```

The sole cause is `HandAction.java`, which lives in the `poker` module but is imported by `pokerserver` in `ServerAIContext.java` and `ServerOpponentTracker.java`.

### Client-Side Game Engine

`HoldemHand`, `PokerPlayer`, `PokerTable`, and `PokerGame` in the `poker` module are the original full client-side game engine from the pre-thin-client era. Today:

- All game logic runs server-side (embedded or remote server via WebSocket)
- The UI path always receives `RemotePokerTable` and `RemoteHoldemHand` objects
- Those `Remote*` classes extend `PokerTable` and `HoldemHand`, pulling the entire game engine into the classpath

### What Has Already Been Done

- Client-side AI execution removed (AI runs server-side via `ServerAIProvider`)
- `HandInfo`, `HoldemSimulator`, and related classes removed from `poker` (completed in `feature/desktop-thin-client-cleanup`, 2026-03-01)
- `Showdown.java`, `DealCommunity.java` phase logic removed; now delegate to pure calculators (`ShowdownCalculator`, `CommunityCardCalculator`)
- `Remote*` wrappers created with zero poker logic (comment in source: "Contains zero poker logic")

### Target State

```
Before:
  pokerserver → poker (client!)  ← backwards
  poker       → pokergameserver, pokergamecore, pokerengine

After:
  pokerserver → pokergamecore, pokerengine  (no client dep)
  poker       → pokergameserver, pokergamecore, pokerengine
```

---

## Phases

### Phase A — Fix the Backwards Dependency

**Problem:** `HandAction.java` (poker action record) lives in `poker` but is imported by `pokerserver`.

**Fix:** Move `HandAction` to `pokerengine` under `com.donohoedigital.games.poker.engine` — consistent with the other engine-layer types already there (`Card`, `Hand`, `Deck`, `HandInfoFaster`).

**Scope:**
- Move: `poker/src/main/java/com/donohoedigital/games/poker/HandAction.java`
  → `pokerengine/src/main/java/com/donohoedigital/games/poker/engine/HandAction.java`
- Update imports in ~25 files across `poker` (production + test) and `pokerserver` (2 production + 1 test)
- Update `pokerserver/pom.xml`: remove `<artifactId>poker</artifactId>` dependency
- Copyright: original Donohoe — keep header unchanged

**Exit criterion:** `mvn test -P dev` passes; `mvn dependency:tree -pl pokerserver` shows no `poker` artifact.

---

### Phase B — Extract `HandUtils` to `pokerengine`

**What:** `HandUtils.java` is pure hand evaluation logic — `getBestFive()` over hole + community cards — with no game-object or Swing dependencies. It belongs with `HandInfoFaster`, `Hand`, and `Deck` in `pokerengine`.

**Note on `Pot.java`:** `Pot.java` stays in `poker`. It is tightly coupled to `PokerPlayer` (a UI entity) and carries DataCoder serialization for the save-game format. `pokergameserver` already has its own `ServerPot` as a clean parallel implementation. Merging them would be a separate refactor. In Phase C, the display-side pot data will be redesigned as a simple view model reflecting what the WebSocket sends — not a mutable game-engine `Pot` with player references.

**Scope:**
- Move: `poker/src/main/java/com/donohoedigital/games/poker/HandUtils.java`
  → `pokerengine/src/main/java/com/donohoedigital/games/poker/engine/HandUtils.java`
- Update imports in ~5 files within `poker`
- Copyright: apply dual copyright (minor modification + relocation)

**Exit criterion:** `mvn test -P dev` passes.

---

### Phase D — Audit and Strip ChainPhase Poker Logic

**Context:** The ChainPhase classes (`CheckEndHand`, `NewLevelActions`, `Bet`, `ColorUp`, `ColorUpFinish`) are still registered in `gamedef.xml` and execute in both practice and online WS-driven game paths. They are not dead code. However, some contain poker rule logic that is now authoritative on the server — those duplicated checks should be removed from the client.

**Per-class plan:**

| Class | What stays | What gets stripped |
|-------|-----------|-------------------|
| `CheckEndHand` | Dialog/UI coordination for rebuy prompt and game-over screen | Client-side game-over condition evaluation (server sends `HAND_RESULT`; client should not re-evaluate) |
| `NewLevelActions` | Rebuy dialog UI, level-display update, `RebuyDecisionProvider` hook for dev-control-server | Level-advance arithmetic, blind/ante computation (server owns level state) |
| `Bet` | Betting UI input widgets, amount display, player-input coordination | Action-validation guards that duplicate server validation |
| `ColorUp` / `ColorUpFinish` | Chip animation, user-prompt display | Any chip arithmetic (color-up amounts are server-computed) |

**Process:** For each class, audit method-by-method. Mark each block as: (a) UI coordination — keep, (b) server-duplicated rule — strip, (c) unclear — document and discuss. Only strip (b) in this phase.

**Exit criterion:** No poker rule logic duplicates server decisions; all UI coordination remains; `mvn test -P dev` passes; scenario tests pass.

---

### Phase C — Break `Remote*` Inheritance

**Problem:** `RemotePokerTable extends PokerTable` and `RemoteHoldemHand extends HoldemHand` pull the entire client-side game engine into the UI classpath at load time.

**Goal:** Make `RemotePokerTable` and `RemoteHoldemHand` standalone classes — no `extends`. The game table display path has zero dependency on the client-side game engine.

**Approach: Define client-facing view interfaces**

Two new interfaces in the `poker` module capture the display contract:

```java
// poker/src/main/java/com/donohoedigital/games/poker/online/ClientPokerTable.java
public interface ClientPokerTable {
    // All getters the UI currently calls on PokerTable/RemotePokerTable
    int getNumPlayers();
    PokerPlayer getPlayerAt(int seat);
    ClientHoldemHand getHand();
    int getLevel();
    int getSmallBlind();
    int getBigBlind();
    int getAnte();
    // ... (full list determined during implementation by scanning all callers)
}

// poker/src/main/java/com/donohoedigital/games/poker/online/ClientHoldemHand.java
public interface ClientHoldemHand {
    // All getters the UI currently calls on HoldemHand/RemoteHoldemHand
    Hand getCommunity();
    List<Pot> getPots();
    int getCurrentPot();
    BettingRound getRound();
    List<HandAction> getActions();
    // ... (full list determined during implementation)
}
```

`RemotePokerTable` and `RemoteHoldemHand` are updated to `implements ClientPokerTable / ClientHoldemHand` (removing `extends PokerTable / HoldemHand`).

All UI callers (`ShowTournamentTable`, dashboard items, `WebSocketTournamentDirector`, display panels) have their `PokerTable` / `HoldemHand` variable types changed to `ClientPokerTable` / `ClientHoldemHand`.

**What remains of `PokerTable`/`HoldemHand`/`PokerPlayer`/`PokerGame` in `poker` after Phase C:**

These classes are still needed by:
- Save game serialization (`PokerGameState`, `PokerGameStateDelegate`, `PokerSaveGame`)
- Local hand history display (`HandHistoryPanel`, `PokerDatabase`)
- Import/export (`ImpExpParadise`, `ImpExpUB`)
- ChainPhase orchestrators (still in game flow per Phase D)

They cannot be fully deleted. But they are no longer in the live UI rendering path — they become internal implementation details of persistence and legacy import code. That is the win.

**`Pot` in Phase C:** The `Pot.java` class is used by `HoldemHand` and display classes. In Phase C, `ClientHoldemHand.getPots()` returns a list of display-oriented pot records (not mutable game-engine `Pot` objects). Design this as an immutable `ClientPot` record:

```java
public record ClientPot(int chips, List<Long> eligiblePlayerIds, List<Long> winnerPlayerIds) {}
```

`RemoteHoldemHand` populates `ClientPot` instances from WebSocket data. The legacy `Pot` class stays for the persistence path.

**Scope:** Large — variable type changes across `ShowTournamentTable`, all `DashboardItem` subclasses, `WebSocketTournamentDirector`, and several display panels. The interface definitions are the critical design artifact that must be complete before implementation begins.

**Exit criterion:** `RemotePokerTable` and `RemoteHoldemHand` have no `extends` clause; `mvn test -P dev` passes; practice and online game play correctly through manual smoke test.

---

## What Does NOT Change

- `PokerDatabase`, `PokerGameState`, `PokerSaveGame` — local file I/O, stays in `poker`
- `HandHistoryPanel`, `ImpExp*` — local data display/IO, stays in `poker`
- AI config UI (`ai/gui/*`) — display-only configuration panels
- All Swing UI components — dashboard, dialogs, panels
- `Pot.java` and `PokerPlayer.java` — used for persistence/history; will not be deleted

---

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Phase C interface surface is large — callers may use many methods not captured in initial interface | Implement Phase C in a worktree; use compiler errors to drive the full interface definition |
| ChainPhase logic removal in Phase D breaks game flow | Write/run scenario tests before and after each class change |
| `pokerserver` POM removal of `poker` dep breaks transitive dep on something unexpected | Run `mvn dependency:tree` before and after; check for any class-not-found at test time |
| `HandAction` move breaks DataCoder registration (if it has a `@DataCoder` key) | Check DataCoder annotation on `HandAction` before move; update registration map if needed |

---

## Implementation Notes

- All phases run sequentially: A → B → D → C
- Each phase is a standalone commit (or small commit set) that passes `mvn test -P dev`
- Phase C uses a git worktree to isolate the large refactor
- Scenario tests (`tests/scenarios/`) run after Phase D and Phase C to catch regressions in game flow
- Copyright: Phase A/B file moves keep original header; Phase C new interfaces use community copyright (2026)
