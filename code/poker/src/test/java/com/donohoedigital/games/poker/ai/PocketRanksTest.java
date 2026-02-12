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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PocketRanks raw hand strength calculation - THE critical AI metric.
 */
class PocketRanksTest {
    // ========================================
    // getInstance() Validation Tests
    // ========================================

    @Test
    void should_ThrowError_When_CommunityIsNull() {
        assertThatThrownBy(() -> PocketRanks.getInstance(null)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("null community");
    }

    @Test
    void should_ThrowError_When_CommunityIsEmpty() {
        Hand empty = new Hand();

        assertThatThrownBy(() -> PocketRanks.getInstance(empty)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("pre-flop");
    }

    @Test
    void should_ThrowError_When_CommunityHasTwoCards() {
        Hand twoCards = new Hand(SPADES_A, HEARTS_K);

        assertThatThrownBy(() -> PocketRanks.getInstance(twoCards)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("pre-flop");
    }

    @Test
    void should_ReturnInstance_When_CommunityHasThreeCards() {
        // Flop: A♠ K♥ Q♦
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);

        PocketRanks ranks = PocketRanks.getInstance(flop);

        assertThat(ranks).isNotNull();
    }

    @Test
    void should_ReturnInstance_When_CommunityHasFourCards() {
        // Flop + Turn: A♠ K♥ Q♦ J♣
        Hand turn = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);

        PocketRanks ranks = PocketRanks.getInstance(turn);

        assertThat(ranks).isNotNull();
    }

    @Test
    void should_ReturnInstance_When_CommunityHasFiveCards() {
        // Full board: A♠ K♥ Q♦ J♣ T♠
        Hand river = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);

        PocketRanks ranks = PocketRanks.getInstance(river);

        assertThat(ranks).isNotNull();
    }

    // ========================================
    // Caching Behavior Tests
    // ========================================

