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

import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandSorted;
import com.donohoedigital.games.poker.engine.state.BettingRound;
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
 * Implements {@link ClientHoldemHand} directly — no longer extends
 * {@link com.donohoedigital.games.poker.HoldemHand}. All state comes from
 * WebSocket messages; contains zero poker logic.
 *
 * <p>
 * State is updated by {@code updateRound}, {@code updateCommunity},
 * {@code updateCurrentPlayer}, {@code updatePot}, and
 * {@code updatePlayerOrder}. All update methods are non-firing — the caller
 * (WebSocketTournamentDirector) fires events after state is fully applied.
 */
public class RemoteHoldemHand implements ClientHoldemHand {

    /**
     * Sentinel value meaning "no current player". Mirrors
     * {@code HoldemHand.NO_CURRENT_PLAYER}.
     */
    public static final int NO_CURRENT_PLAYER = -999;

    private BettingRound remoteRound_ = BettingRound.PRE_FLOP;
    private Hand remoteCommunity_ = new Hand();
    private HandSorted remoteCommunitySorted_ = new HandSorted(5);
    private List<ClientPlayer> remotePlayers_ = new ArrayList<>();
    private int remoteCurrentPlayerIndex_ = NO_CURRENT_PLAYER;
    private int remotePotTotal_;
    private ActionOptionsData remoteOptions_;
    private final Map<Integer, Integer> remoteBets_ = new HashMap<>();
    private final Map<Integer, Integer> remoteWins_ = new HashMap<>();
    private int remoteSmallBlindSeat_ = NO_CURRENT_PLAYER;
    private int remoteBigBlindSeat_ = NO_CURRENT_PLAYER;
    private int remoteSmallBlind_;
    private int remoteBigBlind_;
    private int remoteAnte_;
    private ClientPokerTable ownerTable_;

