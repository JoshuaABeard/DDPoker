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
 * Tests for DatabaseColumn - represents database column metadata
 */
class DatabaseColumnTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateColumn_When_GivenNameAndType() {
        DatabaseColumn column = new DatabaseColumn("user_id", Types.INTEGER);

        assertThat(column.getName()).isEqualTo("user_id");
        assertThat(column.getType()).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_UseDefaultValues_When_Created() {
        DatabaseColumn column = new DatabaseColumn("column_name", Types.VARCHAR);

        assertThat(column.isSequence()).isFalse();
        assertThat(column.isCreateDate()).isFalse();
        assertThat(column.isModifyDate()).isFalse();
        assertThat(column.isDataMarshal()).isFalse();
    }

    // =================================================================
    // Name Tests
    // =================================================================

    @Test
    void should_ReturnName_When_GetNameCalled() {
        DatabaseColumn column = new DatabaseColumn("email", Types.VARCHAR);

        assertThat(column.getName()).isEqualTo("email");
    }

    @Test
    void should_ReturnName_When_ToStringCalled() {
        DatabaseColumn column = new DatabaseColumn("username", Types.VARCHAR);

        assertThat(column.toString()).isEqualTo("username");
    }

    // =================================================================
    // Type Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveType() {
        DatabaseColumn column = new DatabaseColumn("count", Types.INTEGER);

        assertThat(column.getType()).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_UpdateType_When_SetTypeCalled() {
        DatabaseColumn column = new DatabaseColumn("value", Types.VARCHAR);
        column.setType(Types.INTEGER);

        assertThat(column.getType()).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_HandleVarcharType() {
        DatabaseColumn column = new DatabaseColumn("description", Types.VARCHAR);

        assertThat(column.getType()).isEqualTo(Types.VARCHAR);
    }

    @Test
    void should_HandleTimestampType() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);

        assertThat(column.getType()).isEqualTo(Types.TIMESTAMP);
    }

    @Test
    void should_HandleBooleanType() {
        DatabaseColumn column = new DatabaseColumn("is_active", Types.BOOLEAN);

        assertThat(column.getType()).isEqualTo(Types.BOOLEAN);
    }

    // =================================================================
    // Sequence Flag Tests
    // =================================================================

    @Test
    void should_SetAndGetSequenceFlag() {
        DatabaseColumn column = new DatabaseColumn("id", Types.INTEGER);
        column.setSequence(true);

        assertThat(column.isSequence()).isTrue();
    }

    @Test
    void should_UnsetSequenceFlag_When_SetToFalse() {
        DatabaseColumn column = new DatabaseColumn("id", Types.INTEGER);
        column.setSequence(true);
        column.setSequence(false);

        assertThat(column.isSequence()).isFalse();
    }

    // =================================================================
    // Create Date Flag Tests
    // =================================================================

    @Test
    void should_SetAndGetCreateDateFlag() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);
        column.setCreateDate(true);

        assertThat(column.isCreateDate()).isTrue();
    }

    @Test
    void should_UnsetCreateDateFlag_When_SetToFalse() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);
        column.setCreateDate(true);
        column.setCreateDate(false);

        assertThat(column.isCreateDate()).isFalse();
    }

    // =================================================================
    // Modify Date Flag Tests
    // =================================================================

    @Test
    void should_SetAndGetModifyDateFlag() {
        DatabaseColumn column = new DatabaseColumn("updated_at", Types.TIMESTAMP);
        column.setModifyDate(true);

        assertThat(column.isModifyDate()).isTrue();
    }

    @Test
    void should_UnsetModifyDateFlag_When_SetToFalse() {
        DatabaseColumn column = new DatabaseColumn("updated_at", Types.TIMESTAMP);
        column.setModifyDate(true);
        column.setModifyDate(false);

        assertThat(column.isModifyDate()).isFalse();
    }

    // =================================================================
    // Data Marshal Flag Tests
    // =================================================================

    @Test
    void should_SetAndGetDataMarshalFlag() {
        DatabaseColumn column = new DatabaseColumn("config_data", Types.BLOB);
        column.setDataMarshal(true);

        assertThat(column.isDataMarshal()).isTrue();
    }

    @Test
    void should_UnsetDataMarshalFlag_When_SetToFalse() {
        DatabaseColumn column = new DatabaseColumn("config_data", Types.BLOB);
        column.setDataMarshal(true);
        column.setDataMarshal(false);

        assertThat(column.isDataMarshal()).isFalse();
    }

    // =================================================================
    // Combined Flag Tests
    // =================================================================

    @Test
    void should_AllowMultipleFlagsSet_Simultaneously() {
        DatabaseColumn column = new DatabaseColumn("id", Types.INTEGER);
        column.setSequence(true);
        column.setCreateDate(true);

        assertThat(column.isSequence()).isTrue();
        assertThat(column.isCreateDate()).isTrue();
        assertThat(column.isModifyDate()).isFalse();
        assertThat(column.isDataMarshal()).isFalse();
    }

    @Test
    void should_MaintainIndependentFlags() {
        DatabaseColumn column = new DatabaseColumn("data", Types.BLOB);
        column.setDataMarshal(true);
        column.setModifyDate(true);

        assertThat(column.isDataMarshal()).isTrue();
        assertThat(column.isModifyDate()).isTrue();
        assertThat(column.isSequence()).isFalse();
        assertThat(column.isCreateDate()).isFalse();
    }

    // =================================================================
    // Real-world Scenario Tests
    // =================================================================

    @Test
    void should_ConfigurePrimaryKeyColumn() {
        DatabaseColumn column = new DatabaseColumn("id", Types.BIGINT);
        column.setSequence(true);

        assertThat(column.getName()).isEqualTo("id");
        assertThat(column.getType()).isEqualTo(Types.BIGINT);
        assertThat(column.isSequence()).isTrue();
    }

    @Test
    void should_ConfigureAuditColumn() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);
        column.setCreateDate(true);

        assertThat(column.getName()).isEqualTo("created_at");
        assertThat(column.isCreateDate()).isTrue();
    }

    @Test
    void should_ConfigureModifiedColumn() {
        DatabaseColumn column = new DatabaseColumn("modified_at", Types.TIMESTAMP);
        column.setModifyDate(true);

        assertThat(column.getName()).isEqualTo("modified_at");
        assertThat(column.isModifyDate()).isTrue();
    }

    @Test
    void should_ConfigureSerializedDataColumn() {
        DatabaseColumn column = new DatabaseColumn("settings", Types.BLOB);
        column.setDataMarshal(true);

        assertThat(column.getName()).isEqualTo("settings");
        assertThat(column.isDataMarshal()).isTrue();
    }
}
