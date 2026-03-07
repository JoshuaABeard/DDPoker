/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.SwingUtilities;
import java.util.Map;

/**
 * POST /online/start — start the hosted game and navigate to InitializeOnlineGame.
 *
 * <p>Skips the HostStart countdown for test speed.
 *
 * <p>Response:
 * <pre>{"started":true}</pre>
 */
class OnlineStartHandler extends BaseHandler {

    OnlineStartHandler(String apiKey) {
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
        if (game == null || game.getWebSocketConfig() == null) {
            sendJson(exchange, 409, Map.of("error", "Conflict", "message", "No game in lobby"));
            return;
        }

        String gameId = game.getWebSocketConfig().gameId();
        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());

        // Navigate to InitializeOnlineGame FIRST — this triggers the phase chain
        // that eventually connects the WebSocket via WebSocketTournamentDirector.
        // We must wait for the WebSocket to connect before starting the server-side
        // game director, otherwise ACTION_REQUIRED messages fire before the client
        // is connected and the director auto-folds the human player.
        SwingUtilities.invokeAndWait(() ->
                PokerMain.getPokerMain().getDefaultContext().processPhase("InitializeOnlineGame"));

        // Poll until the WebSocketTournamentDirector has set its PlayerActionListener,
        // which indicates the WebSocket is connected and ready to receive game events.
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            if (game.getPlayerActionListener() != null) break;
            Thread.sleep(100);
        }

        try {
            restClient.startGame(gameId);
        } catch (RestGameClient.RestGameClientException ex) {
            sendJson(exchange, 502, Map.of("error", "StartGameFailed", "message", ex.getMessage()));
            return;
        }

        sendJson(exchange, 200, Map.of("started", true));
    }
}
