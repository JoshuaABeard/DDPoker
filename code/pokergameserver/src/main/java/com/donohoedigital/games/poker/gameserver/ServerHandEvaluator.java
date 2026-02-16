/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandScoreConstants;

/**
 * Server-native poker hand evaluator. Ported from HandInfoFaster but works with
 * List&lt;Card&gt; instead of Hand objects, avoiding Swing dependencies.
 *
 * Thread-safe: create new instance per evaluation for performance.
 */
public class ServerHandEvaluator implements HandScoreConstants {
    private static final int NUM_CARDS = 5;
    private static final int NUM_SUITS = 4;

    // Rank tracking
    private final byte[] numRank = new byte[Card.ACE + 1];
    private final byte[] groupings = new byte[NUM_CARDS + 1];
    private final byte[][] topGroupings = new byte[NUM_CARDS + 1][2];

    // Straight tracking
    private byte straightHigh = 0;

    // Flush tracking
    private final byte[] numSuit = new byte[NUM_SUITS];
    private byte biggestSuit = 0;

    // Straight flush tracking
    private final boolean[] exist = new boolean[Card.ACE + 1];

    /**
     * Evaluate a poker hand and return its score.
     *
     * @param holeCards
     *            player's 2 hole cards (can be null/empty)
     * @param communityCards
     *            community cards (can be null/empty)
     * @return hand score (higher is better)
     */
    public int getScore(List<Card> holeCards, List<Card> communityCards) {
        // Combine all cards
        int totalCards = 0;
        Card[] allCards = new Card[7];

        if (holeCards != null) {
            for (Card card : holeCards) {
                if (card != null && !card.isBlank()) {
                    allCards[totalCards++] = card;
                }
            }
        }

        if (communityCards != null) {
            for (Card card : communityCards) {
                if (card != null && !card.isBlank()) {
                    allCards[totalCards++] = card;
                }
            }
        }

        return evaluateHand(allCards, totalCards);
    }

    /**
     * Core hand evaluation logic ported from HandInfoFaster.
     */
    private int evaluateHand(Card[] cards, int size) {
        byte maxHandSize = (byte) Math.min(size, NUM_CARDS);
        boolean straight = false;
        boolean flush = false;

        // Initialize arrays
        for (int r = Card.TWO; r <= Card.ACE; r++) {
            numRank[r] = 0;
        }
        for (int r = 0; r <= NUM_CARDS; r++) {
            groupings[r] = 0;
            topGroupings[r][0] = 0;
            topGroupings[r][1] = 0;
        }
        for (int r = 0; r < NUM_SUITS; r++) {
            numSuit[r] = 0;
        }
        straightHigh = 0;
        biggestSuit = 0;

        // Count ranks and suits
        for (int i = 0; i < size; i++) {
            byte rank = (byte) cards[i].getRank();
            byte suit = (byte) cards[i].getSuit();

            numRank[rank]++;

            groupings[numRank[rank]]++;
            if (numRank[rank] != 0) {
                groupings[numRank[rank] - 1]--;
            }

            if ((++numSuit[suit]) >= NUM_CARDS) {
                flush = true;
            }

            if (numSuit[suit] > numSuit[biggestSuit]) {
                biggestSuit = suit;
            }
        }

        // Check for straight (including wheel A-2-3-4-5)
        byte straightSize = (byte) (numRank[Card.ACE] != 0 ? 1 : 0);

        for (int r = Card.TWO; r <= Card.ACE; r++) {
            if (numRank[r] != 0) {
                if ((++straightSize) >= NUM_CARDS) {
                    straight = true;
                    straightHigh = (byte) r;
                }
            } else {
                straightSize = 0;
            }

            // Track top ranks for each grouping type
            int count = numRank[r];
            if (count != 0) {
                topGroupings[count][1] = topGroupings[count][0];
                topGroupings[count][0] = (byte) r;
            }
        }

        // Evaluate hand type and calculate score
        int score;

        if (straight && flush && isStraightFlush(cards, size)) {
            if (straightHigh == Card.ACE) {
                score = ROYAL_FLUSH * SCORE_BASE;
            } else {
                score = STRAIGHT_FLUSH * SCORE_BASE;
            }
            score += straightHigh * H0;
        } else if (groupings[4] != 0) {
            // Four of a kind
            score = QUADS * SCORE_BASE;
            score += topGroupings[4][0] * H1;
            topGroupings[4][1] = 0; // just in case 2 sets quads
            score += getKickers(1, topGroupings[4], H0);
        } else if (groupings[3] >= 2) {
            // Full house (two sets of trips)
            score = FULL_HOUSE * SCORE_BASE;
            score += topGroupings[3][0] * H1;
            score += topGroupings[3][1] * H0;
        } else if (groupings[3] == 1 && groupings[2] != 0) {
            // Full house (trips + pair)
            score = FULL_HOUSE * SCORE_BASE;
            score += topGroupings[3][0] * H1;
            score += topGroupings[2][0] * H0;
        } else if (flush) {
            score = FLUSH * SCORE_BASE;
            score += getFlushKickers(cards, size, 5, biggestSuit, H4);
        } else if (straight) {
            score = STRAIGHT * SCORE_BASE;
            score += straightHigh * H0;
        } else if (groupings[3] == 1) {
            // Three of a kind
            score = TRIPS * SCORE_BASE;
            score += topGroupings[3][0] * H2;
            score += getKickers(maxHandSize - 3, topGroupings[3], H1);
        } else if (groupings[2] >= 2) {
            // Two pair
            score = TWO_PAIR * SCORE_BASE;
            score += topGroupings[2][0] * H2;
            score += topGroupings[2][1] * H1;
            score += getKickers(maxHandSize - 4, topGroupings[2], H0);
        } else if (groupings[2] == 1) {
            // One pair
            score = PAIR * SCORE_BASE;
            score += topGroupings[2][0] * H3;
            score += getKickers(maxHandSize - 2, topGroupings[2], H2);
        } else {
            // High card
            score = HIGH_CARD * SCORE_BASE;
            score += getKickers(maxHandSize, topGroupings[2], H4);
        }

        return score;
    }

