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
package com.donohoedigital.games.poker.gameserver;

import java.util.List;
import java.util.Map;

/**
 * Result of advisor computation for a player's hand.
 *
 * @param handRank
 *            0-9 (HIGH_CARD=0 to ROYAL_FLUSH=9)
 * @param handDescription
 *            e.g. "One Pair, Aces" or null if fewer than 5 cards
 * @param equity
 *            0-100 win percentage from Monte Carlo simulation
 * @param potOdds
 *            0-100 pot odds percentage
 * @param recommendation
 *            action recommendation text
 * @param startingHandCategory
 *            "premium", "strong", "playable", "marginal", "fold" or null
 *            post-flop
 * @param startingHandNotation
 *            e.g. "AKs", "AA", "72o" or null post-flop
 * @param improvementOdds
 *            map of hand type name to improvement probability (0-100), null on
 *            preflop or river; only entries with probability &gt; 0 are
 *            included
 * @param handPotential
 *            hand potential data (positive/negative percent and hand type
 *            breakdown), null on preflop or river
 */
public record AdvisorResult(int handRank, String handDescription, double equity, double potOdds, String recommendation,
        String startingHandCategory, String startingHandNotation, Map<String, Double> improvementOdds,
        HandPotentialResult handPotential) {

    /**
     * Hand potential data for the current position.
     *
     * @param positivePercent
     *            % of remaining boards where the player's hand type improves
     * @param negativePercent
     *            % of remaining boards where the player's hand type worsens
     * @param handTypeBreakdown
     *            list of hand type entries showing how often each type appears
     *            across remaining boards; only entries with percent &gt; 0 are
     *            included
     */
    public record HandPotentialResult(double positivePercent, double negativePercent,
            List<HandTypeEntry> handTypeBreakdown) {

        /**
         * A single hand type and its probability across remaining boards.
         *
         * @param type
         *            hand type name (e.g. "ONE_PAIR", "FLUSH")
         * @param percent
         *            probability 0-100
         */
        public record HandTypeEntry(String type, double percent) {
        }
    }
}
