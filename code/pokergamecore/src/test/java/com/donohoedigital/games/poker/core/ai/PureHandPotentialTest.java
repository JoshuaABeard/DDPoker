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

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class PureHandPotentialTest {

    // Index constants matching PureHandPotential internal convention
    private static final int TURN = 0;
    private static final int RIVER = 1;

    // Flop with 3 community cards: 47 remaining cards for turn, C(47,2) = 1081 for
    // river
    private static final int TURN_COMBOS = 47;
    private static final int RIVER_COMBOS = 1081;

    // ---------------------------------------------------------------
    // Pair / made-hand counts
    // ---------------------------------------------------------------

    @Nested
    class PairCounts {

        @Test
        void should_CountOverpair_When_PocketAcesOnLowFlop() {
            // AA on 7-5-2 rainbow - always overpair on turn (47 boards)
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_5, Card.HEARTS_2);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int overpairTurn = php.getHandCount(PureHandPotential.OVERPAIR, TURN);
            int pairTurn = php.getHandCount(PureHandPotential.PAIR, TURN);

            // Overpair should be a subset of pair
            assertThat(overpairTurn).isGreaterThan(0);
            assertThat(overpairTurn).isLessThanOrEqualTo(pairTurn);
        }

        @Test
        void should_CountSetAndTrips_When_PocketPairHitsBoard() {
            // Pocket 8s on K-8-3 rainbow - the 8 on board pairs the pocket
            Hand pocket = new Hand(Card.SPADES_8, Card.HEARTS_8);
            Hand flop = new Hand(Card.DIAMONDS_K, Card.CLUBS_8, Card.HEARTS_3);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            // Already a set (three of a kind via pocket pair), so most turn cards keep set
            int setTurn = php.getHandCount(PureHandPotential.SET, TURN);
            int threeOfAKindTurn = php.getHandCount(PureHandPotential.THREE_OF_A_KIND, TURN);

            assertThat(setTurn).isGreaterThan(0);
            assertThat(setTurn).isLessThanOrEqualTo(threeOfAKindTurn);
        }

        @Test
        void should_CountTopPair_When_HighCardPairsTopBoardCard() {
            // Pocket A-K on K-7-2 rainbow - K pairs top board card
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_K, Card.CLUBS_7, Card.HEARTS_2);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int topPairTurn = php.getHandCount(PureHandPotential.TOP_PAIR, TURN);

            // Already has top pair, most turn cards keep it
            assertThat(topPairTurn).isGreaterThan(0);
        }

        @Test
        void should_CountUnderpair_When_PocketBelowAllBoardCards() {
            // Pocket 3s on A-K-Q rainbow - always underpair
            Hand pocket = new Hand(Card.SPADES_3, Card.HEARTS_3);
            Hand flop = new Hand(Card.DIAMONDS_A, Card.CLUBS_K, Card.HEARTS_Q);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int underpairTurn = php.getHandCount(PureHandPotential.UNDERPAIR, TURN);

            assertThat(underpairTurn).isGreaterThan(0);
        }
    }

    // ---------------------------------------------------------------
    // Flush draw counts
    // ---------------------------------------------------------------

    @Nested
    class FlushDrawCounts {

        @Test
        void should_CountFlushDraw_When_TwoSuitedWithTwoBoardSuited() {
            // Two spades in pocket, two spades on flop = 4 to a flush (flush draw)
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand flop = new Hand(Card.SPADES_7, Card.SPADES_3, Card.HEARTS_9);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int flushDrawTurn = php.getHandCount(PureHandPotential.FLUSH_DRAW, TURN);
            int flushDrawTwoCards = php.getHandCount(PureHandPotential.FLUSH_DRAW_WITH_TWO_CARDS, TURN);

            // With 4 suited cards after flop, a non-spade turn still leaves a flush draw
            assertThat(flushDrawTurn).isGreaterThan(0);
            assertThat(flushDrawTwoCards).isGreaterThan(0);
            assertThat(flushDrawTwoCards).isLessThanOrEqualTo(flushDrawTurn);
        }

        @Test
        void should_CountNutFlushDraw_When_AceHighFlushDraw() {
            // Ace-high suited spades with 2 spades on board
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_6);
            Hand flop = new Hand(Card.SPADES_T, Card.SPADES_4, Card.HEARTS_J);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int nutFlushDrawTurn = php.getHandCount(PureHandPotential.NUT_FLUSH_DRAW_WITH_TWO_CARDS, TURN);

            assertThat(nutFlushDrawTurn).isGreaterThan(0);
        }

        @Test
        void should_CountMadeFlush_When_ThreeSuitedOnBoard() {
            // Two spades in pocket, three spades on flop = made flush
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand flop = new Hand(Card.SPADES_7, Card.SPADES_3, Card.SPADES_9);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int flushTurn = php.getHandCount(PureHandPotential.FLUSH, TURN);

            // Already have a flush, most turn cards keep it
            assertThat(flushTurn).isGreaterThan(0);
        }

        @Test
        void should_NotCountFlushDraw_When_NoSuitMatch() {
            // Pocket cards have no suit matching any two board cards for a draw
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_3, Card.DIAMONDS_9);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int flushDrawTwoCards = php.getHandCount(PureHandPotential.FLUSH_DRAW_WITH_TWO_CARDS, TURN);

            // No flush draw possible with two pocket cards of different suits and no
            // 3-of-suit on board
            assertThat(flushDrawTwoCards).isEqualTo(0);
        }
    }

    // ---------------------------------------------------------------
    // Straight draw counts
    // ---------------------------------------------------------------

    @Nested
    class StraightDrawCounts {

        @Test
        void should_CountOpenEndedStraightDraw_When_EightOuts() {
            // 9-8 on 7-6-2 rainbow = open-ended straight draw (5 or T completes)
            Hand pocket = new Hand(Card.SPADES_9, Card.HEARTS_8);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_6, Card.HEARTS_2);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int straightDrawTurn = php.getHandCount(PureHandPotential.STRAIGHT_DRAW, TURN);
            int eightOutsTurn = php.getHandCount(PureHandPotential.STRAIGHT_DRAW_8_OUTS, TURN);

            // Should detect straight draw with 8 outs
            assertThat(straightDrawTurn).isGreaterThan(0);
            assertThat(eightOutsTurn).isGreaterThan(0);
            assertThat(eightOutsTurn).isLessThanOrEqualTo(straightDrawTurn);
        }

        @Test
        void should_CountGutshotStraightDraw_When_FourOuts() {
            // A-K on Q-J-8 rainbow = gutshot straight draw (T completes)
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.HEARTS_8);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int straightDrawTurn = php.getHandCount(PureHandPotential.STRAIGHT_DRAW, TURN);
            int fourOutsTurn = php.getHandCount(PureHandPotential.STRAIGHT_DRAW_4_OUTS, TURN);

            assertThat(straightDrawTurn).isGreaterThan(0);
            assertThat(fourOutsTurn).isGreaterThan(0);
        }

        @Test
        void should_CountMadeStraight_When_FiveConnected() {
            // 9-8 on 7-6-5 = made straight
            Hand pocket = new Hand(Card.SPADES_9, Card.HEARTS_8);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_6, Card.HEARTS_5);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int straightTurn = php.getHandCount(PureHandPotential.STRAIGHT, TURN);

            assertThat(straightTurn).isGreaterThan(0);
        }
    }

    // ---------------------------------------------------------------
    // Pre-flop handling
    // ---------------------------------------------------------------

    @Nested
    class PreFlop {

        @Test
        void should_ProduceValidCounts_When_NoCommunityCards() {
            // Pre-flop: iterates C(47,3) = 16,215 combinations at index 0
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);

            PureHandPotential php = new PureHandPotential(pocket);

            // With AK offsuit, should get some pair counts and high card counts
            int pairTurn = php.getHandCount(PureHandPotential.PAIR, TURN);
            int highCardTurn = php.getHandCount(PureHandPotential.HIGH_CARD, TURN);

            assertThat(pairTurn).isGreaterThan(0);
            assertThat(highCardTurn).isGreaterThan(0);

            // Total across all major hand types should sum to C(50,3) = 19,600
            // (52 - 2 pocket = 50 remaining, choose 3 for the flop)
            int total = php.getHandCount(PureHandPotential.ROYAL_FLUSH, TURN)
                    + php.getHandCount(PureHandPotential.STRAIGHT_FLUSH, TURN)
                    + php.getHandCount(PureHandPotential.FOUR_OF_A_KIND, TURN)
                    + php.getHandCount(PureHandPotential.FULL_HOUSE, TURN)
                    + php.getHandCount(PureHandPotential.FLUSH, TURN)
                    + php.getHandCount(PureHandPotential.STRAIGHT, TURN)
                    + php.getHandCount(PureHandPotential.THREE_OF_A_KIND, TURN)
                    + php.getHandCount(PureHandPotential.TWO_PAIR, TURN)
                    + php.getHandCount(PureHandPotential.PAIR, TURN)
                    + php.getHandCount(PureHandPotential.HIGH_CARD, TURN);

            assertThat(total).isEqualTo(19600);
        }

        @Test
        void should_HaveZeroRiverCounts_When_PreFlop() {
            // Pre-flop only populates index 0, not index 1
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);

            PureHandPotential php = new PureHandPotential(pocket);

            assertThat(php.getHandCount(PureHandPotential.PAIR, RIVER)).isEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.FLUSH, RIVER)).isEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.STRAIGHT, RIVER)).isEqualTo(0);
        }

        @Test
        void should_CountPocketPairAsOverpair_When_AcesPreFlop() {
            // AA pre-flop: overpair on any flop where both aces remain above the board
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_A);

            PureHandPotential php = new PureHandPotential(pocket);

            int overpairTurn = php.getHandCount(PureHandPotential.OVERPAIR, TURN);
            int pairTurn = php.getHandCount(PureHandPotential.PAIR, TURN);

            // AA is overpair on any flop with no ace (and classified as PAIR hand type)
            assertThat(overpairTurn).isGreaterThan(0);
            assertThat(overpairTurn).isLessThanOrEqualTo(pairTurn);
        }
    }

    // ---------------------------------------------------------------
    // Valid range checks
    // ---------------------------------------------------------------

    @Nested
    class ValidRanges {

        @Test
        void should_HaveNonNegativeCounts_When_AnyHand() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_5, Card.HEARTS_2);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            assertThat(php.getHandCount(PureHandPotential.PAIR, TURN)).isGreaterThanOrEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.FLUSH, TURN)).isGreaterThanOrEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.STRAIGHT, TURN)).isGreaterThanOrEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.FLUSH_DRAW, TURN)).isGreaterThanOrEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.STRAIGHT_DRAW, TURN)).isGreaterThanOrEqualTo(0);
            assertThat(php.getHandCount(PureHandPotential.HIGH_CARD, TURN)).isGreaterThanOrEqualTo(0);
        }

        @Test
        void should_SumHandTypesToTotalCombinations_When_FlopGiven() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_5, Card.HEARTS_2);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            // Turn: 47 remaining cards, each classifies as exactly one hand type
            int turnTotal = php.getHandCount(PureHandPotential.ROYAL_FLUSH, TURN)
                    + php.getHandCount(PureHandPotential.STRAIGHT_FLUSH, TURN)
                    + php.getHandCount(PureHandPotential.FOUR_OF_A_KIND, TURN)
                    + php.getHandCount(PureHandPotential.FULL_HOUSE, TURN)
                    + php.getHandCount(PureHandPotential.FLUSH, TURN)
                    + php.getHandCount(PureHandPotential.STRAIGHT, TURN)
                    + php.getHandCount(PureHandPotential.THREE_OF_A_KIND, TURN)
                    + php.getHandCount(PureHandPotential.TWO_PAIR, TURN)
                    + php.getHandCount(PureHandPotential.PAIR, TURN)
                    + php.getHandCount(PureHandPotential.HIGH_CARD, TURN);

            assertThat(turnTotal).isEqualTo(TURN_COMBOS);

            // River: C(47,2) = 1081 two-card combos
            int riverTotal = php.getHandCount(PureHandPotential.ROYAL_FLUSH, RIVER)
                    + php.getHandCount(PureHandPotential.STRAIGHT_FLUSH, RIVER)
                    + php.getHandCount(PureHandPotential.FOUR_OF_A_KIND, RIVER)
                    + php.getHandCount(PureHandPotential.FULL_HOUSE, RIVER)
                    + php.getHandCount(PureHandPotential.FLUSH, RIVER)
                    + php.getHandCount(PureHandPotential.STRAIGHT, RIVER)
                    + php.getHandCount(PureHandPotential.THREE_OF_A_KIND, RIVER)
                    + php.getHandCount(PureHandPotential.TWO_PAIR, RIVER)
                    + php.getHandCount(PureHandPotential.PAIR, RIVER)
                    + php.getHandCount(PureHandPotential.HIGH_CARD, RIVER);

            assertThat(riverTotal).isEqualTo(RIVER_COMBOS);
        }

        @Test
        void should_HaveSubcategorySumEqualCategory_When_FlushSubtypes() {
            // Flush subcategories should sum to flush total
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand flop = new Hand(Card.SPADES_7, Card.SPADES_3, Card.SPADES_9);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int flushTurn = php.getHandCount(PureHandPotential.FLUSH, TURN);
            int nutFlush = php.getHandCount(PureHandPotential.NUT_FLUSH, TURN);
            int secondNutFlush = php.getHandCount(PureHandPotential.SECOND_NUT_FLUSH, TURN);
            int weakFlush = php.getHandCount(PureHandPotential.WEAK_FLUSH, TURN);

            assertThat(nutFlush + secondNutFlush + weakFlush).isEqualTo(flushTurn);
        }

        @Test
        void should_HaveSubcategorySumEqualCategory_When_StraightDrawSubtypes() {
            // Straight draw subtypes should sum to straight draw total
            Hand pocket = new Hand(Card.SPADES_9, Card.HEARTS_8);
            Hand flop = new Hand(Card.DIAMONDS_7, Card.CLUBS_6, Card.HEARTS_2);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int drawTotal = php.getHandCount(PureHandPotential.STRAIGHT_DRAW, TURN);
            int eightOuts = php.getHandCount(PureHandPotential.STRAIGHT_DRAW_8_OUTS, TURN);
            int sixOuts = php.getHandCount(PureHandPotential.STRAIGHT_DRAW_6_OUTS, TURN);
            int fourOuts = php.getHandCount(PureHandPotential.STRAIGHT_DRAW_4_OUTS, TURN);
            int threeOuts = php.getHandCount(PureHandPotential.STRAIGHT_DRAW_3_OUTS, TURN);

            assertThat(eightOuts + sixOuts + fourOuts + threeOuts).isEqualTo(drawTotal);
        }

        @Test
        void should_HaveDrawCountsWithinBounds_When_FlushDrawOnFlop() {
            Hand pocket = new Hand(Card.SPADES_A, Card.SPADES_K);
            Hand flop = new Hand(Card.SPADES_7, Card.SPADES_3, Card.HEARTS_9);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int flushDrawTurn = php.getHandCount(PureHandPotential.FLUSH_DRAW, TURN);
            int flushDrawRiver = php.getHandCount(PureHandPotential.FLUSH_DRAW, RIVER);

            assertThat(flushDrawTurn).isBetween(0, TURN_COMBOS);
            assertThat(flushDrawRiver).isBetween(0, RIVER_COMBOS);
        }
    }

    // ---------------------------------------------------------------
    // Two-card river index for flop
    // ---------------------------------------------------------------

    @Nested
    class RiverIndex {

        @Test
        void should_PopulateBothIndexes_When_FlopCommunity() {
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.HEARTS_9);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            // Both turn and river should have nonzero pair counts for AK on Q-J-9
            int pairTurn = php.getHandCount(PureHandPotential.PAIR, TURN);
            int pairRiver = php.getHandCount(PureHandPotential.PAIR, RIVER);

            assertThat(pairTurn).isGreaterThan(0);
            assertThat(pairRiver).isGreaterThan(0);
        }

        @Test
        void should_HaveStraightOnRiver_When_GutshotWithTwoCards() {
            // A-K on Q-J-8: gutshot needing a T; with two cards more likely
            Hand pocket = new Hand(Card.SPADES_A, Card.HEARTS_K);
            Hand flop = new Hand(Card.DIAMONDS_Q, Card.CLUBS_J, Card.HEARTS_8);

            PureHandPotential php = new PureHandPotential(pocket, flop);

            int straightRiver = php.getHandCount(PureHandPotential.STRAIGHT, RIVER);

            // With two cards to come, at least some combos should hit the straight
            assertThat(straightRiver).isGreaterThan(0);
        }
    }
}
