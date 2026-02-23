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

import com.donohoedigital.games.poker.gameserver.CardInjectionRegistry;
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
 * Tests for {@code POST /cards/inject} and {@code DELETE /cards/inject}.
 */
class CardInjectHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-cardinject-test-");
        server = new TestableServer(tempDir);
        server.start();
        port = Integer.parseInt(Files.readString(tempDir.resolve(GameControlServer.PORT_FILE)).strip());
        apiKey = Files.readString(tempDir.resolve(GameControlServer.KEY_FILE)).strip();
    }

    @AfterEach
    void tearDown() throws Exception {
        CardInjectionRegistry.clear();
        if (server != null) server.stop();
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    // -------------------------------------------------------------------------
    // POST /cards/inject — explicit card list
    // -------------------------------------------------------------------------

    @Test
    void injectCards_validList_returns200AndAccepted() throws Exception {
        String body = """
                {"cards": ["As","Ks","2d","3c","Qh","Jd","Ts","9s","8h","7c","6d","5s","4h"]}
                """;
        HttpResponse<String> resp = post(body);
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode json = json(resp);
        assertThat(json.get("accepted").asBoolean()).isTrue();
        assertThat(json.get("cardCount").asInt()).isEqualTo(13);
    }

    @Test
    void injectCards_validList_registryHasCards() throws Exception {
        post("{\"cards\": [\"As\",\"Ks\"]}");
        // Registry should have a pending deck
        assertThat(CardInjectionRegistry.takeDeck()).isNotNull();
    }

    @Test
    void injectCards_unknownCard_returns400() throws Exception {
        String body = "{\"cards\": [\"As\",\"XX\"]}";
        HttpResponse<String> resp = post(body);
        assertThat(resp.statusCode()).isEqualTo(400);
        JsonNode json = json(resp);
        assertThat(json.get("error").asText()).isEqualTo("InvalidCards");
        assertThat(json.get("details").toString()).contains("unrecognized card");
    }

    @Test
    void injectCards_duplicateCard_returns400() throws Exception {
        String body = "{\"cards\": [\"As\",\"As\"]}";
        HttpResponse<String> resp = post(body);
        assertThat(resp.statusCode()).isEqualTo(400);
        JsonNode json = json(resp);
        assertThat(json.get("error").asText()).isEqualTo("InvalidCards");
        assertThat(json.get("details").toString()).contains("duplicate card");
    }

    // -------------------------------------------------------------------------
    // POST /cards/inject — seeded shuffle
    // -------------------------------------------------------------------------

    @Test
    void injectSeed_validSeed_returns200AndAccepted() throws Exception {
        HttpResponse<String> resp = post("{\"seed\": 42}");
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode json = json(resp);
        assertThat(json.get("accepted").asBoolean()).isTrue();
        assertThat(json.get("seed").asLong()).isEqualTo(42L);
    }

    @Test
    void injectSeed_registryHasDeck() throws Exception {
        post("{\"seed\": 99}");
        assertThat(CardInjectionRegistry.takeDeck()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // POST /cards/inject — bad request
    // -------------------------------------------------------------------------

    @Test
    void injectCards_emptyList_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"cards\": []}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void injectSeed_nonNumericSeed_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"seed\": \"hello\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void injectCards_nonArrayCards_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"cards\": 42}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void inject_noCardsOrSeed_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"other\": 1}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void inject_emptyBody_returns400() throws Exception {
        HttpResponse<String> resp = post("");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    // -------------------------------------------------------------------------
    // DELETE /cards/inject
    // -------------------------------------------------------------------------

    @Test
    void deleteInject_clearsRegistry() throws Exception {
        post("{\"seed\": 42}");
        HttpResponse<String> resp = delete();
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("accepted").asBoolean()).isTrue();
        assertThat(CardInjectionRegistry.takeDeck()).isNull();
    }

    @Test
    void deleteInject_noExistingInjection_returns200() throws Exception {
        HttpResponse<String> resp = delete();
        assertThat(resp.statusCode()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // Auth / method checks
    // -------------------------------------------------------------------------

    @Test
    void inject_missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/cards/inject"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"seed\": 1}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    @Test
    void inject_getMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/cards/inject"))
                .GET()
                .header("X-Control-Key", apiKey)
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(405);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/cards/inject"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("X-Control-Key", apiKey)
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/cards/inject"))
                .DELETE()
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
