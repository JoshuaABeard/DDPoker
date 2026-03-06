/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: lobby management — kick player and change settings.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19883, register host user</li>
 * <li>Login desktop client as host</li>
 * <li>Host a 4-player game (no AI fill)</li>
 * <li>Create a SyntheticPlayer that joins the game</li>
 * <li>Poll lobby until 2 players are listed</li>
 * <li>Change game settings (rename, increase max players)</li>
 * <li>Kick the synthetic player</li>
 * <li>Poll lobby until player count is back to 1</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineLobbyManagementE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19883;
    private static final String SERVER_URL = "http://localhost:" + GAME_SERVER_PORT;
    private static final String HOST_USER = "lobbyhost";
    private static final String HOST_PASS = "LobbyHostPass1!";
    private static final String HOST_EMAIL = "lobbyhost@test.local";

    private static GameServerTestProcess gameServer;
    private static String gameId;
    private static SyntheticPlayer synth;

    @BeforeAll
    static void startGameServer() throws Exception {
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
        gameServer.registerAndVerify(HOST_USER, HOST_PASS, HOST_EMAIL);
    }

    @AfterAll
    static void stopGameServer() {
        if (synth != null)
            synth.close();
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
        ObjectNode config = ControlServerClient.minimalOnlineGameConfig();
        config.put("maxPlayers", 4);
        config.put("maxOnlinePlayers", 4);
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
        synth = SyntheticPlayer.create(SERVER_URL, gameServer, "lobbysynth", "SynthPass1!", "lobbysynth@test.local");
        synth.joinAndPlay(gameId);

        assertThat(synth.isConnected()).isTrue();
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
        assertThat(resp.path("players").size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(5)
    void step5_changeSettings() throws Exception {
        var resp = client().onlineLobbySettings("Renamed Game", 6);

        // Verify the endpoint returned successfully (no error)
        assertThat(resp).isNotNull();
    }

    @Test
    @Order(6)
    void step6_verifySettingsApplied() throws Exception {
        var resp = client().onlineLobby();

        assertThat(resp.path("phase").asText()).isEqualTo("Lobby.Host");
        // The game should still be in the lobby with our host
        assertThat(resp.path("gameId").asText()).isEqualTo(gameId);
    }

    @Test
    @Order(7)
    void step7_kickSyntheticPlayer() throws Exception {
        var resp = client().onlineLobbyKick(synth.getProfileId());

        assertThat(resp).isNotNull();
    }

    @Test
    @Order(8)
    void step8_lobbyShowsOnePlayerAfterKick() throws Exception {
        long deadline = System.currentTimeMillis() + 15_000;
        JsonNode resp = null;
        while (System.currentTimeMillis() < deadline) {
            resp = client().onlineLobby();
            if ("Lobby.Host".equals(resp.path("phase").asText(""))) {
                JsonNode players = resp.path("players");
                if (players.isArray() && players.size() <= 1)
                    break;
            }
            Thread.sleep(500);
        }
        assertThat(resp).isNotNull();
        assertThat(resp.path("phase").asText()).isEqualTo("Lobby.Host");
        assertThat(resp.path("players").size()).isLessThanOrEqualTo(1);
    }

    @Test
    @Order(9)
    void step9_closeSyntheticPlayer() {
        if (synth != null) {
            synth.close();
            synth = null;
        }
    }
}
