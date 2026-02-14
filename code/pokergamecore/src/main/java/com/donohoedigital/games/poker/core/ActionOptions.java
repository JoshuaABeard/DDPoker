/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
