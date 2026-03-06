/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
 *
 * The "DD Poker" and "Donohoe Digital" names and logos, as well as any images,
 * graphics, text, and documentation found in this repository (including but not
 * limited to written documentation, website content, and marketing materials)
 * are licensed under the Creative Commons Attribution-NonCommercial-NoDerivatives
 * 4.0 International License (CC BY-NC-ND 4.0). You may not use these assets
 * without explicit written permission for any uses not covered by this License.
 * For the full License text, please see the LICENSE-CREATIVE-COMMONS.txt file
 * in the root directory of this project.
 *
 * For inquiries regarding commercial licensing of this source code or
 * the use of names, logos, images, text, or other assets, please contact
 * doug [at] donohoe [dot] info.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.protocol.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Client-safe tournament history data, replacing the pokerengine
 * TournamentHistory JPA entity for client display.
 *
 * @param gameId
 *            Game ID
 * @param place
 *            Finishing place
 * @param tournamentName
 *            Tournament name
 * @param tournamentType
 *            Tournament type
 * @param startDate
 *            Tournament start date
 * @param endDate
 *            Tournament end date (null if still running)
 * @param buyin
 *            Buy-in cost
 * @param rebuys
 *            Total rebuy cost
 * @param addons
 *            Total add-on cost
 * @param prize
 *            Prize won
 * @param numPlayers
 *            Total number of players
 * @param numRemaining
 *            Number of players remaining
 * @param ended
 *            Whether the tournament has ended
 * @param numChips
 *            Current chip count
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TournamentHistoryData(Long gameId, int place, String tournamentName, String tournamentType,
        Instant startDate, Instant endDate, int buyin, int rebuys, int addons, int prize, int numPlayers,
        int numRemaining, boolean ended, int numChips) {

    /**
     * Total amount spent (buy-in + rebuys + add-ons).
     */
    public int totalSpent() {
        return buyin + rebuys + addons;
    }

    /**
     * Net result (prize - total spent).
     */
    public int net() {
        return prize - totalSpent();
    }
}
