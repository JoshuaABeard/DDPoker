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
package com.donohoedigital.games.poker.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for LevelAdvanceMode enum.
 */
public class LevelAdvanceModeTest {

    @Test
    public void testEnumValues() {
        LevelAdvanceMode[] values = LevelAdvanceMode.values();
        assertEquals(2, values.length, "Should have 2 values");
        assertEquals(LevelAdvanceMode.TIME, values[0], "First value should be TIME");
        assertEquals(LevelAdvanceMode.HANDS, values[1], "Second value should be HANDS");
    }

    @Test
    public void testGetDisplayNameForTime() {
        assertEquals("Time", LevelAdvanceMode.TIME.getDisplayName(), "TIME display name should be 'Time'");
    }

    @Test
    public void testGetDisplayNameForHands() {
        assertEquals("Hands", LevelAdvanceMode.HANDS.getDisplayName(), "HANDS display name should be 'Hands'");
    }

    @Test
    public void testToStringForTime() {
        assertEquals("Time", LevelAdvanceMode.TIME.toString(), "TIME toString should be 'Time'");
    }

    @Test
    public void testToStringForHands() {
        assertEquals("Hands", LevelAdvanceMode.HANDS.toString(), "HANDS toString should be 'Hands'");
    }

    @Test
    public void testFromStringWithValidUpperCase() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("TIME"), "Should parse 'TIME'");
        assertEquals(LevelAdvanceMode.HANDS, LevelAdvanceMode.fromString("HANDS"), "Should parse 'HANDS'");
    }

    @Test
    public void testFromStringWithValidLowerCase() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("time"),
                "Should parse 'time' (case insensitive)");
        assertEquals(LevelAdvanceMode.HANDS, LevelAdvanceMode.fromString("hands"),
                "Should parse 'hands' (case insensitive)");
    }

    @Test
    public void testFromStringWithValidMixedCase() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("Time"), "Should parse 'Time'");
        assertEquals(LevelAdvanceMode.HANDS, LevelAdvanceMode.fromString("Hands"), "Should parse 'Hands'");
    }

    @Test
    public void testFromStringWithNull() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString(null), "Null should default to TIME");
    }

    @Test
    public void testFromStringWithEmptyString() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString(""), "Empty string should default to TIME");
    }

    @Test
    public void testFromStringWithWhitespace() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("   "), "Whitespace should default to TIME");
    }

    @Test
    public void testFromStringWithInvalidValue() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("INVALID"),
                "Invalid value should default to TIME");
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("xyz"), "Random string should default to TIME");
    }

    @Test
    public void testValueOfMethod() {
        assertEquals(LevelAdvanceMode.TIME, LevelAdvanceMode.valueOf("TIME"), "valueOf('TIME') should work");
        assertEquals(LevelAdvanceMode.HANDS, LevelAdvanceMode.valueOf("HANDS"), "valueOf('HANDS') should work");
    }
}
