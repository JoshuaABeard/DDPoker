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

import com.donohoedigital.comms.DMTypedHashMap;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for BlindStructure - encapsulates blind/ante access logic with
 * doubling.
 *
 * <p>
 * BlindStructure wraps a DMTypedHashMap and provides clean access to: - Blind
 * amounts (small, big, ante) for any level - Break detection - Automatic
 * doubling after last defined level - Overflow protection for MAX_BLINDANTE
 */
public class BlindStructureTest {

    // ========== Basic Access Tests ==========

    @Test
    public void should_ReturnSmallBlind_ForDefinedLevel() {
        // Given: profile with level 1 defined
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small1", "10");
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When: get small blind for level 1
        int small = blinds.getSmallBlind(1);

        // Then: should return 10
        assertEquals(10, small);
    }

    @Test
    public void should_ReturnBigBlind_ForDefinedLevel() {
        // Given: profile with level 1 defined
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("big1", "20");
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When: get big blind for level 1
        int big = blinds.getBigBlind(1);

        // Then: should return 20
        assertEquals(20, big);
    }

    @Test
    public void should_ReturnAnte_ForDefinedLevel() {
        // Given: profile with level 1 ante
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("ante1", "5");
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When: get ante for level 1
        int ante = blinds.getAnte(1);

        // Then: should return 5
        assertEquals(5, ante);
    }

    @Test
    public void should_ReturnZero_WhenBlindNotDefined() {
        // Given: empty map
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When: get undefined blind
        int small = blinds.getSmallBlind(1);

        // Then: should return 0
        assertEquals(0, small);
    }

    // ========== Break Detection Tests ==========

    @Test
    public void should_DetectBreakLevel() {
        // Given: level 2 is a break
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("ante2", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        map.setInteger("lastlevel", 2);

        BlindStructure blinds = new BlindStructure(map);

        // When: check if level 2 is a break
        boolean isBreak = blinds.isBreak(2);

        // Then: should return true
        assertTrue(isBreak);
    }

    @Test
    public void should_NotDetectNormalLevelAsBreak() {
        // Given: level 1 is normal
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("ante1", "5");
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When: check if level 1 is a break
        boolean isBreak = blinds.isBreak(1);

        // Then: should return false
        assertFalse(isBreak);
    }

    // ========== Doubling Tests ==========

    @Test
    public void should_DoubleBlinds_AfterLastLevel() {
        // Given: last level is 3, doubling enabled
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small3", "100");
        map.setString("big3", "200");
        map.setInteger("lastlevel", 3);
        map.setBoolean("doubleafterlast", true);

        BlindStructure blinds = new BlindStructure(map);

        // When: get blinds for level 4 (beyond last)
        int small4 = blinds.getSmallBlind(4);
        int big4 = blinds.getBigBlind(4);

        // Then: should be doubled
        assertEquals(200, small4);
        assertEquals(400, big4);
    }

    @Test
    public void should_DoubleMultipleTimes_ForLevelsFarBeyondLast() {
        // Given: last level is 2
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small2", "10");
        map.setInteger("lastlevel", 2);
        map.setBoolean("doubleafterlast", true);

        BlindStructure blinds = new BlindStructure(map);

        // When: get blind for level 5 (3 levels beyond)
        int small5 = blinds.getSmallBlind(5);

        // Then: should be 10 * 2^3 = 80
        assertEquals(80, small5);
    }

    @Test
    public void should_NotDouble_WhenDoublingDisabled() {
        // Given: last level is 2, doubling disabled
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small2", "100");
        map.setInteger("lastlevel", 2);
        map.setBoolean("doubleafterlast", false);

        BlindStructure blinds = new BlindStructure(map);

        // When: get blind for level 3
        int small3 = blinds.getSmallBlind(3);

        // Then: should stay at 100 (no doubling)
        assertEquals(100, small3);
    }

    @Test
    public void should_StopDoubling_WhenNextDoubleWouldExceedMax() {
        // Given: last level has large blind that would overflow if doubled
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small1", "60000000"); // 60M
        map.setInteger("lastlevel", 1);
        map.setBoolean("doubleafterlast", true);

        BlindStructure blinds = new BlindStructure(map);

        // When: get blind for level 2 (doubling 60M -> 120M exceeds MAX of 100M)
        int small2 = blinds.getSmallBlind(2);

        // Then: should stop doubling and return last valid value (60M)
        // Original behavior: when l >= MAX_BLINDANTE, undo last doubling and break
        assertEquals(60000000, small2);
    }

    @Test
    public void should_SkipBreaks_WhenDoublingBeyondLast() {
        // Given: level 2 is break, level 1 is normal
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small1", "10");
        map.setString("ante2", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        map.setInteger("lastlevel", 2);
        map.setBoolean("doubleafterlast", true);

        BlindStructure blinds = new BlindStructure(map);

        // When: get blind for level 3 (level 2 is break, so use level 1 and double
        // once)
        int small3 = blinds.getSmallBlind(3);

        // Then: should be 10 * 2 = 20 (skipped break at level 2)
        assertEquals(20, small3);
    }

    // ========== Last Non-Break Blind Tests ==========

    @Test
    public void should_GetLastSmallBlind_SkippingBreaks() {
        // Given: level 3 is break, level 2 is normal
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small2", "20");
        map.setString("ante3", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        map.setInteger("lastlevel", 3);

        BlindStructure blinds = new BlindStructure(map);

        // When: get last small blind for level 3 (which is a break)
        int lastSmall = blinds.getLastSmallBlind(3);

        // Then: should return level 2's small blind
        assertEquals(20, lastSmall);
    }

    @Test
    public void should_GetLastBigBlind_SkippingBreaks() {
        // Given: level 3 is break, level 2 is normal
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("big2", "40");
        map.setString("ante3", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        map.setInteger("lastlevel", 3);

        BlindStructure blinds = new BlindStructure(map);

        // When: get last big blind for level 3
        int lastBig = blinds.getLastBigBlind(3);

        // Then: should return level 2's big blind
        assertEquals(40, lastBig);
    }

    @Test
    public void should_ReturnSameLevel_WhenNotABreak() {
        // Given: level 2 is not a break
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("small2", "20");
        map.setInteger("lastlevel", 2);

        BlindStructure blinds = new BlindStructure(map);

        // When: get last small blind for level 2
        int lastSmall = blinds.getLastSmallBlind(2);

        // Then: should return level 2's own blind
        assertEquals(20, lastSmall);
    }

    // ========== Edge Cases ==========

    @Test
    public void should_HandleLevel0_Gracefully() {
        // Given: normal structure
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When: get blind for level 0
        int small = blinds.getSmallBlind(0);

        // Then: should return 0 (no error)
        assertEquals(0, small);
    }

    @Test(expected = com.donohoedigital.base.ApplicationError.class)
    public void should_ThrowError_WhenGettingBlindForBreakLevel() {
        // Given: level 1 is a break
        DMTypedHashMap map = new DMTypedHashMap();
        map.setString("ante1", String.valueOf(TournamentProfile.BREAK_ANTE_VALUE));
        map.setInteger("lastlevel", 1);

        BlindStructure blinds = new BlindStructure(map);

        // When/Then: attempting to get blind for break should throw
        blinds.getSmallBlind(1);
    }
}
