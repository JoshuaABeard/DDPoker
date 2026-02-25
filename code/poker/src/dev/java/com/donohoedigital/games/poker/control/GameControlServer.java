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

import com.donohoedigital.games.poker.NewLevelActions;
import com.donohoedigital.games.poker.online.WebSocketTournamentDirector;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.donohoedigital.config.FilePrefs;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight embedded HTTP control server for dev builds only (src/dev/java).
 * <p>
 * Allows Claude Code to query and control the running DD Poker desktop client
 * via simple curl commands. Binds to localhost only with API key authentication.
 * <p>
 * On startup, writes its port and API key to:
 * <ul>
 *   <li>{@code <config-dir>/control-server.port}</li>
 *   <li>{@code <config-dir>/control-server.key}</li>
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
 *   <li>{@code POST /cards/inject}     — stage a specific card order or seed for the next hand</li>
 *   <li>{@code DELETE /cards/inject}   — clear any pending card injection</li>
 *   <li>{@code GET  /options}          — read current game options and cheat toggles</li>
 *   <li>{@code POST /options}          — set one or more game options or cheat toggles</li>
 *   <li>{@code POST /cheat}            — manipulate live game state (setChips, setLevel, setButton, eliminatePlayer)</li>
 *   <li>{@code GET  /ws-log}           — last 40 WebSocket messages and last 50 game events</li>
 *   <li>{@code GET  /validate}         — chip conservation and game-state invariant checks</li>
 *   <li>{@code GET  /tournament-profiles}   — list all tournament profiles</li>
 *   <li>{@code POST /tournament-profiles}   — create a new tournament profile</li>
 *   <li>{@code DELETE /tournament-profiles}  — delete a tournament profile by name</li>
 *   <li>{@code POST /navigate}         — programmatic menu/phase navigation</li>
 *   <li>{@code GET  /navigate/status}  — durable status of latest navigation request</li>
 *   <li>{@code GET  /ui/state}         — Swing/UI observability snapshot</li>
 *   <li>{@code GET/POST /ui/dashboard} — dashboard layout snapshot and customization controls</li>
 *   <li>{@code GET  /ui/dashboard/widgets} — semantic widget snapshot with timing metadata</li>
 *   <li>{@code GET/POST /ui/dialogs} — active dialog snapshot and generic dialog interactions</li>
 *   <li>{@code GET  /system-info}      — version, config paths, runtime info</li>
 *   <li>{@code POST /game/save}        — save current game</li>
 *   <li>{@code POST /game/load}        — load a saved game</li>
 *   <li>{@code POST /keyboard}         — inject keyboard events</li>
 *   <li>{@code GET  /help/topics}      — list help topics with existence check</li>
 *   <li>{@code GET/POST/DELETE /hand-groups} — CRUD for starting hand groups</li>
 *   <li>{@code GET  /ai-types}        — list AI player type profiles</li>
 *   <li>{@code POST /simulator}       — run hand equity simulation</li>
 *   <li>{@code GET  /history}         — tournament history for current profile</li>
 *   <li>{@code GET  /history/hand}    — specific hand history by ID</li>
 *   <li>{@code GET  /game/saves}      — list saved game files</li>
 * </ul>
 */
public class GameControlServer {

    private static final Logger logger = LogManager.getLogger(GameControlServer.class);

    static final String KEY_FILE = "control-server.key";
    static final String PORT_FILE = "control-server.port";

    private HttpServer server;
    String apiKey;

    // -------------------------------------------------------------------------
    // Rebuy decision latch — lets ActionHandler resolve a pending rebuy prompt
    // that NewLevelActions is blocking on (on the EDT) waiting for API input.
    // -------------------------------------------------------------------------

    private static volatile CountDownLatch rebuyLatch;
    private static final AtomicBoolean rebuyAccepted = new AtomicBoolean(false);

    /**
     * Called by ActionHandler when REBUY or DECLINE_REBUY is received while
     * NewLevelActions is waiting for a rebuy decision via the control server.
     *
     * @param accepted true if the player chose to rebuy
     */
    static void resolveRebuyDecision(boolean accepted) {
        CountDownLatch latch = rebuyLatch;
        if (latch != null) {
            rebuyAccepted.set(accepted);
            latch.countDown();
        }
    }

    /** Returns true if a rebuy decision is currently pending. */
    static boolean isPendingRebuyDecision() {
        return rebuyLatch != null;
    }

