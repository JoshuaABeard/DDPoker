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

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Deck;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFaster;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.*;

/**
 * {@code POST /simulator} — run hand equity simulation.
 *
 * <p>Request body:
 * <pre>{
 *   "holeCards": ["As", "Ks"],
 *   "community": ["Td", "9d", "4c"],
 *   "numSimulations": 10000
 * }</pre>
 *
 * <p>Returns win/lose/tie percentages.
 */
class SimulatorHandler extends BaseHandler {

    SimulatorHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body required"));
            return;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON"));
            return;
        }

        if (!json.has("holeCards") || !json.get("holeCards").isArray()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "holeCards array required"));
            return;
        }

        // Parse hole cards
        Hand hole = new Hand();
        for (JsonNode cardNode : json.get("holeCards")) {
            Card c = Card.getCard(cardNode.asText());
            if (c == null) {
                sendJson(exchange, 400, Map.of("error", "BadRequest",
                        "message", "Invalid card: " + cardNode.asText()));
                return;
            }
            hole.addCard(c);
        }

        if (hole.size() != 2) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Exactly 2 hole cards required"));
            return;
        }

        // Parse community cards (optional)
        Hand community = new Hand();
        if (json.has("community") && json.get("community").isArray()) {
            for (JsonNode cardNode : json.get("community")) {
                Card c = Card.getCard(cardNode.asText());
                if (c == null) {
                    sendJson(exchange, 400, Map.of("error", "BadRequest",
                            "message", "Invalid community card: " + cardNode.asText()));
                    return;
                }
                community.addCard(c);
            }
        }

        // Compute win rate via Monte Carlo enumeration of opponent hands
        HandInfoFaster fast = new HandInfoFaster();
        Hand holecopy = new Hand(hole);
        int ourscore = fast.getScore(holecopy, community);

        Deck deck = new Deck(false);
        deck.removeCards(hole);
        deck.removeCards(community);

        long win = 0, tie = 0, lose = 0;
        int nSize = deck.size();
        for (int i = 0; i < nSize - 1; i++) {
            for (int j = i + 1; j < nSize; j++) {
                holecopy.setCard(0, deck.getCard(i));
                holecopy.setCard(1, deck.getCard(j));
                int oppscore = fast.getScore(holecopy, community);
                if (ourscore > oppscore) win++;
                else if (ourscore == oppscore) tie++;
                else lose++;
            }
        }

        long total = win + tie + lose;
        double winPct = total > 0 ? 100.0 * win / total : 0.0;
        double tiePct = total > 0 ? 100.0 * tie / total : 0.0;
        double losePct = total > 0 ? 100.0 * lose / total : 0.0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("holeCards", holeCardsToStrings(hole));
        response.put("community", holeCardsToStrings(community));
        response.put("completed", true);
        response.put("win", winPct);
        response.put("tie", tiePct);
        response.put("lose", losePct);

        sendJson(exchange, 200, response);
    }

    private List<String> holeCardsToStrings(Hand hand) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.getCard(i);
            if (c != null) result.add(c.getDisplay());
        }
        return result;
    }
}
