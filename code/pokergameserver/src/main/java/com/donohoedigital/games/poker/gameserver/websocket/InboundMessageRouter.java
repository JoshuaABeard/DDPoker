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

import com.donohoedigital.games.poker.core.PlayerAction;
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.GameInstanceManager;
import com.donohoedigital.games.poker.gameserver.websocket.message.ClientMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ClientMessageType;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes inbound WebSocket messages from clients to the appropriate game
 * handlers.
 *
 * <p>
 * Validates sequence numbers (anti-replay), enforces rate limits for actions
 * and chat, sanitizes chat content, and dispatches to the correct game
 * operation.
 *
 * <p>
 * Player identity is always taken from the {@link PlayerConnection} (set during
 * JWT authentication), never from message content.
 */
public class InboundMessageRouter {

    private static final Logger logger = LoggerFactory.getLogger(InboundMessageRouter.class);
    private static final int CHAT_MAX_LENGTH = 500;
    private static final String HTML_TAG_PATTERN = "<[^>]*>";

    private final GameInstanceManager gameInstanceManager;
    private final GameConnectionManager connectionManager;
    private final RateLimiter actionRateLimiter;
    private final RateLimiter chatRateLimiter;
    private final ObjectMapper objectMapper;
    private final OutboundMessageConverter converter;

    /**
     * Creates an inbound message router.
     *
     * @param gameInstanceManager
     *            manager for looking up game instances
     * @param connectionManager
     *            manager for game connections (for broadcasts)
     * @param actionRateLimiter
     *            rate limiter for player actions
     * @param chatRateLimiter
     *            rate limiter for chat messages
     * @param objectMapper
     *            JSON object mapper
     */
    public InboundMessageRouter(GameInstanceManager gameInstanceManager, GameConnectionManager connectionManager,
            RateLimiter actionRateLimiter, RateLimiter chatRateLimiter, ObjectMapper objectMapper) {
        this.gameInstanceManager = gameInstanceManager;
        this.connectionManager = connectionManager;
        this.actionRateLimiter = actionRateLimiter;
        this.chatRateLimiter = chatRateLimiter;
        this.objectMapper = objectMapper;
        this.converter = new OutboundMessageConverter();
    }

    /**
     * Handles a raw JSON message from the given player connection.
     *
     * <p>
     * Performs sequence number validation, rate limiting, and dispatches based on
     * message type. Sends an ERROR message back to the player if validation fails.
     *
     * @param connection
     *            the player's connection (provides identity and game context)
     * @param rawJson
     *            raw JSON message text from the client
     */
    public void handleMessage(PlayerConnection connection, String rawJson) {
        String gameId = connection.getGameId();

        GameInstance game = gameInstanceManager.getGame(gameId);
        if (game == null) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            sendError(connection, "PARSE_ERROR", "Malformed JSON");
            return;
        }

        // Parse type
        JsonNode typeNode = root.get("type");
        if (typeNode == null || typeNode.isNull()) {
            sendError(connection, "INVALID_MESSAGE", "Missing message type");
            return;
        }

        ClientMessageType type;
        try {
            type = ClientMessageType.valueOf(typeNode.asText());
        } catch (IllegalArgumentException e) {
            sendError(connection, "INVALID_MESSAGE", "Unknown message type: " + typeNode.asText());
            return;
        }

        // Parse sequence number
        JsonNode seqNode = root.get("sequenceNumber");
        long sequenceNumber = seqNode != null ? seqNode.asLong(0) : 0;

        // Validate sequence number (must be strictly greater than last seen)
        if (sequenceNumber <= connection.getLastSequenceNumber()) {
            logger.debug("[ROUTER] OUT_OF_ORDER seq={} lastSeen={} type={}", sequenceNumber,
                    connection.getLastSequenceNumber(), type);
            sendError(connection, "OUT_OF_ORDER", "Sequence number must be greater than last received");
            return;
        }
        connection.setLastSequenceNumber(sequenceNumber);

        // Rate limiting: actions use actionRateLimiter, chat uses chatRateLimiter.
        // Both are scoped per (player, game) so activity in one game does not
        // throttle the same player in a different game.
        if (type == ClientMessageType.CHAT) {
            if (!chatRateLimiter.allowAction(connection.getProfileId(), gameId)) {
                sendError(connection, "RATE_LIMITED", "Chat rate limit exceeded");
                return;
            }
        } else if (type == ClientMessageType.PLAYER_ACTION) {
            if (!actionRateLimiter.allowAction(connection.getProfileId(), gameId)) {
                sendError(connection, "RATE_LIMITED", "Action rate limit exceeded");
                return;
            }
        }

        JsonNode dataNode = root.get("data");

