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
package com.donohoedigital.games.poker.gameserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.core.ActionOptions;
import com.donohoedigital.games.poker.engine.PlayerAction;
import com.donohoedigital.games.poker.engine.state.ActionType;

/**
 * Tests for {@link ServerPlayerActionProvider#validateAction} — verifies that
 * illegal inputs are clamped, rejected, or converted to fold as appropriate.
 */
class ServerPlayerActionProviderValidationTest {

    private ServerPlayerActionProvider provider;

    @BeforeEach
    void setUp() {
        // validateAction doesn't use any constructor dependencies, so pass minimal
        // values
        provider = new ServerPlayerActionProvider(null, ignored -> {
        }, 0, 0, new ConcurrentHashMap<>());
    }

    /**
     * Helper to build ActionOptions with only the fields relevant to a test. All
     * boolean flags default to false and all amounts default to 0.
     */
    private ActionOptions options(boolean canCheck, boolean canCall, boolean canBet, boolean canRaise, boolean canFold,
            int callAmount, int minBet, int maxBet, int minRaise, int maxRaise) {
        return new ActionOptions(canCheck, canCall, canBet, canRaise, canFold, callAmount, minBet, maxBet, minRaise,
                maxRaise, 0);
    }

    // Poker rule: you can't bet more than your chips; maxBet represents the
    // player's remaining stack, so exceeding it must be clamped to all-in.
    @Test
    void betExceedsMax_clampsToMax() {
        ActionOptions opts = options(false, false, true, false, true, 0, 100, 500, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.bet(999), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(500);
    }

    @Test
    void betBelowMin_clampsToMin() {
        ActionOptions opts = options(false, false, true, false, true, 0, 100, 500, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.bet(50), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(100);
    }

    @Test
    void betWithinRange_unchanged() {
        ActionOptions opts = options(false, false, true, false, true, 0, 100, 500, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.bet(300), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(300);
    }

    // Poker rule: you can't check when facing a bet — the player must call,
    // raise, or fold. Submitting CHECK when there's an outstanding bet converts
    // to FOLD.
    @Test
    void checkWhenCannotCheck_folds() {
        ActionOptions opts = options(false, true, false, false, true, 100, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.check(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void checkWhenCanCheck_unchanged() {
        ActionOptions opts = options(true, false, false, false, true, 0, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.check(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.CHECK);
    }

    @Test
    void callWhenCannotCall_folds() {
        ActionOptions opts = options(false, false, false, false, true, 0, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.call(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void callWhenCanCall_unchanged() {
        ActionOptions opts = options(false, true, false, false, true, 100, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.call(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.CALL);
    }

    @Test
    void raiseWhenCannotRaise_folds() {
        ActionOptions opts = options(false, false, false, false, true, 0, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.raise(500), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    // Poker rule: a raise must be at least the size of the previous raise
    // (the minimum raise). Submitting below that floor clamps up to the minimum.
    @Test
    void raiseBelowMin_clampsToMin() {
        ActionOptions opts = options(false, false, false, true, true, 0, 0, 0, 200, 1000);
        PlayerAction result = provider.validateAction(PlayerAction.raise(50), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(result.amount()).isEqualTo(200);
    }

    @Test
    void raiseAboveMax_clampsToMax() {
        ActionOptions opts = options(false, false, false, true, true, 0, 0, 0, 200, 1000);
        PlayerAction result = provider.validateAction(PlayerAction.raise(5000), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(result.amount()).isEqualTo(1000);
    }

    // Poker rule: fold is always valid regardless of game state.
    @Test
    void foldAlwaysValid() {
        ActionOptions opts = options(true, true, true, true, true, 100, 100, 500, 200, 1000);
        PlayerAction result = provider.validateAction(PlayerAction.fold(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    @Test
    void betWhenCannotBet_folds() {
        ActionOptions opts = options(false, false, false, false, true, 0, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.bet(200), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    // Poker rule: a player may go all-in even when their remaining stack is
    // below the normal minimum raise. The engine signals this by setting
    // minRaise == maxRaise == the player's stack. The raise must be accepted
    // (not converted to fold).
    @Test
    void allInBelowNormalMinRaise_accepted() {
        // Player has only 80 chips but normal min raise would be 200.
        // Engine sets minRaise = maxRaise = 80 (all-in).
        ActionOptions opts = options(false, false, false, true, true, 0, 0, 0, 80, 80);
        PlayerAction result = provider.validateAction(PlayerAction.raise(80), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.RAISE);
        assertThat(result.amount()).isEqualTo(80);
    }

    // Poker rule: fold is always valid — even if the options don't explicitly
    // list canFold (edge case). validateAction converts to fold via the else
    // branch, so the action type is still FOLD.
    @Test
    void foldWhenCanFoldFalse_stillFolds() {
        ActionOptions opts = options(true, false, false, false, false, 0, 0, 0, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.fold(), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.FOLD);
    }

    // Poker rule: all-in bet — when maxBet equals minBet the player is putting
    // in their entire stack. Even if the submitted amount is below that, it
    // clamps up to the all-in amount.
    @Test
    void allInBet_clampedToStack() {
        // Player can only bet exactly 150 (their entire stack).
        ActionOptions opts = options(false, false, true, false, true, 0, 150, 150, 0, 0);
        PlayerAction result = provider.validateAction(PlayerAction.bet(50), opts);
        assertThat(result.actionType()).isEqualTo(ActionType.BET);
        assertThat(result.amount()).isEqualTo(150);
    }
}