    @Test
    void should_ReturnSameInstance_When_SameCommunityRequested() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);

        PocketRanks ranks1 = PocketRanks.getInstance(flop);
        PocketRanks ranks2 = PocketRanks.getInstance(flop);

        assertThat(ranks1).isSameAs(ranks2);
    }

    @Test
    void should_ReturnSameInstance_When_SameTurnRequested() {
        Hand turn = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);

        PocketRanks ranks1 = PocketRanks.getInstance(turn);
        PocketRanks ranks2 = PocketRanks.getInstance(turn);

        assertThat(ranks1).isSameAs(ranks2);
    }

    @Test
    void should_CacheDifferentInstances_When_DifferentTurnsWithSameFlop() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        Hand turn1 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J);
        Hand turn2 = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_T);

        PocketRanks ranksFlop = PocketRanks.getInstance(flop);
        PocketRanks ranksTurn1 = PocketRanks.getInstance(turn1);
        PocketRanks ranksTurn2 = PocketRanks.getInstance(turn2);

        // All should be different instances (different boards)
        assertThat(ranksFlop).isNotSameAs(ranksTurn1);
        assertThat(ranksFlop).isNotSameAs(ranksTurn2);
        assertThat(ranksTurn1).isNotSameAs(ranksTurn2);
    }

    // ========================================
    // getRawHandStrength() with Hand Tests
    // ========================================

    @Test
    void should_ReturnHighStrength_When_NutsOnBoard() {
        // Board: K♠ Q♠ J♠ (flush draw + straight draw board)
        Hand flop = new Hand(SPADES_K, SPADES_Q, SPADES_J);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // A♠ T♠ makes royal flush (the nuts)
        Hand pocket = new Hand(SPADES_A, SPADES_T);
        float strength = ranks.getRawHandStrength(pocket);

        // Should be very high (beats almost everything)
        assertThat(strength).isGreaterThan(0.95f);
    }

    @Test
    void should_ReturnLowStrength_When_NothingOnDryBoard() {
        // Board: A♠ K♥ 7♦ (rainbow, dry board)
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_7);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // 3♣ 2♣ (nothing - no pair, no draw)
        Hand pocket = new Hand(CLUBS_3, CLUBS_2);
        float strength = ranks.getRawHandStrength(pocket);

        // Should be very low (loses to almost everything)
        assertThat(strength).isLessThan(0.10f);
    }

    @Test
    void should_ReturnMediumStrength_When_MiddlePair() {
        // Board: A♠ 7♥ 4♦
        Hand flop = new Hand(SPADES_A, HEARTS_7, DIAMONDS_4);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // 7♣ 6♣ (middle pair - pair of 7s)
        Hand pocket = new Hand(CLUBS_7, CLUBS_6);
        float strength = ranks.getRawHandStrength(pocket);

        // Middle pair is actually quite strong on dry board (beats unpaired hands)
        assertThat(strength).isBetween(0.50f, 0.85f);
    }

    // ========================================
    // getRawHandStrength() with Cards Tests
    // ========================================

    @Test
    void should_ReturnStrength_When_CalledWithCards() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        float strength = ranks.getRawHandStrength(SPADES_J, SPADES_T);

        // Should return valid strength (0.0-1.0)
        assertThat(strength).isBetween(0.0f, 1.0f);
    }

    @Test
    void should_ReturnSameStrength_When_CardsReversed() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        float strength1 = ranks.getRawHandStrength(SPADES_J, SPADES_T);
        float strength2 = ranks.getRawHandStrength(SPADES_T, SPADES_J);

        assertThat(strength1).isEqualTo(strength2);
    }

    // ========================================
    // getRawHandStrength() with Indices Tests
    // ========================================

    @Test
    void should_ReturnStrength_When_CalledWithIndices() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // Card indices for some pocket cards
        float strength = ranks.getRawHandStrength(47, 46); // Some valid indices

        assertThat(strength).isBetween(0.0f, 1.0f);
    }

    // ========================================
    // Specific Hand Type Tests
    // ========================================

    @Test
    void should_ReturnHighStrength_When_TopPairTopKicker() {
        // Board: A♠ 7♥ 4♦
        Hand flop = new Hand(SPADES_A, HEARTS_7, DIAMONDS_4);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // A♣ K♣ (top pair, top kicker)
        Hand pocket = new Hand(CLUBS_A, CLUBS_K);
        float strength = ranks.getRawHandStrength(pocket);

        // Should be quite strong
        assertThat(strength).isGreaterThan(0.75f);
    }

    @Test
    void should_ReturnVeryHighStrength_When_FloppedSet() {
        // Board: A♠ 7♥ 4♦
        Hand flop = new Hand(SPADES_A, HEARTS_7, DIAMONDS_4);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // A♣ A♦ (set of aces - very strong)
        Hand pocket = new Hand(CLUBS_A, DIAMONDS_A);
        float strength = ranks.getRawHandStrength(pocket);

        // Should be very strong (beats most hands)
        assertThat(strength).isGreaterThan(0.90f);
    }

    @Test
    void should_ReturnHighStrength_When_FloppedStraight() {
        // Board: T♠ 9♥ 8♦
        Hand flop = new Hand(SPADES_T, HEARTS_9, DIAMONDS_8);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // Q♣ J♣ (flopped nut straight)
        Hand pocket = new Hand(CLUBS_Q, CLUBS_J);
        float strength = ranks.getRawHandStrength(pocket);

        // Should be very strong
        assertThat(strength).isGreaterThan(0.85f);
    }

    @Test
    void should_ReturnHighStrength_When_FloppedFlush() {
        // Board: A♠ K♠ 7♠ (all spades)
        Hand flop = new Hand(SPADES_A, SPADES_K, SPADES_7);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // Q♠ J♠ (flush)
        Hand pocket = new Hand(SPADES_Q, SPADES_J);
        float strength = ranks.getRawHandStrength(pocket);

        // Flush is very strong
        assertThat(strength).isGreaterThan(0.80f);
    }

    // ========================================
    // Board Texture Tests
    // ========================================

    @Test
    void should_HandlePairedBoard_When_Calculating() {
        // Board: A♠ A♥ 7♦ (paired board)
        Hand flop = new Hand(SPADES_A, HEARTS_A, DIAMONDS_7);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // K♣ K♦ (pocket kings)
        Hand pocket = new Hand(CLUBS_K, DIAMONDS_K);
        float strength = ranks.getRawHandStrength(pocket);

        // Pocket kings on AA7 is strong (beats all non-A hands, only loses to
        // trips/quads)
        assertThat(strength).isBetween(0.85f, 0.95f);
    }

    @Test
    void should_HandleMonotoneBoard_When_Calculating() {
        // Board: K♠ 9♠ 3♠ (all spades - monotone)
        Hand flop = new Hand(SPADES_K, SPADES_9, SPADES_3);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // A♠ Q♠ (nut flush)
        Hand nutFlush = new Hand(SPADES_A, SPADES_Q);
        float nutStrength = ranks.getRawHandStrength(nutFlush);

        // 7♥ 6♥ (no spade - very weak on monotone board)
        Hand noFlush = new Hand(HEARTS_7, HEARTS_6);
        float weakStrength = ranks.getRawHandStrength(noFlush);

        assertThat(nutStrength).isGreaterThan(0.90f);
        assertThat(weakStrength).isLessThan(0.20f);
    }

    @Test
    void should_HandleConnectedBoard_When_Calculating() {
        // Board: 9♠ 8♥ 7♦ (connected - many straight possibilities)
        Hand flop = new Hand(SPADES_9, HEARTS_8, DIAMONDS_7);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // T♣ 6♣ (nut straight 6-7-8-9-T)
        Hand straight = new Hand(CLUBS_T, CLUBS_6);
        float strength = ranks.getRawHandStrength(straight);

        // Should be very strong
        assertThat(strength).isGreaterThan(0.85f);
    }

    // ========================================
    // Range Tests (0.0 to 1.0)
    // ========================================

    @Test
    void should_ReturnValueInRange_When_AnyValidInput() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        // Test multiple different pockets
        Hand[] pockets = {new Hand(SPADES_J, SPADES_T), // Broadway straight
                new Hand(CLUBS_2, CLUBS_3), // Low cards
                new Hand(HEARTS_A, DIAMONDS_A), // Pocket aces
                new Hand(CLUBS_K, DIAMONDS_K), // Pocket kings
                new Hand(HEARTS_Q, CLUBS_Q) // Pocket queens
        };

        for (Hand pocket : pockets) {
            float strength = ranks.getRawHandStrength(pocket);
            assertThat(strength).as("Strength for %s", pocket).isBetween(0.0f, 1.0f);
        }
    }

    // ========================================
    // Full Board (River) Tests
    // ========================================

    @Test
    void should_CalculateStrength_When_RiverCard() {
        // Full board: A♠ K♥ Q♦ J♣ T♠ (broadway straight on board)
        Hand river = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q, CLUBS_J, SPADES_T);
        PocketRanks ranks = PocketRanks.getInstance(river);

        // Any pocket cards should chop (everyone has the straight)
        Hand pocket = new Hand(CLUBS_2, CLUBS_3);
        float strength = ranks.getRawHandStrength(pocket);

        // With straight on board, most hands tie/chop
        assertThat(strength).isBetween(0.0f, 1.0f);
    }

    @Test
    void should_CalculateStrength_When_PairedRiver() {
        // Board: A♠ A♥ 7♦ 7♣ 3♠ (two pair on board)
        Hand river = new Hand(SPADES_A, HEARTS_A, DIAMONDS_7, CLUBS_7, SPADES_3);
        PocketRanks ranks = PocketRanks.getInstance(river);

        // K♣ K♦ (pocket kings - makes full house Aces over Kings)
        Hand pocket = new Hand(CLUBS_K, DIAMONDS_K);
        float strength = ranks.getRawHandStrength(pocket);

        // Full house should be very strong
        assertThat(strength).isGreaterThan(0.80f);
    }

    // ========================================
    // Consistency Tests
    // ========================================

    @Test
    void should_ReturnConsistentStrength_When_CalledMultipleTimes() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketRanks ranks = PocketRanks.getInstance(flop);
        Hand pocket = new Hand(SPADES_J, SPADES_T);

        float strength1 = ranks.getRawHandStrength(pocket);
        float strength2 = ranks.getRawHandStrength(pocket);
        float strength3 = ranks.getRawHandStrength(pocket);

        assertThat(strength1).isEqualTo(strength2).isEqualTo(strength3);
    }

    @Test
    void should_ReturnSameStrength_When_CalledWithDifferentMethods() {
        Hand flop = new Hand(SPADES_A, HEARTS_K, DIAMONDS_Q);
        PocketRanks ranks = PocketRanks.getInstance(flop);

        Hand pocket = new Hand(SPADES_J, SPADES_T);
        float strengthHand = ranks.getRawHandStrength(pocket);
        float strengthCards = ranks.getRawHandStrength(SPADES_J, SPADES_T);
        float strengthIndices = ranks.getRawHandStrength(SPADES_J.getIndex(), SPADES_T.getIndex());

        assertThat(strengthHand).isEqualTo(strengthCards).isEqualTo(strengthIndices);
    }
}
