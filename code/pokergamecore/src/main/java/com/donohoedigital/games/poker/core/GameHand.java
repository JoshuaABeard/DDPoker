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
import com.donohoedigital.games.poker.engine.Card;

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

    // === Card Access (for V2 AI) ===

    /**
     * Get community cards currently dealt.
     *
     * @return array of community cards (0-5 cards depending on round), or null if
     *         none dealt
     */
    Card[] getCommunityCards();

    /**
     * Get a player's hole cards.
     *
     * @param player
     *            the player
     * @return array of 2 hole cards, or null if player folded or not in hand
     */
    Card[] getPlayerCards(GamePlayerInfo player);

    // === Pot State (for V2 AI) ===

    /**
     * Get total chips in the pot (all bets from all rounds).
     *
     * @return total pot size in chips
     */
    int getPotSize();

    /**
     * Get pot status for current betting situation.
     *
     * @return pot status constant (NO_POT_ACTION, RAISED_POT, RERAISED_POT, etc.)
     * @see com.donohoedigital.games.poker.engine.PokerConstants
     */
    int getPotStatus();

    /**
     * Calculate pot odds for a player (pot size / amount to call).
     *
     * @param player
     *            the player
     * @return pot odds as ratio (e.g., 3.5 means getting 3.5:1 odds)
     */
    float getPotOdds(GamePlayerInfo player);

    // === Betting History (for V2 AI) ===

    /**
     * Check if pot was raised pre-flop.
     *
     * @return true if there was at least one raise pre-flop
     */
    boolean wasRaisedPreFlop();

    /**
     * Get first player to bet in a given round.
     *
     * @param round
     *            the betting round (PRE_FLOP, FLOP, TURN, RIVER)
     * @param includeRaises
     *            if true, consider raises as first bet; if false, only initial bets
     * @return first bettor, or null if no one bet
     */
    GamePlayerInfo getFirstBettor(int round, boolean includeRaises);

    /**
     * Get last player to bet/raise in a given round.
     *
     * @param round
     *            the betting round
     * @param includeRaises
     *            if true, consider raises; if false, only initial bets
     * @return last bettor, or null if no one bet
     */
    GamePlayerInfo getLastBettor(int round, boolean includeRaises);

    /**
     * Check if player was first raiser pre-flop.
     *
     * @param player
     *            the player to check
     * @return true if player made first raise pre-flop
     */
    boolean wasFirstRaiserPreFlop(GamePlayerInfo player);

    /**
     * Check if player was last raiser pre-flop.
     *
     * @param player
     *            the player to check
     * @return true if player made last raise pre-flop
     */
    boolean wasLastRaiserPreFlop(GamePlayerInfo player);

    /**
     * Check if player was the only raiser pre-flop.
     *
     * @param player
     *            the player to check
     * @return true if player raised and no one else raised
     */
    boolean wasOnlyRaiserPreFlop(GamePlayerInfo player);

    /**
     * Check if there was any betting action in a round.
     *
     * @param round
     *            the betting round to check
     * @return true if there was at least one bet/raise
     */
    boolean wasPotAction(int round);

    // === Player State (for V2 AI) ===

    /**
     * Check if player paid to enter the hand (posted blind or called).
     *
     * @param player
     *            the player
     * @return true if player put money in pre-flop
     */
    boolean paidToPlay(GamePlayerInfo player);

    /**
     * Check if player could have limped (no raise before their first action).
     *
     * @param player
     *            the player
     * @return true if calling big blind was an option for first action
     */
    boolean couldLimp(GamePlayerInfo player);

    /**
     * Check if player limped (called big blind when they could have).
     *
     * @param player
     *            the player
     * @return true if player limped
     */
    boolean limped(GamePlayerInfo player);

    /**
     * Check if player is in a blind position (small or big blind).
     *
     * @param player
     *            the player
     * @return true if player is small or big blind
     */
    boolean isBlind(GamePlayerInfo player);

    /**
     * Check if player has acted in current betting round.
     *
     * @param player
     *            the player
     * @return true if player has made a decision this round
     */
    boolean hasActedThisRound(GamePlayerInfo player);

    /**
     * Get player's last action in current round.
     *
     * @param player
     *            the player
     * @return action constant (FOLD, CHECK, CALL, BET, RAISE), or 0 if not acted
     */
    int getLastActionThisRound(GamePlayerInfo player);

    /**
     * Get player's first voluntary action in a specific round.
     *
     * @param player
     *            the player
     * @param round
     *            the betting round
     * @return action constant for first voluntary action
     */
    int getFirstVoluntaryAction(GamePlayerInfo player, int round);

    // === Counts (for V2 AI) ===

    /**
     * Count players who limped pre-flop.
     *
     * @return number of limpers
     */
    int getNumLimpers();

    /**
     * Count consecutive folds since last bet/raise.
     *
     * @return number of folds
     */
    int getNumFoldsSinceLastBet();
}
