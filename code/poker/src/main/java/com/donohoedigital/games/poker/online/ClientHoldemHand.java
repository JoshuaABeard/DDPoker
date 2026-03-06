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

import java.util.List;

/**
 * Read-only view of a poker hand for Swing UI components.
 *
 * <p>
 * Implemented by {@link RemoteHoldemHand} (WebSocket-driven) and by
 * {@link com.donohoedigital.games.poker.HoldemHand} (local game engine). UI
 * code depends on this interface, not on the game-engine {@code HoldemHand}
 * class directly.
 *
 * <p>
 * Methods were added by compiler-driven discovery in Task 12.
 */
public interface ClientHoldemHand {

    // -------------------------------------------------------------------------
    // Table linkage
    // -------------------------------------------------------------------------

    /**
     * Returns the owning table as a {@link ClientPokerTable}. Works for both local
     * and remote tables.
     */
    ClientPokerTable getClientTable();

    // -------------------------------------------------------------------------
    // Betting round
    // -------------------------------------------------------------------------

    /** Returns the current betting round. */
    ClientBettingRound getRound();

    /**
     * Returns the legacy {@code int} constant for the current round (for code that
     * switches on {@code HoldemHand.ROUND_*} values).
     */
    int getRoundForDisplay();

    // -------------------------------------------------------------------------
    // Community cards
    // -------------------------------------------------------------------------

    /** Returns the community cards. */
    ClientHand getCommunity();

    /**
     * Returns the community cards adjusted for all-in showdown display (only cards
     * up to the previously completed round are shown).
     */
    ClientHand getCommunityForDisplay();

    /** Returns the community cards as a sorted hand. */
    ClientHand getCommunitySorted();

    // -------------------------------------------------------------------------
    // Players
    // -------------------------------------------------------------------------

    /** Returns the number of players in this hand. */
    int getNumPlayers();

    /**
     * Returns the number of players who still have cards (not folded) in this hand.
     */
    int getNumWithCards();

    /**
     * Returns the player at the given index in hand-order, or {@code null} if out
     * of range.
     */
    ClientPlayer getPlayerAt(int index);

    /** Returns the index of the currently acting player, or -999 if none. */
    int getCurrentPlayerIndex();

    /** Returns the currently acting player, or {@code null} if none. */
    ClientPlayer getCurrentPlayer();

    // -------------------------------------------------------------------------
    // Pot / chips
    // -------------------------------------------------------------------------

    /** Returns the total chip count across all pots. */
    int getTotalPotChipCount();

    /** Returns the number of pots. */
    int getNumPots();

    /** Returns the pot at the given index as a {@link ClientPot}. */
    ClientPot getPot(int index);

    // -------------------------------------------------------------------------
    // Bet / call amounts
    // -------------------------------------------------------------------------

    /**
     * Returns the bet placed by the given player in the given round.
     *
     * @param player
     *            the player
     * @param nRound
     *            legacy round integer ({@code HoldemHand.ROUND_*})
     */
    int getBet(ClientPlayer player, int nRound);

    /**
     * Returns the bet placed by the given player in the current round.
     *
     * @param player
     *            the player
     */
    int getBet(ClientPlayer player);

    /**
     * Returns the current highest bet in the active round (i.e., the amount the
     * next player would need to match to call).
     */
    int getBet();

    /**
     * Returns the amount the given player needs to add to call the current bet.
     *
     * @param player
     *            the player
     */
    int getCall(ClientPlayer player);

    /** Returns the minimum allowed bet for the current round. */
    int getMinBet();

    /** Returns the minimum allowed raise for the current round. */
    int getMinRaise();

    /**
     * Returns the maximum allowed bet for the given player.
     *
     * @param player
     *            the player
     */
    int getMaxBet(ClientPlayer player);

    /**
     * Returns the maximum allowed raise for the given player.
     *
     * @param player
     *            the player
     */
    int getMaxRaise(ClientPlayer player);

    /**
     * Returns the minimum chip denomination for this hand (used for amount slider
     * step).
     */
    int getMinChip();

    // -------------------------------------------------------------------------
    // Game type
    // -------------------------------------------------------------------------

    /**
     * Returns the game type constant (e.g.
     * {@code ProtocolConstants.TYPE_NO_LIMIT_HOLDEM}).
     */
    int getGameType();

    // -------------------------------------------------------------------------
    // Odds
    // -------------------------------------------------------------------------

    /**
     * Returns the pot odds for the given player as a value 0–100.
     *
     * @param player
     *            the player
     */
    float getPotOdds(ClientPlayer player);

