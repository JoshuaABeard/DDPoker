/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
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
package com.donohoedigital.games.poker.control;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.Executors;

/**
 * Lightweight embedded HTTP control server for dev builds only (src/dev/java).
 * <p>
 * Allows Claude Code to query and control the running DD Poker desktop client
 * via simple curl commands. Binds to localhost only with API key authentication.
 * <p>
 * On startup, writes its port and API key to:
 * <ul>
 *   <li>{@code ~/.ddpoker/control-server.port}</li>
 *   <li>{@code ~/.ddpoker/control-server.key}</li>
 * </ul>
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code GET  /health}           — liveness check</li>
 *   <li>{@code GET  /state}            — full game state as JSON</li>
 *   <li>{@code GET  /screenshot}       — PNG of the game window</li>
 *   <li>{@code POST /action}           — submit a player action (betting, deal, continue, rebuy)</li>
 *   <li>{@code POST /control}          — game flow control (pause, resume, phase transition)</li>
 *   <li>{@code POST /game/start}       — launch a new practice game</li>
 *   <li>{@code GET  /game/resumable}   — list in-progress/paused games from the embedded server</li>
 *   <li>{@code POST /game/resume}      — reconnect to an existing server-side game</li>
 *   <li>{@code GET  /profiles}         — list all player profiles on disk</li>
 *   <li>{@code POST /profiles}         — create a new player profile</li>
 *   <li>{@code GET  /profiles/default} — get the currently active default profile</li>
 * </ul>
 */
public class GameControlServer {

    private static final Logger logger = LogManager.getLogger(GameControlServer.class);

    static final String KEY_FILE = "control-server.key";
    static final String PORT_FILE = "control-server.port";

    private HttpServer server;
    String apiKey;

    /** Start the HTTP server on a random localhost port. */
    public void start() throws IOException {
        apiKey = resolveKey();

        server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 10);
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "control-server-handler");
            t.setDaemon(true);
            return t;
        }));

        server.createContext("/health",           new HealthHandler(apiKey));
        server.createContext("/state",            new StateHandler(apiKey));
        server.createContext("/screenshot",       new ScreenshotHandler(apiKey));
        server.createContext("/action",           new ActionHandler(apiKey));
        server.createContext("/control",          new ControlHandler(apiKey));
        server.createContext("/game/start",       new GameStartHandler(apiKey));
        GameResumeHandler resumeHandler = new GameResumeHandler(apiKey);
        server.createContext("/game/resumable",   resumeHandler);
        server.createContext("/game/resume",      resumeHandler);
        ProfilesHandler profilesHandler = new ProfilesHandler(apiKey);
        server.createContext("/profiles/default", profilesHandler);
        server.createContext("/profiles",         profilesHandler);

        server.start();

        int port = server.getAddress().getPort();
        writePortFile(port);
        logger.info("Dev control server started on localhost:{}", port);
    }

    /** Stop the HTTP server gracefully. */
    public void stop() {
        if (server != null) {
            server.stop(2);
            logger.info("Dev control server stopped");
        }
    }

    // -------------------------------------------------------------------------
    // Key / port persistence
    // -------------------------------------------------------------------------

    private String resolveKey() throws IOException {
        Path keyPath = ddPokerDir().resolve(KEY_FILE);
        if (Files.exists(keyPath)) {
            String existing = Files.readString(keyPath).strip();
            if (!existing.isEmpty()) return existing;
        }
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        String key = HexFormat.of().formatHex(bytes);
        Files.createDirectories(keyPath.getParent());
        Files.writeString(keyPath, key);
        return key;
    }

    private void writePortFile(int port) throws IOException {
        Path portPath = ddPokerDir().resolve(PORT_FILE);
        Files.createDirectories(portPath.getParent());
        Files.writeString(portPath, String.valueOf(port));
    }

    Path ddPokerDir() {
        return Path.of(System.getProperty("user.home"), ".ddpoker");
    }
}
