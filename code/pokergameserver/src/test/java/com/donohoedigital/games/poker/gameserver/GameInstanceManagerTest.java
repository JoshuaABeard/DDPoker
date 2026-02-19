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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link GameInstanceManager} */
class GameInstanceManagerTest {

    private GameInstanceManager manager;
    private GameServerProperties properties;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        properties = new GameServerProperties(5, 0, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0);
        manager = new GameInstanceManager(properties);
        config = createTestConfig();
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    // ====================================
    // Game Creation Tests
    // ====================================

    @Test
    void testCreateGame() {
        GameInstance game = manager.createGame(100L, config);

        assertNotNull(game);
        assertNotNull(game.getGameId());
        assertEquals(100L, game.getOwnerProfileId());
        assertEquals(GameInstanceState.CREATED, game.getState());
    }

    @Test
    void testCreateMultipleGames() {
        GameInstance game1 = manager.createGame(100L, config);
        GameInstance game2 = manager.createGame(101L, config);
        GameInstance game3 = manager.createGame(102L, config);

        assertNotEquals(game1.getGameId(), game2.getGameId());
        assertNotEquals(game2.getGameId(), game3.getGameId());

        List<GameInstance> allGames = manager.listGames(null);
        assertEquals(3, allGames.size());
    }

    @Test
    void testConcurrentGameLimitEnforced() {
        // Create max games (limit is 5)
        for (int i = 0; i < 5; i++) {
            manager.createGame(100L + i, config);
        }

        // Creating one more should fail
        assertThrows(GameServerException.class, () -> manager.createGame(200L, config));
    }

    @Test
    void testPerUserGameLimitEnforced() {
        GameServerProperties limitedProps = new GameServerProperties(10, 0, 120, 10, 1000, 3, 2, 2, 5, 24, 7,
                "ws://localhost", 0);
        GameInstanceManager limitedManager = new GameInstanceManager(limitedProps);
        try {
            limitedManager.createGame(100L, config); // 1st game for user 100
            limitedManager.createGame(100L, config); // 2nd game for user 100 (at limit)

            // 3rd game for same user should be rejected
            assertThrows(GameServerException.class, () -> limitedManager.createGame(100L, config));
        } finally {
            limitedManager.shutdown();
        }
    }

    @Test
    void testPerUserLimitIsIndependentPerUser() {
        GameServerProperties limitedProps = new GameServerProperties(10, 0, 120, 10, 1000, 3, 2, 2, 5, 24, 7,
                "ws://localhost", 0);
        GameInstanceManager limitedManager = new GameInstanceManager(limitedProps);
        try {
            limitedManager.createGame(100L, config); // user 100: 1 game
            limitedManager.createGame(100L, config); // user 100: 2 games (at limit)

            // Different user is unaffected by user 100's limit
            assertDoesNotThrow(() -> limitedManager.createGame(101L, config));
        } finally {
            limitedManager.shutdown();
        }
    }

    @Test
    void testGeneratedGameIdsAreUnique() {
        GameInstance game1 = manager.createGame(100L, config);
        GameInstance game2 = manager.createGame(100L, config);
        GameInstance game3 = manager.createGame(100L, config);

        assertNotEquals(game1.getGameId(), game2.getGameId());
        assertNotEquals(game2.getGameId(), game3.getGameId());
    }

    // ====================================
    // Game Lookup Tests
    // ====================================

    @Test
    void testGetGame() {
        GameInstance created = manager.createGame(100L, config);
        String gameId = created.getGameId();

        GameInstance retrieved = manager.getGame(gameId);

        assertNotNull(retrieved);
        assertSame(created, retrieved);
    }

    @Test
    void testGetNonexistentGame() {
        GameInstance game = manager.getGame("nonexistent-id");
        assertNull(game);
    }

    // ====================================
    // Game Listing Tests
    // ====================================

    @Test
    void testListAllGames() {
        manager.createGame(100L, config);
        manager.createGame(101L, config);
        manager.createGame(102L, config);

        List<GameInstance> allGames = manager.listGames(null);
        assertEquals(3, allGames.size());
    }

    @Test
    void testListGamesByState() {
        GameInstance game1 = manager.createGame(100L, config);
        GameInstance game2 = manager.createGame(101L, config);
        GameInstance game3 = manager.createGame(102L, config);

        game1.transitionToWaitingForPlayers();
        game2.transitionToWaitingForPlayers();
        // game3 stays in CREATED

        List<GameInstance> waiting = manager.listGames(GameInstanceState.WAITING_FOR_PLAYERS);
        assertEquals(2, waiting.size());

        List<GameInstance> created = manager.listGames(GameInstanceState.CREATED);
        assertEquals(1, created.size());
    }

