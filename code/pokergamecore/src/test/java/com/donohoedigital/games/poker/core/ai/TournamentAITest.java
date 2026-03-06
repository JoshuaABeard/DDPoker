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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.core.TournamentContext;
import com.donohoedigital.games.poker.engine.GamePlayerInfo;
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.engine.state.ActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TournamentAITest {

    private static final long SEED = 12345L;

    private TournamentAI ai;
    private GamePlayerInfo player;
    private AIContext context;
    private TournamentContext tournament;

    @BeforeEach
    void setUp() {
        ai = new TournamentAI(SEED);
        player = mock(GamePlayerInfo.class);
        context = mock(AIContext.class);
        tournament = mock(TournamentContext.class);
        when(context.getTournament()).thenReturn(tournament);

        // Default blind structure: SB=50, BB=100, ante=0, level=1
        when(tournament.getLevel()).thenReturn(1);
        when(tournament.getSmallBlind(1)).thenReturn(50);
        when(tournament.getBigBlind(1)).thenReturn(100);
        when(tournament.getAnte(1)).thenReturn(0);
    }

    // ========== getAction - Critical Zone (M < 5) ==========

    @Test
    void should_ReturnActionInCriticalZone_When_MRatioBelow5() {
        // Stack=400, costPerOrbit=150 (50+100+0), M=400/150=2.67 -> critical
        when(player.getChipCount()).thenReturn(400);

        // Can bet and raise
        ActionOptions options = new ActionOptions(false, true, true, false, true, 100, 100, 400, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action).isNotNull();
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.FOLD);
    }

    @Test
    void should_GoAllInOrFold_When_CriticalZoneWithBet() {
        // M < 5: 70% all-in, 30% fold when can bet
        when(player.getChipCount()).thenReturn(300);
        ActionOptions options = new ActionOptions(false, false, true, false, true, 0, 100, 300, 0, 0, 0);

        // Run multiple times with fresh AI instances to verify only bet/fold occur
        int betCount = 0;
        int foldCount = 0;
        for (int i = 0; i < 100; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            PlayerAction action = freshAi.getAction(player, options, context);
            if (action.actionType() == ActionType.BET) {
                betCount++;
                assertThat(action.amount()).isEqualTo(300); // all-in
            } else {
                foldCount++;
                assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
            }
        }
        // Should see both bet and fold, roughly 70/30
        assertThat(betCount).isGreaterThan(0);
        assertThat(foldCount).isGreaterThan(0);
    }

    @Test
    void should_CallOrFold_When_CriticalZoneWithCallOnly() {
        // M < 5, can only call (no bet/raise)
        when(player.getChipCount()).thenReturn(200);
        ActionOptions options = new ActionOptions(false, true, false, false, true, 100, 0, 0, 0, 0, 0);

        int callCount = 0;
        int foldCount = 0;
        for (int i = 0; i < 100; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            PlayerAction action = freshAi.getAction(player, options, context);
            if (action.actionType() == ActionType.CALL)
                callCount++;
            else
                foldCount++;
        }
        assertThat(callCount).isGreaterThan(0);
        assertThat(foldCount).isGreaterThan(0);
    }

    @Test
    void should_Check_When_CriticalZoneWithCheckOnly() {
        when(player.getChipCount()).thenReturn(200);
        ActionOptions options = new ActionOptions(true, false, false, false, false, 0, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action.actionType()).isEqualTo(ActionType.CHECK);
    }

    @Test
    void should_Fold_When_CriticalZoneWithNoOptions() {
        when(player.getChipCount()).thenReturn(200);
        ActionOptions options = new ActionOptions(false, false, false, false, true, 0, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void should_UseRaise_When_CriticalZoneCanRaiseNotBet() {
        when(player.getChipCount()).thenReturn(300);
        ActionOptions options = new ActionOptions(false, false, false, true, true, 0, 0, 0, 100, 300, 0);

        // Run multiple times - should produce raise or fold
        int raiseCount = 0;
        int foldCount = 0;
        for (int i = 0; i < 100; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            PlayerAction action = freshAi.getAction(player, options, context);
            if (action.actionType() == ActionType.RAISE)
                raiseCount++;
            else
                foldCount++;
        }
        assertThat(raiseCount).isGreaterThan(0);
        assertThat(foldCount).isGreaterThan(0);
    }

    // ========== getAction - Danger Zone (5 <= M < 10) ==========

    @Test
    void should_PlayAggressively_When_DangerZone() {
        // Stack=1000, costPerOrbit=150, M=6.67 -> danger zone
        when(player.getChipCount()).thenReturn(1000);
        ActionOptions options = new ActionOptions(true, true, true, true, true, 100, 100, 1000, 200, 1000, 0);

        // Run many times to see action distribution
        int raiseCount = 0;
        int betCount = 0;
        int callCount = 0;
        int checkCount = 0;
        int foldCount = 0;
        for (int i = 0; i < 200; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            PlayerAction action = freshAi.getAction(player, options, context);
            switch (action.actionType()) {
                case RAISE -> raiseCount++;
                case BET -> betCount++;
                case CALL -> callCount++;
                case CHECK -> checkCount++;
                case FOLD -> foldCount++;
                default -> fail("Unexpected action: " + action.actionType());
            }
        }
        // Should see aggressive actions (raise or bet) from at least some
        assertThat(raiseCount + betCount).isGreaterThan(0);
    }

    @Test
    void should_RaiseWithinRange_When_DangerZone() {
        // Stack=1200, M=8 -> danger
        when(player.getChipCount()).thenReturn(1200);
        ActionOptions options = new ActionOptions(false, false, false, true, true, 0, 0, 0, 200, 600, 0);

        int raiseCount = 0;
        for (int i = 0; i < 100; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            PlayerAction action = freshAi.getAction(player, options, context);
            if (action.actionType() == ActionType.RAISE) {
                raiseCount++;
                assertThat(action.amount()).isBetween(200, 1200);
            }
        }
        assertThat(raiseCount).isGreaterThan(0);
    }

    // ========== getAction - Comfortable Zone (M >= 10) ==========

    @Test
    void should_PlayBalanced_When_ComfortableZone() {
        // Stack=3000, costPerOrbit=150, M=20 -> comfortable
        when(player.getChipCount()).thenReturn(3000);
        ActionOptions options = new ActionOptions(true, true, true, true, true, 100, 100, 3000, 200, 3000, 0);

        int checkCount = 0;
        int callCount = 0;
        int betCount = 0;
        int raiseCount = 0;
        int foldCount = 0;
        for (int i = 0; i < 300; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            PlayerAction action = freshAi.getAction(player, options, context);
            switch (action.actionType()) {
                case CHECK -> checkCount++;
                case CALL -> callCount++;
                case BET -> betCount++;
                case RAISE -> raiseCount++;
                case FOLD -> foldCount++;
                default -> fail("Unexpected action: " + action.actionType());
            }
        }
        // Should see a mix of all actions in comfortable zone
        assertThat(checkCount).isGreaterThan(0);
        assertThat(callCount).isGreaterThan(0);
        assertThat(betCount).isGreaterThan(0);
        assertThat(raiseCount).isGreaterThan(0);
        assertThat(foldCount).isGreaterThan(0);
    }

    @Test
    void should_ReturnFold_When_ComfortableZoneEmptyActions() {
        // Stack=3000, M=20, but no options available
        when(player.getChipCount()).thenReturn(3000);
        ActionOptions options = new ActionOptions(false, false, false, false, false, 0, 0, 0, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action.actionType()).isEqualTo(ActionType.FOLD);
    }

    // ========== getAction - M-ratio calculation with antes ==========

    @Test
    void should_IncludeAntesInMRatio_When_AntesPresent() {
        // SB=100, BB=200, ante=25 -> costPerOrbit = 100+200+250 = 550
        when(tournament.getSmallBlind(1)).thenReturn(100);
        when(tournament.getBigBlind(1)).thenReturn(200);
        when(tournament.getAnte(1)).thenReturn(25);

        // Stack=2000, M=2000/550=3.6 -> critical zone (should push/fold)
        when(player.getChipCount()).thenReturn(2000);
        ActionOptions options = new ActionOptions(false, false, true, false, true, 0, 200, 2000, 0, 0, 0);

        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action.actionType()).isIn(ActionType.BET, ActionType.FOLD);
    }

    @Test
    void should_HandleZeroCostPerOrbit_When_AllBlindsZero() {
        // Edge case: blinds are zero -> costPerOrbit = 0 -> Math.max(1, 0) = 1
        when(tournament.getSmallBlind(1)).thenReturn(0);
        when(tournament.getBigBlind(1)).thenReturn(0);
        when(tournament.getAnte(1)).thenReturn(0);

        when(player.getChipCount()).thenReturn(1000);
        ActionOptions options = new ActionOptions(true, false, false, false, true, 0, 0, 0, 0, 0, 0);

        // M = 1000/1 = 1000 -> comfortable zone -> check weight is high
        PlayerAction action = ai.getAction(player, options, context);
        assertThat(action).isNotNull();
    }

    // ========== Deterministic seed ==========

    @Test
    void should_ProduceSameResults_When_SameSeed() {
        when(player.getChipCount()).thenReturn(3000);
        ActionOptions options = new ActionOptions(true, true, true, true, true, 100, 100, 3000, 200, 3000, 0);

        TournamentAI ai1 = new TournamentAI(SEED);
        TournamentAI ai2 = new TournamentAI(SEED);

        for (int i = 0; i < 20; i++) {
            PlayerAction a1 = ai1.getAction(player, options, context);
            PlayerAction a2 = ai2.getAction(player, options, context);
            assertThat(a1).isEqualTo(a2);
        }
    }

    // ========== wantsRebuy ==========

    @Test
    void should_ReturnBooleanForRebuy_When_Called() {
        // With fixed seed, wantsRebuy is deterministic
        boolean result = ai.wantsRebuy(player, context);
        assertThat(result).isInstanceOf(Boolean.class);
    }

    @Test
    void should_ReturnBothRebuyValues_When_MultipleSeeds() {
        int trueCount = 0;
        int falseCount = 0;
        for (long i = 0; i < 1000; i++) {
            TournamentAI freshAi = new TournamentAI(i * 7919L);
            if (freshAi.wantsRebuy(player, context))
                trueCount++;
            else
                falseCount++;
        }
        // ~50% chance, should see both
        assertThat(trueCount).isGreaterThan(0);
        assertThat(falseCount).isGreaterThan(0);
    }

    // ========== wantsAddon ==========

    @Test
    void should_ReturnBooleanForAddon_When_Called() {
        boolean result = ai.wantsAddon(player, context);
        assertThat(result).isInstanceOf(Boolean.class);
    }

    @Test
    void should_BiasTowardAddon_When_MultipleSeeds() {
        int trueCount = 0;
        int falseCount = 0;
        for (int i = 0; i < 200; i++) {
            TournamentAI freshAi = new TournamentAI(i);
            if (freshAi.wantsAddon(player, context))
                trueCount++;
            else
                falseCount++;
        }
        // 75% bias toward addon
        assertThat(trueCount).isGreaterThan(falseCount);
        assertThat(trueCount).isGreaterThan(0);
        assertThat(falseCount).isGreaterThan(0);
    }

    // ========== Default constructor ==========

    @Test
    void should_CreateWithDefaultSeed_When_NoArgConstructor() {
        TournamentAI defaultAi = new TournamentAI();
        when(player.getChipCount()).thenReturn(3000);
        ActionOptions options = new ActionOptions(true, false, false, false, true, 0, 0, 0, 0, 0, 0);

        PlayerAction action = defaultAi.getAction(player, options, context);
        assertThat(action).isNotNull();
    }
}
