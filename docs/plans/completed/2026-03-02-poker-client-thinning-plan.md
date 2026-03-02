# Poker Desktop Client Thinning — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove poker logic from the desktop client module so `pokerserver` no longer depends on `poker`, shared logic lives in `pokerengine`, and the UI path has zero dependency on the client-side game engine.

**Architecture:** Four sequential phases — A (fix backwards dependency), B (extract shared logic), D (strip ChainPhase rule logic), C (break Remote* inheritance). Each phase is self-contained and ships as a passing build. Design doc: `docs/plans/2026-03-02-poker-client-thinning-design.md`.

**Tech Stack:** Java 25, Maven multi-module, JUnit 5, AssertJ. Build: `cd code && mvn test -P dev` (fast, skips integration tests, 4 threads). Full: `mvn test`.

---

## Phase A — Fix the Backwards Dependency

**Problem:** `pokerserver` (backend) imports `com.donohoedigital.games.poker.HandAction` from the `poker` (desktop client) module. The import is only for integer constants (`ACTION_RAISE`, `ACTION_CALL`, etc.). `HandAction` itself can't move to `pokerengine` because it holds a `PokerPlayer` reference.

**Fix:** Create `PokerActionConstants` in `pokerengine` with the 12 integer constants. Update `pokerserver` to import from there instead. Remove `poker` from `pokerserver`'s POM.

---

### Task 1: Create `PokerActionConstants` in `pokerengine`

**Files:**
- Create: `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/PokerActionConstants.java`

**Step 1: Create the file**

```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.engine;

/**
 * Integer constants for poker hand action types.
 *
 * <p>These match the legacy {@code HandAction.ACTION_*} values exactly so that
 * server-side code can switch on action integers without depending on the
 * desktop-client {@code HandAction} class.
 */
public final class PokerActionConstants {

    public static final int ACTION_NONE = -1;
    public static final int ACTION_FOLD = 0;
    public static final int ACTION_CHECK = 1;
    public static final int ACTION_CHECK_RAISE = 2;
    public static final int ACTION_CALL = 3;
    public static final int ACTION_BET = 4;
    public static final int ACTION_RAISE = 5;
    public static final int ACTION_BLIND_BIG = 6;
    public static final int ACTION_BLIND_SM = 7;
    public static final int ACTION_ANTE = 8;
    public static final int ACTION_WIN = 9;
    public static final int ACTION_OVERBET = 10;
    public static final int ACTION_LOSE = 11;

    // Fold sub-type constants (stored in HandAction.nSubAmount_)
    public static final int FOLD_NORMAL = 0;
    public static final int FOLD_FORCED = -1;
    public static final int FOLD_SITTING_OUT = -2;

    private PokerActionConstants() {
    }
}
```

**Step 2: Verify it compiles**

