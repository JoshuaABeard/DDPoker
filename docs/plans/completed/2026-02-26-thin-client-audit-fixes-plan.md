# Thin Client Audit Fixes — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix 4 bugs discovered during systematic audit of the thin client WebSocket system.

**Architecture:** All fixes are surgical — 1-5 lines of production code each. Bugs span two files: `AdvanceAction.java` (pre-action button logic) and `WebSocketTournamentDirector.java` (WS message handlers and table data application). Tests use the existing `WebSocketTournamentDirectorTest` infrastructure for Bugs 2-4 and a new `AdvanceActionWsTest` for Bug 1.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Mockito, Jackson (JSON), Swing EDT

**Design doc:** `docs/plans/2026-02-25-thin-client-audit-fixes.md`

---

### Task 1: Fix `canAllIn` ignored in `_getAdvanceActionWS`

**Severity:** Medium (gameplay impact — sends ALL_IN when server expects CALL)

**Root cause:** The 7-param `getAdvanceActionWS` accepts `canAllIn` and `allInAmount` but never forwards them to `_getAdvanceActionWS`. When the all-in button is pre-selected and `canAllIn=false` (short-stack scenario), it sends `"ALL_IN"` instead of `"CALL"`.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/AdvanceAction.java:375-402`
- Create: `code/poker/src/test/java/com/donohoedigital/games/poker/dashboard/AdvanceActionWsTest.java`

**Step 1: Write the failing test**

Create `AdvanceActionWsTest.java` using reflection to set up the static `impl_` and checkbox fields, then call the public static `getAdvanceActionWS()` method:

```java
package com.donohoedigital.games.poker.dashboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class AdvanceActionWsTest {

    @BeforeEach
    void setUp() throws Exception {
        setImpl(createBareImpl());
    }

    @AfterEach
    void tearDown() throws Exception {
        setImpl(null);
    }

    @Test
    void allinSelectedWithCanAllInTrueReturnsAllIn() throws Exception {
        selectOnly("allin_");
        String[] result = AdvanceAction.getAdvanceActionWS(
                false, false, true, 500, false, 0, 0);
        assertThat(result).isNotNull();
        assertThat(result[0]).isEqualTo("ALL_IN");
        assertThat(result[1]).isEqualTo("500");
    }

    @Test
    void allinSelectedWithCanAllInFalseReturnsCall() throws Exception {
        selectOnly("allin_");
        String[] result = AdvanceAction.getAdvanceActionWS(
                false, false, false, 0, false, 0, 0);
        assertThat(result).isNotNull();
        assertThat(result[0]).isEqualTo("CALL");
        assertThat(result[1]).isEqualTo("0");
    }

    @Test
    void checkfoldSelectedWithCanCheckTrueReturnsCheck() throws Exception {
        selectOnly("checkfold_");
        String[] result = AdvanceAction.getAdvanceActionWS(
                true, false, false, 0, false, 0, 0);
        assertThat(result).isNotNull();
        assertThat(result[0]).isEqualTo("CHECK");
    }

    @Test
    void checkfoldSelectedWithCanCheckFalseReturnsFold() throws Exception {
        selectOnly("checkfold_");
        String[] result = AdvanceAction.getAdvanceActionWS(
                false, false, false, 0, false, 0, 0);
        assertThat(result).isNotNull();
        assertThat(result[0]).isEqualTo("FOLD");
    }

    @Test
    void noButtonSelectedReturnsNull() throws Exception {
        String[] result = AdvanceAction.getAdvanceActionWS(
                true, false, true, 500, false, 0, 0);
        assertThat(result).isNull();
    }

    // ---- reflection helpers ----

    private static AdvanceAction createBareImpl() throws Exception {
        // Bypass normal constructor — use Unsafe or Objenesis-style allocation
        // Actually, we allocate via reflection and manually init the fields we need
        Constructor<AdvanceAction> ctor = AdvanceAction.class.getDeclaredConstructor(
                com.donohoedigital.games.engine.GameContext.class);
        // Can't call the real constructor without GameContext; use field injection instead.
        // sun.misc.Unsafe approach to allocate without calling constructor:
        var unsafe = getUnsafe();
        AdvanceAction impl = (AdvanceAction) unsafe.allocateInstance(AdvanceAction.class);

        // Initialize buttons_ list and checkbox fields
        Field buttonsField = AdvanceAction.class.getDeclaredField("buttons_");
        buttonsField.setAccessible(true);
        buttonsField.set(impl, new java.util.ArrayList<>());

        // Create real JCheckBox instances for each advance button field
        for (String name : new String[]{"checkfold_", "call_", "bet_", "raise_",
                "betpot_", "raisepot_", "allin_"}) {
            JCheckBox cb = new JCheckBox();
            Field f = AdvanceAction.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(impl, cb);
            @SuppressWarnings("unchecked")
            java.util.ArrayList<Object> buttons = (java.util.ArrayList<Object>) buttonsField.get(impl);
            buttons.add(cb);
        }

        return impl;
    }

    private static void setImpl(AdvanceAction impl) throws Exception {
        Field f = AdvanceAction.class.getDeclaredField("impl_");
        f.setAccessible(true);
        f.set(null, impl);
    }

    private static void selectOnly(String fieldName) throws Exception {
        Field implField = AdvanceAction.class.getDeclaredField("impl_");
        implField.setAccessible(true);
        AdvanceAction impl = (AdvanceAction) implField.get(null);

        // Deselect all
        for (String name : new String[]{"checkfold_", "call_", "bet_", "raise_",
                "betpot_", "raisepot_", "allin_"}) {
            Field f = AdvanceAction.class.getDeclaredField(name);
            f.setAccessible(true);
            ((JCheckBox) f.get(impl)).setSelected(false);
        }
        // Select target
        Field target = AdvanceAction.class.getDeclaredField(fieldName);
        target.setAccessible(true);
        ((JCheckBox) target.get(impl)).setSelected(true);
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception {
        Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd code && mvn test -pl poker -Dtest=AdvanceActionWsTest -P fast`

Expected: `allinSelectedWithCanAllInTrueReturnsAllIn` FAILS (amount is `"0"` not `"500"`) and `allinSelectedWithCanAllInFalseReturnsCall` FAILS (action is `"ALL_IN"` not `"CALL"`).

**Step 3: Write minimal implementation**

In `AdvanceAction.java`, change the 7-param overload to forward `canAllIn` and `allInAmount`:

```java
// Line 379: change the call to _getAdvanceActionWS to include canAllIn, allInAmount
String[] result = impl_._getAdvanceActionWS(canCheck, canBet, maxBet, canRaise, maxRaise, canAllIn, allInAmount);
```

Update `_getAdvanceActionWS` signature and guard the `allin_` branch:

```java
private String[] _getAdvanceActionWS(boolean canCheck, boolean canBet, int maxBet, boolean canRaise, int maxRaise,
        boolean canAllIn, int allInAmount) {
    if (checkfold_.isSelected()) {
        return new String[]{canCheck ? "CHECK" : "FOLD", "0"};
    } else if (call_.isSelected()) {
        return new String[]{"CALL", "0"};
    } else if (bet_.isSelected() && canBet) {
        return new String[]{"BET", String.valueOf(maxBet)};
    } else if (raise_.isSelected() && canRaise) {
        return new String[]{"RAISE", String.valueOf(maxRaise)};
    } else if (betpot_.isSelected() && canBet) {
        return new String[]{"BET", String.valueOf(maxBet)};
    } else if (raisepot_.isSelected() && canRaise) {
        return new String[]{"RAISE", String.valueOf(maxRaise)};
    } else if (allin_.isSelected()) {
        if (canAllIn) {
            return new String[]{"ALL_IN", String.valueOf(allInAmount)};
        } else {
            return new String[]{"CALL", "0"};
        }
    }
    return null;
}
```

**Step 4: Run test to verify it passes**

Run: `cd code && mvn test -pl poker -Dtest=AdvanceActionWsTest -P fast`
Expected: All 5 tests PASS.

**Step 5: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/dashboard/AdvanceAction.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/dashboard/AdvanceActionWsTest.java
git commit -m "fix: forward canAllIn/allInAmount in AdvanceAction._getAdvanceActionWS"
```

---

### Task 2: Fix `isPreFlop_` race condition in `onPlayerActed`

**Severity:** Low (opponent tracking stats miscategorization)

**Root cause:** `isPreFlop_` is written on the WS listener thread (`onHandStarted` line 726, `onCommunityCardsDealt` line 818) but read inside EDT lambdas (`onPlayerActed` line 929). When the EDT is backlogged, `onCommunityCardsDealt` can set `isPreFlop_=false` before the queued `onPlayerActed` lambda executes.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java:919-935`
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Step 1: Write the failing test**

Add to `WebSocketTournamentDirectorTest.java`:

```java
@Test
void playerActedPreFlopCapturedBeforeCommunityCards() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    requireTable();

    // Dispatch PLAYER_ACTED but do NOT drain EDT yet — lambda is queued
    ObjectNode payload = mapper.createObjectNode();
    payload.put("playerId", 1L).put("playerName", "Alice").put("action", "RAISE")
            .put("amount", 200).put("totalBet", 200).put("chipCount", 800).put("potTotal", 300);
    wsTD.onMessage(new WebSocketGameClient.InboundMessage(
            ServerMessageType.PLAYER_ACTED, "test-game-id", payload, null));

    // Now dispatch COMMUNITY_CARDS_DEALT on the same (calling) thread —
    // this sets isPreFlop_ = false BEFORE the queued PLAYER_ACTED lambda runs
    ObjectNode ccPayload = mapper.createObjectNode();
    ccPayload.put("tableId", 1).put("round", "FLOP");
    ccPayload.putArray("cards").add("As").add("Kd").add("Qc");
    ccPayload.putArray("allCommunityCards").add("As").add("Kd").add("Qc");
    wsTD.onMessage(new WebSocketGameClient.InboundMessage(
            ServerMessageType.COMMUNITY_CARDS_DEALT, "test-game-id", ccPayload, null));

    // Now drain EDT — both lambdas execute
    drainEdt();

    // The RAISE action should have been tracked as a pre-flop action
    // (isPreFlop_ was true when PLAYER_ACTED arrived, even though it was false
    // by the time the EDT lambda ran). Call onHandComplete() to commit stats,
    // then verify tightness is not NaN (= action was recorded) and aggression
    // is 1.0 (= the RAISE was recorded as aggressive).
    WebSocketOpponentTracker tracker = wsTD.getOpponentTrackerForTest();
    tracker.onHandComplete();
    assertThat(tracker.getTightness(1)).isNotNaN();
    assertThat(tracker.getAggression(1)).isEqualTo(1.0f);
}
```

This test requires adding a `getOpponentTrackerForTest()` accessor on `WebSocketTournamentDirector` (package-private, same pattern as `getTableForTest`).

**Step 2: Run test to verify it fails**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#playerActedPreFlopCapturedBeforeCommunityCards -P fast`

Expected: FAIL — `getTightness(1)` returns NaN because the lambda reads `isPreFlop_` which is already `false`, so `onPreFlopAction` was never called and no stats were recorded.

**Step 3: Write minimal implementation**

In `WebSocketTournamentDirector.java`, capture `isPreFlop_` into a local before `invokeLater`:

```java
private void onPlayerActed(PlayerActedData d) {
    final boolean preFlop = isPreFlop_;
    SwingUtilities.invokeLater(() -> {
        RemotePokerTable table = tables_.getOrDefault(d.tableId(), currentTable());
        if (table == null)
            return;
        RemoteHoldemHand hand = table.getRemoteHand();
        if (hand == null)
            return;

        // Record pre-flop action for opponent style tracking (skip blinds/antes)
        if (preFlop) {
            String actionStr = d.action();
            if (!"ANTE".equalsIgnoreCase(actionStr)
                    && !"BLIND_SM".equalsIgnoreCase(actionStr) && !"BLIND_BIG".equalsIgnoreCase(actionStr)) {
                opponentTracker_.onPreFlopAction((int) d.playerId(), actionStr);
            }
        }
        // ... rest of method unchanged ...
```

Also remove the dead `"BLIND_BET"` entry from the filter (server `ActionType` enum has no `BLIND_BET`; only `BLIND_SM` and `BLIND_BIG` are sent).

Add test accessor to `WebSocketTournamentDirector`:

```java
/** Exposes the opponent tracker for unit testing. */
WebSocketOpponentTracker getOpponentTrackerForTest() {
    return opponentTracker_;
}
```

**Step 4: Run test to verify it passes**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#playerActedPreFlopCapturedBeforeCommunityCards -P fast`
Expected: PASS

**Step 5: Run full test suite for regressions**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`
Expected: All tests PASS.

**Step 6: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git add code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java
git commit -m "fix: capture isPreFlop_ before invokeLater in onPlayerActed, remove dead BLIND_BET filter"
```

---

### Task 3: Clear community cards in `onHandStarted`

**Severity:** Low (stale community cards visible after reconnect)

**Root cause:** `onHandStarted` reuses the previous `RemoteHoldemHand` but only calls `clearWins()`. Community cards from the previous hand persist. After reconnect (no per-player GAME_STATE before HAND_STARTED), old community cards remain visible.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java:754-759`
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Step 1: Write the failing test**

Add to `WebSocketTournamentDirectorTest.java`:

```java
@Test
void handStartedClearsCommunityCards() throws Exception {
    dispatch(ServerMessageType.GAME_STATE, buildGameState(1, 0, 1L));
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));
    RemotePokerTable table = requireTable();

    // Simulate flop — community cards are set
    ObjectNode ccPayload = mapper.createObjectNode();
    ccPayload.put("tableId", 1).put("round", "FLOP");
    ccPayload.putArray("cards").add("As").add("Kd").add("Qc");
    ccPayload.putArray("allCommunityCards").add("As").add("Kd").add("Qc");
    dispatch(ServerMessageType.COMMUNITY_CARDS_DEALT, ccPayload);

    assertThat(table.getRemoteHand().getCommunity().size()).isEqualTo(3);

    // New hand starts — community cards must be cleared
    dispatch(ServerMessageType.HAND_STARTED, handStarted(0, 1, 2));

    assertThat(table.getRemoteHand().getCommunity().size()).isEqualTo(0);
}
```

**Step 2: Run test to verify it fails**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#handStartedClearsCommunityCards -P fast`

