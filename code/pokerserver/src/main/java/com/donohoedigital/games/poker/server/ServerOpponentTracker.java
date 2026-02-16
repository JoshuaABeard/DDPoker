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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.ai.V2OpponentModel;
import com.donohoedigital.games.poker.HandAction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks opponent behavior statistics across hands for AI opponent modeling.
 * <p>
 * Accumulates per-player statistics for pre-flop and post-flop actions, chip
 * counts, and betting patterns. Provides V2OpponentModel instances with real
 * data for V2 AI decision-making.
 */
public class ServerOpponentTracker {

    private final Map<Integer, MutableOpponentStats> playerStats = new ConcurrentHashMap<>();

    /**
     * Record the start of a new hand for a player.
     *
     * @param player
     *            Player starting hand
     * @param chipCount
     *            Player's chip count at hand start
     */
    public void onHandStart(GamePlayerInfo player, int chipCount) {
        MutableOpponentStats stats = playerStats.computeIfAbsent(player.getID(), k -> new MutableOpponentStats());
        stats.onHandStart(chipCount);
    }

    /**
     * Record a player action during a hand.
     *
     * @param player
     *            Player taking action
     * @param action
     *            Action type (HandAction.ACTION_*)
     * @param amount
     *            Amount bet/raised/called
     * @param round
     *            Betting round (0=preflop, 1=flop, 2=turn, 3=river)
     * @param positionCategory
     *            Position category (0=blind, 1=early, 2=middle, 3=late)
     */
    public void onPlayerAction(GamePlayerInfo player, int action, int amount, int round, int positionCategory) {
        MutableOpponentStats stats = playerStats.get(player.getID());
        if (stats != null) {
            stats.recordAction(action, amount, round, positionCategory);
        }
    }

    /**
     * Record that a player made an overbet (bet > pot size).
     *
     * @param player
     *            Player who overbetted
     */
    public void onOverbet(GamePlayerInfo player) {
        MutableOpponentStats stats = playerStats.get(player.getID());
        if (stats != null) {
            stats.recordOverbet();
        }
    }

    /**
     * Record that a player bet then folded to a raise.
     *
     * @param player
     *            Player who bet then folded
     */
    public void onBetFold(GamePlayerInfo player) {
        MutableOpponentStats stats = playerStats.get(player.getID());
        if (stats != null) {
            stats.recordBetFold();
        }
    }

    /**
     * Record the end of a hand for a player.
     *
     * @param player
     *            Player ending hand
     */
    public void onHandEnd(GamePlayerInfo player) {
        MutableOpponentStats stats = playerStats.get(player.getID());
        if (stats != null) {
            stats.onHandEnd();
        }
    }

    /**
     * Get opponent model for a player.
     *
     * @param playerId
     *            Player ID
     * @return Opponent model with accumulated statistics
     */
    public V2OpponentModel getModel(int playerId) {
        MutableOpponentStats stats = playerStats.get(playerId);
        return stats != null ? stats : new MutableOpponentStats();
    }

    /**
     * Get chip count at start of current/last hand for a player.
     *
     * @param playerId
     *            Player ID
     * @return Chip count at hand start, or 0 if not tracked
     */
    public int getChipCountAtStart(int playerId) {
        MutableOpponentStats stats = playerStats.get(playerId);
        return stats != null ? stats.getChipCountAtStart() : 0;
    }

    /**
     * Calculate hands before a player posts big blind.
     *
     * @param playerSeat
     *            Player's seat number
     * @param buttonSeat
     *            Current button seat
     * @param numSeats
     *            Total seats at table
     * @return Number of hands until player is big blind
     */
    public int getHandsBeforeBigBlind(int playerSeat, int buttonSeat, int numSeats) {
        // BB is 2 seats after button (button -> SB -> BB)
        int bbSeat = (buttonSeat + 2) % numSeats;

        // Calculate distance from player to BB position
        int distance = (bbSeat - playerSeat + numSeats) % numSeats;

        return distance;
    }