    /**
     * Check if hand has a straight flush.
     */
    private boolean isStraightFlush(Card[] cards, int size) {
        // Initialize
        for (int i = Card.TWO; i <= Card.ACE; i++) {
            exist[i] = false;
        }

        // Mark cards that are in flush suit
        for (int i = 0; i < size; i++) {
            if (cards[i].getSuit() == biggestSuit) {
                exist[cards[i].getRank()] = true;
            }
        }

        // Check for straight in flush suit
        int straightCount = exist[Card.ACE] ? 1 : 0;
        byte high = 0;

        for (int i = Card.TWO; i <= Card.ACE; i++) {
            if (exist[i]) {
                if ((++straightCount) >= NUM_CARDS) {
                    high = (byte) i;
                }
            } else {
                straightCount = 0;
            }
        }

        if (high == 0) {
            return false;
        }

        straightHigh = high;
        return true;
    }

    /**
     * Get kicker values for hand.
     */
    private int getKickers(int numKickers, byte[] notAllowed, int hStart) {
        int i = Card.ACE;
        int value = 0;

        while (numKickers != 0) {
            while (numRank[i] == 0 || i == notAllowed[0] || i == notAllowed[1]) {
                i--;
            }
            numKickers--;
            value += (i * hStart);
            hStart = (hStart >> 4);
            i--;
        }

        return value;
    }

    /**
     * Get suited kicker values for flush.
     */
    private int getFlushKickers(Card[] cards, int size, int numKickers, byte suit, int hStart) {
        // Initialize
        for (int i = Card.TWO; i <= Card.ACE; i++) {
            exist[i] = false;
        }

        // Mark suited cards
        for (int i = 0; i < size; i++) {
            if (cards[i].getSuit() == suit) {
                exist[cards[i].getRank()] = true;
            }
        }

        // Get highest suited cards as kickers
        int i = Card.ACE;
        int value = 0;

        while (numKickers != 0) {
            while (!exist[i]) {
                i--;
            }
            numKickers--;
            value += (i * hStart);
            hStart = (hStart >> 4);
            i--;
        }

        return value;
    }
}
