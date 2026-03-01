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
package com.donohoedigital.games.poker.core.ai;

import static org.assertj.core.api.Assertions.*;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandSorted;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for {@link SklankskyRanking}. */
class SklankskyRankingTest {

    // ------------------------------------------------------------------
    // Group 1 hands
    // ------------------------------------------------------------------

    @Test
    void should_ReturnGroup1_When_AA() {
        int rank = SklankskyRanking.getRank(SklankskyRanking.AA);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(1);
        assertThat(rank).isEqualTo(101);
    }

    @Test
    void should_ReturnGroup1_When_KK() {
        int rank = SklankskyRanking.getRank(SklankskyRanking.KK);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(1);
        assertThat(rank).isEqualTo(102);
    }

    @Test
    void should_ReturnGroup1_When_AKs() {
        int rank = SklankskyRanking.getRank(SklankskyRanking.AKs);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(1);
        assertThat(rank).isEqualTo(105);
    }

    // ------------------------------------------------------------------
    // Group 2 hands
    // ------------------------------------------------------------------

    @Test
    void should_ReturnGroup2_When_TT() {
        int rank = SklankskyRanking.getRank(SklankskyRanking.TT);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(2);
        assertThat(rank).isEqualTo(201);
    }

    @Test
    void should_ReturnGroup2_When_AKo() {
        int rank = SklankskyRanking.getRank(SklankskyRanking.AKo);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(2);
        assertThat(rank).isEqualTo(205);
    }

    // ------------------------------------------------------------------
    // Suited vs offsuit distinction
    // ------------------------------------------------------------------

