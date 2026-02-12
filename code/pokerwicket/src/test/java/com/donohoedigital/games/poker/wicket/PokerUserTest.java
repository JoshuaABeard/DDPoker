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
package com.donohoedigital.games.poker.wicket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerUser admin functionality using PropertyConfig.
 */
class PokerUserTest {
    @AfterEach
    void cleanup() {
        // Clear any system properties set during tests
        System.clearProperty("settings.admin.user");
    }

    @Test
    void should_ReturnTrue_When_NameMatchesAdminUser() {
        // Set admin user via system property (simulates environment variable)
        System.setProperty("settings.admin.user", "testadmin");

        // Create user with matching name
        PokerUser user = new PokerUser(1L, "testadmin", "key123", "admin@test.com", false);

        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NameDoesNotMatchAdminUser() {
        // Set admin user via system property
        System.setProperty("settings.admin.user", "testadmin");

        // Create user with different name
        PokerUser user = new PokerUser(1L, "regularuser", "key123", "user@test.com", false);

        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_AdminUserNotSet() {
        // Ensure no admin user is set
        System.clearProperty("settings.admin.user");

        // Create user
        PokerUser user = new PokerUser(1L, "someuser", "key123", "user@test.com", false);

        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void should_BeCaseSensitive_When_CheckingAdminUser() {
        // Set admin user via system property
        System.setProperty("settings.admin.user", "TestAdmin");

        // Create user with different case
        PokerUser user = new PokerUser(1L, "testadmin", "key123", "admin@test.com", false);

        assertThat(user.isAdmin()).isFalse();
    }
}
