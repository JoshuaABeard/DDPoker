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
import com.donohoedigital.games.poker.protocol.dto.GameSettingsRequest;
import com.donohoedigital.games.poker.protocol.dto.GameSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * PUT /online/lobby/settings — update game settings in the lobby (host only).
 *
 * <p>Request body:
 * <pre>{"name":"New Name","maxPlayers":6}</pre>
 *
 * <p>Response on success:
 * <pre>{"updated":true,"game":{...}}</pre>
 */
class OnlineLobbySettingsHandler extends BaseHandler {

    OnlineLobbySettingsHandler(String apiKey) {
        super(apiKey);
    }

    @Override
    protected void handleAuthenticated(HttpExchange exchange) throws Exception {
        if (!"PUT".equals(exchange.getRequestMethod())) {
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
        String name = req.has("name") ? req.path("name").asText(null) : null;
        Integer maxPlayers = req.has("maxPlayers") ? req.path("maxPlayers").asInt() : null;

        GameSettingsRequest settings = new GameSettingsRequest(name, maxPlayers, null, null);

        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());
        try {
            GameSummary updated = restClient.updateSettings(config.gameId(), settings);
            sendJson(exchange, 200, Map.of("updated", true, "game", updated));
        } catch (RestGameClient.RestGameClientException ex) {
            sendJson(exchange, 502, Map.of("error", "UpdateSettingsFailed", "message", ex.getMessage()));
        }
    }
}
