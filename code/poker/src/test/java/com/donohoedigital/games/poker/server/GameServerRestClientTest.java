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

import com.donohoedigital.games.poker.model.TournamentProfile;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GameServerRestClient} using a JDK embedded HTTP server.
 */
class GameServerRestClientTest {

    private HttpServer testServer;
    private int port;
    private GameServerRestClient client;
    private TournamentProfile profile;

    @BeforeEach
    void setUp() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress(0), 0);
        testServer.start();
        port = testServer.getAddress().getPort();
        client = new GameServerRestClient(port);

        profile = new TournamentProfile("Test");
        profile.setNumPlayers(3);
        profile.setBuyinChips(1000);
        profile.setLevel(1, 0, 10, 20, 15);
        profile.fixLevels();
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    @Test
    void createPracticeGameReturnsGameId() {
        testServer.createContext("/api/v1/games/practice", exchange -> {
            byte[] response = "{\"gameId\":\"practice-xyz\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        String gameId = client.createPracticeGame(profile, List.of("Bot1"), 4, "test-jwt");

        assertThat(gameId).isEqualTo("practice-xyz");
    }

    @Test
    void createPracticeGameSendsAuthHeader() {
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        testServer.createContext("/api/v1/games/practice", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"gameId\":\"game-1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        client.createPracticeGame(profile, List.of(), 4, "my-jwt-token");

        assertThat(capturedAuth.get()).isEqualTo("Bearer my-jwt-token");
    }

    @Test
    void createPracticeGameSendsJsonBody() {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        testServer.createContext("/api/v1/games/practice", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"gameId\":\"game-2\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        client.createPracticeGame(profile, List.of("Bot"), 4, "jwt");

        assertThat(capturedBody.get()).contains("\"name\"");
        assertThat(capturedBody.get()).contains("\"blindStructure\"");
        assertThat(capturedBody.get()).contains("\"startingChips\"");
    }

    @Test
    void createPracticeGameThrowsOnServerError() {
        testServer.createContext("/api/v1/games/practice", exchange -> {
            byte[] response = "{\"error\":\"server error\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        assertThatThrownBy(() -> client.createPracticeGame(profile, List.of(), 4, "jwt"))
                .isInstanceOf(GameServerRestClient.GameServerClientException.class).hasMessageContaining("500");
    }

    @Test
    void listGamesReturnsGameSummaries() {
        testServer.createContext("/api/v1/games", exchange -> {
            String json = "[{\"gameId\":\"g1\",\"name\":\"Test\",\"ownerName\":\"user\",\"playerCount\":1,\"maxPlayers\":9,\"status\":\"WAITING_FOR_PLAYERS\"}]";
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        var games = client.listGames("test-jwt");

        assertThat(games).hasSize(1);
        assertThat(games.get(0).gameId()).isEqualTo("g1");
    }

    @Test
    void listGamesThrowsOnServerError() {
        testServer.createContext("/api/v1/games", exchange -> {
            exchange.sendResponseHeaders(503, 0);
            exchange.getResponseBody().close();
        });

        assertThatThrownBy(() -> client.listGames("jwt"))
                .isInstanceOf(GameServerRestClient.GameServerClientException.class).hasMessageContaining("503");
    }
}
