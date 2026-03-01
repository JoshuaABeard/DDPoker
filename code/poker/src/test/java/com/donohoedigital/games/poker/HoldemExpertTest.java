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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandSorted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HoldemExpert - Sklansky starting hand group rankings.
 */
class HoldemExpertTest {

    // ===== Group 1 Tests =====

    @Test
    void should_AssignGroup1_When_PocketAces() {
        HandSorted hand = new HandSorted(Card.DIAMONDS_A, Card.HEARTS_A);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_AssignGroup1_When_PocketKings() {
        HandSorted hand = new HandSorted(Card.DIAMONDS_K, Card.HEARTS_K);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_AssignGroup1_When_PocketQueens() {
        HandSorted hand = new HandSorted(Card.DIAMONDS_Q, Card.HEARTS_Q);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_AssignGroup1_When_PocketJacks() {
        HandSorted hand = new HandSorted(Card.DIAMONDS_J, Card.HEARTS_J);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    @Test
    void should_AssignGroup1_When_AKSuited() {
        HandSorted hand = new HandSorted(Card.HEARTS_A, Card.HEARTS_K);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
    }

    // ===== Group 2 Tests =====

    @Test
    void should_AssignGroup2_When_PocketTens() {
        HandSorted hand = new HandSorted(Card.DIAMONDS_T, Card.HEARTS_T);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(2);
    }

    @Test
    void should_AssignGroup2_When_AQSuited() {
        HandSorted hand = new HandSorted(Card.SPADES_A, Card.SPADES_Q);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(2);
    }

    @Test
    void should_AssignGroup2_When_AKOffsuit() {
        HandSorted hand = new HandSorted(Card.HEARTS_A, Card.SPADES_K);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(2);
    }

    // ===== Suited vs Offsuit Distinction =====

    @Test
    void should_DistinguishSuitedFromOffsuit_When_AK() {
        HandSorted suited = new HandSorted(Card.HEARTS_A, Card.HEARTS_K);
        HandSorted offsuit = new HandSorted(Card.HEARTS_A, Card.SPADES_K);

        int suitedRank = HoldemExpert.getSklanskyRank(suited);
        int offsuitRank = HoldemExpert.getSklanskyRank(offsuit);

        // AKs is group 1, AKo is group 2
        assertThat(HoldemExpert.getGroupFromRank(suitedRank)).isEqualTo(1);
        assertThat(HoldemExpert.getGroupFromRank(offsuitRank)).isEqualTo(2);
        assertThat(suitedRank).isLessThan(offsuitRank);
    }

    @Test
    void should_DistinguishSuitedFromOffsuit_When_AQ() {
        HandSorted suited = new HandSorted(Card.DIAMONDS_A, Card.DIAMONDS_Q);
        HandSorted offsuit = new HandSorted(Card.DIAMONDS_A, Card.SPADES_Q);

        int suitedRank = HoldemExpert.getSklanskyRank(suited);
        int offsuitRank = HoldemExpert.getSklanskyRank(offsuit);

        // AQs is group 2, AQo is group 3
        assertThat(HoldemExpert.getGroupFromRank(suitedRank)).isEqualTo(2);
        assertThat(HoldemExpert.getGroupFromRank(offsuitRank)).isEqualTo(3);
    }

    @Test
    void should_DistinguishSuitedFromOffsuit_When_KQ() {
        HandSorted suited = new HandSorted(Card.SPADES_K, Card.SPADES_Q);
        HandSorted offsuit = new HandSorted(Card.SPADES_K, Card.HEARTS_Q);

        int suitedRank = HoldemExpert.getSklanskyRank(suited);
        int offsuitRank = HoldemExpert.getSklanskyRank(offsuit);

        // KQs is group 2, KQo is group 4
        assertThat(HoldemExpert.getGroupFromRank(suitedRank)).isEqualTo(2);
        assertThat(HoldemExpert.getGroupFromRank(offsuitRank)).isEqualTo(4);
    }

    // ===== Rank Calculation Tests =====

    @Test
    void should_ReturnCorrectRankFormat_When_FirstInGroup() {
        // AA is 1st in group 1 => MULT*(1+0) + (0+1) = 100 + 1 = 101
        // Wait: i=0 for group 1, n=0 for first item => MULT*(0+1) + (0+1) = 101
        HandSorted hand = new HandSorted(Card.DIAMONDS_A, Card.HEARTS_A);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isEqualTo(101);
    }

    @Test
    void should_ReturnCorrectRankFormat_When_LastInGroup1() {
        // AKs is 5th in group 1 => MULT*1 + 5 = 105
        HandSorted hand = new HandSorted(Card.HEARTS_A, Card.HEARTS_K);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isEqualTo(105);
    }

    @Test
    void should_ReturnCorrectRankFormat_When_FirstInGroup2() {
        // TT is 1st in group 2 => MULT*2 + 1 = 201
        HandSorted hand = new HandSorted(Card.DIAMONDS_T, Card.HEARTS_T);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isEqualTo(201);
    }

    // ===== Unranked Hands =====

    @Test
    void should_ReturnGroup10_When_HandNotInAnyGroup() {
        // 72 offsuit (worst hand in poker)
        HandSorted hand = new HandSorted(Card.HEARTS_7, Card.SPADES_2);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isEqualTo(HoldemExpert.MULT * 10);
        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(10);
    }

    @Test
    void should_ReturnGroup10_When_LowOffsuitHand() {
        // 83 offsuit - not in any Sklansky group
        HandSorted hand = new HandSorted(Card.HEARTS_8, Card.SPADES_3);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isEqualTo(1000);
    }

    // ===== getGroupFromRank Tests =====

    @Test
    void should_ExtractGroup_When_GivenRank() {
        assertThat(HoldemExpert.getGroupFromRank(101)).isEqualTo(1);
        assertThat(HoldemExpert.getGroupFromRank(199)).isEqualTo(1);
        assertThat(HoldemExpert.getGroupFromRank(201)).isEqualTo(2);
        assertThat(HoldemExpert.getGroupFromRank(899)).isEqualTo(8);
        assertThat(HoldemExpert.getGroupFromRank(1000)).isEqualTo(10);
    }

    // ===== MAXGROUP Constants =====

    @Test
    void should_DefineMaxGroupConstants_Correctly() {
        assertThat(HoldemExpert.MAXGROUP1).isEqualTo(199);
        assertThat(HoldemExpert.MAXGROUP2).isEqualTo(299);
        assertThat(HoldemExpert.MAXGROUP3).isEqualTo(399);
        assertThat(HoldemExpert.MAXGROUP4).isEqualTo(499);
        assertThat(HoldemExpert.MAXGROUP5).isEqualTo(599);
        assertThat(HoldemExpert.MAXGROUP6).isEqualTo(699);
        assertThat(HoldemExpert.MAXGROUP7).isEqualTo(799);
        assertThat(HoldemExpert.MAXGROUP8).isEqualTo(899);
    }

    @Test
    void should_HaveGroup1RankBelowMaxGroup1() {
        HandSorted hand = new HandSorted(Card.DIAMONDS_A, Card.HEARTS_A);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isLessThanOrEqualTo(HoldemExpert.MAXGROUP1);
    }

    @Test
    void should_HaveGroup8RankBelowMaxGroup8() {
        // 87o is first in group 8
        HandSorted hand = new HandSorted(Card.HEARTS_8, Card.SPADES_7);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(rank).isLessThanOrEqualTo(HoldemExpert.MAXGROUP8);
        assertThat(rank).isGreaterThan(HoldemExpert.MAXGROUP7);
    }

    // ===== Pre-built Hand Constants =====

    @Test
    void should_DefinePrebuiltPairConstants() {
        assertThat(HoldemExpert.AA).isNotNull();
        assertThat(HoldemExpert.KK).isNotNull();
        assertThat(HoldemExpert.QQ).isNotNull();
        assertThat(HoldemExpert.JJ).isNotNull();
        assertThat(HoldemExpert.TT).isNotNull();
        assertThat(HoldemExpert.p99).isNotNull();
        assertThat(HoldemExpert.p88).isNotNull();
    }

    @Test
    void should_DefinePrebuiltSuitedConstants() {
        assertThat(HoldemExpert.AKs).isNotNull();
        assertThat(HoldemExpert.AQs).isNotNull();
        assertThat(HoldemExpert.AJs).isNotNull();
        assertThat(HoldemExpert.KQs).isNotNull();
        assertThat(HoldemExpert.T9s).isNotNull();
    }

    @Test
    void should_DefinePrebuiltOffsuitConstants() {
        assertThat(HoldemExpert.AKo).isNotNull();
        assertThat(HoldemExpert.AQo).isNotNull();
        assertThat(HoldemExpert.KQo).isNotNull();
    }

    @Test
    void should_HaveSuitedConstants_WithMatchingSuit() {
        // Suited constants should have cards of the same suit (isSuited == true)
        assertThat(HoldemExpert.AKs.isSuited()).isTrue();
        assertThat(HoldemExpert.AQs.isSuited()).isTrue();
        assertThat(HoldemExpert.AJs.isSuited()).isTrue();
        assertThat(HoldemExpert.KQs.isSuited()).isTrue();
        assertThat(HoldemExpert.T9s.isSuited()).isTrue();
    }

    @Test
    void should_HaveOffsuitConstants_WithDifferentSuits() {
        assertThat(HoldemExpert.AKo.isSuited()).isFalse();
        assertThat(HoldemExpert.AQo.isSuited()).isFalse();
        assertThat(HoldemExpert.KQo.isSuited()).isFalse();
    }

    @Test
    void should_HavePairConstants_WithDifferentSuits() {
        // Pairs always have different suits
        assertThat(HoldemExpert.AA.isSuited()).isFalse();
        assertThat(HoldemExpert.KK.isSuited()).isFalse();
        assertThat(HoldemExpert.p99.isSuited()).isFalse();
        assertThat(HoldemExpert.p88.isSuited()).isFalse();
    }

    // ===== getIndex Tests =====

    @Test
    void should_ReturnNegativeOne_When_HandNotInGroup() {
        // 72o is not in group 1
        HandSorted hand = new HandSorted(Card.HEARTS_7, Card.SPADES_2);
        java.util.ArrayList group = new java.util.ArrayList();
        group.add(HoldemExpert.AA);
        group.add(HoldemExpert.KK);

        int index = HoldemExpert.getIndex(group, hand);

        assertThat(index).isEqualTo(-1);
    }

    @Test
    void should_ReturnIndex_When_HandFoundInGroup() {
        java.util.ArrayList group = new java.util.ArrayList();
        group.add(HoldemExpert.AA);
        group.add(HoldemExpert.KK);
        group.add(HoldemExpert.QQ);

        // KK is at index 1
        HandSorted kk = new HandSorted(Card.DIAMONDS_K, Card.HEARTS_K);
        int index = HoldemExpert.getIndex(group, kk);

        assertThat(index).isEqualTo(1);
    }

    // ===== Equivalence / Suit Independence =====

    @Test
    void should_MatchEquivalentHand_When_DifferentSuitsButSameSuitedness() {
        // AA in spades+hearts should be equivalent to AA in clubs+spades
        HandSorted hand = new HandSorted(Card.SPADES_A, Card.HEARTS_A);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
        assertThat(rank).isEqualTo(101);
    }

    @Test
    void should_MatchSuitedHand_When_DifferentSuit() {
        // AKs in diamonds should match AKs in clubs
        HandSorted hand = new HandSorted(Card.DIAMONDS_A, Card.DIAMONDS_K);
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).isEqualTo(1);
        assertThat(rank).isEqualTo(105);
    }

    // ===== Parameterized Group Checks =====

    @ParameterizedTest(name = "Group {2}: {0}")
    @MethodSource("sklanskyHandProvider")
    void should_AssignCorrectGroup_When_GivenHand(String description, HandSorted hand, int expectedGroup) {
        int rank = HoldemExpert.getSklanskyRank(hand);

        assertThat(HoldemExpert.getGroupFromRank(rank)).as("Expected %s to be in group %d", description, expectedGroup)
                .isEqualTo(expectedGroup);
    }

    static Stream<Arguments> sklanskyHandProvider() {
        return Stream.of(
                // Group 1
                Arguments.of("AA", new HandSorted(Card.HEARTS_A, Card.DIAMONDS_A), 1),
                Arguments.of("KK", new HandSorted(Card.HEARTS_K, Card.DIAMONDS_K), 1),
                Arguments.of("QQ", new HandSorted(Card.HEARTS_Q, Card.DIAMONDS_Q), 1),
                Arguments.of("JJ", new HandSorted(Card.HEARTS_J, Card.DIAMONDS_J), 1),
                Arguments.of("AKs", new HandSorted(Card.DIAMONDS_A, Card.DIAMONDS_K), 1),

                // Group 2
                Arguments.of("TT", new HandSorted(Card.HEARTS_T, Card.DIAMONDS_T), 2),
                Arguments.of("AQs", new HandSorted(Card.HEARTS_A, Card.HEARTS_Q), 2),
                Arguments.of("AJs", new HandSorted(Card.SPADES_A, Card.SPADES_J), 2),
                Arguments.of("KQs", new HandSorted(Card.DIAMONDS_K, Card.DIAMONDS_Q), 2),
                Arguments.of("AKo", new HandSorted(Card.HEARTS_A, Card.DIAMONDS_K), 2),

                // Group 3
                Arguments.of("99", new HandSorted(Card.HEARTS_9, Card.DIAMONDS_9), 3),
                Arguments.of("JTs", new HandSorted(Card.SPADES_J, Card.SPADES_T), 3),
                Arguments.of("AQo", new HandSorted(Card.HEARTS_A, Card.DIAMONDS_Q), 3),

                // Group 4
                Arguments.of("T9s", new HandSorted(Card.HEARTS_T, Card.HEARTS_9), 4),
                Arguments.of("KQo", new HandSorted(Card.HEARTS_K, Card.DIAMONDS_Q), 4),
                Arguments.of("88", new HandSorted(Card.HEARTS_8, Card.DIAMONDS_8), 4),

                // Group 5
                Arguments.of("77", new HandSorted(Card.HEARTS_7, Card.DIAMONDS_7), 5),
                Arguments.of("87s", new HandSorted(Card.SPADES_8, Card.SPADES_7), 5),
                Arguments.of("A2s", new HandSorted(Card.HEARTS_A, Card.HEARTS_2), 5),

                // Group 6
                Arguments.of("66", new HandSorted(Card.HEARTS_6, Card.DIAMONDS_6), 6),
                Arguments.of("55", new HandSorted(Card.HEARTS_5, Card.DIAMONDS_5), 6),
                Arguments.of("ATo", new HandSorted(Card.HEARTS_A, Card.DIAMONDS_T), 6),

                // Group 7
                Arguments.of("44", new HandSorted(Card.HEARTS_4, Card.DIAMONDS_4), 7),
                Arguments.of("33", new HandSorted(Card.HEARTS_3, Card.DIAMONDS_3), 7),
                Arguments.of("22", new HandSorted(Card.HEARTS_2, Card.DIAMONDS_2), 7),

                // Group 8
                Arguments.of("87o", new HandSorted(Card.HEARTS_8, Card.DIAMONDS_7), 8),
                Arguments.of("K9o", new HandSorted(Card.HEARTS_K, Card.DIAMONDS_9), 8),
                Arguments.of("A9o", new HandSorted(Card.HEARTS_A, Card.DIAMONDS_9), 8),

                // Unranked (group 10)
                Arguments.of("72o", new HandSorted(Card.HEARTS_7, Card.DIAMONDS_2), 10),
                Arguments.of("83o", new HandSorted(Card.HEARTS_8, Card.DIAMONDS_3), 10),
                Arguments.of("92o", new HandSorted(Card.HEARTS_9, Card.DIAMONDS_2), 10));
    }

    // ===== MULT Constant =====

    @Test
    void should_DefineMult_As100() {
        assertThat(HoldemExpert.MULT).isEqualTo(100);
    }

    // ===== Rank Ordering =====

    @Test
    void should_RankGroup1BetterThanGroup2() {
        HandSorted group1Hand = new HandSorted(Card.HEARTS_A, Card.DIAMONDS_A); // AA
        HandSorted group2Hand = new HandSorted(Card.HEARTS_T, Card.DIAMONDS_T); // TT

        int rank1 = HoldemExpert.getSklanskyRank(group1Hand);
        int rank2 = HoldemExpert.getSklanskyRank(group2Hand);

        assertThat(rank1).isLessThan(rank2);
    }

    @Test
    void should_RankAllGroupedHandsBetterThanUnranked() {
        // T8o is last in group 8
        HandSorted group8Hand = new HandSorted(Card.HEARTS_T, Card.DIAMONDS_8);
        HandSorted unrankedHand = new HandSorted(Card.HEARTS_7, Card.DIAMONDS_2);

        int group8Rank = HoldemExpert.getSklanskyRank(group8Hand);
        int unrankedRank = HoldemExpert.getSklanskyRank(unrankedHand);

        assertThat(group8Rank).isLessThan(unrankedRank);
    }
}
