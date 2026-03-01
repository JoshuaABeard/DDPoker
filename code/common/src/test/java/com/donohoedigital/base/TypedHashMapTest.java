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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TypedHashMap — a TreeMap subclass with typed get/set accessors.
 */
class TypedHashMapTest {

    private TypedHashMap map;

    @BeforeEach
    void setUp() {
        map = new TypedHashMap();
    }

    // -----------------------------------------------------------------------
    // String
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStoredString_When_StringIsSet() {
        map.setString("key", "hello");
        assertThat(map.getString("key")).isEqualTo("hello");
    }

    @Test
    void should_ReturnNull_When_StringKeyMissing() {
        assertThat(map.getString("missing")).isNull();
    }

    @Test
    void should_ReturnDefault_When_StringKeyMissingAndDefaultProvided() {
        assertThat(map.getString("missing", "fallback")).isEqualTo("fallback");
    }

    @Test
    void should_RemoveString_When_RemoveStringCalled() {
        map.setString("key", "value");
        String removed = map.removeString("key");
        assertThat(removed).isEqualTo("value");
        assertThat(map.getString("key")).isNull();
    }

    // -----------------------------------------------------------------------
    // Integer
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStoredInteger_When_IntegerIsSet() {
        map.setInteger("count", 42);
        assertThat(map.getInteger("count")).isEqualTo(42);
    }

    @Test
    void should_ReturnDefault_When_IntegerKeyMissingAndDefaultProvided() {
        assertThat(map.getInteger("missing", 99)).isEqualTo(99);
    }

    @Test
    void should_ClampToMin_When_StoredIntegerIsBelowMin() {
        map.setInteger("val", 1);
        assertThat(map.getInteger("val", 0, 5, 10)).isEqualTo(5);
    }

    @Test
    void should_ClampToMax_When_StoredIntegerIsAboveMax() {
        map.setInteger("val", 99);
        assertThat(map.getInteger("val", 0, 0, 50)).isEqualTo(50);
    }

    @Test
    void should_RemoveInteger_When_RemoveIntegerCalled() {
        map.setInteger("num", 7);
        Integer removed = map.removeInteger("num");
        assertThat(removed).isEqualTo(7);
        assertThat(map.getInteger("num")).isNull();
    }

    // -----------------------------------------------------------------------
    // Long
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStoredLong_When_LongIsSet() {
        map.setLong("timestamp", 123456789L);
        assertThat(map.getLong("timestamp")).isEqualTo(123456789L);
    }

    @Test
    void should_ReturnDefault_When_LongKeyMissingAndDefaultProvided() {
        assertThat(map.getLong("missing", -1L)).isEqualTo(-1L);
    }

    @Test
    void should_RoundTripDateAsLong_When_SetLongFromDateAndGetLongAsDateUsed() {
        Date now = new Date(1_700_000_000_000L);
        map.setLongFromDate("created", now);
        Date retrieved = map.getLongAsDate("created");
        assertThat(retrieved).isEqualTo(now);
    }

    @Test
    void should_RemoveKey_When_SetLongFromDateCalledWithNull() {
        map.setLong("created", 1L);
        map.setLongFromDate("created", null);
        assertThat(map.getLong("created")).isNull();
    }

    // -----------------------------------------------------------------------
    // Double
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStoredDouble_When_DoubleIsSet() {
        map.setDouble("rate", 3.14);
        assertThat(map.getDouble("rate")).isEqualTo(3.14);
    }

    @Test
    void should_ReturnDefault_When_DoubleKeyMissingAndDefaultProvided() {
        assertThat(map.getDouble("missing", 0.5)).isEqualTo(0.5);
    }

    // -----------------------------------------------------------------------
    // Boolean
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStoredBoolean_When_BooleanIsSet() {
        map.setBoolean("enabled", Boolean.TRUE);
        assertThat(map.getBoolean("enabled")).isTrue();
    }

    @Test
    void should_ReturnDefault_When_BooleanKeyMissingAndDefaultProvided() {
        assertThat(map.getBoolean("missing", true)).isTrue();
    }

    @Test
    void should_RemoveBoolean_When_RemoveBooleanCalled() {
        map.setBoolean("flag", Boolean.FALSE);
        Boolean removed = map.removeBoolean("flag");
        assertThat(removed).isFalse();
        assertThat(map.getBoolean("flag")).isNull();
    }

    // -----------------------------------------------------------------------
    // containsKey, size, remove
    // -----------------------------------------------------------------------

    @Test
    void should_ReportContainsKey_When_KeyHasBeenInserted() {
        map.setString("x", "val");
        assertThat(map.containsKey("x")).isTrue();
        assertThat(map.containsKey("y")).isFalse();
    }

    @Test
    void should_ReportCorrectSize_When_EntriesAreAddedAndRemoved() {
        assertThat(map.size()).isZero();
        map.setString("a", "1");
        map.setString("b", "2");
        assertThat(map.size()).isEqualTo(2);
        map.remove("a");
        assertThat(map.size()).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // List
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnStoredList_When_ListIsSet() {
        List<String> items = Arrays.asList("alpha", "beta");
        map.setList("items", items);
        @SuppressWarnings("unchecked")
        List<String> retrieved = (List<String>) map.getList("items");
        assertThat(retrieved).containsExactly("alpha", "beta");
    }

    // -----------------------------------------------------------------------
    // Copy constructor (TreeMap copy)
    // -----------------------------------------------------------------------

    @Test
    void should_CopyAllEntries_When_PutAllUsed() {
        map.setString("name", "Bob");
        map.setInteger("age", 30);

        TypedHashMap copy = new TypedHashMap();
        copy.putAll(map);

        assertThat(copy.getString("name")).isEqualTo("Bob");
        assertThat(copy.getInteger("age")).isEqualTo(30);
        // Verify independence: modifying copy does not affect original
        copy.setString("name", "Carol");
        assertThat(map.getString("name")).isEqualTo("Bob");
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------

    @Test
    void should_IncludeKeyValuePairs_When_ToStringCalled() {
        map.setString("city", "NYC");
        map.setInteger("pop", 8);
        String s = map.toString();
        // TreeMap orders keys alphabetically: city, pop
        assertThat(s).contains("city=NYC");
        assertThat(s).contains("pop=8");
    }

    @Test
    void should_FormatListEntries_When_ToStringCalledWithListValue() {
        map.setList("tags", Arrays.asList("a", "b"));
        String s = map.toString();
        assertThat(s).contains("tags=[a, b]");
    }
}
