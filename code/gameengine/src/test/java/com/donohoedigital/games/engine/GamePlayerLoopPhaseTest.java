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

import com.donohoedigital.games.config.GamePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GamePlayerLoopPhase - Phase that loops through game players.
 */
@ExtendWith(MockitoExtension.class)
class GamePlayerLoopPhaseTest {

    @Mock
    private GameEngine engine;

    @Mock
    private GameContext context;

    @Mock
    private GamePhase gamephase;

    @Mock
    private Game game;

    private GamePlayerLoopPhase phase;

    @BeforeEach
    void setUp() {
        when(context.getGame()).thenReturn(game);
        when(gamephase.getString("next-phase", null)).thenReturn("NextPhase");
        when(gamephase.getString("loop-phase", null)).thenReturn("LoopPhase");
        when(gamephase.getString(GamePlayerLoopPhase.PARAM_SAVE_LOOP_PHASE, null)).thenReturn(null);
        when(gamephase.getInteger(GamePlayerLoopPhase.PARAM_STARTING_INDEX, GamePlayerLoopPhase.INITIAL_INDEX))
                .thenReturn(GamePlayerLoopPhase.INITIAL_INDEX);

        phase = new GamePlayerLoopPhase();
        phase.init(engine, context, gamephase);
    }

    // ========== Constants Tests ==========

    @Test
    void should_HaveCorrectConstant_When_ParamStartingIndex() {
        assertThat(GamePlayerLoopPhase.PARAM_STARTING_INDEX).isEqualTo("idx");
    }

    @Test
    void should_HaveCorrectConstant_When_ParamSaveLoopPhase() {
        assertThat(GamePlayerLoopPhase.PARAM_SAVE_LOOP_PHASE).isEqualTo("loop");
    }

    @Test
    void should_HaveCorrectConstant_When_InitialIndex() {
        assertThat(GamePlayerLoopPhase.INITIAL_INDEX).isEqualTo(-1);
    }

    // ========== init Tests ==========

    @Test
    void should_StoreGame_When_InitCalled() {
        assertThat(phase.game_).isSameAs(game);
    }

    @Test
    void should_StoreNextPhase_When_InitCalled() {
        assertThat(phase.sNextPhase_).isEqualTo("NextPhase");
    }

    @Test
    void should_StoreLoopPhase_When_InitCalled() {
        assertThat(phase.sLoopPhase_).isEqualTo("LoopPhase");
    }

    // ========== start() - First Invocation (INITIAL_INDEX) Tests ==========

    @Test
    void should_ProcessLoopPhase_When_StartCalledFirstTimeWithPlayers() {
        when(game.getNumPlayers()).thenReturn(3);

        phase.start();

        // Starting index defaults to 0, not done since 0 < 3
        verify(context).processPhase("LoopPhase", null);
        verify(game).setCurrentPlayer(0);
    }

    @Test
    void should_ProcessNextPhase_When_StartCalledWithNoPlayers() {
        when(game.getNumPlayers()).thenReturn(0);

        phase.start();

        // Starting index defaults to 0, isDone(0) is true since 0 >= 0
        verify(game).setCurrentPlayer(Game.NO_CURRENT_PLAYER);
        verify(context).removeCachedPhase(gamephase);
        verify(context).processPhase("NextPhase", null);
    }

    // ========== start() - Loop Progression Tests ==========

    @Test
    void should_AdvanceToNextPlayer_When_StartCalledAgain() {
        when(game.getNumPlayers()).thenReturn(3);

        // First start: index goes from INITIAL to 0
        phase.start();
        verify(game).setCurrentPlayer(0);

        // Second start: index goes from 0 to 1
        phase.start();
        verify(game).setCurrentPlayer(1);

        // Third start: index goes from 1 to 2
        phase.start();
        verify(game).setCurrentPlayer(2);
    }

    @Test
    void should_ProcessNextPhase_When_AllPlayersProcessed() {
        when(game.getNumPlayers()).thenReturn(2);

        // First: index 0
        phase.start();
        // Second: index 1
        phase.start();
        // Third: index 2, isDone(2) is true since 2 >= 2
        phase.start();

        verify(context).processPhase("NextPhase", null);
    }

    // ========== start() - Starting Index Provided Tests ==========

    @Test
    void should_UseProvidedStartingIndex_When_IndexGivenInParams() {
        // Reinitialize with a starting index
        when(gamephase.getInteger(GamePlayerLoopPhase.PARAM_STARTING_INDEX, GamePlayerLoopPhase.INITIAL_INDEX))
                .thenReturn(2);
        when(game.getNumPlayers()).thenReturn(5);

        GamePlayerLoopPhase phaseWithIndex = new GamePlayerLoopPhase();
        phaseWithIndex.init(engine, context, gamephase);
        phaseWithIndex.start();

        verify(game).setCurrentPlayer(2);
        verify(context).processPhase("LoopPhase", null);
    }

    @Test
    void should_OnlyUseProvidedIndexOnce_When_StartCalledMultipleTimes() {
        when(gamephase.getInteger(GamePlayerLoopPhase.PARAM_STARTING_INDEX, GamePlayerLoopPhase.INITIAL_INDEX))
                .thenReturn(1);
        when(game.getNumPlayers()).thenReturn(5);

        GamePlayerLoopPhase phaseWithIndex = new GamePlayerLoopPhase();
        phaseWithIndex.init(engine, context, gamephase);

        // First call uses provided index
        phaseWithIndex.start();
        verify(game).setCurrentPlayer(1);

        // Second call advances from 1 to 2
        phaseWithIndex.start();
        verify(game).setCurrentPlayer(2);
    }

