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
package com.donohoedigital.games.poker.online;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * WebSocket client for DD Poker lobby chat.
 *
 * <p>
 * Connects to the game server's {@code /ws/lobby} endpoint and dispatches
 * received messages to a {@link LobbyMessageListener}. Automatically reconnects
 * on unexpected disconnect.
 *
 * <p>
 * This class replaces the legacy {@code TcpChatClient} P2P lobby chat
 * mechanism.
 */
public class LobbyChatWebSocketClient {

    private static final Logger logger = LogManager.getLogger(LobbyChatWebSocketClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final int RECONNECT_DELAY_SECONDS = 5;

    private final LobbyMessageListener listener_;
    private final HttpClient httpClient_;
    private final ScheduledExecutorService reconnectScheduler_;

    private volatile WebSocket webSocket_;
    private volatile boolean connected_;
    private volatile boolean shouldReconnect_;
    private volatile String lastServerUrl_;
    private volatile String lastJwt_;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Production constructor — creates its own {@link HttpClient} and reconnect
     * scheduler.
     */
    public LobbyChatWebSocketClient(LobbyMessageListener listener) {
        this(listener, HttpClient.newHttpClient(), Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lobby-chat-reconnect");
            t.setDaemon(true);
            return t;
        }));
    }

    /** Package-private constructor for testing — allows injecting dependencies. */
    LobbyChatWebSocketClient(LobbyMessageListener listener, HttpClient httpClient,
            ScheduledExecutorService reconnectScheduler) {
        this.listener_ = listener;
        this.httpClient_ = httpClient;
        this.reconnectScheduler_ = reconnectScheduler;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Connect to the lobby WebSocket on the given server.
     *
     * @param serverUrl
     *            base URL of the server (e.g. {@code http://localhost:8877})
     * @param jwt
     *            JWT bearer token for authentication
     */
    public void connect(String serverUrl, String jwt) {
        this.lastServerUrl_ = serverUrl;
        this.lastJwt_ = jwt;
        this.shouldReconnect_ = true;
        doConnect(serverUrl, jwt);
    }

    /**
     * Disconnect from the lobby WebSocket. Stops auto-reconnect.
     */
    public void disconnect() {
        shouldReconnect_ = false;
        WebSocket ws = webSocket_;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
            } catch (Exception e) {
                logger.warn("Error closing lobby WebSocket", e);
            }
            webSocket_ = null;
        }
        connected_ = false;
    }

    /**
     * Send a chat message to the lobby. No-op if not currently connected.
     *
     * @param message
     *            the chat text to send (max 500 characters enforced server-side)
     */
    public void sendChat(String message) {
        WebSocket ws = webSocket_;
        if (ws == null || !connected_) {
            logger.debug("Not connected to lobby, dropping chat message");
            return;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(Map.of("type", "LOBBY_CHAT", "message", message));
            ws.sendText(json, true);
        } catch (Exception e) {
            logger.warn("Failed to send lobby chat message", e);
        }
    }

    /** Returns {@code true} if currently connected to the lobby. */
    public boolean isConnected() {
        return connected_;
    }

    // -------------------------------------------------------------------------
    // Internal connection management
    // -------------------------------------------------------------------------

    private void doConnect(String serverUrl, String jwt) {
        // Convert http:// → ws://, https:// → wss://
        String wsUrl = serverUrl.replaceFirst("^http://", "ws://").replaceFirst("^https://", "wss://");
        URI uri = URI.create(wsUrl + "/ws/lobby?token=" + jwt);
        logger.info("Connecting to lobby WebSocket at {}", wsUrl + "/ws/lobby");

        httpClient_.newWebSocketBuilder().buildAsync(uri, new LobbyWebSocketListener()).whenComplete((ws, ex) -> {
            if (ex != null) {
                logger.warn("Failed to connect to lobby WebSocket at {}", uri, ex);
                scheduleReconnect();
            }
            // On success: onOpen() is called on the listener by the JDK WebSocket API
        });
    }

    private void scheduleReconnect() {
        if (!shouldReconnect_ || reconnectScheduler_ == null) {
            return;
        }
        logger.info("Scheduling lobby chat reconnect in {} seconds", RECONNECT_DELAY_SECONDS);
        reconnectScheduler_.schedule(() -> {
            if (shouldReconnect_ && !connected_) {
                logger.info("Attempting lobby chat reconnect to {}", lastServerUrl_);
                doConnect(lastServerUrl_, lastJwt_);
            }
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    // -------------------------------------------------------------------------
    // WebSocket listener (package-private for testing)
    // -------------------------------------------------------------------------

    /**
     * Creates a new WebSocket listener for this client. Package-private to allow
     * tests to invoke message callbacks directly without a real network connection.
     */
    WebSocket.Listener createListenerForTesting() {
        return new LobbyWebSocketListener();
    }

    private class LobbyWebSocketListener implements WebSocket.Listener {

        private final StringBuilder messageBuffer_ = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket_ = webSocket;
            connected_ = true;
            logger.info("Connected to lobby WebSocket");
            listener_.onConnected();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messageBuffer_.append(data);
            if (last) {
                processMessage(messageBuffer_.toString());
                messageBuffer_.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            webSocket_ = null;
            connected_ = false;
            logger.info("Lobby WebSocket closed: {} {}", statusCode, reason);
            listener_.onDisconnected();
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logger.warn("Lobby WebSocket error", error);
            webSocket_ = null;
            connected_ = false;
            listener_.onDisconnected();
            scheduleReconnect();
        }

        private void processMessage(String json) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(json);
                String type = node.path("type").asText();
                switch (type) {
                    case "LOBBY_PLAYER_LIST" -> {
                        List<LobbyPlayer> players = new ArrayList<>();
                        for (JsonNode p : node.path("players")) {
                            players.add(new LobbyPlayer(p.path("playerId").asLong(), p.path("playerName").asText()));
                        }
                        listener_.onPlayerList(players);
                    }
                    case "LOBBY_JOIN" -> listener_.onPlayerJoined(node.path("playerId").asLong(),
                            node.path("playerName").asText());
                    case "LOBBY_LEAVE" -> listener_.onPlayerLeft(node.path("playerId").asLong(),
                            node.path("playerName").asText());
                    case "LOBBY_CHAT" -> listener_.onChatReceived(node.path("playerId").asLong(),
                            node.path("playerName").asText(), node.path("message").asText());
                    default -> logger.warn("Unknown lobby message type: {}", type);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse lobby message: {}", json, e);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Public types
    // -------------------------------------------------------------------------

    /**
     * Receives lobby chat events dispatched by {@link LobbyChatWebSocketClient}.
     */
    public interface LobbyMessageListener {
        /** Called when the WebSocket connection is established. */
        void onConnected();

        /** Called when the WebSocket connection is closed or lost. */
        void onDisconnected();

        /** Called when the server sends the initial list of connected players. */
        void onPlayerList(List<LobbyPlayer> players);

        /** Called when another player joins the lobby. */
        void onPlayerJoined(long playerId, String playerName);

        /** Called when a player leaves the lobby. */
        void onPlayerLeft(long playerId, String playerName);

        /** Called when a chat message is received from another player. */
        void onChatReceived(long playerId, String playerName, String message);
    }

    /** Represents a player visible in the lobby. */
    public record LobbyPlayer(long playerId, String playerName) {
    }

    // -------------------------------------------------------------------------
    // NoOpWebSocket — package-private stub for tests
    // -------------------------------------------------------------------------

    /**
     * Minimal {@link WebSocket} stub for unit tests. All send operations return an
     * already-completed future; all other calls are no-ops.
     */
    static class NoOpWebSocket implements WebSocket {
        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public String getSubprotocol() {
            return "";
        }

        @Override
        public boolean isOutputClosed() {
            return false;
        }

        @Override
        public boolean isInputClosed() {
            return false;
        }

        @Override
        public void abort() {
        }
    }
}
