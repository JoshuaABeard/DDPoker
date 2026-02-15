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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for EscapeStringTokenizer - string tokenizer with escape character
 * support
 */
class EscapeStringTokenizerTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateTokenizer_When_FullConstructorUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,c", ",", false);

        assertThat(tokenizer.hasMoreTokens()).isTrue();
    }

    @Test
    void should_CreateTokenizer_When_StringDelimConstructorUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,c", ",");

        assertThat(tokenizer.hasMoreTokens()).isTrue();
    }

    @Test
    void should_CreateTokenizer_When_CharDelimConstructorUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,c", ',');

        assertThat(tokenizer.hasMoreTokens()).isTrue();
    }

    @Test
    void should_CreateTokenizer_When_DefaultDelimConstructorUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a b c");

        assertThat(tokenizer.hasMoreTokens()).isTrue();
    }

    // =================================================================
    // Basic Tokenization Tests
    // =================================================================

    @Test
    void should_TokenizeSimpleString_When_CommaDelimiter() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,c", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
        assertThat(tokenizer.nextToken()).isEqualTo("c");
        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    void should_TokenizeSimpleString_When_WhitespaceDelimiter() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a b c");

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
        assertThat(tokenizer.nextToken()).isEqualTo("c");
        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    void should_TokenizeWithMultipleDelimiters_When_MultipleDelimitersUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b;c", ",;");

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
        assertThat(tokenizer.nextToken()).isEqualTo("c");
    }

    @Test
    void should_SkipAdjacentDelimiters_When_ReturnTokensFalse() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,,b", ",", false);

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    void should_ReturnDelimiters_When_ReturnTokensTrue() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b", ",", true);

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo(",");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
    }

    @Test
    void should_ReturnAdjacentDelimiters_When_ReturnTokensTrue() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,,b", ",", true);

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo(",,");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
    }

    // =================================================================
    // Escape Character Tests
    // =================================================================

    @Test
    void should_EscapeDelimiter_When_BackslashUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a\\,b,c", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a,b");
        assertThat(tokenizer.nextToken()).isEqualTo("c");
    }

    @Test
    void should_EscapeBackslash_When_DoubleBackslash() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a\\\\b,c", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a\\b");
        assertThat(tokenizer.nextToken()).isEqualTo("c");
    }

    @Test
    void should_ConvertDecimalValue_When_BackslashDigits() {
        // \065 = 'A' in decimal
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("\\065BC,d", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("ABC");
        assertThat(tokenizer.nextToken()).isEqualTo("d");
    }

    @Test
    void should_EscapeNewline_When_BackslashN() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a\\Nb,c", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a\nb");
        assertThat(tokenizer.nextToken()).isEqualTo("c");
    }

    // =================================================================
    // Quoted String Tests
    // =================================================================

    @Test
    void should_TreatQuotedAsOneToken_When_DoubleQuotesUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,\"b,c\",d", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b,c");
        assertThat(tokenizer.nextToken()).isEqualTo("d");
    }

    @Test
    void should_HandleEmptyQuotedString_When_AdjacentQuotes() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,\"\",b", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
    }

    @Test
    void should_HandleQuotedWithSpaces_When_QuotesUsed() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a \"b c\" d");

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b c");
        assertThat(tokenizer.nextToken()).isEqualTo("d");
    }

    // =================================================================
    // hasMoreTokens Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_HasMoreTokens() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b", ",");

        assertThat(tokenizer.hasMoreTokens()).isTrue();
        tokenizer.nextToken();
        assertThat(tokenizer.hasMoreTokens()).isTrue();
        tokenizer.nextToken();
        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_EmptyString() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("", ",");

        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_OnlyDelimiters() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer(",,,", ",");

        assertThat(tokenizer.hasMoreTokens()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_OnlyDelimitersAndReturnTokensTrue() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer(",,,", ",", true);

        assertThat(tokenizer.hasMoreTokens()).isTrue();
    }

    // =================================================================
    // hasMoreElements Tests (Enumeration interface)
    // =================================================================

    @Test
    void should_ReturnTrue_When_HasMoreElements() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b", ",");

        assertThat(tokenizer.hasMoreElements()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NoMoreElements() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a", ",");

        tokenizer.nextElement();
        assertThat(tokenizer.hasMoreElements()).isFalse();
    }

    // =================================================================
    // hasMoreDelimiters Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_TrailingDelimiters() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,,,", ",");

        tokenizer.nextToken(); // "a"
        tokenizer.nextToken(); // "b"
        assertThat(tokenizer.hasMoreTokens()).isFalse();
        assertThat(tokenizer.hasMoreDelimiters()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NoTrailingDelimiters() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b", ",");

        tokenizer.nextToken(); // "a"
        tokenizer.nextToken(); // "b"
        assertThat(tokenizer.hasMoreTokens()).isFalse();
        assertThat(tokenizer.hasMoreDelimiters()).isFalse();
    }

    // =================================================================
    // putBackToken Tests
    // =================================================================

    @Test
    void should_ReturnPutBackToken_When_NextTokenCalled() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b", ",");

        String first = tokenizer.nextToken();
        tokenizer.putBackToken(first);

        assertThat(tokenizer.nextToken()).isEqualTo("a");
        assertThat(tokenizer.nextToken()).isEqualTo("b");
    }

    @Test
    void should_ReturnPutBackTokenBeforeNext_When_PutBackCalled() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b", ",");

        tokenizer.putBackToken("custom");

        assertThat(tokenizer.nextToken()).isEqualTo("custom");
        assertThat(tokenizer.nextToken()).isEqualTo("a");
    }

    // =================================================================
    // remainingTokens Tests
    // =================================================================

    @Test
    void should_ReturnAllTokens_When_RemainingTokensCalled() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,c", ",");

        assertThat(tokenizer.remainingTokens()).isEqualTo("abc");
    }

    @Test
    void should_ReturnPartialTokens_When_RemainingTokensCalledMidway() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a,b,c", ",");

        tokenizer.nextToken(); // consume "a"
        assertThat(tokenizer.remainingTokens()).isEqualTo("bc");
    }

    @Test
    void should_ReturnEmptyString_When_RemainingTokensCalledAtEnd() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a", ",");

        tokenizer.nextToken();
        assertThat(tokenizer.remainingTokens()).isEqualTo("");
    }

    // =================================================================
    // nextToken Edge Cases
    // =================================================================

    @Test
    void should_ReturnNull_When_NextTokenCalledAtEnd() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a", ",");

        tokenizer.nextToken();
        assertThat(tokenizer.nextToken()).isNull();
    }

    @Test
    void should_ReturnNull_When_NextTokenCalledOnEmpty() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("", ",");

        assertThat(tokenizer.nextToken()).isNull();
    }

    @Test
    void should_HandleLeadingDelimiters_When_ReturnTokensFalse() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer(",,,a", ",", false);

        assertThat(tokenizer.nextToken()).isEqualTo("a");
    }

    // =================================================================
    // Complex Scenario Tests
    // =================================================================

    @Test
    void should_HandleMixedEscapingAndQuoting_When_ComplexString() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("a\\,b,\"c,d\",e\\Ne,f", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("a,b");
        assertThat(tokenizer.nextToken()).isEqualTo("c,d");
        assertThat(tokenizer.nextToken()).isEqualTo("e\ne");
        assertThat(tokenizer.nextToken()).isEqualTo("f");
    }

    @Test
    void should_TreatEscapeLiterally_When_InsideQuotes() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("\"\\065BC\",d", ",");

        // Inside quotes, escape sequences are treated literally
        assertThat(tokenizer.nextToken()).isEqualTo("\\065BC");
        assertThat(tokenizer.nextToken()).isEqualTo("d");
    }

    @Test
    void should_TokenizeCSVLikeData_When_RealisticInput() {
        EscapeStringTokenizer tokenizer = new EscapeStringTokenizer("John,\"Doe, Jr.\",30,\"New York, NY\"", ",");

        assertThat(tokenizer.nextToken()).isEqualTo("John");
        assertThat(tokenizer.nextToken()).isEqualTo("Doe, Jr.");
        assertThat(tokenizer.nextToken()).isEqualTo("30");
        assertThat(tokenizer.nextToken()).isEqualTo("New York, NY");
    }
}
