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

import com.donohoedigital.games.poker.online.GameEventLog;
import com.donohoedigital.games.poker.online.WsMessageLog;
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
 * Tests for {@code GET /ws-log}.
 */
class WsLogHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        WsMessageLog.clear();
        GameEventLog.clear();
        tempDir = Files.createTempDirectory("ddpoker-wslog-test-");
        server = new TestableServer(tempDir);
        server.start();
        port = Integer.parseInt(Files.readString(tempDir.resolve(GameControlServer.PORT_FILE)).strip());
        apiKey = Files.readString(tempDir.resolve(GameControlServer.KEY_FILE)).strip();
    }

    @AfterEach
    void tearDown() throws Exception {
        WsMessageLog.clear();
        GameEventLog.clear();
        if (server != null) server.stop();
        Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    // -------------------------------------------------------------------------
    // Auth and method enforcement
    // -------------------------------------------------------------------------

    @Test
    void missingKey_returns403() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/ws-log"))
                .GET()
                .build();
        assertThat(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
    }

    @Test
    void postMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/ws-log"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("X-Control-Key", apiKey)
                .build();
        assertThat(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(405);
    }

    // -------------------------------------------------------------------------
    // Structure of empty response
    // -------------------------------------------------------------------------

    @Test
    void emptyLog_returnsBothArrays() throws Exception {
        JsonNode json = get();
        assertThat(json.get("messages").isArray()).isTrue();
        assertThat(json.get("events").isArray()).isTrue();
        assertThat(json.get("messages").size()).isEqualTo(0);
        assertThat(json.get("events").size()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // Messages appear in response
    // -------------------------------------------------------------------------

    @Test
    void outboundMessage_appearsInMessages() throws Exception {
        WsMessageLog.logOutbound("PLAYER_ACTION", "CALL:0");
        JsonNode json = get();
        JsonNode messages = json.get("messages");
        assertThat(messages.size()).isEqualTo(1);
        JsonNode msg = messages.get(0);
        assertThat(msg.get("direction").asText()).isEqualTo("OUT");
        assertThat(msg.get("type").asText()).isEqualTo("PLAYER_ACTION");
        assertThat(msg.get("payload").asText()).isEqualTo("CALL:0");
        assertThat(msg.get("ms").asLong()).isPositive();
    }

    @Test
    void inboundMessage_appearsInMessages() throws Exception {
        WsMessageLog.logInbound("HAND_STARTED", "{\"handNum\":1}");
        JsonNode json = get();
        JsonNode msg = json.get("messages").get(0);
        assertThat(msg.get("direction").asText()).isEqualTo("IN");
        assertThat(msg.get("type").asText()).isEqualTo("HAND_STARTED");
    }

    // -------------------------------------------------------------------------
    // Events appear in response
    // -------------------------------------------------------------------------

    @Test
    void event_appearsInEvents() throws Exception {
        GameEventLog.log("NEW_HAND", 1);
        JsonNode json = get();
        JsonNode events = json.get("events");
        assertThat(events.size()).isEqualTo(1);
        JsonNode ev = events.get(0);
        assertThat(ev.get("type").asText()).isEqualTo("NEW_HAND");
        assertThat(ev.get("table").asInt()).isEqualTo(1);
        assertThat(ev.get("ms").asLong()).isPositive();
    }

    @Test
    void multipleEvents_returnedOldestFirst() throws Exception {
        GameEventLog.log("NEW_HAND", 1);
        GameEventLog.log("CURRENT_PLAYER_CHANGED", 1);
        JsonNode events = get().get("events");
        assertThat(events.get(0).get("type").asText()).isEqualTo("NEW_HAND");
        assertThat(events.get(1).get("type").asText()).isEqualTo("CURRENT_PLAYER_CHANGED");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JsonNode get() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/ws-log"))
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