    // -------------------------------------------------------------------------
    // Addon decision latch — lets ActionHandler resolve a pending addon prompt
    // that WebSocketTournamentDirector.onAddonOffered is blocking on (EDT).
    // -------------------------------------------------------------------------

    private static volatile CountDownLatch addonLatch;
    private static final AtomicBoolean addonAccepted = new AtomicBoolean(false);

    /**
     * Called by ActionHandler when ADDON or DECLINE_ADDON is received while
     * WebSocketTournamentDirector is waiting for an addon decision via the
     * control server.
     *
     * @param accepted true if the player chose to take the addon
     */
    static void resolveAddonDecision(boolean accepted) {
        CountDownLatch latch = addonLatch;
        if (latch != null) {
            addonAccepted.set(accepted);
            latch.countDown();
        }
    }

    /** Returns true if an addon decision is currently pending. */
    static boolean isPendingAddonDecision() {
        return addonLatch != null;
    }

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
        server.createContext("/cards/inject",     new CardInjectHandler(apiKey));
        server.createContext("/options",          new OptionsHandler(apiKey));
        server.createContext("/cheat",            new CheatHandler(apiKey));
        server.createContext("/ws-log",           new WsLogHandler(apiKey));
        server.createContext("/validate",         new ValidateHandler(apiKey));
        server.createContext("/tournament-profiles", new TournamentProfilesHandler(apiKey));
        server.createContext("/navigate",         new NavigateHandler(apiKey));
        server.createContext("/navigate/status",  new NavigateStatusHandler(apiKey));
        server.createContext("/ui/state",         new UiStateHandler(apiKey));
        server.createContext("/ui/dashboard",     new UiDashboardHandler(apiKey));
        server.createContext("/ui/dashboard/widgets", new UiDashboardWidgetsHandler(apiKey));
        server.createContext("/ui/dialogs",       new UiDialogsHandler(apiKey));
        server.createContext("/system-info",      new SystemInfoHandler(apiKey));
        server.createContext("/game/save",        new SaveLoadHandler(apiKey, "save"));
        server.createContext("/game/load",        new SaveLoadHandler(apiKey, "load"));
        server.createContext("/keyboard",         new KeyboardHandler(apiKey));
        server.createContext("/help/topics",      new HelpTopicsHandler(apiKey));
        server.createContext("/hand-groups",      new HandGroupsHandler(apiKey));
        server.createContext("/ai-types",         new AiTypesHandler(apiKey));
        server.createContext("/simulator",        new SimulatorHandler(apiKey));
        server.createContext("/history/hand",     new HistoryHandler(apiKey));
        server.createContext("/history",          new HistoryHandler(apiKey));
        server.createContext("/game/saves",       new SaveListHandler(apiKey));

        server.start();

        // Install the control-server-aware rebuy provider so NewLevelActions
        // bypasses the Swing dialog and exposes REBUY_CHECK via the API instead.
        NewLevelActions.rebuyDecisionProvider = (setInputModeFn, timeoutSeconds) -> {
            rebuyLatch = new CountDownLatch(1);
            rebuyAccepted.set(false);
            setInputModeFn.run();  // sets inputMode = MODE_REBUY_CHECK on EDT
            try {
                rebuyLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                rebuyLatch = null;
            }
            return rebuyAccepted.get();
        };

        // Install the control-server-aware addon provider so
        // WebSocketTournamentDirector.onAddonOffered bypasses the Swing button
        // and exposes REBUY_CHECK via the API instead.
        WebSocketTournamentDirector.addonDecisionProvider = (setInputModeFn, timeoutSeconds) -> {
            addonLatch = new CountDownLatch(1);
            addonAccepted.set(false);
            setInputModeFn.run();  // sets inputMode = MODE_REBUY_CHECK on EDT
            try {
                addonLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                addonLatch = null;
            }
            return addonAccepted.get();
        };

        int port = server.getAddress().getPort();
        writePortFile(port);
        logger.info("Dev control server started on localhost:{}", port);
    }

    /** Stop the HTTP server gracefully. */
    public void stop() {
        NewLevelActions.rebuyDecisionProvider = null;
        WebSocketTournamentDirector.addonDecisionProvider = null;
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
        return Path.of(FilePrefs.getConfigDirectory());
    }
}
