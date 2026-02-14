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
}
