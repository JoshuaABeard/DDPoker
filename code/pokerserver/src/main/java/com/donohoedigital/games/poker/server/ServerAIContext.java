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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.AIContext;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.engine.CardSuit;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFaster;

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
    private final GameHand currentHand;
    private final TournamentContext tournament;
    private final GamePlayerInfo aiPlayer;
    private final HandInfoFaster handEvaluator;

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
     */
    public ServerAIContext(GameTable table, GameHand currentHand, TournamentContext tournament,
            GamePlayerInfo aiPlayer) {
        this.table = table;
        this.currentHand = currentHand;
        this.tournament = tournament;
        this.aiPlayer = aiPlayer;
        this.handEvaluator = new HandInfoFaster();
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

    // ========== Stub Methods (not yet needed by TournamentAI) ==========
    // TODO: Implement these when V1/V2 algorithms are extracted

    @Override
    public boolean isButton(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public boolean isSmallBlind(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public boolean isBigBlind(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public int getPosition(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getPotSize() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getAmountToCall(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getAmountBetThisRound(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getLastBetAmount() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getNumActivePlayers() {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getNumPlayersYetToAct(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public int getNumPlayersWhoActed(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public boolean hasBeenBet() {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public boolean hasBeenRaised() {
        // TODO: Implement for V1/V2 AI
        return false;
    }

    @Override
    public GamePlayerInfo getLastBettor() {
        // TODO: Implement for V1/V2 AI
        return null;
    }

    @Override
    public GamePlayerInfo getLastRaiser() {
        // TODO: Implement for V1/V2 AI
        return null;
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
        // TODO: Implement for V1/V2 AI
        return 0;
    }

    @Override
    public Card[] getHoleCards(GamePlayerInfo player) {
        // TODO: Implement for V1/V2 AI
        // SECURITY: Must enforce that player == this AI's player (no cheating by seeing
        // opponents' cards)
        // Example: if (player != this.aiPlayer) return null;
        return null;
    }

    @Override
    public Card[] getCommunityCards() {
        // TODO: Implement for V1/V2 AI
        return null;
    }

    @Override
    public int getNumCallers() {
        // TODO: Implement for V1/V2 AI
        return 0;
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

        // Use HandInfo.isOurHandInvolved to check if hole cards are part of best hand
        // strictTwoPair = false for standard checking
        return HandInfo.isOurHandInvolved(hole, score, suit, false);
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
        if (handRank < HandInfo.FLUSH) {
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

        // Use HandInfoFast to detect straight draws
        Hand community = toHand(communityCards);
        HandInfoFast hif = new HandInfoFast();

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

        // Use HandStrength to calculate opponent straight possibilities
        // This is expensive (Monte Carlo) but matches original V1Player logic
        Hand community = toHand(communityCards);

        // Create dummy hole cards for calculation (we're just analyzing board)
        Hand dummyHole = new Hand(2);
        dummyHole.addCard(new Card(CardSuit.CLUBS, Card.TWO));
        dummyHole.addCard(new Card(CardSuit.DIAMONDS, Card.THREE));

        HandStrength hs = new HandStrength();
        int numActivePlayers = currentHand.getNumWithCards() - 1; // Exclude ourselves
        hs.getStrength(dummyHole, community, Math.max(1, numActivePlayers));

        return hs.getNumStraights();
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

        // Use HandFutures for Monte Carlo simulation (matches original V1Player)
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        try {
            HandFutures fut = new HandFutures(handEvaluator, hole, community);
            double improveOdds = fut.getOddsImprove() / 100.0; // Convert from percentage
            return improveOdds;
        } catch (Exception e) {
            // Fallback to conservative estimate on error
            return 0.15;
        }
    }

    @Override
    public boolean isNutFlush(Card[] holeCards, Card[] communityCards, int majorSuit, int nCards) {
        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        // Use HandInfo.isNutFlush() exactly as original V1Player (lines 803, 807)
        // Note: community must be HandSorted for isNutFlush()
        return HandInfo.isNutFlush(hole, new com.donohoedigital.games.poker.engine.HandSorted(community), majorSuit,
                nCards);
    }

    @Override
    public double calculateHandStrength(Card[] holeCards, Card[] communityCards, int numOpponents) {
        if (holeCards == null || communityCards == null) {
            return 0.0;
        }

        Hand hole = toHand(holeCards);
        Hand community = toHand(communityCards);

        // Use HandStrength for Monte Carlo simulation (matches V1Player line 712)
        HandStrength hs = new HandStrength();
        return hs.getStrength(hole, community, Math.max(1, numOpponents));
    }

    @Override
    public int getLastActionInRound(GamePlayerInfo player, int bettingRound) {
        if (player == null || currentHand == null) {
            return ACTION_NONE;
        }

        // TODO: Implement when server tracks action history per round
        // For now, return ACTION_NONE (no limper detection on server)
        // Desktop client can implement this using HoldemHand.getLastAction()
        return ACTION_NONE;
    }

    @Override
    public int getOpponentRaiseFrequency(GamePlayerInfo opponent, int bettingRound) {
        // TODO: Implement when server has opponent modeling/profile tracking
        // For now, return neutral assumption (50% = moderate aggression)
        // Desktop client can implement this using TournamentProfile.getFrequency()
        return 50;
    }

    @Override
    public int getOpponentBetFrequency(GamePlayerInfo opponent, int bettingRound) {
        // TODO: Implement when server has opponent modeling/profile tracking
        // For now, return neutral assumption (50% = moderate aggression)
        // Desktop client can implement this using TournamentProfile.getFrequency()
        return 50;
    }
}
