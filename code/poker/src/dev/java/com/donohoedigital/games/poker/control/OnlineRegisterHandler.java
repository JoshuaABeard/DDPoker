/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.online.OnlineServerUrl;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /online/register — register a new user on the game server.
 *
 * <p>Request body:
 * <pre>{"serverUrl":"http://localhost:19877","username":"alice","password":"secret","email":"a@b.c"}</pre>
 *
 * <p>Response on success:
 * <pre>{"success":true,"token":"jwt..."}</pre>
 *
 * <p>Response on failure:
 * <pre>{"success":false,"error":"Registration failed"}</pre>
 */
class OnlineRegisterHandler extends BaseHandler {

    OnlineRegisterHandler(String apiKey) {
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

        JsonNode req = MAPPER.readTree(body);
        String serverUrl = req.path("serverUrl").asText(null);
        String username = req.path("username").asText(null);
        String password = req.path("password").asText(null);
        String email = req.path("email").asText(null);

        if (serverUrl == null || username == null || password == null || email == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "serverUrl, username, password, and email are required"));
            return;
        }

        String normalizedUrl = OnlineServerUrl.normalizeBaseUrl(serverUrl);
        if (normalizedUrl == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid serverUrl"));
            return;
        }

        try {
            LoginResponse resp =
                    RestAuthClient.getInstance().register(normalizedUrl, username, password, email);

            sendJson(exchange, 200, Map.of(
                    "success", true,
                    "token", resp.token() != null ? resp.token() : ""));
        } catch (RestAuthClient.RestAuthException ex) {
            sendJson(exchange, 200, Map.of(
                    "success", false,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Registration failed"));
        }
    }
}
