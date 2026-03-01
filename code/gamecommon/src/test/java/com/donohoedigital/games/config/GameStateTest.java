/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2026 Joshua Beard and contributors
 *
 * This file is part of DD Poker, originally created by Doug Donohoe.
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
package com.donohoedigital.games.config;

import com.donohoedigital.base.TypedHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GameState — lightweight game-save container covering
 * name/description, entry list management, save-details, delegate wiring, and
 * file metadata.
 */
class GameStateTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState("My Game", "A test game");
    }

    @AfterEach
    void tearDown() {
        // Reset static delegate so tests don't bleed into each other.
        GameState.setDelegate(null);
    }

    // ===== Construction: name and description =====

    @Test
    void should_ReturnName_When_ConstructedWithNameAndDescription() {
        assertThat(state.getGameName()).isEqualTo("My Game");
    }

    @Test
    void should_ReturnDescription_When_ConstructedWithNameAndDescription() {
        assertThat(state.getDescription()).isEqualTo("A test game");
    }

    @Test
    void should_HaveNullFile_When_ConstructedWithNameAndDescription() {
        assertThat(state.getFile()).isNull();
    }

    // ===== setName / getGameName =====

    @Test
    void should_UpdateName_When_SetNameCalled() {
        state.setName("New Name");

        assertThat(state.getGameName()).isEqualTo("New Name");
    }

    // ===== setDescription / getDescription =====

    @Test
    void should_UpdateDescription_When_SetDescriptionCalled() {
        state.setDescription("Updated description");

        assertThat(state.getDescription()).isEqualTo("Updated description");
    }

    // ===== addEntry / removeEntry / peekEntry =====

    @Test
    void should_ReturnNullPeek_When_NoEntriesAdded() {
        assertThat(state.peekEntry()).isNull();
    }

    @Test
    void should_ReturnEntry_When_OneEntryAdded() {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);
        state.addEntry(entry);

        assertThat(state.peekEntry()).isSameAs(entry);
    }

    @Test
    void should_NotRemoveEntry_When_PeekCalled() {
        GameStateEntry entry = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);
        state.addEntry(entry);

        state.peekEntry();

        assertThat(state.peekEntry()).isSameAs(entry);
    }

    @Test
    void should_RemoveEntryFromFront_When_RemoveEntryCalled() {
        GameStateEntry first = new GameStateEntry(state, null, ConfigConstants.SAVE_DATA);
        GameStateEntry second = new GameStateEntry(state, null, ConfigConstants.SAVE_TOKEN);
        state.addEntry(first);
        state.addEntry(second);

        GameStateEntry removed = state.removeEntry();

        assertThat(removed).isSameAs(first);
        assertThat(state.peekEntry()).isSameAs(second);
    }

    @Test
    void should_IgnoreNullEntry_When_AddEntryCalledWithNull() {
        state.addEntry(null);

        assertThat(state.peekEntry()).isNull();
    }

    @Test
    void should_ThrowError_When_RemoveEntryCalledOnEmptyList() {
        assertThatThrownBy(() -> state.removeEntry()).isInstanceOf(RuntimeException.class);
    }

    // ===== resetAfterRead / resetAfterWrite lifecycle =====

    @Test
    void should_ClearEntries_When_ResetAfterWriteCalled() {
        state.addEntry(new GameStateEntry(state, null, ConfigConstants.SAVE_DATA));
        state.addEntry(new GameStateEntry(state, null, ConfigConstants.SAVE_TOKEN));

        state.resetAfterWrite();

        assertThat(state.peekEntry()).isNull();
    }

    @Test
    void should_ClearEntries_When_ResetAfterReadCalledWithFalse() {
        state.addEntry(new GameStateEntry(state, null, ConfigConstants.SAVE_DATA));

        state.resetAfterRead(false);

        assertThat(state.peekEntry()).isNull();
    }

    @Test
    void should_ClearEntries_When_ResetAfterReadCalledWithTrue() {
        state.addEntry(new GameStateEntry(state, null, ConfigConstants.SAVE_DATA));

        // bCheckEmpty=true logs a warning but still clears
        state.resetAfterRead(true);

        assertThat(state.peekEntry()).isNull();
    }

    // ===== setSaveDetails / getSaveDetails =====

    @Test
    void should_ReturnNull_When_SaveDetailsNotSet() {
        assertThat(state.getSaveDetails()).isNull();
    }

    @Test
    void should_ReturnSaveDetails_When_SaveDetailsSet() {
        SaveDetails details = new SaveDetails();
        state.setSaveDetails(details);

        assertThat(state.getSaveDetails()).isSameAs(details);
    }

    @Test
    void should_ReplaceOldDetails_When_SaveDetailsSetTwice() {
        SaveDetails first = new SaveDetails();
        SaveDetails second = new SaveDetails();
        state.setSaveDetails(first);
        state.setSaveDetails(second);

        assertThat(state.getSaveDetails()).isSameAs(second);
    }

    // ===== static delegate management =====

    @Test
    void should_ReturnNull_When_DelegateNotSet() {
        assertThat(GameState.getDelegate()).isNull();
    }

    @Test
    void should_ReturnDelegate_When_DelegateSet() {
        GameStateDelegate delegate = new NoOpGameStateDelegate();
        GameState.setDelegate(delegate);

        assertThat(GameState.getDelegate()).isSameAs(delegate);
    }

    @Test
    void should_ClearDelegate_When_DelegateSetToNull() {
        GameState.setDelegate(new NoOpGameStateDelegate());
        GameState.setDelegate(null);

        assertThat(GameState.getDelegate()).isNull();
    }

    @Test
    void should_ShareDelegateAcrossInstances_When_DelegateIsStatic() {
        GameStateDelegate delegate = new NoOpGameStateDelegate();
        GameState.setDelegate(delegate);

        GameState other = new GameState("Other", "other desc");

        assertThat(other.getDelegate()).isSameAs(delegate);
    }

    // ===== File-related metadata =====

    @Test
    void should_ReturnZeroLastModified_When_NoFileAssociated() {
        assertThat(state.lastModified()).isEqualTo(0L);
    }

    @Test
    void should_ReturnNegativeOneFileNumber_When_NoFileAssociated() {
        assertThat(state.getFileNumber()).isEqualTo(-1);
    }

    @Test
    void should_ReturnFalseIsOnlineGame_When_NoFileAssociated() {
        assertThat(state.isOnlineGame()).isFalse();
    }

    @Test
    void should_ReturnFalseIsFileInSaveDirectory_When_NoFileAssociated() {
        assertThat(state.isFileInSaveDirectory()).isFalse();
    }

    // ===== Constants =====

    @Test
    void should_HaveExpectedGameBeginConstant() {
        assertThat(GameState.GAME_BEGIN).isEqualTo("save");
    }

    @Test
    void should_HaveExpectedOnlineGameBeginConstant() {
        assertThat(GameState.ONLINE_GAME_BEGIN).isEqualTo("online");
    }

    @Test
    void should_HaveExpectedEntryEndlineConstant() {
        assertThat(GameState.ENTRY_ENDLINE).isEqualTo('\n');
    }

    // ===== initForLoad with SAVE_NONE details =====

    @Test
    void should_NotLoadGameData_When_SaveDetailsHashDataIsNone() {
        SaveDetails details = new SaveDetails(SaveDetails.SAVE_NONE);
        state.setSaveDetails(details);

        TypedHashMap map = new TypedHashMap();
        // With SAVE_NONE, initForLoad is a no-op — should not throw.
        assertThatCode(() -> state.initForLoad(map)).doesNotThrowAnyException();
    }

    // ===== Helpers =====

    /**
     * No-op delegate for testing static delegate management without game
     * infrastructure.
     */
    private static class NoOpGameStateDelegate implements GameStateDelegate {

        @Override
        public boolean saveTerritory(Territory t) {
            return false;
        }

        @Override
        public void prepopulateCustomIds(Object game, GameState state) {
        }

        @Override
        public boolean createNewInstance(Class cClass) {
            return false;
        }

        @Override
        public Object getInstance(Class cClass, GameState state, GameStateEntry entry) {
            return null;
        }

        @Override
        public void saveCustomData(GameState state) {
        }

        @Override
        public void loadCustomData(GameState state) {
        }

        @Override
        public String getBeginGamePhase(Object context, Object game, GameState state, TypedHashMap params) {
            return null;
        }
    }
}
