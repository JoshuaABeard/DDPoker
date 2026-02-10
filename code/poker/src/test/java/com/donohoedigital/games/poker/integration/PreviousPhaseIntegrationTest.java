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
package com.donohoedigital.games.poker.integration;

import com.donohoedigital.games.engine.BasePhase;
import com.donohoedigital.games.engine.GameEngine;
import com.donohoedigital.games.engine.PreviousLoopPhase;
import com.donohoedigital.games.engine.PreviousPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PreviousPhase and PreviousLoopPhase using real GameEngine infrastructure.
 *
 * <p>Tests phase navigation classes with minimal mocking.</p>
 */
class PreviousPhaseIntegrationTest extends IntegrationTestBase {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = GameEngine.getGameEngine();
    }

    // =================================================================
    // PreviousPhase Tests
    // =================================================================

    @Test
    void should_CreatePreviousPhase_When_Instantiated() {
        PreviousPhase phase = new PreviousPhase();

        assertThat(phase).isNotNull();
        assertThat(phase).isInstanceOf(BasePhase.class);
    }

    @Test
    void should_InitializePreviousPhase_When_InitCalled() {
        PreviousPhase phase = new PreviousPhase();

        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_ReinitializePreviousPhase_When_ReinitCalled() {
        PreviousPhase phase = new PreviousPhase();
        phase.init(engine, null, null);

        assertThatCode(() -> phase.reinit(null)).doesNotThrowAnyException();
        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_HandleMultipleInitializations_When_CalledRepeatedly() {
        PreviousPhase phase = new PreviousPhase();

        phase.init(engine, null, null);
        phase.reinit(null);
        phase.reinit(null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // PreviousLoopPhase Tests
    // =================================================================

    @Test
    void should_CreatePreviousLoopPhase_When_Instantiated() {
        PreviousLoopPhase phase = new PreviousLoopPhase();

        assertThat(phase).isNotNull();
        assertThat(phase).isInstanceOf(BasePhase.class);
    }

    @Test
    void should_InitializePreviousLoopPhase_When_InitCalled() {
        PreviousLoopPhase phase = new PreviousLoopPhase();

        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_ReinitializePreviousLoopPhase_When_ReinitCalled() {
        PreviousLoopPhase phase = new PreviousLoopPhase();
        phase.init(engine, null, null);

        assertThatCode(() -> phase.reinit(null)).doesNotThrowAnyException();
        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_HandleMultipleInitializations_When_PreviousLoopPhaseCalledRepeatedly() {
        PreviousLoopPhase phase = new PreviousLoopPhase();

        phase.init(engine, null, null);
        phase.reinit(null);
        phase.reinit(null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // Comparison Tests
    // =================================================================

    @Test
    void should_BeDifferentClasses_When_ComparingPreviousPhases() {
        PreviousPhase previousPhase = new PreviousPhase();
        PreviousLoopPhase previousLoopPhase = new PreviousLoopPhase();

        assertThat(previousPhase.getClass()).isNotEqualTo(previousLoopPhase.getClass());
    }

    @Test
    void should_BothExtendBasePhase_When_CheckingInheritance() {
        PreviousPhase previousPhase = new PreviousPhase();
        PreviousLoopPhase previousLoopPhase = new PreviousLoopPhase();

        assertThat(previousPhase).isInstanceOf(BasePhase.class);
        assertThat(previousLoopPhase).isInstanceOf(BasePhase.class);
    }
}
