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
import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.gameserver.ServerPlayerActionProvider;
import com.donohoedigital.games.poker.gameserver.ServerTournamentDirector;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /players/puppet} — lists currently puppeted seat numbers.
 * <p>
 * {@code POST /players/puppet} — enables/disables puppet mode for an AI seat.
 */
class PuppetHandler extends BaseHandler {

    PuppetHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET" -> handleGet(exchange);
            case "POST" -> handlePost(exchange);
            default -> sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        ServerPlayerActionProvider provider = director.getActionProvider();
        PokerGame game = getGame();

        // Map puppeted player IDs back to seat numbers
        List<Integer> puppetedSeats = new ArrayList<>();
        if (game != null) {
            ClientPokerTable table = game.getCurrentTable();
            if (table != null) {
                for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
                    ClientPlayer player = table.getPlayer(seat);
                    if (player != null && provider.isPuppeted(player.getID())) {
                        puppetedSeats.add(seat);
                    }
                }
            }
        }

        sendJson(exchange, 200, Map.of("puppetedSeats", puppetedSeats));
    }

    private void handlePost(HttpExchange exchange) throws Exception {
        ServerTournamentDirector director = ServerTournamentDirector.getCurrent();
        if (director == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No active game"));
            return;
        }

        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }

        JsonNode json = MAPPER.readTree(body);
        if (!json.has("seat")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'seat' field"));
            return;
        }

        int seat = json.get("seat").asInt(-1);
        boolean enabled = json.has("enabled") ? json.get("enabled").asBoolean(true) : true;

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

        ClientPlayer player = table.getPlayer(seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "No player at seat " + seat));
            return;
        }

        if (player.isHuman()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "Cannot puppet the human player — use /action instead"));
            return;
        }

        ServerPlayerActionProvider provider = director.getActionProvider();
        provider.setPuppeted(player.getID(), enabled);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accepted", true);
        response.put("seat", seat);
        response.put("name", player.getName());
        response.put("enabled", enabled);
        sendJson(exchange, 200, response);
    }

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }
}
