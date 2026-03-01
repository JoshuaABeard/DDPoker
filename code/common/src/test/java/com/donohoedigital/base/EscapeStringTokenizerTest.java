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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for EscapeStringTokenizer — a StringTokenizer that respects backslash
 * escapes and quoted substrings.
 */
class EscapeStringTokenizerTest {

    /** Drain all tokens from the tokenizer into a list. */
    private List<String> allTokens(EscapeStringTokenizer tok) {
        List<String> result = new ArrayList<>();
        while (tok.hasMoreTokens()) {
            result.add(tok.nextToken());
        }
        return result;
    }

    @Test
    void should_ReturnThreeTokens_When_SimpleCommaDelimitedString() {
        EscapeStringTokenizer tok = new EscapeStringTokenizer("one,two,three", ",");

        assertThat(allTokens(tok)).containsExactly("one", "two", "three");
    }

    @Test
    void should_TreatEscapedDelimiterAsLiteralCharacter_When_BackslashPrecedesDelimiter() {
        // "one\,two" → the comma is escaped, so it is part of the token
        EscapeStringTokenizer tok = new EscapeStringTokenizer("one\\,two,three", ",");

        assertThat(allTokens(tok)).containsExactly("one,two", "three");
    }

    @Test
    void should_ReturnSingleToken_When_NoDelimiterInInput() {
        EscapeStringTokenizer tok = new EscapeStringTokenizer("hello", ",");

        assertThat(allTokens(tok)).containsExactly("hello");
    }

    @Test
    void should_ReturnNoTokens_When_InputIsAllDelimiters() {
        // hasMoreTokens() skips leading delimiters; ",,," produces no tokens
        EscapeStringTokenizer tok = new EscapeStringTokenizer(",,,", ",");

        assertThat(allTokens(tok)).isEmpty();
    }

    @Test
    void should_ReturnSingleEmptyString_When_InputIsEmptyString() {
        // An empty string has no characters → no tokens at all
        EscapeStringTokenizer tok = new EscapeStringTokenizer("", ",");

        assertThat(tok.hasMoreTokens()).isFalse();
        assertThat(allTokens(tok)).isEmpty();
    }

    @Test
    void should_TreatDoubleQuotedSubstringAsOneToken_When_ValueContainsQuotes() {
        // Quoted content that includes the delimiter is treated as a single token
        EscapeStringTokenizer tok = new EscapeStringTokenizer("\"hello,world\",end", ",");

        assertThat(allTokens(tok)).containsExactly("hello,world", "end");
    }

    @Test
    void should_DecodeEscapedNewline_When_BackslashNUsed() {
        // \N is the escape sequence for a literal newline character
        EscapeStringTokenizer tok = new EscapeStringTokenizer("line1\\Nline2", ",");

        String token = tok.nextToken();
        assertThat(token).isEqualTo("line1\nline2");
    }

    @Test
    void should_DecodeDecimalEscape_When_ThreeDigitOctalStyleValueUsed() {
        // \065 is the decimal value 65 = 'A'
        EscapeStringTokenizer tok = new EscapeStringTokenizer("\\065BC", ",");

        String token = tok.nextToken();
        assertThat(token).isEqualTo("ABC");
    }

    @Test
    void should_ReturnPutBackToken_When_PutBackTokenCalled() {
        EscapeStringTokenizer tok = new EscapeStringTokenizer("actual", ",");
        tok.putBackToken("pushed");

        assertThat(tok.nextToken()).isEqualTo("pushed");
        assertThat(tok.nextToken()).isEqualTo("actual");
    }

    @Test
    void should_ReturnRemainingConcatenated_When_RemainingTokensCalled() {
        EscapeStringTokenizer tok = new EscapeStringTokenizer("ab,cd,ef", ",");

        assertThat(tok.remainingTokens()).isEqualTo("abcdef");
    }
}
