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
 * Tests for PocketMatrixFloat storage of float values for all possible poker pockets.
 */
class PocketMatrixFloatTest
{
    private PocketMatrixFloat matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new PocketMatrixFloat();
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
        PocketMatrixFloat matrixWith0_5 = new PocketMatrixFloat(0.5f);

        assertThat(matrixWith0_5.get(0, 1)).isEqualTo(0.5f);
        assertThat(matrixWith0_5.get(10, 20)).isEqualTo(0.5f);
        assertThat(matrixWith0_5.get(50, 51)).isEqualTo(0.5f);
    }

    @Test
    void should_CopyAllValues_When_CopyConstructor()
    {
        PocketMatrixFloat original = new PocketMatrixFloat();
        original.set(5, 10, 0.123f);
        original.set(20, 30, 0.456f);

        PocketMatrixFloat copy = new PocketMatrixFloat(original);

        assertThat(copy.get(5, 10)).isEqualTo(0.123f);
        assertThat(copy.get(20, 30)).isEqualTo(0.456f);
    }

    @Test
    void should_BeIndependent_When_CopyConstructorUsed()
    {
        PocketMatrixFloat original = new PocketMatrixFloat();
        original.set(5, 10, 0.5f);

        PocketMatrixFloat copy = new PocketMatrixFloat(original);
        copy.set(5, 10, 0.9f);

        assertThat(original.get(5, 10)).isEqualTo(0.5f);
        assertThat(copy.get(5, 10)).isEqualTo(0.9f);
    }

    // ========================================
    // Set/Get Tests with Card Indices
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardIndices()
    {
        matrix.set(5, 10, 0.75f);

        assertThat(matrix.get(5, 10)).isEqualTo(0.75f);
    }

    @Test
    void should_ReturnSameValue_When_IndicesReversed()
    {
        matrix.set(5, 10, 0.333f);

        assertThat(matrix.get(10, 5)).isEqualTo(0.333f);
        assertThat(matrix.get(5, 10)).isEqualTo(0.333f);
    }

    @Test
    void should_OverwritePreviousValue_When_SettingSameIndicesTwice()
    {
        matrix.set(7, 14, 0.25f);
        matrix.set(7, 14, 0.99f);

        assertThat(matrix.get(7, 14)).isEqualTo(0.99f);
    }

    // ========================================
    // Set/Get Tests with Card Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardObjects()
    {
        matrix.set(SPADES_A, HEARTS_K, 0.875f);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo(0.875f);
    }

    @Test
    void should_ReturnSameValue_When_CardObjectsReversed()
    {
        matrix.set(CLUBS_2, DIAMONDS_3, 0.111f);

        assertThat(matrix.get(DIAMONDS_3, CLUBS_2)).isEqualTo(0.111f);
        assertThat(matrix.get(CLUBS_2, DIAMONDS_3)).isEqualTo(0.111f);
    }

    // ========================================
    // Set/Get Tests with Hand Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByHandObject()
    {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);

        matrix.set(pocket, 0.999f);

        assertThat(matrix.get(pocket)).isEqualTo(0.999f);
    }

    @Test
    void should_ReturnSameValue_When_AccessedViaHandOrIndividualCards()
    {
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        matrix.set(pocket, 0.625f);

        assertThat(matrix.get(CLUBS_J, SPADES_T)).isEqualTo(0.625f);
        assertThat(matrix.get(pocket)).isEqualTo(0.625f);
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test
    void should_SetAllValuesToZero_When_ClearWithZero()
    {
        matrix.set(5, 10, 0.5f);
        matrix.set(20, 30, 0.8f);

        matrix.clear(0.0f);

        assertThat(matrix.get(5, 10)).isZero();
        assertThat(matrix.get(20, 30)).isZero();
    }

    @Test
    void should_SetAllValuesToSpecified_When_ClearWithValue()
    {
        matrix.set(5, 10, 0.5f);
        matrix.set(20, 30, 0.8f);

        matrix.clear(0.42f);

        assertThat(matrix.get(5, 10)).isEqualTo(0.42f);
        assertThat(matrix.get(20, 30)).isEqualTo(0.42f);
        assertThat(matrix.get(0, 1)).isEqualTo(0.42f);
    }

    // ========================================
    // Floating Point Edge Cases
    // ========================================

    @Test
    void should_HandleVerySmallFloats_When_SettingNearZero()
    {
        matrix.set(10, 20, 0.0001f);

        assertThat(matrix.get(10, 20)).isEqualTo(0.0001f);
    }

    @Test
    void should_HandleNegativeFloats_When_SettingAndGetting()
    {
        matrix.set(10, 20, -0.5f);

        assertThat(matrix.get(10, 20)).isEqualTo(-0.5f);
    }

    @Test
    void should_HandleVeryLargeFloats_When_SettingAndGetting()
    {
        matrix.set(15, 25, 999999.999f);

        assertThat(matrix.get(15, 25)).isEqualTo(999999.999f);
    }

    @Test
    void should_HandleFloatMaxValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Float.MAX_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Float.MAX_VALUE);
    }

    @Test
    void should_HandleFloatMinValue_When_SettingAndGetting()
    {
        matrix.set(15, 25, Float.MIN_VALUE);

        assertThat(matrix.get(15, 25)).isEqualTo(Float.MIN_VALUE);
    }

    @Test
    void should_HandlePrecisionFloats_When_SettingDecimalValues()
    {
        matrix.set(10, 20, 0.123456789f);

        // Float precision is about 7 decimal digits
        assertThat(matrix.get(10, 20)).isCloseTo(0.123456789f, within(0.0000001f));
    }

    // ========================================
    // Negative Index Edge Cases
    // ========================================

    @Test
    void should_ReturnZero_When_NegativeIndexProvided()
    {
        // Based on the implementation, negative indices return 0
        assertThat(matrix.get(-1, 5)).isZero();
    }

    // ========================================
    // Boundary Tests
    // ========================================

    @Test
    void should_HandleBoundaryCardIndices_When_SettingAndGetting()
    {
        // 0 and 51 are valid card indices (52 cards total)
        matrix.set(0, 51, 0.0051f);

        assertThat(matrix.get(0, 51)).isEqualTo(0.0051f);
        assertThat(matrix.get(51, 0)).isEqualTo(0.0051f);
    }

    @Test
    void should_HandleAllSuitedPairs_When_SettingDifferentValues()
    {
        // Aces of different suits
        matrix.set(SPADES_A, HEARTS_A, 0.1f);
        matrix.set(SPADES_A, DIAMONDS_A, 0.2f);

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo(0.1f);
        assertThat(matrix.get(SPADES_A, DIAMONDS_A)).isEqualTo(0.2f);
    }

    @Test
    void should_HandleSequentialIndices_When_TestingAdjacentCards()
    {
        // Test adjacent card indices
        matrix.set(0, 1, 0.01f);
        matrix.set(1, 2, 0.12f);
        matrix.set(2, 3, 0.23f);

        assertThat(matrix.get(0, 1)).isEqualTo(0.01f);
        assertThat(matrix.get(1, 2)).isEqualTo(0.12f);
        assertThat(matrix.get(2, 3)).isEqualTo(0.23f);
    }

    @Test
    void should_HandleCommonProbabilityValues_When_SettingTypicalRanges()
    {
        // Test common probability values (0.0 to 1.0)
        matrix.set(SPADES_A, HEARTS_K, 1.0f);
        matrix.set(CLUBS_K, DIAMONDS_Q, 0.8f);
        matrix.set(SPADES_J, HEARTS_T, 0.5f);
        matrix.set(CLUBS_9, DIAMONDS_8, 0.2f);
        matrix.set(SPADES_7, HEARTS_6, 0.0f);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo(1.0f);
        assertThat(matrix.get(CLUBS_K, DIAMONDS_Q)).isEqualTo(0.8f);
        assertThat(matrix.get(SPADES_J, HEARTS_T)).isEqualTo(0.5f);
        assertThat(matrix.get(CLUBS_9, DIAMONDS_8)).isEqualTo(0.2f);
        assertThat(matrix.get(SPADES_7, HEARTS_6)).isEqualTo(0.0f);
    }
}
