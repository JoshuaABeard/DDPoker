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
package com.donohoedigital.games.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AbstractPlayerList - sorted player list with CSV import/export.
 */
class AbstractPlayerListTest {

    private TestPlayerList list;

    @BeforeEach
    void setUp() {
        list = new TestPlayerList();
    }

    // ========== Add Tests ==========

    @Test
    void should_AddPlayer_When_AddCalledWithNameAndKey() {
        list.add("Alice", "key1", false);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.containsPlayer("Alice")).isTrue();
    }

    @Test
    void should_NotAddDuplicate_When_SameNameAddedTwice() {
        list.add("Alice", "key1", false);
        list.add("Alice", "key2", false);

        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    void should_MaintainSortedOrder_When_MultiplePlayersAdded() {
        list.add("Charlie", "k3", false);
        list.add("Alice", "k1", false);
        list.add("Bob", "k2", false);

        assertThat(list.get(0).getName()).isEqualTo("Alice");
        assertThat(list.get(1).getName()).isEqualTo("Bob");
        assertThat(list.get(2).getName()).isEqualTo("Charlie");
    }

    @Test
    void should_AssignDefaultKey_When_NullKeyProvided() {
        list.add("Alice", null, false);

        assertThat(list.containsKey("NoKey-Alice")).isTrue();
    }

    // ========== Remove Tests ==========

    @Test
    void should_RemovePlayer_When_RemoveCalledWithExistingName() {
        list.add("Alice", "key1", false);
        list.add("Bob", "key2", false);

        list.remove("Alice", false);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.containsPlayer("Alice")).isFalse();
        assertThat(list.containsPlayer("Bob")).isTrue();
    }

    @Test
    void should_DoNothing_When_RemoveCalledWithNonExistingName() {
        list.add("Alice", "key1", false);

        list.remove("NonExistent", false);

        assertThat(list.size()).isEqualTo(1);
    }

    // ========== Contains Tests ==========

    @Test
    void should_ReturnTrue_When_ContainsPlayerByName() {
        list.add("Alice", "key1", false);

        assertThat(list.containsPlayer("Alice")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_PlayerNotInList() {
        assertThat(list.containsPlayer("Alice")).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ContainsKey() {
        list.add("Alice", "secret-key", false);

        assertThat(list.containsKey("secret-key")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_KeyNotInList() {
        assertThat(list.containsKey("nonexistent")).isFalse();
    }

    @Test
    void should_ReturnTrue_When_ContainsPlayerByNameOrKey() {
        list.add("Alice", "key1", false);

        assertThat(list.containsPlayer("Alice", "differentKey")).isTrue();
        assertThat(list.containsPlayer("Unknown", "key1")).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NeitherNameNorKeyMatch() {
        list.add("Alice", "key1", false);

        assertThat(list.containsPlayer("Bob", "key2")).isFalse();
    }

    // ========== CSV Round-trip Tests ==========

    @Test
    void should_ProduceCSV_When_ToCSVCalled() {
        list.add("Alice", "k1", false);
        list.add("Bob", "k2", false);

        String csv = list.toCSV();

        assertThat(csv).contains("Alice");
        assertThat(csv).contains("Bob");
    }

    @Test
    void should_RestoreList_When_FromCSVCalledWithCSVOutput() {
        list.add("Alice", "k1", false);
        list.add("Bob", "k2", false);
        list.add("Charlie", "k3", false);
        String csv = list.toCSV();

        TestPlayerList newList = new TestPlayerList();
        // Add same names so keys are preserved
        newList.add("Alice", "k1", false);
        newList.add("Bob", "k2", false);
        newList.add("Charlie", "k3", false);
        newList.fromCSV(csv, false);

        assertThat(newList.size()).isEqualTo(3);
        assertThat(newList.containsPlayer("Alice")).isTrue();
        assertThat(newList.containsPlayer("Bob")).isTrue();
        assertThat(newList.containsPlayer("Charlie")).isTrue();
    }

    @Test
    void should_HandleEmptyCSV_When_FromCSVCalledWithEmpty() {
        list.add("Alice", "k1", false);

        list.fromCSV("", false);

        assertThat(list.size()).isZero();
    }

    // ========== toString Tests ==========

    @Test
    void should_ReturnCSV_When_ToStringCalled() {
        list.add("Alice", "k1", false);
        list.add("Bob", "k2", false);

        assertThat(list.toString()).isEqualTo(list.toCSV());
    }

    @Test
    void should_ReturnEmpty_When_ToStringCalledOnEmptyList() {
        assertThat(list.toString()).isEmpty();
    }

    // ========== Test Helpers ==========

    /**
     * Minimal concrete subclass of AbstractPlayerList with in-memory storage.
     */
    private static class TestPlayerList extends AbstractPlayerList {
        private String savedNames;
        private String savedKeys;

        @Override
        public String getName() {
            return "TestPlayerList";
        }

        @Override
        protected String fetchNames() {
            return savedNames;
        }

        @Override
        protected String fetchKeys() {
            return savedKeys;
        }

        @Override
        protected void saveNames(String sNames) {
            savedNames = sNames;
        }

        @Override
        protected void saveKeys(String sKeys) {
            savedKeys = sKeys;
        }
    }
}
