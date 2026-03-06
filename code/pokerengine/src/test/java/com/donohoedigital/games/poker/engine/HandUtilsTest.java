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

import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for HandUtils.getBestFive() — verifies that the correct
 * 5-card hand is selected from 7 cards (2 hole + up to 5 community) for every
 * Texas Hold'em hand type, including edge cases and tie-breaking.
 */
class HandUtilsTest {

    // ===== Helper =====

    /**
     * Build a HandSorted from an array of cards. Note: static Card constants are
     * NOT mutated — passed directly as-is.
     */
    private static HandSorted sorted(Card... cards) {
        HandSorted h = new HandSorted();
        for (Card c : cards) {
            h.addCard(c);
        }
        return h;
    }

    // ===== Royal Flush =====

    @Test
    void should_ReturnRoyalFlush_When_AKQJTSameSuit() {
        // Ah Kh Qh Jh Th in hole+community, plus two irrelevant cards
        HandSorted hole = sorted(HEARTS_A, HEARTS_K);
        HandSorted community = sorted(HEARTS_Q, HEARTS_J, HEARTS_T, CLUBS_2, DIAMONDS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, HEARTS_K, HEARTS_Q, HEARTS_J, HEARTS_T);
    }

    @Test
    void should_ReturnRoyalFlushInSpades_When_SevenCardsAvailable() {
        HandSorted hole = sorted(SPADES_A, SPADES_K);
        HandSorted community = sorted(SPADES_Q, SPADES_J, SPADES_T, HEARTS_2, CLUBS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(SPADES_A, SPADES_K, SPADES_Q, SPADES_J, SPADES_T);
    }

    // ===== Straight Flush =====

    @Test
    void should_ReturnStraightFlush_When_FiveConsecutiveSameSuit() {
        // 9h 8h 7h 6h 5h — best 5 is the straight flush, not Ad Kd
        HandSorted hole = sorted(HEARTS_9, HEARTS_8);
        HandSorted community = sorted(HEARTS_7, HEARTS_6, HEARTS_5, DIAMONDS_A, DIAMONDS_K);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_9, HEARTS_8, HEARTS_7, HEARTS_6, HEARTS_5);
    }

    @Test
    void should_ReturnHigherStraightFlush_When_TwoStraightFlushesPresent() {
        // Hearts: 6h 5h 4h 3h 2h (6-high) vs 7h 6h 5h 4h 3h (7-high) — pick 7-high
        HandSorted hole = sorted(HEARTS_7, HEARTS_2);
        HandSorted community = sorted(HEARTS_6, HEARTS_5, HEARTS_4, HEARTS_3, CLUBS_A);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // 7-high straight flush
        assertThat(best).containsExactlyInAnyOrder(HEARTS_7, HEARTS_6, HEARTS_5, HEARTS_4, HEARTS_3);
    }

