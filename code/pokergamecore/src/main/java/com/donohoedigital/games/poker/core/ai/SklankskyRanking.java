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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandSorted;

import java.util.ArrayList;

/**
 * Sklansky starting hand ranking system for Texas Hold'em.
 * <p>
 * Provides classification of pre-flop hands into 8 groups based on David
 * Sklansky's system from "Tournament Poker for Advanced Players". Lower rank =
 * stronger hand.
 * <p>
 * Extracted from {@code HoldemExpert} to be Swing-free for use in pure AI
 * algorithms.
 *
 * @see com.donohoedigital.games.poker.HoldemExpert
 */
public class SklankskyRanking {

    /** Multiplier for group numbers */
    public static final int MULT = 100;

    /** Maximum rank value for Group 1 (AA, KK, QQ, JJ, AKs) */
    public static final int MAXGROUP1 = (MULT * 2) - 1;

    /** Maximum rank value for Group 2 (TT, AQs, AJs, KQs, AKo) */
    public static final int MAXGROUP2 = (MULT * 3) - 1;

    /** Maximum rank value for Group 3 (99, JTs, QJs, KJs, ATs, AQ) */
    public static final int MAXGROUP3 = (MULT * 4) - 1;

    /** Maximum rank value for Group 4 (T9s, KQ, 88, QTs, 98s, J9s, AJ, KTs) */
    public static final int MAXGROUP4 = (MULT * 5) - 1;

    /**
     * Maximum rank value for Group 5 (77, 87s, Q9s, T8s, KJ, QJ, JT, 76s, 97s, AXs,
     * 65s)
     */
    public static final int MAXGROUP5 = (MULT * 6) - 1;

    /**
     * Maximum rank value for Group 6 (66, AT, 55, 86s, KT, QT, 54s, K9s, J8s, 75s)
     */
    public static final int MAXGROUP6 = (MULT * 7) - 1;

    /**
     * Maximum rank value for Group 7 (44, J9, 43s, T9, 33, 98, 64s, 22, KXs, T7s,
     * Q8s)
     */
    public static final int MAXGROUP7 = (MULT * 8) - 1;

    /**
     * Maximum rank value for Group 8 (87, 53s, A9, Q9, 76, 42s, 32s, 96s, 85s, J8,
     * J7s, 65, 54, 74s, K9, T8)
     */
    public static final int MAXGROUP8 = (MULT * 9) - 1;

    // Specific hands for reference
    public static final HandSorted AA = new HandSorted(Card.CLUBS_A, Card.SPADES_A);
    public static final HandSorted KK = new HandSorted(Card.CLUBS_K, Card.SPADES_K);
    public static final HandSorted QQ = new HandSorted(Card.CLUBS_Q, Card.SPADES_Q);
    public static final HandSorted JJ = new HandSorted(Card.CLUBS_J, Card.SPADES_J);
    public static final HandSorted TT = new HandSorted(Card.CLUBS_T, Card.SPADES_T);
    public static final HandSorted AKs = new HandSorted(Card.CLUBS_A, Card.CLUBS_K);
    public static final HandSorted AQs = new HandSorted(Card.CLUBS_A, Card.CLUBS_Q);
    public static final HandSorted AJs = new HandSorted(Card.CLUBS_A, Card.CLUBS_J);
    public static final HandSorted KQs = new HandSorted(Card.CLUBS_K, Card.CLUBS_Q);
    public static final HandSorted AKo = new HandSorted(Card.CLUBS_A, Card.SPADES_K);
    public static final HandSorted AQo = new HandSorted(Card.CLUBS_A, Card.SPADES_Q);
    public static final HandSorted KQo = new HandSorted(Card.CLUBS_K, Card.SPADES_Q);

    // Groups (initialized in static block)
    private static final ArrayList<HandSorted>[] GROUPS = new ArrayList[8];

