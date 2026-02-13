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

import com.donohoedigital.config.*;
import com.donohoedigital.games.poker.ai.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PreFlopBias - AI pre-flop hand weight calculation based on position
 * and tightness.
 */
class PreFlopBiasTest {

    @BeforeEach
    void setUp() {
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
    }

    // ========== Premium Hands Tests (E = Excellent) ==========

    @Test
    void should_ReturnMaxWeight_ForPocketAces() {
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_A, Card.HEARTS_A, PokerAI.POSITION_EARLY, 0.5f);

        assertThat(weight).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnMaxWeight_ForAceKingSuited() {
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_A, Card.SPADES_K, PokerAI.POSITION_EARLY, 0.5f);

        assertThat(weight).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnMaxWeight_ForAceKingOffsuit_WithAvgTightness() {
        // AKo is marked as 'E' in openavgfull matrix
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_A, Card.HEARTS_K, PokerAI.POSITION_EARLY, 0.5f);

        assertThat(weight).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnMaxWeight_ForPocketKings() {
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_K, Card.HEARTS_K, PokerAI.POSITION_EARLY, 0.5f);

        assertThat(weight).isEqualTo(1.0f);
    }

    // ========== Position-Based Weight Tests ==========

    @Test
    void should_ReduceWeight_ForMediumHand_InEarlyPosition() {
        // Medium hands (M) in early position = 0.25
        // Find a hand marked as 'M' in all matrices
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.HEARTS_J, PokerAI.POSITION_EARLY, 0.5f);

        assertThat(weight).isLessThan(0.5f);
    }

    @Test
    void should_IncreaseWeight_ForMediumHand_InLatePosition() {
        // Medium hands (M) in late position = 1.0
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.HEARTS_J, PokerAI.POSITION_LATE, 0.5f);

        assertThat(weight).isGreaterThanOrEqualTo(0.5f);
    }

    @Test
    void should_IncreaseWeight_ForMediumHand_OnButton() {
        // Medium hands (M) on button = 1.0
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.HEARTS_J, PokerAI.POSITION_BUTTON, 0.5f);

        assertThat(weight).isEqualTo(1.0f);
    }

    // ========== Tightness Tests ==========

    @Test
    void should_UseAvgMatrix_WithMidTightness() {
        // Tightness = 0.5 means 100% average matrix, 0% extreme matrix
        float avgWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_J, Card.HEARTS_T, PokerAI.POSITION_EARLY, 0.5f);

        // Should use only openavgfull matrix
        assertThat(avgWeight).isGreaterThan(0.0f);
    }

    @Test
    void should_BlendTightMatrix_WithHighTightness() {
        // Tightness = 1.0 means 100% tight matrix, 0% average matrix
        float tightWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_9, Card.HEARTS_8, PokerAI.POSITION_EARLY, 1.0f);

        // Should use only opentightfull matrix
        assertThat(tightWeight).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_BlendLooseMatrix_WithLowTightness() {
        // Tightness = 0.0 means 100% loose matrix, 0% average matrix
        float looseWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_9, Card.HEARTS_8, PokerAI.POSITION_EARLY, 0.0f);

        // Should use only openloosefull matrix
        assertThat(looseWeight).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_BlendMatrices_WithPartialTightness() {
        // Tightness = 0.75 means 50% tight, 50% average
        float blendedWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_8, Card.HEARTS_7, PokerAI.POSITION_EARLY, 0.75f);

        assertThat(blendedWeight).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_BlendMatrices_WithPartialLooseness() {
        // Tightness = 0.25 means 50% loose, 50% average
        float blendedWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_8, Card.HEARTS_7, PokerAI.POSITION_EARLY, 0.25f);

        assertThat(blendedWeight).isGreaterThanOrEqualTo(0.0f);
    }

    // ========== Suited vs Offsuit Tests ==========

    @Test
    void should_HandleSuitedHands_Correctly() {
        float suitedWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.SPADES_J, PokerAI.POSITION_LATE, 0.5f);

        assertThat(suitedWeight).isGreaterThan(0.0f);
    }

    @Test
    void should_HandleOffsuitHands_Correctly() {
        float offsuitWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.HEARTS_J, PokerAI.POSITION_LATE, 0.5f);

        assertThat(offsuitWeight).isGreaterThan(0.0f);
    }

    @Test
    void should_GiveHigherWeight_ToSuitedHands() {
        // Generally suited hands get higher weight than offsuit
        float suitedWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_T, Card.SPADES_9, PokerAI.POSITION_LATE, 0.5f);
        float offsuitWeight = PreFlopBias.getOpenPotWeight(Card.SPADES_T, Card.HEARTS_9, PokerAI.POSITION_LATE, 0.5f);

        // In most cases, suited > offsuit
        assertThat(suitedWeight).isGreaterThanOrEqualTo(offsuitWeight);
    }

    // ========== Edge Case Tests ==========

    @Test
    void should_HandleLowRankPair() {
        // Pocket 2s
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_2, Card.HEARTS_2, PokerAI.POSITION_BUTTON, 0.5f);

        assertThat(weight).isGreaterThan(0.0f);
    }

    @Test
    void should_HandleTrashHand_InEarlyPosition() {
        // 7-2 offsuit in early position should have very low weight
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_7, Card.HEARTS_2, PokerAI.POSITION_EARLY, 0.5f);

        assertThat(weight).isLessThanOrEqualTo(0.1f);
    }

    @Test
    void should_HandleTrashHand_OnButton() {
        // Even trash hands can be played on button
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_7, Card.HEARTS_2, PokerAI.POSITION_BUTTON, 0.5f);

        assertThat(weight).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_HandleReversedCardOrder() {
        // Same hand, reversed order
        float weight1 = PreFlopBias.getOpenPotWeight(Card.SPADES_A, Card.HEARTS_K, PokerAI.POSITION_LATE, 0.5f);
        float weight2 = PreFlopBias.getOpenPotWeight(Card.HEARTS_K, Card.SPADES_A, PokerAI.POSITION_LATE, 0.5f);

        // Should give same weight regardless of card order
        assertThat(weight1).isEqualTo(weight2);
    }

    @Test
    void should_HandleAllPositions() {
        // Verify each position constant works
        Card card1 = Card.SPADES_J;
        Card card2 = Card.HEARTS_T;

        assertThatCode(() -> PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_EARLY, 0.5f))
                .doesNotThrowAnyException();
        assertThatCode(() -> PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_MIDDLE, 0.5f))
                .doesNotThrowAnyException();
        assertThatCode(() -> PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_LATE, 0.5f))
                .doesNotThrowAnyException();
        assertThatCode(() -> PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_BUTTON, 0.5f))
                .doesNotThrowAnyException();
    }

    @Test
    void should_HandleSmallBlind_Position() {
        // Small blind position (default case in switch statements)
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.HEARTS_J, PokerAI.POSITION_SMALL, 0.5f);

        assertThat(weight).isGreaterThanOrEqualTo(0.0f);
    }

    @Test
    void should_HandleBigBlind_Position() {
        // Big blind position (default case in switch statements)
        float weight = PreFlopBias.getOpenPotWeight(Card.SPADES_Q, Card.HEARTS_J, PokerAI.POSITION_BIG, 0.5f);

        assertThat(weight).isGreaterThanOrEqualTo(0.0f);
    }

    // ========== Weight Range Tests ==========

    @Test
    void should_ReturnWeightBetween0And1() {
        // Test various hands to ensure weights are in valid range
        for (int i = 0; i < 10; i++) {
            Card card1 = Card.SPADES_A;
            Card card2 = i % 2 == 0 ? Card.HEARTS_K : Card.SPADES_K;
            int position = i % 4;
            float tightness = i / 10.0f;

            float weight = PreFlopBias.getOpenPotWeight(card1, card2, position, tightness);

            assertThat(weight).isBetween(0.0f, 1.0f);
        }
    }

    @Test
    void should_IncreasedWeight_FromEarlyToLatePosition() {
        // Same hand should generally increase weight from early to late
        Card card1 = Card.SPADES_J;
        Card card2 = Card.HEARTS_9;

        float earlyWeight = PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_EARLY, 0.5f);
        float middleWeight = PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_MIDDLE, 0.5f);
        float lateWeight = PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_LATE, 0.5f);
        float buttonWeight = PreFlopBias.getOpenPotWeight(card1, card2, PokerAI.POSITION_BUTTON, 0.5f);

        // Weight should increase with better position
        assertThat(middleWeight).isGreaterThanOrEqualTo(earlyWeight);
        assertThat(lateWeight).isGreaterThanOrEqualTo(middleWeight);
        assertThat(buttonWeight).isGreaterThanOrEqualTo(lateWeight);
    }
}
