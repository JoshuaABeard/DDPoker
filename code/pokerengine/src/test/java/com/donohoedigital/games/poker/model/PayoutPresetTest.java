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
 * Tests for PayoutPreset - predefined payout distributions.
 */
public class PayoutPresetTest {

    @Test
    public void should_HaveCorrectDisplayNamesForAllPresets() {
        assertEquals(PayoutPreset.CUSTOM.getDisplayName(), "Custom");
        assertEquals(PayoutPreset.TOP_HEAVY.getDisplayName(), "Top-Heavy (~50% winner)");
        assertEquals(PayoutPreset.STANDARD.getDisplayName(), "Standard (~40% winner)");
        assertEquals(PayoutPreset.FLAT.getDisplayName(), "Flat (~25% winner)");
    }

    @Test
    public void should_HaveNoDistribution_ForCustomPreset() {
        assertFalse(PayoutPreset.CUSTOM.hasDistribution(), "CUSTOM should have no distribution");
        assertNull(PayoutPreset.CUSTOM.getPercentages(), "CUSTOM percentages should be null");
        assertEquals(0, PayoutPreset.CUSTOM.getSpotCount(), "CUSTOM spot count should be 0");
    }

    @Test
    public void should_HaveCorrectDistribution_ForTopHeavy() {
        assertTrue(PayoutPreset.TOP_HEAVY.hasDistribution(), "TOP_HEAVY should have distribution");
        assertEquals(3, PayoutPreset.TOP_HEAVY.getSpotCount(), "TOP_HEAVY should have 3 spots");

        double[] percentages = PayoutPreset.TOP_HEAVY.getPercentages();
        assertEquals(50.0, percentages[0], 0.01, "1st place should be 50%");
        assertEquals(30.0, percentages[1], 0.01, "2nd place should be 30%");
        assertEquals(20.0, percentages[2], 0.01, "3rd place should be 20%");
    }

    @Test
    public void should_SumTo100Percent_ForTopHeavy() {
        double sum = 0;
        for (double pct : PayoutPreset.TOP_HEAVY.getPercentages()) {
            sum += pct;
        }
        assertEquals(100.0, sum, 0.01, "TOP_HEAVY should sum to 100%");
    }

    @Test
    public void should_HaveCorrectDistribution_ForStandard() {
        assertTrue(PayoutPreset.STANDARD.hasDistribution(), "STANDARD should have distribution");
        assertEquals(5, PayoutPreset.STANDARD.getSpotCount(), "STANDARD should have 5 spots");

        double[] percentages = PayoutPreset.STANDARD.getPercentages();
        assertEquals(40.0, percentages[0], 0.01, "1st place should be 40%");
        assertEquals(25.0, percentages[1], 0.01, "2nd place should be 25%");
        assertEquals(17.5, percentages[2], 0.01, "3rd place should be 17.5%");
        assertEquals(12.5, percentages[3], 0.01, "4th place should be 12.5%");
        assertEquals(5.0, percentages[4], 0.01, "5th place should be 5%");
    }

    @Test
    public void should_SumTo100Percent_ForStandard() {
        double sum = 0;
        for (double pct : PayoutPreset.STANDARD.getPercentages()) {
            sum += pct;
        }
        assertEquals(100.0, sum, 0.01, "STANDARD should sum to 100%");
    }

    @Test
    public void should_HaveCorrectDistribution_ForFlat() {
        assertTrue(PayoutPreset.FLAT.hasDistribution(), "FLAT should have distribution");
        assertEquals(8, PayoutPreset.FLAT.getSpotCount(), "FLAT should have 8 spots");

        double[] percentages = PayoutPreset.FLAT.getPercentages();
        assertEquals(25.0, percentages[0], 0.01, "1st place should be 25%");
        assertEquals(20.0, percentages[1], 0.01, "2nd place should be 20%");
        assertEquals(15.0, percentages[2], 0.01, "3rd place should be 15%");
        assertEquals(12.5, percentages[3], 0.01, "4th place should be 12.5%");
        assertEquals(10.0, percentages[4], 0.01, "5th place should be 10%");
        assertEquals(7.5, percentages[5], 0.01, "6th place should be 7.5%");
        assertEquals(5.0, percentages[6], 0.01, "7th place should be 5%");
        assertEquals(5.0, percentages[7], 0.01, "8th place should be 5%");
    }

    @Test
    public void should_SumTo100Percent_ForFlat() {
        double sum = 0;
        for (double pct : PayoutPreset.FLAT.getPercentages()) {
            sum += pct;
        }
        assertEquals(100.0, sum, 0.01, "FLAT should sum to 100%");
    }

    @Test
    public void should_ApplyTopHeavyPreset_ToTournamentProfile() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply TOP_HEAVY preset with 3 spots
        PayoutPreset.TOP_HEAVY.applyToProfile(profile, 3);

