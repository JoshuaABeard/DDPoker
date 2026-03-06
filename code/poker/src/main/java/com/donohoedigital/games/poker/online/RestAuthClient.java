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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.donohoedigital.games.config.GameConfigUtils;
import com.donohoedigital.games.poker.protocol.dto.ChangePasswordRequest;
import com.donohoedigital.games.poker.protocol.dto.EmailChangeRequest;
import com.donohoedigital.games.poker.protocol.dto.ForgotPasswordRequest;
import com.donohoedigital.games.poker.protocol.dto.LoginRequest;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.protocol.dto.ProfileResponse;
import com.donohoedigital.games.poker.protocol.dto.RegisterRequest;
import com.donohoedigital.games.poker.protocol.dto.RequestEmailChangeResponse;
import com.donohoedigital.games.poker.protocol.dto.ResendVerificationResponse;
import com.donohoedigital.games.poker.protocol.dto.ResetPasswordRequest;
import com.donohoedigital.games.poker.protocol.dto.UpdateProfileRequest;
import com.donohoedigital.games.poker.protocol.dto.UsernameCheckResponse;
import com.donohoedigital.games.poker.protocol.dto.VerifyEmailResponse;
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

    private static final String JWT_FILE = "auth.token";

    private final HttpClient http;

    /**
     * Base directory used to resolve profile JWT files. {@code null} in production
     * — falls back to {@link GameConfigUtils#getSaveDir()}/profiles. Set via the
     * test constructor to use a temp directory.
     */
    private final Path jwtBaseDir;

    private volatile String cachedJwt_;
    private volatile String cachedServerUrl_;
    private volatile boolean cachedEmailVerified_;

    /** Returns the shared singleton instance. */
    public static RestAuthClient getInstance() {
        return INSTANCE;
    }

    /** Production constructor. */
    public RestAuthClient() {
        this.http = HttpClient.newHttpClient();
        this.jwtBaseDir = null;
    }

    /** Test constructor — allows injecting a custom {@link HttpClient}. */
    RestAuthClient(HttpClient http) {
        this.http = http;
        this.jwtBaseDir = null;
    }

    /**
     * Test constructor — allows injecting both a custom {@link HttpClient} and a
     * base directory for JWT persistence, avoiding any dependency on
     * {@link GameConfigUtils}.
     */
    RestAuthClient(HttpClient http, Path jwtBaseDir) {
        this.http = http;
        this.jwtBaseDir = jwtBaseDir;
    }

    // -------------------------------------------------------------------------
    // Session cache
    // -------------------------------------------------------------------------

    /**
     * Stores the JWT and server URL from a successful login. Package-private for
     * testing.
     */
    void cacheSession(String serverUrl, String jwt) {
        this.cachedServerUrl_ = serverUrl;
        this.cachedJwt_ = jwt;
    }

    /**
     * Returns the cached JWT from the last successful login, or {@code null} if not
     * logged in.
     */
    public String getCachedJwt() {
        return cachedJwt_;
    }

    /**
     * Returns the cached server URL from the last successful login, or {@code null}
     * if not logged in.
     */
    public String getCachedServerUrl() {
        return cachedServerUrl_;
    }

    /**
     * Returns {@code true} if a JWT is currently cached (i.e. the user is logged
     * in).
     */
    public boolean hasSession() {
        return cachedJwt_ != null;
    }

    /** Clears the cached JWT and server URL. */
    public void clearSession() {
        cachedJwt_ = null;
        cachedServerUrl_ = null;
    }

    /**
     * Sets the cached JWT directly. Used when restoring a persisted JWT on startup
     * for silent re-authentication.
     *
     * @param jwt
     *            the JWT to cache
     */
    public void setCachedJwt(String jwt) {
        this.cachedJwt_ = jwt;
    }

    /**
     * Returns {@code true} if the currently cached session has a verified email
     * address.
     */
    public boolean isEmailVerified() {
        return cachedEmailVerified_;
    }

    // -------------------------------------------------------------------------
    // JWT persistence (Remember Me)
    // -------------------------------------------------------------------------

    /**
     * Persist the JWT to disk in the profile directory for "Remember Me" support.
     * The file is written with owner-only read permissions (best effort on
     * Windows).
     *
     * @param profileName
     *            the name of the profile (used to locate the profile directory)
     * @param jwt
     *            the JWT to persist
     */
    public void persistJwt(String profileName, String jwt) {
        try {
            Path dir = getProfileDir(profileName);
            Files.createDirectories(dir);
            Path file = dir.resolve(JWT_FILE);
            Files.writeString(file, jwt, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            // Restrict to owner-only read (best effort on Windows)
            File f = file.toFile();
            f.setReadable(false, false);
            f.setReadable(true, true);
            f.setWritable(false, false);
            f.setWritable(true, true);
        } catch (IOException e) {
            logger.warn("Could not persist JWT for profile {}: {}", profileName, e.getMessage());
        }
    }

    /**
     * Load a persisted JWT from the profile directory. Returns empty if no file
     * exists or the stored value is malformed.
     *
     * @param profileName
     *            the name of the profile
     * @return the persisted JWT, or empty if not found/invalid
     */
    public Optional<String> loadPersistedJwt(String profileName) {
        try {
            Path dir = getProfileDir(profileName);
            Path file = dir.resolve(JWT_FILE);
            if (!Files.exists(file))
                return Optional.empty();
            String jwt = Files.readString(file, StandardCharsets.UTF_8).trim();
            // Basic sanity: a JWT has exactly 3 base64url parts separated by '.'
            if (jwt.split("\\.", -1).length != 3)
                return Optional.empty();
            return Optional.of(jwt);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Delete the persisted JWT for the profile (called on logout).
     *
     * @param profileName
     *            the name of the profile
     */
    public void clearPersistedJwt(String profileName) {
        try {
            Path dir = getProfileDir(profileName);
            Files.deleteIfExists(dir.resolve(JWT_FILE));
        } catch (IOException e) {
            logger.warn("Could not clear persisted JWT for profile {}: {}", profileName, e.getMessage());
        }
    }

    /**
     * Returns the directory used to store per-profile JWT files.
     *
     * <p>
     * In production the directory is
     * {@code <GameConfigUtils.getSaveDir()>/profiles/<profileName>}. In tests a
     * {@code jwtBaseDir} injected via the package-private constructor is used
     * instead, avoiding any dependency on the application framework.
     */
    Path getProfileDir(String profileName) {
        Path base = (jwtBaseDir != null) ? jwtBaseDir : GameConfigUtils.getSaveDir().toPath().resolve("profiles");
        return base.resolve(profileName);
    }

    /**
     * Authenticate with username and password (rememberMe defaults to
     * {@code false}).
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
        return login(serverUrl, username, password, false);
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
     * @param rememberMe
     *            if {@code true}, requests an extended-lifetime JWT (30 days)
     * @return login response including JWT token and profile ID
     * @throws RestAuthException
     *             if authentication fails or network error occurs
     */
    public LoginResponse login(String serverUrl, String username, String password, boolean rememberMe) {
        try {
            LoginRequest req = new LoginRequest(username, password, rememberMe);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            LoginResponse result = OBJECT_MAPPER.readValue(response.body(), LoginResponse.class);
            if (!result.success()) {
                throw new RestAuthException(result.message() != null ? result.message() : "Login failed");
            }
            cacheSession(serverUrl, result.token());
            cachedEmailVerified_ = result.emailVerified();
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
            cachedEmailVerified_ = result.emailVerified();
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
                } catch (IOException | RuntimeException parseError) {
                    logger.debug("Unable to parse reset-password error response body", parseError);
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
                } catch (IOException | RuntimeException parseError) {
                    logger.debug("Unable to parse update-profile error response body", parseError);
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
     * <p>
     * Clears the in-memory session. If a {@code profileName} is provided, also
     * deletes the persisted JWT so "Remember Me" auto-login is cancelled.
     *
     * @param serverUrl
     *            base URL of the server
     * @param jwt
     *            JWT bearer token
     * @param profileName
     *            the local profile name whose persisted JWT should be removed, or
     *            {@code null} if no persisted JWT needs to be cleared
     */
    public void logout(String serverUrl, String jwt, String profileName) {
        clearSession();
        if (profileName != null) {
            clearPersistedJwt(profileName);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/logout"))
                    .header("Authorization", "Bearer " + jwt).POST(HttpRequest.BodyPublishers.noBody()).build();

            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            logger.warn("Failed to logout", e);
        }
    }

    /**
     * Log out the current session without clearing a persisted JWT. Provided for
     * backward compatibility; prefer {@link #logout(String, String, String)} when
     * the profile name is available.
     *
     * @param serverUrl
     *            base URL of the server
     * @param jwt
     *            JWT bearer token
     */
    public void logout(String serverUrl, String jwt) {
        logout(serverUrl, jwt, null);
    }

    /**
     * Verify email using the token from the verification email.
     *
     * <p>
     * On success, updates {@code cachedJwt_} with the new token (which has
     * {@code emailVerified=true}) and sets {@code cachedEmailVerified_} to
     * {@code true}.
     *
     * @param serverUrl
     *            base URL of the server
     * @param token
     *            the one-time token from the verification email
     * @throws RestAuthException
     *             if the token is invalid, expired, or the request fails
     */
    public void verifyEmail(String serverUrl, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/v1/auth/verify-email?token=" + token)).GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            VerifyEmailResponse result = OBJECT_MAPPER.readValue(response.body(), VerifyEmailResponse.class);
            if (!result.success()) {
                throw new RestAuthException(result.message() != null ? result.message() : "Email verification failed");
            }
            cachedJwt_ = result.token();
            cachedEmailVerified_ = true;
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to verify email", e);
        }
    }

    /**
     * Request a new verification email for the current account.
     *
     * @param serverUrl
     *            base URL of the server
     * @throws RestAuthException
     *             if the request fails (e.g. already verified, rate-limited)
     */
    public void resendVerification(String serverUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/v1/auth/resend-verification"))
                    .header("Authorization", "Bearer " + cachedJwt_).POST(HttpRequest.BodyPublishers.noBody()).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            ResendVerificationResponse result = OBJECT_MAPPER.readValue(response.body(),
                    ResendVerificationResponse.class);
            if (!result.success()) {
                String msg = result.message() != null ? result.message() : "Failed to resend verification email";
                if (result.rateLimited()) {
                    throw new ResendRateLimitedException(msg);
                }
                throw new RestAuthException(msg);
            }
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to resend verification email", e);
        }
    }

    /**
     * Check if a username is available for registration.
     *
     * @param serverUrl
     *            base URL of the server
     * @param username
     *            the username to check
     * @return {@code true} if the username is available, {@code false} if taken
     * @throws RestAuthException
     *             if the request fails
     */
    public boolean checkUsername(String serverUrl, String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/v1/auth/check-username?username=" + username)).GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RestAuthException("Check username failed (HTTP " + response.statusCode() + ")");
            }
            UsernameCheckResponse result = OBJECT_MAPPER.readValue(response.body(), UsernameCheckResponse.class);
            return result.available();
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to check username", e);
        }
    }

    /**
     * Request an email address change for the current account.
     *
     * <p>
     * Sends a confirmation link to the new address. The change is not applied until
     * the user clicks the link.
     *
     * @param serverUrl
     *            base URL of the server
     * @param newEmail
     *            the desired new email address
     * @throws RestAuthException
     *             if the request fails (e.g. email already in use)
     */
    public void requestEmailChange(String serverUrl, String newEmail) {
        try {
            EmailChangeRequest req = new EmailChangeRequest(newEmail);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(serverUrl + "/api/v1/auth/email"))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + cachedJwt_)
                    .PUT(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(req))).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            RequestEmailChangeResponse result = OBJECT_MAPPER.readValue(response.body(),
                    RequestEmailChangeResponse.class);
            if (!result.success()) {
                throw new RestAuthException(
                        result.message() != null ? result.message() : "Failed to request email change");
            }
        } catch (RestAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new RestAuthException("Failed to request email change", e);
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

    /**
     * Thrown by {@link #resendVerification} when the server rejects the request
     * because the user has already requested a verification email recently.
     */
    public static class ResendRateLimitedException extends RestAuthException {
        public ResendRateLimitedException(String message) {
            super(message);
        }
    }
}
