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

import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PocketMatrixInt storage of int values for all possible poker pockets.
 */
class PocketMatrixIntTest
{
    private PocketMatrixInt matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new PocketMatrixInt();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_InitializeWithZeros_When_DefaultConstructor()
    {
        assertThat(matrix.get(0, 1)).isZero();
        assertThat(matrix.get(10, 20)).isZero();
        assertThat(matrix.get(50, 51)).isZero();
    }

    @Test
    void should_InitializeWithValue_When_ValueConstructor()
    {
        PocketMatrixInt matrixWith42 = new PocketMatrixInt(42);

        assertThat(matrixWith42.get(0, 1)).isEqualTo(42);
        assertThat(matrixWith42.get(10, 20)).isEqualTo(42);
        assertThat(matrixWith42.get(50, 51)).isEqualTo(42);
    }

    // ========================================
    // Set/Get Tests with Card Indices
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardIndices()
    {
        matrix.set(5, 10, 100);

        assertThat(matrix.get(5, 10)).isEqualTo(100);
    }

    @Test
    void should_ReturnSameValue_When_IndicesReversed()
    {
        matrix.set(5, 10, 100);

        assertThat(matrix.get(10, 5)).isEqualTo(100);
        assertThat(matrix.get(5, 10)).isEqualTo(100);
    }

    @Test
    void should_OverwritePreviousValue_When_SettingSameIndicesTwice()
    {
        matrix.set(7, 14, 50);
        matrix.set(7, 14, 75);

        assertThat(matrix.get(7, 14)).isEqualTo(75);
    }

    // ========================================
    // Set/Get Tests with Card Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardObjects()
    {
        matrix.set(SPADES_A, HEARTS_K, 200);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo(200);
    }

    @Test
    void should_ReturnSameValue_When_CardObjectsReversed()
    {
        matrix.set(CLUBS_2, DIAMONDS_3, 150);

        assertThat(matrix.get(DIAMONDS_3, CLUBS_2)).isEqualTo(150);
        assertThat(matrix.get(CLUBS_2, DIAMONDS_3)).isEqualTo(150);
    }

    // ========================================
    // Set/Get Tests with Hand Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByHandObject()
    {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);

        matrix.set(pocket, 300);

