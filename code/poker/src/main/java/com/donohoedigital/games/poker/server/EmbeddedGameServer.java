/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.donohoedigital.games.poker.gameserver.auth.JwtKeyManager;
import com.donohoedigital.games.poker.gameserver.auth.JwtTokenProvider;
import com.donohoedigital.games.poker.gameserver.dto.LoginResponse;
import com.donohoedigital.games.poker.gameserver.service.AuthService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Properties;

/**
 * Manages the lifecycle of the embedded Spring Boot game server within the
 * desktop client JVM.
 *
 * <p>
 * On {@link #start()}, this class:
 * <ol>
 * <li>Generates an RSA key pair for JWT auth if one does not already exist at
 * {@code ~/.ddpoker/jwt/}</li>
 * <li>Starts a Spring Boot application context, binding to a random port</li>
 * <li>Exposes {@link #getPort()} for the rest of the desktop client to connect
 * to</li>
 * </ol>
 *
 * <p>
 * On {@link #stop()}, the Spring context is closed and resources released.
 *
 * <p>
 * {@link #getLocalUserJwt()} returns a valid JWT derived from the active
 * {@link PlayerProfile}. The server identity (username + password) is derived
 * deterministically from the profile's file name and create date — no OS
 * username or stored credentials file is used.
 */
public class EmbeddedGameServer {

    private static final Logger logger = LogManager.getLogger(EmbeddedGameServer.class);

    private static final Path DDPOKER_DIR = Path.of(System.getProperty("user.home"), ".ddpoker");
    private static final Path JWT_DIR = DDPOKER_DIR.resolve("jwt");
    private static final Path PRIVATE_KEY_PATH = JWT_DIR.resolve("private-key.pem");
    private static final Path PUBLIC_KEY_PATH = JWT_DIR.resolve("public-key.pem");

    private ConfigurableApplicationContext context;
    private volatile int port = -1;
    private volatile boolean running = false;

    // JWT cache: avoid repeated H2 lookups for the same profile
    private volatile String cachedProfileKey_;
    private volatile String cachedJwt_;

    /**
     * Starts the embedded Spring Boot server on a random OS-assigned port.
     *
     * <p>
     * Generates JWT keys if needed, then starts Spring Boot with the
     * {@code embedded} profile active. Blocks until the server is ready (typically
     * 1-2 seconds).
     *
     * @throws EmbeddedServerStartupException
     *             if the server fails to start
     */
    public void start() throws EmbeddedServerStartupException {
        startInternal(null, false);
    }

    /**
     * Starts the embedded Spring Boot server on a specific port, bound to all
     * network interfaces for external access.
     *
     * <p>
     * Used for community hosting where a predictable port is required for port
     * forwarding. Port 0 retains random-port behavior.
     *
     * @param port
     *            the port to listen on (default community port: 11885)
     * @throws EmbeddedServerStartupException
     *             if the server fails to start
     */
    public void start(int port) throws EmbeddedServerStartupException {
        startInternal(port, true);
    }

    private void startInternal(Integer port, boolean bindAllInterfaces) throws EmbeddedServerStartupException {
        if (running) {
            return;
        }
        try {
            ensureJwtKeys();
        } catch (Exception e) {
            throw new EmbeddedServerStartupException("Failed to generate JWT keys", e);
        }

        try {
            SpringApplication app = new SpringApplication(EmbeddedServerConfig.class);
            app.setAdditionalProfiles("embedded");
            app.setHeadless(false); // Running inside a Swing application
            if (port != null || bindAllInterfaces) {
                Properties props = new Properties();
                if (port != null) {
                    props.setProperty("server.port", String.valueOf(port));
                }
                if (bindAllInterfaces) {
                    props.setProperty("server.address", "0.0.0.0");
                }
                app.setDefaultProperties(props);
            }
            context = app.run();
            this.port = resolvePort();
            running = true;
            logger.info("Embedded game server started on port {}{}", this.port,
                    bindAllInterfaces ? " (external access)" : "");
        } catch (Exception e) {
            throw new EmbeddedServerStartupException("Failed to start embedded Spring Boot server", e);
        }
    }

    /**
     * Returns the HTTP/WebSocket port the embedded server is listening on. Only
     * valid after {@link #start()} returns successfully.
     */
    public int getPort() {
        return port;
    }

    /** Returns {@code true} if the server is running. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Returns a valid JWT for the active {@link PlayerProfile}.
     *
     * <p>
     * The server identity (username + password) is derived deterministically from
     * the profile's file name and create date — no OS username or stored
     * credentials file is used. When the active profile changes, a different
     * identity is used automatically.
     *
     * @return signed JWT string
     * @throws IllegalStateException
     *             if no active player profile is set
     */
    public String getLocalUserJwt() {
        PlayerProfile profile = PlayerProfileOptions.getDefaultProfile();
        if (profile == null) {
            throw new IllegalStateException("No active player profile");
        }
        return getJwtForProfile(profile);
    }

