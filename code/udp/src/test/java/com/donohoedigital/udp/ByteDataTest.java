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
package com.donohoedigital.udp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ByteDataTest {

    @Test
    void should_ReturnSameByteArray_When_ConstructedWithArray() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteData byteData = new ByteData(data, 0, 5);
        assertThat(byteData.getBytes()).isSameAs(data);
    }

    @Test
    void should_ReturnOffset_When_ConstructedWithOffset() {
        byte[] data = {10, 20, 30};
        ByteData byteData = new ByteData(data, 1, 2);
        assertThat(byteData.getOffest()).isEqualTo(1);
    }

    @Test
    void should_ReturnLength_When_ConstructedWithLength() {
        byte[] data = {10, 20, 30};
        ByteData byteData = new ByteData(data, 1, 2);
        assertThat(byteData.getLength()).isEqualTo(2);
    }

    @Test
    void should_ReturnZeroOffsetAndEmptyLength_When_ConstructedWithEmptyArray() {
        byte[] data = new byte[0];
        ByteData byteData = new ByteData(data, 0, 0);
        assertThat(byteData.getBytes()).isEmpty();
        assertThat(byteData.getOffest()).isZero();
        assertThat(byteData.getLength()).isZero();
    }

    @Test
    void should_ReturnFullArrayLength_When_OffsetIsZeroAndLengthMatchesArray() {
        byte[] data = {5, 10, 15, 20};
        ByteData byteData = new ByteData(data, 0, 4);
        assertThat(byteData.getOffest()).isZero();
        assertThat(byteData.getLength()).isEqualTo(4);
    }

    @Test
    void should_ReturnPartialLength_When_OffsetAndLengthDescribeSlice() {
        byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
        ByteData byteData = new ByteData(data, 2, 4);
        assertThat(byteData.getOffest()).isEqualTo(2);
        assertThat(byteData.getLength()).isEqualTo(4);
    }

    @Test
    void should_ReturnNullBytes_When_ConstructedWithNullArray() {
        ByteData byteData = new ByteData(null, 0, 0);
        assertThat(byteData.getBytes()).isNull();
    }

    @Test
    void should_ReturnLargeOffset_When_ConstructedWithLargeOffset() {
        byte[] data = new byte[100];
        ByteData byteData = new ByteData(data, 99, 1);
        assertThat(byteData.getOffest()).isEqualTo(99);
        assertThat(byteData.getLength()).isEqualTo(1);
    }
}
