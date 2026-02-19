/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
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
package com.donohoedigital.games.poker.online;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.dto.ProfileResponse;
import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link RestAuthClient} using a JDK embedded HTTP server.
 */
class RestAuthClientTest {

    private HttpServer testServer;
    private int port;
    private RestAuthClient client;
    private String serverUrl;

    @BeforeEach
    void setUp() throws IOException {
        testServer = HttpServer.create(new InetSocketAddress(0), 0);
        testServer.start();
        port = testServer.getAddress().getPort();
        serverUrl = "http://localhost:" + port;
        client = new RestAuthClient();
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    void login_success_returnsLoginResponse() throws IOException {
        testServer.createContext("/api/v1/auth/login", exchange -> {
            String json = "{\"success\":true,\"token\":\"tok123\",\"profileId\":42,\"username\":\"Alice\",\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        LoginResponse resp = client.login(serverUrl, "Alice", "password");

        assertThat(resp.success()).isTrue();
        assertThat(resp.token()).isEqualTo("tok123");
        assertThat(resp.profileId()).isEqualTo(42L);
        assertThat(resp.username()).isEqualTo("Alice");
    }

    @Test
    void login_failure_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/login", exchange -> {
            String json = "{\"success\":false,\"token\":null,\"profileId\":null,\"username\":null,\"message\":\"Invalid username or password\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.login(serverUrl, "Alice", "wrong"))
                .isInstanceOf(RestAuthClient.RestAuthException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_sendsJsonBody() throws IOException {
        java.util.concurrent.atomic.AtomicReference<String> capturedBody = new java.util.concurrent.atomic.AtomicReference<>();
        testServer.createContext("/api/v1/auth/login", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"success\":true,\"token\":\"t\",\"profileId\":1,\"username\":\"u\",\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.login(serverUrl, "Alice", "pass");

        assertThat(capturedBody.get()).contains("\"username\"");
        assertThat(capturedBody.get()).contains("\"password\"");
    }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    void register_success_returnsLoginResponse() throws IOException {
        testServer.createContext("/api/v1/auth/register", exchange -> {
            String json = "{\"success\":true,\"token\":\"tok456\",\"profileId\":99,\"username\":\"Bob\",\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        LoginResponse resp = client.register(serverUrl, "Bob", "pass1234", "bob@example.com");

        assertThat(resp.success()).isTrue();
        assertThat(resp.token()).isEqualTo("tok456");
        assertThat(resp.profileId()).isEqualTo(99L);
    }

    @Test
    void register_duplicate_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/register", exchange -> {
            String json = "{\"success\":false,\"token\":null,\"profileId\":null,\"username\":null,\"message\":\"Username already exists\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.register(serverUrl, "Bob", "pass1234", "bob@example.com"))
                .isInstanceOf(RestAuthClient.RestAuthException.class).hasMessageContaining("Username already exists");
    }

    // -------------------------------------------------------------------------
    // getCurrentUser
    // -------------------------------------------------------------------------

    @Test
    void getCurrentUser_returnsProfileResponse() {
        testServer.createContext("/api/v1/auth/me", exchange -> {
            String json = "{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\",\"retired\":false}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        ProfileResponse profile = client.getCurrentUser(serverUrl, "tok123");

        assertThat(profile.username()).isEqualTo("Alice");
        assertThat(profile.email()).isEqualTo("alice@example.com");
        assertThat(profile.retired()).isFalse();
    }

    @Test
    void getCurrentUser_sendsAuthHeader() {
        java.util.concurrent.atomic.AtomicReference<String> capturedAuth = new java.util.concurrent.atomic.AtomicReference<>();
        testServer.createContext("/api/v1/auth/me", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String json = "{\"id\":1,\"username\":\"u\",\"email\":\"u@e.com\",\"retired\":false}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.getCurrentUser(serverUrl, "my-jwt");

        assertThat(capturedAuth.get()).isEqualTo("Bearer my-jwt");
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    void changePassword_success_doesNotThrow() {
        testServer.createContext("/api/v1/profiles/42/password", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        assertThatNoException()
                .isThrownBy(() -> client.changePassword(serverUrl, "tok123", 42L, "oldpass", "newpass12"));
    }

    @Test
    void changePassword_wrongPassword_throwsRestAuthException() {
        testServer.createContext("/api/v1/profiles/42/password", exchange -> {
            String json = "{\"message\":\"Wrong password\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.changePassword(serverUrl, "tok123", 42L, "wrong", "newpass12"))
                .isInstanceOf(RestAuthClient.RestAuthException.class);
    }

    // -------------------------------------------------------------------------
    // forgotPassword
    // -------------------------------------------------------------------------

    @Test
    void forgotPassword_success_doesNotThrow() {
        testServer.createContext("/api/v1/auth/forgot-password", exchange -> {
            String json = "{\"resetToken\":\"reset-abc\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        // Should not throw - forgotPassword is fire-and-forget
        assertThatNoException().isThrownBy(() -> client.forgotPassword(serverUrl, "alice@example.com"));
    }

    @Test
    void forgotPassword_serverError_doesNotThrow() {
        testServer.createContext("/api/v1/auth/forgot-password", exchange -> {
            exchange.sendResponseHeaders(503, 0);
            exchange.getResponseBody().close();
        });

        // forgotPassword swallows all errors
        assertThatNoException().isThrownBy(() -> client.forgotPassword(serverUrl, "alice@example.com"));
    }

    // -------------------------------------------------------------------------
    // updateProfile
    // -------------------------------------------------------------------------

    @Test
    void updateProfile_success_doesNotThrow() throws IOException {
        testServer.createContext("/api/v1/profiles/42", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        assertThatNoException()
                .isThrownBy(() -> client.updateProfile(serverUrl, "tok123", 42L, "newemail@example.com"));
    }

    @Test
    void updateProfile_emailInUse_throwsRestAuthExceptionWithMessage() throws IOException {
        testServer.createContext("/api/v1/profiles/42", exchange -> {
            String json = "{\"message\":\"Email already in use\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.updateProfile(serverUrl, "tok123", 42L, "taken@example.com"))
                .isInstanceOf(RestAuthClient.RestAuthException.class).hasMessageContaining("Email already in use");
    }

    // -------------------------------------------------------------------------
    // resetPassword
    // -------------------------------------------------------------------------

    @Test
    void resetPassword_success_doesNotThrow() {
        testServer.createContext("/api/v1/auth/reset-password", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        assertThatNoException().isThrownBy(() -> client.resetPassword(serverUrl, "reset-token", "newpass12"));
    }

    @Test
    void resetPassword_invalidToken_throwsRestAuthExceptionWithMessage() {
        testServer.createContext("/api/v1/auth/reset-password", exchange -> {
            String json = "{\"message\":\"Invalid or expired reset token\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.resetPassword(serverUrl, "bad-token", "newpass12"))
                .isInstanceOf(RestAuthClient.RestAuthException.class)
                .hasMessageContaining("Invalid or expired reset token");
    }
}
