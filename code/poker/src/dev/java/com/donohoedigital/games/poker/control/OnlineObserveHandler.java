/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.donohoedigital.games.poker.protocol.dto.GameJoinResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import javax.swing.SwingUtilities;
import java.net.URI;
import java.util.Map;

/**
 * POST /online/observe — start observing a game as a spectator.
 *
 * <p>Request body:
 * <pre>{"gameId":"abc123"}</pre>
 *
 * <p>Response on success:
 * <pre>{"gameId":"abc123","wsUrl":"ws://..."}</pre>
 */
class OnlineObserveHandler extends BaseHandler {

    OnlineObserveHandler(String apiKey) {
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
        JsonNode req = MAPPER.readTree(body.isBlank() ? "{}" : body);
        String gameId = req.path("gameId").asText(null);

        if (gameId == null || gameId.isBlank()) {
            sendJson(exchange, 400, Map.of("error", "BadRequest", "message", "gameId is required"));
            return;
        }

        RestGameClient restClient = new RestGameClient(auth.getCachedServerUrl(), auth.getCachedJwt());
        GameJoinResponse resp;
        try {
            resp = restClient.observeGame(gameId);
        } catch (RestGameClient.RestGameClientException ex) {
            sendJson(exchange, 502, Map.of("error", "ObserveFailed", "message", ex.getMessage()));
            return;
        }

        URI wsUri = URI.create(resp.wsUrl());
        String host = wsUri.getHost();
        int port = wsUri.getPort();
        String jwt = auth.getCachedJwt();

        SwingUtilities.invokeAndWait(() -> {
            PokerMain main = PokerMain.getPokerMain();
            if (main == null) return;
            GameContext context = main.getDefaultContext();
            if (context == null) return;

            PokerGame game = (PokerGame) context.getGame();
            if (game == null) {
                game = (PokerGame) context.createNewGame();
                context.setGame(game);

                GameStartHandler.ensureDefaultProfile();
                PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
                if (profile != null) {
                    GameEngine engine = GameEngine.getGameEngine();
                    ClientPlayer player = new ClientPlayer(engine.getPlayerId(), game.getNextPlayerID(), profile, true);
                    player.setPlayerType(PlayerType.getAdvisor());
                    game.addPlayer(player);
                }
            }
            game.setWebSocketConfig(resp.gameId(), jwt, host, port);
            context.processPhase("InitializeOnlineGame");
        });

        sendJson(exchange, 200, Map.of("gameId", resp.gameId(), "wsUrl", resp.wsUrl()));
    }
}
