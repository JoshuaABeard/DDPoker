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

import com.donohoedigital.games.poker.PokerGame;
import com.donohoedigital.games.poker.PokerPlayer;
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.integration.IntegrationTestBase;
import com.donohoedigital.games.poker.logic.TableManager.TableSelectionResult;
import com.donohoedigital.games.poker.model.TournamentProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for TableManager - table management logic extracted from
 * TournamentDirector.java. Requires GameEngine infrastructure for
 * PokerGame/PokerTable. Part of Wave 3 testability refactoring.
 */
@Tag("integration")
class TableManagerTest extends IntegrationTestBase {

    private PokerGame game;

    @BeforeEach
    void setUp() {
        // Create fresh PokerGame for each test
        // (IntegrationTestBase handles GameEngine/PokerMain infrastructure)
        game = new PokerGame(null);
        TournamentProfile profile = new TournamentProfile("table-manager-test");
        profile.setBuyinChips(1500);
        game.setProfile(profile);
    }

    // =================================================================
    // selectNewTable() Tests
    // =================================================================

    @Test
    void should_SelectHostTable_When_HostTableValid() {
        PokerTable currentTable = createTable(game, "Table1", true);
        PokerTable hostTable = createTable(game, "HostTable", false);
        PokerTable otherTable = createTable(game, "Table2", true);
        List<PokerTable> allTables = Arrays.asList(currentTable, hostTable, otherTable);

        TableSelectionResult result = TableManager.selectNewTable(currentTable, hostTable, allTables);

        assertThat(result.getSelectedTable()).isEqualTo(hostTable);
        assertThat(result.isUsedFallback()).isFalse();
    }

    @Test
    void should_SelectFirstHumanTable_When_HostTableRemoved() {
        PokerTable currentTable = createTable(game, "Table1", true);
        PokerTable hostTable = createRemovedTable(game, "HostTable");
        PokerTable humanTable = createTable(game, "HumanTable", false);
        PokerTable computerTable = createTable(game, "ComputerTable", true);
        List<PokerTable> allTables = Arrays.asList(currentTable, computerTable, humanTable);

        TableSelectionResult result = TableManager.selectNewTable(currentTable, hostTable, allTables);

        assertThat(result.getSelectedTable()).isEqualTo(humanTable);
        assertThat(result.isUsedFallback()).isFalse();
    }

    @Test
    void should_SelectCurrentTable_When_CurrentTableValidAndNoHumanTables() {
        PokerTable currentTable = createTable(game, "Table1", true);
        PokerTable computerTable2 = createTable(game, "Table2", true);
        List<PokerTable> allTables = Arrays.asList(currentTable, computerTable2);

        TableSelectionResult result = TableManager.selectNewTable(currentTable, null, allTables);

        assertThat(result.getSelectedTable()).isEqualTo(currentTable);
        assertThat(result.isUsedFallback()).isFalse();
    }

    @Test
    void should_SelectFallback_When_CurrentTableRemovedAndNoHumanTables() {
        PokerTable currentTable = createRemovedTable(game, "Table1");
        PokerTable fallbackTable = createTable(game, "Fallback", true);
        List<PokerTable> allTables = Arrays.asList(currentTable, fallbackTable);

        TableSelectionResult result = TableManager.selectNewTable(currentTable, null, allTables);

        assertThat(result.getSelectedTable()).isEqualTo(fallbackTable);
        assertThat(result.isUsedFallback()).isTrue();
    }

    @Test
    void should_ThrowException_When_NoValidTablesAvailable() {
        PokerTable currentTable = createRemovedTable(game, "Table1");
        List<PokerTable> allTables = Collections.singletonList(currentTable);

        assertThatThrownBy(() -> TableManager.selectNewTable(currentTable, null, allTables))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No valid table found");
    }

    @Test
    void should_SelectFirstHumanTable_When_MultipleHumanTablesAvailable() {
        PokerTable currentTable = createTable(game, "Table1", true);
        PokerTable humanTable1 = createTable(game, "Human1", false);
        PokerTable humanTable2 = createTable(game, "Human2", false);
        List<PokerTable> allTables = Arrays.asList(currentTable, humanTable1, humanTable2);

        TableSelectionResult result = TableManager.selectNewTable(currentTable, null, allTables);

        assertThat(result.getSelectedTable()).isEqualTo(humanTable1);
        assertThat(result.isUsedFallback()).isFalse();
    }

    // =================================================================
    // shouldMoveObservers() Tests
    // =================================================================

    @Test
    void should_MoveObservers_When_TableIsAllComputer() {
        PokerTable table = createTable(game, "ComputerTable", true);

        boolean shouldMove = TableManager.shouldMoveObservers(table);

        assertThat(shouldMove).isTrue();
    }

    @Test
    void should_NotMoveObservers_When_TableHasHumans() {
        PokerTable table = createTable(game, "MixedTable", false);

        boolean shouldMove = TableManager.shouldMoveObservers(table);

        assertThat(shouldMove).isFalse();
    }

    // =================================================================
    // isAllComputer() Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_TableIsAllComputer() {
        PokerTable table = createTable(game, "ComputerTable", true);

