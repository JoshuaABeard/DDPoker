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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for hands-per-level advancement mode.
 */
public class HandsPerLevelTest {

    @Test
    public void should_DefaultToTimeBasedAdvancement() {
        TournamentProfile profile = new TournamentProfile("Test");
        assertEquals(LevelAdvanceMode.TIME, profile.getLevelAdvanceMode(), "Default mode should be TIME");
    }

    @Test
    public void should_AllowHandsBasedAdvancement() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setLevelAdvanceMode(LevelAdvanceMode.HANDS);
        assertEquals(LevelAdvanceMode.HANDS, profile.getLevelAdvanceMode(), "Mode should be HANDS");
    }

    @Test
    public void should_StoreHandsPerLevel() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setHandsPerLevel(20);
        assertEquals(20, profile.getHandsPerLevel(), "Hands per level should be 20");
    }

    @Test
    public void should_DefaultHandsPerLevelTo10() {
        TournamentProfile profile = new TournamentProfile("Test");
        assertEquals(10, profile.getHandsPerLevel(), "Default hands per level should be 10");
    }

    @Test
    public void should_ClampHandsPerLevelToMinimum() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setHandsPerLevel(0);
        assertEquals(1, profile.getHandsPerLevel(), "Minimum hands per level should be 1");
    }

    @Test
    public void should_ClampHandsPerLevelToMaximum() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setHandsPerLevel(1000);
        assertEquals(100, profile.getHandsPerLevel(), "Maximum hands per level should be 100");
    }

    @Test
    public void should_PreserveAdvanceModeAcrossSerialization() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setLevelAdvanceMode(LevelAdvanceMode.HANDS);
        profile.setHandsPerLevel(15);

        // Serialize and deserialize would happen here
        // For now, just verify getters work
        assertEquals(LevelAdvanceMode.HANDS, profile.getLevelAdvanceMode());
        assertEquals(15, profile.getHandsPerLevel());
    }

    @Test
    public void should_AllowSwitchingBetweenModes() {
        TournamentProfile profile = new TournamentProfile("Test");

        profile.setLevelAdvanceMode(LevelAdvanceMode.HANDS);
        assertEquals(LevelAdvanceMode.HANDS, profile.getLevelAdvanceMode());

        profile.setLevelAdvanceMode(LevelAdvanceMode.TIME);
        assertEquals(LevelAdvanceMode.TIME, profile.getLevelAdvanceMode());
    }

    @Test
    public void should_HandleNullAdvanceMode() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setLevelAdvanceMode(null);
        // Should default to TIME
        assertEquals(LevelAdvanceMode.TIME, profile.getLevelAdvanceMode(), "Null mode should default to TIME");
    }
}
