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
 * Tests for {@code POST /game/start}.
 * <p>
 * Without a running desktop app (no PokerMain), the handler still validates
 * the HTTP layer correctly and returns 200 (the actual game start is async).
 */
class GameStartHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-gamestart-test-");
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

    @Test
    void start_defaultConfig_returns200Accepted() throws Exception {
        HttpResponse<String> resp = post("{}");
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode body = json(resp);
        assertThat(body.get("accepted").asBoolean()).isTrue();
    }

    @Test
    void start_customConfig_returns200Accepted() throws Exception {
        String body = """
                {
                  "numPlayers": 2,
                  "buyinChips": 1000,
                  "blindLevels": [
                    {"small": 50, "big": 100, "ante": 0, "minutes": 5}
                  ],
                  "rebuys": false,
                  "addons": false
                }
                """;
        HttpResponse<String> resp = post(body);
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("accepted").asBoolean()).isTrue();
    }

    @Test
    void start_emptyBody_returns400() throws Exception {
        HttpResponse<String> resp = post("");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void start_invalidJson_returns400() throws Exception {
        HttpResponse<String> resp = post("{not valid json");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void start_getMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/game/start"))
                .GET()
                .header("X-Control-Key", apiKey)
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(405);
    }

    @Test
    void start_missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/game/start"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/game/start"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("X-Control-Key", apiKey)
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> resp) throws Exception {
        return MAPPER.readTree(resp.body());
    }

    private static class TestableServer extends GameControlServer {
        private final Path dir;
        TestableServer(Path dir) { this.dir = dir; }
        @Override Path ddPokerDir() { return dir; }
    }
}
