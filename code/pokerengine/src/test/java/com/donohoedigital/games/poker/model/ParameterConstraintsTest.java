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
 * Tests for ParameterConstraints - tournament parameter limits and constraints.
 */
public class ParameterConstraintsTest {

    // ========== getMaxPayoutSpots() Tests ==========

    @Test
    public void should_CalculateMaxSpots_AsPercentOfPlayers() {
        // MAX_SPOTS_PERCENT = 0.333... (33.33%)
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 100 players, 33.33% = 33 spots
        int maxSpots = constraints.getMaxPayoutSpots(100);
        assertEquals("Should be 33% of 100 players", 33, maxSpots);
    }

    @Test
    public void should_CapMaxSpots_AtMAX_SPOTS() {
        // MAX_SPOTS = 560
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 2000 players, 33.33% = 666, but cap at 560
        int maxSpots = constraints.getMaxPayoutSpots(2000);
        assertEquals("Should be capped at MAX_SPOTS (560)", 560, maxSpots);
    }

    @Test
    public void should_EnforceMinimum_OfMIN_SPOTS() {
        // MIN_SPOTS = 10
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 10 players, 33.33% = 3, but enforce min of 10
        int maxSpots = constraints.getMaxPayoutSpots(10);
        assertEquals("Should be at least MIN_SPOTS (10)", 10, maxSpots);
    }

    @Test
    public void should_CapMaxSpots_AtPlayerCount() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 8 players, MIN_SPOTS would be 10, but cap at player count
        int maxSpots = constraints.getMaxPayoutSpots(8);
        assertEquals("Should be capped at player count when less than MIN_SPOTS", 8, maxSpots);
    }

    @Test
    public void should_HandleZeroPlayers() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxSpots = constraints.getMaxPayoutSpots(0);
        assertEquals("Should return 0 for 0 players (can't exceed player count)", 0, maxSpots);
    }

    // ========== getMaxPayoutPercent() Tests ==========

    @Test
    public void should_CalculateMaxPercent_BasedOnMaxSpots() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 100 players, max spots = 33, so max % = 33%
        int maxPercent = constraints.getMaxPayoutPercent(100);
        assertEquals("Max percent should be 33%", 33, maxPercent);
    }

    @Test
    public void should_CapMaxPercent_WhenSpotsCapApplies() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 2000 players, max spots = 560 (capped), so max % = 560/2000 = 28%
        int maxPercent = constraints.getMaxPayoutPercent(2000);
        assertEquals("Max percent should be 28% when spots are capped", 28, maxPercent);
    }

    @Test
    public void should_ReturnZero_WhenZeroPlayers() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxPercent = constraints.getMaxPayoutPercent(0);
        assertEquals("Should return 0% for 0 players", 0, maxPercent);
    }

    @Test
    public void should_CalculateCorrectPercent_ForSmallTournament() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 20 players, max spots = 10 (MIN_SPOTS), so max % = 10/20 = 50%
        int maxPercent = constraints.getMaxPayoutPercent(20);
        assertEquals("Max percent should be 50% for 20 players", 50, maxPercent);
    }

    // ========== getMaxOnlinePlayers() Tests ==========

    @Test
    public void should_ReturnNumPlayers_WhenBelowOnlineMax() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_ONLINE_PLAYERS = 90
        int maxOnline = constraints.getMaxOnlinePlayers(50);
        assertEquals("Should return configured player count when below max", 50, maxOnline);
    }

    @Test
    public void should_CapAtMAX_ONLINE_PLAYERS() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_ONLINE_PLAYERS = 90
        int maxOnline = constraints.getMaxOnlinePlayers(200);
        assertEquals("Should be capped at MAX_ONLINE_PLAYERS (90)", 90, maxOnline);
    }

    @Test
    public void should_ReturnExactly90_WhenAt90Players() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxOnline = constraints.getMaxOnlinePlayers(90);
        assertEquals("Should return 90 when exactly at limit", 90, maxOnline);
    }

    // ========== getMaxRaises() Tests ==========

    @Test
    public void should_AllowUnlimitedRaises_HeadsUpHuman() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(2, false, true);
        assertEquals("Should allow unlimited raises for heads-up human", Integer.MAX_VALUE, maxRaises);
    }

    @Test
    public void should_Cap4Raises_HeadsUpComputer() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_AI_RAISES = 4
        int maxRaises = constraints.getMaxRaises(2, true, true);
        assertEquals("Should cap at 4 raises for heads-up computer", 4, maxRaises);
    }

    @Test
    public void should_RespectConfiguredMax_WhenNotHeadsUp() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxraises", 5);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(5, false, true);
        assertEquals("Should use configured max when not heads-up", 5, maxRaises);
    }

    @Test
    public void should_UseDefault3Raises_WhenNotConfigured() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(5, false, false);
        assertEquals("Should default to 3 raises", 3, maxRaises);
    }

    @Test
    public void should_CapComputerRaisesAt4_WhenNotHeadsUp() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxraises", 9);
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_AI_RAISES = 4, even if configured higher
        int maxRaises = constraints.getMaxRaises(5, true, false);
        assertEquals("Computer should be capped at 4 raises", 4, maxRaises);
    }

    @Test
    public void should_ObserveRaiseCap_WhenHeadsUpButCapNotIgnored() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxraises", 5);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(2, false, false);
        assertEquals("Should observe configured cap when heads-up cap not ignored", 5, maxRaises);
    }

    // ========== getMaxRebuys() Tests ==========

    @Test
    public void should_ReturnConfiguredRebuys() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxrebuys", 5);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRebuys = constraints.getMaxRebuys();
        assertEquals("Should return configured max rebuys", 5, maxRebuys);
    }

    @Test
    public void should_Default0Rebuys_WhenNotConfigured() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRebuys = constraints.getMaxRebuys();
        assertEquals("Should default to 0 rebuys", 0, maxRebuys);
    }

    @Test
    public void should_CapRebuysAtMAX_REBUYS() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxrebuys", 200); // Exceeds MAX_REBUYS (99)
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRebuys = constraints.getMaxRebuys();
        assertEquals("Should be capped at MAX_REBUYS (99)", 99, maxRebuys);
    }

    // ========== getMaxObservers() Tests ==========

    @Test
    public void should_Default5Observers() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals("Should default to 5 observers", 5, maxObservers);
    }

    @Test
    public void should_ReturnConfiguredObservers() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxobs", 15);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals("Should return configured max observers", 15, maxObservers);
    }

    @Test
    public void should_CapObserversAtMAX_OBSERVERS() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxobs", 50); // Exceeds MAX_OBSERVERS (30)
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals("Should be capped at MAX_OBSERVERS (30)", 30, maxObservers);
    }

    @Test
    public void should_Allow0Observers() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxobs", 0);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals("Should allow 0 observers", 0, maxObservers);
    }
}
