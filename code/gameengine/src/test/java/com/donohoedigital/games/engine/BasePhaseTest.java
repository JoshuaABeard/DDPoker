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
package com.donohoedigital.games.engine;

import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.config.GamePhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for BasePhase - Abstract base class for game phases.
 */
@ExtendWith(MockitoExtension.class)
class BasePhaseTest {

    @Mock
    private GameEngine engine;

    @Mock
    private GameContext context;

    @Mock
    private GamePhase gamephase;

    private TestPhase phase;

    @BeforeEach
    void setUp() {
        phase = new TestPhase();
    }

    // ========== init Tests ==========

    @Test
    void should_StoreEngine_When_InitCalled() {
        phase.init(engine, context, gamephase);

        assertThat(phase.getGameEngine()).isSameAs(engine);
    }

    @Test
    void should_StoreContext_When_InitCalled() {
        phase.init(engine, context, gamephase);

        assertThat(phase.context_).isSameAs(context);
    }

    @Test
    void should_StoreGamePhase_When_InitCalled() {
        phase.init(engine, context, gamephase);

        assertThat(phase.getGamePhase()).isSameAs(gamephase);
    }

    // ========== reinit Tests ==========

    @Test
    void should_UpdateGamePhase_When_ReinitCalled() {
        phase.init(engine, context, gamephase);
        GamePhase newGamePhase = mock(GamePhase.class);

        phase.reinit(newGamePhase);

        assertThat(phase.getGamePhase()).isSameAs(newGamePhase);
    }

    // ========== Result Tests ==========

    @Test
    void should_ReturnNull_When_ResultNotSet() {
        assertThat(phase.getResult()).isNull();
    }

    @Test
    void should_ReturnValue_When_ResultSet() {
        phase.setResult("test-result");

        assertThat(phase.getResult()).isEqualTo("test-result");
    }

    @Test
    void should_ReturnNull_When_OnlineResultNotSet() {
        assertThat(phase.getOnlineResult()).isNull();
    }

    @Test
    void should_ReturnValue_When_OnlineResultSet() {
        phase.setOnlineResult(42);

        assertThat(phase.getOnlineResult()).isEqualTo(42);
    }

    // ========== processButton Tests ==========

    @Test
    void should_ReturnTrue_When_ProcessButtonCalled() {
        GameButton button = mock(GameButton.class);

        assertThat(phase.processButton(button)).isTrue();
    }

    // ========== setFromPhase Tests ==========

    @Test
    void should_NotThrow_When_SetFromPhaseCalled() {
        Phase otherPhase = mock(Phase.class);

        assertThatCode(() -> phase.setFromPhase(otherPhase)).doesNotThrowAnyException();
    }

    // ========== finish Tests ==========

    @Test
    void should_NotThrow_When_FinishCalled() {
        assertThatCode(() -> phase.finish()).doesNotThrowAnyException();
    }

    // ========== toString Tests ==========

    @Test
    void should_ReturnPhaseNameAndClassName_When_ToStringCalled() {
        when(gamephase.getName()).thenReturn("TestPhaseName");
        phase.init(engine, context, gamephase);

        String result = phase.toString();

        assertThat(result).contains("TestPhaseName");
        assertThat(result).contains(TestPhase.class.getName());
        assertThat(result).isEqualTo("TestPhaseName: " + TestPhase.class.getName());
    }

    // ========== Test Helper ==========

    /**
     * Minimal concrete subclass of BasePhase for testing.
     */
    private static class TestPhase extends BasePhase {
        @Override
        public void start() {
            // no-op
        }
    }
}
