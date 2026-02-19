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
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.*;
import java.util.Map;

/**
 * {@code POST /game/start} — launches a new practice game.
 * <p>
 * Request body (all fields optional; defaults shown):
 * <pre>
 * {
 *   "numPlayers": 6,
 *   "buyinChips": 1500,
 *   "blindLevels": [
 *     {"small": 25,  "big": 50,  "ante": 0, "minutes": 15},
 *     {"small": 50,  "big": 100, "ante": 0, "minutes": 15},
 *     {"small": 100, "big": 200, "ante": 25,"minutes": 15}
 *   ],
 *   "rebuys": false,
 *   "addons": false
 * }
 * </pre>
 * <p>
 * Returns {@code {"accepted": true}} immediately; the game phase chain starts
 * asynchronously. Poll {@code GET /state} to see when the hand is live.
 */
class GameStartHandler extends BaseHandler {

    private static final int DEFAULT_NUM_PLAYERS = 6;
    private static final int DEFAULT_BUYIN_CHIPS = 1500;

    GameStartHandler(String apiKey) {
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

        JsonNode json;
        try {
            json = MAPPER.readTree(body);
        } catch (Exception e) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid JSON: " + e.getMessage()));
            return;
        }

        TournamentProfile profile = buildProfile(json);

        SwingUtilities.invokeLater(() -> {
            try {
                PokerMain main = PokerMain.getPokerMain();
                if (main == null) {
                    logger.warn("Cannot start game: PokerMain not initialized");
                    return;
                }
                GameContext context = main.getDefaultContext();
                if (context == null) {
                    logger.warn("Cannot start game: no default context");
                    return;
                }
                GameEngine engine = GameEngine.getGameEngine();

                PokerGame game = TournamentOptions.setupPracticeGame(engine, context);
                game.initTournament(profile);
                context.processPhase("InitializeTournamentGame");
                logger.info("Practice game started with {} players, {} buyin chips",
                        profile.getNumPlayers(), profile.getBuyinChips());
            } catch (Exception e) {
                logger.error("Error starting practice game", e);
            }
        });

        sendJson(exchange, 200, Map.of("accepted", true));
    }

    private TournamentProfile buildProfile(JsonNode json) {
        TournamentProfile profile = new TournamentProfile("API Practice");

        int numPlayers = getInt(json, "numPlayers", DEFAULT_NUM_PLAYERS);
        int buyinChips = getInt(json, "buyinChips", DEFAULT_BUYIN_CHIPS);

        profile.setNumPlayers(numPlayers);
        profile.setBuyinChips(buyinChips);
        profile.setRebuys(getBool(json, "rebuys", false));
        profile.setAddons(getBool(json, "addons", false));

        // Set default player type (AI difficulty)
        try {
            PlayerType defaultType = PlayerType.getDefaultProfile();
            if (defaultType != null) {
                profile.setPlayerTypePercent(defaultType.getUniqueKey(), 100);
            }
        } catch (Exception e) {
            logger.warn("Could not load default player type; AI opponents will use default settings", e);
        }

        // Blind levels (1-indexed in TournamentProfile)
        if (json.has("blindLevels") && json.get("blindLevels").isArray()) {
            JsonNode levels = json.get("blindLevels");
            for (int i = 0; i < levels.size(); i++) {
                JsonNode level = levels.get(i);
                int small   = getInt(level, "small",   25 * (1 << i));
                int big     = getInt(level, "big",     50 * (1 << i));
                int ante    = getInt(level, "ante",    0);
                int minutes = getInt(level, "minutes", 15);
                profile.setLevel(i + 1, ante, small, big, minutes);
            }
        } else {
            // Default 3-level structure
            profile.setLevel(1, 0,  25,  50, 15);
            profile.setLevel(2, 0,  50, 100, 15);
            profile.setLevel(3, 25, 100, 200, 15);
        }

        profile.fixAll();
        return profile;
    }

    private int getInt(JsonNode node, String field, int defaultValue) {
        return node.has(field) ? node.get(field).asInt(defaultValue) : defaultValue;
    }

    private boolean getBool(JsonNode node, String field, boolean defaultValue) {
        return node.has(field) ? node.get(field).asBoolean(defaultValue) : defaultValue;
    }
}
