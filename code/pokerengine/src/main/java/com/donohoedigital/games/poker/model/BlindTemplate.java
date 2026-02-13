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
 * Predefined blind structure templates for quick tournament setup.
 *
 * <p>
 * Provides templates that generate progressive blind schedules with
 * configurable breaks:
 * <ul>
 * <li>SLOW: 1.5x progression, 20 minutes per level
 * <li>STANDARD: 2x progression, 15 minutes per level
 * <li>TURBO: 2x progression, 10 minutes per level
 * <li>HYPER: 2x progression, 5 minutes per level
 * </ul>
 */
public enum BlindTemplate {
    /**
     * Slow progression: 1.5x multiplier, 20 minutes per level.
     */
    SLOW("Slow (x1.5, 20min)", 1.5, 20, new int[]{25, 50}),

    /**
     * Standard progression: 2x multiplier, 15 minutes per level.
     */
    STANDARD("Standard (x2.0, 15min)", 2.0, 15, new int[]{25, 50}),

    /**
     * Turbo progression: 2x multiplier, 10 minutes per level.
     */
    TURBO("Turbo (x2.0, 10min)", 2.0, 10, new int[]{25, 50}),

    /**
     * Hyper-turbo progression: 2x multiplier, 5 minutes per level.
     */
    HYPER("Hyper (x2.0, 5min)", 2.0, 5, new int[]{25, 50});

    private final String displayName;
    private final double progression;
    private final int minutesPerLevel;
    private final int[] startingBlinds; // [small, big]

    BlindTemplate(String displayName, double progression, int minutesPerLevel, int[] startingBlinds) {
        this.displayName = displayName;
        this.progression = progression;
        this.minutesPerLevel = minutesPerLevel;
        this.startingBlinds = startingBlinds;
    }

    /**
     * Get the display name for this template.
     *
     * @return Human-readable name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the blind progression multiplier.
     *
     * @return Multiplier applied to blinds each level
     */
    public double getProgression() {
        return progression;
    }

    /**
     * Get the default minutes per level.
     *
     * @return Minutes for each level
     */
    public int getMinutesPerLevel() {
        return minutesPerLevel;
    }

    /**
     * Get the starting blind values.
     *
     * @return Array of [small blind, big blind]
     */
    public int[] getStartingBlinds() {
        return startingBlinds.clone();
    }

    /**
     * Generate blind levels and apply to a tournament profile.
     *
     * <p>
     * Clears existing levels and generates a progressive blind structure.
     * Optionally inserts breaks at regular intervals.
     *
     * @param profile
     *            The tournament profile to update
     * @param numLevels
     *            Number of blind levels to generate (1-40)
     * @param includeBreaks
     *            Whether to insert breaks between levels
     * @param breakFrequency
     *            Insert break every N levels (only used if includeBreaks is true)
     */
    public void generateLevels(TournamentProfile profile, int numLevels, boolean includeBreaks, int breakFrequency) {
        if (numLevels < 1 || numLevels > TournamentProfile.MAX_LEVELS) {
            throw new IllegalArgumentException(
                    "Number of levels must be between 1 and " + TournamentProfile.MAX_LEVELS);
        }

        if (includeBreaks && breakFrequency < 1) {
            throw new IllegalArgumentException("Break frequency must be >= 1");
        }

        // Clear existing levels
        profile.clearAllLevels();

        // Set default minutes per level
        profile.setMinutesPerLevel(minutesPerLevel);

        int small = startingBlinds[0];
        int big = startingBlinds[1];
        int ante = 0;

        int currentLevel = 1;
        int blindsSinceBreak = 0;

        for (int i = 1; i <= numLevels; i++) {
            // Add antes starting at level 5 (20% of current small blind)
            if (i >= 5 && ante == 0) {
                ante = Math.max(1, small / 5);
            }

            // Set level blinds
            profile.setLevel(currentLevel, ante, small, big, minutesPerLevel);
            currentLevel++;
            blindsSinceBreak++;

            // Insert break after every breakFrequency blind levels
            if (includeBreaks && blindsSinceBreak == breakFrequency && i < numLevels) {
                profile.setBreak(currentLevel, 15); // 15 minute break
                currentLevel++;
                blindsSinceBreak = 0;
            }

            // Apply progression for next level
            small = roundBlind((int) (small * progression));
            big = roundBlind((int) (big * progression));
            if (ante > 0) {
                ante = roundBlind((int) (ante * progression));
            }
        }

        // Normalize levels
        profile.fixLevels();
    }

    /**
     * Round blind value to nice number.
     *
     * <p>
     * Rounds to nearest 5, 10, 25, 50, 100, 250, 500, etc. based on magnitude.
     *
     * @param value
     *            The blind value to round
     * @return Rounded value
     */
    private int roundBlind(int value) {
        if (value < 10)
            return value;
        if (value < 50)
            return (value / 5) * 5; // Round to nearest 5
        if (value < 100)
            return (value / 10) * 10; // Round to nearest 10
        if (value < 500)
            return (value / 25) * 25; // Round to nearest 25
        if (value < 1000)
            return (value / 50) * 50; // Round to nearest 50
        if (value < 5000)
            return (value / 100) * 100; // Round to nearest 100
        if (value < 10000)
            return (value / 250) * 250; // Round to nearest 250
        if (value < 50000)
            return (value / 500) * 500; // Round to nearest 500

        return (value / 1000) * 1000; // Round to nearest 1000
    }

    @Override
    public String toString() {
        return displayName;
    }
}
