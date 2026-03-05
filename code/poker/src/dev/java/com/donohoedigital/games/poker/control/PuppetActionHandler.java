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
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.gameserver.ServerPlayerActionProvider;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * {@code POST /players/action} — submits an action for a puppeted AI player.
 * <p>
 * Supported action types: FOLD, CHECK, CALL, BET, RAISE, ALL_IN.
 * <pre>
 *   {"seat": 1, "type": "CALL"}
 *   {"seat": 1, "type": "RAISE", "amount": 200}
 * </pre>
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
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }

        JsonNode json = MAPPER.readTree(body);

        // Validate required fields
        if (!json.has("seat")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'seat' field"));
            return;
        }
        if (!json.has("type")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'type' field"));
            return;
        }

        int seat = json.get("seat").asInt(-1);
        String type = json.get("type").asText("").toUpperCase();
        int amount = json.has("amount") ? json.get("amount").asInt(0) : 0;

        // Check active game
        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        ClientPokerTable table = game.getCurrentTable();
        if (table == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        if (seat < 0 || seat >= PokerConstants.SEATS) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid seat number: " + seat));
            return;
        }

        PokerPlayer player = table.getPlayer(seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "No player at seat " + seat));
            return;
        }

        ServerPlayerActionProvider provider = director.getActionProvider();
        if (!provider.isPuppeted(player.getID())) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "Seat " + seat + " is not puppeted — call POST /players/puppet first"));
            return;
        }

        // Map action type string to PlayerAction
        PlayerAction action = switch (type) {
            case "FOLD" -> PlayerAction.fold();
            case "CHECK" -> PlayerAction.check();
            case "CALL" -> PlayerAction.call();
            case "BET" -> PlayerAction.bet(amount);
            case "RAISE" -> PlayerAction.raise(amount);
            case "ALL_IN" -> PlayerAction.raise(Integer.MAX_VALUE);
            default -> null;
        };

        if (action == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "Unknown action type: " + type +
                               ". Valid: FOLD, CHECK, CALL, BET, RAISE, ALL_IN"));
            return;
        }

        provider.submitAction(player.getID(), action);
        sendJson(exchange, 200, Map.of("accepted", true, "seat", seat, "type", type, "amount", amount));
    }

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }
}
