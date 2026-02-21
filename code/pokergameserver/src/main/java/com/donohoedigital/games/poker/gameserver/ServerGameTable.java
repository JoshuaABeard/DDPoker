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
import java.util.List;

import com.donohoedigital.games.poker.core.GameHand;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.GameTable;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.core.state.TableState;

/**
 * Server-side game table implementation. Implements GameTable without Swing
 * dependencies. Manages seat array, button position, and hand lifecycle.
 *
 * Replaces PokerTable for server-side game hosting.
 */
public class ServerGameTable implements GameTable, ServerHand.MockTable {
    private final int tableNumber;
    private final int seats;
    private final ServerPlayer[] players; // Null = empty seat
    private final TournamentContext tournament;

    // Table state
    private TableState tableState = TableState.BEGIN; // Initial state for tournament start
    private TableState pendingTableState;
    private TableState previousTableState;
    private String pendingPhase;

    // Hand state
    private ServerHand currentHand;
    private int handNum;
    private int level;
    private int button = -1; // Dealer button seat
    private int minChip;
    private int nextMinChip;

    // Blinds for next hand
    private final int smallBlindAmount;
    private final int bigBlindAmount;
    private final int anteAmount;

    // Wait list for online coordination
    private final List<ServerPlayer> waitList = new ArrayList<>();
    private final List<ServerPlayer> addedList = new ArrayList<>();

    // Timing
    private long lastStateChangeMillis = System.currentTimeMillis();
    private int pauseMillis;
    private int autoDealDelay = 0;

    // Configuration
    private boolean autoDeal = true;
    private boolean zipMode = false;
    private boolean removed = false;
    private boolean coloringUp = false;

    /**
     * Create a new server game table.
     *
     * @param tableNumber
     *            table identifier
     * @param seats
     *            number of seats (typically 10)
     * @param tournament
     *            tournament context (can be null for testing)
     * @param smallBlindAmount
     *            small blind for next hand
     * @param bigBlindAmount
     *            big blind for next hand
     * @param anteAmount
     *            ante for next hand
     */
    public ServerGameTable(int tableNumber, int seats, TournamentContext tournament, int smallBlindAmount,
            int bigBlindAmount, int anteAmount) {
        this.tableNumber = tableNumber;
        this.seats = seats;
        this.players = new ServerPlayer[seats];
        this.tournament = tournament;
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.anteAmount = anteAmount;
        this.handNum = 0;
        this.level = 0;
        this.minChip = 1;
    }

    // === GameTable Interface Implementation ===

    @Override
    public int getNumber() {
        return tableNumber;
    }

    @Override
    public int getSeats() {
        return seats;
    }

