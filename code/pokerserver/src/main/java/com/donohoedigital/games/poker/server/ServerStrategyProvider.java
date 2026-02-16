/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.ai.StrategyProvider;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side implementation of StrategyProvider that provides AI personality
 * factors without Swing dependencies.
 * <p>
 * Loads strategy values from PlayerType .dat files to provide full personality
 * customization. Falls back to Sklansky-style hand rankings if
 * HandSelectionScheme data is not available.
 */
public class ServerStrategyProvider implements StrategyProvider {

    /** Per-player random strategy modifiers (cached). Key: "strat." + name */
    private final Map<String, Integer> modifiers = new HashMap<>();

    /** Random number generator for creating per-player modifiers */
    private final SecureRandom random = new SecureRandom();

    /** Player identifier for caching modifiers */
    private final String playerId;

    /** Loaded strategy data from PlayerType .dat file */
    private final StrategyData strategyData;

    /**
     * Embedded hand strength data extracted from HandSelectionScheme .dat files.
     * Provides lookup tables for different table sizes without requiring file I/O.
     */
    private static class EmbeddedHandStrength {
        // Parsed hand group for efficient lookup
        private static class HandGroup {
            final String[] hands;
            final int strength;

            HandGroup(String handsStr, int strength) {
                this.hands = handsStr.split(",");
                this.strength = strength;
            }
        }

        // Heads-up (2 players) - from handselection.0994.dat
        private static final String HEADSUP_DATA = "AA-66,AKs-A8s,KQs,KJs,AK-AT|10:"
                + "55,A7s-A3s,KTs-K8s,QJs,QTs,A9-A7,KQ-KT,QJ|9:" + "44,A2s,K7s-K5s,Q9s,Q8s,JTs,J9s,A6-A3,K9-K7,QT|8:"
                + "33,K4s-K2s,Q7s-Q5s,J8s,T9s,A2,K6-K4,Q9,Q8,JT,J9|7:"
                + "22,Q4s-Q2s,J7s-J5s,T8s,T7s,98s,K3,K2,Q7-Q5,J8,T9|6:"
                + "J4s-J2s,T6s,T5s,97s,96s,87s,Q4-Q2,J7-J5,T8,T7,98|5:"
                + "T4s-T2s,95s,86s,85s,76s,75s,J4-J2,T6,T5,97,96,87|4:"
                + "94s-92s,84s,83s,74s,65s,64s,54s,T4-T2,95,86,85,76|3:"
                + "82s,73s,72s,63s,62s,53s,52s,43s,94-92,84,83,75,74,65,64,54|2:"
                + "42s,32s,82,73,72,63,62,53,52,43,42,32|1";

        // Very short-handed (3-4 players) - from handselection.1000.dat
        private static final String VERYSHORT_DATA = "AA-QQ|10:" + "JJ,TT,AKs,AQs,AK,AQ|9:" + "99,88,AJs,AJ|8:"
                + "77-55,ATs,KQs,AT,KQ|7:" + "44-22,A9s,A8s,KJs,A9,A8,KJ|6:" + "A7s,A6s,KTs,QJs,A7,A6,KT,QJ|5:"
                + "A5s-A2s,K9s,QTs,A5-A2,K9,QT|4:" + "K8s,Q9s,JTs,K8,Q9,JT|3:"
                + "K7s,Q8s,J9s,T9s,98s,87s,76s,65s,54s,43s,32s,K7,Q8,J9|2:" + "T9,98,87,76,65,54,43,32|1";

        // Short-handed (5-6 players) - from handselection.0995.dat
        private static final String SHORT_DATA = "AA-QQ|10:" + "JJ,TT,AKs,AQs|9:" + "99,88,AJs,AK,AQ|8:"
                + "77-55,ATs,KQs,AJ|7:" + "44-22,A9s,A8s,KJs,AT,KQ|6:" + "A7s,A6s,KTs,QJs,A9,A8,KJ|5:"
                + "A5s-A2s,K9s,QTs,A7,A6,KT,QJ|4:" + "K8s,Q9s,JTs,A5-A2,K9,QT|3:"
                + "K7s,Q8s,J9s,T9s,98s,87s,76s,65s,54s,43s,32s,K8,Q9,JT|2:" + "K7,Q8,J9,T9,98,87,76,65,54,43,32|1";

