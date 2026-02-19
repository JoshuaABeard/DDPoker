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

import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.core.state.BettingRound;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData.ActionOptionsData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ActionOptionsData remoteOptions_;
    private final Map<Integer, Integer> remoteBets_ = new HashMap<>();

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

    /**
     * Returns the server-provided bet for this player in the current round.
     * Overrides to avoid NPE from {@code synchronized(history_)} in the parent; the
     * no-arg HoldemHand constructor leaves {@code history_} null.
     */
    @Override
    public int getBet(PokerPlayer player, int nRound) {
        return remoteBets_.getOrDefault(player.getID(), 0);
    }

    /** Returns the server-provided min bet, or 0 if no options are stored. */
    @Override
    public int getMinBet() {
        return remoteOptions_ != null ? remoteOptions_.minBet() : 0;
    }

    /** Returns the server-provided min raise, or 0 if no options are stored. */
    @Override
    public int getMinRaise() {
        return remoteOptions_ != null ? remoteOptions_.minRaise() : 0;
    }

    /**
     * Returns the server-provided max bet for a player, or 0 if no options are
     * stored.
     */
    @Override
    public int getMaxBet(PokerPlayer player) {
        return remoteOptions_ != null ? remoteOptions_.maxBet() : 0;
    }

    /**
     * Returns the server-provided max raise for a player, or 0 if no options are
     * stored.
     */
    @Override
    public int getMaxRaise(PokerPlayer player) {
        return remoteOptions_ != null ? remoteOptions_.maxRaise() : 0;
    }

    /**
     * Returns {@code false} — the remote hand has no action history. Overrides to
     * avoid NPE from {@code synchronized(history_)} in the parent.
     */
    @Override
    public boolean hasPlayerActed(PokerPlayer player) {
        return false;
    }

    /**
     * Returns an empty list — the remote hand has no action history. Overrides to
     * avoid NPE from {@code synchronized(history_)} in the parent.
     */
    @Override
    public List<HandAction> getHistoryCopy() {
        return Collections.emptyList();
    }

    /**
     * Returns 0 — the remote hand has no action history. Overrides to avoid NPE
     * from {@code synchronized(history_)} in the parent.
     */
    @Override
    public int getHistorySize() {
        return 0;
    }

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

    /**
     * Stores the current action options from the server. Used by
     * {@link #getMinBet()}, {@link #getMinRaise()},
     * {@link #getMaxBet(PokerPlayer)}, and {@link #getMaxRaise(PokerPlayer)} to
     * provide correct bet/raise amounts to the Swing input UI.
     */
    public void updateActionOptions(ActionOptionsData opts) {
        this.remoteOptions_ = opts;
    }

    /**
     * Updates the current-round bet total for a specific player.
     *
     * @param playerId
     *            player ID
     * @param totalBet
     *            the player's total bet this round (0 clears the entry)
     */
    public void updatePlayerBet(int playerId, int totalBet) {
        if (totalBet > 0) {
            remoteBets_.put(playerId, totalBet);
        } else {
            remoteBets_.remove(playerId);
        }
    }

    /**
     * Clears all per-player bets. Called when a new betting round starts (flop,
     * turn, river) so the display resets to zero.
     */
    public void clearBets() {
        remoteBets_.clear();
    }
}
