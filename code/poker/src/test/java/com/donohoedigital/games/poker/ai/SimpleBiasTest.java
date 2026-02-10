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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SimpleBias hand strength bias table lookups.
 */
class SimpleBiasTest
{
    // ========================================
    // Pocket Pairs Tests (table index 0)
    // ========================================

    @Test
    void should_ReturnMaxBias_When_PocketAcesAtZeroTable()
    {
        Card aceSpades = SPADES_A;
        Card aceHearts = HEARTS_A;

        float bias = SimpleBias.getBiasValue(0, aceSpades, aceHearts);

        assertThat(bias).isEqualTo(1.0f); // 1000/1000
    }

    @Test
    void should_ReturnLowBias_When_LowPairAtZeroTable()
    {
        Card twoClubs = CLUBS_2;
        Card twoDiamonds = DIAMONDS_2;

        float bias = SimpleBias.getBiasValue(0, twoClubs, twoDiamonds);

        assertThat(bias).isEqualTo(0.001f); // Very tight table, low pair has minimal value
    }

    // ========================================
    // Suited vs Unsuited Tests
    // ========================================

    @Test
    void should_ReturnHigherBias_When_SuitedHighCards()
    {
        // Ace-King suited
        Card aceSpades = SPADES_A;
        Card kingSpades = SPADES_K;

        float suitedBias = SimpleBias.getBiasValue(1, aceSpades, kingSpades);

        // Ace-King unsuited
        Card aceHearts = HEARTS_A;
        Card kingDiamonds = DIAMONDS_K;

        float unsuitedBias = SimpleBias.getBiasValue(1, aceHearts, kingDiamonds);

        assertThat(suitedBias).isGreaterThan(unsuitedBias);
    }

    // ========================================
    // Table Index Tests
    // ========================================

    @Test
    void should_ReturnMaxBias_When_AnyHandAt100PercentTable()
    {
        // Table index 10 is 100% (plays all hands)
        Card twoClubs = CLUBS_2;
        Card sevenDiamonds = DIAMONDS_7;

        float bias = SimpleBias.getBiasValue(10, twoClubs, sevenDiamonds);

        assertThat(bias).isEqualTo(1.0f);
    }

    @Test
    void should_ReturnIncreasingBias_When_IncreasingTableIndex()
    {
        Card aceSpades = SPADES_A;
        Card queenSpades = SPADES_Q;

        float bias0 = SimpleBias.getBiasValue(0, aceSpades, queenSpades);
        float bias5 = SimpleBias.getBiasValue(5, aceSpades, queenSpades);
        float bias10 = SimpleBias.getBiasValue(10, aceSpades, queenSpades);

        assertThat(bias5).isGreaterThan(bias0);
        assertThat(bias10).isGreaterThanOrEqualTo(bias5);
    }

    // ========================================
    // Hand Object Tests
    // ========================================

    @Test
    void should_ReturnCorrectBias_When_HandObjectProvided()
    {
        Card kingClubs = CLUBS_K;
        Card kingHearts = HEARTS_K;
        Hand pocket = new Hand(kingClubs, kingHearts);

        float biasFromHand = SimpleBias.getBiasValue(2, pocket);
        float biasFromCards = SimpleBias.getBiasValue(2, kingClubs, kingHearts);

        assertThat(biasFromHand).isEqualTo(biasFromCards);
    }

    @Test
    void should_ReturnSameBias_When_CardOrderReversed()
    {
        Card jack = SPADES_J;
        Card ten = HEARTS_T;

        float bias1 = SimpleBias.getBiasValue(3, jack, ten);
        float bias2 = SimpleBias.getBiasValue(3, ten, jack);

        assertThat(bias1).isEqualTo(bias2);
    }

    // ========================================
    // Card Index Tests
    // ========================================

    @Test
    void should_ReturnCorrectBias_When_CardIndicesProvided()
    {
        // Ace of spades = 51, King of hearts = 50
        float biasFromIndices = SimpleBias.getBiasValue(4, 51, 50);

        Card aceSpades = SPADES_A;
        Card kingHearts = HEARTS_K;
        float biasFromCards = SimpleBias.getBiasValue(4, aceSpades, kingHearts);

        assertThat(biasFromIndices).isEqualTo(biasFromCards);
    }

    // ========================================
    // Value Range Tests
    // ========================================

    @Test
    void should_ReturnValueBetweenZeroAndOne_When_AnyValidInput()
    {
        Card queen = DIAMONDS_Q;
        Card jack = CLUBS_J;

        for (int tableIndex = 0; tableIndex <= 10; tableIndex++)
        {
            float bias = SimpleBias.getBiasValue(tableIndex, queen, jack);

            assertThat(bias)
                .isGreaterThanOrEqualTo(0.0f)
                .isLessThanOrEqualTo(1.0f);
        }
    }

    // ========================================
    // Specific Hand Type Tests
    // ========================================

    @Test
    void should_ReturnHighBias_When_PremiumPocketPair()
    {
        Card queenSpades = SPADES_Q;
        Card queenHearts = HEARTS_Q;

        float bias = SimpleBias.getBiasValue(5, queenSpades, queenHearts);

        assertThat(bias).isGreaterThan(0.6f); // Premium pairs should have high bias
    }

