/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * PUT /account/email — request an email address change.
 *
 * <p>Request body:
 * <pre>{"newEmail":"new@email.com"}</pre>
 *
 * <p>Response on success:
 * <pre>{"requested":true}</pre>
 */
class AccountEmailHandler extends BaseHandler {

    AccountEmailHandler(String apiKey) {
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

        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body required"));
            return;
        }

        JsonNode req = MAPPER.readTree(body);
        String newEmail = req.path("newEmail").asText(null);

        if (newEmail == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "newEmail is required"));
            return;
        }

        String serverUrl = auth.getCachedServerUrl();

        try {
            auth.requestEmailChange(serverUrl, newEmail);
            sendJson(exchange, 200, Map.of("requested", true));
        } catch (RestAuthClient.RestAuthException ex) {
            sendJson(exchange, 200, Map.of(
                    "requested", false,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Email change request failed"));
        }
    }
}
