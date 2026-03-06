/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.donohoedigital.games.poker.protocol.dto.GameSummary;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * GET /online/games — list available games on the server.
 *
 * <p>Response:
 * <pre>{"games":[...]}</pre>
 */
class OnlineGamesHandler extends BaseHandler {

    OnlineGamesHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        RestAuthClient auth = RestAuthClient.getInstance();
        if (!auth.hasSession()) {
            sendJson(exchange, 401, Map.of("error", "Unauthorized",
                    "message", "No cached session. Call POST /online/login first."));
            return;
        }

        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());
        try {
            List<GameSummary> games = restClient.listGames();
            sendJson(exchange, 200, Map.of("games", games));
        } catch (RestGameClient.RestGameClientException ex) {
            sendJson(exchange, 502, Map.of("error", "ListGamesFailed", "message", ex.getMessage()));
        }
    }
}
