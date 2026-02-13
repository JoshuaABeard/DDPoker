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
 * Types of validation warnings for tournament profiles.
 *
 * <p>
 * These are soft warnings that don't prevent profile creation but indicate
 * potentially problematic configurations.
 */
public enum ValidationWarning {
    /**
     * Rebuy period ends before last blind level is reached.
     */
    UNREACHABLE_LEVELS,

    /**
     * More payout spots configured than available players.
     */
    TOO_MANY_PAYOUT_SPOTS,

    /**
     * Starting stack depth is less than 10 big blinds.
     */
    SHALLOW_STARTING_DEPTH,

    /**
     * House take exceeds 20% of buy-in.
     */
    EXCESSIVE_HOUSE_TAKE
}
