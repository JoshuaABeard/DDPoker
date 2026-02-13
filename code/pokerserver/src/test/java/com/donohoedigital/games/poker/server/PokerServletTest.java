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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.base.InputValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TEST-1: Basic unit tests for PokerServlet security-critical validation logic.
 * Focuses on input validation utilities that were added in P2 fixes
 * (VALIDATION-1).
 *
 * Note: Full servlet testing with Spring context would require extensive setup.
 * These tests verify the validation logic that the servlet uses.
 */
class PokerServletTest {

    // ========================================
    // Input Validation Tests (VALIDATION-1)
    // ========================================

    @Test
    void should_ValidateProfileNames_Correctly() {
        // Valid profile names (1-50 chars)
        assertThat(InputValidator.isValidProfileName("")).isFalse();
        assertThat(InputValidator.isValidProfileName("a")).isTrue();
        assertThat(InputValidator.isValidProfileName("ValidUser123")).isTrue();
        assertThat(InputValidator.isValidProfileName("a".repeat(50))).isTrue();
        assertThat(InputValidator.isValidProfileName("a".repeat(51))).isFalse();
    }

    @Test
    void should_ValidateEmails_Correctly() {
        // Valid emails
        assertThat(InputValidator.isValidEmail("test@example.com")).isTrue();
        assertThat(InputValidator.isValidEmail("user+tag@domain.co.uk")).isTrue();

        // Invalid emails
        assertThat(InputValidator.isValidEmail("")).isFalse();
        assertThat(InputValidator.isValidEmail("not-an-email")).isFalse();
        assertThat(InputValidator.isValidEmail("@example.com")).isFalse();
        assertThat(InputValidator.isValidEmail("user@")).isFalse();
        assertThat(InputValidator.isValidEmail("user @example.com")).isFalse();
    }

    @Test
    void should_ValidatePasswords_Correctly() {
        // Passwords must be 8-128 characters (VALIDATION-1)
        assertThat(InputValidator.isValidPassword("")).isFalse();
        assertThat(InputValidator.isValidPassword("short")).isFalse();
        assertThat(InputValidator.isValidPassword("1234567")).isFalse();
        assertThat(InputValidator.isValidPassword("12345678")).isTrue();
        assertThat(InputValidator.isValidPassword("a".repeat(128))).isTrue();
        assertThat(InputValidator.isValidPassword("a".repeat(129))).isFalse();
    }

    @Test
    void should_ValidateChatMessages_Correctly() {
        // Chat messages must be 1-500 characters (SEC-BACKEND-6)
        assertThat(InputValidator.isValidChatMessage("")).isFalse();
        assertThat(InputValidator.isValidChatMessage("Hello")).isTrue();
        assertThat(InputValidator.isValidChatMessage("a".repeat(500))).isTrue();
        assertThat(InputValidator.isValidChatMessage("a".repeat(501))).isFalse();
    }

    @Test
    void should_ValidateGameNames_Correctly() {
        // Game names must be 1-100 characters
        assertThat(InputValidator.isValidGameName("")).isFalse();
        assertThat(InputValidator.isValidGameName("MyGame")).isTrue();
        assertThat(InputValidator.isValidGameName("a".repeat(100))).isTrue();
        assertThat(InputValidator.isValidGameName("a".repeat(101))).isFalse();
    }

    @Test
    void should_ValidateStringLengths_Correctly() {
        // Test the underlying length validation
        assertThat(InputValidator.isValidLength("test", 1, 10)).isTrue();
        assertThat(InputValidator.isValidLength("", 1, 10)).isFalse();
        assertThat(InputValidator.isValidLength("  test  ", 1, 10)).isTrue(); // Trimmed
        assertThat(InputValidator.isValidLength("toolongstring", 1, 10)).isFalse();
        assertThat(InputValidator.isValidLength(null, 1, 10)).isFalse();
    }

    @Test
    void should_ValidateIntegerBounds_Correctly() {
        // Test integer validation
        assertThat(InputValidator.isValidInt(5, 1, 10)).isTrue();
        assertThat(InputValidator.isValidInt(1, 1, 10)).isTrue();
        assertThat(InputValidator.isValidInt(10, 1, 10)).isTrue();
        assertThat(InputValidator.isValidInt(0, 1, 10)).isFalse();
        assertThat(InputValidator.isValidInt(11, 1, 10)).isFalse();
    }
}