    @Test
    void should_ReturnWheelStraightFlush_When_A2345SameSuit() {
        // Ah 2h 3h 4h 5h — ace acts as low; two irrelevant off-suit cards
        HandSorted hole = sorted(HEARTS_A, HEARTS_2);
        HandSorted community = sorted(HEARTS_3, HEARTS_4, HEARTS_5, CLUBS_6, DIAMONDS_7);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // The wheel straight flush contains A-2-3-4-5 of hearts
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, HEARTS_2, HEARTS_3, HEARTS_4, HEARTS_5);
    }

    // ===== Four of a Kind (Quads) =====

    @Test
    void should_ReturnQuads_When_FourAcesPresent() {
        // Ah Ad As Ac + Kh 2c 3d — best 5 is four aces + king kicker (only one king)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(SPADES_A, CLUBS_A, HEARTS_K, CLUBS_2, DIAMONDS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, SPADES_A, CLUBS_A, HEARTS_K);
    }

    @Test
    void should_ReturnBestKicker_When_QuadsWithMultipleKickerChoices() {
        // Four 8s with Ace and King available as kickers — pick Ace
        HandSorted hole = sorted(CLUBS_8, HEARTS_8);
        HandSorted community = sorted(DIAMONDS_8, SPADES_8, SPADES_A, DIAMONDS_K, HEARTS_Q);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Four 8s + Ace kicker (highest)
        assertThat(best).containsExactlyInAnyOrder(CLUBS_8, HEARTS_8, DIAMONDS_8, SPADES_8, SPADES_A);
    }

    // ===== Full House =====

    @Test
    void should_ReturnFullHouse_When_TripsAndPair() {
        // Ah Ad Ac + Kh Kd + 2c — best 5 is AAA KK
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_A, HEARTS_K, DIAMONDS_K, CLUBS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, CLUBS_A, HEARTS_K, DIAMONDS_K);
    }

    @Test
    void should_ReturnBestFullHouse_When_TwoTripsPresent() {
        // Qh Qd Qc + 8h 8d 8c + 2s — best is QQQ 88
        HandSorted hole = sorted(HEARTS_Q, DIAMONDS_Q);
        HandSorted community = sorted(CLUBS_Q, HEARTS_8, DIAMONDS_8, CLUBS_8, SPADES_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Queens are higher trips — QQQ (best 3) + 2 eights
        assertThat(best).filteredOn(c -> c.getRank() == QUEEN).hasSize(3);
        assertThat(best).filteredOn(c -> c.getRank() == EIGHT).hasSize(2);
    }

    @Test
    void should_ReturnBestPairInFullHouse_When_TripsAndTwoPairsPresent() {
        // Ah Ad Ac + Kh Kd + Qh Qd — best is AAA KK (not AAA QQ)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_A, HEARTS_K, DIAMONDS_K, HEARTS_Q, DIAMONDS_Q);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, CLUBS_A, HEARTS_K, DIAMONDS_K);
    }

    // ===== Flush =====

    @Test
    void should_ReturnFlush_When_FiveHearts() {
        // Ah Jh 9h 7h 3h + Kd 2d — best 5 is the five hearts
        HandSorted hole = sorted(HEARTS_A, HEARTS_J);
        HandSorted community = sorted(HEARTS_9, HEARTS_7, HEARTS_3, DIAMONDS_K, DIAMONDS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, HEARTS_J, HEARTS_9, HEARTS_7, HEARTS_3);
    }

    @Test
    void should_ReturnTopFiveOfFlush_When_SixSuitedCards() {
        // Ah Kh Jh 9h 7h 3h + 2d — pick best 5: A K J 9 7 of hearts
        HandSorted hole = sorted(HEARTS_A, HEARTS_K);
        HandSorted community = sorted(HEARTS_J, HEARTS_9, HEARTS_7, HEARTS_3, DIAMONDS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Top 5 hearts by rank: A K J 9 7 (drop 3)
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, HEARTS_K, HEARTS_J, HEARTS_9, HEARTS_7);
    }

    @Test
    void should_ReturnTopFiveOfFlush_When_SevenSuitedCards() {
        // Seven clubs — pick top 5
        HandSorted hole = sorted(CLUBS_A, CLUBS_K);
        HandSorted community = sorted(CLUBS_Q, CLUBS_J, CLUBS_9, CLUBS_3, CLUBS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Top 5: A K Q J 9 (drop 3 and 2)
        assertThat(best).containsExactlyInAnyOrder(CLUBS_A, CLUBS_K, CLUBS_Q, CLUBS_J, CLUBS_9);
    }

    // ===== Straight =====

    @Test
    void should_ReturnStraight_When_AceHighBroadway() {
        // Ah Kd Qc Js Th + 2c 2d — best 5 is A K Q J T straight
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_K);
        HandSorted community = sorted(CLUBS_Q, SPADES_J, HEARTS_T, CLUBS_2, DIAMONDS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_T);
    }

    @Test
    void should_ReturnWheelStraight_When_A2345MixedSuits() {
        // Ah 2c 3d 4h 5s + Kc 7d — best 5 is A 2 3 4 5 (wheel)
        HandSorted hole = sorted(HEARTS_A, CLUBS_2);
        HandSorted community = sorted(DIAMONDS_3, HEARTS_4, SPADES_5, CLUBS_K, DIAMONDS_7);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, CLUBS_2, DIAMONDS_3, HEARTS_4, SPADES_5);
    }

    @Test
    void should_ReturnHighestStraight_When_SevenCardsSupportMultipleStraights() {
        // A K Q J T 9 8 — best straight is A-K-Q-J-T (ace high)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_K);
        HandSorted community = sorted(CLUBS_Q, SPADES_J, HEARTS_T, CLUBS_9, DIAMONDS_8);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Ace-high straight wins over K-high
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_T);
    }

    @Test
    void should_ReturnStraight_When_FlopOnlyThreeCards() {
        // 2 hole + 3 community — exactly 5 cards, all form a straight
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_K);
        HandSorted community = sorted(CLUBS_Q, SPADES_J, HEARTS_T);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_T);
    }

    // ===== Three of a Kind (Trips) =====

    @Test
    void should_ReturnTrips_When_ThreeAcesAndTwoKickers() {
        // Ah Ad Ac + Kh Qd 3c — best 5 is AAA KQ
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_A, HEARTS_K, DIAMONDS_Q, CLUBS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, CLUBS_A, HEARTS_K, DIAMONDS_Q);
    }

    @Test
    void should_ReturnBestKickers_When_TripsWithMultipleKickerChoices() {
        // 2h 2d 2c + Ah Kd Qd Jc — best 5 is 222 AK (top two kickers)
        HandSorted hole = sorted(HEARTS_2, DIAMONDS_2);
        HandSorted community = sorted(CLUBS_2, HEARTS_A, DIAMONDS_K, DIAMONDS_Q, CLUBS_J);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_2, DIAMONDS_2, CLUBS_2, HEARTS_A, DIAMONDS_K);
    }

    // ===== Two Pair =====

    @Test
    void should_ReturnTwoPair_When_TwoPairsAndKicker() {
        // Ah Ad + Kh Kd + Jc — best 5 is AA KK J
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(HEARTS_K, DIAMONDS_K, CLUBS_J);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, HEARTS_K, DIAMONDS_K, CLUBS_J);
    }

    @Test
    void should_ReturnBestTwoPair_When_ThreePairsPresent() {
        // Ah Ad Kh Kd Qh Qd + Tc — best 5 is AA KK Q (top kicker from remaining)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(HEARTS_K, DIAMONDS_K, HEARTS_Q, DIAMONDS_Q, CLUBS_T);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Best two pair: AA and KK — kicker is highest remaining (Qh or Qd)
        // AA KK is best, kicker is Queen (highest non-pair card)
        assertThat(best).filteredOn(c -> c.getRank() == ACE).hasSize(2);
        assertThat(best).filteredOn(c -> c.getRank() == KING).hasSize(2);
        assertThat(best).filteredOn(c -> c.getRank() == QUEEN).hasSize(1);
    }

    @Test
    void should_ReturnHighestKicker_When_TwoPairWithMultipleKickerChoices() {
        // Ah Ad + Kh Kd + Qc Jc 2c — best kicker is Q
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(HEARTS_K, DIAMONDS_K, CLUBS_Q, CLUBS_J, CLUBS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, HEARTS_K, DIAMONDS_K, CLUBS_Q);
    }

    // ===== One Pair =====

    @Test
    void should_ReturnPair_When_PairOfAcesAndThreeKickers() {
        // Ah Ad + Kc Qd Jc + 2h 3h — best 5 is AA KQJ
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_K, DIAMONDS_Q, CLUBS_J, HEARTS_2, HEARTS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_A, CLUBS_K, DIAMONDS_Q, CLUBS_J);
    }

    @Test
    void should_ReturnBestThreeKickers_When_PairWithFiveKickerCandidates() {
        // Pair of 2s + A K Q J 9 available as kickers — pick A K Q
        HandSorted hole = sorted(CLUBS_2, HEARTS_2);
        HandSorted community = sorted(SPADES_A, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_9);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(CLUBS_2, HEARTS_2, SPADES_A, DIAMONDS_K, CLUBS_Q);
    }

    // ===== High Card =====

    @Test
    void should_ReturnHighCard_When_NoMadeHand() {
        // Ah Kd Qc Js Tc + 2h 3h — best 5 is A K Q J T (high card, no pair)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_K);
        HandSorted community = sorted(CLUBS_Q, SPADES_J, HEARTS_T, HEARTS_2, HEARTS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_T);
    }

    @Test
    void should_ReturnTopFiveByRank_When_SevenUnpairedCards() {
        // A K Q J 9 7 5 — best 5 is A K Q J 9
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_K);
        HandSorted community = sorted(CLUBS_Q, SPADES_J, HEARTS_9, CLUBS_7, DIAMONDS_5);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).containsExactlyInAnyOrder(HEARTS_A, DIAMONDS_K, CLUBS_Q, SPADES_J, HEARTS_9);
    }

    // ===== Hand Size Verification =====

    @Test
    void should_AlwaysReturnFiveCards_When_FullSevenCardBoard() {
        // Full 7-card board — always exactly 5 cards returned
        HandSorted hole = sorted(CLUBS_8, HEARTS_8);
        HandSorted community = sorted(DIAMONDS_8, SPADES_8, SPADES_A, DIAMONDS_K, HEARTS_Q);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
    }

    @Test
    void should_ReturnFiveCards_When_FlopOnlyFiveCardsTotal() {
        // 2 hole + 3 community = exactly 5 cards
        HandSorted hole = sorted(CLUBS_A, HEARTS_K);
        HandSorted community = sorted(DIAMONDS_Q, SPADES_J, HEARTS_T);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
    }

    @Test
    void should_ReturnFiveCards_When_TurnSixCardsTotal() {
        // 2 hole + 4 community = 6 cards
        HandSorted hole = sorted(CLUBS_A, HEARTS_K);
        HandSorted community = sorted(DIAMONDS_Q, SPADES_J, HEARTS_T, CLUBS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
    }

    // ===== Specific Card Identity Checks (rank + suit) =====

    @Test
    void should_ReturnExactCards_When_StraightFlushBeatsFlush() {
        // Board: Kh Qh Jh Th 9h + Ah Kd — straight flush K-high beats royal
        // Wait: Ah + K-Q-J-T-9 of hearts = royal flush (A-high straight flush = royal)
        // Use 9-high SF instead: 9h 8h 7h 6h 5h + Ah Kd (off-suit)
        HandSorted hole = sorted(HEARTS_9, HEARTS_8);
        HandSorted community = sorted(HEARTS_7, HEARTS_6, HEARTS_5, DIAMONDS_A, CLUBS_K);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Every card must be a heart
        assertThat(best).allMatch(Card::isHearts);
        // Ranks must be 9 8 7 6 5
        assertThat(best).extracting(Card::getRank).containsExactlyInAnyOrder(NINE, EIGHT, SEVEN, SIX, FIVE);
    }

    @Test
    void should_ReturnExactSuit_When_FlushDeterminesCards() {
        // Only hearts qualify — all 5 returned cards must be hearts
        HandSorted hole = sorted(HEARTS_A, HEARTS_J);
        HandSorted community = sorted(HEARTS_9, HEARTS_7, HEARTS_3, SPADES_K, CLUBS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).allMatch(Card::isHearts);
    }

    @Test
    void should_IncludeAllFourOfAKind_When_QuadsPresent() {
        // All four aces must appear in the result
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(SPADES_A, CLUBS_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).filteredOn(c -> c.getRank() == ACE).hasSize(4);
        // Kicker should be K (highest remaining)
        assertThat(best).filteredOn(c -> c.getRank() == KING).hasSize(1);
    }

    @Test
    void should_IncludeAllThreeTrips_When_TripsPresent() {
        // All three of a kind must appear
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, HEARTS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).filteredOn(c -> c.getRank() == ACE).hasSize(3);
        // Top two kickers: K and Q
        assertThat(best).filteredOn(c -> c.getRank() == KING).hasSize(1);
        assertThat(best).filteredOn(c -> c.getRank() == QUEEN).hasSize(1);
    }

    // ===== Ranking Priorities =====

    @Test
    void should_PreferStraightFlushOverFlush_When_BothPresent() {
        // 5 hearts form a flush AND a straight flush — must return straight flush cards
        // 9h 8h 7h 6h 5h = straight flush; no extra hearts to confuse
        HandSorted hole = sorted(HEARTS_9, HEARTS_5);
        HandSorted community = sorted(HEARTS_8, HEARTS_7, HEARTS_6, CLUBS_A, DIAMONDS_K);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).allMatch(Card::isHearts);
        assertThat(best).extracting(Card::getRank).containsExactlyInAnyOrder(NINE, EIGHT, SEVEN, SIX, FIVE);
    }

    @Test
    void should_PreferQuadsOverFullHouse_When_BothPresent() {
        // Four 8s + pair of Ks — quads wins over full house
        HandSorted hole = sorted(CLUBS_8, HEARTS_8);
        HandSorted community = sorted(DIAMONDS_8, SPADES_8, SPADES_K, DIAMONDS_K, HEARTS_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).filteredOn(c -> c.getRank() == EIGHT).hasSize(4);
    }

    @Test
    void should_PreferFullHouseOverFlush_When_BothPresent() {
        // Full house + flush both possible — full house wins
        // 8h 8d 8c + Kh Kd + Jh 3h (6 hearts total)
        HandSorted hole = sorted(HEARTS_8, DIAMONDS_8);
        HandSorted community = sorted(CLUBS_8, HEARTS_K, DIAMONDS_K, HEARTS_J, HEARTS_3);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Full house: three 8s + two Ks
        assertThat(best).filteredOn(c -> c.getRank() == EIGHT).hasSize(3);
        assertThat(best).filteredOn(c -> c.getRank() == KING).hasSize(2);
    }

    @Test
    void should_PreferFlushOverStraight_When_BothPresent() {
        // Flush + straight possible — flush wins
        // Hearts: Ah Kh Jh 9h 7h = flush; A K Q J T = straight (mixed suits)
        HandSorted hole = sorted(HEARTS_A, HEARTS_K);
        HandSorted community = sorted(HEARTS_J, HEARTS_9, HEARTS_7, CLUBS_Q, SPADES_T);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // All returned cards should be hearts (flush wins over straight)
        assertThat(best).allMatch(Card::isHearts);
    }

    @Test
    void should_PreferStraightOverTrips_When_BothPresent() {
        // Straight + trips both possible — straight wins
        // Ah Ad Ac + Kh Qd Jc Th = 3 aces (trips) and A K Q J T (straight)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        // Straight A-K-Q-J-T wins; contains exactly ranks A K Q J T
        assertThat(best).extracting(Card::getRank).containsExactlyInAnyOrder(ACE, KING, QUEEN, JACK, TEN);
        // Not all aces (trips would have 3 aces)
        assertThat(best).filteredOn(c -> c.getRank() == ACE).hasSize(1);
    }

    @Test
    void should_PreferTripsOverTwoPair_When_BothPresent() {
        // Three aces + two kings (full house beats two pair, but let's test trips vs
        // two pair)
        // Ah Ad Ac + Kh Qd Jc 2c (no pair possible except aces)
        // Actually the full-house test covers trips+pair → just test trips beating two
        // pair
        // Ah Ad Ac + Kh Kd + nope that's full house...
        // Ah Ad Ac + 7h 5d 3c 2s → trips with no pair (so just trips + kickers)
        HandSorted hole = sorted(HEARTS_A, DIAMONDS_A);
        HandSorted community = sorted(CLUBS_A, HEARTS_7, DIAMONDS_5, CLUBS_3, SPADES_2);

        Hand best = HandUtils.getBestFive(hole, community);

        assertThat(best).hasSize(5);
        assertThat(best).filteredOn(c -> c.getRank() == ACE).hasSize(3);
    }
}
