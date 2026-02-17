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
package com.donohoedigital.games.poker.gameserver.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimiter.
 */
class RateLimiterTest {

    private static final String GAME_1 = "game-1";
    private static final String GAME_2 = "game-2";

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // 100ms minimum interval for testing (allows 10 actions per second)
        rateLimiter = new RateLimiter(100);
    }

    @Test
    void allowAction_allowsFirstAction() {
        assertTrue(rateLimiter.allowAction(1L, GAME_1));
    }

    @Test
    void allowAction_blocksRapidSuccessiveActions() {
        long profileId = 1L;

        // First action should be allowed
        assertTrue(rateLimiter.allowAction(profileId, GAME_1));

        // Second action immediately after should be blocked
        assertFalse(rateLimiter.allowAction(profileId, GAME_1));
    }

    @Test
    void allowAction_allowsActionAfterInterval() throws InterruptedException {
        long profileId = 1L;

        // First action
        assertTrue(rateLimiter.allowAction(profileId, GAME_1));

        // Wait for the interval to pass
        Thread.sleep(150);

        // Second action should be allowed now
        assertTrue(rateLimiter.allowAction(profileId, GAME_1));
    }

    @Test
    void allowAction_isolatesPlayersByProfileId() {
        long player1 = 1L;
        long player2 = 2L;

        // Player 1's action
        assertTrue(rateLimiter.allowAction(player1, GAME_1));

        // Player 2's action should not be blocked by player 1's rate limit
        assertTrue(rateLimiter.allowAction(player2, GAME_1));

        // But player 1's second action should still be blocked
        assertFalse(rateLimiter.allowAction(player1, GAME_1));
    }

    @Test
    void allowAction_isolatesByGame_samePlayerDifferentGames() {
        long profileId = 1L;

        // Player uses up the rate limit in game-1
        assertTrue(rateLimiter.allowAction(profileId, GAME_1));
        assertFalse(rateLimiter.allowAction(profileId, GAME_1));

        // The same player in game-2 should NOT be throttled
        assertTrue(rateLimiter.allowAction(profileId, GAME_2));
    }

    @Test
    void removePlayer_clearsPlayerState() {
        long profileId = 1L;

        // Use up the rate limit
        rateLimiter.allowAction(profileId, GAME_1);
        assertFalse(rateLimiter.allowAction(profileId, GAME_1));

        // Remove player from game
        rateLimiter.removePlayer(profileId, GAME_1);

        // Should be allowed again immediately
        assertTrue(rateLimiter.allowAction(profileId, GAME_1));
    }

    @Test
    void concurrentAccess_threadSafe() throws InterruptedException {
        int threadCount = 10;
        int actionsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long profileId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < actionsPerThread; j++) {
                        if (rateLimiter.allowAction(profileId, GAME_1)) {
                            allowedCount.incrementAndGet();
                        } else {
                            blockedCount.incrementAndGet();
                        }

                        // Small random delay to simulate real usage
                        try {
                            Thread.sleep((long) (Math.random() * 5));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Should have some allowed and some blocked (exact numbers depend on timing)
        assertTrue(allowedCount.get() > 0, "Should have allowed some actions");
        assertTrue(blockedCount.get() > 0, "Should have blocked some actions");
        assertEquals(threadCount * actionsPerThread, allowedCount.get() + blockedCount.get());
    }

    @Test
    void allowAction_tracksLastActionTimestamp() {
        long profileId = 1L;

        // First action
        rateLimiter.allowAction(profileId, GAME_1);

        // Timestamp should be tracked (tested indirectly via rate limiting behavior)
        assertFalse(rateLimiter.allowAction(profileId, GAME_1), "Second action should be blocked");
    }
}
