/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: registration, login, and profile verification via the desktop
 * client control server against a live pokergameserver.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19878</li>
 * <li>Register a new user via POST /online/register</li>
 * <li>Login with the new user via POST /online/login</li>
 * <li>Verify the profile via GET /account/profile</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineRegistrationE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19878;
    private static final String TEST_USER = "e2ereg";
    private static final String TEST_PASS = "E2eRegPass1!";
    private static final String TEST_EMAIL = "e2ereg@test.local";

    private static GameServerTestProcess gameServer;

    @BeforeAll
    static void startGameServer() throws Exception {
        gameServer = GameServerTestProcess.start(GAME_SERVER_PORT);
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
    void step1_register() throws Exception {
        var resp = client().onlineRegister(serverUrl(), TEST_USER, TEST_PASS, TEST_EMAIL);

        assertThat(resp.path("success").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void step2_login() throws Exception {
        var resp = client().onlineLogin(serverUrl(), TEST_USER, TEST_PASS);

        assertThat(resp.path("success").asBoolean()).isTrue();
        assertThat(resp.path("emailVerified").asBoolean()).isTrue();
    }

    @Test
    @Order(3)
    void step3_verifyProfile() throws Exception {
        var resp = client().accountProfile();

        assertThat(resp.path("username").asText()).isEqualTo(TEST_USER);
        assertThat(resp.path("email").asText()).isEqualTo(TEST_EMAIL);
    }
}
