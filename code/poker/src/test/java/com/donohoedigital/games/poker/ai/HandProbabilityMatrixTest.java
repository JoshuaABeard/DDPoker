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

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for HandProbabilityMatrix probability storage and management.
 */
class HandProbabilityMatrixTest
{
    private HandProbabilityMatrix matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new HandProbabilityMatrix();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_InitializeWithDefaultProbability_When_DefaultConstructor()
    {
        // Default constructor calls init(1.0f), so all probabilities should be 1.0
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(CLUBS_2, DIAMONDS_3)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(SPADES_Q, HEARTS_J)).isEqualTo(1.0f);
    }

    // ========================================
    // Init Method Tests
    // ========================================

    @Test
    void should_SetAllProbabilities_When_InitCalledWithValue()
    {
        matrix.init(0.5f);

        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.5f);
        assertThat(matrix.getProbability(CLUBS_2, DIAMONDS_3)).isEqualTo(0.5f);
        assertThat(matrix.getProbability(SPADES_Q, HEARTS_J)).isEqualTo(0.5f);
    }

    @Test
    void should_ResetAllProbabilities_When_InitCalledMultipleTimes()
    {
        matrix.init(0.25f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.25f);

        matrix.init(0.75f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.75f);
    }

    @Test
    void should_AcceptZeroProbability_When_InitCalledWithZero()
    {
        matrix.init(0.0f);

        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isZero();
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_Q)).isZero();
    }

    @Test
    void should_AcceptFullProbability_When_InitCalledWithOne()
    {
        matrix.init(1.0f);

        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_Q)).isEqualTo(1.0f);
    }

    // ========================================
    // GetProbability Tests
    // ========================================

    @Test
    void should_ReturnSameProbability_When_CardsReversed()
    {
        matrix.init(0.8f);

        float prob1 = matrix.getProbability(SPADES_A, HEARTS_K);
        float prob2 = matrix.getProbability(HEARTS_K, SPADES_A);

        assertThat(prob1).isEqualTo(prob2);
        assertThat(prob1).isEqualTo(0.8f);
    }

    @Test
    void should_ReturnCorrectProbability_When_DifferentCardCombinations()
    {
        matrix.init(0.6f);

        assertThat(matrix.getProbability(SPADES_A, CLUBS_A)).isEqualTo(0.6f);
        assertThat(matrix.getProbability(HEARTS_K, DIAMONDS_K)).isEqualTo(0.6f);
        assertThat(matrix.getProbability(SPADES_2, CLUBS_3)).isEqualTo(0.6f);
    }

    @Test
    void should_HandleAllCardRanks_When_GettingProbability()
    {
        matrix.init(0.9f);

        // Test different ranks
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(CLUBS_Q, DIAMONDS_J)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(SPADES_T, HEARTS_9)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(CLUBS_8, DIAMONDS_7)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(SPADES_6, HEARTS_5)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(CLUBS_4, DIAMONDS_3)).isEqualTo(0.9f);
    }

    @Test
    void should_HandleAllSuits_When_GettingProbability()
    {
        matrix.init(0.7f);

        // Test different suits
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.7f);
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_K)).isEqualTo(0.7f);
        assertThat(matrix.getProbability(SPADES_Q, CLUBS_Q)).isEqualTo(0.7f);
        assertThat(matrix.getProbability(HEARTS_J, DIAMONDS_J)).isEqualTo(0.7f);
    }

    // ========================================
    // Probability Range Tests
    // ========================================

    @Test
    void should_HandleSmallProbabilities_When_InitCalledWithSmallValues()
    {
        matrix.init(0.001f);

        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.001f);
    }

    @Test
    void should_HandleNegativeProbabilities_When_InitCalledWithNegative()
    {
        // Though unusual, the code doesn't prevent negative probabilities
        matrix.init(-0.5f);

        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(-0.5f);
    }

    @Test
    void should_HandleLargeProbabilities_When_InitCalledWithLargeValues()
    {
        // Though unusual (probabilities should be 0-1), the code doesn't prevent this
        matrix.init(2.0f);

        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(2.0f);
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    void should_HandleSameCardPair_When_GettingProbability()
    {
        matrix.init(0.5f);

        // Same rank, different suits (pocket pair)
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.5f);
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_K)).isEqualTo(0.5f);
    }

    @Test
    void should_HandleBoundaryCards_When_LowestAndHighestRanks()
    {
        matrix.init(0.3f);

        // Lowest rank (2) and highest rank (A)
        assertThat(matrix.getProbability(SPADES_2, CLUBS_2)).isEqualTo(0.3f);
        assertThat(matrix.getProbability(SPADES_A, CLUBS_A)).isEqualTo(0.3f);
        assertThat(matrix.getProbability(SPADES_2, SPADES_A)).isEqualTo(0.3f);
    }

    @Test
    void should_HandleSuitedCards_When_SameSuitDifferentRanks()
    {
        matrix.init(0.4f);

        // Same suit, different ranks
        assertThat(matrix.getProbability(SPADES_A, SPADES_K)).isEqualTo(0.4f);
        assertThat(matrix.getProbability(HEARTS_Q, HEARTS_J)).isEqualTo(0.4f);
    }

    @Test
    void should_HandleOffsuitCards_When_DifferentSuitsDifferentRanks()
    {
        matrix.init(0.35f);

        // Different suits, different ranks
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.35f);
        assertThat(matrix.getProbability(CLUBS_Q, DIAMONDS_J)).isEqualTo(0.35f);
    }

    @Test
    void should_MaintainProbabilities_When_MultipleReadsWithoutInit()
    {
        matrix.init(0.55f);

        // Multiple reads should return consistent values
        float prob1 = matrix.getProbability(SPADES_A, HEARTS_K);
        float prob2 = matrix.getProbability(SPADES_A, HEARTS_K);
        float prob3 = matrix.getProbability(SPADES_A, HEARTS_K);

        assertThat(prob1).isEqualTo(0.55f);
        assertThat(prob2).isEqualTo(0.55f);
        assertThat(prob3).isEqualTo(0.55f);
    }

    @Test
    void should_HandleFloatPrecision_When_GettingProbabilities()
    {
        matrix.init(0.123456789f);

        float prob = matrix.getProbability(SPADES_A, HEARTS_K);

        // Float precision is about 7 decimal digits
        assertThat(prob).isCloseTo(0.123456789f, within(0.0000001f));
    }

    // ========================================
    // Comprehensive Card Combination Tests
    // ========================================

    @Test
    void should_HandleAllThirteenPocketPairs_When_GettingProbabilities()
    {
        matrix.init(0.42f);

        // Test all 13 pocket pairs (AA through 22)
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_K, HEARTS_K)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_Q, HEARTS_Q)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_J, HEARTS_J)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_T, HEARTS_T)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_9, HEARTS_9)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_8, HEARTS_8)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_7, HEARTS_7)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_6, HEARTS_6)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_5, HEARTS_5)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_4, HEARTS_4)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_3, HEARTS_3)).isEqualTo(0.42f);
        assertThat(matrix.getProbability(SPADES_2, HEARTS_2)).isEqualTo(0.42f);
    }

    @Test
    void should_HandleAllSuitCombinations_When_PocketAces()
    {
        matrix.init(0.88f);

        // All 6 possible combinations of pocket aces
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.88f);
        assertThat(matrix.getProbability(SPADES_A, DIAMONDS_A)).isEqualTo(0.88f);
        assertThat(matrix.getProbability(SPADES_A, CLUBS_A)).isEqualTo(0.88f);
        assertThat(matrix.getProbability(HEARTS_A, DIAMONDS_A)).isEqualTo(0.88f);
        assertThat(matrix.getProbability(HEARTS_A, CLUBS_A)).isEqualTo(0.88f);
        assertThat(matrix.getProbability(DIAMONDS_A, CLUBS_A)).isEqualTo(0.88f);
    }

    @Test
    void should_HandleSuitedConnectors_When_GettingProbabilities()
    {
        matrix.init(0.65f);

        // Suited connectors of different suits
        assertThat(matrix.getProbability(SPADES_A, SPADES_K)).isEqualTo(0.65f);
        assertThat(matrix.getProbability(HEARTS_K, HEARTS_Q)).isEqualTo(0.65f);
        assertThat(matrix.getProbability(CLUBS_J, CLUBS_T)).isEqualTo(0.65f);
        assertThat(matrix.getProbability(DIAMONDS_9, DIAMONDS_8)).isEqualTo(0.65f);
    }

    @Test
    void should_HandleOffsuitBroadways_When_GettingProbabilities()
    {
        matrix.init(0.72f);

        // Offsuit broadway cards (T-A)
        assertThat(matrix.getProbability(SPADES_A, HEARTS_Q)).isEqualTo(0.72f);
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_J)).isEqualTo(0.72f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_T)).isEqualTo(0.72f);
    }

    @Test
    void should_HandleGapHands_When_GettingProbabilities()
    {
        matrix.init(0.33f);

        // Gap hands (one or more ranks between cards)
        assertThat(matrix.getProbability(SPADES_A, HEARTS_J)).isEqualTo(0.33f); // 2-gap
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_T)).isEqualTo(0.33f); // 2-gap
        assertThat(matrix.getProbability(SPADES_9, HEARTS_6)).isEqualTo(0.33f); // 2-gap
    }

    @Test
    void should_HandleLowCards_When_GettingProbabilities()
    {
        matrix.init(0.15f);

        // Low card combinations (2-7)
        assertThat(matrix.getProbability(SPADES_7, HEARTS_6)).isEqualTo(0.15f);
        assertThat(matrix.getProbability(CLUBS_5, DIAMONDS_4)).isEqualTo(0.15f);
        assertThat(matrix.getProbability(SPADES_3, HEARTS_2)).isEqualTo(0.15f);
        assertThat(matrix.getProbability(CLUBS_7, DIAMONDS_2)).isEqualTo(0.15f);
    }

    // ========================================
    // Symmetry Tests (Extended)
    // ========================================

    @Test
    void should_MaintainSymmetry_When_AllCardPairsReversed()
    {
        matrix.init(0.5f);

        // Test symmetry across multiple card combinations
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K))
            .isEqualTo(matrix.getProbability(HEARTS_K, SPADES_A));
        assertThat(matrix.getProbability(CLUBS_Q, DIAMONDS_J))
            .isEqualTo(matrix.getProbability(DIAMONDS_J, CLUBS_Q));
        assertThat(matrix.getProbability(SPADES_9, HEARTS_2))
            .isEqualTo(matrix.getProbability(HEARTS_2, SPADES_9));
        assertThat(matrix.getProbability(CLUBS_7, DIAMONDS_3))
            .isEqualTo(matrix.getProbability(DIAMONDS_3, CLUBS_7));
    }

    @Test
    void should_MaintainSymmetry_When_SameSuitDifferentOrder()
    {
        matrix.init(0.66f);

        // Suited cards reversed
        assertThat(matrix.getProbability(SPADES_A, SPADES_K))
            .isEqualTo(matrix.getProbability(SPADES_K, SPADES_A));
        assertThat(matrix.getProbability(HEARTS_Q, HEARTS_J))
            .isEqualTo(matrix.getProbability(HEARTS_J, HEARTS_Q));
    }

    // ========================================
    // Sequential Operations Tests
    // ========================================

    @Test
    void should_UpdateAllProbabilities_When_InitCalledRepeatedly()
    {
        matrix.init(0.1f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.1f);

        matrix.init(0.2f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.2f);

        matrix.init(0.3f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.3f);

        matrix.init(0.4f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.4f);
    }

    @Test
    void should_AffectAllCards_When_InitCalledAfterMultipleReads()
    {
        matrix.init(0.5f);

        // Read multiple times
        matrix.getProbability(SPADES_A, HEARTS_K);
        matrix.getProbability(CLUBS_Q, DIAMONDS_J);
        matrix.getProbability(SPADES_9, HEARTS_8);

        // Reinitialize
        matrix.init(0.9f);

        // All should have new value
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(CLUBS_Q, DIAMONDS_J)).isEqualTo(0.9f);
        assertThat(matrix.getProbability(SPADES_9, HEARTS_8)).isEqualTo(0.9f);
    }

    @Test
    void should_ResetCompletely_When_InitCalledFromZeroToOne()
    {
        matrix.init(0.0f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isZero();

        matrix.init(1.0f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(1.0f);

        matrix.init(0.0f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isZero();
    }

    // ========================================
    // Matrix Coverage Tests
    // ========================================

    @Test
    void should_HandleDifferentMatrixRegions_When_GettingProbabilities()
    {
        matrix.init(0.77f);

        // Test corners of the matrix
        assertThat(matrix.getProbability(CLUBS_2, DIAMONDS_2)).isEqualTo(0.77f); // Low-low
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.77f); // High-high
        assertThat(matrix.getProbability(CLUBS_2, SPADES_A)).isEqualTo(0.77f); // Low-high
        assertThat(matrix.getProbability(SPADES_A, CLUBS_2)).isEqualTo(0.77f); // High-low
    }

    @Test
    void should_HandleMiddleRanks_When_GettingProbabilities()
    {
        matrix.init(0.44f);

        // Middle ranks (7-9)
        assertThat(matrix.getProbability(SPADES_9, HEARTS_8)).isEqualTo(0.44f);
        assertThat(matrix.getProbability(CLUBS_8, DIAMONDS_7)).isEqualTo(0.44f);
        assertThat(matrix.getProbability(SPADES_7, HEARTS_7)).isEqualTo(0.44f);
    }

    @Test
    void should_HandleDiagonalPattern_When_PocketPairsOfEachRank()
    {
        matrix.init(0.81f);

        // Diagonal elements (pocket pairs)
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.81f);
        assertThat(matrix.getProbability(SPADES_K, HEARTS_K)).isEqualTo(0.81f);
        assertThat(matrix.getProbability(SPADES_Q, HEARTS_Q)).isEqualTo(0.81f);
        assertThat(matrix.getProbability(SPADES_J, HEARTS_J)).isEqualTo(0.81f);
        assertThat(matrix.getProbability(SPADES_T, HEARTS_T)).isEqualTo(0.81f);
    }

    // ========================================
    // Specific Probability Value Tests
    // ========================================

    @Test
    void should_HandleCommonProbabilityValues_When_InitCalled()
    {
        // Test common probability values in poker (quarters)
        float[] probabilities = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

        for (float prob : probabilities)
        {
            matrix.init(prob);
            assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(prob);
        }
    }

    @Test
    void should_HandleDecimalProbabilities_When_InitCalled()
    {
        // Test various decimal probabilities
        float[] probabilities = {0.1f, 0.2f, 0.3f, 0.4f, 0.6f, 0.7f, 0.8f, 0.9f};

        for (float prob : probabilities)
        {
            matrix.init(prob);
            assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(prob);
        }
    }

    @Test
    void should_HandleVerySmallProbabilities_When_InitCalled()
    {
        matrix.init(0.0001f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.0001f);

        matrix.init(0.00001f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.00001f);
    }

    @Test
    void should_HandleProbabilitiesNearOne_When_InitCalled()
    {
        matrix.init(0.999f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.999f);

        matrix.init(0.9999f);
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.9999f);
    }

    // ========================================
    // Well-Known Poker Hands Tests
    // ========================================

    @Test
    void should_HandlePremiumPocketPairs_When_GettingProbabilities()
    {
        matrix.init(0.95f);

        // Premium pairs (AA, KK, QQ)
        assertThat(matrix.getProbability(SPADES_A, HEARTS_A)).isEqualTo(0.95f);
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_K)).isEqualTo(0.95f);
        assertThat(matrix.getProbability(SPADES_Q, HEARTS_Q)).isEqualTo(0.95f);
    }

    @Test
    void should_HandleBigSlick_When_AceKingCombinations()
    {
        matrix.init(0.82f);

        // AK suited
        assertThat(matrix.getProbability(SPADES_A, SPADES_K)).isEqualTo(0.82f);
        assertThat(matrix.getProbability(HEARTS_A, HEARTS_K)).isEqualTo(0.82f);

        // AK offsuit
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.82f);
        assertThat(matrix.getProbability(CLUBS_A, DIAMONDS_K)).isEqualTo(0.82f);
    }

    @Test
    void should_HandleRagCards_When_WeakHands()
    {
        matrix.init(0.05f);

        // Rag hands (27o, 32o, etc.)
        assertThat(matrix.getProbability(SPADES_7, HEARTS_2)).isEqualTo(0.05f);
        assertThat(matrix.getProbability(CLUBS_3, DIAMONDS_2)).isEqualTo(0.05f);
        assertThat(matrix.getProbability(SPADES_8, HEARTS_3)).isEqualTo(0.05f);
    }

    // ========================================
    // Consistency Tests
    // ========================================

    @Test
    void should_ReturnConsistentValues_When_SameCardPairReadMultipleTimes()
    {
        matrix.init(0.632f);

        float[] readings = new float[10];
        for (int i = 0; i < 10; i++)
        {
            readings[i] = matrix.getProbability(SPADES_A, HEARTS_K);
        }

        // All readings should be identical
        for (float reading : readings)
        {
            assertThat(reading).isEqualTo(0.632f);
        }
    }

    @Test
    void should_ReturnDifferentValues_When_DifferentProbabilities()
    {
        matrix.init(0.1f);
        float prob1 = matrix.getProbability(SPADES_A, HEARTS_K);

        matrix.init(0.9f);
        float prob2 = matrix.getProbability(SPADES_A, HEARTS_K);

        assertThat(prob1).isNotEqualTo(prob2);
        assertThat(prob1).isEqualTo(0.1f);
        assertThat(prob2).isEqualTo(0.9f);
    }

    // ========================================
    // adjustWeightsPreFlop Tests
    // ========================================

    @Test
    void should_SetProbabilityToZero_When_HoleCardsContainCard()
    {
        matrix.init(1.0f);

        Hand hole = new Hand(SPADES_A, HEARTS_K);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands containing As or Kh should have 0 probability
        assertThat(matrix.getProbability(SPADES_A, CLUBS_Q)).isZero();
        assertThat(matrix.getProbability(SPADES_A, DIAMONDS_2)).isZero();
        assertThat(matrix.getProbability(HEARTS_K, CLUBS_7)).isZero();
        assertThat(matrix.getProbability(HEARTS_K, DIAMONDS_3)).isZero();
    }

    @Test
    void should_PreserveNonConflictingProbabilities_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        Hand hole = new Hand(SPADES_A, HEARTS_K);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands not containing As or Kh should still have probability 1.0
        assertThat(matrix.getProbability(CLUBS_Q, DIAMONDS_J)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(SPADES_T, HEARTS_9)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(CLUBS_7, DIAMONDS_6)).isEqualTo(1.0f);
    }

    @Test
    void should_SetProbabilitiesToZero_When_HolePocketPair()
    {
        matrix.init(1.0f);

        Hand hole = new Hand(SPADES_A, HEARTS_A);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands containing Spades Ace or Hearts Ace should be zero
        assertThat(matrix.getProbability(SPADES_A, CLUBS_K)).isZero();
        assertThat(matrix.getProbability(HEARTS_A, DIAMONDS_Q)).isZero();
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isZero();

        // Other pocket aces (without As or Ah) should be unaffected
        assertThat(matrix.getProbability(CLUBS_A, DIAMONDS_A)).isEqualTo(1.0f);
        // Non-ace hands should be unaffected
        assertThat(matrix.getProbability(CLUBS_K, DIAMONDS_Q)).isEqualTo(1.0f);
    }

    @Test
    void should_SkipAlreadyZeroProbabilities_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        // Set some probabilities to zero manually
        Hand tempHole = new Hand(CLUBS_Q, DIAMONDS_Q);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        matrix.adjustWeightsPreFlop(null, player, 0, tempHole);

        // Now adjust with different hole cards
        Hand newHole = new Hand(SPADES_A, HEARTS_K);
        matrix.adjustWeightsPreFlop(null, player, 0, newHole);

        // Previously zeroed probabilities should remain zero
        assertThat(matrix.getProbability(CLUBS_Q, SPADES_J)).isZero();
        assertThat(matrix.getProbability(DIAMONDS_Q, HEARTS_J)).isZero();
    }

    @Test
    void should_HandleLowPocketPair_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        Hand hole = new Hand(CLUBS_2, DIAMONDS_2);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands containing CLUBS_2 or DIAMONDS_2 should be zero
        assertThat(matrix.getProbability(CLUBS_2, SPADES_A)).isZero();
        assertThat(matrix.getProbability(DIAMONDS_2, HEARTS_K)).isZero();
        assertThat(matrix.getProbability(CLUBS_2, DIAMONDS_2)).isZero();

        // Other pocket pairs of 2s (without 2c or 2d) should be unaffected
        assertThat(matrix.getProbability(SPADES_2, HEARTS_2)).isEqualTo(1.0f);
        // High cards should be unaffected
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(1.0f);
    }

    @Test
    void should_HandleSuitedHole_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        Hand hole = new Hand(SPADES_A, SPADES_K);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands with As or Ks should be zero
        assertThat(matrix.getProbability(SPADES_A, CLUBS_Q)).isZero();
        assertThat(matrix.getProbability(SPADES_K, HEARTS_Q)).isZero();

        // Other suited AK should be unaffected
        assertThat(matrix.getProbability(HEARTS_A, HEARTS_K)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(CLUBS_A, CLUBS_K)).isEqualTo(1.0f);
    }

    @Test
    void should_HandleConnectors_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        Hand hole = new Hand(CLUBS_J, DIAMONDS_T);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands with Jc or Td should be zero
        assertThat(matrix.getProbability(CLUBS_J, SPADES_A)).isZero();
        assertThat(matrix.getProbability(DIAMONDS_T, HEARTS_9)).isZero();

        // Other JT combinations should be unaffected
        assertThat(matrix.getProbability(SPADES_J, CLUBS_T)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(HEARTS_J, SPADES_T)).isEqualTo(1.0f);
    }

    @Test
    void should_HandleMultipleAdjustments_When_CalledSequentially()
    {
        matrix.init(1.0f);

        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        // First adjustment
        Hand hole1 = new Hand(SPADES_A, HEARTS_K);
        matrix.adjustWeightsPreFlop(scheme, player, 0, hole1);

        // Second adjustment (different hole cards)
        Hand hole2 = new Hand(CLUBS_Q, DIAMONDS_J);
        matrix.adjustWeightsPreFlop(scheme, player, 0, hole2);

        // Both sets should be zeroed
        assertThat(matrix.getProbability(SPADES_A, CLUBS_2)).isZero();
        assertThat(matrix.getProbability(HEARTS_K, DIAMONDS_3)).isZero();
        assertThat(matrix.getProbability(CLUBS_Q, SPADES_4)).isZero();
        assertThat(matrix.getProbability(DIAMONDS_J, HEARTS_5)).isZero();

        // Non-conflicting hands should still be 1.0
        assertThat(matrix.getProbability(SPADES_T, HEARTS_9)).isEqualTo(1.0f);
    }

    @Test
    void should_HandleDifferentSchemes_When_AdjustWeightsPreFlop()
    {
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        Hand hole = new Hand(SPADES_A, HEARTS_K);

        // Test with different schemes
        HandSelectionScheme[] schemes = {
            null,
            null,
            null
        };

        for (HandSelectionScheme scheme : schemes)
        {
            matrix.init(1.0f);
            matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

            // Hole cards should always be zeroed regardless of scheme
            assertThat(matrix.getProbability(SPADES_A, CLUBS_Q)).isZero();
            assertThat(matrix.getProbability(HEARTS_K, DIAMONDS_7)).isZero();
        }
    }

    @Test
    void should_HandleDifferentActions_When_AdjustWeightsPreFlop()
    {
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        Hand hole = new Hand(SPADES_K, HEARTS_Q);
        HandSelectionScheme scheme = null;

        // Test with different actions (0, 1, 2, etc.)
        for (int action = 0; action < 5; action++)
        {
            matrix.init(1.0f);
            matrix.adjustWeightsPreFlop(scheme, player, action, hole);

            // Hole cards should always be zeroed regardless of action
            assertThat(matrix.getProbability(SPADES_K, CLUBS_J)).isZero();
            assertThat(matrix.getProbability(HEARTS_Q, DIAMONDS_J)).isZero();
        }
    }

    @Test
    void should_PreserveInitialZeros_When_AdjustWeightsPreFlop()
    {
        // Initialize with some zero probabilities
        matrix.init(0.0f);

        Hand hole = new Hand(SPADES_A, HEARTS_K);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        // Should not crash even though all probabilities are already zero
        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // All should still be zero
        assertThat(matrix.getProbability(SPADES_A, CLUBS_Q)).isZero();
        assertThat(matrix.getProbability(CLUBS_7, DIAMONDS_6)).isZero();
    }

    @Test
    void should_HandlePartialProbabilities_When_AdjustWeightsPreFlop()
    {
        matrix.init(0.5f);

        Hand hole = new Hand(CLUBS_9, DIAMONDS_8);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Conflicting hands should be zero
        assertThat(matrix.getProbability(CLUBS_9, SPADES_A)).isZero();
        assertThat(matrix.getProbability(DIAMONDS_8, HEARTS_K)).isZero();

        // Non-conflicting hands should still be 0.5
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isEqualTo(0.5f);
        assertThat(matrix.getProbability(CLUBS_7, DIAMONDS_6)).isEqualTo(0.5f);
    }

    @Test
    void should_HandleAllFourSuits_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        // Use one card from each suit
        Hand hole = new Hand(SPADES_A, HEARTS_A);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hands containing SPADES_A or HEARTS_A should be zero
        assertThat(matrix.getProbability(SPADES_A, CLUBS_K)).isZero();
        assertThat(matrix.getProbability(HEARTS_A, DIAMONDS_K)).isZero();

        // Hands with other aces (CLUBS_A, DIAMONDS_A) should be unaffected
        assertThat(matrix.getProbability(CLUBS_A, SPADES_K)).isEqualTo(1.0f);
        assertThat(matrix.getProbability(DIAMONDS_A, CLUBS_K)).isEqualTo(1.0f);
    }

    @Test
    void should_HandleGapHoleCards_When_AdjustWeightsPreFlop()
    {
        matrix.init(1.0f);

        // Gap hand (A-9, 4 ranks between)
        Hand hole = new Hand(SPADES_A, CLUBS_9);
        PokerPlayer player = new PokerPlayer(1, "TestPlayer", true);
        HandSelectionScheme scheme = null;

        matrix.adjustWeightsPreFlop(scheme, player, 0, hole);

        // Hole cards should be zeroed
        assertThat(matrix.getProbability(SPADES_A, HEARTS_K)).isZero();
        assertThat(matrix.getProbability(CLUBS_9, DIAMONDS_8)).isZero();

        // Other A9 combinations should be unaffected
        assertThat(matrix.getProbability(HEARTS_A, DIAMONDS_9)).isEqualTo(1.0f);
    }
}
