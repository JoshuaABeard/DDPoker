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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.HandInfoFaster;
import com.donohoedigital.games.poker.engine.HandSorted;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandInfo scoring and hand ranking calculations.
 */
class HandInfoTest {

    @Test
    void should_CalculateCorrectScores_When_EvaluatingPokerHands() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        verify("Royal Flush (clubs)        ", 10000014, CLUBS_A, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2,
                HEARTS_K);
        verify("Royal Flush (spades)       ", 10000014, SPADES_A, SPADES_J, SPADES_K, SPADES_Q, SPADES_T, SPADES_2,
                HEARTS_K);
        verify("Straight Flush K, (hearts) ", 9000013, HEARTS_9, HEARTS_J, HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T,
                HEARTS_3);
        verify("Straight Flush K, A str    ", 9000013, HEARTS_9, HEARTS_J, HEARTS_K, HEARTS_Q, CLUBS_Q, HEARTS_T,
                CLUBS_A);
        verify("Straight Flush (+1 hearts) ", 9000006, HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5, HEARTS_6,
                SPADES_A);
        verify("Straight Flush (low hearts)", 9000005, HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5, CLUBS_A,
                SPADES_A);
        verify("Quads                      ", 8000135, CLUBS_8, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7,
                HEARTS_7);
        verify("Full House (two trips)     ", 7000200, CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_Q,
                HEARTS_7);
        verify("Full House                 ", 7000140, CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_7,
                HEARTS_7);
        verify("Full House/Trips           ", 7000135, CLUBS_K, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_7,
                HEARTS_7);
        verify("Flush (clubs)              ", 6904104, CLUBS_8, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        verify("Flush (clubs lower kicker) ", 6904103, CLUBS_7, CLUBS_J, CLUBS_K, CLUBS_Q, CLUBS_T, SPADES_2, HEARTS_3);
        verify("Straight (6 high)          ", 5000006, CLUBS_2, HEARTS_A, HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_6,
                SPADES_A);
        verify("Straight (5 high)          ", 5000005, CLUBS_2, HEARTS_A, HEARTS_3, SPADES_4, DIAMONDS_5, CLUBS_A,
                SPADES_A);
        verify("Straight/Two Pair          ", 5000008, CLUBS_8, HEARTS_8, DIAMONDS_6, SPADES_5, SPADES_7, DIAMONDS_7,
                HEARTS_4);
        verify("Trips                      ", 4002267, CLUBS_K, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_J,
                HEARTS_3);
        verify("Pair                       ", 2028376, CLUBS_8, HEARTS_A, DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2,
                HEARTS_6);
        verify("High Card                  ", 1973958, CLUBS_8, HEARTS_A, DIAMONDS_6, SPADES_5, SPADES_K, DIAMONDS_2,
                HEARTS_Q);
    }

    @Test
    void should_RankQuadsHigher_When_ComparingQuadsToFullHouse() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo quads = createHandInfo("Quads", CLUBS_8, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_7, DIAMONDS_2,
                HEARTS_3);
        HandInfo fullHouse = createHandInfo("Full House", CLUBS_A, HEARTS_A, DIAMONDS_A, CLUBS_K, SPADES_K, DIAMONDS_2,
                HEARTS_3);

