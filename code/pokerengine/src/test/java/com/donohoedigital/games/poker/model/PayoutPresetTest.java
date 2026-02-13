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
 * Tests for PayoutPreset - predefined payout distributions.
 */
public class PayoutPresetTest {

    @Test
    public void should_HaveCorrectDisplayNamesForAllPresets() {
        assertEquals("Custom", PayoutPreset.CUSTOM.getDisplayName());
        assertEquals("Top-Heavy (~50% winner)", PayoutPreset.TOP_HEAVY.getDisplayName());
        assertEquals("Standard (~40% winner)", PayoutPreset.STANDARD.getDisplayName());
        assertEquals("Flat (~25% winner)", PayoutPreset.FLAT.getDisplayName());
    }

    @Test
    public void should_HaveNoDistribution_ForCustomPreset() {
        assertFalse("CUSTOM should have no distribution", PayoutPreset.CUSTOM.hasDistribution());
        assertNull("CUSTOM percentages should be null", PayoutPreset.CUSTOM.getPercentages());
        assertEquals("CUSTOM spot count should be 0", 0, PayoutPreset.CUSTOM.getSpotCount());
    }

    @Test
    public void should_HaveCorrectDistribution_ForTopHeavy() {
        assertTrue("TOP_HEAVY should have distribution", PayoutPreset.TOP_HEAVY.hasDistribution());
        assertEquals("TOP_HEAVY should have 3 spots", 3, PayoutPreset.TOP_HEAVY.getSpotCount());

        double[] percentages = PayoutPreset.TOP_HEAVY.getPercentages();
        assertEquals("1st place should be 50%", 50.0, percentages[0], 0.01);
        assertEquals("2nd place should be 30%", 30.0, percentages[1], 0.01);
        assertEquals("3rd place should be 20%", 20.0, percentages[2], 0.01);
    }

    @Test
    public void should_SumTo100Percent_ForTopHeavy() {
        double sum = 0;
        for (double pct : PayoutPreset.TOP_HEAVY.getPercentages()) {
            sum += pct;
        }
        assertEquals("TOP_HEAVY should sum to 100%", 100.0, sum, 0.01);
    }

    @Test
    public void should_HaveCorrectDistribution_ForStandard() {
        assertTrue("STANDARD should have distribution", PayoutPreset.STANDARD.hasDistribution());
        assertEquals("STANDARD should have 5 spots", 5, PayoutPreset.STANDARD.getSpotCount());

        double[] percentages = PayoutPreset.STANDARD.getPercentages();
        assertEquals("1st place should be 40%", 40.0, percentages[0], 0.01);
        assertEquals("2nd place should be 25%", 25.0, percentages[1], 0.01);
        assertEquals("3rd place should be 17.5%", 17.5, percentages[2], 0.01);
        assertEquals("4th place should be 12.5%", 12.5, percentages[3], 0.01);
        assertEquals("5th place should be 5%", 5.0, percentages[4], 0.01);
    }

    @Test
    public void should_SumTo100Percent_ForStandard() {
        double sum = 0;
        for (double pct : PayoutPreset.STANDARD.getPercentages()) {
            sum += pct;
        }
        assertEquals("STANDARD should sum to 100%", 100.0, sum, 0.01);
    }

    @Test
    public void should_HaveCorrectDistribution_ForFlat() {
        assertTrue("FLAT should have distribution", PayoutPreset.FLAT.hasDistribution());
        assertEquals("FLAT should have 8 spots", 8, PayoutPreset.FLAT.getSpotCount());

        double[] percentages = PayoutPreset.FLAT.getPercentages();
        assertEquals("1st place should be 25%", 25.0, percentages[0], 0.01);
        assertEquals("2nd place should be 20%", 20.0, percentages[1], 0.01);
        assertEquals("3rd place should be 15%", 15.0, percentages[2], 0.01);
        assertEquals("4th place should be 12.5%", 12.5, percentages[3], 0.01);
        assertEquals("5th place should be 10%", 10.0, percentages[4], 0.01);
        assertEquals("6th place should be 7.5%", 7.5, percentages[5], 0.01);
        assertEquals("7th place should be 5%", 5.0, percentages[6], 0.01);
        assertEquals("8th place should be 5%", 5.0, percentages[7], 0.01);
    }

    @Test
    public void should_SumTo100Percent_ForFlat() {
        double sum = 0;
        for (double pct : PayoutPreset.FLAT.getPercentages()) {
            sum += pct;
        }
        assertEquals("FLAT should sum to 100%", 100.0, sum, 0.01);
    }

