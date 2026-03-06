/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test: account management — password change, email change, and profile
 * viewing via the desktop client control server against a live pokergameserver.
 *
 * <p>
 * Scenario:
 * <ol>
 * <li>Start pokergameserver on port 19880, register + verify test user</li>
 * <li>Login via POST /online/login</li>
 * <li>View profile via GET /account/profile</li>
 * <li>Change password via POST /account/password</li>
 * <li>Login with the new password</li>
 * <li>Request email change via PUT /account/email</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OnlineAccountManagementE2ETest extends ControlServerTestBase {

    private static final int GAME_SERVER_PORT = 19880;
    private static final String TEST_USER = "e2eacct";
    private static final String TEST_PASS = "E2eAcctPass1!";
    private static final String TEST_EMAIL = "e2eacct@test.local";
    private static final String NEW_PASS = "E2eNewPass2!";
    private static final String NEW_EMAIL = "e2eacct-new@test.local";

    private static GameServerTestProcess gameServer;

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
        assertThat(resp.path("emailVerified").asBoolean()).isTrue();
    }

    @Test
    @Order(2)
    void step2_viewProfile() throws Exception {
        var resp = client().accountProfile();

        assertThat(resp.path("username").asText()).isEqualTo(TEST_USER);
        assertThat(resp.path("email").asText()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @Order(3)
    void step3_changePassword() throws Exception {
        var resp = client().accountChangePassword(TEST_PASS, NEW_PASS);

        assertThat(resp.path("success").asBoolean()).isTrue();
    }

    @Test
    @Order(4)
    void step4_loginWithNewPassword() throws Exception {
        var resp = client().onlineLogin(serverUrl(), TEST_USER, NEW_PASS);

        assertThat(resp.path("success").asBoolean()).isTrue();
    }

    @Test
    @Order(5)
    void step5_changeEmail() throws Exception {
        var resp = client().accountChangeEmail(NEW_EMAIL);

        // Email change request should succeed (actual change requires confirmation)
        assertThat(resp.path("success").asBoolean()).isTrue();
    }
}
