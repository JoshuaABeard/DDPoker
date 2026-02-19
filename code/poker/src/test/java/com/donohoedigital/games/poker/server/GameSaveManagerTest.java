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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GameSaveManager} using a JDK embedded HTTP server and
 * Mockito.
 */
@ExtendWith(MockitoExtension.class)
class GameSaveManagerTest {

    @Mock
    private EmbeddedGameServer embeddedServer;

    private HttpServer testServer;
    private GameSaveManager manager;

    @BeforeEach
    void setUp() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress(0), 0);
        testServer.start();
        manager = new GameSaveManager(embeddedServer);
    }

    /**
     * Configures the shared embeddedServer mock for tests that call
     * loadResumableGames().
     */
    private void stubEmbeddedServer() {
        when(embeddedServer.getPort()).thenReturn(testServer.getAddress().getPort());
        when(embeddedServer.getLocalUserJwt()).thenReturn("test-jwt");
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    @Test
    void initiallyNoResumableGames() {
        assertThat(manager.hasResumableGames()).isFalse();
        assertThat(manager.getResumableGames()).isEmpty();
    }

    @Test
    void loadResumableGamesFiltersInProgressAndPaused() {
        stubEmbeddedServer();
        testServer.createContext("/api/v1/games", exchange -> {
            String games = "["
                    + "{\"gameId\":\"g1\",\"name\":\"Game1\",\"ownerName\":\"user\",\"playerCount\":2,\"maxPlayers\":9,\"status\":\"IN_PROGRESS\"},"
                    + "{\"gameId\":\"g2\",\"name\":\"Game2\",\"ownerName\":\"user\",\"playerCount\":1,\"maxPlayers\":9,\"status\":\"PAUSED\"},"
                    + "{\"gameId\":\"g3\",\"name\":\"Game3\",\"ownerName\":\"user\",\"playerCount\":0,\"maxPlayers\":9,\"status\":\"WAITING_FOR_PLAYERS\"}"
                    + "]";
            String json = "{\"games\":" + games + ",\"total\":3,\"page\":0,\"pageSize\":50}";
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        manager.loadResumableGames();

        assertThat(manager.hasResumableGames()).isTrue();
        assertThat(manager.getResumableGames()).hasSize(2);
        assertThat(manager.getResumableGames()).extracting("gameId").containsExactly("g1", "g2");
    }

    @Test
    void loadResumableGamesEmptyWhenNoMatchingStatus() {
        stubEmbeddedServer();
        testServer.createContext("/api/v1/games", exchange -> {
            String games = "["
                    + "{\"gameId\":\"g1\",\"name\":\"Game1\",\"ownerName\":\"user\",\"playerCount\":0,\"maxPlayers\":9,\"status\":\"WAITING_FOR_PLAYERS\"},"
                    + "{\"gameId\":\"g2\",\"name\":\"Game2\",\"ownerName\":\"user\",\"playerCount\":9,\"maxPlayers\":9,\"status\":\"COMPLETED\"}"
                    + "]";
            String json = "{\"games\":" + games + ",\"total\":2,\"page\":0,\"pageSize\":50}";
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        manager.loadResumableGames();

        assertThat(manager.hasResumableGames()).isFalse();
        assertThat(manager.getResumableGames()).isEmpty();
    }

    @Test
    void loadResumableGamesHandlesEmptyList() {
        stubEmbeddedServer();
        testServer.createContext("/api/v1/games", exchange -> {
            byte[] response = "{\"games\":[],\"total\":0,\"page\":0,\"pageSize\":50}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        manager.loadResumableGames();

        assertThat(manager.hasResumableGames()).isFalse();
    }

    @Test
    void loadResumableGamesSilentlyHandlesServerError() {
        stubEmbeddedServer();
        testServer.createContext("/api/v1/games", exchange -> {
            exchange.sendResponseHeaders(503, 0);
            exchange.getResponseBody().close();
        });

        // Should not throw; errors are swallowed and logged
        manager.loadResumableGames();

        assertThat(manager.hasResumableGames()).isFalse();
    }

    @Test
    void loadResumableGamesSilentlyHandlesConnectionFailure() {
        // Use a port with no server — connection refused
        EmbeddedGameServer unreachable = org.mockito.Mockito.mock(EmbeddedGameServer.class);
        when(unreachable.getPort()).thenReturn(1); // port 1 is privileged, always refused
        when(unreachable.getLocalUserJwt()).thenReturn("jwt");

        GameSaveManager mgr = new GameSaveManager(unreachable);

        // Should not throw
        mgr.loadResumableGames();

        assertThat(mgr.hasResumableGames()).isFalse();
    }

    @Test
    void getResumableGamesReturnsUnmodifiableList() {
        stubEmbeddedServer();
        testServer.createContext("/api/v1/games", exchange -> {
            String json = "{\"games\":[{\"gameId\":\"g1\",\"name\":\"Game1\",\"ownerName\":\"user\",\"playerCount\":1,\"maxPlayers\":9,\"status\":\"IN_PROGRESS\"}],\"total\":1,\"page\":0,\"pageSize\":50}";
            byte[] response = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        manager.loadResumableGames();

        assertThat(manager.getResumableGames()).hasSize(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> manager.getResumableGames().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
