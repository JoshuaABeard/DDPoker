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
package com.donohoedigital.games.poker.core;

/**
 * Pure Java tournament clock with no Swing dependencies. Tracks time using
 * System.currentTimeMillis() without relying on javax.swing.Timer.
 *
 * This class is suitable for server-side use where GUI components are not
 * available. The Swing client uses GameClock which delegates to this class for
 * time tracking while adding UI timer functionality.
 *
 * Phase 4: Extracted from GameClock to enable pokergamecore to be completely
 * Swing-free for server-hosted games.
 */
public class PureTournamentClock {
    private long millisRemaining;
    private boolean running;
    private long tickBeginTime;

    /**
     * Create a new clock with zero time remaining.
     */
    public PureTournamentClock() {
        this.millisRemaining = 0;
        this.running = false;
        this.tickBeginTime = 0;
    }

    /**
     * Set the time remaining in seconds.
     *
     * @param seconds
     *            time remaining in seconds
     */
    public synchronized void setSecondsRemaining(int seconds) {
        this.tickBeginTime = System.currentTimeMillis();
        this.millisRemaining = seconds * 1000L;
    }

    /**
     * Set the time remaining in milliseconds. This method preserves sub-second
     * precision and is used for deserialization from network messages.
     *
     * @param millis
     *            time remaining in milliseconds
     */
    public synchronized void setMillisRemaining(long millis) {
        this.tickBeginTime = System.currentTimeMillis();
        this.millisRemaining = millis;
    }

    /**
     * Get the time remaining in seconds.
     *
     * @return seconds remaining (rounded down)
     */
    public synchronized int getSecondsRemaining() {
        return (int) (millisRemaining / 1000);
    }

    /**
     * Get the time remaining in milliseconds.
     *
     * @return milliseconds remaining
     */
    public synchronized long getMillisRemaining() {
        return millisRemaining;
    }

    /**
     * Check if the clock has expired (reached zero or below).
     *
     * @return true if no time remaining
     */
    public synchronized boolean isExpired() {
        return millisRemaining <= 0;
    }

    /**
     * Check if the clock is currently running.
     *
     * @return true if running
     */
    public synchronized boolean isRunning() {
        return running;
    }

    /**
     * Start the clock. Records the current time as the tick begin time.
     */
    public synchronized void start() {
        if (!running) {
            tickBeginTime = System.currentTimeMillis();
            running = true;
        }
    }

    /**
     * Stop the clock. Call tick() first to ensure time is up-to-date.
     */
    public synchronized void stop() {
        running = false;
    }

    /**
     * Update the clock based on elapsed wall time since last tick. This should be
     * called periodically (e.g., every second) to update the remaining time. If
     * more time has elapsed than remains, the clock stops at zero.
     */
    public synchronized void tick() {
        if (!running) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - tickBeginTime;

        if (elapsed >= millisRemaining) {
            // Time expired
            millisRemaining = 0;
            running = false;
        } else {
            // Subtract elapsed time
            millisRemaining -= elapsed;
        }

        tickBeginTime = now;
    }

    /**
     * Reset the clock to the specified time and stop it.
     *
     * @param seconds
     *            time in seconds
     */
    public synchronized void reset(int seconds) {
        stop();
        setSecondsRemaining(seconds);
    }
}
