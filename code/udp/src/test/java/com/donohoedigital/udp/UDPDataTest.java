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

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for UDPData - UDP datagram wrapper with serialization and state
 * tracking.
 */
class UDPDataTest {

    // =================================================================
    // Type Enum Tests
    // =================================================================

    @Test
    void should_HaveSixValues_When_TypeEnumExamined() {
        assertThat(UDPData.Type.values()).hasSize(6);
    }

    @Test
    void should_ReturnCorrectName_When_TypeToStringCalled() {
        assertThat(UDPData.Type.PING_ACK.toString()).isEqualTo("ping-ack");
        assertThat(UDPData.Type.MESSAGE.toString()).isEqualTo("message");
        assertThat(UDPData.Type.HELLO.toString()).isEqualTo("hello");
        assertThat(UDPData.Type.GOODBYE.toString()).isEqualTo("good-bye");
        assertThat(UDPData.Type.MTU_TEST.toString()).isEqualTo("mtu-test");
        assertThat(UDPData.Type.MTU_ACK.toString()).isEqualTo("mtu-ack");
    }

    @Test
    void should_ReturnTypeByOrdinal_When_GetMatchCalledWithValidIndex() {
        assertThat(UDPData.Type.getMatch(0)).isEqualTo(UDPData.Type.PING_ACK);
        assertThat(UDPData.Type.getMatch(1)).isEqualTo(UDPData.Type.MESSAGE);
        assertThat(UDPData.Type.getMatch(2)).isEqualTo(UDPData.Type.HELLO);
        assertThat(UDPData.Type.getMatch(3)).isEqualTo(UDPData.Type.GOODBYE);
        assertThat(UDPData.Type.getMatch(4)).isEqualTo(UDPData.Type.MTU_TEST);
        assertThat(UDPData.Type.getMatch(5)).isEqualTo(UDPData.Type.MTU_ACK);
    }

