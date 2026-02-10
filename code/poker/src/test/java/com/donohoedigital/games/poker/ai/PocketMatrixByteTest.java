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
 * Tests for PocketMatrixByte storage of byte values for all possible poker pockets.
 */
class PocketMatrixByteTest
{
    private PocketMatrixByte matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new PocketMatrixByte();
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
        PocketMatrixByte matrixWith42 = new PocketMatrixByte((byte) 42);

        assertThat(matrixWith42.get(0, 1)).isEqualTo((byte) 42);
        assertThat(matrixWith42.get(10, 20)).isEqualTo((byte) 42);
        assertThat(matrixWith42.get(50, 51)).isEqualTo((byte) 42);
    }

    // ========================================
    // Set/Get Tests with Card Indices
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardIndices()
    {
        matrix.set(5, 10, (byte) 100);

        assertThat(matrix.get(5, 10)).isEqualTo((byte) 100);
    }

    @Test
    void should_ReturnSameValue_When_IndicesReversed()
    {
        matrix.set(5, 10, (byte) 100);

        assertThat(matrix.get(10, 5)).isEqualTo((byte) 100);
        assertThat(matrix.get(5, 10)).isEqualTo((byte) 100);
    }

    @Test
    void should_OverwritePreviousValue_When_SettingSameIndicesTwice()
    {
        matrix.set(7, 14, (byte) 50);
        matrix.set(7, 14, (byte) 75);

        assertThat(matrix.get(7, 14)).isEqualTo((byte) 75);
    }

    // ========================================
    // Set/Get Tests with Card Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardObjects()
    {
        matrix.set(SPADES_A, HEARTS_K, (byte) 127);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo((byte) 127);
    }

    @Test
    void should_ReturnSameValue_When_CardObjectsReversed()
    {
        matrix.set(CLUBS_2, DIAMONDS_3, (byte) 64);

        assertThat(matrix.get(DIAMONDS_3, CLUBS_2)).isEqualTo((byte) 64);
        assertThat(matrix.get(CLUBS_2, DIAMONDS_3)).isEqualTo((byte) 64);
    }

    // ========================================
    // Set/Get Tests with Hand Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByHandObject()
    {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);

        matrix.set(pocket, (byte) 100);

        assertThat(matrix.get(pocket)).isEqualTo((byte) 100);
    }

    @Test
    void should_ReturnSameValue_When_AccessedViaHandOrIndividualCards()
    {
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        matrix.set(pocket, (byte) 88);

        assertThat(matrix.get(CLUBS_J, SPADES_T)).isEqualTo((byte) 88);
        assertThat(matrix.get(pocket)).isEqualTo((byte) 88);
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test
    void should_SetAllValuesToZero_When_ClearWithZero()
    {
        matrix.set(5, 10, (byte) 100);
        matrix.set(20, 30, (byte) 50);

        matrix.clear((byte) 0);

        assertThat(matrix.get(5, 10)).isZero();
        assertThat(matrix.get(20, 30)).isZero();
    }

    @Test
    void should_SetAllValuesToSpecified_When_ClearWithValue()
    {
        matrix.set(5, 10, (byte) 100);
        matrix.set(20, 30, (byte) 50);

        matrix.clear((byte) 99);

        assertThat(matrix.get(5, 10)).isEqualTo((byte) 99);
        assertThat(matrix.get(20, 30)).isEqualTo((byte) 99);
        assertThat(matrix.get(0, 1)).isEqualTo((byte) 99);
    }

    // ========================================
    // Byte-Specific Edge Cases
    // ========================================

    @Test
    void should_HandleNegativeValues_When_SettingAndGetting()
    {
        matrix.set(10, 20, (byte) -50);

        assertThat(matrix.get(10, 20)).isEqualTo((byte) -50);
    }

    @Test
    void should_HandleMaxByteValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Byte.MAX_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Byte.MAX_VALUE);
    }

    @Test
    void should_HandleMinByteValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Byte.MIN_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Byte.MIN_VALUE);
    }

    // ========================================
    // Boundary Tests
    // ========================================

    @Test
    void should_HandleBoundaryCardIndices_When_SettingAndGetting()
    {
        // 0 and 51 are valid card indices (52 cards total)
        matrix.set(0, 51, (byte) 77);

        assertThat(matrix.get(0, 51)).isEqualTo((byte) 77);
        assertThat(matrix.get(51, 0)).isEqualTo((byte) 77);
    }

    @Test
    void should_HandleAllSuitedPairs_When_SettingDifferentValues()
    {
        // Aces of different suits
        matrix.set(SPADES_A, HEARTS_A, (byte) 10);
        matrix.set(SPADES_A, DIAMONDS_A, (byte) 20);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo((byte) 10);
        assertThat(matrix.get(SPADES_A, DIAMONDS_A)).isEqualTo((byte) 20);
    }

    // ========================================
    // Additional Edge Cases
    // ========================================

    @Test
    void should_HandleZeroIndex_When_UsingLowestCardIndices()
    {
        // Test with index 0 (lowest valid index)
        matrix.set(0, 1, (byte) 99);

        assertThat(matrix.get(0, 1)).isEqualTo((byte) 99);
        assertThat(matrix.get(1, 0)).isEqualTo((byte) 99);
    }

    @Test
    void should_StoreAllPocketPairs_When_SettingSequentially()
    {
        // Test all pocket pairs systematically
        matrix.set(SPADES_A, HEARTS_A, (byte) 14);
        matrix.set(SPADES_K, HEARTS_K, (byte) 13);
        matrix.set(SPADES_Q, HEARTS_Q, (byte) 12);
        matrix.set(SPADES_J, HEARTS_J, (byte) 11);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo((byte) 14);
        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo((byte) 13);
        assertThat(matrix.get(SPADES_Q, HEARTS_Q)).isEqualTo((byte) 12);
        assertThat(matrix.get(SPADES_J, HEARTS_J)).isEqualTo((byte) 11);
    }

    @Test
    void should_KeepPairsDistinct_When_SameRankDifferentSuits()
    {
        // Multiple kings with different suits
        matrix.set(SPADES_K, HEARTS_K, (byte) 1);
        matrix.set(SPADES_K, DIAMONDS_K, (byte) 2);
        matrix.set(SPADES_K, CLUBS_K, (byte) 3);
        matrix.set(HEARTS_K, DIAMONDS_K, (byte) 4);

        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo((byte) 1);
        assertThat(matrix.get(SPADES_K, DIAMONDS_K)).isEqualTo((byte) 2);
        assertThat(matrix.get(SPADES_K, CLUBS_K)).isEqualTo((byte) 3);
        assertThat(matrix.get(HEARTS_K, DIAMONDS_K)).isEqualTo((byte) 4);
    }

    @Test
    void should_HandleMaximumIndexCombinations_When_Using51()
    {
        // Index 51 is the highest valid card index
        matrix.set(50, 51, (byte) 51);
        matrix.set(49, 51, (byte) 49);

        assertThat(matrix.get(50, 51)).isEqualTo((byte) 51);
        assertThat(matrix.get(51, 50)).isEqualTo((byte) 51);
        assertThat(matrix.get(49, 51)).isEqualTo((byte) 49);
    }

    @Test
    void should_HandleSequentialIndices_When_TestingAdjacentCards()
    {
        // Test adjacent card indices
        matrix.set(0, 1, (byte) 1);
        matrix.set(1, 2, (byte) 2);
        matrix.set(2, 3, (byte) 3);

        assertThat(matrix.get(0, 1)).isEqualTo((byte) 1);
        assertThat(matrix.get(1, 2)).isEqualTo((byte) 2);
        assertThat(matrix.get(2, 3)).isEqualTo((byte) 3);
    }

    @Test
    void should_HandleByteRangeValues_When_SettingTypicalFlags()
    {
        // Test typical byte range values (flags, small counts)
        matrix.set(SPADES_A, HEARTS_K, (byte) 0);
        matrix.set(CLUBS_K, DIAMONDS_Q, (byte) 1);
        matrix.set(SPADES_J, HEARTS_T, (byte) 10);
        matrix.set(CLUBS_9, DIAMONDS_8, (byte) 100);
        matrix.set(SPADES_7, HEARTS_6, (byte) 127);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo((byte) 0);
        assertThat(matrix.get(CLUBS_K, DIAMONDS_Q)).isEqualTo((byte) 1);
        assertThat(matrix.get(SPADES_J, HEARTS_T)).isEqualTo((byte) 10);
        assertThat(matrix.get(CLUBS_9, DIAMONDS_8)).isEqualTo((byte) 100);
        assertThat(matrix.get(SPADES_7, HEARTS_6)).isEqualTo((byte) 127);
    }

    @Test
    void should_HandleNegativeByteRange_When_SettingNegativeFlags()
    {
        // Test negative byte values
        matrix.set(SPADES_A, HEARTS_K, (byte) -1);
        matrix.set(CLUBS_K, DIAMONDS_Q, (byte) -10);
        matrix.set(SPADES_J, HEARTS_T, (byte) -100);
        matrix.set(CLUBS_9, DIAMONDS_8, (byte) -128);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo((byte) -1);
        assertThat(matrix.get(CLUBS_K, DIAMONDS_Q)).isEqualTo((byte) -10);
        assertThat(matrix.get(SPADES_J, HEARTS_T)).isEqualTo((byte) -100);
        assertThat(matrix.get(CLUBS_9, DIAMONDS_8)).isEqualTo((byte) -128);
    }
}
