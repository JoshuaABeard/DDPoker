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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for player actions and chat messages.
 *
 * Enforces a minimum time interval between consecutive actions from the same
 * player to prevent spam and abuse.
 */
public class RateLimiter {

    private final ConcurrentHashMap<String, Long> lastActionTimestamps = new ConcurrentHashMap<>();
    private final long minIntervalMillis;

    /**
     * Creates a rate limiter with the specified minimum interval.
     *
     * @param minIntervalMillis
     *            Minimum milliseconds between actions
     */
    public RateLimiter(long minIntervalMillis) {
        this.minIntervalMillis = minIntervalMillis;
    }

    /**
     * Checks if an action is allowed for the given player in the given game.
     *
     * <p>
     * Rate limiting is scoped per (player, game) so activity in one game does not
     * throttle the same player in a different game. Uses an atomic {@code compute}
     * to avoid the TOCTOU race that would occur with separate
     * {@code get}/{@code put} calls.
     *
     * @param profileId
     *            Player's profile ID
     * @param gameId
     *            Game ID (limits are per player per game, not global)
     * @return true if action is allowed, false if rate limited
     */
    public boolean allowAction(long profileId, String gameId) {
        String key = profileId + ":" + gameId;
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};
        lastActionTimestamps.compute(key, (k, lastTimestamp) -> {
            if (lastTimestamp == null || (now - lastTimestamp) >= minIntervalMillis) {
                allowed[0] = true;
                return now;
            }
            return lastTimestamp;
        });
        return allowed[0];
    }

    /**
     * Removes a player's rate-limit state for the given game.
     *
     * Clears the (player, game) entry, allowing immediate actions in that game.
     *
     * @param profileId
     *            Player's profile ID
     * @param gameId
     *            Game ID
     */
    public void removePlayer(long profileId, String gameId) {
        lastActionTimestamps.remove(profileId + ":" + gameId);
    }
}
