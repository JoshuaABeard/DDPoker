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
package com.donohoedigital.games.poker.ai;

import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for V1Player static utility methods.
 * Uses real PokerPlayer instances (Mockito can't mock it).
 */
class V1PlayerStaticTest
{
    private PokerPlayer player;
    private TournamentProfile profile;

    @BeforeEach
    void setUp()
    {
        player = new PokerPlayer(1, "TestPlayer", false);
        profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
    }

    /**
     * Helper to set rebuy count via reflection (no public setter exists)
     */
    private void setNumRebuys(int count)
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
    // ========================================
    // WantsRebuy Tests
    // ========================================

    @Test
    void should_AlwaysRebuy_When_PropensityIs0to25()
    {
        setNumRebuys(0);

        boolean result = V1Player.WantsRebuy(player, 25);

        assertThat(result).isTrue();
    }

    @Test
    void should_AlwaysRebuy_When_PropensityIs0()
    {
        setNumRebuys(3);

        boolean result = V1Player.WantsRebuy(player, 0);

        assertThat(result).isTrue();
    }

    @Test
    void should_RebuyUpTo3Times_When_PropensityIs26to50()
    {
        // Should rebuy when rebuys < 3
        setNumRebuys(0);
        assertThat(V1Player.WantsRebuy(player, 40)).isTrue();

        setNumRebuys(2);
        assertThat(V1Player.WantsRebuy(player, 50)).isTrue();

        // Should not rebuy when rebuys >= 3
        setNumRebuys(3);
        assertThat(V1Player.WantsRebuy(player, 40)).isFalse();

        setNumRebuys(4);
        assertThat(V1Player.WantsRebuy(player, 26)).isFalse();
    }

    @Test
    void should_RebuyUpTo2Times_When_PropensityIs51to75()
    {
        // Should rebuy when rebuys < 2
        setNumRebuys(0);
        assertThat(V1Player.WantsRebuy(player, 60)).isTrue();

        setNumRebuys(1);
        assertThat(V1Player.WantsRebuy(player, 75)).isTrue();

        // Should not rebuy when rebuys >= 2
        setNumRebuys(2);
        assertThat(V1Player.WantsRebuy(player, 51)).isFalse();

        setNumRebuys(3);
        assertThat(V1Player.WantsRebuy(player, 70)).isFalse();
    }

    @Test
    void should_RebuyOnce_When_PropensityIs76to90()
    {
        // Should rebuy when rebuys < 1
        setNumRebuys(0);
        assertThat(V1Player.WantsRebuy(player, 85)).isTrue();

        // Should not rebuy when rebuys >= 1
        setNumRebuys(1);
        assertThat(V1Player.WantsRebuy(player, 76)).isFalse();

        setNumRebuys(2);
        assertThat(V1Player.WantsRebuy(player, 90)).isFalse();
    }

    @Test
    void should_NeverRebuy_When_PropensityIs91Plus()
    {
        setNumRebuys(0);
        assertThat(V1Player.WantsRebuy(player, 91)).isFalse();

        setNumRebuys(0);
        assertThat(V1Player.WantsRebuy(player, 100)).isFalse();
    }

    @Test
    void should_NeverRebuy_When_AlreadyRebuilt5Times()
    {
        // Even with propensity 0 (always rebuy), should refuse after 5 rebuys
        setNumRebuys(5);
        assertThat(V1Player.WantsRebuy(player, 0)).isFalse();

        setNumRebuys(10);
        assertThat(V1Player.WantsRebuy(player, 25)).isFalse();
    }

    // ========================================
    // WantsAddon Tests
    // ========================================

    @Test
    void should_AlwaysAddon_When_PropensityLessThan25()
    {
        player.setChipCount(5000); // Any chip count

        boolean result = V1Player.WantsAddon(player, 24, profile);

        assertThat(result).isTrue();
    }

    @Test
    void should_AlwaysAddon_When_PropensityIs0()
    {
        player.setChipCount(10000);

        boolean result = V1Player.WantsAddon(player, 0, profile);

        assertThat(result).isTrue();
    }

    @Test
    void should_AddonIfUnder3xBuyin_When_PropensityIs25to49()
    {
        // Should addon when chips < 3x buyin (4500)
        player.setChipCount(4499);
        assertThat(V1Player.WantsAddon(player, 40, profile)).isTrue();

        player.setChipCount(3000);
        assertThat(V1Player.WantsAddon(player, 49, profile)).isTrue();

        // Should not addon when chips >= 3x buyin
        player.setChipCount(4500);
        assertThat(V1Player.WantsAddon(player, 25, profile)).isFalse();

        player.setChipCount(5000);
        assertThat(V1Player.WantsAddon(player, 40, profile)).isFalse();
    }

    @Test
    void should_AddonIfUnder2xBuyin_When_PropensityIs50to74()
    {
        // Should addon when chips < 2x buyin (3000)
        player.setChipCount(2999);
        assertThat(V1Player.WantsAddon(player, 60, profile)).isTrue();

        player.setChipCount(1500);
        assertThat(V1Player.WantsAddon(player, 74, profile)).isTrue();

        // Should not addon when chips >= 2x buyin
        player.setChipCount(3000);
        assertThat(V1Player.WantsAddon(player, 50, profile)).isFalse();

        player.setChipCount(4000);
        assertThat(V1Player.WantsAddon(player, 70, profile)).isFalse();
    }

    @Test
    void should_NeverAddon_When_PropensityIs75Plus()
    {
        // Never addon regardless of chip count
        player.setChipCount(100);
        assertThat(V1Player.WantsAddon(player, 75, profile)).isFalse();

        player.setChipCount(5000);
        assertThat(V1Player.WantsAddon(player, 100, profile)).isFalse();
    }

    // ========================================
    // getRaise Tests
    // ========================================

    @Test
    void should_Return3xOr4xBigBlind_When_Called()
    {
        int bigBlind = 20;

        // Call multiple times to test probabilistic behavior
        // 75% should be 3x, 25% should be 4x
        int count3x = 0;
        int count4x = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++)
        {
            int raise = V1Player.getRaise(bigBlind);

            if (raise == 60) count3x++; // 3 * 20
            else if (raise == 80) count4x++; // 4 * 20
            else fail("getRaise returned unexpected value: " + raise);
        }

        // Both should occur at least once
        assertThat(count3x).isGreaterThan(0);
        assertThat(count4x).isGreaterThan(0);

        // Total should be 100
        assertThat(count3x + count4x).isEqualTo(iterations);

        // 3x should be more common (roughly 3:1 ratio, but allow variance)
        assertThat(count3x).isGreaterThan(count4x);
    }

    @Test
    void should_ScaleWithBigBlind_When_DifferentBigBlinds()
    {
        int raise1 = V1Player.getRaise(10);
        int raise2 = V1Player.getRaise(50);
        int raise3 = V1Player.getRaise(100);

        // All raises should be either 3x or 4x the big blind
        assertThat(raise1).isIn(30, 40);
        assertThat(raise2).isIn(150, 200);
        assertThat(raise3).isIn(300, 400);
    }
}
