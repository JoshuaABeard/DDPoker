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
package com.donohoedigital.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Perf — static performance monitoring toggle.
 */
class PerfTest {

    @AfterEach
    void tearDown() {
        // Reset to default state after each test
        Perf.setOn(false);
    }

    // -----------------------------------------------------------------------
    // On/Off state
    // -----------------------------------------------------------------------

    @Test
    void should_BeOffByDefault_When_NotExplicitlyEnabled() {
        assertThat(Perf.isOn()).isFalse();
    }

    @Test
    void should_BeOn_When_SetOnTrue() {
        Perf.setOn(true);
        assertThat(Perf.isOn()).isTrue();
    }

    @Test
    void should_BeOff_When_SetOnFalse() {
        Perf.setOn(true);
        Perf.setOn(false);
        assertThat(Perf.isOn()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Start / Stop
    // -----------------------------------------------------------------------

    @Test
    void should_BeStartedByDefault_When_NotStopped() {
        assertThat(Perf.isStarted()).isTrue();
    }

    @Test
    void should_StopGathering_When_StopCalledWhileOn() {
        Perf.setOn(true);
        Perf.stop();
        assertThat(Perf.isStarted()).isFalse();
    }

    @Test
    void should_RestartGathering_When_StartCalledAfterStop() {
        Perf.setOn(true);
        Perf.stop();
        Perf.start();
        assertThat(Perf.isStarted()).isTrue();
    }

    @Test
    void should_DoNothing_When_StopCalledWhileOff() {
        // When OFF, stop is a no-op
        Perf.stop();
        assertThat(Perf.isStarted()).isTrue();
    }

    @Test
    void should_DoNothing_When_StartCalledWhileAlreadyRunning() {
        Perf.setOn(true);
        Perf.start(); // already running, should be no-op
        assertThat(Perf.isStarted()).isTrue();
    }
}
