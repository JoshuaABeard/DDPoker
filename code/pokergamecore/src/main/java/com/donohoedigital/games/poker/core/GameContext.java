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
 * Interface for game-level operations that the core engine needs. Implemented
 * by PokerGame in Phase 2.
 */
public interface GameContext {
    /** @return total number of tables in the tournament */
    int getNumTables();

    /**
     * Get a specific table.
     *
     * @param index
     *            table index (0-based)
     * @return the table at that index
     */
    GameTable getTable(int index);

    /** @return total number of players in the tournament */
    int getNumPlayers();

    /**
     * Get a player by their ID.
     *
     * @param playerId
     *            player's unique ID
     * @return player info, or null if not found
     */
    GamePlayerInfo getPlayerByID(int playerId);

    /** @return true if this is a practice game (offline, single player) */
    boolean isPractice();

    /** @return true if this is an online multiplayer game */
    boolean isOnlineGame();

    /** @return true if the tournament is over */
    boolean isGameOver();
}
