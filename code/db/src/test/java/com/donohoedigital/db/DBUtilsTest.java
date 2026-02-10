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
package com.donohoedigital.db;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DBUtils - SQL utility methods for wildcard handling and escaping.
 */
class DBUtilsTest {

    // =================================================================
    // sqlEscapeWildcards Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_EscapingNullString() {
        String result = DBUtils.sqlEscapeWildcards(null);

        assertThat(result).isNull();
    }

    @Test
    void should_ReturnOriginal_When_NoWildcardsPresent() {
        String result = DBUtils.sqlEscapeWildcards("simple text");

        assertThat(result).isEqualTo("simple text");
    }

    @Test
    void should_EscapePercent_When_PercentPresent() {
        String result = DBUtils.sqlEscapeWildcards("50%");

        assertThat(result).isEqualTo("50\\%");
    }

    @Test
    void should_EscapeUnderscore_When_UnderscorePresent() {
        String result = DBUtils.sqlEscapeWildcards("file_name");

        assertThat(result).isEqualTo("file\\_name");
    }

    @Test
    void should_EscapeMultipleWildcards_When_MultiplePresent() {
        String result = DBUtils.sqlEscapeWildcards("test_%_data");

        assertThat(result).isEqualTo("test\\_\\%\\_data");
    }

    @Test
    void should_EscapeConsecutiveWildcards_When_ConsecutivePresent() {
        String result = DBUtils.sqlEscapeWildcards("%%__");

        assertThat(result).isEqualTo("\\%\\%\\_\\_");
    }

    @Test
    void should_PreserveNonWildcards_When_Escaping() {
        String result = DBUtils.sqlEscapeWildcards("a1!@#$^&*()");

        assertThat(result).isEqualTo("a1!@#$^&*()");
    }

    @Test
    void should_HandleEmptyString_When_Escaping() {
        String result = DBUtils.sqlEscapeWildcards("");

        assertThat(result).isEqualTo("");
    }

    // =================================================================
    // sqlWildcard Tests
    // =================================================================

    @Test
    void should_ReturnWildcard_When_TermIsNull() {
        String result = DBUtils.sqlWildcard(null);

        assertThat(result).isEqualTo("%");
    }

    @Test
    void should_ReturnWildcard_When_TermIsEmpty() {
        String result = DBUtils.sqlWildcard("");

        assertThat(result).isEqualTo("%");
    }

    @Test
    void should_WrapWithWildcards_When_TermIsSimple() {
        String result = DBUtils.sqlWildcard("test");

        assertThat(result).isEqualTo("%test%");
    }

    @Test
    void should_EscapeAndWrap_When_TermContainsPercent() {
        String result = DBUtils.sqlWildcard("50%");

        assertThat(result).isEqualTo("%50\\%%");
    }

    @Test
    void should_EscapeAndWrap_When_TermContainsUnderscore() {
        String result = DBUtils.sqlWildcard("file_name");

        assertThat(result).isEqualTo("%file\\_name%");
    }

    @Test
    void should_ReturnExactMatch_When_TermStartsWithExactPrefix() {
        String term = DBUtils.SQL_EXACT_MATCH + "exactTerm";
        String result = DBUtils.sqlWildcard(term);

        assertThat(result).isEqualTo("exactTerm");
    }

    @Test
    void should_StripOnlyPrefix_When_ExactMatchHasWildcards() {
        String term = DBUtils.SQL_EXACT_MATCH + "term%with_wildcards";
        String result = DBUtils.sqlWildcard(term);

        assertThat(result).isEqualTo("term%with_wildcards");
    }

    @Test
    void should_ReturnEmpty_When_ExactMatchPrefixOnly() {
        String term = DBUtils.SQL_EXACT_MATCH;
        String result = DBUtils.sqlWildcard(term);

        assertThat(result).isEqualTo("");
    }

    @Test
    void should_HandleMultipleWildcards_When_Wrapping() {
        String result = DBUtils.sqlWildcard("a%b_c");

        assertThat(result).isEqualTo("%a\\%b\\_c%");
    }

    // =================================================================
    // sqlExactMatch Tests
    // =================================================================

    @Test
    void should_ReturnPrefixOnly_When_TermIsNull() {
        String result = DBUtils.sqlExactMatch(null);

        assertThat(result).isEqualTo("=");
    }

    @Test
    void should_PrefixTerm_When_TermProvided() {
        String result = DBUtils.sqlExactMatch("exact");

        assertThat(result).isEqualTo("=exact");
    }

    @Test
    void should_PrefixEmptyString_When_TermIsEmpty() {
        String result = DBUtils.sqlExactMatch("");

        assertThat(result).isEqualTo("=");
    }

    @Test
    void should_PreserveWildcards_When_PrefixingExact() {
        String result = DBUtils.sqlExactMatch("term%with_wildcards");

        assertThat(result).isEqualTo("=term%with_wildcards");
    }

    @Test
    void should_HandleSpecialCharacters_When_PrefixingExact() {
        String result = DBUtils.sqlExactMatch("sp3c!@l#ch@rs");

        assertThat(result).isEqualTo("=sp3c!@l#ch@rs");
    }

    // =================================================================
    // Integration Tests - Combining Methods
    // =================================================================

    @Test
    void should_HandleRoundTrip_When_UsingExactMatchAndWildcard() {
        String original = "testTerm";
        String exact = DBUtils.sqlExactMatch(original);
        String result = DBUtils.sqlWildcard(exact);

        assertThat(result).isEqualTo(original);
    }

    @Test
    void should_ProduceValidLikePattern_When_UserInputContainsWildcards() {
        // Simulate user searching for literal "50% off"
        String userInput = "50% off";
        String safePattern = DBUtils.sqlWildcard(userInput);

        // Should escape the % so it's treated as literal character
        assertThat(safePattern).isEqualTo("%50\\% off%");
    }

    @Test
    void should_AllowExactMatchOverride_When_UserWantsNoWildcard() {
        String userInput = DBUtils.sqlExactMatch("exactValue");
        String result = DBUtils.sqlWildcard(userInput);

        // Should return exact value without wildcard wrapping
        assertThat(result).isEqualTo("exactValue");
    }
}
