/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

/**
 * Record of a single player action during a hand. Replaces HandAction (which
 * extends DMArrayList and has DataMarshal serialization).
 *
 * @param player
 *            player who took the action
 * @param round
 *            betting round (0=preflop, 1=flop, 2=turn, 3=river)
 * @param action
 *            action type (ACTION_FOLD, ACTION_CHECK, etc.)
 * @param amount
 *            total amount of the action
 * @param subAmount
 *            call portion for raises
 * @param allIn
 *            true if this action put the player all-in
 */
public record ServerHandAction(ServerPlayer player, int round, int action, int amount, int subAmount, boolean allIn) {

    // Action type constants (same values as HandAction in pokerengine)
    public static final int ACTION_NONE = -1;
    public static final int ACTION_FOLD = 0;
    public static final int ACTION_CHECK = 1;
    public static final int ACTION_CALL = 2;
    public static final int ACTION_BET = 3;
    public static final int ACTION_RAISE = 4;
}
