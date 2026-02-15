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
package com.donohoedigital.games.poker.core;

/**
 * Available actions and constraints for a player decision. Immutable record
 * passed to PlayerActionProvider.
 *
 * @param canCheck
 *            true if player can check
 * @param canCall
 *            true if player can call
 * @param canBet
 *            true if player can bet
 * @param canRaise
 *            true if player can raise
 * @param canFold
 *            true if player can fold
 * @param callAmount
 *            amount required to call (0 if can check)
 * @param minBet
 *            minimum bet amount
 * @param maxBet
 *            maximum bet amount (chip count if no limit)
 * @param minRaise
 *            minimum raise amount
 * @param maxRaise
 *            maximum raise amount (chip count if no limit)
 * @param timeoutSeconds
 *            seconds before auto-fold (0 for no timeout)
 */
public record ActionOptions(boolean canCheck, boolean canCall, boolean canBet, boolean canRaise, boolean canFold,
        int callAmount, int minBet, int maxBet, int minRaise, int maxRaise, int timeoutSeconds) {
}
