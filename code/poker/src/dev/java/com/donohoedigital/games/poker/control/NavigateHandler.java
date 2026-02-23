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

import com.donohoedigital.games.config.GamePhases;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.PokerMain;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.Map;

/**
 * {@code POST /navigate} — programmatic menu navigation via phase transitions.
 *
 * <p>Request body: {@code {"phase": "PhaseName"}}
 *
 * <p>Common phases:
 * <ul>
 *   <li>{@code "MainMenu"} — return to main menu</li>
 *   <li>{@code "Practice"} — open practice mode</li>
 *   <li>{@code "Analysis"} — open statistics viewer</li>
 *   <li>{@code "PokerNight"} — open poker clock</li>
 *   <li>{@code "HandSimulator"} — open calculator</li>
 *   <li>{@code "GamePrefs"} — open preferences</li>
 *   <li>{@code "SupportDialog"} — open support</li>
 *   <li>{@code "HelpMenu"} — open help</li>
 * </ul>
 */
class NavigateHandler extends BaseHandler {

    NavigateHandler(String apiKey) {
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

        String phase = json.has("phase") ? json.get("phase").asText().trim() : "";
        if (phase.isEmpty()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "phase is required"));
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

        // Validate the phase name synchronously before dispatching.
        // Return 200 with success:false rather than 404 so that curl without -f
        // still captures the JSON error body (curl exits non-zero on 4xx, making
        // $(...) capture empty in the test scripts).
        GameEngine engine = context.getGameEngine();
        GamePhases phases = engine.getGamedefconfig().getGamePhases();
        if (!phases.containsKey(phase)) {
            sendJson(exchange, 200, Map.of("error", "NotFound", "message", "Unknown phase: " + phase,
                    "success", false));
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                context.processPhase(phase);
                logger.info("Navigated to phase: {}", phase);
            } catch (Exception e) {
                logger.error("Error navigating to phase: {}", phase, e);
            }
        });

        sendJson(exchange, 200, Map.of("success", true, "phase", phase));
    }
}