    @Test
    void should_DistinguishSuitedFromOffsuit_When_AK() {
        int suitedRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_A, Card.SPADES_K));
        int offsuitRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_A, Card.HEARTS_K));
        // AKs is group 1, AKo is group 2
        assertThat(SklankskyRanking.getGroupFromRank(suitedRank)).isEqualTo(1);
        assertThat(SklankskyRanking.getGroupFromRank(offsuitRank)).isEqualTo(2);
        assertThat(suitedRank).isLessThan(offsuitRank);
    }

    @Test
    void should_DistinguishSuitedFromOffsuit_When_AQ() {
        int suitedRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_A, Card.SPADES_Q));
        int offsuitRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_A, Card.HEARTS_Q));
        // AQs is group 2, AQo is group 3
        assertThat(SklankskyRanking.getGroupFromRank(suitedRank)).isEqualTo(2);
        assertThat(SklankskyRanking.getGroupFromRank(offsuitRank)).isEqualTo(3);
    }

    @Test
    void should_DistinguishSuitedFromOffsuit_When_KQ() {
        int suitedRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_K, Card.SPADES_Q));
        int offsuitRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_K, Card.HEARTS_Q));
        // KQs is group 2, KQo is group 4
        assertThat(SklankskyRanking.getGroupFromRank(suitedRank)).isEqualTo(2);
        assertThat(SklankskyRanking.getGroupFromRank(offsuitRank)).isEqualTo(4);
    }

    // ------------------------------------------------------------------
    // Equivalence across suits (same rank/suited pattern)
    // ------------------------------------------------------------------

    @Test
    void should_ReturnSameGroup_When_AKsWithDifferentSuits() {
        int clubsRank = SklankskyRanking.getRank(new HandSorted(Card.CLUBS_A, Card.CLUBS_K));
        int spadesRank = SklankskyRanking.getRank(new HandSorted(Card.SPADES_A, Card.SPADES_K));
        int heartsRank = SklankskyRanking.getRank(new HandSorted(Card.HEARTS_A, Card.HEARTS_K));
        assertThat(clubsRank).isEqualTo(spadesRank).isEqualTo(heartsRank);
    }

    @Test
    void should_ReturnSameGroup_When_AAPairsWithDifferentSuits() {
        int rank1 = SklankskyRanking.getRank(new HandSorted(Card.SPADES_A, Card.HEARTS_A));
        int rank2 = SklankskyRanking.getRank(new HandSorted(Card.CLUBS_A, Card.SPADES_A));
        assertThat(rank1).isEqualTo(rank2);
    }

    // ------------------------------------------------------------------
    // Ungrouped hands (group 10)
    // ------------------------------------------------------------------

    @Test
    void should_ReturnGroup10_When_72o() {
        HandSorted hand72o = new HandSorted(Card.SPADES_7, Card.HEARTS_2);
        int rank = SklankskyRanking.getRank(hand72o);
        assertThat(rank).isEqualTo(1000);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).isEqualTo(10);
    }

    @Test
    void should_ReturnGroup10_When_83o() {
        HandSorted hand83o = new HandSorted(Card.SPADES_8, Card.HEARTS_3);
        int rank = SklankskyRanking.getRank(hand83o);
        assertThat(rank).isEqualTo(1000);
    }

    // ------------------------------------------------------------------
    // getGroupFromRank
    // ------------------------------------------------------------------

    @Test
    void should_ExtractGroup_When_ValidRank() {
        assertThat(SklankskyRanking.getGroupFromRank(101)).isEqualTo(1);
        assertThat(SklankskyRanking.getGroupFromRank(205)).isEqualTo(2);
        assertThat(SklankskyRanking.getGroupFromRank(899)).isEqualTo(8);
        assertThat(SklankskyRanking.getGroupFromRank(1000)).isEqualTo(10);
    }

    // ------------------------------------------------------------------
    // MAXGROUP constants
    // ------------------------------------------------------------------

    @Test
    void should_HaveCorrectMaxGroupBoundaries() {
        assertThat(SklankskyRanking.MAXGROUP1).isEqualTo(199);
        assertThat(SklankskyRanking.MAXGROUP2).isEqualTo(299);
        assertThat(SklankskyRanking.MAXGROUP3).isEqualTo(399);
        assertThat(SklankskyRanking.MAXGROUP4).isEqualTo(499);
        assertThat(SklankskyRanking.MAXGROUP5).isEqualTo(599);
        assertThat(SklankskyRanking.MAXGROUP6).isEqualTo(699);
        assertThat(SklankskyRanking.MAXGROUP7).isEqualTo(799);
        assertThat(SklankskyRanking.MAXGROUP8).isEqualTo(899);
    }

    @Test
    void should_HaveAllGroup1RanksBelowMaxGroup1() {
        assertThat(SklankskyRanking.getRank(SklankskyRanking.AA)).isLessThanOrEqualTo(SklankskyRanking.MAXGROUP1);
        assertThat(SklankskyRanking.getRank(SklankskyRanking.KK)).isLessThanOrEqualTo(SklankskyRanking.MAXGROUP1);
        assertThat(SklankskyRanking.getRank(SklankskyRanking.QQ)).isLessThanOrEqualTo(SklankskyRanking.MAXGROUP1);
        assertThat(SklankskyRanking.getRank(SklankskyRanking.JJ)).isLessThanOrEqualTo(SklankskyRanking.MAXGROUP1);
        assertThat(SklankskyRanking.getRank(SklankskyRanking.AKs)).isLessThanOrEqualTo(SklankskyRanking.MAXGROUP1);
    }

    // ------------------------------------------------------------------
    // Parameterized: every group from 1 through 8
    // ------------------------------------------------------------------

    static Stream<Arguments> groupedHandsProvider() {
        return Stream.of(
                // Group 1
                Arguments.of(new HandSorted(Card.SPADES_A, Card.HEARTS_A), 1, "AA"),
                Arguments.of(new HandSorted(Card.SPADES_K, Card.HEARTS_K), 1, "KK"),
                Arguments.of(new HandSorted(Card.SPADES_Q, Card.HEARTS_Q), 1, "QQ"),
                Arguments.of(new HandSorted(Card.SPADES_J, Card.HEARTS_J), 1, "JJ"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.SPADES_K), 1, "AKs"),
                // Group 2
                Arguments.of(new HandSorted(Card.SPADES_T, Card.HEARTS_T), 2, "TT"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.SPADES_Q), 2, "AQs"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.SPADES_J), 2, "AJs"),
                Arguments.of(new HandSorted(Card.SPADES_K, Card.SPADES_Q), 2, "KQs"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.HEARTS_K), 2, "AKo"),
                // Group 3
                Arguments.of(new HandSorted(Card.SPADES_9, Card.HEARTS_9), 3, "99"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.SPADES_T), 3, "ATs"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.HEARTS_Q), 3, "AQo"),
                // Group 4
                Arguments.of(new HandSorted(Card.SPADES_8, Card.HEARTS_8), 4, "88"),
                Arguments.of(new HandSorted(Card.SPADES_K, Card.HEARTS_Q), 4, "KQo"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.HEARTS_J), 4, "AJo"),
                // Group 5
                Arguments.of(new HandSorted(Card.SPADES_7, Card.HEARTS_7), 5, "77"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.SPADES_2), 5, "A2s"),
                Arguments.of(new HandSorted(Card.SPADES_6, Card.SPADES_5), 5, "65s"),
                // Group 6
                Arguments.of(new HandSorted(Card.SPADES_6, Card.HEARTS_6), 6, "66"),
                Arguments.of(new HandSorted(Card.SPADES_5, Card.HEARTS_5), 6, "55"),
                Arguments.of(new HandSorted(Card.SPADES_A, Card.HEARTS_T), 6, "ATo"),
                // Group 7
                Arguments.of(new HandSorted(Card.SPADES_4, Card.HEARTS_4), 7, "44"),
                Arguments.of(new HandSorted(Card.SPADES_3, Card.HEARTS_3), 7, "33"),
                Arguments.of(new HandSorted(Card.SPADES_2, Card.HEARTS_2), 7, "22"),
                // Group 8
                Arguments.of(new HandSorted(Card.SPADES_8, Card.HEARTS_7), 8, "87o"),
                Arguments.of(new HandSorted(Card.SPADES_K, Card.HEARTS_9), 8, "K9o"),
                Arguments.of(new HandSorted(Card.SPADES_T, Card.HEARTS_8), 8, "T8o"));
    }

    @ParameterizedTest(name = "{2} should be group {1}")
    @MethodSource("groupedHandsProvider")
    void should_ReturnCorrectGroup_When_GroupedHand(HandSorted hand, int expectedGroup, String label) {
        int rank = SklankskyRanking.getRank(hand);
        assertThat(SklankskyRanking.getGroupFromRank(rank)).as("group for %s", label).isEqualTo(expectedGroup);
    }

    // ------------------------------------------------------------------
    // Position ordering within a group
    // ------------------------------------------------------------------

    @Test
    void should_AssignIncreasingPositions_When_Group1Hands() {
        int aaRank = SklankskyRanking.getRank(SklankskyRanking.AA);
        int kkRank = SklankskyRanking.getRank(SklankskyRanking.KK);
        int qqRank = SklankskyRanking.getRank(SklankskyRanking.QQ);
        int jjRank = SklankskyRanking.getRank(SklankskyRanking.JJ);
        int aksRank = SklankskyRanking.getRank(SklankskyRanking.AKs);

        assertThat(aaRank).isLessThan(kkRank);
        assertThat(kkRank).isLessThan(qqRank);
        assertThat(qqRank).isLessThan(jjRank);
        assertThat(jjRank).isLessThan(aksRank);
    }
}
