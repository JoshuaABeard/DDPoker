/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.HoldemHand;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.core.GamePlayerInfo;
import com.donohoedigital.games.poker.core.ai.AIConstants;
import com.donohoedigital.games.poker.core.ai.StrategyProvider;
import com.donohoedigital.games.poker.core.ai.V2AIContext;
import com.donohoedigital.games.poker.core.ai.V2OpponentModel;
import com.donohoedigital.games.poker.engine.Card;
import com.donohoedigital.games.poker.HandAction;
import com.donohoedigital.games.poker.engine.Hand;
import com.donohoedigital.games.poker.engine.HandInfoFaster;

import java.util.ArrayList;
import java.util.List;

/**
 * Desktop implementation of V2AIContext that wraps HoldemHand and PokerPlayer.
 * <p>
 * This adapter allows V2Algorithm to work with desktop poker objects by
 * translating between the pure game core interfaces and the desktop Swing-based
 * implementations.
 */
public class ClientV2AIContext implements V2AIContext {

    private final HoldemHand hand;
    private final StrategyProvider strategy;
    private final HandInfoFaster handEval = new HandInfoFaster();

    public ClientV2AIContext(HoldemHand hand, StrategyProvider strategy) {
        this.hand = hand;
        this.strategy = strategy;
    }

    // ========== AIContext Methods (inherited) ==========

    @Override
    public com.donohoedigital.games.poker.core.GameTable getTable() {
        // Not needed for V2Algorithm - return null
        return null;
    }

    @Override
    public com.donohoedigital.games.poker.core.GameHand getCurrentHand() {
        // Not needed for V2Algorithm - return null
        return null;
    }

    @Override
    public com.donohoedigital.games.poker.core.TournamentContext getTournament() {
        // Not needed for V2Algorithm - return null
        return null;
    }

    @Override
    public boolean isButton(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getSeat() == hand.getTable().getButton();
    }

    @Override
    public boolean isSmallBlind(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getSeat() == hand.getSmallBlindSeat();
    }

    @Override
    public boolean isBigBlind(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getSeat() == hand.getBigBlindSeat();
    }

    @Override
    public int getPosition(GamePlayerInfo player) {
        // Simplified - use starting position category
        return getStartingPositionCategory(player);
    }

    @Override
    public int getPotSize() {
        return hand.getTotalPotChipCount();
    }

    @Override
    public int getAmountToCall(GamePlayerInfo player) {
        return hand.getAmountToCall(player);
    }

    @Override
    public int getAmountBetThisRound(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.getBet(pp);
    }

    @Override
    public int getLastBetAmount() {
        return hand.getBet();
    }

    @Override
    public int getNumActivePlayers() {
        return hand.getNumWithCards();
    }

    @Override
    public int getNumPlayersYetToAct(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.getNumAfter(pp);
    }

    @Override
    public int getNumPlayersWhoActed(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.getNumBefore(pp);
    }

    @Override
    public boolean hasBeenBet() {
        return hand.getBet() > 0;
    }

    @Override
    public boolean hasBeenRaised() {
        return hand.getNumRaises() > 0;
    }

    @Override
    public GamePlayerInfo getLastBettor() {
        PokerPlayer pp = hand.getBettor();
        return pp != null ? new PokerPlayerAdapter(pp) : null;
    }

    @Override
    public GamePlayerInfo getLastRaiser() {
        PokerPlayer pp = hand.getRaiser();
        return pp != null ? new PokerPlayerAdapter(pp) : null;
    }

    @Override
    public int evaluateHandRank(Card[] holeCards, Card[] communityCards) {
        Hand pocket = cardsToHand(holeCards);
        Hand community = cardsToHand(communityCards);
        int score = handEval.getScore(pocket, community);
        return score / 1000000; // Simplified type extraction
    }

    @Override
    public long evaluateHandScore(Card[] holeCards, Card[] communityCards) {
        Hand pocket = cardsToHand(holeCards);
        Hand community = cardsToHand(communityCards);
        return handEval.getScore(pocket, community);
    }

    @Override
    public double calculateImprovementOdds(Card[] holeCards, Card[] communityCards) {
        // Simplified - actual implementation would use hand potential calculation
        return 0.5;
    }

    @Override
    public Card[] getBest5Cards(Card[] holeCards, Card[] communityCards) {
        // Simplified - complex HandInfo API
        return new Card[0];
    }

