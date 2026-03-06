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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Hide - obfuscation/deobfuscation of save data strings.
 */
class HideTest {

    // ========== Round-trip Tests ==========

    @Test
    void should_RestoreOriginal_When_ObfuscateThenDeobfuscate_WithLetters() {
        String original = "HelloWorld";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 5);
        assertThat(sb.toString()).isNotEqualTo(original);

        Hide.deobfuscate(sb, 5);
        assertThat(sb.toString()).isEqualTo(original);
    }

    @Test
    void should_RestoreOriginal_When_ObfuscateThenDeobfuscate_WithDigits() {
        String original = "1234567890";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 3);
        assertThat(sb.toString()).isNotEqualTo(original);

        Hide.deobfuscate(sb, 3);
        assertThat(sb.toString()).isEqualTo(original);
    }

    @Test
    void should_RestoreOriginal_When_ObfuscateThenDeobfuscate_WithMixed() {
        String original = "Player1Score99zZ";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 7);
        Hide.deobfuscate(sb, 7);

        assertThat(sb.toString()).isEqualTo(original);
    }

    // ========== Entry Number Variation Tests ==========

    @Test
    void should_ProduceDifferentResults_When_DifferentEntryNumbers() {
        String original = "TestData123";

        StringBuilder sb1 = new StringBuilder(original);
        Hide.obfuscate(sb1, 2);

        StringBuilder sb2 = new StringBuilder(original);
        Hide.obfuscate(sb2, 5);

        assertThat(sb1.toString()).isNotEqualTo(sb2.toString());
    }

    @Test
    void should_NotObfuscate_When_EntryIsZero() {
        // nEntry=0 means rotate = 0 * (i+127) = 0 for all positions,
        // and the code only forces rotate=7 when nEntry > 0
        String original = "abcXYZ123";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 0);

        assertThat(sb.toString()).isEqualTo(original);
    }

    // ========== Character Type Tests ==========

    @Test
    void should_OnlyRotateDigits_When_InputIsDigitsOnly() {
        String original = "0123456789";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 2);

        // All characters should still be digits
        for (int i = 0; i < sb.length(); i++) {
            assertThat(sb.charAt(i)).isBetween('0', '9');
        }

        Hide.deobfuscate(sb, 2);
        assertThat(sb.toString()).isEqualTo(original);
    }

    @Test
    void should_OnlyRotateLowercase_When_InputIsLowercaseOnly() {
        String original = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 4);

        for (int i = 0; i < sb.length(); i++) {
            assertThat(sb.charAt(i)).isBetween('a', 'z');
        }

        Hide.deobfuscate(sb, 4);
        assertThat(sb.toString()).isEqualTo(original);
    }

    @Test
    void should_OnlyRotateUppercase_When_InputIsUppercaseOnly() {
        String original = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 6);

        for (int i = 0; i < sb.length(); i++) {
            assertThat(sb.charAt(i)).isBetween('A', 'Z');
        }

        Hide.deobfuscate(sb, 6);
        assertThat(sb.toString()).isEqualTo(original);
    }

    @Test
    void should_PassThroughSpecialChars_When_InputContainsSpecialChars() {
        String original = "a=b,c:d;e!f@g#h";
        StringBuilder sb = new StringBuilder(original);

        Hide.obfuscate(sb, 3);

        // Special characters should remain unchanged
        assertThat(sb.charAt(1)).isEqualTo('=');
        assertThat(sb.charAt(3)).isEqualTo(',');
        assertThat(sb.charAt(5)).isEqualTo(':');
        assertThat(sb.charAt(7)).isEqualTo(';');
        assertThat(sb.charAt(9)).isEqualTo('!');
        assertThat(sb.charAt(11)).isEqualTo('@');
        assertThat(sb.charAt(13)).isEqualTo('#');

        // Round-trip should still work
        Hide.deobfuscate(sb, 3);
        assertThat(sb.toString()).isEqualTo(original);
    }

    // ========== Empty String Tests ==========

    @Test
    void should_HandleEmptyString_When_Obfuscating() {
        StringBuilder sb = new StringBuilder("");

        Hide.obfuscate(sb, 1);

        assertThat(sb.toString()).isEmpty();
    }

    @Test
    void should_HandleEmptyString_When_Deobfuscating() {
        StringBuilder sb = new StringBuilder("");

        Hide.deobfuscate(sb, 1);

        assertThat(sb.toString()).isEmpty();
    }
}
