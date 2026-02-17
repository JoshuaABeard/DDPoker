/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.gameserver.ActionRequest;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.ServerPlayerSession;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for game connections.
 *
 * <p>
 * Handles the full lifecycle of a player's WebSocket session: authentication
 * via JWT token in query string, joining or reconnecting to a game, message
 * routing via {@link InboundMessageRouter}, and clean disconnection handling.
 *
 * <p>
 * Session-to-connection mapping is tracked internally using a
 * {@link ConcurrentHashMap} keyed by WebSocket session ID.
 */
public class GameWebSocketHandler extends TextWebSocketHandler {

    private static final int SESSION_MAX_TEXT_BUFFER_SIZE = 8192;

    private final JwtTokenProvider jwtTokenProvider;
    private final GameInstanceManager gameInstanceManager;
    private final GameConnectionManager connectionManager;
    private final InboundMessageRouter inboundMessageRouter;
    private final OutboundMessageConverter converter;
    private final ObjectMapper objectMapper;

    /** Maps WebSocket session ID → PlayerConnection */
    private final ConcurrentHashMap<String, PlayerConnection> sessionConnections = new ConcurrentHashMap<>();

    /**
     * Maps game ID → single broadcaster per game (created lazily on first
     * connection)
     */
    private final ConcurrentHashMap<String, GameEventBroadcaster> gameBroadcasters = new ConcurrentHashMap<>();

    /**
     * Creates the WebSocket handler.
     *
     * @param jwtTokenProvider
     *            JWT token provider for authentication
     * @param gameInstanceManager
     *            manager for game instances
     * @param connectionManager
     *            manager for tracking player connections
     * @param inboundMessageRouter
     *            router for inbound client messages
     * @param converter
     *            converter for outbound server messages
     * @param objectMapper
     *            JSON object mapper
     */
    public GameWebSocketHandler(JwtTokenProvider jwtTokenProvider, GameInstanceManager gameInstanceManager,
            GameConnectionManager connectionManager, InboundMessageRouter inboundMessageRouter,
            OutboundMessageConverter converter, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.gameInstanceManager = gameInstanceManager;
        this.connectionManager = connectionManager;
        this.inboundMessageRouter = inboundMessageRouter;
        this.converter = converter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(new CloseStatus(4001, "Invalid request URI"));
            return;
        }

        // Extract token from query string
        String token = extractToken(uri.getQuery());
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.close(new CloseStatus(4001, "Invalid token"));
            return;
        }

        long profileId = jwtTokenProvider.getProfileIdFromToken(token);
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Extract gameId from URI path (last segment of /ws/games/{gameId})
        String gameId = extractGameId(uri.getPath());
        if (gameId == null) {
            session.close(new CloseStatus(4004, "Game not found"));
            return;
        }

        // Look up the game
        GameInstance game = gameInstanceManager.getGame(gameId);
        if (game == null) {
            session.close(new CloseStatus(4004, "Game not found"));
            return;
        }

        GameInstanceState state = game.getState();
        boolean alreadyInGame = game.hasPlayer(profileId);

        if (state == GameInstanceState.WAITING_FOR_PLAYERS && !alreadyInGame) {
            // Auto-join player
            game.addPlayer(profileId, username, false, 0);
        } else if ((state == GameInstanceState.IN_PROGRESS || state == GameInstanceState.PAUSED) && alreadyInGame) {
            // Reconnect existing player — handled below
        } else if (!alreadyInGame) {
            session.close(new CloseStatus(4003, "Not in game"));
            return;
        }

        // Set session buffer size
        session.setTextMessageSizeLimit(SESSION_MAX_TEXT_BUFFER_SIZE);

        // Create player connection
        PlayerConnection playerConnection = new PlayerConnection(session, profileId, username, gameId, objectMapper);
        sessionConnections.put(session.getId(), playerConnection);

        // Wire message sender to player session so ACTION_REQUIRED events reach client
        ServerPlayerSession playerSession = game.getPlayerSessions().get(profileId);
        if (playerSession != null) {
            playerSession.setMessageSender(obj -> {
                if (obj instanceof ActionRequest actionRequest) {
                    ServerMessage actionMsg = converter.createActionRequiredMessage(gameId, actionRequest.options(),
                            30);
                    playerConnection.sendMessage(actionMsg);
                }
            });
        }

        // Register connection
        connectionManager.addConnection(gameId, profileId, playerConnection);

        // Wire event bus broadcaster — create once per game, not once per connection.
        // computeIfAbsent ensures only the first connecting player creates the
        // broadcaster;
        // subsequent connections reuse it, so no player loses their event stream.
        if (game.getEventBus() != null) {
            gameBroadcasters.computeIfAbsent(gameId, id -> {
                GameEventBroadcaster broadcaster = new GameEventBroadcaster(id, connectionManager, converter);
                game.getEventBus().setBroadcastCallback(broadcaster);
                return broadcaster;
            });
        }

        // If reconnecting, notify game
        if (alreadyInGame && (state == GameInstanceState.IN_PROGRESS || state == GameInstanceState.PAUSED)) {
            game.reconnectPlayer(profileId);
        }

        // Send CONNECTED message (null snapshot; full state comes from events)
        ServerMessage connectedMsg = converter.createConnectedMessage(gameId, profileId, null);
        playerConnection.sendMessage(connectedMsg);

        // Broadcast PLAYER_JOINED to other players
        ServerMessage joinedMsg = converter.createPlayerJoinedMessage(gameId, profileId, username, -1);
        connectionManager.broadcastToGame(gameId, joinedMsg, profileId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        PlayerConnection connection = findConnection(session);
        if (connection != null) {
            inboundMessageRouter.handleMessage(connection, message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        PlayerConnection connection = findConnection(session);
        if (connection != null) {
            sessionConnections.remove(session.getId());
            connectionManager.removeConnection(connection.getGameId(), connection.getProfileId());

            GameInstance game = gameInstanceManager.getGame(connection.getGameId());
            if (game != null) {
                game.removePlayer(connection.getProfileId());
            }

            // Broadcast PLAYER_DISCONNECTED when game is in progress (player may
            // reconnect),
            // or PLAYER_LEFT when game is not in progress (player truly left).
            GameInstanceState state = game != null ? game.getState() : null;
            boolean reconnectable = state == GameInstanceState.IN_PROGRESS || state == GameInstanceState.PAUSED;
            ServerMessage leftMsg = reconnectable
                    ? converter.createPlayerDisconnectedMessage(connection.getGameId(), connection.getProfileId(),
                            connection.getUsername())
                    : converter.createPlayerLeftMessage(connection.getGameId(), connection.getProfileId(),
                            connection.getUsername());
            connectionManager.broadcastToGame(connection.getGameId(), leftMsg);

            // Clean up per-game broadcaster when the last connection closes
            if (connectionManager.getConnections(connection.getGameId()).isEmpty()) {
                gameBroadcasters.remove(connection.getGameId());
            }
        }
    }

    private PlayerConnection findConnection(WebSocketSession session) {
        return sessionConnections.get(session.getId());
    }

    /**
     * Extracts the token value from a URI query string.
     *
     * @param query
     *            query string (e.g. "token=xxx&other=y")
     * @return token value or null if not found
     */
    private String extractToken(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                String value = param.substring("token=".length());
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * Extracts the game ID from a URI path like "/ws/games/{gameId}".
     *
     * @param path
     *            URI path
     * @return game ID (last path segment) or null if path is invalid
     */
    private String extractGameId(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        // Strip trailing slash
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            return null;
        }
        String segment = path.substring(lastSlash + 1);
        return segment.isEmpty() ? null : segment;
    }
}
