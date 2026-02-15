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
package com.donohoedigital.games.poker.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PureTournamentClock.
 */
class PureTournamentClockTest {

    private PureTournamentClock clock;

    @BeforeEach
    void setUp() {
        clock = new PureTournamentClock();
    }

    @Test
    void newClock_shouldHaveZeroTime() {
        assertThat(clock.getSecondsRemaining()).isEqualTo(0);
        assertThat(clock.getMillisRemaining()).isEqualTo(0);
        assertThat(clock.isExpired()).isTrue();
        assertThat(clock.isRunning()).isFalse();
    }

    @Test
    void setSecondsRemaining_shouldUpdateTime() {
        clock.setSecondsRemaining(60);

        assertThat(clock.getSecondsRemaining()).isEqualTo(60);
        assertThat(clock.getMillisRemaining()).isEqualTo(60000);
        assertThat(clock.isExpired()).isFalse();
    }

    @Test
    void setSecondsRemaining_shouldConvertToMillis() {
        clock.setSecondsRemaining(120);

        assertThat(clock.getMillisRemaining()).isEqualTo(120000);
    }

    @Test
    void start_shouldSetRunningTrue() {
        clock.setSecondsRemaining(60);
        clock.start();

        assertThat(clock.isRunning()).isTrue();
    }

    @Test
    void stop_shouldSetRunningFalse() {
        clock.setSecondsRemaining(60);
        clock.start();
        clock.stop();

        assertThat(clock.isRunning()).isFalse();
    }

    @Test
    void start_whenAlreadyRunning_shouldNotResetTickTime() throws InterruptedException {
        clock.setSecondsRemaining(10);
        clock.start();

        Thread.sleep(100); // Let some time pass
        long timeAfterSleep = clock.getMillisRemaining();

        clock.start(); // Call start again
        clock.tick();

        // Time should continue from where it was, not reset
        assertThat(clock.getMillisRemaining()).isLessThan(timeAfterSleep);
    }

    @Test
    void tick_whenNotRunning_shouldNotChangeTime() {
        clock.setSecondsRemaining(60);
        clock.tick();

        assertThat(clock.getSecondsRemaining()).isEqualTo(60);
    }

    @Test
    void tick_whenRunning_shouldDecreaseTime() throws InterruptedException {
        clock.setSecondsRemaining(10);
        clock.start();

        Thread.sleep(1100); // Wait just over 1 second
        clock.tick();

        assertThat(clock.getSecondsRemaining()).isLessThan(10);
        assertThat(clock.getSecondsRemaining()).isGreaterThanOrEqualTo(8); // Allow for timing variance
    }

    @Test
    void tick_whenTimeExpires_shouldStopAtZero() throws InterruptedException {
        clock.setSecondsRemaining(1);
        clock.start();

        Thread.sleep(1100); // Wait for time to expire
        clock.tick();

        assertThat(clock.getMillisRemaining()).isEqualTo(0);
        assertThat(clock.isExpired()).isTrue();
        assertThat(clock.isRunning()).isFalse();
    }

    @Test
    void tick_whenMoreTimePasses_thanRemaining_shouldStopAtZero() throws InterruptedException {
        clock.setSecondsRemaining(1);
        clock.start();

        Thread.sleep(2000); // Wait longer than remaining time
        clock.tick();

        assertThat(clock.getMillisRemaining()).isEqualTo(0);
        assertThat(clock.isExpired()).isTrue();
        assertThat(clock.isRunning()).isFalse();
    }

    @Test
    void multipleTicks_shouldAccumulateElapsedTime() throws InterruptedException {
        clock.setSecondsRemaining(10);
        clock.start();

        // Tick multiple times
        for (int i = 0; i < 3; i++) {
            Thread.sleep(600);
            clock.tick();
        }

        // Should have at least 1.8 seconds elapsed (3 * 600ms), max 10 remaining
        // Allow variance - Thread.sleep is not precise
        assertThat(clock.getSecondsRemaining()).isLessThan(10);
        assertThat(clock.getSecondsRemaining()).isGreaterThanOrEqualTo(6); // Allow generous variance
    }

    @Test
    void reset_shouldStopClockAndSetTime() {
        clock.setSecondsRemaining(60);
        clock.start();
        clock.reset(120);

        assertThat(clock.getSecondsRemaining()).isEqualTo(120);
        assertThat(clock.isRunning()).isFalse();
    }

    @Test
    void getSecondsRemaining_shouldRoundDown() throws InterruptedException {
        // Use setMillisRemaining to test actual truncation
        clock.setMillisRemaining(5999); // 5.999 seconds

        // Should round down to 5 seconds
        assertThat(clock.getSecondsRemaining()).isEqualTo(5);
        assertThat(clock.getMillisRemaining()).isEqualTo(5999);

        // Test with another value
        clock.setMillisRemaining(1500); // 1.5 seconds
        assertThat(clock.getSecondsRemaining()).isEqualTo(1);

        // Test edge case: just under 1 second
        clock.setMillisRemaining(999);
        assertThat(clock.getSecondsRemaining()).isEqualTo(0);
    }

    @Test
    void isExpired_withNonZeroTime_shouldBeFalse() {
        clock.setSecondsRemaining(1);

        assertThat(clock.isExpired()).isFalse();
    }

    @Test
    void isExpired_withZeroTime_shouldBeTrue() {
        clock.setSecondsRemaining(0);

        assertThat(clock.isExpired()).isTrue();
    }

    @Test
    void threadSafety_concurrentTicksDoNotCorruptState() throws InterruptedException {
        clock.setSecondsRemaining(10);
        clock.start();

        // Create multiple threads that tick concurrently
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    clock.tick();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // Start all threads concurrently
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Clock should be in a valid state (not corrupted)
        // Time should be between 0 and 10 seconds
        int remaining = clock.getSecondsRemaining();
        assertThat(remaining).isGreaterThanOrEqualTo(0);
        assertThat(remaining).isLessThanOrEqualTo(10);

        // Should either be running or expired (but not in invalid state)
        if (clock.isExpired()) {
            assertThat(remaining).isEqualTo(0);
            assertThat(clock.isRunning()).isFalse();
        }
    }
}
