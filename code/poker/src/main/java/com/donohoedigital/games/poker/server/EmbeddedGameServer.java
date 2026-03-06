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

import com.donohoedigital.config.FilePrefs;
import com.donohoedigital.games.poker.PlayerProfile;
import com.donohoedigital.games.poker.PlayerProfileOptions;
import com.donohoedigital.games.poker.protocol.dto.LoginRequest;
import com.donohoedigital.games.poker.protocol.dto.LoginResponse;
import com.donohoedigital.games.poker.protocol.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
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
 * {@code <config-dir>/jwt/}</li>
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

    private static final Path DDPOKER_DIR = Path.of(FilePrefs.getConfigDirectory());
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
     * <p>
     * Binding is intentionally unrestricted (all interfaces) in this mode because
     * the random port is not advertised outside the local process — only the
     * desktop client JVM connects to it. Use {@link #start(int)} when a fixed port
     * is required; that overload binds to {@code 127.0.0.1} explicitly.
     *
     * @throws EmbeddedServerStartupException
     *             if the server fails to start
     */
    public void start() throws EmbeddedServerStartupException {
        startInternal(null, false);
    }

    /**
     * Starts the embedded Spring Boot server on a specific port, bound to
     * {@code 127.0.0.1} to prevent external access.
     *
     * <p>
     * Used when a predictable port is required (e.g. for local tooling). Port 0
     * retains random-port behavior.
     *
     * @param port
     *            the port to listen on
     * @throws EmbeddedServerStartupException
     *             if the server fails to start
     */
    public void start(int port) throws EmbeddedServerStartupException {
        startInternal(port, true);
    }

    private void startInternal(Integer port, boolean localhostOnly) throws EmbeddedServerStartupException {
        if (running) {
            return;
        }
        try {
            ensureJwtKeys();
        } catch (Exception e) {
            throw new EmbeddedServerStartupException("Failed to generate JWT keys", e);
        }

        try {
            System.setProperty("ddpoker.config.dir", FilePrefs.getConfigDirectory());
            SpringApplication app = new SpringApplication(EmbeddedServerConfig.class);
            app.setAdditionalProfiles("embedded");
            app.setHeadless(false); // Running inside a Swing application
            Properties props = buildStartupProperties(port, localhostOnly);
            if (!props.isEmpty()) {
                app.setDefaultProperties(props);
            }
            context = app.run();
            this.port = resolvePort();
            running = true;
            logger.info("Embedded game server started on port {}{}", this.port,
                    localhostOnly ? " (localhost only)" : "");
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
        String key = profile.getName() + ":" + profile.getFileName() + ":" + profile.getCreateDate();
        if (key.equals(cachedProfileKey_) && cachedJwt_ != null) {
            return cachedJwt_;
        }

        String username = deriveServerUsername(profile);
        String password = deriveServerPassword(profile);
        String jwt = registerOrLoginViaRest(username, password);
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
     * Builds the Spring Boot startup properties for the given port and binding
     * mode. Package-private for testing.
     *
     * <ul>
     * <li>When {@code port} is non-null, {@code server.port} is set.</li>
     * <li>When {@code localhostOnly} is {@code true}, {@code server.address} is set
     * to {@code 127.0.0.1} (localhost-only binding).</li>
     * </ul>
     */
    Properties buildStartupProperties(Integer port, boolean localhostOnly) {
        Properties props = new Properties();
        if (port != null) {
            props.setProperty("server.port", String.valueOf(port));
        }
        if (localhostOnly) {
            props.setProperty("server.address", "127.0.0.1");
        }
        return props;
    }

    /**
     * Generates the RSA key pair and saves it to {@code <config-dir>/jwt/} if it
     * does not exist. Inlined to avoid a compile-time dependency on
     * {@code pokergameserver}'s {@code JwtKeyManager}.
     */
    private void ensureJwtKeys() throws Exception {
        Files.createDirectories(JWT_DIR);
        if (!Files.exists(PRIVATE_KEY_PATH) || !Files.exists(PUBLIC_KEY_PATH)) {
            logger.info("Generating RSA key pair for JWT auth at {}", JWT_DIR);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";
            Files.writeString(PRIVATE_KEY_PATH, privatePem);

            String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----\n";
            Files.writeString(PUBLIC_KEY_PATH, publicPem);
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
     * Registers the local user via the auth REST API, or logs in if already
     * registered. Returns the JWT token from the response.
     */
    private String registerOrLoginViaRest(String username, String password) {
        String baseUrl = "http://localhost:" + port + "/api/v1/auth";
        HttpClient http = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        // Try register first
        try {
            String email = username + "@local.ddpoker";
            RegisterRequest registerReq = new RegisterRequest(username, password, email);
            String body = mapper.writeValueAsString(registerReq);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/register"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                LoginResponse loginResp = mapper.readValue(response.body(), LoginResponse.class);
                if (loginResp.success() && loginResp.token() != null) {
                    return loginResp.token();
                }
            }
            // Fall through to login on any non-success (e.g. 409 Conflict)
        } catch (Exception e) {
            logger.debug("Register failed for '{}', trying login: {}", username, e.getMessage());
        }

        // Register failed (username exists) — try login
        try {
            LoginRequest loginReq = new LoginRequest(username, password, false);
            String body = mapper.writeValueAsString(loginReq);

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(baseUrl + "/login"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LoginResponse loginResp = mapper.readValue(response.body(), LoginResponse.class);
                if (loginResp.success() && loginResp.token() != null) {
                    return loginResp.token();
                }
                throw new IllegalStateException(
                        "Login returned success=false for '" + username + "': " + loginResp.message());
            }
            throw new IllegalStateException(
                    "Login returned HTTP " + response.statusCode() + " for '" + username + "': " + response.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not register or log in local user '" + username + "'", e);
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
