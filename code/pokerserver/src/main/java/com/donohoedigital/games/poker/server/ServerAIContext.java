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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.AIContext;
import com.donohoedigital.games.poker.core.ai.HandInfoFast;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFaster;
import com.donohoedigital.games.poker.engine.HandScoreConstants;
import com.donohoedigital.games.poker.engine.HandSorted;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-side implementation of AIContext for providing game state to AI.
 * <p>
 * <strong>Status:</strong> Minimal implementation for Phase 7D
 * <p>
 * Currently provides only the essential methods needed by TournamentAI
 * (tournament context for M-ratio calculation). Other methods return stub
 * values.
 * <p>
 * <strong>Future:</strong> When V1/V2 algorithms are extracted, implement
 * remaining methods (hand evaluation, position queries, pot queries, etc.).
 *
 * @see AIContext
 * @see ServerAIProvider
 */
public class ServerAIContext implements AIContext {

    private final GameTable table;
    private GameHand currentHand; // Mutable - updated per hand
    private final TournamentContext tournament;
    private final GamePlayerInfo aiPlayer;
    private final HandInfoFaster handEvaluator;
    protected final ServerOpponentTracker opponentTracker;

    // Action tracking (per hand)
    private final Map<Integer, int[]> playerActionsPerRound = new HashMap<>(); // [playerID][round] = action
    private final Map<Integer, int[]> playerBetsPerRound = new HashMap<>(); // [playerID][round] = amount
    private int lastBetAmount = 0;

    /**
     * Create AI context for server game.
     *
     * @param table
     *            Current table state
     * @param currentHand
     *            Current hand being played (or null between hands)
     * @param tournament
     *            Tournament context for blind structure
     * @param aiPlayer
     *            The AI player this context is for (used for rebuy period check)
     * @param opponentTracker
     *            Shared opponent tracker for behavioral statistics
     */
    public ServerAIContext(GameTable table, GameHand currentHand, TournamentContext tournament, GamePlayerInfo aiPlayer,
            ServerOpponentTracker opponentTracker) {
        this.table = table;
        this.currentHand = currentHand;
        this.tournament = tournament;
        this.aiPlayer = aiPlayer;
        this.handEvaluator = new HandInfoFaster();
        this.opponentTracker = opponentTracker;
    }

    /**
     * Update the current hand reference. Called at the start of each hand.
     *
     * @param hand
     *            The new current hand
     */
    public void setCurrentHand(GameHand hand) {
        this.currentHand = hand;
        // Reset per-hand tracking
        playerActionsPerRound.clear();
        playerBetsPerRound.clear();
        lastBetAmount = 0;
    }

    /**
     * Track player action for later queries. Called after each player acts.
     *
     * @param player
     *            Player who acted
     * @param action
     *            Action taken (HandAction constants)
     * @param amount
     *            Amount bet/raised
     * @param round
     *            Betting round
     */
    public void onPlayerAction(GamePlayerInfo player, int action, int amount, int round) {
        playerActionsPerRound.computeIfAbsent(player.getID(), k -> new int[4])[round] = action;

        if (action == HandAction.ACTION_BET || action == HandAction.ACTION_RAISE) {
            lastBetAmount = amount;
        }

        playerBetsPerRound.computeIfAbsent(player.getID(), k -> new int[4])[round] += amount;
    }

    /**
     * Convert Card array to Hand object.
     *
     * @param cards
     *            Array of cards (can be null or empty)
     * @return Hand object, or null if cards is null/empty
     */
    private Hand toHand(Card[] cards) {
        if (cards == null || cards.length == 0) {
            return null;
        }
        Hand hand = new Hand(cards.length);
        for (Card card : cards) {
            if (card != null) {
                hand.addCard(card);
            }
        }
        return hand;
    }

    // ========== Implemented Methods (used by TournamentAI) ==========

    @Override
    public GameTable getTable() {
        return table;
    }

    @Override
    public GameHand getCurrentHand() {
        return currentHand;
    }

    @Override
    public TournamentContext getTournament() {
        return tournament;
    }

    // ========== Game State Methods ==========

    @Override
    public boolean isButton(GamePlayerInfo player) {
        if (player == null || table == null) {
            return false;
        }
        return table.getSeat(player) == table.getButton();
    }

