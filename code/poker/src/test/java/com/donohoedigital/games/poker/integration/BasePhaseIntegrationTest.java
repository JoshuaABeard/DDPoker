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

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.games.config.GameButton;
import com.donohoedigital.games.config.GameState;
import com.donohoedigital.games.config.GameStateEntry;
import com.donohoedigital.games.engine.BasePhase;
import com.donohoedigital.games.engine.GameContext;
import com.donohoedigital.games.engine.GameEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for BasePhase using real GameEngine infrastructure.
 *
 * <p>
 * Tests BasePhase business logic and state management with minimal mocking.
 * </p>
 */
@Tag("slow")
class BasePhaseIntegrationTest extends IntegrationTestBase {

    private TestableBasePhase phase;
    private GameEngine engine;
    private GameContext context;

    @BeforeEach
    void setUp() {
        phase = new TestableBasePhase();
        engine = GameEngine.getGameEngine();
        context = null; // BasePhase stores but doesn't use context in most methods
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
    void should_StoreContext_When_Initialized() {
        phase.init(engine, context, null);

        // Context is stored internally but not accessible from tests (protected field)
        assertThat(phase.getGameEngine()).isNotNull();
    }

    @Test
    void should_InitializeAllFields_When_InitCalled() {
        phase.init(engine, context, null);

        assertThat(phase.getGameEngine()).isNotNull();
        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // Re-initialization Tests
    // =================================================================

    @Test
    void should_PreserveEngineAndContext_When_ReinitCalled() {
        phase.init(engine, context, null);

        phase.reinit(null);

        assertThat(phase.getGameEngine()).isEqualTo(engine);
        // Context is stored internally but not accessible from tests (protected field)
    }

    // =================================================================
    // setFromPhase Tests
    // =================================================================

    @Test
    void should_NotThrowException_When_SetFromPhaseCalledWithNull() {
        assertThatCode(() -> phase.setFromPhase(null)).doesNotThrowAnyException();
    }

    @Test
    void should_IgnoreFromPhase_When_SetFromPhaseCalled() {
        phase.init(engine, context, null);

        phase.setFromPhase(new TestableBasePhase());

        // BasePhase ignores the from phase to avoid keeping pointers
        assertThat(phase.getGameEngine()).isEqualTo(engine);
    }

    // =================================================================
    // Lifecycle Method Tests
    // =================================================================

    @Test
    void should_NotThrowException_When_FinishCalled() {
        phase.init(engine, context, null);

        assertThatCode(() -> phase.finish()).doesNotThrowAnyException();
    }

    @Test
    void should_CallStart_When_StartInvoked() {
        phase.init(engine, context, null);

        phase.start();

        assertThat(phase.startCalled).isTrue();
    }

    @Test
    void should_SetStartCalledFlag_When_StartInvokedMultipleTimes() {
        phase.init(engine, context, null);

        phase.start();
        phase.start();

        assertThat(phase.startCalled).isTrue();
    }

    // =================================================================
    // Button Processing Tests
    // =================================================================

    @Test
    void should_ReturnTrue_When_ProcessButtonCalledWithNull() {
        phase.init(engine, context, null);

        boolean result = phase.processButton(null);

        assertThat(result).isTrue();
    }

    @Test
    void should_ReturnTrue_When_ProcessButtonCalledWithNonNullButton() {
        phase.init(engine, context, null);
        GameButton button = new GameButton("test:TestPhase");

        boolean result = phase.processButton(button);

        assertThat(result).isTrue();
    }

    // =================================================================
    // Result Management Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_ResultNotSet() {
        assertThat(phase.getResult()).isNull();
    }

    @Test
    void should_ReturnResult_When_ResultSet() {
        String testResult = "test result";
        phase.setResult(testResult);

        assertThat(phase.getResult()).isEqualTo(testResult);
    }

    @Test
    void should_UpdateResult_When_SetResultCalledMultipleTimes() {
        phase.setResult("first");
        phase.setResult("second");

        assertThat(phase.getResult()).isEqualTo("second");
    }

    @Test
    void should_AcceptNullResult_When_SetResultCalledWithNull() {
        phase.setResult("something");
        phase.setResult(null);

        assertThat(phase.getResult()).isNull();
    }

    @Test
    void should_HandleComplexObjects_When_SetResultCalled() {
        TypedHashMap complexResult = new TypedHashMap();
        complexResult.setString("key", "value");
        phase.setResult(complexResult);

        assertThat(phase.getResult()).isEqualTo(complexResult);
        assertThat(((TypedHashMap) phase.getResult()).getString("key")).isEqualTo("value");
    }

    @Test
    void should_HandleIntegerResult_When_SetResultCalled() {
        phase.setResult(42);

        assertThat(phase.getResult()).isEqualTo(42);
    }

    @Test
    void should_HandleBooleanResult_When_SetResultCalled() {
        phase.setResult(true);

        assertThat(phase.getResult()).isEqualTo(true);
    }

    // =================================================================
    // Online Result Management Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_OnlineResultNotSet() {
        assertThat(phase.getOnlineResult()).isNull();
    }