        // Full table (7-10 players) - from handselection.0996.dat
        private static final String FULL_DATA = "AA,KK|10:" + "QQ,AKs|9:" + "JJ,AQs,AK|8:" + "TT,99,AJs,KQs,AQ|7:"
                + "88-66,ATs,KJs,AJ,KQ|6:" + "55-22,KTs,QJs,QTs,AT,KJ|5:"
                + "A9s-A2s,JTs,T9s,98s,87s,76s,65s,54s,43s,32s|4:" + "J9s,T8s,97s,86s,75s,64s,53s,42s,KT,QJ|3:"
                + "A9-A2,QT|2:" + "K9s,Q9s,K9,Q9,JT|1";

        // Pre-parsed data for efficient lookup (initialized on first access)
        private static final HandGroup[] HEADSUP_GROUPS = parseData(HEADSUP_DATA);
        private static final HandGroup[] VERYSHORT_GROUPS = parseData(VERYSHORT_DATA);
        private static final HandGroup[] SHORT_GROUPS = parseData(SHORT_DATA);
        private static final HandGroup[] FULL_GROUPS = parseData(FULL_DATA);

        private static HandGroup[] parseData(String data) {
            String[] groups = data.split(":");
            HandGroup[] result = new HandGroup[groups.length];
            for (int i = 0; i < groups.length; i++) {
                String[] parts = groups[i].split("\\|");
                if (parts.length == 2) {
                    result[i] = new HandGroup(parts[0], Integer.parseInt(parts[1]));
                }
            }
            return result;
        }

        /**
         * Get hand strength for given cards and table size.
         *
         * @param rank1
         *            First card rank (2-14, where 14=Ace)
         * @param rank2
         *            Second card rank
         * @param suited
         *            Whether cards are suited
         * @param numPlayers
         *            Number of players at table
         * @return Strength 0.0-1.0, or -1 if not found
         */
        static float getStrength(int rank1, int rank2, boolean suited, int numPlayers) {
            HandGroup[] groups;
            if (numPlayers <= 2) {
                groups = HEADSUP_GROUPS;
            } else if (numPlayers <= 4) {
                groups = VERYSHORT_GROUPS;
            } else if (numPlayers <= 6) {
                groups = SHORT_GROUPS;
            } else {
                groups = FULL_GROUPS;
            }

            return lookupStrength(rank1, rank2, suited, groups);
        }

        private static float lookupStrength(int rank1, int rank2, boolean suited, HandGroup[] groups) {
            // Normalize: higher rank first
            int high = Math.max(rank1, rank2);
            int low = Math.min(rank1, rank2);
            boolean isPair = (high == low);

            // Check each pre-parsed group
            for (HandGroup group : groups) {
                if (group == null)
                    continue;

                // Check each hand notation in this group
                for (String hand : group.hands) {
                    if (matchesHand(high, low, isPair, suited, hand.trim())) {
                        return group.strength / 10.0f; // Convert 1-10 to 0.1-1.0
                    }
                }
            }

            return -1; // Not found
        }

        private static boolean matchesHand(int high, int low, boolean isPair, boolean suited, String notation) {
            // Handle suited indicator
            boolean notationSuited = notation.endsWith("s");
            String baseNotation = notationSuited ? notation.substring(0, notation.length() - 1) : notation;

            // If notation specifies suited/offsuit, must match
            if (notationSuited && !suited)
                return false;
            if (!notationSuited && !notation.contains("-") && notation.length() == 2 && suited && !isPair) {
                // Offsuit notation (e.g., "AK" without 's') - only matches offsuit
                return false;
            }

            // Handle ranges (e.g., "AA-66" or "A9-A2")
            if (baseNotation.contains("-")) {
                String[] range = baseNotation.split("-");
                if (range.length == 2) {
                    return matchesRange(high, low, isPair, suited, range[0], range[1], notationSuited);
                }
            }

            // Handle specific hands (e.g., "AK", "AA", "AKs")
            if (baseNotation.length() >= 2) {
                int notHigh = parseRank(baseNotation.charAt(0));
                int notLow = parseRank(baseNotation.charAt(1));
                return (high == notHigh && low == notLow && (!notation.contains("s") || suited || isPair));
            }

            return false;
        }