```bash
cd code && mvn compile -pl pokerengine -P fast -q
```

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/PokerActionConstants.java
git commit -m "feat(pokerengine): add PokerActionConstants — shared action integer constants"
```

---

### Task 2: Replace `HandAction` imports in `pokerserver` and remove POM dependency

**Files:**
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerOpponentTracker.java`
- Modify: `code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerAIContext.java`
- Modify: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerOpponentTrackerTest.java`
- Modify: `code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerAIContextTest.java`
- Modify: `code/pokerserver/pom.xml`

**Step 1: Update `ServerOpponentTracker.java`**

Replace:
```java
import com.donohoedigital.games.poker.HandAction;
```
With:
```java
import com.donohoedigital.games.poker.engine.PokerActionConstants;
```

Then replace all uses of `HandAction.ACTION_*` and `HandAction.FOLD_*`:
- `HandAction.ACTION_RAISE` → `PokerActionConstants.ACTION_RAISE`
- `HandAction.ACTION_CALL` → `PokerActionConstants.ACTION_CALL`
- `HandAction.ACTION_FOLD` → `PokerActionConstants.ACTION_FOLD`
- `HandAction.ACTION_BET` → `PokerActionConstants.ACTION_BET`
- `HandAction.ACTION_CHECK` → `PokerActionConstants.ACTION_CHECK`

The javadoc comment on line 64 (`Action type (HandAction.ACTION_*)`) should be updated to:
```
Action type (PokerActionConstants.ACTION_*)
```

**Step 2: Update `ServerAIContext.java`**

Replace:
```java
import com.donohoedigital.games.poker.HandAction;
```
With:
```java
import com.donohoedigital.games.poker.engine.PokerActionConstants;
```

Then replace `HandAction.ACTION_BET` and `HandAction.ACTION_RAISE` (line 134) with:
```java
if (action == PokerActionConstants.ACTION_BET || action == PokerActionConstants.ACTION_RAISE) {
```

**Step 3: Update `ServerOpponentTrackerTest.java`**

This test has `import com.donohoedigital.games.poker.HandAction;` at top.
Replace with:
```java
import com.donohoedigital.games.poker.engine.PokerActionConstants;
```

Replace all `HandAction.ACTION_*` usages with `PokerActionConstants.ACTION_*`.

**Step 4: Update `ServerAIContextTest.java`**

This test uses fully-qualified `com.donohoedigital.games.poker.HandAction.ACTION_BET` etc. (no import at top).
Add import:
```java
import com.donohoedigital.games.poker.engine.PokerActionConstants;
```
Replace all `com.donohoedigital.games.poker.HandAction.ACTION_*` with `PokerActionConstants.ACTION_*`.

**Step 5: Remove `poker` dependency from `pokerserver/pom.xml`**

Find and delete the block:
```xml
<dependency>
    <groupId>com.donohoedigital</groupId>
    <artifactId>poker</artifactId>
    <version>${project.version}</version>
</dependency>
```

**Step 6: Verify `pokerserver` compiles without `poker`**

```bash
cd code && mvn compile -pl pokerengine,pokerserver -am -P fast -q
```

Expected: BUILD SUCCESS

**Step 7: Run `pokerserver` tests**

```bash
cd code && mvn test -pl pokerengine,pokerserver -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 8: Verify no `poker` in `pokerserver` dependency tree**

```bash
cd code && mvn dependency:tree -pl pokerserver | grep "poker"
```

Expected: Only `pokerengine`, `pokergamecore`, `pokernetwork` appear — NOT the `poker` artifact.

**Step 9: Commit**

```bash
git add code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerOpponentTracker.java
git add code/pokerserver/src/main/java/com/donohoedigital/games/poker/server/ServerAIContext.java
git add code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerOpponentTrackerTest.java
git add code/pokerserver/src/test/java/com/donohoedigital/games/poker/server/ServerAIContextTest.java
git add code/pokerserver/pom.xml
git commit -m "fix(pokerserver): remove backwards dependency on poker client module

Replace HandAction.ACTION_* constant imports with PokerActionConstants
from pokerengine. Remove poker from pokerserver POM dependencies."
```

---

### Task 3: Phase A Full Verification

**Step 1: Run full project build**

```bash
cd code && mvn test -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 2: Update `docs/memory.md`**

Remove the entry about `HandInfo`, `HoldemSimulator` etc. being stuck (already resolved in prior cleanup). Add new entry:

```
- [architecture] `pokerserver` no longer depends on `poker` (client). Action constants moved to `PokerActionConstants` in `pokerengine` (2026-03-02)
```

**Step 3: Commit memory update**

```bash
git add docs/memory.md
git commit -m "docs: update memory — pokerserver→poker backwards dependency resolved"
```

---

## Phase B — Extract `HandUtils` to `pokerengine`

**Why:** `HandUtils` contains pure hand evaluation logic (`getBestFive` from hole + community cards). It has zero game-object or Swing dependencies — only `Card`, `Hand`, `HandSorted`, `CardSuit` from `pokerengine`. Moving it there puts it alongside `HandInfoFaster` and other pure evaluation utilities.

---

### Task 4: Move `HandUtils` from `poker` to `pokerengine`

**Files:**
- Delete: `code/poker/src/main/java/com/donohoedigital/games/poker/HandUtils.java`
- Create: `code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/HandUtils.java`
- Modify: all callers in `poker` that import `com.donohoedigital.games.poker.HandUtils`

**Step 1: Find all callers**

```bash
grep -rn "import com.donohoedigital.games.poker.HandUtils\|HandUtils\." \
  code/poker/src/main/java code/poker/src/test/java --include="*.java" -l
```

Note the files returned — those are the ones needing import updates.

**Step 2: Create `HandUtils.java` in `pokerengine`**

Copy the content of the existing file verbatim, changing only:
- Line 1 copyright block: keep community copyright (already present)
- Line 20: `package com.donohoedigital.games.poker;` → `package com.donohoedigital.games.poker.engine;`
- Remove the four import lines for `Card`, `CardSuit`, `Hand`, `HandSorted` (they are now in the same package)

The rest of the class body is identical.

**Step 3: Delete the original**

```bash
git rm code/poker/src/main/java/com/donohoedigital/games/poker/HandUtils.java
```

**Step 4: Update callers in `poker`**

For each file found in Step 1, change:
```java
import com.donohoedigital.games.poker.HandUtils;
```
To:
```java
import com.donohoedigital.games.poker.engine.HandUtils;
```

**Step 5: Verify build**

```bash
cd code && mvn compile -pl pokerengine,poker -am -P fast -q
```

Expected: BUILD SUCCESS

**Step 6: Run tests**

```bash
cd code && mvn test -pl pokerengine,poker -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 7: Commit**

```bash
git add code/pokerengine/src/main/java/com/donohoedigital/games/poker/engine/HandUtils.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/
git add code/poker/src/test/java/
git commit -m "refactor(pokerengine): move HandUtils from poker to pokerengine

Pure hand evaluation logic belongs alongside HandInfoFaster and other
engine utilities. No behavioral change — package rename only."
```

---

## Phase D — Strip Server-Duplicated Logic from ChainPhase Orchestrators

**Context:** `CheckEndHand`, `NewLevelActions`, `Bet`, `ColorUp`, `ColorUpFinish` are registered in `gamedef.xml` and execute in the WS-driven game path. They are not dead code. However, some contain poker *rule* logic that the server now owns authoritatively. The goal is to remove only rule logic; leave all UI coordination and dialog handling.

**Decision criteria for each block of code:**
- **Keep:** Dialog display, animation, input mode changes, event firing, calling `WebSocketTournamentDirector` methods
- **Strip:** Chip arithmetic, game-over condition evaluation, level-advance computation, action-validation guards that the server also enforces

**Process for each class:** read → mark → strip → test.

---

### Task 5: Audit and strip `CheckEndHand.java`

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/CheckEndHand.java`
- Modify (if needed): `code/poker/src/test/java/com/donohoedigital/games/poker/CheckEndHandTest.java`

**Step 1: Read the file**

Read `CheckEndHand.java` in full. For each method, determine:
- Is this method computing a game-over condition (e.g., `isGameOver()`, player broke checks)? → **Strip** (server decides this via `HAND_RESULT` or `GAME_OVER` message)
- Is this method driving a UI dialog (rebuy prompt, next-level display)? → **Keep**
- Is this method calling into `WebSocketTournamentDirector`? → **Keep**

**Step 2: Identify and strip redundant logic**

Common candidates for removal:
- Any method checking `PokerGame.isGameOver()` or similar client-side conditions — the server sends a `GAME_OVER` WebSocket message when the game ends; the client should respond to that, not compute it independently
- Any client-side chip-broke checks — the server sends `HAND_RESULT` with winner info; chip tracking on the client is duplicative
- Any `nextPhase()` calls that transition to game-over based on client-evaluated conditions — replace with a no-op or early return since server will drive this via WS event

Replacement pattern: where code previously made a game-logic decision, replace with a comment and a no-op:
```java
// Server now decides game-over via GAME_OVER WebSocket message.
// Client responds to that event in WebSocketTournamentDirector.onGameOver().
```

**Step 3: Delete any tests that tested the stripped logic**

If a test existed for a method that is now deleted, delete the test too.

**Step 4: Run tests**

```bash
cd code && mvn test -pl poker -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 5: Smoke test**

Start the game (embedded server) and play one full hand to verify game-over still works. Use the dev control server or manual play.

**Step 6: Commit**

```bash
git commit -m "refactor(poker): strip server-duplicated rule logic from CheckEndHand

Game-over conditions are now server-authoritative. Client responds to
GAME_OVER WebSocket message. Removed client-side evaluation."
```

---

### Task 6: Audit and strip `NewLevelActions.java`

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/NewLevelActions.java`
- Modify (if needed): corresponding test file

**Step 1: Read the file**

For each method, determine:
- Is this computing next blind/ante values or level timing? → **Strip** (server owns level state)
- Is this displaying a "new level" announcement dialog? → **Keep**
- Is this the `RebuyDecisionProvider` hook (used by `WebSocketTournamentDirector.onRebuyOffered()`)? → **Keep** (this is the WS integration point)
- Is this calling `NewLevelActions.rebuy()` (called from `ShowTournamentTable` on rebuy-offered event)? → **Keep**

**Step 2: Strip level-advance arithmetic**

Any block that computes `nextSmallBlind`, `nextBigBlind`, `nextAnte`, or evaluates the `BlindStructure` on the client side → remove. The server sends level info via WebSocket `LEVEL_CHANGED` or similar message.

**Step 3: Run tests and smoke test**

```bash
cd code && mvn test -pl poker -P dev
```

**Step 4: Commit**

```bash
git commit -m "refactor(poker): strip server-duplicated level logic from NewLevelActions

Blind/ante values and level timing are server-authoritative. Client
displays level info from WebSocket messages, not computed locally."
```

---

### Task 7: Audit and strip `Bet.java`

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/Bet.java`

**Step 1: Read the file**

For each method:
- Is this validating bet amounts, enforcing min/max raise rules, or checking if an action is legal? → **Strip** (server validates all actions; an invalid action is rejected, not previewed)
- Is this rendering the betting UI (sliders, buttons, amount display)? → **Keep**
- Is this calling `WebSocketTournamentDirector` to submit an action? → **Keep**

**Step 2: Strip validation guards**

Any client-side guard like `if (amount < minRaise) return false;` or similar — remove. The server rejects invalid actions; the client does not need to enforce poker betting rules locally.

**Step 3: Run tests and smoke test**

```bash
cd code && mvn test -pl poker -P dev
```

Test interactively: start a game and place a bet to verify the UI still works.

**Step 4: Commit**

```bash
git commit -m "refactor(poker): strip server-duplicated action validation from Bet

Bet amount validation is server-authoritative. Removed client-side
min/max-raise enforcement. UI still renders correct options from server."
```

---

### Task 8: Audit and strip `ColorUp.java` and `ColorUpFinish.java`

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ColorUp.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/ColorUpFinish.java`

**Step 1: Read both files**

For each method:
- Is this computing how many chips are exchanged, what denominations change? → **Strip** (color-up arithmetic is server-computed)
- Is this animating the chip color change on screen or displaying a color-up message? → **Keep**
- Is this waiting for user input to continue? → **Keep**

**Step 2: Strip chip arithmetic**

Any calculation like `nNew = nOld * oldDenom / newDenom` or similar color-up math → remove. The server sends updated chip counts after color-up; the client animates the change based on received state.

**Step 3: Run tests**

```bash
cd code && mvn test -pl poker -P dev
```

**Step 4: Commit**

```bash
git commit -m "refactor(poker): strip chip arithmetic from ColorUp/ColorUpFinish

Color-up chip calculation is server-authoritative. Client displays
server-provided chip counts rather than computing locally."
```

---

### Task 9: Phase D Full Verification

**Step 1: Run full build**

```bash
cd code && mvn test -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 2: Run scenario tests if available**

```bash
# From repo root
bash tests/scenarios/practice-game.sh
```

Expected: All scenario assertions pass

**Step 3: Commit memory update**

Add to `docs/memory.md`:
```
- [poker] ChainPhase orchestrators (CheckEndHand, NewLevelActions, Bet, ColorUp) no longer contain client-side poker rule logic — server is authoritative. Client responds to WS messages only. (2026-03-02)
```

```bash
git commit -m "docs: update memory — Phase D ChainPhase cleanup complete"
```

---

## Phase C — Break `Remote*` Inheritance

**Context:** `RemotePokerTable extends PokerTable` and `RemoteHoldemHand extends HoldemHand` pull the full client-side game engine into the UI classpath. Since all game display is WS-driven, the `Remote*` classes should stand alone as view models — no base class from the game engine.

**Strategy:** Use compiler-driven interface discovery. Remove `extends PokerTable` and `extends HoldemHand` from the `Remote*` classes, fix every resulting compile error by defining the missing methods on a new interface, and update all callers to use the interface type. The compiler tells you exactly what the interface must contain.

**Important — things that stay unchanged:**
- `PokerTable`, `HoldemHand`, `PokerPlayer`, `PokerGame` in `poker` are NOT deleted. They are still used by save game serialization, hand history, and import/export.
- `Pot.java` stays in `poker` — it has the DataCoder save-game serialization and is used by the persistence path.

---

### Task 10: Define `ClientPokerTable` and `ClientHoldemHand` interfaces (empty shells)

**Files:**
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientPokerTable.java`
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientHoldemHand.java`
- Create: `code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientPot.java`

**Step 1: Create empty interface shells**

`ClientPokerTable.java`:
```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
 * [GPL-3.0 header — see docs/guides/copyright-licensing-guide.md Template 3]
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

/**
 * Read-only view of a poker table for Swing UI components.
 *
 * <p>Implemented by {@link RemotePokerTable} which populates it from
 * WebSocket messages. UI code depends on this interface, not on the
 * game-engine {@code PokerTable} class.
 */
public interface ClientPokerTable {
    // Methods added incrementally by compiler-driven discovery in Task 11
}
```

`ClientHoldemHand.java`:
```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
 * [GPL-3.0 header — see docs/guides/copyright-licensing-guide.md Template 3]
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

/**
 * Read-only view of a poker hand for Swing UI components.
 *
 * <p>Implemented by {@link RemoteHoldemHand} which populates it from
 * WebSocket messages. UI code depends on this interface, not on the
 * game-engine {@code HoldemHand} class.
 */
public interface ClientHoldemHand {
    // Methods added incrementally by compiler-driven discovery in Task 12
}
```

`ClientPot.java`:
```java
/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 DD Poker Community
 * [GPL-3.0 header — see docs/guides/copyright-licensing-guide.md Template 3]
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import java.util.List;

/**
 * Immutable display snapshot of a single pot, populated from WebSocket data.
 * Used by UI components that need pot information without pulling in the
 * game-engine {@code Pot} class.
 *
 * @param chips             total chips in this pot
 * @param eligiblePlayerIds seat indices of players eligible to win
 * @param winnerPlayerIds   seat indices of winners (empty until showdown)
 */
public record ClientPot(int chips, List<Integer> eligiblePlayerIds, List<Integer> winnerPlayerIds) {
}
```

**Step 2: Compile to confirm files are valid**

```bash
cd code && mvn compile -pl poker -P fast -q
```

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientPokerTable.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientHoldemHand.java
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientPot.java
git commit -m "feat(poker): add ClientPokerTable, ClientHoldemHand, ClientPot interface shells

Empty interfaces for Phase C — methods populated by compiler-driven
discovery when Remote* inheritance is broken."
```

---

### Task 11: Break `RemotePokerTable` inheritance — compiler-driven interface discovery

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RemotePokerTable.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientPokerTable.java`

**Step 1: Remove `extends PokerTable` and add `implements ClientPokerTable`**

In `RemotePokerTable.java`, change the class declaration:
```java
// Before:
public class RemotePokerTable extends PokerTable {

// After:
public class RemotePokerTable implements ClientPokerTable {
```

Remove the `super(game, nNum)` call from the constructor — `ClientPokerTable` is an interface, there is no superclass constructor.

The constructor will need to store whatever fields it was relying on `PokerTable` to store. Add local fields for anything the constructor previously passed to `super()`.

**Step 2: Attempt to compile**

```bash
cd code && mvn compile -pl poker -P fast 2>&1 | grep "error:"
```

This will produce many compile errors. **Each error is a missing method.** For each error of the form `cannot find symbol: method foo() in RemotePokerTable`, that method exists in `PokerTable` and is called by UI code — it must be declared in `ClientPokerTable` and implemented in `RemotePokerTable`.

**Step 3: For each compile error, add the method to the interface and implement it**

Pattern:
1. Error says caller expects `table.getLevel()` but `RemotePokerTable` no longer has it
2. Add `int getLevel();` to `ClientPokerTable`
3. Add `@Override public int getLevel() { return remoteLevel_; }` to `RemotePokerTable` (with a new `private int remoteLevel_` field if not already present)
4. Add `setRemoteLevel(int level)` setter for the field so `WebSocketTournamentDirector` can populate it

Repeat until `mvn compile -pl poker -P fast` produces zero errors.

**Step 4: Update callers to use `ClientPokerTable` type**

Search for all local variables declared as `PokerTable table = ...` where the assigned value is actually a `RemotePokerTable`:

```bash
grep -rn "PokerTable " code/poker/src/main/java --include="*.java" | grep -v "import\|new PokerTable\|extends PokerTable"
```

For each: change `PokerTable` to `ClientPokerTable` if the variable holds a remote table.

**Step 5: Run tests**

```bash
cd code && mvn test -pl poker -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 6: Commit**

```bash
git commit -m "refactor(poker): RemotePokerTable implements ClientPokerTable (no extends)

Break inheritance from PokerTable. UI path now depends on ClientPokerTable
interface. PokerTable stays for save-game and hand-history paths."
```

---

### Task 12: Break `RemoteHoldemHand` inheritance — compiler-driven interface discovery

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/RemoteHoldemHand.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/ClientHoldemHand.java`
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java` (type references)

**Step 1: Remove `extends HoldemHand` and add `implements ClientHoldemHand`**

In `RemoteHoldemHand.java`, change:
```java
// Before:
public class RemoteHoldemHand extends HoldemHand {

// After:
public class RemoteHoldemHand implements ClientHoldemHand {
```

Remove the `super()` call and the `initHandLists()` call (these were needed to prevent NPE in parent methods; now there is no parent).

**Step 2: Attempt to compile**

```bash
cd code && mvn compile -pl poker -P fast 2>&1 | grep "error:"
```

For each missing method, add it to `ClientHoldemHand` and implement it in `RemoteHoldemHand`.

**Special handling — pot methods:**
If a caller uses `hand.getPot(i)` or `hand.getPots()` expecting `Pot` objects, replace with `hand.getClientPots()` returning `List<ClientPot>`. Add `List<ClientPot> getClientPots();` to `ClientHoldemHand`. In `RemoteHoldemHand`, populate `clientPots_` in `updatePot()`.

**Step 3: Update `WebSocketTournamentDirector`**

Any `RemoteHoldemHand` reference currently typed as `HoldemHand hand` → change to `ClientHoldemHand hand` (or keep as `RemoteHoldemHand` for the concrete type where needed for setters).

**Step 4: Update all UI callers**

```bash
grep -rn "HoldemHand " code/poker/src/main/java --include="*.java" | grep -v "import\|new HoldemHand\|extends HoldemHand\|RemoteHoldemHand"
```

For each: change `HoldemHand` → `ClientHoldemHand` if the variable holds a remote hand.

**Step 5: Run tests**

```bash
cd code && mvn test -pl poker -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 6: Commit**

```bash
git commit -m "refactor(poker): RemoteHoldemHand implements ClientHoldemHand (no extends)

Break inheritance from HoldemHand. UI game path depends on ClientHoldemHand
interface. HoldemHand stays for hand history and persistence paths."
```

---

### Task 13: Phase C Final Verification

**Step 1: Run full build**

```bash
cd code && mvn test -P dev
```

Expected: BUILD SUCCESS, all tests PASS

**Step 2: Verify game engine classes are no longer in the UI call graph**

```bash
grep -rn "extends PokerTable\|extends HoldemHand" code/poker/src/main/java --include="*.java"
```

Expected: Zero results (no class in `poker` extends `PokerTable` or `HoldemHand` except classes not in the game display path).

```bash
grep -rn "new PokerTable\|new HoldemHand" code/poker/src/main/java/com/donohoedigital/games/poker/online --include="*.java"
```

Expected: Zero results in the `online/` package (the WS bridge no longer instantiates game engine objects).

**Step 3: Manual smoke test**

- Start DDPoker with embedded server (`mvn clean package -DskipTests -P dev`)
- Launch a practice game
- Play several hands including showdown, rebuy, and level advance
- Connect a second client and play an online hand to verify WS game display

**Step 4: Update design doc status**

Edit `docs/plans/2026-03-02-poker-client-thinning-design.md`, add at top:
```
**Status:** COMPLETED (2026-03-02)
```

Move both the design doc and this plan to `docs/plans/completed/`.

**Step 5: Update `docs/memory.md`**

Add entry:
```
- [poker] RemotePokerTable and RemoteHoldemHand are now standalone view models (no extends). UI game display path has zero dependency on PokerTable/HoldemHand game engine classes. Use ClientPokerTable/ClientHoldemHand interfaces in all new UI code. (2026-03-02)
```

**Step 6: Final commit**

```bash
git add docs/plans/completed/
git add docs/memory.md
git commit -m "docs: mark poker client thinning plan COMPLETED

All four phases complete:
- Phase A: pokerserver no longer depends on poker client
- Phase B: HandUtils moved to pokerengine
- Phase D: ChainPhase poker rule logic stripped
- Phase C: Remote* inheritance broken, ClientPokerTable/ClientHoldemHand interfaces introduced"
```

---

## Quick Reference

| Phase | What | Key files | Effort |
|-------|------|-----------|--------|
| A | Fix backwards dep (PokerActionConstants) | `pokerengine`, `pokerserver` x2 prod + tests, POM | Small (~1 hr) |
| B | Move HandUtils to pokerengine | 1 move + ~5 import updates | Tiny (~30 min) |
| D | Strip ChainPhase rule logic | `CheckEndHand`, `NewLevelActions`, `Bet`, `ColorUp` | Medium (~3-4 hrs) |
| C | Break Remote* inheritance | `Remote*` + `ClientPokerTable` + `ClientHoldemHand` + all callers | Large (~1 day) |

**Build commands:**
```bash
cd code && mvn test -P dev                    # fast (4-thread, skip integration)
cd code && mvn test -pl poker -P dev          # poker module only
cd code && mvn compile -pl poker -P fast -q   # compile-only for quick iteration
cd code && mvn dependency:tree -pl pokerserver | grep poker  # verify dep graph
```
