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
package com.donohoedigital.db;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BindArray - a list of typed bind values used in database queries.
 */
class BindArrayTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateEmptyArray_When_DefaultConstructorUsed() {
        BindArray array = new BindArray();

        assertThat(array.size()).isZero();
    }

    @Test
    void should_CreateEmptyArray_When_CapacityConstructorUsed() {
        BindArray array = new BindArray(10);

        assertThat(array.size()).isZero();
    }

    // =================================================================
    // addValue / size Tests
    // =================================================================

    @Test
    void should_IncrementSize_When_ValueAdded() {
        BindArray array = new BindArray();

        array.addValue(Types.VARCHAR, "test");

        assertThat(array.size()).isEqualTo(1);
    }

    @Test
    void should_TrackMultipleValues_When_MultipleAdded() {
        BindArray array = new BindArray();

        array.addValue(Types.VARCHAR, "first");
        array.addValue(Types.INTEGER, 42);
        array.addValue(Types.BOOLEAN, true);

        assertThat(array.size()).isEqualTo(3);
    }

    // =================================================================
    // getType Tests
    // =================================================================

    @Test
    void should_ReturnCorrectType_When_IndexValid() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "text");
        array.addValue(Types.INTEGER, 42);

        assertThat(array.getType(0)).isEqualTo(Types.VARCHAR);
        assertThat(array.getType(1)).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_ReturnNegativeOne_When_IndexOutOfRange() {
        BindArray array = new BindArray();

        assertThat(array.getType(0)).isEqualTo(-1);
    }

    @Test
    void should_ReturnNegativeOne_When_IndexExceedsSize() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "only");

        assertThat(array.getType(5)).isEqualTo(-1);
    }

    // =================================================================
    // getValue Tests
    // =================================================================

    @Test
    void should_ReturnCorrectValue_When_IndexValid() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "hello");
        array.addValue(Types.INTEGER, 99);

        assertThat(array.getValue(0)).isEqualTo("hello");
        assertThat(array.getValue(1)).isEqualTo(99);
    }

    @Test
    void should_ReturnNull_When_IndexOutOfRange() {
        BindArray array = new BindArray();

        assertThat(array.getValue(0)).isNull();
    }

    @Test
    void should_ReturnNull_When_IndexExceedsSize() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "only");

        assertThat(array.getValue(5)).isNull();
    }

    // =================================================================
    // Null Value Tests
    // =================================================================

    @Test
    void should_StoreNullValue_When_NullProvided() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, null);

        assertThat(array.size()).isEqualTo(1);
        assertThat(array.getType(0)).isEqualTo(Types.VARCHAR);
        assertThat(array.getValue(0)).isNull();
    }

    // =================================================================
    // Various SQL Type Tests
    // =================================================================

    @Test
    void should_StoreDateType_When_TimestampTypeUsed() {
        BindArray array = new BindArray();
        long timestamp = System.currentTimeMillis();
        array.addValue(Types.TIMESTAMP, timestamp);

        assertThat(array.getType(0)).isEqualTo(Types.TIMESTAMP);
        assertThat(array.getValue(0)).isEqualTo(timestamp);
    }

    @Test
    void should_StoreBlobType_When_BinaryTypeUsed() {
        BindArray array = new BindArray();
        byte[] data = {1, 2, 3};
        array.addValue(Types.BLOB, data);

        assertThat(array.getType(0)).isEqualTo(Types.BLOB);
        assertThat(array.getValue(0)).isEqualTo(data);
    }

    @Test
    void should_StoreBooleanType_When_BooleanTypeUsed() {
        BindArray array = new BindArray();
        array.addValue(Types.BOOLEAN, true);

        assertThat(array.getType(0)).isEqualTo(Types.BOOLEAN);
        assertThat(array.getValue(0)).isEqualTo(true);
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnCommaSeparatedValues_When_MultipleValues() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "abc");
        array.addValue(Types.INTEGER, 42);
        array.addValue(Types.BOOLEAN, true);

        assertThat(array.toString()).isEqualTo("abc, 42, true");
    }

    @Test
    void should_ReturnSingleValue_When_OneValue() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "only");

        assertThat(array.toString()).isEqualTo("only");
    }

    @Test
    void should_IncludeNullText_When_NullValuePresent() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, null);
        array.addValue(Types.INTEGER, 1);

        assertThat(array.toString()).isEqualTo("null, 1");
    }
}
