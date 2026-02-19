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

import com.donohoedigital.games.poker.PokerTableInput;
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
 * Tests for the extended {@code /action} endpoint covering the new action types:
 * DEAL, CONTINUE, CONTINUE_LOWER, REBUY, ADDON, DECLINE_REBUY.
 * <p>
 * Without a running game, all extended action types return 409 (conflict) with
 * helpful inputMode/availableActions context in the body.
 */
class ActionHandlerExtendedTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private GameControlServer server;
    private Path tempDir;
    private int port;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("ddpoker-action-ext-test-");
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
    // Existing action types still work
    // -------------------------------------------------------------------------

    @Test
    void fold_noGame_returns409WithInputMode() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"FOLD\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        JsonNode body = json(resp);
        assertThat(body.get("inputMode").asText()).isEqualTo("NONE");
        assertThat(body.get("availableActions").isArray()).isTrue();
    }

    // -------------------------------------------------------------------------
    // New action types — 409 when no game is running (inputMode=NONE)
    // -------------------------------------------------------------------------

    @Test
    void deal_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"DEAL\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("inputMode").asText()).isEqualTo("NONE");
    }

    @Test
    void continue_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"CONTINUE\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("inputMode").asText()).isEqualTo("NONE");
    }

    @Test
    void continueLower_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"CONTINUE_LOWER\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("inputMode").asText()).isEqualTo("NONE");
    }

    @Test
    void rebuy_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"REBUY\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("inputMode").asText()).isEqualTo("NONE");
    }

    @Test
    void addon_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"ADDON\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("inputMode").asText()).isEqualTo("NONE");
    }

    @Test
    void declineRebuy_noGame_returns409() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"DECLINE_REBUY\"}");
        assertThat(resp.statusCode()).isEqualTo(409);
        assertThat(json(resp).get("inputMode").asText()).isEqualTo("NONE");
    }

    // -------------------------------------------------------------------------
    // Unknown action type
    // -------------------------------------------------------------------------

    @Test
    void unknownType_returns400() throws Exception {
        HttpResponse<String> resp = post("{\"type\":\"EXPLODE\"}");
        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(json(resp).get("error").asText()).isEqualTo("BadRequest");
    }

    // -------------------------------------------------------------------------
    // inputModeToString and availableActionsForMode unit tests
    // -------------------------------------------------------------------------

    @Test
    void inputModeToString_coversAllKnownModes() {
        // Verify the static helper doesn't throw for known mode values
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_NONE)).isEqualTo("NONE");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_DEAL)).isEqualTo("DEAL");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_CHECK_BET)).isEqualTo("CHECK_BET");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_CHECK_RAISE)).isEqualTo("CHECK_RAISE");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_CALL_RAISE)).isEqualTo("CALL_RAISE");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_QUITSAVE)).isEqualTo("QUITSAVE");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_CONTINUE_LOWER)).isEqualTo("CONTINUE_LOWER");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_CONTINUE)).isEqualTo("CONTINUE");
        assertThat(ActionHandler.inputModeToString(PokerTableInput.MODE_REBUY_CHECK)).isEqualTo("REBUY_CHECK");
        assertThat(ActionHandler.inputModeToString(99)).startsWith("UNKNOWN_");
    }

    @Test
    void availableActionsForMode_checkBet_includesCheckAndBet() {
        var actions = ActionHandler.availableActionsForMode(PokerTableInput.MODE_CHECK_BET);
        assertThat(actions).contains("FOLD", "CHECK", "BET", "ALL_IN");
        assertThat(actions).doesNotContain("CALL", "RAISE");
    }

    @Test
    void availableActionsForMode_callRaise_includesCallAndRaise() {
        var actions = ActionHandler.availableActionsForMode(PokerTableInput.MODE_CALL_RAISE);
        assertThat(actions).contains("FOLD", "CALL", "RAISE", "ALL_IN");
        assertThat(actions).doesNotContain("CHECK", "BET");
    }

    @Test
    void availableActionsForMode_deal_returnsDealOnly() {
        var actions = ActionHandler.availableActionsForMode(PokerTableInput.MODE_DEAL);
        assertThat(actions).containsExactly("DEAL");
    }

    @Test
    void availableActionsForMode_rebuyCheck_includesAllRebuyOptions() {
        var actions = ActionHandler.availableActionsForMode(PokerTableInput.MODE_REBUY_CHECK);
        assertThat(actions).contains("REBUY", "ADDON", "DECLINE_REBUY");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/action"))
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
