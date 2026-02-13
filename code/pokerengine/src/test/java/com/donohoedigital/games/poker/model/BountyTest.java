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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for bounty/knockout tournament support in TournamentProfile.
 */
public class BountyTest {

    @Test
    public void should_DisableBountyByDefault() {
        TournamentProfile profile = new TournamentProfile("Test");
        assertFalse("Bounty should be disabled by default", profile.isBountyEnabled());
    }

    @Test
    public void should_EnableBounty() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyEnabled(true);
        assertTrue("Bounty should be enabled", profile.isBountyEnabled());
    }

    @Test
    public void should_SetBountyAmount() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyAmount(500);
        assertEquals(500, profile.getBountyAmount());
    }

    @Test
    public void should_ReturnZeroBountyAmount_WhenNotSet() {
        TournamentProfile profile = new TournamentProfile("Test");
        assertEquals(0, profile.getBountyAmount());
    }

    @Test
    public void should_PersistBountySettings() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyEnabled(true);
        profile.setBountyAmount(1000);

        // Verify settings persist
        assertTrue(profile.isBountyEnabled());
        assertEquals(1000, profile.getBountyAmount());
    }

    @Test
    public void should_AllowMaxBountyAmount() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyAmount(10000); // MAX_BOUNTY
        assertEquals(10000, profile.getBountyAmount());
    }

    @Test
    public void should_AllowMinBountyAmount() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyAmount(1);
        assertEquals(1, profile.getBountyAmount());
    }

    @Test
    public void should_DisableBounty_AfterEnabling() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyEnabled(true);
        profile.setBountyEnabled(false);
        assertFalse("Bounty should be disabled", profile.isBountyEnabled());
    }

    @Test
    public void should_PreserveBountyAmount_WhenTogglingEnabled() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyAmount(500);
        profile.setBountyEnabled(true);
        profile.setBountyEnabled(false);

        // Amount should still be set even when disabled
        assertEquals(500, profile.getBountyAmount());
    }

    @Test
    public void should_ClampNegativeBountyAmount_ToZero() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyAmount(-100);
        assertEquals("Negative bounty should be clamped to 0", 0, profile.getBountyAmount());
    }

    @Test
    public void should_ClampExcessiveBountyAmount_ToMax() {
        TournamentProfile profile = new TournamentProfile("Test");
        profile.setBountyAmount(99999);
        assertEquals("Excessive bounty should be clamped to MAX_BOUNTY", TournamentProfile.MAX_BOUNTY,
                profile.getBountyAmount());
    }
}
