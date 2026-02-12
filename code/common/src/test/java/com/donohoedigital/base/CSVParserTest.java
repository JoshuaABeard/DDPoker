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
 * Tests for CSVParser - CSV line parsing with quote and escape handling. CSV
 * format uses double-quote (") as both quote and escape character.
 */
class CSVParserTest {

    // =================================================================
    // Simple Parsing Tests
    // =================================================================

    @Test
    void should_ParseSingleValue_When_NoCommas() {
        String[] result = CSVParser.parseLine("value");

        assertThat(result).containsExactly("value");
    }

    @Test
    void should_ParseMultipleValues_When_CommasSeparate() {
        String[] result = CSVParser.parseLine("a,b,c");

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void should_ParseEmptyValues_When_ConsecutiveCommas() {
        String[] result = CSVParser.parseLine("a,,c");

        assertThat(result).containsExactly("a", "", "c");
    }

    @Test
    void should_ReturnEmptyArray_When_EmptyLineProvided() {
        String[] result = CSVParser.parseLine("");

        assertThat(result).isEmpty();
    }

    @Test
    void should_AddEmptyValue_When_TrailingComma() {
        String[] result = CSVParser.parseLine("a,b,");

        assertThat(result).containsExactly("a", "b", "");
    }

    // =================================================================
    // Quoted Value Tests
    // =================================================================

    @Test
    void should_RemoveQuotes_When_ValueQuoted() {
        String[] result = CSVParser.parseLine("\"foo bar\"");

        assertThat(result).containsExactly("foo bar");
    }

    @Test
    void should_PreserveCommas_When_InsideQuotes() {
        String[] result = CSVParser.parseLine("\"foo,bar\",baz");

        assertThat(result).containsExactly("foo,bar", "baz");
    }

    @Test
    void should_ParseMixedQuoted_When_SomeValuesQuoted() {
        String[] result = CSVParser.parseLine("\"quoted\",unquoted,\"also quoted\"");

        assertThat(result).containsExactly("quoted", "unquoted", "also quoted");
    }

    @Test
    void should_HandleSpaces_When_InsideQuotes() {
        String[] result = CSVParser.parseLine("\"  spaces  \",\"more  spaces\"");

        assertThat(result).containsExactly("  spaces  ", "more  spaces");
    }

    // =================================================================
    // Escaped Quote Tests
    // =================================================================

    @Test
    void should_HandleEscapedQuotes_When_DoubleQuotesUsed() {
        String[] result = CSVParser.parseLine("\"he said \"\"hi\"\" to me\"");

        assertThat(result).containsExactly("he said \"hi\" to me");
    }

    @Test
    void should_HandleMultipleEscapedQuotes_When_MultiplePresent() {
        String[] result = CSVParser.parseLine("\"\"\"quoted\"\"\"");

        // Parser removes outer quotes and processes escape sequences
        assertThat(result).containsExactly("\"quoted\"");
    }

    @Test
    void should_HandleMixedContent_When_EscapesAndCommas() {
        String[] result = CSVParser.parseLine("\"a \"\"test\"\" value\",normal");

        assertThat(result).containsExactly("a \"test\" value", "normal");
    }

    // =================================================================
    // Complex Example Tests
    // =================================================================

    @Test
    void should_ParseDocExample_When_DocExampleProvided() {
        // From JavaDoc: "foo bar","he said ""hi there"" to me, Joe",doh
        String[] result = CSVParser.parseLine("\"foo bar\",\"he said \"\"hi there\"\" to me, Joe\",doh");

        assertThat(result).containsExactly("foo bar", "he said \"hi there\" to me, Joe", "doh");
    }

    @Test
    void should_HandleComplexLine_When_MultipleFeaturesPresent() {
        String[] result = CSVParser.parseLine("simple,\"with space\",\"with,comma\",\"with\"\"quote\"\"\",last");

        assertThat(result).containsExactly("simple", "with space", "with,comma", "with\"quote\"", "last");
    }

    // =================================================================
    // Edge Cases Tests
    // =================================================================

    @Test
    void should_HandleOnlyComma_When_SingleCommaProvided() {
        String[] result = CSVParser.parseLine(",");

        assertThat(result).containsExactly("", "");
    }

    @Test
    void should_HandleMultipleCommas_When_OnlyCommasProvided() {
        String[] result = CSVParser.parseLine(",,,");

        assertThat(result).containsExactly("", "", "", "");
    }

    @Test
    void should_HandleQuotedEmpty_When_EmptyQuotedValue() {
        String[] result = CSVParser.parseLine("\"\",value,\"\"");

        assertThat(result).containsExactly("", "value", "");
    }

    @Test
    void should_HandleOnlyQuotes_When_EmptyQuotedString() {
        String[] result = CSVParser.parseLine("\"\"");

        assertThat(result).containsExactly("");
    }

    @Test
    void should_HandleLeadingComma_When_StartsWithComma() {
        String[] result = CSVParser.parseLine(",a,b");

        assertThat(result).containsExactly("", "a", "b");
    }

    @Test
    void should_HandleLongLine_When_ManyValues() {
        String[] result = CSVParser.parseLine("a,b,c,d,e,f,g,h,i,j");

        assertThat(result).hasSize(10);
        assertThat(result).containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    }

    // =================================================================
    // Special Character Tests
    // =================================================================

    @Test
    void should_HandleNewlines_When_InsideQuotes() {
        // Note: Actual newlines in quoted strings
        String[] result = CSVParser.parseLine("\"line1\nline2\",value");

        assertThat(result).containsExactly("line1\nline2", "value");
    }

    @Test
    void should_HandleTabs_When_InsideQuotes() {
        String[] result = CSVParser.parseLine("\"tab\there\",value");

        assertThat(result).containsExactly("tab\there", "value");
    }

    @Test
    void should_HandleSpecialChars_When_InsideQuotes() {
        String[] result = CSVParser.parseLine("\"!@#$%^&*()\",\"+=\"");

        assertThat(result).containsExactly("!@#$%^&*()", "+=");
    }

    // =================================================================
    // Whitespace Tests
    // =================================================================

    @Test
    void should_PreserveSpaces_When_OutsideQuotes() {
        // Unquoted spaces are preserved as-is
        String[] result = CSVParser.parseLine(" a , b , c ");

        assertThat(result).containsExactly(" a ", " b ", " c ");
    }

    @Test
    void should_PreserveSpaces_When_InsideQuotes() {
        String[] result = CSVParser.parseLine("\" leading\",\"trailing \",\" both \"");

        assertThat(result).containsExactly(" leading", "trailing ", " both ");
    }

    // =================================================================
    // Number and Data Type Tests
    // =================================================================

    @Test
    void should_ParseNumbers_When_NumericValues() {
        String[] result = CSVParser.parseLine("123,456,789");

        assertThat(result).containsExactly("123", "456", "789");
    }

    @Test
    void should_ParseFloats_When_DecimalValues() {
        String[] result = CSVParser.parseLine("1.5,2.7,3.14");

        assertThat(result).containsExactly("1.5", "2.7", "3.14");
    }

    @Test
    void should_ParseNegatives_When_NegativeNumbers() {
        String[] result = CSVParser.parseLine("-1,-2,-3");

        assertThat(result).containsExactly("-1", "-2", "-3");
    }

    // =================================================================
    // Real-world Data Tests
    // =================================================================

    @Test
    void should_ParseAddress_When_AddressLineProvided() {
        String[] result = CSVParser.parseLine("\"Smith, John\",\"123 Main St, Apt 4\",\"City, State 12345\"");

        assertThat(result).containsExactly("Smith, John", "123 Main St, Apt 4", "City, State 12345");
    }

    @Test
    void should_ParseCSVHeader_When_HeaderLineProvided() {
        String[] result = CSVParser.parseLine("Name,Age,\"Email Address\",Phone");

        assertThat(result).containsExactly("Name", "Age", "Email Address", "Phone");
    }

    @Test
    void should_ParseDataRow_When_MixedDataProvided() {
        String[] result = CSVParser.parseLine("John,25,john@example.com,555-1234");

        assertThat(result).containsExactly("John", "25", "john@example.com", "555-1234");
    }

    // =================================================================
    // Quote at End Tests
    // =================================================================

    @Test
    void should_HandleQuoteAtEnd_When_LastCharIsQuote() {
        String[] result = CSVParser.parseLine("value,\"quoted\"");

        assertThat(result).containsExactly("value", "quoted");
    }

    @Test
    void should_HandleMultipleQuotedAtEnd_When_MultipleEndWithQuotes() {
        String[] result = CSVParser.parseLine("\"a\",\"b\",\"c\"");

        assertThat(result).containsExactly("a", "b", "c");
    }

    // =================================================================
    // Array Size Tests
    // =================================================================

    @Test
    void should_ReturnCorrectSize_When_SingleValue() {
        String[] result = CSVParser.parseLine("value");

        assertThat(result).hasSize(1);
    }

    @Test
    void should_ReturnCorrectSize_When_MultipleValues() {
        String[] result = CSVParser.parseLine("a,b,c");

        assertThat(result).hasSize(3);
    }

    @Test
    void should_ReturnCorrectSize_When_EmptyValues() {
        String[] result = CSVParser.parseLine(",,");

        assertThat(result).hasSize(3);
    }
}
