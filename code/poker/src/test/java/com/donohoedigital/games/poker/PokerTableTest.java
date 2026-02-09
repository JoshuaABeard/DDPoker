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
package com.donohoedigital.games.poker;

import com.donohoedigital.config.ApplicationType;
import com.donohoedigital.config.ConfigManager;
import com.donohoedigital.games.poker.engine.PokerConstants;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

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
    @Disabled("Integration test - requires players at seats and GameEngine validation")
    void should_SetButton_When_ButtonSet() {
        table.setButton(3);

        assertThat(table.getButton()).isEqualTo(3);
    }

    @Test
    @Disabled("Integration test - requires GameEngine.isDemo() for button calculation")
    void should_CallSetButton_When_SetButtonNoArgsCalled() {
        PokerPlayer player1 = createTestPlayer("Player1");
        PokerPlayer player2 = createTestPlayer("Player2");
        table.setPlayer(player1, 0);
        table.setPlayer(player2, 1);

        // setButton() with no args calculates button position based on players
        table.setButton();

        assertThat(table.getButton()).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // Table State Tests
    // =================================================================

    @Test
    void should_HaveNoneState_When_TableCreated() {
        assertThat(table.getTableState()).isEqualTo(PokerTable.STATE_NONE);
    }

    @Test
    void should_SetTableState_When_StateSet() {
        table.setTableState(PokerTable.STATE_START_HAND);

        assertThat(table.getTableState()).isEqualTo(PokerTable.STATE_START_HAND);
    }

    @Test
    void should_TrackPreviousState_When_StateChanged() {
        table.setTableState(PokerTable.STATE_START_HAND);
        table.setTableState(PokerTable.STATE_BEGIN);

        assertThat(table.getPreviousTableState()).isEqualTo(PokerTable.STATE_START_HAND);
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
    // Observer Tests
    // =================================================================

    @Test
    @Disabled("Integration test - requires GameEngine event system for observer registration")
    void should_AddObserver_When_ObserverAdded() {
        PokerPlayer observer = createTestPlayer("Observer");

        table.addObserver(observer);

        assertThat(table.getNumObservers()).isEqualTo(1);
    }

    @Test
    @Disabled("Integration test - requires GameEngine event system for observer registration")
    void should_RemoveObserver_When_ObserverRemoved() {
        PokerPlayer observer = createTestPlayer("Observer");
        table.addObserver(observer);

        table.removeObserver(observer);

        assertThat(table.getNumObservers()).isZero();
    }

    @Test
    @Disabled("Integration test - requires GameEngine event system for observer registration")
    void should_AddMultipleObservers_When_MultipleObserversAdded() {
        PokerPlayer observer1 = createTestPlayer("Observer1");
        PokerPlayer observer2 = createTestPlayer("Observer2");

        table.addObserver(observer1);
        table.addObserver(observer2);

        assertThat(table.getNumObservers()).isEqualTo(2);
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
