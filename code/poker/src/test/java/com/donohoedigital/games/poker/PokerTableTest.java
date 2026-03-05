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

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import com.donohoedigital.games.poker.engine.state.TableState;

/**
 * Tests for PokerTable - table setup, seat management, player management,
 * button tracking, and table state.
 */
class PokerTableTest {

    private PokerTable table;
    private PokerGame game;

    @BeforeEach
    void setUp() {
        // Initialize ConfigManager for headless testing
        new ConfigManager("poker", ApplicationType.HEADLESS_CLIENT);

        // Create a game with tournament profile
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);

        // Create table (will now have access to profile)
        table = new PokerTable(game, 1);
    }

    // =================================================================
    // Table Initialization Tests
    // =================================================================

    @Test
    void should_CreateTable_When_ConstructorCalled() {
        assertThat(table).isNotNull();
        assertThat(table.getGame()).isEqualTo(game);
        assertThat(table.getNumber()).isEqualTo(1);
    }

    @Test
    void should_HaveDefaultSeats_When_TableCreated() {
        int seats = table.getSeats();

        assertThat(seats).isGreaterThan(0);
        assertThat(seats).isLessThanOrEqualTo(PokerConstants.SEATS);
    }

    @Test
    void should_HaveTableName_When_TableCreated() {
        String name = table.getName();

        assertThat(name).isNotNull();
        assertThat(name).contains("1");
    }

    @Test
    void should_HaveNoButton_When_TableCreated() {
        int button = table.getButton();

        assertThat(button).isEqualTo(PokerTable.NO_SEAT);
    }

    @Test
    void should_NotBeRemoved_When_TableCreated() {
        assertThat(table.isRemoved()).isFalse();
    }

    @Test
    void should_NotBeSimulation_When_TableCreated() {
        assertThat(table.isSimulation()).isFalse();
    }

    // =================================================================
    // Seat Management Tests
    // =================================================================

    @Test
    void should_HaveAllSeatsEmpty_When_TableCreated() {
        int occupiedSeats = table.getNumOccupiedSeats();

        assertThat(occupiedSeats).isZero();
    }

    @Test
    void should_HaveAllSeatsOpen_When_TableCreated() {
        int openSeats = table.getNumOpenSeats();
        int totalSeats = table.getSeats();

        assertThat(openSeats).isEqualTo(totalSeats);
    }

    @Test
    void should_AddPlayerToSeat_When_SetPlayerCalled() {
        PokerPlayer player = createTestPlayer("Player1");

        table.setPlayer(player, 0);

        assertThat(table.getPlayer(0)).isEqualTo(player);
        assertThat(player.getTable()).isEqualTo(table);
        assertThat(player.getSeat()).isZero();
    }

    @Test
    void should_IncrementOccupiedSeats_When_PlayerAdded() {
        PokerPlayer player = createTestPlayer("Player1");

        table.setPlayer(player, 0);

        assertThat(table.getNumOccupiedSeats()).isEqualTo(1);
    }

    @Test
    void should_DecrementOpenSeats_When_PlayerAdded() {
        int initialOpenSeats = table.getNumOpenSeats();
        PokerPlayer player = createTestPlayer("Player1");

        table.setPlayer(player, 0);

        assertThat(table.getNumOpenSeats()).isEqualTo(initialOpenSeats - 1);
    }

    @Test
    void should_AddMultiplePlayers_When_MultiplePlayersSet() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");

        table.setPlayer(player1, 0);
        table.setPlayer(player2, 2);
        table.setPlayer(player3, 5);

        assertThat(table.getNumOccupiedSeats()).isEqualTo(3);
        assertThat(table.getPlayer(0)).isEqualTo(player1);
        assertThat(table.getPlayer(2)).isEqualTo(player2);
        assertThat(table.getPlayer(5)).isEqualTo(player3);
    }

    @Test
    void should_RemovePlayer_When_RemovePlayerCalled() {
        PokerPlayer player = createTestPlayer("Player1");
        table.setPlayer(player, 0);

        PokerPlayer removed = table.removePlayer(0);

        assertThat(removed).isEqualTo(player);
        assertThat(table.getPlayer(0)).isNull();
        assertThat(table.getNumOccupiedSeats()).isZero();
    }

    @Test
    void should_ReturnNull_When_GetPlayerOnEmptySeat() {
        PokerPlayer player = table.getPlayer(3);

        assertThat(player).isNull();
    }

    @Test
    void should_AddPlayerToNextSeat_When_AddPlayerCalled() {
        PokerPlayer player = createTestPlayer("Player1");

        table.addPlayer(player);

        assertThat(table.getNumOccupiedSeats()).isEqualTo(1);
        assertThat(player.getTable()).isEqualTo(table);
        assertThat(player.getSeat()).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // Button and Position Tests
    // =================================================================

    @Test
    void should_SetButton_When_ButtonPositionSet() {
        PokerPlayer player = createTestPlayer("Player1");
        table.setPlayer(player, 3);

        table.setButton(3);

        assertThat(table.getButton()).isEqualTo(3);
    }

    @Test
    void should_MoveButton_When_MoveButtonCalled() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 3);
        table.setButton(0);

        table.moveButton();

        assertThat(table.getButton()).isEqualTo(3);
    }

    @Test
    void should_WrapAround_When_ButtonAtLastOccupiedSeat() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        table.setPlayer(player1, 1);
        table.setPlayer(player2, 8);
        table.setButton(8);

        table.moveButton();

        assertThat(table.getButton()).isEqualTo(1);
    }

    @Test
    void should_SkipEmptySeats_When_MovingButton() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 4);
        table.setPlayer(player3, 7);
        table.setButton(0);

        table.moveButton();

        // Should skip seats 1, 2, 3 and land on seat 4
        assertThat(table.getButton()).isEqualTo(4);
    }

    @Test
    void should_ReturnNextOccupiedSeat_When_GetNextSeatCalled() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        table.setPlayer(player1, 2);
        table.setPlayer(player2, 6);

        int nextAfterTwo = table.getNextSeat(2);

        assertThat(nextAfterTwo).isEqualTo(6);
    }

    @Test
    void should_WrapAroundNextSeat_When_NoHigherOccupiedSeat() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        table.setPlayer(player1, 2);
        table.setPlayer(player2, 6);

        int nextAfterSix = table.getNextSeat(6);

        assertThat(nextAfterSix).isEqualTo(2);
    }

    @Test
    void should_ReturnNextSeatAfterButton_When_ButtonSet() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 3);
        table.setPlayer(player3, 7);
        table.setButton(3);

        int nextSeat = table.getNextSeatAfterButton();

        assertThat(nextSeat).isEqualTo(7);
    }

    @Test
    void should_MoveThroughAllPlayers_When_ButtonMovedRepeatedly() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        PokerPlayer player3 = createTestPlayer("Player3");
        table.setPlayer(player1, 1);
        table.setPlayer(player2, 4);
        table.setPlayer(player3, 8);
        table.setButton(1);

        table.moveButton();
        assertThat(table.getButton()).isEqualTo(4);

        table.moveButton();
        assertThat(table.getButton()).isEqualTo(8);

        table.moveButton();
        assertThat(table.getButton()).isEqualTo(1);
    }

    // =================================================================
    // Player List Access Tests
    // =================================================================

    @Test
    void should_ThrowException_When_GetPlayerRequiredOnEmptySeat() {
        assertThatThrownBy(() -> table.getPlayerRequired(0))
                .isInstanceOf(com.donohoedigital.base.ApplicationError.class);
    }

    @Test
    void should_ReturnPlayer_When_GetPlayerRequiredOnOccupiedSeat() {
        PokerPlayer player = createTestPlayer("Player1");
        table.setPlayer(player, 5);

        PokerPlayer result = table.getPlayerRequired(5);

        assertThat(result).isEqualTo(player);
    }

    @Test
    void should_ReturnCorrectCount_When_MultiplePlayersAdded() {
        table.setPlayer(createTestPlayer("P1"), 0);
        table.setPlayer(createTestPlayer("P2"), 3);
        table.setPlayer(createTestPlayer("P3"), 7);

        assertThat(table.getNumOccupiedSeats()).isEqualTo(3);
        assertThat(table.getNumOpenSeats()).isEqualTo(table.getSeats() - 3);
    }

    @Test
    void should_ReturnSortedPlayers_When_GetPlayersSortedByLastMove() {
        PokerPlayer player1 = createTestPlayer("P1");
        PokerPlayer player2 = createTestPlayer("P2");
        PokerPlayer player3 = createTestPlayer("P3");
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 3);
        table.setPlayer(player3, 7);

        PokerPlayer[] sorted = table.getPlayersSortedByLastMove();

        assertThat(sorted).hasSize(3);
        assertThat(sorted).containsExactlyInAnyOrder(player1, player2, player3);
    }

    // =================================================================
    // Level and Chip Management Tests
    // =================================================================

    @Test
    void should_TrackLevel_When_LevelSet() {
        table.setLevel(5);

        assertThat(table.getLevel()).isEqualTo(5);
    }

    @Test
    void should_UpdateLevel_When_LevelChangedMultipleTimes() {
        table.setLevel(2);
        assertThat(table.getLevel()).isEqualTo(2);

        table.setLevel(7);
        assertThat(table.getLevel()).isEqualTo(7);
    }

    @Test
    void should_TrackMinChip_When_MinChipSet() {
        table.setMinChip(25);

        assertThat(table.getMinChip()).isEqualTo(25);
    }

    @Test
    void should_TrackNextMinChip_When_NextMinChipSet() {
        table.setNextMinChip(50);

        assertThat(table.getNextMinChip()).isEqualTo(50);
    }

    @Test
    void should_TrackHandNum_When_HandNumSet() {
        table.setHandNum(42);

        assertThat(table.getHandNum()).isEqualTo(42);
    }

    @Test
    void should_ReturnProfile_When_GameHasProfile() {
        assertThat(table.getProfile()).isNotNull();
        assertThat(table.getProfile().getBuyinChips()).isEqualTo(1500);
    }

    @Test
    void should_ReturnBlinds_When_ProfileQueried() {
        // Default profile at level 1 should have some blind values
        var profile = table.getProfile();
        int level = table.getLevel();

        int bigBlind = profile.getBigBlind(level);
        int smallBlind = profile.getSmallBlind(level);

        assertThat(bigBlind).isGreaterThan(0);
        assertThat(smallBlind).isGreaterThan(0);
        assertThat(bigBlind).isGreaterThanOrEqualTo(smallBlind);
    }

    // =================================================================
    // Additional Properties Tests
    // =================================================================

    @Test
    void should_SetCurrent_When_CurrentFlagSet() {
        table.setCurrent(true);

        assertThat(table.isCurrent()).isTrue();
    }

    @Test
    void should_NotBeCurrent_When_TableCreated() {
        assertThat(table.isCurrent()).isFalse();
    }

    @Test
    void should_SetZipMode_When_ZipModeFlagSet() {
        table.setZipMode(true);

        assertThat(table.isZipMode()).isTrue();
    }

    @Test
    void should_NotBeZipMode_When_TableCreated() {
        assertThat(table.isZipMode()).isFalse();
    }

    @Test
    void should_HaveNoHoldemHand_When_TableCreated() {
        assertThat(table.getHoldemHand()).isNull();
    }

    @Test
    void should_ReturnTableString_When_ToStringCalled() {
        String result = table.toString();

        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void should_IncludePlayerInfo_When_ToStringWithPlayers() {
        PokerPlayer player = createTestPlayer("TestPlayer");
        table.setPlayer(player, 0);

        String result = table.toString();

        assertThat(result).contains("TestPlayer");
    }

    @Test
    void should_IndicateButtonInToString_When_ButtonSet() {
        PokerPlayer player = createTestPlayer("Player1");
        table.setPlayer(player, 0);
        table.setButton(0);

        String result = table.toString();

        // Button is indicated with an asterisk in toString
        assertThat(result).contains("*");
    }

    // =================================================================
    // Table State Tests
    // =================================================================

    @Test
    void should_HaveNoneState_When_TableCreated() {
        assertThat(table.getTableState()).isEqualTo(TableState.NONE);
    }

    @Test
    void should_SetTableState_When_StateSet() {
        table.setTableState(PokerTable.STATE_START_HAND);

        assertThat(table.getTableState()).isEqualTo(TableState.START_HAND);
    }

    @Test
    void should_TrackPreviousState_When_StateChanged() {
        table.setTableState(PokerTable.STATE_START_HAND);
        table.setTableState(PokerTable.STATE_BEGIN);

        assertThat(table.getPreviousTableState()).isEqualTo(TableState.START_HAND);
    }

    @Test
    void should_UpdateStateChangeTime_When_StateChanged() {
        long beforeTime = System.currentTimeMillis();
        table.setTableState(PokerTable.STATE_START_HAND);
        long afterTime = System.currentTimeMillis();

        long stateChangeTime = table.getLastStateChangeTime();

        assertThat(stateChangeTime).isGreaterThanOrEqualTo(beforeTime);
        assertThat(stateChangeTime).isLessThanOrEqualTo(afterTime);
    }

    // =================================================================
    // Table Properties Tests
    // =================================================================

    @Test
    void should_SetRemoved_When_RemovedFlagSet() {
        table.setRemoved(true);

        assertThat(table.isRemoved()).isTrue();
    }

    @Test
    void should_SetSimulation_When_SimulationFlagSet() {
        table.setSimulation(true);

        assertThat(table.isSimulation()).isTrue();
    }

    @Test
    void should_SetDirty_When_DirtyFlagSet() {
        table.setDirty(true);

        assertThat(table.isDirty()).isTrue();
    }

    @Test
    void should_BeAllComputer_When_OnlyComputerPlayers() {
        PokerPlayer computer1 = createTestPlayer("Computer1", false);
        PokerPlayer computer2 = createTestPlayer("Computer2", false);

        table.setPlayer(computer1, 0);
        table.setPlayer(computer2, 1);

        assertThat(table.isAllComputer()).isTrue();
    }

    @Test
    void should_NotBeAllComputer_When_HumanPlayerPresent() {
        PokerPlayer human = createTestPlayer("Human", true);
        PokerPlayer computer = createTestPlayer("Computer", false);

        table.setPlayer(human, 0);
        table.setPlayer(computer, 1);

        assertThat(table.isAllComputer()).isFalse();
    }

    // =================================================================
    // Hand Number Tests
    // =================================================================

    @Test
    void should_StartWithHandZero_When_TableCreated() {
        assertThat(table.getHandNum()).isZero();
    }

    @Test
    void should_HaveLevel_When_LevelQueried() {
        int level = table.getLevel();

        assertThat(level).isGreaterThan(0);
    }

    @Test
    void should_HaveMinChip_When_MinChipQueried() {
        int minChip = table.getMinChip();

        assertThat(minChip).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // Test Helper Methods
    // =================================================================

    private PokerPlayer createTestPlayer(String name) {
        return createTestPlayer(name, true);
    }

    private PokerPlayer createTestPlayer(String name, boolean isHuman) {
        return new PokerPlayer(0, name, isHuman);
    }
}
