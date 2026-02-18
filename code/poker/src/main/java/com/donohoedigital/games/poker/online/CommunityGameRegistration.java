/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.gameserver.dto.CommunityGameRegisterRequest;
import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the WAN registration lifecycle for a community-hosted game.
 *
 * <p>
 * Workflow:
 * <ol>
 * <li>Call {@link #register} to register the game and start heartbeats.</li>
 * <li>Call {@link #deregister} when the game ends or the host shuts down.</li>
 * </ol>
 *
 * <p>
 * A JVM shutdown hook ensures deregistration even on abnormal exit.
 */
public class CommunityGameRegistration {

    private static final Logger logger = LogManager.getLogger(CommunityGameRegistration.class);

    /** Send a heartbeat every 2 minutes (WAN server timeout is 5 minutes). */
    private static final long HEARTBEAT_INTERVAL_SECONDS = 120;

    private final RestGameClient client;

    private volatile String registeredGameId;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatTask;
    private Thread shutdownHook;

    /**
     * @param client
     *            REST client connected to the WAN server
     */
    public CommunityGameRegistration(RestGameClient client) {
        this.client = client;
    }

    /**
     * Register this community-hosted game with the WAN server and start heartbeats.
     *
     * @param gameName
     *            display name of the game
     * @param wsUrl
     *            publicly reachable WebSocket URL of the host's embedded server
     * @param password
     *            optional join password; {@code null} for a public game
     * @return the server-assigned game ID
     * @throws RestGameClient.RestGameClientException
     *             if registration fails
     */
    public String register(String gameName, String wsUrl, String password) {
        CommunityGameRegisterRequest req = new CommunityGameRegisterRequest(gameName, wsUrl, null, password);
        GameSummary created = client.registerCommunityGame(req);
        registeredGameId = created.gameId();
        logger.info("Registered community game {} with WAN server (id={})", gameName, registeredGameId);

        startHeartbeats();
        installShutdownHook();
        return registeredGameId;
    }

    /**
     * Deregister the game from the WAN server and stop heartbeats. Safe to call
     * multiple times.
     */
    public void deregister() {
        stopHeartbeats();
        removeShutdownHook();

        String gameId = registeredGameId;
        if (gameId != null) {
            registeredGameId = null;
            client.cancelGame(gameId);
            logger.info("Deregistered community game {}", gameId);
        }
    }

    /** @return the server-assigned game ID, or {@code null} if not registered */
    public String getRegisteredGameId() {
        return registeredGameId;
    }

    private void startHeartbeats() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CommunityGameHeartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatTask = scheduler.scheduleWithFixedDelay(this::sendHeartbeat, HEARTBEAT_INTERVAL_SECONDS,
                HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        String gameId = registeredGameId;
        if (gameId != null) {
            client.sendHeartbeat(gameId);
            logger.debug("Sent heartbeat for community game {}", gameId);
        }
    }

    private void stopHeartbeats() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void installShutdownHook() {
        shutdownHook = new Thread(() -> {
            logger.info("JVM shutdown — deregistering community game");
            deregister();
        }, "CommunityGameShutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void removeShutdownHook() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM already shutting down — hook is running
            }
            shutdownHook = null;
        }
    }
}
