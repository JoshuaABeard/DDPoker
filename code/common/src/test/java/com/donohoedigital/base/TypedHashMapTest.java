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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TypedHashMap - typed wrapper around TreeMap
 */
class TypedHashMapTest {

    // =================================================================
    // String Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_StringNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getString("key")).isNull();
    }

    @Test
    void should_ReturnDefault_When_StringNotSetWithDefault() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getString("key", "default")).isEqualTo("default");
    }

    @Test
    void should_ReturnString_When_StringSet() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key", "value");

        assertThat(map.getString("key")).isEqualTo("value");
    }

    @Test
    void should_ReturnString_When_StringSetAndDefaultProvided() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key", "value");

        assertThat(map.getString("key", "default")).isEqualTo("value");
    }

    @Test
    void should_RemoveString_When_RemoveStringCalled() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key", "value");

        String removed = map.removeString("key");

        assertThat(removed).isEqualTo("value");
        assertThat(map.getString("key")).isNull();
    }

    // =================================================================
    // Integer Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_IntegerNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getInteger("key")).isNull();
    }

    @Test
    void should_ReturnDefault_When_IntegerNotSetWithDefault() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getInteger("key", 42)).isEqualTo(42);
    }

    @Test
    void should_ReturnInteger_When_IntegerSet() {
        TypedHashMap map = new TypedHashMap();
        map.setInteger("key", 123);

        assertThat(map.getInteger("key")).isEqualTo(123);
    }

    @Test
    void should_ReturnInteger_When_IntegerSetAndDefaultProvided() {
        TypedHashMap map = new TypedHashMap();
        map.setInteger("key", 123);

        assertThat(map.getInteger("key", 42)).isEqualTo(123);
    }

    @Test
    void should_ReturnBoundedValue_When_IntegerBelowMin() {
        TypedHashMap map = new TypedHashMap();
        map.setInteger("key", 5);

        assertThat(map.getInteger("key", 50, 10, 100)).isEqualTo(10);
    }

    @Test
    void should_ReturnBoundedValue_When_IntegerAboveMax() {
        TypedHashMap map = new TypedHashMap();
        map.setInteger("key", 150);

        assertThat(map.getInteger("key", 50, 10, 100)).isEqualTo(100);
    }

    @Test
    void should_ReturnValue_When_IntegerWithinBounds() {
        TypedHashMap map = new TypedHashMap();
        map.setInteger("key", 50);

        assertThat(map.getInteger("key", 0, 10, 100)).isEqualTo(50);
    }

    @Test
    void should_ReturnDefault_When_IntegerNotSetWithBounds() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getInteger("key", 50, 10, 100)).isEqualTo(50);
    }

    @Test
    void should_RemoveInteger_When_RemoveIntegerCalled() {
        TypedHashMap map = new TypedHashMap();
        map.setInteger("key", 123);

        Integer removed = map.removeInteger("key");

        assertThat(removed).isEqualTo(123);
        assertThat(map.getInteger("key")).isNull();
    }

    // =================================================================
    // Long Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_LongNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getLong("key")).isNull();
    }

    @Test
    void should_ReturnDefault_When_LongNotSetWithDefault() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getLong("key", 42L)).isEqualTo(42L);
    }

    @Test
    void should_ReturnLong_When_LongSet() {
        TypedHashMap map = new TypedHashMap();
        map.setLong("key", 123L);

        assertThat(map.getLong("key")).isEqualTo(123L);
    }

    @Test
    void should_ReturnLong_When_LongSetAndDefaultProvided() {
        TypedHashMap map = new TypedHashMap();
        map.setLong("key", 123L);

        assertThat(map.getLong("key", 42L)).isEqualTo(123L);
    }

    @Test
    void should_RemoveLong_When_RemoveLongCalled() {
        TypedHashMap map = new TypedHashMap();
        map.setLong("key", 123L);

        Long removed = map.removeLong("key");

        assertThat(removed).isEqualTo(123L);
        assertThat(map.getLong("key")).isNull();
    }

    @Test
    void should_ReturnDate_When_LongAsDateCalled() {
        TypedHashMap map = new TypedHashMap();
        long timestamp = 1234567890000L;
        map.setLong("key", timestamp);

        Date date = map.getLongAsDate("key");

        assertThat(date).isNotNull();
        assertThat(date.getTime()).isEqualTo(timestamp);
    }

    @Test
    void should_ReturnNull_When_LongAsDateCalledOnMissing() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getLongAsDate("key")).isNull();
    }

    @Test
    void should_StoreDateAsLong_When_SetLongFromDateCalled() {
        TypedHashMap map = new TypedHashMap();
        Date date = new Date(1234567890000L);

        map.setLongFromDate("key", date);

        assertThat(map.getLong("key")).isEqualTo(1234567890000L);
    }

    @Test
    void should_RemoveEntry_When_SetLongFromDateCalledWithNull() {
        TypedHashMap map = new TypedHashMap();
        map.setLong("key", 123L);

        map.setLongFromDate("key", null);

        assertThat(map.getLong("key")).isNull();
    }

    // =================================================================
    // Double Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_DoubleNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getDouble("key")).isNull();
    }

    @Test
    void should_ReturnDefault_When_DoubleNotSetWithDefault() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getDouble("key", 3.14)).isEqualTo(3.14);
    }

    @Test
    void should_ReturnDouble_When_DoubleSet() {
        TypedHashMap map = new TypedHashMap();
        map.setDouble("key", 2.71);

        assertThat(map.getDouble("key")).isEqualTo(2.71);
    }

    @Test
    void should_ReturnDouble_When_DoubleSetAndDefaultProvided() {
        TypedHashMap map = new TypedHashMap();
        map.setDouble("key", 2.71);

        assertThat(map.getDouble("key", 3.14)).isEqualTo(2.71);
    }

    @Test
    void should_RemoveDouble_When_RemoveDoubleCalled() {
        TypedHashMap map = new TypedHashMap();
        map.setDouble("key", 2.71);

        Double removed = map.removeDouble("key");

        assertThat(removed).isEqualTo(2.71);
        assertThat(map.getDouble("key")).isNull();
    }

    // =================================================================
    // Boolean Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_BooleanNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getBoolean("key")).isNull();
    }

    @Test
    void should_ReturnDefault_When_BooleanNotSetWithDefault() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getBoolean("key", true)).isTrue();
        assertThat(map.getBoolean("key", false)).isFalse();
    }

    @Test
    void should_ReturnBoolean_When_BooleanSet() {
        TypedHashMap map = new TypedHashMap();
        map.setBoolean("key", true);

        assertThat(map.getBoolean("key")).isTrue();
    }

    @Test
    void should_ReturnBoolean_When_BooleanSetAndDefaultProvided() {
        TypedHashMap map = new TypedHashMap();
        map.setBoolean("key", true);

        assertThat(map.getBoolean("key", false)).isTrue();
    }

    @Test
    void should_RemoveBoolean_When_RemoveBooleanCalled() {
        TypedHashMap map = new TypedHashMap();
        map.setBoolean("key", true);

        Boolean removed = map.removeBoolean("key");

        assertThat(removed).isTrue();
        assertThat(map.getBoolean("key")).isNull();
    }

    // =================================================================
    // List Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_ListNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getList("key")).isNull();
    }

    @Test
    void should_ReturnList_When_ListSet() {
        TypedHashMap map = new TypedHashMap();
        List<String> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");

        map.setList("key", list);

        assertThat(map.getList("key")).isEqualTo(list);
    }

    @Test
    void should_RemoveList_When_RemoveListCalled() {
        TypedHashMap map = new TypedHashMap();
        List<String> list = new ArrayList<>();
        list.add("item1");
        map.setList("key", list);

        List<?> removed = map.removeList("key");

        assertThat(removed).isEqualTo(list);
        assertThat(map.getList("key")).isNull();
    }

    // =================================================================
    // Object Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_ObjectNotSet() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.getObject("key")).isNull();
    }

    @Test
    void should_ReturnObject_When_ObjectSet() {
        TypedHashMap map = new TypedHashMap();
        Object obj = new Object();

        map.setObject("key", obj);

        assertThat(map.getObject("key")).isEqualTo(obj);
    }

    @Test
    void should_RemoveObject_When_RemoveObjectCalled() {
        TypedHashMap map = new TypedHashMap();
        Object obj = new Object();
        map.setObject("key", obj);

        Object removed = map.removeObject("key");

        assertThat(removed).isEqualTo(obj);
        assertThat(map.getObject("key")).isNull();
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnEmptyString_When_MapIsEmpty() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.toString()).isEqualTo("");
    }

    @Test
    void should_ReturnFormattedString_When_MapHasSingleEntry() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key", "value");

        assertThat(map.toString()).isEqualTo("key=value");
    }

    @Test
    void should_ReturnFormattedString_When_MapHasMultipleEntries() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key1", "value1");
        map.setString("key2", "value2");

        String result = map.toString();

        // TreeMap sorts keys, so we can predict the order
        assertThat(result).isEqualTo("key1=value1, key2=value2");
    }

    @Test
    void should_FormatList_When_ToStringCalledWithList() {
        TypedHashMap map = new TypedHashMap();
        List<String> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");
        map.setList("key", list);

        assertThat(map.toString()).isEqualTo("key=[item1, item2]");
    }

    @Test
    void should_FormatEmptyList_When_ToStringCalledWithEmptyList() {
        TypedHashMap map = new TypedHashMap();
        List<String> list = new ArrayList<>();
        map.setList("key", list);

        assertThat(map.toString()).isEqualTo("key=[]");
    }

    @Test
    void should_FormatMixedTypes_When_ToStringCalledWithMixedTypes() {
        TypedHashMap map = new TypedHashMap();
        map.setString("str", "value");
        map.setInteger("int", 42);
        map.setBoolean("bool", true);

        String result = map.toString();

        // TreeMap sorts keys alphabetically
        assertThat(result).isEqualTo("bool=true, int=42, str=value");
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleNullValues_When_SetToNull() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key", null);

        assertThat(map.getString("key")).isNull();
        assertThat(map.getString("key", "default")).isEqualTo("default");
    }

    @Test
    void should_OverwriteValue_When_SetTwice() {
        TypedHashMap map = new TypedHashMap();
        map.setString("key", "value1");
        map.setString("key", "value2");

        assertThat(map.getString("key")).isEqualTo("value2");
    }

    @Test
    void should_HandleMultipleTypes_When_SameKeyUsedWithDifferentTypes() {
        TypedHashMap map = new TypedHashMap();

        map.setString("key", "value");
        assertThat(map.getString("key")).isEqualTo("value");

        map.setInteger("key", 42);
        assertThat(map.getInteger("key")).isEqualTo(42);
        // Note: getString will now throw ClassCastException, which is expected behavior
    }

    @Test
    void should_ReturnNull_When_RemovingNonExistentKey() {
        TypedHashMap map = new TypedHashMap();

        assertThat(map.removeString("missing")).isNull();
        assertThat(map.removeInteger("missing")).isNull();
        assertThat(map.removeLong("missing")).isNull();
        assertThat(map.removeDouble("missing")).isNull();
        assertThat(map.removeBoolean("missing")).isNull();
        assertThat(map.removeList("missing")).isNull();
        assertThat(map.removeObject("missing")).isNull();
    }
}
