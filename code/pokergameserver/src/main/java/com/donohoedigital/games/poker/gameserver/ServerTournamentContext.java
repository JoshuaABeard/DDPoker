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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.model.LevelAdvanceMode;
import com.donohoedigital.games.poker.core.TournamentContext;

/**
 * Server-side tournament context implementation. Implements TournamentContext
 * without Swing dependencies. Manages tournament state: tables, players,
 * levels, and clock.
 *
 * Replaces PokerGame for server-side game hosting.
 */
public class ServerTournamentContext implements TournamentContext {
    private final List<ServerGameTable> tables;
    private final List<ServerPlayer> allPlayers;
    private final Map<Integer, ServerPlayer> playerById;

    // Tournament configuration
    private final int startingChips;
    private final int[] smallBlinds;
    private final int[] bigBlinds;
    private final int[] antes;
    private final int[] levelMinutes;
    private final boolean[] breakLevels;
    private final boolean practice;
    private final int maxRebuys;
    private final int rebuyMaxLevel;
    private final boolean allowAddons;
    private final int timeoutSeconds;
    private final LevelAdvanceMode levelAdvanceMode;
    private final int handsPerLevel;

    // Level management
    private int currentLevel;
    private long levelStartTimeMillis;
    private long totalPauseTimeMillis;
    private int minChip;
    private int lastMinChip;
    private int handsPlayedThisLevel;

    // Clock management
    private long gameStartTimeMillis;
    private int clockAdvanceCount; // For simulating time in practice mode

    // Tournament state
    private boolean gameOver;

    /**
     * Create a new server tournament context (time-based level advancement).
     *
     * @param players
     *            all tournament players
     * @param numTables
     *            number of tables to create
     * @param startingChips
     *            starting chip count per player
     * @param smallBlinds
     *            small blind amounts per level
     * @param bigBlinds
     *            big blind amounts per level
     * @param antes
     *            ante amounts per level
     * @param levelMinutes
     *            duration of each level in minutes
     * @param breakLevels
     *            true for break levels
     * @param practice
     *            true for practice mode (offline)
     * @param maxRebuys
     *            maximum rebuys allowed per player
     * @param rebuyMaxLevel
     *            last level where rebuys are allowed
     * @param allowAddons
     *            true if add-ons are allowed
     * @param timeoutSeconds
     *            player action timeout in seconds
     */
    public ServerTournamentContext(List<ServerPlayer> players, int numTables, int startingChips, int[] smallBlinds,
            int[] bigBlinds, int[] antes, int[] levelMinutes, boolean[] breakLevels, boolean practice, int maxRebuys,
            int rebuyMaxLevel, boolean allowAddons, int timeoutSeconds) {
        this(players, numTables, startingChips, smallBlinds, bigBlinds, antes, levelMinutes, breakLevels, practice,
                maxRebuys, rebuyMaxLevel, allowAddons, timeoutSeconds, LevelAdvanceMode.TIME, 0);
    }

    /**
     * Create a new server tournament context with configurable level advancement.
     *
     * @param players
     *            all tournament players
     * @param numTables
     *            number of tables to create
     * @param startingChips
     *            starting chip count per player
     * @param smallBlinds
     *            small blind amounts per level
     * @param bigBlinds
     *            big blind amounts per level
     * @param antes
     *            ante amounts per level
     * @param levelMinutes
     *            duration of each level in minutes (used when mode is TIME)
     * @param breakLevels
     *            true for break levels
     * @param practice
     *            true for practice mode (offline)
     * @param maxRebuys
     *            maximum rebuys allowed per player
     * @param rebuyMaxLevel
     *            last level where rebuys are allowed
     * @param allowAddons
     *            true if add-ons are allowed
     * @param timeoutSeconds
     *            player action timeout in seconds
     * @param levelAdvanceMode
     *            how levels advance (TIME or HANDS)
     * @param handsPerLevel
     *            number of hands per level (used when mode is HANDS)
     */
    public ServerTournamentContext(List<ServerPlayer> players, int numTables, int startingChips, int[] smallBlinds,
            int[] bigBlinds, int[] antes, int[] levelMinutes, boolean[] breakLevels, boolean practice, int maxRebuys,
            int rebuyMaxLevel, boolean allowAddons, int timeoutSeconds, LevelAdvanceMode levelAdvanceMode,
            int handsPerLevel) {

        this.allPlayers = new ArrayList<>(players);
        this.playerById = new HashMap<>();
        for (ServerPlayer player : players) {
            playerById.put(player.getID(), player);
        }

        this.startingChips = startingChips;
        this.smallBlinds = smallBlinds;
        this.bigBlinds = bigBlinds;
        this.antes = antes;
        this.levelMinutes = levelMinutes;
        this.breakLevels = breakLevels;
        this.practice = practice;
        this.maxRebuys = maxRebuys;
        this.rebuyMaxLevel = rebuyMaxLevel;
        this.allowAddons = allowAddons;
        this.timeoutSeconds = timeoutSeconds;
        this.levelAdvanceMode = levelAdvanceMode != null ? levelAdvanceMode : LevelAdvanceMode.TIME;
        this.handsPerLevel = handsPerLevel;

        // Initialize state
        this.currentLevel = 0;
        this.levelStartTimeMillis = System.currentTimeMillis();
        this.gameStartTimeMillis = System.currentTimeMillis();
        this.totalPauseTimeMillis = 0;
        this.minChip = 1;
        this.lastMinChip = 1;
        this.gameOver = false;
        this.clockAdvanceCount = 0;
        this.handsPlayedThisLevel = 0;

        // Create and populate tables
        this.tables = createTables(players, numTables);
    }

