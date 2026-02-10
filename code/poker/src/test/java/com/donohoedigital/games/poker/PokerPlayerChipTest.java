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
 * Tests for PokerPlayer chip management methods.
 * Critical money-handling operations that must be 100% accurate.
 * Extends IntegrationTestBase for game infrastructure.
 */
@Tag("integration")
class PokerPlayerChipTest extends IntegrationTestBase
{
    private PokerGame game;
    private PokerTable table;
    private PokerPlayer player;

    @BeforeEach
    void setUp()
    {
        // Create minimal game infrastructure
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        table = new PokerTable(game, 1);

        // Create test player
        player = new PokerPlayer(1, "TestPlayer", true);
        player.setChipCount(1000);
        game.addPlayer(player);
        table.setPlayer(player, 0);
    }

    // ========================================
    // addChips() Tests - Direct Chip Addition
    // ========================================

    @Test
    void should_AddChips_When_PositiveAmount()
    {
        int initialChips = player.getChipCount();

        player.addChips(500);

        assertThat(player.getChipCount()).isEqualTo(initialChips + 500);
    }

    @Test
    void should_AddZeroChips_When_ZeroAmount()
    {
        int initialChips = player.getChipCount();

        player.addChips(0);

        assertThat(player.getChipCount()).isEqualTo(initialChips);
    }

    @Test
    void should_HandleNegativeChips_When_NegativeAmount()
    {
        int initialChips = player.getChipCount();

        // Note: addChips allows negative (it's just addition)
        // This is used internally for chip removal
        player.addChips(-200);

        assertThat(player.getChipCount()).isEqualTo(initialChips - 200);
    }

    // ========================================
    // Rebuy Tests - Pending and Immediate
    // ========================================

    @Test
    void should_TrackPendingRebuy_When_PendingTrue()
    {
        player.addRebuy(1500, 1500, true);

        // Pending rebuys tracked but not applied to chip count yet
        assertThat(player.getChipCount()).isEqualTo(1000); // unchanged
        assertThat(player.getNumRebuys()).isEqualTo(0); // not finalized yet
    }

    @Test
    void should_ApplyImmediateRebuy_When_PendingFalse()
    {
        player.addRebuy(1500, 1500, false);

        // Immediate rebuy applied to chip count
        assertThat(player.getChipCount()).isEqualTo(2500); // 1000 + 1500
        assertThat(player.getNumRebuys()).isEqualTo(1);
    }

    @Test
    void should_FinalizePendingRebuys_When_AddPendingRebuysCall()
    {
        // Add multiple pending rebuys
        player.addRebuy(1500, 1500, true);
        player.addRebuy(1500, 1500, true);

        // Finalize all pending
        player.addPendingRebuys();

        // Chips should now include both rebuys
        assertThat(player.getChipCount()).isEqualTo(4000); // 1000 + 1500 + 1500
        assertThat(player.getNumRebuys()).isEqualTo(2);
    }

    @Test
    void should_ResetPendingCounters_When_AddPendingRebuysCompletes()
    {
        player.addRebuy(1500, 1500, true);
        player.addPendingRebuys();

        // Add another pending - should start fresh
        player.addRebuy(1500, 1500, true);

        // Should only have 1 pending now (previous was finalized)
        assertThat(player.getChipCount()).isEqualTo(2500); // Only first finalized
    }

    // ========================================
    // Chip Count Invariant Tests
    // ========================================

    @Test
    void should_MaintainChipAccuracy_When_MultipleOperations()
    {
        // Start with 1000
        assertThat(player.getChipCount()).isEqualTo(1000);

        // Add chips
        player.addChips(500);
        assertThat(player.getChipCount()).isEqualTo(1500);

        // Immediate rebuy
        player.addRebuy(1500, 1500, false);
        assertThat(player.getChipCount()).isEqualTo(3000);

        // Verify final state
        assertThat(player.getNumRebuys()).isEqualTo(1);
    }

    @Test
    void should_NotCreateChips_When_NoOperations()
    {
        int initialChips = player.getChipCount();

        // No operations - chips should be unchanged
        assertThat(player.getChipCount()).isEqualTo(initialChips);
    }
}
