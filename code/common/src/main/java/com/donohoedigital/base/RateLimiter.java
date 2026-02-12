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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple time-window based rate limiter for preventing DoS attacks.
 *
 * <p>
 * Uses a sliding window approach where requests are counted within a time
 * window. When the window expires, the counter resets.
 *
 * <p>
 * Thread-safe using ConcurrentHashMap.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * RateLimiter limiter = new RateLimiter();
 * if (limiter.allowRequest(userIp, 10, 60000)) {
 * 	// Allow request (< 10 requests per minute)
 * } else {
 * 	// Deny request (rate limit exceeded)
 * }
 * </pre>
 */
public class RateLimiter {
    private final ConcurrentMap<String, RateLimitEntry> entries;

    /**
     * Creates a new RateLimiter instance.
     */
    public RateLimiter() {
        this.entries = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a request should be allowed based on rate limiting rules.
     *
     * @param key
     *            unique identifier for the rate limit (e.g., IP address, user ID)
     * @param maxRequests
     *            maximum number of requests allowed in the time window
     * @param windowMs
     *            time window in milliseconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean allowRequest(String key, int maxRequests, long windowMs) {
        if (key == null) {
            key = "null"; // Handle null keys gracefully
        }

        if (maxRequests <= 0) {
            return false; // No requests allowed if limit is 0 or negative
        }

        long now = System.currentTimeMillis();

        // Compute or update the entry atomically
        RateLimitEntry entry = entries.compute(key, (k, existing) -> {
            if (existing == null) {
                // First request for this key
                return new RateLimitEntry(now, 1);
            }

            // Check if window has expired
            if (now - existing.windowStart >= windowMs) {
                // Window expired, start new window
                return new RateLimitEntry(now, 1);
            }

            // Within same window, increment count
            return new RateLimitEntry(existing.windowStart, existing.count + 1);
        });

        // Check if this request exceeds the limit
        return entry.count <= maxRequests;
    }

    /**
     * Removes expired entries to prevent memory leaks. Should be called
     * periodically (e.g., every minute).
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        long maxAge = 60000; // Remove entries older than 1 minute

        entries.entrySet().removeIf(e -> now - e.getValue().windowStart > maxAge);
    }

    /**
     * Clears all rate limit entries. Useful for testing or manual reset.
     */
    public void reset() {
        entries.clear();
    }

    /**
     * Immutable entry tracking request count and window start time.
     */
    private static class RateLimitEntry {
        final long windowStart;
        final int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
