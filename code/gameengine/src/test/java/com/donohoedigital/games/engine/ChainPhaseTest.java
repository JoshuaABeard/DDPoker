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

import com.donohoedigital.base.TypedHashMap;
import com.donohoedigital.games.config.GamePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ChainPhase - Abstract phase that chains to a next phase after
 * processing.
 */
@ExtendWith(MockitoExtension.class)
class ChainPhaseTest {

    @Mock
    private GameEngine engine;

    @Mock
    private GameContext context;

    @Mock
    private GamePhase gamephase;

    // ========== Constants Tests ==========

    @Test
    void should_HaveCorrectConstant_When_NextPhaseNone() {
        assertThat(ChainPhase.NEXT_PHASE_NONE).isEqualTo("NONE");
    }

    @Test
    void should_HaveCorrectConstant_When_ParamNextPhase() {
        assertThat(ChainPhase.PARAM_NEXT_PHASE).isEqualTo("next-phase");
    }

    @Test
    void should_HaveCorrectConstant_When_ParamNextPhaseParams() {
        assertThat(ChainPhase.PARAM_NEXT_PHASE_PARAMS).isEqualTo("next-phase-params");
    }

    // ========== start() Tests ==========

    @Test
    void should_CallProcessAndNextPhase_When_StartCalled() {
        when(gamephase.getString("next-phase", null)).thenReturn("SomePhase");

        TestChainPhase phase = new TestChainPhase();
        phase.init(engine, context, gamephase);
        phase.start();

        assertThat(phase.processCallCount).isEqualTo(1);
        verify(context).processPhase("SomePhase", null);
    }

    @Test
    void should_NotCallProcessPhase_When_NextPhaseIsNONE() {
        when(gamephase.getString("next-phase", null)).thenReturn("NONE");

        TestChainPhase phase = new TestChainPhase();
        phase.init(engine, context, gamephase);
        phase.start();

        assertThat(phase.processCallCount).isEqualTo(1);
        verify(context, never()).processPhase(anyString(), any());
        verify(context, never()).processPhaseNow(anyString(), any());
    }

    @Test
    void should_PassParams_When_NextPhaseHasParams() {
        TypedHashMap params = new TypedHashMap();
        params.setString("key", "value");
        when(gamephase.getString("next-phase", null)).thenReturn("NextPhase");
        when(gamephase.getObject("next-phase-params")).thenReturn(params);

        TestChainPhase phase = new TestChainPhase();
        phase.init(engine, context, gamephase);
        phase.start();

        verify(context).processPhase("NextPhase", params);
    }

    @Test
    void should_PassNullParams_When_NextPhaseParamsNotSet() {
        when(gamephase.getString("next-phase", null)).thenReturn("NextPhase");
        when(gamephase.getObject("next-phase-params")).thenReturn(null);

        TestChainPhase phase = new TestChainPhase();
        phase.init(engine, context, gamephase);
        phase.start();

        verify(context).processPhase("NextPhase", null);
    }

    // ========== nextPhaseNow() Tests ==========

    @Test
    void should_CallProcessPhaseNow_When_NextPhaseNowCalled() {
        when(gamephase.getString("next-phase", null)).thenReturn("ImmediatePhase");
        when(gamephase.getObject("next-phase-params")).thenReturn(null);

        NextPhaseNowTestChainPhase phase = new NextPhaseNowTestChainPhase();
        phase.init(engine, context, gamephase);
        phase.start();

        verify(context).processPhaseNow("ImmediatePhase", null);
    }

    @Test
    void should_NotCallProcessPhaseNow_When_NextPhaseIsNONEAndNowCalled() {
        when(gamephase.getString("next-phase", null)).thenReturn("NONE");

        NextPhaseNowTestChainPhase phase = new NextPhaseNowTestChainPhase();
        phase.init(engine, context, gamephase);
        phase.start();

        verify(context, never()).processPhaseNow(anyString(), any());
    }

    // ========== Empty Inner Class Tests ==========

    @Test
    void should_DoNothing_When_EmptyProcessCalled() {
        when(gamephase.getString("next-phase", null)).thenReturn("NONE");

        ChainPhase.Empty emptyPhase = new ChainPhase.Empty();
        emptyPhase.init(engine, context, gamephase);

        assertThatCode(emptyPhase::start).doesNotThrowAnyException();
    }

    @Test
    void should_StillChainToNextPhase_When_EmptyStartCalled() {
        when(gamephase.getString("next-phase", null)).thenReturn("AfterEmpty");
        when(gamephase.getObject("next-phase-params")).thenReturn(null);

        ChainPhase.Empty emptyPhase = new ChainPhase.Empty();
        emptyPhase.init(engine, context, gamephase);
        emptyPhase.start();

        verify(context).processPhase("AfterEmpty", null);
    }

    // ========== Test Helpers ==========

    /**
     * Concrete subclass that tracks process() calls and uses default nextPhase()
     * (invokeLater).
     */
    private static class TestChainPhase extends ChainPhase {
        int processCallCount = 0;

        @Override
        public void process() {
            processCallCount++;
        }
    }

    /**
     * Concrete subclass that calls nextPhaseNow() instead of nextPhase() in
     * process().
     */
    private static class NextPhaseNowTestChainPhase extends ChainPhase {
        @Override
        public void start() {
            process();
            nextPhaseNow();
        }

        @Override
        public void process() {
            // no-op
        }
    }
}