Expected: FAIL — community size is still 3 because `clearWins()` doesn't clear community cards.

**Step 3: Write minimal implementation**

In `WebSocketTournamentDirector.java` `onHandStarted`, add `hand.updateCommunity(new Hand())` after `clearWins()`:

```java
} else {
    hand.clearWins();
    hand.updateCommunity(new Hand());
}
```

**Step 4: Run test to verify it passes**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#handStartedClearsCommunityCards -P fast`
Expected: PASS

**Step 5: Run full test suite for regressions**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`
Expected: All tests PASS.

**Step 6: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git commit -m "fix: clear community cards in onHandStarted to prevent stale display after reconnect"
```

---

### Task 4: Add SITTING_OUT guard in `applyTableData`

**Severity:** Low (incorrect face-down card images for non-participating players)

**Root cause:** `applyTableData` adds blank cards for all non-local, non-`"FOLDED"` players. Server sends `"SITTING_OUT"` for sitting-out players (confirmed in `OutboundMessageConverter.java:297`), so they pass the condition and get face-down card images despite not being in the hand.

**Files:**
- Modify: `code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java:2350`
- Modify: `code/poker/src/test/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirectorTest.java`

**Step 1: Write the failing test**

Add to `WebSocketTournamentDirectorTest.java`:

```java
@Test
void applyTableDataSittingOutPlayerGetsNoCards() throws Exception {
    // Build a GAME_STATE with a SITTING_OUT player at seat 1
    ObjectNode gs = mapper.createObjectNode();
    gs.put("status", "IN_PROGRESS").put("level", 0);
    ObjectNode blinds = mapper.createObjectNode();
    blinds.put("small", 50).put("big", 100).put("ante", 0);
    gs.set("blinds", blinds);
    gs.putNull("nextLevelIn");

    ObjectNode t = mapper.createObjectNode();
    t.put("tableId", 1).put("currentRound", "PRE_FLOP").put("handNumber", 1);
    t.putArray("communityCards");
    t.putArray("pots");
    var seats = t.putArray("seats");

    // Local player at seat 0 (ACTIVE)
    ObjectNode seat0 = mapper.createObjectNode();
    seat0.put("seatIndex", 0).put("playerId", 1L).put("playerName", "Alice").put("chipCount", 1000)
            .put("status", "ACTIVE").put("isDealer", true).put("isSmallBlind", false)
            .put("isBigBlind", false).put("currentBet", 0).put("isCurrentActor", false);
    seat0.putArray("holeCards");
    seats.add(seat0);

    // Opponent at seat 1 (SITTING_OUT)
    ObjectNode seat1 = mapper.createObjectNode();
    seat1.put("seatIndex", 1).put("playerId", 2L).put("playerName", "Bob").put("chipCount", 1000)
            .put("status", "SITTING_OUT").put("isDealer", false).put("isSmallBlind", false)
            .put("isBigBlind", false).put("currentBet", 0).put("isCurrentActor", false);
    seat1.putArray("holeCards");
    seats.add(seat1);

    gs.putArray("tables").add(t);
    gs.putArray("players");

    dispatch(ServerMessageType.GAME_STATE, gs);
    RemotePokerTable table = requireTable();

    // Find Bob (seat 1) and verify no blank cards
    com.donohoedigital.games.poker.PokerPlayer bob = table.getPlayerAt(1);
    assertThat(bob).isNotNull();
    assertThat(bob.getHand().size()).isEqualTo(0);
}
```

