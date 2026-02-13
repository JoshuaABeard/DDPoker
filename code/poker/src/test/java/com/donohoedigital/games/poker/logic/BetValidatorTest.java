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
package com.donohoedigital.games.poker.logic;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.PokerTableInput;
import com.donohoedigital.games.poker.logic.BetValidator.BetValidationResult;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BetValidator - bet validation and input mode determination
 * logic extracted from Bet.java. Tests run in headless mode with no UI
 * dependencies. Part of Wave 1 testability refactoring.
 */
@Tag("unit")
class BetValidatorTest {

    private PokerGame game;
    private PokerTable table;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create minimal game and table for validation tests
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        game.setProfile(profile);
        table = new PokerTable(game, 1);
    }

    // =================================================================
    // determineInputMode() Tests
    // =================================================================

    @Test
    void should_ReturnCheckBet_When_NoCallAndNoBet() {
        // No call needed (toCall=0) and no bet yet (currentBet=0)
        // Player can check or bet
        int mode = BetValidator.determineInputMode(0, 0);

        assertThat(mode).isEqualTo(PokerTableInput.MODE_CHECK_BET);
    }

    @Test
    void should_ReturnCheckRaise_When_NoCallButThereIsBet() {
        // No call needed (toCall=0) but there's already a bet (currentBet>0)
        // This happens when player has already matched the bet
        // Player can check or raise
        int mode = BetValidator.determineInputMode(0, 100);

        assertThat(mode).isEqualTo(PokerTableInput.MODE_CHECK_RAISE);
    }

    @Test
    void should_ReturnCallRaise_When_CallIsNeeded() {
        // Call is needed (toCall>0)
        // Player must call or raise
        int mode = BetValidator.determineInputMode(100, 100);

        assertThat(mode).isEqualTo(PokerTableInput.MODE_CALL_RAISE);
    }

    @Test
    void should_ReturnCallRaise_When_CallIsNeededRegardlessOfCurrentBet() {
        // Call is needed even if currentBet is 0 (edge case)
        int mode = BetValidator.determineInputMode(50, 0);

        assertThat(mode).isEqualTo(PokerTableInput.MODE_CALL_RAISE);
    }

    @Test
    void should_ReturnCheckRaise_When_NoCallButLargeBet() {
        // No call needed but large bet exists
        int mode = BetValidator.determineInputMode(0, 1000);

        assertThat(mode).isEqualTo(PokerTableInput.MODE_CHECK_RAISE);
    }

    // =================================================================
    // validateBetAmount() Tests
    // =================================================================

    @Test
    void should_ReturnValid_When_AmountIsExactMultipleOfMinChip() {
        table.setMinChip(5);

        BetValidationResult result = BetValidator.validateBetAmount(table, 100);

        assertThat(result.isValid()).isTrue();
        assertThat(result.needsRounding()).isFalse();
        assertThat(result.getOriginalAmount()).isEqualTo(100);
        assertThat(result.getRoundedAmount()).isEqualTo(100);
    }

    @Test
    void should_ReturnInvalid_When_AmountNeedsRounding() {
        table.setMinChip(10);

        BetValidationResult result = BetValidator.validateBetAmount(table, 123);

        assertThat(result.isValid()).isFalse();
        assertThat(result.needsRounding()).isTrue();
        assertThat(result.getOriginalAmount()).isEqualTo(123);
        assertThat(result.getRoundedAmount()).isEqualTo(120);
    }

    @Test
    void should_RoundDown_When_BelowHalfwayPoint() {
        table.setMinChip(10);

        // 123 % 10 = 3, which is < 5, so round down to 120
        BetValidationResult result = BetValidator.validateBetAmount(table, 123);

        assertThat(result.getRoundedAmount()).isEqualTo(120);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_RoundUp_When_AtHalfwayPoint() {
        table.setMinChip(10);

        // 125 % 10 = 5, which is >= 5, so round up to 130
        BetValidationResult result = BetValidator.validateBetAmount(table, 125);

        assertThat(result.getRoundedAmount()).isEqualTo(130);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_RoundUp_When_AboveHalfwayPoint() {
        table.setMinChip(10);

        // 128 % 10 = 8, which is >= 5, so round up to 130
        BetValidationResult result = BetValidator.validateBetAmount(table, 128);

        assertThat(result.getRoundedAmount()).isEqualTo(130);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_HandleMinChipOfOne() {
        table.setMinChip(1);

        // Any amount is valid when min chip is 1
        BetValidationResult result = BetValidator.validateBetAmount(table, 123);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getRoundedAmount()).isEqualTo(123);
    }

    @Test
    void should_HandleMinChipOfTwentyFive() {
        table.setMinChip(25);

        // 113 % 25 = 13, which is >= 12.5, so round up to 125
        BetValidationResult result = BetValidator.validateBetAmount(table, 113);

        assertThat(result.getRoundedAmount()).isEqualTo(125);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_HandleMinChipOfHundred() {
        table.setMinChip(100);

        // 550 % 100 = 50, which is >= 50, so round up to 600
        BetValidationResult result = BetValidator.validateBetAmount(table, 550);

        assertThat(result.getRoundedAmount()).isEqualTo(600);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_HandleZeroAmount() {
        table.setMinChip(5);

        BetValidationResult result = BetValidator.validateBetAmount(table, 0);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getRoundedAmount()).isEqualTo(0);
    }

    @Test
    void should_HandleSmallAmountRoundingDown() {
        table.setMinChip(5);

        // 2 % 5 = 2, which is < 2.5, so round down to 0
        BetValidationResult result = BetValidator.validateBetAmount(table, 2);

        assertThat(result.getRoundedAmount()).isEqualTo(0);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_HandleSmallAmountRoundingUp() {
        table.setMinChip(5);

        // 3 % 5 = 3, which is >= 2.5, so round up to 5
        BetValidationResult result = BetValidator.validateBetAmount(table, 3);

        assertThat(result.getRoundedAmount()).isEqualTo(5);
        assertThat(result.needsRounding()).isTrue();
    }

    @Test
    void should_HandleLargeAmounts() {
        table.setMinChip(100);

        BetValidationResult result = BetValidator.validateBetAmount(table, 123456);

        // 123456 % 100 = 56, which is >= 50, so round up to 123500
        assertThat(result.getRoundedAmount()).isEqualTo(123500);
        assertThat(result.needsRounding()).isTrue();
    }

    // =================================================================
    // BetValidationResult Tests
    // =================================================================

    @Test
    void should_CreateValidResult_When_NoRoundingNeeded() {
        BetValidationResult result = new BetValidationResult(100, 100, false);

        assertThat(result.getOriginalAmount()).isEqualTo(100);
        assertThat(result.getRoundedAmount()).isEqualTo(100);
        assertThat(result.needsRounding()).isFalse();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void should_CreateInvalidResult_When_RoundingNeeded() {
        BetValidationResult result = new BetValidationResult(123, 120, true);

        assertThat(result.getOriginalAmount()).isEqualTo(123);
        assertThat(result.getRoundedAmount()).isEqualTo(120);
        assertThat(result.needsRounding()).isTrue();
        assertThat(result.isValid()).isFalse();
    }
}
