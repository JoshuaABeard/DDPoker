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
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DDByteArrayOutputStream — exposes the internal byte buffer.
 */
class DDByteArrayOutputStreamTest {

    // -----------------------------------------------------------------------
    // Default constructor
    // -----------------------------------------------------------------------

    @Test
    void should_ReturnInternalBuffer_When_GetBufferCalled() {
        DDByteArrayOutputStream stream = new DDByteArrayOutputStream();
        byte[] buf = stream.getBuffer();
        assertThat(buf).isNotNull();
    }

    @Test
    void should_ContainWrittenData_When_DataWritten() throws IOException {
        DDByteArrayOutputStream stream = new DDByteArrayOutputStream();
        stream.write(new byte[]{1, 2, 3});

        byte[] buf = stream.getBuffer();
        assertThat(buf[0]).isEqualTo((byte) 1);
        assertThat(buf[1]).isEqualTo((byte) 2);
        assertThat(buf[2]).isEqualTo((byte) 3);
    }

    @Test
    void should_MatchToByteArray_When_DataWritten() throws IOException {
        DDByteArrayOutputStream stream = new DDByteArrayOutputStream();
        stream.write(new byte[]{10, 20, 30});

        // toByteArray returns a copy of exact size; getBuffer returns the internal
        // (possibly larger) array
        byte[] exact = stream.toByteArray();
        byte[] buf = stream.getBuffer();
        assertThat(exact).hasSize(3);
        assertThat(buf.length).isGreaterThanOrEqualTo(3);
        assertThat(buf[0]).isEqualTo(exact[0]);
        assertThat(buf[1]).isEqualTo(exact[1]);
        assertThat(buf[2]).isEqualTo(exact[2]);
    }

    // -----------------------------------------------------------------------
    // Sized constructor
    // -----------------------------------------------------------------------

    @Test
    void should_AllocateRequestedSize_When_SizedConstructor() {
        DDByteArrayOutputStream stream = new DDByteArrayOutputStream(64);
        byte[] buf = stream.getBuffer();
        assertThat(buf).hasSize(64);
    }

    @Test
    void should_GrowBuffer_When_WritingBeyondInitialSize() throws IOException {
        DDByteArrayOutputStream stream = new DDByteArrayOutputStream(2);
        stream.write(new byte[]{1, 2, 3, 4, 5});

        byte[] buf = stream.getBuffer();
        assertThat(buf.length).isGreaterThanOrEqualTo(5);
        assertThat(stream.toByteArray()).containsExactly(1, 2, 3, 4, 5);
    }
}