        // Verify spots were set correctly
        assertEquals(50.0, profile.getSpot(1), 0.01, "1st spot should be 50%");
        assertEquals(30.0, profile.getSpot(2), 0.01, "2nd spot should be 30%");
        assertEquals(20.0, profile.getSpot(3), 0.01, "3rd spot should be 20%");
    }

    @Test
    public void should_ApplyStandardPreset_ToTournamentProfile() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply STANDARD preset with 5 spots
        PayoutPreset.STANDARD.applyToProfile(profile, 5);

        // Verify spots were set correctly
        assertEquals(40.0, profile.getSpot(1), 0.01, "1st spot should be 40%");
        assertEquals(25.0, profile.getSpot(2), 0.01, "2nd spot should be 25%");
        assertEquals(17.5, profile.getSpot(3), 0.01, "3rd spot should be 17.5%");
        assertEquals(12.5, profile.getSpot(4), 0.01, "4th spot should be 12.5%");
        assertEquals(5.0, profile.getSpot(5), 0.01, "5th spot should be 5%");
    }

    @Test
    public void should_ApplyOnlyRequestedSpots_WhenFewerThanPreset() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply STANDARD preset (5 spots) but only request 3
        PayoutPreset.STANDARD.applyToProfile(profile, 3);

        // Verify only 3 spots were set
        assertEquals(40.0, profile.getSpot(1), 0.01, "1st spot should be 40%");
        assertEquals(25.0, profile.getSpot(2), 0.01, "2nd spot should be 25%");
        assertEquals(17.5, profile.getSpot(3), 0.01, "3rd spot should be 17.5%");
        assertEquals(0.0, profile.getSpot(4), 0.01, "4th spot should be 0 (not set)");
    }

    @Test
    public void should_SetAllocPercMode_WhenApplyingPreset() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Default allocation mode should be Auto
        assertTrue(profile.isAllocAuto(), "Default allocation should be Auto");
        assertFalse(profile.isAllocPercent(), "Should not be in Percent mode initially");

        // Apply preset
        PayoutPreset.TOP_HEAVY.applyToProfile(profile, 3);

        // Verify allocation mode was set to Percent
        assertTrue(profile.isAllocPercent(), "Should switch to Percent allocation mode");
        assertFalse(profile.isAllocAuto(), "Should no longer be in Auto mode");
    }

    @Test
    public void should_ApplyAllPresetSpots_WhenMoreSpotsRequested() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply TOP_HEAVY preset (3 spots) but request 5 spots
        PayoutPreset.TOP_HEAVY.applyToProfile(profile, 5);

        // Verify only preset's 3 spots were set
        assertEquals(50.0, profile.getSpot(1), 0.01, "1st spot should be 50%");
        assertEquals(30.0, profile.getSpot(2), 0.01, "2nd spot should be 30%");
        assertEquals(20.0, profile.getSpot(3), 0.01, "3rd spot should be 20%");
        assertEquals(0.0, profile.getSpot(4), 0.01, "4th spot should be 0 (preset doesn't define it)");
        assertEquals(0.0, profile.getSpot(5), 0.01, "5th spot should be 0 (preset doesn't define it)");
    }

    @Test
    public void should_ThrowException_WhenApplyingCustomPreset() {
        assertThrows(IllegalStateException.class, () -> {
            TournamentProfile profile = new TournamentProfile("Test");

            // Attempting to apply CUSTOM preset should throw exception
            PayoutPreset.CUSTOM.applyToProfile(profile, 3);

        });
    }

    @Test
    public void should_IdentifyTopHeavyPreset_FromFirstSpot() {
        assertEquals(PayoutPreset.TOP_HEAVY, PayoutPreset.fromFirstSpot(50.0), "50% should identify TOP_HEAVY");
    }

    @Test
    public void should_IdentifyStandardPreset_FromFirstSpot() {
        assertEquals(PayoutPreset.STANDARD, PayoutPreset.fromFirstSpot(40.0), "40% should identify STANDARD");
    }

    @Test
    public void should_IdentifyFlatPreset_FromFirstSpot() {
        assertEquals(PayoutPreset.FLAT, PayoutPreset.fromFirstSpot(25.0), "25% should identify FLAT");
    }

    @Test
    public void should_ReturnCustom_WhenNoPresetMatches() {
        assertEquals(PayoutPreset.CUSTOM, PayoutPreset.fromFirstSpot(35.0), "35% should return CUSTOM (no match)");
    }

    @Test
    public void should_MatchPreset_FromFullDistribution() {
        // Exact matches
        assertEquals(PayoutPreset.TOP_HEAVY, PayoutPreset.fromDistribution(new double[]{50.0, 30.0, 20.0}));
        assertEquals(PayoutPreset.STANDARD, PayoutPreset.fromDistribution(new double[]{40.0, 25.0, 17.5, 12.5, 5.0}));
        assertEquals(PayoutPreset.FLAT,
                PayoutPreset.fromDistribution(new double[]{25.0, 20.0, 15.0, 12.5, 10.0, 7.5, 5.0, 5.0}));

        // Tolerance matching (within 0.1%)
        assertEquals(PayoutPreset.TOP_HEAVY, PayoutPreset.fromDistribution(new double[]{50.05, 30.03, 19.92}));

        // Wrong length returns CUSTOM
        assertEquals(PayoutPreset.CUSTOM, PayoutPreset.fromDistribution(new double[]{50.0, 30.0}));

        // Wrong values return CUSTOM
        assertEquals(PayoutPreset.CUSTOM, PayoutPreset.fromDistribution(new double[]{45.0, 30.0, 25.0}));

        // Null/empty returns CUSTOM
        assertEquals(PayoutPreset.CUSTOM, PayoutPreset.fromDistribution(null));
        assertEquals(PayoutPreset.CUSTOM, PayoutPreset.fromDistribution(new double[]{}));
    }

    @Test
    public void should_UseDisplayName_ForToString() {
        assertEquals(PayoutPreset.CUSTOM.toString(), "Custom");
        assertEquals(PayoutPreset.TOP_HEAVY.toString(), "Top-Heavy (~50% winner)");
        assertEquals(PayoutPreset.STANDARD.toString(), "Standard (~40% winner)");
        assertEquals(PayoutPreset.FLAT.toString(), "Flat (~25% winner)");
    }
}
