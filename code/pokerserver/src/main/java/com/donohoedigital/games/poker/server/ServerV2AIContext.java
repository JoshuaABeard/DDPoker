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
import com.donohoedigital.games.poker.HandPotential;

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
     * @param opponentTracker
     *            Shared opponent tracker for behavioral statistics
     */
    public ServerV2AIContext(GameTable table, GameHand currentHand, TournamentContext tournament,
            GamePlayerInfo aiPlayer, StrategyProvider strategyProvider, ServerOpponentTracker opponentTracker) {
        super(table, currentHand, tournament, aiPlayer, opponentTracker);
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
        // In server context, we only have access to current table data.
        // Tournament-wide average would require querying tournament engine for all
        // tables.
        // Table average provides reasonable approximation for AI decision-making.
        return getTableAverageHohM();
    }

    // === Opponent Models ===

    @Override
    public V2OpponentModel getOpponentModel(GamePlayerInfo player) {
        if (player == null || opponentTracker == null) {
            return new StubV2OpponentModel();
        }
        return opponentTracker.getModel(player.getID());
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
        if (pocket == null || community == null || community.size() < 3) {
            return 0.0f;
        }
        // Use PocketRanks for accurate post-flop hand strength
        PocketRanks ranks = PocketRanks.getInstance(community);
        return ranks.getRawHandStrength(pocket);
    }

    @Override
    public float getBiasedRawHandStrength(int seat, Hand community) {
        if (community == null || community.size() < 3) {
            return 0.0f;
        }
        GamePlayerInfo player = getPlayerAt(seat);
        if (player == null) {
            return 0.0f;
        }
        Hand pocket = getPocketCards(player);
        if (pocket == null) {
            return 0.0f;
        }

        // Get raw hand strength
        float rawStrength = getRawHandStrength(pocket, community);

        // Apply SimpleBias for opponent range adjustment
        // Use pre-flop table index (0-10) based on opponent tightness
        // For now, use moderate table (index 5 = 50% range)
        int tableIndex = 5;
        float bias = SimpleBias.getBiasValue(tableIndex, pocket);

        // Multiply raw strength by bias factor
        return Math.min(1.0f, rawStrength * bias);
    }

    @Override
    public float getBiasedEffectiveHandStrength(int seat, Hand community) {
        if (community == null || community.size() < 3 || community.size() >= 5) {
            return 0.0f;
        }
        GamePlayerInfo player = getPlayerAt(seat);
        if (player == null) {
            return 0.0f;
        }
        Hand pocket = getPocketCards(player);
        if (pocket == null) {
            return 0.0f;
        }

        // Use PocketOdds for effective hand strength with one-card lookahead
        PocketOdds odds = PocketOdds.getInstance(community, pocket);
        float ehs = odds.getEffectiveHandStrength();

        // Apply SimpleBias for opponent range adjustment
        int tableIndex = 5; // Moderate opponent range
        float bias = SimpleBias.getBiasValue(tableIndex, pocket);

        return Math.min(1.0f, ehs * bias);
    }

    @Override
    public float getApparentStrength(int seat, Hand community) {
        // Apparent strength is the strength perceived by opponents based on betting
        // For now, use biased effective hand strength as approximation
        // Future: Incorporate betting patterns and opponent modeling
        return getBiasedEffectiveHandStrength(seat, community);
    }

    // === Draw Detection ===

    @Override
    public int getNutFlushCount(Hand pocket, Hand community) {
        if (pocket == null || community == null || community.size() < 3) {
            return 0;
        }
        HandPotential hp = new HandPotential(pocket, community);
        // Nut flush draw with two cards = turn + river combined counts
        int turnCount = hp.getHandCount(HandPotential.NUT_FLUSH_DRAW_WITH_TWO_CARDS, 0);
        int riverCount = (community.size() == 3) ? hp.getHandCount(HandPotential.NUT_FLUSH_DRAW_WITH_TWO_CARDS, 1) : 0;
        return turnCount + riverCount;
    }

    @Override
    public int getNonNutFlushCount(Hand pocket, Hand community) {
        if (pocket == null || community == null || community.size() < 3) {
            return 0;
        }
        HandPotential hp = new HandPotential(pocket, community);
        // Second nut + weak flush draws
        int secondNutTurn = hp.getHandCount(HandPotential.SECOND_NUT_FLUSH_DRAW_WITH_TWO_CARDS, 0);
        int weakTurn = hp.getHandCount(HandPotential.WEAK_FLUSH_DRAW_WITH_TWO_CARDS, 0);
        int secondNutRiver = (community.size() == 3)
                ? hp.getHandCount(HandPotential.SECOND_NUT_FLUSH_DRAW_WITH_TWO_CARDS, 1)
                : 0;
        int weakRiver = (community.size() == 3) ? hp.getHandCount(HandPotential.WEAK_FLUSH_DRAW_WITH_TWO_CARDS, 1) : 0;
        return secondNutTurn + weakTurn + secondNutRiver + weakRiver;
    }

    @Override
    public int getNutStraightCount(Hand pocket, Hand community) {
        if (pocket == null || community == null || community.size() < 3) {
            return 0;
        }
        HandPotential hp = new HandPotential(pocket, community);
        // Approximate: use 8-out straight draws as nut straight draws
        int turnCount = hp.getHandCount(HandPotential.STRAIGHT_DRAW_8_OUTS, 0);
        int riverCount = (community.size() == 3) ? hp.getHandCount(HandPotential.STRAIGHT_DRAW_8_OUTS, 1) : 0;
        return turnCount + riverCount;
    }

    @Override
    public int getNonNutStraightCount(Hand pocket, Hand community) {
        if (pocket == null || community == null || community.size() < 3) {
            return 0;
        }
        HandPotential hp = new HandPotential(pocket, community);
        // 6-out, 4-out, and 3-out straight draws
        int turn6 = hp.getHandCount(HandPotential.STRAIGHT_DRAW_6_OUTS, 0);
        int turn4 = hp.getHandCount(HandPotential.STRAIGHT_DRAW_4_OUTS, 0);
        int turn3 = hp.getHandCount(HandPotential.STRAIGHT_DRAW_3_OUTS, 0);
        int river6 = (community.size() == 3) ? hp.getHandCount(HandPotential.STRAIGHT_DRAW_6_OUTS, 1) : 0;
        int river4 = (community.size() == 3) ? hp.getHandCount(HandPotential.STRAIGHT_DRAW_4_OUTS, 1) : 0;
        int river3 = (community.size() == 3) ? hp.getHandCount(HandPotential.STRAIGHT_DRAW_3_OUTS, 1) : 0;
        return turn6 + turn4 + turn3 + river6 + river4 + river3;
    }

    // === Table State (V2-specific) ===

    @Override
    public int getStartingPositionCategory(GamePlayerInfo player) {
        // Position categories based on distance from button (early/middle/late/blind)
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
        if (player == null || getTable() == null) {
            return 2; // Middle position default
        }
        int playerSeat = getSeat(player);
        int buttonSeat = getTable().getButton();
        int numSeats = getTable().getSeats();

        // Calculate post-flop position (distance from button)
        int positionsFromButton = (playerSeat - buttonSeat + numSeats) % numSeats;

        // Post-flop categories (similar to pre-flop but accounting for action order):
        // 0: Blinds (SB=1, BB=2 from button)
        // 1: Early (next 2-3 positions)
        // 2: Middle
        // 3: Late (button and cutoff)
        if (positionsFromButton <= 2) {
            return 0; // Blinds
        } else if (positionsFromButton >= numSeats - 2) {
            return 3; // Late (button and cutoff)
        } else if (positionsFromButton <= numSeats / 2) {
            return 1; // Early
        } else {
            return 2; // Middle
        }
    }

    @Override
    public int getStartingOrder(GamePlayerInfo player) {
        if (player == null || getTable() == null) {
            return 0;
        }
        int playerSeat = getSeat(player);
        int buttonSeat = getTable().getButton();
        int numSeats = getTable().getSeats();

        // Calculate pre-flop betting order (positions from button, clockwise)
        // Button acts last (highest order), small blind acts first (lowest order)
        int positionsFromButton = (playerSeat - buttonSeat + numSeats) % numSeats;
        return positionsFromButton;
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
        if (player == null || opponentTracker == null) {
            return player != null ? player.getChipCount() : 0;
        }
        return opponentTracker.getChipCountAtStart(player.getID());
    }

    @Override
    public int getHandsBeforeBigBlind(GamePlayerInfo player) {
        if (player == null || getTable() == null) {
            return 0;
        }
        int playerSeat = getSeat(player);
        int buttonSeat = getTable().getButton();
        int numSeats = getTable().getSeats();
        if (playerSeat < 0 || buttonSeat < 0 || opponentTracker == null) {
            return 0;
        }
        return opponentTracker.getHandsBeforeBigBlind(playerSeat, buttonSeat, numSeats);
    }

    @Override
    public int getConsecutiveHandsUnpaid(GamePlayerInfo player) {
        // Intentional simplification: Returns 0 (no history tracking).
        // Full implementation would require extending ServerOpponentTracker to track
        // fold/pay status across hands. This has minimal impact on V2 AI quality
        // as most decisions are based on current hand state and position.
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
        // Game type not available in current context
        // (GameTable/GameHand/TournamentContext).
        // DD Poker primarily uses no-limit format, so this is a reasonable default.
        // If limit games are added, this would need tournament profile access.
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

    // Note: isButton(), isSmallBlind(), isBigBlind() inherited from ServerAIContext

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
