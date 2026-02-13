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

/**
 * Predefined payout distribution presets for tournament profiles.
 *
 * <p>
 * Provides quick-apply payout structures that match common tournament formats:
 * <ul>
 * <li>TOP_HEAVY: Winner takes ~50%, runners-up split remainder
 * <li>STANDARD: Winner takes ~40%, more gradual distribution
 * <li>FLAT: Winner takes ~25%, very even distribution
 * <li>CUSTOM: User-defined distribution (no preset)
 * </ul>
 */
public enum PayoutPreset {
    /**
     * Custom/user-defined payout distribution.
     */
    CUSTOM("Custom", null),

    /**
     * Top-heavy distribution: ~50% to winner, steep drop-off.
     */
    TOP_HEAVY("Top-Heavy (~50% winner)", new double[]{50.0, 30.0, 20.0}),

    /**
     * Standard distribution: ~40% to winner, gradual taper.
     */
    STANDARD("Standard (~40% winner)", new double[]{40.0, 25.0, 17.5, 12.5, 5.0}),

    /**
     * Flat distribution: ~25% to winner, very even payouts.
     */
    FLAT("Flat (~25% winner)", new double[]{25.0, 20.0, 15.0, 12.5, 10.0, 7.5, 5.0, 5.0});

    private final String displayName;
    private final double[] percentages;

    PayoutPreset(String displayName, double[] percentages) {
        this.displayName = displayName;
        this.percentages = percentages;
    }

    /**
     * Get the display name for this preset.
     *
     * @return Human-readable name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the percentage distribution for this preset.
     *
     * @return Defensive copy of percentages array for each payout spot, or null for
     *         CUSTOM
     */
    public double[] getPercentages() {
        return percentages == null ? null : percentages.clone();
    }

    /**
     * Check if this preset has a defined distribution.
     *
     * @return true if percentages are defined (not CUSTOM)
     */
    public boolean hasDistribution() {
        return percentages != null;
    }

    /**
     * Get the number of payout spots in this preset's distribution.
     *
     * @return Number of spots, or 0 for CUSTOM
     */
    public int getSpotCount() {
        return percentages == null ? 0 : percentages.length;
    }

    /**
     * Apply this preset's payout distribution to a tournament profile.
     *
     * <p>
     * Sets the payout percentages for the number of spots defined by this preset.
     * If the preset has fewer spots than requested, only the preset's spots are
     * set. If the preset has more spots than requested, only the requested number
     * are set.
     *
     * @param profile
     *            The tournament profile to update
     * @param numSpots
     *            The number of payout spots to configure
     * @throws IllegalStateException
     *             if called on CUSTOM preset (no distribution to apply)
     */
    public void applyToProfile(TournamentProfile profile, int numSpots) {
        if (!hasDistribution()) {
            throw new IllegalStateException("Cannot apply CUSTOM preset - no distribution defined");
        }

        int spotsToSet = Math.min(numSpots, percentages.length);

        for (int i = 1; i <= spotsToSet; i++) {
            // Use existing spot storage pattern: PARAM_SPOTAMOUNT + position
            profile.setSpot(i, percentages[i - 1]);
        }

        // If fewer spots requested than preset has, just set what was requested
        // If more spots requested, the remaining spots keep their existing values
    }

    /**
     * Get the preset that best matches the given percentages.
     *
     * <p>
     * Compares the first spot's percentage to identify the preset. Returns CUSTOM
     * if no preset matches.
     *
     * @param firstSpotPercent
     *            The percentage for the first place payout
     * @return The matching preset, or CUSTOM if no match
     */
    public static PayoutPreset fromFirstSpot(double firstSpotPercent) {
        // Simple matching based on first spot percentage with tolerance
        for (PayoutPreset preset : values()) {
            if (preset.hasDistribution() && Math.abs(preset.percentages[0] - firstSpotPercent) < 0.1) {
                return preset;
            }
        }
        return CUSTOM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
