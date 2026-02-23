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
 * Tests for {@code GET /validate}.
 */
class ValidateHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-validate-test-");
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
    void missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/validate"))
                .GET()
                .build();
        assertThat(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
    }

    @Test
    void postMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/validate"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-Control-Key", apiKey)
                .build();
        assertThat(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(405);
    }

    // -------------------------------------------------------------------------
    // No game running — vacuous pass
    // -------------------------------------------------------------------------

    @Test
    void noGame_returnsValidWithEmptyTables() throws Exception {
        JsonNode json = get();
        assertThat(json.get("chipConservation").get("valid").asBoolean()).isTrue();
        assertThat(json.get("chipConservation").get("tables").isArray()).isTrue();
        assertThat(json.get("chipConservation").get("tables").size()).isEqualTo(0);
        assertThat(json.get("inputModeConsistent").asBoolean()).isTrue();
        assertThat(json.get("warnings").isArray()).isTrue();
        assertThat(json.get("warnings").size()).isEqualTo(0);
    }

    @Test
    void noGame_responseHasAllExpectedTopLevelFields() throws Exception {
        JsonNode json = get();
        assertThat(json.has("chipConservation")).isTrue();
        assertThat(json.has("inputModeConsistent")).isTrue();
        assertThat(json.has("warnings")).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JsonNode get() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/validate"))
                .GET()
                .header("X-Control-Key", apiKey)
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(200);
        return MAPPER.readTree(resp.body());
    }

    private static class TestableServer extends GameControlServer {
        private final Path dir;
        TestableServer(Path dir) { this.dir = dir; }
        @Override Path ddPokerDir() { return dir; }
    }
}