        private static boolean matchesRange(int high, int low, boolean isPair, boolean suited, String start, String end,
                boolean notationSuited) {
            if (start.length() < 2 || end.length() < 2)
                return false;

            // Handle pair ranges (e.g., "AA-66")
            if (start.charAt(0) == start.charAt(1) && end.charAt(0) == end.charAt(1)) {
                if (!isPair)
                    return false;
                int rangeHigh = parseRank(start.charAt(0));
                int rangeLow = parseRank(end.charAt(0));
                return high >= rangeLow && high <= rangeHigh;
            }

            // Handle non-pair ranges (e.g., "A9-A2" or "KTs-K8s")
            int startHigh = parseRank(start.charAt(0));
            int startLow = parseRank(start.charAt(1));
            int endHigh = parseRank(end.charAt(0));
            int endLow = parseRank(end.charAt(1));

            // Must match high card
            if (high != startHigh || startHigh != endHigh)
                return false;

            // Check if low card is in range
            boolean inRange = low >= endLow && low <= startLow;

            // Check suited requirement
            if (notationSuited && !suited)
                return false;
            // Offsuit ranges should not match suited hands (except pairs)
            if (!notationSuited && suited && !isPair)
                return false;

            return inRange;
        }

        private static int parseRank(char c) {
            return switch (c) {
                case 'A' -> Card.ACE;
                case 'K' -> Card.KING;
                case 'Q' -> Card.QUEEN;
                case 'J' -> Card.JACK;
                case 'T' -> Card.TEN;
                case '9' -> Card.NINE;
                case '8' -> Card.EIGHT;
                case '7' -> Card.SEVEN;
                case '6' -> Card.SIX;
                case '5' -> Card.FIVE;
                case '4' -> Card.FOUR;
                case '3' -> Card.THREE;
                case '2' -> Card.TWO;
                default -> -1;
            };
        }
    }

    /**
     * Create strategy provider for a specific player using default strategy.
     *
     * @param playerId
     *            unique identifier for this player (for caching modifiers)
     */
    public ServerStrategyProvider(String playerId) {
        this(playerId, StrategyDataLoader.loadDefaultStrategy());
    }

    /**
     * Create strategy provider with specific strategy file.
     *
     * @param playerId
     *            unique identifier for this player
     * @param strategyFilename
     *            PlayerType .dat filename (e.g., "playertype.0991.dat")
     */
    public ServerStrategyProvider(String playerId, String strategyFilename) {
        this(playerId, StrategyDataLoader.loadStrategy(strategyFilename));
    }

    /**
     * Create strategy provider with loaded strategy data.
     *
     * @param playerId
     *            unique identifier for this player
     * @param strategyData
     *            Pre-loaded strategy data
     */
    public ServerStrategyProvider(String playerId, StrategyData strategyData) {
        this.playerId = playerId;
        this.strategyData = strategyData != null ? strategyData : StrategyDataLoader.loadDefaultStrategy();
    }

    @Override
    public float getStratFactor(String name, float min, float max) {
        return getStratFactor(name, null, min, max);
    }

