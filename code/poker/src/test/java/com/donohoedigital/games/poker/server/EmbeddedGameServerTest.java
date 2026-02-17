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

import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link EmbeddedGameServer}.
 *
 * <p>
 * Tests that the embedded Spring Boot server starts on a random port, responds
 * to HTTP requests, and stops cleanly.
 */
@Tag("integration")
@Tag("slow")
class EmbeddedGameServerTest {

    private EmbeddedGameServer server;

    @BeforeEach
    void setUp() {
        server = new EmbeddedGameServer();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void startsOnRandomPort() throws Exception {
        server.start();

        assertThat(server.isRunning()).isTrue();
        assertThat(server.getPort()).isGreaterThan(0);
        assertThat(server.getPort()).isLessThanOrEqualTo(65535);
    }

    @Test
    void httpHealthCheckResponds() throws Exception {
        server.start();

        int port = server.getPort();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/v1/games"))
                .timeout(Duration.ofSeconds(5)).GET().build();

        // Any HTTP response (even 401 Unauthorized) proves the server is listening
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isBetween(200, 499);
    }

    @Test
    void stopsCleanly() throws Exception {
        server.start();
        int port = server.getPort();
        assertThat(server.isRunning()).isTrue();

        server.stop();

        assertThat(server.isRunning()).isFalse();
        assertThat(server.getPort()).isEqualTo(-1);

        // Verify port is released — attempt to connect should fail
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/v1/games"))
                .timeout(Duration.ofSeconds(2)).GET().build();
        assertThatThrownBy(() -> client.send(request, HttpResponse.BodyHandlers.ofString()))
                .isInstanceOf(java.io.IOException.class);
    }

    @Test
    void startIsIdempotent() throws Exception {
        server.start();
        int firstPort = server.getPort();

        // Second start should be a no-op
        server.start();

        assertThat(server.getPort()).isEqualTo(firstPort);
    }

    @Test
    void stopBeforeStartIsNoOp() {
        assertThatNoException().isThrownBy(() -> server.stop());
        assertThat(server.isRunning()).isFalse();
    }
}
