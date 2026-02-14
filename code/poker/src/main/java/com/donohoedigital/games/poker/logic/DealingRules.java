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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.games.poker.core.state.BettingRound;

/**
 * Texas Hold'em dealing and card visibility rules extracted from
 * DealCommunity.java. Contains pure business logic for card dealing decisions
 * with no UI dependencies. Part of Wave 2 testability refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>Draw decision based on player count and rabbit hunt option</li>
 * <li>Card visibility determination for specific card indexes</li>
 * <li>Round-to-card mapping for community cards</li>
 * </ul>
 */
public class DealingRules {

    // Utility class - no instantiation
    private DealingRules() {
    }

    /**
     * Value object representing the decision about whether to draw community cards.
     */
    public static class DrawDecision {
        private final boolean drawnNormal;
        private final boolean drawn;

        public DrawDecision(boolean drawnNormal, boolean drawn) {
            this.drawnNormal = drawnNormal;
            this.drawn = drawn;
        }

        public boolean isDrawnNormal() {
            return drawnNormal;
        }

        public boolean isDrawn() {
            return drawn;
        }
    }

    /**
     * Determine whether community cards should be drawn.
     *
     * <p>
     * Extracted from DealCommunity.java lines 84-89.
     *
     * <p>
     * Cards are normally drawn when more than one player has cards. Rabbit hunt
     * option allows showing cards even with one player (to see what would have
     * come).
     *
     * @param numWithCards
     *            number of players with cards
     * @param rabbitHuntEnabled
     *            true if rabbit hunt cheat option is enabled
     * @return DrawDecision indicating whether cards are drawn normally and whether
     *         drawn at all
     */
    public static DrawDecision determineDrawDecision(int numWithCards, boolean rabbitHuntEnabled) {
        boolean drawnNormal = numWithCards > 1;
        boolean drawn = rabbitHuntEnabled || drawnNormal;
        return new DrawDecision(drawnNormal, drawn);
    }

    /**
     * Determine if a specific community card should be visible.
     *
     * <p>
     * Extracted from DealCommunity.syncCards() lines 288-301.
     *
     * <p>
     * Cards become visible when they're dealt in normal play or when all cards have
     * been dealt in all-in showdown situations.
     *
     * @param cardIndex
     *            the card index (0-4 for flop1, flop2, flop3, turn, river)
     * @param currentRound
     *            current round (FLOP, TURN, RIVER, SHOWDOWN)
     * @param lastBettingRound
     *            last betting round that occurred
     * @param drawnNormal
     *            true if cards drawn normally (>1 player)
     * @param drawn
     *            true if cards drawn at all (including rabbit hunt)
     * @return true if card should be visible
     */
    public static boolean isCardVisible(int cardIndex, int currentRound, int lastBettingRound, boolean drawnNormal,
            boolean drawn) {
        int cardRound = getCardRound(cardIndex);
        boolean cardDealt = lastBettingRound >= cardRound;
        return drawnNormal || cardDealt || drawn;
    }

    /**
     * Get the round when a specific card is dealt.
     *
     * <p>
     * Maps card index to the round when that card appears.
     *
     * @param cardIndex
     *            the card index (0-4)
     * @return the round constant (FLOP, TURN, or RIVER)
     */
    public static int getCardRound(int cardIndex) {
        if (cardIndex <= 2) {
            return BettingRound.FLOP.toLegacy();
        } else if (cardIndex == 3) {
            return BettingRound.TURN.toLegacy();
        } else {
            return BettingRound.RIVER.toLegacy();
        }
    }

    /**
     * Get the number of cards dealt in a specific round.
     *
     * <p>
     * Flop deals 3 cards, turn and river each deal 1 card.
     *
     * @param round
     *            the round (FLOP, TURN, or RIVER)
     * @return number of cards dealt in that round
     */
    public static int getCardsDealtInRound(int round) {
        if (round == BettingRound.FLOP.toLegacy()) {
            return 3;
        } else if (round == BettingRound.TURN.toLegacy()) {
            return 1;
        } else if (round == BettingRound.RIVER.toLegacy()) {
            return 1;
        }
        return 0;
    }

    /**
     * Get total number of community cards visible by end of a round.
     *
     * <p>
     * After flop: 3 cards, after turn: 4 cards, after river: 5 cards.
     *
     * @param round
     *            the round (FLOP, TURN, RIVER, or SHOWDOWN)
     * @return total cards visible by end of round
     */
    public static int getTotalCardsVisibleByRound(int round) {
        if (round >= BettingRound.SHOWDOWN.toLegacy()) {
            return 5;
        } else if (round >= BettingRound.RIVER.toLegacy()) {
            return 5;
        } else if (round >= BettingRound.TURN.toLegacy()) {
            return 4;
        } else if (round >= BettingRound.FLOP.toLegacy()) {
            return 3;
        }
        return 0;
    }
}
