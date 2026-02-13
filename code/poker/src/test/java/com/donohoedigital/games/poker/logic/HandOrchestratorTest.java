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

import com.donohoedigital.games.poker.logic.HandOrchestrator.PlayerActionType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for HandOrchestrator - hand lifecycle logic extracted from
 * TournamentDirector.java. Tests run in headless mode with no UI dependencies.
 * Part of Wave 3 testability refactoring.
 */
@Tag("unit")
class HandOrchestratorTest {

    // Mock constants (matching PokerTable and HoldemHand constants)
    private static final int STATE_BETTING = 1;
    private static final int STATE_COMMUNITY = 2;
    private static final int STATE_PRE_SHOWDOWN = 3;
    private static final int ROUND_RIVER = 4;
    private static final int ROUND_TURN = 3;

    // =================================================================
    // determineNextBettingState() Tests
    // =================================================================

    @Test
    void should_ReturnBettingState_When_HandNotDone() {
        int nextState = HandOrchestrator.determineNextBettingState(false, ROUND_TURN, ROUND_RIVER, STATE_BETTING,
                STATE_PRE_SHOWDOWN, STATE_COMMUNITY);

        assertThat(nextState).isEqualTo(STATE_BETTING);
    }

    @Test
    void should_ReturnPreShowdown_When_HandDoneOnRiver() {
        int nextState = HandOrchestrator.determineNextBettingState(true, ROUND_RIVER, ROUND_RIVER, STATE_BETTING,
                STATE_PRE_SHOWDOWN, STATE_COMMUNITY);

        assertThat(nextState).isEqualTo(STATE_PRE_SHOWDOWN);
    }

    @Test
    void should_ReturnCommunity_When_HandDoneBeforeRiver() {
        int nextState = HandOrchestrator.determineNextBettingState(true, ROUND_TURN, ROUND_RIVER, STATE_BETTING,
                STATE_PRE_SHOWDOWN, STATE_COMMUNITY);

        assertThat(nextState).isEqualTo(STATE_COMMUNITY);
    }

    // =================================================================
    // shouldRunDealCommunityPhase() Tests
    // =================================================================

    @Test
    void should_RunDealCommunity_When_PracticeMode() {
        boolean shouldRun = HandOrchestrator.shouldRunDealCommunityPhase(false, 1);

        assertThat(shouldRun).isTrue();
    }

    @Test
    void should_RunDealCommunity_When_OnlineWithMultiplePlayers() {
        boolean shouldRun = HandOrchestrator.shouldRunDealCommunityPhase(true, 3);

        assertThat(shouldRun).isTrue();
    }

    @Test
    void should_NotRunDealCommunity_When_OnlineWithOnePlayer() {
        boolean shouldRun = HandOrchestrator.shouldRunDealCommunityPhase(true, 1);

        assertThat(shouldRun).isFalse();
    }

    @Test
    void should_NotRunDealCommunity_When_OnlineWithNoPlayers() {
        boolean shouldRun = HandOrchestrator.shouldRunDealCommunityPhase(true, 0);

        assertThat(shouldRun).isFalse();
    }

    // =================================================================
    // determinePlayerActionType() Tests
    // =================================================================

    @Test
    void should_ReturnSittingOut_When_PlayerSittingOut() {
        PlayerActionType type = HandOrchestrator.determinePlayerActionType(true, false, false, false, true);

        assertThat(type).isEqualTo(PlayerActionType.SITTING_OUT);
    }

    @Test
    void should_ReturnLocalCurrentTable_When_LocalPlayerOnCurrentTable() {
        PlayerActionType type = HandOrchestrator.determinePlayerActionType(false, true, true, false, true);

        assertThat(type).isEqualTo(PlayerActionType.LOCAL_CURRENT_TABLE);
    }

    @Test
    void should_ReturnComputerOtherTable_When_LocalPlayerOnOtherTable() {
        PlayerActionType type = HandOrchestrator.determinePlayerActionType(false, true, false, true, true);

        assertThat(type).isEqualTo(PlayerActionType.COMPUTER_OTHER_TABLE);
    }

    @Test
    void should_ReturnRemote_When_RemotePlayerOnHost() {
        PlayerActionType type = HandOrchestrator.determinePlayerActionType(false, false, false, false, true);

        assertThat(type).isEqualTo(PlayerActionType.REMOTE);
    }

    @Test
    void should_ThrowException_When_NotHostAndRemotePlayer() {
        assertThatThrownBy(() -> HandOrchestrator.determinePlayerActionType(false, false, false, false, false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot determine action type");
    }

    // =================================================================
    // shouldProcessAllComputerBetting() Tests
    // =================================================================

    @Test
    void should_ProcessAllComputerBetting_When_HostCurrentTableBetting() {
        boolean shouldProcess = HandOrchestrator.shouldProcessAllComputerBetting(true, true, STATE_BETTING,
                STATE_BETTING);

        assertThat(shouldProcess).isTrue();
    }

    @Test
    void should_NotProcessAllComputerBetting_When_NotHost() {
        boolean shouldProcess = HandOrchestrator.shouldProcessAllComputerBetting(false, true, STATE_BETTING,
                STATE_BETTING);

        assertThat(shouldProcess).isFalse();
    }

    @Test
    void should_NotProcessAllComputerBetting_When_NotCurrentTable() {
        boolean shouldProcess = HandOrchestrator.shouldProcessAllComputerBetting(true, false, STATE_BETTING,
                STATE_BETTING);

        assertThat(shouldProcess).isFalse();
    }

    @Test
    void should_NotProcessAllComputerBetting_When_NotBettingState() {
        boolean shouldProcess = HandOrchestrator.shouldProcessAllComputerBetting(true, true, STATE_COMMUNITY,
                STATE_BETTING);

        assertThat(shouldProcess).isFalse();
    }

    // =================================================================
    // isHandComplete() Tests
    // =================================================================

    @Test
    void isHandComplete_Should_ReturnFalse_When_HandIsNull() {
        assertThat(HandOrchestrator.isHandComplete(null)).isFalse();
    }

    // =================================================================
    // calculateAIPauseMillis() Tests
    // =================================================================

    @Test
    void should_CalculateCorrectPause_When_GivenTenths() {
        assertThat(HandOrchestrator.calculateAIPauseMillis(5)).isEqualTo(500);
        assertThat(HandOrchestrator.calculateAIPauseMillis(10)).isEqualTo(1000);
        assertThat(HandOrchestrator.calculateAIPauseMillis(0)).isEqualTo(0);
    }
}
