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

import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.donohoedigital.games.poker.gameserver.ActionRequest;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.GameInstanceState;
import com.donohoedigital.games.poker.gameserver.GameServerProperties;
import com.donohoedigital.games.poker.gameserver.GameStateSnapshot;
import com.donohoedigital.games.poker.gameserver.ServerGameEventBus;
import com.donohoedigital.games.poker.gameserver.ServerPlayerSession;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.service.AuthService;
import com.donohoedigital.games.poker.gameserver.service.GameService;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData.LobbyPlayerData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;

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

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);
    private static final int SESSION_MAX_TEXT_BUFFER_SIZE = 8192;

    /**
     * Close code sent to the old tab when the same player opens the game in a new
     * tab.
     */
    private static final int CLOSE_CONNECTION_REPLACED = 4409;

    private final JwtTokenProvider jwtTokenProvider;
    private final GameInstanceManager gameInstanceManager;
    private final GameConnectionManager connectionManager;
    private final InboundMessageRouter inboundMessageRouter;
    private final OutboundMessageConverter converter;
    private final ObjectMapper objectMapper;
    private final GameService gameService;
    private final AuthService authService;
    private final int actionTimeoutSeconds;

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
     * @param gameService
     *            game service for lobby state
     * @param authService
     *            auth service for jti tracking and reconnect token generation
     * @param properties
     *            server configuration properties
     */
    public GameWebSocketHandler(JwtTokenProvider jwtTokenProvider, GameInstanceManager gameInstanceManager,
            GameConnectionManager connectionManager, InboundMessageRouter inboundMessageRouter,
            OutboundMessageConverter converter, ObjectMapper objectMapper, GameService gameService,
            AuthService authService, GameServerProperties properties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.gameInstanceManager = gameInstanceManager;
        this.connectionManager = connectionManager;
        this.inboundMessageRouter = inboundMessageRouter;
        this.converter = converter;
        this.objectMapper = objectMapper;
        this.gameService = gameService;
        this.authService = authService;
        this.actionTimeoutSeconds = properties.actionTimeoutSeconds();
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

        // Validate token scope and extract claims
        Claims claims = jwtTokenProvider.getClaims(token);
        String scope = claims.get("scope", String.class);
        long profileId = claims.get("profileId", Long.class);
        String username = claims.getSubject();

        // Extract gameId from URI path (last segment of /ws/games/{gameId})
        String gameId = extractGameId(uri.getPath());
        if (gameId == null) {
            session.close(new CloseStatus(4004, "Game not found"));
            return;
        }

        // Enforce scope restrictions
        if ("reconnect".equals(scope)) {
            // Reconnect tokens must be scoped to this specific game
            String tokenGameId = claims.get("gameId", String.class);
            if (!gameId.equals(tokenGameId)) {
                session.close(new CloseStatus(4001, "Token not valid for this game"));
                return;
            }
        } else if ("ws-connect".equals(scope)) {
            // ws-connect tokens are single-use — check and mark jti
            String jti = claims.getId();
            long expiryMs = claims.getExpiration().getTime();
            if (jti == null || authService.isJtiUsed(jti)) {
                session.close(new CloseStatus(4001, "Token already used"));
                return;
            }
            authService.markJtiUsed(jti, expiryMs);
        } else if (scope != null) {
            // Unknown scope — reject
            session.close(new CloseStatus(4001, "Invalid token scope"));
            return;
        }
        // null scope = legacy regular JWT (desktop client compatibility)

        // Look up the game
        GameInstance game = gameInstanceManager.getGame(gameId);
        if (game == null) {
            session.close(new CloseStatus(4004, "Game not found"));
            return;
        }

        GameInstanceState state = game.getState();
        boolean alreadyInGame = game.hasPlayer(profileId);
        boolean reconnecting = alreadyInGame
                && (state == GameInstanceState.IN_PROGRESS || state == GameInstanceState.PAUSED);
        log.debug("[WS-CONNECT] player={} profileId={} gameId={} state={} alreadyInGame={}", username, profileId,
                gameId, state, alreadyInGame);

        if (state == GameInstanceState.WAITING_FOR_PLAYERS && !alreadyInGame) {
            // Auto-join player
            game.addPlayer(profileId, username, false, 0);
        } else if (reconnecting) {
            // Reconnect: close the existing WebSocket session (if any) with code 4409
            // so the old tab knows it was superseded. Only applies during active gameplay.
            final long fProfileId = profileId;
            PlayerConnection existingConnection = sessionConnections.values().stream()
                    .filter(c -> c.getProfileId() == fProfileId && c.getGameId().equals(gameId)).findFirst()
                    .orElse(null);
            if (existingConnection != null) {
                try {
                    existingConnection.getSession()
                            .close(new CloseStatus(CLOSE_CONNECTION_REPLACED, "Connection replaced by new session"));
                } catch (Exception e) {
                    log.debug("Could not close replaced session for player {} in game {}: {}", profileId, gameId,
                            e.getMessage());
                }
            }
        } else if (!alreadyInGame) {
            log.debug("[WS-CONNECT] rejecting player={} not in game gameId={}", username, gameId);
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
                            actionTimeoutSeconds);
                    playerConnection.sendMessage(actionMsg);
                }
            });
            log.debug("[WS-HANDLER] Wired messageSender for profileId={} gameId={}", profileId, gameId);
        }

        // Register connection (replaces any existing connection for this player)
        connectionManager.addConnection(gameId, profileId, playerConnection);

        // If reconnecting, notify game
        if (reconnecting) {
            game.reconnectPlayer(profileId);
        }

        // Generate a reconnect token (24h, game-scoped) for this player
        String reconnectToken = authService.generateReconnectToken(profileId, username, gameId);

        // Send CONNECTED message (with reconnect token)
        ServerMessage connectedMsg = converter.createConnectedMessage(gameId, profileId, null, reconnectToken);
        playerConnection.sendMessage(connectedMsg);

        if (state == GameInstanceState.WAITING_FOR_PLAYERS) {
            // Lobby phase: send lobby state snapshot to the joining player...
            sendLobbyState(playerConnection, game);
            // ...and broadcast LOBBY_PLAYER_JOINED to all other lobby connections
            LobbyPlayerData playerData = new LobbyPlayerData(profileId, username, profileId == game.getOwnerProfileId(),
                    false, null);
            ServerMessage joinedMsg = ServerMessage.of(ServerMessageType.LOBBY_PLAYER_JOINED, gameId,
                    new ServerMessageData.LobbyPlayerJoinedData(playerData));
            connectionManager.broadcastToGame(gameId, joinedMsg, profileId);
            // Practice game auto-start: all players were pre-added by the REST endpoint;
            // start the game when the owner's WebSocket connects.
            if (alreadyInGame && profileId == game.getOwnerProfileId()) {
                // Pre-create the eventBus and wire the broadcaster BEFORE starting the
                // director. The director runs on a thread pool and fires HandStarted
                // almost immediately; wiring after startGame() risks missing it.
                // game.prepareStart() creates eventBus without starting the director;
                // game.start() (inside startGame) reuses it.
                ServerGameEventBus earlyEventBus = game.prepareStart();
                gameBroadcasters.computeIfAbsent(gameId, id -> {
                    GameEventBroadcaster broadcaster = new GameEventBroadcaster(id, connectionManager, converter, game);
                    if (game.getConfig() != null && game.getConfig().practiceConfig() != null
                            && Boolean.TRUE.equals(game.getConfig().practiceConfig().aiFaceUp())) {
                        broadcaster.setAiFaceUp(true);
                    }
                    earlyEventBus.setBroadcastCallback(broadcaster);
                    return broadcaster;
                });
                gameInstanceManager.startGame(gameId, profileId);

                // Send initial game state snapshot to the client so the table and players
                // are available before ACTION_REQUIRED arrives. Without this, the client's
                // tables_ map stays empty and action buttons never appear.
                GameStateSnapshot snapshot = game.getGameStateSnapshot(profileId);
                if (snapshot != null) {
                    playerConnection.sendMessage(converter.createGameStateMessage(gameId, snapshot));
                    log.debug("[WS-CONNECT] sent initial GAME_STATE to practice game owner player={}", username);
                }
            }
        } else {
            // Wire event bus broadcaster — create once per game, not once per connection.
            // computeIfAbsent ensures only the first connecting player creates the
            // broadcaster; subsequent connections reuse it. For reconnects the eventBus
            // already exists (game is IN_PROGRESS/PAUSED).
            if (game.getEventBus() != null) {
                gameBroadcasters.computeIfAbsent(gameId, id -> {
                    GameEventBroadcaster broadcaster = new GameEventBroadcaster(id, connectionManager, converter, game);
                    if (game.getConfig() != null && game.getConfig().practiceConfig() != null
                            && Boolean.TRUE.equals(game.getConfig().practiceConfig().aiFaceUp())) {
                        broadcaster.setAiFaceUp(true);
                    }
                    game.getEventBus().setBroadcastCallback(broadcaster);
                    return broadcaster;
                });
            }
            // Sync reconnecting player to current game state so they see the table,
            // players, and cards without waiting for the next event to fire.
            GameStateSnapshot snapshot = game.getGameStateSnapshot(profileId);
            log.debug("[WS-CONNECT] in-game reconnect player={} snapshot={}", username,
                    snapshot != null
                            ? "tableId=" + snapshot.tableId() + " players=" + snapshot.players().size()
                            : "null");

            // In-game: broadcast PLAYER_JOINED to others (include tableId so multi-table
            // clients seat the player at the correct table).
            int joinTableId = snapshot != null ? snapshot.tableId() : -1;
            ServerMessage joinedMsg = converter.createPlayerJoinedMessage(gameId, profileId, username, -1, joinTableId);
            connectionManager.broadcastToGame(gameId, joinedMsg, profileId);

            if (snapshot != null) {
                playerConnection.sendMessage(converter.createGameStateMessage(gameId, snapshot));
                log.debug("[WS-CONNECT] sent GAME_STATE to player={}", username);
            }
            // Re-send ACTION_REQUIRED if the game was already waiting for this player.
            game.resendPendingActionIfAny(profileId);
            log.debug("[WS-CONNECT] resendPendingActionIfAny called for player={}", username);
        }
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
            log.debug("[WS-HANDLER] connection closed profileId={} gameId={} status={}", connection.getProfileId(),
                    connection.getGameId(), closeStatus);
            sessionConnections.remove(session.getId());
            connectionManager.removeConnection(connection.getGameId(), connection.getProfileId());

            GameInstance game = gameInstanceManager.getGame(connection.getGameId());
            if (game != null) {
                game.removePlayer(connection.getProfileId());
            }

            // Broadcast the appropriate leave message based on game phase.
            GameInstanceState state = game != null ? game.getState() : null;
            final ServerMessage leftMsg;
            if (state == GameInstanceState.WAITING_FOR_PLAYERS) {
                // Lobby phase: LOBBY_PLAYER_LEFT
                LobbyPlayerData playerData = new LobbyPlayerData(connection.getProfileId(), connection.getUsername(),
                        false, false, null);
                leftMsg = ServerMessage.of(ServerMessageType.LOBBY_PLAYER_LEFT, connection.getGameId(),
                        new ServerMessageData.LobbyPlayerLeftData(playerData));
            } else {
                boolean reconnectable = state == GameInstanceState.IN_PROGRESS || state == GameInstanceState.PAUSED;
                leftMsg = reconnectable
                        ? converter.createPlayerDisconnectedMessage(connection.getGameId(), connection.getProfileId(),
                                connection.getUsername())
                        : converter.createPlayerLeftMessage(connection.getGameId(), connection.getProfileId(),
                                connection.getUsername());
            }
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
     * Sends a {@link ServerMessageType#LOBBY_STATE} snapshot to a newly connected
     * player when the game is in {@link GameInstanceState#WAITING_FOR_PLAYERS}.
     */
    private void sendLobbyState(PlayerConnection connection, GameInstance game) {
        String gameId = connection.getGameId();
        GameSummary summary = gameService != null ? gameService.getGameSummary(gameId) : null;
        if (summary == null) {
            return;
        }

        long ownerProfileId = game.getOwnerProfileId();
        List<LobbyPlayerData> players = game.getPlayerSessions().values().stream()
                .map(s -> new LobbyPlayerData(s.getProfileId(), s.getPlayerName(), s.getProfileId() == ownerProfileId,
                        s.isAI(), s.isAI() ? String.valueOf(s.getSkillLevel()) : null))
                .toList();

        ServerMessageData.LobbyStateData data = new ServerMessageData.LobbyStateData(gameId, summary.name(),
                summary.hostingType(), summary.ownerName(), ownerProfileId, summary.maxPlayers(), summary.isPrivate(),
                players, summary.blinds());

        connection.sendMessage(ServerMessage.of(ServerMessageType.LOBBY_STATE, gameId, data));
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
