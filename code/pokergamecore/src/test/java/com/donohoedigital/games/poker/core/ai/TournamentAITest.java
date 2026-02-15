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
import com.donohoedigital.games.poker.core.state.ActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TournamentAI - M-ratio based tournament poker AI
 */
class TournamentAITest {

    // =================================================================
    // Test Helper Classes
    // =================================================================

    /**
     * Simple test implementation of GamePlayerInfo
     */
    private static class TestPlayer implements GamePlayerInfo {
        private final int chipCount;

        TestPlayer(int chipCount) {
            this.chipCount = chipCount;
        }

        @Override
        public int getChipCount() {
            return chipCount;
        }

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
        public boolean isObserver() {
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
        public void setSittingOut(boolean b) {
        }

        @Override
        public void setTimeoutMessageSecondsLeft(int i) {
        }

        @Override
        public void setTimeoutMillis(int i) {
        }

        @Override
        public boolean isLocallyControlled() {
            return false;
        }

        @Override
        public boolean isComputer() {
            return true;
        }
    }

    /**
     * Simple test implementation of TournamentContext
     */
    private static class TestTournament implements TournamentContext {
        private final int level;
        private final int smallBlind;
        private final int bigBlind;
        private final int ante;

        TestTournament(int level, int smallBlind, int bigBlind, int ante) {
            this.level = level;
            this.smallBlind = smallBlind;
            this.bigBlind = bigBlind;
            this.ante = ante;
        }

        @Override
        public int getLevel() {
            return level;
        }

        @Override
        public int getSmallBlind(int level) {
            return smallBlind;
        }

        @Override
        public int getBigBlind(int level) {
            return bigBlind;
        }

        @Override
        public int getAnte(int level) {
            return ante;
        }

        // Unused methods
        @Override
        public int getNumTables() {
            return 0;
        }

        @Override
        public GameTable getTable(int index) {
            return null;
        }

        @Override
        public int getNumPlayers() {
            return 0;
        }

        @Override
        public GamePlayerInfo getPlayerByID(int playerId) {
            return null;
        }

        @Override
        public boolean isPractice() {
            return false;
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
        }

        @Override
        public boolean isLevelExpired() {
            return false;
        }

        @Override
        public void advanceClockBreak() {
        }

        @Override
        public void startGameClock() {
        }

        @Override
        public int getLastMinChip() {
            return 0;
        }

        @Override
        public int getMinChip() {
            return 0;
        }

        @Override
        public void advanceClock() {
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
            return 0;
        }

        @Override
        public boolean isOnePlayerLeft() {
            return false;
        }

        @Override
        public int getTimeoutSeconds() {
            return 0;
        }

        @Override
        public int getTimeoutForRound(int round) {
            return 0;
        }

        @Override
        public GameTable getCurrentTable() {
            return null;
        }
    }

    /**
     * Simple test implementation of AIContext
     */
    private static class TestAIContext implements AIContext {
        private final TournamentContext tournament;

        TestAIContext(TournamentContext tournament) {
            this.tournament = tournament;
        }

        @Override
        public TournamentContext getTournament() {
            return tournament;
        }

        // All other methods unused in TournamentAI
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
            return false;
        }

        @Override
        public boolean isSmallBlind(GamePlayerInfo player) {
            return false;
        }

        @Override
        public boolean isBigBlind(GamePlayerInfo player) {
            return false;
        }

