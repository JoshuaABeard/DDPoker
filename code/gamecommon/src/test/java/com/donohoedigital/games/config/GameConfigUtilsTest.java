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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GameConfigUtils - File number parsing and formatting utilities.
 */
class GameConfigUtilsTest {

    // =================================================================
    // getFileNumber Tests
    // =================================================================

    @Test
    void should_ParseFileNumber_When_ValidFilenameProvided() {
        File file = new File("savegame.000001.sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_ParseLargeFileNumber_When_LargeNumberInFilename() {
        File file = new File("game.999999.dat");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(999999);
    }

    @Test
    void should_ParseZeroFileNumber_When_ZeroInFilename() {
        File file = new File("data.000000.txt");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void should_ParseMultiDigitNumber_When_NoLeadingZeros() {
        File file = new File("save.12345.sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(12345);
    }

    @Test
    void should_ReturnMinusOne_When_FilenameHasTooFewTokens() {
        File file = new File("savegame.sav"); // Only 2 tokens

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnMinusOne_When_FilenameHasTooManyTokens() {
        File file = new File("save.game.001.extra.sav"); // 5 tokens

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnMinusOne_When_NumberTokenIsNotNumeric() {
        File file = new File("savegame.abc.sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnMinusOne_When_NumberTokenIsEmpty() {
        File file = new File("savegame..sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnMinusOne_When_NumberTokenHasSpaces() {
        File file = new File("save.1 2 3.sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnMinusOne_When_FilenameHasNoDelimiters() {
        File file = new File("savegame001sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ParseCorrectly_When_FilenameHasDifferentExtension() {
        File file = new File("game.042.xml");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(42);
    }

    @Test
    void should_ParseCorrectly_When_LeadingPartHasMultipleWords() {
        File file = new File("my-save-game.123.dat");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(123);
    }

    @Test
    void should_ParseCorrectly_When_ExtensionHasMultipleCharacters() {
        File file = new File("save.500.backup");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(500);
    }

    @Test
    void should_HandlePathSeparators_When_FileHasPath() {
        File file = new File("/path/to/save.777.sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(777);
    }

    // =================================================================
    // formatFileNumber Tests
    // =================================================================

    @Test
    void should_FormatWithLeadingZeros_When_SingleDigit() {
        String result = GameConfigUtils.formatFileNumber(1);

        assertThat(result).isEqualTo("000001");
    }

    @Test
    void should_FormatWithLeadingZeros_When_TwoDigits() {
        String result = GameConfigUtils.formatFileNumber(42);

        assertThat(result).isEqualTo("000042");
    }

    @Test
    void should_FormatWithLeadingZeros_When_ThreeDigits() {
        String result = GameConfigUtils.formatFileNumber(123);

        assertThat(result).isEqualTo("000123");
    }

    @Test
    void should_FormatWithLeadingZeros_When_FourDigits() {
        String result = GameConfigUtils.formatFileNumber(5678);

        assertThat(result).isEqualTo("005678");
    }

    @Test
    void should_FormatWithLeadingZeros_When_FiveDigits() {
        String result = GameConfigUtils.formatFileNumber(12345);

        assertThat(result).isEqualTo("012345");
    }

    @Test
    void should_FormatWithoutLeadingZeros_When_SixDigits() {
        String result = GameConfigUtils.formatFileNumber(999999);

        assertThat(result).isEqualTo("999999");
    }

    @Test
    void should_FormatZero_When_ZeroProvided() {
        String result = GameConfigUtils.formatFileNumber(0);

        assertThat(result).isEqualTo("000000");
    }

    @Test
    void should_HaveLength6_When_AnyNumberFormatted() {
        String result1 = GameConfigUtils.formatFileNumber(1);
        String result2 = GameConfigUtils.formatFileNumber(12345);
        String result3 = GameConfigUtils.formatFileNumber(999999);

        assertThat(result1).hasSize(6);
        assertThat(result2).hasSize(6);
        assertThat(result3).hasSize(6);
    }

    @Test
    void should_PreserveAllDigits_When_MoreThanSixDigits() {
        // If number exceeds 6 digits, format should preserve all digits
        String result = GameConfigUtils.formatFileNumber(1234567);

        assertThat(result).hasSize(7);
        assertThat(result).isEqualTo("1234567");
    }

    @Test
    void should_FormatWithLeadingZeros_When_NegativeProvided() {
        // Format class preserves sign and adds leading zeros
        String result = GameConfigUtils.formatFileNumber(-1);

        assertThat(result).isEqualTo("-00001");
    }

    // =================================================================
    // Round-trip Tests
    // =================================================================

    @Test
    void should_RoundTripCorrectly_When_ParsingAndFormatting() {
        int original = 42;
        String formatted = GameConfigUtils.formatFileNumber(original);
        File file = new File("save." + formatted + ".sav");
        int parsed = GameConfigUtils.getFileNumber(file);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void should_RoundTripCorrectly_When_LargeNumber() {
        int original = 999999;
        String formatted = GameConfigUtils.formatFileNumber(original);
        File file = new File("game." + formatted + ".dat");
        int parsed = GameConfigUtils.getFileNumber(file);

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void should_RoundTripCorrectly_When_Zero() {
        int original = 0;
        String formatted = GameConfigUtils.formatFileNumber(original);
        File file = new File("data." + formatted + ".txt");
        int parsed = GameConfigUtils.getFileNumber(file);

        assertThat(parsed).isEqualTo(original);
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleEmptyFilename_When_EmptyStringProvided() {
        File file = new File("");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_HandleSingleDot_When_FilenameIsJustDot() {
        File file = new File(".");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_HandleDoubleDot_When_FilenameIsDoubleDot() {
        File file = new File("..");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ParseCorrectly_When_HiddenFile() {
        // StringTokenizer treats ".hidden.123.sav" as 3 tokens (hidden, 123, sav)
        File file = new File(".hidden.123.sav");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(123);
    }

    @Test
    void should_HandleTrailingDot_When_FilenameEndsWithDot() {
        File file = new File("save.123.");

        int result = GameConfigUtils.getFileNumber(file);

        assertThat(result).isEqualTo(-1); // 4 tokens (save, 123, empty, empty)
    }
}
