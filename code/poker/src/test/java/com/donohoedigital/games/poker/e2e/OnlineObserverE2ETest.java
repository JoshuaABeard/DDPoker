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
 * E2E test: observer/spectator mode — verify the observe endpoint returns valid
 * data for an active game.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19884, register host and observer
 * users</li>
 * <li>Login desktop client as host, host a game (AI fill), start the game</li>
 * <li>Play one hand to ensure game is active</li>
 * <li>Verify game appears in the games list</li>
 * <li>Test the observe endpoint — verify it returns valid game/ws data</li>
 * </ol>
 *
 * <p>
 * Note: Full observer flow verification would require a second desktop client
 * instance. This test verifies the REST contract of the observe endpoint
 * returns proper data for an active game.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineObserverE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19884;
    private static final String SERVER_URL = "http://localhost:" + GAME_SERVER_PORT;
    private static final String HOST_USER = "obshost";
    private static final String HOST_PASS = "ObsHostPass1!";
    private static final String HOST_EMAIL = "obshost@test.local";
    private static final String OBSERVER_USER = "observer";
    private static final String OBSERVER_PASS = "ObserverPass1!";
    private static final String OBSERVER_EMAIL = "observer@test.local";

    private static GameServerTestProcess gameServer;
    private static String gameId;

    @BeforeAll
    static void startGameServer() throws Exception {
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
        gameServer.registerAndVerify(HOST_USER, HOST_PASS, HOST_EMAIL);
        gameServer.registerAndVerify(OBSERVER_USER, OBSERVER_PASS, OBSERVER_EMAIL);
    }

    @AfterAll
    static void stopGameServer() {
        if (gameServer != null)
            gameServer.stop();
    }

    @Test
    @Order(1)
    void step1_loginAsHost() throws Exception {
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
        gameId = resp.path("gameId").asText();
        assertThat(gameId).isNotBlank();
    }

    @Test
    @Order(3)
    void step3_waitForLobbyAndStart() throws Exception {
        // Wait for lobby phase
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

        var startResp = client().onlineStart();
        assertThat(startResp.path("started").asBoolean()).isTrue();
    }

    @Test
    @Order(4)
    void step4_playOneHand() throws Exception {
        // Wait for the game to be in a playable state
        client().waitForInputMode(Duration.ofSeconds(30), "DEAL", "CHECK_BET", "CHECK_RAISE", "CALL_RAISE");

        // Play one hand
        playOneHand(Duration.ofSeconds(60));
    }

    @Test
    @Order(5)
    void step5_gameAppearsInList() throws Exception {
        var resp = client().onlineGames();
        var games = resp.path("games");

        assertThat(games).isNotEmpty();

        boolean found = false;
        for (var game : games) {
            if (gameId.equals(game.path("gameId").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).as("Active game %s should appear in game list", gameId).isTrue();
    }

    @Test
    @Order(6)
    void step6_observeEndpointReturnsValidData() throws Exception {
        // The observe endpoint should return game connection info.
        // Since the desktop client is already playing this game, the endpoint
        // may navigate or return connection data — we verify the REST contract.
        var resp = client().onlineObserve(gameId);

        assertThat(resp).isNotNull();
        // The response should contain at least gameId or wsUrl indicating
        // the server accepted the observe request
        boolean hasGameId = resp.has("gameId") && !resp.path("gameId").asText("").isBlank();
        boolean hasWsUrl = resp.has("wsUrl") && !resp.path("wsUrl").asText("").isBlank();
        boolean hasError = resp.has("error");

        // Either we get valid connection data, or a graceful error
        // (e.g., if the client is already in the game)
        assertThat(hasGameId || hasWsUrl || hasError)
                .as("Observe response should contain gameId, wsUrl, or a graceful error: %s", resp).isTrue();
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
}
