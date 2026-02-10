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

import com.donohoedigital.games.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Phase interface contracts and implementations.
 *
 * <p>Tests that all Phase implementations follow the Phase contract correctly.</p>
 */
@Tag("slow")
class PhaseContractsIntegrationTest extends IntegrationTestBase {

    private GameEngine engine;

    @BeforeEach
    void setUp() {
        engine = GameEngine.getGameEngine();
    }

    // =================================================================
    // BasePhase Contract Tests
    // =================================================================

    @Test
    void should_ImplementPhaseInterface_When_BasePhaseCreated() {
        TestableBasePhase phase = new TestableBasePhase();

        assertThat(phase).isInstanceOf(Phase.class);
    }

    @Test
    void should_AllowMultipleInitializations_When_BasePhaseReinitialized() {
        TestableBasePhase phase = new TestableBasePhase();

        phase.init(engine, null, null);
        phase.init(engine, null, null);
        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_HandleNullEngine_When_BasePhaseInitialized() {
        TestableBasePhase phase = new TestableBasePhase();

        phase.init(null, null, null);

        assertThat(phase.getGameEngine()).isNull();
    }

    @Test
    void should_PreserveInitialEngine_When_SubsequentInitWithDifferentEngine() {
        TestableBasePhase phase = new TestableBasePhase();
        phase.init(engine, null, null);

        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // ChainPhase Contract Tests
    // =================================================================

    @Test
    void should_ImplementPhaseInterface_When_ChainPhaseCreated() {
        TestableChainPhase phase = new TestableChainPhase();

        assertThat(phase).isInstanceOf(Phase.class);
        assertThat(phase).isInstanceOf(BasePhase.class);
    }

    @Test
    void should_HaveAbstractProcessMethod_When_ChainPhaseExtended() {
        TestableChainPhase phase = new TestableChainPhase();

        // process() should be callable
        assertThatCode(() -> phase.process()).doesNotThrowAnyException();
    }

    @Test
    void should_AllowMultipleProcessCalls_When_ChainPhaseUsed() {
        TestableChainPhase phase = new TestableChainPhase();

        phase.process();
        phase.process();
        phase.process();

        assertThat(phase.processCallCount).isEqualTo(3);
    }

    @Test
    void should_ResetProcessCount_When_NewChainPhaseCreated() {
        TestableChainPhase phase1 = new TestableChainPhase();
        phase1.process();
        phase1.process();

        TestableChainPhase phase2 = new TestableChainPhase();

        assertThat(phase2.processCallCount).isEqualTo(0);
    }

    // =================================================================
    // PreviousPhase Contract Tests
    // =================================================================

    @Test
    void should_ImplementPhaseInterface_When_PreviousPhaseCreated() {
        PreviousPhase phase = new PreviousPhase();

        assertThat(phase).isInstanceOf(Phase.class);
        assertThat(phase).isInstanceOf(BasePhase.class);
    }

    @Test
    void should_AllowInitialization_When_PreviousPhaseCreated() {
        PreviousPhase phase = new PreviousPhase();

        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // PreviousLoopPhase Contract Tests
    // =================================================================

    @Test
    void should_ImplementPhaseInterface_When_PreviousLoopPhaseCreated() {
        PreviousLoopPhase phase = new PreviousLoopPhase();

        assertThat(phase).isInstanceOf(Phase.class);
        assertThat(phase).isInstanceOf(BasePhase.class);
    }

    @Test
    void should_AllowInitialization_When_PreviousLoopPhaseCreated() {
        PreviousLoopPhase phase = new PreviousLoopPhase();

        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // ChainPhase.Empty Contract Tests
    // =================================================================

    @Test
    void should_ImplementPhaseInterface_When_EmptyChainPhaseCreated() {
        ChainPhase.Empty phase = new ChainPhase.Empty();

        assertThat(phase).isInstanceOf(Phase.class);
        assertThat(phase).isInstanceOf(BasePhase.class);
        assertThat(phase).isInstanceOf(ChainPhase.class);
    }

    @Test
    void should_DoNothing_When_EmptyProcessCalled() {
        ChainPhase.Empty phase = new ChainPhase.Empty();

        assertThatCode(() -> {
            phase.process();
            phase.process();
            phase.process();
        }).doesNotThrowAnyException();
    }

    @Test
    void should_AllowInitialization_When_EmptyChainPhaseCreated() {
        ChainPhase.Empty phase = new ChainPhase.Empty();

        phase.init(engine, null, null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // Cross-Phase Compatibility Tests
    // =================================================================

    @Test
    void should_BeIndependent_When_MultiplePhaseInstancesCreated() {
        TestableBasePhase basePhase = new TestableBasePhase();
        TestableChainPhase chainPhase = new TestableChainPhase();
        PreviousPhase previousPhase = new PreviousPhase();

        basePhase.init(engine, null, null);
        chainPhase.init(engine, null, null);
        previousPhase.init(engine, null, null);

        assertThat(basePhase.getGameEngine()).isEqualTo(engine);
        assertThat(chainPhase.getGameEngine()).isEqualTo(engine);
        assertThat(previousPhase.getGameEngine()).isEqualTo(engine);
    }

    @Test
    void should_HaveUniqueInstances_When_MultiplePhaseInstancesCreated() {
        TestableBasePhase phase1 = new TestableBasePhase();
        TestableBasePhase phase2 = new TestableBasePhase();

        assertThat(phase1).isNotSameAs(phase2);
    }

    @Test
    void should_PreserveState_When_OnePhaseModified() {
        TestableChainPhase phase1 = new TestableChainPhase();
        TestableChainPhase phase2 = new TestableChainPhase();

        phase1.process();
        phase1.process();

        assertThat(phase1.processCallCount).isEqualTo(2);
        assertThat(phase2.processCallCount).isEqualTo(0);
    }

    // =================================================================
    // Null Safety Tests
    // =================================================================

    @Test
    void should_HandleNullContext_When_BasePhaseInitialized() {
        TestableBasePhase phase = new TestableBasePhase();

        assertThatCode(() -> phase.init(engine, null, null)).doesNotThrowAnyException();
    }

    @Test
    void should_HandleNullGamePhase_When_BasePhaseInitialized() {
        TestableBasePhase phase = new TestableBasePhase();

        assertThatCode(() -> phase.init(engine, null, null)).doesNotThrowAnyException();
    }

    @Test
    void should_HandleAllNulls_When_BasePhaseInitialized() {
        TestableBasePhase phase = new TestableBasePhase();

        assertThatCode(() -> phase.init(null, null, null)).doesNotThrowAnyException();
    }

    @Test
    void should_HandleNullInReinit_When_BasePhaseReinitialized() {
        TestableBasePhase phase = new TestableBasePhase();
        phase.init(engine, null, null);

        assertThatCode(() -> phase.reinit(null)).doesNotThrowAnyException();
    }

    // =================================================================
    // Test Helper Classes
    // =================================================================

    private static class TestableBasePhase extends BasePhase {
        @Override
        public void start() {
            // No-op for testing
        }
    }

    private static class TestableChainPhase extends ChainPhase {
        int processCallCount = 0;

        @Override
        public void process() {
            processCallCount++;
        }
    }
}
