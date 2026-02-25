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
import com.donohoedigital.games.engine.DialogPhase;
import com.donohoedigital.games.engine.GameManager;
import com.donohoedigital.games.engine.Phase;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.online.PokerDirector;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code POST /control} — game flow control commands.
 * <p>
 * Request body (JSON):
 * <pre>
 * {"action": "PAUSE"}
 * {"action": "RESUME"}
 * {"action": "PHASE", "phase": "StartMenu"}
 * </pre>
 * <p>
 * Supported actions:
 * <ul>
 *   <li>{@code PAUSE}  — pause the tournament (equivalent to F5)</li>
 *   <li>{@code RESUME} — resume the tournament</li>
 *   <li>{@code PHASE, phase: "<phaseName>"} — transition to a named game phase;
 *       any phase name is accepted in dev builds</li>
 * </ul>
 */
class ControlHandler extends BaseHandler {

    // Known-safe phases for reference (not enforced in dev builds — any phase is accepted).
    // private static final List<String> SAFE_PHASES = List.of("StartMenu", "GamePrefs", "GamePrefsDialog");

    ControlHandler(String apiKey) {
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
        if (!json.has("action")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'action' field"));
            return;
        }

        String action = json.get("action").asText("").toUpperCase();

        switch (action) {
            case "PAUSE" -> handlePause(exchange, true);
            case "RESUME" -> handlePause(exchange, false);
            case "CLOSE_TOURNAMENT_OVER", "CLOSE_GAME_OVER" -> handleCloseTournamentOver(exchange, action);
            case "PHASE" -> {
                String phase = json.has("phase") ? json.get("phase").asText("") : "";
                handlePhase(exchange, phase);
            }
            default -> sendJson(exchange, 400, Map.of(
                    "error", "BadRequest",
                    "message", "Unknown action: " + action
                            + ". Valid values: PAUSE, RESUME, PHASE, CLOSE_TOURNAMENT_OVER"));
        }
    }

    private void handlePause(HttpExchange exchange, boolean pause) throws Exception {
        SwingUtilities.invokeLater(() -> {
            PokerDirector director = getDirector();
            if (director != null) {
                director.setPaused(pause);
            }
        });
        sendJson(exchange, 200, Map.of("accepted", true, "action", pause ? "PAUSE" : "RESUME"));
    }

    private void handlePhase(HttpExchange exchange, String phase) throws Exception {
        if (phase.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'phase' field"));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            PokerMain main = PokerMain.getPokerMain();
            if (main != null && main.getDefaultContext() != null) {
                main.getDefaultContext().processPhase(phase);
            }
        });
        sendJson(exchange, 200, Map.of("accepted", true, "action", "PHASE", "phase", phase));
    }

    private void handleCloseTournamentOver(HttpExchange exchange, String action) throws Exception {
        AtomicReference<Map<String, Object>> resultRef = new AtomicReference<>(Map.of());
        SwingUtilities.invokeAndWait(() -> resultRef.set(closeTournamentOverOnEdt(action)));
        Map<String, Object> result = resultRef.get();
        boolean accepted = Boolean.TRUE.equals(result.get("accepted"));
        sendJson(exchange, accepted ? 200 : 409, result);
    }

    private Map<String, Object> closeTournamentOverOnEdt(String action) {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null || main.getDefaultContext() == null) {
            return Map.of(
                    "accepted", false,
                    "action", action,
                    "error", "Conflict",
                    "message", "Game context is not initialized");
        }

        GameContext context = main.getDefaultContext();
        Phase uiPhase = context.getCurrentUIPhase();
        String uiPhaseClass = uiPhase != null ? uiPhase.getClass().getSimpleName() : "NONE";
        String uiPhaseName = uiPhase != null && uiPhase.getGamePhase() != null
                ? uiPhase.getGamePhase().getName()
                : "NONE";

        boolean looksGameOver = containsIgnoreCase(uiPhaseClass, "gameover")
                || containsIgnoreCase(uiPhaseName, "gameover");
        if (!looksGameOver) {
            return Map.of(
                    "accepted", false,
                    "action", action,
                    "error", "Conflict",
                    "message", "Current UI phase is not a tournament-over dialog",
                    "uiPhaseClass", uiPhaseClass,
                    "uiPhaseName", uiPhaseName);
        }

        if (uiPhase instanceof DialogPhase dialogPhase) {
            dialogPhase.removeDialog();
        }
        context.processPhase("StartMenu");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accepted", true);
        out.put("action", action);
        out.put("closedPhaseClass", uiPhaseClass);
        out.put("closedPhaseName", uiPhaseName);
        out.put("nextPhase", "StartMenu");
        return out;
    }

    private static boolean containsIgnoreCase(String value, String token) {
        return value != null && token != null && value.toLowerCase().contains(token.toLowerCase());
    }

    private PokerDirector getDirector() {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) return null;
        GameContext context = main.getDefaultContext();
        if (context == null) return null;
        GameManager mgr = context.getGameManager();
        return mgr instanceof PokerDirector pd ? pd : null;
    }
}
