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
 * Tests for DatabaseColumn - represents a database column with name, type, and
 * flags.
 */
class DatabaseColumnTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_SetNameAndType_When_Constructed() {
        DatabaseColumn column = new DatabaseColumn("user_id", Types.INTEGER);

        assertThat(column.getName()).isEqualTo("user_id");
        assertThat(column.getType()).isEqualTo(Types.INTEGER);
    }

    @Test
    void should_AcceptNullName_When_Constructed() {
        DatabaseColumn column = new DatabaseColumn(null, Types.VARCHAR);

        assertThat(column.getName()).isNull();
        assertThat(column.getType()).isEqualTo(Types.VARCHAR);
    }

    // =================================================================
    // Name / toString Tests
    // =================================================================

    @Test
    void should_ReturnName_When_ToStringCalled() {
        DatabaseColumn column = new DatabaseColumn("email", Types.VARCHAR);

        assertThat(column.toString()).isEqualTo("email");
    }

    @Test
    void should_ReturnNull_When_ToStringCalledOnNullName() {
        DatabaseColumn column = new DatabaseColumn(null, Types.VARCHAR);

        assertThat(column.toString()).isNull();
    }

    // =================================================================
    // Type Setter Tests
    // =================================================================

    @Test
    void should_UpdateType_When_SetTypeCalled() {
        DatabaseColumn column = new DatabaseColumn("data", Types.VARCHAR);

        column.setType(Types.BLOB);

        assertThat(column.getType()).isEqualTo(Types.BLOB);
    }

    // =================================================================
    // Sequence Flag Tests
    // =================================================================

    @Test
    void should_DefaultToFalse_When_SequenceNotSet() {
        DatabaseColumn column = new DatabaseColumn("id", Types.INTEGER);

        assertThat(column.isSequence()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_SequenceSetTrue() {
        DatabaseColumn column = new DatabaseColumn("id", Types.INTEGER);

        column.setSequence(true);

        assertThat(column.isSequence()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_SequenceSetFalse() {
        DatabaseColumn column = new DatabaseColumn("id", Types.INTEGER);
        column.setSequence(true);

        column.setSequence(false);

        assertThat(column.isSequence()).isFalse();
    }

    // =================================================================
    // CreateDate Flag Tests
    // =================================================================

    @Test
    void should_DefaultToFalse_When_CreateDateNotSet() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);

        assertThat(column.isCreateDate()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_CreateDateSetTrue() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);

        column.setCreateDate(true);

        assertThat(column.isCreateDate()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_CreateDateSetFalse() {
        DatabaseColumn column = new DatabaseColumn("created_at", Types.TIMESTAMP);
        column.setCreateDate(true);

        column.setCreateDate(false);

        assertThat(column.isCreateDate()).isFalse();
    }

    // =================================================================
    // ModifyDate Flag Tests
    // =================================================================

    @Test
    void should_DefaultToFalse_When_ModifyDateNotSet() {
        DatabaseColumn column = new DatabaseColumn("updated_at", Types.TIMESTAMP);

        assertThat(column.isModifyDate()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ModifyDateSetTrue() {
        DatabaseColumn column = new DatabaseColumn("updated_at", Types.TIMESTAMP);

        column.setModifyDate(true);

        assertThat(column.isModifyDate()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_ModifyDateSetFalse() {
        DatabaseColumn column = new DatabaseColumn("updated_at", Types.TIMESTAMP);
        column.setModifyDate(true);

        column.setModifyDate(false);

        assertThat(column.isModifyDate()).isFalse();
    }

    // =================================================================
    // DataMarshal Flag Tests
    // =================================================================

    @Test
    void should_DefaultToFalse_When_DataMarshalNotSet() {
        DatabaseColumn column = new DatabaseColumn("payload", Types.VARCHAR);

        assertThat(column.isDataMarshal()).isFalse();
    }

    @Test
    void should_ReturnTrue_When_DataMarshalSetTrue() {
        DatabaseColumn column = new DatabaseColumn("payload", Types.VARCHAR);

        column.setDataMarshal(true);

        assertThat(column.isDataMarshal()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_DataMarshalSetFalse() {
        DatabaseColumn column = new DatabaseColumn("payload", Types.VARCHAR);
        column.setDataMarshal(true);

        column.setDataMarshal(false);

        assertThat(column.isDataMarshal()).isFalse();
    }

    // =================================================================
    // Multiple Flags Tests
    // =================================================================

    @Test
    void should_AllowMultipleFlags_When_SetIndependently() {
        DatabaseColumn column = new DatabaseColumn("special", Types.VARCHAR);

        column.setSequence(true);
        column.setCreateDate(true);
        column.setModifyDate(true);
        column.setDataMarshal(true);

        assertThat(column.isSequence()).isTrue();
        assertThat(column.isCreateDate()).isTrue();
        assertThat(column.isModifyDate()).isTrue();
        assertThat(column.isDataMarshal()).isTrue();
    }

    // =================================================================
    // Various SQL Types Tests
    // =================================================================

    @Test
    void should_AcceptTimestampType_When_Constructed() {
        DatabaseColumn column = new DatabaseColumn("ts", Types.TIMESTAMP);

        assertThat(column.getType()).isEqualTo(Types.TIMESTAMP);
    }

    @Test
    void should_AcceptBlobType_When_Constructed() {
        DatabaseColumn column = new DatabaseColumn("data", Types.BLOB);

        assertThat(column.getType()).isEqualTo(Types.BLOB);
    }

    @Test
    void should_AcceptBooleanType_When_Constructed() {
        DatabaseColumn column = new DatabaseColumn("active", Types.BOOLEAN);

        assertThat(column.getType()).isEqualTo(Types.BOOLEAN);
    }
}
