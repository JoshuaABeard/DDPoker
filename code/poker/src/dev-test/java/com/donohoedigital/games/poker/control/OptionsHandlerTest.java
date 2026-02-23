/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
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
package com.donohoedigital.games.poker.control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code GET /options} and {@code POST /options}.
 *
 * <p>These tests verify the HTTP layer (auth, method, and request validation) independently of
 * whether a {@code GameEngine} is running. End-to-end correctness of option reads/writes is
 * verified via the dev control server test scripts that run against a live game instance.
 */
class OptionsHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-options-test-");
        server = new TestableServer(tempDir);
        server.start();
        port = Integer.parseInt(Files.readString(tempDir.resolve(GameControlServer.PORT_FILE)).strip());
        apiKey = Files.readString(tempDir.resolve(GameControlServer.KEY_FILE)).strip();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) server.stop();
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    // -------------------------------------------------------------------------
    // Auth and method enforcement
    // -------------------------------------------------------------------------

    @Test
    void get_missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/options"))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    @Test
    void post_missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/options"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"cheat.neverbroke\": true}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    @Test
    void delete_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/options"))
                .DELETE()
                .header("X-Control-Key", apiKey)
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(405);
    }

    // -------------------------------------------------------------------------
    // Validation of POST body — unknown key (detected before GameEngine access)
    // -------------------------------------------------------------------------

    @Test
    void post_unknownKey_returns400() throws Exception {
        // OptionsHandler validates keys before reading/writing prefs, so this
        // returns 400 even without a running GameEngine.
        HttpResponse<String> resp = post("{\"unknown.key\": true}");
        assertThat(resp.statusCode()).isEqualTo(400);
        JsonNode json = MAPPER.readTree(resp.body());
        assertThat(json.get("error").asText()).isEqualTo("InvalidOptions");
        assertThat(json.get("details").has("unknown.key")).isTrue();
    }

    @Test
    void post_wrongTypeForBoolean_returns400() throws Exception {
        // "cheat.neverbroke" expects boolean, not a number
        HttpResponse<String> resp = post("{\"cheat.neverbroke\": 123}");
        assertThat(resp.statusCode()).isEqualTo(400);
        JsonNode json = MAPPER.readTree(resp.body());
        assertThat(json.get("error").asText()).isEqualTo("InvalidOptions");
    }

    @Test
    void post_wrongTypeForInt_returns400() throws Exception {
        // "gameplay.aiDelayMs" expects a number, not a boolean
        HttpResponse<String> resp = post("{\"gameplay.aiDelayMs\": true}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void post_emptyBody_returns400() throws Exception {
        HttpResponse<String> resp = post("");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/options"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("X-Control-Key", apiKey)
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static class TestableServer extends GameControlServer {
        private final Path dir;
        TestableServer(Path dir) { this.dir = dir; }
        @Override Path ddPokerDir() { return dir; }
    }
}
