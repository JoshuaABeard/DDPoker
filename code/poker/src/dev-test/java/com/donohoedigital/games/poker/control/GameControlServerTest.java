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
 * Tests for {@link GameControlServer} — verifies startup, key/port file creation,
 * authentication enforcement, and basic endpoint responses.
 * <p>
 * Tests redirect the config directory to a temp folder so they never touch
 * {@code ~/.ddpoker/}.
 */
class GameControlServerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-control-test-");
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
    // Startup: key and port file creation
    // -------------------------------------------------------------------------

    @Test
    void start_writesPortFile() {
        assertThat(tempDir.resolve(GameControlServer.PORT_FILE)).exists();
        assertThat(port).isGreaterThan(0).isLessThan(65536);
    }

    @Test
    void start_writesApiKeyFile() {
        assertThat(tempDir.resolve(GameControlServer.KEY_FILE)).exists();
        assertThat(apiKey).isNotBlank().hasSize(32); // 16 random bytes → 32 hex chars
    }

    @Test
    void start_reusesExistingKey() throws Exception {
        server.stop();
        String originalKey = apiKey;

        server = new TestableServer(tempDir);
        server.start();

        String reloaded = Files.readString(tempDir.resolve(GameControlServer.KEY_FILE)).strip();
        assertThat(reloaded).isEqualTo(originalKey);
    }

    // -------------------------------------------------------------------------
    // Authentication enforcement
    // -------------------------------------------------------------------------

    @Test
    void health_missingKey_returns403() throws Exception {
        HttpResponse<String> resp = get("/health", null);
        assertThat(resp.statusCode()).isEqualTo(403);
        assertThat(json(resp).get("error").asText()).isEqualTo("Forbidden");
    }

    @Test
    void health_wrongKey_returns403() throws Exception {
        HttpResponse<String> resp = get("/health", "wrongkey");
        assertThat(resp.statusCode()).isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // Health endpoint
    // -------------------------------------------------------------------------

    @Test
    void health_returns200WithStatusOk() throws Exception {
        HttpResponse<String> resp = get("/health", apiKey);
        assertThat(resp.statusCode()).isEqualTo(200);
        JsonNode body = json(resp);
        assertThat(body.get("status").asText()).isEqualTo("ok");
        assertThat(body.has("version")).isTrue();
    }

    // -------------------------------------------------------------------------
    // State endpoint — no game running (PokerMain.getPokerMain() == null in tests)
    // -------------------------------------------------------------------------

    @Test
    void state_noGame_returnsNonePhase() throws Exception {
        HttpResponse<String> resp = get("/state", apiKey);
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("gamePhase").asText()).isEqualTo("NONE");
    }

    @Test
    void state_postMethod_returns405() throws Exception {
        HttpResponse<String> resp = post("/state", apiKey, "{}");
        assertThat(resp.statusCode()).isEqualTo(405);
    }

    // -------------------------------------------------------------------------
    // Action endpoint
    // -------------------------------------------------------------------------

    @Test
    void action_noGame_returns409() throws Exception {
        // No game running → inputMode is NONE → Conflict
        HttpResponse<String> resp = post("/action", apiKey, "{\"type\":\"FOLD\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
    }

    @Test
    void action_missingBody_returns400() throws Exception {
        HttpResponse<String> resp = post("/action", apiKey, "");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void action_missingTypeField_returns400() throws Exception {
        HttpResponse<String> resp = post("/action", apiKey, "{\"amount\":100}");
        // Either 400 (bad type) or 409 (no game) — both are acceptable without a running game
        assertThat(resp.statusCode()).isIn(400, 409);
    }

    // -------------------------------------------------------------------------
    // Control endpoint
    // -------------------------------------------------------------------------

    @Test
    void control_missingBody_returns400() throws Exception {
        HttpResponse<String> resp = post("/control", apiKey, "");
        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void control_unknownAction_returns400() throws Exception {
        HttpResponse<String> resp = post("/control", apiKey, "{\"action\":\"EXPLODE\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    @Test
    void control_pause_accepts() throws Exception {
        // No director (no game), but the HTTP layer still returns 200 accepted
        HttpResponse<String> resp = post("/control", apiKey, "{\"action\":\"PAUSE\"}");
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("accepted").asBoolean()).isTrue();
    }

    @Test
    void control_resume_accepts() throws Exception {
        HttpResponse<String> resp = post("/control", apiKey, "{\"action\":\"RESUME\"}");
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("accepted").asBoolean()).isTrue();
    }

    @Test
    void control_anyPhase_accepts() throws Exception {
        // Phase allowlist removed in dev builds — any phase name is accepted
        HttpResponse<String> resp = post("/control", apiKey, "{\"action\":\"PHASE\",\"phase\":\"EvilPhase\"}");
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("accepted").asBoolean()).isTrue();
    }

    @Test
    void control_phaseSafe_accepts() throws Exception {
        HttpResponse<String> resp = post("/control", apiKey, "{\"action\":\"PHASE\",\"phase\":\"StartMenu\"}");
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(json(resp).get("accepted").asBoolean()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> get(String path, String key) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .GET();
        if (key != null) req.header("X-Control-Key", key);
        return HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String key, String body) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (key != null) req.header("X-Control-Key", key);
        return HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> resp) throws Exception {
        return MAPPER.readTree(resp.body());
    }

    /** Subclass that redirects ddPokerDir to a temp directory for test isolation. */
    private static class TestableServer extends GameControlServer {
        private final Path dir;

        TestableServer(Path dir) {
            this.dir = dir;
        }

        @Override
        Path ddPokerDir() {
            return dir;
        }
    }
}
