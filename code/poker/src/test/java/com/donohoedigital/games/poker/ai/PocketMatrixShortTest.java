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

import com.donohoedigital.games.poker.engine.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.donohoedigital.games.poker.engine.Card.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PocketMatrixShort storage of short values for all possible poker pockets.
 */
class PocketMatrixShortTest
{
    private PocketMatrixShort matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new PocketMatrixShort();
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
        PocketMatrixShort matrixWith42 = new PocketMatrixShort((short) 42);

        assertThat(matrixWith42.get(0, 1)).isEqualTo((short) 42);
        assertThat(matrixWith42.get(10, 20)).isEqualTo((short) 42);
        assertThat(matrixWith42.get(50, 51)).isEqualTo((short) 42);
    }

    // ========================================
    // Set/Get Tests with Card Indices
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardIndices()
    {
        matrix.set(5, 10, (short) 100);

        assertThat(matrix.get(5, 10)).isEqualTo((short) 100);
    }

    @Test
    void should_ReturnSameValue_When_IndicesReversed()
    {
        matrix.set(5, 10, (short) 100);

        assertThat(matrix.get(10, 5)).isEqualTo((short) 100);
        assertThat(matrix.get(5, 10)).isEqualTo((short) 100);
    }

    @Test
    void should_OverwritePreviousValue_When_SettingSameIndicesTwice()
    {
        matrix.set(7, 14, (short) 50);
        matrix.set(7, 14, (short) 75);

        assertThat(matrix.get(7, 14)).isEqualTo((short) 75);
    }

    // ========================================
    // Set/Get Tests with Card Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardObjects()
    {
        matrix.set(SPADES_A, HEARTS_K, (short) 200);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo((short) 200);
    }

    @Test
    void should_ReturnSameValue_When_CardObjectsReversed()
    {
        matrix.set(CLUBS_2, DIAMONDS_3, (short) 150);

        assertThat(matrix.get(DIAMONDS_3, CLUBS_2)).isEqualTo((short) 150);
        assertThat(matrix.get(CLUBS_2, DIAMONDS_3)).isEqualTo((short) 150);
    }

    // ========================================
    // Set/Get Tests with Hand Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByHandObject()
    {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);

        matrix.set(pocket, (short) 300);

        assertThat(matrix.get(pocket)).isEqualTo((short) 300);
    }

    @Test
    void should_ReturnSameValue_When_AccessedViaHandOrIndividualCards()
    {
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        matrix.set(pocket, (short) 250);

        assertThat(matrix.get(CLUBS_J, SPADES_T)).isEqualTo((short) 250);
        assertThat(matrix.get(pocket)).isEqualTo((short) 250);
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test
    void should_SetAllValuesToZero_When_ClearWithZero()
    {
        matrix.set(5, 10, (short) 100);
        matrix.set(20, 30, (short) 200);

        matrix.clear((short) 0);

        assertThat(matrix.get(5, 10)).isZero();
        assertThat(matrix.get(20, 30)).isZero();
    }

    @Test
    void should_SetAllValuesToSpecified_When_ClearWithValue()
    {
        matrix.set(5, 10, (short) 100);
        matrix.set(20, 30, (short) 200);

        matrix.clear((short) 999);

        assertThat(matrix.get(5, 10)).isEqualTo((short) 999);
        assertThat(matrix.get(20, 30)).isEqualTo((short) 999);
        assertThat(matrix.get(0, 1)).isEqualTo((short) 999);
    }

    // ========================================
    // Short-Specific Edge Cases
    // ========================================

    @Test
    void should_HandleNegativeValues_When_SettingAndGetting()
    {
        matrix.set(10, 20, (short) -500);

        assertThat(matrix.get(10, 20)).isEqualTo((short) -500);
    }

    @Test
    void should_HandleMaxShortValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Short.MAX_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Short.MAX_VALUE);
    }

    @Test
    void should_HandleMinShortValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Short.MIN_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Short.MIN_VALUE);
    }

    // ========================================
    // Boundary Tests
    // ========================================

    @Test
    void should_HandleBoundaryCardIndices_When_SettingAndGetting()
    {
        // 0 and 51 are valid card indices (52 cards total)
        matrix.set(0, 51, (short) 777);

        assertThat(matrix.get(0, 51)).isEqualTo((short) 777);
        assertThat(matrix.get(51, 0)).isEqualTo((short) 777);
    }

    @Test
    void should_HandleAllSuitedPairs_When_SettingDifferentValues()
    {
        // Aces of different suits
        matrix.set(SPADES_A, HEARTS_A, (short) 100);
        matrix.set(SPADES_A, DIAMONDS_A, (short) 200);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo((short) 100);
        assertThat(matrix.get(SPADES_A, DIAMONDS_A)).isEqualTo((short) 200);
    }

    // ========================================
    // Additional Edge Cases
    // ========================================

    @Test
    void should_HandleZeroIndex_When_UsingLowestCardIndices()
    {
        // Test with index 0 (lowest valid index)
        matrix.set(0, 1, (short) 999);

        assertThat(matrix.get(0, 1)).isEqualTo((short) 999);
        assertThat(matrix.get(1, 0)).isEqualTo((short) 999);
    }

    @Test
    void should_StoreAllPocketPairs_When_SettingSequentially()
    {
        // Test all pocket pairs systematically
        matrix.set(SPADES_A, HEARTS_A, (short) 1400);
        matrix.set(SPADES_K, HEARTS_K, (short) 1300);
        matrix.set(SPADES_Q, HEARTS_Q, (short) 1200);
        matrix.set(SPADES_J, HEARTS_J, (short) 1100);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo((short) 1400);
        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo((short) 1300);
        assertThat(matrix.get(SPADES_Q, HEARTS_Q)).isEqualTo((short) 1200);
        assertThat(matrix.get(SPADES_J, HEARTS_J)).isEqualTo((short) 1100);
    }

    @Test
    void should_KeepPairsDistinct_When_SameRankDifferentSuits()
    {
        // Multiple kings with different suits
        matrix.set(SPADES_K, HEARTS_K, (short) 10);
        matrix.set(SPADES_K, DIAMONDS_K, (short) 20);
        matrix.set(SPADES_K, CLUBS_K, (short) 30);
        matrix.set(HEARTS_K, DIAMONDS_K, (short) 40);

        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo((short) 10);
        assertThat(matrix.get(SPADES_K, DIAMONDS_K)).isEqualTo((short) 20);
        assertThat(matrix.get(SPADES_K, CLUBS_K)).isEqualTo((short) 30);
        assertThat(matrix.get(HEARTS_K, DIAMONDS_K)).isEqualTo((short) 40);
    }

    @Test
    void should_HandleMaximumIndexCombinations_When_Using51()
    {
        // Index 51 is the highest valid card index
        matrix.set(50, 51, (short) 5051);
        matrix.set(49, 51, (short) 4951);

        assertThat(matrix.get(50, 51)).isEqualTo((short) 5051);
        assertThat(matrix.get(51, 50)).isEqualTo((short) 5051);
        assertThat(matrix.get(49, 51)).isEqualTo((short) 4951);
    }

    @Test
    void should_HandleSequentialIndices_When_TestingAdjacentCards()
    {
        // Test adjacent card indices
        matrix.set(0, 1, (short) 100);
        matrix.set(1, 2, (short) 200);
        matrix.set(2, 3, (short) 300);

        assertThat(matrix.get(0, 1)).isEqualTo((short) 100);
        assertThat(matrix.get(1, 2)).isEqualTo((short) 200);
        assertThat(matrix.get(2, 3)).isEqualTo((short) 300);
    }

    @Test
    void should_HandleShortRangeValues_When_SettingTypicalCounts()
    {
        // Test typical short range values (counts, small numbers)
        matrix.set(SPADES_A, HEARTS_K, (short) 1);
        matrix.set(CLUBS_K, DIAMONDS_Q, (short) 10);
        matrix.set(SPADES_J, HEARTS_T, (short) 100);
        matrix.set(CLUBS_9, DIAMONDS_8, (short) 1000);
        matrix.set(SPADES_7, HEARTS_6, (short) 10000);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo((short) 1);
        assertThat(matrix.get(CLUBS_K, DIAMONDS_Q)).isEqualTo((short) 10);
        assertThat(matrix.get(SPADES_J, HEARTS_T)).isEqualTo((short) 100);
        assertThat(matrix.get(CLUBS_9, DIAMONDS_8)).isEqualTo((short) 1000);
        assertThat(matrix.get(SPADES_7, HEARTS_6)).isEqualTo((short) 10000);
    }
}
