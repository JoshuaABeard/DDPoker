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
import com.donohoedigital.games.poker.engine.HandSorted;
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
    private HandSorted remoteCommunitySorted_ = new HandSorted(5);
    private List<PokerPlayer> remotePlayers_ = new ArrayList<>();
    private int remoteCurrentPlayerIndex_ = NO_CURRENT_PLAYER;
    private int remotePotTotal_;
    private ActionOptionsData remoteOptions_;
    private final Map<Integer, Integer> remoteBets_ = new HashMap<>();
    private final Map<Integer, Integer> remoteWins_ = new HashMap<>();
    private int remoteSmallBlindSeat_ = NO_CURRENT_PLAYER;
    private int remoteBigBlindSeat_ = NO_CURRENT_PLAYER;

    /**
     * Creates a remote hand with no-arg parent constructor. Calls
     * {@code initHandLists()} so that any {@code synchronized(pots_)} or
     * {@code synchronized(history_)} methods in the parent that are not overridden
     * here operate on non-null lists rather than NPE.
     */
    public RemoteHoldemHand() {
        super();
        initHandLists();
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

    /**
     * Overrides the parent which reads {@code nRound_} directly (never set in
     * remote mode). Returns the legacy int for the server-provided round so
     * {@link com.donohoedigital.games.poker.DealCommunity#syncCards} can switch on
     * it.
     */
    @Override
    public int getRoundForDisplay() {
        return remoteRound_.toLegacy();
    }

    @Override
    public Hand getCommunity() {
        return remoteCommunity_;
    }

    @Override
    public HandSorted getCommunitySorted() {
        if (remoteCommunitySorted_.fingerprint() != remoteCommunity_.fingerprint()) {
            remoteCommunitySorted_ = new HandSorted(remoteCommunity_);
        }
        return remoteCommunitySorted_;
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

    /**
     * Records a win for the given player. Overrides to avoid {@code addHistory()},
     * which fires a {@code TYPE_PLAYER_ACTION} event on {@code table_} — a field
     * that is {@code null} in remote mode (no-arg constructor never sets it).
     * Accumulated so that split-pot wins are summed correctly.
     */
    @Override
    public void wins(PokerPlayer player, int nChips, int nPot) {
        remoteWins_.merge(player.getID(), nChips, Integer::sum);
    }

    /**
     * Returns the total chips won by this player in the current hand, or {@code 0}.
     * Overrides to read from the remote wins map instead of {@code history_} (which
     * would also work, but only if {@code addHistory()} didn't throw first due to
     * {@code table_} being {@code null}).
     */
    @Override
    public int getWin(PokerPlayer player) {
        return remoteWins_.getOrDefault(player.getID(), 0);
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

    /**
     * Clears all per-player win records. Called at the start of each new hand so
     * wins from the previous hand do not accumulate into the current hand's total.
     */
    public void clearWins() {
        remoteWins_.clear();
    }

    /** Updates the small blind seat index for this hand. */
    public void updateSmallBlindSeat(int seat) {
        this.remoteSmallBlindSeat_ = seat;
    }

    /** Updates the big blind seat index for this hand. */
    public void updateBigBlindSeat(int seat) {
        this.remoteBigBlindSeat_ = seat;
    }

    /**
     * Returns the small blind seat index, or {@code NO_CURRENT_PLAYER} if unknown.
     */
    public int getRemoteSmallBlindSeat() {
        return remoteSmallBlindSeat_;
    }

    /**
     * Returns the big blind seat index, or {@code NO_CURRENT_PLAYER} if unknown.
     */
    public int getRemoteBigBlindSeat() {
        return remoteBigBlindSeat_;
    }
}
