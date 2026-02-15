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
 * Interface for tournament-level operations that the core engine needs.
 * Implemented by PokerGame in Phase 2.
 *
 * Renamed from GameContext to TournamentContext to avoid name collision with
 * com.donohoedigital.games.engine.GameContext.
 */
public interface TournamentContext {
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

    // Level and timing management

    /** @return current tournament level (0-based) */
    int getLevel();

    /** Advance to the next tournament level */
    void nextLevel();

    /** @return true if the current level's time has expired */
    boolean isLevelExpired();

    /** Advance the tournament clock during a break */
    void advanceClockBreak();

    /** Start the game clock (for online games) */
    void startGameClock();

    // Chip denomination management

    /** @return the previous minimum chip denomination */
    int getLastMinChip();

    /** @return the current minimum chip denomination */
    int getMinChip();

    // Clock operations

    /** Advance the tournament clock (for practice games) */
    void advanceClock();

    // Tournament profile queries

    /**
     * Check if a level is a break period.
     *
     * @param level
     *            the level to check
     * @return true if the level is a break
     */
    boolean isBreakLevel(int level);

    /** @return the local player (this client's player) */
    GamePlayerInfo getLocalPlayer();

    // Tournament profile queries

    /** @return true if scheduled start is enabled */
    boolean isScheduledStartEnabled();

    /** @return scheduled start time in milliseconds since epoch */
    long getScheduledStartTime();

    /** @return minimum players required for scheduled start */
    int getMinPlayersForScheduledStart();

    /**
     * Get timeout for a specific betting round.
     *
     * @param round
     *            the betting round (legacy int value)
     * @return timeout in seconds
     */
    int getTimeoutForRound(int round);

    /** @return the current table (the one being displayed in UI) */
    GameTable getCurrentTable();

    /** @return default timeout in seconds for player actions */
    int getTimeoutSeconds();

    /** @return true if only one player has chips remaining */
    boolean isOnePlayerLeft();

    // Blind level queries (added for AI support in Phase 7)

    /**
     * Get the small blind amount for a specific level.
     *
     * @param level
     *            the tournament level (1-based)
     * @return small blind amount in chips
     */
    int getSmallBlind(int level);

    /**
     * Get the big blind amount for a specific level.
     *
     * @param level
     *            the tournament level (1-based)
     * @return big blind amount in chips
     */
    int getBigBlind(int level);

    /**
     * Get the ante amount for a specific level.
     *
     * @param level
     *            the tournament level (1-based)
     * @return ante amount in chips (0 if no ante)
     */
    int getAnte(int level);

    /**
     * Get the starting chip count for the tournament.
     * <p>
     * This is the buy-in chip amount that players start with. Used by AI to
     * determine addon value.
     *
     * @return starting chips per player
     */
    int getStartingChips();

    /**
     * Check if rebuy period is still active for a player.
     * <p>
     * During rebuy period, players can rebuy if eliminated or their stack falls
     * below a threshold. This affects AI strategy - the AI plays looser during
     * rebuy period (adjusts tight factor by -20).
     * <p>
     * Extracted from V1Player line 190:
     * {@code !player.getTable().isRebuyDone(player)}
     *
     * @param player
     *            Player to check rebuy status for
     * @return {@code true} if rebuy period is still active for this player
     */
    boolean isRebuyPeriodActive(GamePlayerInfo player);
}
