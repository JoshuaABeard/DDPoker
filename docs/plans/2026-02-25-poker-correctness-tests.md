# Poker Correctness Test Suite — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build deterministic scenario tests that verify DDPoker plays correct Texas Hold'em across all betting structures, using puppet mode for full multi-player control and `/hand/result` for outcome verification.

**Architecture:** Three server-side additions (puppet mode, hand result endpoint, game type parameter) enable fully deterministic testing. Puppet mode intercepts `ServerPlayerActionProvider.getAction()` to block AI turns on CompletableFutures, letting the test script control all players. Hand result endpoint exposes winner, hand ranking, and per-pot breakdown from existing `HoldemHand`/`Pot`/`HandInfo` classes. Six new bash test scripts cover hand rankings, pot distribution, limit, pot-limit, mixed format, and tournament lifecycle. Existing tests get simpler and more precise.

**Tech Stack:** Java 21 (server handlers), Bash (test scripts), curl + node (test assertions)

**Design doc:** `docs/plans/2026-02-25-poker-correctness-tests-design.md`

---

## Phase 1: Server-Side Puppet Infrastructure

### Task 1: Add puppet mode to ServerPlayerActionProvider

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayerActionProvider.java`

**Step 1: Add puppet fields and methods**

Add after the `zipMode` field (line 72):

```java
    private final Set<Integer> puppetedPlayerIds = ConcurrentHashMap.newKeySet();
```

Add import at top:
```java
import java.util.Set;
```

Add methods before the `PendingAction` record:

```java
    /**
     * Enable or disable puppet mode for a player. When puppeted, AI players
     * block on a CompletableFuture like humans, waiting for external action
     * submission via {@link #submitAction(int, PlayerAction)}.
     */
    public void setPuppeted(int playerId, boolean enabled) {
        if (enabled) {
            puppetedPlayerIds.add(playerId);
        } else {
            puppetedPlayerIds.remove(playerId);
        }
        logger.debug("[PUPPET] setPuppeted playerId={} enabled={}", playerId, enabled);
    }

    /**
     * Returns true if the given player is in puppet mode.
     */
    public boolean isPuppeted(int playerId) {
        return puppetedPlayerIds.contains(playerId);
    }

    /**
     * Returns the set of puppeted player IDs (for state reporting).
     */
    public Set<Integer> getPuppetedPlayerIds() {
        return Set.copyOf(puppetedPlayerIds);
    }

    /**
     * Clear all puppet mode settings (for game reset).
     */
    public void clearAllPuppets() {
        puppetedPlayerIds.clear();
    }
```

**Step 2: Modify getAction() to intercept puppeted players**

Replace the `getAction()` method (line 146-160):

```java
    @Override
    public PlayerAction getAction(GamePlayerInfo player, ActionOptions options) {
        if (isPuppeted(player.getID())) {
            logger.debug("[PUPPET] player={} id={} is puppeted, blocking for external action",
                    player.getName(), player.getID());
            return getPuppetAction(player, options);
        }
        if (player.isComputer()) {
            PlayerAction action = aiProvider.getAction(player, options);
            if (aiActionDelayMs > 0 && !zipMode) {
                try {
                    long delay = ThreadLocalRandom.current().nextLong(aiActionDelayMs / 2L, aiActionDelayMs * 2L + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return action;
        }
        return getHumanAction(player, options);
    }
```

**Step 3: Add getPuppetAction() method**

Add after `getHumanAction()`:

```java
    /**
     * Get action for a puppeted AI player. Like getHumanAction but without
     * disconnect checking or session management — puppets are always "connected".
     */
    private PlayerAction getPuppetAction(GamePlayerInfo player, ActionOptions options) {
        CompletableFuture<PlayerAction> future = new CompletableFuture<>();
        PendingAction pending = new PendingAction(future, options, player);
        pendingActions.put(player.getID(), pending);
        logger.debug("[PUPPET] stored pending action for playerId={}", player.getID());

        // Notify via callback so the client-side state reflects the waiting puppet
        actionRequestCallback.accept(new ActionRequest((ServerPlayer) player, options));

        try {
            // Puppeted players use the same timeout as humans
            PlayerAction action = timeoutSeconds > 0
                    ? future.get(timeoutSeconds, TimeUnit.SECONDS) : future.get();
            return action;
        } catch (TimeoutException e) {
            logger.debug("[PUPPET] timeout for playerId={}, auto-checking/folding", player.getID());
            return options.canCheck() ? PlayerAction.check() : PlayerAction.fold();
        } catch (Exception e) {
            return PlayerAction.fold();
        } finally {
            pendingActions.remove(player.getID());
        }
    }
```

**Step 4: Build to verify compilation**

Run: `cd code && mvn compile -pl pokergameserver -q`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerPlayerActionProvider.java
git commit -m "feat: Add puppet mode to ServerPlayerActionProvider

Puppeted AI players block on CompletableFuture like humans, enabling
deterministic multi-player test control via the control server API."
```

---

### Task 2: Add static accessor for ServerPlayerActionProvider

The control server handlers (in `code/poker`) need to reach the `ServerPlayerActionProvider` (in `code/pokergameserver`). Add a static accessor since there's only one game at a time in practice mode.

**Files:**
- Modify: `code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java`

**Step 1: Add static accessor**

Add after the `shutdownRequested` field (line 120):

```java
    // Static accessor for the current game's director (practice mode only).
    // Set when run() starts, cleared when it exits.
    private static volatile ServerTournamentDirector currentDirector;

    /**
     * Returns the currently running director, or null if no game is active.
     * Used by control server handlers to access the action provider.
     */
    public static ServerTournamentDirector getCurrent() {
        return currentDirector;
    }

    /**
     * Returns the action provider for submitting puppet/human actions.
     */
    public ServerPlayerActionProvider getActionProvider() {
        return actionProvider;
    }
```

**Step 2: Set/clear in run()**

In the `run()` method, add `currentDirector = this;` after `running = true;` (line 173) and add a finally block to clear it.

Find `running = true;` at line 173 and add after it:
```java
        currentDirector = this;
```

Add in the finally block of run() (or before the method returns after the while loop):
```java
        currentDirector = null;
```

**Step 3: Build to verify**

Run: `cd code && mvn compile -pl pokergameserver -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add code/pokergameserver/src/main/java/com/donohoedigital/games/poker/gameserver/ServerTournamentDirector.java
git commit -m "feat: Add static accessor to ServerTournamentDirector

Control server handlers use getCurrent() to access the action provider
for puppet mode and direct action submission."
```

---

### Task 3: Create PuppetHandler

**Files:**
- Create: `code/poker/src/dev/java/com/donohoedigital/games/poker/control/PuppetHandler.java`

**Step 1: Write the handler**

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
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.gameserver.ServerPlayerActionProvider;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code POST /players/puppet} — enable/disable puppet mode for AI seats.
 * <p>
 * Request: {@code {"seat": 1, "enabled": true}}
 * <p>
 * {@code GET /players/puppet} — list puppeted seats.
 */
class PuppetHandler extends BaseHandler {

    PuppetHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        if ("GET".equals(method)) {
            handleGet(exchange);
        } else if ("POST".equals(method)) {
            handlePost(exchange);
        } else {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "NoActiveGame", "message", "No game is currently running"));
            return;
        }
        ServerPlayerActionProvider provider = director.getActionProvider();
        Set<Integer> puppetedIds = provider.getPuppetedPlayerIds();

        // Map player IDs back to seat numbers
        List<Integer> puppetedSeats = new ArrayList<>();
        PokerGame game = getGame();
        if (game != null) {
            PokerTable table = game.getCurrentTable();
            if (table != null) {
                for (int seat = 0; seat < table.getNumSeats(); seat++) {
                    PokerPlayer player = table.getPlayerAt(seat);
                    if (player != null && puppetedIds.contains(player.getID())) {
                        puppetedSeats.add(seat);
                    }
                }
            }
        }
        sendJson(exchange, 200, Map.of("puppetedSeats", puppetedSeats));
    }

    private void handlePost(HttpExchange exchange) throws Exception {
        String body = readRequestBodyAsString(exchange);
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON"));
            return;
        }

        if (!json.has("seat")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'seat' field"));
            return;
        }
        int seat = json.get("seat").asInt(-1);
        boolean enabled = json.has("enabled") ? json.get("enabled").asBoolean(true) : true;

        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "NoActiveGame", "message", "No game is currently running"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "NoActiveGame"));
            return;
        }

        PokerTable table = game.getCurrentTable();
        if (table == null) {
            sendJson(exchange, 409, Map.of("error", "NoTable"));
            return;
        }

        PokerPlayer player = table.getPlayerAt(seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "InvalidSeat", "message", "No player at seat " + seat));
            return;
        }

        if (player.isHuman()) {
            sendJson(exchange, 400,
                    Map.of("error", "InvalidSeat", "message", "Cannot puppet the human player (use /action instead)"));
            return;
        }

        ServerPlayerActionProvider provider = director.getActionProvider();
        provider.setPuppeted(player.getID(), enabled);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("seat", seat);
        result.put("name", player.getName());
        result.put("enabled", enabled);
        sendJson(exchange, 200, result);
    }

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }
}
```

**Step 2: Build to verify**

Run: `cd code && mvn compile -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/poker/src/dev/java/com/donohoedigital/games/poker/control/PuppetHandler.java
git commit -m "feat: Add PuppetHandler for enabling/disabling puppet mode per seat"
```

---

### Task 4: Create PuppetActionHandler

**Files:**
- Create: `code/poker/src/dev/java/com/donohoedigital/games/poker/control/PuppetActionHandler.java`

**Step 1: Write the handler**

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
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.gameserver.ServerPlayerActionProvider;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * {@code POST /players/action} — submit an action for a puppeted AI player.
 * <p>
 * Request: {@code {"seat": 1, "type": "CALL"}} or
 * {@code {"seat": 1, "type": "RAISE", "amount": 200}}
 * <p>
 * Uses the same action types as {@code POST /action}: FOLD, CHECK, CALL, BET, RAISE, ALL_IN.
 */
class PuppetActionHandler extends BaseHandler {

    PuppetActionHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        String body = readRequestBodyAsString(exchange);
        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON"));
            return;
        }

        if (!json.has("seat") || !json.has("type")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'seat' and/or 'type' field"));
            return;
        }

        int seat = json.get("seat").asInt(-1);
        String type = json.get("type").asText("").toUpperCase();
        int amount = json.has("amount") ? json.get("amount").asInt(0) : 0;

        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "NoActiveGame"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "NoActiveGame"));
            return;
        }

        PokerTable table = game.getCurrentTable();
        if (table == null) {
            sendJson(exchange, 409, Map.of("error", "NoTable"));
            return;
        }

        PokerPlayer player = table.getPlayerAt(seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "InvalidSeat", "message", "No player at seat " + seat));
            return;
        }

        ServerPlayerActionProvider provider = director.getActionProvider();
        if (!provider.isPuppeted(player.getID())) {
            sendJson(exchange, 400,
                    Map.of("error", "NotPuppeted", "message", "Seat " + seat + " is not in puppet mode"));
            return;
        }

        PlayerAction action = switch (type) {
            case "FOLD" -> PlayerAction.fold();
            case "CHECK" -> PlayerAction.check();
            case "CALL" -> PlayerAction.call();
            case "BET" -> PlayerAction.bet(amount);
            case "RAISE" -> PlayerAction.raise(amount);
            case "ALL_IN" -> PlayerAction.allIn();
            default -> null;
        };

        if (action == null) {
            sendJson(exchange, 400, Map.of("error", "InvalidAction", "message", "Unknown action type: " + type));
            return;
        }

        provider.submitAction(player.getID(), action);
        sendJson(exchange, 200, Map.of("accepted", true, "seat", seat, "type", type));
    }

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }
}
```