    @Override
    public float getStratFactor(String name, Hand hand, float min, float max) {
        // Get base strategy value (default 50, or hand-specific if hand provided)
        int baseValue = getStratValue(name, hand, 50);

        // Get or create per-player random modifier (-10 to +10)
        // Matches V2Player.getStratFactor() logic (lines 1637-1647)
        String key = "strat." + name;
        Integer mod = modifiers.get(key);
        if (mod == null) {
            mod = random.nextInt(21) - 10; // -10 to +10
            modifiers.put(key, mod);
        }

        // Apply modifier and clamp to 0-100
        // Matches PlayerType.getStratFactor() logic (line 458)
        float clampedValue = Math.min(Math.max((float) baseValue + mod, 0f), 100f);

        // Map to target range
        return min + (max - min) / 100.0f * clampedValue;
    }

    @Override
    public float getHandStrength(Hand pocket) {
        // Use default table size (full table = 9 players)
        return getHandStrength(pocket, 9);
    }

    @Override
    public float getHandStrength(Hand pocket, int numPlayers) {
        if (pocket == null || pocket.size() < 2) {
            return 0.0f;
        }

        // Use embedded hand strength data extracted from HandSelectionScheme .dat
        // files.
        // This provides Doug Donohoe's exact hand rankings for different table sizes
        // without requiring file I/O or desktop framework dependencies.
        Card card1 = pocket.getCard(0);
        Card card2 = pocket.getCard(1);

        int rank1 = card1.getRank();
        int rank2 = card2.getRank();
        boolean suited = (card1.getSuit() == card2.getSuit());

        float strength = EmbeddedHandStrength.getStrength(rank1, rank2, suited, numPlayers);

        // Return 0.0 for hands not found in lookup table (matches original behavior)
        // Original HandSelectionScheme returns 0.0 for unmatched hands
        if (strength < 0) {
            strength = 0.0f;
        }

        return strength;
    }

    /**
     * Get strategy value for a factor name, with optional hand-specific adjustment.
     *
     * @param name
     *            strategy factor name
     * @param hand
     *            optional hole cards for hand-specific factors
     * @param defval
     *            default value if not found
     * @return strategy value (0-100)
     */
    private int getStratValue(String name, Hand hand, int defval) {
        if (hand != null) {
            // Categorize hand and look up hand-specific factor
            // Matches PlayerType.getStratValue(name, hand, defval) logic (lines
            // 478-526)
            String handCategory = categorizeHand(hand);
            String handSpecificName = name + "." + handCategory;

            // Look up hand-specific value from loaded strategy data
            return strategyData.getStrategyFactor(handSpecificName, defval);
        }

        // Look up base value from loaded strategy data
        return strategyData.getStrategyFactor(name, defval);
    }

    /**
     * Categorize a hand for hand-specific strategy factors. Matches
     * PlayerType.getStratValue() logic (lines 484-519).
     *
     * @param hand
     *            the hole cards (2 cards)
     * @return category string (e.g., "big_pair", "suited_ace", "other")
     */
    private String categorizeHand(Hand hand) {
        if (hand.isPair()) {
            int rank = hand.getHighestRank();
            if (rank < 7)
                return "small_pair"; // 22-66
            else if (rank > 10)
                return "big_pair"; // JJ-AA
            else
                return "medium_pair"; // 77-TT
        } else {
            if (hand.getLowestRank() > 9) {
                // Both cards T or higher
                return hand.isSuited() ? "suited_high_cards" : "unsuited_high_cards";
            } else if (hand.getHighestRank() == Card.ACE) {
                // Ace with kicker <= 9
                return hand.isSuited() ? "suited_ace" : "unsuited_ace";
            } else if (hand.isConnectors(Card.TWO, Card.TEN)) {
                // Connected cards (not high cards, not aces)
                return hand.isSuited() ? "suited_connectors" : "unsuited_connectors";
            } else {
                return "other";
            }
        }
    }

