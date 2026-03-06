/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.protocol.dto.ProfileResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /account/password — change the user's password.
 *
 * <p>Request body:
 * <pre>{"oldPassword":"old","newPassword":"new"}</pre>
 *
 * <p>Response on success:
 * <pre>{"changed":true}</pre>
 */
class AccountPasswordHandler extends BaseHandler {

    AccountPasswordHandler(String apiKey) {
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

        String body = readRequestBodyAsString(exchange);
        if (body.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Request body required"));
            return;
        }

        JsonNode req = MAPPER.readTree(body);
        String oldPassword = req.path("oldPassword").asText(null);
        String newPassword = req.path("newPassword").asText(null);

        if (oldPassword == null || newPassword == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "oldPassword and newPassword are required"));
            return;
        }

        String serverUrl = auth.getCachedServerUrl();
        String jwt = auth.getCachedJwt();

        // Get the profile ID from the current user
        ProfileResponse profile = auth.getCurrentUser(serverUrl, jwt);

        try {
            auth.changePassword(serverUrl, jwt, profile.id(), oldPassword, newPassword);
            sendJson(exchange, 200, Map.of("changed", true));
        } catch (RestAuthClient.RestAuthException ex) {
            sendJson(exchange, 200, Map.of(
                    "changed", false,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Password change failed"));
        }
    }
}
