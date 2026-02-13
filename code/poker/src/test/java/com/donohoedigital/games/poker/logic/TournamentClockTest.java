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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TournamentClock - tournament timing logic extracted from
 * TournamentDirector.java. Tests run in headless mode with no UI dependencies.
 * Part of Wave 3 testability refactoring.
 */
@Tag("unit")
class TournamentClockTest {

    // =================================================================
    // hasLevelChanged() Tests
    // =================================================================

    @Test
    void should_DetectLevelChange_When_LevelsDiffer() {
        assertThat(TournamentClock.hasLevelChanged(5, 4)).isTrue();
        assertThat(TournamentClock.hasLevelChanged(10, 9)).isTrue();
    }

    @Test
    void should_NotDetectLevelChange_When_LevelsSame() {
        assertThat(TournamentClock.hasLevelChanged(5, 5)).isFalse();
        assertThat(TournamentClock.hasLevelChanged(1, 1)).isFalse();
    }

    // =================================================================
    // shouldColorUp() Tests
    // =================================================================

    @Test
    void should_ColorUp_When_MinChipIncreases() {
        assertThat(TournamentClock.shouldColorUp(25, 100)).isTrue();
        assertThat(TournamentClock.shouldColorUp(100, 500)).isTrue();
    }

    @Test
    void should_NotColorUp_When_MinChipSame() {
        assertThat(TournamentClock.shouldColorUp(100, 100)).isFalse();
    }

    @Test
    void should_NotColorUp_When_MinChipDecreases() {
        assertThat(TournamentClock.shouldColorUp(100, 25)).isFalse();
    }

    // =================================================================
    // shouldProcessAllComputerLevelCheck() Tests
    // =================================================================

    @Test
    void should_ProcessLevelCheck_When_HostAndCurrentTable() {
        assertThat(TournamentClock.shouldProcessAllComputerLevelCheck(true, true)).isTrue();
    }

    @Test
    void should_NotProcessLevelCheck_When_NotHost() {
        assertThat(TournamentClock.shouldProcessAllComputerLevelCheck(false, true)).isFalse();
    }

    @Test
    void should_NotProcessLevelCheck_When_NotCurrentTable() {
        assertThat(TournamentClock.shouldProcessAllComputerLevelCheck(true, false)).isFalse();
    }

    @Test
    void should_NotProcessLevelCheck_When_NeitherHostNorCurrent() {
        assertThat(TournamentClock.shouldProcessAllComputerLevelCheck(false, false)).isFalse();
    }

    // =================================================================
    // shouldProcessAllComputerColorUp() Tests
    // =================================================================

    @Test
    void should_ProcessColorUp_When_HostCurrentAndColoring() {
        assertThat(TournamentClock.shouldProcessAllComputerColorUp(true, true, true)).isTrue();
    }

    @Test
    void should_NotProcessColorUp_When_NotHost() {
        assertThat(TournamentClock.shouldProcessAllComputerColorUp(false, true, true)).isFalse();
    }

    @Test
    void should_NotProcessColorUp_When_NotCurrentTable() {
        assertThat(TournamentClock.shouldProcessAllComputerColorUp(true, false, true)).isFalse();
    }

    @Test
    void should_NotProcessColorUp_When_NotColoringUp() {
        assertThat(TournamentClock.shouldProcessAllComputerColorUp(true, true, false)).isFalse();
    }

    // =================================================================
    // hasBreakEnded() Tests
    // =================================================================

    @Test
    void should_DetectBreakEnded_When_LevelChangedFromBreak() {
        assertThat(TournamentClock.hasBreakEnded(5, true, true)).isTrue();
    }

    @Test
    void should_NotDetectBreakEnded_When_LevelNotChanged() {
        assertThat(TournamentClock.hasBreakEnded(5, true, false)).isFalse();
    }

    @Test
    void should_NotDetectBreakEnded_When_NotBreakLevel() {
        assertThat(TournamentClock.hasBreakEnded(5, false, true)).isFalse();
    }

    // =================================================================
    // calculateTimeRemaining() Tests
    // =================================================================

    @Test
    void should_CalculateTimeRemaining_When_TimeLeft() {
        assertThat(TournamentClock.calculateTimeRemaining(600, 200)).isEqualTo(400);
        assertThat(TournamentClock.calculateTimeRemaining(1200, 0)).isEqualTo(1200);
    }

    @Test
    void should_ReturnZero_When_TimeExpired() {
        assertThat(TournamentClock.calculateTimeRemaining(600, 600)).isZero();
        assertThat(TournamentClock.calculateTimeRemaining(600, 700)).isZero();
    }

    // =================================================================
    // shouldAdvanceLevel() Tests
    // =================================================================

    @Test
    void should_AdvanceLevel_When_TimeExpired() {
        assertThat(TournamentClock.shouldAdvanceLevel(0)).isTrue();
        assertThat(TournamentClock.shouldAdvanceLevel(-1)).isTrue();
    }

    @Test
    void should_NotAdvanceLevel_When_TimeRemaining() {
        assertThat(TournamentClock.shouldAdvanceLevel(1)).isFalse();
        assertThat(TournamentClock.shouldAdvanceLevel(100)).isFalse();
    }
}