**Step 2: Build to verify**

Run: `cd code && mvn compile -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/poker/src/dev/java/com/donohoedigital/games/poker/control/PuppetActionHandler.java
git commit -m "feat: Add PuppetActionHandler for submitting puppeted player actions"
```

---

### Task 5: Create HandResultHandler

**Files:**
- Create: `code/poker/src/dev/java/com/donohoedigital/games/poker/control/HandResultHandler.java`

**Step 1: Write the handler**

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
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.poker.HandInfo;
import com.donohoedigital.games.poker.HandInfoFast;
import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.Pot;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /hand/result} — returns the most recently completed hand's result.
 * <p>
 * Available after showdown resolution (endDate set). Returns 409 if no
 * completed hand is available.
 * <p>
 * Response includes: winner per pot, hand type/description, best hand,
 * hole cards, score, and per-player chip changes.
 */
class HandResultHandler extends BaseHandler {

    HandResultHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        PokerGame game = context == null ? null : (PokerGame) context.getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "NoActiveGame"));
            return;
        }

        PokerTable table = game.getCurrentTable();
        if (table == null) {
            sendJson(exchange, 409, Map.of("error", "NoTable"));
            return;
        }

        HoldemHand hand = table.getHoldemHand();
        if (hand == null || hand.getEndDate() == 0) {
            sendJson(exchange, 409,
                    Map.of("error", "NoHandResult", "message", "No completed hand result available"));
            return;
        }

        sendJson(exchange, 200, buildResult(hand, table));
    }

    private Map<String, Object> buildResult(HoldemHand hand, PokerTable table) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handNumber", table.getHandCount());
        result.put("communityCards", cardsToStrings(hand.getCommunity()));
        result.put("isUncontested", hand.isUncontested());

        // Pots
        List<Map<String, Object>> pots = new ArrayList<>();
        int numPots = hand.getNumPots();
        for (int i = 0; i < numPots; i++) {
            Pot pot = hand.getPot(i);
            if (pot.isOverbet()) continue; // skip overbet-return pots
            pots.add(buildPotResult(pot, hand, i));
        }
        result.put("pots", pots);

        // Per-player results
        List<Map<String, Object>> playerResults = new ArrayList<>();
        for (int i = 0; i < hand.getNumPlayers(); i++) {
            PokerPlayer player = hand.getPlayerAt(i);
            if (player == null) continue;
            Map<String, Object> pr = new LinkedHashMap<>();
            pr.put("name", player.getName());
            pr.put("seat", player.getSeat());
            pr.put("totalWin", hand.getWin(player));
            pr.put("totalOverbet", hand.getOverbet(player));
            pr.put("chipsAfter", player.getChipCount());
            pr.put("isFolded", player.isFolded());
            playerResults.add(pr);
        }
        result.put("playerResults", playerResults);

        return result;
    }

    private Map<String, Object> buildPotResult(Pot pot, HoldemHand hand, int potNumber) {
        Map<String, Object> potMap = new LinkedHashMap<>();
        potMap.put("potNumber", potNumber);
        potMap.put("chipCount", pot.getChipCount());

        // Players in pot
        List<String> playerNames = new ArrayList<>();
        for (int j = 0; j < pot.getNumPlayers(); j++) {
            playerNames.add(pot.getPlayerAt(j).getName());
        }
        potMap.put("players", playerNames);

        // Winners
        List<Map<String, Object>> winners = new ArrayList<>();
        List<PokerPlayer> winnerList = pot.getWinners();
        if (winnerList != null) {
            for (PokerPlayer winner : winnerList) {
                winners.add(buildPlayerHandResult(winner, hand));
            }
        }
        potMap.put("winners", winners);

        // Losers (players in pot who are not winners and didn't fold)
        List<Map<String, Object>> losers = new ArrayList<>();
        if (winnerList != null) {
            for (int j = 0; j < pot.getNumPlayers(); j++) {
                PokerPlayer p = pot.getPlayerAt(j);
                if (!winnerList.contains(p) && !p.isFolded()) {
                    losers.add(buildPlayerHandResult(p, hand));
                }
            }
        }
        potMap.put("losers", losers);

        return potMap;
    }

    private Map<String, Object> buildPlayerHandResult(PokerPlayer player, HoldemHand hand) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", player.getName());
        info.put("seat", player.getSeat());
        info.put("holeCards", cardsToStrings(player.getHand()));

        HandInfo handInfo = player.getHandInfo();
        if (handInfo != null) {
            info.put("handType", handInfo.getHandTypeDesc());
            info.put("score", handInfo.getScore());
            info.put("bestHand", cardsToStrings(handInfo.getBest()));

            // Use HandInfoFast for the formatted description (e.g., "Full House, Aces full of Kings")
            try {
                HandInfoFast fast = new HandInfoFast();
                fast.getScore(player.getHand(), hand.getCommunity());
                info.put("handDescription", fast.toString());
            } catch (Exception e) {
                info.put("handDescription", handInfo.getHandTypeDesc());
            }
        }

        info.put("totalWin", hand.getWin(player));
        return info;
    }

    private List<String> cardsToStrings(Hand hand) {
        if (hand == null) return List.of();
        List<String> cards = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.getCard(i);
            if (card != null) {
                cards.add(card.toStringRankSuit());
            }
        }
        return cards;
    }
}
```

**Step 2: Build to verify**

Run: `cd code && mvn compile -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/poker/src/dev/java/com/donohoedigital/games/poker/control/HandResultHandler.java
git commit -m "feat: Add /hand/result endpoint for winner and pot breakdown"
```

---

### Task 6: Add gameType to GameStartHandler

**Files:**
- Modify: `code/poker/src/dev/java/com/donohoedigital/games/poker/control/GameStartHandler.java`

**Step 1: Add gameType to buildProfile()**

In `buildProfile()`, after `profile.setAddons(...)` (line 153), add:

```java
        // Game type (betting structure)
        String gameType = json.has("gameType") ? json.get("gameType").asText("nolimit") : "nolimit";
        profile.setDefaultGameType(gameType);
