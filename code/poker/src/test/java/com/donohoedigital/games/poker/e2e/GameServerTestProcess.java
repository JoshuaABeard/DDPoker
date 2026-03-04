/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

/**
 * Manages a pokergameserver child process for E2E tests.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * private static GameServerTestProcess gameServer;
 *
 * {@literal @}BeforeAll
 * static void startServer() throws Exception {
 *     gameServer = GameServerTestProcess.start(19877);
 * }
 *
 * {@literal @}AfterAll
 * static void stopServer() {
 *     if (gameServer != null) gameServer.stop();
 * }
 * </pre>
 *
 * <p>
 * Call {@link #skipIfJarMissing()} as the first line of {@code startServer()}
 * to mark the test class as skipped when the pokergameserver JAR has not been
 * built yet.
 */
public class GameServerTestProcess {

    private static final int STARTUP_TIMEOUT_MS = 30_000;
    private static final int POLL_INTERVAL_MS = 500;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int port;
    private final String baseUrl;
    private final HttpClient http;
    private Process process;

    private GameServerTestProcess(int port) {
        this.port = port;
        this.baseUrl = "http://localhost:" + port;
        this.http = HttpClient.newHttpClient();
    }

    /**
     * Skips the current test class if the pokergameserver JAR has not been built.
     * Call this before launching the process.
     */
    public static void skipIfJarMissing() {
        Assumptions.assumeTrue(findJar() != null,
                "pokergameserver JAR not found in code/pokergameserver/target/ — run 'mvn package -pl pokergameserver -DskipTests' first");
    }

    /**
     * Starts the pokergameserver on the given port and waits for it to become
     * healthy.
     *
     * @param port
     *            TCP port to bind (use a fixed high port like 19877 to avoid
     *            conflicts)
     * @throws Exception
     *             if the JAR cannot be found, the process fails to start, or
     *             startup times out
     */
    public static GameServerTestProcess start(int port) throws Exception {
        Path jar = findJar();
        if (jar == null) {
            throw new IllegalStateException(
                    "pokergameserver JAR not found in code/pokergameserver/target/ — run 'mvn package -pl pokergameserver -DskipTests' first");
        }

        GameServerTestProcess proc = new GameServerTestProcess(port);
        proc.launch(jar);
        return proc;
    }

    /** Stops the pokergameserver process. */
    public void stop() {
        if (process == null || !process.isAlive())
            return;
        process.destroy();
        try {
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (process.isAlive())
            process.destroyForcibly();
    }

    /**
     * Returns the base URL of the server (e.g. {@code "http://localhost:19877"}).
     */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * Registers a new user and verifies their email via the dev endpoint.
     *
     * @return JWT token from registration
     */
    public String registerAndVerify(String username, String password, String email) throws Exception {
        // Register
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void launch(Path jar) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", jar.toAbsolutePath().toString(), "--server.port=" + port,
                "--spring.profiles.active=embedded", "--spring.datasource.url=jdbc:h2:mem:e2etest;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.jpa.hibernate.ddl-auto=create-drop",
                "--settings.smtp.host=");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        process = pb.start();

        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException("pokergameserver process exited unexpectedly");
            }
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/v1/auth/check-username?username=healthcheck"))
                        .timeout(Duration.ofSeconds(3)).GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200)
                    return; // healthy
            } catch (IOException ignored) {
                // not ready yet
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        stop();
        throw new IllegalStateException("pokergameserver did not become healthy within " + STARTUP_TIMEOUT_MS + " ms");
    }

    private JsonNode post(String pathOrUrl, ObjectNode body) throws Exception {
        String url = pathOrUrl.startsWith("http") ? pathOrUrl : baseUrl + pathOrUrl;
        String json = MAPPER.writeValueAsString(body);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readTree(resp.body());
    }

    private static Path findJar() {
        Path target = Paths.get("code/pokergameserver/target");
        if (!Files.isDirectory(target))
            return null;
        try (Stream<Path> files = Files.list(target)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("-sources"))
                    .filter(p -> !p.getFileName().toString().contains("-javadoc")).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