    @Test
    void should_ThrowException_When_GetMatchCalledWithNegativeOrdinal() {
        assertThatThrownBy(() -> UDPData.Type.getMatch(-1)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void should_ThrowException_When_GetMatchCalledWithOrdinalTooLarge() {
        assertThatThrownBy(() -> UDPData.Type.getMatch(6)).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("6");
    }

    // =================================================================
    // Construction from byte array Tests
    // =================================================================

    @Test
    void should_StoreAllFields_When_ConstructedFromByteArray() {
        byte[] payload = "hello".getBytes();
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 42, (short) 1, (short) 1, payload, 0, payload.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        assertThat(data.getType()).isEqualTo(UDPData.Type.MESSAGE);
        assertThat(data.getID()).isEqualTo(42);
        assertThat(data.getPartID()).isEqualTo((short) 1);
        assertThat(data.getNumParts()).isEqualTo((short) 1);
        assertThat(data.getLength()).isEqualTo(5);
        assertThat(data.getUserType()).isEqualTo(UDPData.USER_TYPE_UNSPECIFIED);
        assertThat(data.getData()).isSameAs(payload);
        assertThat(data.getOffset()).isEqualTo(0);
    }

    @Test
    void should_StoreOffset_When_ConstructedWithNonZeroOffset() {
        byte[] payload = "XYhelloXY".getBytes();
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, payload, 2, 5,
                UDPData.USER_TYPE_UNSPECIFIED);

        assertThat(data.getOffset()).isEqualTo(2);
        assertThat(data.getLength()).isEqualTo(5);
    }

    // =================================================================
    // HEADER_SIZE constant Tests
    // =================================================================

    @Test
    void should_HaveCorrectHeaderSize_When_ConstantChecked() {
        // 1 (type) + 1 (send count) + 4 (id) + 4 (length) + 2+2 (partid, #parts) + 1
        // (user type) = 15
        assertThat(UDPData.HEADER_SIZE).isEqualTo(15);
    }

    // =================================================================
    // getBufferedLength Tests
    // =================================================================

    @Test
    void should_ReturnHeaderSizePlusPayload_When_GetBufferedLengthCalled() {
        byte[] payload = "hello".getBytes();
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, payload, 0, payload.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        assertThat(data.getBufferedLength()).isEqualTo(UDPData.HEADER_SIZE + 5);
    }

    @Test
    void should_ReturnJustHeaderSize_When_EmptyPayload() {
        UDPData data = new UDPData(UDPData.Type.PING_ACK, 0, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        assertThat(data.getBufferedLength()).isEqualTo(UDPData.HEADER_SIZE);
    }

    // =================================================================
    // Serialization Round-trip Tests (put / ByteBuffer constructor)
    // =================================================================

    @Test
    void should_SerializeAndDeserialize_When_PutAndReadFromBuffer() {
        byte[] payload = "hello".getBytes();
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, payload, 0, payload.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        ByteBuffer buffer = ByteBuffer.allocate(data.getBufferedLength());
        data.put(buffer);
        buffer.flip();

        UDPData restored = new UDPData(buffer);
        assertThat(restored.getType()).isEqualTo(UDPData.Type.MESSAGE);
        assertThat(restored.getID()).isEqualTo(1);
        assertThat(restored.getLength()).isEqualTo(5);
    }

    @Test
    void should_PreservePayloadBytes_When_RoundTripped() {
        byte[] payload = "test-payload".getBytes();
        UDPData data = new UDPData(UDPData.Type.HELLO, 99, (short) 2, (short) 3, payload, 0, payload.length, (byte) 7);

        ByteBuffer buffer = ByteBuffer.allocate(data.getBufferedLength());
        data.put(buffer);
        buffer.flip();

        UDPData restored = new UDPData(buffer);
        assertThat(restored.getType()).isEqualTo(UDPData.Type.HELLO);
        assertThat(restored.getID()).isEqualTo(99);
        assertThat(restored.getPartID()).isEqualTo((short) 2);
        assertThat(restored.getNumParts()).isEqualTo((short) 3);
        assertThat(restored.getUserType()).isEqualTo((byte) 7);
        assertThat(new String(restored.getData())).isEqualTo("test-payload");
    }

    @Test
    void should_IncrementSendCount_When_DeserializedFromBuffer() {
        // The receiver side increments sendCnt by 1 on deserialization (send-count + 1)
        byte[] payload = "data".getBytes();
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 5, (short) 1, (short) 1, payload, 0, payload.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        ByteBuffer buffer = ByteBuffer.allocate(data.getBufferedLength());
        data.put(buffer);
        buffer.flip();

        UDPData restored = new UDPData(buffer);
        // sendCnt was 0 on the sender; receiver adds 1, so result is 1
        assertThat(restored.getSendCount()).isEqualTo(1);
    }

    @Test
    void should_HandleEmptyPayload_When_RoundTripped() {
        UDPData data = new UDPData(UDPData.Type.PING_ACK, 0, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        ByteBuffer buffer = ByteBuffer.allocate(data.getBufferedLength());
        data.put(buffer);
        buffer.flip();

        UDPData restored = new UDPData(buffer);
        assertThat(restored.getType()).isEqualTo(UDPData.Type.PING_ACK);
        assertThat(restored.getLength()).isEqualTo(0);
        assertThat(restored.getData()).isNull();
    }

    @Test
    void should_SerializeAllTypeValues_When_PutAndReadFromBuffer() {
        for (UDPData.Type type : UDPData.Type.values()) {
            UDPData data = new UDPData(type, 1, (short) 1, (short) 1, new byte[0], 0, 0, UDPData.USER_TYPE_UNSPECIFIED);
            ByteBuffer buffer = ByteBuffer.allocate(data.getBufferedLength());
            data.put(buffer);
            buffer.flip();

            UDPData restored = new UDPData(buffer);
            assertThat(restored.getType()).isEqualTo(type);
        }
    }

    // =================================================================
    // combine() Tests
    // =================================================================

    @Test
    void should_ConcatenatePayloads_When_CombineCalledWithSingleChunk() {
        byte[] part1 = "hello".getBytes();
        byte[] part2 = " world".getBytes();
        UDPData first = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 2, part1, 0, part1.length,
                UDPData.USER_TYPE_UNSPECIFIED);
        UDPData second = new UDPData(UDPData.Type.MESSAGE, 1, (short) 2, (short) 2, part2, 0, part2.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        ArrayList<UDPData> rest = new ArrayList<>();
        rest.add(second);
        first.combine(rest);

        assertThat(first.getLength()).isEqualTo(11);
        assertThat(new String(first.getData())).isEqualTo("hello world");
    }

    @Test
    void should_ConcatenateAllParts_When_CombineCalledWithMultipleChunks() {
        byte[] p1 = "AAA".getBytes();
        byte[] p2 = "BBB".getBytes();
        byte[] p3 = "CCC".getBytes();
        UDPData first = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 3, p1, 0, p1.length,
                UDPData.USER_TYPE_UNSPECIFIED);
        UDPData second = new UDPData(UDPData.Type.MESSAGE, 1, (short) 2, (short) 3, p2, 0, p2.length,
                UDPData.USER_TYPE_UNSPECIFIED);
        UDPData third = new UDPData(UDPData.Type.MESSAGE, 1, (short) 3, (short) 3, p3, 0, p3.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        ArrayList<UDPData> rest = new ArrayList<>();
        rest.add(second);
        rest.add(third);
        first.combine(rest);

        assertThat(first.getLength()).isEqualTo(9);
        assertThat(new String(first.getData())).isEqualTo("AAABBBCCC");
    }

    @Test
    void should_CombineWithOffset_When_FirstChunkHasNonZeroOffset() {
        byte[] p1 = "XXhelloXX".getBytes();
        byte[] p2 = " world".getBytes();
        UDPData first = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 2, p1, 2, 5,
                UDPData.USER_TYPE_UNSPECIFIED);
        UDPData second = new UDPData(UDPData.Type.MESSAGE, 1, (short) 2, (short) 2, p2, 0, p2.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        ArrayList<UDPData> rest = new ArrayList<>();
        rest.add(second);
        first.combine(rest);

        assertThat(first.getLength()).isEqualTo(11);
        assertThat(new String(first.getData())).isEqualTo("hello world");
    }

    // =================================================================
    // State Transition Tests (sent / resend / queued)
    // =================================================================

    @Test
    void should_BeNotSentAndNotQueued_When_NewlyCreated() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        assertThat(data.isSent()).isFalse();
        assertThat(data.isQueued()).isFalse();
        assertThat(data.getSendCount()).isEqualTo(0);
    }

    @Test
    void should_BeQueued_When_QueuedCalled() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        data.queued();

        assertThat(data.isQueued()).isTrue();
        assertThat(data.isSent()).isFalse();
    }

    @Test
    void should_BeSentAndNotQueued_When_SentCalled() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);
        data.queued();