    @Override
    public int[] getBest5CardRanks(Card[] holeCards, Card[] communityCards) {
        // Simplified
        return new int[0];
    }

    @Override
    public boolean isHoleCardInvolved(Card[] holeCards, Card[] communityCards) {
        // Assume involved
        return true;
    }

    @Override
    public int getMajorSuit(Card[] holeCards, Card[] communityCards) {
        Hand pocket = cardsToHand(holeCards);
        Hand community = cardsToHand(communityCards);
        handEval.getScore(pocket, community); // Must call to calculate
        return handEval.getLastMajorSuit();
    }

    @Override
    public int getBettingRound() {
        return hand.getRound().toLegacy();
    }

    @Override
    public Card[] getHoleCards(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        Hand pocket = pp.getHand();
        if (pocket == null)
            return new Card[0];
        Card[] cards = new Card[pocket.size()];
        for (int i = 0; i < pocket.size(); i++) {
            cards[i] = pocket.getCard(i);
        }
        return cards;
    }

    @Override
    public Card[] getCommunityCards() {
        Hand community = hand.getCommunity();
        if (community == null)
            return new Card[0];
        Card[] cards = new Card[community.size()];
        for (int i = 0; i < community.size(); i++) {
            cards[i] = community.getCard(i);
        }
        return cards;
    }

    @Override
    public int getNumCallers() {
        return hand.getNumCallers();
    }

    @Override
    public boolean hasFlushDraw(Card[] communityCards) {
        // Simplified
        return false;
    }

    @Override
    public boolean hasStraightDraw(Card[] communityCards) {
        // Simplified
        return false;
    }

    @Override
    public int getNumOpponentStraights(Card[] communityCards) {
        // Simplified
        return 0;
    }

    @Override
    public boolean isRebuyPeriodActive() {
        // Rebuy period check requires player context - return false for now
        return false;
    }

    @Override
    public boolean isNutFlush(Card[] holeCards, Card[] communityCards, int majorSuit, int nCards) {
        // Simplified
        return false;
    }

    @Override
    public double calculateHandStrength(Card[] holeCards, Card[] communityCards, int numOpponents) {
        // Simplified - use score approximation
        long score = evaluateHandScore(holeCards, communityCards);
        int type = (int) (score / 1000000);
        return type >= 0 && type <= 10 ? type * 0.1 : 0.5;
    }

    @Override
    public int getLastActionInRound(GamePlayerInfo player, int bettingRound) {
        PokerPlayer pp = adaptPlayer(player);
        HandAction action = hand.getLastAction(pp, bettingRound);
        return action != null ? action.getAction() : ACTION_NONE;
    }

    @Override
    public int getOpponentRaiseFrequency(GamePlayerInfo opponent, int bettingRound) {
        // Simplified - would use profile data
        return 50;
    }

    @Override
    public int getOpponentBetFrequency(GamePlayerInfo opponent, int bettingRound) {
        // Simplified - would use profile data
        return 50;
    }

    // ========== V2AIContext Methods ==========

    @Override
    public StrategyProvider getStrategy() {
        return strategy;
    }

    @Override
    public float getHohM(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        // Formula from V2Player.getHohM()
        float m = (float) pp.getChipCountAtStart()
                / (float) (hand.getAnte() * hand.getNumPlayers() + hand.getSmallBlind() + hand.getBigBlind());
        // Adjust to get effective M
        return m * (2.0f / 3.0f + (float) (hand.getNumPlayers() - 1) / 27.0f);
    }

    @Override
    public float getHohQ(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        PokerGame game = hand.getTable().getGame();
        return (float) pp.getChipCountAtStart() / (float) game.getAverageStack();
    }

    @Override
    public int getHohZone(GamePlayerInfo player) {
        float m = getHohM(player);
        if (m <= 1.0f)
            return AIConstants.HOH_DEAD;
        if (m <= 5.0f)
            return AIConstants.HOH_RED;
        if (m <= 10.0f)
            return AIConstants.HOH_ORANGE;
        if (m <= 20.0f)
            return AIConstants.HOH_YELLOW;
        return AIConstants.HOH_GREEN;
    }