        assertThat(TableManager.isAllComputer(table)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_TableHasHumans() {
        PokerTable table = createTable(game, "MixedTable", false);

        assertThat(TableManager.isAllComputer(table)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_TableIsNull() {
        assertThat(TableManager.isAllComputer(null)).isFalse();
    }

    // =================================================================
    // isTableRemoved() Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_TableIsRemoved() {
        PokerTable table = createRemovedTable(game, "RemovedTable");

        assertThat(TableManager.isTableRemoved(table)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_TableIsNotRemoved() {
        PokerTable table = createTable(game, "ActiveTable", false);

        assertThat(TableManager.isTableRemoved(table)).isFalse();
    }

    @Test
    void isTableRemoved_Should_ReturnTrue_When_TableIsNull() {
        assertThat(TableManager.isTableRemoved(null)).isTrue();
    }

    // =================================================================
    // wasAllComputerNoObservers() Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_AllComputerWithNoObservers() {
        PokerTable table = createTable(game, "ComputerTable", true);

        assertThat(TableManager.wasAllComputerNoObservers(table)).isTrue();
    }

    @Test
    void wasAllComputerNoObservers_Should_ReturnFalse_When_TableHasHumans() {
        PokerTable table = createTable(game, "MixedTable", false);

        assertThat(TableManager.wasAllComputerNoObservers(table)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_TableHasObservers() {
        PokerTable table = createTable(game, "ComputerTable", true);
        PokerPlayer observer = new PokerPlayer(null, 99, "Observer", true);
        observer.setObserver(true);
        table.addObserver(observer);

        assertThat(TableManager.wasAllComputerNoObservers(table)).isFalse();
    }

    @Test
    void wasAllComputerNoObservers_Should_ReturnFalse_When_TableIsNull() {
        assertThat(TableManager.wasAllComputerNoObservers(null)).isFalse();
    }

    // =================================================================
    // countHumanPlayers() Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_TableHasNoPlayers() {
        PokerTable table = new PokerTable(game, 1);

        assertThat(TableManager.countHumanPlayers(table)).isZero();
    }

    @Test
    void should_ReturnZero_When_TableHasOnlyComputerPlayers() {
        PokerTable table = createTable(game, "ComputerTable", true);

        assertThat(TableManager.countHumanPlayers(table)).isZero();
    }

    @Test
    void should_ReturnCorrectCount_When_TableHasMixedPlayers() {
        PokerTable table = new PokerTable(game, 1);
        PokerPlayer human1 = new PokerPlayer(null, 1, "Human1", true);
        PokerPlayer computer1 = new PokerPlayer(null, 2, "Computer1", false);
        PokerPlayer human2 = new PokerPlayer(null, 3, "Human2", true);

        human1.setChipCount(1000);
        computer1.setChipCount(1000);
        human2.setChipCount(1000);

        table.addPlayer(human1);
        table.addPlayer(computer1);
        table.addPlayer(human2);

        assertThat(TableManager.countHumanPlayers(table)).isEqualTo(2);
    }

    @Test
    void countHumanPlayers_Should_ReturnZero_When_TableIsNull() {
        assertThat(TableManager.countHumanPlayers(null)).isZero();
    }

    // =================================================================
    // hasHumanObservers() Tests
    // =================================================================

    @Test
    void should_ReturnFalse_When_NoObservers() {
        PokerTable table = createTable(game, "Table", false);

        assertThat(TableManager.hasHumanObservers(table)).isFalse();
    }

    @Test
    void should_ReturnTrue_When_HasHumanObserver() {
        PokerTable table = createTable(game, "Table", false);
        PokerPlayer humanObserver = new PokerPlayer(null, 99, "Observer", true);
        humanObserver.setObserver(true);
        table.addObserver(humanObserver);

        assertThat(TableManager.hasHumanObservers(table)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_OnlyComputerObservers() {
        PokerTable table = createTable(game, "Table", false);
        PokerPlayer computerObserver = new PokerPlayer(null, 99, "AIObserver", false);
        computerObserver.setObserver(true);
        table.addObserver(computerObserver);

        assertThat(TableManager.hasHumanObservers(table)).isFalse();
    }

    @Test
    void hasHumanObservers_Should_ReturnFalse_When_TableIsNull() {
        assertThat(TableManager.hasHumanObservers(null)).isFalse();
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    private PokerTable createTable(PokerGame game, String name, boolean allComputer) {
        PokerTable table = new PokerTable(game, game.getNumTables());
        game.addTable(table);

        if (allComputer) {
            // Add only computer players (bHuman = false)
            PokerPlayer computer = new PokerPlayer(null, 1, "Computer1", false);
            computer.setChipCount(1000);
            table.addPlayer(computer);
        } else {
            // Add at least one human player (bHuman = true)
            PokerPlayer human = new PokerPlayer(null, 1, "Human1", true);
            human.setChipCount(1000);
            table.addPlayer(human);
        }

        return table;
    }

    private PokerTable createRemovedTable(PokerGame game, String name) {
        PokerTable table = new PokerTable(game, game.getNumTables());
        game.addTable(table);
        table.setRemoved(true);
        return table;
    }
}
