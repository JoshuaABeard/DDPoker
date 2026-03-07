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

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Format — a printf-style number/string formatter plus C-like
 * atoi/atof.
 */
class FormatTest {

    // -----------------------------------------------------------------------
    // Integer formatting (%d / %i)
    // -----------------------------------------------------------------------

    @Test
    void should_FormatInteger_When_SimpleDecimalFormatUsed() {
        assertThat(new Format("%d").form(42L)).isEqualTo("42");
    }

    @Test
    void should_FormatNegativeInteger_When_NegativeValueGiven() {
        assertThat(new Format("%d").form(-99L)).isEqualTo("-99");
    }

    @Test
    void should_PadWithLeadingZeros_When_ZeroModifierAndWidthGiven() {
        assertThat(new Format("%05d").form(42L)).isEqualTo("00042");
    }

    @Test
    void should_RightAlignInField_When_WidthGivenWithoutLeftAlign() {
        assertThat(new Format("%8d").form(42L)).isEqualTo("      42");
    }

    @Test
    void should_LeftAlignInField_When_MinusModifierGiven() {
        assertThat(new Format("%-8d").form(42L)).isEqualTo("42      ");
    }

    @Test
    void should_ShowPlusSign_When_PlusModifierGiven() {
        assertThat(new Format("%+d").form(42L)).isEqualTo("+42");
    }

    // -----------------------------------------------------------------------
    // Float formatting (%f)
    // -----------------------------------------------------------------------

    @Test
    void should_FormatFloat_When_FixedFormatUsed() {
        // Default precision is 6
        assertThat(new Format("%f").form(1.5)).isEqualTo("1.500000");
    }

    @Test
    void should_RoundToTwoDecimalPlaces_When_PrecisionTwoGiven() {
        assertThat(new Format("%.2f").form(3.14159)).isEqualTo("3.14");
    }

    @Test
    void should_FormatNegativeFloat_When_NegativeValueGiven() {
        assertThat(new Format("%.2f").form(-2.5)).isEqualTo("-2.50");
    }

    // -----------------------------------------------------------------------
    // Exponential formatting (%e)
    // -----------------------------------------------------------------------

    @Test
    void should_FormatInScientificNotation_When_EFormatUsed() {
        String result = new Format("%e").form(12345.6789);
        // Expected: 1.234568e+004
        assertThat(result).startsWith("1.23456");
        assertThat(result).contains("e+");
    }

    // -----------------------------------------------------------------------
    // General formatting (%g)
    // -----------------------------------------------------------------------

    @Test
    void should_UseFixedFormatForSmallNumbers_When_GFormatUsed() {
        // 1.5 → small number, general format should use fixed
        String result = new Format("%g").form(1.5);
        assertThat(result).isEqualTo("1.5");
    }

    // -----------------------------------------------------------------------
    // String formatting (%s)
    // -----------------------------------------------------------------------

    @Test
    void should_FormatString_When_StringFormatUsed() {
        assertThat(new Format("%s").form("hello")).isEqualTo("hello");
    }

    @Test
    void should_TruncateString_When_PrecisionShorterThanString() {
        assertThat(new Format("%.3s").form("hello")).isEqualTo("hel");
    }

    @Test
    void should_PadString_When_WidthLargerThanString() {
        assertThat(new Format("%8s").form("hi")).isEqualTo("      hi");
    }

    // -----------------------------------------------------------------------
    // Hexadecimal formatting (%x / %X)
    // -----------------------------------------------------------------------

    @Test
    void should_FormatHex_When_LowercaseXFormatUsed() {
        assertThat(new Format("%x").form(255L)).isEqualTo("ff");
    }

    @Test
    void should_FormatHex_When_UppercaseXFormatUsed() {
        assertThat(new Format("%X").form(255L)).isEqualTo("FF");
    }

    // -----------------------------------------------------------------------
    // atoi()
    // -----------------------------------------------------------------------

    @Test
    void should_ConvertDecimalString_When_AtoiCalledWithPlainNumber() {
        assertThat(Format.atoi("42")).isEqualTo(42);
    }

    @Test
    void should_ConvertNegativeString_When_AtoiCalledWithMinus() {
        assertThat(Format.atoi("-7")).isEqualTo(-7);
    }

    @Test
    void should_StopAtNonNumericCharacter_When_AtoiCalledWithTrailingText() {
        assertThat(Format.atoi("123abc")).isEqualTo(123);
    }

    @Test
    void should_ParseHexString_When_AtoiCalledWith0xPrefix() {
        assertThat(Format.atoi("0xFF")).isEqualTo(255);
    }

    @Test
    void should_ReturnZero_When_AtoiCalledWithNonNumericString() {
        assertThat(Format.atoi("abc")).isEqualTo(0);
    }

    @Test
    void should_ThrowException_When_AtoiCalledWithNullInput() {
        assertThatThrownBy(() -> Format.atoi(null)).isInstanceOf(NullPointerException.class);
    }

    // -----------------------------------------------------------------------
    // atof()
    // -----------------------------------------------------------------------

    @Test
    void should_ConvertFloatString_When_AtofCalledWithDecimal() {
        assertThat(Format.atof("3.14")).isCloseTo(3.14, within(0.0001));
    }

    @Test
    void should_ConvertNegativeFloat_When_AtofCalledWithMinusSign() {
        assertThat(Format.atof("-1.5")).isCloseTo(-1.5, within(0.0001));
    }