    @Override
    public float getTableAverageHohM() {
        float total = 0;
        int count = 0;
        for (int i = 0; i < hand.getNumPlayers(); i++) {
            PokerPlayer pp = hand.getPlayerAt(i);
            if (pp != null) {
                total += getHohM(new PokerPlayerAdapter(pp));
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    @Override
    public float getRemainingAverageHohM() {
        float total = 0;
        int count = 0;
        for (int i = 0; i < hand.getNumPlayers(); i++) {
            PokerPlayer pp = hand.getPlayerAt(i);
            if (pp != null && !pp.isFolded()) {
                total += getHohM(new PokerPlayerAdapter(pp));
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    @Override
    public V2OpponentModel getOpponentModel(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        OpponentModel model = pp.getOpponentModel();
        return new OpponentModelAdapter(model);
    }

    @Override
    public V2OpponentModel getSelfModel() {
        // Return stub model (self-statistics not available in desktop context)
        return new StubV2OpponentModel();
    }

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
        @Override
        public boolean isOverbetPotPostFlop() {
            return false;
        }
        @Override
        public void setOverbetPotPostFlop(boolean value) {
            // No-op in stub
        }
    }

    @Override
    public int getHandScore(Hand pocket, Hand community) {
        return handEval.getScore(pocket, community);
    }

    @Override
    public float getRawHandStrength(Hand pocket, Hand community) {
        // Simplified score-based approximation
        int score = handEval.getScore(pocket, community);
        int type = score / 1000000;
        return type >= 0 && type <= 10 ? type * 0.1f : 0.5f;
    }

    @Override
    public float getBiasedRawHandStrength(int seat, Hand community) {
        PokerPlayer pp = hand.getPlayerAt(seat);
        if (pp == null || pp.getHand() == null)
            return 0.5f;
        return getRawHandStrength(pp.getHand(), community);
    }

    @Override
    public float getBiasedEffectiveHandStrength(int seat, Hand community) {
        // Simplified - would use hand potential calculation
        return getBiasedRawHandStrength(seat, community);
    }

    @Override
    public float getApparentStrength(int seat, Hand community) {
        // Apparent strength based on betting patterns
        return getBiasedRawHandStrength(seat, community);
    }

    @Override
    public int getNutFlushCount(Hand pocket, Hand community) {
        // Simplified
        return 0;
    }

    @Override
    public int getNonNutFlushCount(Hand pocket, Hand community) {
        // Simplified
        return 0;
    }

    @Override
    public int getNutStraightCount(Hand pocket, Hand community) {
        // Simplified - actual implementation would check all straight possibilities
        return 0;
    }

    @Override
    public int getNonNutStraightCount(Hand pocket, Hand community) {
        // Simplified - actual implementation would check all straight possibilities
        return 0;
    }

    @Override
    public int getStartingPositionCategory(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getStartingPositionCategory();
    }

    @Override
    public int getPostFlopPositionCategory(GamePlayerInfo player) {
        // Simplified - same as starting position for now
        return getStartingPositionCategory(player);
    }

    @Override
    public int getStartingOrder(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        // Calculate starting order based on seat and button
        PokerTable table = hand.getTable();
        int buttonSeat = table.getButton();
        int seat = pp.getSeat();
        int numPlayers = hand.getNumPlayers();

        // Order is seats after button (button is last to act preflop)
        int order = (seat - buttonSeat - 1 + numPlayers) % numPlayers;
        return order;
    }

    @Override
    public boolean wasRaisedPreFlop() {
        return hand.wasRaisedPreFlop();
    }

    @Override
    public boolean wasFirstRaiserPreFlop(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.wasFirstRaiserPreFlop(pp);
    }

    @Override
    public boolean wasLastRaiserPreFlop(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.wasLastRaiserPreFlop(pp);
    }

    @Override
    public boolean wasOnlyRaiserPreFlop(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.wasOnlyRaiserPreFlop(pp);
    }

    @Override
    public GamePlayerInfo getFirstBettor(int round, boolean includeRaises) {
        PokerPlayer pp = hand.getFirstBettor(round, includeRaises);
        return pp != null ? new PokerPlayerAdapter(pp) : null;
    }

    @Override
    public int getFirstVoluntaryAction(GamePlayerInfo player, int round) {
        PokerPlayer pp = adaptPlayer(player);
        HandAction action = hand.getFirstVoluntaryAction(pp, round);
        return action != null ? action.getAction() : HandAction.ACTION_NONE;
    }

    @Override
    public boolean wasPotAction(int round) {
        return hand.isActionInRound(round);
    }

    @Override
    public int getPotStatus() {
        return hand.getPotStatus();
    }

    @Override
    public int getLastActionThisRound(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.getLastActionThisRound(pp);
    }

    @Override
    public int getSeat(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getSeat();
    }

    @Override
    public int getChipCountAtStart(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getChipCountAtStart();
    }

    @Override
    public int getHandsBeforeBigBlind(GamePlayerInfo player) {
        // Not directly available - return default
        return 5;
    }

    @Override
    public int getConsecutiveHandsUnpaid(GamePlayerInfo player) {
        // Not directly available - return default
        return 0;
    }

    @Override
    public int getMinRaise() {
        return hand.getMinRaise();
    }

    @Override
    public float getPotOdds(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.getPotOdds(pp);
    }

    @Override
    public boolean paidToPlay(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.paidToPlay(pp);
    }

    @Override
    public boolean couldLimp(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.couldLimp(pp);
    }

    @Override
    public boolean limped(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.limped(pp);
    }

    @Override
    public boolean isLimit() {
        return hand.isLimit();
    }

    @Override
    public int getBigBlind() {
        return hand.getBigBlind();
    }

    @Override
    public int getMinChip() {
        return hand.getMinChip();
    }

    @Override
    public int getCall(GamePlayerInfo player) {
        return hand.getCall(adaptPlayer(player));
    }

    @Override
    public int getTotalPotChipCount() {
        return hand.getTotalPotChipCount();
    }

    @Override
    public Hand getCommunity() {
        return hand.getCommunity();
    }

    @Override
    public Hand getPocketCards(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.getHand();
    }

    @Override
    public int getNumLimpers() {
        return hand.getNumLimpers();
    }

    @Override
    public boolean hasActedThisRound(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return hand.getLastActionThisRound(pp) != HandAction.ACTION_NONE;
    }

    @Override
    public GamePlayerInfo getLastBettor(int round, boolean includeRaises) {
        return getFirstBettor(round, includeRaises);
    }

    @Override
    public int getNumFoldsSinceLastBet() {
        // Not directly available - return default
        return 0;
    }

    @Override
    public boolean isBlind(GamePlayerInfo player) {
        PokerPlayer pp = adaptPlayer(player);
        return pp.isBlind();
    }

    @Override
    public int getNumPlayersWithCards() {
        return hand.getNumWithCards();
    }

    @Override
    public int getNumPlayersAtTable() {
        return hand.getNumPlayers();
    }

    @Override
    public GamePlayerInfo getPlayerAt(int index) {
        PokerPlayer pp = hand.getPlayerAt(index);
        return pp != null ? new PokerPlayerAdapter(pp) : null;
    }

    @Override
    public List<GamePlayerInfo> getPlayersLeft(GamePlayerInfo excludePlayer) {
        List<GamePlayerInfo> players = new ArrayList<>();
        PokerPlayer exclude = excludePlayer != null ? adaptPlayer(excludePlayer) : null;
        for (int i = 0; i < hand.getNumPlayers(); i++) {
            PokerPlayer pp = hand.getPlayerAt(i);
            if (pp != null && pp != exclude) {
                players.add(new PokerPlayerAdapter(pp));
            }
        }
        return players;
    }

    // ========== Helper Methods ==========

    /**
     * Adapt GamePlayerInfo to PokerPlayer.
     */
    private PokerPlayer adaptPlayer(GamePlayerInfo player) {
        if (player instanceof PokerPlayerAdapter) {
            return ((PokerPlayerAdapter) player).getPokerPlayer();
        }
        throw new IllegalArgumentException("Cannot adapt player: " + player);
    }

    /**
     * Convert Card array to Hand.
     */
    private Hand cardsToHand(Card[] cards) {
        Hand h = new Hand(cards.length);
        for (Card card : cards) {
            h.addCard(card);
        }
        return h;
    }

    /**
     * Adapter that wraps PokerPlayer to implement GamePlayerInfo.
     */
    private static class PokerPlayerAdapter implements GamePlayerInfo {
        private final PokerPlayer player;

        PokerPlayerAdapter(PokerPlayer player) {
            this.player = player;
        }

        PokerPlayer getPokerPlayer() {
            return player;
        }

        @Override
        public int getID() {
            return player.getID();
        }

        @Override
        public String getName() {
            return player.getName();
        }

        @Override
        public boolean isHuman() {
            return player.isHuman();
        }

        @Override
        public int getChipCount() {
            return player.getChipCount();
        }

        @Override
        public boolean isFolded() {
            return player.isFolded();
        }

        @Override
        public boolean isAllIn() {
            return player.isAllIn();
        }

        @Override
        public int getSeat() {
            return player.getSeat();
        }

        @Override
        public boolean isAskShowWinning() {
            // Not available on PokerPlayer - return default
            return false;
        }

        @Override
        public boolean isAskShowLosing() {
            // Not available on PokerPlayer - return default
            return false;
        }

        @Override
        public boolean isObserver() {
            return player.isObserver();
        }

        @Override
        public boolean isHumanControlled() {
            return player.isHumanControlled();
        }

        @Override
        public int getThinkBankMillis() {
            return player.getThinkBankMillis();
        }

        @Override
        public boolean isSittingOut() {
            return player.isSittingOut();
        }

        @Override
        public void setSittingOut(boolean sittingOut) {
            player.setSittingOut(sittingOut);
        }

        @Override
        public boolean isLocallyControlled() {
            return player.isLocallyControlled();
        }

        @Override
        public boolean isComputer() {
            return player.isComputer();
        }

        @Override
        public void setTimeoutMillis(int millis) {
            player.setTimeoutMillis(millis);
        }

        @Override
        public void setTimeoutMessageSecondsLeft(int seconds) {
            player.setTimeoutMessageSecondsLeft(seconds);
        }

        @Override
        public int getNumRebuys() {
            return player.getNumRebuys();
        }
    }

    /**
     * Adapter that wraps OpponentModel to implement V2OpponentModel.
     */
    private static class OpponentModelAdapter implements V2OpponentModel {
        private final OpponentModel model;

        OpponentModelAdapter(OpponentModel model) {
            this.model = model;
        }

        @Override
        public float getPreFlopTightness(int position, float defVal) {
            return model.getPreFlopTightness(position, defVal);
        }

        @Override
        public float getPreFlopAggression(int position, float defVal) {
            return model.getPreFlopAggression(position, defVal);
        }

        @Override
        public float getActPostFlop(int round, float defVal) {
            return model.actFlop.getWeightedPercentTrue(defVal);
        }

        @Override
        public float getCheckFoldPostFlop(int round, float defVal) {
            return model.checkFoldFlop.getWeightedPercentTrue(defVal);
        }

        @Override
        public float getOpenPostFlop(int round, float defVal) {
            return model.openFlop.getWeightedPercentTrue(defVal);
        }

        @Override
        public float getRaisePostFlop(int round, float defVal) {
            return model.raiseFlop.getWeightedPercentTrue(defVal);
        }

        @Override
        public int getHandsPlayed() {
            return model.getHandsPlayed();
        }

        @Override
        public float getHandsPaidPercent(float defVal) {
            return model.getHandsPaidPercent(defVal);
        }

        @Override
        public float getHandsLimpedPercent(float defVal) {
            return model.getHandsLimpedPercent(defVal);
        }

        @Override
        public float getHandsFoldedUnraisedPercent(float defVal) {
            return model.getHandsFoldedUnraisedPercent(defVal);
        }

        @Override
        public float getOverbetFrequency(float defVal) {
            return model.getOverbetFrequency(defVal);
        }

        @Override
        public float getBetFoldFrequency(float defVal) {
            return model.getBetFoldFrequency(defVal);
        }

        @Override
        public boolean isOverbetPotPostFlop() {
            return model.isOverbetPotPostFlop();
        }

        @Override
        public void setOverbetPotPostFlop(boolean value) {
            model.setOverbetPotPostFlop(value);
        }

        // NOTE: getConsecutiveHandsUnpaid not in V2OpponentModel interface
        // Only available from V2AIContext.getConsecutiveHandsUnpaid(GamePlayerInfo)
        // No implementation needed here

        @Override
        public float getHandsRaisedPreFlopPercent(float defVal) {
            return model.getHandsRaisedPreFlopPercent(defVal);
        }
    }
}