        data.sent();

        assertThat(data.isSent()).isTrue();
        assertThat(data.isQueued()).isFalse();
        assertThat(data.getSendCount()).isEqualTo(1);
    }

    @Test
    void should_IncrementSendCount_When_SentCalledMultipleTimes() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        data.sent();
        data.sent();
        data.sent();

        assertThat(data.getSendCount()).isEqualTo(3);
    }

    @Test
    void should_BeNotSent_When_ResendCalledAfterSent() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);
        data.sent();

        data.resend();

        assertThat(data.isSent()).isFalse();
    }

    @Test
    void should_ResetElapsed_When_ResendCalledAfterSent() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);
        data.sent();

        data.resend();

        assertThat(data.elapsed()).isEqualTo(0);
    }

    // =================================================================
    // elapsed() Tests
    // =================================================================

    @Test
    void should_ReturnZero_When_NotYetSent() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        assertThat(data.elapsed()).isEqualTo(0);
    }

    @Test
    void should_ReturnNonNegative_When_SentAndElapsedCalled() {
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 1, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        data.sent();

        assertThat(data.elapsed()).isGreaterThanOrEqualTo(0);
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ContainTypeAndId_When_ToStringCalled() {
        byte[] payload = "x".getBytes();
        UDPData data = new UDPData(UDPData.Type.MESSAGE, 7, (short) 1, (short) 1, payload, 0, payload.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        String s = data.toString();
        assertThat(s).contains("message");
        assertThat(s).contains("7");
    }

    @Test
    void should_ContainTypeAndId_When_ToStringShortCalled() {
        byte[] payload = "x".getBytes();
        UDPData data = new UDPData(UDPData.Type.HELLO, 3, (short) 1, (short) 2, payload, 0, payload.length,
                UDPData.USER_TYPE_UNSPECIFIED);

        String s = data.toStringShort();
        assertThat(s).contains("hello");
        assertThat(s).contains("3");
    }

    @Test
    void should_ContainTypeAndId_When_ToStringTypeCalled() {
        UDPData data = new UDPData(UDPData.Type.GOODBYE, 99, (short) 1, (short) 1, new byte[0], 0, 0,
                UDPData.USER_TYPE_UNSPECIFIED);

        String s = data.toStringType();
        assertThat(s).contains("good-bye");
        assertThat(s).contains("99");
    }
}
