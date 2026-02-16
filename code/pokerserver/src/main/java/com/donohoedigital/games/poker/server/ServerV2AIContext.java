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
package com.donohoedigital.games.poker.server;

import com.donohoedigital.games.poker.core.*;
import com.donohoedigital.games.poker.core.ai.*;
import com.donohoedigital.games.poker.engine.Hand;

import java.util.List;

/**
 * Server-side implementation of V2AIContext for V2 AI algorithm. Extends
 * ServerAIContext with V2-specific methods for strategy, opponent modeling,
 * hand evaluation, and detailed table state queries.
 */
public class ServerV2AIContext extends ServerAIContext implements V2AIContext {

    private final StrategyProvider strategyProvider;
    private final GamePlayerInfo aiPlayer;

    /**
     * Create V2 AI context for server game.
     *
     * @param table
     *            Current table state
     * @param currentHand
     *            Current hand being played (or null between hands)
     * @param tournament
     *            Tournament context for blind structure
     * @param aiPlayer
     *            The AI player this context is for
     * @param strategyProvider
     *            Strategy factor provider
     */
    public ServerV2AIContext(GameTable table, GameHand currentHand, TournamentContext tournament,
            GamePlayerInfo aiPlayer, StrategyProvider strategyProvider) {
        super(table, currentHand, tournament, aiPlayer);
        this.aiPlayer = aiPlayer;
        this.strategyProvider = strategyProvider;
    }

    // === Strategy ===

    @Override
    public StrategyProvider getStrategy() {
        return strategyProvider;
    }

    // === Tournament Metrics ===

    @Override
    public float getHohM(GamePlayerInfo player) {
        if (player == null || getTournament() == null || getTable() == null) {
            return 0.0f;
        }
        // Calculate Harrington M-ratio: chipstack / cost per round
        // Cost per round = small blind + big blind + (ante * players at table)
        int level = getTable().getLevel();
        int smallBlind = getTournament().getSmallBlind(level);
        int bigBlind = getTournament().getBigBlind(level);
        int ante = getTournament().getAnte(level);
        int playersAtTable = getTable().getNumOccupiedSeats();

        int costPerRound = smallBlind + bigBlind + (ante * playersAtTable);
        if (costPerRound == 0) {
            return 0.0f;
        }

        return (float) player.getChipCount() / costPerRound;
    }

    @Override
    public float getHohQ(GamePlayerInfo player) {
        // Q = M / average_M_at_table
        // For now, return simplified value
        float m = getHohM(player);
        float avgM = getTableAverageHohM();
        if (avgM == 0.0f) {
            return 1.0f;
        }
        return m / avgM;
    }

    @Override
    public int getHohZone(GamePlayerInfo player) {
        // Harrington zones based on M-ratio:
        // 0 = Dead zone (M <= 1)
        // 1 = Red zone (1 < M <= 5)
        // 2 = Orange zone (5 < M <= 10)
        // 3 = Yellow zone (10 < M <= 20)
        // 4 = Green zone (M > 20)
        float m = getHohM(player);
        if (m <= 1.0f) {
            return 0;
        } else if (m <= 5.0f) {
            return 1;
        } else if (m <= 10.0f) {
            return 2;
        } else if (m <= 20.0f) {
            return 3;
        } else {
            return 4;
        }
    }

    @Override
    public float getTableAverageHohM() {
        if (getTable() == null) {
            return 0.0f;
        }
        float totalM = 0.0f;
        int count = 0;
        for (int i = 0; i < getTable().getSeats(); i++) {
            GamePlayerInfo p = getTable().getPlayer(i);
            if (p != null) {
                totalM += getHohM(p);
                count++;
            }
        }
        return count > 0 ? totalM / count : 0.0f;
    }

    @Override
    public float getRemainingAverageHohM() {
        // TODO: Calculate average M across all remaining players in tournament
        // For now, return table average as approximation
        return getTableAverageHohM();
    }

    // === Opponent Models ===

    @Override
    public V2OpponentModel getOpponentModel(GamePlayerInfo player) {
        // TODO: Implement opponent modeling with persistence
        // For now, return stub with neutral assumptions
        return new StubV2OpponentModel();
    }

