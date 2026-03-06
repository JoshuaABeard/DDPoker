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
 * Predefined blind structure templates for quick tournament setup.
 *
 * <p>
 * Client-side version of {@code BlindTemplate} that uses
 * {@link ClientTournamentProfile} instead of the engine's
 * {@code TournamentProfile}.
 */
public enum ClientBlindTemplate {
    SLOW("Slow (x1.5, 20min)", 1.5, 20, new int[]{25, 50}), STANDARD("Standard (x2.0, 15min)", 2.0, 15,
            new int[]{25, 50}), TURBO("Turbo (x2.0, 10min)", 2.0, 10,
                    new int[]{25, 50}), HYPER("Hyper (x2.0, 5min)", 2.0, 5, new int[]{25, 50});

    private final String displayName;
    private final double progression;
    private final int minutesPerLevel;
    private final int[] startingBlinds;

    ClientBlindTemplate(String displayName, double progression, int minutesPerLevel, int[] startingBlinds) {
        this.displayName = displayName;
        this.progression = progression;
        this.minutesPerLevel = minutesPerLevel;
        this.startingBlinds = startingBlinds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getProgression() {
        return progression;
    }

    public int getMinutesPerLevel() {
        return minutesPerLevel;
    }

    public int[] getStartingBlinds() {
        return startingBlinds.clone();
    }

    /**
     * Generate blind levels and apply to a tournament profile.
     */
    public void generateLevels(ClientTournamentProfile profile, int numLevels, boolean includeBreaks,
            int breakFrequency) {
        if (numLevels < 1 || numLevels > ClientTournamentProfile.MAX_LEVELS) {
            throw new IllegalArgumentException(
                    "Number of levels must be between 1 and " + ClientTournamentProfile.MAX_LEVELS);
        }

        if (includeBreaks && breakFrequency < 1) {
            throw new IllegalArgumentException("Break frequency must be >= 1");
        }

        profile.clearAllLevels();
        profile.setMinutesPerLevel(minutesPerLevel);

        int small = startingBlinds[0];
        int big = startingBlinds[1];
        int ante = 0;

        int currentLevel = 1;
        int blindsSinceBreak = 0;

        for (int i = 1; i <= numLevels; i++) {
            if (i >= 5 && ante == 0) {
                ante = Math.max(1, small / 5);
            }

            profile.setLevel(currentLevel, ante, small, big, minutesPerLevel);
            currentLevel++;
            blindsSinceBreak++;

            if (includeBreaks && blindsSinceBreak == breakFrequency && i < numLevels) {
                profile.setBreak(currentLevel, 15);
                currentLevel++;
                blindsSinceBreak = 0;
            }

            small = roundBlind((int) (small * progression));
            big = roundBlind((int) (big * progression));
            if (ante > 0) {
                ante = roundBlind((int) (ante * progression));
            }
        }

        profile.fixLevels();
    }

    private int roundBlind(int value) {
        if (value < 10)
            return value;
        if (value < 50)
            return (value / 5) * 5;
        if (value < 100)
            return (value / 10) * 10;
        if (value < 500)
            return (value / 25) * 25;
        if (value < 1000)
            return (value / 50) * 50;
        if (value < 5000)
            return (value / 100) * 100;
        if (value < 10000)
            return (value / 250) * 250;
        if (value < 50000)
            return (value / 500) * 500;

        return (value / 1000) * 1000;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