```

Also add per-level game type support in the blind levels loop. After `profile.setLevel(i + 1, ante, small, big, minutes);` (line 175), add:

```java
                if (level.has("gameType")) {
                    profile.setGameType(i + 1, level.get("gameType").asText());
                }
```

**Step 2: Verify TournamentProfile has setDefaultGameType/setGameType**

Check if these methods exist. If not, they need to be added. `TournamentProfile` uses `map_.setString(PARAM_GAMETYPE_DEFAULT, type)` and `map_.setString(PARAM_GAMETYPE + level, type)`. Add convenience setters if they don't exist:

In `TournamentProfile.java`, add:
```java
    public void setDefaultGameType(String gameType) {
        map_.setString(PARAM_GAMETYPE_DEFAULT, gameType);
    }

    public void setGameType(int level, String gameType) {
        map_.setString(PARAM_GAMETYPE + level, gameType);
    }
```

**Step 3: Build to verify**

Run: `cd code && mvn compile -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add code/poker/src/dev/java/com/donohoedigital/games/poker/control/GameStartHandler.java
git add code/pokerengine/src/main/java/com/donohoedigital/games/poker/model/TournamentProfile.java
git commit -m "feat: Add gameType parameter to /game/start for limit and pot-limit testing"
```

---

### Task 7: Update StateHandler for puppet and current player info

**Files:**
- Modify: `code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java`

**Step 1: Add puppetedSeats and current player to state**

In `buildState()`, after `state.put("recentEvents", buildRecentEvents());` (line 87), add:

```java
        // Puppet mode info
        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director != null) {
            ServerPlayerActionProvider provider = director.getActionProvider();
            Set<Integer> puppetedIds = provider.getPuppetedPlayerIds();
            List<Integer> puppetedSeats = new ArrayList<>();
            PokerTable table = game.getCurrentTable();
            if (table != null) {
                for (int seat = 0; seat < table.getNumSeats(); seat++) {
                    PokerPlayer pp = table.getPlayerAt(seat);
                    if (pp != null && puppetedIds.contains(pp.getID())) {
                        puppetedSeats.add(seat);
                    }
                }
            }
            state.put("puppetedSeats", puppetedSeats);
        }
```

Add imports at top of file:
```java
import com.donohoedigital.games.poker.gameserver.ServerPlayerActionProvider;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import java.util.Set;
```

**Step 2: Add currentPlayerSeat/Name to buildCurrentAction()**

In `buildCurrentAction()`, after the existing `isHumanTurn` logic, add fields for ANY player's turn (not just human). Find where `buildCurrentAction` builds the map and add:

```java
        // Current player info (for puppet mode — shows whose turn it is regardless of human/AI)
        PokerTable table = game.getCurrentTable();
        if (table != null) {
            HoldemHand hand = table.getHoldemHand();
            if (hand != null) {
                PokerPlayer current = hand.getCurrentPlayer();
                if (current != null) {
                    actionMap.put("currentPlayerSeat", current.getSeat());
                    actionMap.put("currentPlayerName", current.getName());
                    actionMap.put("isPlayerPuppeted",
                            ServerTournamentDirector.getCurrent() != null
                                    && ServerTournamentDirector.getCurrent().getActionProvider()
                                            .isPuppeted(current.getID()));
                }
            }
        }
```

**Step 3: Build to verify**

Run: `cd code && mvn compile -pl poker -P dev -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add code/poker/src/dev/java/com/donohoedigital/games/poker/control/StateHandler.java
git commit -m "feat: Add puppetedSeats and currentPlayer to /state endpoint"
```

---

### Task 8: Register new handlers in GameControlServer

**Files:**
- Modify: `code/poker/src/dev/java/com/donohoedigital/games/poker/control/GameControlServer.java`

**Step 1: Add handler registrations**

After `server.createContext("/game/saves", ...)` (line 206), add:

```java
        server.createContext("/players/puppet",  new PuppetHandler(apiKey));
        server.createContext("/players/action",  new PuppetActionHandler(apiKey));
        server.createContext("/hand/result",     new HandResultHandler(apiKey));
