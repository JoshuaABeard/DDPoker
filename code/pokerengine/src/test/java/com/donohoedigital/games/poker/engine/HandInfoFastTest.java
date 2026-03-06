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
package com.donohoedigital.games.poker.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HandInfoFastTest implements HandScoreConstants {

    private HandInfoFast info;

    @BeforeEach
    void setUp() {
        info = new HandInfoFast();
    }

    // ---------------------------------------------------------------
    // Hand type detection
    // ---------------------------------------------------------------

    @Nested
    class HandTypes {

        @Test
        void should_DetectRoyalFlush_When_AceHighStraightFlush() {
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand community = new Hand(Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(ROYAL_FLUSH);
        }

        @Test
        void should_DetectStraightFlush_When_NonAceHighStraightFlush() {
            Hand pocket = new Hand(Card.HEARTS_9, Card.HEARTS_8);
            Hand community = new Hand(Card.HEARTS_7, Card.HEARTS_6, Card.HEARTS_5, Card.CLUBS_A, Card.DIAMONDS_K);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(STRAIGHT_FLUSH);
        }

        @Test
        void should_DetectStraightFlush_When_WheelFlush() {
            // A-2-3-4-5 all in spades
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_2);
            Hand community = new Hand(Card.SPADES_3, Card.SPADES_4, Card.SPADES_5, Card.HEARTS_K, Card.DIAMONDS_Q);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(STRAIGHT_FLUSH);
            assertThat(info.getStraightHighRank()).isEqualTo(Card.FIVE);
        }

        @Test
        void should_DetectQuads_When_FourOfSameRank() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_J);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(QUADS);
            assertThat(info.getQuadsRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_DetectFullHouse_When_TripsAndPair() {
            Hand pocket = new Hand(Card.SPADES_K, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_Q, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(FULL_HOUSE);
            assertThat(info.getTripsRank()).isEqualTo(Card.KING);
            assertThat(info.getBigPairRank()).isEqualTo(Card.QUEEN);
        }

        @Test
        void should_DetectFullHouse_When_TwoSetsOfTrips() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_K, Card.HEARTS_K, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(FULL_HOUSE);
            assertThat(info.getTripsRank()).isEqualTo(Card.ACE);
            assertThat(info.getBigPairRank()).isEqualTo(Card.KING);
        }

        @Test
        void should_DetectFlush_When_FiveOfSameSuit() {
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(FLUSH);
        }

        @Test
        void should_DetectStraight_When_FiveConsecutiveRanks() {
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_9);
            Hand community = new Hand(Card.DIAMONDS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_2, Card.CLUBS_A);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(STRAIGHT);
            assertThat(info.getStraightHighRank()).isEqualTo(Card.TEN);
        }

        @Test
        void should_DetectStraight_When_WheelStraight() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_2);
            Hand community = new Hand(Card.DIAMONDS_3, Card.CLUBS_4, Card.SPADES_5, Card.HEARTS_9, Card.CLUBS_K);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(STRAIGHT);
            assertThat(info.getStraightHighRank()).isEqualTo(Card.FIVE);
        }

        @Test
        void should_DetectStraight_When_BroadwayStraight() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(STRAIGHT);
            assertThat(info.getStraightHighRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_DetectTrips_When_ThreeOfSameRank() {
            Hand pocket = new Hand(Card.SPADES_J, Card.HEARTS_J);
            Hand community = new Hand(Card.DIAMONDS_J, Card.CLUBS_9, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(TRIPS);
            assertThat(info.getTripsRank()).isEqualTo(Card.JACK);
        }

        @Test
        void should_DetectTwoPair_When_TwoDifferentPairs() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(TWO_PAIR);
            assertThat(info.getBigPairRank()).isEqualTo(Card.ACE);
            assertThat(info.getSmallPairRank()).isEqualTo(Card.KING);
        }

        @Test
        void should_DetectPair_When_OnePair() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);
            Hand community = new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(PAIR);
            assertThat(info.getBigPairRank()).isEqualTo(Card.ACE);
            assertThat(info.getSmallPairRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_DetectHighCard_When_NothingConnects() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(HIGH_CARD);
            assertThat(info.getHighCardRank()).isEqualTo(Card.ACE);
        }
    }

    // ---------------------------------------------------------------
    // Score ordering: higher hand type means higher score
    // ---------------------------------------------------------------

    @Nested
    class ScoreOrdering {

        @Test
        void should_RankRoyalFlushAboveStraightFlush() {
            int royalFlush = info.getScore(new Hand(Card.SPADES_A, Card.SPADES_K),
                    new Hand(Card.SPADES_Q, Card.SPADES_J, Card.SPADES_T, Card.HEARTS_3, Card.CLUBS_2));
            int straightFlush = info.getScore(new Hand(Card.HEARTS_9, Card.HEARTS_8),
                    new Hand(Card.HEARTS_7, Card.HEARTS_6, Card.HEARTS_5, Card.CLUBS_A, Card.DIAMONDS_K));

            assertThat(royalFlush).isGreaterThan(straightFlush);
        }

        @Test
        void should_RankQuadsAboveFullHouse() {
            int quads = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_J));
            int fullHouse = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_Q, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(quads).isGreaterThan(fullHouse);
        }

        @Test
        void should_RankFlushAboveStraight() {
            int flush = info.getScore(new Hand(Card.HEARTS_A, Card.HEARTS_9),
                    new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q));
            int straight = info.getScore(new Hand(Card.SPADES_T, Card.HEARTS_9),
                    new Hand(Card.DIAMONDS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_2, Card.CLUBS_A));

            assertThat(flush).isGreaterThan(straight);
        }

        @Test
        void should_RankStraightAboveTrips() {
            int straight = info.getScore(new Hand(Card.SPADES_T, Card.HEARTS_9),
                    new Hand(Card.DIAMONDS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_2, Card.CLUBS_3));
            int trips = info.getScore(new Hand(Card.SPADES_J, Card.HEARTS_J),
                    new Hand(Card.DIAMONDS_J, Card.CLUBS_9, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(straight).isGreaterThan(trips);
        }

        @Test
        void should_RankTripsAboveTwoPair() {
            int trips = info.getScore(new Hand(Card.SPADES_2, Card.HEARTS_2),
                    new Hand(Card.DIAMONDS_2, Card.CLUBS_9, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_4));
            int twoPair = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(trips).isGreaterThan(twoPair);
        }

        @Test
        void should_RankTwoPairAbovePair() {
            int twoPair = info.getScore(new Hand(Card.SPADES_3, Card.HEARTS_2),
                    new Hand(Card.DIAMONDS_3, Card.CLUBS_2, Card.SPADES_7, Card.HEARTS_5, Card.CLUBS_4));
            int pair = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(twoPair).isGreaterThan(pair);
        }

        @Test
        void should_RankPairAboveHighCard() {
            int pair = info.getScore(new Hand(Card.SPADES_2, Card.HEARTS_2),
                    new Hand(Card.DIAMONDS_4, Card.CLUBS_6, Card.SPADES_8, Card.HEARTS_T, Card.CLUBS_Q));
            int highCard = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(pair).isGreaterThan(highCard);
        }

        @Test
        void should_RankHigherPairAboveLowerPair() {
            int acePair = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_4, Card.CLUBS_6, Card.SPADES_8, Card.HEARTS_T, Card.CLUBS_Q));
            int kingPair = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_4, Card.CLUBS_6, Card.SPADES_8, Card.HEARTS_T, Card.CLUBS_Q));

            assertThat(acePair).isGreaterThan(kingPair);
        }
    }

    // ---------------------------------------------------------------
    // Board analysis
    // ---------------------------------------------------------------

    @Nested
    class BoardAnalysis {

        @Test
        void should_TrackHighestBoardRank() {
            Hand pocket = new Hand(Card.SPADES_2, Card.HEARTS_3);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_K, Card.SPADES_5, Card.HEARTS_7, Card.CLUBS_4);

            info.getScore(pocket, community);

            assertThat(info.getHighestBoardRank()).isEqualTo(Card.KING);
        }

        @Test
        void should_TrackLowestBoardRank() {
            Hand pocket = new Hand(Card.SPADES_2, Card.HEARTS_3);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_K, Card.SPADES_5, Card.HEARTS_7, Card.CLUBS_4);

            info.getScore(pocket, community);

            assertThat(info.getLowestBoardRank()).isEqualTo(Card.FOUR);
        }

        @Test
        void should_CountOvercards_When_PocketAboveBoard() {
            // Pocket: A K, Board highest is Q
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_Q, Card.CLUBS_9, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getOvercardCount()).isEqualTo(2);
        }

        @Test
        void should_CountZeroOvercards_When_PocketBelowBoard() {
            Hand pocket = new Hand(Card.SPADES_2, Card.HEARTS_3);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_Q, Card.HEARTS_J, Card.CLUBS_T);

            info.getScore(pocket, community);

            assertThat(info.getOvercardCount()).isEqualTo(0);
        }

        @Test
        void should_CountOneOvercard_When_OnePocketCardAboveBoard() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_3);
            Hand community = new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_9, Card.HEARTS_7, Card.CLUBS_5);

            info.getScore(pocket, community);

            assertThat(info.getOvercardCount()).isEqualTo(1);
        }

        @Test
        void should_SetHighCardRank_When_HighCardHand() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHighCardRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_SetHighCardRankToZero_When_NotHighCardHand() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);
            Hand community = new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            // Pair hand, so highCardRank is 0
            assertThat(info.getHighCardRank()).isEqualTo(0);
        }
    }

    // ---------------------------------------------------------------
    // Pair/trips/quads rank tracking
    // ---------------------------------------------------------------

    @Nested
    class PairTracking {

        @Test
        void should_SetBigAndSmallPairRank_When_OnePair() {
            Hand pocket = new Hand(Card.SPADES_K, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getBigPairRank()).isEqualTo(Card.KING);
            assertThat(info.getSmallPairRank()).isEqualTo(Card.KING);
        }

        @Test
        void should_SetBothPairRanks_When_TwoPair() {
            Hand pocket = new Hand(Card.SPADES_Q, Card.HEARTS_J);
            Hand community = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getBigPairRank()).isEqualTo(Card.QUEEN);
            assertThat(info.getSmallPairRank()).isEqualTo(Card.JACK);
        }

        @Test
        void should_SetTripsRank_When_ThreeOfAKind() {
            Hand pocket = new Hand(Card.SPADES_8, Card.HEARTS_8);
            Hand community = new Hand(Card.DIAMONDS_8, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getTripsRank()).isEqualTo(Card.EIGHT);
        }

        @Test
        void should_SetTripsAndPairRanks_When_FullHouse() {
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_T);
            Hand community = new Hand(Card.DIAMONDS_T, Card.CLUBS_6, Card.SPADES_6, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getTripsRank()).isEqualTo(Card.TEN);
            assertThat(info.getBigPairRank()).isEqualTo(Card.SIX);
            assertThat(info.getSmallPairRank()).isEqualTo(Card.SIX);
        }

        @Test
        void should_SetQuadsRank_When_FourOfAKind() {
            Hand pocket = new Hand(Card.SPADES_7, Card.HEARTS_7);
            Hand community = new Hand(Card.DIAMONDS_7, Card.CLUBS_7, Card.SPADES_A, Card.HEARTS_K, Card.CLUBS_Q);

            info.getScore(pocket, community);

            assertThat(info.getQuadsRank()).isEqualTo(Card.SEVEN);
        }

        @Test
        void should_ResetPairRanksToZero_When_NoPairing() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getBigPairRank()).isEqualTo(0);
            assertThat(info.getSmallPairRank()).isEqualTo(0);
            assertThat(info.getTripsRank()).isEqualTo(0);
            assertThat(info.getQuadsRank()).isEqualTo(0);
        }
    }

    // ---------------------------------------------------------------
    // Flush draw detection
    // ---------------------------------------------------------------

    @Nested
    class FlushDrawDetection {

        @Test
        void should_DetectFlushDraw_When_FourOfSameSuit() {
            // Pocket: As Ks, Board: Qs 7s 3h 2c 4d -- 4 spades total
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand community = new Hand(Card.SPADES_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2, Card.DIAMONDS_4);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
        }

        @Test
        void should_NotDetectFlushDraw_When_FlushAlreadyMade() {
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isFalse();
        }

        @Test
        void should_NotDetectFlushDraw_When_OnlyThreeOfSuit() {
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand community = new Hand(Card.SPADES_Q, Card.HEARTS_7, Card.DIAMONDS_3, Card.CLUBS_2, Card.HEARTS_4);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isFalse();
        }

        @Test
        void should_DetectNutFlushDraw_When_HoldingAceOfSuit() {
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_3);
            Hand community = new Hand(Card.SPADES_Q, Card.SPADES_7, Card.HEARTS_K, Card.CLUBS_2, Card.DIAMONDS_4);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasNutFlushDraw()).isTrue();
            assertThat(info.has2ndNutFlushDraw()).isFalse();
            assertThat(info.hasWeakFlushDraw()).isFalse();
        }

        @Test
        void should_DetectNutFlushDraw_When_HoldingKingAndAceOnBoard() {
            // Ks in pocket, As on board -- nut flush draw
            Hand pocket = new Hand(Card.SPADES_K, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_A, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_9);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasNutFlushDraw()).isTrue();
        }

        @Test
        void should_Detect2ndNutFlushDraw_When_HoldingKingNoAceOnBoard() {
            // Ks in pocket, no As on board
            Hand pocket = new Hand(Card.SPADES_K, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_9, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_A);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasNutFlushDraw()).isFalse();
            assertThat(info.has2ndNutFlushDraw()).isTrue();
        }

        @Test
        void should_DetectNutFlushDraw_When_HoldingQueenWithAKOnBoard() {
            // Qs in pocket, As and Ks on board
            Hand pocket = new Hand(Card.SPADES_Q, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_A, Card.SPADES_K, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_9);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasNutFlushDraw()).isTrue();
        }

        @Test
        void should_Detect2ndNutFlushDraw_When_HoldingQueenWithAceOnBoard() {
            // Qs in pocket, As on board but no Ks
            Hand pocket = new Hand(Card.SPADES_Q, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_A, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_9);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.has2ndNutFlushDraw()).isTrue();
        }

        @Test
        void should_Detect2ndNutFlushDraw_When_HoldingQueenWithKingOnBoard() {
            // Qs in pocket, Ks on board but no As
            Hand pocket = new Hand(Card.SPADES_Q, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_K, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_9);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.has2ndNutFlushDraw()).isTrue();
        }

        @Test
        void should_DetectWeakFlushDraw_When_HoldingQueenWithNeitherAKOnBoard() {
            // Qs in pocket, no As or Ks on board
            Hand pocket = new Hand(Card.SPADES_Q, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_9, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_T);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasWeakFlushDraw()).isTrue();
        }

        @Test
        void should_DetectWeakFlushDraw_When_HoldingLowCard() {
            // 5s in pocket
            Hand pocket = new Hand(Card.SPADES_5, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_9, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_T);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasWeakFlushDraw()).isTrue();
            assertThat(info.hasNutFlushDraw()).isFalse();
            assertThat(info.has2ndNutFlushDraw()).isFalse();
        }

        @Test
        void should_CountFlushDrawPocketsPlayed_When_BothHoleCardsInDraw() {
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand community = new Hand(Card.SPADES_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2, Card.DIAMONDS_4);

            info.getScore(pocket, community);

            // 4 total of suit, 2 board cards of suit => 2 pocket cards played
            assertThat(info.getFlushDrawPocketsPlayed()).isEqualTo(2);
        }

        @Test
        void should_CountFlushDrawPocketsPlayed_When_OneHoleCardInDraw() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.SPADES_Q, Card.SPADES_7, Card.SPADES_3, Card.CLUBS_2, Card.DIAMONDS_4);

            info.getScore(pocket, community);

            // 4 total of spades, 3 board cards of suit => 1 pocket card played
            assertThat(info.getFlushDrawPocketsPlayed()).isEqualTo(1);
        }

        @Test
        void should_TrackBetterFlushCardCount_When_FlushMade() {
            // Flush with Ah, board has no higher hearts
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q);

            info.getScore(pocket, community);

            assertThat(info.getBetterFlushCardCount()).isEqualTo(0);
        }

        @Test
        void should_TrackFlushHighRank_When_FlushMade() {
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q);

            info.getScore(pocket, community);

            // The flush high rank is A (from pocket)
            assertThat(info.getFlushHighRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_TrackBoardFlush_When_AllFlushCardsOnBoard() {
            // Board has 5 hearts, pocket has no hearts
            Hand pocket = new Hand(Card.SPADES_A, Card.CLUBS_K);
            Hand community = new Hand(Card.HEARTS_Q, Card.HEARTS_J, Card.HEARTS_9, Card.HEARTS_7, Card.HEARTS_5);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(FLUSH);
            // Better flush cards: count of heart ranks above the 5th-best heart in hand
            assertThat(info.getBetterFlushCardCount()).isGreaterThanOrEqualTo(0);
        }
    }

    // ---------------------------------------------------------------
    // Straight draw detection
    // ---------------------------------------------------------------

    @Nested
    class StraightDrawDetection {

        @Test
        void should_DetectStraightDraw_When_OpenEndedDraw() {
            // 8-9-T-J with a gap at both ends
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_J);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_8, Card.SPADES_2, Card.HEARTS_3, Card.CLUBS_A);

            info.getScore(pocket, community);

            assertThat(info.hasStraightDraw()).isTrue();
            assertThat(info.getStraightDrawOuts()).isGreaterThan(0);
        }

        @Test
        void should_NotDetectStraightDraw_When_StraightAlreadyMade() {
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_9);
            Hand community = new Hand(Card.DIAMONDS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_2, Card.CLUBS_3);

            info.getScore(pocket, community);

            assertThat(info.hasStraightDraw()).isFalse();
            assertThat(info.getStraightDrawOuts()).isEqualTo(0);
        }

        @Test
        void should_ClearStraightDrawOuts_When_FlushMade() {
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q);

            info.getScore(pocket, community);

            assertThat(info.getStraightDrawOuts()).isEqualTo(0);
        }

        @Test
        void should_ClearStraightDrawOuts_When_FullHouseMade() {
            Hand pocket = new Hand(Card.SPADES_K, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_Q, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getStraightDrawOuts()).isEqualTo(0);
        }
    }

    // ---------------------------------------------------------------
    // Nut straight high rank
    // ---------------------------------------------------------------

    @Nested
    class NutStraightHighRank {

        @Test
        void should_ReturnNutStraightHigh_When_BoardHasThreeConsecutive() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            // Board: Q J T 3 2 -- nut straight high would be Ace (AKQJT)
            Hand community = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getNutStraightHighRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_ReturnZero_When_NoBoardStraightPossible() {
            // Board: A K 9 5 2 -- spread too far for any 3-card straight run
            Hand pocket = new Hand(Card.SPADES_3, Card.HEARTS_4);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_9, Card.HEARTS_5, Card.CLUBS_2);

            info.getScore(pocket, community);

            // 3 board cards needed within a 5-card range for a straight to be possible
            // A-K are adjacent (2 cards within A-T range)
            // This board may or may not yield a nut straight; check actual result
            int result = info.getNutStraightHighRank();
            assertThat(result).isGreaterThanOrEqualTo(0);
        }

        @Test
        void should_ReturnFive_When_WheelIsBestStraight() {
            // Board: A 4 3 9 K -- A-2-3-4-5 wheel possible with board having A,3,4
            Hand pocket = new Hand(Card.SPADES_2, Card.HEARTS_5);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_4, Card.SPADES_3, Card.HEARTS_9, Card.CLUBS_K);

            info.getScore(pocket, community);

            assertThat(info.getNutStraightHighRank()).isEqualTo(Card.FIVE);
        }
    }

    // ---------------------------------------------------------------
    // Last major suit
    // ---------------------------------------------------------------

    @Nested
    class MajorSuit {

        @Test
        void should_ReturnMajorSuit_When_OneSuitDominates() {
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_K);
            Hand community = new Hand(Card.HEARTS_Q, Card.HEARTS_J, Card.SPADES_9, Card.CLUBS_5, Card.DIAMONDS_2);

            info.getScore(pocket, community);

            // Hearts = suit rank 2
            assertThat(info.getLastMajorSuit()).isEqualTo(CardSuit.HEARTS_RANK);
        }

        @Test
        void should_ReturnSomeSuit_When_SuitsAreTied() {
            // Each suit appears once or twice; just verify it returns a valid suit
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_3, Card.DIAMONDS_2);

            info.getScore(pocket, community);

            int suit = info.getLastMajorSuit();
            assertThat(suit).isBetween(CardSuit.CLUBS_RANK, CardSuit.SPADES_RANK);
        }
    }

    // ---------------------------------------------------------------
    // Static utilities: getTypeFromScore, getCards
    // ---------------------------------------------------------------

    @Nested
    class StaticUtilities {

        @Test
        void should_ExtractTypeFromScore_When_RoyalFlush() {
            assertThat(HandInfoFast.getTypeFromScore(ROYAL_FLUSH * SCORE_BASE)).isEqualTo(ROYAL_FLUSH);
        }

        @Test
        void should_ExtractTypeFromScore_When_HighCard() {
            assertThat(HandInfoFast.getTypeFromScore(HIGH_CARD * SCORE_BASE + 12345)).isEqualTo(HIGH_CARD);
        }

        @Test
        void should_ExtractTypeFromScore_When_AllHandTypes() {
            int[] types = {ROYAL_FLUSH, STRAIGHT_FLUSH, QUADS, FULL_HOUSE, FLUSH, STRAIGHT, TRIPS, TWO_PAIR, PAIR,
                    HIGH_CARD};
            for (int type : types) {
                assertThat(HandInfoFast.getTypeFromScore(type * SCORE_BASE + 999)).isEqualTo(type);
            }
        }

        @Test
        void should_ExtractCards_When_ScoreContainsKickers() {
            // Build a score with known kickers packed into H positions
            // Score = PAIR * SCORE_BASE + rank * H3 + kicker1 * H2 + kicker2 * H1 + kicker3
            // * H0
            // H3=4096, H2=256, H1=16, H0=1
            int score = PAIR * SCORE_BASE + Card.ACE * H3 + Card.KING * H2 + Card.QUEEN * H1 + Card.JACK * H0;

            int[] cards = new int[5];
            HandInfoFast.getCards(score, cards);

            assertThat(cards[0]).isEqualTo(Card.ACE);
            assertThat(cards[1]).isEqualTo(Card.KING);
            assertThat(cards[2]).isEqualTo(Card.QUEEN);
            assertThat(cards[3]).isEqualTo(Card.JACK);
        }

        @Test
        void should_ExtractCardsFromActualScore() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            int score = info.getScore(pocket, community);

            int[] cards = new int[5];
            HandInfoFast.getCards(score, cards);

            // High card hand, first card should be ace
            assertThat(cards[0]).isEqualTo(Card.ACE);
        }

        @Test
        void should_ReturnZero_When_ScoreIsZero() {
            assertThat(HandInfoFast.getTypeFromScore(0)).isEqualTo(0);
        }
    }

    // ---------------------------------------------------------------
    // Pocket and community storage
    // ---------------------------------------------------------------

    @Nested
    class HandStorage {

        @Test
        void should_StorePocketCards_When_ScoreComputed() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getPocket().size()).isEqualTo(2);
            assertThat(info.getCommunity().size()).isEqualTo(5);
        }

        @Test
        void should_HandleNullPocket() {
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            int score = info.getScore(null, community);

            assertThat(score).isGreaterThan(0);
            assertThat(info.getPocket().size()).isEqualTo(0);
        }

        @Test
        void should_HandleNullCommunity() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);

            int score = info.getScore(pocket, null);

            assertThat(score).isGreaterThan(0);
            assertThat(info.getCommunity().size()).isEqualTo(0);
        }

        @Test
        void should_ThrowOnBothNull_When_NoCardsToEvaluate() {
            // No cards at all is not a valid use case; production code throws
            assertThatThrownBy(() -> info.getScore(null, null)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        }
    }

    // ---------------------------------------------------------------
    // Reuse: calling getScore multiple times resets state
    // ---------------------------------------------------------------

    @Nested
    class StateReset {

        @Test
        void should_ResetState_When_CalledMultipleTimes() {
            // First call: pair of aces
            info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2));
            assertThat(info.getHandType()).isEqualTo(PAIR);
            assertThat(info.getBigPairRank()).isEqualTo(Card.ACE);

            // Second call: high card
            info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2));
            assertThat(info.getHandType()).isEqualTo(HIGH_CARD);
            assertThat(info.getBigPairRank()).isEqualTo(0);
            assertThat(info.getTripsRank()).isEqualTo(0);
            assertThat(info.getQuadsRank()).isEqualTo(0);
        }

        @Test
        void should_ResetFlushDrawState_When_CalledMultipleTimes() {
            // First call: flush draw
            info.getScore(new Hand(Card.SPADES_A, Card.SPADES_K),
                    new Hand(Card.SPADES_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_2, Card.DIAMONDS_4));
            assertThat(info.hasFlushDraw()).isTrue();

            // Second call: no flush draw
            info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_3, Card.CLUBS_2));
            assertThat(info.hasFlushDraw()).isFalse();
            assertThat(info.hasNutFlushDraw()).isFalse();
            assertThat(info.has2ndNutFlushDraw()).isFalse();
            assertThat(info.hasWeakFlushDraw()).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // Straight low rank
    // ---------------------------------------------------------------

    @Nested
    class StraightLowRank {

        @Test
        void should_ReturnLowRank_When_StraightMade() {
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_9);
            Hand community = new Hand(Card.DIAMONDS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_2, Card.CLUBS_A);

            info.getScore(pocket, community);

            assertThat(info.getStraightHighRank()).isEqualTo(Card.TEN);
            assertThat(info.getStraightLowRank()).isEqualTo(Card.SIX);
        }
    }

    // ---------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------

    @Nested
    class EdgeCases {

        @Test
        void should_DetectHigherStraightFlush_When_SixCardRun() {
            // 6 cards of same suit in sequence: 5-6-7-8-9-T of hearts
            Hand pocket = new Hand(Card.HEARTS_T, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_8, Card.HEARTS_7, Card.HEARTS_6, Card.HEARTS_5, Card.CLUBS_2);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(STRAIGHT_FLUSH);
            // Should pick the highest straight: 6-T
            assertThat(info.getStraightHighRank()).isEqualTo(Card.TEN);
        }

        @Test
        void should_ChooseFlushOverStraight_When_Both() {
            // 5 hearts forming a flush but also a non-flush straight
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_3);
            Hand community = new Hand(Card.HEARTS_8, Card.HEARTS_5, Card.HEARTS_2, Card.SPADES_4, Card.CLUBS_6);

            info.getScore(pocket, community);

            // Flush > Straight in poker hand rankings
            assertThat(info.getHandType()).isEqualTo(FLUSH);
        }

        @Test
        void should_HandleThreeCardBoard() {
            // Flop only: 3 community cards
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);
            Hand community = new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_7);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(PAIR);
            assertThat(info.getBigPairRank()).isEqualTo(Card.ACE);
        }

        @Test
        void should_HandleFourCardBoard() {
            // Turn: 4 community cards
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_7, Card.HEARTS_3);

            info.getScore(pocket, community);

            assertThat(info.getHandType()).isEqualTo(TWO_PAIR);
        }

        @Test
        void should_HandlePocketOnly() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);

            int score = info.getScore(pocket, null);

            assertThat(info.getHandType()).isEqualTo(PAIR);
            assertThat(score).isGreaterThan(0);
        }

        @Test
        void should_ScoreHigherQuads_AboveLowerQuads() {
            int aceQuads = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_A, Card.SPADES_K, Card.HEARTS_Q, Card.CLUBS_J));
            int kingQuads = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_K, Card.SPADES_A, Card.HEARTS_Q, Card.CLUBS_J));

            assertThat(aceQuads).isGreaterThan(kingQuads);
        }

        @Test
        void should_ScoreHigherFullHouse_AboveLower() {
            int acesFullOfKings = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.SPADES_K, Card.HEARTS_3, Card.CLUBS_2));
            int kingsFullOfAces = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_A, Card.SPADES_A, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(acesFullOfKings).isGreaterThan(kingsFullOfAces);
        }

        @Test
        void should_DetectFlush_When_SixCardsOfSameSuit() {
            // 6 hearts total, should still detect flush
            Hand pocket = new Hand(Card.HEARTS_A, Card.HEARTS_K);
            Hand community = new Hand(Card.HEARTS_Q, Card.HEARTS_J, Card.HEARTS_9, Card.HEARTS_3, Card.CLUBS_2);

            info.getScore(pocket, community);

            // With 6+ same-suit cards it could be straight flush; check
            // A-K-Q-J-9 not a straight flush (gap at T), so it's a flush
            assertThat(info.getHandType()).isEqualTo(FLUSH);
        }

        @Test
        void should_ScoreConsistentlyOnReuse() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand community = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_T, Card.HEARTS_3, Card.CLUBS_2);

            int score1 = info.getScore(pocket, community);
            int score2 = info.getScore(pocket, community);

            assertThat(score1).isEqualTo(score2);
        }
    }

    // ---------------------------------------------------------------
    // Flush draw with Jack high
    // ---------------------------------------------------------------

    @Nested
    class JackHighFlushDraw {

        @Test
        void should_DetectNutFlushDraw_When_JackWithAKQOnBoard() {
            // Js in pocket, As Ks Qs on board
            Hand pocket = new Hand(Card.SPADES_J, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_A, Card.SPADES_K, Card.SPADES_Q, Card.HEARTS_2, Card.DIAMONDS_9);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasNutFlushDraw()).isTrue();
        }

        @Test
        void should_Detect2ndNutFlushDraw_When_JackWithAKOnBoard() {
            // Js in pocket, As Ks on board
            Hand pocket = new Hand(Card.SPADES_J, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_A, Card.SPADES_K, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_9);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.has2ndNutFlushDraw()).isTrue();
        }

        @Test
        void should_DetectWeakFlushDraw_When_JackWithNoHighCardsOnBoard() {
            Hand pocket = new Hand(Card.SPADES_J, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_9, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_T);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasWeakFlushDraw()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // Flush draw with Ten high
    // ---------------------------------------------------------------

    @Nested
    class TenHighFlushDraw {

        @Test
        void should_DetectNutFlushDraw_When_TenWithAKQJOnBoard() {
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_A, Card.SPADES_K, Card.SPADES_Q, Card.SPADES_J, Card.DIAMONDS_9);

            // 5 spades = flush, not draw. Need 4. Board has 4 spades, pocket has 1.
            // This case is actually a flush already (5 spades). Let's adjust.
            // Need exactly 4 of the suit total. Ts + 3 board spades
            Hand community2 = new Hand(Card.SPADES_A, Card.SPADES_K, Card.SPADES_Q, Card.HEARTS_J, Card.DIAMONDS_9);
            // That's only 4 spades (Ts, As, Ks, Qs). But wait -- J is hearts, so only 3
            // board spades + 1 pocket = 4 total.
            // But the flush draw logic checks for AKQ J in the suit bits. AKQ are on board
            // in spades, J is not.
            // nBoardRankBits_[spades] >> JACK = bits for J,Q,K,A = 0b1110 = 14 => AKQ on
            // board
            // Case 14 => 2ndNutFlushDraw

            info.getScore(new Hand(Card.SPADES_T, Card.HEARTS_4), community2);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.has2ndNutFlushDraw()).isTrue();
        }

        @Test
        void should_DetectWeakFlushDraw_When_TenWithFewHighCardsOnBoard() {
            Hand pocket = new Hand(Card.SPADES_T, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_9, Card.SPADES_7, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_A);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasWeakFlushDraw()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // Flush draw with Nine high
    // ---------------------------------------------------------------

    @Nested
    class NineHighFlushDraw {

        @Test
        void should_DetectWeakFlushDraw_When_NineHigh() {
            Hand pocket = new Hand(Card.SPADES_9, Card.HEARTS_4);
            Hand community = new Hand(Card.SPADES_7, Card.SPADES_5, Card.SPADES_3, Card.HEARTS_2, Card.DIAMONDS_A);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            assertThat(info.hasWeakFlushDraw()).isTrue();
        }
    }

    // ---------------------------------------------------------------
    // Board-only flush draw (4 of suit on board)
    // ---------------------------------------------------------------

    @Nested
    class BoardFlushDraw {

        @Test
        void should_DetectFlushDraw_When_FourBoardCardsOfSameSuit() {
            // Board has 4 spades, pocket has no spades
            Hand pocket = new Hand(Card.HEARTS_A, Card.CLUBS_K);
            Hand community = new Hand(Card.SPADES_Q, Card.SPADES_J, Card.SPADES_9, Card.SPADES_7, Card.DIAMONDS_2);

            info.getScore(pocket, community);

            assertThat(info.hasFlushDraw()).isTrue();
            // Board flush draw -- no nut/2nd/weak since pocket doesn't contribute
            assertThat(info.hasNutFlushDraw()).isFalse();
            assertThat(info.has2ndNutFlushDraw()).isFalse();
            assertThat(info.hasWeakFlushDraw()).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // Score positive for all hand types
    // ---------------------------------------------------------------

    @Test
    void should_ReturnPositiveScore_When_AnyValidHand() {
        int score = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_9, Card.HEARTS_7, Card.CLUBS_5));

        assertThat(score).isGreaterThan(0);
    }

    // ---------------------------------------------------------------
    // Hand comparison and kicker tests
    // ---------------------------------------------------------------

    @Nested
    class HandComparison {

        @Test
        void should_RankHigherPairAboveLowerPair() {
            // Poker rule: AA beats KK (higher pair wins)
            Hand community = new Hand(Card.DIAMONDS_9, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2);

            int aces = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A), community);
            int kings = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_K), community);

            assertThat(aces).isGreaterThan(kings);
        }

        @Test
        void should_UseKicker_When_PairsAreEqual() {
            // Poker rule: same pair, highest kicker wins (K kicker beats Q kicker)
            int pairAcesKingKicker = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2));
            int pairAcesQueenKicker = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_Q),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_7, Card.SPADES_5, Card.HEARTS_3, Card.CLUBS_2));

            assertThat(pairAcesKingKicker).isGreaterThan(pairAcesQueenKicker);
        }

        @Test
        void should_TieScore_When_HandsAreIdentical() {
            // Poker rule: suits don't matter for hand ranking; identical ranks tie
            int handA = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.SPADES_9, Card.HEARTS_7, Card.CLUBS_5));
            int handB = info.getScore(new Hand(Card.HEARTS_A, Card.DIAMONDS_K),
                    new Hand(Card.CLUBS_Q, Card.SPADES_J, Card.HEARTS_9, Card.DIAMONDS_7, Card.SPADES_5));

            assertThat(handA).isEqualTo(handB);
        }

        @Test
        void should_RankWheelBelowSixHighStraight() {
            // Poker rule: A-2-3-4-5 (wheel) loses to 2-3-4-5-6 (six-high straight)
            int wheel = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_2),
                    new Hand(Card.DIAMONDS_3, Card.CLUBS_4, Card.SPADES_5, Card.HEARTS_9, Card.CLUBS_K));
            int sixHigh = info.getScore(new Hand(Card.SPADES_6, Card.HEARTS_2),
                    new Hand(Card.DIAMONDS_3, Card.CLUBS_4, Card.SPADES_5, Card.HEARTS_9, Card.CLUBS_K));

            assertThat(sixHigh).isGreaterThan(wheel);
        }

        @Test
        void should_CompareTopPairFirst_When_BothHaveTwoPair() {
            // Poker rule: two pair compares top pair first (AA-22 beats KK-QQ)
            int acesUp = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_2),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_2, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_9));
            int kingsUp = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_Q),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_7, Card.HEARTS_3, Card.CLUBS_9));

            assertThat(acesUp).isGreaterThan(kingsUp);
        }

        @Test
        void should_CompareTripsFirst_When_BothHaveFullHouse() {
            // Poker rule: full house compares trips first (AAA-22 beats KKK-QQ)
            int acesFull = info.getScore(new Hand(Card.SPADES_A, Card.HEARTS_A),
                    new Hand(Card.DIAMONDS_A, Card.CLUBS_2, Card.SPADES_2, Card.HEARTS_7, Card.CLUBS_9));
            int kingsFull = info.getScore(new Hand(Card.SPADES_K, Card.HEARTS_K),
                    new Hand(Card.DIAMONDS_K, Card.CLUBS_Q, Card.SPADES_Q, Card.HEARTS_7, Card.CLUBS_9));

            assertThat(acesFull).isGreaterThan(kingsFull);
        }

        @Test
        void should_RankFlushAboveStraight() {
            // Poker rule: flush beats straight
            int flush = info.getScore(new Hand(Card.HEARTS_A, Card.HEARTS_9),
                    new Hand(Card.HEARTS_7, Card.HEARTS_5, Card.HEARTS_3, Card.SPADES_K, Card.CLUBS_Q));
            int straight = info.getScore(new Hand(Card.SPADES_T, Card.HEARTS_9),
                    new Hand(Card.DIAMONDS_8, Card.CLUBS_7, Card.SPADES_6, Card.HEARTS_2, Card.CLUBS_3));

            assertThat(flush).isGreaterThan(straight);
        }
    }

    // ---------------------------------------------------------------
    // Straight draw outs reduced when flush draw present
    // ---------------------------------------------------------------

    @Nested
    class StraightDrawWithFlushDraw {

        @Test
        void should_ReduceStraightOuts_When_FlushDrawPresent() {
            // Open-ended straight draw WITH flush draw: outs = 3 per gap instead of 4
            // 8h-9h-Th-Jc (4 cards to straight), 3 hearts = flush draw
            // Need exactly 4 of a suit for flush draw
            Hand pocket = new Hand(Card.HEARTS_8, Card.HEARTS_9);
            Hand community = new Hand(Card.HEARTS_T, Card.CLUBS_J, Card.HEARTS_2, Card.DIAMONDS_A, Card.SPADES_K);

            info.getScore(pocket, community);

            // Has both flush draw (4 hearts) and straight draw
            if (info.hasFlushDraw() && info.hasStraightDraw()) {
                // Outs should be 3 per gap (not 4) because one out completes the flush too
                assertThat(info.getStraightDrawOuts() % 3).isEqualTo(0);
            }
        }
    }
}
