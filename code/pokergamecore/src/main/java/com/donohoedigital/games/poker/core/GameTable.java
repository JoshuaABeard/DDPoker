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

import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Interface for table operations that the core game engine needs. Implemented
 * by PokerTable in Phase 2.
 */
public interface GameTable {
    // State management

    /** @return table number/ID */
    int getNumber();

    /** @return current table state */
    TableState getTableState();

    /**
     * @param state
     *            the new table state
     */
    void setTableState(TableState state);

    /** @return pending table state (for state transitions) */
    TableState getPendingTableState();

    /**
     * @param state
     *            the pending table state
     */
    void setPendingTableState(TableState state);

    /** @return previous table state */
    TableState getPreviousTableState();

    /** @return name of pending phase to run */
    String getPendingPhase();

    /**
     * @param phase
     *            name of phase to run
     */
    void setPendingPhase(String phase);

    // Player access

    /** @return total number of seats at table */
    int getSeats();

    /** @return number of occupied seats */
    int getNumOccupiedSeats();

    /**
     * Get player at a specific seat.
     *
     * @param seat
     *            seat number (0-based)
     * @return player info, or null if seat is empty
     */
    GamePlayerInfo getPlayer(int seat);

    // Button

    /** @return current button position (seat number) */
    int getButton();

    /**
     * @param seat
     *            new button position
     */
    void setButton(int seat);

    /** Deal cards to determine initial button position */
    void setButton();

    /** @return next occupied seat after the button */
    int getNextSeatAfterButton();

    /**
     * Get the next occupied seat after the given seat.
     *
     * @param seat
     *            starting seat
     * @return next occupied seat, wrapping around table
     */
    int getNextSeat(int seat);

    // Hand

    /** @return current hand number */
    int getHandNum();

    /**
     * @param handNum
     *            new hand number
     */
    void setHandNum(int handNum);

    /** @return current tournament level */
    int getLevel();

    /**
     * @param level
     *            new tournament level
     */
    void setLevel(int level);

    /** @return minimum chip denomination */
    int getMinChip();

    /** @return current hold'em hand, or null if no hand in progress */
    GameHand getHoldemHand();

    /**
     * @param hand
     *            the current hold'em hand
     */
    void setHoldemHand(GameHand hand);

    // Configuration

    /** @return true if table should auto-deal next hand */
    boolean isAutoDeal();

    /** @return true if this is the current table (in UI) */
    boolean isCurrent();

    // Player management operations

    /** Process AI rebuys at this table */
    void processAIRebuys();

    /** Process AI add-ons at this table */
    void processAIAddOns();

    /** Clear the pending rebuy list */
    void clearRebuyList();

    // Color-up operations

    /**
     * @param minChip
     *            the next minimum chip denomination
     */
    void setNextMinChip(int minChip);

    /** Determine if color-up is needed and mark table accordingly */
    void doColorUpDetermination();

    /** @return true if table is in the process of coloring up */
    boolean isColoringUp();

    /** Perform the color-up operation */
    void colorUp();

    /** Finalize the color-up operation */
    void colorUpFinish();

    // Hand operations

    /** Start a break period at this table */
    void startBreak();

    /** Start a new hand at this table */
    void startNewHand();

    /** @return true if table is in zip mode (fast play) */
    boolean isZipMode();

    /**
     * Set zip mode (fast play).
     *
     * @param zipMode
     *            true to enable zip mode
     */
    void setZipMode(boolean zipMode);

    // Wait list operations (for online showdown coordination)

    /** Remove all players from wait list */
    void removeWaitAll();

    /**
     * Add a player to the wait list.
     *
     * @param player
     *            player to add to wait list
     */
    void addWait(GamePlayerInfo player);

    /** @return number of players in wait list */
    int getWaitSize();

    /**
     * Get player from wait list by index.
     *
     * @param index
     *            index in wait list (0-based)
     * @return player at that index
     */
    GamePlayerInfo getWaitPlayer(int index);

    /** @return milliseconds since last table state change */
    long getMillisSinceLastStateChange();

    /**
     * Set pause time before next state transition.
     *
     * @param millis
     *            milliseconds to pause
     */
    void setPause(int millis);

    /** @return delay in milliseconds for auto-deal pause */
    int getAutoDealDelay();

    /** Quickly simulate hand for all-AI table (skip UI phases) */
    void simulateHand();

    // Table cleanup operations

    /**
     * Get list of players added to this table during cleanup.
     *
     * @return list of added players
     */
    java.util.List<GamePlayerInfo> getAddedPlayersList();

    /** @return true if this table was removed during consolidation */
    boolean isRemoved();

    /** @return true if this table has only computer players */
    boolean isAllComputer();

    // === V2 AI Support ===

    /**
     * Get maximum number of players (total seats). Alias for getSeats() for V2 AI
     * compatibility.
     *
     * @return maximum players
     */
    default int getMaxPlayers() {
        return getSeats();
    }

    /**
     * Get seat number for a player (reverse lookup).
     *
     * @param player
     *            the player to find
     * @return seat number (0-based), or -1 if player not at table
     */
    default int getSeat(GamePlayerInfo player) {
        for (int i = 0; i < getSeats(); i++) {
            GamePlayerInfo p = getPlayer(i);
            if (p != null && p.equals(player)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get all players still in current hand (not folded), optionally excluding one
     * player.
     *
     * @param excludePlayer
     *            player to exclude from list, or null to include all
     * @return list of remaining players
     */
    default java.util.List<GamePlayerInfo> getPlayersLeft(GamePlayerInfo excludePlayer) {
        java.util.List<GamePlayerInfo> players = new java.util.ArrayList<>();
        GameHand hand = getHoldemHand();
        if (hand == null)
            return players;

        for (int i = 0; i < getSeats(); i++) {
            GamePlayerInfo p = getPlayer(i);
            if (p != null && !p.isFolded() && (excludePlayer == null || !p.equals(excludePlayer))) {
                players.add(p);
            }
        }
        return players;
    }
}