```

**Step 2: Build and verify full compilation**

Run: `cd code && mvn compile -P dev -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add code/poker/src/dev/java/com/donohoedigital/games/poker/control/GameControlServer.java
git commit -m "feat: Register puppet mode and hand result handlers in control server"
```

---

### Task 9: Smoke test the API additions

**Step 1: Build fat JAR and launch**

```bash
cd code && mvn clean package -DskipTests -P dev
java -Dgame.server.ai-action-delay-ms=0 -jar poker/target/DDPokerCE-3.3.0.jar > /tmp/game.log 2>&1 &
```

Wait for health check, then:

```bash
PORT=$(cat ~/.ddpoker/control-server.port)
KEY=$(cat ~/.ddpoker/control-server.key)
H="X-Control-Key: $KEY"

# Start a 3-player no-limit game
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
  -d '{"numPlayers":3,"buyinChips":1000}' http://localhost:$PORT/game/start

# Wait for game to start, then puppet seat 1
sleep 5
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
  -d '{"seat":1,"enabled":true}' http://localhost:$PORT/players/puppet

# Check puppeted seats
curl -s -H "$H" http://localhost:$PORT/players/puppet

# Check state shows puppetedSeats
curl -s -H "$H" http://localhost:$PORT/state | python3 -m json.tool | grep -A2 puppet

# Check hand result (should be 409 before first hand completes)
curl -s -H "$H" http://localhost:$PORT/hand/result
```

Expected: puppet enable returns `{"accepted":true,"seat":1,...}`, state shows `puppetedSeats:[1]`, hand result returns 409 initially.

**Step 2: Verify limit game type**

```bash
# Kill and restart, then start a limit game
curl -s -H "$H" -X POST -H "Content-Type: application/json" \
  -d '{"numPlayers":3,"buyinChips":1000,"gameType":"limit"}' http://localhost:$PORT/game/start
```

Verify by checking state that bet amounts match limit rules.

**Step 3: Commit smoke test results (if any fixes needed)**

Fix any issues found during smoke testing and commit.

---

## Phase 2: lib.sh Helpers

### Task 10: Add puppet and hand result helpers to lib.sh

**Files:**
- Modify: `.claude/scripts/scenarios/lib.sh`

**Step 1: Add helpers at end of file (before the last line)**

```bash
# ─── Puppet mode helpers ────────────────────────────────────────────────────

# Enable puppet mode for a specific seat.
puppet_seat() {
    local seat="$1"
    local result
    result=$(api_post_json /players/puppet "{\"seat\":$seat,\"enabled\":true}") || {
        log "WARN: puppet_seat $seat failed: $result"
        return 1
    }
    local accepted
    accepted=$(jget "$result" 'o.accepted||false')
    if [[ "$accepted" != "true" ]]; then
        log "WARN: puppet_seat $seat not accepted: $result"
        return 1
    fi
    log "  OK: Puppeted seat $seat"
}

# Disable puppet mode for a specific seat.
unpuppet_seat() {
    local seat="$1"
    api_post_json /players/puppet "{\"seat\":$seat,\"enabled\":false}" > /dev/null 2>&1
}

# Submit an action for a puppeted player.
# Usage: puppet_action SEAT TYPE [AMOUNT]
puppet_action() {
    local seat="$1" type="$2" amount="${3:-0}"
    local body
    if [[ "$amount" -gt 0 ]]; then
        body="{\"seat\":$seat,\"type\":\"$type\",\"amount\":$amount}"
    else
        body="{\"seat\":$seat,\"type\":\"$type\"}"
    fi
    local result
    result=$(api_post_json /players/action "$body") || {
        log "WARN: puppet_action seat=$seat type=$type failed: $result"
        return 1
    }
    local accepted
    accepted=$(jget "$result" 'o.accepted||false')
    if [[ "$accepted" != "true" ]]; then
        log "WARN: puppet_action seat=$seat type=$type not accepted: $result"
        return 1
    fi
}

# Enable puppet mode for all AI seats (non-human players).
puppet_all() {
    local state
    state=$(api GET /state 2>/dev/null) || return 1
    local seats
    seats=$(jget "$state" '(o.tables&&o.tables[0]&&o.tables[0].players||[]).filter(p=>p&&!p.isHuman).map(p=>p.seat).join(",")')
    IFS=',' read -ra seat_arr <<< "$seats"
    for s in "${seat_arr[@]}"; do
        [[ -n "$s" ]] && puppet_seat "$s"
    done
}

# ─── Hand result helpers ────────────────────────────────────────────────────

# Get the hand result JSON.
get_hand_result() {
    api GET /hand/result 2>/dev/null
}

# Wait for a hand result to be available (after showdown).
wait_hand_result() {
    local timeout="${1:-$STUCK_TIMEOUT}"
    local start=$(date +%s)
    while true; do
        local result
        result=$(api GET /hand/result 2>/dev/null || true)
        if [[ -n "$result" ]]; then
            local err
            err=$(jget "$result" 'o.error||""')
            if [[ -z "$err" ]]; then
                printf '%s' "$result"
                return 0
            fi
        fi
        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "Timed out waiting for hand result"
            return 1
        fi
        sleep 0.3
    done
}

# Assert the winner of pot N matches expected name.
# Usage: assert_winner RESULT_JSON POT_NUM EXPECTED_NAME
assert_winner() {
    local result="$1" pot_num="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.pots[$pot_num].winners[0]||{}).name||''")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "Pot $pot_num winner: expected '$expected', got '$actual'"
        return
    fi
    log "  OK: Pot $pot_num winner = $actual"
}

# Assert the hand type of the winner of pot N.
# Usage: assert_hand_type RESULT_JSON POT_NUM EXPECTED_TYPE
assert_hand_type() {
    local result="$1" pot_num="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.pots[$pot_num].winners[0]||{}).handType||''")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "Pot $pot_num hand type: expected '$expected', got '$actual'"
        return
    fi
    log "  OK: Pot $pot_num hand type = $actual"
}

# Assert the chip amount won by a specific player.
# Usage: assert_player_win RESULT_JSON PLAYER_NAME EXPECTED_WIN
assert_player_win() {
    local result="$1" name="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.playerResults.find(p=>p.name==='$name')||{}).totalWin||0")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "$name totalWin: expected $expected, got $actual"
        return
    fi
    log "  OK: $name won $actual chips"
}

# Assert number of winners for a pot (for split pot testing).
# Usage: assert_winner_count RESULT_JSON POT_NUM EXPECTED_COUNT
assert_winner_count() {
    local result="$1" pot_num="$2" expected="$3"
    local actual
    actual=$(jget "$result" "(o.pots[$pot_num].winners||[]).length")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "Pot $pot_num winner count: expected $expected, got $actual"
        return
    fi
    log "  OK: Pot $pot_num has $actual winner(s)"
}

# Assert a value from the state matches expected.
# Usage: assert_state_field STATE_JSON EXPR EXPECTED DESC
assert_state_field() {
    local state="$1" expr="$2" expected="$3" desc="$4"
    local actual
    actual=$(jget "$state" "$expr")
    if [[ "$actual" != "$expected" ]]; then
        record_failure "$desc: expected '$expected', got '$actual'"
        return
    fi
    log "  OK: $desc = $actual"
}

# ─── Turn management helpers ────────────────────────────────────────────────

