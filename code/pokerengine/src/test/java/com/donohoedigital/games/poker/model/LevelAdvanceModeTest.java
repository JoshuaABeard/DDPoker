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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for LevelAdvanceMode enum.
 */
public class LevelAdvanceModeTest {

    @Test
    public void testEnumValues() {
        LevelAdvanceMode[] values = LevelAdvanceMode.values();
        assertEquals("Should have 2 values", 2, values.length);
        assertEquals("First value should be TIME", LevelAdvanceMode.TIME, values[0]);
        assertEquals("Second value should be HANDS", LevelAdvanceMode.HANDS, values[1]);
    }

    @Test
    public void testGetDisplayNameForTime() {
        assertEquals("TIME display name should be 'Time'", "Time", LevelAdvanceMode.TIME.getDisplayName());
    }

    @Test
    public void testGetDisplayNameForHands() {
        assertEquals("HANDS display name should be 'Hands'", "Hands", LevelAdvanceMode.HANDS.getDisplayName());
    }

    @Test
    public void testToStringForTime() {
        assertEquals("TIME toString should be 'Time'", "Time", LevelAdvanceMode.TIME.toString());
    }

    @Test
    public void testToStringForHands() {
        assertEquals("HANDS toString should be 'Hands'", "Hands", LevelAdvanceMode.HANDS.toString());
    }

    @Test
    public void testFromStringWithValidUpperCase() {
        assertEquals("Should parse 'TIME'", LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("TIME"));
        assertEquals("Should parse 'HANDS'", LevelAdvanceMode.HANDS, LevelAdvanceMode.fromString("HANDS"));
    }

    @Test
    public void testFromStringWithValidLowerCase() {
        assertEquals("Should parse 'time' (case insensitive)", LevelAdvanceMode.TIME,
                LevelAdvanceMode.fromString("time"));
        assertEquals("Should parse 'hands' (case insensitive)", LevelAdvanceMode.HANDS,
                LevelAdvanceMode.fromString("hands"));
    }

    @Test
    public void testFromStringWithValidMixedCase() {
        assertEquals("Should parse 'Time'", LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("Time"));
        assertEquals("Should parse 'Hands'", LevelAdvanceMode.HANDS, LevelAdvanceMode.fromString("Hands"));
    }

    @Test
    public void testFromStringWithNull() {
        assertEquals("Null should default to TIME", LevelAdvanceMode.TIME, LevelAdvanceMode.fromString(null));
    }

    @Test
    public void testFromStringWithEmptyString() {
        assertEquals("Empty string should default to TIME", LevelAdvanceMode.TIME, LevelAdvanceMode.fromString(""));
    }

    @Test
    public void testFromStringWithWhitespace() {
        assertEquals("Whitespace should default to TIME", LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("   "));
    }

    @Test
    public void testFromStringWithInvalidValue() {
        assertEquals("Invalid value should default to TIME", LevelAdvanceMode.TIME,
                LevelAdvanceMode.fromString("INVALID"));
        assertEquals("Random string should default to TIME", LevelAdvanceMode.TIME, LevelAdvanceMode.fromString("xyz"));
    }

    @Test
    public void testValueOfMethod() {
        assertEquals("valueOf('TIME') should work", LevelAdvanceMode.TIME, LevelAdvanceMode.valueOf("TIME"));
        assertEquals("valueOf('HANDS') should work", LevelAdvanceMode.HANDS, LevelAdvanceMode.valueOf("HANDS"));
    }
}
