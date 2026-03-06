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
package com.donohoedigital.games.poker.protocol.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Aggregated tournament statistics for a player.
 *
 * @param totalTournaments
 *            Total number of tournaments played
 * @param totalWins
 *            Number of 1st place finishes
 * @param totalPrize
 *            Sum of all prizes won
 * @param totalSpent
 *            Sum of all buy-ins, rebuys, and add-ons
 * @param netProfit
 *            Total prize minus total spent
 * @param avgFinish
 *            Average finishing position
 * @param avgROI
 *            Average return on investment as a percentage
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OverallStatsData(int totalTournaments, int totalWins, int totalPrize, int totalSpent, int netProfit,
        double avgFinish, double avgROI) {
}
