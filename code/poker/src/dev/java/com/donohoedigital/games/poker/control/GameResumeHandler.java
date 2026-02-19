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
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.TournamentOptions;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.donohoedigital.games.poker.server.GameSaveManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles game resume endpoints:
 * <ul>
 *   <li>{@code GET  /game/resumable} — list in-progress or paused games from the embedded server</li>
 *   <li>{@code POST /game/resume}    — reconnect to an existing server-side game by gameId</li>
 * </ul>
 * <p>
 * Resume works by pre-setting the WebSocket configuration on the newly created
 * {@link PokerGame} before the phase chain starts. {@code ShowTournamentTable}
 * detects the pre-set config and skips calling {@code PracticeGameLauncher},
 * so the client connects to the existing server-side game instead of creating
 * a new one.
 */
class GameResumeHandler extends BaseHandler {

    GameResumeHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && path.endsWith("/game/resumable")) {
            handleList(exchange);
        } else if ("POST".equals(method) && path.endsWith("/game/resume")) {
            handleResume(exchange);
        } else {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /game/resumable
    // -------------------------------------------------------------------------

    private void handleList(HttpExchange exchange) throws Exception {
        PokerMain main = PokerMain.getPokerMain();
        if (main == null) {
            sendJson(exchange, 200, Map.of("resumableGames", List.of()));
            return;
        }

        GameSaveManager gsm = main.getGameSaveManager();
        List<GameSummary> games = gsm == null ? List.of() : gsm.getResumableGames();

        List<Map<String, Object>> result = new ArrayList<>();
        for (GameSummary g : games) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("gameId", g.gameId());
            entry.put("name", g.name());
            entry.put("status", g.status());
            entry.put("playerCount", g.playerCount());
            entry.put("maxPlayers", g.maxPlayers());
            if (g.blinds() != null) {
                entry.put("smallBlind", g.blinds().smallBlind());
                entry.put("bigBlind", g.blinds().bigBlind());
            }
            result.add(entry);
        }
        sendJson(exchange, 200, Map.of("resumableGames", result));
    }

    // -------------------------------------------------------------------------
    // POST /game/resume
    // -------------------------------------------------------------------------

    private void handleResume(HttpExchange exchange) throws Exception {
        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body is required"));
            return;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON: " + e.getMessage()));
            return;
        }

        if (!json.has("gameId")) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Missing 'gameId' field"));
            return;
        }

        String gameId = json.get("gameId").asText("");
        if (gameId.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "'gameId' must not be blank"));
            return;
        }

        PokerMain main = PokerMain.getPokerMain();
        if (main == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "Game not initialized"));
            return;
        }

        // Verify the gameId appears in the resumable list
        GameSaveManager gsm = main.getGameSaveManager();
        boolean found = gsm != null && gsm.getResumableGames().stream()
                .anyMatch(g -> gameId.equals(g.gameId()));
        if (!found) {
            sendJson(exchange, 404, Map.of("error", "NotFound",
                    "message", "Game '" + gameId + "' not found in resumable games list"));
            return;
        }

        final String finalGameId = gameId;
        SwingUtilities.invokeLater(() -> {
            try {
                PokerMain m = PokerMain.getPokerMain();
                if (m == null) return;
                GameContext context = m.getDefaultContext();
                if (context == null) return;
                GameEngine engine = GameEngine.getGameEngine();

                // Create a minimal profile so the game object initialises
                TournamentProfile minProfile = new TournamentProfile("Resume");
                minProfile.setNumPlayers(2);
                minProfile.setBuyinChips(1500);
                minProfile.setLevel(1, 0, 25, 50, 15);
                minProfile.setRebuys(false);
                minProfile.setAddons(false);
                minProfile.fixAll();

                // Create game and pre-set the WebSocket config to point to the existing game.
                // ShowTournamentTable.poststart() will skip PracticeGameLauncher when it sees
                // that getWebSocketConfig() is non-null.
                PokerGame game = TournamentOptions.setupPracticeGame(engine, context);
                game.initTournament(minProfile);
                game.setWebSocketConfig(finalGameId, m.getEmbeddedServer().getLocalUserJwt(),
                        m.getEmbeddedServer().getPort());
                context.processPhase("InitializeTournamentGame");
                logger.info("Resuming game {} via embedded server port {}",
                        finalGameId, m.getEmbeddedServer().getPort());
            } catch (Exception e) {
                logger.error("Error resuming game {}", finalGameId, e);
            }
        });

        sendJson(exchange, 200, Map.of("accepted", true, "gameId", gameId));
    }
}