    @Override
    public boolean isSmallBlind(GamePlayerInfo player) {
        if (player == null || table == null) {
            return false;
        }
        int seat = table.getSeat(player);
        int smallBlindSeat = calculateSmallBlindSeat();
        return seat == smallBlindSeat;
    }

    @Override
    public boolean isBigBlind(GamePlayerInfo player) {
        if (player == null || table == null) {
            return false;
        }
        int seat = table.getSeat(player);
        int bigBlindSeat = calculateBigBlindSeat();
        return seat == bigBlindSeat;
    }

    @Override
    public int getPosition(GamePlayerInfo player) {
        if (player == null || table == null) {
            return 0;
        }
        int seat = table.getSeat(player);
        int button = table.getButton();
        int numSeats = table.getSeats();
        // Position 0 = button, increases clockwise
        return (seat - button + numSeats) % numSeats;
    }

    /**
     * Calculate small blind seat based on button position and number of players. In
     * heads-up, button is small blind. Otherwise, small blind is 1 seat after
     * button.
     */
    private int calculateSmallBlindSeat() {
        int button = table.getButton();
        int numSeats = table.getSeats();
        int numOccupied = table.getNumOccupiedSeats();

        // In heads-up, button is small blind
        if (numOccupied == 2) {
            return button;
        }

        // Otherwise, small blind is 1 seat after button
        return (button + 1) % numSeats;
    }

    /**
     * Calculate big blind seat based on button position and number of players. In
     * heads-up, non-button is big blind. Otherwise, big blind is 2 seats after
     * button.
     */
    private int calculateBigBlindSeat() {
        int button = table.getButton();
        int numSeats = table.getSeats();
        int numOccupied = table.getNumOccupiedSeats();

        // In heads-up, big blind is opposite of button
        // Since button is small blind in heads-up, big blind is the other player
        if (numOccupied == 2) {
            // Find the first occupied seat that isn't the button
            for (int i = 0; i < numSeats; i++) {
                if (i != button && table.getPlayer(i) != null) {
                    return i;
                }
            }
        }

        // Otherwise, big blind is 2 seats after button
        return (button + 2) % numSeats;
    }

    @Override
    public int getPotSize() {
        if (currentHand == null) {
            return 0;
        }
        return currentHand.getPotSize();
    }

    @Override
    public int getAmountToCall(GamePlayerInfo player) {
        if (currentHand == null) {
            return 0;
        }
        return currentHand.getAmountToCall(player);
    }

    @Override
    public int getAmountBetThisRound(GamePlayerInfo player) {
        if (player == null || currentHand == null) {
            return 0;
        }
        int round = currentHand.getRound().toLegacy();
        int[] bets = playerBetsPerRound.get(player.getID());
        return bets != null && round >= 0 && round < bets.length ? bets[round] : 0;
    }

    @Override
    public int getLastBetAmount() {
        return lastBetAmount;
    }

    @Override
    public int getNumActivePlayers() {
        if (currentHand == null) {
            return 0;
        }
        return currentHand.getNumWithCards();
    }