    /**
     * Mutable opponent statistics that implements V2OpponentModel. Accumulates
     * statistics across hands and provides frequencies as floats (0.0-1.0).
     */
    private static class MutableOpponentStats implements V2OpponentModel {

        // Hand tracking
        private int handsPlayed = 0;
        private int chipCountAtStart = 0;

        // Pre-flop stats (per position category 0-3)
        private final int[] preFlopRaises = new int[4];
        private final int[] preFlopCalls = new int[4];
        private final int[] preFlopLimps = new int[4];
        private final int[] preFlopFoldsUnraised = new int[4];
        private final int[] preFlopHandsByPosition = new int[4];

        // Post-flop stats (per round 1-3: flop, turn, river)
        private final int[] postFlopActions = new int[3]; // Bet/raise
        private final int[] postFlopChecks = new int[3]; // Check/call
        private final int[] postFlopCheckFolds = new int[3];
        private final int[] postFlopOpens = new int[3]; // First to act and bet
        private final int[] postFlopRaises = new int[3];
        private final int[] postFlopRounds = new int[3];

        // Special patterns
        private int overbets = 0;
        private int betFolds = 0;
        private int totalBets = 0;

        // Transient hand state
        private boolean inHand = false;
        private boolean actedPreFlop = false;

        void onHandStart(int chipCount) {
            this.chipCountAtStart = chipCount;
            this.inHand = true;
            this.actedPreFlop = false;
        }

        void recordAction(int action, int amount, int round, int positionCategory) {
            if (!inHand) {
                return;
            }

            if (round == 0) {
                // Pre-flop
                if (!actedPreFlop) {
                    actedPreFlop = true;
                    preFlopHandsByPosition[positionCategory]++;
                }

                switch (action) {
                    case HandAction.ACTION_RAISE :
                        preFlopRaises[positionCategory]++;
                        break;
                    case HandAction.ACTION_CALL :
                        // Distinguish limp (call BB) from call raise
                        // Simplified: treat all calls as limps for now
                        preFlopLimps[positionCategory]++;
                        preFlopCalls[positionCategory]++;
                        break;
                    case HandAction.ACTION_FOLD :
                        preFlopFoldsUnraised[positionCategory]++;
                        break;
                }
            } else if (round >= 1 && round <= 3) {
                // Post-flop (flop=1, turn=2, river=3)
                int idx = round - 1;
                postFlopRounds[idx]++;

                switch (action) {
                    case HandAction.ACTION_BET :
                        postFlopActions[idx]++;
                        postFlopOpens[idx]++;
                        totalBets++;
                        break;
                    case HandAction.ACTION_RAISE :
                        postFlopActions[idx]++;
                        postFlopRaises[idx]++;
                        totalBets++;
                        break;
                    case HandAction.ACTION_CHECK :
                        postFlopChecks[idx]++;
                        break;
                    case HandAction.ACTION_FOLD :
                        // Check-fold tracked separately via previous check
                        if (postFlopChecks[idx] > 0) {
                            postFlopCheckFolds[idx]++;
                        }
                        break;
                }
            }
        }

        void recordOverbet() {
            overbets++;
        }

        void recordBetFold() {
            betFolds++;
        }

        void onHandEnd() {
            if (inHand) {
                handsPlayed++;
                inHand = false;
            }
        }

        int getChipCountAtStart() {
            return chipCountAtStart;
        }

        // === V2OpponentModel Implementation ===

        @Override
        public float getPreFlopTightness(int position, float defVal) {
            if (position < 0 || position >= 4) {
                return defVal;
            }
            int handsAtPos = preFlopHandsByPosition[position];
            if (handsAtPos == 0) {
                return defVal;
            }
            int voluntaryHands = preFlopRaises[position] + preFlopCalls[position];
            // Tightness = 1.0 - VPIP (lower VPIP = tighter)
            float vpip = (float) voluntaryHands / handsAtPos;
            return 1.0f - vpip;
        }

