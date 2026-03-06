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
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /online/lobby/kick — kick a player from the lobby (host only).
 *
 * <p>Request body:
 * <pre>{"profileId":42}</pre>
 *
 * <p>Response on success:
 * <pre>{"kicked":true}</pre>
 */
class OnlineLobbyKickHandler extends BaseHandler {

    OnlineLobbyKickHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }

        RestAuthClient auth = RestAuthClient.getInstance();
        if (!auth.hasSession()) {
            sendJson(exchange, 401, Map.of("error", "Unauthorized",
                    "message", "No cached session. Call POST /online/login first."));
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

        String body = readRequestBodyAsString(exchange);
        JsonNode req = MAPPER.readTree(body.isBlank() ? "{}" : body);
        long profileId = req.path("profileId").asLong(-1);

        if (profileId < 0) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "profileId is required"));
            return;
        }

        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());
        try {
            restClient.kickPlayer(config.gameId(), profileId);
            sendJson(exchange, 200, Map.of("kicked", true));
        } catch (RestGameClient.RestGameClientException ex) {
            sendJson(exchange, 502, Map.of("error", "KickFailed", "message", ex.getMessage()));
        }
    }
}