    /** Creates a remote hand. */
    public RemoteHoldemHand() {
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — table linkage
    // -------------------------------------------------------------------------

    /**
     * Returns the owning table as a {@link ClientPokerTable}. Works for both local
     * and remote tables.
     */
    @Override
    public ClientPokerTable getClientTable() {
        return ownerTable_;
    }

    /** Called by {@link RemotePokerTable#setRemoteHand} to back-link the table. */
    void setOwnerTable(ClientPokerTable table) {
        ownerTable_ = table;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — betting round
    // -------------------------------------------------------------------------

    @Override
    public BettingRound getRound() {
        return remoteRound_;
    }

    /**
     * Returns the legacy int for the server-provided round so
     * {@link com.donohoedigital.games.poker.DealCommunity#syncCards} can switch on
     * it.
     */
    @Override
    public int getRoundForDisplay() {
        return remoteRound_.toLegacy();
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — community cards
    // -------------------------------------------------------------------------

    @Override
    public Hand getCommunity() {
        return remoteCommunity_;
    }

    /**
     * Returns the community cards. For the remote hand there is no all-in showdown
     * state tracking, so this is the same as {@link #getCommunity()}.
     */
    @Override
    public Hand getCommunityForDisplay() {
        return remoteCommunity_;
    }

    @Override
    public HandSorted getCommunitySorted() {
        if (remoteCommunitySorted_.fingerprint() != remoteCommunity_.fingerprint()) {
            remoteCommunitySorted_ = new HandSorted(remoteCommunity_);
        }
        return remoteCommunitySorted_;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — players
    // -------------------------------------------------------------------------

    @Override
    public int getNumPlayers() {
        return remotePlayers_.size();
    }

    /** Returns the number of players who have not yet folded. */
    @Override
    public int getNumWithCards() {
        int count = 0;
        for (ClientPlayer p : remotePlayers_) {
            if (!p.isFolded())
                count++;
        }
        return count;
    }

    @Override
    public ClientPlayer getPlayerAt(int index) {
        if (index < 0 || index >= remotePlayers_.size())
            return null;
        return remotePlayers_.get(index);
    }

    @Override
    public int getCurrentPlayerIndex() {
        return remoteCurrentPlayerIndex_;
    }

    @Override
    public ClientPlayer getCurrentPlayer() {
        if (remoteCurrentPlayerIndex_ < 0 || remoteCurrentPlayerIndex_ >= remotePlayers_.size()) {
            return null;
        }
        return remotePlayers_.get(remoteCurrentPlayerIndex_);
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — pot / chips
    // -------------------------------------------------------------------------

    @Override
    public int getTotalPotChipCount() {
        return remotePotTotal_;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — bet / call amounts
    // -------------------------------------------------------------------------

    /** Returns the server-provided bet for this player in the current round. */
    @Override
    public int getBet(ClientPlayer player, int nRound) {
        return remoteBets_.getOrDefault(player.getID(), 0);
    }

    /**
     * Returns the server-provided bet for this player (round is ignored for
     * remote).
     */
    @Override
    public int getBet(ClientPlayer player) {
        return remoteBets_.getOrDefault(player.getID(), 0);
    }

    /**
     * Returns the highest bet in the current round (the amount a caller must
     * match). Derived by scanning the remote bets map.
     */
    @Override
    public int getBet() {
        int max = 0;
        for (int v : remoteBets_.values()) {
            if (v > max)
                max = v;
        }
        return max;
    }

    /**
     * Returns the amount the given player needs to add to call the current bet,
     * using the server-provided call amount from action options when available.
     */
    @Override
    public int getCall(ClientPlayer player) {
        if (remoteOptions_ != null) {
            return remoteOptions_.callAmount();
        }
        // Fallback: compute from bets
        int highBet = getBet();
        int playerBet = getBet(player);
        return Math.max(0, highBet - playerBet);
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
    public int getMaxBet(ClientPlayer player) {
        return remoteOptions_ != null ? remoteOptions_.maxBet() : 0;
    }

    /**
     * Returns the server-provided max raise for a player, or 0 if no options are
     * stored.
     */
    @Override
    public int getMaxRaise(ClientPlayer player) {
        return remoteOptions_ != null ? remoteOptions_.maxRaise() : 0;
    }

    /**
     * Returns the minimum chip denomination for this hand, delegating to the owning
     * table.
     */
    @Override
    public int getMinChip() {
        return ownerTable_ != null ? ownerTable_.getMinChip() : 1;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — game type
    // -------------------------------------------------------------------------

    /**
     * Returns the game type constant for this hand. Delegates to the owning table's
     * game profile at the current level.
     */
    @Override
    public int getGameType() {
        if (ownerTable_ != null && ownerTable_.getGame() != null) {
            return ownerTable_.getGame().getProfile().getGameType(ownerTable_.getLevel());
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — odds
    // -------------------------------------------------------------------------

    /**
     * Returns the pot odds for the given player as a value 0–100. Uses the
     * server-provided call amount when action options are available.
     */
    @Override
    public float getPotOdds(ClientPlayer player) {
        int nCall = getCall(player);
        int nPot = getTotalPotChipCount();
        if (nCall <= 0 || (nCall + nPot) == 0) {
            return 0.0f;
        }
        return 100.0f * ((float) nCall / ((float) nCall + (float) nPot));
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — state flags
    // -------------------------------------------------------------------------

    /**
     * Returns {@code false} — the remote hand does not track all-in showdown state
     * separately; the UI derives this from player chip counts.
     */
    @Override
    public boolean isAllInShowdown() {
        return false;
    }

    /**
     * Returns {@code false} — remote hands are view models and are never stored in
     * the local hand history database.
     */
    @Override
    public boolean isStoredInDatabase() {
        return false;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — action history
    // -------------------------------------------------------------------------

    /** Delegates to {@link PokerPlayer#isFolded()}. */
    @Override
    public boolean isFolded(ClientPlayer player) {
        return player.isFolded();
    }

    /** Returns {@code false} — the remote hand has no action history. */
    @Override
    public boolean hasPlayerActed(ClientPlayer player) {
        return false;
    }

    /**
     * Returns {@code HandAction#ACTION_NONE} — the remote hand has no action
     * history.
     */
    @Override
    public int getLastAction(ClientPlayer player) {
        return HandAction.ACTION_NONE;
    }

    /** Returns an empty list — the remote hand has no action history. */
    @Override
    public List<HandAction> getHistoryCopy() {
        return Collections.emptyList();
    }

    /** Returns 0 — the remote hand has no action history. */
    @Override
    public int getHistorySize() {
        return 0;
    }

    /** Returns 0 — the remote hand has no action history to sum bets from. */
    @Override
    public int getTotalBet(ClientPlayer player) {
        return 0;
    }

    /**
     * Returns 0 — the remote hand has no action history to count prior raises from.
     */
    @Override
    public int getNumPriorRaises(ClientPlayer player) {
        return 0;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — win recording
    // -------------------------------------------------------------------------

    /**
     * Records a win for the given player. Accumulated so that split-pot wins are
     * summed correctly.
     */
    @Override
    public void wins(ClientPlayer player, int nChips, int nPot) {
        remoteWins_.merge(player.getID(), nChips, Integer::sum);
    }

    /**
     * Returns the total chips won by this player in the current hand, or {@code 0}.
     */
    @Override
    public int getWin(ClientPlayer player) {
        return remoteWins_.getOrDefault(player.getID(), 0);
    }

    // -------------------------------------------------------------------------
    // Blind / ante accessors (mirrors HoldemHand, set by
    // WebSocketTournamentDirector)
    // -------------------------------------------------------------------------

    /** Sets the small blind amount for display purposes. */
    public void setSmallBlind(int n) {
        remoteSmallBlind_ = n;
    }

    /** Sets the big blind amount for display purposes. */
    public void setBigBlind(int n) {
        remoteBigBlind_ = n;
    }

    /** Sets the ante amount for display purposes. */
    public void setAnte(int n) {
        remoteAnte_ = n;
    }

    /** Returns the small blind amount. */
    public int getSmallBlind() {
        return remoteSmallBlind_;
    }

    /** Returns the big blind amount. */
    public int getBigBlind() {
        return remoteBigBlind_;
    }

    /** Returns the ante amount. */
    public int getAnte() {
        return remoteAnte_;
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
    public void updatePlayerOrder(List<ClientPlayer> players) {
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
