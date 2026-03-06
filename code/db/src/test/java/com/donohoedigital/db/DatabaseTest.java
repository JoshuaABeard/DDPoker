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

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Database - represents a logical database with connection
 * properties. Tests focus on constructor, getter/setter behavior, and toString
 * without requiring actual JDBC driver loading.
 */
class DatabaseTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_SetName_When_Constructed() {
        Database db = new Database("testdb");

        assertThat(db.getName()).isEqualTo("testdb");
    }

    @Test
    void should_AcceptNullName_When_Constructed() {
        Database db = new Database(null);

        assertThat(db.getName()).isNull();
    }

    // =================================================================
    // DriverClassName Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_DriverClassNameNotSet() {
        Database db = new Database("testdb");

        assertThat(db.getDriverClassName()).isNull();
    }

    @Test
    void should_ReturnDriverClassName_When_Set() {
        Database db = new Database("testdb");

        db.setDriverClassName("org.postgresql.Driver");

        assertThat(db.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    }

    // =================================================================
    // DriverConnectURL Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_DriverConnectURLNotSet() {
        Database db = new Database("testdb");

        assertThat(db.getDriverConnectURL()).isNull();
    }

    @Test
    void should_ReturnDriverConnectURL_When_Set() {
        Database db = new Database("testdb");

        db.setDriverConnectURL("jdbc:postgresql://localhost:5432/{0}");

        assertThat(db.getDriverConnectURL()).isEqualTo("jdbc:postgresql://localhost:5432/{0}");
    }

    // =================================================================
    // Username Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_UsernameNotSet() {
        Database db = new Database("testdb");

        assertThat(db.getUsername()).isNull();
    }

    @Test
    void should_ReturnUsername_When_Set() {
        Database db = new Database("testdb");

        db.setUsername("admin");

        assertThat(db.getUsername()).isEqualTo("admin");
    }

    // =================================================================
    // Password Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_PasswordNotSet() {
        Database db = new Database("testdb");

        assertThat(db.getPassword()).isNull();
    }

    @Test
    void should_ReturnPassword_When_Set() {
        Database db = new Database("testdb");

        db.setPassword("secret");

        assertThat(db.getPassword()).isEqualTo("secret");
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_IncludeNameAndUrl_When_ToStringCalled() {
        Database db = new Database("mydb");
        db.setDriverConnectURL("jdbc:mysql://localhost/mydb");

        String result = db.toString();

        assertThat(result).contains("name=mydb");
        assertThat(result).contains("url=jdbc:mysql://localhost/mydb");
    }

    @Test
    void should_HandleNullUrl_When_ToStringCalled() {
        Database db = new Database("mydb");

        String result = db.toString();

        assertThat(result).contains("name=mydb");
        assertThat(result).contains("url=null");
    }

    // =================================================================
    // init Tests
    // =================================================================

    @Test
    void should_ThrowApplicationError_When_InitCalledWithInvalidDriver() {
        Database db = new Database("testdb");
        db.setDriverClassName("com.nonexistent.Driver");
        db.setDriverConnectURL("jdbc:fake://localhost/{0}");
        db.setUsername("user");
        db.setPassword("pass");

        assertThatThrownBy(db::init).isInstanceOf(com.donohoedigital.base.ApplicationError.class);
    }

    // =================================================================
    // getConnection Tests
    // =================================================================

    @Test
    void should_ThrowApplicationError_When_GetConnectionCalledWithoutInit() {
        Database db = new Database("testdb");
        // driverFormattedURL_ is null since init() was not called
        assertThatThrownBy(db::getConnection).isInstanceOf(com.donohoedigital.base.ApplicationError.class);
    }
}
