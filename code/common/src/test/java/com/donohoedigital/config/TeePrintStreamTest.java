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

import org.junit.jupiter.api.Test;

import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TeePrintStream — captures System.out while still passing through to
 * original.
 */
class TeePrintStreamTest {

    // -----------------------------------------------------------------------
    // Capture and restore
    // -----------------------------------------------------------------------

    @Test
    void should_CaptureOutput_When_PrintlnCalled() {
        PrintStream originalOut = System.out;
        TeePrintStream tee = new TeePrintStream();
        try {
            System.out.println("hello capture");
            String[] lines = tee.getCapturedLines();
            assertThat(lines).contains("hello capture");
        } finally {
            tee.restoreOriginal();
            assertThat(System.out).isSameAs(originalOut);
        }
    }

    @Test
    void should_CaptureMultipleLines_When_MultiplePrintlnCalled() {
        TeePrintStream tee = new TeePrintStream();
        try {
            System.out.println("line one");
            System.out.println("line two");
            String[] lines = tee.getCapturedLines();
            assertThat(lines).hasSize(2);
            assertThat(lines[0]).isEqualTo("line one");
            assertThat(lines[1]).isEqualTo("line two");
        } finally {
            tee.restoreOriginal();
        }
    }

    @Test
    void should_ReturnEmptyArray_When_NothingCaptured() {
        TeePrintStream tee = new TeePrintStream();
        try {
            String[] lines = tee.getCapturedLines();
            assertThat(lines).isEmpty();
        } finally {
            tee.restoreOriginal();
        }
    }

    @Test
    void should_RestoreOriginalOut_When_RestoreCalled() {
        PrintStream originalOut = System.out;
        TeePrintStream tee = new TeePrintStream();
        // System.out should now be different (the tee stream)
        assertThat(System.out).isNotSameAs(originalOut);
        tee.restoreOriginal();
        assertThat(System.out).isSameAs(originalOut);
    }
}