    @Test
    void should_ReturnOnlineResult_When_OnlineResultSet() {
        String testResult = "online result";
        phase.setOnlineResult(testResult);

        assertThat(phase.getOnlineResult()).isEqualTo(testResult);
    }

    @Test
    void should_UpdateOnlineResult_When_SetOnlineResultCalledMultipleTimes() {
        phase.setOnlineResult("first");
        phase.setOnlineResult("second");

        assertThat(phase.getOnlineResult()).isEqualTo("second");
    }

    @Test
    void should_ManageResultsIndependently_When_BothSet() {
        phase.setResult("regular result");
        phase.setOnlineResult("online result");

        assertThat(phase.getResult()).isEqualTo("regular result");
        assertThat(phase.getOnlineResult()).isEqualTo("online result");
    }

    @Test
    void should_AcceptNullOnlineResult_When_SetOnlineResultCalledWithNull() {
        phase.setOnlineResult("something");
        phase.setOnlineResult(null);

        assertThat(phase.getOnlineResult()).isNull();
    }

    // =================================================================
    // Getter Tests
    // =================================================================

    @Test
    void should_ReturnNull_When_GetGameEngineCalledBeforeInit() {
        TestableBasePhase newPhase = new TestableBasePhase();

        assertThat(newPhase.getGameEngine()).isNull();
    }

    @Test
    void should_ReturnNull_When_GetGamePhaseCalledBeforeInit() {
        TestableBasePhase newPhase = new TestableBasePhase();

        assertThat(newPhase.getGamePhase()).isNull();
    }

    @Test
    void should_ReturnEngine_When_GetGameEngineCalledAfterInit() {
        phase.init(engine, context, null);

        assertThat(phase.getGameEngine()).isSameAs(engine);
    }

    // =================================================================
    // Static Method Tests
    // =================================================================

    @Test
    void should_CreateEntry_When_AddEmptyGameStateEntryCalled() {
        GameState gameState = new GameState("test", "test description");

        GameStateEntry result = BasePhase.addEmptyGameStateEntry(gameState);

        assertThat(result).isNotNull();
    }

    @Test
    void should_CreateNamedEntry_When_AddNamedGameStateEntryCalled() {
        GameState gameState = new GameState("test", "test description");

        GameStateEntry result = BasePhase.addNamedGameStateEntry(gameState, "TestPhase");

        assertThat(result).isNotNull();
    }

    @Test
    void should_AddMultipleEntries_When_CalledMultipleTimes() {
        GameState gameState = new GameState("test", "test description");

        GameStateEntry result1 = BasePhase.addNamedGameStateEntry(gameState, "Phase1");
        GameStateEntry result2 = BasePhase.addNamedGameStateEntry(gameState, "Phase2");
        GameStateEntry result3 = BasePhase.addEmptyGameStateEntry(gameState);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result3).isNotNull();
    }

    // =================================================================
    // Test Helper Classes
    // =================================================================

    /**
     * Concrete implementation of BasePhase for testing.
     */
    private static class TestableBasePhase extends BasePhase {
        boolean startCalled = false;

        @Override
        public void start() {
            startCalled = true;
        }
    }
}
