/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.e2e;

import com.donohoedigital.games.poker.online.RestAuthClient;
import com.donohoedigital.games.poker.online.RestGameClient;
import com.donohoedigital.games.poker.online.WebSocketGameClient;
import com.donohoedigital.games.poker.protocol.dto.GameJoinResponse;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.protocol.message.ServerMessageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

/**
 * Lightweight in-process player for E2E tests. Connects to the game server via
 * REST + WebSocket and auto-responds to action requests.
 *
 * <p>
 * Simple strategy: check when possible, call otherwise, fold as last resort.
 */
public class SyntheticPlayer implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(SyntheticPlayer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final String serverBaseUrl;
    private final String username;
    private final RestAuthClient authClient;
    private final WebSocketGameClient wsClient;

    private String jwt;
    private long profileId;
    private RestGameClient gameClient;

    SyntheticPlayer(String serverBaseUrl, String username, RestAuthClient authClient, WebSocketGameClient wsClient) {
        this.serverBaseUrl = serverBaseUrl;
        this.username = username;
        this.authClient = authClient;
        this.wsClient = wsClient;
    }

    /**
     * Creates a SyntheticPlayer, registers the user, verifies email, and logs in.
     *
     * @param serverBaseUrl
     *            base URL of the game server (e.g., {@code http://localhost:19877})
     * @param gameServer
     *            the test game server process (used for email verification)
     * @param username
     *            desired username
     * @param password
     *            password
     * @param email
     *            email address
     * @return a ready-to-use SyntheticPlayer with valid JWT
     */
    public static SyntheticPlayer create(String serverBaseUrl, GameServerTestProcess gameServer, String username,
            String password, String email) throws Exception {
        SyntheticPlayer player = new SyntheticPlayer(serverBaseUrl, username, new RestAuthClient(),
                new WebSocketGameClient());
        player.registerAndLogin(gameServer, password, email);
        return player;
    }

    /**
     * Creates a SyntheticPlayer with injected dependencies (for unit testing).
     */
    static SyntheticPlayer createForTest(String serverBaseUrl, String username, RestAuthClient authClient,
            WebSocketGameClient wsClient) {
        return new SyntheticPlayer(serverBaseUrl, username, authClient, wsClient);
    }

    /**
     * Joins a game via REST and connects via WebSocket with auto-play enabled.
     *
     * @param gameId
     *            the game to join
     */
    public void joinAndPlay(String gameId) {
        GameJoinResponse joinResp = gameClient.joinGame(gameId, null);
        String wsUrl = joinResp.wsUrl();

        // Parse host and port from wsUrl (e.g., ws://localhost:19877/ws/games/abc)
        URI wsUri = URI.create(wsUrl);
        String host = wsUri.getHost();
        int port = wsUri.getPort();

        wsClient.setMessageHandler(msg -> handleMessage(msg));
        wsClient.connect(host, port, gameId, jwt);
        logger.info("SyntheticPlayer {} joined game {} and connected via WebSocket", username, gameId);
    }

    /** Returns {@code true} if the WebSocket is currently connected. */
    public boolean isConnected() {
        return wsClient.isConnected();
    }

    /** Returns the profile ID from login. */
    public long getProfileId() {
        return profileId;
    }

    /** Returns the username. */
    public String getUsername() {
        return username;
    }

    /** Returns the JWT token. */
    public String getJwt() {
        return jwt;
    }

    @Override
    public void close() {
        wsClient.disconnect();
        logger.info("SyntheticPlayer {} disconnected", username);
    }

    // -------------------------------------------------------------------------
    // Auto-play logic
    // -------------------------------------------------------------------------

    void handleMessage(WebSocketGameClient.InboundMessage msg) {
        if (msg.type() == ServerMessageType.CONNECTED) {
            // Store reconnect token if provided
            JsonNode reconnectToken = msg.data().path("reconnectToken");
            if (!reconnectToken.isMissingNode() && !reconnectToken.isNull()) {
                wsClient.setReconnectToken(reconnectToken.asText());
            }
        } else if (msg.type() == ServerMessageType.ACTION_REQUIRED) {
            handleActionRequired(msg.data());
        }
    }

    void handleActionRequired(JsonNode data) {
        JsonNode options = data.path("options");
        if (options.isMissingNode() || options.isNull()) {
            logger.warn("SyntheticPlayer {} received ACTION_REQUIRED with no options", username);
            return;
        }

        String action = chooseAction(options);
        int amount = chooseAmount(action, options);
        logger.debug("SyntheticPlayer {} auto-acting: {} amount={}", username, action, amount);
        wsClient.sendAction(action, amount);
    }

    /**
     * Chooses an action based on simple strategy: CHECK > CALL > FOLD.
     *
     * <p>
     * Package-private for testing.
     */
    static String chooseAction(JsonNode options) {
        if (options.path("canCheck").asBoolean(false)) {
            return "CHECK";
        }
        if (options.path("canCall").asBoolean(false)) {
            return "CALL";
        }
        if (options.path("canFold").asBoolean(false)) {
            return "FOLD";
        }
        // Fallback — should not happen with valid server data
        return "FOLD";
    }

    /**
     * Chooses the amount for the given action.
     *
     * <p>
     * Package-private for testing.
     */
    static int chooseAmount(String action, JsonNode options) {
        return switch (action) {
            case "CALL" -> options.path("callAmount").asInt(0);
            default -> 0;
        };
    }

    // -------------------------------------------------------------------------
    // Registration and login
    // -------------------------------------------------------------------------

    private void registerAndLogin(GameServerTestProcess gameServer, String password, String email) throws Exception {
        // Register and verify email via server's dev endpoint
        gameServer.registerAndVerify(username, password, email);

        // Login to get JWT
        LoginResponse loginResp = authClient.login(serverBaseUrl, username, password);
        this.jwt = loginResp.token();
        this.profileId = loginResp.profile().id();
        this.gameClient = new RestGameClient(serverBaseUrl, jwt);

        logger.info("SyntheticPlayer {} registered and logged in (profileId={})", username, profileId);
    }
}
