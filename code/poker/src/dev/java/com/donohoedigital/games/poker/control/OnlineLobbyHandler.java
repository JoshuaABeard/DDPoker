/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerGame.WebSocketConfig;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * GET /online/lobby — return current lobby state.
 *
 * <p>Response:
 * <pre>
 * {
 *   "gameId": "abc123",
 *   "phase": "Lobby.Host",
 *   "players": [{"name":"alice","role":"PLAYER"}]
 * }
 * </pre>
 *
 * <p>Returns 404 if no WebSocketConfig is set (no game in progress).
 */
class OnlineLobbyHandler extends BaseHandler {

    OnlineLobbyHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        PokerGame game = (PokerGame) PokerMain.getPokerMain().getDefaultContext().getGame();
        if (game == null) {
            sendJson(exchange, 404, Map.of("error", "NoGame"));
            return;
        }

        WebSocketConfig config = game.getWebSocketConfig();
        if (config == null) {
            sendJson(exchange, 404, Map.of("error", "NoConfig", "message", "No WebSocketConfig — game not in lobby"));
            return;
        }

        String phase = PokerMain.getPokerMain().getDefaultContext().getCurrentPhase().getGamePhase().getName();

        RestAuthClient auth = RestAuthClient.getInstance();
        if (!auth.hasSession()) {
            // Return basic state without player list
            sendJson(exchange, 200, Map.of("gameId", config.gameId(), "phase", phase));
            return;
        }

        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());
        try {
            GameSummary summary = restClient.getGameSummary(config.gameId());
            sendJson(exchange, 200, Map.of(
                    "gameId", config.gameId(),
                    "phase", phase,
                    "players", summary.players() != null ? summary.players() : java.util.List.of()));
        } catch (RestGameClient.RestGameClientException ex) {
            // Return basic state on REST failure
            sendJson(exchange, 200, Map.of(
                    "gameId", config.gameId(),
                    "phase", phase,
                    "players", java.util.List.of()));
        }
    }
}
