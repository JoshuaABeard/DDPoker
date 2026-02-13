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

import com.donohoedigital.games.poker.PokerPlayer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ColorUpLogic - color-up decision logic extracted from
 * ColorUpFinish.java. Tests run in headless mode with no UI dependencies. Part
 * of Wave 2 testability refactoring.
 */
@Tag("unit")
class ColorUpLogicTest {

    // =================================================================
    // shouldPauseColorUp() Tests
    // =================================================================

    @Test
    void should_Pause_When_PracticeWithOptionEnabled() {
        boolean shouldPause = ColorUpLogic.shouldPauseColorUp(false, false, true);

        assertThat(shouldPause).isTrue();
    }

    @Test
    void should_NotPause_When_PracticeButOptionDisabled() {
        boolean shouldPause = ColorUpLogic.shouldPauseColorUp(false, false, false);

        assertThat(shouldPause).isFalse();
    }

    @Test
    void should_NotPause_When_Autopilot() {
        boolean shouldPause = ColorUpLogic.shouldPauseColorUp(true, false, true);

        assertThat(shouldPause).isFalse();
    }

    @Test
    void should_NotPause_When_OnlineGame() {
        boolean shouldPause = ColorUpLogic.shouldPauseColorUp(false, true, true);

        assertThat(shouldPause).isFalse();
    }

    @Test
    void should_NotPause_When_OnlineGameAndAutopilot() {
        boolean shouldPause = ColorUpLogic.shouldPauseColorUp(true, true, true);

        assertThat(shouldPause).isFalse();
    }

    @Test
    void should_NotPause_When_OnlineGameButOptionEnabled() {
        boolean shouldPause = ColorUpLogic.shouldPauseColorUp(false, true, true);

        assertThat(shouldPause).isFalse();
    }

    // =================================================================
    // shouldSkipPhase1() Tests
    // =================================================================

    @Test
    void should_SkipPhase1_When_NoPlayersWithOddChips() {
        boolean shouldSkip = ColorUpLogic.shouldSkipPhase1(0);

        assertThat(shouldSkip).isTrue();
    }

    @Test
    void should_NotSkipPhase1_When_OnePlayerWithOddChips() {
        boolean shouldSkip = ColorUpLogic.shouldSkipPhase1(1);

        assertThat(shouldSkip).isFalse();
    }

    @Test
    void should_NotSkipPhase1_When_MultiplePlayersWithOddChips() {
        boolean shouldSkip = ColorUpLogic.shouldSkipPhase1(5);

        assertThat(shouldSkip).isFalse();
    }

    // =================================================================
    // countPlayersWithOddChips() Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_EmptyPlayerList() {
        List<PokerPlayer> players = new ArrayList<>();

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isZero();
    }

    @Test
    void should_ReturnZero_When_AllPlayersNull() {
        List<PokerPlayer> players = Arrays.asList(null, null, null);

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isZero();
    }

    @Test
    void should_ReturnZero_When_NoPlayersHaveOddChips() {
        PokerPlayer p1 = new PokerPlayer(null, 1000, "Player1", false);
        PokerPlayer p2 = new PokerPlayer(null, 1000, "Player2", false);
        p1.setOddChips(0);
        p2.setOddChips(0);
        List<PokerPlayer> players = Arrays.asList(p1, p2);

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isZero();
    }

    @Test
    void should_ReturnOne_When_OnePlayerHasOddChips() {
        PokerPlayer p1 = new PokerPlayer(null, 1000, "Player1", false);
        PokerPlayer p2 = new PokerPlayer(null, 1000, "Player2", false);
        p1.setOddChips(3);
        p2.setOddChips(0);
        List<PokerPlayer> players = Arrays.asList(p1, p2);

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void should_ReturnCorrectCount_When_MultiplePlayersHaveOddChips() {
        PokerPlayer p1 = new PokerPlayer(null, 1000, "Player1", false);
        PokerPlayer p2 = new PokerPlayer(null, 1000, "Player2", false);
        PokerPlayer p3 = new PokerPlayer(null, 1000, "Player3", false);
        p1.setOddChips(2);
        p2.setOddChips(0);
        p3.setOddChips(4);
        List<PokerPlayer> players = Arrays.asList(p1, p2, p3);

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_IgnoreNulls_When_CountingOddChips() {
        PokerPlayer p1 = new PokerPlayer(null, 1000, "Player1", false);
        PokerPlayer p2 = new PokerPlayer(null, 1000, "Player2", false);
        p1.setOddChips(3);
        p2.setOddChips(1);
        List<PokerPlayer> players = Arrays.asList(p1, null, p2, null);

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_CountAllSeats_When_AllHaveOddChips() {
        List<PokerPlayer> players = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PokerPlayer player = new PokerPlayer(null, i, "Player" + i, false);
            player.setOddChips(i + 1); // 1, 2, 3, ... 10
            players.add(player);
        }

        int count = ColorUpLogic.countPlayersWithOddChips(players);

        assertThat(count).isEqualTo(10);
    }
}
