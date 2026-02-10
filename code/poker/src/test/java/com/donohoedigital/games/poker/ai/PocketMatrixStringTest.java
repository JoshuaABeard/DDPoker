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
 * Tests for PocketMatrixString storage of String values for all possible poker pockets.
 */
class PocketMatrixStringTest
{
    private PocketMatrixString matrix;

    @BeforeEach
    void setUp()
    {
        matrix = new PocketMatrixString();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_InitializeWithNulls_When_DefaultConstructor()
    {
        assertThat(matrix.get(0, 1)).isNull();
        assertThat(matrix.get(10, 20)).isNull();
        assertThat(matrix.get(50, 51)).isNull();
    }

    @Test
    void should_InitializeWithValue_When_ValueConstructor()
    {
        PocketMatrixString matrixWithDefault = new PocketMatrixString("default");

        assertThat(matrixWithDefault.get(0, 1)).isEqualTo("default");
        assertThat(matrixWithDefault.get(10, 20)).isEqualTo("default");
        assertThat(matrixWithDefault.get(50, 51)).isEqualTo("default");
    }

    @Test
    void should_InitializeWithNull_When_NullConstructorArgument()
    {
        PocketMatrixString matrixWithNull = new PocketMatrixString(null);

        assertThat(matrixWithNull.get(0, 1)).isNull();
        assertThat(matrixWithNull.get(10, 20)).isNull();
    }

    // ========================================
    // Set/Get Tests with Card Indices
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardIndices()
    {
        matrix.set(5, 10, "test-value");

        assertThat(matrix.get(5, 10)).isEqualTo("test-value");
    }

    @Test
    void should_ReturnSameValue_When_IndicesReversed()
    {
        matrix.set(5, 10, "symmetric");

        assertThat(matrix.get(10, 5)).isEqualTo("symmetric");
        assertThat(matrix.get(5, 10)).isEqualTo("symmetric");
    }

    @Test
    void should_OverwritePreviousValue_When_SettingSameIndicesTwice()
    {
        matrix.set(7, 14, "first");
        matrix.set(7, 14, "second");

        assertThat(matrix.get(7, 14)).isEqualTo("second");
    }

    // ========================================
    // Set/Get Tests with Card Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByCardObjects()
    {
        matrix.set(SPADES_A, HEARTS_K, "AK suited");

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo("AK suited");
    }

    @Test
    void should_ReturnSameValue_When_CardObjectsReversed()
    {
        matrix.set(CLUBS_2, DIAMONDS_3, "23 off");

        assertThat(matrix.get(DIAMONDS_3, CLUBS_2)).isEqualTo("23 off");
        assertThat(matrix.get(CLUBS_2, DIAMONDS_3)).isEqualTo("23 off");
    }

    // ========================================
    // Set/Get Tests with Hand Objects
    // ========================================

    @Test
    void should_StoreAndRetrieveValue_When_SettingByHandObject()
    {
        Hand pocket = new Hand(SPADES_A, HEARTS_A);

        matrix.set(pocket, "Pocket Aces");

        assertThat(matrix.get(pocket)).isEqualTo("Pocket Aces");
    }

    @Test
    void should_ReturnSameValue_When_AccessedViaHandOrIndividualCards()
    {
        Hand pocket = new Hand(CLUBS_J, SPADES_T);

        matrix.set(pocket, "Jack-Ten");

        assertThat(matrix.get(CLUBS_J, SPADES_T)).isEqualTo("Jack-Ten");
        assertThat(matrix.get(pocket)).isEqualTo("Jack-Ten");
    }

    // ========================================
    // Clear Tests
    // ========================================

    @Test
    void should_SetAllValuesToNull_When_ClearWithNull()
    {
        matrix.set(5, 10, "value1");
        matrix.set(20, 30, "value2");

        matrix.clear(null);

        assertThat(matrix.get(5, 10)).isNull();
        assertThat(matrix.get(20, 30)).isNull();
    }

    @Test
    void should_SetAllValuesToSpecified_When_ClearWithValue()
    {
        matrix.set(5, 10, "old1");
        matrix.set(20, 30, "old2");

        matrix.clear("cleared");

        assertThat(matrix.get(5, 10)).isEqualTo("cleared");
        assertThat(matrix.get(20, 30)).isEqualTo("cleared");
        assertThat(matrix.get(0, 1)).isEqualTo("cleared");
    }

    // ========================================
    // String-Specific Tests
    // ========================================

    @Test
    void should_HandleEmptyString_When_SettingAndGetting()
    {
        matrix.set(10, 20, "");

        assertThat(matrix.get(10, 20)).isEmpty();
    }

    @Test
    void should_HandleLongString_When_SettingAndGetting()
    {
        String longString = "This is a very long string representing complex pocket hand information with lots of details";
        matrix.set(15, 25, longString);

        assertThat(matrix.get(15, 25)).isEqualTo(longString);
    }

    @Test
    void should_HandleSpecialCharacters_When_SettingAndGetting()
    {
        matrix.set(10, 20, "A♠K♥ $100 @position #3");

        assertThat(matrix.get(10, 20)).isEqualTo("A♠K♥ $100 @position #3");
    }

    @Test
    void should_HandleUnicodeCharacters_When_SettingAndGetting()
    {
        matrix.set(10, 20, "Pocket: 🃁🂱 Strength: 💪");

        assertThat(matrix.get(10, 20)).isEqualTo("Pocket: 🃁🂱 Strength: 💪");
    }

    // ========================================
    // Boundary Tests
    // ========================================

    @Test
    void should_HandleBoundaryCardIndices_When_SettingAndGetting()
    {
        // 0 and 51 are valid card indices (52 cards total)
        matrix.set(0, 51, "boundary-test");

        assertThat(matrix.get(0, 51)).isEqualTo("boundary-test");
        assertThat(matrix.get(51, 0)).isEqualTo("boundary-test");
    }

    @Test
    void should_HandleAllSuitedPairs_When_SettingDifferentValues()
    {
        // Aces of different suits
        matrix.set(SPADES_A, HEARTS_A, "A♠A♥");
        matrix.set(SPADES_A, DIAMONDS_A, "A♠A♦");

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo("A♠A♥");
        assertThat(matrix.get(SPADES_A, DIAMONDS_A)).isEqualTo("A♠A♦");
    }

    // ========================================
    // Common Use Case Tests
    // ========================================

    @Test
    void should_StoreHandNames_When_SettingCommonPokerHands()
    {
        matrix.set(SPADES_A, HEARTS_A, "Pocket Rockets");
        matrix.set(SPADES_K, HEARTS_K, "Cowboys");
        matrix.set(SPADES_Q, HEARTS_Q, "Ladies");
        matrix.set(SPADES_J, HEARTS_J, "Fishhooks");
        matrix.set(SPADES_A, SPADES_K, "Big Slick");

        assertThat(matrix.get(SPADES_A, HEARTS_A)).isEqualTo("Pocket Rockets");
        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo("Cowboys");
        assertThat(matrix.get(SPADES_Q, HEARTS_Q)).isEqualTo("Ladies");
        assertThat(matrix.get(SPADES_J, HEARTS_J)).isEqualTo("Fishhooks");
        assertThat(matrix.get(SPADES_A, SPADES_K)).isEqualTo("Big Slick");
    }

    @Test
    void should_StoreJSONStrings_When_UsedForSerialization()
    {
        String jsonData = "{\"hand\":\"AK\",\"strength\":0.85,\"position\":\"late\"}";
        matrix.set(SPADES_A, HEARTS_K, jsonData);

        assertThat(matrix.get(SPADES_A, HEARTS_K)).isEqualTo(jsonData);
    }

    @Test
    void should_HandleMultilineStrings_When_SettingAndGetting()
    {
        String multiline = "Line 1: Ace-King\nLine 2: Strong hand\nLine 3: Play aggressively";
        matrix.set(10, 20, multiline);

        assertThat(matrix.get(10, 20)).isEqualTo(multiline);
    }

    @Test
    void should_HandleWhitespaceStrings_When_SettingAndGetting()
    {
        matrix.set(10, 20, "   spaces   ");
        matrix.set(15, 25, "\t\ttabs\t\t");
        matrix.set(20, 30, "\n\nnewlines\n\n");

        assertThat(matrix.get(10, 20)).isEqualTo("   spaces   ");
        assertThat(matrix.get(15, 25)).isEqualTo("\t\ttabs\t\t");
        assertThat(matrix.get(20, 30)).isEqualTo("\n\nnewlines\n\n");
    }

    @Test
    void should_StoreDistinctStrings_When_SameRankDifferentSuits()
    {
        // Multiple kings with different suits
        matrix.set(SPADES_K, HEARTS_K, "K♠K♥");
        matrix.set(SPADES_K, DIAMONDS_K, "K♠K♦");
        matrix.set(SPADES_K, CLUBS_K, "K♠K♣");
        matrix.set(HEARTS_K, DIAMONDS_K, "K♥K♦");

        assertThat(matrix.get(SPADES_K, HEARTS_K)).isEqualTo("K♠K♥");
        assertThat(matrix.get(SPADES_K, DIAMONDS_K)).isEqualTo("K♠K♦");
        assertThat(matrix.get(SPADES_K, CLUBS_K)).isEqualTo("K♠K♣");
        assertThat(matrix.get(HEARTS_K, DIAMONDS_K)).isEqualTo("K♥K♦");
    }

    @Test
    void should_HandleSequentialIndices_When_TestingAdjacentCards()
    {
        // Test adjacent card indices
        matrix.set(0, 1, "pair-0-1");
        matrix.set(1, 2, "pair-1-2");
        matrix.set(2, 3, "pair-2-3");

        assertThat(matrix.get(0, 1)).isEqualTo("pair-0-1");
        assertThat(matrix.get(1, 2)).isEqualTo("pair-1-2");
        assertThat(matrix.get(2, 3)).isEqualTo("pair-2-3");
    }

    @Test
    void should_AllowNullValues_When_ExplicitlySet()
    {
        matrix.set(10, 20, "initially-set");
        matrix.set(10, 20, null);

        assertThat(matrix.get(10, 20)).isNull();
    }
}
