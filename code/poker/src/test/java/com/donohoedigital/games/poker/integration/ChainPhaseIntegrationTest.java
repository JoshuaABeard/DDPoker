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
package com.donohoedigital.games.poker.integration;

import com.donohoedigital.games.engine.ChainPhase;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ChainPhase using real GameEngine infrastructure.
 *
 * <p>
 * Tests ChainPhase chaining logic and phase processing with minimal mocking.
 * </p>
 */
@Tag("slow")
class ChainPhaseIntegrationTest extends IntegrationTestBase {

    private TestableChainPhase phase;
    private GameEngine engine;
    private GameContext context;

    @BeforeEach
    void setUp() {
        phase = new TestableChainPhase();
        engine = GameEngine.getGameEngine();
        context = null; // Will be set up per test
    }

    // =================================================================
    // Constants Tests
    // =================================================================

    @Test
    void should_HaveCorrectNextPhaseNoneConstant() {
        assertThat(ChainPhase.NEXT_PHASE_NONE).isEqualTo("NONE");
    }

    @Test
    void should_HaveCorrectParamNextPhaseConstant() {
        assertThat(ChainPhase.PARAM_NEXT_PHASE).isEqualTo("next-phase");
    }

    @Test
    void should_HaveCorrectParamNextPhaseParamsConstant() {
        assertThat(ChainPhase.PARAM_NEXT_PHASE_PARAMS).isEqualTo("next-phase-params");
    }

    // =================================================================
    // Inner Class Tests
    // =================================================================

    @Test
    void should_HaveEmptyInnerClass() {
        ChainPhase.Empty empty = new ChainPhase.Empty();

        assertThat(empty).isNotNull();
        assertThat(empty).isInstanceOf(ChainPhase.class);
    }

    @Test
    void should_ProcessWithoutError_When_EmptyProcessCalled() {
        ChainPhase.Empty empty = new ChainPhase.Empty();

        assertThatCode(() -> empty.process()).doesNotThrowAnyException();
    }

    // =================================================================
    // Process Method Tests
    // =================================================================

    @Test
    void should_CallProcess_When_AbstractMethodImplemented() {
        phase.process();

        assertThat(phase.processCalled).isTrue();
    }

    @Test
    void should_CallProcessMultipleTimes_When_InvokedRepeatedly() {
        phase.process();
        phase.process();
        phase.process();

        assertThat(phase.processCallCount).isEqualTo(3);
    }

    // =================================================================
    // Initialization Tests
    // =================================================================

    @Test
    void should_StoreEngine_When_Initialized() {
        phase.init(engine, context, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_InitializeWithoutError_When_InitCalled() {
        assertThatCode(() -> phase.init(engine, context, null)).doesNotThrowAnyException();
    }

    // =================================================================
    // Test Helper Classes
    // =================================================================

    /**
     * Concrete implementation of ChainPhase for testing.
     */
    private static class TestableChainPhase extends ChainPhase {
        boolean processCalled = false;
        int processCallCount = 0;

        @Override
        public void process() {
            processCalled = true;
            processCallCount++;
        }
    }
}
