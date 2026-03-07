/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.control;

import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.poker.ClientTournamentProfile;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.donohoedigital.games.poker.PokerClientConstants;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerMain;
import com.donohoedigital.games.poker.ai.PlayerType;
import com.donohoedigital.games.poker.online.ClientPlayer;
import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.donohoedigital.games.poker.protocol.dto.GameConfig;
import com.donohoedigital.games.poker.protocol.dto.GameConfig.BlindLevel;
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
            PokerMain main = PokerMain.getPokerMain();
            if (main == null) return;
            GameContext context = main.getDefaultContext();
            if (context == null) return;

            PokerGame game = (PokerGame) context.getGame();
            if (game == null) {
                // No game exists yet (e.g. API-driven flow that skips TournamentOptions).
                // Create one so we have somewhere to store the WebSocketConfig.
                game = (PokerGame) context.createNewGame();
                context.setGame(game);

                // Ensure a human player exists so the lobby/game phases work correctly.
                GameStartHandler.ensureDefaultProfile();
                PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
                if (profile != null) {
                    GameEngine engine = GameEngine.getGameEngine();
                    ClientPlayer player = new ClientPlayer(engine.getPlayerId(), game.getNextPlayerID(), profile, true);
                    player.setPlayerType(PlayerType.getAdvisor());
                    player.setVersion(PokerClientConstants.VERSION);
                    game.addPlayer(player);
                }
            }
            // Build a ClientTournamentProfile from the GameConfig so the UI phases
            // (ShowTournamentTable, etc.) have the blind structure and settings they need.
            if (game.getProfile() == null) {
                ClientTournamentProfile tp = buildProfileFromConfig(config);
                game.initTournament(tp);
            }

            game.setWebSocketConfig(gameId, jwt, host, port);
            context.processPhase("Lobby.Host");
        });

        sendJson(exchange, 200, Map.of("gameId", gameId, "wsUrl", wsUrl));
    }

    private ClientTournamentProfile buildProfileFromConfig(GameConfig config) {
        ClientTournamentProfile tp = new ClientTournamentProfile("Online Game");
        tp.setNumPlayers(config.maxPlayers() > 0 ? config.maxPlayers() : 6);
        tp.setBuyinChips(config.startingChips() > 0 ? config.startingChips() : 1500);

        java.util.List<BlindLevel> blinds = config.blindStructure();
        if (blinds != null) {
            for (int i = 0; i < blinds.size(); i++) {
                BlindLevel bl = blinds.get(i);
                tp.setLevel(i + 1, bl.ante(), bl.smallBlind(), bl.bigBlind(), bl.minutes());
            }
        } else {
            tp.setLevel(1, 0, 25, 50, 15);
            tp.setLevel(2, 0, 50, 100, 15);
            tp.setLevel(3, 25, 100, 200, 15);
        }

        tp.fixAll();
        return tp;
    }
}
