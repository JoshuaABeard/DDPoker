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
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.core.state.BettingRound;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin view model for a poker hand driven by WebSocket state updates.
 *
 * <p>
 * Extends {@link HoldemHand} using its no-arg constructor (which exists for
 * deserialization) and overrides ~10 getters to return simple stored fields
 * populated from server-to-client messages. Contains zero poker logic.
 *
 * <p>
 * The no-arg constructor avoids the normal constructor's wasteful creation of a
 * Deck, pots list, community cards, and blind lookups that are irrelevant for a
 * remote hand. The Swing UI reads getters identically to the originals.
 *
 * <p>
 * State is updated by {@code updateRound}, {@code updateCommunity},
 * {@code updateCurrentPlayer}, {@code updatePot}, and
 * {@code updatePlayerOrder}. All update methods are non-firing — the caller
 * (WebSocketTournamentDirector) fires events after state is fully applied.
 */
public class RemoteHoldemHand extends HoldemHand {

    private BettingRound remoteRound_ = BettingRound.PRE_FLOP;
    private Hand remoteCommunity_ = new Hand();
    private List<PokerPlayer> remotePlayers_ = new ArrayList<>();
    private int remoteCurrentPlayerIndex_ = NO_CURRENT_PLAYER;
    private int remotePotTotal_;

    /**
     * Creates a remote hand with no-arg parent constructor. The no-arg HoldemHand
     * constructor (used for deserialization) does not create a Deck or pots —
     * appropriate for a remote view model.
     */
    public RemoteHoldemHand() {
        super();
    }

    // -------------------------------------------------------------------------
    // Overridden getters (return remote-state values)
    // -------------------------------------------------------------------------

    @Override
    public BettingRound getRound() {
        return remoteRound_;
    }

    @Override
    public Hand getCommunity() {
        return remoteCommunity_;
    }

    @Override
    public int getNumPlayers() {
        return remotePlayers_.size();
    }

    @Override
    public PokerPlayer getPlayerAt(int index) {
        if (index < 0 || index >= remotePlayers_.size())
            return null;
        return remotePlayers_.get(index);
    }

    @Override
    public int getCurrentPlayerIndex() {
        return remoteCurrentPlayerIndex_;
    }

    @Override
    public PokerPlayer getCurrentPlayer() {
        if (remoteCurrentPlayerIndex_ < 0 || remoteCurrentPlayerIndex_ >= remotePlayers_.size()) {
            return null;
        }
        return remotePlayers_.get(remoteCurrentPlayerIndex_);
    }

    @Override
    public int getTotalPotChipCount() {
        return remotePotTotal_;
    }

    // -------------------------------------------------------------------------
    // State update methods (called by WebSocketTournamentDirector)
    // -------------------------------------------------------------------------

    /** Updates the betting round. */
    public void updateRound(BettingRound round) {
        this.remoteRound_ = round;
    }

    /** Replaces the community cards. */
    public void updateCommunity(Hand community) {
        this.remoteCommunity_ = community != null ? community : new Hand();
    }

    /**
     * Replaces the ordered player list for this hand. The index of the current
     * player is preserved if it is still valid.
     */
    public void updatePlayerOrder(List<PokerPlayer> players) {
        this.remotePlayers_ = new ArrayList<>(players);
        // Clamp current player index to new size
        if (remoteCurrentPlayerIndex_ >= remotePlayers_.size()) {
            remoteCurrentPlayerIndex_ = NO_CURRENT_PLAYER;
        }
    }

    /**
     * Updates the index of the current (acting) player.
     *
     * @param index
     *            index into the player order list, or {@code NO_CURRENT_PLAYER}
     */
    public void updateCurrentPlayer(int index) {
        this.remoteCurrentPlayerIndex_ = index;
    }

    /** Updates the total pot chip count. */
    public void updatePot(int totalPot) {
        this.remotePotTotal_ = totalPot;
    }
}
