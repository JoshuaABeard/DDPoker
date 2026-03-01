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

import com.donohoedigital.games.poker.core.ai.HandInfoFast;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
import com.donohoedigital.games.poker.gameserver.ServerHand;
import com.donohoedigital.games.poker.gameserver.ServerPlayer;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /hand/result} — returns the most recently completed hand's result
 * including winner, hand ranking, and pot breakdown.
 * <p>
 * Uses server-side data cached by {@link ServerTournamentDirector} after hand
 * resolution, so results are available even after the table moves to the next hand.
 */
class HandResultHandler extends BaseHandler {

    /** Tracks which ServerHand we last built a result for, to avoid rebuilding. */
    private volatile ServerHand lastProcessedHand;
    /** Cached result map from the most recently completed hand. */
    private volatile Map<String, Object> lastResult;

    HandResultHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        ServerHand resolvedHand = director.getLastResolvedHand();
        if (resolvedHand == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No completed hand available"));
            return;
        }

        // Build result only once per resolved hand
        if (resolvedHand != lastProcessedHand) {
            lastResult = buildHandResult(resolvedHand, director.getLastResolvedTable(),
                    director.getLastResolvedHandNum());
            lastProcessedHand = resolvedHand;
        }

        sendJson(exchange, 200, lastResult);
    }

    private Map<String, Object> buildHandResult(ServerHand hand, ServerGameTable table, int handNum) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("handNumber", handNum);

        Card[] communityCards = hand.getCommunityCards();
        result.put("communityCards", cardsToDisplayList(communityCards));
        result.put("isUncontested", hand.isUncontested());

        Hand communityHand = toHand(communityCards);

        // Build pot results from resolution data
        List<ServerHand.PotResolutionResult> resolutions = hand.getResolutionResults();
        List<Map<String, Object>> pots = new ArrayList<>();
        for (ServerHand.PotResolutionResult pr : resolutions) {
            pots.add(buildPotResult(pr, hand, table, communityHand));
        }
        result.put("pots", pots);

        // Player results from the table
        List<Map<String, Object>> playerResults = new ArrayList<>();
        if (table != null) {
            for (int seat = 0; seat < table.getSeats(); seat++) {
                ServerPlayer player = table.getPlayer(seat);
                if (player == null) continue;
                // Only include players who had hole cards (participated in this hand)
                List<Card> holeCards = hand.getPlayerCards(player.getID());
                if (holeCards.isEmpty()) continue;

                Map<String, Object> pr = new LinkedHashMap<>();
                pr.put("name", player.getName());
                pr.put("seat", player.getSeat());
                pr.put("chipsAfter", player.getChipCount());
                pr.put("isFolded", hand.wasPlayerFolded(player.getID()));
                pr.put("isHuman", player.isHuman());
                playerResults.add(pr);
            }
        }
        result.put("playerResults", playerResults);

        return result;
    }

    private Map<String, Object> buildPotResult(ServerHand.PotResolutionResult pr, ServerHand hand,
            ServerGameTable table, Hand communityHand) {
        Map<String, Object> potMap = new LinkedHashMap<>();
        potMap.put("potNumber", pr.potIndex());
        potMap.put("chipCount", pr.amount());

        List<Map<String, Object>> winners = new ArrayList<>();
        int share = pr.winnerIds().length > 0 ? pr.amount() / pr.winnerIds().length : 0;

        for (int winnerId : pr.winnerIds()) {
            Map<String, Object> winnerMap = new LinkedHashMap<>();

            // Look up player name and seat from table
            if (table != null) {
                for (int seat = 0; seat < table.getSeats(); seat++) {
                    ServerPlayer p = table.getPlayer(seat);
                    if (p != null && p.getID() == winnerId) {
                        winnerMap.put("name", p.getName());
                        winnerMap.put("seat", p.getSeat());
                        break;
                    }
                }
            }

            List<Card> holeCards = hand.getPlayerCards(winnerId);
            winnerMap.put("holeCards", cardsToDisplayList(holeCards));

            // Evaluate hand for description
            if (communityHand != null && communityHand.size() >= 3 && !holeCards.isEmpty()) {
                Hand playerHand = toHand(holeCards);
                HandInfoFast fast = new HandInfoFast();
                int score = fast.getScore(playerHand, communityHand);
                winnerMap.put("handDescription", handTypeName(HandInfoFast.getTypeFromScore(score)));
                winnerMap.put("score", score);
            }

            winnerMap.put("totalWin", share);
            winners.add(winnerMap);
        }
        potMap.put("winners", winners);

        return potMap;
    }

    private static String handTypeName(int type) {
        return switch (type) {
            case HandInfoFast.ROYAL_FLUSH -> "Royal Flush";
            case HandInfoFast.STRAIGHT_FLUSH -> "Straight Flush";
            case HandInfoFast.QUADS -> "Four of a Kind";
            case HandInfoFast.FULL_HOUSE -> "Full House";
            case HandInfoFast.FLUSH -> "Flush";
            case HandInfoFast.STRAIGHT -> "Straight";
            case HandInfoFast.TRIPS -> "Three of a Kind";
            case HandInfoFast.TWO_PAIR -> "Two Pair";
            case HandInfoFast.PAIR -> "One Pair";
            case HandInfoFast.HIGH_CARD -> "High Card";
            default -> "Unknown";
        };
    }

    private List<String> cardsToDisplayList(Card[] cards) {
        List<String> list = new ArrayList<>();
        if (cards == null) return list;
        for (Card card : cards) {
            list.add(card.getDisplay());
        }
        return list;
    }

    private List<String> cardsToDisplayList(List<Card> cards) {
        List<String> list = new ArrayList<>();
        if (cards == null) return list;
        for (Card card : cards) {
            list.add(card.getDisplay());
        }
        return list;
    }

    private Hand toHand(Card[] cards) {
        if (cards == null || cards.length == 0) return null;
        Hand hand = new Hand();
        for (Card card : cards) {
            hand.addCard(card);
        }
        return hand;
    }

    private Hand toHand(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return null;
        Hand hand = new Hand();
        for (Card card : cards) {
            hand.addCard(card);
        }
        return hand;
    }
}
