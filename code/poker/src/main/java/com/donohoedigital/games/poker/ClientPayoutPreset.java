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
package com.donohoedigital.games.poker;

/**
 * Predefined payout distribution presets for tournament profiles.
 *
 * <p>
 * Client-side version of {@code PayoutPreset} that uses
 * {@link ClientTournamentProfile} instead of the engine's
 * {@code TournamentProfile}.
 */
public enum ClientPayoutPreset {
    CUSTOM("Custom", null), TOP_HEAVY("Top-Heavy (~50% winner)", new double[]{50.0, 30.0, 20.0}), STANDARD(
            "Standard (~40% winner)", new double[]{40.0, 25.0, 17.5, 12.5, 5.0}), FLAT("Flat (~25% winner)",
                    new double[]{25.0, 20.0, 15.0, 12.5, 10.0, 7.5, 5.0, 5.0});

    private final String displayName;
    private final double[] percentages;

    ClientPayoutPreset(String displayName, double[] percentages) {
        this.displayName = displayName;
        this.percentages = percentages;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double[] getPercentages() {
        return percentages == null ? null : percentages.clone();
    }

    public boolean hasDistribution() {
        return percentages != null;
    }

    public int getSpotCount() {
        return percentages == null ? 0 : percentages.length;
    }

    /**
     * Apply this preset's payout distribution to a tournament profile.
     */
    public void applyToProfile(ClientTournamentProfile profile, int numSpots) {
        if (!hasDistribution()) {
            throw new IllegalStateException("Cannot apply CUSTOM preset - no distribution defined");
        }

        profile.setAlloc(PokerClientConstants.ALLOC_PERC);

        int spotsToSet = Math.min(numSpots, percentages.length);

        for (int i = 1; i <= spotsToSet; i++) {
            profile.setSpot(i, percentages[i - 1]);
        }
    }

    public static ClientPayoutPreset fromFirstSpot(double firstSpotPercent) {
        for (ClientPayoutPreset preset : values()) {
            if (preset.hasDistribution() && Math.abs(preset.percentages[0] - firstSpotPercent) < 0.1) {
                return preset;
            }
        }
        return CUSTOM;
    }

    public static ClientPayoutPreset fromDistribution(double[] distribution) {
        if (distribution == null || distribution.length == 0) {
            return CUSTOM;
        }

        for (ClientPayoutPreset preset : values()) {
            if (preset.hasDistribution() && preset.matchesDistribution(distribution)) {
                return preset;
            }
        }
        return CUSTOM;
    }

    private boolean matchesDistribution(double[] distribution) {
        if (percentages.length != distribution.length) {
            return false;
        }

        for (int i = 0; i < percentages.length; i++) {
            if (Math.abs(percentages[i] - distribution[i]) >= 0.1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