    /**
     * Eagerly authenticates the given profile against the embedded server and
     * caches the resulting JWT.
     *
     * <p>
     * Call this whenever the active profile changes so the server identity is
     * established immediately, and subsequent {@link #getLocalUserJwt()} calls
     * return the cached token without an H2 round-trip.
     *
     * <p>
     * Safe to call when the server is not yet running (no-op in that case).
     *
     * @param profile
     *            the newly active player profile
     */
    public void preAuthenticateProfile(PlayerProfile profile) {
        if (!running || context == null) {
            return;
        }
        getJwtForProfile(profile);
    }

    /**
     * Returns a valid JWT for the given {@link PlayerProfile}, using a cached token
     * when the profile has not changed. Package-private for testing.
     */
    String getJwtForProfile(PlayerProfile profile) {
        String key = profile.getFileName() + ":" + profile.getCreateDate();
        if (key.equals(cachedProfileKey_) && cachedJwt_ != null) {
            return cachedJwt_;
        }

        String username = deriveServerUsername(profile);
        String password = deriveServerPassword(profile);

        AuthService authService = context.getBean(AuthService.class);
        JwtTokenProvider jwtProvider = context.getBean(JwtTokenProvider.class);

        long profileId = registerOrLogin(authService, username, password);
        String jwt = jwtProvider.generateToken(username, profileId, false);
        cachedProfileKey_ = key;
        cachedJwt_ = jwt;
        return jwt;
    }

    /**
     * Stops the embedded server and releases all resources. Safe to call even if
     * {@link #start()} was never called.
     */
    public void stop() {
        if (context != null) {
            try {
                context.close();
                logger.info("Embedded game server stopped");
            } catch (Exception e) {
                logger.warn("Error stopping embedded server", e);
            } finally {
                context = null;
                running = false;
                port = -1;
                cachedProfileKey_ = null;
                cachedJwt_ = null;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Generates the RSA key pair and saves it to {@code ~/.ddpoker/jwt/} if it does
     * not exist.
     */
    private void ensureJwtKeys() throws Exception {
        Files.createDirectories(JWT_DIR);
        if (!Files.exists(PRIVATE_KEY_PATH) || !Files.exists(PUBLIC_KEY_PATH)) {
            logger.info("Generating RSA key pair for JWT auth at {}", JWT_DIR);
            KeyPair keyPair = JwtKeyManager.generateKeyPair();
            JwtKeyManager.savePrivateKey(keyPair.getPrivate(), PRIVATE_KEY_PATH);
            JwtKeyManager.savePublicKey(keyPair.getPublic(), PUBLIC_KEY_PATH);
        }
    }

    /**
     * Resolves the actual port Spring Boot bound to (since we requested port 0).
     */
    private int resolvePort() {
        if (context instanceof ServletWebServerApplicationContext webContext) {
            WebServer webServer = webContext.getWebServer();
            if (webServer != null) {
                return webServer.getPort();
            }
        }
        throw new IllegalStateException("Could not resolve embedded server port");
    }

    /**
     * Derives a stable server username from the profile's file name (without
     * extension). Falls back to the profile display name if the file name is
     * unavailable.
     */
    private String deriveServerUsername(PlayerProfile profile) {
        String fileName = profile.getFileName();
        if (fileName != null && !fileName.isBlank()) {
            int dot = fileName.lastIndexOf('.');
            String base = (dot > 0) ? fileName.substring(0, dot) : fileName;
            return sanitizeUsername(base);
        }
        String name = profile.getName();
        return sanitizeUsername(name != null ? name : "local");
    }

    /**
     * Derives a stable server password from the profile's file name and create date
     * via SHA-256. The result is a 32-character Base64-URL string (no padding),
     * giving 192 bits of entropy.
     */
    private String deriveServerPassword(PlayerProfile profile) {
        try {
            String key = profile.getFileName() + ":" + profile.getCreateDate();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Ensures the username is valid for registration (letters/digits/underscores,
     * max 20 chars).
     */
    private String sanitizeUsername(String raw) {
        String sanitized = raw.replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitized.isEmpty()) {
            sanitized = "local";
        }
        return sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
    }

    /**
     * Registers the local user in H2, or logs in if already registered. Returns the
     * profile ID.
     */
    private long registerOrLogin(AuthService authService, String username, String password) {
        try {
            String email = username + "@local.ddpoker";
            LoginResponse response = authService.register(username, password, email);
            return response.profileId();
        } catch (Exception e) {
            // Username already exists — log in with stored credentials
            try {
                LoginResponse response = authService.login(username, password, false);
                return response.profileId();
            } catch (Exception loginEx) {
                throw new IllegalStateException("Could not register or log in local user '" + username + "'", loginEx);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Exception type
    // -------------------------------------------------------------------------

    /** Thrown when the embedded server fails to start. */
    public static class EmbeddedServerStartupException extends Exception {
        public EmbeddedServerStartupException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