        assertThat(quads.getScore()).isGreaterThan(fullHouse.getScore());
    }

    @Test
    void should_RankFullHouseHigher_When_ComparingFullHouseToFlush() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo fullHouse = createHandInfo("Full House", CLUBS_8, HEARTS_8, DIAMONDS_8, CLUBS_Q, SPADES_Q, DIAMONDS_2,
                HEARTS_3);
        HandInfo flush = createHandInfo("Flush", CLUBS_A, CLUBS_K, CLUBS_Q, CLUBS_J, CLUBS_9, SPADES_2, HEARTS_3);

        assertThat(fullHouse.getScore()).isGreaterThan(flush.getScore());
    }

    @Test
    void should_RankFlushHigher_When_ComparingFlushToStraight() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo flush = createHandInfo("Flush", CLUBS_2, CLUBS_4, CLUBS_6, CLUBS_8, CLUBS_T, SPADES_A, HEARTS_K);
        HandInfo straight = createHandInfo("Straight", CLUBS_9, HEARTS_T, DIAMONDS_J, SPADES_Q, CLUBS_K, SPADES_2,
                HEARTS_3);

        assertThat(flush.getScore()).isGreaterThan(straight.getScore());
    }

    @Test
    void should_RankStraightHigher_When_ComparingStrightToTrips() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo straight = createHandInfo("Straight", CLUBS_2, HEARTS_3, DIAMONDS_4, SPADES_5, CLUBS_6, SPADES_A,
                HEARTS_K);
        HandInfo trips = createHandInfo("Trips", CLUBS_A, HEARTS_A, DIAMONDS_A, SPADES_K, CLUBS_Q, SPADES_2, HEARTS_3);

        assertThat(straight.getScore()).isGreaterThan(trips.getScore());
    }

    @Test
    void should_RankTripsHigher_When_ComparingTripsTwoTwoPair() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo trips = createHandInfo("Trips", CLUBS_2, HEARTS_2, DIAMONDS_2, SPADES_A, CLUBS_K, SPADES_Q, HEARTS_J);
        HandInfo twoPair = createHandInfo("Two Pair", CLUBS_A, HEARTS_A, DIAMONDS_K, SPADES_K, CLUBS_Q, SPADES_2,
                HEARTS_3);

        assertThat(trips.getScore()).isGreaterThan(twoPair.getScore());
    }

    @Test
    void should_RankTwoPairHigher_When_ComparingTwoPairToPair() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo twoPair = createHandInfo("Two Pair", CLUBS_2, HEARTS_2, DIAMONDS_3, SPADES_3, CLUBS_A, SPADES_K,
                HEARTS_Q);
        HandInfo pair = createHandInfo("Pair", CLUBS_A, HEARTS_A, DIAMONDS_K, SPADES_Q, CLUBS_J, SPADES_9, HEARTS_7);

        assertThat(twoPair.getScore()).isGreaterThan(pair.getScore());
    }

    @Test
    void should_RankPairHigher_When_ComparingPairToHighCard() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo pair = createHandInfo("Pair", CLUBS_2, HEARTS_2, DIAMONDS_A, SPADES_K, CLUBS_Q, SPADES_J, HEARTS_T);
        HandInfo highCard = createHandInfo("High Card", CLUBS_A, HEARTS_K, DIAMONDS_Q, SPADES_J, CLUBS_9, SPADES_7,
                HEARTS_5);

        assertThat(pair.getScore()).isGreaterThan(highCard.getScore());
    }

    @Test
    void should_RankHigherQuads_When_QuadsDiffer() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo quadsAces = createHandInfo("Quads Aces", CLUBS_A, HEARTS_A, DIAMONDS_A, SPADES_A, CLUBS_K, SPADES_2,
                HEARTS_3);
        HandInfo quadsKings = createHandInfo("Quads Kings", CLUBS_K, HEARTS_K, DIAMONDS_K, SPADES_K, CLUBS_A, SPADES_2,
                HEARTS_3);

        assertThat(quadsAces.getScore()).isGreaterThan(quadsKings.getScore());
    }

    @Test
    void should_RankHigherFullHouse_When_TripsPartDiffers() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo fullHouseAces = createHandInfo("FH Aces", CLUBS_A, HEARTS_A, DIAMONDS_A, SPADES_K, CLUBS_K, SPADES_2,
                HEARTS_3);
        HandInfo fullHouseKings = createHandInfo("FH Kings", CLUBS_K, HEARTS_K, DIAMONDS_K, SPADES_A, CLUBS_A, SPADES_2,
                HEARTS_3);

        assertThat(fullHouseAces.getScore()).isGreaterThan(fullHouseKings.getScore());
    }

    @Test
    void should_RankHigherStraight_When_HighCardDiffers() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo straightNine = createHandInfo("Straight 9", CLUBS_5, HEARTS_6, DIAMONDS_7, SPADES_8, CLUBS_9, SPADES_A,
                HEARTS_K);
        HandInfo straightEight = createHandInfo("Straight 8", CLUBS_4, HEARTS_5, DIAMONDS_6, SPADES_7, CLUBS_8,
                SPADES_A, HEARTS_K);

        assertThat(straightNine.getScore()).isGreaterThan(straightEight.getScore());
    }

    @Test
    void should_UseKickerForTieBreak_When_PairsSame() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo pairAcesKingKicker = createHandInfo("Pair A K", CLUBS_A, HEARTS_A, DIAMONDS_K, SPADES_Q, CLUBS_J,
                SPADES_2, HEARTS_3);
        HandInfo pairAcesQueenKicker = createHandInfo("Pair A Q", CLUBS_A, HEARTS_A, DIAMONDS_Q, SPADES_J, CLUBS_T,
                SPADES_2, HEARTS_3);

        assertThat(pairAcesKingKicker.getScore()).isGreaterThan(pairAcesQueenKicker.getScore());
    }

    @Test
    void should_TieWhenIdentical_When_HandsHaveSameScore() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo hand1 = createHandInfo("Hand 1", CLUBS_A, HEARTS_K, DIAMONDS_Q, SPADES_J, CLUBS_T, SPADES_2, HEARTS_3);
        HandInfo hand2 = createHandInfo("Hand 2", DIAMONDS_A, SPADES_K, CLUBS_Q, HEARTS_J, DIAMONDS_T, CLUBS_2,
                SPADES_3);

        assertThat(hand1.getScore()).isEqualTo(hand2.getScore());
    }

    @Test
    void should_IdentifyBestFiveCards_When_SevenCardsAvailable() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Should use the Ace-high flush, not the pair of Aces
        HandInfo info = createHandInfo("Flush", CLUBS_A, CLUBS_K, CLUBS_Q, CLUBS_J, CLUBS_9, SPADES_A, HEARTS_A);

        assertThat(info.getScore()).isGreaterThan(6000000).isLessThan(7000000); // Flush range is 6,000,000+
    }

    @Test
    void should_CreateHandInfoFromCards_When_ValidCardsProvided() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo info = createHandInfo("Test", CLUBS_A, HEARTS_K, DIAMONDS_Q, SPADES_J, CLUBS_T, SPADES_9, HEARTS_8);

        assertThat(info).isNotNull();
        assertThat(info.getScore()).isGreaterThan(0);
    }

    @Test
    void should_HandleMinimumCards_When_FiveCardsProvided() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo info = createHandInfo("Five Cards", CLUBS_A, HEARTS_K, DIAMONDS_Q, SPADES_J, CLUBS_T, null, null);

        assertThat(info).isNotNull();
        assertThat(info.getScore()).isGreaterThan(0);
    }

    @Test
    void should_EvaluateRoyalFlushTie_When_BothHaveRoyalFlush() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        HandInfo royal1 = createHandInfo("Royal 1", CLUBS_A, CLUBS_K, CLUBS_Q, CLUBS_J, CLUBS_T, SPADES_2, HEARTS_3);
        HandInfo royal2 = createHandInfo("Royal 2", DIAMONDS_A, DIAMONDS_K, DIAMONDS_Q, DIAMONDS_J, DIAMONDS_T,
                SPADES_2, HEARTS_3);

        assertThat(royal1.getScore()).isEqualTo(royal2.getScore());
    }

    private static void verify(String name, int expected, Card c1, Card c2, Card c3, Card c4, Card c5, Card c6,
            Card c7) {
        HandSorted hand = new HandSorted();
        PokerPlayer testPlayer = new PokerPlayer(0, "Test", true);
        testPlayer.setName(name);

        if (c1 != null)
            hand.addCard(c1);
        if (c2 != null)
            hand.addCard(c2);
        if (c3 != null)
            hand.addCard(c3);
        if (c4 != null)
            hand.addCard(c4);
        if (c5 != null)
            hand.addCard(c5);
        if (c6 != null)
            hand.addCard(c6);
        if (c7 != null)
            hand.addCard(c7);

        HandInfo info = new HandInfo(testPlayer, hand, null);
        int fastScore = new HandInfoFast().getScore(info.getHole(), info.getCommunity());
        int fasterScore = new HandInfoFaster().getScore(info.getHole(), info.getCommunity());

        System.out.println(
                "====================================================================================================================");
        System.out.println(name + " - " + info + " fastscore=" + fastScore + " fasterScore=" + fasterScore);
        System.out.println(info.toStringDebug());
        System.out.println();

        assertThat(fastScore).as("Score for %s doesn't match expected", name).isEqualTo(expected);
        assertThat(fastScore).as("Fast score for %s doesn't match info score", name).isEqualTo(info.getScore());
        assertThat(fasterScore).as("Faster score for %s doesn't match info score", name).isEqualTo(info.getScore());
    }

    private static HandInfo createHandInfo(String name, Card c1, Card c2, Card c3, Card c4, Card c5, Card c6, Card c7) {
        HandSorted hand = new HandSorted();
        PokerPlayer testPlayer = new PokerPlayer(0, "Test", true);
        testPlayer.setName(name);

        if (c1 != null)
            hand.addCard(c1);
        if (c2 != null)
            hand.addCard(c2);
        if (c3 != null)
            hand.addCard(c3);
        if (c4 != null)
            hand.addCard(c4);
        if (c5 != null)
            hand.addCard(c5);
        if (c6 != null)
            hand.addCard(c6);
        if (c7 != null)
            hand.addCard(c7);

        return new HandInfo(testPlayer, hand, null);
    }
}
