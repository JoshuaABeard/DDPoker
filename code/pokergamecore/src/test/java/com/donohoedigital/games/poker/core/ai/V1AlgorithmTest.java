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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for V1Algorithm poker AI.
 */
class V1AlgorithmTest {

    private V1Algorithm algorithm;
    private static final long TEST_SEED = 12345L;

    @BeforeEach
    void setUp() {
        algorithm = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM);
    }

    @Test
    void testConstructor_withRandomTraits() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_HARD);
        assertNotNull(ai);
    }

    @Test
    void testConstructor_withExplicitTraits() {
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_EASY, 50, 30, 25, 75);
        assertNotNull(ai);
    }

    @Test
    void testWantsRebuy_alwaysRebuy() {
        // Rebuy propensity 0-25: always rebuy (up to 5 times)
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 10, 50);

        StubGamePlayerInfo player = new StubGamePlayerInfo();
        player.setNumRebuys(0);

        StubAIContext context = new StubAIContext();

        assertTrue(ai.wantsRebuy(player, context), "Should rebuy with low propensity");
    }

    @Test
    void testWantsRebuy_neverRebuy() {
        // Rebuy propensity 91-100: never rebuy
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 95, 50);

        StubGamePlayerInfo player = new StubGamePlayerInfo();
        player.setNumRebuys(0);

        StubAIContext context = new StubAIContext();

        assertFalse(ai.wantsRebuy(player, context), "Should not rebuy with high propensity");
    }

    @Test
    void testWantsRebuy_maxRebuysReached() {
        // Even with low propensity, stop at 5 rebuys (BUG 395 protection)
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 10, 50);

        StubGamePlayerInfo player = new StubGamePlayerInfo();
        player.setNumRebuys(5);

        StubAIContext context = new StubAIContext();

        assertFalse(ai.wantsRebuy(player, context), "Should not rebuy after 5 rebuys");
    }

    @Test
    void testWantsAddon_alwaysAddon() {
        // Addon propensity 0-24: always addon
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 50, 10);

        StubGamePlayerInfo player = new StubGamePlayerInfo();
        player.setChipCount(1000);

        StubAIContext context = new StubAIContext();
        StubTournamentContext tournament = new StubTournamentContext();
        tournament.setStartingChips(1000);
        context.setTournament(tournament);

        assertTrue(ai.wantsAddon(player, context), "Should addon with low propensity");
    }

    @Test
    void testWantsAddon_neverAddon() {
        // Addon propensity 75-100: never addon
        V1Algorithm ai = new V1Algorithm(TEST_SEED, V1Algorithm.AI_MEDIUM, 50, 50, 50, 85);

        StubGamePlayerInfo player = new StubGamePlayerInfo();
        player.setChipCount(5000);

        StubAIContext context = new StubAIContext();
        StubTournamentContext tournament = new StubTournamentContext();
        tournament.setStartingChips(1000);
        context.setTournament(tournament);

        assertFalse(ai.wantsAddon(player, context), "Should not addon with high propensity");
    }

    @Test
    void testGetAction_preFlopWithNoCards() {
        StubGamePlayerInfo player = new StubGamePlayerInfo();

        ActionOptions options = new ActionOptions(true, true, true, true, true, 0, 100, 10000, 200, 10000, 30);
        StubAIContext context = new StubAIContext();
        context.setBettingRound(0); // Pre-flop
        context.setHoleCards(null); // No cards

        PlayerAction action = algorithm.getAction(player, options, context);

        assertNotNull(action);
        assertEquals("fold", action.actionType().name().toLowerCase(), "Should fold with no hole cards");
    }

    @Test
    void testGetAction_preFlopWithPremiumHand() {
        StubGamePlayerInfo player = new StubGamePlayerInfo();
        Card[] aces = {Card.CLUBS_A, Card.SPADES_A};
        player.setChipCount(10000);

        ActionOptions options = new ActionOptions(true, true, true, true, true, 0, 100, 10000, 200, 10000, 30);

        StubAIContext context = new StubAIContext();
        context.setBettingRound(0); // Pre-flop
        context.setPosition(player, 1); // Middle position
        context.setNumActivePlayers(6);
        context.setAmountToCall(0);
        context.setHoleCards(aces);

        PlayerAction action = algorithm.getAction(player, options, context);

        assertNotNull(action);
        // With pocket aces, should not fold
        assertNotEquals("fold", action.actionType().name().toLowerCase(), "Should not fold with pocket aces");
    }

    @Test
    void testSklankskyRanking_pocketAces() {
        Card[] aces = {Card.CLUBS_A, Card.SPADES_A};
        com.donohoedigital.games.poker.engine.HandSorted hand = new com.donohoedigital.games.poker.engine.HandSorted(
                aces[0], aces[1]);

        int rank = SklankskyRanking.getRank(hand);

        assertTrue(rank <= SklankskyRanking.MAXGROUP1, "Pocket aces should be in group 1");
        assertEquals(1, SklankskyRanking.getGroupFromRank(rank), "Should be in group 1");
    }

    @Test
    void testSklankskyRanking_sevenTwoOffsuit() {
        Card[] hand72o = {Card.CLUBS_7, Card.SPADES_2};
        com.donohoedigital.games.poker.engine.HandSorted hand = new com.donohoedigital.games.poker.engine.HandSorted(
                hand72o[0], hand72o[1]);

        int rank = SklankskyRanking.getRank(hand);

        assertEquals(1000, rank, "72o should have rank 1000 (not in groups)");
        assertEquals(10, SklankskyRanking.getGroupFromRank(rank), "Should be in group 10 (unranked)");
    }

    // Stub implementations for testing

    private static class StubGamePlayerInfo implements GamePlayerInfo {
        private int chipCount;
        private int numRebuys;

        @Override
        public int getChipCount() {
            return chipCount;
        }
        @Override
        public int getNumRebuys() {
            return numRebuys;
        }

        public void setChipCount(int chips) {
            this.chipCount = chips;
        }
        public void setNumRebuys(int rebuys) {
            this.numRebuys = rebuys;
        }

        // Minimal implementations
        @Override
        public int getID() {
            return 1;
        }
        @Override
        public String getName() {
            return "TestPlayer";
        }
        @Override
        public boolean isHuman() {
            return false;
        }
        @Override
        public boolean isComputer() {
            return true;
        }
        @Override
        public boolean isObserver() {
            return false;
        }
        @Override
        public boolean isFolded() {
            return false;
        }
        @Override
        public boolean isAllIn() {
            return false;
        }
        @Override
        public int getSeat() {
            return 0;
        }
        @Override
        public boolean isAskShowWinning() {
            return false;
        }
        @Override
        public boolean isAskShowLosing() {
            return false;
        }
        @Override
        public boolean isHumanControlled() {
            return false;
        }
        @Override
        public int getThinkBankMillis() {
            return 0;
        }
        @Override
        public boolean isSittingOut() {
            return false;
        }
        @Override
        public void setSittingOut(boolean sittingOut) {
            // No-op for stub
        }
        @Override
        public boolean isLocallyControlled() {
            return true;
        }
        @Override
        public void setTimeoutMillis(int millis) {
            // No-op for stub
        }
        @Override
        public void setTimeoutMessageSecondsLeft(int seconds) {
            // No-op for stub
        }
    }

    private static class StubAIContext implements AIContext {
        private int bettingRound;
        private int position;
        private int numActivePlayers = 6;
        private int amountToCall;
        private TournamentContext tournament;
        private Card[] holeCards;

        public void setBettingRound(int round) {
            this.bettingRound = round;
        }
        public void setPosition(GamePlayerInfo player, int pos) {
            this.position = pos;
        }
        public void setNumActivePlayers(int num) {
            this.numActivePlayers = num;
        }
        public void setAmountToCall(int amount) {
            this.amountToCall = amount;
        }
        public void setTournament(TournamentContext t) {
            this.tournament = t;
        }
        public void setHoleCards(Card[] cards) {
            this.holeCards = cards;
        }

        @Override
        public int getBettingRound() {
            return bettingRound;
        }
        @Override
        public int getPosition(GamePlayerInfo player) {
            return position;
        }
        @Override
        public int getNumActivePlayers() {
            return numActivePlayers;
        }
        @Override
        public int getAmountToCall(GamePlayerInfo player) {
            return amountToCall;
        }
        @Override
        public TournamentContext getTournament() {
            return tournament;
        }

        // Minimal stubs
        @Override
        public GameTable getTable() {
            return null;
        }
        @Override
        public GameHand getCurrentHand() {
            return null;
        }
        @Override
        public boolean isButton(GamePlayerInfo player) {
            return position == 3;
        }
        @Override
        public boolean isSmallBlind(GamePlayerInfo player) {
            return position == 4;
        }
        @Override
        public boolean isBigBlind(GamePlayerInfo player) {
            return position == 5;
        }
        @Override
        public int getPotSize() {
            return 100;
        }
        @Override
        public int getAmountBetThisRound(GamePlayerInfo player) {
            return 0;
        }
        @Override
        public int getLastBetAmount() {
            return 0;
        }
        @Override
        public int getNumPlayersYetToAct(GamePlayerInfo player) {
            return 2;
        }
        @Override
        public int getNumPlayersWhoActed(GamePlayerInfo player) {
            return 3;
        }
        @Override
        public boolean hasBeenBet() {
            return amountToCall > 0;
        }
        @Override
        public boolean hasBeenRaised() {
            return false;
        }
        @Override
        public GamePlayerInfo getLastBettor() {
            return null;
        }
        @Override
        public GamePlayerInfo getLastRaiser() {
            return null;
        }
        @Override
        public int evaluateHandRank(Card[] hole, Card[] community) {
            return 0;
        }
        @Override
        public long evaluateHandScore(Card[] hole, Card[] community) {
            return 0;
        }
        @Override
        public double calculateImprovementOdds(Card[] hole, Card[] community) {
            return 0.0;
        }
        @Override
        public Card[] getHoleCards(GamePlayerInfo player) {
            return holeCards;
        }
        @Override
        public Card[] getCommunityCards() {
            return new Card[0];
        }
        @Override
        public int getNumCallers() {
            return 0;
        }
        @Override
        public Card[] getBest5Cards(Card[] hole, Card[] community) {
            return new Card[5];
        }
        @Override
        public int[] getBest5CardRanks(Card[] hole, Card[] community) {
            return new int[5];
        }
        @Override
        public boolean isHoleCardInvolved(Card[] hole, Card[] community) {
            return true;
        }
        @Override
        public int getMajorSuit(Card[] hole, Card[] community) {
            return -1;
        }
        @Override
        public boolean hasFlushDraw(Card[] communityCards) {
            return false;
        }
        @Override
        public boolean hasStraightDraw(Card[] communityCards) {
            return false;
        }
        @Override
        public int getNumOpponentStraights(Card[] communityCards) {
            return 0;
        }
        @Override
        public boolean isRebuyPeriodActive() {
            return false;
        }
        @Override
        public boolean isNutFlush(Card[] holeCards, Card[] communityCards, int majorSuit, int nCards) {
            return false;
        }

        @Override
        public double calculateHandStrength(Card[] holeCards, Card[] communityCards, int numOpponents) {
            // Return moderate strength for testing
            return 0.5; // 50% strength
        }

        @Override
        public int getLastActionInRound(GamePlayerInfo player, int bettingRound) {
            return ACTION_NONE; // Stub: no action history tracking
        }

        @Override
        public int getOpponentRaiseFrequency(GamePlayerInfo opponent, int bettingRound) {
            return 50; // Stub: neutral assumption (50% frequency)
        }

        @Override
        public int getOpponentBetFrequency(GamePlayerInfo opponent, int bettingRound) {
            return 50; // Stub: neutral assumption (50% frequency)
        }
    }

    private static class StubTournamentContext implements TournamentContext {
        private int startingChips = 1000;

        public void setStartingChips(int chips) {
            this.startingChips = chips;
        }

        @Override
        public int getStartingChips() {
            return startingChips;
        }

        // Minimal stubs
        @Override
        public int getLevel() {
            return 1;
        }
        @Override
        public int getSmallBlind(int level) {
            return 10;
        }
        @Override
        public int getBigBlind(int level) {
            return 20;
        }
        @Override
        public int getAnte(int level) {
            return 0;
        }
        @Override
        public int getNumTables() {
            return 1;
        }
        @Override
        public GameTable getTable(int index) {
            return null;
        }
        @Override
        public int getNumPlayers() {
            return 6;
        }
        @Override
        public GamePlayerInfo getPlayerByID(int playerId) {
            return null;
        }
        @Override
        public boolean isPractice() {
            return true;
        }
        @Override
        public boolean isOnlineGame() {
            return false;
        }
        @Override
        public boolean isGameOver() {
            return false;
        }
        @Override
        public void nextLevel() {
            // No-op for stub
        }
        @Override
        public boolean isLevelExpired() {
            return false;
        }
        @Override
        public void advanceClockBreak() {
            // No-op for stub
        }
        @Override
        public void startGameClock() {
            // No-op for stub
        }
        @Override
        public int getLastMinChip() {
            return 1;
        }
        @Override
        public int getMinChip() {
            return 1;
        }
        @Override
        public void advanceClock() {
            // No-op for stub
        }
        @Override
        public boolean isBreakLevel(int level) {
            return false;
        }
        @Override
        public GamePlayerInfo getLocalPlayer() {
            return null;
        }
        @Override
        public boolean isScheduledStartEnabled() {
            return false;
        }
        @Override
        public long getScheduledStartTime() {
            return 0;
        }
        @Override
        public int getMinPlayersForScheduledStart() {
            return 2;
        }
        @Override
        public int getTimeoutForRound(int round) {
            return 30;
        }
        @Override
        public GameTable getCurrentTable() {
            return null;
        }
        @Override
        public int getTimeoutSeconds() {
            return 30;
        }
        @Override
        public boolean isOnePlayerLeft() {
            return false;
        }

        @Override
        public boolean isRebuyPeriodActive(GamePlayerInfo player) {
            return false; // Stub: no rebuy period
        }
    }
}
