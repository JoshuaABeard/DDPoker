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
 * Tests for {@code POST /cheat}.
 *
 * <p>These tests verify HTTP-layer behaviour (auth, method, request validation) without
 * a running {@code GameEngine}. The 409 "no game running" response is tested for all
 * actions that reach game lookup. End-to-end correctness of state mutation is verified
 * by scenario scripts that run against a live game instance.
 */
class CheatHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-cheat-test-");
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
                .uri(URI.create("http://127.0.0.1:" + port + "/cheat"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"setLevel\",\"level\":2}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    @Test
    void getMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/cheat"))
                .GET()
                .header("X-Control-Key", apiKey)
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(405);
    }

    // -------------------------------------------------------------------------
    // Request validation — all checked before game lookup
    // -------------------------------------------------------------------------

    @Test
    void emptyBody_returns400() throws Exception {
        assertThat(post("").statusCode()).isEqualTo(400);
    }

    @Test
    void missingAction_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"level\": 2}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void unknownAction_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\": \"doSomethingWeird\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
        JsonNode json = json(resp);
        assertThat(json.get("error").asText()).isEqualTo("BadRequest");
        assertThat(json.get("message").asText()).contains("Unknown action");
    }

    // -------------------------------------------------------------------------
    // setChips — validation
    // -------------------------------------------------------------------------

    @Test
    void setChips_missingSeat_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setChips\",\"amount\":100}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void setChips_missingAmount_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setChips\",\"seat\":0}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void setChips_negativeAmount_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setChips\",\"seat\":0,\"amount\":-1}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void setChips_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setChips\",\"seat\":0,\"amount\":500}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("error").asText()).isEqualTo("Conflict");
    }

    // -------------------------------------------------------------------------
    // setLevel — validation
    // -------------------------------------------------------------------------

    @Test
    void setLevel_missingLevel_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setLevel\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void setLevel_levelZero_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setLevel\",\"level\":0}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void setLevel_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setLevel\",\"level\":3}");
        assertThat(resp.statusCode()).isEqualTo(409);
    }

    // -------------------------------------------------------------------------
    // setButton — validation
    // -------------------------------------------------------------------------

    @Test
    void setButton_missingSeat_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setButton\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void setButton_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"setButton\",\"seat\":1}");
        assertThat(resp.statusCode()).isEqualTo(409);
    }

    // -------------------------------------------------------------------------
    // eliminatePlayer — validation
    // -------------------------------------------------------------------------

    @Test
    void eliminatePlayer_missingSeat_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"eliminatePlayer\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void eliminatePlayer_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"action\":\"eliminatePlayer\",\"seat\":2}");
        assertThat(resp.statusCode()).isEqualTo(409);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/cheat"))
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