    @Override
    public int getNumPlayersYetToAct(GamePlayerInfo player) {
        if (table == null || currentHand == null || player == null) {
            return 0;
        }
        int count = 0;
        int playerSeat = table.getSeat(player);
        int numSeats = table.getSeats();

        // Count players after this player who haven't acted yet
        for (int i = 1; i < numSeats; i++) {
            int seat = (playerSeat + i) % numSeats;
            GamePlayerInfo p = table.getPlayer(seat);
            if (p != null && !currentHand.hasActedThisRound(p)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getNumPlayersWhoActed(GamePlayerInfo player) {
        if (table == null || currentHand == null) {
            return 0;
        }
        int count = 0;
        int numSeats = table.getSeats();

        // Count all players who have acted this round
        for (int i = 0; i < numSeats; i++) {
            GamePlayerInfo p = table.getPlayer(i);
            if (p != null && currentHand.hasActedThisRound(p)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean hasBeenBet() {
        if (currentHand == null) {
            return false;
        }
        int round = currentHand.getRound().toLegacy();
        return currentHand.wasPotAction(round);
    }

    @Override
    public boolean hasBeenRaised() {
        if (currentHand == null) {
            return false;
        }
        int round = currentHand.getRound().toLegacy();
        if (round == 0) {
            // Pre-flop: check if there was a raise
            return currentHand.wasRaisedPreFlop();
        } else {
            // Post-flop: check if there's a raiser this round
            return currentHand.getLastBettor(round, false) != null;
        }
    }

    @Override
    public GamePlayerInfo getLastBettor() {
        if (currentHand == null) {
            return null;
        }
        int round = currentHand.getRound().toLegacy();
        return currentHand.getLastBettor(round, true); // true = include bets and raises
    }

    @Override
    public GamePlayerInfo getLastRaiser() {
        if (currentHand == null) {
            return null;
        }
        int round = currentHand.getRound().toLegacy();
        return currentHand.getLastBettor(round, false); // false = raises only
    }

    @Override
    public int evaluateHandRank(Card[] holeCards, Card[] communityCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);
        int score = handEvaluator.getScore(hole, community);
        return HandInfoFast.getTypeFromScore(score);
    }

    @Override
    public long evaluateHandScore(Card[] holeCards, Card[] communityCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);
        return handEvaluator.getScore(hole, community);
    }

    @Override
    public int getBettingRound() {
        if (currentHand == null) {
            return 0; // PRE_FLOP
        }
        return currentHand.getRound().toLegacy();
    }

    @Override
    public Card[] getHoleCards(GamePlayerInfo player) {
        // SECURITY: AI can only see its own cards, not opponents' cards
        if (player != aiPlayer) {
            return null;
        }
        if (currentHand == null) {
            return null;
        }
        return currentHand.getPlayerCards(player);
    }

    @Override
    public Card[] getCommunityCards() {
        if (currentHand == null) {
            return null;
        }
        return currentHand.getCommunityCards();
    }

    @Override
    public int getNumCallers() {
        if (currentHand == null) {
            return 0;
        }
        return currentHand.getNumLimpers();
    }

    @Override
    public Card[] getBest5Cards(Card[] holeCards, Card[] communityCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);
        int score = handEvaluator.getScore(hole, community);

        // Get the best 5 card ranks
        int[] ranks = new int[5];
        HandInfoFast.getCards(score, ranks);

        // Get the major suit for this hand
        int majorSuit = handEvaluator.getLastMajorSuit();

        // Build the best 5 cards from ranks and suit
        // Note: This is a simplified implementation that creates cards from ranks
        // The actual cards from the board may differ, but ranks are what matter for
        // hand strength
        Card[] best5 = new Card[5];
        for (int i = 0; i < 5; i++) {
            // Use major suit for flushes, otherwise distribute suits
            // This is simplified - real implementation would track actual cards used
            int suitRank = majorSuit >= 0 ? majorSuit : (i % 4);
            best5[i] = new Card(CardSuit.forRank(suitRank), ranks[i]);
        }
        return best5;
    }

    @Override
    public int[] getBest5CardRanks(Card[] holeCards, Card[] communityCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);
        int score = handEvaluator.getScore(hole, community);

        int[] ranks = new int[5];
        HandInfoFast.getCards(score, ranks);
        return ranks;
    }

    @Override
    public boolean isHoleCardInvolved(Card[] holeCards, Card[] communityCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        if (hole == null || hole.size() == 0) {
            return false;
        }

        int score = handEvaluator.getScore(hole, community);
        int suit = handEvaluator.getLastMajorSuit();

        // Check if hole cards are part of the best hand
        return isOurHandInvolved(hole, score, suit, false);
    }

    @Override
    public int getMajorSuit(Card[] holeCards, Card[] communityCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        // Calculate score to populate major suit
        handEvaluator.getScore(hole, community);

        // Get the major suit from last calculation
        int majorSuit = handEvaluator.getLastMajorSuit();

        // Return -1 if no flush possible (major suit would be set only if 5+ of same
        // suit)
        // Check if we actually have a flush (5+ cards of same suit)
        int handRank = HandInfoFast.getTypeFromScore(handEvaluator.getScore(hole, community));
        if (handRank < HandScoreConstants.FLUSH) {
            return -1; // Not a flush, so major suit is irrelevant
        }

        return majorSuit;
    }

    @Override
    public boolean hasFlushDraw(Card[] communityCards) {
        if (communityCards == null || communityCards.length < 2) {
            return false;
        }

        // Count suits - flush draw = exactly 2 of same suit
        int[] suitCounts = new int[4];
        for (Card card : communityCards) {
            if (card != null) {
                suitCounts[card.getSuit()]++;
            }
        }

        for (int count : suitCounts) {
            if (count == 2) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasStraightDraw(Card[] communityCards) {
        if (communityCards == null || communityCards.length < 3) {
            return false;
        }

        // Use pokergamecore HandInfoFast to detect straight draws
        Hand community = toHand(communityCards);
        HandInfoFast hif = new HandInfoFast(); // pokergamecore.HandInfoFast

        // Need to call getScore to populate straight draw information
        // Use dummy hole cards
        Hand dummyHole = new Hand(2);
        dummyHole.addCard(new Card(CardSuit.CLUBS, Card.TWO));
        dummyHole.addCard(new Card(CardSuit.DIAMONDS, Card.THREE));

        hif.getScore(dummyHole, community);
        return hif.hasStraightDraw();
    }

    @Override
    public int getNumOpponentStraights(Card[] communityCards) {
        if (currentHand == null || communityCards == null || communityCards.length < 3) {
            return 0;
        }

        // Approximate: count ranks in 4 or 5 consecutive values on community board
        // to estimate straight possibilities (conservative heuristic)
        Hand community = toHand(communityCards);
        HandInfoFast hif = new HandInfoFast(); // pokergamecore.HandInfoFast
        Hand dummyHole = new Hand(2);
        dummyHole.addCard(new Card(CardSuit.CLUBS, Card.TWO));
        dummyHole.addCard(new Card(CardSuit.DIAMONDS, Card.THREE));
        hif.getScore(dummyHole, community);
        // If the board already supports a straight draw, assume ~1-2 opponents could
        // have it
        return hif.hasStraightDraw() ? Math.max(1, currentHand.getNumWithCards() / 3) : 0;
    }

    @Override
    public boolean isRebuyPeriodActive() {
        if (tournament == null || aiPlayer == null) {
            return false;
        }

        // Delegate to tournament context (matches V1Player line 190)
        return tournament.isRebuyPeriodActive(aiPlayer);
    }

    @Override
    public double calculateImprovementOdds(Card[] holeCards, Card[] communityCards) {
        if (holeCards == null || communityCards == null) {
            return 0.15; // Conservative fallback
        }

        // On river, no more cards to come (V1Algorithm will apply MIN_IMPROVE_ODDS
        // floor)
        int bettingRound = getBettingRound();
        if (bettingRound == 3) {
            return 0.0; // No improvement possible on river
        }

        // Use pokergamecore HandInfoFast for draw-based improvement odds estimate
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        HandInfoFast fast = new HandInfoFast(); // pokergamecore.HandInfoFast
        fast.getScore(hole, community);
        // Estimate improvement odds based on draw presence
        if (fast.hasFlushDraw()) {
            return 0.35; // ~9 outs flush draw
        } else if (fast.hasStraightDraw()) {
            int outs = fast.getStraightDrawOuts();
            return outs >= 8 ? 0.32 : 0.17; // open-ended vs gutshot
        }
        return 0.10; // conservative default (pick up a pair, etc.)
    }

    @Override
    public boolean isNutFlush(Card[] holeCards, Card[] communityCards, int majorSuit, int nCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        // Inline nut flush check: find highest missing suit card in hole cards
        return isNutFlush(hole, new HandSorted(community), majorSuit, nCards);
    }

    @Override
    public double calculateHandStrength(Card[] holeCards, Card[] communityCards, int numOpponents) {
        if (holeCards == null || communityCards == null) {
            return 0.0;
        }

        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        // Approximate hand strength using hand type score ranking
        HandInfoFast fast = new HandInfoFast(); // pokergamecore.HandInfoFast
        int score = fast.getScore(hole, community);
        int handType = HandInfoFast.getTypeFromScore(score);
        // Normalize hand type (1-10) to approximate hand strength (0.0-1.0)
        // Adjust for number of opponents: more opponents = lower effective strength
        double baseStrength = (handType - 1.0) / 9.0;
        double opponentFactor = Math.pow(baseStrength, Math.max(1, numOpponents));
        return opponentFactor;
    }

    @Override
    public int getLastActionInRound(GamePlayerInfo player, int bettingRound) {
        if (player == null) {
            return ACTION_NONE;
        }
        int[] actions = playerActionsPerRound.get(player.getID());
        if (actions != null && bettingRound >= 0 && bettingRound < actions.length) {
            return actions[bettingRound];
        }
        return ACTION_NONE;
    }

    @Override
    public int getOpponentRaiseFrequency(GamePlayerInfo opponent, int bettingRound) {
        if (opponent == null || opponentTracker == null) {
            return 50; // Neutral default
        }
        com.donohoedigital.games.poker.core.ai.V2OpponentModel model = opponentTracker.getModel(opponent.getID());
        if (bettingRound == 0) {
            // Pre-flop: use raise percentage
            float raisePercent = model.getHandsRaisedPreFlopPercent(0.5f);
            return Math.round(raisePercent * 100);
        } else {
            // Post-flop: use raise frequency for the round
            float raiseFreq = model.getRaisePostFlop(bettingRound, 0.5f);
            return Math.round(raiseFreq * 100);
        }
    }

    @Override
    public int getOpponentBetFrequency(GamePlayerInfo opponent, int bettingRound) {
        if (opponent == null || opponentTracker == null || bettingRound == 0) {
            return 50; // Neutral default (pre-flop doesn't have "bet", only raise)
        }
        com.donohoedigital.games.poker.core.ai.V2OpponentModel model = opponentTracker.getModel(opponent.getID());
        // Post-flop: use open (first to act and bet) frequency
        float openFreq = model.getOpenPostFlop(bettingRound, 0.5f);
        return Math.round(openFreq * 100);
    }

    // =========================================================================
    // Private helpers (inlined from deleted HandInfo static methods)
    // =========================================================================

    /**
     * Returns true if the hole cards participate in the best hand described by
     * {@code score}/{@code suit}. Inlined from HandInfo.isOurHandInvolved().
     */
    private static boolean isOurHandInvolved(Hand hole, int score, int suit, boolean bStrictTwoPair) {
        int[] cards = new int[5];
        HandInfoFast.getCards(score, cards);
        int nType = HandInfoFast.getTypeFromScore(score);

        switch (nType) {
            case HandScoreConstants.HIGH_CARD :
            case HandScoreConstants.PAIR :
            case HandScoreConstants.TRIPS :
            case HandScoreConstants.QUADS :
                return hole.isInHand(cards[0]);

            case HandScoreConstants.TWO_PAIR :
                if (bStrictTwoPair)
                    return hole.isInHand(cards[0]) && hole.isInHand(cards[1]);
                else
                    return hole.isInHand(cards[0]) || hole.isInHand(cards[1]);

            case HandScoreConstants.STRAIGHT :
                return hole.isInHand(cards[0]) || hole.isInHand(cards[0] - 1) || hole.isInHand(cards[0] - 2)
                        || hole.isInHand(cards[0] - 3) || hole.isInHand(cards[0] > 5 ? cards[0] - 4 : Card.ACE);

            case HandScoreConstants.FLUSH :
                return hole.isInHand(cards[0], suit) || hole.isInHand(cards[1], suit) || hole.isInHand(cards[2], suit)
                        || hole.isInHand(cards[3], suit) || hole.isInHand(cards[4], suit);

            case HandScoreConstants.FULL_HOUSE :
                return hole.isInHand(cards[0]) || hole.isInHand(cards[1]);

            case HandScoreConstants.STRAIGHT_FLUSH :
            case HandScoreConstants.ROYAL_FLUSH :
                return hole.isInHand(cards[0], suit) || hole.isInHand(cards[0] - 1, suit)
                        || hole.isInHand(cards[0] - 2, suit) || hole.isInHand(cards[0] - 3, suit)
                        || hole.isInHand(cards[0] > 5 ? cards[0] - 4 : Card.ACE, suit);

            default :
                return false;
        }
    }

    /**
     * Returns true if {@code hole} contains the nut flush (or within top
     * {@code nCards} flush hands) for {@code nSuit} given the community. Inlined
     * from HandInfo.isNutFlush().
     */
    private static boolean isNutFlush(Hand hole, HandSorted community, int nSuit, int nCards) {
        int nNeedCard = 10;
        for (int i = Card.ACE; i >= Card.JACK; i--) {
            if (!community.isInHand(i, nSuit)) {
                nNeedCard = i;
                break;
            }
        }
        while (nCards > 0) {
            if (!community.isInHand(nNeedCard, nSuit)) {
                if (hole.isInHand(nNeedCard, nSuit))
                    return true;
                nCards--;
            }
            nNeedCard--;
        }
        return false;
    }
}