    @Override
    public V2OpponentModel getSelfModel() {
        return getOpponentModel(aiPlayer);
    }

    // === Hand Evaluation (V2-specific) ===

    @Override
    public int getHandScore(Hand pocket, Hand community) {
        if (pocket == null) {
            return 0;
        }
        // Use parent's hand evaluator
        return (int) evaluateHandScore(handToCards(pocket), handToCards(community));
    }

    @Override
    public float getRawHandStrength(Hand pocket, Hand community) {
        // TODO: Implement PocketRanks-based strength calculation
        // For now, use fallback to basic hand strength
        if (pocket == null || community == null) {
            return 0.0f;
        }
        int numOpponents = getNumPlayersWithCards() - 1;
        return (float) calculateHandStrength(handToCards(pocket), handToCards(community), Math.max(1, numOpponents));
    }

    @Override
    public float getBiasedRawHandStrength(int seat, Hand community) {
        // TODO: Implement opponent-biased strength using SimpleBias
        // For now, delegate to basic strength
        GamePlayerInfo player = getPlayerAt(seat);
        if (player == null) {
            return 0.0f;
        }
        Hand pocket = getPocketCards(player);
        return getRawHandStrength(pocket, community);
    }

    @Override
    public float getBiasedEffectiveHandStrength(int seat, Hand community) {
        // TODO: Implement biased EHS using PocketOdds
        // For now, delegate to raw strength
        return getBiasedRawHandStrength(seat, community);
    }

    @Override
    public float getApparentStrength(int seat, Hand community) {
        // TODO: Implement apparent strength calculation
        // For now, use biased strength
        return getBiasedRawHandStrength(seat, community);
    }

    // === Draw Detection ===

    @Override
    public int getNutFlushCount(Hand pocket, Hand community) {
        // TODO: Implement via HandPotential
        return 0;
    }

    @Override
    public int getNonNutFlushCount(Hand pocket, Hand community) {
        // TODO: Implement via HandPotential
        return 0;
    }

    @Override
    public int getNutStraightCount(Hand pocket, Hand community) {
        // TODO: Implement via HandPotential
        return 0;
    }

    @Override
    public int getNonNutStraightCount(Hand pocket, Hand community) {
        // TODO: Implement via HandPotential
        return 0;
    }

    // === Table State (V2-specific) ===

    @Override
    public int getStartingPositionCategory(GamePlayerInfo player) {
        // TODO: Implement position categories (early/middle/late/blind)
        // Based on PokerPlayer.getStartingPositionCategory()
        if (player == null || getTable() == null) {
            return 0;
        }
        int seat = getSeat(player);
        int button = getTable().getButton();
        int numSeats = getTable().getSeats();

        // Calculate position relative to button
        int distanceFromButton = (seat - button + numSeats) % numSeats;

        // Map to position category (0=blind, 1=early, 2=middle, 3=late)
        if (distanceFromButton == 1 || distanceFromButton == 2) {
            return 0; // Blinds
        } else if (distanceFromButton <= 4) {
            return 1; // Early
        } else if (distanceFromButton <= 7) {
            return 2; // Middle
        } else {
            return 3; // Late
        }
    }

    @Override
    public int getPostFlopPositionCategory(GamePlayerInfo player) {
        // TODO: Implement post-flop position categories
        // Similar to starting position but based on post-flop order
        return getStartingPositionCategory(player);
    }

    @Override
    public int getStartingOrder(GamePlayerInfo player) {
        if (player == null || getCurrentHand() == null) {
            return 0;
        }
        // TODO: Get position in pre-flop betting order
        // For now, return seat number as approximation
        return getSeat(player);
    }

    @Override
    public boolean wasRaisedPreFlop() {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().wasRaisedPreFlop();
    }

    @Override
    public boolean wasFirstRaiserPreFlop(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().wasFirstRaiserPreFlop(player);
    }

    @Override
    public boolean wasLastRaiserPreFlop(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().wasLastRaiserPreFlop(player);
    }

    @Override
    public boolean wasOnlyRaiserPreFlop(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().wasOnlyRaiserPreFlop(player);
    }

    @Override
    public GamePlayerInfo getFirstBettor(int round, boolean includeRaises) {
        if (getCurrentHand() == null) {
            return null;
        }
        return getCurrentHand().getFirstBettor(round, includeRaises);
    }