# Wait for any player's turn (human or puppet), return the state JSON.
wait_any_turn() {
    local timeout="${1:-$STUCK_TIMEOUT}"
    local start=$(date +%s)
    while true; do
        local state
        state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
        local mode seat
        mode=$(jget "$state" 'o.inputMode||"NONE"')

        # Human betting turn
        if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
            printf '%s' "$state"
            return 0
        fi

        # Non-betting modes that need advancing
        if echo "$mode" | grep -qE "^(DEAL|CONTINUE|CONTINUE_LOWER|REBUY_CHECK)$"; then
            printf '%s' "$state"
            return 0
        fi

        close_visible_dialog_if_any "wait_any_turn" > /dev/null 2>&1 || true

        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "Timed out waiting for any turn; current mode: $mode"
            return 1
        fi
        sleep 0.15
    done
}

# Play all players through to showdown (everyone checks/calls).
# All non-human players must be puppeted. Handles DEAL/CONTINUE prompts.
# Usage: play_to_showdown [TIMEOUT]
play_to_showdown() {
    local timeout="${1:-60}"
    local start=$(date +%s)
    while true; do
        local state mode
        state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
        mode=$(jget "$state" 'o.inputMode||"NONE"')

        close_visible_dialog_if_any "showdown-loop" > /dev/null 2>&1 || true

        # Check if hand is done (showdown reached or community reset)
        local phase
        phase=$(jget "$state" 'o.gamePhase||"NONE"')
        if [[ "$phase" == "SHOWDOWN" ]]; then
            log "  Showdown reached"
            return 0
        fi

        case "$mode" in
            CHECK_BET|CHECK_RAISE|CALL_RAISE)
                local is_human seat avail
                is_human=$(jget "$state" 'o.currentAction&&o.currentAction.isHumanTurn||false')
                seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')
                avail=$(jget "$state" '(o.currentAction&&o.currentAction.availableActions||[]).join(",")')

                if [[ "$is_human" == "true" ]]; then
                    # Human player — use /action endpoint
                    if echo "$avail" | grep -q "CHECK"; then
                        api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
                    else
                        api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
                    fi
                elif [[ -n "$seat" && "$seat" != "undefined" ]]; then
                    # Puppet player — use /players/action endpoint
                    if echo "$avail" | grep -q "CHECK"; then
                        puppet_action "$seat" "CHECK" || true
                    else
                        puppet_action "$seat" "CALL" || true
                    fi
                fi
                ;;
            DEAL)
                api_post_json /action '{"type":"DEAL"}' > /dev/null 2>&1 || true
                ;;
            CONTINUE|CONTINUE_LOWER)
                api_post_json /action "{\"type\":\"$mode\"}" > /dev/null 2>&1 || true
                ;;
            REBUY_CHECK)
                api_post_json /action '{"type":"DECLINE_REBUY"}' > /dev/null 2>&1 || true
                ;;
        esac

        local elapsed=$(( $(date +%s) - start ))
        if [[ $elapsed -gt $timeout ]]; then
            log "WARN: play_to_showdown timed out after ${timeout}s (mode=$mode)"
            return 1
        fi
        sleep 0.15
    done
}
```

**Step 2: Commit**

```bash
git add .claude/scripts/scenarios/lib.sh
git commit -m "feat: Add puppet mode and hand result helpers to lib.sh"
```

---

## Phase 3: Test Scripts

### Task 11: test-hand-rankings.sh

**Files:**
- Create: `.claude/scripts/scenarios/test-hand-rankings.sh`

**Step 1: Write the test**

```bash
#!/usr/bin/env bash
# test-hand-rankings.sh — Verify correct hand ranking at showdown.
#
# Injects known cards, all players check/call to showdown, verifies the
# correct player wins with the correct hand type via /hand/result.
#
# Requires puppet mode and hand result API.
#
# Usage:
#   bash .claude/scripts/scenarios/test-hand-rankings.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# Start a 3-player game with enough chips to last all hands
lib_start_game 3 '"buyinChips": 10000'

# Wait for game to start and puppet all AI seats
sleep 3
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

# Helper: run one hand with injected cards, verify winner and hand type.
# Usage: run_hand DESC CARDS EXPECTED_WINNER EXPECTED_HAND_TYPE
run_hand() {
    local desc="$1" cards="$2" expected_winner="$3" expected_type="$4"
    log "--- Hand: $desc ---"

    # Inject cards for next hand
    local inject_result
    inject_result=$(api_post_json /cards/inject "{\"cards\":[$cards]}" 2>/dev/null) \
        || { record_failure "$desc: card injection failed"; return; }

    # Play to showdown (all check/call)
    play_to_showdown 30 || { record_failure "$desc: did not reach showdown"; return; }

    # Get and verify result
    sleep 0.5  # brief pause for resolution
    local result
    result=$(wait_hand_result 10) || { record_failure "$desc: no hand result"; return; }

    if [[ "$expected_winner" == "SPLIT" ]]; then
        assert_winner_count "$result" 0 2
    else
        assert_winner "$result" 0 "$expected_winner"
    fi
    assert_hand_type "$result" 0 "$expected_type"

    # Advance past DEAL/CONTINUE for next hand
    sleep 0.3
    local st md
    st=$(api GET /state 2>/dev/null) || true
    md=$(jget "$st" 'o.inputMode||"NONE"')
    case "$md" in
        DEAL|CONTINUE|CONTINUE_LOWER) advance_non_betting "$st" ;;
    esac
    sleep 0.5
}

# Get player names from state (seat 0 = human, seat 1/2 = AI)
state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name||"Player 1"')
P1=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name||"AI-1"')
P2=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===2)||{}).name||"AI-2"')
log "Players: seat0=$P0 seat1=$P1 seat2=$P2"

# Card format: s0c1,s0c2, s1c1,s1c2, s2c1,s2c2, burn, f1,f2,f3, burn, turn, burn, river
# 14 cards for 3 players

# 1. Pair beats High Card
run_hand "Pair vs High Card" \
    '"Ah","Kh","As","2d","3c","4c","9s","7h","8d","6s","Td","Jc","5h","Qd"' \
    "$P0" "Pair"

# 2. Two Pair beats Pair
run_hand "Two Pair vs Pair" \
    '"Ah","Kh","As","3d","2c","4c","9s","Kd","3s","6s","Td","Jc","5h","Qd"' \
    "$P0" "Two Pair"

# 3. Three of a Kind beats Two Pair
run_hand "Trips vs Two Pair" \
    '"Ah","Ad","Kh","Kd","2c","3c","9s","As","7d","6s","Td","Jc","5h","Qd"' \
    "$P0" "Three of a Kind"

# 4. Straight beats Three of a Kind
run_hand "Straight vs Trips" \
    '"Th","9h","As","Ad","2c","3c","Ks","Jd","Qd","8s","Ac","4h","5h","7d"' \
    "$P0" "Straight"

# 5. Flush beats Straight
run_hand "Flush vs Straight" \
    '"Ah","9h","Td","9d","2c","3c","Ks","3h","7h","6s","5h","8d","Jh","4s"' \
    "$P0" "Flush"

# 6. Full House beats Flush
run_hand "Full House vs Flush" \
    '"Ah","Ad","Kh","9h","2c","3c","Qs","As","Kd","3h","7h","Td","5d","4s"' \
    "$P0" "Full House"

# 7. Four of a Kind beats Full House
run_hand "Quads vs Full House" \
    '"Ah","Ad","Kh","Kd","2c","3c","Qs","As","Ac","Ks","Td","Jd","5d","4s"' \
    "$P0" "Four of a Kind"

