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

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.protocol.dto.GameSettingsRequest;
import com.donohoedigital.games.poker.protocol.dto.GameSummary;
import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link RestGameClient#updateSettings} and
 * {@link RestGameClient#kickPlayer}.
 */
class RestGameClientKickSettingsTest {

    private static final String GAME_SUMMARY_JSON = "{\"gameId\":\"g1\",\"name\":\"Test\",\"hostingType\":\"SERVER\","
            + "\"status\":\"WAITING_FOR_PLAYERS\",\"ownerName\":\"Alice\",\"playerCount\":1,\"maxPlayers\":6,"
            + "\"isPrivate\":false,\"wsUrl\":null,\"blinds\":{\"smallBlind\":10,\"bigBlind\":20,\"ante\":0},"
            + "\"createdAt\":null,\"startedAt\":null,\"players\":[]}";

    private HttpServer testServer;
    private RestGameClient client;
    private String serverUrl;

    @BeforeEach
    void setUp() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress(0), 0);
        testServer.start();
        int port = testServer.getAddress().getPort();
        serverUrl = "http://localhost:" + port;
        client = new RestGameClient(serverUrl, "test-jwt");
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // updateSettings
    // -------------------------------------------------------------------------

    @Test
    void updateSettings_success_returnsGameSummary() {
        testServer.createContext("/api/v1/games/g1/settings", exchange -> {
            byte[] bytes = GAME_SUMMARY_JSON.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        GameSummary result = client.updateSettings("g1", new GameSettingsRequest("Test", 6, null, null));

        assertThat(result.gameId()).isEqualTo("g1");
        assertThat(result.name()).isEqualTo("Test");
        assertThat(result.maxPlayers()).isEqualTo(6);
    }

    @Test
    void updateSettings_sendsPutWithCorrectPathAndBody() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        testServer.createContext("/api/v1/games/g1/settings", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = GAME_SUMMARY_JSON.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.updateSettings("g1", new GameSettingsRequest("NewName", 8, null, null));

        assertThat(capturedMethod.get()).isEqualTo("PUT");
        assertThat(capturedPath.get()).isEqualTo("/api/v1/games/g1/settings");
        assertThat(capturedAuth.get()).isEqualTo("Bearer test-jwt");
        assertThat(capturedBody.get()).contains("\"name\"");
        assertThat(capturedBody.get()).contains("NewName");
        assertThat(capturedBody.get()).contains("\"maxPlayers\"");
    }

    @Test
    void updateSettings_nonSuccess_throwsRestGameClientException() {
        testServer.createContext("/api/v1/games/g1/settings", exchange -> {
            byte[] bytes = "Forbidden".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.updateSettings("g1", new GameSettingsRequest("X", 2, null, null)))
                .isInstanceOf(RestGameClient.RestGameClientException.class)
                .hasMessageContaining("updateSettings returned 403");
    }

    // -------------------------------------------------------------------------
    // kickPlayer
    // -------------------------------------------------------------------------

    @Test
    void kickPlayer_success_doesNotThrow() {
        testServer.createContext("/api/v1/games/g1/kick", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });

        assertThatNoException().isThrownBy(() -> client.kickPlayer("g1", 42L));
    }

    @Test
    void kickPlayer_sendsPostWithCorrectPathAndBody() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedPath = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        testServer.createContext("/api/v1/games/g1/kick", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        });

        client.kickPlayer("g1", 99L);

        assertThat(capturedMethod.get()).isEqualTo("POST");
        assertThat(capturedPath.get()).isEqualTo("/api/v1/games/g1/kick");
        assertThat(capturedAuth.get()).isEqualTo("Bearer test-jwt");
        assertThat(capturedBody.get()).contains("\"profileId\"");
        assertThat(capturedBody.get()).contains("99");
    }

    @Test
    void kickPlayer_nonSuccess_throwsRestGameClientException() {
        testServer.createContext("/api/v1/games/g1/kick", exchange -> {
            byte[] bytes = "Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.kickPlayer("g1", 42L))
                .isInstanceOf(RestGameClient.RestGameClientException.class)
                .hasMessageContaining("kickPlayer returned 404");
    }
}
