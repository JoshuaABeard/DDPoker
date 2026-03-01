/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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

import com.donohoedigital.config.BaseDataFile;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the SaveFile interface constant and the GameConfigUtils helpers
 * that operate on SaveFile filenames.
 *
 * <p>
 * SaveFile itself is a pure interface (one constant, one method). All
 * behavioural logic is in GameConfigUtils, which is the natural home to test. A
 * minimal inner implementation is used wherever a SaveFile instance is
 * required.
 * </p>
 */
class SaveFileTest {

    // -------------------------------------------------------------------------
    // Minimal SaveFile implementation for use in tests
    // -------------------------------------------------------------------------

    /** Wraps an arbitrary File so it can be used as a SaveFile. */
    private static class StubSaveFile implements SaveFile {
        private final File file;

        StubSaveFile(File file) {
            this.file = file;
        }

        @Override
        public File getFile() {
            return file;
        }
    }

    // =========================================================================
    // SaveFile.DELIM constant
    // =========================================================================

    @Test
    void should_HaveDotAsDelimiter_When_DelimConstantAccessed() {
        assertThat(SaveFile.DELIM).isEqualTo(".");
    }

    @Test
    void should_MatchBaseDataFileDelim_When_DelimConstantCompared() {
        assertThat(SaveFile.DELIM).isEqualTo(BaseDataFile.DELIM);
    }

    // =========================================================================
    // GameConfigUtils.getFileNumber()
    // =========================================================================

    @Test
    void should_ReturnFileNumber_When_FileNameHasThreeTokens() {
        // filename pattern: <leading>.<num>.<ext>
        File file = new File("game.000042.dat");
        int result = GameConfigUtils.getFileNumber(file);
        assertThat(result).isEqualTo(42);
    }

    @Test
    void should_ReturnFileNumber_When_FileNameHasLeadingZeros() {
        File file = new File("poker.000001.sav");
        int result = GameConfigUtils.getFileNumber(file);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_ReturnNegativeOne_When_FileNameHasOnlyTwoTokens() {
        File file = new File("game.dat");
        int result = GameConfigUtils.getFileNumber(file);
        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnNegativeOne_When_FileNameHasMoreThanThreeTokens() {
        // four dot-separated tokens -> countTokens() != 3
        File file = new File("game.extra.000001.dat");
        int result = GameConfigUtils.getFileNumber(file);
        assertThat(result).isEqualTo(-1);
    }

    @Test
    void should_ReturnNegativeOne_When_MiddleTokenIsNotANumber() {
        File file = new File("game.notanumber.dat");
        int result = GameConfigUtils.getFileNumber(file);
        assertThat(result).isEqualTo(-1);
    }

    // =========================================================================
    // GameConfigUtils.getNextSaveNumber()
    // =========================================================================

    @Test
    void should_ReturnOne_When_ExistingArrayIsNull() {
        int result = GameConfigUtils.getNextSaveNumber(null);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_ReturnOne_When_ExistingArrayIsEmpty() {
        int result = GameConfigUtils.getNextSaveNumber(new SaveFile[0]);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void should_ReturnNextNumber_When_ExistingArrayHasOneEntry() {
        SaveFile[] existing = {new StubSaveFile(new File("game.000005.dat"))};
        int result = GameConfigUtils.getNextSaveNumber(existing);
        assertThat(result).isEqualTo(6);
    }

    @Test
    void should_ReturnNextNumber_When_ExistingArrayHasMultipleEntries() {
        // sorted ascending; getNextSaveNumber iterates from the end
        SaveFile[] existing = {new StubSaveFile(new File("game.000001.dat")),
                new StubSaveFile(new File("game.000002.dat")), new StubSaveFile(new File("game.000010.dat"))};
        int result = GameConfigUtils.getNextSaveNumber(existing);
        assertThat(result).isEqualTo(11);
    }

    @Test
    void should_ReturnOne_When_AllEntriesHaveNullFiles() {
        // SaveFile.getFile() returns null — getNextSaveNumber skips null files
        SaveFile nullFile = () -> null;
        int result = GameConfigUtils.getNextSaveNumber(new SaveFile[]{nullFile});
        assertThat(result).isEqualTo(1);
    }

    // =========================================================================
    // GameConfigUtils.formatFileNumber()
    // =========================================================================

    @Test
    void should_FormatWithSixDigitsAndLeadingZeros_When_NumberIsSmall() {
        String result = GameConfigUtils.formatFileNumber(1);
        assertThat(result).isEqualTo("000001");
    }

    @Test
    void should_FormatWithSixDigits_When_NumberFillsAllDigits() {
        String result = GameConfigUtils.formatFileNumber(999999);
        assertThat(result).isEqualTo("999999");
    }
}
