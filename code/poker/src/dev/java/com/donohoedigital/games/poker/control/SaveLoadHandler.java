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
import com.donohoedigital.games.poker.PokerMain;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.Map;

/**
 * Handles game save and load operations.
 *
 * <ul>
 *   <li>{@code POST /game/save} — save current game</li>
 *   <li>{@code POST /game/load} — load a saved game by name</li>
 * </ul>
 */
class SaveLoadHandler extends BaseHandler {

    private final String action; // "save" or "load"

    SaveLoadHandler(String apiKey, String action) {
        super(apiKey);
        this.action = action;
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        PokerMain main = PokerMain.getPokerMain();
        if (main == null) {
            sendJson(exchange, 503, Map.of("error", "ServiceUnavailable", "message", "PokerMain not initialized"));
            return;
        }

        GameContext context = main.getDefaultContext();
        if (context == null) {
            sendJson(exchange, 503, Map.of("error", "ServiceUnavailable", "message", "No game context"));
            return;
        }

        switch (action) {
            case "save" -> handleSave(exchange, context);
            case "load" -> handleLoad(exchange, context);
            default -> sendJson(exchange, 500, Map.of("error", "InternalError"));
        }
    }

    private void handleSave(HttpExchange exchange, GameContext context) throws Exception {
        // Trigger the save phase via the game engine
        SwingUtilities.invokeLater(() -> {
            try {
                context.processPhase("SaveGame");
                logger.info("Save game phase triggered");
            } catch (Exception e) {
                logger.error("Error saving game", e);
            }
        });

        sendJson(exchange, 200, Map.of("accepted", true, "action", "save"));
    }

    private void handleLoad(HttpExchange exchange, GameContext context) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                context.processPhase("LoadSavedGame");
                logger.info("Load game phase triggered");
            } catch (Exception e) {
                logger.error("Error loading game", e);
            }
        });

        sendJson(exchange, 200, Map.of("accepted", true, "action", "load"));
    }
}
