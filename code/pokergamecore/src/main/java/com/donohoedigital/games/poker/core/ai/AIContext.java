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

    /**
     * Get the best 5-card hand from hole cards and community cards.
     * <p>
     * Returns the 5 cards that form the best poker hand, ordered by rank (highest
     * first). This is used for analyzing hand strength and determining which cards
     * contribute to the final hand.
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards)
     * @return Array of 5 Card objects forming the best hand, ordered by rank
     *         (highest first)
     */
    Card[] getBest5Cards(Card[] holeCards, Card[] communityCards);

    /**
     * Get the best 5-card hand ranks (for comparison without Card objects).
     * <p>
     * Returns the ranks of the 5 cards that form the best poker hand, ordered by
     * rank (highest first). This is a lighter-weight alternative to
     * {@link #getBest5Cards} when only rank information is needed.
     * <p>
     * Ranks use Card constants:
     * <ul>
     * <li>14 = {@link Card#ACE}</li>
     * <li>13 = {@link Card#KING}</li>
     * <li>12 = {@link Card#QUEEN}</li>
     * <li>...</li>
     * <li>2 = {@link Card#TWO}</li>
     * </ul>
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards)
     * @return Array of 5 integers (Card.ACE, Card.KING, etc.) ordered highest first
     */
    int[] getBest5CardRanks(Card[] holeCards, Card[] communityCards);

    /**
     * Check if at least one hole card is part of the best 5-card hand.
     * <p>
     * Critical for determining hand strength when the best hand can be made from
     * the board alone. If neither hole card is involved, the player's hand is only
     * as strong as the board, and opponents with any hole card involvement will
     * win.
     * <p>
     * For example:
     * <ul>
     * <li>Board: A♠ K♠ Q♠ J♠ 10♠ (royal flush on board)</li>
     * <li>Hole: 7♣ 2♦</li>
     * <li>Result: {@code false} - neither hole card used, tie with all players</li>
     * </ul>
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards)
     * @return {@code true} if at least one hole card is in the best 5-card hand
     */
    boolean isHoleCardInvolved(Card[] holeCards, Card[] communityCards);

    /**
     * Get the major suit for flush detection.
     * <p>
     * Returns the suit that appears most frequently in the combined hole and
     * community cards. This is used for detecting flush draws and determining nut
     * flush potential.
     * <p>
     * Suits use Card constants:
     * <ul>
     * <li>0 = {@link Card#CLUBS}</li>
     * <li>1 = {@link Card#DIAMONDS}</li>
     * <li>2 = {@link Card#HEARTS}</li>
     * <li>3 = {@link Card#SPADES}</li>
     * </ul>
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards)
     * @return Suit constant (0-3), or -1 if no flush possible
     */
    int getMajorSuit(Card[] holeCards, Card[] communityCards);

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

    // ========== Card Access ==========

    /**
     * Get a player's hole cards.
     * <p>
     * Returns the private cards dealt to the player.
     * <p>
     * <strong>SECURITY:</strong> Implementations MUST enforce that AI can only
     * access its own hole cards, never opponents' cards. If {@code player} is not
     * the AI player, implementations should return {@code null}. This prevents AI
     * cheating by seeing opponents' private cards.
     * <p>
     * Example enforcement:
     *
     * <pre>
     * {@code
     * public Card[] getHoleCards(GamePlayerInfo player) {
     *     if (player != this.aiPlayer) {
     *         return null; // Cannot see opponent cards
     *     }
     *     return player.getHand().getCards();
     * }
     * }
     * </pre>
     *
     * @param player
     *            Player whose hole cards to retrieve (must be the AI player)
     * @return Array of 2 hole cards if {@code player} is the AI, {@code null}
     *         otherwise
     */
    Card[] getHoleCards(GamePlayerInfo player);

    /**
     * Get community cards currently visible.
     * <p>
     * Returns cards based on current betting round:
     * <ul>
     * <li>Pre-flop (round 0): empty array or {@code null}</li>
     * <li>Flop (round 1): 3 cards</li>
     * <li>Turn (round 2): 4 cards</li>
     * <li>River (round 3): 5 cards</li>
     * </ul>
     *
     * @return Community cards, or empty array/null if none visible yet
     */
    Card[] getCommunityCards();

    /**
     * Get number of players who have called (but not raised) this round.
     * <p>
     * Used for pre-flop decision making to distinguish between limping and raising
     * pots.
     *
     * @return Count of players who called
     */
    int getNumCallers();

    // ========== Board Texture Analysis ==========

    /**
     * Check if there's a flush draw on the board.
     * <p>
     * Returns {@code true} if exactly 2 cards of the same suit are on the board,
     * indicating a flush draw is possible.
     * <p>
     * Extracted from V1Player logic for board texture analysis.
     *
     * @param communityCards
     *            Community cards to analyze
     * @return {@code true} if 2 cards of same suit on board
     */
    boolean hasFlushDraw(Card[] communityCards);

    /**
     * Check if there's a straight draw possible on the board.
     * <p>
     * Analyzes community cards to detect if a straight draw is possible based on
     * card connectivity.
     * <p>
     * Extracted from V1Player logic (Hand.hasStraightDraw).
     *
     * @param communityCards
     *            Community cards to analyze
     * @return {@code true} if straight draw is possible
     */
    boolean hasStraightDraw(Card[] communityCards);

    /**
     * Count number of possible straights opponents could have.
     * <p>
     * Analyzes board texture to determine how many different straight combinations
     * are possible given the community cards. Used for risk assessment when holding
     * strong hands.
     * <p>
     * Extracted from V1Player logic (PokerPlayer.getOppNumStraights).
     *
     * @param communityCards
     *            Community cards to analyze
     * @return Count of possible opponent straights (0+)
     */
    int getNumOpponentStraights(Card[] communityCards);

    /**
     * Check if tournament rebuy period is still active.
     * <p>
     * During rebuy period, players can rebuy if eliminated or their stack falls
     * below a threshold. This affects AI strategy - the AI plays looser during
     * rebuy period (taking more risks).
     * <p>
     * Extracted from V1Player logic - adjusts tight factor by -20 during rebuy.
     *
     * @return {@code true} if rebuy period is still active
     */
    boolean isRebuyPeriodActive();

    /**
     * Check if player has the nut flush (or close to it).
     * <p>
     * Analyzes whether the player holds the best possible flush given the board.
     * The {@code nCards} parameter allows checking for "near-nut" flushes (2nd or
     * 3rd best).
     * <p>
     * Extracted from V1Player logic which uses HandInfo.isNutFlush().
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (3-5 cards)
     * @param majorSuit
     *            The flush suit (from getMajorSuit)
     * @param nCards
     *            How close to nut: 1 = nut only, 3 = nut/2nd/3rd
     * @return {@code true} if player has nut flush or within nCards of nut
     */
    boolean isNutFlush(Card[] holeCards, Card[] communityCards, int majorSuit, int nCards);

    /**
     * Calculate hand strength using Monte Carlo simulation.
     * <p>
     * Returns win probability (0.0 to 1.0) against N random opponents by running
     * Monte Carlo simulations. This is expensive but provides accurate strength
     * assessment that varies within the same hand rank (e.g., pair of Aces vs pair
     * of 2s).
     * <p>
     * Extracted from V1Player which uses {@code player.getHandStrength() * 100.0f}
     * at line 712.
     *
     * @param holeCards
     *            Player's hole cards (2 cards)
     * @param communityCards
     *            Community cards (0-5 cards)
     * @param numOpponents
     *            Number of opponents to simulate against
     * @return Win probability (0.0 to 1.0)
     */
    double calculateHandStrength(Card[] holeCards, Card[] communityCards, int numOpponents);

    // ========== Action History ==========

    /**
     * Action constants for player actions (matches HandAction constants).
     */
    int ACTION_NONE = 0;
    int ACTION_FOLD = 1;
    int ACTION_CHECK = 2;
    int ACTION_CALL = 3;
    int ACTION_BET = 4;
    int ACTION_RAISE = 5;

    /**
     * Get player's last action in a specific betting round.
     * <p>
     * Used for limper detection in V1Player lines 1293-1349. Checks if a
     * raiser/bettor limped (called or checked) in previous rounds to adjust fold
     * percentages.
     *
     * @param player
     *            Player to check action for
     * @param bettingRound
     *            Betting round to check (0=pre-flop, 1=flop, 2=turn, 3=river)
     * @return Action constant (ACTION_NONE, ACTION_CALL, ACTION_CHECK, ACTION_BET,
     *         ACTION_RAISE, ACTION_FOLD)
     */
    int getLastActionInRound(GamePlayerInfo player, int bettingRound);

    // ========== Opponent Modeling ==========

    /**
     * Get opponent's raise frequency for current betting round.
     * <p>
     * Returns percentage (0-100) representing how often the player raises when they
     * act. Used for bluffing decisions in V1Player lines 1479, 1488.
     * <p>
     * Extracted from V1Player which uses
     * {@code player.getProfileInitCheck().getFrequency()}
     *
     * @param opponent
     *            Opponent to check frequency for
     * @param bettingRound
     *            Betting round to check (0=pre-flop, 1=flop, 2=turn, 3=river)
     * @return Raise frequency percentage (0-100), or 50 if unknown
     */
    int getOpponentRaiseFrequency(GamePlayerInfo opponent, int bettingRound);

    /**
     * Get opponent's bet frequency for current betting round.
     * <p>
     * Returns percentage (0-100) representing how often the player bets when they
     * act. Used for bluffing decisions.
     * <p>
     * Extracted from V1Player which uses
     * {@code player.getProfileInitCheck().getFrequency()}
     *
     * @param opponent
     *            Opponent to check frequency for
     * @param bettingRound
     *            Betting round to check (0=pre-flop, 1=flop, 2=turn, 3=river)
     * @return Bet frequency percentage (0-100), or 50 if unknown
     */
    int getOpponentBetFrequency(GamePlayerInfo opponent, int bettingRound);
}
