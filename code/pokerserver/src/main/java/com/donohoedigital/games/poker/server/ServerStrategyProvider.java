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

        // TODO: Load HandSelectionScheme data for accurate strength calculation
        // For now, use simplified Sklansky-style hand ranking

        // Get simplified hand strength (0.0 - 1.0)
        float baseStrength = calculateSimplifiedHandStrength(pocket);

        // Adjust for table size (tighter at full tables, looser at short tables)
        // Full table (7-10): use base strength
        // Short table (5-6): boost by 10%
        // Very short (3-4): boost by 20%
        // Heads-up (2): boost by 30%
        float adjustment = 0.0f;
        if (numPlayers <= 2) {
            adjustment = 0.30f;
        } else if (numPlayers <= 4) {
            adjustment = 0.20f;
        } else if (numPlayers <= 6) {
            adjustment = 0.10f;
        }

        return Math.min(baseStrength * (1.0f + adjustment), 1.0f);
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
     * Calculate simplified hand strength using Sklansky-style hand rankings.
     * Returns value 0.0 (worst) to 1.0 (best).
     * <p>
     * This is a placeholder until HandSelectionScheme data can be loaded.
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
