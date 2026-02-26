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
import com.donohoedigital.games.engine.Phase;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.dashboard.DashboardAdvisor;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.gameserver.ServerPlayerActionProvider;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.online.GameEventLog;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code GET /state} — returns the full current game state as JSON.
 */
class StateHandler extends BaseHandler {

    StateHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }
        sendJson(exchange, 200, buildState());
    }

    private Map<String, Object> buildState() {
        PokerMain main = PokerMain.getPokerMain();
        GameContext context = main == null ? null : main.getDefaultContext();
        PokerGame game = context == null ? null : (PokerGame) context.getGame();

        // Always-present lifecycle fields
        int inputMode = game == null ? PokerTableInput.MODE_NONE : game.getInputMode();
        String inputModeName = ActionHandler.inputModeToString(inputMode);
        List<String> availableActions = ActionHandler.availableActionsForMode(inputMode);
        String lifecyclePhase = deriveLifecyclePhase(context);

        if (game == null) {
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("gamePhase", "NONE");
            state.put("lifecyclePhase", lifecyclePhase);
            state.put("inputMode", inputModeName);
            state.put("availableActions", availableActions);
            state.put("version", PokerConstants.VERSION.getMajorAsString());
            return state;
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("gamePhase", deriveGamePhase(game));
        state.put("lifecyclePhase", lifecyclePhase);
        state.put("inputMode", inputModeName);
        state.put("availableActions", availableActions);
        state.put("version", PokerConstants.VERSION.getMajorAsString());
        state.put("tournament", buildTournamentInfo(game));
        List<Map<String, Object>> tables = buildTables(game);
        state.put("tables", tables);
        state.put("tableCount", tables.size());
        state.put("currentAction", buildCurrentAction(game, context, inputMode));
        state.put("recentEvents", buildRecentEvents());

        // Puppet mode info
        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director != null) {
            ServerPlayerActionProvider provider = director.getActionProvider();
            Set<Integer> puppetedIds = provider.getPuppetedPlayerIds();
            List<Integer> puppetedSeats = new ArrayList<>();
            PokerTable currentTable = game.getCurrentTable();
            if (currentTable != null) {
                for (int s = 0; s < PokerConstants.SEATS; s++) {
                    PokerPlayer pp = currentTable.getPlayer(s);
                    if (pp != null && puppetedIds.contains(pp.getID())) {
                        puppetedSeats.add(s);
                    }
                }
            }
            state.put("puppetedSeats", puppetedSeats);
        }

        return state;
    }

    private String deriveLifecyclePhase(GameContext context) {
        if (context == null) return "NONE";
        Phase phase = context.getCurrentUIPhase();
        if (phase == null) return "NONE";
        return phase.getGamePhase().getName();
    }

    private String deriveGamePhase(PokerGame game) {
        if (game.isSimulatorMode()) return "SIMULATOR";
        if (game.isClockMode()) return "CLOCK";
        int inputMode = game.getInputMode();
        if (inputMode == PokerTableInput.MODE_NONE) return "NONE";
        List<PokerTable> tables = game.getTables();
        if (tables == null || tables.isEmpty()) return "LOBBY";
        // Prefer the current table (set by WebSocketTournamentDirector) over tables.get(0)
        // which may be an unrelated loaded game when multiple tables exist in the context.
        PokerTable current = game.getCurrentTable();
        PokerTable table = (current != null) ? current : tables.get(0);
        HoldemHand hand = table.getHoldemHand();
        if (hand == null) return "BETWEEN_HANDS";
        return switch (hand.getRound()) {
            case NONE     -> "NONE";
            case PRE_FLOP -> "PRE_FLOP";
            case FLOP     -> "FLOP";
            case TURN     -> "TURN";
            case RIVER    -> "RIVER";
            case SHOWDOWN -> "SHOWDOWN";
        };
    }

    private Map<String, Object> buildTournamentInfo(PokerGame game) {
        Map<String, Object> t = new LinkedHashMap<>();
        // profile_ may be null during early game initialization; guard to avoid NPE.
        // PokerGame.nLevel_ starts at 0 but TournamentProfile levels are 1-indexed,
        // so getSmallBlind(0) returns 0. Use effectiveLevel = max(1, level) for lookups.
        // Also expose effectiveLevel as the "level" field so 0 is never returned (0 is
        // falsy in JavaScript and confuses test assertions using ||"" fallbacks).
        TournamentProfile profile0 = game.getProfile();
        int effectiveLevel = Math.max(1, game.getLevel());
        t.put("level", effectiveLevel);
        t.put("smallBlind", profile0 != null ? profile0.getSmallBlind(effectiveLevel) : 0);
        t.put("bigBlind",   profile0 != null ? profile0.getBigBlind(effectiveLevel)   : 0);
        t.put("ante",       profile0 != null ? profile0.getAnte(effectiveLevel)       : 0);
        t.put("totalPlayers", game.getNumPlayers());
        t.put("playersRemaining", game.getNumPlayers() - game.getNumPlayersOut());
        t.put("prizePool", game.getPrizePool());
        t.put("prizesPaid", game.getPrizesPaid());
        t.put("isGameOver", game.isGameOver());

        // Hand number from the current table (0 = no hand yet dealt)
        PokerTable currentTable = game.getCurrentTable();
        if (currentTable == null) {
            List<PokerTable> tables = game.getTables();
            currentTable = (tables != null && !tables.isEmpty()) ? tables.get(0) : null;
        }
        t.put("handNumber", currentTable != null ? currentTable.getHandNum() : 0);

        TournamentProfile profile = profile0;
        if (profile != null) {
            t.put("tournamentName", profile.getName());
            t.put("levelCount", profile.getLastLevel());

            // Next level blinds preview
            int nextLevel = effectiveLevel + 1;
            if (nextLevel <= profile.getLastLevel()) {
                t.put("nextSmallBlind", profile.getSmallBlind(nextLevel));
                t.put("nextBigBlind", profile.getBigBlind(nextLevel));
            }

            // Hands-per-level mode
            String advanceMode = profile.getLevelAdvanceMode().name();
            t.put("levelAdvanceMode", advanceMode);
            if ("HANDS".equals(advanceMode)) {
                t.put("handsPerLevel", profile.getHandsPerLevel());
                t.put("handsInLevel", game.getHandsInLevel());
            }

            t.put("payoutTable", buildPayoutTable(game, profile));
        }

        t.put("standings", buildTournamentStandings(game));

        // Chip leaderboard (top players across all tables)
        List<PokerTable> allTables = game.getTables();
        if (allTables != null && !allTables.isEmpty()) {
            List<Map<String, Object>> leaders = new ArrayList<>();
            for (PokerTable tbl : allTables) {
                for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
                    PokerPlayer p = tbl.getPlayer(seat);
                    if (p != null && !p.isEliminated()) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("name", p.getName());
                        entry.put("chips", p.getChipCount());
                        entry.put("table", tbl.getNumber());
                        entry.put("isHuman", p.isHuman());
                        leaders.add(entry);
                    }
                }
            }
            leaders.sort((a, b) -> Integer.compare((int) b.get("chips"), (int) a.get("chips")));
            t.put("chipLeaderboard", leaders.size() > 20 ? leaders.subList(0, 20) : leaders);
        }

        // Clock state
        GameClock clock = game.getGameClock();
        if (clock != null) {
            Map<String, Object> clockInfo = new LinkedHashMap<>();
            clockInfo.put("secondsRemaining", clock.getSecondsRemaining());
            clockInfo.put("isPaused", clock.isPaused());
            clockInfo.put("isExpired", clock.isExpired());
            t.put("clock", clockInfo);
        }

        return t;
    }

    private List<Map<String, Object>> buildTables(PokerGame game) {
        List<PokerTable> tables = game.getTables();
        if (tables == null) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PokerTable table : tables) {
            result.add(buildTable(table, game));
        }
        return result;
    }

    private Map<String, Object> buildTable(PokerTable table, PokerGame game) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", table.getNumber());
        t.put("dealerSeat", table.getButton());
        t.put("occupiedSeats", table.getNumOccupiedSeats());

        HoldemHand hand = table.getHoldemHand();
        if (hand != null) {
            t.put("pot", hand.getTotalPotChipCount());
            t.put("communityCards", cardsToStrings(hand.getCommunityCards()));
            t.put("round", hand.getRound().name());
        } else {
            t.put("pot", 0);
            t.put("communityCards", List.of());
            t.put("round", "NONE");
        }

        List<Map<String, Object>> players = new ArrayList<>();
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                players.add(buildPlayer(player, hand));
            }
        }
        t.put("players", players);
        t.put("chipConservation", buildChipConservation(table, hand));
        if (hand != null) {
            t.put("currentBets", buildCurrentBets(table, hand));
        }
        return t;
    }

    private Map<String, Object> buildPlayer(PokerPlayer player, HoldemHand hand) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("seat", player.getSeat());
        p.put("name", player.getName());
        p.put("chips", player.getChipCount());
        p.put("place", player.getPlace());
        p.put("prize", player.getPrize());
        p.put("totalSpent", player.getTotalSpent());
        p.put("netResult", player.getPrize() - player.getTotalSpent());
        p.put("isHuman", player.isHuman());
        p.put("isEliminated", player.isEliminated());
        p.put("isFolded", player.isFolded());
        p.put("isAllIn", player.isAllIn());
        p.put("isSittingOut", player.isSittingOut());

        // AI player type name
        if (!player.isHuman()) {
            PlayerType pt = player.getPlayerType();
            if (pt != null) {
                p.put("playerType", pt.getName());
            }
        }

        // Hole cards — show for human, showdown, or when aifaceup cheat is active
        Hand holeCards = player.getHand();
        boolean aiFaceUp = !player.isHuman()
                && hand != null
                && PokerUtils.isOptionOn(PokerConstants.OPTION_CHEAT_AIFACEUP);
        if (player.isHuman() || (hand != null && hand.getRound() == BettingRound.SHOWDOWN) || aiFaceUp) {
            p.put("holeCards", handToStrings(holeCards));
        } else {
            p.put("holeCards", List.of()); // hidden for AI players
        }

        if (hand != null) {
            p.put("totalBetThisHand", hand.getTotalBet(player));
        }

        // Hand strength and rank for the human player during a hand
        if (player.isHuman() && hand != null && hand.getRound() != BettingRound.NONE) {
            p.put("handStrength", player.getHandStrength());
            p.put("effectiveHandStrength", player.getEffectiveHandStrength());
            PokerGame game = player.getTable() != null ? player.getTable().getGame() : null;
            if (game != null) {
                try {
                    p.put("rank", game.getRank(player));
                } catch (Exception ignored) {
                    // getRank throws if player not yet in rank list during initialization
                }
            }
        }
        return p;
    }

    private List<Map<String, Object>> buildPayoutTable(PokerGame game, TournamentProfile profile) {
        List<Map<String, Object>> payouts = new ArrayList<>();
        int totalPlayers = Math.max(0, game.getNumPlayers());
        for (int place = 1; place <= totalPlayers; place++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("place", place);
            row.put("payout", profile.getPayout(place));
            payouts.add(row);
        }
        return payouts;
    }

    private List<Map<String, Object>> buildTournamentStandings(PokerGame game) {
        List<Map<String, Object>> standings = new ArrayList<>();
        int totalPlayers = Math.max(0, game.getNumPlayers());

        for (int i = 0; i < totalPlayers; i++) {
            PokerPlayer player = game.getPokerPlayerAt(i);
            if (player == null) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", player.getName());
            row.put("isHuman", player.isHuman());
            row.put("chips", player.getChipCount());
            row.put("place", player.getPlace());
            row.put("prize", player.getPrize());
            row.put("totalSpent", player.getTotalSpent());
            row.put("netResult", player.getPrize() - player.getTotalSpent());
            row.put("isEliminated", player.isEliminated());
            row.put("isObserver", player.isObserver());
            row.put("tableId", player.getTable() != null ? player.getTable().getNumber() : -1);

            try {
                row.put("rank", game.getRank(player));
            } catch (Exception e) {
                row.put("rank", 0);
            }

            standings.add(row);
        }

        standings.sort(Comparator
                .comparingInt((Map<String, Object> row) -> {
                    Object placeObj = row.get("place");
                    int place = placeObj instanceof Number n ? n.intValue() : 0;
                    return place > 0 ? place : Integer.MAX_VALUE;
                })
                .thenComparing((Map<String, Object> row) -> {
                    Object chipsObj = row.get("chips");
                    int chips = chipsObj instanceof Number n ? n.intValue() : 0;
                    return -chips;
                })
                .thenComparing(row -> String.valueOf(row.getOrDefault("name", ""))));

        return standings;
    }

    private Map<String, Object> buildCurrentAction(PokerGame game, GameContext context, int inputMode) {
        boolean isHumanTurn = inputMode == PokerTableInput.MODE_CHECK_BET
                || inputMode == PokerTableInput.MODE_CHECK_RAISE
                || inputMode == PokerTableInput.MODE_CALL_RAISE;

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("isHumanTurn", isHumanTurn);

        if (!isHumanTurn) return action;

        PokerPlayer human = game.getHumanPlayer();
        PokerTable table = game.getCurrentTable();
        HoldemHand hand = table != null ? table.getHoldemHand() : null;

        if (human != null) {
            action.put("humanSeat", human.getSeat());
        }

        List<String> available = new ArrayList<>();
        available.add("FOLD");
        if (inputMode == PokerTableInput.MODE_CHECK_BET || inputMode == PokerTableInput.MODE_CHECK_RAISE) {
            available.add("CHECK");
        } else {
            available.add("CALL");
        }
        if (inputMode == PokerTableInput.MODE_CHECK_BET) {
            available.add("BET");
        } else {
            available.add("RAISE");
        }
        available.add("ALL_IN");
        action.put("availableActions", available);

        if (hand != null && human != null) {
            action.put("callAmount", hand.getAmountToCall(human));
            action.put("minBet", hand.getMinBet());
            action.put("maxBet", hand.getMaxBet(human));
            action.put("minRaise", hand.getMinRaise());
            action.put("pot", hand.getTotalPotChipCount());
        }

        action.put("advisorAdvice", DashboardAdvisor.getCurrentAdvice());
        action.put("advisorTitle", DashboardAdvisor.getCurrentTitle());

        // Current player info (for puppet mode)
        PokerTable actionTable = game.getCurrentTable();
        if (actionTable != null) {
            HoldemHand actionHand = actionTable.getHoldemHand();
            if (actionHand != null) {
                PokerPlayer current = actionHand.getCurrentPlayer();
                if (current != null) {
                    action.put("currentPlayerSeat", current.getSeat());
                    action.put("currentPlayerName", current.getName());
                    ServerTournamentDirector dir = ServerTournamentDirector.getCurrent();
                    action.put("isPlayerPuppeted",
                            dir != null && dir.getActionProvider().isPuppeted(current.getID()));
                }
            }
        }

        return action;
    }

    private List<Map<String, Object>> buildRecentEvents() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GameEventLog.Entry e : GameEventLog.getLastN(20)) {
            Map<String, Object> ev = new LinkedHashMap<>();
            ev.put("ms", e.millis());
            ev.put("type", e.type());
            ev.put("table", e.tableId());
            result.add(ev);
        }
        return result;
    }

    private Map<String, Object> buildChipConservation(PokerTable table, HoldemHand hand) {
        int playerTotal = 0;
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                playerTotal += player.getChipCount();
            }
        }
        int inPot = hand != null ? hand.getTotalPotChipCount() : 0;
        Map<String, Object> cc = new LinkedHashMap<>();
        cc.put("playerTotal", playerTotal);
        cc.put("inPot", inPot);
        cc.put("sum", playerTotal + inPot);
        return cc;
    }

    private Map<String, Object> buildCurrentBets(PokerTable table, HoldemHand hand) {
        Map<String, Object> bets = new LinkedHashMap<>();
        for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
            PokerPlayer player = table.getPlayer(seat);
            if (player != null) {
                int bet = hand.getBet(player);
                if (bet > 0) {
                    bets.put("seat" + seat, bet);
                }
            }
        }
        return bets;
    }

    private List<String> cardsToStrings(Card[] cards) {
        if (cards == null) return List.of();
        List<String> result = new ArrayList<>();
        for (Card c : cards) {
            if (c != null) result.add(c.getDisplay());
        }
        return result;
    }

    private List<String> handToStrings(Hand hand) {
        if (hand == null || hand.size() == 0) return List.of();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.getCard(i);
            if (c != null && !c.isBlank()) result.add(c.getDisplay());
        }
        return result;
    }
}
