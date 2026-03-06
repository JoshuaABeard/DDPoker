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
import com.donohoedigital.games.poker.display.ClientBettingRound;
import com.donohoedigital.games.poker.display.ClientHand;
import com.donohoedigital.games.poker.protocol.constants.ProtocolConstants;
import com.donohoedigital.games.poker.protocol.message.ServerMessageData.ActionOptionsData;

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

    private ClientBettingRound remoteRound_ = ClientBettingRound.PRE_FLOP;
    private ClientHand remoteCommunity_ = ClientHand.empty();
    private ClientHand remoteCommunitySorted_;
    private List<ClientPlayer> remotePlayers_ = new ArrayList<>();
    private int remoteCurrentPlayerIndex_ = NO_CURRENT_PLAYER;
    private int remotePotTotal_;
    private List<ClientPot> remotePots_ = new ArrayList<>();
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
    public ClientBettingRound getRound() {
        return remoteRound_;
    }

    @Override
    public int getRoundForDisplay() {
        return remoteRound_.toLegacy();
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — community cards
    // -------------------------------------------------------------------------

    @Override
    public ClientHand getCommunity() {
        return remoteCommunity_;
    }

    @Override
    public ClientHand getCommunityForDisplay() {
        return remoteCommunity_;
    }

    @Override
    public ClientHand getCommunitySorted() {
        if (remoteCommunitySorted_ == null || remoteCommunitySorted_.fingerprint() != remoteCommunity_.fingerprint()) {
            remoteCommunitySorted_ = remoteCommunity_.sorted();
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

    @Override
    public int getNumPots() {
        return remotePots_.size();
    }

    @Override
    public ClientPot getPot(int index) {
        if (index < 0 || index >= remotePots_.size())
            return null;
        return remotePots_.get(index);
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — bet / call amounts
    // -------------------------------------------------------------------------

    @Override
    public int getBet(ClientPlayer player, int nRound) {
        return remoteBets_.getOrDefault(player.getID(), 0);
    }

    @Override
    public int getBet(ClientPlayer player) {
        return remoteBets_.getOrDefault(player.getID(), 0);
    }

    @Override
    public int getBet() {
        int max = 0;
        for (int v : remoteBets_.values()) {
            if (v > max)
                max = v;
        }
        return max;
    }

    @Override
    public int getCall(ClientPlayer player) {
        if (remoteOptions_ != null) {
            return remoteOptions_.callAmount();
        }
        int highBet = getBet();
        int playerBet = getBet(player);
        return Math.max(0, highBet - playerBet);
    }

    @Override
    public int getMinBet() {
        return remoteOptions_ != null ? remoteOptions_.minBet() : 0;
    }

    @Override
    public int getMinRaise() {
        return remoteOptions_ != null ? remoteOptions_.minRaise() : 0;
    }

    @Override
    public int getMaxBet(ClientPlayer player) {
        return remoteOptions_ != null ? remoteOptions_.maxBet() : 0;
    }

    @Override
    public int getMaxRaise(ClientPlayer player) {
        return remoteOptions_ != null ? remoteOptions_.maxRaise() : 0;
    }

    @Override
    public int getMinChip() {
        return ownerTable_ != null ? ownerTable_.getMinChip() : 1;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — game type
    // -------------------------------------------------------------------------

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

    @Override
    public boolean isAllInShowdown() {
        return false;
    }

    @Override
    public boolean isStoredInDatabase() {
        return false;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — action history
    // -------------------------------------------------------------------------

    @Override
    public boolean isFolded(ClientPlayer player) {
        return player.isFolded();
    }

    @Override
    public boolean hasPlayerActed(ClientPlayer player) {
        return false;
    }

    @Override
    public int getLastAction(ClientPlayer player) {
        return HandAction.ACTION_NONE;
    }

    @Override
    public HandAction getLastAction() {
        return null;
    }

    @Override
    public int getLastActionThisRound(ClientPlayer player) {
        return HandAction.ACTION_NONE;
    }

    @Override
    public boolean isActionInRound(int nRound) {
        return false;
    }

    @Override
    public int getFoldRound(ClientPlayer player) {
        return -1;
    }

    @Override
    public int getOverbet(ClientPlayer player) {
        return 0;
    }

    @Override
    public int getNumPotsExcludingOverbets() {
        int count = 0;
        for (ClientPot pot : remotePots_) {
            if (pot.eligiblePlayerIds().size() > 1)
                count++;
        }
        return count;
    }

    @Override
    public boolean isNoLimit() {
        return getGameType() == ProtocolConstants.TYPE_NO_LIMIT_HOLDEM;
    }

    @Override
    public boolean isPotLimit() {
        return getGameType() == ProtocolConstants.TYPE_POT_LIMIT_HOLDEM;
    }

    @Override
    public long getStartDate() {
        return 0;
    }

    @Override
    public long getEndDate() {
        return 0;
    }

    @Override
    public List<HandAction> getHistoryCopy() {
        return Collections.emptyList();
    }

    @Override
    public int getHistorySize() {
        return 0;
    }

    @Override
    public int getTotalBet(ClientPlayer player) {
        return 0;
    }

    @Override
    public int getNumPriorRaises(ClientPlayer player) {
        return 0;
    }

    // -------------------------------------------------------------------------
    // ClientHoldemHand — win recording
    // -------------------------------------------------------------------------

    @Override
    public void wins(ClientPlayer player, int nChips, int nPot) {
        remoteWins_.merge(player.getID(), nChips, Integer::sum);
    }

    @Override
    public int getWin(ClientPlayer player) {
        return remoteWins_.getOrDefault(player.getID(), 0);
    }

    // -------------------------------------------------------------------------
    // Blind / ante accessors
    // -------------------------------------------------------------------------

    public void setSmallBlind(int n) {
        remoteSmallBlind_ = n;
    }

    public void setBigBlind(int n) {
        remoteBigBlind_ = n;
    }

    public void setAnte(int n) {
        remoteAnte_ = n;
    }

    @Override
    public int getSmallBlind() {
        return remoteSmallBlind_;
    }

    @Override
    public int getBigBlind() {
        return remoteBigBlind_;
    }

    @Override
    public int getAnte() {
        return remoteAnte_;
    }

    // -------------------------------------------------------------------------
    // State update methods (called by WebSocketTournamentDirector)
    // -------------------------------------------------------------------------

    public void updateRound(ClientBettingRound round) {
        this.remoteRound_ = round;
    }

    public void updateCommunity(ClientHand community) {
        this.remoteCommunity_ = community != null ? community : ClientHand.empty();
    }

    public void updatePlayerOrder(List<ClientPlayer> players) {
        this.remotePlayers_ = new ArrayList<>(players);
        if (remoteCurrentPlayerIndex_ >= remotePlayers_.size()) {
            remoteCurrentPlayerIndex_ = NO_CURRENT_PLAYER;
        }
    }

    public void updateCurrentPlayer(int index) {
        this.remoteCurrentPlayerIndex_ = index;
    }

    public void updatePot(int totalPot) {
        this.remotePotTotal_ = totalPot;
    }

    public void updateActionOptions(ActionOptionsData opts) {
        this.remoteOptions_ = opts;
    }

    public void updatePlayerBet(int playerId, int totalBet) {
        if (totalBet > 0) {
            remoteBets_.put(playerId, totalBet);
        } else {
            remoteBets_.remove(playerId);
        }
    }

    public void clearBets() {
        remoteBets_.clear();
    }

    public void clearWins() {
        remoteWins_.clear();
    }

    public void updateSmallBlindSeat(int seat) {
        this.remoteSmallBlindSeat_ = seat;
    }

    public void updateBigBlindSeat(int seat) {
        this.remoteBigBlindSeat_ = seat;
    }

    public int getRemoteSmallBlindSeat() {
        return remoteSmallBlindSeat_;
    }

    public int getRemoteBigBlindSeat() {
        return remoteBigBlindSeat_;
    }

    // -------------------------------------------------------------------------
    // Deck / muck — not available for remote hands
    // -------------------------------------------------------------------------

    @Override
    public ClientHand getMuck() {
        throw new UnsupportedOperationException("Muck not available for remote hands");
    }
}
