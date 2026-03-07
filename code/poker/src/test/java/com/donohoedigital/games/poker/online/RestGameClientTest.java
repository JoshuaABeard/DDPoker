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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.protocol.dto.GameSummary;
import com.sun.net.httpserver.HttpServer;

class RestGameClientTest {

    private HttpServer testServer;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress(0), 0);
        testServer.start();
        port = testServer.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    @Test
    void restGameClientException_messageConstructor() {
        RestGameClient.RestGameClientException ex = new RestGameClient.RestGameClientException("test error");

        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void restGameClientException_messageCauseConstructor() {
        RuntimeException cause = new RuntimeException("root cause");
        RestGameClient.RestGameClientException ex = new RestGameClient.RestGameClientException("wrapped", cause);

        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void joinGame_passwordWithSpecialCharacters_encodedCorrectly() {
        AtomicReference<String> capturedBody = new AtomicReference<>();

        testServer.createContext("/api/v1/games/g1/join", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"wsUrl\":\"ws://localhost/ws/games/g1\",\"gameId\":\"g1\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        RestGameClient client = new RestGameClient("http://localhost:" + port, "jwt-123");
        client.joinGame("g1", "pass\"word\\with\\special");

        // The JSON body must be valid — password must be properly escaped
        assertThat(capturedBody.get()).contains("pass\\\"word\\\\with\\\\special");
    }

    @Test
    void listGames_acceptsHostPortBaseUrlWithoutScheme() {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();

        testServer.createContext("/api/v1/games", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));

            String json = "{\"games\":[],\"total\":0,\"page\":0,\"pageSize\":50}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        RestGameClient client = new RestGameClient("localhost:" + port, "jwt-123");
        List<GameSummary> games = client.listGames();

        assertThat(games).isEmpty();
        assertThat(path.get()).isEqualTo("/api/v1/games");
        assertThat(auth.get()).isEqualTo("Bearer jwt-123");
    }

    @Test
    void cancelGame_serverRejectsWithForbidden_doesNotThrow() {
        testServer.createContext("/api/v1/games/g1", exchange -> {
            byte[] bytes = "Forbidden".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        RestGameClient client = new RestGameClient("http://localhost:" + port, "jwt-123");

        // cancelGame is best-effort — server errors are intentionally swallowed
        assertThatNoException().isThrownBy(() -> client.cancelGame("g1"));
    }

    @Test
    void sendHeartbeat_serverReturnsError_doesNotThrow() {
        testServer.createContext("/api/v1/games/g1/heartbeat", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.getResponseBody().close();
        });

        RestGameClient client = new RestGameClient("http://localhost:" + port, "jwt-123");

        // heartbeat is non-fatal — errors are intentionally swallowed
        assertThatNoException().isThrownBy(() -> client.sendHeartbeat("g1"));
    }

    @Test
    void joinGame_success_returnsJoinResponse() {
        testServer.createContext("/api/v1/games/g1/join", exchange -> {
            String json = "{\"wsUrl\":\"ws://localhost/ws/games/g1\",\"gameId\":\"g1\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        RestGameClient client = new RestGameClient("http://localhost:" + port, "jwt-123");
        var response = client.joinGame("g1", null);

        assertThat(response.gameId()).isEqualTo("g1");
        assertThat(response.wsUrl()).isEqualTo("ws://localhost/ws/games/g1");
    }

    @Test
    void joinGame_serverReturnsNotFound_throwsWithStatusCode() {
        testServer.createContext("/api/v1/games/g1/join", exchange -> {
            byte[] bytes = "Game not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        RestGameClient client = new RestGameClient("http://localhost:" + port, "jwt-123");

        assertThatThrownBy(() -> client.joinGame("g1", null)).isInstanceOf(RestGameClient.RestGameClientException.class)
                .hasMessageContaining("joinGame returned 404");
    }
}