        assertThat(matrix.get(pocket)).isEqualTo(300);
    }

    @Test
    void should_ReturnSameValue_When_AccessedViaHandOrIndividualCards()
    {
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        matrix.set(pocket, 250);

        assertThat(matrix.get(CLUBS_J, SPADES_T)).isEqualTo(250);
        assertThat(matrix.get(pocket)).isEqualTo(250);
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test
    void should_SetAllValuesToZero_When_ClearWithZero()
    {
        matrix.set(5, 10, 100);
        matrix.set(20, 30, 200);

        matrix.clear(0);

        assertThat(matrix.get(5, 10)).isZero();
        assertThat(matrix.get(20, 30)).isZero();
    }

    @Test
    void should_SetAllValuesToSpecified_When_ClearWithValue()
    {
        matrix.set(5, 10, 100);
        matrix.set(20, 30, 200);

        matrix.clear(999);

        assertThat(matrix.get(5, 10)).isEqualTo(999);
        assertThat(matrix.get(20, 30)).isEqualTo(999);
        assertThat(matrix.get(0, 1)).isEqualTo(999);
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    void should_HandleNegativeValues_When_SettingAndGetting()
    {
        matrix.set(10, 20, -500);

        assertThat(matrix.get(10, 20)).isEqualTo(-500);
    }

    @Test
    void should_HandleMaxIntValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Integer.MAX_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void should_HandleMinIntValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Integer.MIN_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void should_HandleBoundaryCardIndices_When_SettingAndGetting()
    {
        // 0 and 51 are valid card indices (52 cards total)
        matrix.set(0, 51, 777);

        assertThat(matrix.get(0, 51)).isEqualTo(777);
        assertThat(matrix.get(51, 0)).isEqualTo(777);
    }

    @Test
    void should_HandleAllSuitedPairs_When_SettingDifferentValues()
    {
        // Aces of different suits
        matrix.set(SPADES_A, HEARTS_A, 100);
        matrix.set(SPADES_A, DIAMONDS_A, 200);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo(100);
        assertThat(matrix.get(SPADES_A, DIAMONDS_A)).isEqualTo(200);
    }

    // ========================================
    // Additional Edge Cases
    // ========================================

    @Test
    void should_HandleZeroIndex_When_UsingLowestCardIndices()
    {
        // Test with index 0 (lowest valid index)
        matrix.set(0, 1, 999);

        assertThat(matrix.get(0, 1)).isEqualTo(999);
        assertThat(matrix.get(1, 0)).isEqualTo(999);
    }

    @Test
    void should_StoreAllPocketPairs_When_SettingSequentially()
    {
        // Test all pocket pairs systematically
        matrix.set(SPADES_A, HEARTS_A, 1400);
        matrix.set(SPADES_K, HEARTS_K, 1300);
        matrix.set(SPADES_Q, HEARTS_Q, 1200);
        matrix.set(SPADES_J, HEARTS_J, 1100);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo(1400);
        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo(1300);
        assertThat(matrix.get(SPADES_Q, HEARTS_Q)).isEqualTo(1200);
        assertThat(matrix.get(SPADES_J, HEARTS_J)).isEqualTo(1100);
    }

    @Test
    void should_KeepPairsDistinct_When_SameRankDifferentSuits()
    {
        // Multiple kings with different suits
        matrix.set(SPADES_K, HEARTS_K, 10);
        matrix.set(SPADES_K, DIAMONDS_K, 20);
        matrix.set(SPADES_K, CLUBS_K, 30);
        matrix.set(HEARTS_K, DIAMONDS_K, 40);

        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo(10);
        assertThat(matrix.get(SPADES_K, DIAMONDS_K)).isEqualTo(20);
        assertThat(matrix.get(SPADES_K, CLUBS_K)).isEqualTo(30);
        assertThat(matrix.get(HEARTS_K, DIAMONDS_K)).isEqualTo(40);
    }

    @Test
    void should_HandleMaximumIndexCombinations_When_Using51()
    {
        // Index 51 is the highest valid card index
        matrix.set(50, 51, 5051);
        matrix.set(49, 51, 4951);

        assertThat(matrix.get(50, 51)).isEqualTo(5051);
        assertThat(matrix.get(51, 50)).isEqualTo(5051);
        assertThat(matrix.get(49, 51)).isEqualTo(4951);
    }

    @Test
    void should_PreserveOtherValues_When_ClearingAndResetting()
    {
        // Set some values
        matrix.set(SPADES_A, HEARTS_K, 100);
        matrix.set(CLUBS_Q, DIAMONDS_J, 200);

        // Clear to different value
        matrix.clear(555);

        // All should be 555 now
        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo(555);
        assertThat(matrix.get(CLUBS_Q, DIAMONDS_J)).isEqualTo(555);

        // Set new values
        matrix.set(SPADES_A, HEARTS_K, 777);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo(777);
        assertThat(matrix.get(CLUBS_Q, DIAMONDS_J)).isEqualTo(555);
    }

    @Test
    void should_HandleAllCombinations_When_SettingMultipleCardPairs()
    {
        // Test multiple different combinations
        matrix.set(SPADES_A, CLUBS_2, 1402);
        matrix.set(HEARTS_K, DIAMONDS_3, 1303);
        matrix.set(SPADES_Q, CLUBS_4, 1204);
        matrix.set(HEARTS_J, DIAMONDS_5, 1105);
        matrix.set(SPADES_T, CLUBS_6, 1006);

        assertThat(matrix.get(SPADES_A, CLUBS_2)).isEqualTo(1402);
        assertThat(matrix.get(HEARTS_K, DIAMONDS_3)).isEqualTo(1303);
        assertThat(matrix.get(SPADES_Q, CLUBS_4)).isEqualTo(1204);
        assertThat(matrix.get(HEARTS_J, DIAMONDS_5)).isEqualTo(1105);
        assertThat(matrix.get(SPADES_T, CLUBS_6)).isEqualTo(1006);
    }

    @Test
    void should_HandleSequentialIndices_When_TestingAdjacentCards()
    {
        // Test adjacent card indices
        matrix.set(0, 1, 100);
        matrix.set(1, 2, 200);
        matrix.set(2, 3, 300);

        assertThat(matrix.get(0, 1)).isEqualTo(100);
        assertThat(matrix.get(1, 2)).isEqualTo(200);
        assertThat(matrix.get(2, 3)).isEqualTo(300);
    }
}
