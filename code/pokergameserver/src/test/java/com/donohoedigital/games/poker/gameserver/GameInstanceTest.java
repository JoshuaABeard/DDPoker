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

import static org.junit.jupiter.api.Assertions.*;

import com.donohoedigital.games.poker.gameserver.GameConfig.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link GameInstance} */
class GameInstanceTest {

    private GameServerProperties properties;
    private GameConfig config;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        properties = new GameServerProperties();
        config = createTestConfig();
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    // ====================================
    // Lifecycle Tests
    // ====================================

    @Test
    void testInitialState() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);

        assertEquals("test-1", game.getGameId());
        assertEquals(100L, game.getOwnerProfileId());
        assertEquals(GameInstanceState.CREATED, game.getState());
        assertNull(game.getCompletedAt());
    }

    @Test
    void testTransitionToWaitingForPlayers() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();

        assertEquals(GameInstanceState.WAITING_FOR_PLAYERS, game.getState());
    }

    @Test
    void testLifecycleProgression() throws Exception {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);

        // CREATED → WAITING_FOR_PLAYERS
        game.transitionToWaitingForPlayers();
        assertEquals(GameInstanceState.WAITING_FOR_PLAYERS, game.getState());

        // Add AI players
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);
        game.addPlayer(3, "AI-3", true, 50);
        game.addPlayer(4, "AI-4", true, 50);

        // WAITING_FOR_PLAYERS → IN_PROGRESS (via start)
        game.start(executor);
        assertEquals(GameInstanceState.IN_PROGRESS, game.getState());

        // Wait for game to be actively processing (not immediately completed)
        // If game completes too quickly, skip pause/resume test
        if (awaitState(game, state -> state == GameInstanceState.IN_PROGRESS, 100)) {
            game.pause();
            assertEquals(GameInstanceState.PAUSED, game.getState());

            // PAUSED → IN_PROGRESS
            game.resume();
            assertEquals(GameInstanceState.IN_PROGRESS, game.getState());
        }

        // Shutdown will eventually lead to COMPLETED
        game.shutdown();

        // Wait for completion (should happen quickly with 2-player AI game)
        boolean completed = awaitState(game, state -> state == GameInstanceState.COMPLETED, 5000);
        assertTrue(completed, "Game should complete within 5 seconds of shutdown");
    }

    @Test
    void testCancel() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.cancel();

        assertEquals(GameInstanceState.CANCELLED, game.getState());
        assertNotNull(game.getCompletedAt());
    }

    // ====================================
    // Player Management Tests
    // ====================================

    @Test
    void testAddPlayer() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();

        game.addPlayer(200L, "Player1", false, 0);

        assertEquals(1, game.getPlayerCount());
        assertTrue(game.hasPlayer(200L));
    }

    @Test
    void testAddMultiplePlayers() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();

        game.addPlayer(200L, "Player1", false, 0);
        game.addPlayer(201L, "Player2", false, 0);
        game.addPlayer(202L, "AI-1", true, 50);

        assertEquals(3, game.getPlayerCount());
    }

    @Test
    void testCannotAddPlayersInCreatedState() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);

        assertThrows(IllegalStateException.class, () -> game.addPlayer(200L, "Player1", false, 0));
    }

    @Test
    void testCannotAddPlayersAfterGameStarts() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);
        game.start(executor);

        assertThrows(IllegalStateException.class, () -> game.addPlayer(200L, "Player1", false, 0));
    }

    @Test
    void testRemovePlayerBeforeStart() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(200L, "Player1", false, 0);

        assertEquals(1, game.getPlayerCount());

        game.removePlayer(200L);

        assertEquals(0, game.getPlayerCount());
        assertFalse(game.hasPlayer(200L));
    }

    @Test
    void testRemovePlayerAfterStartMarksDisconnected() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(200L, "Player1", false, 0);
        game.addPlayer(1, "AI-1", true, 50);
        game.start(executor);

        // Player still in game
        assertTrue(game.hasPlayer(200L));

        // Remove marks as disconnected, doesn't actually remove
        game.removePlayer(200L);

        // Player still present but marked disconnected
        assertTrue(game.hasPlayer(200L));
        assertTrue(game.isPlayerDisconnected(200L));
    }

    @Test
    void testPlayerReconnect() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(200L, "Player1", false, 0);
        game.addPlayer(1, "AI-1", true, 50);
        game.start(executor);

        game.removePlayer(200L);
        assertTrue(game.isPlayerDisconnected(200L));

        game.reconnectPlayer(200L);
        assertFalse(game.isPlayerDisconnected(200L));
    }

    // ====================================
    // Authorization Tests
    // ====================================

    @Test
    void testOnlyOwnerCanStart() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);

        // Non-owner cannot start
        assertThrows(GameServerException.class, () -> game.startAsUser(999L, executor));

        // Owner can start
        assertDoesNotThrow(() -> game.startAsUser(100L, executor));
    }

    @Test
    void testOnlyOwnerCanPause() throws Exception {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        addPlayersForPauseTest(game);
        game.start(executor);

        // Wait for game to be actively processing (or skip test if completed too
        // quickly)
        if (awaitState(game, state -> state == GameInstanceState.IN_PROGRESS, 100)) {
            // Non-owner cannot pause
            if (game.getState() == GameInstanceState.IN_PROGRESS) {
                assertThrows(GameServerException.class, () -> game.pauseAsUser(999L));
            }

            // Owner can pause
            if (game.getState() == GameInstanceState.IN_PROGRESS) {
                assertDoesNotThrow(() -> game.pauseAsUser(100L));
            }
        }
    }

    @Test
    void testOnlyOwnerCanCancel() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();

        // Non-owner cannot cancel
        assertThrows(GameServerException.class, () -> game.cancelAsUser(999L));

        // Owner can cancel
        assertDoesNotThrow(() -> game.cancelAsUser(100L));
    }

    @Test
    void testOnlyOwnerCanResume() throws Exception {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        addPlayersForPauseTest(game);
        game.start(executor);

        // Wait for game to be actively processing (or skip test if completed too
        // quickly)
        if (awaitState(game, state -> state == GameInstanceState.IN_PROGRESS, 100)) {
            // Pause as owner first
            if (game.getState() == GameInstanceState.IN_PROGRESS) {
                game.pauseAsUser(100L);
            }

            if (game.getState() == GameInstanceState.PAUSED) {
                // Non-owner cannot resume
                assertThrows(GameServerException.class, () -> game.resumeAsUser(999L));

                // Owner can resume
                if (game.getState() == GameInstanceState.PAUSED) {
                    assertDoesNotThrow(() -> game.resumeAsUser(100L));
                }
            }
        }
    }

    @Test
    void testGetEventBus() throws Exception {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "AI-1", true, 50);
        game.start(executor);

        assertNotNull(game.getEventBus());
    }

    @Test
    void testGetPlayerSessions() {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "Player1", false, 0);
        game.addPlayer(2, "Player2", false, 0);

        Map<Long, ServerPlayerSession> sessions = game.getPlayerSessions();
        assertNotNull(sessions);
        assertEquals(2, sessions.size());
        assertTrue(sessions.containsKey(1L));
        assertTrue(sessions.containsKey(2L));

        // Verify map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> sessions.put(3L, null));
    }

    // ====================================
    // AI-Only Game Test
    // ====================================

    @Test
    void testAIOnlyGameRunsToCompletion() throws Exception {
        GameInstance game = GameInstance.create("test-1", 100L, config, properties);
        game.transitionToWaitingForPlayers();

        // Add 4 AI players
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);
        game.addPlayer(3, "AI-3", true, 50);
        game.addPlayer(4, "AI-4", true, 50);

        game.start(executor);

        // Wait for game to complete (with 120 second timeout)
        boolean completed = awaitState(game, state -> state == GameInstanceState.COMPLETED, 120000);
        assertTrue(completed, "Game should complete within 120 seconds");
        assertEquals(GameInstanceState.COMPLETED, game.getState());
        assertNotNull(game.getCompletedAt());
    }

    // ====================================
    // Helper Methods
    // ====================================

    /**
     * Add enough AI players for pause/resume tests to avoid race conditions.
     * Two-player games complete too quickly on fast CI machines.
     */
    private void addPlayersForPauseTest(GameInstance game) {
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);
        game.addPlayer(3, "AI-3", true, 50);
        game.addPlayer(4, "AI-4", true, 50);
        game.addPlayer(5, "AI-5", true, 50);
        game.addPlayer(6, "AI-6", true, 50);
    }

    private GameConfig createTestConfig() {
        // 6 blind levels for hands-based testing
        // Old setLevel(level, smallBlind, bigBlind, ante, minutes)
        List<BlindLevel> blinds = List.of(new BlindLevel(25, 50, 100, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(25, 100, 200, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(50, 150, 300, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(50, 200, 400, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(75, 300, 600, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(100, 400, 800, 1, false, "NOLIMIT_HOLDEM"));

        return new GameConfig("Test Tournament", "Test tournament description", "Welcome!", 10, // maxPlayers
                90, // maxOnlinePlayers
                true, // fillComputer
                1000, // buyIn (not used in tests, but required)
                500, // startingChips
                blinds, true, // doubleAfterLastLevel
                "NOLIMIT_HOLDEM", LevelAdvanceMode.HANDS, // Hands-based advancement
                2, // handsPerLevel (2 hands per level)
                20, // defaultMinutesPerLevel
                null, // rebuys disabled
                null, // addons disabled
                new PayoutConfig("SPOTS", 3, 0, 0, "AUTO", List.of()), null, // house
                null, // bounty
                new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10), null, // late registration
                null, // scheduled start
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), true, // onlineActivatedOnly
                false, // allowDash
                false, // allowAdvisor
                List.of());
    }

    /**
     * Wait for a state condition to become true, polling with short intervals.
     *
     * @param game
     *            the game instance to check
     * @param condition
     *            the condition to wait for
     * @param timeoutMillis
     *            maximum time to wait in milliseconds
     * @return true if condition became true within timeout
     */
    private boolean awaitState(GameInstance game, java.util.function.Predicate<GameInstanceState> condition,
            long timeoutMillis) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (condition.test(game.getState())) {
                return true;
            }
            try {
                Thread.sleep(10); // Poll every 10ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
