/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.service.BanService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * WebSocket handler for the lobby chat endpoint ({@code /ws/lobby}).
 *
 * <p>
 * Registered as a Spring bean in {@link WebSocketAutoConfiguration}.
 *
 * <p>
 * Provides a pre-game social space: players can see who is online and chat
 * without being in a specific game. This is independent of the per-game
 * WebSocket handler ({@code /ws/games/*}).
 *
 * <h2>Authentication</h2> JWT is extracted from the {@code ?token=} query
 * parameter on connect. Invalid or missing tokens close the session with status
 * 1008 (Policy Violation). Banned profiles are closed with status 4003.
 *
 * <h2>Rate Limiting</h2> Chat is limited to {@value #MAX_CHAT_MESSAGES}
 * messages per {@value #DEFAULT_CHAT_WINDOW_MILLIS}ms per player. Messages
 * exceeding the limit are silently dropped (no error sent to client).
 *
 * <h2>Message Types (JSON)</h2>
 *
 * <pre>
 * Client → Server: { "type": "LOBBY_CHAT", "message": "..." }
 *
 * Server → Client:
 *   { "type": "LOBBY_PLAYER_LIST", "players": [{ "playerId": 42, "playerName": "Alice" }] }
 *   { "type": "LOBBY_JOIN",  "playerId": 42, "playerName": "Alice" }
 *   { "type": "LOBBY_LEAVE", "playerId": 42, "playerName": "Alice" }
 *   { "type": "LOBBY_CHAT",  "playerId": 42, "playerName": "Alice", "message": "Hi!", "timestamp": "..." }
 * </pre>
 */
public class LobbyWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketHandler.class);

    /** Maximum lobby chat message length in characters. */
    private static final int MAX_MESSAGE_LENGTH = 500;

    /** Close status code for banned players (custom, within 4000-4999 range). */
    private static final int CLOSE_BANNED = 4003;

    /** Default chat rate limit: max messages per window. */
    public static final int MAX_CHAT_MESSAGES = 30;

    /** Default chat rate-limit window in milliseconds. */
    static final long DEFAULT_CHAT_WINDOW_MILLIS = 60_000L;

    private final JwtTokenProvider jwtTokenProvider;
    private final BanService banService;
    private final ObjectMapper objectMapper;
    private final int maxChatMessages;
    private final long chatWindowMillis;

    /** Maps profileId → connected lobby player info. */
    private final ConcurrentHashMap<Long, LobbyPlayer> connectedPlayers = new ConcurrentHashMap<>();

    /** Maps WebSocket session ID → profileId (for lookup on disconnect). */
    private final ConcurrentHashMap<String, Long> sessionToProfile = new ConcurrentHashMap<>();

    /** Per-player chat rate-limit tracking. */
    private final ConcurrentHashMap<Long, ChatRecord> chatRateLimits = new ConcurrentHashMap<>();

    private record LobbyPlayer(long profileId, String playerName, WebSocketSession session) {
    }

    private record ChatRecord(AtomicInteger count, long windowStart) {
    }

    /** Production constructor — uses default rate-limit parameters. */
    public LobbyWebSocketHandler(JwtTokenProvider jwtTokenProvider, BanService banService, ObjectMapper objectMapper) {
        this(jwtTokenProvider, banService, objectMapper, MAX_CHAT_MESSAGES, DEFAULT_CHAT_WINDOW_MILLIS);
    }

    /** Testing constructor — allows overriding rate-limit parameters. */
    public LobbyWebSocketHandler(JwtTokenProvider jwtTokenProvider, BanService banService, ObjectMapper objectMapper,
            int maxChatMessages, long chatWindowMillis) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.banService = banService;
        this.objectMapper = objectMapper;
        this.maxChatMessages = maxChatMessages;
        this.chatWindowMillis = chatWindowMillis;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        long profileId = jwtTokenProvider.getProfileIdFromToken(token);
        String playerName = jwtTokenProvider.getUsernameFromToken(token);

        if (banService.isProfileBanned(profileId)) {
            session.close(new CloseStatus(CLOSE_BANNED, "Account is banned"));
            return;
        }

        LobbyPlayer player = new LobbyPlayer(profileId, playerName, session);
        connectedPlayers.put(profileId, player);
        sessionToProfile.put(session.getId(), profileId);

        // Send current player list to the new client
        sendPlayerList(session);

        // Broadcast LOBBY_JOIN to all other connected players
        ObjectNode joinMsg = objectMapper.createObjectNode();
        joinMsg.put("type", "LOBBY_JOIN");
        joinMsg.put("playerId", profileId);
        joinMsg.put("playerName", playerName);
        broadcastExcept(joinMsg, profileId);

        log.debug("Lobby: {} ({}) connected. Total: {}", playerName, profileId, connectedPlayers.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long profileId = sessionToProfile.get(session.getId());
        if (profileId == null) {
            return;
        }

        LobbyPlayer player = connectedPlayers.get(profileId);
        if (player == null) {
            return;
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            log.debug("Lobby: invalid JSON from {}", profileId);
            return;
        }

        String type = node.path("type").asText();
        if (!"LOBBY_CHAT".equals(type)) {
            return;
        }

        String text = node.path("message").asText("");

        // Validate message length
        if (text.length() > MAX_MESSAGE_LENGTH) {
            log.debug("Lobby: chat from {} too long ({} chars), dropped", profileId, text.length());
            return;
        }

        // Strip HTML tags to prevent injection
        text = text.replaceAll("<[^>]*>", "");

        // Rate limiting: max N messages per window
        if (!allowChat(profileId)) {
            log.debug("Lobby: chat from {} rate-limited, dropped", profileId);
            return;
        }

        // Broadcast to all connected players
        ObjectNode chatMsg = objectMapper.createObjectNode();
        chatMsg.put("type", "LOBBY_CHAT");
        chatMsg.put("playerId", profileId);
        chatMsg.put("playerName", player.playerName());
        chatMsg.put("message", text);
        chatMsg.put("timestamp", Instant.now().toString());
        broadcastAll(chatMsg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long profileId = sessionToProfile.remove(session.getId());
        if (profileId == null) {
            return;
        }

        LobbyPlayer player = connectedPlayers.remove(profileId);
        chatRateLimits.remove(profileId);

        if (player != null) {
            ObjectNode leaveMsg = objectMapper.createObjectNode();
            leaveMsg.put("type", "LOBBY_LEAVE");
            leaveMsg.put("playerId", profileId);
            leaveMsg.put("playerName", player.playerName());
            broadcastAll(leaveMsg);

            log.debug("Lobby: {} ({}) disconnected. Total: {}", player.playerName(), profileId,
                    connectedPlayers.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("Lobby: transport error on session {}: {}", session.getId(), exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void sendPlayerList(WebSocketSession session) throws IOException {
        ObjectNode listMsg = objectMapper.createObjectNode();
        listMsg.put("type", "LOBBY_PLAYER_LIST");
        ArrayNode players = listMsg.putArray("players");

        for (LobbyPlayer p : connectedPlayers.values()) {
            ObjectNode entry = players.addObject();
            entry.put("playerId", p.profileId());
            entry.put("playerName", p.playerName());
        }

        sendSafe(session, listMsg);
    }

    private void broadcastAll(ObjectNode message) {
        String json = toJson(message);
        if (json == null) {
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (LobbyPlayer player : connectedPlayers.values()) {
            sendSafe(player.session(), textMessage);
        }
    }

    private void broadcastExcept(ObjectNode message, long excludeProfileId) {
        String json = toJson(message);
        if (json == null) {
            return;
        }
        TextMessage textMessage = new TextMessage(json);
        for (LobbyPlayer player : connectedPlayers.values()) {
            if (player.profileId() != excludeProfileId) {
                sendSafe(player.session(), textMessage);
            }
        }
    }

    private void sendSafe(WebSocketSession session, ObjectNode message) {
        sendSafe(session, new TextMessage(toJson(message)));
    }

    private void sendSafe(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.debug("Lobby: failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private String toJson(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.error("Lobby: JSON serialization error", e);
            return null;
        }
    }

    /** Extracts the {@code token} query parameter from the WebSocket URI. */
    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String query = uri.getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring("token=".length());
            }
        }
        return null;
    }

    /**
     * Returns true if the player is allowed to send a chat message (not
     * rate-limited). Uses a sliding-window count: resets after
     * {@code chatWindowMillis}.
     */
    private boolean allowChat(long profileId) {
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};

        chatRateLimits.compute(profileId, (id, record) -> {
            if (record == null || now - record.windowStart() >= chatWindowMillis) {
                allowed[0] = true;
                return new ChatRecord(new AtomicInteger(1), now);
            }
            int count = record.count().incrementAndGet();
            allowed[0] = count <= maxChatMessages;
            return record;
        });

        return allowed[0];
    }
}