    /**
     * Create tables and distribute players evenly across them.
     */
    private List<ServerGameTable> createTables(List<ServerPlayer> players, int numTables) {
        List<ServerGameTable> newTables = new ArrayList<>();

        for (int i = 0; i < numTables; i++) {
            ServerGameTable table = new ServerGameTable(i, // table number
                    10, // seats (standard 10-seat table)
                    this, smallBlinds[0], bigBlinds[0], antes[0]);
            newTables.add(table);
        }

        // Distribute players evenly across tables
        int tableIndex = 0;
        int seatIndex = 0;
        for (ServerPlayer player : players) {
            ServerGameTable table = newTables.get(tableIndex);

            // Find next empty seat
            while (table.getPlayer(seatIndex) != null) {
                seatIndex++;
                if (seatIndex >= 10) {
                    seatIndex = 0;
                    tableIndex = (tableIndex + 1) % numTables;
                    table = newTables.get(tableIndex);
                }
            }

            table.addPlayer(player, seatIndex);

            // Move to next table (round-robin distribution)
            seatIndex = 0;
            tableIndex = (tableIndex + 1) % numTables;
        }

        return newTables;
    }

    // === TournamentContext Interface Implementation ===

    @Override
    public int getNumTables() {
        return tables.size();
    }

    @Override
    public GameTable getTable(int index) {
        return tables.get(index);
    }

    @Override
    public int getNumPlayers() {
        return allPlayers.size();
    }

    @Override
    public GamePlayerInfo getPlayerByID(int playerId) {
        return playerById.get(playerId);
    }

    @Override
    public boolean isPractice() {
        return practice;
    }

    @Override
    public boolean isOnlineGame() {
        return !practice;
    }

    @Override
    public boolean isGameOver() {
        return gameOver || isOnePlayerLeft();
    }

    @Override
    public int getLevel() {
        return currentLevel;
    }

    @Override
    public void nextLevel() {
        lastMinChip = minChip;
        currentLevel++;
        levelStartTimeMillis = System.currentTimeMillis();
        clockAdvanceCount = 0;
        handsPlayedThisLevel = 0;

        // Update min chip if needed (color-up logic would go here)
        // For now, keep it simple
    }

    @Override
    public boolean isLevelExpired() {
        if (currentLevel >= levelMinutes.length) {
            return false; // Past last level
        }

        // Hands-based advancement
        if (levelAdvanceMode == LevelAdvanceMode.HANDS) {
            return handsPlayedThisLevel >= handsPerLevel;
        }

        // Time-based advancement
        if (practice) {
            // In practice mode, use clock advance count to simulate time
            int minutes = levelMinutes[currentLevel];
            // Assume each advanceClock() represents ~1 minute of game time
            return clockAdvanceCount >= minutes;
        } else {
            // In online mode, use real time
            long elapsedMillis = System.currentTimeMillis() - levelStartTimeMillis - totalPauseTimeMillis;
            long levelDurationMillis = levelMinutes[currentLevel] * 60L * 1000L;
            return elapsedMillis >= levelDurationMillis;
        }
    }

