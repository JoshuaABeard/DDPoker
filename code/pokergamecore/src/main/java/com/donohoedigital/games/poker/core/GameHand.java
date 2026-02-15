/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.core;

import com.donohoedigital.games.poker.core.state.BettingRound;

import java.util.List;

/**
 * Interface for hand operations that the core game engine needs. Implemented by
 * HoldemHand in Phase 2.
 */
public interface GameHand {
    /** @return current betting round */
    BettingRound getRound();

    /**
     * @param round
     *            the new betting round
     */
    void setRound(BettingRound round);

    /** @return true if the hand is complete */
    boolean isDone();

    /** @return number of players with cards (not folded) */
    int getNumWithCards();

    /** @return initial index of current player in betting order */
    int getCurrentPlayerInitIndex();

    /** Advance to the next betting round (pre-flop -> flop -> turn -> river). */
    void advanceRound();

    /**
     * Pre-resolve the hand before showdown (determine early winners/losers).
     *
     * @param isOnline
     *            true if online game
     */
    void preResolve(boolean isOnline);

    /** Resolve the hand at showdown, determine winners and award pots */
    void resolve();

    /** Store hand history for this hand */
    void storeHandHistory();

    /** @return list of players who won (or will win if uncontested) */
    List<GamePlayerInfo> getPreWinners();

    /** @return list of players who lost (or will lose) */
    List<GamePlayerInfo> getPreLosers();

    /** @return true if only one player remains (all others folded) */
    boolean isUncontested();

    /**
     * Get current player with initialization (sets up player order on first call).
     *
     * @return current player to act
     */
    GamePlayerInfo getCurrentPlayerWithInit();

    /**
     * Get amount player needs to call to stay in hand.
     *
     * @param player
     *            the player
     * @return amount to call (0 if no bet to call)
     */
    int getAmountToCall(GamePlayerInfo player);

    /**
     * Get minimum bet amount for current round.
     *
     * @return minimum bet
     */
    int getMinBet();

    /**
     * Get minimum raise amount for current round.
     *
     * @return minimum raise
     */
    int getMinRaise();

    /**
     * Process a player action from pokergamecore. Converts PlayerAction to internal
     * representation and updates hand state.
     *
     * @param player
     *            the player taking action
     * @param action
     *            the action from pokergamecore
     */
    void applyPlayerAction(GamePlayerInfo player, PlayerAction action);
}
