/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026 Doug Donohoe
 * Copyright (c) 2026 the DD Poker community
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
package com.donohoedigital.games.engine;

import com.donohoedigital.comms.DMArrayList;
import com.donohoedigital.games.comms.GameInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PlayersDieRoll6x2 - rolls dice for all players in a game to
 * determine a winner.
 */
class PlayersDieRoll6x2Test {

    // ========== Constructor and Basic Behavior Tests ==========

    @Test
    void should_DetermineWinner_When_ConstructedWithTwoPlayers() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(2);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        assertThat(roll.getWinningPlayerID()).isBetween(0, 1);
    }

    @Test
    void should_DetermineWinner_When_ConstructedWithThreePlayers() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(3);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        assertThat(roll.getWinningPlayerID()).isBetween(0, 2);
    }

    // ========== getDieRolls() Tests ==========

    @Test
    void should_HaveEntryForEachPlayer_When_GetDieRollsCalled() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(3);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        assertThat(roll.getDieRolls()).hasSize(3);
    }

    @Test
    void should_HaveAtLeastOneRollPerPlayer_When_NoPlayersEliminated() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(2);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        DMArrayList[] dieRolls = roll.getDieRolls();
        for (DMArrayList playerRolls : dieRolls) {
            assertThat(playerRolls).isNotEmpty();
        }
    }

    @Test
    void should_ContainDieRoll6x2Instances_When_GetDieRollsCalled() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(2);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        DMArrayList[] dieRolls = roll.getDieRolls();
        for (DMArrayList playerRolls : dieRolls) {
            for (Object obj : playerRolls) {
                assertThat(obj).isInstanceOf(DieRoll6x2.class);
            }
        }
    }

    // ========== Eliminated Players Tests ==========

    @Test
    void should_SkipEliminatedPlayers_When_SomePlayersEliminated() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(3);
        when(game.isEliminated(0)).thenReturn(false);
        when(game.isEliminated(1)).thenReturn(true);
        when(game.isEliminated(2)).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        // Winner should not be the eliminated player
        assertThat(roll.getWinningPlayerID()).isIn(0, 2);
    }

    @Test
    void should_HaveEmptyRollsForEliminatedPlayer_When_PlayerIsEliminated() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(3);
        when(game.isEliminated(0)).thenReturn(false);
        when(game.isEliminated(1)).thenReturn(true);
        when(game.isEliminated(2)).thenReturn(false);

        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2(game);

        DMArrayList[] dieRolls = roll.getDieRolls();
        assertThat(dieRolls[1]).isEmpty();
    }

    // ========== Empty Constructor Tests ==========

    @Test
    void should_CreateInstance_When_EmptyConstructorUsed() {
        PlayersDieRoll6x2 roll = new PlayersDieRoll6x2();

        assertThat(roll).isNotNull();
        assertThat(roll.getDieRolls()).isNull();
    }

    // ========== Marshal/Demarshal Roundtrip Tests ==========

    @Test
    void should_PreserveWinnerAndRolls_When_MarshalThenDemarshal() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(2);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 original = new PlayersDieRoll6x2(game);
        String marshalled = original.marshal(null);

        PlayersDieRoll6x2 restored = new PlayersDieRoll6x2();
        restored.demarshal(null, marshalled);

        assertThat(restored.getWinningPlayerID()).isEqualTo(original.getWinningPlayerID());
        assertThat(restored.getDieRolls()).hasSameSizeAs(original.getDieRolls());

        for (int i = 0; i < original.getDieRolls().length; i++) {
            assertThat(restored.getDieRolls()[i]).hasSameSizeAs(original.getDieRolls()[i]);
        }
    }

    @Test
    void should_PreserveDieValues_When_MarshalThenDemarshal() {
        GameInfo game = mock(GameInfo.class);
        when(game.getNumPlayers()).thenReturn(3);
        when(game.isEliminated(anyInt())).thenReturn(false);

        PlayersDieRoll6x2 original = new PlayersDieRoll6x2(game);
        String marshalled = original.marshal(null);

        PlayersDieRoll6x2 restored = new PlayersDieRoll6x2();
        restored.demarshal(null, marshalled);

        DMArrayList[] origRolls = original.getDieRolls();
        DMArrayList[] restoredRolls = restored.getDieRolls();

        for (int i = 0; i < origRolls.length; i++) {
            for (int j = 0; j < origRolls[i].size(); j++) {
                DieRoll6x2 origDie = (DieRoll6x2) origRolls[i].get(j);
                DieRoll6x2 restoredDie = (DieRoll6x2) restoredRolls[i].get(j);
                assertThat(restoredDie.getFirst()).isEqualTo(origDie.getFirst());
                assertThat(restoredDie.getSecond()).isEqualTo(origDie.getSecond());
                assertThat(restoredDie.getSum()).isEqualTo(origDie.getSum());
            }
        }
    }
}
