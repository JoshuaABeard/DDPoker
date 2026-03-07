/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.config.Prefs;
import com.donohoedigital.games.config.EngineConstants;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.online.OnlineServerUrl;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.util.Map;

/**
 * POST /online/login — authenticate against the central game server.
 *
 * <p>Request body:
 * <pre>{"serverUrl":"http://localhost:19877","username":"alice","password":"secret"}</pre>
 *
 * <p>Response on success:
 * <pre>{"success":true,"emailVerified":true}</pre>
 *
 * <p>Response on failure:
 * <pre>{"success":false,"error":"Invalid credentials"}</pre>
 *
 * <p>Side effects:
 * <ul>
 *   <li>JWT is cached in {@code RestAuthClient.getInstance()}</li>
 *   <li>{@code EngineConstants.OPTION_ONLINE_SERVER} written to user prefs so the
 *       native UI uses the same server URL</li>
 * </ul>
 */
class OnlineLoginHandler extends BaseHandler {

    OnlineLoginHandler(String apiKey) {
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

        if (serverUrl == null || username == null || password == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest",
                    "message", "serverUrl, username, and password are required"));
            return;
        }

        String normalizedUrl = OnlineServerUrl.normalizeBaseUrl(serverUrl);
        if (normalizedUrl == null) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "Invalid serverUrl"));
            return;
        }

        try {
            LoginResponse resp =
                    RestAuthClient.getInstance().login(normalizedUrl, username, password);

            // Return 200 so callers can always parse the JSON body to check .success
            // rather than handling both HTTP status codes and JSON simultaneously.
            sendJson(exchange, 200, Map.of(
                    "success", true,
                    "emailVerified", resp.profile() != null && resp.profile().emailVerified()));

            // Best-effort: write server URL to prefs so the native UI sees it.
            // Failure here is non-fatal since the JWT is already cached.
            String node = Prefs.NODE_OPTIONS + PokerMain.getPokerMain().getPrefsNodeName();
            Prefs.getUserPrefs(node).put(EngineConstants.OPTION_ONLINE_SERVER, normalizedUrl);
        } catch (RestAuthClient.RestAuthException ex) {
            sendJson(exchange, 200, Map.of(
                    "success", false,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Login failed"));
        }
    }
}
