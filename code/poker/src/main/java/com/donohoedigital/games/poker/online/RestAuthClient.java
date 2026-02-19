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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.donohoedigital.games.poker.gameserver.dto.ChangePasswordRequest;
import com.donohoedigital.games.poker.gameserver.dto.ForgotPasswordRequest;
import com.donohoedigital.games.poker.gameserver.dto.LoginRequest;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.dto.ProfileResponse;
import com.donohoedigital.games.poker.gameserver.dto.RegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.ResetPasswordRequest;
import com.donohoedigital.games.poker.gameserver.dto.UpdateProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * HTTP client for the poker server authentication REST API.
 *
 * <p>
 * Used by the desktop client for all profile operations: login, registration,
 * password management. All calls are synchronous and must not be called from
 * the EDT.
 *
 * <p>
 * Methods take a {@code serverUrl} parameter (e.g.
 * {@code http://localhost:8080}) so the same client instance can be used
 * against any server. Use {@link #getInstance()} for the shared singleton.
 */
public class RestAuthClient {

    private static final Logger logger = LogManager.getLogger(RestAuthClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final RestAuthClient INSTANCE = new RestAuthClient();

    private final HttpClient http;

    /** Returns the shared singleton instance. */
    public static RestAuthClient getInstance() {
        return INSTANCE;
    }

    /** Production constructor. */
    public RestAuthClient() {
        this.http = HttpClient.newHttpClient();
    }

    /** Test constructor — allows injecting a custom {@link HttpClient}. */
    RestAuthClient(HttpClient http) {
        this.http = http;
    }

    /**
     * Authenticate with username and password.
     *
     * @param serverUrl
     *            base URL of the server (e.g. {@code http://localhost:8080})
     * @param username
     *            the username
     * @param password
     *            the plain-text password
     * @return login response including JWT token and profile ID
     * @throws RestAuthException
     *             if authentication fails or network error occurs
     */
    public LoginResponse login(String serverUrl, String username, String password) {
        try {
            LoginRequest req = new LoginRequest(username, password, false);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            LoginResponse result = OBJECT_MAPPER.readValue(response.body(), LoginResponse.class);
            if (!result.success()) {
                throw new RestAuthException(result.message() != null ? result.message() : "Login failed");
            }
            return result;
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to login", e);
        }
    }

    /**
     * Register a new profile.
     *
     * @param serverUrl
     *            base URL of the server
     * @param username
     *            desired username
     * @param password
     *            chosen password (plain text)
     * @param email
     *            email address
     * @return login response including JWT token and profile ID
     * @throws RestAuthException
     *             if registration fails (e.g. duplicate username/email) or network
     *             error
     */
    public LoginResponse register(String serverUrl, String username, String password, String email) {
        try {
            RegisterRequest req = new RegisterRequest(username, password, email);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            LoginResponse result = OBJECT_MAPPER.readValue(response.body(), LoginResponse.class);
            if (!result.success()) {
                throw new RestAuthException(result.message() != null ? result.message() : "Registration failed");
            }
            return result;
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to register", e);
        }
    }

    /**
     * Fetch the current authenticated user's profile.
     *
     * @param serverUrl
     *            base URL of the server
     * @param jwt
     *            JWT bearer token
     * @return profile data
     * @throws RestAuthException
     *             if the request fails
     */
    public ProfileResponse getCurrentUser(String serverUrl, String jwt) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/me"))
                    .header("Authorization", "Bearer " + jwt).GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RestAuthException("Get current user returned " + response.statusCode());
            }
            return OBJECT_MAPPER.readValue(response.body(), ProfileResponse.class);
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to get current user", e);
        }
    }

    /**
     * Change the authenticated user's password.
     *
     * @param serverUrl
     *            base URL of the server
     * @param jwt
     *            JWT bearer token
     * @param profileId
     *            the profile ID (must match the JWT)
     * @param oldPassword
     *            the current password
     * @param newPassword
     *            the new password (min 8 chars)
     * @throws RestAuthException
     *             if the old password is wrong or the request fails
     */
    public void changePassword(String serverUrl, String jwt, Long profileId, String oldPassword, String newPassword) {
        try {
            ChangePasswordRequest req = new ChangePasswordRequest(oldPassword, newPassword);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/v1/profiles/" + profileId + "/password"))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RestAuthException("Change password failed (HTTP " + response.statusCode() + ")");
            }
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to change password", e);
        }
    }

    /**
     * Initiate a password reset for the given email.
     *
     * <p>
     * Always returns without throwing — this is a fire-and-forget operation that
     * must not leak whether the email is registered.
     *
     * @param serverUrl
     *            base URL of the server
     * @param email
     *            the email address to reset
     */
    public void forgotPassword(String serverUrl, String email) {
        try {
            ForgotPasswordRequest req = new ForgotPasswordRequest(email);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/forgot-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.warn("Failed to send forgot-password request", e);
        }
    }

    /**
     * Complete a password reset using a one-time token.
     *
     * @param serverUrl
     *            base URL of the server
     * @param token
     *            the reset token from the forgot-password response
     * @param newPassword
     *            the new password (min 8 chars)
     * @throws RestAuthException
     *             if the token is invalid, expired, or already used
     */
    public void resetPassword(String serverUrl, String token, String newPassword) {
        try {
            ResetPasswordRequest req = new ResetPasswordRequest(token, newPassword);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/reset-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String msg = "Invalid or expired reset token";
                try {
                    var node = OBJECT_MAPPER.readTree(response.body());
                    if (node.has("message")) {
                        msg = node.get("message").asText();
                    }
                } catch (Exception ignore) {
                    // use default message
                }
                throw new RestAuthException(msg);
            }
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to reset password", e);
        }
    }

    /**
     * Update the authenticated user's profile email.
     *
     * @param serverUrl
     *            base URL of the server
     * @param jwt
     *            JWT bearer token
     * @param profileId
     *            the profile ID (must match the JWT)
     * @param email
     *            the new email address
     * @throws RestAuthException
     *             if the update fails (e.g. email already in use) or network error
     */
    public void updateProfile(String serverUrl, String jwt, Long profileId, String email) {
        try {
            UpdateProfileRequest req = new UpdateProfileRequest(email);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/profiles/" + profileId))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + jwt)
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String msg = "Failed to update profile";
                try {
                    var node = OBJECT_MAPPER.readTree(response.body());
                    if (node.has("message")) {
                        msg = node.get("message").asText();
                    }
                } catch (Exception ignore) {
                    // use default message
                }
                throw new RestAuthException(msg);
            }
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to update profile", e);
        }
    }

    /**
     * Log out the current session. Best-effort — swallows exceptions.
     *
     * @param serverUrl
     *            base URL of the server
     * @param jwt
     *            JWT bearer token
     */
    public void logout(String serverUrl, String jwt) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/logout"))
                    .header("Authorization", "Bearer " + jwt).POST(HttpRequest.BodyPublishers.noBody()).build();

            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.warn("Failed to logout", e);
        }
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    /**
     * Thrown when a REST auth call fails or returns an error response.
     */
    public static class RestAuthException extends RuntimeException {
        public RestAuthException(String message) {
            super(message);
        }

        public RestAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
