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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.gameserver.websocket.message.ClientMessageType;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * JDK {@code java.net.http.HttpClient}-based WebSocket client for the DDPoker
 * game server.
 *
 * <p>
 * Connects to {@code ws://host:port/ws/games/{gameId}?token={jwt}}, receives
 * {@code ServerMessage} JSON frames, and dispatches them to a registered
 * handler. Client-to-server messages are sent via type-specific {@code send*()}
 * methods.
 *
 * <p>
 * Auto-reconnects with exponential backoff on unexpected disconnect. The caller
 * must set a message handler via {@link #setMessageHandler} before connecting.
 */
public class WebSocketGameClient {

    private static final Logger logger = LogManager.getLogger(WebSocketGameClient.class);

    private static final long INITIAL_RECONNECT_DELAY_MS = 500;
    private static final long MAX_RECONNECT_DELAY_MS = 30_000;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private Consumer<InboundMessage> messageHandler;
    private volatile WebSocket webSocket;
    private volatile boolean connected = false;
    private volatile boolean intentionallyClosed = false;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    private String wsUrl;
    private final AtomicLong sequenceCounter = new AtomicLong(0);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ws-reconnect");
        t.setDaemon(true);
        return t;
    });

    public WebSocketGameClient() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    // Visible for testing
    WebSocketGameClient(ObjectMapper objectMapper, HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Sets the handler called for each inbound message from the server. Must be set
     * before calling {@link #connect}.
     */
    public void setMessageHandler(Consumer<InboundMessage> handler) {
        this.messageHandler = handler;
    }

    /**
     * Connects to the game server WebSocket endpoint.
     *
     * @param serverPort
     *            port of the embedded server
     * @param gameId
     *            game identifier returned by the REST API
     * @param jwt
     *            JWT token for authentication
     * @return future that completes when the connection is established
     */
    public CompletableFuture<Void> connect(int serverPort, String gameId, String jwt) {
        // JWT is passed as a query parameter — the WebSocket initial handshake is an
        // HTTP GET, so custom request headers are not supported. This is standard
        // practice for WebSocket auth. Acceptable for embedded (localhost-only) mode;
        // for remote servers the token appears in server access logs (see M6 notes).
        this.wsUrl = "ws://localhost:" + serverPort + "/ws/games/" + gameId + "?token=" + jwt;
        this.intentionallyClosed = false;
        this.reconnecting.set(false);
        return openConnection();
    }

    /** Sends a player action (FOLD, CHECK, CALL, BET, RAISE, ALL_IN). */
    public void sendAction(String action, int amount) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("action", action);
        data.put("amount", amount);
        sendMessage(ClientMessageType.PLAYER_ACTION, data);
    }

    /** Sends a chat message. */
    public void sendChat(String message, boolean tableChat) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("message", message);
        data.put("tableChat", tableChat);
        sendMessage(ClientMessageType.CHAT, data);
    }

    /** Sends a rebuy decision. */
    public void sendRebuyDecision(boolean accept) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("accept", accept);
        sendMessage(ClientMessageType.REBUY_DECISION, data);
    }

    /** Sends an add-on decision. */
    public void sendAddonDecision(boolean accept) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("accept", accept);
        sendMessage(ClientMessageType.ADDON_DECISION, data);
    }

    /**
     * Sends a Never Broke decision (accept = transfer chips, decline = eliminate).
     */
    public void sendNeverBrokeDecision(boolean accept) {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("accept", accept);
        sendMessage(ClientMessageType.NEVER_BROKE_DECISION, data);
    }

    /** Sends an admin pause command (owner only). */
    public void sendAdminPause() {
        sendMessage(ClientMessageType.ADMIN_PAUSE, objectMapper.createObjectNode());
    }

    /** Sends an admin resume command (owner only). */
    public void sendAdminResume() {
        sendMessage(ClientMessageType.ADMIN_RESUME, objectMapper.createObjectNode());
    }

    /** Sends a sit-out request. */
    public void sendSitOut() {
        sendMessage(ClientMessageType.SIT_OUT, objectMapper.createObjectNode());
    }

    /** Sends a continue signal during all-in runout (human clicked Continue). */
    public void sendContinueRunout() {
        sendMessage(ClientMessageType.CONTINUE_RUNOUT, objectMapper.createObjectNode());
    }

    /** Returns {@code true} if the WebSocket is currently connected. */
    public boolean isConnected() {
        return connected;
    }

    /** Closes the WebSocket connection without reconnecting. */
    public void disconnect() {
        intentionallyClosed = true;
        connected = false;
        WebSocket ws = this.webSocket;
        if (ws != null && !ws.isOutputClosed()) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnecting");
        }
        scheduler.shutdown();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private CompletableFuture<Void> openConnection() {
        return httpClient.newWebSocketBuilder().buildAsync(URI.create(wsUrl), new GameWebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    this.connected = true;
                    logger.debug("WebSocket connected to {}", wsUrl);
                }).exceptionally(ex -> {
                    logger.error("WebSocket connection failed to {}: {}", wsUrl, ex.getMessage());
                    return null;
                });
    }

    private void sendMessage(ClientMessageType type, Object data) {
        if (!connected || webSocket == null) {
            logger.warn("Attempted to send {} but not connected", type);
            logger.debug("[WS-SEND] DROPPED - not connected, type={}", type);
            return;
        }
        try {
            ObjectNode envelope = objectMapper.createObjectNode();
            envelope.put("type", type.name());
            long seq = sequenceCounter.incrementAndGet();
            envelope.put("sequenceNumber", seq);
            envelope.set("data", objectMapper.valueToTree(data));
            String json = objectMapper.writeValueAsString(envelope);
            logger.debug("[WS-SEND] type={} seq={} json={}", type, seq, json);
            webSocket.sendText(json, true);
        } catch (Exception e) {
            logger.error("Failed to send {} message", type, e);
        }
    }

    private void handleReconnect() {
        if (intentionallyClosed) {
            return;
        }
        // Prevent a duplicate reconnect cycle when both onClose and onError fire for
        // the same disconnect event. The cycle resets reconnecting when it succeeds or
        // exhausts all attempts; connect() also resets it for a fresh session.
        if (!reconnecting.compareAndSet(false, true)) {
            return;
        }
        connected = false;
        logger.info("WebSocket disconnected, scheduling reconnect");

        // Per-cycle flag: once one attempt succeeds, the remaining scheduled
        // attempts for this cycle are no-ops.
        AtomicBoolean reconnected = new AtomicBoolean(false);

        long delay = INITIAL_RECONNECT_DELAY_MS;
        for (int attempt = 1; attempt <= MAX_RECONNECT_ATTEMPTS; attempt++) {
            final int attemptNum = attempt;
            final boolean isLastAttempt = (attempt == MAX_RECONNECT_ATTEMPTS);
            scheduler.schedule(() -> {
                if (intentionallyClosed || reconnected.get())
                    return;
                logger.info("Reconnect attempt {}/{}", attemptNum, MAX_RECONNECT_ATTEMPTS);
                openConnection().whenComplete((v, ex) -> {
                    if (ex != null) {
                        logger.warn("Reconnect attempt {} failed", attemptNum, ex);
                        if (isLastAttempt)
                            reconnecting.set(false);
                    } else {
                        reconnected.set(true);
                        reconnecting.set(false);
                    }
                });
            }, delay, TimeUnit.MILLISECONDS);
            delay = Math.min(delay * 2, MAX_RECONNECT_DELAY_MS);
        }
    }

    // -------------------------------------------------------------------------
    // Inbound message type
    // -------------------------------------------------------------------------

    /**
     * A parsed inbound message from the server.
     *
     * @param type
     *            the message type enum
     * @param gameId
     *            the game ID
     * @param data
     *            raw JSON node for the message data (to be parsed by the handler)
     */
    public record InboundMessage(ServerMessageType type, String gameId, JsonNode data) {
    }

    // -------------------------------------------------------------------------
    // WebSocket listener
    // -------------------------------------------------------------------------

    private class GameWebSocketListener implements WebSocket.Listener {

        private final StringBuilder textAccumulator = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            // Request the first message — required by the Java WebSocket API before
            // any onText/onBinary/onPing/onPong calls are delivered.
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textAccumulator.append(data);
            if (last) {
                String json = textAccumulator.toString();
                textAccumulator.setLength(0);
                dispatch(json);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            logger.debug("WebSocket closed: {} {}", statusCode, reason);
            handleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logger.error("WebSocket error", error);
            handleReconnect();
        }

        private void dispatch(String json) {
            if (messageHandler == null)
                return;
            try {
                JsonNode root = objectMapper.readTree(json);
                String typeName = root.path("type").asText();
                String gameId = root.path("gameId").asText();
                JsonNode data = root.path("data");
                logger.debug("[WS-RAW] type={} gameId={}", typeName, gameId);

                ServerMessageType type = ServerMessageType.valueOf(typeName);
                messageHandler.accept(new InboundMessage(type, gameId, data));
            } catch (Exception e) {
                logger.error("Failed to dispatch inbound message: {}", json, e);
            }
        }
    }
}
