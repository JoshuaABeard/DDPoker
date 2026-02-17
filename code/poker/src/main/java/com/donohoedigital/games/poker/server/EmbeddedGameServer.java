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

import java.io.*;
import java.nio.file.*;
import java.security.KeyPair;
import java.util.Properties;
import java.util.UUID;

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
 * {@link #getLocalUserJwt()} returns a valid JWT for the local OS user,
 * creating the user record in H2 on first call and persisting the identity to
 * {@code ~/.ddpoker/local-identity.properties}.
 */
public class EmbeddedGameServer {

    private static final Logger logger = LogManager.getLogger(EmbeddedGameServer.class);

    private static final Path DDPOKER_DIR = Path.of(System.getProperty("user.home"), ".ddpoker");
    private static final Path JWT_DIR = DDPOKER_DIR.resolve("jwt");
    private static final Path PRIVATE_KEY_PATH = JWT_DIR.resolve("private-key.pem");
    private static final Path PUBLIC_KEY_PATH = JWT_DIR.resolve("public-key.pem");
    private static final Path LOCAL_IDENTITY_PATH = DDPOKER_DIR.resolve("local-identity.properties");

    private ConfigurableApplicationContext context;
    private volatile int port = -1;
    private volatile boolean running = false;

    /**
     * Starts the embedded Spring Boot server.
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
            context = app.run();
            port = resolvePort();
            running = true;
            logger.info("Embedded game server started on port {}", port);
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
     * Returns a valid JWT for the local OS user, creating the user record in H2 on
     * first call.
     *
     * <p>
     * The local user's identity (username + generated password) is persisted to
     * {@code ~/.ddpoker/local-identity.properties} so the same credentials are
     * reused across restarts.
     *
     * @return signed JWT string
     */
    public String getLocalUserJwt() {
        Properties identity = loadOrCreateLocalIdentity();
        String username = identity.getProperty("username");
        String password = identity.getProperty("password");

        AuthService authService = context.getBean(AuthService.class);
        JwtTokenProvider jwtProvider = context.getBean(JwtTokenProvider.class);

        long profileId = registerOrLogin(authService, username, password);
        return jwtProvider.generateToken(username, profileId, false);
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
     * Loads the local identity from disk, or creates a new one and saves it.
     *
     * <p>
     * The identity contains the OS username and a stable random password so the
     * same user record is reused across JVM restarts.
     */
    private Properties loadOrCreateLocalIdentity() {
        try {
            Files.createDirectories(DDPOKER_DIR);
        } catch (IOException e) {
            logger.warn("Could not create .ddpoker directory", e);
        }

        if (Files.exists(LOCAL_IDENTITY_PATH)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(LOCAL_IDENTITY_PATH)) {
                props.load(in);
                if (props.containsKey("username") && props.containsKey("password")) {
                    return props;
                }
            } catch (IOException e) {
                logger.warn("Could not read local identity, recreating", e);
            }
        }

        // Create new identity
        String username = sanitizeUsername(System.getProperty("user.name", "local"));
        String password = UUID.randomUUID().toString();
        Properties props = new Properties();
        props.setProperty("username", username);
        props.setProperty("password", password);

        try (OutputStream out = Files.newOutputStream(LOCAL_IDENTITY_PATH)) {
            props.store(out, "DDPoker local user identity - do not edit");
        } catch (IOException e) {
            logger.warn("Could not save local identity", e);
        }
        return props;
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
