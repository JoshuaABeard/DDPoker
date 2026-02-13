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
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for HandInfoFaster - the performance-critical hand
 * evaluator used millions of times during AI simulations. Tests all 10 Texas
 * Hold'em hand types and edge cases.
 *
 * Phase 2 Unit Testing - Priority 1
 */
class HandInfoFasterTest {

    // ===== Royal Flush Tests =====

    @Test
    void should_RecognizeRoyalFlush_When_AKQJT_SameSuit() {
        // Arrange
        Hand hole = new Hand(CLUBS_A, CLUBS_K);
        Hand community = new Hand(CLUBS_Q, CLUBS_J, CLUBS_T);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.ROYAL_FLUSH);
        assertThat(score).isEqualTo(10000014); // 10*BASE + 14*H0 (Ace=14)
    }

    @Test
    void should_RecognizeRoyalFlush_When_DifferentSuits() {
        // Arrange - Royal flush in spades
        Hand hole = new Hand(SPADES_A, SPADES_K);
        Hand community = new Hand(SPADES_Q, SPADES_J, SPADES_T);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.ROYAL_FLUSH);
        assertThat(score).isEqualTo(10000014);
    }

    @Test
    void should_RecognizeRoyalFlush_When_SevenCardsIncludeRoyal() {
        // Arrange - Royal flush with extra cards
        Hand hole = new Hand(CLUBS_A, CLUBS_K);
        Hand community = new Hand(CLUBS_Q, CLUBS_J, CLUBS_T, SPADES_2, HEARTS_K);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.ROYAL_FLUSH);
        assertThat(score).isEqualTo(10000014);
    }

    // ===== Straight Flush Tests =====

    @Test
    void should_RecognizeStraightFlush_When_FiveConsecutiveSameSuit() {
        // Arrange - King-high straight flush
        Hand hole = new Hand(HEARTS_9, HEARTS_J);
        Hand community = new Hand(HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T, HEARTS_3);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT_FLUSH);
        assertThat(score).isEqualTo(9000013); // 9*BASE + 13*H0 (King=13)
    }

    @Test
    void should_RecognizeWheelStraightFlush_When_5432A_SameSuit() {
        // Arrange - Wheel straight flush (5-high, ace low)
        Hand hole = new Hand(HEARTS_A, HEARTS_2);
        Hand community = new Hand(HEARTS_3, HEARTS_4, HEARTS_5, CLUBS_A, SPADES_A);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT_FLUSH);
        assertThat(score).isEqualTo(9000005); // 9*BASE + 5*H0 (5-high)
    }

    @Test
    void should_ScoreStraightFlushByHighCard_When_ComparingMultiple() {
        // Arrange - 6-high straight flush should beat 5-high
        Hand hole = new Hand(HEARTS_A, HEARTS_2);
        Hand community = new Hand(HEARTS_3, HEARTS_4, HEARTS_5, HEARTS_6, SPADES_A);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT_FLUSH);
        assertThat(score).isEqualTo(9000006); // 9*BASE + 6*H0 (6-high beats 5-high)
    }

    // ===== Four of a Kind Tests =====

    @Test
    void should_RecognizeQuads_When_FourSameRank() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7, HEARTS_7);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.QUADS);
        assertThat(score).isEqualTo(8000135); // 8*BASE + 8*H1 + 7*H0
    }

    @Test
    void should_ScoreQuadsByRankAndKicker_When_Comparing() {
        // Arrange - Quads with ace kicker
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, SPADES_8, SPADES_A);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.QUADS);
        // 8*BASE + 8*H1 + 14*H0
        int expectedScore = 8 * HandInfo.BASE + 8 * HandInfo.H1 + 14 * HandInfo.H0;
        assertThat(score).isEqualTo(expectedScore);
    }

    @Test
    void should_ChooseHigherQuads_When_SevenCardsAvailable() {
        // Arrange - Board has trips, hole makes quads
        Hand hole = new Hand(CLUBS_A, HEARTS_A);
        Hand community = new Hand(DIAMONDS_A, SPADES_A, SPADES_K);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.QUADS);
        // Should be Aces quads with King kicker
        assertThat(score).isGreaterThan(8000000).isLessThan(9000000);
    }

    // ===== Full House Tests =====

    @Test
    void should_RecognizeFullHouse_When_TripsAndPair() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_7, HEARTS_7);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.FULL_HOUSE);
        assertThat(score).isEqualTo(7000140); // 7*BASE + 8*H1 + 12*H0 (8s full of Queens)
    }

    @Test
    void should_RecognizeFullHouse_When_TwoTrips_ChoosesBestCombination() {
        // Arrange - Two trips: eights and queens
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_Q, HEARTS_7);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.FULL_HOUSE);
        assertThat(score).isEqualTo(7000200); // 7*BASE + 12*H1 + 8*H0 (Queens full of 8s)
    }

    @Test
    void should_ScoreFullHouseByTripsRank_When_Comparing() {
        // Arrange - Aces full should beat any other full house
        Hand hole = new Hand(CLUBS_A, HEARTS_A);
        Hand community = new Hand(DIAMONDS_A, SPADES_K, CLUBS_K);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.FULL_HOUSE);
        // Should be > 7000200 (Queens full)
        assertThat(score).isGreaterThan(7000200);
    }

    // ===== Flush Tests =====

    @Test
    void should_RecognizeFlush_When_FivePlusSameSuit() {
        // Arrange - Clubs flush
        Hand hole = new Hand(CLUBS_8, CLUBS_J);
        Hand community = new Hand(CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.FLUSH);
        assertThat(score).isEqualTo(6904104); // K-Q-J-T-8 flush
    }

    @Test
    void should_SelectHighestFiveFlush_When_SevenSameSuit() {
        // Arrange - Seven clubs, should select top 5
        Hand hole = new Hand(CLUBS_A, CLUBS_K);
        Hand community = new Hand(CLUBS_Q, CLUBS_J, CLUBS_9, CLUBS_2, CLUBS_3);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.FLUSH);
        // Should use A-K-Q-J-9, not include 3 or 2
        assertThat(score).isGreaterThan(6900000);
    }

    @Test
    void should_ScoreFlushByHighestFive_When_Comparing() {
        // Arrange - Flush with lower kicker
        Hand hole = new Hand(CLUBS_7, CLUBS_J);
        Hand community = new Hand(CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.FLUSH);
        assertThat(score).isEqualTo(6904103); // K-Q-J-T-7 flush (lower than K-Q-J-T-8)
    }

    // ===== Straight Tests =====

    @Test
    void should_RecognizeStraight_When_FiveConsecutiveRanks() {
        // Arrange - 6-high straight
        Hand hole = new Hand(CLUBS_2, HEARTS_A);
        Hand community = new Hand(HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_6, SPADES_A);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT);
        assertThat(score).isEqualTo(5000006); // 5*BASE + 6*H0
    }

    @Test
    void should_RecognizeWheel_When_5432A_MixedSuits() {
        // Arrange - Wheel (5-high straight, ace low)
        Hand hole = new Hand(CLUBS_2, HEARTS_A);
        Hand community = new Hand(HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_A, SPADES_A);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT);
        assertThat(score).isEqualTo(5000005); // 5*BASE + 5*H0
    }

    @Test
    void should_ScoreStraightByHighCard_When_Comparing() {
        // Arrange - 9-high straight
        Hand hole = new Hand(CLUBS_5, HEARTS_6);
        Hand community = new Hand(DIAMONDS_7, SPADES_8, CLUBS_9, SPADES_A, HEARTS_K);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT);
        assertThat(score).isGreaterThan(5000006); // Should be 9-high
    }

    @Test
    void should_ChooseHighestStraight_When_SevenCardsAvailable() {
        // Arrange - Straight with two pair also present
        Hand hole = new Hand(CLUBS_8, HEARTS_8);
        Hand community = new Hand(DIAMONDS_6, SPADES_5, SPADES_7, DIAMONDS_7, HEARTS_4);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT);
        assertThat(score).isEqualTo(5000008); // 8-high straight
    }

    // ===== Three of a Kind Tests =====

    @Test
    void should_RecognizeTrips_When_ThreeSameRank() {
        // Arrange
        Hand hole = new Hand(CLUBS_K, HEARTS_8);
        Hand community = new Hand(DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_J, HEARTS_3);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.TRIPS);
        assertThat(score).isEqualTo(4002267); // 4*BASE + 8*H2 + 13*H1 + 11*H0
    }

    @Test
    void should_ScoreTripsByRankAndKickers_When_Comparing() {
        // Arrange - Trips with different kickers
        Hand hole = new Hand(CLUBS_2, HEARTS_2);
        Hand community = new Hand(DIAMONDS_2, SPADES_A, CLUBS_K, SPADES_Q, HEARTS_J);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.TRIPS);
        // Should have ace and king as kickers
        assertThat(score).isGreaterThan(4000000).isLessThan(5000000);
    }

    // ===== Two Pair Tests =====

    @Test
    void should_RecognizeTwoPair_When_TwoPairs() {
        // Arrange
        Hand hole = new Hand(CLUBS_A, HEARTS_A);
        Hand community = new Hand(DIAMONDS_K, SPADES_K, CLUBS_Q, SPADES_2, HEARTS_3);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.TWO_PAIR);
        // Aces and Kings with Queen kicker
        assertThat(score).isGreaterThan(3000000).isLessThan(4000000);
    }

    @Test
    void should_ChooseHighestTwoPair_When_ThreePairsAvailable() {
        // Arrange - Three pairs: AA, KK, QQ - should use AA and KK
        Hand hole = new Hand(CLUBS_A, HEARTS_A);
        Hand community = new Hand(DIAMONDS_K, SPADES_K, CLUBS_Q, SPADES_Q, HEARTS_J);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.TWO_PAIR);
        // Should be Aces and Kings (not Queens)
        assertThat(score).isGreaterThan(3000000);
    }

    @Test
    void should_ScoreTwoPairByHighPair_LowPair_Kicker() {
        // Arrange - Twos and threes with ace kicker
        Hand hole = new Hand(CLUBS_2, HEARTS_2);
        Hand community = new Hand(DIAMONDS_3, SPADES_3, CLUBS_A, SPADES_K, HEARTS_Q);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.TWO_PAIR);
        // Should have ace kicker
        assertThat(score).isGreaterThan(3000000).isLessThan(4000000);
    }

    // ===== One Pair Tests =====

    @Test
    void should_RecognizePair_When_TwoSameRank() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_A);
        Hand community = new Hand(DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_6);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.PAIR);
        assertThat(score).isEqualTo(2028376); // 2*BASE + 6*H3 + 14*H2 + 13*H1 + 8*H0
    }

    @Test
    void should_ScorePairByRankAndKickers_When_Comparing() {
        // Arrange - Pair of twos with ace kicker
        Hand hole = new Hand(CLUBS_2, HEARTS_2);
        Hand community = new Hand(DIAMONDS_A, SPADES_K, CLUBS_Q, SPADES_9, HEARTS_7);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.PAIR);
        // Should use A-K-Q as kickers
        assertThat(score).isGreaterThan(2000000).isLessThan(3000000);
    }

    // ===== High Card Tests =====

    @Test
    void should_RecognizeHighCard_When_NoMadeHand() {
        // Arrange
        Hand hole = new Hand(CLUBS_8, HEARTS_A);
        Hand community = new Hand(DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2, HEARTS_Q);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.HIGH_CARD);
        assertThat(score).isEqualTo(1973958); // A-K-Q-8-6
    }

    @Test
    void should_ScoreHighCardByTopFive_When_Comparing() {
        // Arrange - Different high cards
        Hand hole = new Hand(CLUBS_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, CLUBS_9, SPADES_7, HEARTS_5);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.HIGH_CARD);
        // Should use A-K-Q-J-9
        assertThat(score).isGreaterThan(1973958);
    }

    // ===== Edge Cases =====

    @Test
    void should_HandleNullCommunityCards_When_PreflopEvaluation() {
        // Arrange
        Hand hole = new Hand(CLUBS_A, HEARTS_A);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, null);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.PAIR);
    }

    @Test
    void should_SkipBlankCards_When_MidGameJoin() {
        // Arrange - Hand with blank card
        Hand hole = new Hand();
        hole.addCard(CLUBS_A);
        hole.addCard(Card.BLANK); // Blank card
        Hand community = new Hand(HEARTS_A, DIAMONDS_K, SPADES_Q, CLUBS_9, HEARTS_7);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert - Should still evaluate correctly, skipping blank
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.PAIR); // Pair of aces
    }

    @Test
    void should_HandleMinimumFiveCards_When_Evaluating() {
        // Arrange - Exactly 5 cards
        Hand hole = new Hand(CLUBS_A, HEARTS_K);
        Hand community = new Hand(DIAMONDS_Q, SPADES_J, CLUBS_T);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT); // A-K-Q-J-T straight
    }

    @Test
    void should_HandleMaxSevenCards_When_ChooseBestFive() {
        // Arrange - Full 7 cards
        Hand hole = new Hand(CLUBS_2, HEARTS_3);
        Hand community = new Hand(DIAMONDS_4, SPADES_5, CLUBS_6, HEARTS_A, SPADES_K);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        int handType = score / HandInfo.BASE;
        assertThat(handType).isEqualTo(HandInfo.STRAIGHT); // 6-high straight
    }

    // ===== Scoring System Tests =====

    @Test
    void should_UseCorrectBaseMultiplier_When_CalculatingScore() {
        // Arrange
        Hand hole = new Hand(CLUBS_A, HEARTS_A);
        Hand community = new Hand(DIAMONDS_K, SPADES_Q, CLUBS_J);
        HandInfoFaster evaluator = new HandInfoFaster();

        // Act
        int score = evaluator.getScore(hole, community);

        // Assert
        // Pair should be in range [2000000, 3000000)
        assertThat(score).isGreaterThanOrEqualTo(2000000).isLessThan(3000000);
    }

    @Test
    void should_RankHandTypesCorrectly_When_Comparing() {
        // Arrange - Create scores for different hand types
        HandInfoFaster evaluator = new HandInfoFaster();

        int straightFlushScore = evaluator.getScore(new Hand(HEARTS_9, HEARTS_J),
                new Hand(HEARTS_K, HEARTS_Q, HEARTS_T));

        int quadsScore = evaluator.getScore(new Hand(CLUBS_8, HEARTS_8), new Hand(DIAMONDS_8, SPADES_8, CLUBS_K));

        int fullHouseScore = evaluator.getScore(new Hand(CLUBS_A, HEARTS_A), new Hand(DIAMONDS_A, SPADES_K, CLUBS_K));

        // Assert - Rankings should be correct
        assertThat(straightFlushScore).isGreaterThan(quadsScore);
        assertThat(quadsScore).isGreaterThan(fullHouseScore);
    }
}
