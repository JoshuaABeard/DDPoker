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
 * {@code GET /hand/result} — returns the most recently completed hand's result
 * including winner, hand ranking, and pot breakdown.
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

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        PokerTable table = game.getCurrentTable();
        if (table == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active table"));
            return;
        }

        HoldemHand hand = table.getHoldemHand();
        if (hand == null || hand.getEndDate() == 0) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No completed hand available"));
            return;
        }

        Map<String, Object> result = buildHandResult(hand, table);
        sendJson(exchange, 200, result);
    }

    private Map<String, Object> buildHandResult(HoldemHand hand, PokerTable table) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handNumber", table.getHandNum());
        result.put("communityCards", cardsToList(hand.getCommunity()));
        result.put("isUncontested", hand.isUncontested());

        // Pots
        List<Map<String, Object>> pots = new ArrayList<>();
        int numPots = hand.getNumPots();
        for (int i = 0; i < numPots; i++) {
            Pot pot = hand.getPot(i);
            if (pot.isOverbet()) {
                continue; // skip overbet pots
            }
            pots.add(buildPotResult(pot, hand, i));
        }
        result.put("pots", pots);

        // Player results
        List<Map<String, Object>> playerResults = new ArrayList<>();
        int numPlayers = hand.getNumPlayers();
        for (int i = 0; i < numPlayers; i++) {
            PokerPlayer player = hand.getPlayerAt(i);
            playerResults.add(buildPlayerResult(player, hand));
        }
        result.put("playerResults", playerResults);

        return result;
    }

    private Map<String, Object> buildPotResult(Pot pot, HoldemHand hand, int potNumber) {
        Map<String, Object> potMap = new LinkedHashMap<>();
        potMap.put("potNumber", potNumber);
        potMap.put("chipCount", pot.getChipCount());

        // Players in this pot
        List<String> playerNames = new ArrayList<>();
        for (PokerPlayer p : pot.getPlayers()) {
            playerNames.add(p.getName());
        }
        potMap.put("players", playerNames);

        // Winners
        List<Map<String, Object>> winners = new ArrayList<>();
        for (PokerPlayer winner : pot.getWinners()) {
            winners.add(buildHandDetails(winner, hand, true));
        }
        potMap.put("winners", winners);

        // Losers: players in the pot who are not winners and not folded
        List<Map<String, Object>> losers = new ArrayList<>();
        for (PokerPlayer p : pot.getPlayers()) {
            if (pot.getWinners().contains(p) || hand.isFolded(p)) {
                continue;
            }
            losers.add(buildHandDetails(p, hand, false));
        }
        potMap.put("losers", losers);

        return potMap;
    }

    private Map<String, Object> buildHandDetails(PokerPlayer player, HoldemHand hand, boolean includeWin) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", player.getName());
        details.put("seat", player.getSeat());
        details.put("holeCards", cardsToList(player.getHand()));

        HandInfo handInfo = player.getHandInfo();
        if (handInfo != null) {
            details.put("handType", handInfo.getHandTypeDesc());

            // Use HandInfoFast for hand description string
            HandInfoFast fast = new HandInfoFast();
            fast.getScore(player.getHand(), hand.getCommunity());
            details.put("handDescription", fast.toString());

            details.put("bestHand", cardsToList(handInfo.getBest()));
            details.put("score", handInfo.getScore());
        }

        if (includeWin) {
            details.put("totalWin", hand.getWin(player));
        }

        return details;
    }

    private Map<String, Object> buildPlayerResult(PokerPlayer player, HoldemHand hand) {
        Map<String, Object> playerResult = new LinkedHashMap<>();
        playerResult.put("name", player.getName());
        playerResult.put("seat", player.getSeat());
        playerResult.put("totalWin", hand.getWin(player));
        playerResult.put("totalOverbet", hand.getOverbet(player));
        playerResult.put("chipsAfter", player.getChipCount());
        playerResult.put("isFolded", hand.isFolded(player));
        return playerResult;
    }

    private List<String> cardsToList(Hand hand) {
        List<String> cards = new ArrayList<>();
        if (hand == null) {
            return cards;
        }
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.getCard(i);
            cards.add(card.getDisplay());
        }
        return cards;
    }

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }
}
