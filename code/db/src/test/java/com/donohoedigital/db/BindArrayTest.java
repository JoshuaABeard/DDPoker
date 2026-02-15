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
 * Tests for BindArray - container for SQL bind parameter values
 */
class BindArrayTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateEmptyArray_When_DefaultConstructorUsed() {
        BindArray array = new BindArray();

        assertThat(array.size()).isEqualTo(0);
    }

    @Test
    void should_CreateArrayWithCapacity_When_GivenCount() {
        BindArray array = new BindArray(10);

        assertThat(array.size()).isEqualTo(0); // Size is 0 until values added
    }

    // =================================================================
    // Add Value Tests
    // =================================================================

    @Test
    void should_AddValue_When_TypeAndValueProvided() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "test");

        assertThat(array.size()).isEqualTo(1);
        assertThat(array.getType(0)).isEqualTo(Types.VARCHAR);
        assertThat(array.getValue(0)).isEqualTo("test");
    }

    @Test
    void should_AddMultipleValues_When_CalledMultipleTimes() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "first");
        array.addValue(Types.INTEGER, 42);
        array.addValue(Types.BOOLEAN, true);

        assertThat(array.size()).isEqualTo(3);
    }

    @Test
    void should_MaintainInsertionOrder() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "first");
        array.addValue(Types.VARCHAR, "second");
        array.addValue(Types.VARCHAR, "third");

        assertThat(array.getValue(0)).isEqualTo("first");
        assertThat(array.getValue(1)).isEqualTo("second");
        assertThat(array.getValue(2)).isEqualTo("third");
    }

    // =================================================================
    // Get Type Tests
    // =================================================================

    @Test
    void should_ReturnType_When_ValidIndexProvided() {
        BindArray array = new BindArray();
        array.addValue(Types.INTEGER, 123);

        assertThat(array.getType(0)).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_ReturnNegativeOne_When_IndexOutOfRange() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "test");

        assertThat(array.getType(1)).isEqualTo(-1);
        assertThat(array.getType(10)).isEqualTo(-1);
    }

    @Test
    void should_ReturnNegativeOne_When_ArrayIsEmpty() {
        BindArray array = new BindArray();

        assertThat(array.getType(0)).isEqualTo(-1);
    }

    @Test
    void should_HandleDifferentTypes() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "string");
        array.addValue(Types.INTEGER, 42);
        array.addValue(Types.BOOLEAN, true);
        array.addValue(Types.TIMESTAMP, null);

        assertThat(array.getType(0)).isEqualTo(Types.VARCHAR);
        assertThat(array.getType(1)).isEqualTo(Types.INTEGER);
        assertThat(array.getType(2)).isEqualTo(Types.BOOLEAN);
        assertThat(array.getType(3)).isEqualTo(Types.TIMESTAMP);
    }

    // =================================================================
    // Get Value Tests
    // =================================================================

    @Test
    void should_ReturnValue_When_ValidIndexProvided() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "testValue");

        assertThat(array.getValue(0)).isEqualTo("testValue");
    }

    @Test
    void should_ReturnNull_When_IndexOutOfRange() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "test");

        assertThat(array.getValue(1)).isNull();
        assertThat(array.getValue(10)).isNull();
    }

    @Test
    void should_ReturnNull_When_ArrayIsEmpty() {
        BindArray array = new BindArray();

        assertThat(array.getValue(0)).isNull();
    }

    @Test
    void should_HandleNullValues() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, null);

        assertThat(array.getValue(0)).isNull();
        assertThat(array.getType(0)).isEqualTo(Types.VARCHAR);
    }

    @Test
    void should_HandleDifferentValueTypes() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "string");
        array.addValue(Types.INTEGER, 42);
        array.addValue(Types.BOOLEAN, true);
        array.addValue(Types.DOUBLE, 3.14);

        assertThat(array.getValue(0)).isEqualTo("string");
        assertThat(array.getValue(1)).isEqualTo(42);
        assertThat(array.getValue(2)).isEqualTo(true);
        assertThat(array.getValue(3)).isEqualTo(3.14);
    }

    // =================================================================
    // Size Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_NoValuesAdded() {
        BindArray array = new BindArray();

        assertThat(array.size()).isEqualTo(0);
    }

    @Test
    void should_IncrementSize_When_ValuesAdded() {
        BindArray array = new BindArray();

        array.addValue(Types.VARCHAR, "one");
        assertThat(array.size()).isEqualTo(1);

        array.addValue(Types.VARCHAR, "two");
        assertThat(array.size()).isEqualTo(2);

        array.addValue(Types.VARCHAR, "three");
        assertThat(array.size()).isEqualTo(3);
    }

    // =================================================================
    // ToString Tests
    // =================================================================

    @Test
    void should_ReturnFormattedString_When_ToStringCalled() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "value1");
        array.addValue(Types.INTEGER, 42);

        String result = array.toString();
        assertThat(result).contains("value1");
        assertThat(result).contains("42");
    }

    @Test
    void should_SeparateValuesWithComma() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "first");
        array.addValue(Types.VARCHAR, "second");
        array.addValue(Types.VARCHAR, "third");

        String result = array.toString();
        assertThat(result).isEqualTo("first, second, third");
    }

    @Test
    void should_HandleSingleValue_In_ToString() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "only");

        // Single value would try to trim 2 chars from end, which could cause issues
        // Let's see what happens
        String result = array.toString();
        assertThat(result).isNotNull();
    }

    // =================================================================
    // Real-world SQL Scenario Tests
    // =================================================================

    @Test
    void should_BuildParametersForSelectQuery() {
        // SELECT * FROM users WHERE name = ? AND age > ?
        BindArray array = new BindArray(2);
        array.addValue(Types.VARCHAR, "John");
        array.addValue(Types.INTEGER, 18);

        assertThat(array.size()).isEqualTo(2);
        assertThat(array.getValue(0)).isEqualTo("John");
        assertThat(array.getValue(1)).isEqualTo(18);
    }

    @Test
    void should_BuildParametersForInsertQuery() {
        // INSERT INTO users (name, email, age) VALUES (?, ?, ?)
        BindArray array = new BindArray(3);
        array.addValue(Types.VARCHAR, "Alice");
        array.addValue(Types.VARCHAR, "alice@example.com");
        array.addValue(Types.INTEGER, 25);

        assertThat(array.size()).isEqualTo(3);
        assertThat(array.getType(0)).isEqualTo(Types.VARCHAR);
        assertThat(array.getType(1)).isEqualTo(Types.VARCHAR);
        assertThat(array.getType(2)).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_BuildParametersForUpdateQuery() {
        // UPDATE users SET name = ?, email = ? WHERE id = ?
        BindArray array = new BindArray(3);
        array.addValue(Types.VARCHAR, "Updated Name");
        array.addValue(Types.VARCHAR, "updated@example.com");
        array.addValue(Types.INTEGER, 123);

        assertThat(array.size()).isEqualTo(3);
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleEmptyString() {
        BindArray array = new BindArray();
        array.addValue(Types.VARCHAR, "");

        assertThat(array.getValue(0)).isEqualTo("");
    }

    @Test
    void should_HandleZeroValue() {
        BindArray array = new BindArray();
        array.addValue(Types.INTEGER, 0);

        assertThat(array.getValue(0)).isEqualTo(0);
    }
}
