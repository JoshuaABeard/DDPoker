/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.donohoedigital.games.poker.protocol.dto.GameConfig;
import com.donohoedigital.games.poker.protocol.dto.GameSummary;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.SwingUtilities;
import java.net.URI;
import java.util.Map;

/**
 * POST /online/host — create a game on the central server and navigate to Lobby.Host.
 *
 * <p>Requires a prior successful {@code POST /online/login}.
 *
 * <p>Request body: a JSON-serialized {@link GameConfig}. Minimum valid body:
 * <pre>
 * {
 *   "maxPlayers": 4,
 *   "maxOnlinePlayers": 4,
 *   "startingChips": 1500,
 *   "blindStructure": [{"smallBlind":10,"bigBlind":20,"ante":0,"minutes":5,"isBreak":false,
 *                        "gameType":"NOLIMIT_HOLDEM"}],
 *   "doubleAfterLastLevel": true,
 *   "practiceConfig": {"aiActionDelayMs":0,"handResultPauseMs":100,"autoDeal":true}
 * }
 * </pre>
 *
 * <p>Response on success:
 * <pre>{"gameId":"abc123","wsUrl":"ws://localhost:19877/ws/games/abc123"}</pre>
 */
class OnlineHostHandler extends BaseHandler {

    OnlineHostHandler(String apiKey) {
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
        GameConfig config = MAPPER.readValue(body.isBlank() ? "{}" : body, GameConfig.class);

        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());

        GameSummary summary;
        try {
            summary = restClient.createGame(config);
        } catch (RestGameClient.RestGameClientException ex) {
            sendJson(exchange, 502, Map.of("error", "CreateGameFailed", "message", ex.getMessage()));
            return;
        }

        String gameId = summary.gameId();
        String wsUrl  = summary.wsUrl();

        URI wsUri   = URI.create(wsUrl);
        String host = wsUri.getHost();
        int port    = wsUri.getPort();
        String jwt  = auth.getCachedJwt();

        SwingUtilities.invokeAndWait(() -> {
            PokerGame game = (PokerGame) PokerMain.getPokerMain().getDefaultContext().getGame();
            if (game == null) return;
            game.setWebSocketConfig(gameId, jwt, host, port);
            PokerMain.getPokerMain().getDefaultContext().processPhase("Lobby.Host");
        });

        sendJson(exchange, 200, Map.of("gameId", gameId, "wsUrl", wsUrl));
    }
}
