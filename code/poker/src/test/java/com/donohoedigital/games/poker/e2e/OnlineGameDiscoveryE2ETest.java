/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: game discovery — listing available games via the desktop client
 * control server against a live pokergameserver.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19879, register + verify test user</li>
 * <li>Login via POST /online/login</li>
 * <li>List games (expect empty)</li>
 * <li>Host a game via POST /online/host</li>
 * <li>List games again (expect hosted game present)</li>
 * <li>Start the game via POST /online/start</li>
 * <li>List games again (expect game still present, now in progress)</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineGameDiscoveryE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19879;
    private static final String TEST_USER = "e2edisc";
    private static final String TEST_PASS = "E2eDiscPass1!";
    private static final String TEST_EMAIL = "e2edisc@test.local";

    private static GameServerTestProcess gameServer;
    private static String gameId;

    @BeforeAll
    static void startGameServer() throws Exception {
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
        gameServer.registerAndVerify(TEST_USER, TEST_PASS, TEST_EMAIL);
    }

    @AfterAll
    static void stopGameServer() {
        if (gameServer != null)
            gameServer.stop();
    }

    private String serverUrl() {
        return "http://localhost:" + GAME_SERVER_PORT;
    }

    @Test
    @Order(1)
    void step1_login() throws Exception {
        var resp = client().onlineLogin(serverUrl(), TEST_USER, TEST_PASS);

        assertThat(resp.path("success").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void step2_listGamesEmpty() throws Exception {
        var resp = client().onlineGames();

        assertThat(resp.path("games")).isEmpty();
    }

    @Test
    @Order(3)
    void step3_hostGame() throws Exception {
        var resp = client().onlineHost(ControlServerClient.minimalOnlineGameConfig());

        assertThat(resp.has("gameId")).isTrue();
        gameId = resp.path("gameId").asText();
        assertThat(gameId).isNotBlank();
    }

    @Test
    @Order(4)
    void step4_listGamesAfterHost() throws Exception {
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
        assertThat(found).as("Hosted game %s should appear in game list", gameId).isTrue();
    }

    @Test
    @Order(5)
    void step5_startGame() throws Exception {
        // Wait for lobby phase before starting
        long deadline = System.currentTimeMillis() + 15_000;
        com.fasterxml.jackson.databind.JsonNode lobbyResp = null;
        while (System.currentTimeMillis() < deadline) {
            lobbyResp = client().onlineLobby();
            if ("Lobby.Host".equals(lobbyResp.path("phase").asText("")))
                break;
            Thread.sleep(500);
        }
        assertThat(lobbyResp).isNotNull();
        assertThat(lobbyResp.path("phase").asText()).isEqualTo("Lobby.Host");

        var resp = client().onlineStart();
        assertThat(resp.path("started").asBoolean()).isTrue();
    }

    @Test
    @Order(6)
    void step6_listGamesAfterStart() throws Exception {
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
        assertThat(found).as("Started game %s should still appear in game list", gameId).isTrue();
    }
}
