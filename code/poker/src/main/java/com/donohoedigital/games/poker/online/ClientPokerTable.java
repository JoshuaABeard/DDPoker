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
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.event.PokerTableListener;
import com.donohoedigital.games.poker.model.TournamentProfile;

import java.util.List;

/**
 * Read-only view of a poker table for Swing UI components.
 *
 * <p>
 * Implemented by {@link RemotePokerTable} (WebSocket-driven) and by
 * {@code PokerTable} (local game engine). UI code depends on this interface,
 * not on the concrete game-engine class.
 *
 * <p>
 * Methods were added incrementally by compiler-driven discovery in Task 11.
 */
public interface ClientPokerTable {

    /** Sentinel value for "no seat assigned". */
    int NO_SEAT = -1;

    // -------------------------------------------------------------------------
    // Identity and structure
    // -------------------------------------------------------------------------

    /** Returns the table number / ID. */
    int getNumber();

    /** Returns the human-readable table name (e.g. "Table 1"). */
    String getName();

    /** Returns the game this table belongs to. */
    PokerGame getGame();

    /** Returns the maximum number of seats at this table. */
    int getSeats();

    // -------------------------------------------------------------------------
    // Seat / player access
    // -------------------------------------------------------------------------

    /**
     * Returns the player at the given seat, or {@code null} if the seat is empty.
     *
     * @param nSeat
     *            0-based seat index
     */
    ClientPlayer getPlayer(int nSeat);

    /** Returns the number of non-empty seats. */
    int getNumOccupiedSeats();

    /** Returns the 0-based seat of the dealer button, or -1 if no button. */
    int getButton();

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the display seat index for a given table seat, adjusting for the
     * local human player's offset so they appear at seat 5.
     *
     * @param nSeat
     *            0-based table seat index
     * @return 0-based display seat index
     */
    int getDisplaySeat(int nSeat);

    /**
     * Returns the table seat index for a given display seat index (reverse of
     * {@link #getDisplaySeat}).
     *
     * @param nDisplaySeat
     *            0-based display seat index
     * @return 0-based table seat index
     */
    int getTableSeat(int nDisplaySeat);

    // -------------------------------------------------------------------------
    // Hand access
    // -------------------------------------------------------------------------

    /** Returns the current hand, or {@code null} if no hand is in progress. */
    ClientHoldemHand getHoldemHand();

    // -------------------------------------------------------------------------
    // Game-play state
    // -------------------------------------------------------------------------

    /** Returns the current level number (1-based). */
    int getLevel();

    /** Returns the minimum chip denomination for the current level. */
    int getMinChip();

    /**
     * Returns the hand number for the current/most-recent hand (0 = no hand dealt
     * yet).
     */
    int getHandNum();

    /**
     * Returns {@code true} if this is the "current" table (the one the local human
     * player is seated at).
     */
    boolean isCurrent();

    /**
     * Returns {@code true} if this table is in zip-mode (fast-forward / no
     * animations).
     */
    boolean isZipMode();

    /**
     * Returns {@code true} if this table is driven by remote WebSocket state rather
     * than the local game engine.
     */
    boolean isRemoteTable();

    /**
     * Marks this table as the "current" (active) table for the local player. Called
     * by {@code PokerGame.setCurrentTable}.
     *
     * @param b
     *            {@code true} if this is now the current table
     */
    void setCurrent(boolean b);

    /**
     * Marks this table as removed (eliminated from the tournament). Fires a
     * {@code TYPE_TABLE_REMOVED} event to all registered listeners.
     *
     * @param b
     *            {@code true} to mark the table as removed
     */
    void setRemoved(boolean b);

    /**
     * Returns the number of observers (spectators) at this table.
     */
    int getNumObservers();

    /**
     * Returns the observer at the given index.
     */
    ClientPlayer getObserver(int index);

    /**
     * Returns {@code true} if the given player is allowed to rebuy.
     */
    boolean isRebuyAllowed(ClientPlayer player);

    /**
     * Returns {@code true} if the given player is allowed to rebuy at the given
     * level.
     */
    boolean isRebuyAllowed(ClientPlayer player, int nLevel);

    /** Returns {@code true} if the rebuy period is over for the given player. */
    boolean isRebuyDone(ClientPlayer player);

    /** Returns {@code true} if the given player is allowed to add on. */
    boolean isAddonAllowed(ClientPlayer player);

    /** Returns the list of players who rebuyed during the current level. */
    List<ClientPlayer> getRebuyList();

    /** Returns the list of players who added on during the current level. */
    List<ClientPlayer> getAddonList();

    /** Returns the tournament profile for this table's game. */
    TournamentProfile getProfile();

    /** Sets zip (fast-forward) mode for this table. */
    void setZipMode(boolean b);

    /** Sets the dealer button to the given seat. */
    void setButton(int nSeat);

    /**
     * Adds a player to the next available seat at this table.
     */
    void addPlayer(ClientPlayer player);

    /**
     * Removes the player at the given seat from this table.
     */
    void removePlayer(int nSeat);

    /** Returns the number of open (unoccupied) seats. */
    int getNumOpenSeats();

    /** Returns {@code true} if all occupied seats contain computer players. */
    boolean isAllComputer();

    /** Adds an observer (spectator) to this table. */
    void addObserver(ClientPlayer player);

    /** Removes an observer from this table. */
    void removeObserver(ClientPlayer player);

    /** Sets the minimum chip denomination. */
    void setMinChip(int n);

    /**
     * Notifies table listeners that display preferences have changed.
     */
    void prefsChanged();

    /**
     * Tells this table to skip the next automatic button move (used when the user
     * manually repositions the button at showdown via the cheat menu).
     */
    void setSkipNextButtonMove(boolean b);

    /**
     * Performs a level check — adjusts blinds/antes/min-chip if the game level has
     * changed (local games only).
     */
    void levelCheck(PokerGame game);

    // -------------------------------------------------------------------------
    // Listener management
    // -------------------------------------------------------------------------

    /**
     * Registers a listener for the specified event types.
     *
     * @param l
     *            the listener to add
     * @param nTypes
     *            bitmask of {@code PokerTableEvent.TYPE_*} constants
     */
    void addPokerTableListener(PokerTableListener l, int nTypes);

    /**
     * Removes the listener for the specified event types.
     *
     * @param l
     *            the listener to remove
     * @param nTypes
     *            bitmask of {@code PokerTableEvent.TYPE_*} constants
     */
    void removePokerTableListener(PokerTableListener l, int nTypes);

    /**
     * Dispatches the event to all registered listeners that subscribed to its type.
     *
     * @param event
     *            the event to fire
     */
    void firePokerTableEvent(PokerTableEvent event);
}