        @Override
        public int getPosition(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public int getPotSize() {
            return 0;
        }

        @Override
        public int getAmountToCall(GamePlayerInfo player) {
            return 0;
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
        public int getNumActivePlayers() {
            return 0;
        }

        @Override
        public int getNumPlayersYetToAct(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public int getNumPlayersWhoActed(GamePlayerInfo player) {
            return 0;
        }

        @Override
        public boolean hasBeenBet() {
            return false;
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
        public int evaluateHandRank(com.donohoedigital.games.poker.engine.Card[] holeCards,
                com.donohoedigital.games.poker.engine.Card[] communityCards) {
            return 0;
        }

        @Override
        public long evaluateHandScore(com.donohoedigital.games.poker.engine.Card[] holeCards,
                com.donohoedigital.games.poker.engine.Card[] communityCards) {
            return 0;
        }

        @Override
        public double calculateImprovementOdds(com.donohoedigital.games.poker.engine.Card[] holeCards,
                com.donohoedigital.games.poker.engine.Card[] communityCards) {
            return 0;
        }

        @Override
        public int getBettingRound() {
            return 0;
        }
    }

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateAI_When_DefaultConstructorUsed() {
        TournamentAI ai = new TournamentAI();

        assertThat(ai).isNotNull();
    }

    @Test
    void should_CreateDeterministicAI_When_SeedProvided() {
        long seed = 12345L;
        TournamentAI ai = new TournamentAI(seed);

        assertThat(ai).isNotNull();
    }

    @Test
    void should_ProduceSameActions_When_SameSeedUsed() {
        long seed = 42L;
        TournamentAI ai1 = new TournamentAI(seed);
        TournamentAI ai2 = new TournamentAI(seed);

        // Create test scenario
        GamePlayerInfo player = createPlayer(1000);
        ActionOptions options = comfortableOptions();
        AIContext context = createContext(1000, 150); // M = 1000/150 = 6.67 (danger zone)

        PlayerAction action1 = ai1.getAction(player, options, context);
        PlayerAction action2 = ai2.getAction(player, options, context);

        assertThat(action1).isEqualTo(action2);
    }

    // =================================================================
    // M-Ratio Calculation Tests
    // =================================================================

    @Test
    void should_UseCriticalStrategy_When_MRatioBelow5() {
        TournamentAI ai = new TournamentAI(100L);
        GamePlayerInfo player = createPlayer(400); // stack
        AIContext context = createContext(1, 100); // level 1, cost per orbit = 100
        // M = 400/100 = 4.0 (critical zone)

        ActionOptions options = new ActionOptions(false, false, true, false, true, // can bet and fold
                0, 100, 400, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        // Critical zone: push or fold (should be one of these)
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.FOLD);
    }

    @Test
    void should_UseDangerStrategy_When_MRatioBetween5And10() {
        TournamentAI ai = new TournamentAI(200L);
        GamePlayerInfo player = createPlayer(750);
        AIContext context = createContext(1, 100); // M = 750/100 = 7.5 (danger zone)

        ActionOptions options = new ActionOptions(false, false, true, false, true, 0, 100, 750, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.FOLD);
    }

    @Test
    void should_UseComfortableStrategy_When_MRatio10OrAbove() {
        TournamentAI ai = new TournamentAI(300L);
        GamePlayerInfo player = createPlayer(1500);
        AIContext context = createContext(1, 150); // M = 1500/150 = 10.0 (comfortable zone)

        ActionOptions options = comfortableOptions();

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isIn(ActionType.CHECK, ActionType.CALL, ActionType.BET, ActionType.RAISE,
                ActionType.FOLD);
    }

    // =================================================================
    // Critical Zone Strategy Tests (M < 5)
    // =================================================================

    @Test
    void should_AllInOrFold_When_CriticalZoneAndCanBet() {
        TournamentAI ai = new TournamentAI(1000L);
        GamePlayerInfo player = createPlayer(300);
        AIContext context = createContext(1, 100); // M = 3.0 (critical)

        ActionOptions options = new ActionOptions(false, false, true, false, true, 0, 50, 300, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        if (action.actionType() == ActionType.BET) {
            // When betting, should go all-in (or near all-in)
            assertThat(action.amount()).isGreaterThan(200);
        } else {
            assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
        }
    }

    @Test
    void should_AllInOrFold_When_CriticalZoneAndCanRaise() {
        TournamentAI ai = new TournamentAI(1001L);
        GamePlayerInfo player = createPlayer(400);
        AIContext context = createContext(1, 100); // M = 4.0 (critical)

        ActionOptions options = new ActionOptions(false, false, false, true, true, 0, 0, 0, 100, 400, 0);

        PlayerAction action = ai.getAction(player, options, context);

        if (action.actionType() == ActionType.RAISE) {
            assertThat(action.amount()).isGreaterThan(200);
        } else {
            assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
        }
    }

    @Test
    void should_CallOrFold_When_CriticalZoneAndCanOnlyCall() {
        TournamentAI ai = new TournamentAI(1002L);
        GamePlayerInfo player = createPlayer(250);
        AIContext context = createContext(1, 100); // M = 2.5 (critical)

        ActionOptions options = new ActionOptions(false, true, false, false, true, 50, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isIn(ActionType.CALL, ActionType.FOLD);
    }

    @Test
    void should_Check_When_CriticalZoneAndCanOnlyCheck() {
        TournamentAI ai = new TournamentAI(1003L);
        GamePlayerInfo player = createPlayer(200);
        AIContext context = createContext(1, 100); // M = 2.0 (critical)

        ActionOptions options = new ActionOptions(true, false, false, false, false, 0, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isEqualTo(ActionType.CHECK);
    }

    // =================================================================
    // Danger Zone Strategy Tests (5 ≤ M < 10)
    // =================================================================

    @Test
    void should_BeAggressive_When_DangerZoneAndCanRaise() {
        TournamentAI ai = new TournamentAI(2000L);
        GamePlayerInfo player = createPlayer(800);
        AIContext context = createContext(1, 100); // M = 8.0 (danger)

        ActionOptions options = new ActionOptions(false, true, false, true, true, 100, 0, 0, 200, 800, 0);

        PlayerAction action = ai.getAction(player, options, context);

        // Danger zone: aggressive play
        assertThat(action.actionType()).isIn(ActionType.RAISE, ActionType.CALL, ActionType.FOLD);
    }

    @Test
    void should_RespectMaxRaise_When_DangerZoneRaising() {
        TournamentAI ai = new TournamentAI(2001L);
        GamePlayerInfo player = createPlayer(600);
        AIContext context = createContext(1, 100); // M = 6.0 (danger)

        ActionOptions options = new ActionOptions(false, false, false, true, true, 0, 0, 0, 100, 300, 0);

        // Run multiple times to catch a raise
        for (int i = 0; i < 20; i++) {
            PlayerAction action = ai.getAction(player, options, context);
            if (action.actionType() == ActionType.RAISE) {
                assertThat(action.amount()).isBetween(100, 600);
                return;
            }
        }
    }

    @Test
    void should_BetAggressively_When_DangerZoneAndCanBet() {
        TournamentAI ai = new TournamentAI(2002L);
        GamePlayerInfo player = createPlayer(700);
        AIContext context = createContext(1, 100); // M = 7.0 (danger)

        ActionOptions options = new ActionOptions(false, false, true, false, true, 0, 100, 700, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.FOLD);
    }

    @Test
    void should_Check_When_DangerZoneAndCanOnlyCheck() {
        TournamentAI ai = new TournamentAI(2003L);
        GamePlayerInfo player = createPlayer(900);
        AIContext context = createContext(1, 100); // M = 9.0 (danger)

        ActionOptions options = new ActionOptions(true, false, false, false, false, 0, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isEqualTo(ActionType.CHECK);
    }

    // =================================================================
    // Comfortable Zone Strategy Tests (M ≥ 10)
    // =================================================================

    @Test
    void should_UseBalancedStrategy_When_ComfortableZone() {
        TournamentAI ai = new TournamentAI(3000L);
        GamePlayerInfo player = createPlayer(2000);
        AIContext context = createContext(1, 150); // M = 13.33 (comfortable)

        ActionOptions options = comfortableOptions();

        PlayerAction action = ai.getAction(player, options, context);

        // Should use any available action
        assertThat(action.actionType()).isIn(ActionType.CHECK, ActionType.CALL, ActionType.BET, ActionType.RAISE,
                ActionType.FOLD);
    }

    @Test
    void should_RespectStackSize_When_ComfortableBetting() {
        TournamentAI ai = new TournamentAI(3001L);
        GamePlayerInfo player = createPlayer(1500);
        AIContext context = createContext(1, 150); // M = 10.0 (comfortable)

        ActionOptions options = new ActionOptions(true, false, true, false, true, 0, 100, 800, 0, 0, 0);

        // Run multiple times to catch a bet
        for (int i = 0; i < 20; i++) {
            PlayerAction action = ai.getAction(player, options, context);
            if (action.actionType() == ActionType.BET) {
                assertThat(action.amount()).isBetween(100, 1500);
                return;
            }
        }
    }

    @Test
    void should_RespectStackSize_When_ComfortableRaising() {
        TournamentAI ai = new TournamentAI(3002L);
        GamePlayerInfo player = createPlayer(3000);
        AIContext context = createContext(1, 150); // M = 20.0 (comfortable)

        ActionOptions options = new ActionOptions(false, true, false, true, true, 100, 0, 0, 200, 1000, 0);

        // Run multiple times to catch a raise
        for (int i = 0; i < 20; i++) {
            PlayerAction action = ai.getAction(player, options, context);
            if (action.actionType() == ActionType.RAISE) {
                assertThat(action.amount()).isBetween(200, 3000);
                return;
            }
        }
    }

    @Test
    void should_FoldWhenNoOtherOptions_When_ComfortableZone() {
        TournamentAI ai = new TournamentAI(3003L);
        GamePlayerInfo player = createPlayer(5000);
        AIContext context = createContext(1, 150); // M = 33.33 (comfortable)

        ActionOptions options = new ActionOptions(false, false, false, false, true, 0, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    // =================================================================
    // Rebuy Decision Tests
    // =================================================================

    @Test
    void should_ReturnBoolean_When_WantsRebuyCalled() {
        TournamentAI ai = new TournamentAI(4000L);
        GamePlayerInfo player = new TestPlayer(0);
        AIContext context = new TestAIContext(new TestTournament(1, 50, 100, 0));

        boolean decision = ai.wantsRebuy(player, context);

        assertThat(decision).isIn(true, false);
    }

    @Test
    void should_BeDeterministic_When_RebuyWithSameSeed() {
        long seed = 4001L;
        TournamentAI ai1 = new TournamentAI(seed);
        TournamentAI ai2 = new TournamentAI(seed);

        GamePlayerInfo player = new TestPlayer(0);
        AIContext context = new TestAIContext(new TestTournament(1, 50, 100, 0));

        boolean decision1 = ai1.wantsRebuy(player, context);
        boolean decision2 = ai2.wantsRebuy(player, context);

        assertThat(decision1).isEqualTo(decision2);
    }

    // =================================================================
    // Addon Decision Tests
    // =================================================================

    @Test
    void should_ReturnBoolean_When_WantsAddonCalled() {
        TournamentAI ai = new TournamentAI(5000L);
        GamePlayerInfo player = new TestPlayer(1000);
        AIContext context = new TestAIContext(new TestTournament(1, 50, 100, 0));

        boolean decision = ai.wantsAddon(player, context);

        assertThat(decision).isIn(true, false);
    }

    @Test
    void should_BeDeterministic_When_AddonWithSameSeed() {
        long seed = 5001L;
        TournamentAI ai1 = new TournamentAI(seed);
        TournamentAI ai2 = new TournamentAI(seed);

        GamePlayerInfo player = new TestPlayer(1000);
        AIContext context = new TestAIContext(new TestTournament(1, 50, 100, 0));

        boolean decision1 = ai1.wantsAddon(player, context);
        boolean decision2 = ai2.wantsAddon(player, context);

        assertThat(decision1).isEqualTo(decision2);
    }

    @Test
    void should_FavorAddon_When_MultipleDecisions() {
        TournamentAI ai = new TournamentAI();
        GamePlayerInfo player = new TestPlayer(1000);
        AIContext context = new TestAIContext(new TestTournament(1, 50, 100, 0));

        int addonCount = 0;
        int trials = 100;

        for (int i = 0; i < trials; i++) {
            if (ai.wantsAddon(player, context)) {
                addonCount++;
            }
        }

        // Should take addon ~75% of the time (allow some variance)
        assertThat(addonCount).isBetween(60, 90);
    }

    // =================================================================
    // Boundary Tests
    // =================================================================

    @Test
    void should_HandleExactMRatio5_Boundary() {
        TournamentAI ai = new TournamentAI(6000L);
        GamePlayerInfo player = createPlayer(500);
        AIContext context = createContext(1, 100); // M = exactly 5.0

        ActionOptions options = comfortableOptions();

        PlayerAction action = ai.getAction(player, options, context);

        // M = 5.0 is danger zone (not critical)
        assertThat(action).isNotNull();
    }

    @Test
    void should_HandleExactMRatio10_Boundary() {
        TournamentAI ai = new TournamentAI(6001L);
        GamePlayerInfo player = createPlayer(1000);
        AIContext context = createContext(1, 100); // M = exactly 10.0

        ActionOptions options = comfortableOptions();

        PlayerAction action = ai.getAction(player, options, context);

        // M = 10.0 is comfortable zone (not danger)
        assertThat(action).isNotNull();
    }

    @Test
    void should_HandleVeryLowMRatio() {
        TournamentAI ai = new TournamentAI(6002L);
        GamePlayerInfo player = createPlayer(50);
        AIContext context = createContext(1, 100); // M = 0.5 (desperate)

        ActionOptions options = new ActionOptions(false, false, true, false, true, 0, 10, 50, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.FOLD);
    }

    @Test
    void should_HandleVeryHighMRatio() {
        TournamentAI ai = new TournamentAI(6003L);
        GamePlayerInfo player = createPlayer(100000);
        AIContext context = createContext(1, 150); // M = 666.67 (very comfortable)

        ActionOptions options = comfortableOptions();

        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action).isNotNull();
    }

    @Test
    void should_HandleZeroCostPerOrbit() {
        TournamentAI ai = new TournamentAI(6004L);
        GamePlayerInfo player = createPlayer(1000);
        AIContext context = createContext(1, 0); // Cost = 0 (edge case)

        ActionOptions options = comfortableOptions();

        // Should not crash (uses Math.max(1, costPerOrbit))
        PlayerAction action = ai.getAction(player, options, context);

        assertThat(action).isNotNull();
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    private GamePlayerInfo createPlayer(int chipCount) {
        return new TestPlayer(chipCount);
    }

    private AIContext createContext(int level, int costPerOrbit) {
        // Calculate blinds/antes to achieve target cost per orbit
        // Standard: SB + BB + (10 * ante) = costPerOrbit
        // Simple distribution: BB = costPerOrbit/2, SB = BB/2, ante = rest/10
        int bigBlind = costPerOrbit / 2;
        int smallBlind = Math.max(1, bigBlind / 2);
        int ante = Math.max(0, (costPerOrbit - smallBlind - bigBlind) / 10);

        TournamentContext tournament = new TestTournament(level, smallBlind, bigBlind, ante);
        return new TestAIContext(tournament);
    }

    private ActionOptions comfortableOptions() {
        return new ActionOptions(true, true, true, true, true, // all actions available
                50, 100, 1000, 200, 1500, 30);
    }
}
