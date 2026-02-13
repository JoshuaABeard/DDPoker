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
 * Determines how tournament levels advance - by time or by number of hands
 * played.
 */
public enum LevelAdvanceMode {
    /**
     * Levels advance based on time (minutes per level).
     */
    TIME("Time"),

    /**
     * Levels advance based on number of hands played.
     */
    HANDS("Hands");

    private final String displayName;

    LevelAdvanceMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Parse from string value (for serialization).
     *
     * @param value
     *            the string value
     * @return the corresponding mode, or TIME if null/invalid
     */
    public static LevelAdvanceMode fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return TIME;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TIME;
        }
    }
}
