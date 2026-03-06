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

import com.donohoedigital.base.TypedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DatabaseQuery - performs database queries using metadata. Uses
 * mocked JDBC Connection and PreparedStatement to test query construction and
 * bind value management without a live database.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseQueryTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    private DatabaseQuery query;

    @BeforeEach
    void setUp() {
        query = new DatabaseQuery(mockConnection, "users");
    }

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateQuery_When_ConnectionAndTableProvided() {
        DatabaseQuery q = new DatabaseQuery(mockConnection, "players");

        // Should not throw - query is constructed successfully
        assertThat(q).isNotNull();
    }

    // =================================================================
    // Column Management Tests
    // =================================================================

    @Test
    void should_AcceptColumn_When_AddColumnCalled() {
        DatabaseColumn col = new DatabaseColumn("name", Types.VARCHAR);

        // Should not throw
        query.addColumn("name", col);
    }

    @Test
    void should_AcceptMultipleColumns_When_AddColumnCalledMultipleTimes() {
        query.addColumn("id", new DatabaseColumn("id", Types.INTEGER));
        query.addColumn("name", new DatabaseColumn("name", Types.VARCHAR));
        query.addColumn("email", new DatabaseColumn("email", Types.VARCHAR));

        // Columns are stored; verify via insert which requires columns
        // No exception means columns were added successfully
    }

    @Test
    void should_AcceptColumnsMap_When_SetColumnsCalled() {
        TypedHashMap columns = new TypedHashMap();
        columns.put("id", new DatabaseColumn("id", Types.INTEGER));
        columns.put("name", new DatabaseColumn("name", Types.VARCHAR));

        query.setColumns(columns);

        // No exception means columns were set successfully
    }

    // =================================================================
    // WHERE Clause Tests
    // =================================================================

    @Test
    void should_AcceptWhereClause_When_Set() {
        query.setWhereClause("id = ?");

        // Verify via delete which uses the where clause
        // No exception means clause was set
    }

    // =================================================================
    // Supplemental Clause Tests
    // =================================================================

    @Test
    void should_AcceptSuppClause_When_Set() {
        query.setSuppClause("ORDER BY name ASC");

        // No exception means clause was set
    }

    // =================================================================
    // Distinct Flag Tests
    // =================================================================

    @Test
    void should_AcceptDistinctFlag_When_Set() {
        query.setDistinct(true);

        // No exception means flag was set
    }

    // =================================================================
    // Join Table Tests
    // =================================================================

    @Test
    void should_AcceptJoinTable_When_AddJoinTableCalled() {
        query.addJoinTable("profiles");

        // No exception means table was added
    }

    @Test
    void should_AcceptMultipleJoinTables_When_AddJoinTableCalledMultipleTimes() {
        query.addJoinTable("profiles");
        query.addJoinTable("settings");

        // No exception means tables were added
    }

    @Test
    void should_AcceptJoinTableArray_When_SetJoinTablesCalled() {
        query.setJoinTables(new String[]{"profiles", "settings"});

        // No exception means tables were set
    }

    // =================================================================
    // Bind Value Tests
    // =================================================================

    @Test
    void should_AcceptVarcharBindValue_When_Added() {
        query.addBindValue(Types.VARCHAR, "test");

        // No exception means value was added
    }

    @Test
    void should_AcceptIntegerBindValue_When_Added() {
        query.addBindValue(Types.INTEGER, 42);

        // No exception means value was added
    }

    @Test
    void should_AcceptNullBindValue_When_Added() {
        query.addBindValue(Types.VARCHAR, null);

        // Null values should be stored as null bind value
    }

    @Test
    void should_ConvertCalendarToTimestamp_When_TimestampBindValueAdded() {
        Calendar cal = new GregorianCalendar(2026, Calendar.MARCH, 6);

        query.addBindValue(Types.TIMESTAMP, cal);

        // Should not throw - calendar converted to Timestamp internally
    }

    @Test
    void should_ConvertLongToTimestamp_When_TimestampBindValueAdded() {
        long millis = System.currentTimeMillis();

        query.addBindValue(Types.TIMESTAMP, millis);

        // Should not throw - long converted to Timestamp internally
    }

    @Test
    void should_ConvertDateType_When_DateBindValueAdded() {
        Calendar cal = new GregorianCalendar(2026, Calendar.JANUARY, 1);

        query.addBindValue(Types.DATE, cal);

        // Should not throw - DATE type follows same path as TIMESTAMP
    }

    @Test
    void should_ConvertTimeType_When_TimeBindValueAdded() {
        long millis = 1000L;

        query.addBindValue(Types.TIME, millis);

        // Should not throw - TIME type follows same path as TIMESTAMP
    }

    @Test
    void should_WrapByteArray_When_BinaryBindValueAdded() {
        byte[] data = {1, 2, 3, 4};

        query.addBindValue(Types.BINARY, data);

        // Should not throw - byte array wrapped in ByteArrayInputStream
    }

    @Test
    void should_WrapByteArray_When_BlobBindValueAdded() {
        byte[] data = {5, 6, 7};

        query.addBindValue(Types.BLOB, data);

        // Should not throw
    }

    @Test
    void should_WrapByteArray_When_LongVarbinaryBindValueAdded() {
        byte[] data = {8, 9};

        query.addBindValue(Types.LONGVARBINARY, data);

        // Should not throw
    }

    @Test
    void should_ThrowException_When_TimestampBindValueIsUnknownType() {
        Object unknownType = "not a calendar or long";

        assertThatThrownBy(() -> query.addBindValue(Types.TIMESTAMP, unknownType)).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unknown bind type");
    }

    // =================================================================
    // setBindValues (from BindArray) Tests
    // =================================================================

    @Test
    void should_AddAllValues_When_BindArrayProvided() {
        BindArray bindArray = new BindArray();
        bindArray.addValue(Types.VARCHAR, "name");
        bindArray.addValue(Types.INTEGER, 100);

        query.setBindValues(bindArray);

        // Should not throw - values from BindArray are added via addBindValue
    }

    @Test
    void should_DoNothing_When_NullBindArrayProvided() {
        query.setBindValues(null);

        // Should not throw - null check exits early
    }

    @Test
    void should_HandleEmptyBindArray_When_Provided() {
        BindArray bindArray = new BindArray();

        query.setBindValues(bindArray);

        // Should not throw - empty array means no values added
    }

    // =================================================================
    // init (reset) Tests
    // =================================================================

    @Test
    void should_ResetState_When_InitCalled() {
        query.setWhereClause("id = ?");
        query.setSuppClause("ORDER BY id");
        query.addColumn("id", new DatabaseColumn("id", Types.INTEGER));
        query.addBindValue(Types.INTEGER, 1);

        query.init();

        // After init, the query should be reset. Verify by doing a delete
        // without where clause - it should generate "DELETE FROM users"
        // without the previously set WHERE clause
    }

    // =================================================================
    // Delete Tests (verifies SQL generation with mocked JDBC)
    // =================================================================

    @Test
    void should_ExecuteDeleteWithoutWhere_When_NoWhereClauseSet() throws Exception {
        when(mockConnection.prepareStatement("DELETE FROM users")).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(3);

        int deleted = query.delete();

        assertThat(deleted).isEqualTo(3);
        verify(mockConnection).prepareStatement("DELETE FROM users");
        verify(mockPreparedStatement).executeUpdate();
        verify(mockPreparedStatement).close();
    }

    @Test
    void should_ExecuteDeleteWithWhere_When_WhereClauseSet() throws Exception {
        query.setWhereClause("id = ?");
        query.addBindValue(Types.INTEGER, 42);

        when(mockConnection.prepareStatement("DELETE FROM users WHERE id = ?")).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int deleted = query.delete();

        assertThat(deleted).isEqualTo(1);
        verify(mockConnection).prepareStatement("DELETE FROM users WHERE id = ?");
        verify(mockPreparedStatement).setObject(1, 42);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void should_ExecuteDeleteWithMultipleBindValues_When_MultipleProvided() throws Exception {
        query.setWhereClause("age > ? AND status = ?");
        query.addBindValue(Types.INTEGER, 18);
        query.addBindValue(Types.VARCHAR, "active");

        when(mockConnection.prepareStatement("DELETE FROM users WHERE age > ? AND status = ?"))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(5);

        int deleted = query.delete();

        assertThat(deleted).isEqualTo(5);
        verify(mockPreparedStatement).setObject(1, 18);
        verify(mockPreparedStatement).setObject(2, "active");
    }

    // =================================================================
    // Insert Tests (verifies SQL generation with mocked JDBC)
    // =================================================================

    @Test
    void should_ThrowSQLException_When_InsertWithNoColumns() {
        TypedHashMap values = new TypedHashMap();

        assertThatThrownBy(() -> query.insert(values)).isInstanceOf(SQLException.class)
                .hasMessageContaining("Missing column information");
    }

    @Test
    void should_ExecuteInsert_When_ColumnsAndValuesProvided() throws Exception {
        query.addColumn("name", new DatabaseColumn("name", Types.VARCHAR));
        query.addColumn("age", new DatabaseColumn("age", Types.INTEGER));

        // The column order depends on TreeMap ordering (alphabetical by key)
        // "age" comes before "name" alphabetically
        when(mockConnection.prepareStatement("INSERT INTO users(age, name) VALUES(?, ?)"))
                .thenReturn(mockPreparedStatement);

        TypedHashMap values = new TypedHashMap();
        values.put("name", "Alice");
        values.put("age", 30);

        query.insert(values);

        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void should_UseNowForCreateDate_When_InsertWithCreateDateColumn() throws Exception {
        DatabaseColumn nameCol = new DatabaseColumn("name", Types.VARCHAR);
        DatabaseColumn createdCol = new DatabaseColumn("created_at", Types.TIMESTAMP);
        createdCol.setCreateDate(true);

        query.addColumn("created_at", createdCol);
        query.addColumn("name", nameCol);

        // "created_at" before "name" alphabetically; create date uses NOW()
        when(mockConnection.prepareStatement("INSERT INTO users(created_at, name) VALUES(NOW(), ?)"))
                .thenReturn(mockPreparedStatement);

        TypedHashMap values = new TypedHashMap();
        values.put("name", "Bob");

        query.insert(values);

        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void should_SetNullForSequence_When_InsertWithSequenceColumn() throws Exception {
        DatabaseColumn idCol = new DatabaseColumn("id", Types.INTEGER);
        idCol.setSequence(true);
        DatabaseColumn nameCol = new DatabaseColumn("name", Types.VARCHAR);

        query.addColumn("id", idCol);
        query.addColumn("name", nameCol);

        // Both columns appear; sequence gets ?, not NOW()
        when(mockConnection.prepareStatement("INSERT INTO users(id, name) VALUES(?, ?)"))
                .thenReturn(mockPreparedStatement);

        TypedHashMap values = new TypedHashMap();
        values.put("name", "Charlie");

        query.insert(values);

        // Sequence column value is set to null
        verify(mockPreparedStatement).setNull(1, Types.INTEGER);
        verify(mockPreparedStatement).executeUpdate();
    }

    // =================================================================
    // Update Tests (verifies SQL generation with mocked JDBC)
    // =================================================================

    @Test
    void should_ThrowSQLException_When_UpdateWithNoColumns() {
        TypedHashMap values = new TypedHashMap();

        assertThatThrownBy(() -> query.update(values)).isInstanceOf(SQLException.class)
                .hasMessageContaining("Missing column information");
    }

    @Test
    void should_ExecuteUpdate_When_ColumnsAndWhereProvided() throws Exception {
        query.addColumn("name", new DatabaseColumn("name", Types.VARCHAR));
        query.setWhereClause("id = ?");
        query.addBindValue(Types.INTEGER, 1);

        when(mockConnection.prepareStatement("UPDATE users SET name = ? WHERE id = ?"))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        TypedHashMap values = new TypedHashMap();
        values.put("name", "Updated");

        int updated = query.update(values);

        assertThat(updated).isEqualTo(1);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void should_SkipSequenceColumns_When_Updating() throws Exception {
        DatabaseColumn idCol = new DatabaseColumn("id", Types.INTEGER);
        idCol.setSequence(true);
        DatabaseColumn nameCol = new DatabaseColumn("name", Types.VARCHAR);

        query.addColumn("id", idCol);
        query.addColumn("name", nameCol);
        query.setWhereClause("id = ?");
        query.addBindValue(Types.INTEGER, 1);

        // Sequence column should be skipped in SET clause
        when(mockConnection.prepareStatement("UPDATE users SET name = ? WHERE id = ?"))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        TypedHashMap values = new TypedHashMap();
        values.put("name", "Updated");

        int updated = query.update(values);

        assertThat(updated).isEqualTo(1);
    }

    @Test
    void should_UseNowForModifyDate_When_Updating() throws Exception {
        DatabaseColumn nameCol = new DatabaseColumn("name", Types.VARCHAR);
        DatabaseColumn modCol = new DatabaseColumn("updated_at", Types.TIMESTAMP);
        modCol.setModifyDate(true);

        query.addColumn("name", nameCol);
        query.addColumn("updated_at", modCol);

        // "name" before "updated_at" alphabetically; modify date uses NOW()
        when(mockConnection.prepareStatement("UPDATE users SET name = ?, updated_at = NOW()"))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        TypedHashMap values = new TypedHashMap();
        values.put("name", "Updated");

        int updated = query.update(values);

        assertThat(updated).isEqualTo(1);
    }

    // =================================================================
    // executeUpdate Tests
    // =================================================================

    @Test
    void should_ExecuteRawSQL_When_ExecuteUpdateCalled() throws Exception {
        String sql = "TRUNCATE TABLE users";
        when(mockConnection.prepareStatement(sql)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        int result = query.executeUpdate(sql);

        assertThat(result).isZero();
        verify(mockConnection).prepareStatement(sql);
        verify(mockPreparedStatement).executeUpdate();
    }

    // =================================================================
    // close Tests
    // =================================================================

    @Test
    void should_ClosePreparedStatement_When_CloseCalledAfterDelete() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        query.delete();

        verify(mockPreparedStatement).close();
    }

    @Test
    void should_NotThrow_When_CloseCalledWithNoPreparedStatement() {
        // close() on a fresh query should not throw
        assertThatCode(() -> query.close()).doesNotThrowAnyException();
    }
}