    @Test
    void should_ParseScientificNotation_When_AtofCalledWithENotation() {
        assertThat(Format.atof("1.0e3")).isCloseTo(1000.0, within(0.01));
    }

    @Test
    void should_ReturnZero_When_AtofCalledWithEmptyContent() {
        // A string with no numeric prefix should yield 0.0
        assertThat(Format.atof("xyz")).isCloseTo(0.0, within(0.0001));
    }

    // -----------------------------------------------------------------------
    // atol()
    // -----------------------------------------------------------------------

    @Test
    void should_ConvertDecimalString_When_AtolCalled() {
        assertThat(Format.atol("12345678901")).isEqualTo(12345678901L);
    }

    @Test
    void should_ConvertNegative_When_AtolCalledWithMinusSign() {
        assertThat(Format.atol("-999")).isEqualTo(-999L);
    }

    @Test
    void should_ConvertHex_When_AtolCalledWith0xPrefix() {
        assertThat(Format.atol("0xFF")).isEqualTo(255L);
    }

    @Test
    void should_ConvertOctal_When_AtolCalledWithLeadingZero() {
        assertThat(Format.atol("017")).isEqualTo(15L);
    }

    // -----------------------------------------------------------------------
    // form() with prefix/suffix text
    // -----------------------------------------------------------------------

    @Test
    void should_IncludePrefixAndSuffix_When_FormatHasSurroundingText() {
        Format f = new Format("value: %d cents");
        assertThat(f.form(42)).isEqualTo("value: 42 cents");
    }

    @Test
    void should_HandlePercentEscape_When_FormatHasDoublePercent() {
        // Prefix %% is unescaped to %; suffix %% is kept as-is
        Format f = new Format("%%done: %d%%");
        assertThat(f.form(50)).isEqualTo("%done: 50%%");
    }

    // -----------------------------------------------------------------------
    // form(long) - octal
    // -----------------------------------------------------------------------

    @Test
    void should_FormatOctal_When_FormatCodeIsO() {
        Format f = new Format("%o");
        assertThat(f.form(255)).isEqualTo("377");
    }

    @Test
    void should_FormatOctalWithAlt_When_HashModifier() {
        Format f = new Format("%#o");
        assertThat(f.form(255)).isEqualTo("0377");
    }

    // -----------------------------------------------------------------------
    // form(long) - hex with negative
    // -----------------------------------------------------------------------

    @Test
    void should_FormatUpperHex_When_FormatCodeIsUpperX() {
        Format f = new Format("%X");
        assertThat(f.form(255)).isEqualTo("FF");
    }

    @Test
    void should_FormatHexWithAlt_When_HashModifier() {
        Format f = new Format("%#x");
        assertThat(f.form(255)).isEqualTo("0xff");
    }

    // -----------------------------------------------------------------------
    // form(char)
    // -----------------------------------------------------------------------

    @Test
    void should_FormatCharacter_When_FormatCodeIsC() {
        Format f = new Format("%c");
        assertThat(f.form('A')).isEqualTo("A");
    }

    @Test
    void should_PadCharacter_When_WidthSpecified() {
        Format f = new Format("%5c");
        assertThat(f.form('X')).isEqualTo("    X");
    }

    // -----------------------------------------------------------------------
    // form(double) - additional edge cases
    // -----------------------------------------------------------------------

    @Test
    void should_FormatNegativeDouble_When_NegativeValue() {
        Format f = new Format("%10.2f");
        assertThat(f.form(-3.14)).isEqualTo("     -3.14");
    }

    @Test
    void should_FormatWithPlusSign_When_PlusModifier() {
        Format f = new Format("%+.1f");
        assertThat(f.form(5.0)).isEqualTo("+5.0");
    }

    @Test
    void should_FormatScientific_When_FormatCodeIsE() {
        Format f = new Format("%.2e");
        String result = f.form(12345.6);
        assertThat(result).containsIgnoringCase("e");
    }

    @Test
    void should_FormatGeneral_When_FormatCodeIsG() {
        Format f = new Format("%g");
        String result = f.form(0.00001);
        // %g should use scientific notation for very small numbers
        assertThat(result).containsIgnoringCase("e");
    }

    // -----------------------------------------------------------------------
    // form(String) - edge cases
    // -----------------------------------------------------------------------

    @Test
    void should_LeftAlignString_When_MinusModifier() {
        Format f = new Format("%-10s");
        assertThat(f.form("hi")).isEqualTo("hi        ");
    }

    // -----------------------------------------------------------------------
    // atoi() - additional edge cases
    // -----------------------------------------------------------------------

    @Test
    void should_ParseWithPlusSign_When_AtoiCalledWithPlus() {
        assertThat(Format.atoi("+42")).isEqualTo(42);
    }

    @Test
    void should_ReturnZero_When_AtoiCalledWithEmptyString() {
        assertThat(Format.atoi("")).isEqualTo(0);
    }

    @Test
    void should_StopAtNonNumeric_When_AtoiCalledWithMixedContent() {
        assertThat(Format.atoi("123abc")).isEqualTo(123);
    }

    @Test
    void should_HandleWhitespace_When_AtoiCalledWithLeadingSpaces() {
        assertThat(Format.atoi("  42")).isEqualTo(42);
    }

    @Test
    void should_ParseUppercaseHex_When_AtoiCalledWith0X() {
        assertThat(Format.atoi("0XFF")).isEqualTo(255);
    }
}
