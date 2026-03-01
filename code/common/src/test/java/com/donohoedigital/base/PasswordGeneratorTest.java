/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PasswordGenerator - random password generation with configurable
 * character pools.
 */
class PasswordGeneratorTest {

    @Test
    void should_ReturnPasswordOfExactLength_When_LengthSpecified() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(12);

        assertThat(password).hasSize(12);
    }

    @Test
    void should_ReturnPasswordOfMinimumLength_When_LengthIsOne() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(1);

        assertThat(password).hasSize(1);
    }

    @Test
    void should_ReturnPasswordOfMaximumLength_When_LengthIsLarge() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_ALL, null);

        String password = gen.generatePassword(100);

        assertThat(password).hasSize(100);
    }

    @Test
    void should_ContainOnlyLowercaseLetters_When_OnlyLettersOptionSet() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(50, 42L);

        assertThat(password).matches("[a-z]+");
    }

    @Test
    void should_ContainOnlyUppercaseLetters_When_OnlyMixedCaseOptionSet() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_MIXED_CASE, null);

        String password = gen.generatePassword(50, 42L);

        assertThat(password).matches("[A-Z]+");
    }

    @Test
    void should_ContainOnlyDigits_When_OnlyNumbersOptionSet() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_NUMBERS, null);

        String password = gen.generatePassword(50, 42L);

        assertThat(password).matches("[0-9]+");
    }

    @Test
    void should_ExcludeSimilarChars_When_ExcludeSimilarOptionSet() {
        int options = PasswordGenerator.OPTION_INCLUDE_LETTERS | PasswordGenerator.OPTION_INCLUDE_MIXED_CASE
                | PasswordGenerator.OPTION_INCLUDE_NUMBERS | PasswordGenerator.OPTION_EXCLUDE_SIMILAR;
        PasswordGenerator gen = new PasswordGenerator(options, null);

        // Generate a large password to increase the chance of hitting excluded chars if
        // they were present
        String password = gen.generatePassword(200, 99L);

        assertThat(password).doesNotContainAnyWhitespaces();
        // None of the visually similar / excluded characters should appear
        for (char excluded : "ILOUVilouv01".toCharArray()) {
            assertThat(password).doesNotContain(String.valueOf(excluded));
        }
    }

    @Test
    void should_ProduceDifferentPasswords_When_CalledMultipleTimes() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_ALL, null);

        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            passwords.add(gen.generatePassword(16));
        }

        // With 10 independent calls it is astronomically unlikely to get all identical
        // results
        assertThat(passwords).hasSizeGreaterThan(1);
    }

    @Test
    void should_ReturnCorrectLengthWithSeed_When_SeededGeneratePasswordCalled() {
        // Verifies the seeded overload still produces a password of the requested
        // length.
        // SecureRandom does not guarantee reproducibility across calls when seeded, so
        // we only assert length and character-set membership here.
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(20, 123456789L);

        assertThat(password).hasSize(20);
        assertThat(password).matches("[a-z]+");
    }
}