        switch (type) {
            case PLAYER_ACTION -> handlePlayerAction(connection, game, dataNode);
            case CHAT -> handleChat(connection, game, dataNode);
            case ADMIN_KICK -> handleAdminKick(connection, game, dataNode);
            case ADMIN_PAUSE -> handleAdminPause(connection, game);
            case ADMIN_RESUME -> handleAdminResume(connection, game);
            // Silently ignore these — game features coming later
            case REBUY_DECISION, ADDON_DECISION, SIT_OUT, COME_BACK -> {
            }
        }
    }

    private void handlePlayerAction(PlayerConnection connection, GameInstance game, JsonNode dataNode) {
        logger.debug("[ROUTER] handlePlayerAction profileId={} data={}", connection.getProfileId(), dataNode);
        ClientMessageData.PlayerActionData data;
        try {
            data = objectMapper.treeToValue(dataNode, ClientMessageData.PlayerActionData.class);
        } catch (Exception e) {
            logger.debug("[ROUTER] INVALID_DATA: {}", e.getMessage());
            sendError(connection, "INVALID_DATA", "Invalid player action data");
            return;
        }

        PlayerAction action;
        try {
            action = parseAction(data.action(), data.amount());
        } catch (IllegalArgumentException e) {
            logger.debug("[ROUTER] INVALID_ACTION: {}", data.action());
            sendError(connection, "INVALID_ACTION", "Unknown action: " + data.action());
            return;
        }

        logger.debug("[ROUTER] dispatching action={} profileId={}", action, connection.getProfileId());
        game.onPlayerAction(connection.getProfileId(), action);
        logger.debug("[ROUTER] onPlayerAction returned");
        // Clear the player's rate-limit entry after a valid action so the next hand's
        // action isn't blocked by the inter-action minimum interval. Without this,
        // fast-advancing games (AI opponents, practice mode) can trigger RATE_LIMITED
        // on the very next turn, leaving the server waiting indefinitely.
        actionRateLimiter.removePlayer(connection.getProfileId(), connection.getGameId());
    }

    private void handleChat(PlayerConnection connection, GameInstance game, JsonNode dataNode) {
        ClientMessageData.ChatData data;
        try {
            data = objectMapper.treeToValue(dataNode, ClientMessageData.ChatData.class);
        } catch (Exception e) {
            sendError(connection, "INVALID_DATA", "Invalid chat data");
            return;
        }

        String message = sanitizeChat(data.message());
        ServerMessage chatMsg = converter.createChatMessage(connection.getGameId(), connection.getProfileId(),
                connection.getUsername(), message, data.tableChat());
        connectionManager.broadcastToGame(connection.getGameId(), chatMsg);
    }

    private void handleAdminKick(PlayerConnection connection, GameInstance game, JsonNode dataNode) {
        if (connection.getProfileId() != game.getOwnerProfileId()) {
            sendError(connection, "FORBIDDEN", "Only the game owner can kick players");
            return;
        }

        ClientMessageData.AdminKickData data;
        try {
            data = objectMapper.treeToValue(dataNode, ClientMessageData.AdminKickData.class);
        } catch (Exception e) {
            sendError(connection, "INVALID_DATA", "Invalid admin kick data");
            return;
        }

        long targetProfileId = data.playerId();

        // Single pass: find both username and connection simultaneously
        String targetUsername = "";
        PlayerConnection targetConnection = null;
        for (PlayerConnection pc : connectionManager.getConnections(connection.getGameId())) {
            if (pc.getProfileId() == targetProfileId) {
                targetUsername = pc.getUsername();
                targetConnection = pc;
                break;
            }
        }

        // Remove from game instance, broadcast, then close — in that order
        game.removePlayer(targetProfileId);

        ServerMessage kickedMsg = converter.createPlayerKickedMessage(connection.getGameId(), targetProfileId,
                targetUsername, "Kicked by owner");
        connectionManager.broadcastToGame(connection.getGameId(), kickedMsg);

        if (targetConnection != null) {
            connectionManager.removeConnection(connection.getGameId(), targetProfileId);
            targetConnection.close();
        }
    }

    private void handleAdminPause(PlayerConnection connection, GameInstance game) {
        if (connection.getProfileId() != game.getOwnerProfileId()) {
            sendError(connection, "FORBIDDEN", "Only the game owner can pause the game");
            return;
        }

        game.pauseAsUser(connection.getProfileId());

        ServerMessage pausedMsg = converter.createGamePausedMessage(connection.getGameId(), "Owner paused",
                connection.getUsername());
        connectionManager.broadcastToGame(connection.getGameId(), pausedMsg);
    }

    private void handleAdminResume(PlayerConnection connection, GameInstance game) {
        if (connection.getProfileId() != game.getOwnerProfileId()) {
            sendError(connection, "FORBIDDEN", "Only the game owner can resume the game");
            return;
        }

        game.resumeAsUser(connection.getProfileId());

        ServerMessage resumedMsg = converter.createGameResumedMessage(connection.getGameId(), connection.getUsername());
        connectionManager.broadcastToGame(connection.getGameId(), resumedMsg);
    }

    private PlayerAction parseAction(String actionString, int amount) {
        return switch (actionString.toUpperCase()) {
            case "FOLD" -> PlayerAction.fold();
            case "CHECK" -> PlayerAction.check();
            case "CALL" -> PlayerAction.call();
            case "BET" -> PlayerAction.bet(amount);
            case "RAISE" -> PlayerAction.raise(amount);
            // ALL_IN is not a valid client action; clients must use RAISE with the all-in
            // amount
            default -> throw new IllegalArgumentException("Unknown action: " + actionString);
        };
    }

    private String sanitizeChat(String message) {
        if (message == null) {
            return "";
        }
        // Strip HTML tags
        String sanitized = message.replaceAll(HTML_TAG_PATTERN, "");
        // Enforce max length
        if (sanitized.length() > CHAT_MAX_LENGTH) {
            sanitized = sanitized.substring(0, CHAT_MAX_LENGTH);
        }
        return sanitized;
    }

    private void sendError(PlayerConnection connection, String code, String message) {
        ServerMessage error = converter.createErrorMessage(connection.getGameId(), code, message);
        connection.sendMessage(error);
    }
}
