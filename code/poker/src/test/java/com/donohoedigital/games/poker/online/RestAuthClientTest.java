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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.protocol.dto.ProfileResponse;
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
            String json = "{\"success\":true,\"profile\":{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"tok123\",\"message\":null,\"retryAfterSeconds\":null}";
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
        assertThat(resp.profile()).isNotNull();
        assertThat(resp.profile().id()).isEqualTo(42L);
        assertThat(resp.profile().username()).isEqualTo("Alice");
        assertThat(resp.profile().email()).isEqualTo("alice@example.com");
        assertThat(resp.profile().emailVerified()).isFalse();
    }

    @Test
    void login_parsesEmailVerified_andSetsCache() throws IOException {
        testServer.createContext("/api/v1/auth/login", exchange -> {
            String json = "{\"success\":true,\"profile\":{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\",\"emailVerified\":true,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"tok123\",\"message\":null,\"retryAfterSeconds\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.login(serverUrl, "Alice", "password");

        assertThat(client.isEmailVerified()).isTrue();
    }

    @Test
    void login_emailVerifiedFalse_setsCache() throws IOException {
        testServer.createContext("/api/v1/auth/login", exchange -> {
            String json = "{\"success\":true,\"profile\":{\"id\":42,\"username\":\"Alice\",\"email\":\"alice@example.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"tok123\",\"message\":null,\"retryAfterSeconds\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.login(serverUrl, "Alice", "password");

        assertThat(client.isEmailVerified()).isFalse();
    }

    @Test
    void login_failure_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/login", exchange -> {
            String json = "{\"success\":false,\"profile\":null,\"token\":null,\"message\":\"Invalid username or password\",\"retryAfterSeconds\":null}";
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
        AtomicReference<String> capturedBody = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/login", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"success\":true,\"profile\":{\"id\":1,\"username\":\"u\",\"email\":\"u@e.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"t\",\"message\":null,\"retryAfterSeconds\":null}";
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
            String json = "{\"success\":true,\"profile\":{\"id\":99,\"username\":\"Bob\",\"email\":\"bob@example.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"tok456\",\"message\":null,\"retryAfterSeconds\":null}";
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
        assertThat(resp.profile()).isNotNull();
        assertThat(resp.profile().id()).isEqualTo(99L);
        assertThat(resp.profile().email()).isEqualTo("bob@example.com");
        assertThat(resp.profile().emailVerified()).isFalse();
    }

    @Test
    void register_parsesEmailVerified_andSetsCache() throws IOException {
        testServer.createContext("/api/v1/auth/register", exchange -> {
            String json = "{\"success\":true,\"profile\":{\"id\":99,\"username\":\"Bob\",\"email\":\"bob@example.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"tok456\",\"message\":null,\"retryAfterSeconds\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.register(serverUrl, "Bob", "pass1234", "bob@example.com");

        assertThat(client.isEmailVerified()).isFalse();
    }

    @Test
    void register_duplicate_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/register", exchange -> {
            String json = "{\"success\":false,\"profile\":null,\"token\":null,\"message\":\"Username already exists\",\"retryAfterSeconds\":null}";
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
    // isEmailVerified
    // -------------------------------------------------------------------------

    @Test
    void isEmailVerified_returnsFalse_byDefault() {
        RestAuthClient c = new RestAuthClient();
        assertThat(c.isEmailVerified()).isFalse();
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
        AtomicReference<String> capturedAuth = new AtomicReference<>();
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

    // -------------------------------------------------------------------------
    // verifyEmail
    // -------------------------------------------------------------------------

    @Test
    void verifyEmail_success_updatesCachedJwtAndSetsEmailVerified() throws IOException {
        testServer.createContext("/api/v1/auth/verify-email", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            assertThat(query).contains("token=abc123");
            String json = "{\"success\":true,\"token\":\"new-jwt\",\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "old-jwt");
        client.verifyEmail(serverUrl, "abc123");

        assertThat(client.getCachedJwt()).isEqualTo("new-jwt");
        assertThat(client.isEmailVerified()).isTrue();
    }

    @Test
    void verifyEmail_sendsGetRequest() throws IOException {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedQuery = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/verify-email", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedQuery.set(exchange.getRequestURI().getQuery());
            String json = "{\"success\":true,\"token\":\"new-jwt\",\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.verifyEmail(serverUrl, "mytoken");

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedQuery.get()).isEqualTo("token=mytoken");
    }

    @Test
    void verifyEmail_failure_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/verify-email", exchange -> {
            String json = "{\"success\":false,\"token\":null,\"message\":\"Invalid or expired token\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        assertThatThrownBy(() -> client.verifyEmail(serverUrl, "bad-token"))
                .isInstanceOf(RestAuthClient.RestAuthException.class).hasMessageContaining("Invalid or expired token");
    }

    // -------------------------------------------------------------------------
    // resendVerification
    // -------------------------------------------------------------------------

    @Test
    void resendVerification_success_doesNotThrow() {
        testServer.createContext("/api/v1/auth/resend-verification", exchange -> {
            String json = "{\"success\":true,\"rateLimited\":false,\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "my-jwt");
        assertThatNoException().isThrownBy(() -> client.resendVerification(serverUrl));
    }

    @Test
    void resendVerification_sendsPostWithAuthHeader() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/resend-verification", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String json = "{\"success\":true,\"rateLimited\":false,\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "bearer-token");
        client.resendVerification(serverUrl);

        assertThat(capturedMethod.get()).isEqualTo("POST");
        assertThat(capturedAuth.get()).isEqualTo("Bearer bearer-token");
    }

    @Test
    void resendVerification_failure_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/resend-verification", exchange -> {
            String json = "{\"success\":false,\"rateLimited\":false,\"message\":\"Already verified\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "my-jwt");
        assertThatThrownBy(() -> client.resendVerification(serverUrl))
                .isInstanceOf(RestAuthClient.RestAuthException.class).hasMessageContaining("Already verified");
    }

    @Test
    void resendVerification_rateLimited_throwsResendRateLimitedException() {
        testServer.createContext("/api/v1/auth/resend-verification", exchange -> {
            String json = "{\"success\":false,\"rateLimited\":true,\"message\":\"Please wait before requesting another verification email\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(429, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "my-jwt");
        assertThatThrownBy(() -> client.resendVerification(serverUrl))
                .isInstanceOf(RestAuthClient.ResendRateLimitedException.class).hasMessageContaining("Please wait");
    }

    // -------------------------------------------------------------------------
    // checkUsername
    // -------------------------------------------------------------------------

    @Test
    void checkUsername_available_returnsTrue() {
        testServer.createContext("/api/v1/auth/check-username", exchange -> {
            String json = "{\"available\":true}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        boolean result = client.checkUsername(serverUrl, "newuser");
        assertThat(result).isTrue();
    }

    @Test
    void checkUsername_taken_returnsFalse() {
        testServer.createContext("/api/v1/auth/check-username", exchange -> {
            String json = "{\"available\":false}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        boolean result = client.checkUsername(serverUrl, "takenuser");
        assertThat(result).isFalse();
    }

    @Test
    void checkUsername_sendsGetWithUsernameParam() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedQuery = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/check-username", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedQuery.set(exchange.getRequestURI().getQuery());
            String json = "{\"available\":true}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.checkUsername(serverUrl, "alice");

        assertThat(capturedMethod.get()).isEqualTo("GET");
        assertThat(capturedQuery.get()).isEqualTo("username=alice");
    }

    @Test
    void checkUsername_serverError_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/check-username", exchange -> {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();
        });

        assertThatThrownBy(() -> client.checkUsername(serverUrl, "alice"))
                .isInstanceOf(RestAuthClient.RestAuthException.class);
    }

    // -------------------------------------------------------------------------
    // requestEmailChange
    // -------------------------------------------------------------------------

    @Test
    void requestEmailChange_success_doesNotThrow() {
        testServer.createContext("/api/v1/auth/email", exchange -> {
            String json = "{\"success\":true,\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "my-jwt");
        assertThatNoException().isThrownBy(() -> client.requestEmailChange(serverUrl, "new@example.com"));
    }

    @Test
    void requestEmailChange_sendsPutWithAuthHeaderAndBody() {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/email", exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"success\":true,\"message\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "bearer-token");
        client.requestEmailChange(serverUrl, "new@example.com");

        assertThat(capturedMethod.get()).isEqualTo("PUT");
        assertThat(capturedAuth.get()).isEqualTo("Bearer bearer-token");
        assertThat(capturedBody.get()).contains("\"email\"");
        assertThat(capturedBody.get()).contains("new@example.com");
    }

    @Test
    void requestEmailChange_failure_throwsRestAuthException() {
        testServer.createContext("/api/v1/auth/email", exchange -> {
            String json = "{\"success\":false,\"message\":\"Email already in use\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.cacheSession(serverUrl, "my-jwt");
        assertThatThrownBy(() -> client.requestEmailChange(serverUrl, "taken@example.com"))
                .isInstanceOf(RestAuthClient.RestAuthException.class).hasMessageContaining("Email already in use");
    }

    // -------------------------------------------------------------------------
    // session cache
    // -------------------------------------------------------------------------

    @Test
    void cacheSession_storesJwtAndServerUrl() {
        RestAuthClient c = new RestAuthClient();
        c.cacheSession("http://server:8080", "jwt-abc");
        assertThat(c.getCachedJwt()).isEqualTo("jwt-abc");
        assertThat(c.getCachedServerUrl()).isEqualTo("http://server:8080");
        assertThat(c.hasSession()).isTrue();
    }

    @Test
    void clearSession_removesCache() {
        RestAuthClient c = new RestAuthClient();
        c.cacheSession("http://server:8080", "jwt-abc");
        c.clearSession();
        assertThat(c.hasSession()).isFalse();
        assertThat(c.getCachedJwt()).isNull();
        assertThat(c.getCachedServerUrl()).isNull();
    }

    @Test
    void hasSession_returnsFalse_whenNoCacheSet() {
        RestAuthClient c = new RestAuthClient();
        assertThat(c.hasSession()).isFalse();
    }

    // -------------------------------------------------------------------------
    // setCachedJwt
    // -------------------------------------------------------------------------

    @Test
    void setCachedJwt_storesJwtInCache() {
        RestAuthClient c = new RestAuthClient();
        c.setCachedJwt("direct-jwt");
        assertThat(c.getCachedJwt()).isEqualTo("direct-jwt");
        assertThat(c.hasSession()).isTrue();
    }

    // -------------------------------------------------------------------------
    // JWT persistence (Remember Me)
    // -------------------------------------------------------------------------

    @TempDir
    Path tempDir;

    @Test
    void persistJwt_writesFileToProfileDirectory() throws IOException {
        RestAuthClient c = new RestAuthClient(null, tempDir);
        String jwt = "header.payload.signature";

        c.persistJwt("alice", jwt);

        Path expected = tempDir.resolve("alice").resolve("auth.token");
        assertThat(Files.exists(expected)).isTrue();
        assertThat(Files.readString(expected)).isEqualTo(jwt);
    }

    @Test
    void loadPersistedJwt_returnsJwtWhenFileExists() throws IOException {
        RestAuthClient c = new RestAuthClient(null, tempDir);
        String jwt = "aaa.bbb.ccc";
        Path dir = tempDir.resolve("bob");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("auth.token"), jwt);

        Optional<String> result = c.loadPersistedJwt("bob");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(jwt);
    }

    @Test
    void loadPersistedJwt_returnsEmptyWhenNoFile() {
        RestAuthClient c = new RestAuthClient(null, tempDir);

        Optional<String> result = c.loadPersistedJwt("nobody");

        assertThat(result).isEmpty();
    }

    @Test
    void loadPersistedJwt_returnsEmptyForMalformedContent() throws IOException {
        RestAuthClient c = new RestAuthClient(null, tempDir);
        Path dir = tempDir.resolve("charlie");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("auth.token"), "not-a-jwt");

        Optional<String> result = c.loadPersistedJwt("charlie");

        assertThat(result).isEmpty();
    }

    @Test
    void loadPersistedJwt_returnsEmptyForTwoPartToken() throws IOException {
        RestAuthClient c = new RestAuthClient(null, tempDir);
        Path dir = tempDir.resolve("dave");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("auth.token"), "header.payload");

        Optional<String> result = c.loadPersistedJwt("dave");

        assertThat(result).isEmpty();
    }

    @Test
    void clearPersistedJwt_deletesFile() throws IOException {
        RestAuthClient c = new RestAuthClient(null, tempDir);
        Path dir = tempDir.resolve("eve");
        Files.createDirectories(dir);
        Path tokenFile = dir.resolve("auth.token");
        Files.writeString(tokenFile, "x.y.z");

        c.clearPersistedJwt("eve");

        assertThat(Files.exists(tokenFile)).isFalse();
    }

    @Test
    void clearPersistedJwt_doesNotThrowWhenFileAbsent() {
        RestAuthClient c = new RestAuthClient(null, tempDir);

        assertThatNoException().isThrownBy(() -> c.clearPersistedJwt("nobody"));
    }

    @Test
    void login_withRememberMeTrue_sendsRememberMeInRequestBody() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/login", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"success\":true,\"profile\":{\"id\":1,\"username\":\"u\",\"email\":\"u@e.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"t\",\"message\":null,\"retryAfterSeconds\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.login(serverUrl, "u", "p", true);

        assertThat(capturedBody.get()).contains("\"rememberMe\":true");
    }

    @Test
    void login_withRememberMeFalse_sendsRememberMeFalseInRequestBody() throws IOException {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        testServer.createContext("/api/v1/auth/login", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"success\":true,\"profile\":{\"id\":1,\"username\":\"u\",\"email\":\"u@e.com\",\"emailVerified\":false,\"admin\":false,\"retired\":false,\"createDate\":null},\"token\":\"t\",\"message\":null,\"retryAfterSeconds\":null}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.login(serverUrl, "u", "p", false);

        assertThat(capturedBody.get()).contains("\"rememberMe\":false");
    }
}
