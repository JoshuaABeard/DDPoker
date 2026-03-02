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

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.PropertyConfig;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.event.PokerTableEvent;
import com.donohoedigital.games.poker.event.PokerTableListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin view model for a poker table driven by WebSocket state updates.
 *
 * <p>
 * Implements {@link ClientPokerTable} directly — no longer extends
 * {@link PokerTable}. All state is stored in local fields populated from
 * server-to-client WebSocket messages. Contains zero poker logic. The existing
 * Swing UI reads it identically to a real {@code PokerTable} via the
 * {@code ClientPokerTable} interface.
 *
 * <p>
 * State is updated atomically by {@link #updateFromState} and
 * {@link #setRemoteHand}. Events are fired explicitly by the caller (typically
 * {@code WebSocketTournamentDirector}) after state is fully updated.
 */
public class RemotePokerTable implements ClientPokerTable {

    private static final Logger logger = LogManager.getLogger(RemotePokerTable.class);

    // -------------------------------------------------------------------------
    // Identity fields (replaces super(game, nNum) from PokerTable)
    // -------------------------------------------------------------------------
    private final PokerGame game_;
    private final int nNum_;
    private final String sName_;

    // -------------------------------------------------------------------------
    // Remote-state storage — never null after construction
    // -------------------------------------------------------------------------
    private final PokerPlayer[] remotePlayers_ = new PokerPlayer[PokerConstants.SEATS];
    private RemoteHoldemHand remoteHand_;
    private int remoteButton_ = PokerTable.NO_SEAT;
    private int nMinChip_ = 0;
    private int nHandNum_ = 0;

    // -------------------------------------------------------------------------
    // Listener infrastructure (copied from PokerTable — no superclass available)
    // -------------------------------------------------------------------------
    private List<ListenerInfo> listeners_ = new ArrayList<>();

    private static class ListenerInfo {
        static final ListenerInfo NULL_LISTENER = new ListenerInfo(null, 0);

        PokerTableListener listener;
        int nTypes;

        ListenerInfo(PokerTableListener listener, int nTypes) {
            this.listener = listener;
            this.nTypes = nTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ListenerInfo))
                return false;
            ListenerInfo info = (ListenerInfo) o;
            return info.listener == listener;
        }

        @Override
        public int hashCode() {
            return listener == null ? 0 : listener.hashCode();
        }
    }

    /**
     * Creates a new remote table view model.
     *
     * @param game
     *            the containing poker game
     * @param nNum
     *            the table number / ID
     */
    public RemotePokerTable(PokerGame game, int nNum) {
        game_ = game;
        nNum_ = nNum;
        sName_ = PropertyConfig.getMessage("msg.table.name", nNum_);
    }

    // -------------------------------------------------------------------------
    // ClientPokerTable — identity and structure
    // -------------------------------------------------------------------------

    @Override
    public int getNumber() {
        return nNum_;
    }

    @Override
    public String getName() {
        return sName_;
    }

    @Override
    public PokerGame getGame() {
        return game_;
    }

    @Override
    public int getSeats() {
        return game_ == null ? 10 : game_.getSeats();
    }

    // -------------------------------------------------------------------------
    // ClientPokerTable — seat / player access
    // -------------------------------------------------------------------------

    /**
     * Returns the player at the given seat, or {@code null} if the seat is empty.
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
    // ClientPokerTable — display helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the display seat for the given table seat, adjusting so the local
     * human player appears at seat 5 (index 4). Matches {@code PokerTable}
     * behaviour.
     */
    @Override
    public int getDisplaySeat(int nSeat) {
        nSeat += getSeatOffset();
        if (nSeat >= PokerConstants.SEATS) {
            nSeat -= PokerConstants.SEATS;
        } else if (nSeat < 0) {
            nSeat += PokerConstants.SEATS;
        }
        return nSeat;
    }

    /**
     * Returns the table seat for the given display seat (reverse of
     * {@link #getDisplaySeat}).
     */
    @Override
    public int getTableSeat(int nDisplaySeat) {
        nDisplaySeat -= getSeatOffset();
        if (nDisplaySeat >= PokerConstants.SEATS) {
            nDisplaySeat -= PokerConstants.SEATS;
        } else if (nDisplaySeat < 0) {
            nDisplaySeat += PokerConstants.SEATS;
        }
        return nDisplaySeat;
    }

    /**
     * Returns the seat offset so that the local human player appears at seat 5.
     * Matches {@code PokerTable.getSeatOffset()}.
     */
    public int getSeatOffset() {
        int nSeat = PokerTable.NO_SEAT;
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            PokerPlayer player = remotePlayers_[i];
            if (player == null)
                continue;
            if (player.isLocallyControlled() && player.isHuman()) {
                nSeat = i;
            }
        }
        if (nSeat == PokerTable.NO_SEAT)
            return 0;
        return 4 - nSeat; // seat 5 is index 4
    }

    // -------------------------------------------------------------------------
    // ClientPokerTable — hand access
    // -------------------------------------------------------------------------

    /** Returns the remote hand (populated by WebSocket messages). */
    @Override
    public RemoteHoldemHand getHoldemHand() {
        return remoteHand_;
    }

    // -------------------------------------------------------------------------
    // ClientPokerTable — game-play state
    // -------------------------------------------------------------------------

    /**
     * Returns the current level from the game. Remote tables don't maintain their
     * own level counter — it is read from the game profile via the game object.
     */
    @Override
    public int getLevel() {
        return game_ != null ? game_.getLevel() : 1;
    }

    /**
     * Returns the minimum chip denomination. Stored in a local field set by
     * {@link #setMinChip} (called from WebSocketTournamentDirector) which mirrors
     * the value from {@code game_.getMinChip()}.
     */
    @Override
    public int getMinChip() {
        return nMinChip_;
    }

    /**
     * Sets the minimum chip denomination. Called by
     * {@code WebSocketTournamentDirector} when the level changes.
     *
     * @param n
     *            the new minimum chip value
     */
    public void setMinChip(int n) {
        nMinChip_ = n;
    }

    /**
     * Returns the hand number for the current/most-recent hand. Updated by
     * {@code WebSocketTournamentDirector} each time a new hand starts. Returns 0 if
     * no hand has been dealt yet.
     */
    @Override
    public int getHandNum() {
        return nHandNum_;
    }

    /**
     * Sets the hand number. Called by {@code WebSocketTournamentDirector} when a
     * new hand starts.
     *
     * @param n
     *            the hand number from the server
     */
    public void setHandNum(int n) {
        nHandNum_ = n;
    }

    /**
     * Returns {@code true} — in the remote (online) path the local client is always
     * the "current" table.
     */
    @Override
    public boolean isCurrent() {
        return true;
    }

    /**
     * Returns {@code false} — remote tables never use zip/fast-forward mode.
     */
    @Override
    public boolean isZipMode() {
        return false;
    }

    /**
     * Returns {@code true} — this table is driven by WebSocket remote state.
     */
    @Override
    public boolean isRemoteTable() {
        return true;
    }

    /**
     * No-op for remote tables — {@link #isCurrent()} always returns {@code true} in
     * the remote path. Exists to satisfy the {@link ClientPokerTable} interface so
     * {@code PokerGame.setCurrentTable} can call it on both local and remote tables
     * uniformly.
     */
    @Override
    public void setCurrent(boolean b) {
        // Remote tables are always "current" — no internal state to update.
    }

    /**
     * Marks this table as removed and fires a
     * {@link PokerTableEvent#TYPE_TABLE_REMOVED} event so listeners (e.g. table
     * list UI) can react.
     *
     * @param b
     *            {@code true} to mark the table as removed
     */
    @Override
    public void setRemoved(boolean b) {
        if (b) {
            firePokerTableEvent(new PokerTableEvent(PokerTableEvent.TYPE_TABLE_REMOVED, this));
        }
    }

    // -------------------------------------------------------------------------
    // ClientPokerTable — listener management
    // -------------------------------------------------------------------------

    @Override
    public synchronized void addPokerTableListener(PokerTableListener l, int nTypes) {
        ListenerInfo nu = new ListenerInfo(l, nTypes);
        int old = listeners_.indexOf(nu);
        if (old != -1) {
            nu = listeners_.get(old);
            ApplicationError.assertTrue(nu.listener == l, "Mismatched listeners", null);
            nu.nTypes |= nTypes;
        } else {
            listeners_.add(nu);
        }
    }

    @Override
    public synchronized void removePokerTableListener(PokerTableListener l, int nTypes) {
        ListenerInfo nu = new ListenerInfo(l, nTypes);
        int old = listeners_.indexOf(nu);
        if (old != -1) {
            nu = listeners_.get(old);
            ApplicationError.assertTrue(nu.listener == l, "Mismatched listeners", null);
            nu.nTypes &= ~nTypes;
            if (nu.nTypes == 0) {
                listeners_.set(old, ListenerInfo.NULL_LISTENER);
            }
        }
    }

    @Override
    public synchronized void firePokerTableEvent(PokerTableEvent event) {
        ListenerInfo info;
        int nSize = listeners_.size();
        for (int i = 0; i < nSize;) {
            info = listeners_.get(i);
            if (info == ListenerInfo.NULL_LISTENER) {
                listeners_.remove(i);
                --nSize;
            } else {
                if ((info.nTypes & event.getType()) > 0) {
                    info.listener.tableEventOccurred(event);
                }
                i++;
            }
        }
    }

    // -------------------------------------------------------------------------
    // State update methods (called by WebSocketTournamentDirector)
    // -------------------------------------------------------------------------

    /**
     * Updates the player array from a server-provided seat list. Replaces all seats
     * atomically. Does NOT fire events — call {@link #fireEvent} after all updates
     * are applied.
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
     * Sets or replaces the current hand view model. Back-links the hand to this
     * table so {@link RemoteHoldemHand#getTable()} returns the correct value (used
     * by {@link com.donohoedigital.games.poker.dashboard.DashboardAdvisor} and
     * other components that call {@code hhand.getTable()}). Does NOT fire events.
     */
    public void setRemoteHand(RemoteHoldemHand hand) {
        this.remoteHand_ = hand;
        if (hand != null)
            hand.setOwnerTable(this);
    }

    /** Returns the current hand as {@link RemoteHoldemHand}, or {@code null}. */
    public RemoteHoldemHand getRemoteHand() {
        return remoteHand_;
    }

    /**
     * Fires a {@link PokerTableEvent} on this table with no extra parameters.
     *
     * @param eventType
     *            one of the {@code PokerTableEvent.TYPE_*} constants
     */
    public void fireEvent(int eventType) {
        logger.debug("[RemotePokerTable] table={} fireEvent type={}", getNumber(), eventType);
        firePokerTableEvent(new PokerTableEvent(eventType, this));
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