    // -------------------------------------------------------------------------
    // State flags
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this hand is in an all-in showdown (all remaining
     * players are all-in and cards are being run out).
     */
    boolean isAllInShowdown();

    /**
     * Returns {@code true} if this hand has already been stored in the hand history
     * database.
     */
    boolean isStoredInDatabase();

    // -------------------------------------------------------------------------
    // Action history
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given player has folded in this hand.
     *
     * @param player
     *            the player
     */
    boolean isFolded(ClientPlayer player);

    /**
     * Returns {@code true} if the given player has taken an action in the current
     * round.
     *
     * @param player
     *            the player
     */
    boolean hasPlayerActed(ClientPlayer player);

    /**
     * Returns the last action type taken by the given player in the current round,
     * or {@code HandAction#ACTION_NONE} if none.
     *
     * @param player
     *            the player
     */
    int getLastAction(ClientPlayer player);

    /** Returns an unmodifiable copy of the full action history for this hand. */
    List<HandAction> getHistoryCopy();

    /** Returns the number of actions recorded in the history. */
    int getHistorySize();

    /**
     * Returns the last non-result action (non-WIN/LOSE/OVERBET) from the history,
     * or {@code null} if none.
     */
    HandAction getLastAction();

    /**
     * Returns {@code true} if the hand is uncontested (only one player with cards
     * remaining).
     */
    default boolean isUncontested() {
        return getNumWithCards() == 1;
    }

    /**
     * Returns the HTML snippet for hand history list display.
     */
    default String getHandListHTML() {
        return "";
    }

    /**
     * Returns the sum of all bets (including antes) placed by the given player
     * across all rounds in this hand.
     *
     * @param player
     *            the player
     */
    int getTotalBet(ClientPlayer player);

    /**
     * Returns the number of raises that occurred after the given player's last
     * action in the current round. Used by the bet-label display to select the
     * re-raise icon.
     *
     * @param player
     *            the player
     */
    int getNumPriorRaises(ClientPlayer player);

    // -------------------------------------------------------------------------
    // Blind / ante amounts
    // -------------------------------------------------------------------------

    /** Returns the small blind amount. */
    int getSmallBlind();

    /** Returns the big blind amount. */
    int getBigBlind();

    /** Returns the ante amount. */
    int getAnte();

    // -------------------------------------------------------------------------
    // Date / time
    // -------------------------------------------------------------------------

    /** Returns the hand start time in millis, or 0 if unknown. */
    long getStartDate();

    /** Returns the hand end time in millis, or 0 if unknown. */
    long getEndDate();

    // -------------------------------------------------------------------------
    // Game type helpers
    // -------------------------------------------------------------------------

    /** Returns {@code true} if this hand is no-limit. */
    boolean isNoLimit();

    /** Returns {@code true} if this hand is pot-limit. */
    boolean isPotLimit();

    // -------------------------------------------------------------------------
    // Round / action queries
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if there was any action in the given round.
     *
     * @param nRound
     *            legacy round integer ({@code ClientBettingRound.ROUND_*})
     */
    boolean isActionInRound(int nRound);

    /**
     * Returns the round in which the given player folded, as a legacy round
     * integer.
     *
     * @param player
     *            the player
     */
    int getFoldRound(ClientPlayer player);

    /**
     * Returns the overbet (excess chips returned) for the given player, or 0.
     *
     * @param player
     *            the player
     */
    int getOverbet(ClientPlayer player);

    /**
     * Returns the last action type taken by the given player in the current round,
     * or {@code HandAction#ACTION_NONE} if none. Unlike
     * {@link #getLastAction(ClientPlayer)}, this specifically queries the current
     * betting round only.
     *
     * @param player
     *            the player
     */
    int getLastActionThisRound(ClientPlayer player);

    /**
     * Returns the number of pots excluding overbet pots (pots with only one
     * eligible player).
     */
    int getNumPotsExcludingOverbets();

    // -------------------------------------------------------------------------
    // Win recording
    // -------------------------------------------------------------------------

    /**
     * Records that the given player won {@code nChips} chips from pot {@code nPot}.
     *
     * @param player
     *            the winning player
     * @param nChips
     *            chips awarded
     * @param nPot
     *            pot index
     */
    void wins(ClientPlayer player, int nChips, int nPot);

    /**
     * Returns the total chips won by the given player in this hand, or {@code 0}.
     *
     * @param player
     *            the player
     */
    int getWin(ClientPlayer player);

    // -------------------------------------------------------------------------
    // Deck / muck (local cheat mode only)
    // -------------------------------------------------------------------------

    /**
     * Returns the muck (folded/discarded cards) for this hand (local games only).
     */
    ClientHand getMuck();
}
