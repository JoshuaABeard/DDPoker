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

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for AckList - tracks ranges of acknowledged UDP message IDs.
 */
class AckListTest {

    // =================================================================
    // Constructor / accessors
    // =================================================================

    @Test
    void should_StoreSessionId_When_Constructed() {
        AckList list = new AckList(42L);

        assertThat(list.getSessionID()).isEqualTo(42L);
    }

    @Test
    void should_BeEmpty_When_NewlyConstructed() {
        AckList list = new AckList(1L);

        assertThat(list.size()).isEqualTo(0);
    }

    @Test
    void should_ReturnEmptyString_When_NoAcksAdded() {
        AckList list = new AckList(1L);

        assertThat(list.toString()).isEqualTo("[empty]");
    }

    // =================================================================
    // Single ack
    // =================================================================

    @Test
    void should_HaveSizeOne_When_SingleIdAcked() {
        AckList list = new AckList(1L);

        list.ack(10);

        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    void should_ContainId_When_IdHasBeenAcked() {
        AckList list = new AckList(1L);
        list.ack(10);

        UDPData data = makeData(10);

        assertThat(list.contains(data)).isTrue();
    }

    @Test
    void should_NotContainId_When_IdHasNotBeenAcked() {
        AckList list = new AckList(1L);
        list.ack(10);

        UDPData data = makeData(99);

        assertThat(list.contains(data)).isFalse();
    }

    // =================================================================
    // Duplicate detection
    // =================================================================

    @Test
    void should_NotGrowList_When_SameIdAckedTwice() {
        AckList list = new AckList(1L);
        list.ack(5);
        list.ack(5);

        assertThat(list.size()).isEqualTo(1);
    }

    // =================================================================
    // Range merging — consecutive IDs
    // =================================================================

    @Test
    void should_MergeIntoOneRange_When_ConsecutiveIdsAckedInOrder() {
        AckList list = new AckList(1L);
        list.ack(1);
        list.ack(2);
        list.ack(3);

        // 1, 2, 3 are consecutive — must collapse into a single [1...3] node
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.toString()).isEqualTo("[1...3]");
    }

    @Test
    void should_MergeIntoOneRange_When_ConsecutiveIdsAckedInReverseOrder() {
        AckList list = new AckList(1L);
        list.ack(3);
        list.ack(2);
        list.ack(1);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.toString()).isEqualTo("[1...3]");
    }

    @Test
    void should_HaveTwoRanges_When_NonConsecutiveIdsAcked() {
        AckList list = new AckList(1L);
        list.ack(1);
        list.ack(3); // gap at 2 — two separate ranges

        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void should_BridgeTwoRanges_When_GapIdAcked() {
        AckList list = new AckList(1L);
        list.ack(1);
        list.ack(3); // two ranges: [1], [3]
        list.ack(2); // bridges the gap

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.toString()).isEqualTo("[1...3]");
    }

    // =================================================================
    // toString format
    // =================================================================

    @Test
    void should_UseSingleBracketFormat_When_RangeHasSingleId() {
        AckList list = new AckList(1L);
        list.ack(7);

        assertThat(list.toString()).isEqualTo("[7]");
    }

    @Test
    void should_UseEllipsisFormat_When_RangeHasMultipleIds() {
        AckList list = new AckList(1L);
        list.ack(4);
        list.ack(5);

        assertThat(list.toString()).isEqualTo("[4...5]");
    }

    @Test
    void should_ListMultipleRanges_When_NonContiguousIdsAcked() {
        AckList list = new AckList(1L);
        list.ack(1);
        list.ack(5);

        assertThat(list.toString()).isEqualTo("[1], [5]");
    }

    // =================================================================
    // ack via UDPData
    // =================================================================

    @Test
    void should_AckDataId_When_AckedViaUdpData() {
        AckList list = new AckList(1L);
        UDPData data = makeData(20);

        list.ack(data);

        assertThat(list.contains(data)).isTrue();
    }

    // =================================================================
    // Helpers
    // =================================================================

    private static UDPData makeData(int id) {
        return new UDPData(UDPData.Type.MESSAGE, id, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);
    }
}