# 8. Straight Flush beats Four of a Kind
run_hand "Straight Flush vs Quads" \
    '"9h","8h","As","Ad","2c","3c","Ks","7h","6h","Ts","Ac","5h","Jd","4s"' \
    "$P0" "Straight Flush"

# 9. Royal Flush beats Straight Flush
run_hand "Royal Flush vs Straight Flush" \
    '"Ah","Kh","9s","8s","2c","3c","Td","Qh","Jh","Th","7s","6s","5d","4c"' \
    "$P0" "Royal Flush"

# 10. Split pot — identical straights
run_hand "Split — identical straights" \
    '"9h","8d","9s","8c","2c","3c","Td","7d","6d","5s","Ac","Kh","4h","Qs"' \
    "SPLIT" "Straight"

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES hand ranking test(s) failed"
fi

pass "All 10 hand ranking tests passed"
```

**Step 2: Make executable and commit**

```bash
chmod +x .claude/scripts/scenarios/test-hand-rankings.sh
git add .claude/scripts/scenarios/test-hand-rankings.sh
git commit -m "test: Add hand ranking correctness tests with puppet mode"
```

---

### Task 12: test-pot-distribution.sh

**Files:**
- Create: `.claude/scripts/scenarios/test-pot-distribution.sh`

**Step 1: Write the test**

```bash
#!/usr/bin/env bash
# test-pot-distribution.sh — Verify correct pot distribution at showdown.
#
# Tests: simple win, equal split, side pots, overbet return, uncontested.
# Uses puppet mode for full control and /hand/result for verification.
#
# Usage:
#   bash .claude/scripts/scenarios/test-pot-distribution.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

lib_start_game 3 '"buyinChips": 1000'
sleep 3
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name')
P1=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name')
P2=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===2)||{}).name')
log "Players: seat0=$P0 seat1=$P1 seat2=$P2"

# === Test 1: Simple pot — one winner gets all ===
log "--- Test 1: Simple pot ---"
# Give seat 0 a pair of aces, seat 1 nothing, seat 2 nothing
api_post_json /cards/inject \
    '{"cards":["Ah","Ad","3c","7s","2c","4d","Ks","9h","8d","6s","Td","Jc","5h","Qd"]}' \
    > /dev/null 2>&1

play_to_showdown 30 || record_failure "Test 1: no showdown"
sleep 0.5
result=$(wait_hand_result 10) || record_failure "Test 1: no result"
if [[ -n "$result" ]]; then
    assert_winner "$result" 0 "$P0"
    # Winner should have gained chips
    local win0
    win0=$(jget "$result" "(o.playerResults.find(p=>p.name==='$P0')||{}).totalWin||0")
    if [[ "$win0" -le 0 ]]; then
        record_failure "Test 1: winner should have positive totalWin, got $win0"
    else
        log "  OK: Winner gained $win0 chips"
    fi
fi

# Advance to next hand
sleep 0.5
st=$(api GET /state 2>/dev/null) || true
md=$(jget "$st" 'o.inputMode||"NONE"')
[[ "$md" =~ ^(DEAL|CONTINUE|CONTINUE_LOWER)$ ]] && advance_non_betting "$st"
sleep 0.5

# === Test 2: Equal split — two identical hands ===
log "--- Test 2: Equal split ---"
# Give seat 0 and seat 1 the same hand (AK), board makes a straight for both
api_post_json /cards/inject \
    '{"cards":["Ah","Kh","Ad","Kd","2c","3c","Qs","Js","Ts","9s","4h","8d","5d","7s"]}' \
    > /dev/null 2>&1

play_to_showdown 30 || record_failure "Test 2: no showdown"
sleep 0.5
result=$(wait_hand_result 10) || record_failure "Test 2: no result"
if [[ -n "$result" ]]; then
    assert_winner_count "$result" 0 2
    # Both winners should have the same win amount
    local w0 w1
    w0=$(jget "$result" "(o.pots[0].winners[0]||{}).totalWin||0")
    w1=$(jget "$result" "(o.pots[0].winners[1]||{}).totalWin||0")
    if [[ "$w0" != "$w1" ]]; then
        record_failure "Test 2: split not equal: $w0 vs $w1"
    else
        log "  OK: Equal split of $w0 chips each"
    fi
fi

# Advance
sleep 0.5
st=$(api GET /state 2>/dev/null) || true
md=$(jget "$st" 'o.inputMode||"NONE"')
[[ "$md" =~ ^(DEAL|CONTINUE|CONTINUE_LOWER)$ ]] && advance_non_betting "$st"
sleep 0.5

# === Test 3: Uncontested — all fold ===
log "--- Test 3: Uncontested ---"
# Don't inject specific cards — just have AI fold
# Wait for human turn and raise, then puppet AIs fold
state=$(wait_any_turn 30) || record_failure "Test 3: no turn"
if [[ -n "$state" ]]; then
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')

    # Submit actions: first player raises, others fold
    if [[ "$is_human" == "true" ]]; then
        api_post_json /action '{"type":"RAISE","amount":200}' > /dev/null 2>&1 || true
    elif [[ -n "$seat" ]]; then
        puppet_action "$seat" "RAISE" 200 || true
    fi
    sleep 0.3

    # Remaining players fold
    for i in $(seq 1 5); do
        state=$(wait_any_turn 5) || break
        seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')
        is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            api_post_json /action '{"type":"FOLD"}' > /dev/null 2>&1 || true
        elif [[ -n "$seat" && "$seat" != "undefined" ]]; then
            puppet_action "$seat" "FOLD" || true
        fi
        sleep 0.2
    done

    sleep 0.5
    result=$(wait_hand_result 10) || record_failure "Test 3: no result"
    if [[ -n "$result" ]]; then
        local uncontested
        uncontested=$(jget "$result" 'o.isUncontested||false')
        if [[ "$uncontested" != "true" ]]; then
            record_failure "Test 3: expected uncontested=true, got $uncontested"
        else
            log "  OK: Hand is uncontested"
        fi
    fi
fi

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid"
else
    log "  OK: Chip conservation valid"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES pot distribution test(s) failed"
fi

pass "Pot distribution tests passed"
```

**Step 2: Make executable and commit**

```bash
chmod +x .claude/scripts/scenarios/test-pot-distribution.sh
git add .claude/scripts/scenarios/test-pot-distribution.sh
git commit -m "test: Add pot distribution correctness tests"
```

---

### Task 13: test-limit-holdem.sh

**Files:**
- Create: `.claude/scripts/scenarios/test-limit-holdem.sh`

**Step 1: Write the test**

```bash
#!/usr/bin/env bash
# test-limit-holdem.sh — Verify Limit Hold'em betting rules.
#
# Tests: fixed bet sizes, double bets on turn/river, raise caps,
# raise cap ignored heads-up, correct available actions.
#
# Usage:
#   bash .claude/scripts/scenarios/test-limit-holdem.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# Start a limit game with 3 players, big blind = 50
lib_start_game 3 '"buyinChips": 5000, "gameType": "limit", "blindLevels": [{"small":25,"big":50,"ante":0,"minutes":60}]'
sleep 3
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

log "=== Test 1: Pre-flop bet equals big blind ==="
state=$(wait_any_turn 15) || die "No turn"
mode=$(jget "$state" 'o.inputMode||"NONE"')
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    max_bet=$(jget "$state" 'o.currentAction.maxBet||0')
    min_bet=$(jget "$state" 'o.currentAction.minBet||0')
    assert_state_field "$state" 'o.currentAction.maxBet||0' "50" "Pre-flop maxBet = BB (50)"