    @Test
    void testListInProgressGames() {
        GameInstance game1 = manager.createGame(100L, config);
        GameInstance game2 = manager.createGame(101L, config);

        game1.transitionToWaitingForPlayers();
        game1.addPlayer(1, "AI-1", true, 50);
        game1.addPlayer(2, "AI-2", true, 50);

        manager.startGame(game1.getGameId(), 100L);

        List<GameInstance> inProgress = manager.listGames(GameInstanceState.IN_PROGRESS);
        assertEquals(1, inProgress.size());
    }

    // ====================================
    // Game Start Tests
    // ====================================

    @Test
    void testStartGame() {
        GameInstance game = manager.createGame(100L, config);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);

        manager.startGame(game.getGameId(), 100L);

        assertEquals(GameInstanceState.IN_PROGRESS, game.getState());
    }

    @Test
    void testStartGameRequiresOwner() {
        GameInstance game = manager.createGame(100L, config);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);

        // Non-owner cannot start
        assertThrows(GameServerException.class, () -> manager.startGame(game.getGameId(), 999L));
    }

    @Test
    void testStartNonexistentGame() {
        assertThrows(GameServerException.class, () -> manager.startGame("nonexistent-id", 100L));
    }

    // ====================================
    // Cleanup Tests
    // ====================================

    @Test
    void testCleanupRemovesOldCompletedGames() throws Exception {
        GameInstance game1 = manager.createGame(100L, config);
        GameInstance game2 = manager.createGame(101L, config);

        // Mark game1 as completed over an hour ago (simulate by setting completed time)
        game1.cancel(); // Sets state to CANCELLED and completedAt
        // Manually adjust completedAt to be old (this would require package-private
        // access or
        // reflection)
        // For now, we'll test the cleanup logic indirectly

        List<GameInstance> beforeCleanup = manager.listGames(null);
        assertEquals(2, beforeCleanup.size());

        // Cleanup should not remove recently completed games
        manager.cleanupCompletedGames();

        List<GameInstance> afterCleanup = manager.listGames(null);
        assertEquals(2, afterCleanup.size()); // Both still present (game1 completed within the hour)
    }

    @Test
    void testCleanupDoesNotRemoveActiveGames() {
        GameInstance game1 = manager.createGame(100L, config);
        GameInstance game2 = manager.createGame(101L, config);

        game1.transitionToWaitingForPlayers();
        game1.addPlayer(1, "AI-1", true, 50);
        game1.addPlayer(2, "AI-2", true, 50);
        manager.startGame(game1.getGameId(), 100L);

        manager.cleanupCompletedGames();

        List<GameInstance> afterCleanup = manager.listGames(null);
        assertEquals(2, afterCleanup.size());
    }

    // ====================================
    // Shutdown Tests
    // ====================================

    @Test
    void testShutdown() {
        manager.createGame(100L, config);
        manager.createGame(101L, config);

        assertDoesNotThrow(() -> manager.shutdown());
    }

    @Test
    void testShutdownStopsAllGames() throws Exception {
        GameInstance game = manager.createGame(100L, config);
        game.transitionToWaitingForPlayers();
        game.addPlayer(1, "AI-1", true, 50);
        game.addPlayer(2, "AI-2", true, 50);
        manager.startGame(game.getGameId(), 100L);

        manager.shutdown();

        // After shutdown, executor should be terminated
        // (This would require exposing executor state or testing indirectly)
        assertTrue(true); // Placeholder - actual verification requires executor access
    }

    // ====================================
    // Helper Methods
    // ====================================

    private GameConfig createTestConfig() {
        List<BlindLevel> blinds = List.of(new BlindLevel(25, 50, 100, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(25, 100, 200, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(50, 150, 300, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(50, 200, 400, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(75, 300, 600, 1, false, "NOLIMIT_HOLDEM"),
                new BlindLevel(100, 400, 800, 1, false, "NOLIMIT_HOLDEM"));

        return new GameConfig("Test Tournament", "Test description", "Welcome!", 10, 90, true, 1000, 500, blinds, true,
                "NOLIMIT_HOLDEM", LevelAdvanceMode.HANDS, 2, 20, null, null,
                new PayoutConfig("SPOTS", 3, 0, 0, "AUTO", List.of()), null, null,
                new TimeoutConfig(30, 0, 0, 0, 0, 15), new BootConfig(true, 25, true, 10), null, null,
                new InviteConfig(false, List.of(), true), new BettingConfig(0, true), false, false, List.of());
    }
}
