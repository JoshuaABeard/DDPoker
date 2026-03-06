/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: desktop client hosts a 2-player online game, a SyntheticPlayer
 * joins, and they play one hand together.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19881</li>
 * <li>Register host user</li>
 * <li>Login desktop client via control server</li>
 * <li>Host a 2-player game (no AI fill)</li>
 * <li>Create a SyntheticPlayer that joins the game</li>
 * <li>Poll lobby until 2 players are present</li>
 * <li>Start the game</li>
 * <li>Play one hand to completion</li>
 * <li>Verify chip conservation</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineMultiPlayerE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19881;
    private static final String SERVER_URL = "http://localhost:" + GAME_SERVER_PORT;
    private static final String HOST_USER = "mphost";
    private static final String HOST_PASS = "MpHostPass1!";
    private static final String HOST_EMAIL = "mphost@test.local";

    private static GameServerTestProcess gameServer;
    private static String gameId;
    private static SyntheticPlayer synth1;

    @BeforeAll
    static void startGameServer() throws Exception {
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
        gameServer.registerAndVerify(HOST_USER, HOST_PASS, HOST_EMAIL);
    }

    @AfterAll
    static void stopGameServer() {
        if (synth1 != null)
            synth1.close();
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
    void step2_hostTwoPlayerGame() throws Exception {
        ObjectNode config = ControlServerClient.minimalOnlineGameConfig();
        config.put("maxPlayers", 2);
        config.put("maxOnlinePlayers", 2);
        config.put("fillComputer", false);

        var resp = client().onlineHost(config);

        assertThat(resp.has("gameId")).isTrue();
        assertThat(resp.has("wsUrl")).isTrue();
        gameId = resp.path("gameId").asText();
        assertThat(gameId).isNotBlank();
    }

    @Test
    @Order(3)
    void step3_syntheticPlayerJoins() throws Exception {
        synth1 = SyntheticPlayer.create(SERVER_URL, gameServer, "synth1", "SynthPass1!", "synth1@test.local");
        synth1.joinAndPlay(gameId);

        assertThat(synth1.isConnected()).isTrue();
    }

    @Test
    @Order(4)
    void step4_lobbyShowsTwoPlayers() throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        JsonNode resp = null;
        while (System.currentTimeMillis() < deadline) {
            resp = client().onlineLobby();
            if ("Lobby.Host".equals(resp.path("phase").asText(""))) {
                JsonNode players = resp.path("players");
                if (players.isArray() && players.size() >= 2)
                    break;
            }
            Thread.sleep(500);
        }
        assertThat(resp).isNotNull();
        assertThat(resp.path("phase").asText()).isEqualTo("Lobby.Host");
        assertThat(resp.path("players")).isNotNull();
        assertThat(resp.path("players").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(5)
    void step5_startGame() throws Exception {
        var resp = client().onlineStart();

        assertThat(resp.path("started").asBoolean()).isTrue();
    }

    @Test
    @Order(6)
    void step6_playOneHandAndVerifyChips() throws Exception {
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