    @Test
    public void should_ApplyTopHeavyPreset_ToTournamentProfile() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply TOP_HEAVY preset with 3 spots
        PayoutPreset.TOP_HEAVY.applyToProfile(profile, 3);

        // Verify spots were set correctly
        assertEquals("1st spot should be 50%", 50.0, profile.getSpot(1), 0.01);
        assertEquals("2nd spot should be 30%", 30.0, profile.getSpot(2), 0.01);
        assertEquals("3rd spot should be 20%", 20.0, profile.getSpot(3), 0.01);
    }

    @Test
    public void should_ApplyStandardPreset_ToTournamentProfile() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply STANDARD preset with 5 spots
        PayoutPreset.STANDARD.applyToProfile(profile, 5);

        // Verify spots were set correctly
        assertEquals("1st spot should be 40%", 40.0, profile.getSpot(1), 0.01);
        assertEquals("2nd spot should be 25%", 25.0, profile.getSpot(2), 0.01);
        assertEquals("3rd spot should be 17.5%", 17.5, profile.getSpot(3), 0.01);
        assertEquals("4th spot should be 12.5%", 12.5, profile.getSpot(4), 0.01);
        assertEquals("5th spot should be 5%", 5.0, profile.getSpot(5), 0.01);
    }

    @Test
    public void should_ApplyOnlyRequestedSpots_WhenFewerThanPreset() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply STANDARD preset (5 spots) but only request 3
        PayoutPreset.STANDARD.applyToProfile(profile, 3);

        // Verify only 3 spots were set
        assertEquals("1st spot should be 40%", 40.0, profile.getSpot(1), 0.01);
        assertEquals("2nd spot should be 25%", 25.0, profile.getSpot(2), 0.01);
        assertEquals("3rd spot should be 17.5%", 17.5, profile.getSpot(3), 0.01);
        assertEquals("4th spot should be 0 (not set)", 0.0, profile.getSpot(4), 0.01);
    }

    @Test
    public void should_ApplyAllPresetSpots_WhenMoreSpotsRequested() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Apply TOP_HEAVY preset (3 spots) but request 5 spots
        PayoutPreset.TOP_HEAVY.applyToProfile(profile, 5);

        // Verify only preset's 3 spots were set
        assertEquals("1st spot should be 50%", 50.0, profile.getSpot(1), 0.01);
        assertEquals("2nd spot should be 30%", 30.0, profile.getSpot(2), 0.01);
        assertEquals("3rd spot should be 20%", 20.0, profile.getSpot(3), 0.01);
        assertEquals("4th spot should be 0 (preset doesn't define it)", 0.0, profile.getSpot(4), 0.01);
        assertEquals("5th spot should be 0 (preset doesn't define it)", 0.0, profile.getSpot(5), 0.01);
    }

    @Test(expected = IllegalStateException.class)
    public void should_ThrowException_WhenApplyingCustomPreset() {
        TournamentProfile profile = new TournamentProfile("Test");

        // Attempting to apply CUSTOM preset should throw exception
        PayoutPreset.CUSTOM.applyToProfile(profile, 3);
    }

    @Test
    public void should_IdentifyTopHeavyPreset_FromFirstSpot() {
        assertEquals("50% should identify TOP_HEAVY", PayoutPreset.TOP_HEAVY, PayoutPreset.fromFirstSpot(50.0));
    }

    @Test
    public void should_IdentifyStandardPreset_FromFirstSpot() {
        assertEquals("40% should identify STANDARD", PayoutPreset.STANDARD, PayoutPreset.fromFirstSpot(40.0));
    }

    @Test
    public void should_IdentifyFlatPreset_FromFirstSpot() {
        assertEquals("25% should identify FLAT", PayoutPreset.FLAT, PayoutPreset.fromFirstSpot(25.0));
    }

    @Test
    public void should_ReturnCustom_WhenNoPresetMatches() {
        assertEquals("35% should return CUSTOM (no match)", PayoutPreset.CUSTOM, PayoutPreset.fromFirstSpot(35.0));
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
        assertEquals("Custom", PayoutPreset.CUSTOM.toString());
        assertEquals("Top-Heavy (~50% winner)", PayoutPreset.TOP_HEAVY.toString());
        assertEquals("Standard (~40% winner)", PayoutPreset.STANDARD.toString());
        assertEquals("Flat (~25% winner)", PayoutPreset.FLAT.toString());
    }
}