    @Override
    public int getFirstVoluntaryAction(GamePlayerInfo player, int round) {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getFirstVoluntaryAction(player, round);
    }

    @Override
    public boolean wasPotAction(int round) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().wasPotAction(round);
    }

    @Override
    public int getPotStatus() {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getPotStatus();
    }

    @Override
    public int getLastActionThisRound(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getLastActionThisRound(player);
    }

    @Override
    public int getSeat(GamePlayerInfo player) {
        if (player == null || getTable() == null) {
            return -1;
        }
        return getTable().getSeat(player);
    }

    @Override
    public int getChipCountAtStart(GamePlayerInfo player) {
        // TODO: Track chip counts at start of hand
        // For now, return current chip count
        if (player == null) {
            return 0;
        }
        return player.getChipCount();
    }

    @Override
    public int getHandsBeforeBigBlind(GamePlayerInfo player) {
        // TODO: Calculate hands until player posts big blind
        // Requires tracking button position and player seat
        return 0;
    }

    @Override
    public int getConsecutiveHandsUnpaid(GamePlayerInfo player) {
        // TODO: Track consecutive hands where player folded pre-flop without paying
        // Requires hand history tracking
        return 0;
    }

    @Override
    public int getMinRaise() {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getMinRaise();
    }

    @Override
    public float getPotOdds(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return 0.0f;
        }
        return getCurrentHand().getPotOdds(player);
    }

    @Override
    public boolean paidToPlay(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().paidToPlay(player);
    }

    @Override
    public boolean couldLimp(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().couldLimp(player);
    }

    @Override
    public boolean limped(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().limped(player);
    }

    @Override
    public boolean isLimit() {
        // TODO: Get game type from hand or table
        // For now, assume no-limit
        return false;
    }

    @Override
    public int getBigBlind() {
        if (getTournament() == null || getTable() == null) {
            return 0;
        }
        int level = getTable().getLevel();
        return getTournament().getBigBlind(level);
    }

    @Override
    public int getMinChip() {
        if (getTable() == null) {
            return 1;
        }
        return getTable().getMinChip();
    }

    @Override
    public int getCall(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getAmountToCall(player);
    }

    @Override
    public int getTotalPotChipCount() {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getPotSize();
    }

    // === Cards ===

    @Override
    public Hand getCommunity() {
        if (getCurrentHand() == null) {
            return null;
        }
        return cardsToHand(getCurrentHand().getCommunityCards());
    }

    @Override
    public Hand getPocketCards(GamePlayerInfo player) {
        if (player == null || getCurrentHand() == null) {
            return null;
        }
        // Security: Only allow AI to see its own cards
        if (player != aiPlayer) {
            return null;
        }
        return cardsToHand(getCurrentHand().getPlayerCards(player));
    }

    // === Player State ===

    @Override
    public int getNumLimpers() {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getNumLimpers();
    }

    @Override
    public boolean hasActedThisRound(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().hasActedThisRound(player);
    }

    @Override
    public GamePlayerInfo getLastBettor(int round, boolean includeRaises) {
        if (getCurrentHand() == null) {
            return null;
        }
        return getCurrentHand().getLastBettor(round, includeRaises);
    }

    @Override
    public int getNumFoldsSinceLastBet() {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getNumFoldsSinceLastBet();
    }

    @Override
    public boolean isBlind(GamePlayerInfo player) {
        if (getCurrentHand() == null) {
            return false;
        }
        return getCurrentHand().isBlind(player);
    }

    @Override
    public boolean isButton(GamePlayerInfo player) {
        if (player == null || getTable() == null) {
            return false;
        }
        return getSeat(player) == getTable().getButton();
    }

    @Override
    public boolean isSmallBlind(GamePlayerInfo player) {
        if (player == null || getTable() == null) {
            return false;
        }
        int seat = getSeat(player);
        int smallBlindSeat = calculateSmallBlindSeat();
        return seat == smallBlindSeat;
    }

    @Override
    public boolean isBigBlind(GamePlayerInfo player) {
        if (player == null || getTable() == null) {
            return false;
        }
        int seat = getSeat(player);
        int bigBlindSeat = calculateBigBlindSeat();
        return seat == bigBlindSeat;
    }

    /**
     * Calculate small blind seat based on button position and number of players. In
     * heads-up, button is small blind. Otherwise, small blind is 1 seat after
     * button.
     */
    private int calculateSmallBlindSeat() {
        int button = getTable().getButton();
        int numSeats = getTable().getSeats();
        int numOccupied = getTable().getNumOccupiedSeats();

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
        int button = getTable().getButton();
        int numSeats = getTable().getSeats();
        int numOccupied = getTable().getNumOccupiedSeats();

        // In heads-up, big blind is opposite of button
        // Since button is small blind in heads-up, big blind is the other player
        if (numOccupied == 2) {
            // Find the first occupied seat that isn't the button
            for (int i = 0; i < numSeats; i++) {
                if (i != button && getTable().getPlayer(i) != null) {
                    return i;
                }
            }
        }

        // Otherwise, big blind is 2 seats after button
        return (button + 2) % numSeats;
    }

    @Override
    public int getNumPlayersWithCards() {
        if (getCurrentHand() == null) {
            return 0;
        }
        return getCurrentHand().getNumWithCards();
    }

    // === Player Iteration ===

    @Override
    public int getNumPlayersAtTable() {
        if (getTable() == null) {
            return 0;
        }
        return getTable().getSeats();
    }

    @Override
    public GamePlayerInfo getPlayerAt(int index) {
        if (getTable() == null) {
            return null;
        }
        return getTable().getPlayer(index);
    }

    @Override
    public List<GamePlayerInfo> getPlayersLeft(GamePlayerInfo excludePlayer) {
        if (getTable() == null) {
            return List.of();
        }
        return getTable().getPlayersLeft(excludePlayer);
    }

    // === Helper Methods ===

    private com.donohoedigital.games.poker.engine.Card[] handToCards(Hand hand) {
        if (hand == null || hand.size() == 0) {
            return new com.donohoedigital.games.poker.engine.Card[0];
        }
        com.donohoedigital.games.poker.engine.Card[] cards = new com.donohoedigital.games.poker.engine.Card[hand
                .size()];
        for (int i = 0; i < hand.size(); i++) {
            cards[i] = hand.getCard(i);
        }
        return cards;
    }

    private Hand cardsToHand(com.donohoedigital.games.poker.engine.Card[] cards) {
        if (cards == null || cards.length == 0) {
            return null;
        }
        Hand hand = new Hand(cards.length);
        for (com.donohoedigital.games.poker.engine.Card card : cards) {
            if (card != null) {
                hand.addCard(card);
            }
        }
        return hand;
    }

    /**
     * Stub opponent model for when no real model is available.
     */
    private static class StubV2OpponentModel implements V2OpponentModel {
        @Override
        public float getPreFlopTightness(int position, float defVal) {
            return defVal;
        }

        @Override
        public float getPreFlopAggression(int position, float defVal) {
            return defVal;
        }

        @Override
        public float getActPostFlop(int round, float defVal) {
            return defVal;
        }

        @Override
        public float getCheckFoldPostFlop(int round, float defVal) {
            return defVal;
        }

        @Override
        public float getOpenPostFlop(int round, float defVal) {
            return defVal;
        }

        @Override
        public float getRaisePostFlop(int round, float defVal) {
            return defVal;
        }

        @Override
        public int getHandsPlayed() {
            return 0;
        }

        @Override
        public float getHandsPaidPercent(float defVal) {
            return defVal;
        }

        @Override
        public float getHandsLimpedPercent(float defVal) {
            return defVal;
        }

        @Override
        public float getHandsFoldedUnraisedPercent(float defVal) {
            return defVal;
        }

        @Override
        public float getOverbetFrequency(float defVal) {
            return defVal;
        }

        @Override
        public float getBetFoldFrequency(float defVal) {
            return defVal;
        }

        @Override
        public float getHandsRaisedPreFlopPercent(float defVal) {
            return defVal;
        }

        private boolean overbetPotPostFlop = false;

        @Override
        public boolean isOverbetPotPostFlop() {
            return overbetPotPostFlop;
        }

        @Override
        public void setOverbetPotPostFlop(boolean value) {
            this.overbetPotPostFlop = value;
        }
    }
}