    /**
     * Calculate hand strength using embedded Sklansky-style hand rankings. Returns
     * value 0.0 (worst) to 1.0 (best).
     * <p>
     * Uses pre-computed rankings embedded in code for server simplicity, avoiding
     * HandSelectionScheme file loading dependencies.
     *
     * @param pocket
     *            the 2 hole cards
     * @return hand strength 0.0 - 1.0
     */
    private float calculateSimplifiedHandStrength(Hand pocket) {
        Card card1 = pocket.getCard(0);
        Card card2 = pocket.getCard(1);

        int rank1 = card1.getRank();
        int rank2 = card2.getRank();
        int highRank = Math.max(rank1, rank2);
        int lowRank = Math.min(rank1, rank2);

        boolean isPair = (rank1 == rank2);
        boolean isSuited = (card1.getSuit() == card2.getSuit());
        boolean isConnected = Math.abs(rank1 - rank2) == 1 || (highRank == Card.ACE && lowRank == Card.KING);

        // Sklansky Group 1 (Premium): AA, KK, QQ, JJ, AKs (0.90 - 1.0)
        if (isPair && highRank >= Card.JACK) {
            return 0.90f + (highRank - Card.JACK) * 0.025f; // JJ=0.90, QQ=0.925, KK=0.95,
                                                            // AA=0.975
        }
        if (highRank == Card.ACE && lowRank == Card.KING && isSuited) {
            return 0.95f;
        }

        // Sklansky Group 2: TT, AKo, AQs, AJs, KQs (0.80 - 0.89)
        if (isPair && highRank == Card.TEN)
            return 0.85f;
        if (highRank == Card.ACE && lowRank == Card.KING && !isSuited)
            return 0.88f;
        if (highRank == Card.ACE && lowRank >= Card.JACK && isSuited) {
            return 0.82f + (lowRank - Card.JACK) * 0.02f;
        }
        if (highRank == Card.KING && lowRank == Card.QUEEN && isSuited)
            return 0.80f;

        // Sklansky Group 3: 99, 88, AQo, AJo, KQo, KJs, QJs, JTs (0.70 - 0.79)
        if (isPair && highRank >= 8 && highRank <= 9) {
            return 0.72f + (highRank - 8) * 0.03f;
        }
        if (highRank == Card.ACE && lowRank >= Card.JACK && !isSuited) {
            return 0.74f + (lowRank - Card.JACK) * 0.02f;
        }
        if (highRank == Card.KING && lowRank == Card.QUEEN && !isSuited)
            return 0.72f;
        if ((highRank == Card.KING && lowRank == Card.JACK && isSuited)
                || (highRank == Card.QUEEN && lowRank == Card.JACK && isSuited)
                || (highRank == Card.JACK && lowRank == Card.TEN && isSuited)) {
            return 0.70f + (14 - highRank) * 0.02f;
        }

        // Sklansky Group 4: 77, 66, ATo, KJo, QJo, JTo, A9s-A2s, KTs, QTs (0.55 -
        // 0.69)
        if (isPair && highRank >= 6 && highRank <= 7) {
            return 0.60f + (highRank - 6) * 0.04f;
        }
        if (highRank == Card.ACE && isSuited) {
            return 0.56f + (lowRank - 2) * 0.01f; // A2s-A9s
        }
        if (highRank == Card.ACE && lowRank == Card.TEN)
            return 0.62f;
        if ((highRank == Card.KING || highRank == Card.QUEEN) && lowRank == Card.TEN && isSuited) {
            return 0.58f;
        }

        // Sklansky Group 5-8: Remaining playable hands (0.20 - 0.54)
        if (isPair) {
            return 0.30f + (highRank - 2) * 0.05f; // 22-55
        }
        if (highRank == Card.ACE) {
            return 0.35f + (lowRank - 2) * 0.015f; // Ace with any kicker
        }
        if (isSuited && isConnected) {
            return 0.40f + (highRank - 2) * 0.015f; // Suited connectors
        }
        if (isSuited) {
            return 0.25f + ((highRank + lowRank) - 4) * 0.01f; // Other suited
        }
        if (highRank >= Card.KING && lowRank >= Card.JACK) {
            return 0.45f; // KQ, KJ, QJ offsuit
        }
        if (isConnected) {
            return 0.30f + (highRank - 2) * 0.01f; // Offsuit connectors
        }

        // Trash hands (0.0 - 0.19)
        return 0.05f + ((highRank + lowRank) - 4) * 0.005f;
    }
}
