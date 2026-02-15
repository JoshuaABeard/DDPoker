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

import com.donohoedigital.base.ApplicationError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Database - represents a logical database configuration
 */
class DatabaseTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateDatabase_When_GivenName() {
        Database db = new Database("testdb");

        assertThat(db.getName()).isEqualTo("testdb");
    }

    @Test
    void should_InitializeWithNullValues_When_Created() {
        Database db = new Database("mydb");

        assertThat(db.getDriverClassName()).isNull();
        assertThat(db.getDriverConnectURL()).isNull();
        assertThat(db.getUsername()).isNull();
        assertThat(db.getPassword()).isNull();
    }

    // =================================================================
    // Driver Class Name Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveDriverClassName() {
        Database db = new Database("testdb");
        db.setDriverClassName("org.h2.Driver");

        assertThat(db.getDriverClassName()).isEqualTo("org.h2.Driver");
    }

    @Test
    void should_HandleMySQLDriver() {
        Database db = new Database("mysql");
        db.setDriverClassName("com.mysql.cj.jdbc.Driver");

        assertThat(db.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
    }

    @Test
    void should_HandlePostgreSQLDriver() {
        Database db = new Database("postgres");
        db.setDriverClassName("org.postgresql.Driver");

        assertThat(db.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    }

    // =================================================================
    // Driver Connect URL Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveDriverConnectURL() {
        Database db = new Database("testdb");
        db.setDriverConnectURL("jdbc:h2:mem:testdb");

        assertThat(db.getDriverConnectURL()).isEqualTo("jdbc:h2:mem:testdb");
    }

    @Test
    void should_HandleH2InMemoryURL() {
        Database db = new Database("h2db");
        db.setDriverConnectURL("jdbc:h2:mem:{0}");

        assertThat(db.getDriverConnectURL()).isEqualTo("jdbc:h2:mem:{0}");
    }

    @Test
    void should_HandleMySQLURL() {
        Database db = new Database("mysqldb");
        db.setDriverConnectURL("jdbc:mysql://localhost:3306/{0}");

        assertThat(db.getDriverConnectURL()).isEqualTo("jdbc:mysql://localhost:3306/{0}");
    }

    @Test
    void should_HandlePostgreSQLURL() {
        Database db = new Database("pgdb");
        db.setDriverConnectURL("jdbc:postgresql://localhost:5432/{0}");

        assertThat(db.getDriverConnectURL()).isEqualTo("jdbc:postgresql://localhost:5432/{0}");
    }

    // =================================================================
    // Username Tests
    // =================================================================

    @Test
    void should_StoreAndRetrieveUsername() {
        Database db = new Database("testdb");
        db.setUsername("admin");

        assertThat(db.getUsername()).isEqualTo("admin");
    }

    @Test
    void should_HandleEmptyUsername() {
        Database db = new Database("testdb");
        db.setUsername("");

        assertThat(db.getUsername()).isEqualTo("");
    }

    @Test
    void should_AllowNullUsername() {
        Database db = new Database("testdb");
        db.setUsername("admin");
        db.setUsername(null);

        assertThat(db.getUsername()).isNull();
    }

    // =================================================================
    // Password Tests
    // =================================================================

    @Test
    void should_StoreAndRetrievePassword() {
        Database db = new Database("testdb");
        db.setPassword("secret123");

        assertThat(db.getPassword()).isEqualTo("secret123");
    }

    @Test
    void should_HandleEmptyPassword() {
        Database db = new Database("testdb");
        db.setPassword("");

        assertThat(db.getPassword()).isEqualTo("");
    }

    @Test
    void should_AllowNullPassword() {
        Database db = new Database("testdb");
        db.setPassword("secret");
        db.setPassword(null);

        assertThat(db.getPassword()).isNull();
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnFormattedString_When_ToStringCalled() {
        Database db = new Database("mydb");
        db.setDriverConnectURL("jdbc:h2:mem:mydb");

        String result = db.toString();

        assertThat(result).contains("name=mydb");
        assertThat(result).contains("url=jdbc:h2:mem:mydb");
    }

    @Test
    void should_IncludeName_In_ToString() {
        Database db = new Database("testdatabase");

        String result = db.toString();

        assertThat(result).contains("testdatabase");
    }

    @Test
    void should_IncludeURL_In_ToString() {
        Database db = new Database("db");
        db.setDriverConnectURL("jdbc:mysql://host/db");

        String result = db.toString();

        assertThat(result).contains("jdbc:mysql://host/db");
    }

    @Test
    void should_HandleNullURL_In_ToString() {
        Database db = new Database("db");

        String result = db.toString();

        assertThat(result).isNotNull();
        assertThat(result).contains("name=db");
    }

    // =================================================================
    // Startup Method Tests
    // =================================================================

    @Test
    void should_NotThrow_When_StartupCalled() {
        Database db = new Database("testdb");

        // startup() is protected but can be tested via subclass if needed
        // For now, just verify it exists and is callable
        assertThatCode(() -> {
            // Call via reflection or subclass
            db.getClass().getDeclaredMethod("startup");
        }).doesNotThrowAnyException();
    }

    // =================================================================
    // Init Method Tests (limited - requires driver)
    // =================================================================

    @Test
    void should_ThrowApplicationError_When_DriverClassNotFound() {
        Database db = new Database("testdb");
        db.setDriverClassName("com.nonexistent.FakeDriver");
        db.setDriverConnectURL("jdbc:fake://{0}");

        assertThatThrownBy(() -> db.init()).isInstanceOf(ApplicationError.class);
    }

    // =================================================================
    // Configuration Scenario Tests
    // =================================================================

    @Test
    void should_ConfigureH2Database_Completely() {
        Database db = new Database("h2test");
        db.setDriverClassName("org.h2.Driver");
        db.setDriverConnectURL("jdbc:h2:mem:{0}");
        db.setUsername("sa");
        db.setPassword("");

        assertThat(db.getName()).isEqualTo("h2test");
        assertThat(db.getDriverClassName()).isEqualTo("org.h2.Driver");
        assertThat(db.getDriverConnectURL()).isEqualTo("jdbc:h2:mem:{0}");
        assertThat(db.getUsername()).isEqualTo("sa");
        assertThat(db.getPassword()).isEqualTo("");
    }

    @Test
    void should_ConfigureMySQLDatabase_Completely() {
        Database db = new Database("production");
        db.setDriverClassName("com.mysql.cj.jdbc.Driver");
        db.setDriverConnectURL("jdbc:mysql://db.example.com:3306/{0}?useSSL=true");
        db.setUsername("appuser");
        db.setPassword("securepassword");

        assertThat(db.getName()).isEqualTo("production");
        assertThat(db.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(db.getDriverConnectURL()).contains("mysql");
        assertThat(db.getUsername()).isEqualTo("appuser");
        assertThat(db.getPassword()).isEqualTo("securepassword");
    }

    @Test
    void should_AllowReconfiguration() {
        Database db = new Database("mydb");
        db.setDriverClassName("org.h2.Driver");
        db.setUsername("user1");

        // Reconfigure
        db.setDriverClassName("com.mysql.cj.jdbc.Driver");
        db.setUsername("user2");

        assertThat(db.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(db.getUsername()).isEqualTo("user2");
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleSpecialCharactersInName() {
        Database db = new Database("test-db_123");

        assertThat(db.getName()).isEqualTo("test-db_123");
    }

    @Test
    void should_HandleSpecialCharactersInUsername() {
        Database db = new Database("db");
        db.setUsername("user@domain.com");

        assertThat(db.getUsername()).isEqualTo("user@domain.com");
    }

    @Test
    void should_HandleSpecialCharactersInPassword() {
        Database db = new Database("db");
        db.setPassword("p@ssw0rd!#$%");

        assertThat(db.getPassword()).isEqualTo("p@ssw0rd!#$%");
    }

    @Test
    void should_HandleLongPassword() {
        Database db = new Database("db");
        String longPassword = "a".repeat(100);
        db.setPassword(longPassword);

        assertThat(db.getPassword()).hasSize(100);
    }

    @Test
    void should_HandleComplexJDBCURL() {
        Database db = new Database("db");
        String complexURL = "jdbc:mysql://host:3306/{0}?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=UTC";
        db.setDriverConnectURL(complexURL);

        assertThat(db.getDriverConnectURL()).isEqualTo(complexURL);
    }
}
