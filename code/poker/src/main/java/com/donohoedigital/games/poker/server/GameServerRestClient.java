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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.gameserver.GameConfig;
import com.donohoedigital.games.poker.gameserver.controller.TournamentProfileConverter;
import com.donohoedigital.games.poker.gameserver.dto.CreateGameResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameListResponse;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.model.TournamentProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * HTTP client wrapper for the embedded game server REST API.
 *
 * <p>
 * Uses {@link HttpClient} (JDK built-in) and Jackson for serialization. All
 * calls are synchronous and must not be called from the Swing EDT.
 */
public class GameServerRestClient {

    private static final Logger logger = LogManager.getLogger(GameServerRestClient.class);

    // Shared instance. ObjectMapper is thread-safe once configured.
    // Avoids creating a new instance per GameServerRestClient.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final int port;
    private final HttpClient http;
    private final TournamentProfileConverter converter;

    public GameServerRestClient(int port) {
        this.port = port;
        this.http = HttpClient.newHttpClient();
        this.converter = new TournamentProfileConverter();
    }

    /**
     * Create a practice game on the embedded server.
     *
     * <p>
     * Converts the {@link TournamentProfile} to a {@link GameConfig}, posts it to
     * {@code POST /api/v1/games/practice}, and returns the game ID.
     *
     * @param profile
     *            tournament profile from the desktop UI
     * @param aiNames
     *            names to use for the AI players (one per seat)
     * @param aiSkillLevel
     *            AI skill level (1-7)
     * @param jwt
     *            JWT token for the local user
     * @param humanDisplayName
     *            display name for the human player (uses profile name instead of OS
     *            username)
     * @return game ID
     * @throws GameServerClientException
     *             if the HTTP call fails or the server returns an error
     */
    public String createPracticeGame(TournamentProfile profile, List<String> aiNames, int aiSkillLevel, String jwt,
            String humanDisplayName) {
        GameConfig config = converter.convert(profile)
                .withAiPlayers(converter.buildAiPlayers(profile, aiNames, aiSkillLevel))
                .withHumanDisplayName(humanDisplayName);
        try {
            String body = OBJECT_MAPPER.writeValueAsString(config);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/games/practice"))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new GameServerClientException(
                        "Server returned " + response.statusCode() + ": " + response.body());
            }

            CreateGameResponse resp = OBJECT_MAPPER.readValue(response.body(), CreateGameResponse.class);
            logger.debug("Practice game created: {}", resp.gameId());
            return resp.gameId();
        } catch (GameServerClientException e) {
            throw e;
        } catch (Exception e) {
            throw new GameServerClientException("Failed to create practice game", e);
        }
    }

    /**
     * List all games visible to the caller.
     *
     * @param jwt
     *            JWT token for authentication
     * @return list of game summaries
     * @throws GameServerClientException
     *             if the call fails
     */
    public List<GameSummary> listGames(String jwt) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/v1/games"))
                    .header("Authorization", "Bearer " + jwt).GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new GameServerClientException(
                        "Server returned " + response.statusCode() + ": " + response.body());
            }

            GameListResponse result = OBJECT_MAPPER.readValue(response.body(), GameListResponse.class);
            return result.games();
        } catch (GameServerClientException e) {
            throw e;
        } catch (Exception e) {
            throw new GameServerClientException("Failed to list games", e);
        }
    }

    /** Thrown when a REST call to the embedded game server fails. */
    public static class GameServerClientException extends RuntimeException {
        public GameServerClientException(String message) {
            super(message);
        }

        public GameServerClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