        @Override
        public float getPreFlopAggression(int position, float defVal) {
            if (position < 0 || position >= 4) {
                return defVal;
            }
            int voluntaryHands = preFlopRaises[position] + preFlopCalls[position];
            if (voluntaryHands == 0) {
                return defVal;
            }
            // Aggression = raise / (raise + call)
            return (float) preFlopRaises[position] / voluntaryHands;
        }

        @Override
        public float getActPostFlop(int round, float defVal) {
            int idx = round - 1;
            if (idx < 0 || idx >= 3) {
                return defVal;
            }
            if (postFlopRounds[idx] == 0) {
                return defVal;
            }
            return (float) postFlopActions[idx] / postFlopRounds[idx];
        }

        @Override
        public float getCheckFoldPostFlop(int round, float defVal) {
            int idx = round - 1;
            if (idx < 0 || idx >= 3) {
                return defVal;
            }
            int checks = postFlopChecks[idx];
            if (checks == 0) {
                return defVal;
            }
            return (float) postFlopCheckFolds[idx] / checks;
        }

        @Override
        public float getOpenPostFlop(int round, float defVal) {
            int idx = round - 1;
            if (idx < 0 || idx >= 3) {
                return defVal;
            }
            if (postFlopRounds[idx] == 0) {
                return defVal;
            }
            return (float) postFlopOpens[idx] / postFlopRounds[idx];
        }

        @Override
        public float getRaisePostFlop(int round, float defVal) {
            int idx = round - 1;
            if (idx < 0 || idx >= 3) {
                return defVal;
            }
            if (postFlopRounds[idx] == 0) {
                return defVal;
            }
            return (float) postFlopRaises[idx] / postFlopRounds[idx];
        }

        @Override
        public int getHandsPlayed() {
            return handsPlayed;
        }

        @Override
        public float getHandsPaidPercent(float defVal) {
            if (handsPlayed == 0) {
                return defVal;
            }
            // Sum all voluntary pre-flop actions across positions
            int paidHands = 0;
            for (int i = 0; i < 4; i++) {
                paidHands += preFlopRaises[i] + preFlopCalls[i];
            }
            return (float) paidHands / handsPlayed;
        }

        @Override
        public float getHandsLimpedPercent(float defVal) {
            if (handsPlayed == 0) {
                return defVal;
            }
            int limpedHands = 0;
            for (int i = 0; i < 4; i++) {
                limpedHands += preFlopLimps[i];
            }
            return (float) limpedHands / handsPlayed;
        }

        @Override
        public float getHandsFoldedUnraisedPercent(float defVal) {
            if (handsPlayed == 0) {
                return defVal;
            }
            int foldedHands = 0;
            for (int i = 0; i < 4; i++) {
                foldedHands += preFlopFoldsUnraised[i];
            }
            return (float) foldedHands / handsPlayed;
        }

        @Override
        public float getOverbetFrequency(float defVal) {
            if (handsPlayed == 0) {
                return defVal;
            }
            return (float) overbets / handsPlayed;
        }

        @Override
        public float getBetFoldFrequency(float defVal) {
            if (handsPlayed == 0) {
                return defVal;
            }
            return (float) betFolds / handsPlayed;
        }

        @Override
        public float getHandsRaisedPreFlopPercent(float defVal) {
            if (handsPlayed == 0) {
                return defVal;
            }
            int raisedHands = 0;
            for (int i = 0; i < 4; i++) {
                raisedHands += preFlopRaises[i];
            }
            return (float) raisedHands / handsPlayed;
        }

        private boolean overbetPotPostFlop = false;

        @Override
        public boolean isOverbetPotPostFlop() {
            return overbetPotPostFlop;
        }

        @Override
        public void setOverbetPotPostFlop(boolean value) {
            this.overbetPotPostFlop = value;
        }
    }
}
