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

import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for NameValueToken - name/value pair serialization used by
 * TokenizedList.
 */
class NameValueTokenTest {

    // ---- Constructor ----

    @Test
    void should_CreateEmptyToken_When_DefaultConstructed() {
        NameValueToken nvt = new NameValueToken();

        assertThat(nvt.getName()).isNull();
        assertThat(nvt.getValue()).isNull();
    }

    // ---- Marshal/demarshal round-trip for different value types ----

    @Test
    void should_RoundTripStringValue_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        String marshalled = DataMarshaller.marshal(state, new NameValueToken("key", "value"));

        DataMarshal dm = DataMarshaller.demarshal(state, marshalled);
        assertThat(dm).isInstanceOf(NameValueToken.class);
        NameValueToken restored = (NameValueToken) dm;

        assertThat(restored.getName()).isEqualTo("key");
        assertThat(((DataMarshaller.DMWrapper) restored.getValue()).value()).isEqualTo("value");
    }

    @Test
    void should_RoundTripIntegerValue_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        String marshalled = DataMarshaller.marshal(state, new NameValueToken("count", 42));

        NameValueToken restored = (NameValueToken) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getName()).isEqualTo("count");
        assertThat(((DataMarshaller.DMWrapper) restored.getValue()).value()).isEqualTo(42);
    }

    @Test
    void should_RoundTripLongValue_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        String marshalled = DataMarshaller.marshal(state, new NameValueToken("time", 123456789L));

        NameValueToken restored = (NameValueToken) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getName()).isEqualTo("time");
        assertThat(((DataMarshaller.DMWrapper) restored.getValue()).value()).isEqualTo(123456789L);
    }

    @Test
    void should_RoundTripBooleanValue_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        String marshalled = DataMarshaller.marshal(state, new NameValueToken("active", true));

        NameValueToken restored = (NameValueToken) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getName()).isEqualTo("active");
        assertThat(((DataMarshaller.DMWrapper) restored.getValue()).value()).isEqualTo(true);
    }

    @Test
    void should_RoundTripDoubleValue_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        String marshalled = DataMarshaller.marshal(state, new NameValueToken("ratio", 0.75));

        NameValueToken restored = (NameValueToken) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getName()).isEqualTo("ratio");
        assertThat(((DataMarshaller.DMWrapper) restored.getValue()).value()).isEqualTo(0.75);
    }

    // ---- loadNameValueTokensIntoList / loadNameValueTokensIntoMap ----

    @Test
    void should_RoundTripMapThroughTokenizedList_When_LoadMethodsUsed() {
        LinkedHashMap<String, Object> original = new LinkedHashMap<>();
        original.put("name", "Alice");
        original.put("age", 30);
        original.put("active", true);

        TokenizedList list = new TokenizedList();
        NameValueToken.loadNameValueTokensIntoList(list, original);

        HashMap<String, Object> restored = new HashMap<>();
        NameValueToken.loadNameValueTokensIntoMap(list, restored);

        assertThat(restored).containsEntry("name", "Alice");
        assertThat(restored).containsEntry("age", 30);
        assertThat(restored).containsEntry("active", true);
    }

    @Test
    void should_HandleLongValues_When_LoadedIntoList() {
        LinkedHashMap<String, Object> original = new LinkedHashMap<>();
        original.put("timestamp", 123456789L);

        TokenizedList list = new TokenizedList();
        NameValueToken.loadNameValueTokensIntoList(list, original);

        HashMap<String, Object> restored = new HashMap<>();
        NameValueToken.loadNameValueTokensIntoMap(list, restored);

        assertThat(restored).containsEntry("timestamp", 123456789L);
    }

    @Test
    void should_HandleDoubleValues_When_LoadedIntoList() {
        LinkedHashMap<String, Object> original = new LinkedHashMap<>();
        original.put("ratio", 3.14);

        TokenizedList list = new TokenizedList();
        NameValueToken.loadNameValueTokensIntoList(list, original);

        HashMap<String, Object> restored = new HashMap<>();
        NameValueToken.loadNameValueTokensIntoMap(list, restored);

        assertThat(restored).containsEntry("ratio", 3.14);
    }

    @Test
    void should_HandleSpecialCharsInName_When_MarshalledAndDemarshalled() {
        MsgState state = new MsgState();
        // NameValueToken uses '=' as separator, so names with special chars need
        // escaping
        String marshalled = DataMarshaller.marshal(state, new NameValueToken("key:special", "val"));

        NameValueToken restored = (NameValueToken) DataMarshaller.demarshal(state, marshalled);

        assertThat(restored.getName()).isEqualTo("key:special");
        assertThat(((DataMarshaller.DMWrapper) restored.getValue()).value()).isEqualTo("val");
    }
}
