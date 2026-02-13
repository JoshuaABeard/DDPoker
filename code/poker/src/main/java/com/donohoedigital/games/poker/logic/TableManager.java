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
import com.donohoedigital.games.poker.PokerTable;
import com.donohoedigital.games.poker.engine.PokerConstants;

import java.util.List;

/**
 * Table management decision logic extracted from TournamentDirector.java.
 * Contains pure business logic for table selection, observer management, and
 * table state decisions with no UI dependencies. Part of Wave 3 testability
 * refactoring.
 *
 * <p>
 * Extracted logic:
 * <ul>
 * <li>New table selection when current table becomes unavailable</li>
 * <li>Fallback table selection logic</li>
 * <li>Observer movement decisions</li>
 * <li>Table removal state checks</li>
 * </ul>
 *
 * <p>
 * Note: Table consolidation logic remains in OtherTables.java
 */
public class TableManager {

    // Utility class - no instantiation
    private TableManager() {
    }

    /**
     * Value object representing table selection result.
     */
    public static class TableSelectionResult {
        private final PokerTable selectedTable;
        private final boolean usedFallback;

        public TableSelectionResult(PokerTable selectedTable, boolean usedFallback) {
            this.selectedTable = selectedTable;
            this.usedFallback = usedFallback;
        }

        public PokerTable getSelectedTable() {
            return selectedTable;
        }

        public boolean isUsedFallback() {
            return usedFallback;
        }
    }

    /**
     * Select appropriate table when current table becomes unavailable.
     *
     * <p>
     * Extracted from TournamentDirector.getNewTable() lines 1775-1809.
     *
     * <p>
     * Selection priority:
     * <ol>
     * <li>Host's current table (if not removed)</li>
     * <li>First table with human players</li>
     * <li>Original table (if not removed)</li>
     * <li>First available all-computer table (fallback)</li>
     * </ol>
     *
     * @param currentTable
     *            the current table that may be unavailable
     * @param hostTable
     *            the host player's table (may be null or removed)
     * @param allTables
     *            all available tables in the game
     * @return TableSelectionResult with selected table and fallback flag
     * @throws IllegalArgumentException
     *             if no valid table can be found
     */
    public static TableSelectionResult selectNewTable(PokerTable currentTable, PokerTable hostTable,
            List<PokerTable> allTables) {

        PokerTable selected = null;
        PokerTable fallback = null;
        boolean usedFallback = false;

        // Try host's table first (if valid)
        if (hostTable != null && !isTableRemoved(hostTable)) {
            return new TableSelectionResult(hostTable, false);
        }

        // Look for first table with human players, or remember first all-computer table
        // as fallback
        for (PokerTable table : allTables) {
            if (isTableRemoved(table)) {
                continue; // Skip removed tables
            }

            if (!isAllComputer(table)) {
                selected = table;
                break;
            } else if (fallback == null) {
                fallback = table;
            }
        }

        // Use found human table, or fall back to current table if valid, or use
        // fallback
        if (selected != null) {
            return new TableSelectionResult(selected, false);
        } else if (!isTableRemoved(currentTable)) {
            return new TableSelectionResult(currentTable, false);
        } else if (fallback != null) {
            return new TableSelectionResult(fallback, true);
        }

        throw new IllegalArgumentException("No valid table found. Current: " + currentTable + ", Host: " + hostTable
                + ", Available tables: " + allTables.size());
    }

    /**
     * Determine if observers should be moved from one table to another.
     *
     * <p>
     * Extracted from TournamentDirector.processRemovedPlayers() lines 1743-1747.
     *
     * <p>
     * Observers should be moved when the source table becomes all-computer (no
     * humans remaining to observe).
     *
     * @param sourceTable
     *            table to check
     * @return true if observers should be moved to a different table
     */
    public static boolean shouldMoveObservers(PokerTable sourceTable) {
        return isAllComputer(sourceTable);
    }

    /**
     * Determine if table has only computer players.
     *
     * <p>
     * Helper method wrapping PokerTable.isAllComputer() for testability.
     *
     * @param table
     *            table to check
     * @return true if table has only computer players
     */
    public static boolean isAllComputer(PokerTable table) {
        return table != null && table.isAllComputer();
    }

    /**
     * Determine if table has been removed from play.
     *
     * <p>
     * Helper method wrapping PokerTable.isRemoved() for testability.
     *
     * @param table
     *            table to check
     * @return true if table has been removed
     */
    public static boolean isTableRemoved(PokerTable table) {
        return table == null || table.isRemoved();
    }

    /**
     * Determine if a table should be cleaned of eliminated players.
     *
     * <p>
     * Extracted logic from TournamentDirector.cleanTable() lines 1716-1722.
     *
     * <p>
     * All-computer tables with no observers can be cleaned more aggressively (AI
     * verification).
     *
     * @param table
     *            table to check
     * @return true if table was all-computer with no observers prior to cleaning
     */
    public static boolean wasAllComputerNoObservers(PokerTable table) {
        return table != null && table.isAllComputer() && table.getNumObservers() == 0;
    }

    /**
     * Count non-computer players in a table.
     *
     * <p>
     * Used for determining if a table has human players worth observing.
     *
     * @param table
     *            table to check
     * @return number of human players
     */
    public static int countHumanPlayers(PokerTable table) {
        if (table == null) {
            return 0;
        }

        int count = 0;
        PokerPlayer player;
        for (int i = 0; i < PokerConstants.SEATS; i++) {
            player = table.getPlayer(i);
            if (player != null && !player.isComputer()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determine if a table has human observers.
     *
     * <p>
     * Used to decide if observer-related processing is needed.
     *
     * @param table
     *            table to check
     * @return true if table has at least one human observer
     */
    public static boolean hasHumanObservers(PokerTable table) {
        if (table == null) {
            return false;
        }

        int numObservers = table.getNumObservers();
        for (int i = 0; i < numObservers; i++) {
            if (!table.getObserver(i).isComputer()) {
                return true;
            }
        }
        return false;
    }
}
