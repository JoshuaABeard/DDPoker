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
     * Create a practice game on the embedded server (no practice config).
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
        return createPracticeGame(profile, aiNames, aiSkillLevel, jwt, humanDisplayName, null);
    }

    /**
     * Create a practice game on the embedded server with practice configuration.
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
     *            display name for the human player (null = use JWT username)
     * @param practiceConfig
     *            practice mode timing configuration (null = server defaults)
     * @return game ID
     * @throws GameServerClientException
     *             if the HTTP call fails or the server returns an error
     */
    public String createPracticeGame(TournamentProfile profile, List<String> aiNames, int aiSkillLevel, String jwt,
            String humanDisplayName, GameConfig.PracticeConfig practiceConfig) {
        GameConfig config = converter.convert(profile)
                .withAiPlayers(converter.buildAiPlayers(profile, aiNames, aiSkillLevel))
                .withHumanDisplayName(humanDisplayName).withPracticeConfig(practiceConfig);
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

    // -------------------------------------------------------------------------
    // Cheat endpoints (practice mode only, owner only)
    // -------------------------------------------------------------------------

    /**
     * Set a player's chip count.
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param playerId
     *            player ID (negative for AI players)
     * @param chipCount
     *            new chip count
     */
    public void cheatChips(String gameId, String jwt, int playerId, int chipCount) {
        String body = "{\"playerId\":" + playerId + ",\"chipCount\":" + chipCount + "}";
        cheatPost(gameId, jwt, "chips", body);
    }

    /**
     * Change a player's display name.
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param playerId
     *            player ID
     * @param name
     *            new display name
     */
    public void cheatName(String gameId, String jwt, int playerId, String name) {
        try {
            String escapedName = OBJECT_MAPPER.writeValueAsString(name);
            String body = "{\"playerId\":" + playerId + ",\"name\":" + escapedName + "}";
            cheatPost(gameId, jwt, "name", body);
        } catch (Exception e) {
            throw new GameServerClientException("Failed to serialize cheat name request", e);
        }
    }

    /**
     * Jump to a blind level.
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param level
     *            0-based blind level index
     */
    public void cheatLevel(String gameId, String jwt, int level) {
        String body = "{\"level\":" + level + "}";
        cheatPost(gameId, jwt, "level", body);
    }

    /**
     * Move the dealer button to the given seat. Only allowed between hands.
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param seat
     *            0-based seat index
     */
    public void cheatButton(String gameId, String jwt, int seat) {
        String body = "{\"seat\":" + seat + "}";
        cheatPost(gameId, jwt, "button", body);
    }

    /**
     * Eliminate a player by zeroing their chips.
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param playerId
     *            player ID to eliminate
     */
    public void cheatRemovePlayer(String gameId, String jwt, int playerId) {
        String body = "{\"playerId\":" + playerId + "}";
        cheatPost(gameId, jwt, "remove-player", body);
    }

    /**
     * Swap a card in the current hand.
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param location
     *            card location: {@code "COMMUNITY:<index>"} or
     *            {@code "PLAYER:<playerId>:<index>"}
     * @param newCard
     *            card string such as {@code "Ah"} or {@code "Kd"}
     */
    public void cheatCard(String gameId, String jwt, String location, String newCard) {
        try {
            String escapedLocation = OBJECT_MAPPER.writeValueAsString(location);
            String escapedCard = OBJECT_MAPPER.writeValueAsString(newCard);
            String body = "{\"location\":" + escapedLocation + ",\"newCard\":" + escapedCard + "}";
            cheatPost(gameId, jwt, "card", body);
        } catch (Exception e) {
            throw new GameServerClientException("Failed to serialize cheat card request", e);
        }
    }

    /**
     * Override the AI skill level for a player (1–10).
     *
     * @param gameId
     *            practice game ID
     * @param jwt
     *            owner JWT
     * @param playerId
     *            AI player ID
     * @param skillLevel
     *            skill level 1 (weakest) to 10 (strongest)
     */
    public void cheatAiStrategy(String gameId, String jwt, int playerId, int skillLevel) {
        String body = "{\"playerId\":" + playerId + ",\"skillLevel\":" + skillLevel + "}";
        cheatPost(gameId, jwt, "ai-strategy", body);
    }

    /** POST to {@code /api/v1/games/{gameId}/cheat/{action}} and expect 200. */
    private void cheatPost(String gameId, String jwt, String action, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/api/v1/games/" + gameId + "/cheat/" + action))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new GameServerClientException(
                        "Cheat " + action + " returned " + response.statusCode() + ": " + response.body());
            }
            logger.debug("Cheat {} game={} ok", action, gameId);
        } catch (GameServerClientException e) {
            throw e;
        } catch (Exception e) {
            throw new GameServerClientException("Failed cheat " + action, e);
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
