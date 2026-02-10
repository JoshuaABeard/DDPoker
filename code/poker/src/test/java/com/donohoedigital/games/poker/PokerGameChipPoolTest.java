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

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PokerGame chip pool tracking methods.
 * Critical money-tracking operations - total chips must equal sum of all player chips.
 * Extends IntegrationTestBase for game infrastructure.
 */
@Tag("integration")
class PokerGameChipPoolTest extends IntegrationTestBase
{
    private PokerGame game;
    private TournamentProfile profile;
    private PokerPlayer[] players;

    @BeforeEach
    void setUp()
    {
        // Create game with tournament profile
        game = new PokerGame(null);
        profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        setProfileInteger("rebuychips", 1500);
        setProfileInteger("addonchips", 2000);
        game.setProfile(profile);

        // Create 3 players
        players = new PokerPlayer[3];
        for (int i = 0; i < 3; i++)
        {
            players[i] = new PokerPlayer(i + 1, "Player" + i, true);
            players[i].setChipCount(1500); // Starting chips
            game.addPlayer(players[i]);
        }
    }

    // ========================================
    // computeTotalChipsInPlay() Tests
    // ========================================

    @Test
    void should_CalculateTotalChips_When_OnlyBuyins()
    {
        game.computeTotalChipsInPlay();

        // 3 players * 1500 buyin = 4500 total
        assertThat(game.getTotalChipsInPlay()).isEqualTo(4500);
    }

    @Test
    void should_IncludeRebuys_When_PlayersRebuy()
    {
        // Player 0 rebuys once (1500 chips)
        setNumRebuys(players[0], 1);

        game.computeTotalChipsInPlay();

        // 3 buyins (4500) + 1 rebuy (1500) = 6000
        assertThat(game.getTotalChipsInPlay()).isEqualTo(6000);
    }

    @Test
    void should_IncludeAddons_When_PlayersTakeAddon()
    {
        // Player 1 takes addon (2000 chips)
        setAddon(players[1], 1);

        game.computeTotalChipsInPlay();

        // 3 buyins (4500) + 1 addon (2000) = 6500
        assertThat(game.getTotalChipsInPlay()).isEqualTo(6500);
    }

    @Test
    void should_CalculateCorrectly_When_MultipleRebuysAndAddons()
    {
        // Player 0: 2 rebuys
        setNumRebuys(players[0], 2);
        // Player 1: 1 rebuy + addon
        setNumRebuys(players[1], 1);
        setAddon(players[1], 1);
        // Player 2: just buyin

        game.computeTotalChipsInPlay();

        // 3 buyins (4500) + 3 rebuys (4500) + 1 addon (2000) = 11000
        assertThat(game.getTotalChipsInPlay()).isEqualTo(11000);
    }

    // ========================================
    // chipsBought() Tests
    // ========================================

    @Test
    void should_IncrementTotal_When_ChipsBought()
    {
        game.computeTotalChipsInPlay();
        int initial = game.getTotalChipsInPlay();

        // Buy 1500 more chips (rebuy)
        game.chipsBought(1500);

        assertThat(game.getTotalChipsInPlay()).isEqualTo(initial + 1500);
    }

    @Test
    void should_AllowMultiplePurchases_When_CalledRepeatedly()
    {
        game.computeTotalChipsInPlay();
        int initial = game.getTotalChipsInPlay();

        // Multiple chip purchases
        game.chipsBought(1500); // Rebuy
        game.chipsBought(2000); // Addon
        game.chipsBought(1500); // Another rebuy

        // Should have added 5000 total
        assertThat(game.getTotalChipsInPlay()).isEqualTo(initial + 5000);
    }

    // ========================================
    // getTotalChipsInPlay() Tests
    // ========================================

    @Test
    void should_ReturnZero_When_NotYetComputed()
    {
        // Before computeTotalChipsInPlay() is called, should be 0
        assertThat(game.getTotalChipsInPlay()).isEqualTo(0);
    }

    @Test
    void should_ReturnCorrectValue_When_AfterCompute()
    {
        game.computeTotalChipsInPlay();

        // Should reflect computed value
        assertThat(game.getTotalChipsInPlay()).isGreaterThan(0);
        assertThat(game.getTotalChipsInPlay()).isEqualTo(4500); // 3 * 1500
    }

    // ========================================
    // Chip Pool Invariant Tests
    // ========================================

    @Test
    void should_MaintainAccuracy_When_CombiningComputeAndBought()
    {
        // Start with computed total
        game.computeTotalChipsInPlay();
        assertThat(game.getTotalChipsInPlay()).isEqualTo(4500);

        // Add a rebuy incrementally
        game.chipsBought(1500);
        assertThat(game.getTotalChipsInPlay()).isEqualTo(6000);

        // Recompute (should still be accurate if players updated)
        setNumRebuys(players[0], 1);
        game.computeTotalChipsInPlay();
        assertThat(game.getTotalChipsInPlay()).isEqualTo(6000);
    }

    @Test
    void should_NotCreateChips_When_NoOperations()
    {
        int initial = game.getTotalChipsInPlay();

        // No operations - should remain unchanged
        assertThat(game.getTotalChipsInPlay()).isEqualTo(initial);
        assertThat(game.getTotalChipsInPlay()).isEqualTo(0); // Not computed yet
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Set the number of rebuys for a player using reflection.
     * The nNumRebuy_ field is private and has no public setter.
     */
    private void setNumRebuys(PokerPlayer player, int count)
    {
        try
        {
            Field field = PokerPlayer.class.getDeclaredField("nNumRebuy_");
            field.setAccessible(true);
            field.set(player, count);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to set rebuy count", e);
        }
    }

    /**
     * Set the addon flag for a player using reflection.
     * The nAddon_ field is private and has no public setter.
     */
    private void setAddon(PokerPlayer player, int value)
    {
        try
        {
            Field field = PokerPlayer.class.getDeclaredField("nAddon_");
            field.setAccessible(true);
            field.set(player, value);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to set addon", e);
        }
    }

    /**
     * Set a profile integer value using reflection.
     * TournamentProfile stores values in a private DMTypedHashMap.
     */
    private void setProfileInteger(String key, int value)
    {
        try
        {
            Field mapField = TournamentProfile.class.getDeclaredField("map_");
            mapField.setAccessible(true);
            Object map = mapField.get(profile);

            // DMTypedHashMap has setInteger method
            map.getClass().getMethod("setInteger", String.class, Integer.class)
               .invoke(map, key, value);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to set profile integer", e);
        }
    }
}
