/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for InputValidator utility class
 */
class InputValidatorTest {
    // ========================================
    // Email Validation Tests
    // ========================================

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "test.user@example.com", "user+tag@example.co.uk",
            "user_name@example.com", "user123@test-domain.org", "a@b.c"})
    void validEmails_ShouldPass(String email) {
        assertThat(InputValidator.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-an-email", "@example.com", "user@", "user @example.com",
            "user@example .com", "user@@example.com", "user@.com", "user@example.", "user@", "@", "user@example@com"})
    void invalidEmails_ShouldFail(String email) {
        assertThat(InputValidator.isValidEmail(email)).isFalse();
    }

    @Test
    void nullEmail_ShouldFail() {
        assertThat(InputValidator.isValidEmail(null)).isFalse();
    }

    @Test
    void emailTooLong_ShouldFail() {
        // RFC 5321 max length is 254 characters
        String longEmail = "a".repeat(250) + "@example.com";
        assertThat(InputValidator.isValidEmail(longEmail)).isFalse();
    }

    // ========================================
    // String Length Validation Tests
    // ========================================

    @Test
    void validStringLength_WithinBounds_ShouldPass() {
        assertThat(InputValidator.isValidLength("hello", 1, 10)).isTrue();
        assertThat(InputValidator.isValidLength("a", 1, 1)).isTrue();
        assertThat(InputValidator.isValidLength("test", 4, 4)).isTrue();
    }

    @Test
    void stringTooShort_ShouldFail() {
        assertThat(InputValidator.isValidLength("", 1, 10)).isFalse();
        assertThat(InputValidator.isValidLength("ab", 3, 10)).isFalse();
    }

    @Test
    void stringTooLong_ShouldFail() {
        assertThat(InputValidator.isValidLength("hello world", 1, 5)).isFalse();
        assertThat(InputValidator.isValidLength("test", 1, 3)).isFalse();
    }

    @Test
    void nullString_ShouldFail() {
        assertThat(InputValidator.isValidLength(null, 1, 10)).isFalse();
    }

    @Test
    void whitespaceOnlyString_ShouldFailWhenTrimmed() {
        // Strings should be trimmed before length check
        assertThat(InputValidator.isValidLength("   ", 1, 10)).isFalse();
    }

    // ========================================
    // Integer Bounds Validation Tests
    // ========================================

    @Test
    void validInteger_WithinBounds_ShouldPass() {
        assertThat(InputValidator.isValidInt(5, 1, 10)).isTrue();
        assertThat(InputValidator.isValidInt(1, 1, 10)).isTrue();
        assertThat(InputValidator.isValidInt(10, 1, 10)).isTrue();
        assertThat(InputValidator.isValidInt(0, 0, 0)).isTrue();
    }

    @Test
    void integerTooSmall_ShouldFail() {
        assertThat(InputValidator.isValidInt(0, 1, 10)).isFalse();
        assertThat(InputValidator.isValidInt(-1, 0, 10)).isFalse();
    }

    @Test
    void integerTooLarge_ShouldFail() {
        assertThat(InputValidator.isValidInt(11, 1, 10)).isFalse();
        assertThat(InputValidator.isValidInt(100, 1, 10)).isFalse();
    }

    // ========================================
    // Combined Validation Tests
    // ========================================

    @Test
    void profileName_StandardRules_ShouldValidate() {
        // Profile names: 1-50 characters
        assertThat(InputValidator.isValidLength("JohnDoe", 1, 50)).isTrue();
        assertThat(InputValidator.isValidLength("", 1, 50)).isFalse();
        assertThat(InputValidator.isValidLength("a".repeat(51), 1, 50)).isFalse();
    }

    @Test
    void gameName_StandardRules_ShouldValidate() {
        // Game names: 1-100 characters
        assertThat(InputValidator.isValidLength("Friday Night Poker", 1, 100)).isTrue();
        assertThat(InputValidator.isValidLength("", 1, 100)).isFalse();
        assertThat(InputValidator.isValidLength("a".repeat(101), 1, 100)).isFalse();
    }

    @Test
    void chatMessage_StandardRules_ShouldValidate() {
        // Chat messages: 1-500 characters
        assertThat(InputValidator.isValidLength("Hello!", 1, 500)).isTrue();
        assertThat(InputValidator.isValidLength("", 1, 500)).isFalse();
        assertThat(InputValidator.isValidLength("a".repeat(501), 1, 500)).isFalse();
    }
}
