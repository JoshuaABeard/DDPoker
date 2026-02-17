/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Registry and lifecycle manager for all GameInstances. Thread pool management
 * for running game director threads.
 *
 * <p>
 * This class is thread-safe and manages the lifecycle of all active games on
 * the server.
 */
public class GameInstanceManager {

    private final GameServerProperties properties;
    private final ConcurrentHashMap<String, GameInstance> games = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private volatile boolean shutdown = false;

    public GameInstanceManager(GameServerProperties properties) {
        this.properties = properties;
        this.executor = Executors.newScheduledThreadPool(properties.threadPoolSize());

        // Schedule periodic cleanup of completed games (every minute)
        executor.scheduleAtFixedRate(this::cleanupCompletedGames, 1, 1, TimeUnit.MINUTES);
    }

    // ====================================
    // Game Lifecycle
    // ====================================

    /**
     * Create a new game instance.
     *
     * @param ownerProfileId
     *            profile ID of the game owner
     * @param config
     *            game configuration
     * @return new GameInstance in CREATED state
     * @throws GameServerException
     *             if maximum concurrent games limit is reached
     */
    public GameInstance createGame(long ownerProfileId, GameConfig config) {
        if (shutdown) {
            throw new GameServerException("Server is shutting down");
        }

        if (games.size() >= properties.maxConcurrentGames()) {
            throw new GameServerException("Maximum concurrent games reached: " + properties.maxConcurrentGames());
        }

        // Check per-user game limit
        int userGameCount = (int) games
                .values().stream().filter(g -> g.getOwnerProfileId() == ownerProfileId
                        && g.getState() != GameInstanceState.COMPLETED && g.getState() != GameInstanceState.CANCELLED)
                .count();
        if (userGameCount >= properties.maxGamesPerUser()) {
            throw new GameServerException(
                    "User has reached maximum active game limit: " + properties.maxGamesPerUser());
        }

        String gameId = generateGameId();
        GameInstance instance = GameInstance.create(gameId, ownerProfileId, config, properties);
        games.put(gameId, instance);

        return instance;
    }

    /**
     * Start a game with owner authorization.
     *
     * @param gameId
     *            the game to start
     * @param requesterId
     *            the user requesting the start
     * @throws GameServerException
     *             if game not found or requester is not the owner
     */
    public void startGame(String gameId, long requesterId) {
        GameInstance instance = getGameOrThrow(gameId);
        instance.startAsUser(requesterId, executor);
    }

    /**
     * Get a game by ID.
     *
     * @param gameId
     *            the game ID
     * @return the GameInstance, or null if not found
     */
    public GameInstance getGame(String gameId) {
        return games.get(gameId);
    }

    /**
     * List all games, optionally filtered by state.
     *
     * @param statusFilter
     *            state filter, or null for all games
     * @return list of matching games
     */
    public List<GameInstance> listGames(GameInstanceState statusFilter) {
        return games.values().stream().filter(g -> statusFilter == null || g.getState() == statusFilter)
                .collect(Collectors.toList());
    }

    // ====================================
    // Cleanup
    // ====================================

    /**
     * Remove completed games that finished over an hour ago.
     *
     * <p>
     * Called automatically every minute by scheduled executor.
     */
    public void cleanupCompletedGames() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));

        games.entrySet().removeIf(entry -> {
            GameInstance game = entry.getValue();
            GameInstanceState state = game.getState();
            Instant completedAt = game.getCompletedAt();

            return (state == GameInstanceState.COMPLETED || state == GameInstanceState.CANCELLED) && completedAt != null
                    && completedAt.isBefore(cutoff);
        });
    }

    // ====================================
    // Shutdown
    // ====================================

    /**
     * Shutdown the manager and all active games.
     *
     * <p>
     * Stops accepting new games, shuts down all active games gracefully, and
     * terminates the executor.
     */
    public void shutdown() {
        shutdown = true;

        // Shutdown all active games
        games.values().forEach(GameInstance::shutdown);

        // Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ====================================
    // Helper Methods
    // ====================================

    private GameInstance getGameOrThrow(String gameId) {
        GameInstance game = games.get(gameId);
        if (game == null) {
            throw new GameServerException("Game not found: " + gameId);
        }
        return game;
    }

    private String generateGameId() {
        return UUID.randomUUID().toString();
    }
}
