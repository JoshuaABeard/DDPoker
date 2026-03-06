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
 * E2E test: desktop client hosts and plays a multi-hand tournament.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19882</li>
 * <li>Register host user, login desktop client</li>
 * <li>Host a 4-player game with AI fill</li>
 * <li>Start the game</li>
 * <li>Play 3 hands in sequence</li>
 * <li>Verify hand number advances to at least 3</li>
 * <li>Verify chip conservation after each hand</li>
 * <li>Verify no game-over state (1500 chips, 10/20 blinds — 3 hands won't
 * eliminate anyone)</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineTournamentE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19882;
    private static final String SERVER_URL = "http://localhost:" + GAME_SERVER_PORT;
    private static final String HOST_USER = "tournhost";
    private static final String HOST_PASS = "TournPass1!";
    private static final String HOST_EMAIL = "tournhost@test.local";

    private static GameServerTestProcess gameServer;

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
    void step2_hostGame() throws Exception {
        var resp = client().onlineHost(ControlServerClient.minimalOnlineGameConfig());

        assertThat(resp.has("gameId")).isTrue();
        assertThat(resp.has("wsUrl")).isTrue();
    }

    @Test
    @Order(3)
    void step3_waitForLobby() throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        JsonNode resp = null;
        while (System.currentTimeMillis() < deadline) {
            resp = client().onlineLobby();
            if ("Lobby.Host".equals(resp.path("phase").asText("")))
                break;
            Thread.sleep(500);
        }
        assertThat(resp).isNotNull();
        assertThat(resp.path("phase").asText()).isEqualTo("Lobby.Host");
    }

    @Test
    @Order(4)
    void step4_startGame() throws Exception {
        var resp = client().onlineStart();

        assertThat(resp.path("started").asBoolean()).isTrue();
    }

    @Test
    @Order(5)
    void step5_playThreeHandsAndVerify() throws Exception {
        // Wait for the game to be in a playable state
        client().waitForInputMode(Duration.ofSeconds(30), "DEAL", "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");

        int handsToPlay = 3;
        for (int i = 0; i < handsToPlay; i++) {
            // Record total chips before each hand
            var stateBefore = client().getState();
            int totalChipsBefore = totalChips(stateBefore);

            // Play one hand
            playOneHand(Duration.ofSeconds(60));

            // Verify chip conservation after each hand
            var stateAfter = client().getState();
            int totalChipsAfter = totalChips(stateAfter);
            assertThat(totalChipsAfter).as("Chip conservation after hand %d", i + 1).isEqualTo(totalChipsBefore);

            // Verify game is not over
            assertThat(stateAfter.path("tournament").path("isGameOver").asBoolean(false))
                    .as("Game should not be over after hand %d", i + 1).isFalse();
        }

        // Verify hand number advanced to at least 3
        var finalState = client().getState();
        int finalHandNumber = finalState.path("tournament").path("handNumber").asInt(0);
        assertThat(finalHandNumber).as("Hand number should be at least 3 after playing 3 hands")
                .isGreaterThanOrEqualTo(3);
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