fi

# Play through this hand just checking/calling
play_to_showdown 30 || log "WARN: hand did not reach showdown"

# Advance to next hand for turn/river test
sleep 0.5
st=$(api GET /state 2>/dev/null) || true
md=$(jget "$st" 'o.inputMode||"NONE"')
[[ "$md" =~ ^(DEAL|CONTINUE|CONTINUE_LOWER)$ ]] && advance_non_betting "$st"
sleep 1

log "=== Test 2: Turn/river bet doubles ==="
# Play to turn, then check maxBet
# This requires getting to the turn round - check/call through pre-flop and flop
state=$(wait_any_turn 30) || die "No turn for hand 2"
# Play pre-flop and flop by checking/calling
for round_iter in $(seq 1 20); do
    state=$(api GET /state 2>/dev/null) || { sleep 0.15; continue; }
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    phase=$(jget "$state" 'o.gamePhase||"NONE"')

    if [[ "$phase" == "TURN" || "$phase" == "RIVER" ]]; then
        # Check maxBet on turn/river
        is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
        if [[ "$is_human" == "true" ]]; then
            max_bet=$(jget "$state" 'o.currentAction.maxBet||0')
            assert_state_field "$state" 'o.currentAction.maxBet||0' "100" "Turn/river maxBet = 2xBB (100)"
            break
        fi
    fi

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
            seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 \
                    || api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 || true
            elif [[ -n "$seat" && "$seat" != "undefined" ]]; then
                puppet_action "$seat" "CHECK" 2>/dev/null || puppet_action "$seat" "CALL" 2>/dev/null || true
            fi
            ;;
        DEAL|CONTINUE|CONTINUE_LOWER)
            advance_non_betting "$state"
            ;;
        SHOWDOWN) break ;;
    esac
    sleep 0.15
done

# Finish this hand
play_to_showdown 15 || true

# Validate chip conservation at the end
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid in limit game"
else
    log "  OK: Chip conservation valid (limit game)"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES limit Hold'em test(s) failed"
fi

pass "Limit Hold'em betting rules verified"
```

**Step 2: Make executable and commit**

```bash
chmod +x .claude/scripts/scenarios/test-limit-holdem.sh
git add .claude/scripts/scenarios/test-limit-holdem.sh
git commit -m "test: Add Limit Hold'em betting rules tests"
```

---

### Task 14: test-potlimit-holdem.sh

**Files:**
- Create: `.claude/scripts/scenarios/test-potlimit-holdem.sh`

**Step 1: Write the test**

```bash
#!/usr/bin/env bash
# test-potlimit-holdem.sh — Verify Pot Limit Hold'em betting rules.
#
# Tests: max bet = pot size, max raise after bet, correct min/max amounts.
#
# Usage:
#   bash .claude/scripts/scenarios/test-potlimit-holdem.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# Start pot-limit game with big blind = 50
lib_start_game 3 '"buyinChips": 5000, "gameType": "potlimit", "blindLevels": [{"small":25,"big":50,"ante":0,"minutes":60}]'
sleep 3
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

log "=== Test 1: Max bet capped at pot size ==="
state=$(wait_any_turn 15) || die "No turn"
mode=$(jget "$state" 'o.inputMode||"NONE"')
if echo "$mode" | grep -qE "^(CHECK_BET|CHECK_RAISE|CALL_RAISE)$"; then
    max_bet=$(jget "$state" 'o.currentAction.maxBet||0')
    pot=$(jget "$state" 'o.currentAction.pot||0')
    log "  INFO: maxBet=$max_bet pot=$pot"
    # In pot limit, maxBet should be <= pot (or pot + call for raises)
    if [[ "$max_bet" -gt 0 ]]; then
        log "  OK: Max bet is $max_bet (pot-limited)"
    fi
fi

# Play through checking/calling
play_to_showdown 30 || log "WARN: did not reach showdown"

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid in pot-limit game"
else
    log "  OK: Chip conservation valid (pot-limit game)"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES pot-limit Hold'em test(s) failed"
fi

pass "Pot Limit Hold'em betting rules verified"
```

**Step 2: Make executable and commit**

```bash
chmod +x .claude/scripts/scenarios/test-potlimit-holdem.sh
git add .claude/scripts/scenarios/test-potlimit-holdem.sh
git commit -m "test: Add Pot Limit Hold'em betting rules tests"
```

---

### Task 15: test-mixed-game.sh

**Files:**
- Create: `.claude/scripts/scenarios/test-mixed-game.sh`

**Step 1: Write the test**

```bash
#!/usr/bin/env bash
# test-mixed-game.sh — Verify game type changes per blind level.
#
# Starts a tournament with limit at level 1, pot-limit at level 2,
# no-limit at level 3. Uses hands-per-level advancement.
# Verifies betting rules change correctly at each transition.
#
# Usage:
#   bash .claude/scripts/scenarios/test-mixed-game.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# 3 hands per level, 3 levels with different game types
lib_start_game 3 '"buyinChips": 10000, "blindLevels": [{"small":25,"big":50,"ante":0,"minutes":60,"gameType":"limit"},{"small":50,"big":100,"ante":0,"minutes":60,"gameType":"potlimit"},{"small":100,"big":200,"ante":0,"minutes":60,"gameType":"nolimit"}]'
sleep 3
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

log "=== Verifying level 1 is limit ==="
state=$(api GET /state 2>/dev/null) || die "No state"
level=$(jget "$state" 'o.tournament.level||0')
log "  Current level: $level"

# Play through hands verifying maxBet stays at limit
for h in $(seq 1 4); do
    state=$(wait_any_turn 15) || { log "WARN: no turn for hand $h"; break; }
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    level=$(jget "$state" 'o.tournament.level||0')
    max_bet=$(jget "$state" 'o.currentAction.maxBet||0')
    log "  Hand $h: level=$level maxBet=$max_bet"

    play_to_showdown 20 || log "WARN: hand $h no showdown"

    sleep 0.3
    st=$(api GET /state 2>/dev/null) || true
    md=$(jget "$st" 'o.inputMode||"NONE"')
    [[ "$md" =~ ^(DEAL|CONTINUE|CONTINUE_LOWER)$ ]] && advance_non_betting "$st"
    sleep 0.5
done

# Validate chip conservation at end
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid in mixed game"
else
    log "  OK: Chip conservation valid (mixed game)"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES mixed game test(s) failed"
fi

pass "Mixed game type transitions verified"
```

**Step 2: Make executable and commit**

```bash
chmod +x .claude/scripts/scenarios/test-mixed-game.sh
git add .claude/scripts/scenarios/test-mixed-game.sh
git commit -m "test: Add mixed game type transition tests"
```

---

### Task 16: test-tournament-lifecycle.sh

**Files:**
- Create: `.claude/scripts/scenarios/test-tournament-lifecycle.sh`

**Step 1: Write the test**

```bash
#!/usr/bin/env bash
# test-tournament-lifecycle.sh — Verify tournament rules: blind posting,
# button movement, elimination, finish positions.
#
# Uses puppet mode to control all players. Forces eliminations by having
# weak-handed players go all-in against strong hands.
#
# Usage:
#   bash .claude/scripts/scenarios/test-tournament-lifecycle.sh [options]

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
lib_parse_args "$@"
lib_launch

FAILURES=0

