/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: desktop client hosts an online game on a live pokergameserver.
 *
 * Prerequisites: both JARs built via {@code mvn package -DskipTests -P dev}.
 * Test is automatically skipped if either JAR is missing.
 *
 * Scenario (host + AI fill): 1. Start pokergameserver on port 19877 2. Register
 * + verify test user 3. Start desktop client (via ControlServerTestBase
 * lifecycle) 4. Login via POST /online/login 5. Host via POST /online/host →
 * navigate to Lobby.Host 6. Verify lobby state via GET /online/lobby 7. Start
 * via POST /online/start → navigate to InitializeOnlineGame 8. Wait for DEAL
 * mode (AI fills empty seats on game start) 9. Play one hand to completion 10.
 * Assert chip conservation
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineCrossPlayE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19877;
    private static final String TEST_USER = "e2ehost";
    private static final String TEST_PASS = "E2eTestPass1!";
    private static final String TEST_EMAIL = "e2ehost@test.local";

    private static GameServerTestProcess gameServer;
    private static String gameId;

    @BeforeAll
    static void startGameServer() throws Exception {
        GameServerTestProcess.skipIfJarMissing();
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
        gameServer.registerAndVerify(TEST_USER, TEST_PASS, TEST_EMAIL);
    }

    @AfterAll
    static void stopGameServer() {
        if (gameServer != null)
            gameServer.stop();
    }

    @Test
    @Order(1)
    void step1_login() throws Exception {
        var resp = client().onlineLogin("http://localhost:" + GAME_SERVER_PORT, TEST_USER, TEST_PASS);

        assertThat(resp.path("success").asBoolean()).isTrue();
        assertThat(resp.path("emailVerified").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void step2_host() throws Exception {
        var resp = client().onlineHost(ControlServerClient.minimalOnlineGameConfig());

        assertThat(resp.has("gameId")).isTrue();
        assertThat(resp.has("wsUrl")).isTrue();
        gameId = resp.path("gameId").asText();
        assertThat(gameId).isNotBlank();
    }

    @Test
    @Order(3)
    void step3_lobbyState() throws Exception {
        var resp = client().onlineLobby();

        assertThat(resp.path("gameId").asText()).isEqualTo(gameId);
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
    void step5_playOneHand() throws Exception {
        // Wait for the game to be in a playable state
        client().waitForInputMode(Duration.ofSeconds(30), "DEAL", "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");

        // Record total chips before hand
        var stateBefore = client().getState();
        int totalChipsBefore = totalChips(stateBefore);

        // Play until the hand ends
        playOneHand(Duration.ofSeconds(60));

        // Verify chip conservation
        var stateAfter = client().getState();
        int totalChipsAfter = totalChips(stateAfter);
        assertThat(totalChipsAfter).isEqualTo(totalChipsBefore);
    }

    /** Plays one full hand by responding to each input mode. */
    private void playOneHand(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        // Remember starting hand number
        var initialState = client().getState();
        int startHandNumber = initialState.path("handNumber").asInt(0);

        while (System.currentTimeMillis() < deadline) {
            var state = client().getState();
            String mode = state.path("inputMode").asText("NONE");
            int currentHand = state.path("handNumber").asInt(0);

            // Hand complete when hand number increments
            if (currentHand > startHandNumber)
                return;

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
        }
        throw new AssertionError("Hand did not complete within " + timeout);
    }

    /** Sums player chip stacks + pot from game state JSON. */
    private int totalChips(com.fasterxml.jackson.databind.JsonNode state) {
        int total = 0;
        for (var player : state.path("players")) {
            total += player.path("chips").asInt(0);
        }
        total += state.path("pot").asInt(0);
        return total;
    }
}
