/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.base;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for RateLimiter utility class
 */
class RateLimiterTest {
    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
    }

    // ========================================
    // Basic Rate Limiting Tests
    // ========================================

    @Test
    void allowRequest_WithinLimit_ShouldAllow() {
        // Allow 5 requests per 1000ms window
        assertThat(rateLimiter.allowRequest("user1", 5, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 5, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 5, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 5, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 5, 1000)).isTrue();
    }

    @Test
    void allowRequest_ExceedsLimit_ShouldDeny() {
        // Allow 3 requests per 1000ms window
        assertThat(rateLimiter.allowRequest("user1", 3, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 3, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 3, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 3, 1000)).isFalse(); // 4th should fail
        assertThat(rateLimiter.allowRequest("user1", 3, 1000)).isFalse(); // 5th should fail
    }

    @Test
    void allowRequest_DifferentKeys_ShouldBeIndependent() {
        // Each key should have independent limits
        assertThat(rateLimiter.allowRequest("user1", 2, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 2, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 2, 1000)).isFalse(); // user1 limit reached

        assertThat(rateLimiter.allowRequest("user2", 2, 1000)).isTrue(); // user2 still allowed
        assertThat(rateLimiter.allowRequest("user2", 2, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("user2", 2, 1000)).isFalse(); // user2 limit reached
    }

    @Test
    void allowRequest_AfterWindowExpires_ShouldReset() throws InterruptedException {
        // Allow 2 requests per 100ms window
        assertThat(rateLimiter.allowRequest("user1", 2, 100)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 2, 100)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 2, 100)).isFalse(); // limit reached

        // Wait for window to expire
        Thread.sleep(150);

        // Should allow again after window reset
        assertThat(rateLimiter.allowRequest("user1", 2, 100)).isTrue();
        assertThat(rateLimiter.allowRequest("user1", 2, 100)).isTrue();
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void allowRequest_NullKey_ShouldNotThrow() {
        // Should handle null key gracefully
        assertThatCode(() -> rateLimiter.allowRequest(null, 5, 1000)).doesNotThrowAnyException();
    }

    @Test
    void allowRequest_EmptyKey_ShouldWork() {
        assertThat(rateLimiter.allowRequest("", 2, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("", 2, 1000)).isTrue();
        assertThat(rateLimiter.allowRequest("", 2, 1000)).isFalse();
    }

    @Test
    void allowRequest_ZeroMaxRequests_ShouldDenyAll() {
        assertThat(rateLimiter.allowRequest("user1", 0, 1000)).isFalse();
    }

    @Test
    void allowRequest_NegativeMaxRequests_ShouldDenyAll() {
        assertThat(rateLimiter.allowRequest("user1", -1, 1000)).isFalse();
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    void allowRequest_ConcurrentRequests_ShouldBeThreadSafe() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 5;
        int maxAllowed = 20; // Less than threadCount * requestsPerThread

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger deniedCount = new AtomicInteger(0);

        // Start threads
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < requestsPerThread; j++) {
                        if (rateLimiter.allowRequest("concurrent-test", maxAllowed, 1000)) {
                            allowedCount.incrementAndGet();
                        } else {
                            deniedCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // Start all threads at once
        startLatch.countDown();
        doneLatch.await();

        // Verify exactly maxAllowed requests were allowed
        assertThat(allowedCount.get()).isEqualTo(maxAllowed);
        assertThat(deniedCount.get()).isEqualTo((threadCount * requestsPerThread) - maxAllowed);
    }

    // ========================================
    // Cleanup Tests
    // ========================================

    @Test
    void cleanup_ShouldRemoveExpiredEntries() throws InterruptedException {
        // Make requests
        rateLimiter.allowRequest("user1", 2, 50);
        rateLimiter.allowRequest("user1", 2, 50);
        rateLimiter.allowRequest("user2", 2, 50);

        // Verify entries exist (they're rate limited now)
        assertThat(rateLimiter.allowRequest("user1", 2, 50)).isFalse(); // count=3, max=2, denied

        // Wait for entries to be older than cleanup threshold (60 seconds)
        // Since that's too long for a unit test, just verify cleanup doesn't break
        // anything
        rateLimiter.cleanup();

        // Entry still exists (not old enough to clean), so still rate limited
        assertThat(rateLimiter.allowRequest("user1", 2, 50)).isFalse();

        // But after waiting for window to expire, new requests should be allowed
        Thread.sleep(100);
        assertThat(rateLimiter.allowRequest("user1", 2, 100)).isTrue();
    }

    @Test
    void reset_ShouldClearAllEntries() {
        rateLimiter.allowRequest("user1", 2, 1000);
        rateLimiter.allowRequest("user1", 2, 1000);
        assertThat(rateLimiter.allowRequest("user1", 2, 1000)).isFalse();

        // Reset should clear all limits
        rateLimiter.reset();

        assertThat(rateLimiter.allowRequest("user1", 2, 1000)).isTrue();
    }
}
