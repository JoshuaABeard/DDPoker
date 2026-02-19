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
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.PokerConstants;
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
            return state;
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("gamePhase", deriveGamePhase(game));
        state.put("lifecyclePhase", lifecyclePhase);
        state.put("inputMode", inputModeName);
        state.put("availableActions", availableActions);
        state.put("tournament", buildTournamentInfo(game));
        state.put("tables", buildTables(game));
        state.put("currentAction", buildCurrentAction(game, inputMode));
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
        t.put("level", game.getLevel());
        t.put("smallBlind", game.getSmallBlind());
        t.put("bigBlind", game.getBigBlind());
        t.put("ante", game.getAnte());
        t.put("totalPlayers", game.getNumPlayers());
        t.put("playersRemaining", game.getNumPlayers() - game.getNumPlayersOut());
        if (game.getProfile() != null) {
            t.put("tournamentName", game.getProfile().getName());
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
        return t;
    }

    private Map<String, Object> buildPlayer(PokerPlayer player, HoldemHand hand) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("seat", player.getSeat());
        p.put("name", player.getName());
        p.put("chips", player.getChipCount());
        p.put("isHuman", player.isHuman());
        p.put("isEliminated", player.isEliminated());
        p.put("isFolded", player.isFolded());
        p.put("isAllIn", player.isAllIn());
        p.put("isSittingOut", player.isSittingOut());

        // Hole cards — only show for the local human player or during showdown
        Hand holeCards = player.getHand();
        if (player.isHuman() || (hand != null && hand.getRound() == BettingRound.SHOWDOWN)) {
            p.put("holeCards", handToStrings(holeCards));
        } else {
            p.put("holeCards", List.of()); // hidden for AI players
        }

        if (hand != null) {
            p.put("totalBetThisHand", hand.getTotalBet(player));
        }
        return p;
    }

    private Map<String, Object> buildCurrentAction(PokerGame game, int inputMode) {
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

        return action;
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