**Step 2: Run test to verify it fails**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#applyTableDataSittingOutPlayerGetsNoCards -P fast`

Expected: FAIL — Bob's hand size is 2 (two blank cards) because the `SITTING_OUT` status is not filtered.

**Step 3: Write minimal implementation**

In `WebSocketTournamentDirector.java` line 2350, add `&& !"SITTING_OUT".equals(sd.status())`:

```java
} else if (sd.playerId() != localPlayerId_ && !"FOLDED".equals(sd.status())
        && !"SITTING_OUT".equals(sd.status())) {
```

**Step 4: Run test to verify it passes**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest#applyTableDataSittingOutPlayerGetsNoCards -P fast`
Expected: PASS

**Step 5: Run full test suite for regressions**

Run: `cd code && mvn test -pl poker -Dtest=WebSocketTournamentDirectorTest -P fast`
Expected: All tests PASS.

**Step 6: Commit**

```bash
git add code/poker/src/main/java/com/donohoedigital/games/poker/online/WebSocketTournamentDirector.java
git commit -m "fix: exclude SITTING_OUT players from blank-card assignment in applyTableData"
```

---

### Task 5: Final verification

**Step 1: Run full poker module tests**

Run: `cd code && mvn test -pl poker -P fast`
Expected: All tests PASS.

**Step 2: Run full project build**

Run: `cd code && mvn test -P dev`
Expected: BUILD SUCCESS with all tests passing.

**Step 3: Mark plan as completed**

Move the design doc status to COMPLETED and move both documents to `docs/plans/completed/`.
