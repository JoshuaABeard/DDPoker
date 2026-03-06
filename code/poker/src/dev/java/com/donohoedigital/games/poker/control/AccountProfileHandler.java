/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.protocol.dto.ProfileResponse;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * GET /account/profile — get the current user's profile.
 *
 * <p>Response:
 * <pre>{"username":"alice","email":"a@b.c","emailVerified":true,...}</pre>
 */
class AccountProfileHandler extends BaseHandler {

    AccountProfileHandler(String apiKey) {
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

        String serverUrl = auth.getCachedServerUrl();
        String jwt = auth.getCachedJwt();

        try {
            ProfileResponse profile = auth.getCurrentUser(serverUrl, jwt);
            sendJson(exchange, 200, Map.of(
                    "id", profile.id(),
                    "username", profile.username(),
                    "email", profile.email(),
                    "emailVerified", profile.emailVerified(),
                    "admin", profile.admin(),
                    "retired", profile.retired(),
                    "createDate", profile.createDate() != null ? profile.createDate() : ""));
        } catch (RestAuthClient.RestAuthException ex) {
            sendJson(exchange, 502, Map.of("error", "GetProfileFailed", "message", ex.getMessage()));
        }
    }
}
