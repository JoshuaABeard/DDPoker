/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thin view model for a poker table driven by WebSocket state updates.
 *
 * <p>
 * Extends {@link PokerTable} and overrides ~15 getters to return simple stored
 * fields populated from server-to-client WebSocket messages. Contains zero
 * poker logic. The existing Swing UI reads it identically to a real
 * {@code PokerTable}.
 *
 * <p>
 * The parent constructor {@code super(game, nNum)} is safe: it only assigns
 * three fields plus a PropertyConfig string lookup.
 *
 * <p>
 * State is updated atomically by {@link #updateFromState} and
 * {@link #setRemoteHand}. Events are fired explicitly by the caller (typically
 * {@code WebSocketTournamentDirector}) after state is fully updated.
 */
public class RemotePokerTable extends PokerTable {

    private static final Logger logger = LogManager.getLogger(RemotePokerTable.class);

    /** Remote-state storage — never null after construction. */
    private final PokerPlayer[] remotePlayers_ = new PokerPlayer[PokerConstants.SEATS];
    private RemoteHoldemHand remoteHand_;
    private int remoteButton_ = NO_SEAT;

    /**
     * Creates a new remote table view model.
     *
     * @param game
     *            the containing poker game (used by PokerTable infrastructure)
     * @param nNum
     *            the table number / ID, forwarded to PokerTable
     */
    public RemotePokerTable(PokerGame game, int nNum) {
        super(game, nNum);
    }

    // -------------------------------------------------------------------------
    // Overridden getters (return remote-state values, not local-engine values)
    // -------------------------------------------------------------------------

    /** Returns the remote hand (populated by WebSocket messages). */
    @Override
    public HoldemHand getHoldemHand() {
        return remoteHand_;
    }

    /**
     * Returns the player at the given seat, or {@code null} if the seat is empty.
     * Overrides the parent because the parent's {@code players_[]} is
     * package-private and not accessible from this subpackage.
     */
    @Override
    public PokerPlayer getPlayer(int nSeat) {
        if (nSeat < 0 || nSeat >= PokerConstants.SEATS)
            return null;
        return remotePlayers_[nSeat];
    }

    /** Returns the number of non-empty seats in the remote player array. */
    @Override
    public int getNumOccupiedSeats() {
        int count = 0;
        for (PokerPlayer p : remotePlayers_) {
            if (p != null)
                count++;
        }
        return count;
    }

    /** Returns the stored button seat index. */
    @Override
    public int getButton() {
        return remoteButton_;
    }

    // -------------------------------------------------------------------------
    // State update methods (called by WebSocketTournamentDirector)
    // -------------------------------------------------------------------------

    /**
     * Updates the player array from a server-provided seat list. Replaces all seats
     * atomically. Does NOT fire events — call {@link #firePokerTableEvent} after
     * all updates are applied.
     *
     * @param players
     *            array of players indexed by seat, null entries = empty seat
     * @param button
     *            dealer button seat index
     */
    public void updateFromState(PokerPlayer[] players, int button) {
        int len = Math.min(players.length, remotePlayers_.length);
        for (int i = 0; i < len; i++) {
            remotePlayers_[i] = players[i];
            if (players[i] != null && players[i].getTable() == null) {
                // Required so DealDisplay.displayCard() can call player.getTable()
                // without NPE in PokerUtils.getTerritoryForTableSeat.
                players[i].setTable(this, i);
            }
        }
        remoteButton_ = button;
    }

    /**
     * Sets a single player in a specific seat. Does NOT fire events.
     */
    public void setRemotePlayer(int seat, PokerPlayer player) {
        if (seat >= 0 && seat < PokerConstants.SEATS) {
            remotePlayers_[seat] = player;
        }
    }

    /**
     * Clears a specific seat (player left/was eliminated). Does NOT fire events.
     */
    public void clearSeat(int seat) {
        if (seat >= 0 && seat < PokerConstants.SEATS) {
            remotePlayers_[seat] = null;
        }
    }

    /**
     * Updates only the button seat without replacing the player array. Does NOT
     * fire events.
     */
    public void setRemoteButton(int seat) {
        remoteButton_ = seat;
    }

    /**
     * Sets or replaces the current hand view model. Does NOT fire events.
     */
    public void setRemoteHand(RemoteHoldemHand hand) {
        this.remoteHand_ = hand;
    }

    /** Returns the current hand as {@link RemoteHoldemHand}, or {@code null}. */
    public RemoteHoldemHand getRemoteHand() {
        return remoteHand_;
    }

    /**
     * Fires a {@link PokerTableEvent} on this table. Delegates to the inherited
     * {@link PokerTable#firePokerTableEvent} which dispatches to all registered
     * {@code PokerTableListener}s.
     *
     * @param eventType
     *            one of the {@code PokerTableEvent.TYPE_*} constants
     */
    public void fireEvent(int eventType) {
        logger.debug("[RemotePokerTable] table={} fireEvent type={}", getNumber(), eventType);
        firePokerTableEvent(new PokerTableEvent(eventType, this));
    }

    /**
     * Returns {@code true} — this table is driven by WebSocket remote state.
     */
    @Override
    public boolean isRemoteTable() {
        return true;
    }

    /**
     * Fires a {@link PokerTableEvent} with an int value parameter.
     *
     * @param eventType
     *            one of the {@code PokerTableEvent.TYPE_*} constants
     * @param value
     *            event-specific int value
     */
    public void fireEvent(int eventType, int value) {
        logger.debug("[RemotePokerTable] table={} fireEvent type={} value={}", getNumber(), eventType, value);
        firePokerTableEvent(new PokerTableEvent(eventType, this, value));
    }
}