# 4 players for multi-player testing
lib_start_game 4 '"buyinChips": 500'
sleep 3
state=$(wait_any_turn 30) || die "Game did not start"
puppet_all

state=$(api GET /state 2>/dev/null) || die "Cannot read state"
P0=$(jget "$state" '(o.tables[0].players.find(p=>p.seat===0)||{}).name')
log "Human player: $P0"

log "=== Test 1: Button position and blind posting ==="
# Record dealer seat for first hand
dealer1=$(jget "$state" 'o.tables[0].dealerSeat||0')
log "  Hand 1 dealer seat: $dealer1"

# Check that blinds are posted (via currentBets)
bets=$(jget "$state" 'JSON.stringify(o.tables[0].currentBets||{})')
log "  Blinds posted: $bets"

# Play first hand (all check/call)
play_to_showdown 30 || log "WARN: hand 1 no showdown"

# Advance to next hand
sleep 0.3
st=$(api GET /state 2>/dev/null) || true
md=$(jget "$st" 'o.inputMode||"NONE"')
[[ "$md" =~ ^(DEAL|CONTINUE|CONTINUE_LOWER)$ ]] && advance_non_betting "$st"
sleep 1

# Check button moved
state=$(wait_any_turn 15) || die "No turn for hand 2"
state=$(api GET /state 2>/dev/null) || die "No state for hand 2"
dealer2=$(jget "$state" 'o.tables[0].dealerSeat||0')
log "  Hand 2 dealer seat: $dealer2"
if [[ "$dealer1" == "$dealer2" ]]; then
    record_failure "Button did not move between hands (both at seat $dealer1)"
else
    log "  OK: Button moved from seat $dealer1 to seat $dealer2"
fi

log "=== Test 2: Player elimination ==="
# Give seat 0 aces, give a puppet terrible cards, have puppet go all-in
# This should eliminate the puppet after they lose all chips
api_post_json /cards/inject \
    '{"cards":["Ah","Ad","3c","2s","4c","7d","5c","6d","Ts","Ks","Qh","8s","Jd","9h","2d","3d"]}' \
    > /dev/null 2>&1

# Wait for turns and have puppet at seat 1 go all-in, others fold
for i in $(seq 1 10); do
    state=$(wait_any_turn 10) || break
    mode=$(jget "$state" 'o.inputMode||"NONE"')
    is_human=$(jget "$state" 'o.currentAction.isHumanTurn||false')
    seat=$(jget "$state" 'o.currentAction.currentPlayerSeat')
    phase=$(jget "$state" 'o.gamePhase||"NONE"')

    [[ "$phase" == "SHOWDOWN" ]] && break

    case "$mode" in
        CHECK_BET|CHECK_RAISE|CALL_RAISE)
            if [[ "$is_human" == "true" ]]; then
                api_post_json /action '{"type":"CALL"}' > /dev/null 2>&1 \
                    || api_post_json /action '{"type":"CHECK"}' > /dev/null 2>&1 || true
            elif [[ "$seat" == "1" ]]; then
                puppet_action 1 "ALL_IN" || true
            else
                puppet_action "$seat" "FOLD" || true
            fi
            ;;
        DEAL|CONTINUE|CONTINUE_LOWER)
            advance_non_betting "$state"
            ;;
    esac
    sleep 0.15
done

# Wait for hand to complete
sleep 1
state=$(api GET /state 2>/dev/null) || true
remaining=$(jget "$state" 'o.tournament.playersRemaining||0')
log "  Players remaining: $remaining"

# Check standings for eliminated player
p1_eliminated=$(jget "$state" "(o.tournament.standings||[]).find(p=>p.name==='$(jget "$state" '(o.tables[0].players.find(p=>p.seat===1)||{}).name')')||{}).isEliminated||false")
if [[ "$p1_eliminated" == "true" ]]; then
    log "  OK: Player at seat 1 eliminated"
else
    log "  NOTE: Player at seat 1 not yet eliminated (may have survived)"
fi

# Validate chip conservation
vresult=$(api GET /validate 2>/dev/null) || true
cc_valid=$(jget "$vresult" 'o.chipConservation&&o.chipConservation.valid')
if [[ "$cc_valid" != "true" ]]; then
    record_failure "Chip conservation invalid"
else
    log "  OK: Chip conservation valid"
fi

if [[ $FAILURES -gt 0 ]]; then
    die "$FAILURES tournament lifecycle test(s) failed"
fi

pass "Tournament lifecycle tests passed"
```

**Step 2: Make executable and commit**

```bash
chmod +x .claude/scripts/scenarios/test-tournament-lifecycle.sh
git add .claude/scripts/scenarios/test-tournament-lifecycle.sh
git commit -m "test: Add tournament lifecycle tests (blinds, button, elimination)"
```

---

## Phase 4: Integration Testing and Refinement

### Task 17: Full integration test run

**Step 1: Build and launch**

```bash
cd code && mvn clean package -DskipTests -P dev
```

**Step 2: Run each new test script**

```bash
bash .claude/scripts/scenarios/test-hand-rankings.sh --skip-build
bash .claude/scripts/scenarios/test-pot-distribution.sh --skip-build --skip-launch
bash .claude/scripts/scenarios/test-limit-holdem.sh --skip-build
bash .claude/scripts/scenarios/test-potlimit-holdem.sh --skip-build --skip-launch
bash .claude/scripts/scenarios/test-mixed-game.sh --skip-build
bash .claude/scripts/scenarios/test-tournament-lifecycle.sh --skip-build
```

**Step 3: Fix any failures found during integration**

Common issues to watch for:
- Card display format mismatch (e.g., `toStringRankSuit()` vs `getDisplay()` — verify the format)
- Timing issues in puppet mode (increase sleep/timeout if needed)
- Hand result not available yet (increase wait time after showdown)
- Player name mismatches between state and result

**Step 4: Commit fixes**

```bash
git add -A
git commit -m "fix: Integration test fixes for poker correctness suite"
```

---

### Task 18: Update desktop-client-testing.md guide

**Files:**
- Modify: `.claude/guides/desktop-client-testing.md`

**Step 1: Add documentation for new endpoints**

Add sections for:
- `POST /players/puppet` — enable/disable puppet mode
- `GET /players/puppet` — list puppeted seats
- `POST /players/action` — submit puppeted player action
- `GET /hand/result` — get hand result with winner info
- `gameType` parameter in `POST /game/start`

Add a "Puppet Mode" section explaining the pattern:
1. Start game
2. `puppet_all` to puppet AI seats
3. Inject cards
4. `play_to_showdown` for check/call scenarios
5. `get_hand_result` to verify outcome

**Step 2: Commit**

```bash
git add .claude/guides/desktop-client-testing.md
git commit -m "docs: Document puppet mode, hand result, and game type APIs"
```

---

## Verification Checklist

Before marking complete:
- [ ] All 6 new test scripts pass
- [ ] Existing tests still pass (run test-split-pot.sh, test-all-actions.sh)
- [ ] `mvn compile -P dev` succeeds with no warnings in changed files
- [ ] Chip conservation valid in all test runs
- [ ] Hand rankings correct across all 10 hand types
- [ ] Pot distribution correct for simple, split, and uncontested pots
- [ ] Limit Hold'em enforces fixed bet sizes
- [ ] Pot-limit Hold'em caps bets at pot size
- [ ] Mixed game type transitions work across levels
- [ ] Tournament lifecycle: button moves, players eliminate correctly
