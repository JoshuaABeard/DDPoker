/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Manages an in-process pokergameserver Spring Boot context for E2E tests.
 *
 * <p>
 * Starts the server using {@link GameServerTestApplication} with an in-memory
 * H2 database and the {@code embedded} profile.
 */
public class GameServerTestProcess {

    private static final int STARTUP_TIMEOUT_MS = 30_000;
    private static final int POLL_INTERVAL_MS = 500;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int port;
    private final String baseUrl;
    private final HttpClient http;
    private ConfigurableApplicationContext context;

    private GameServerTestProcess(int port) {
        this.port = port;
        this.baseUrl = "http://localhost:" + port;
        this.http = HttpClient.newHttpClient();
    }

    /**
     * Starts the pokergameserver on the given port and waits for it to become
     * healthy.
     */
    public static GameServerTestProcess start(int port) throws Exception {
        GameServerTestProcess proc = new GameServerTestProcess(port);
        proc.launch();
        return proc;
    }

    /** Stops the pokergameserver Spring context. */
    public void stop() {
        if (context != null && context.isActive()) {
            context.close();
        }
        context = null;
    }

    /** Returns the base URL of the server. */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Registers a new user and verifies their email via the dev endpoint.
     *
     * @return JWT token from registration
     */
    public String registerAndVerify(String username, String password, String email) throws Exception {
        ObjectNode body = MAPPER.createObjectNode().put("username", username).put("password", password).put("email",
                email);
        JsonNode regResp = post("/api/v1/auth/register", body);
        String jwt = regResp.path("token").asText(null);
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalStateException("Registration failed: " + regResp);
        }

        // Verify email via dev endpoint
        post("/api/v1/dev/verify-user?username=" + username, MAPPER.createObjectNode());

        return jwt;
    }

    /**
     * Looks up a user's profile ID by username via the admin search endpoint.
     *
     * @return the profile ID
     * @throws IllegalStateException
     *             if the profile is not found
     */
    public long lookupProfileId(String username) throws Exception {
        JsonNode resp = get("/api/v1/admin/profiles?name=" + username);
        JsonNode content = resp.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new IllegalStateException("Profile not found for username: " + username);
        }
        // Find exact match (search uses LIKE %name%)
        for (JsonNode profile : content) {
            if (username.equals(profile.path("name").asText())) {
                return profile.path("id").asLong();
            }
        }
        throw new IllegalStateException("Exact profile not found for username: " + username);
    }

    /**
     * Unlocks a user account that may have been locked due to failed login
     * attempts.
     */
    public void unlockAccount(String username) throws Exception {
        long profileId = lookupProfileId(username);
        post("/api/v1/admin/profiles/" + profileId + "/unlock", MAPPER.createObjectNode());
    }

    /**
     * Logs in and returns the JWT token.
     *
     * @return JWT token
     * @throws IllegalStateException
     *             if login fails
     */
    public String loginAndGetJwt(String username, String password) throws Exception {
        ObjectNode body = MAPPER.createObjectNode().put("username", username).put("password", password)
                .put("rememberMe", false);
        JsonNode resp = post("/api/v1/auth/login", body);
        String jwt = resp.path("token").asText(null);
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalStateException("Login failed: " + resp);
        }
        return jwt;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void launch() throws Exception {
        context = SpringApplication.run(GameServerTestApplication.class, "--server.port=" + port,
                "--spring.profiles.active=embedded",
                "--spring.datasource.url=jdbc:h2:mem:e2etest" + port + ";DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--game.server.server-base-url=ws://localhost:" + port, "--settings.smtp.host=");

        // Poll until the server responds
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/v1/auth/check-username?username=healthcheck"))
                        .timeout(Duration.ofSeconds(3)).GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200)
                    return;
            } catch (IOException ignored) {
                // not ready yet
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        stop();
        throw new IllegalStateException("pokergameserver did not become healthy within " + STARTUP_TIMEOUT_MS + " ms");
    }

    private JsonNode get(String pathOrUrl) throws Exception {
        String url = pathOrUrl.startsWith("http") ? pathOrUrl : baseUrl + pathOrUrl;
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(resp.body());
    }

    private JsonNode post(String pathOrUrl, ObjectNode body) throws Exception {
        String url = pathOrUrl.startsWith("http") ? pathOrUrl : baseUrl + pathOrUrl;
        String json = MAPPER.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(resp.body());
    }
}
