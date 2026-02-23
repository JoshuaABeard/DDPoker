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
import com.donohoedigital.games.poker.gameserver.CardInjectionRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dev-only HTTP handler for {@code POST /cards/inject} and {@code DELETE /cards/inject}.
 *
 * <p>{@code POST /cards/inject} stages a card injection for the next hand:
 * <ul>
 *   <li>{@code {"cards": ["As","Ks","2d","3c",...]}} — explicit deal order:
 *       seat0-card1, seat0-card2, seat1-card1, seat1-card2, …, burn, flop1-3, burn, turn,
 *       burn, river.</li>
 *   <li>{@code {"seed": 42}} — reproducible seeded shuffle.</li>
 * </ul>
 *
 * <p>{@code DELETE /cards/inject} clears any pending injection.
 *
 * <p>Returns 400 if any card string is unrecognized (maps to {@code BLANK}) or duplicated.
 */
class CardInjectHandler extends BaseHandler {

    CardInjectHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        switch (exchange.getRequestMethod().toUpperCase()) {
            case "POST" -> handlePost(exchange);
            case "DELETE" -> handleDelete(exchange);
            default -> sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handlePost(HttpExchange exchange) throws Exception {
        String rawBody = readRequestBodyAsString(exchange);
        if (rawBody.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }
        Map<String, Object> body;
        try {
            body = MAPPER.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON: " + e.getMessage()));
            return;
        }

        if (body.containsKey("seed")) {
            Object rawSeed = body.get("seed");
            if (!(rawSeed instanceof Number)) {
                sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'seed' must be a number"));
                return;
            }
            long seed = ((Number) rawSeed).longValue();
            CardInjectionRegistry.setSeed(seed);
            sendJson(exchange, 200, Map.of("accepted", true, "seed", seed));
            return;
        }

        if (body.containsKey("cards")) {
            Object rawCards = body.get("cards");
            if (!(rawCards instanceof List)) {
                sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'cards' must be an array"));
                return;
            }
            @SuppressWarnings("unchecked")
            List<String> cardStrings = (List<String>) rawCards;
            if (cardStrings.isEmpty()) {
                sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'cards' array must not be empty"));
                return;
            }
            List<Card> cards = new ArrayList<>(cardStrings.size());
            Set<Integer> seen = new HashSet<>();
            List<String> errors = new ArrayList<>();

            for (String s : cardStrings) {
                Card c = Card.getCard(s);
                if (c == null || c.isBlank()) {
                    errors.add("unrecognized card: \"" + s + "\"");
                } else if (!seen.add(c.getIndex())) {
                    errors.add("duplicate card: \"" + s + "\"");
                } else {
                    cards.add(c);
                }
            }

            if (!errors.isEmpty()) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("error", "InvalidCards");
                err.put("details", errors);
                sendJson(exchange, 400, err);
                return;
            }

            CardInjectionRegistry.setCards(cards);
            sendJson(exchange, 200, Map.of("accepted", true, "cardCount", cards.size()));
            return;
        }

        sendJson(exchange, 400, Map.of("error", "BadRequest",
                "message", "Request body must contain 'cards' array or 'seed' number"));
    }

    private void handleDelete(HttpExchange exchange) throws Exception {
        CardInjectionRegistry.clear();
        sendJson(exchange, 200, Map.of("accepted", true));
    }
}
