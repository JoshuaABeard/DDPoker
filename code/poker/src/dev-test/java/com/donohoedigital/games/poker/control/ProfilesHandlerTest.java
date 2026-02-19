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
 * Tests for profile management endpoints:
 * <ul>
 *   <li>{@code GET /profiles}         — list profiles</li>
 *   <li>{@code POST /profiles}        — create profile</li>
 *   <li>{@code GET /profiles/default} — get default profile</li>
 * </ul>
 * <p>
 * These tests validate the HTTP layer only; they do not exercise the full
 * profile file I/O (which requires a running desktop app with config loaded).
 */
class ProfilesHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-profiles-test-");
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
    // GET /profiles
    // -------------------------------------------------------------------------

    @Test
    void listProfiles_returns200WithProfilesArray() throws Exception {
        HttpResponse<String> resp = get("/profiles");
        // May return 200 with empty list or 200 with real profiles — both OK
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode body = json(resp);
        assertThat(body.has("profiles")).isTrue();
        assertThat(body.get("profiles").isArray()).isTrue();
    }

    @Test
    void listProfiles_missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/profiles"))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // POST /profiles
    // -------------------------------------------------------------------------

    @Test
    void createProfile_emptyBody_returns400() throws Exception {
        HttpResponse<String> resp = post("/profiles", "");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void createProfile_missingName_returns400() throws Exception {
        HttpResponse<String> resp = post("/profiles", "{\"email\":\"test@test.com\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void createProfile_blankName_returns400() throws Exception {
        HttpResponse<String> resp = post("/profiles", "{\"name\":\"   \"}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void createProfile_invalidJson_returns400() throws Exception {
        HttpResponse<String> resp = post("/profiles", "{bad json}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    // -------------------------------------------------------------------------
    // GET /profiles/default
    // -------------------------------------------------------------------------

    @Test
    void getDefault_returns200() throws Exception {
        // Without a running game, getDefaultProfile() may fail or return null —
        // the handler gracefully handles both cases and returns 200.
        HttpResponse<String> resp = get("/profiles/default");
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode body = json(resp);
        assertThat(body.has("defaultProfile")).isTrue();
    }

    @Test
    void getDefault_missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/profiles/default"))
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .header("X-Control-Key", apiKey)
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
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
