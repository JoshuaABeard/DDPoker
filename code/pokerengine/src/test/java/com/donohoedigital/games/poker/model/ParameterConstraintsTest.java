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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(33, maxSpots, "Should be 33% of 100 players");
    }

    @Test
    public void should_CapMaxSpots_AtMAX_SPOTS() {
        // MAX_SPOTS = 560
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 2000 players, 33.33% = 666, but cap at 560
        int maxSpots = constraints.getMaxPayoutSpots(2000);
        assertEquals(560, maxSpots, "Should be capped at MAX_SPOTS (560)");
    }

    @Test
    public void should_EnforceMinimum_OfMIN_SPOTS() {
        // MIN_SPOTS = 10
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 10 players, 33.33% = 3, but enforce min of 10
        int maxSpots = constraints.getMaxPayoutSpots(10);
        assertEquals(10, maxSpots, "Should be at least MIN_SPOTS (10)");
    }

    @Test
    public void should_CapMaxSpots_AtPlayerCount() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 8 players, MIN_SPOTS would be 10, but cap at player count
        int maxSpots = constraints.getMaxPayoutSpots(8);
        assertEquals(8, maxSpots, "Should be capped at player count when less than MIN_SPOTS");
    }

    @Test
    public void should_HandleZeroPlayers() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxSpots = constraints.getMaxPayoutSpots(0);
        assertEquals(0, maxSpots, "Should return 0 for 0 players (can't exceed player count)");
    }

    // ========== getMaxPayoutPercent() Tests ==========

    @Test
    public void should_CalculateMaxPercent_BasedOnMaxSpots() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 100 players, max spots = 33, so max % = 33%
        int maxPercent = constraints.getMaxPayoutPercent(100);
        assertEquals(33, maxPercent, "Max percent should be 33%");
    }

    @Test
    public void should_CapMaxPercent_WhenSpotsCapApplies() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 2000 players, max spots = 560 (capped), so max % = 560/2000 = 28%
        int maxPercent = constraints.getMaxPayoutPercent(2000);
        assertEquals(28, maxPercent, "Max percent should be 28% when spots are capped");
    }

    @Test
    public void should_ReturnZero_WhenZeroPlayers() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxPercent = constraints.getMaxPayoutPercent(0);
        assertEquals(0, maxPercent, "Should return 0% for 0 players");
    }

    @Test
    public void should_CalculateCorrectPercent_ForSmallTournament() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // For 20 players, max spots = 10 (MIN_SPOTS), so max % = 10/20 = 50%
        int maxPercent = constraints.getMaxPayoutPercent(20);
        assertEquals(50, maxPercent, "Max percent should be 50% for 20 players");
    }

    // ========== getMaxOnlinePlayers() Tests ==========

    @Test
    public void should_ReturnNumPlayers_WhenBelowOnlineMax() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_ONLINE_PLAYERS = 120
        int maxOnline = constraints.getMaxOnlinePlayers(50);
        assertEquals(50, maxOnline, "Should return configured player count when below max");
    }

    @Test
    public void should_CapAtMAX_ONLINE_PLAYERS() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_ONLINE_PLAYERS = 120
        int maxOnline = constraints.getMaxOnlinePlayers(200);
        assertEquals(120, maxOnline, "Should be capped at MAX_ONLINE_PLAYERS (120)");
    }

    @Test
    public void should_ReturnExactly90_WhenAt90Players() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxOnline = constraints.getMaxOnlinePlayers(120);
        assertEquals(120, maxOnline, "Should return 120 when exactly at limit");
    }

    // ========== getMaxRaises() Tests ==========

    @Test
    public void should_AllowUnlimitedRaises_HeadsUpHuman() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(2, false, true);
        assertEquals(Integer.MAX_VALUE, maxRaises, "Should allow unlimited raises for heads-up human");
    }

    @Test
    public void should_Cap4Raises_HeadsUpComputer() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_AI_RAISES = 4
        int maxRaises = constraints.getMaxRaises(2, true, true);
        assertEquals(4, maxRaises, "Should cap at 4 raises for heads-up computer");
    }

    @Test
    public void should_RespectConfiguredMax_WhenNotHeadsUp() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxraises", 5);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(5, false, true);
        assertEquals(5, maxRaises, "Should use configured max when not heads-up");
    }

    @Test
    public void should_UseDefault3Raises_WhenNotConfigured() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(5, false, false);
        assertEquals(3, maxRaises, "Should default to 3 raises");
    }

    @Test
    public void should_CapComputerRaisesAt4_WhenNotHeadsUp() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxraises", 9);
        ParameterConstraints constraints = new ParameterConstraints(map);

        // MAX_AI_RAISES = 4, even if configured higher
        int maxRaises = constraints.getMaxRaises(5, true, false);
        assertEquals(4, maxRaises, "Computer should be capped at 4 raises");
    }

    @Test
    public void should_ObserveRaiseCap_WhenHeadsUpButCapNotIgnored() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxraises", 5);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRaises = constraints.getMaxRaises(2, false, false);
        assertEquals(5, maxRaises, "Should observe configured cap when heads-up cap not ignored");
    }

    // ========== getMaxRebuys() Tests ==========

    @Test
    public void should_ReturnConfiguredRebuys() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxrebuys", 5);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRebuys = constraints.getMaxRebuys();
        assertEquals(5, maxRebuys, "Should return configured max rebuys");
    }

    @Test
    public void should_Default0Rebuys_WhenNotConfigured() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRebuys = constraints.getMaxRebuys();
        assertEquals(0, maxRebuys, "Should default to 0 rebuys");
    }

    @Test
    public void should_CapRebuysAtMAX_REBUYS() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxrebuys", 200); // Exceeds MAX_REBUYS (99)
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxRebuys = constraints.getMaxRebuys();
        assertEquals(99, maxRebuys, "Should be capped at MAX_REBUYS (99)");
    }

    // ========== getMaxObservers() Tests ==========

    @Test
    public void should_Default5Observers() {
        DMTypedHashMap map = new DMTypedHashMap();
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals(5, maxObservers, "Should default to 5 observers");
    }

    @Test
    public void should_ReturnConfiguredObservers() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxobs", 15);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals(15, maxObservers, "Should return configured max observers");
    }

    @Test
    public void should_CapObserversAtMAX_OBSERVERS() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxobs", 50); // Exceeds MAX_OBSERVERS (30)
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals(30, maxObservers, "Should be capped at MAX_OBSERVERS (30)");
    }

    @Test
    public void should_Allow0Observers() {
        DMTypedHashMap map = new DMTypedHashMap();
        map.setInteger("maxobs", 0);
        ParameterConstraints constraints = new ParameterConstraints(map);

        int maxObservers = constraints.getMaxObservers();
        assertEquals(0, maxObservers, "Should allow 0 observers");
    }
}
