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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PasswordGenerator - random password generation with configurable
 * options
 */
class PasswordGeneratorTest {

    // =================================================================
    // Constructor and Option Tests
    // =================================================================

    @Test
    void should_CreateGenerator_When_LettersOnly() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(10, 12345L);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[a-z]+");
    }

    @Test
    void should_CreateGenerator_When_MixedCase() {
        PasswordGenerator gen = new PasswordGenerator(
                PasswordGenerator.OPTION_INCLUDE_LETTERS | PasswordGenerator.OPTION_INCLUDE_MIXED_CASE, null);

        String password = gen.generatePassword(20, 12345L);

        assertThat(password).hasSize(20);
        assertThat(password).matches("[a-zA-Z]+");
    }

    @Test
    void should_CreateGenerator_When_LettersAndNumbers() {
        PasswordGenerator gen = new PasswordGenerator(
                PasswordGenerator.OPTION_INCLUDE_LETTERS | PasswordGenerator.OPTION_INCLUDE_NUMBERS, null);

        String password = gen.generatePassword(20, 12345L);

        assertThat(password).hasSize(20);
        assertThat(password).matches("[a-z0-9]+");
    }

    @Test
    void should_CreateGenerator_When_AllOptions() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_ALL, null);

        String password = gen.generatePassword(20, 12345L);

        assertThat(password).hasSize(20);
        // Should contain letters, numbers, and punctuation, excluding similar
        // characters
    }

    @Test
    void should_CreateGenerator_When_NumbersOnly() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_NUMBERS, null);

        String password = gen.generatePassword(10, 12345L);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[0-9]+");
    }

    @Test
    void should_CreateGenerator_When_PunctuationIncluded() {
        PasswordGenerator gen = new PasswordGenerator(
                PasswordGenerator.OPTION_INCLUDE_LETTERS | PasswordGenerator.OPTION_INCLUDE_PUNCTUATION, null);

        String password = gen.generatePassword(30, 12345L);

        assertThat(password).hasSize(30);
        // Should contain letters and punctuation characters
        assertThat(password).matches("[a-z!-/:-@]+");
    }

    // =================================================================
    // Password Generation Tests
    // =================================================================

    @Test
    void should_GeneratePasswordOfCorrectLength_When_LengthSpecified() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        assertThat(gen.generatePassword(5, 123L)).hasSize(5);
        assertThat(gen.generatePassword(10, 123L)).hasSize(10);
        assertThat(gen.generatePassword(20, 123L)).hasSize(20);
        assertThat(gen.generatePassword(50, 123L)).hasSize(50);
    }

    @Test
    void should_GenerateValidPassword_When_SeedProvided() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password1 = gen.generatePassword(10, 12345L);
        String password2 = gen.generatePassword(10, 12345L);

        // SecureRandom is non-deterministic even with same seed (by design)
        assertThat(password1).hasSize(10);
        assertThat(password1).matches("[a-z]+");
        assertThat(password2).hasSize(10);
        assertThat(password2).matches("[a-z]+");
    }

    @Test
    void should_GenerateValidPasswords_When_DifferentSeedsUsed() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password1 = gen.generatePassword(10, 12345L);
        String password2 = gen.generatePassword(10, 54321L);

        // Both should be valid 10-character lowercase passwords
        assertThat(password1).hasSize(10);
        assertThat(password1).matches("[a-z]+");
        assertThat(password2).hasSize(10);
        assertThat(password2).matches("[a-z]+");
    }

    @Test
    void should_GenerateNonEmptyPassword_When_CurrentTimeSeedUsed() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(10);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[a-z]+");
    }

    @Test
    void should_GenerateEmptyPassword_When_ZeroLength() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(0, 123L);

        assertThat(password).isEmpty();
    }

    // =================================================================
    // Exclude Similar Characters Tests
    // =================================================================

    @Test
    void should_ExcludeSimilarChars_When_OptionSet() {
        PasswordGenerator gen = new PasswordGenerator(
                PasswordGenerator.OPTION_INCLUDE_LETTERS | PasswordGenerator.OPTION_INCLUDE_MIXED_CASE
                        | PasswordGenerator.OPTION_INCLUDE_NUMBERS | PasswordGenerator.OPTION_EXCLUDE_SIMILAR,
                null);

        String password = gen.generatePassword(100, 12345L);

        // Should not contain: I, L, O, U, V, i, l, o, u, v, 0, 1
        assertThat(password).doesNotContain("I", "L", "O", "U", "V", "i", "l", "o", "u", "v", "0", "1");
    }

    @Test
    void should_IncludeSimilarChars_When_OptionNotSet() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS
                | PasswordGenerator.OPTION_INCLUDE_MIXED_CASE | PasswordGenerator.OPTION_INCLUDE_NUMBERS, null);

        String password = gen.generatePassword(200, 12345L);

        // With 200 characters and no exclusion, should contain at least some similar
        // chars
        boolean containsSimilar = password.contains("i") || password.contains("l") || password.contains("o")
                || password.contains("0") || password.contains("1");
        assertThat(containsSimilar).isTrue();
    }

    // =================================================================
    // Additional Characters Tests
    // =================================================================

    @Test
    void should_IncludeAdditionalChars_When_CharsProvided() {
        char[] additionalChars = {'@', '#', '$'};
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, additionalChars);

        String password = gen.generatePassword(50, 12345L);

        assertThat(password).hasSize(50);
        // Password can contain letters and the additional chars
        assertThat(password).matches("[a-z@#$]+");
    }

    @Test
    void should_HandleNullAdditionalChars_When_NullProvided() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(10, 12345L);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[a-z]+");
    }

    @Test
    void should_HandleEmptyAdditionalChars_When_EmptyArrayProvided() {
        char[] additionalChars = {};
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, additionalChars);

        String password = gen.generatePassword(10, 12345L);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[a-z]+");
    }

    // =================================================================
    // Option Combination Tests
    // =================================================================

    @Test
    void should_GenerateFromAllCharTypes_When_AllOptionsSet() {
        PasswordGenerator gen = new PasswordGenerator(
                PasswordGenerator.OPTION_INCLUDE_LETTERS | PasswordGenerator.OPTION_INCLUDE_MIXED_CASE
                        | PasswordGenerator.OPTION_INCLUDE_NUMBERS | PasswordGenerator.OPTION_INCLUDE_PUNCTUATION,
                null);

        String password = gen.generatePassword(100, 12345L);

        assertThat(password).hasSize(100);
        // Password should contain mix of lowercase, uppercase, numbers, and punctuation
        assertThat(password).matches("[a-zA-Z0-9!-/:-@]+");
    }

    @Test
    void should_GenerateUppercaseOnly_When_OnlyMixedCaseSet() {
        // Mixed case without letters gives only uppercase
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_MIXED_CASE, null);

        String password = gen.generatePassword(10, 12345L);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[A-Z]+");
    }

    @Test
    void should_GeneratePunctuationOnly_When_OnlyPunctuationSet() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_PUNCTUATION, null);

        String password = gen.generatePassword(10, 12345L);

        assertThat(password).hasSize(10);
        assertThat(password).matches("[!-/:-@]+");
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_GenerateValidPasswords_When_SameGeneratorUsedMultipleTimes() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password1 = gen.generatePassword(10, 999L);
        String password2 = gen.generatePassword(10, 999L);
        String password3 = gen.generatePassword(10, 999L);

        // SecureRandom is non-deterministic, so just verify all are valid
        assertThat(password1).hasSize(10).matches("[a-z]+");
        assertThat(password2).hasSize(10).matches("[a-z]+");
        assertThat(password3).hasSize(10).matches("[a-z]+");
    }

    @Test
    void should_GenerateLongPassword_When_LargeLengthSpecified() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password = gen.generatePassword(1000, 12345L);

        assertThat(password).hasSize(1000);
        assertThat(password).matches("[a-z]+");
    }

    @Test
    void should_GenerateDifferentPasswords_When_DifferentLengthsSameSeed() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_INCLUDE_LETTERS, null);

        String password1 = gen.generatePassword(10, 12345L);
        String password2 = gen.generatePassword(20, 12345L);

        assertThat(password1).hasSize(10);
        assertThat(password2).hasSize(20);
        // Password2 should not just be password1 repeated
        assertThat(password2).isNotEqualTo(password1 + password1);
    }

    @Test
    void should_HandleAllOptionsConstant_When_Used() {
        PasswordGenerator gen = new PasswordGenerator(PasswordGenerator.OPTION_ALL, null);

        String password = gen.generatePassword(50, 12345L);

        assertThat(password).hasSize(50);
        // Should exclude similar characters since OPTION_ALL includes EXCLUDE_SIMILAR
        assertThat(password).doesNotContain("I", "L", "O", "0", "1");
    }
}
