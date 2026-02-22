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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.dto.CommunityGameRegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.CreateGameResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

/**
 * HTTP client for the poker game server REST API.
 *
 * <p>
 * Used by the desktop client to list games, register community-hosted games,
 * and send heartbeats. All calls are synchronous and must not be called from
 * the EDT.
 */
public class RestGameClient {

    private static final Logger logger = LogManager.getLogger(RestGameClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final String baseUrl;
    private final HttpClient http;
    private volatile String jwt;

    /**
     * @param baseUrl
     *            HTTP base URL of the game server (e.g.,
     *            {@code http://localhost:54321})
     * @param jwt
     *            JWT bearer token for authenticated requests
     */
    public RestGameClient(String baseUrl, String jwt) {
        this.baseUrl = baseUrl;
        this.jwt = jwt;
        this.http = HttpClient.newHttpClient();
    }

    /** Update the JWT (e.g., after re-authentication). */
    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    /**
     * List active games (WAITING_FOR_PLAYERS and IN_PROGRESS) from the server.
     *
     * @return game summaries, or an empty list on error
     */
    public List<GameSummary> listGames() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/games"))
                    .header("Authorization", "Bearer " + jwt).GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("listGames returned {}: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            GameListResponse listResponse = OBJECT_MAPPER.readValue(response.body(), GameListResponse.class);
            return listResponse.games();
        } catch (Exception e) {
            logger.warn("Failed to list games from {}", baseUrl, e);
            return Collections.emptyList();
        }
    }

    /**
     * Create a new server-hosted game.
     *
     * @param config
     *            tournament configuration
     * @return the created game summary (includes gameId and wsUrl)
     * @throws RestGameClientException
     *             if creation fails
     */
    public GameSummary createGame(GameConfig config) {
        try {
            String body = OBJECT_MAPPER.writeValueAsString(config);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/games"))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new RestGameClientException(
                        "createGame returned " + response.statusCode() + ": " + response.body());
            }
            CreateGameResponse created = OBJECT_MAPPER.readValue(response.body(), CreateGameResponse.class);
            return getGameSummary(created.gameId());
        } catch (RestGameClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RestGameClientException("Failed to create game", e);
        }
    }

    /**
     * Start a game that is waiting for players.
     *
     * @param gameId
     *            the game to start
     * @return the updated game summary
     * @throws RestGameClientException
     *             if the start fails
     */
    public GameSummary startGame(String gameId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/games/" + gameId + "/start"))
                    .header("Authorization", "Bearer " + jwt).POST(HttpRequest.BodyPublishers.noBody()).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RestGameClientException(
                        "startGame returned " + response.statusCode() + ": " + response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), GameSummary.class);
        } catch (RestGameClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RestGameClientException("Failed to start game " + gameId, e);
        }
    }

    /**
     * Fetch the current state/summary of a game.
     *
     * @param gameId
     *            the game to query
     * @return the game summary
     * @throws RestGameClientException
     *             if the game is not found or request fails
     */
    public GameSummary getGameSummary(String gameId) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/games/" + gameId))
                    .header("Authorization", "Bearer " + jwt).GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RestGameClientException(
                        "getGameSummary returned " + response.statusCode() + ": " + response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), GameSummary.class);
        } catch (RestGameClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RestGameClientException("Failed to get game summary for " + gameId, e);
        }
    }

    /**
     * Register a community-hosted game with the WAN server.
     *
     * @param req
     *            registration request containing game name, WebSocket URL, and
     *            profile
     * @return the created {@link GameSummary} (including the server-assigned
     *         {@code gameId})
     * @throws RestGameClientException
     *             if the server rejects the registration
     */
    public GameSummary registerCommunityGame(CommunityGameRegisterRequest req) {
        try {
            String body = OBJECT_MAPPER.writeValueAsString(req);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/games/community"))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new RestGameClientException(
                        "registerCommunityGame returned " + response.statusCode() + ": " + response.body());
            }
            return OBJECT_MAPPER.readValue(response.body(), GameSummary.class);
        } catch (RestGameClientException e) {
            throw e;
        } catch (Exception e) {
            throw new RestGameClientException("Failed to register community game", e);
        }
    }

    /**
     * Send a heartbeat to keep a community-hosted game alive on the WAN server.
     *
     * <p>
     * Swallows exceptions — heartbeat failures are non-fatal.
     *
     * @param gameId
     *            server-assigned game ID returned by {@link #registerCommunityGame}
     */
    public void sendHeartbeat(String gameId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/games/" + gameId + "/heartbeat"))
                    .header("Authorization", "Bearer " + jwt).POST(HttpRequest.BodyPublishers.noBody()).build();

            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                logger.warn("Heartbeat for game {} returned {}", gameId, response.statusCode());
            }
        } catch (Exception e) {
            logger.warn("Failed to send heartbeat for game {}", gameId, e);
        }
    }

    /**
     * Cancel / deregister a game on the WAN server.
     *
     * <p>
     * Swallows exceptions — best-effort deregistration.
     *
     * @param gameId
     *            the game to cancel
     */
    public void cancelGame(String gameId) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/api/v1/games/" + gameId))
                    .header("Authorization", "Bearer " + jwt).DELETE().build();

            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.warn("Failed to cancel game {}", gameId, e);
        }
    }

    /** Thrown when a REST call to the game server fails with an error response. */
    public static class RestGameClientException extends RuntimeException {
        public RestGameClientException(String message) {
            super(message);
        }

        public RestGameClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
