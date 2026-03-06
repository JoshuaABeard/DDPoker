/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * HTTP client for the DDPoker Dev Control Server.
 *
 * <p>
 * Wraps all endpoints documented in docs/guides/desktop-client-testing.md. All
 * methods throw {@link Exception} for simplicity in test code.
 *
 * <p>
 * Uses Java's built-in {@code java.net.http.HttpClient} and Jackson for JSON.
 * Does not import any classes from
 * {@code com.donohoedigital.games.poker.control}.
 */
class ControlServerClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HEADER_KEY = "X-Control-Key";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;

    /**
     * Creates a client targeting the given port and authenticating with the given
     * API key.
     *
     * @param port
     *            the TCP port the control server listens on
     * @param apiKey
     *            the API key read from {@code ~/.ddpoker/control-server.key}
     */
    public ControlServerClient(int port, String apiKey) {
        this.baseUrl = "http://127.0.0.1:" + port;
        this.apiKey = apiKey;
        this.http = HttpClient.newHttpClient();
    }

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    /**
     * GET /health — returns true if the server responds with HTTP 200.
     */
    public boolean isHealthy() throws Exception {
        HttpResponse<String> resp = get("/health");
        return resp.statusCode() == 200;
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * GET /state — returns the full game state as a {@link JsonNode}.
     */
    public JsonNode getState() throws Exception {
        HttpResponse<String> resp = get("/state");
        requireSuccess(resp, "/state");
        return MAPPER.readTree(resp.body());
    }

    // -------------------------------------------------------------------------
    // Game start
    // -------------------------------------------------------------------------

    /**
     * POST /game/start with {@code numPlayers} only (uses server defaults for chips
     * and blinds).
     */
    public void startGame(int numPlayers) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("numPlayers", numPlayers);
        HttpResponse<String> resp = post("/game/start", body.toString());
        requireSuccess(resp, "/game/start");
    }

    /**
     * POST /game/start with explicit {@code numPlayers}, {@code startingChips}, and
     * {@code smallBlind}.
     */
    public void startGame(int numPlayers, int startingChips, int smallBlind) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("numPlayers", numPlayers);
        body.put("startingChips", startingChips);
        body.put("smallBlind", smallBlind);
        HttpResponse<String> resp = post("/game/start", body.toString());
        requireSuccess(resp, "/game/start");
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * POST /action with {@code {"type": type}} — no amount field.
     *
     * @return the parsed response body
     */
    public JsonNode submitAction(String type) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", type);
        HttpResponse<String> resp = post("/action", body.toString());
        requireSuccess(resp, "/action");
        return MAPPER.readTree(resp.body());
    }

    /**
     * POST /action with {@code {"type": type, "amount": amount}}.
     *
     * @return the parsed response body
     */
    public JsonNode submitAction(String type, int amount) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("type", type);
        body.put("amount", amount);
        HttpResponse<String> resp = post("/action", body.toString());
        requireSuccess(resp, "/action");
        return MAPPER.readTree(resp.body());
    }

    // -------------------------------------------------------------------------
    // Validate
    // -------------------------------------------------------------------------

    /**
     * GET /validate — returns the invariant validation result as a
     * {@link JsonNode}.
     */
    public JsonNode validate() throws Exception {
        HttpResponse<String> resp = get("/validate");
        requireSuccess(resp, "/validate");
        return MAPPER.readTree(resp.body());
    }

    // -------------------------------------------------------------------------
    // Card injection
    // -------------------------------------------------------------------------

    /**
     * POST /cards/inject with an explicit ordered card list.
     *
     * <p>
     * Card format: rank + suit, e.g. {@code "As"}, {@code "Kh"}, {@code "2c"}.
     */
    public void injectCards(String... cards) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        ArrayNode cardArray = body.putArray("cards");
        for (String card : cards) {
            cardArray.add(card);
        }
        HttpResponse<String> resp = post("/cards/inject", body.toString());
        requireSuccess(resp, "/cards/inject");
    }

    /**
     * POST /cards/inject with a seed for a reproducible shuffle.
     */
    public void injectCardsBySeed(long seed) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("seed", seed);
        HttpResponse<String> resp = post("/cards/inject", body.toString());
        requireSuccess(resp, "/cards/inject");
    }

    // -------------------------------------------------------------------------
    // Polling helpers
    // -------------------------------------------------------------------------

    /**
     * Polls /state until {@code currentAction.isHumanTurn} is {@code true}, or
     * until {@code timeout} elapses. Polls every 500ms.
     *
     * @param timeout
     *            maximum time to wait
     * @throws Exception
     *             if the timeout elapses without a human turn becoming available
     */
    public void waitForHumanTurn(Duration timeout) throws Exception {
        long deadlineMs = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadlineMs) {
            JsonNode state = getState();
            JsonNode isHumanTurn = state.path("currentAction").path("isHumanTurn");
            if (isHumanTurn.asBoolean(false)) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for isHumanTurn == true");
    }

    /**
     * Polls /state until {@code inputMode} matches one of the given modes, or until
     * {@code timeout} elapses. Polls every 500ms.
     *
     * @param timeout
     *            maximum time to wait
     * @param modes
     *            one or more inputMode strings to match (e.g. "CHECK_BET", "DEAL")
     * @return the state {@link JsonNode} captured when the matching mode was
     *         observed
     * @throws Exception
     *             if the timeout elapses without the mode being reached
     */
    public JsonNode waitForInputMode(Duration timeout, String... modes) throws Exception {
        Set<String> modeSet = new HashSet<>(Arrays.asList(modes));
        long deadlineMs = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadlineMs) {
            JsonNode state = getState();
            String currentMode = state.path("inputMode").asText("");
            if (modeSet.contains(currentMode)) {
                return state;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Timed out after " + timeout + " waiting for inputMode in " + Arrays.toString(modes));
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header(HEADER_KEY, apiKey).GET()
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String jsonBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header(HEADER_KEY, apiKey)
                .header(HEADER_CONTENT_TYPE, APPLICATION_JSON).POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private void requireSuccess(HttpResponse<String> resp, String path) {
        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new AssertionError("Expected 2xx from " + path + " but got " + status + ": " + resp.body());
        }
    }

    private JsonNode postJson(String path, JsonNode body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header(HEADER_KEY, apiKey)
                .header(HEADER_CONTENT_TYPE, APPLICATION_JSON).POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(resp.body());
    }

    private JsonNode getJson(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header(HEADER_KEY, apiKey).GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(resp.body());
    }

    private JsonNode putJson(String path, JsonNode body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).header(HEADER_KEY, apiKey)
                .header(HEADER_CONTENT_TYPE, APPLICATION_JSON).PUT(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(resp.body());
    }

    // -------------------------------------------------------------------------
    // Online game management
    // -------------------------------------------------------------------------

    /**
     * POST /online/login — authenticate against the central server.
     */
    public JsonNode onlineLogin(String serverUrl, String username, String password) throws Exception {
        return postJson("/online/login", MAPPER.createObjectNode().put("serverUrl", serverUrl).put("username", username)
                .put("password", password));
    }

    /**
     * POST /online/host — create a game on the central server and navigate to
     * Lobby.Host.
     */
    public JsonNode onlineHost(JsonNode config) throws Exception {
        return postJson("/online/host", config);
    }

    /**
     * GET /online/lobby — return current lobby state.
     */
    public JsonNode onlineLobby() throws Exception {
        return getJson("/online/lobby");
    }

    /**
     * POST /online/start — start the hosted game.
     */
    public JsonNode onlineStart() throws Exception {
        return postJson("/online/start", MAPPER.createObjectNode());
    }

    /**
     * POST /online/join — join an existing game.
     */
    public JsonNode onlineJoin(String gameId) throws Exception {
        return postJson("/online/join", MAPPER.createObjectNode().put("gameId", gameId));
    }

    /**
     * POST /online/register — register a new user on the game server.
     */
    public JsonNode onlineRegister(String serverUrl, String username, String password, String email) throws Exception {
        return postJson("/online/register", MAPPER.createObjectNode().put("serverUrl", serverUrl)
                .put("username", username).put("password", password).put("email", email));
    }

    /**
     * GET /online/games — list available games on the server.
     */
    public JsonNode onlineGames() throws Exception {
        return getJson("/online/games");
    }

    /**
     * POST /online/observe — start observing a game as a spectator.
     */
    public JsonNode onlineObserve(String gameId) throws Exception {
        return postJson("/online/observe", MAPPER.createObjectNode().put("gameId", gameId));
    }

    /**
     * POST /online/lobby/kick — kick a player from the lobby (host only).
     */
    public JsonNode onlineLobbyKick(long profileId) throws Exception {
        return postJson("/online/lobby/kick", MAPPER.createObjectNode().put("profileId", profileId));
    }

    /**
     * PUT /online/lobby/settings — update game settings in the lobby (host only).
     */
    public JsonNode onlineLobbySettings(String name, Integer maxPlayers) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        if (name != null)
            body.put("name", name);
        if (maxPlayers != null)
            body.put("maxPlayers", maxPlayers);
        return putJson("/online/lobby/settings", body);
    }

    /**
     * POST /account/password — change the user's password.
     */
    public JsonNode accountChangePassword(String oldPassword, String newPassword) throws Exception {
        return postJson("/account/password",
                MAPPER.createObjectNode().put("oldPassword", oldPassword).put("newPassword", newPassword));
    }

    /**
     * PUT /account/email — request an email address change.
     */
    public JsonNode accountChangeEmail(String newEmail) throws Exception {
        return putJson("/account/email", MAPPER.createObjectNode().put("newEmail", newEmail));
    }

    /**
     * GET /account/profile — get the current user's profile.
     */
    public JsonNode accountProfile() throws Exception {
        return getJson("/account/profile");
    }

    /**
     * Returns a minimal valid GameConfig JSON node for E2E test game creation. 4
     * players, 1500 chips, 10/20 blinds, zero AI delays, auto-deal enabled.
     */
    public static ObjectNode minimalOnlineGameConfig() {
        ObjectNode cfg = MAPPER.createObjectNode();
        cfg.put("name", "E2E Test Game");
        cfg.put("maxPlayers", 4);
        cfg.put("maxOnlinePlayers", 4);
        cfg.put("startingChips", 1500);
        cfg.put("fillComputer", true);
        cfg.put("doubleAfterLastLevel", true);

        ObjectNode level = MAPPER.createObjectNode();
        level.put("smallBlind", 10);
        level.put("bigBlind", 20);
        level.put("ante", 0);
        level.put("minutes", 5);
        level.put("isBreak", false);
        level.put("gameType", "NOLIMIT_HOLDEM");
        cfg.set("blindStructure", MAPPER.createArrayNode().add(level));

        ObjectNode practice = MAPPER.createObjectNode();
        practice.put("aiActionDelayMs", 0);
        practice.put("handResultPauseMs", 100);
        practice.put("allInRunoutPauseMs", 0);
        practice.put("autoDeal", true);
        cfg.set("practiceConfig", practice);

        return cfg;
    }
}