    // ========== isDone Tests ==========

    @Test
    void should_ReturnTrue_When_IndexEqualsPlayerCount() {
        when(game.getNumPlayers()).thenReturn(3);

        // Access via start behavior - create phase that exposes isDone
        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testIsDone(3)).isTrue();
    }

    @Test
    void should_ReturnTrue_When_IndexExceedsPlayerCount() {
        when(game.getNumPlayers()).thenReturn(3);

        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testIsDone(5)).isTrue();
    }

    @Test
    void should_ReturnFalse_When_IndexLessThanPlayerCount() {
        when(game.getNumPlayers()).thenReturn(3);

        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testIsDone(2)).isFalse();
    }

    @Test
    void should_ReturnFalse_When_IndexIsZeroAndPlayersExist() {
        when(game.getNumPlayers()).thenReturn(1);

        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testIsDone(0)).isFalse();
    }

    // ========== getNextPlayerIndex Tests ==========

    @Test
    void should_ReturnIncrementedIndex_When_GetNextPlayerIndexCalled() {
        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testGetNextPlayerIndex(0)).isEqualTo(1);
        assertThat(testPhase.testGetNextPlayerIndex(4)).isEqualTo(5);
    }

    // ========== getStartingPlayerIndex Tests ==========

    @Test
    void should_ReturnZero_When_GetStartingPlayerIndexCalled() {
        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testGetStartingPlayerIndex()).isEqualTo(0);
    }

    // ========== adjustProvidedStartingIndex Tests ==========

    @Test
    void should_ReturnSameIndex_When_AdjustProvidedStartingIndexCalled() {
        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testAdjustProvidedStartingIndex(5)).isEqualTo(5);
    }

    // ========== getCurrentPlayerIndex Tests ==========

    @Test
    void should_ReturnInitialIndex_When_StartNotYetCalled() {
        assertThat(phase.getCurrentPlayerIndex()).isEqualTo(GamePlayerLoopPhase.INITIAL_INDEX);
    }

    @Test
    void should_ReturnCurrentIndex_When_StartCalled() {
        when(game.getNumPlayers()).thenReturn(3);

        phase.start();

        assertThat(phase.getCurrentPlayerIndex()).isEqualTo(0);
    }

    // ========== getLoopParams / getNextParams Tests ==========

    @Test
    void should_ReturnNull_When_GetLoopParamsCalled() {
        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testGetLoopParams()).isNull();
    }

    @Test
    void should_ReturnNull_When_GetNextParamsCalled() {
        TestableLoopPhase testPhase = new TestableLoopPhase();
        testPhase.init(engine, context, gamephase);

        assertThat(testPhase.testGetNextParams()).isNull();
    }

    // ========== processNextPhase Tests ==========

    @Test
    void should_ClearCurrentPlayer_When_ProcessNextPhaseCalled() {
        when(game.getNumPlayers()).thenReturn(1);

        // First start loops (index 0)
        phase.start();
        // Second start: isDone(1) is true since 1 >= 1
        phase.start();

        verify(game).setCurrentPlayer(Game.NO_CURRENT_PLAYER);
        verify(context).removeCachedPhase(gamephase);
    }

    // ========== Save Loop Phase Tests ==========

    @Test
    void should_UseSaveLoopPhase_When_SaveLoopPhaseSet() {
        when(gamephase.getString(GamePlayerLoopPhase.PARAM_SAVE_LOOP_PHASE, null)).thenReturn("SavedLoopPhase");
        when(game.getNumPlayers()).thenReturn(3);

        GamePlayerLoopPhase phaseWithSave = new GamePlayerLoopPhase();
        phaseWithSave.init(engine, context, gamephase);
        phaseWithSave.start();

        // First call should use the saved loop phase
        verify(context).processPhase("SavedLoopPhase", null);
    }

    @Test
    void should_UseNormalLoopPhase_When_SaveLoopPhaseConsumed() {
        when(gamephase.getString(GamePlayerLoopPhase.PARAM_SAVE_LOOP_PHASE, null)).thenReturn("SavedLoopPhase");
        when(game.getNumPlayers()).thenReturn(3);

        GamePlayerLoopPhase phaseWithSave = new GamePlayerLoopPhase();
        phaseWithSave.init(engine, context, gamephase);

        // First call uses saved loop phase
        phaseWithSave.start();
        verify(context).processPhase("SavedLoopPhase", null);

        // Second call should use normal loop phase since saved was consumed
        phaseWithSave.start();
        verify(context).processPhase("LoopPhase", null);
    }

    // ========== Test Helper ==========

    /**
     * Subclass that exposes protected methods for testing.
     */
    private static class TestableLoopPhase extends GamePlayerLoopPhase {
        boolean testIsDone(int index) {
            return isDone(index);
        }

        int testGetNextPlayerIndex(int index) {
            return getNextPlayerIndex(index);
        }

        int testGetStartingPlayerIndex() {
            return getStartingPlayerIndex();
        }

        int testAdjustProvidedStartingIndex(int index) {
            return adjustProvidedStartingIndex(index);
        }

        Object testGetLoopParams() {
            return getLoopParams();
        }

        Object testGetNextParams() {
            return getNextParams();
        }
    }
}