    @Override
    public int getNumOccupiedSeats() {
        int count = 0;
        for (ServerPlayer player : players) {
            if (player != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public ServerPlayer getPlayer(int seat) {
        if (seat < 0 || seat >= seats) {
            return null;
        }
        return players[seat];
    }

    /**
     * Get number of seats (alias for MockTable interface).
     *
     * @return number of seats
     */
    public int getNumSeats() {
        return seats;
    }

    @Override
    public TableState getTableState() {
        return tableState;
    }

    @Override
    public void setTableState(TableState state) {
        if (this.tableState != state) {
            this.previousTableState = this.tableState;
            this.tableState = state;
            this.lastStateChangeMillis = System.currentTimeMillis();
        }
    }

    @Override
    public TableState getPendingTableState() {
        return pendingTableState;
    }

    @Override
    public void setPendingTableState(TableState state) {
        this.pendingTableState = state;
    }

    @Override
    public TableState getPreviousTableState() {
        return previousTableState;
    }

    @Override
    public String getPendingPhase() {
        return pendingPhase;
    }

    @Override
    public void setPendingPhase(String phase) {
        this.pendingPhase = phase;
    }

    @Override
    public int getButton() {
        return button;
    }

    @Override
    public void setButton(int seat) {
        this.button = seat;
    }

    @Override
    public void setButton() {
        // Deal cards to each player to determine button
        // Player with highest card gets the button
        if (getNumOccupiedSeats() == 0) {
            return;
        }

        ServerDeck deck = new ServerDeck();
        deck.shuffle();

        int highSeat = -1;
        int highRank = -1;

        for (int seat = 0; seat < seats; seat++) {
            ServerPlayer player = players[seat];
            if (player != null) {
                int cardRank = deck.nextCard().getRank();
                if (cardRank > highRank) {
                    highRank = cardRank;
                    highSeat = seat;
                }
            }
        }

        this.button = highSeat;
    }

    @Override
    public int getNextSeatAfterButton() {
        return getNextSeat(button);
    }

    @Override
    public int getNextSeat(int seat) {
        if (getNumOccupiedSeats() == 0) {
            return -1;
        }

        int nextSeat = (seat + 1) % seats;
        int searched = 0;

        while (searched < seats) {
            if (players[nextSeat] != null) {
                return nextSeat;
            }
            nextSeat = (nextSeat + 1) % seats;
            searched++;
        }

        return -1; // No occupied seats found
    }

    @Override
    public int getHandNum() {
        return handNum;
    }

    @Override
    public void setHandNum(int handNum) {
        this.handNum = handNum;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public int getMinChip() {
        return minChip;
    }

    @Override
    public GameHand getHoldemHand() {
        return currentHand;
    }

    @Override
    public void setHoldemHand(GameHand hand) {
        this.currentHand = (ServerHand) hand;
    }

    @Override
    public boolean isAutoDeal() {
        return autoDeal;
    }

    @Override
    public boolean isCurrent() {
        // For server, all tables are conceptually "current"
        return true;
    }

    @Override
    public void processAIRebuys() {
        // Not supported in M1 - requires rebuy infrastructure
        // TournamentEngine calls this method to process AI rebuy decisions.
        // Rebuys are a complex feature requiring rebuy tracking, chip purchasing
        // logic, and rebuy period validation. Deferred to future milestone.
    }

    @Override
    public void processAIAddOns() {
        // Not supported in M1 - requires add-on infrastructure
        // TournamentEngine calls this method to process AI add-on decisions.
        // Add-ons are a complex feature requiring add-on tracking, chip purchasing
        // logic, and add-on period validation. Deferred to future milestone.
    }

    @Override
    public void clearRebuyList() {
        // Not supported in M1 - no rebuy tracking to clear
        // This method clears the list of players requesting rebuys.
        // No-op is correct behavior when rebuy infrastructure is not present.
    }

    @Override
    public void setNextMinChip(int minChip) {
        this.nextMinChip = minChip;
    }

    @Override
    public void doColorUpDetermination() {
        // Determine if color-up is needed based on nextMinChip
        if (nextMinChip > minChip) {
            coloringUp = true;
        }
    }

    @Override
    public boolean isColoringUp() {
        return coloringUp;
    }

    @Override
    public void colorUp() {
        // Intentionally stubbed for M1 - color-up detection implemented, chip exchange
        // deferred
        // Color-up detection (doColorUpDetermination/isColoringUp) is fully
        // implemented.
        // The actual chip exchange logic (trading small chips for larger denominations)
        // is non-critical for game correctness and deferred to future milestone.
        // No-op is acceptable - players keep their existing chip stacks.
    }

    @Override
    public void colorUpFinish() {
        this.minChip = this.nextMinChip;
        this.coloringUp = false;
    }

    @Override
    public void startBreak() {
        setTableState(TableState.BREAK);
    }

    @Override
    public void startNewHand() {
        // Advance button to next seat
        advanceButton();

        // Increment hand number
        handNum++;

        // Create new ServerHand
        int smallBlindSeat = getNextSeat(button);
        int bigBlindSeat = getNextSeat(smallBlindSeat);

        // Read current blinds from tournament context if available (supports level
        // advancement). Fall back to construction-time amounts if no context.
        int sb = (tournament != null) ? tournament.getSmallBlind(tournament.getLevel()) : smallBlindAmount;
        int bb = (tournament != null) ? tournament.getBigBlind(tournament.getLevel()) : bigBlindAmount;
        int ante = (tournament != null) ? tournament.getAnte(tournament.getLevel()) : anteAmount;

        currentHand = new ServerHand(this, handNum, sb, bb, ante, button, smallBlindSeat, bigBlindSeat);

        // Deal the hand
        currentHand.deal();
    }

    @Override
    public boolean isZipMode() {
        return zipMode;
    }

    @Override
    public void setZipMode(boolean zipMode) {
        this.zipMode = zipMode;
    }

    @Override
    public void removeWaitAll() {
        waitList.clear();
    }

    @Override
    public void addWait(GamePlayerInfo player) {
        waitList.add((ServerPlayer) player);
    }

    @Override
    public int getWaitSize() {
        return waitList.size();
    }

    @Override
    public GamePlayerInfo getWaitPlayer(int index) {
        return waitList.get(index);
    }

    @Override
    public long getMillisSinceLastStateChange() {
        return System.currentTimeMillis() - lastStateChangeMillis;
    }

    @Override
    public void setPause(int millis) {
        this.pauseMillis = millis;
    }

    @Override
    public int getAutoDealDelay() {
        return autoDealDelay;
    }

    @Override
    public void simulateHand() {
        // Optimization feature deferred - using normal hand execution
        // Fast simulation skips normal hand execution and quickly determines winner
        // for all-AI tables. This is a performance optimization, not required for
        // correctness. Normal hand execution (via startNewHand) works correctly
        // and is sufficient for M1. Simulation can be added in future milestone
        // if all-AI game performance becomes a concern.
        startNewHand();
    }

    @Override
    public List<GamePlayerInfo> getAddedPlayersList() {
        return new ArrayList<>(addedList);
    }

    @Override
    public boolean isRemoved() {
        return removed;
    }

    @Override
    public boolean isAllComputer() {
        if (getNumOccupiedSeats() == 0) {
            return false;
        }

        for (ServerPlayer player : players) {
            if (player != null && player.isHuman()) {
                return false;
            }
        }
        return true;
    }

    // === Additional Methods for Server Implementation ===

    /**
     * Add a player to a specific seat.
     *
     * @param player
     *            the player to add
     * @param seat
     *            the seat number (0-based)
     */
    public void addPlayer(ServerPlayer player, int seat) {
        if (seat < 0 || seat >= seats) {
            throw new IllegalArgumentException("Invalid seat: " + seat);
        }
        if (players[seat] != null) {
            throw new IllegalStateException("Seat " + seat + " is already occupied");
        }

        players[seat] = player;
        player.setSeat(seat);
    }

    /**
     * Remove a player from a seat.
     *
     * @param seat
     *            the seat number (0-based)
     */
    public void removePlayer(int seat) {
        if (seat < 0 || seat >= seats) {
            return;
        }

        ServerPlayer player = players[seat];
        if (player != null) {
            player.setSeat(-1);
            players[seat] = null;
        }
    }

    /**
     * Advance the button to the next occupied seat.
     */
    public void advanceButton() {
        if (button == -1) {
            // First hand - find first occupied seat
            for (int i = 0; i < seats; i++) {
                if (players[i] != null) {
                    button = i;
                    return;
                }
            }
        } else {
            // Advance to next occupied seat
            button = getNextSeat(button);
        }
    }

    /**
     * Mark this table as removed (for consolidation tracking).
     *
     * @param removed
     *            true if table was removed
     */
    public void setRemoved(boolean removed) {
        this.removed = removed;
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
     * Set auto-deal flag.
     *
     * @param autoDeal
     *            true to enable auto-deal
     */
    public void setAutoDeal(boolean autoDeal) {
        this.autoDeal = autoDeal;
    }

    /**
     * Get the tournament context.
     *
     * @return tournament context (or null if not set)
     */
    public Object getTournament() {
        return tournament;
    }

    @Override
    public String toString() {
        return "ServerGameTable{" + "number=" + tableNumber + ", seats=" + seats + ", occupied=" + getNumOccupiedSeats()
                + ", button=" + button + ", state=" + tableState + ", handNum=" + handNum + "}";
    }
}
