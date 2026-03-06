/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: multi-hand stability — verify the online game flow remains
 * consistent across multiple hands.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19885, register host user</li>
 * <li>Login desktop client, host a game (AI fill), start the game</li>
 * <li>Play 2 hands successfully</li>
 * <li>Verify game state is consistent: valid data, hand number incremented</li>
 * <li>Verify chip conservation across all hands</li>
 * </ol>
 *
 * <p>
 * This is a stability/regression test that ensures the online game flow does
 * not break after multiple hands. True reconnection testing (disconnect +
 * reconnect WebSocket) is complex to automate via the control server.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineReconnectionE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19885;
    private static final String SERVER_URL = "http://localhost:" + GAME_SERVER_PORT;
    private static final String HOST_USER = "reconhost";
    private static final String HOST_PASS = "ReconHostPass1!";
    private static final String HOST_EMAIL = "reconhost@test.local";

    private static GameServerTestProcess gameServer;
    private static int initialTotalChips;

    @BeforeAll
    static void startGameServer() throws Exception {
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
        gameServer.registerAndVerify(HOST_USER, HOST_PASS, HOST_EMAIL);
    }

    @AfterAll
    static void stopGameServer() {
        if (gameServer != null)
            gameServer.stop();
    }

    @Test
    @Order(1)
    void step1_login() throws Exception {
        var resp = client().onlineLogin(SERVER_URL, HOST_USER, HOST_PASS);

        assertThat(resp.path("success").asBoolean()).isTrue();
        assertThat(resp.path("emailVerified").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void step2_hostAndStartGame() throws Exception {
        var hostResp = client().onlineHost(ControlServerClient.minimalOnlineGameConfig());

        assertThat(hostResp.has("gameId")).isTrue();
        assertThat(hostResp.path("gameId").asText()).isNotBlank();

        // Wait for lobby phase
        long deadline = System.currentTimeMillis() + 15_000;
        JsonNode lobbyResp = null;
        while (System.currentTimeMillis() < deadline) {
            lobbyResp = client().onlineLobby();
            if ("Lobby.Host".equals(lobbyResp.path("phase").asText("")))
                break;
            Thread.sleep(500);
        }
        assertThat(lobbyResp).isNotNull();
        assertThat(lobbyResp.path("phase").asText()).isEqualTo("Lobby.Host");

        var startResp = client().onlineStart();
        assertThat(startResp.path("started").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    void step3_recordInitialChips() throws Exception {
        // Wait for the game to be in a playable state
        client().waitForInputMode(Duration.ofSeconds(30), "DEAL", "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");

        var state = client().getState();
        initialTotalChips = totalChips(state);
        assertThat(initialTotalChips).isGreaterThan(0);
    }

    @Test
    @Order(4)
    void step4_playFirstHand() throws Exception {
        playOneHand(Duration.ofSeconds(60));

        var state = client().getState();
        assertThat(totalChips(state)).isEqualTo(initialTotalChips);
    }

    @Test
    @Order(5)
    void step5_playSecondHand() throws Exception {
        playOneHand(Duration.ofSeconds(60));

        var state = client().getState();
        assertThat(totalChips(state)).isEqualTo(initialTotalChips);
    }

    @Test
    @Order(6)
    void step6_verifyGameStateConsistency() throws Exception {
        var state = client().getState();

        // Game state should be valid and parseable
        assertThat(state).isNotNull();
        assertThat(state.has("inputMode")).isTrue();
        assertThat(state.has("tournament")).isTrue();
        assertThat(state.has("tables")).isTrue();

        // Hand number should have advanced (we played 2 hands)
        int handNumber = state.path("tournament").path("handNumber").asInt(0);
        assertThat(handNumber).as("Hand number should have advanced after 2 hands").isGreaterThanOrEqualTo(2);

        // Tables should still have players
        JsonNode tables = state.path("tables");
        assertThat(tables).isNotEmpty();
        for (var table : tables) {
            assertThat(table.path("players")).isNotEmpty();
        }

        // Final chip conservation check
        assertThat(totalChips(state)).isEqualTo(initialTotalChips);
    }

    /** Plays one full hand by responding to each input mode. */
    private void playOneHand(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        var initialState = client().getState();
        int startHandNumber = initialState.path("tournament").path("handNumber").asInt(0);

        while (System.currentTimeMillis() < deadline) {
            var state = client().getState();
            String mode = state.path("inputMode").asText("NONE");
            int currentHand = state.path("tournament").path("handNumber").asInt(0);

            if (currentHand > startHandNumber)
                return;
            if (state.path("tournament").path("isGameOver").asBoolean(false))
                return;

            try {
                switch (mode) {
                    case "DEAL" -> client().submitAction("DEAL");
                    case "CONTINUE" -> client().submitAction("CONTINUE");
                    case "CONTINUE_LOWER" -> client().submitAction("CONTINUE_LOWER");
                    case "CHECK_BET" -> client().submitAction("CHECK");
                    case "CHECK_RAISE" -> client().submitAction("CHECK");
                    case "CALL_RAISE" -> client().submitAction("CALL");
                    case "QUITSAVE", "NONE" -> Thread.sleep(200);
                    default -> Thread.sleep(200);
                }
            } catch (AssertionError e) {
                Thread.sleep(100);
            }
        }
        throw new AssertionError("Hand did not complete within " + timeout);
    }

    /** Sums player chip stacks + pot from game state JSON. */
    private int totalChips(JsonNode state) {
        int total = 0;
        for (var table : state.path("tables")) {
            for (var player : table.path("players")) {
                total += player.path("chips").asInt(0);
            }
            total += table.path("pot").asInt(0);
        }
        return total;
    }
}