    static {
        ArrayList<HandSorted> aGROUP1 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP2 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP3 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP4 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP5 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP6 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP7 = new ArrayList<>();
        ArrayList<HandSorted> aGROUP8 = new ArrayList<>();

        // Group 1: AA, KK, QQ, JJ, AKs
        aGROUP1.add(AA);
        aGROUP1.add(KK);
        aGROUP1.add(QQ);
        aGROUP1.add(JJ);
        aGROUP1.add(AKs);

        // Group 2: TT, AQs, AJs, KQs, AK
        aGROUP2.add(TT);
        aGROUP2.add(AQs);
        aGROUP2.add(AJs);
        aGROUP2.add(KQs);
        aGROUP2.add(AKo);

        // Group 3: 99, JTs, QJs, KJs, ATs, AQ
        aGROUP3.add(new HandSorted(Card.CLUBS_9, Card.SPADES_9));
        aGROUP3.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_T));
        aGROUP3.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_J));
        aGROUP3.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_J));
        aGROUP3.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_T));
        aGROUP3.add(AQo);

        // Group 4: T9s, KQ, 88, QTs, 98s, J9s, AJ, KTs
        aGROUP4.add(new HandSorted(Card.CLUBS_T, Card.CLUBS_9));
        aGROUP4.add(KQo);
        aGROUP4.add(new HandSorted(Card.CLUBS_8, Card.SPADES_8));
        aGROUP4.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_T));
        aGROUP4.add(new HandSorted(Card.CLUBS_9, Card.CLUBS_8));
        aGROUP4.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_9));
        aGROUP4.add(new HandSorted(Card.CLUBS_A, Card.SPADES_J));
        aGROUP4.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_T));

        // Group 5: 77, 87s, Q9s, T8s, KJ, QJ, JT, 76s, 97s, AXs (9-2), 65s
        aGROUP5.add(new HandSorted(Card.CLUBS_7, Card.SPADES_7));
        aGROUP5.add(new HandSorted(Card.CLUBS_8, Card.CLUBS_7));
        aGROUP5.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_9));
        aGROUP5.add(new HandSorted(Card.CLUBS_T, Card.CLUBS_8));
        aGROUP5.add(new HandSorted(Card.CLUBS_K, Card.SPADES_J));
        aGROUP5.add(new HandSorted(Card.CLUBS_Q, Card.SPADES_J));
        aGROUP5.add(new HandSorted(Card.CLUBS_J, Card.SPADES_T));
        aGROUP5.add(new HandSorted(Card.CLUBS_7, Card.CLUBS_6));
        aGROUP5.add(new HandSorted(Card.CLUBS_9, Card.CLUBS_7));
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_9)); // AXs 9
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_8)); // AXs 8
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_7)); // AXs 7
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_6)); // AXs 6
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_5)); // AXs 5
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_4)); // AXs 4
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_3)); // AXs 3
        aGROUP5.add(new HandSorted(Card.CLUBS_A, Card.CLUBS_2)); // AXs 2
        aGROUP5.add(new HandSorted(Card.CLUBS_6, Card.CLUBS_5));

        // Group 6: 66, AT, 55, 86s, KT, QT, 54s, K9s, J8s, 75s
        aGROUP6.add(new HandSorted(Card.CLUBS_6, Card.SPADES_6));
        aGROUP6.add(new HandSorted(Card.CLUBS_A, Card.SPADES_T));
        aGROUP6.add(new HandSorted(Card.CLUBS_5, Card.SPADES_5));
        aGROUP6.add(new HandSorted(Card.CLUBS_8, Card.CLUBS_6));
        aGROUP6.add(new HandSorted(Card.CLUBS_K, Card.SPADES_T));
        aGROUP6.add(new HandSorted(Card.CLUBS_Q, Card.SPADES_T));
        aGROUP6.add(new HandSorted(Card.CLUBS_5, Card.CLUBS_4));
        aGROUP6.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_9));
        aGROUP6.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_8));
        aGROUP6.add(new HandSorted(Card.CLUBS_7, Card.CLUBS_5));

        // Group 7: 44, J9, 43s, T9, 33, 98, 64s, 22, KXs (8-2), T7s, Q8s
        aGROUP7.add(new HandSorted(Card.CLUBS_4, Card.SPADES_4));
        aGROUP7.add(new HandSorted(Card.CLUBS_J, Card.SPADES_9));
        aGROUP7.add(new HandSorted(Card.CLUBS_4, Card.CLUBS_3));
        aGROUP7.add(new HandSorted(Card.CLUBS_T, Card.SPADES_9));
        aGROUP7.add(new HandSorted(Card.CLUBS_3, Card.SPADES_3));
        aGROUP7.add(new HandSorted(Card.CLUBS_9, Card.SPADES_8));
        aGROUP7.add(new HandSorted(Card.CLUBS_6, Card.CLUBS_4));
        aGROUP7.add(new HandSorted(Card.CLUBS_2, Card.SPADES_2));
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_8)); // KXs 8
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_7)); // KXs 7
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_6)); // KXs 6
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_5)); // KXs 5
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_4)); // KXs 4
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_3)); // KXs 3
        aGROUP7.add(new HandSorted(Card.CLUBS_K, Card.CLUBS_2)); // KXs 2
        aGROUP7.add(new HandSorted(Card.CLUBS_T, Card.CLUBS_7));
        aGROUP7.add(new HandSorted(Card.CLUBS_Q, Card.CLUBS_8));

        // Group 8: 87, 53s, A9, Q9, 76, 42s, 32s, 96s, 85s, J8, J7s, 65, 54, 74s, K9,
        // T8
        aGROUP8.add(new HandSorted(Card.CLUBS_8, Card.SPADES_7));
        aGROUP8.add(new HandSorted(Card.CLUBS_5, Card.CLUBS_3));
        aGROUP8.add(new HandSorted(Card.CLUBS_A, Card.SPADES_9));
        aGROUP8.add(new HandSorted(Card.CLUBS_Q, Card.SPADES_9));
        aGROUP8.add(new HandSorted(Card.CLUBS_7, Card.SPADES_6));
        aGROUP8.add(new HandSorted(Card.CLUBS_4, Card.CLUBS_2));
        aGROUP8.add(new HandSorted(Card.CLUBS_3, Card.CLUBS_2));
        aGROUP8.add(new HandSorted(Card.CLUBS_9, Card.CLUBS_6));
        aGROUP8.add(new HandSorted(Card.CLUBS_8, Card.CLUBS_5));
        aGROUP8.add(new HandSorted(Card.CLUBS_J, Card.SPADES_8));
        aGROUP8.add(new HandSorted(Card.CLUBS_J, Card.CLUBS_7));
        aGROUP8.add(new HandSorted(Card.CLUBS_6, Card.SPADES_5));
        aGROUP8.add(new HandSorted(Card.CLUBS_5, Card.SPADES_4));
        aGROUP8.add(new HandSorted(Card.CLUBS_7, Card.CLUBS_4));
        aGROUP8.add(new HandSorted(Card.CLUBS_K, Card.SPADES_9));
        aGROUP8.add(new HandSorted(Card.CLUBS_T, Card.SPADES_8));

        GROUPS[0] = aGROUP1;
        GROUPS[1] = aGROUP2;
        GROUPS[2] = aGROUP3;
        GROUPS[3] = aGROUP4;
        GROUPS[4] = aGROUP5;
        GROUPS[5] = aGROUP6;
        GROUPS[6] = aGROUP7;
        GROUPS[7] = aGROUP8;
    }

    /**
     * Private constructor - utility class should not be instantiated.
     */
    private SklankskyRanking() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Return Sklansky rank for given hand.
     * <p>
     * Rank is calculated as: (group * 100) + position_in_group
     * <p>
     * Example ranks:
     * <ul>
     * <li>AA = 101 (group 1, position 1)</li>
     * <li>KK = 102 (group 1, position 2)</li>
     * <li>TT = 201 (group 2, position 1)</li>
     * <li>72o = 1000 (not in groups, default group 10)</li>
     * </ul>
     * <p>
     * Lower rank = stronger hand.
     *
     * @param hand
     *            The two-card starting hand (sorted)
     * @return Sklansky rank (100-899 for grouped hands, 1000 for ungrouped hands)
     */
    public static int getRank(HandSorted hand) {
        int index;
        for (int i = 0; i < GROUPS.length; i++) {
            if ((index = getIndex(GROUPS[i], hand)) != -1) {
                return (MULT * (i + 1)) + (index + 1);
            }
        }

        // Not in one of the groups, return default rank (group 10)
        return MULT * 10;
    }

    /**
     * Return group number from rank (1-8, or 10 for ungrouped hands).
     *
     * @param rank
     *            Sklansky rank
     * @return Group number
     */
    public static int getGroupFromRank(int rank) {
        return rank / MULT;
    }

    /**
     * Is hand part of group? Return index or -1.
     */
    private static int getIndex(ArrayList<HandSorted> group, HandSorted hand) {
        for (int i = 0; i < group.size(); i++) {
            HandSorted h = group.get(i);
            if (h.isEquivalent(hand)) {
                return i;
            }
        }
        return -1;
    }
}
