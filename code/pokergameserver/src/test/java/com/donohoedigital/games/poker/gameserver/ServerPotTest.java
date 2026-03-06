/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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
package com.donohoedigital.games.poker.gameserver;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for ServerPot — verifies poker pot semantics.
 */
class ServerPotTest {

    @Test
    void should_TrackEligibility_When_PlayerContributesChips() {
        // Poker rule: You can only win pots you've contributed to
        ServerPot pot = new ServerPot(0, 0);
        ServerPlayer alice = new ServerPlayer(1, "Alice", true, 0, 5000);
        ServerPlayer bob = new ServerPlayer(2, "Bob", true, 0, 5000);
        ServerPlayer charlie = new ServerPlayer(3, "Charlie", true, 0, 5000);

        pot.addChips(alice, 200);
        pot.addChips(bob, 200);
        // Charlie didn't contribute

        assertThat(pot.isPlayerEligible(alice)).isTrue();
        assertThat(pot.isPlayerEligible(bob)).isTrue();
        assertThat(pot.isPlayerEligible(charlie)).isFalse();
    }

    @Test
    void should_DetectUncalledBet_When_OnlyOnePlayerContributed() {
        // Poker rule: If everyone folds to a bet, the uncalled portion is returned
        ServerPot pot = new ServerPot(0, 0);
        ServerPlayer bettor = new ServerPlayer(1, "Bettor", true, 0, 5000);

        pot.addChips(bettor, 500);

        assertThat(pot.isOverbet()).isTrue();
    }

    @Test
    void should_NotBeOverbet_When_MultiplePlayersContested() {
        // Poker rule: A pot with 2+ contributors is contested and goes to showdown
        ServerPot pot = new ServerPot(0, 0);
        ServerPlayer p1 = new ServerPlayer(1, "P1", true, 0, 5000);
        ServerPlayer p2 = new ServerPlayer(2, "P2", true, 0, 5000);

        pot.addChips(p1, 500);
        pot.addChips(p2, 500);

        assertThat(pot.isOverbet()).isFalse();
    }

    @Test
    void should_AccumulateChipsFromMultipleBettingRounds() {
        // Poker rule: Pot grows as betting rounds progress
        ServerPot pot = new ServerPot(0, 0);
        ServerPlayer p1 = new ServerPlayer(1, "P1", true, 0, 5000);
        ServerPlayer p2 = new ServerPlayer(2, "P2", true, 0, 5000);

        pot.addChips(p1, 100);
        pot.addChips(p2, 100);
        pot.addChips(p1, 200);
        pot.addChips(p2, 200);

        assertThat(pot.getChips()).isEqualTo(600);
        assertThat(pot.getEligiblePlayers()).hasSize(2);
    }

    @Test
    void should_TrackSidePotAllInLevel() {
        // Poker rule: Side pots created when player goes all-in for less than current
        // bet
        ServerPot mainPot = new ServerPot(0, 0);
        ServerPot sidePot = new ServerPot(0, 500);

        assertThat(mainPot.getSideBet()).isEqualTo(0);
        assertThat(sidePot.getSideBet()).isEqualTo(500);
    }

    @Test
    void should_TrackMultipleWinners_ForSplitPot() {
        // Poker rule: Tied hands split the pot equally
        ServerPot pot = new ServerPot(0, 0);
        ServerPlayer w1 = new ServerPlayer(1, "Winner1", true, 0, 5000);
        ServerPlayer w2 = new ServerPlayer(2, "Winner2", true, 0, 5000);

        pot.addWinner(w1);
        pot.addWinner(w2);

        assertThat(pot.getWinners()).hasSize(2);
        assertThat(pot.getWinners()).containsExactly(w1, w2);
    }

    @Test
    void should_ResetForNewHand() {
        // Between hands, pots are cleared for the next deal
        ServerPot pot = new ServerPot(0, 0);
        ServerPlayer p1 = new ServerPlayer(1, "P1", true, 0, 5000);
        pot.addChips(p1, 500);
        pot.addWinner(p1);

        pot.reset();

        assertThat(pot.getChips()).isEqualTo(0);
        assertThat(pot.getEligiblePlayers()).isEmpty();
        assertThat(pot.getWinners()).isEmpty();
    }
}
