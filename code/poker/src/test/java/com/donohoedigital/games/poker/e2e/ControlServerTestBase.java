/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for E2E tests that drive a live DDPoker process via the
 * Dev Control Server HTTP API.
 *
 * <p>
 * Lifecycle:
 * <ol>
 * <li>{@code @BeforeAll} locates the fat JAR; skips the test class if not
 * found.</li>
 * <li>Starts the DDPoker process as a subprocess.</li>
 * <li>Polls {@code ~/.ddpoker/control-server.port} and
 * {@code ~/.ddpoker/control-server.key} until they exist (up to 30s).</li>
 * <li>Polls {@code /health} until it returns HTTP 200 (up to 30s).</li>
 * <li>{@code @AfterAll} destroys the process, forcibly if needed.</li>
 * </ol>
 *
 * <p>
 * Each subclass (test class) starts its own process to avoid state leakage
 * between test classes.
 *
 * <p>
 * Does not import any classes from
 * {@code com.donohoedigital.games.poker.control}. All communication is via HTTP
 * only.
 */
@Tag("e2e")
@Tag("slow")
abstract class ControlServerTestBase {

    private static final String PORT_FILE = "control-server.port";
    private static final String KEY_FILE = "control-server.key";
    private static final long STARTUP_TIMEOUT_MS = 30_000;
    private static final long POLL_INTERVAL_MS = 500;

    private static Process pokerProcess;
    protected static ControlServerClient client;

    @BeforeAll
    static void startApplication() throws Exception {
        Path jar = findJar();
        Assumptions.assumeTrue(jar != null,
                "DDPoker fat JAR not found at expected location (code/poker/target/DDPokerCE-3.3.0.jar); "
                        + "build with: cd code && mvn clean package -DskipTests -P dev");

        // Delete stale port/key files from any previous run so we can detect fresh
        // startup
        Path ddPokerDir = ddPokerDir();
        Path portFile = ddPokerDir.resolve(PORT_FILE);
        Path keyFile = ddPokerDir.resolve(KEY_FILE);
        Files.deleteIfExists(portFile);
        Files.deleteIfExists(keyFile);

        // Start the DDPoker process. On Windows, just launch normally — headless mode
        // breaks Swing. On other platforms, the display must be available (CI
        // environments
        // may need DISPLAY set or a virtual framebuffer).
        ProcessBuilder pb = new ProcessBuilder("java", "-Dgame.server.ai-action-delay-ms=0", "-jar",
                jar.toAbsolutePath().toString());
        pb.redirectErrorStream(true);
        Path logFile = java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "ddpoker-e2e.log");
        pb.redirectOutput(logFile.toFile());
        pokerProcess = pb.start();

        // Poll for port and key files
        pollUntilExists(portFile, STARTUP_TIMEOUT_MS,
                "Timed out waiting for control-server.port file after " + STARTUP_TIMEOUT_MS + "ms");
        pollUntilExists(keyFile, STARTUP_TIMEOUT_MS,
                "Timed out waiting for control-server.key file after " + STARTUP_TIMEOUT_MS + "ms");

        int port = Integer.parseInt(Files.readString(portFile).strip());
        String apiKey = Files.readString(keyFile).strip();
        client = new ControlServerClient(port, apiKey);

        // Poll /health until the server responds with 200
        pollUntilHealthy(STARTUP_TIMEOUT_MS);
    }

    @AfterAll
    static void stopApplication() {
        if (pokerProcess != null && pokerProcess.isAlive()) {
            pokerProcess.destroy();
            try {
                pokerProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            if (pokerProcess.isAlive()) {
                pokerProcess.destroyForcibly();
            }
        }
        pokerProcess = null;
        client = null;
    }

    /**
     * Returns the {@link ControlServerClient} connected to the running DDPoker
     * process.
     */
    protected ControlServerClient client() {
        return client;
    }

    // -------------------------------------------------------------------------
    // JAR discovery
    // -------------------------------------------------------------------------

    /**
     * Searches known locations for the DDPoker fat JAR, relative to the working
     * directory.
     *
     * @return the path if found, or {@code null} if not present
     */
    private static Path findJar() {
        Path[] candidates = {Path.of("target/DDPokerCE-3.3.0.jar"), Path.of("code/poker/target/DDPokerCE-3.3.0.jar"),
                Path.of("poker/target/DDPokerCE-3.3.0.jar"),};
        for (Path p : candidates) {
            if (Files.exists(p)) {
                return p;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // DDPoker config directory
    // -------------------------------------------------------------------------

    /**
     * Returns {@code ~/.ddpoker/} — the directory where the application writes its
     * port and key files on startup.
     */
    private static Path ddPokerDir() {
        return Path.of(com.donohoedigital.config.FilePrefs.getConfigDirectory());
    }

    // -------------------------------------------------------------------------
    // Polling utilities
    // -------------------------------------------------------------------------

    private static void pollUntilExists(Path file, long timeoutMs, String timeoutMessage) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(file)) {
                return;
            }
            if (pokerProcess != null && !pokerProcess.isAlive()) {
                throw new AssertionError(
                        "DDPoker process exited unexpectedly before " + file.getFileName() + " was written");
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new AssertionError(timeoutMessage);
    }

    private static void pollUntilHealthy(long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        Exception lastException = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (client.isHealthy()) {
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }
            if (pokerProcess != null && !pokerProcess.isAlive()) {
                throw new AssertionError("DDPoker process exited unexpectedly while waiting for /health to respond");
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        String suffix = lastException != null ? ": " + lastException.getMessage() : "";
        throw new AssertionError("Timed out after " + timeoutMs + "ms waiting for /health to return 200" + suffix);
    }
}
