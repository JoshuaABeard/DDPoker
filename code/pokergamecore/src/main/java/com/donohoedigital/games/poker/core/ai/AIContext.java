/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.core.ai;

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.engine.Card;

/**
 * Provides AI with read-only access to game state for decision making.
 * <p>
 * This interface abstracts game state queries in a Swing-free way, allowing AI
 * implementations to work in any environment (desktop, server, tests).
 * <p>
 * The context provides:
 * <ul>
 * <li>Table state - players, positions, button</li>
 * <li>Hand state - hole cards, community cards, betting round</li>
 * <li>Tournament state - blinds, antes, levels</li>
 * <li>Pot state - pot size, bets, raises</li>
 * <li>Player state - stacks, actions, status</li>
 * <li>Hand evaluation - strength calculations</li>
 * </ul>
 *
 * @see PurePokerAI
 */
public interface AIContext {

    // ========== Table Queries ==========

    /**
     * Get the table being played.
     *
     * @return Current table state
     */
    GameTable getTable();

    /**
     * Get the current hand being played.
     *
     * @return Current hand state, or {@code null} if no hand in progress
     */
    GameHand getCurrentHand();

    /**
     * Get tournament context (blinds, levels, etc.).
     *
     * @return Tournament context
     */
    TournamentContext getTournament();

    // ========== Position Queries ==========

    /**
     * Is the given player on the button?
     *
     * @param player
     *            Player to check
     * @return {@code true} if player is on button
     */
    boolean isButton(GamePlayerInfo player);

    /**
     * Is the given player in the small blind position?
     *
     * @param player
     *            Player to check
     * @return {@code true} if player is small blind
     */
    boolean isSmallBlind(GamePlayerInfo player);

    /**
     * Is the given player in the big blind position?
     *
     * @param player
     *            Player to check
     * @return {@code true} if player is big blind
     */
    boolean isBigBlind(GamePlayerInfo player);

    /**
     * Get relative position (early, middle, late, button, small blind, big blind).
     * <p>
     * Position values:
     * <ul>
     * <li>0 = Early position</li>
     * <li>1 = Middle position</li>
     * <li>2 = Late position</li>
     * <li>3 = Button</li>
     * <li>4 = Small blind</li>
     * <li>5 = Big blind</li>
     * </ul>
     *
     * @param player
     *            Player to check
     * @return Position code (0-5)
     */
    int getPosition(GamePlayerInfo player);

    // ========== Pot Queries ==========

    /**
     * Get total pot size (main pot + side pots).
     *
     * @return Total chips in pot
     */
    int getPotSize();

    /**
     * Get amount player must call to stay in hand.
     *
     * @param player
     *            Player to check
     * @return Amount to call, or 0 if no bet to call
     */
    int getAmountToCall(GamePlayerInfo player);

    /**
     * Get amount player has already bet this round.
     *
     * @param player
     *            Player to check
     * @return Amount bet this round
     */
    int getAmountBetThisRound(GamePlayerInfo player);

    /**
     * Get the last bet or raise amount.
     *
     * @return Last bet/raise amount, or 0 if no betting yet
     */
    int getLastBetAmount();

    // ========== Player Queries ==========

    /**
     * Get number of active players (not folded, not eliminated).
     *
     * @return Count of active players
     */
    int getNumActivePlayers();

    /**
     * Get number of players yet to act this round.
     *
     * @param player
     *            The player checking
     * @return Count of players after this player who haven't acted
     */
    int getNumPlayersYetToAct(GamePlayerInfo player);

    /**
     * Get number of players who have already acted this round.
     *
     * @param player
     *            The player checking
     * @return Count of players before this player who have acted
     */
    int getNumPlayersWhoActed(GamePlayerInfo player);

    /**
     * Has there been a bet or raise this round?
     *
     * @return {@code true} if someone has bet or raised
     */
    boolean hasBeenBet();

    /**
     * Has there been a raise this round?
     *
     * @return {@code true} if someone has raised
     */
    boolean hasBeenRaised();

    /**
     * Get the player who made the last bet.
     *
     * @return Player who bet, or {@code null} if no bet
     */
    GamePlayerInfo getLastBettor();

    /**
     * Get the player who made the last raise.
     *
     * @return Player who raised, or {@code null} if no raise
     */
    GamePlayerInfo getLastRaiser();

    // ========== Hand Evaluation ==========

    /**
     * Evaluate hand strength.
     * <p>
     * Returns hand rank (0-9):
     * <ul>
     * <li>9 = Royal Flush</li>
     * <li>8 = Straight Flush</li>
     * <li>7 = Four of a Kind</li>
     * <li>6 = Full House</li>
     * <li>5 = Flush</li>
     * <li>4 = Straight</li>
     * <li>3 = Three of a Kind</li>
     * <li>2 = Two Pair</li>
     * <li>1 = One Pair</li>
     * <li>0 = High Card</li>
     * </ul>
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards depending on round)
     * @return Hand rank (0-9)
     */
    int evaluateHandRank(Card[] holeCards, Card[] communityCards);

    /**
     * Get detailed hand score (for comparing hands of same rank).
     * <p>
     * Score is comparable: higher score = better hand.
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards)
     * @return Detailed hand score for comparison
     */
    long evaluateHandScore(Card[] holeCards, Card[] communityCards);

    /**
     * Calculate probability of improving hand.
     * <p>
     * Estimates chance of improving on next card (turn or river).
     *
     * @param holeCards
     *            Player's hole cards
     * @param communityCards
     *            Community cards so far
     * @return Probability of improvement (0.0 to 1.0)
     */
    double calculateImprovementOdds(Card[] holeCards, Card[] communityCards);

    // ========== Betting Round ==========

    /**
     * Get current betting round.
     * <p>
     * Rounds:
     * <ul>
     * <li>0 = Pre-flop</li>
     * <li>1 = Flop</li>
     * <li>2 = Turn</li>
     * <li>3 = River</li>
     * </ul>
     *
     * @return Betting round code (0-3)
     */
    int getBettingRound();
}
