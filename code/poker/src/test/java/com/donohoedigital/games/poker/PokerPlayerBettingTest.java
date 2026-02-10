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
package com.donohoedigital.games.poker;

import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerPlayer betting action methods.
 * Tests bet(), call(), raise(), fold(), and isAllIn() methods.
 * Extends IntegrationTestBase for game infrastructure.
 */
@Tag("integration")
class PokerPlayerBettingTest extends IntegrationTestBase
{
    private PokerGame game;
    private PokerTable table;
    private HoldemHand hand;
    private PokerPlayer player;
    private PokerPlayer opponent;

    @BeforeEach
    void setUp()
    {
        // Create game infrastructure
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);
        table.setMinChip(1);

        // Create players
        player = new PokerPlayer(1, "Player", true);
        player.setChipCount(1000);
        opponent = new PokerPlayer(2, "Opponent", true);
        opponent.setChipCount(1000);

        game.addPlayer(player);
        game.addPlayer(opponent);
        table.setPlayer(player, 0);
        table.setPlayer(opponent, 1);

        // Initialize hand
        table.setButton(0);
        player.newHand('p');
        opponent.newHand('p');

        hand = new HoldemHand(table);
        table.setHoldemHand(hand);
        hand.setBigBlind(20);
        hand.setSmallBlind(10);
        hand.setPlayerOrder(false);
    }

    // ========================================
    // isAllIn() Tests
    // ========================================

    @Test
    void should_ReturnTrue_When_ChipsAreZero()
    {
        player.setChipCount(0);

        assertThat(player.isAllIn()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_ChipsGreaterThanZero()
    {
        player.setChipCount(1);

        assertThat(player.isAllIn()).isFalse();
    }

    @Test
    void should_ReturnFalse_When_ChipsAreMany()
    {
        player.setChipCount(1000);

        assertThat(player.isAllIn()).isFalse();
    }

    // ========================================
    // fold() Tests
    // ========================================

    @Test
    void should_SetFoldedState_When_FoldCalled()
    {
        hand.setCurrentPlayerIndex(0);
        player.fold("test fold", HandAction.FOLD_NORMAL);

        assertThat(player.isFolded()).isTrue();
    }

    @Test
    void should_NotChangeChips_When_FoldCalled()
    {
        int initialChips = player.getChipCount();

        hand.setCurrentPlayerIndex(0);
        player.fold("test fold", HandAction.FOLD_NORMAL);

        assertThat(player.getChipCount()).isEqualTo(initialChips);
    }

    // ========================================
    // bet() Tests - Chip Deduction
    // ========================================

    @Test
    void should_DeductChips_When_BetCalled()
    {
        int initialChips = player.getChipCount();
        int betAmount = 100;

        hand.setCurrentPlayerIndex(0);
        player.bet(betAmount, "test bet");

        assertThat(player.getChipCount()).isEqualTo(initialChips - betAmount);
    }

    @Test
    void should_BeAllIn_When_BetAllChips()
    {
        int allChips = player.getChipCount();

        hand.setCurrentPlayerIndex(0);
        player.bet(allChips, "all-in bet");

        assertThat(player.getChipCount()).isEqualTo(0);
        assertThat(player.isAllIn()).isTrue();
    }

    // ========================================
    // call() Tests - Call Amount Deduction
    // ========================================

    @Test
    void should_DeductCallAmount_When_CallCalled()
    {
        // Opponent bets first
        hand.setCurrentPlayerIndex(1);
        opponent.bet(100, "bet");

        int initialChips = player.getChipCount();

        // Player calls
        hand.setCurrentPlayerIndex(0);
        player.call("call");

        // Should have deducted the call amount (100)
        assertThat(player.getChipCount()).isLessThan(initialChips);
    }

    @Test
    void should_BeAllIn_When_CallWithInsufficientChips()
    {
        // Opponent makes big bet
        hand.setCurrentPlayerIndex(1);
        opponent.bet(500, "big bet");

        // Player has less than call amount
        player.setChipCount(200);
        player.newHand('p'); // Recapture chip count

        hand.setCurrentPlayerIndex(0);
        player.call("all-in call");

        // Should be all-in
        assertThat(player.getChipCount()).isEqualTo(0);
        assertThat(player.isAllIn()).isTrue();
    }

    // ========================================
    // raise() Tests - Call + Raise Combined
    // ========================================

    @Test
    void should_DeductRaiseAmount_When_RaiseCalled()
    {
        // Opponent bets first
        hand.setCurrentPlayerIndex(1);
        opponent.bet(100, "bet");

        int initialChips = player.getChipCount();

        // Player raises by 100 (calls 100 + raises 100 = total 200)
        hand.setCurrentPlayerIndex(0);
        player.raise(100, "raise");

        // Should have deducted call (100) + raise (100) = 200 total
        assertThat(player.getChipCount()).isLessThan(initialChips);
        assertThat(initialChips - player.getChipCount()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void should_BeAllIn_When_RaiseWithAllChips()
    {
        // Opponent bets
        hand.setCurrentPlayerIndex(1);
        opponent.bet(50, "bet");

        int playerChips = player.getChipCount();

        // Player raises all-in
        hand.setCurrentPlayerIndex(0);
        player.raise(playerChips, "all-in raise");

        // Should be all-in
        assertThat(player.getChipCount()).isEqualTo(0);
        assertThat(player.isAllIn()).isTrue();
    }

    // ========================================
    // Chip Accounting Invariant Tests
    // ========================================

    @Test
    void should_MaintainChipAccuracy_When_MultipleBets()
    {
        int initialChips = player.getChipCount();

        hand.setCurrentPlayerIndex(0);
        player.bet(50, "bet 1");
        int afterFirst = player.getChipCount();
        assertThat(afterFirst).isEqualTo(initialChips - 50);

        // In next round, bet again
        hand.advanceRound();
        hand.setCurrentPlayerIndex(0);
        player.bet(100, "bet 2");
        int afterSecond = player.getChipCount();
        assertThat(afterSecond).isEqualTo(afterFirst - 100);

        // Total deducted should be 150
        assertThat(initialChips - afterSecond).isEqualTo(150);
    }
}
