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
 * Interface for obtaining player decisions. The core engine calls this when a
 * player must act. Implementations may:
 *
 * <ul>
 * <li>Show UI to a human player and wait for input (Swing)
 * <li>Invoke AI strategy logic (AI player)
 * <li>Wait for a network message (server-hosted game)
 * </ul>
 *
 * <p>
 * Implementations are expected to handle timeouts internally and return a
 * default action (e.g., fold) if the player doesn't respond in time.
 */
public interface PlayerActionProvider {
    /**
     * Get the player's action.
     *
     * @param player
     *            the player who must act
     * @param options
     *            available actions and constraints
     * @return the player's decision, or null if delegating to existing code path
     *         (Phase 2: null return triggers fallback; Phase 3: never null)
     */
    PlayerAction getAction(GamePlayerInfo player, ActionOptions options);
}
