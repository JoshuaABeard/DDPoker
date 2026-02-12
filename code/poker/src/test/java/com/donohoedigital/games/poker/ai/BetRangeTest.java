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
 * MERCHANTABILITY or FITNESS FOR ANY PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file
 * in the root directory of this project.
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.base.ApplicationError;
import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.*;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for BetRange AI bet sizing logic.
 */
class BetRangeTest {
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer player1;
    private PokerPlayer player2;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for tests (only once)
        if (!com.donohoedigital.config.PropertyConfig.isInitialized()) {
            new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);
        }

        // Create game and table
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);
        table = new PokerTable(game, 1);
        table.setMinChip(1);

        // Create players with chips
        player1 = new PokerPlayer(1, "Player1", true);
        player1.setChipCount(1000);
        player2 = new PokerPlayer(2, "Player2", true);
        player2.setChipCount(1000);

        game.addPlayer(player1);
        game.addPlayer(player2);
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 1);
        table.setButton(0);

        // Give players pocket cards
        player1.newHand('p');
        player2.newHand('p');

        // Create hand
        hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setPlayerOrder(false);
        hand.setCurrentPlayerIndex(0);
        hand.setBigBlind(20);
        hand.setSmallBlind(10);
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    void should_CreateAllInRange_When_AllInTypeProvided() {
        BetRange range = new BetRange(BetRange.ALL_IN);

        assertThat(range.getType()).isEqualTo(BetRange.ALL_IN);
        assertThat(range.getMin()).isZero();
        assertThat(range.getMax()).isZero();
        assertThat(range.getPlayer()).isNull();
    }

    @Test
    void should_CreatePotSizeRange_When_PotSizeTypeAndMinMaxProvided() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.5f, 1.0f);

        assertThat(range.getType()).isEqualTo(BetRange.POT_SIZE);
        assertThat(range.getMin()).isEqualTo(0.5f);
        assertThat(range.getMax()).isEqualTo(1.0f);
        assertThat(range.getPlayer()).isNull();
    }

    @Test
    void should_CreateBigBlindRange_When_BigBlindTypeAndMinMaxProvided() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 2.0f, 3.0f);

        assertThat(range.getType()).isEqualTo(BetRange.BIG_BLIND);
        assertThat(range.getMin()).isEqualTo(2.0f);
        assertThat(range.getMax()).isEqualTo(3.0f);
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Test
    void should_ThrowError_When_MinGreaterThanMax() {
        assertThatThrownBy(() -> new BetRange(BetRange.POT_SIZE, 1.5f, 0.5f)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("min greater than max");
    }

    @Test
    void should_ThrowError_When_PotSizeWithZeroMinAndMax() {
        assertThatThrownBy(() -> new BetRange(BetRange.POT_SIZE, 0.0f, 0.0f)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("min/max both zero");
    }

    @Test
    void should_ThrowError_When_BigBlindWithZeroMinAndMax() {
        assertThatThrownBy(() -> new BetRange(BetRange.BIG_BLIND, 0.0f, 0.0f)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("min/max both zero");
    }

    @Test
    void should_AllowEqualMinAndMax_When_NonZeroValues() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.75f, 0.75f);

        assertThat(range.getMin()).isEqualTo(0.75f);
        assertThat(range.getMax()).isEqualTo(0.75f);
    }

    // ========================================
    // Accessor Tests
    // ========================================

    @Test
    void should_ReturnCorrectType_When_GetTypeCall() {
        BetRange potRange = new BetRange(BetRange.POT_SIZE, 0.5f, 1.0f);
        BetRange bbRange = new BetRange(BetRange.BIG_BLIND, 2.0f, 3.0f);
        BetRange allInRange = new BetRange(BetRange.ALL_IN);

        assertThat(potRange.getType()).isEqualTo(BetRange.POT_SIZE);
        assertThat(bbRange.getType()).isEqualTo(BetRange.BIG_BLIND);
        assertThat(allInRange.getType()).isEqualTo(BetRange.ALL_IN);
    }

    @Test
    void should_ReturnCorrectMinMax_When_AccessorsCall() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.25f, 0.75f);

        assertThat(range.getMin()).isEqualTo(0.25f);
        assertThat(range.getMax()).isEqualTo(0.75f);
    }

    // ========================================
    // Range Type Tests
    // ========================================

    @Test
    void should_AcceptZeroMin_When_MaxIsNonZero() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.0f, 1.0f);

        assertThat(range.getMin()).isZero();
        assertThat(range.getMax()).isEqualTo(1.0f);
    }

    @Test
    void should_AcceptLargeMultipliers_When_BigBlindRange() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 5.0f, 10.0f);

        assertThat(range.getMin()).isEqualTo(5.0f);
        assertThat(range.getMax()).isEqualTo(10.0f);
    }

    @Test
    void should_AcceptFractionalMultipliers_When_PotSizeRange() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.33f, 0.67f);

        assertThat(range.getMin()).isEqualTo(0.33f);
        assertThat(range.getMax()).isEqualTo(0.67f);
    }

    // ========================================
    // Edge Case Tests
    // ========================================

    @Test
    void should_AcceptVerySmallRange_When_ValidMinMax() {
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.01f, 0.02f);

        assertThat(range.getMin()).isEqualTo(0.01f);
        assertThat(range.getMax()).isEqualTo(0.02f);
    }

    @Test
    void should_AcceptVeryLargeRange_When_ValidMinMax() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 1.0f, 100.0f);

        assertThat(range.getMin()).isEqualTo(1.0f);
        assertThat(range.getMax()).isEqualTo(100.0f);
    }

    // ========================================
    // Stack-Relative Constructor Tests
    // ========================================

    @Test
    void should_CreateStackRange_When_PlayerAndMinMaxProvided() {
        PokerPlayer player = new PokerPlayer(1, "Test", true);
        player.setChipCount(500);

        BetRange range = new BetRange(player, 0.25f, 0.5f);

        assertThat(range.getType()).isEqualTo(BetRange.STACK_SIZE);
        assertThat(range.getPlayer()).isSameAs(player);
        assertThat(range.getMin()).isEqualTo(0.25f);
        assertThat(range.getMax()).isEqualTo(0.5f);
    }

    @Test
    void should_ThrowError_When_StackRangeWithNullPlayer() {
        assertThatThrownBy(() -> new BetRange(BetRange.STACK_SIZE, 0.5f, 1.0f)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("no player");
    }

    @Test
    void should_ThrowError_When_InvalidBetRangeType() {
        assertThatThrownBy(() -> new BetRange(99, 0.5f, 1.0f)).isInstanceOf(ApplicationError.class)
                .hasMessageContaining("Unrecognized BetRange type");
    }

    // ========================================
    // Bet Calculation Tests - All-In
    // ========================================

    @Test
    void should_ReturnPlayerChips_When_AllInRange() {
        BetRange range = new BetRange(BetRange.ALL_IN);

        int bet = range.chooseBetAmount(player1);

        assertThat(bet).isEqualTo(player1.getChipCount());
    }

    @Test
    void should_ReturnPlayerChips_When_GetMaxBetOnAllIn() {
        BetRange range = new BetRange(BetRange.ALL_IN);

        int bet = range.getMaxBet(player1);

        assertThat(bet).isEqualTo(player1.getChipCount());
    }

    @Test
    void should_ReturnPlayerChips_When_GetMinBetOnAllIn() {
        BetRange range = new BetRange(BetRange.ALL_IN);

        int bet = range.getMinBet(player1);

        assertThat(bet).isEqualTo(player1.getChipCount());
    }

    // ========================================
    // Bet Calculation Tests - Pot Size
    // ========================================

    @Test
    void should_CalculatePotSizeBet_When_PotSizeRange() {
        // Create pot by having player2 bet
        hand.bet(player2, 50, "test bet");

        BetRange range = new BetRange(BetRange.POT_SIZE, 0.5f, 0.5f);

        int bet = range.chooseBetAmount(player1);

        // Should be approximately 50% of pot
        assertThat(bet).isGreaterThan(0);
    }

    @Test
    void should_ReturnMinimumBet_When_PotSizeMinRequested() {
        // Create pot by having player2 bet
        hand.bet(player2, 50, "test bet");

        BetRange range = new BetRange(BetRange.POT_SIZE, 0.25f, 0.75f);

        int minBet = range.getMinBet(player1);
        int maxBet = range.getMaxBet(player1);

        assertThat(minBet).isLessThanOrEqualTo(maxBet);
    }

    // ========================================
    // Bet Calculation Tests - Big Blind
    // ========================================

    @Test
    void should_CalculateBigBlindBet_When_BigBlindRange() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 3.0f, 3.0f);

        int bet = range.chooseBetAmount(player1);

        // Should be approximately 3 * big blind (3 * 20 = 60)
        assertThat(bet).isGreaterThan(0);
        assertThat(bet).isLessThanOrEqualTo(100);
    }

    @Test
    void should_ScaleWithBigBlind_When_BigBlindRange() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 2.0f, 5.0f);

        int minBet = range.getMinBet(player1);
        int maxBet = range.getMaxBet(player1);

        assertThat(minBet).isGreaterThan(0);
        assertThat(maxBet).isGreaterThan(minBet);
    }

    // ========================================
    // Bet Calculation Tests - Stack Size
    // ========================================

    @Test
    void should_CalculateStackSizeBet_When_StackSizeRange() {
        PokerPlayer player = new PokerPlayer(3, "Player3", true);
        player.setChipCount(500);
        player.newHand('p');
        game.addPlayer(player);
        table.setPlayer(player, 2);

        BetRange range = new BetRange(player, 0.5f, 0.5f);

        int bet = range.chooseBetAmount(player);

        // Should be approximately 50% of player's stack
        assertThat(bet).isGreaterThan(0);
        assertThat(bet).isLessThanOrEqualTo(500);
    }

    @Test
    void should_ScaleWithPlayerStack_When_StackSizeRange() {
        PokerPlayer player = new PokerPlayer(3, "Player3", true);
        player.setChipCount(800);
        player.newHand('p');
        game.addPlayer(player);
        table.setPlayer(player, 2);

        BetRange range = new BetRange(player, 0.25f, 0.75f);

        int minBet = range.getMinBet(player);
        int maxBet = range.getMaxBet(player);

        assertThat(minBet).isGreaterThanOrEqualTo(0);
        assertThat(maxBet).isGreaterThan(minBet);
    }

    // ========================================
    // Random Bet Selection Tests
    // ========================================

    @Test
    void should_ReturnBetWithinRange_When_RandomBetChosen() {
        // Create pot by having player2 bet
        hand.bet(player2, 100, "test bet");

        BetRange range = new BetRange(BetRange.POT_SIZE, 0.25f, 0.75f);

        // Call multiple times to test randomness
        for (int i = 0; i < 10; i++) {
            int bet = range.chooseBetAmount(player1);

            int minExpected = range.getMinBet(player1);
            int maxExpected = range.getMaxBet(player1);

            assertThat(bet).isBetween(minExpected, maxExpected);
        }
    }

    // ========================================
    // toString() Formatting Tests
    // ========================================

    @Test
    void should_FormatAllIn_When_BetEqualsPlayerStack() {
        // Set player with low chips so all-in bet is chosen
        player1.setChipCount(50);
        BetRange range = new BetRange(BetRange.ALL_IN);

        String result = range.toString(player1, false);

        assertThat(result).contains("All In");
        assertThat(result).contains("chips");
    }

    @Test
    void should_FormatPotSizeFixed_When_MinEqualsMax() {
        hand.bet(player2, 100, "test bet");
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.5f, 0.5f);

        String result = range.toString(player1, false);

        assertThat(result).contains("50%");
        assertThat(result).contains("pot size");
        assertThat(result).contains(", or $");
    }

    @Test
    void should_FormatPotSizeRange_When_MinNotEqualsMax() {
        hand.bet(player2, 100, "test bet");
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.25f, 0.75f);

        String result = range.toString(player1, false);

        assertThat(result).contains("25%-75% pot size");
        assertThat(result).contains(", or $");
    }

    @Test
    void should_FormatWithBreak_When_BreakParameterTrue() {
        hand.bet(player2, 100, "test bet");
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.5f, 0.5f);

        String result = range.toString(player1, true);

        assertThat(result).contains("<br>");
        assertThat(result).doesNotContain(", or ");
    }

    @Test
    void should_FormatBigBlindFixed_When_MinEqualsMax() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 3.0f, 3.0f);

        String result = range.toString(player1, false);

        assertThat(result).contains("3.0x BB");
        assertThat(result).contains(", or $");
    }

    @Test
    void should_FormatBigBlindRange_When_MinNotEqualsMax() {
        BetRange range = new BetRange(BetRange.BIG_BLIND, 2.0f, 5.0f);

        String result = range.toString(player1, false);

        assertThat(result).contains("2.0-5.0x BB");
        assertThat(result).contains(", or $");
    }

    @Test
    void should_FormatStackSizeSamePlayer_When_PlayerMatches() {
        BetRange range = new BetRange(player1, 0.5f, 0.5f);

        String result = range.toString(player1, false);

        assertThat(result).contains("50%");
        assertThat(result).contains("stack");
        assertThat(result).doesNotContain(player1.getName());
    }

    @Test
    void should_FormatStackSizeDifferentPlayer_When_PlayerDoesNotMatch() {
        PokerPlayer player3 = new PokerPlayer(3, "Player3", true);
        player3.setChipCount(800);
        player3.newHand('p');
        game.addPlayer(player3);
        table.setPlayer(player3, 2);

        BetRange range = new BetRange(player3, 0.25f, 0.75f);

        String result = range.toString(player1, false);

        assertThat(result).contains("Player3's stack");
        assertThat(result).contains("25%-75%");
    }

    @Test
    void should_ShowSingleAmount_When_MinBetEqualsMaxBet() {
        // Create a small pot to ensure min and max are same after rounding
        hand.bet(player2, 20, "test bet");
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.5f, 0.5f);

        String result = range.toString(player1, false);

        // Should show single dollar amount, not a range
        assertThat(result).contains("$");
        // Should not have two dollar signs (which would indicate a range)
        int dollarCount = result.length() - result.replace("$", "").length();
        assertThat(dollarCount).isEqualTo(1);
    }

    @Test
    void should_ShowAllInNotation_When_MaxBetEqualsAllIn() {
        // Set player with enough chips so min bet won't be all-in
        player1.setChipCount(500);
        // Small pot so min bet is small
        hand.bet(player2, 50, "small bet");
        // Large range so max bet will exceed player's stack
        BetRange range = new BetRange(BetRange.POT_SIZE, 0.1f, 10.0f);

        String result = range.toString(player1, false);

        // Max bet should equal all-in, so should show "(All In)" notation
        assertThat(result).contains("(All In)");
    }
}
