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
import com.donohoedigital.games.poker.GameClock;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.online.ClientPokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Dev-only HTTP handler for {@code POST /cheat}.
 *
 * <p>Manipulates live game state for testing purposes. All state changes are dispatched
 * on the Swing EDT via {@code SwingUtilities.invokeLater}.
 *
 * <p>Supported actions:
 * <pre>
 *   {"action": "setChips",        "seat": 2, "amount": 100}
 *   {"action": "setLevel",        "level": 3}
 *   {"action": "setButton",       "seat": 1}
 *   {"action": "eliminatePlayer", "seat": 0}
 * </pre>
 *
 * <p>Returns 400 if the action is unknown or required parameters are missing.
 * Returns 409 if no game is currently running.
 */
class CheatHandler extends BaseHandler {

    CheatHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

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

        if (!body.containsKey("action")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'action' field"));
            return;
        }

        String action = body.get("action").toString();
        switch (action) {
            case "setChips" -> handleSetChips(exchange, body);
            case "setLevel" -> handleSetLevel(exchange, body);
            case "setButton" -> handleSetButton(exchange, body);
            case "eliminatePlayer" -> handleEliminatePlayer(exchange, body);
            case "completeGame" -> handleCompleteGame(exchange);
            case "advanceClock" -> handleAdvanceClock(exchange, body);
            default -> sendJson(exchange, 400, Map.of(
                    "error", "BadRequest",
                    "message", "Unknown action: '" + action + "'. Valid: setChips, setLevel, setButton, eliminatePlayer, completeGame, advanceClock"));
        }
    }

    // -------------------------------------------------------------------------
    // Action handlers
    // -------------------------------------------------------------------------

    private void handleSetChips(HttpExchange exchange, Map<String, Object> body) throws Exception {
        Integer seat = getIntParam(body, "seat");
        Integer amount = getIntParam(body, "amount");
        if (seat == null || amount == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "setChips requires 'seat' (int) and 'amount' (int)"));
            return;
        }
        if (amount < 0) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'amount' must be >= 0"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game is currently running"));
            return;
        }

        PokerPlayer player = getPlayerAtSeat(game, seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "No player at seat " + seat));
            return;
        }

        int finalAmount = amount;
        SwingUtilities.invokeLater(() -> player.setChipCount(finalAmount));
        sendJson(exchange, 200, Map.of("accepted", true, "action", "setChips", "seat", seat, "amount", finalAmount));
    }

    private void handleSetLevel(HttpExchange exchange, Map<String, Object> body) throws Exception {
        Integer level = getIntParam(body, "level");
        if (level == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "setLevel requires 'level' (int, 1-based)"));
            return;
        }
        if (level < 1) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'level' must be >= 1"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game is currently running"));
            return;
        }

        int finalLevel = level;
        SwingUtilities.invokeLater(() -> game.setLevel(finalLevel));
        sendJson(exchange, 200, Map.of("accepted", true, "action", "setLevel", "level", finalLevel));
    }

    private void handleSetButton(HttpExchange exchange, Map<String, Object> body) throws Exception {
        Integer seat = getIntParam(body, "seat");
        if (seat == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "setButton requires 'seat' (int)"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game is currently running"));
            return;
        }

        PokerPlayer player = getPlayerAtSeat(game, seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "No player at seat " + seat));
            return;
        }

        PokerTable table = (PokerTable) game.getCurrentTable();
        int finalSeat = seat;
        SwingUtilities.invokeLater(() -> table.setButton(finalSeat));
        sendJson(exchange, 200, Map.of("accepted", true, "action", "setButton", "seat", finalSeat));
    }

    private void handleEliminatePlayer(HttpExchange exchange, Map<String, Object> body) throws Exception {
        Integer seat = getIntParam(body, "seat");
        if (seat == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "eliminatePlayer requires 'seat' (int)"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game is currently running"));
            return;
        }

        PokerPlayer player = getPlayerAtSeat(game, seat);
        if (player == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "No player at seat " + seat));
            return;
        }

        SwingUtilities.invokeLater(() -> player.setEliminated(true));
        sendJson(exchange, 200, Map.of("accepted", true, "action", "eliminatePlayer", "seat", seat));
    }

    private void handleCompleteGame(HttpExchange exchange) throws Exception {
        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game is currently running"));
            return;
        }

        SwingUtilities.invokeLater(() -> {
            PokerPlayer human = game.getHumanPlayer();
            if (human == null) return;

            // Collect all chips from AI players and give them to the human
            int totalChips = human.getChipCount();
            List<ClientPokerTable> tables = game.getTables();
            if (tables == null) return;

            for (ClientPokerTable table : tables) {
                for (int seat = 0; seat < PokerConstants.SEATS; seat++) {
                    PokerPlayer p = table.getPlayer(seat);
                    if (p != null && !p.isHuman() && !p.isEliminated()) {
                        totalChips += p.getChipCount();
                        p.setChipCount(0);
                    }
                }
            }
            human.setChipCount(totalChips);
        });

        sendJson(exchange, 200, Map.of("accepted", true, "action", "completeGame"));
    }

    private void handleAdvanceClock(HttpExchange exchange, Map<String, Object> body) throws Exception {
        Integer seconds = getIntParam(body, "seconds");
        if (seconds == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "advanceClock requires 'seconds' (int)"));
            return;
        }
        if (seconds < 1) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'seconds' must be >= 1"));
            return;
        }

        PokerGame game = getGame();
        if (game == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game is currently running"));
            return;
        }

        int finalSeconds = seconds;
        SwingUtilities.invokeLater(() -> {
            GameClock clock = game.getGameClock();
            if (clock == null) return;
            int newRemaining = Math.max(0, clock.getSecondsRemaining() - finalSeconds);
            clock.setSecondsRemaining(newRemaining);
        });

        sendJson(exchange, 200, Map.of("accepted", true, "action", "advanceClock", "seconds", finalSeconds));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PokerGame getGame() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        return (PokerGame) context.getGame();
    }

    /**
     * Returns the player at the given 0-based seat, or null if the seat is out of
     * range or unoccupied.
     */
    private PokerPlayer getPlayerAtSeat(PokerGame game, int seat) {
        ClientPokerTable table = game.getCurrentTable();
        if (table == null) return null;
        if (seat < 0 || seat >= table.getSeats()) return null;
        return table.getPlayer(seat);
    }

    /** Returns the integer value of a body parameter, or null if missing or wrong type. */
    private Integer getIntParam(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (!(val instanceof Number)) return null;
        return ((Number) val).intValue();
    }
}
