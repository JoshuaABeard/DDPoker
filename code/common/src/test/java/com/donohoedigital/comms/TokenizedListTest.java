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
package com.donohoedigital.comms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TokenizedList - token-based serialization container supporting
 * multiple primitive types and marshal/demarshal round-trips.
 */
class TokenizedListTest {

    // ---- String tokens ----

    @Test
    void should_ReturnStringToken_When_StringAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken("hello");

        assertThat(list.removeStringToken()).isEqualTo("hello");
    }

    @Test
    void should_ReturnEmptyString_When_EmptyStringAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken("");

        assertThat(list.removeStringToken()).isEmpty();
    }

    @Test
    void should_ReturnNull_When_NullStringAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken((String) null);

        assertThat(list.removeStringToken()).isNull();
    }

    // ---- Int tokens ----

    @Test
    void should_ReturnIntToken_When_IntAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(42);

        assertThat(list.removeIntToken()).isEqualTo(42);
    }

    @Test
    void should_ReturnNegativeInt_When_NegativeIntAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(-100);

        assertThat(list.removeIntToken()).isEqualTo(-100);
    }

    @Test
    void should_ReturnZero_When_ZeroAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(0);

        assertThat(list.removeIntToken()).isZero();
    }

    // ---- Integer tokens ----

    @Test
    void should_ReturnIntegerToken_When_IntegerAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(Integer.valueOf(99));

        assertThat(list.removeIntegerToken()).isEqualTo(99);
    }

    @Test
    void should_ReturnNull_When_NullIntegerAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken((Integer) null);

        assertThat(list.removeIntegerToken()).isNull();
    }

    // ---- Long tokens ----

    @Test
    void should_ReturnLongToken_When_LongAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(123456789L);

        assertThat(list.removeLongToken()).isEqualTo(123456789L);
    }

    @Test
    void should_ReturnNegativeLong_When_NegativeLongAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(-999999999999L);

        assertThat(list.removeLongToken()).isEqualTo(-999999999999L);
    }

    // ---- Double tokens ----

    @Test
    void should_ReturnDoubleToken_When_DoubleAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(3.14159);

        assertThat(list.removeDoubleToken()).isEqualTo(3.14159);
    }

    @Test
    void should_ReturnNegativeDouble_When_NegativeDoubleAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(-2.5);

        assertThat(list.removeDoubleToken()).isEqualTo(-2.5);
    }

    // ---- Boolean tokens ----

    @Test
    void should_ReturnTrue_When_TrueAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(true);

        assertThat(list.removeBooleanToken()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_FalseAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken(false);

        assertThat(list.removeBooleanToken()).isFalse();
    }

    // ---- Null tokens ----

    @Test
    void should_ReturnNull_When_NullTokenAdded() {
        TokenizedList list = new TokenizedList();
        list.addTokenNull();

        assertThat(list.removeStringToken()).isNull();
    }

    // ---- Multiple tokens and ordering ----

    @Test
    void should_PreserveOrder_When_MultipleTokensAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken("first");
        list.addToken(42);
        list.addToken(true);
        list.addToken(3.14);

        assertThat(list.removeStringToken()).isEqualTo("first");
        assertThat(list.removeIntToken()).isEqualTo(42);
        assertThat(list.removeBooleanToken()).isTrue();
        assertThat(list.removeDoubleToken()).isEqualTo(3.14);
    }

    @Test
    void should_ReturnTrue_When_TokensExist() {
        TokenizedList list = new TokenizedList();
        list.addToken("a");

        assertThat(list.hasMoreTokens()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NoTokensExist() {
        TokenizedList list = new TokenizedList();

        assertThat(list.hasMoreTokens()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_AllTokensRemoved() {
        TokenizedList list = new TokenizedList();
        list.addToken("a");
        list.removeStringToken();

        assertThat(list.hasMoreTokens()).isFalse();
    }

    @Test
    void should_PeekWithoutRemoving_When_PeekTokenCalled() {
        TokenizedList list = new TokenizedList();
        list.addToken("peek");

        list.peekToken();
        assertThat(list.hasMoreTokens()).isTrue();
        assertThat(list.removeStringToken()).isEqualTo("peek");
    }

    // ---- Marshal/demarshal round-trip ----

    @Test
    void should_RoundTripString_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken("hello world");

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeStringToken()).isEqualTo("hello world");
    }

    @Test
    void should_RoundTripInt_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken(42);

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeIntToken()).isEqualTo(42);
    }

    @Test
    void should_RoundTripMultipleTypes_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken("text");
        original.addToken(99);
        original.addToken(true);
        original.addToken(2.718);
        original.addToken(Long.MAX_VALUE);
        original.addTokenNull();

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeStringToken()).isEqualTo("text");
        assertThat(restored.removeIntToken()).isEqualTo(99);
        assertThat(restored.removeBooleanToken()).isTrue();
        assertThat(restored.removeDoubleToken()).isEqualTo(2.718);
        assertThat(restored.removeLongToken()).isEqualTo(Long.MAX_VALUE);
        assertThat(restored.removeStringToken()).isNull();
    }

    @Test
    void should_RoundTripEmptyList_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.hasMoreTokens()).isFalse();
    }

    // ---- escape() ----

    @Test
    void should_ReturnNull_When_EscapingNull() {
        assertThat(TokenizedList.escape(null)).isNull();
    }

    @Test
    void should_ReturnSameString_When_NoSpecialChars() {
        String input = "simple text";
        assertThat(TokenizedList.escape(input)).isSameAs(input);
    }

    @Test
    void should_EscapeDelimiterChar_When_Present() {
        String input = "a:b";
        String escaped = TokenizedList.escape(input);

        assertThat(escaped).contains("\\");
        assertThat(escaped).isNotEqualTo(input);
    }

    @Test
    void should_EscapeNullChar_When_Present() {
        String input = "a~b";
        String escaped = TokenizedList.escape(input);

        assertThat(escaped).contains("\\");
    }

    @Test
    void should_EscapeBackslash_When_Present() {
        String input = "a\\b";
        String escaped = TokenizedList.escape(input);

        // should have two backslashes: the escape + the original
        assertThat(escaped).isEqualTo("a\\\\b");
    }

    @Test
    void should_EscapeEqualsSign_When_Present() {
        // '=' is NameValueToken.TOKEN_NVT_SEP
        String input = "key=value";
        String escaped = TokenizedList.escape(input);

        assertThat(escaped).contains("\\");
    }

    @Test
    void should_RoundTripStringWithSpecialChars_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken("has:colons~tildes\\backslashes\"quotes=equals");

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeStringToken()).isEqualTo("has:colons~tildes\\backslashes\"quotes=equals");
    }

    @Test
    void should_EscapeNewlines_When_Present() {
        String input = "line1\nline2";
        String escaped = TokenizedList.escape(input);

        assertThat(escaped).doesNotContain("\n");
    }

    @Test
    void should_RoundTripStringWithNewlines_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken("line1\nline2\nline3");

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        assertThat(restored.removeStringToken()).isEqualTo("line1\nline2\nline3");
    }

    // ---- toString() ----

    @Test
    void should_ReturnClassName_When_ToStringCalled() {
        TokenizedList list = new TokenizedList();

        assertThat(list.toString()).isEqualTo(TokenizedList.class.getName());
    }

    // ---- NameValueToken integration ----

    @Test
    void should_RoundTripNameValueToken_When_IntegerValueAdded() {
        TokenizedList list = new TokenizedList();
        list.addNameValueToken("count", Integer.valueOf(5));

        NameValueToken nvt = list.removeNameValueToken();

        assertThat(nvt.getName()).isEqualTo("count");
        assertThat(((DataMarshaller.DMWrapper) nvt.getValue()).value()).isEqualTo(5);
    }

    @Test
    void should_RoundTripNameValueToken_When_StringValueAdded() {
        TokenizedList list = new TokenizedList();
        list.addNameValueToken("name", "Alice");

        NameValueToken nvt = list.removeNameValueToken();

        assertThat(nvt.getName()).isEqualTo("name");
        assertThat(((DataMarshaller.DMWrapper) nvt.getValue()).value()).isEqualTo("Alice");
    }

    @Test
    void should_RoundTripNameValueToken_When_BooleanValueAdded() {
        TokenizedList list = new TokenizedList();
        list.addNameValueToken("active", Boolean.TRUE);

        NameValueToken nvt = list.removeNameValueToken();

        assertThat(nvt.getName()).isEqualTo("active");
        assertThat(((DataMarshaller.DMWrapper) nvt.getValue()).value()).isEqualTo(true);
    }

    @Test
    void should_RoundTripNameValueToken_When_LongValueAdded() {
        TokenizedList list = new TokenizedList();
        list.addNameValueToken("timestamp", Long.valueOf(123456789L));

        NameValueToken nvt = list.removeNameValueToken();

        assertThat(nvt.getName()).isEqualTo("timestamp");
        assertThat(((DataMarshaller.DMWrapper) nvt.getValue()).value()).isEqualTo(123456789L);
    }

    @Test
    void should_RoundTripNameValueToken_When_DoubleValueAdded() {
        TokenizedList list = new TokenizedList();
        list.addNameValueToken("ratio", Double.valueOf(0.75));

        NameValueToken nvt = list.removeNameValueToken();

        assertThat(nvt.getName()).isEqualTo("ratio");
        assertThat(((DataMarshaller.DMWrapper) nvt.getValue()).value()).isEqualTo(0.75);
    }

    @Test
    void should_MarshalAndDemarshalNameValueTokens_When_RoundTripped() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addNameValueToken("key", "value");
        original.addNameValueToken("num", Integer.valueOf(42));

        String marshalled = original.marshal(state);
        TokenizedList restored = new TokenizedList();
        restored.demarshal(state, marshalled);

        NameValueToken nvt1 = restored.removeNameValueToken();
        assertThat(nvt1.getName()).isEqualTo("key");
        assertThat(((DataMarshaller.DMWrapper) nvt1.getValue()).value()).isEqualTo("value");

        NameValueToken nvt2 = restored.removeNameValueToken();
        assertThat(nvt2.getName()).isEqualTo("num");
        assertThat(((DataMarshaller.DMWrapper) nvt2.getValue()).value()).isEqualTo(42);
    }

    // ---- Constructor with string data ----

    @Test
    void should_ParseTokens_When_ConstructedFromMarshalledString() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken("abc");
        original.addToken(7);
        String marshalled = original.marshal(state);

        TokenizedList parsed = new TokenizedList(state, marshalled, TokenizedList.TOKEN_READ_ALL);

        assertThat(parsed.removeStringToken()).isEqualTo("abc");
        assertThat(parsed.removeIntToken()).isEqualTo(7);
    }

    @Test
    void should_ReadLimitedTokens_When_ReadNumTokensSpecified() {
        MsgState state = new MsgState();
        TokenizedList original = new TokenizedList();
        original.addToken("first");
        original.addToken("second");
        original.addToken("third");
        String marshalled = original.marshal(state);

        TokenizedList parsed = new TokenizedList(state, marshalled, 1);

        assertThat(parsed.removeStringToken()).isEqualTo("first");
        assertThat(parsed.hasMoreTokens()).isFalse();

        // Finish parsing remaining tokens
        parsed.finishParsing(state);
        assertThat(parsed.removeStringToken()).isEqualTo("second");
        assertThat(parsed.removeStringToken()).isEqualTo("third");
    }

    // ---- DataMarshal token ----

    @Test
    void should_ReturnNull_When_NullDataMarshalAdded() {
        TokenizedList list = new TokenizedList();
        list.addToken((DataMarshal) null);

        assertThat(list.removeToken()).isNull();
    }
}
