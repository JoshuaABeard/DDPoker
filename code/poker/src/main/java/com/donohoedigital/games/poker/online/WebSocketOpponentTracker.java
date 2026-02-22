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
package com.donohoedigital.games.poker.online;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks per-player pre-flop statistics from observed PLAYER_ACTED events in
 * WebSocket mode. Used by {@link WebSocketTournamentDirector} to populate
 * {@link com.donohoedigital.games.poker.dashboard.DashboardPlayerInfo} with
 * useful style data when the server-side {@code OpponentModel} is unavailable
 * on the client.
 */
public class WebSocketOpponentTracker {

    // Running totals per player
    private final Map<Integer, PlayerStats> stats = new HashMap<>();

    // Per-hand tracking, cleared on each new hand
    private final Map<Integer, Boolean> handFolded = new HashMap<>();
    private final Map<Integer, Boolean> handRaised = new HashMap<>();
    private final Set<Integer> handActors = new HashSet<>();

    /** Call at start of each hand (HAND_STARTED). */
    public void onHandStart() {
        handFolded.clear();
        handRaised.clear();
        handActors.clear();
    }

    /**
     * Record a voluntary pre-flop action (not blinds/antes). Actions: FOLD, CALL,
     * CHECK, RAISE, BET
     */
    public void onPreFlopAction(int playerId, String action) {
        handActors.add(playerId);
        if ("FOLD".equalsIgnoreCase(action)) {
            handFolded.put(playerId, true);
        } else if ("RAISE".equalsIgnoreCase(action) || "BET".equalsIgnoreCase(action)) {
            handRaised.put(playerId, true);
        }
        // CALL / CHECK = passive non-fold, no flag needed
    }

    /** Call at end of each hand (HAND_COMPLETE) to commit per-hand stats. */
    public void onHandComplete() {
        for (int playerId : handActors) {
            PlayerStats s = stats.computeIfAbsent(playerId, k -> new PlayerStats());
            s.handsActed++;
            if (Boolean.TRUE.equals(handFolded.get(playerId))) {
                s.foldedPreFlop++;
            } else if (Boolean.TRUE.equals(handRaised.get(playerId))) {
                s.raisedPreFlop++;
            } else {
                s.calledPreFlop++;
            }
        }
    }

    /**
     * Tightness: fold rate. Returns Float.NaN if no data yet. &gt; 0.6 = tight,
     * &lt; 0.4 = loose (matches DashboardPlayerInfo thresholds).
     */
    public float getTightness(int playerId) {
        PlayerStats s = stats.get(playerId);
        if (s == null || s.handsActed == 0)
            return Float.NaN;
        return (float) s.foldedPreFlop / s.handsActed;
    }

    /**
     * Aggression: raise rate among non-folds. Returns Float.NaN if no data yet.
     * &gt; 0.6 = aggressive, &lt; 0.4 = passive (matches DashboardPlayerInfo
     * thresholds).
     */
    public float getAggression(int playerId) {
        PlayerStats s = stats.get(playerId);
        if (s == null || s.handsActed == 0)
            return Float.NaN;
        int nonFolds = s.raisedPreFlop + s.calledPreFlop;
        if (nonFolds == 0)
            return Float.NaN;
        return (float) s.raisedPreFlop / nonFolds;
    }

    private static class PlayerStats {
        int handsActed, foldedPreFlop, raisedPreFlop, calledPreFlop;
    }
}