    @Test
    void should_ReturnLowerBias_When_LowUnsuitedCards()
    {
        Card five = CLUBS_5;
        Card three = DIAMONDS_3;

        float biasLoose = SimpleBias.getBiasValue(0, five, three); // Tight table (0%)

        assertThat(biasLoose).isLessThan(0.1f); // Low unsuited should have very low bias in tight tables
    }

    @Test
    void should_HandleAllTableIndices_When_ValidRange()
    {
        Card ace = SPADES_A;
        Card king = SPADES_K;

        // All table indices from 0-10 should work
        for (int i = 0; i <= 10; i++)
        {
            float bias = SimpleBias.getBiasValue(i, ace, king);
            assertThat(bias).isBetween(0.0f, 1.0f);
        }
    }

    // ========================================
    // Additional Edge Cases
    // ========================================

    @Test
    void should_ShowProgressiveIncrease_When_ComparingAllTableLevels()
    {
        Card ace = SPADES_A;
        Card queen = SPADES_Q;

        float previousBias = -1.0f;

        // Bias should generally increase or stay same as table index increases
        for (int i = 0; i <= 10; i++)
        {
            float bias = SimpleBias.getBiasValue(i, ace, queen);

            if (previousBias >= 0)
            {
                assertThat(bias).isGreaterThanOrEqualTo(previousBias);
            }

            previousBias = bias;
        }
    }

    @Test
    void should_ReturnHigherBias_When_SuitedConnectors()
    {
        // Suited connectors should have higher bias than unsuited
        float suited89 = SimpleBias.getBiasValue(5, SPADES_8, SPADES_9);
        float unsuited89 = SimpleBias.getBiasValue(5, SPADES_8, HEARTS_9);

        assertThat(suited89).isGreaterThan(unsuited89);
    }

    @Test
    void should_ReturnLowerBias_When_GapHands()
    {
        // Gap hands (non-connectors) should have lower bias than connectors
        float connector = SimpleBias.getBiasValue(5, SPADES_9, SPADES_T);
        float oneGap = SimpleBias.getBiasValue(5, SPADES_8, SPADES_T);

        assertThat(connector).isGreaterThanOrEqualTo(oneGap);
    }

    @Test
    void should_ReturnConsistentBias_When_SamePairAcrossTables()
    {
        // Pocket jacks should have different bias at different tables
        float tightJacks = SimpleBias.getBiasValue(0, SPADES_J, HEARTS_J);
        float looseJacks = SimpleBias.getBiasValue(10, SPADES_J, HEARTS_J);

        assertThat(looseJacks).isGreaterThan(tightJacks);
        assertThat(looseJacks).isEqualTo(1.0f); // Should be 1.0 at 100% table
    }

    @Test
    void should_TestAllThirteenRanks_When_IteratingPairs()
    {
        // Test that all 13 ranks work correctly at middle table
        Hand[] pairs = {
            new Hand(SPADES_A, HEARTS_A),
            new Hand(SPADES_K, HEARTS_K),
            new Hand(SPADES_Q, HEARTS_Q),
            new Hand(SPADES_J, HEARTS_J),
            new Hand(SPADES_T, HEARTS_T),
            new Hand(SPADES_9, HEARTS_9),
            new Hand(SPADES_8, HEARTS_8),
            new Hand(SPADES_7, HEARTS_7),
            new Hand(SPADES_6, HEARTS_6),
            new Hand(SPADES_5, HEARTS_5),
            new Hand(SPADES_4, HEARTS_4),
            new Hand(SPADES_3, HEARTS_3),
            new Hand(SPADES_2, HEARTS_2)
        };

        for (Hand pair : pairs)
        {
            float bias = SimpleBias.getBiasValue(5, pair);
            assertThat(bias).isBetween(0.0f, 1.0f);
        }
    }

    @Test
    void should_DifferentiateBetweenHighAndLowCards_When_SameSuit()
    {
        // High suited cards should have higher bias than low suited cards
        float highSuited = SimpleBias.getBiasValue(3, SPADES_A, SPADES_K);
        float lowSuited = SimpleBias.getBiasValue(3, SPADES_3, SPADES_2);

        assertThat(highSuited).isGreaterThan(lowSuited);
    }

    @Test
    void should_HandleTableBoundaries_When_ComparingExtremes()
    {
        Card ace = SPADES_A;
        Card two = CLUBS_2;

        // Table 0 (0%) - ultra tight
        float ultraTight = SimpleBias.getBiasValue(0, ace, two);

        // Table 10 (100%) - play all hands
        float playAll = SimpleBias.getBiasValue(10, ace, two);

        assertThat(playAll).isEqualTo(1.0f);
        assertThat(ultraTight).isLessThan(playAll);
    }

    @Test
    void should_ReturnMaxBias_When_PremiumHandsAt100PercentTable()
    {
        // At 100% table, all hands should have bias of 1.0
        assertThat(SimpleBias.getBiasValue(10, SPADES_A, HEARTS_A)).isEqualTo(1.0f);
        assertThat(SimpleBias.getBiasValue(10, SPADES_A, SPADES_K)).isEqualTo(1.0f);
        assertThat(SimpleBias.getBiasValue(10, CLUBS_7, DIAMONDS_2)).isEqualTo(1.0f);
    }
}