    @Override
    public void advanceClockBreak() {
        // Advance clock during break
        clockAdvanceCount++;
    }

    @Override
    public void startGameClock() {
        this.gameStartTimeMillis = System.currentTimeMillis();
        this.levelStartTimeMillis = System.currentTimeMillis();
    }

    @Override
    public int getLastMinChip() {
        return lastMinChip;
    }

    @Override
    public int getMinChip() {
        return minChip;
    }

    @Override
    public void advanceClock() {
        // Advance clock in practice mode
        clockAdvanceCount++;
    }

    /**
     * Increment hands played this level (for hands-based level advancement).
     */
    public void incrementHandsPlayed() {
        handsPlayedThisLevel++;
    }

    @Override
    public boolean isBreakLevel(int level) {
        if (level < 0 || level >= breakLevels.length) {
            return false;
        }
        return breakLevels[level];
    }

    @Override
    public GamePlayerInfo getLocalPlayer() {
        // For server, local player is the first human player
        for (ServerPlayer player : allPlayers) {
            if (player.isHuman()) {
                return player;
            }
        }
        return null;
    }

    @Override
    public boolean isScheduledStartEnabled() {
        // Scheduled start not supported in M1
        return false;
    }

    @Override
    public long getScheduledStartTime() {
        return 0;
    }

    @Override
    public int getMinPlayersForScheduledStart() {
        return 0;
    }

    @Override
    public int getTimeoutForRound(int round) {
        // For M1, same timeout for all rounds
        return timeoutSeconds;
    }

    @Override
    public GameTable getCurrentTable() {
        // For server, return first table by default
        return tables.isEmpty() ? null : tables.get(0);
    }

    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public boolean isOnePlayerLeft() {
        int playersWithChips = 0;
        for (ServerPlayer player : allPlayers) {
            if (player.getChipCount() > 0) {
                playersWithChips++;
                if (playersWithChips > 1) {
                    return false;
                }
            }
        }
        return playersWithChips == 1;
    }

    @Override
    public int getSmallBlind(int level) {
        if (level < 0 || level >= smallBlinds.length) {
            return 0;
        }
        return smallBlinds[level];
    }

    @Override
    public int getBigBlind(int level) {
        if (level < 0 || level >= bigBlinds.length) {
            return 0;
        }
        return bigBlinds[level];
    }

    @Override
    public int getAnte(int level) {
        if (level < 0 || level >= antes.length) {
            return 0;
        }
        return antes[level];
    }

    @Override
    public int getStartingChips() {
        return startingChips;
    }

    @Override
    public boolean isRebuyPeriodActive(GamePlayerInfo player) {
        // Rebuy period is active if:
        // 1. Current level is <= rebuy max level
        // 2. Player has not exceeded max rebuys

        if (currentLevel > rebuyMaxLevel) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        return serverPlayer.getNumRebuys() < maxRebuys;
    }

    // === Additional Methods ===

    /**
     * Mark the tournament as over.
     *
     * @param gameOver
     *            true if game is over
     */
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    /**
     * Set the minimum chip denomination.
     *
     * @param minChip
     *            the minimum chip value
     */
    public void setMinChip(int minChip) {
        this.minChip = minChip;
    }

    /**
     * Get all players in the tournament.
     *
     * @return list of all players
     */
    public List<ServerPlayer> getAllPlayers() {
        return new ArrayList<>(allPlayers);
    }

    /**
     * Get all tables in the tournament.
     *
     * @return list of all tables
     */
    public List<ServerGameTable> getAllTables() {
        return new ArrayList<>(tables);
    }

    /**
     * Get the number of levels configured.
     *
     * @return number of levels
     */
    public int getNumLevels() {
        return smallBlinds.length;
    }

    /**
     * Check if add-ons are allowed.
     *
     * @return true if add-ons allowed
     */
    public boolean isAllowAddons() {
        return allowAddons;
    }

    /**
     * Get maximum rebuys per player.
     *
     * @return max rebuys
     */
    public int getMaxRebuys() {
        return maxRebuys;
    }

    @Override
    public String toString() {
        return "ServerTournamentContext{" + "tables=" + tables.size() + ", players=" + allPlayers.size() + ", level="
                + currentLevel + ", practice=" + practice + ", gameOver=" + gameOver + "}";
    }
}
