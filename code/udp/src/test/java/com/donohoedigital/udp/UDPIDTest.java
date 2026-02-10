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
package com.donohoedigital.udp;

import com.donohoedigital.base.ApplicationError;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for UDPID - 36-character UUID validation and byte conversion.
 */
class UDPIDTest {

    private static final String VALID_ID = "12345678-1234-1234-1234-123456789012";
    private static final String ANOTHER_ID = "abcdefgh-abcd-abcd-abcd-abcdefghijkl";

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateID_When_ValidStringProvided() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id.toString()).isEqualTo(VALID_ID);
        assertThat(id.isUnknown()).isFalse();
    }

    @Test
    void should_ThrowException_When_StringTooShort() {
        String shortId = "12345678-1234-1234-1234-12345678901"; // 35 chars

        assertThatThrownBy(() -> new UDPID(shortId))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_StringTooLong() {
        String longId = "12345678-1234-1234-1234-1234567890123"; // 37 chars

        assertThatThrownBy(() -> new UDPID(longId))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_ThrowException_When_StringEmpty() {
        assertThatThrownBy(() -> new UDPID(""))
                .isInstanceOf(ApplicationError.class);
    }

    @Test
    void should_CreateID_When_ValidByteBufferProvided() {
        ByteBuffer buffer = ByteBuffer.wrap(VALID_ID.getBytes());
        UDPID id = new UDPID(buffer);

        assertThat(id.toString()).isEqualTo(VALID_ID);
        assertThat(id.isUnknown()).isFalse();
    }

    // =================================================================
    // UNKNOWN_ID Tests
    // =================================================================

    @Test
    void should_HaveValidLength_When_UnknownID() {
        UDPID id = UDPID.UNKNOWN_ID;

        assertThat(id.toString()).hasSize(36);
        assertThat(id.isUnknown()).isTrue();
    }

    @Test
    void should_ReturnTrue_When_UnknownIDChecked() {
        assertThat(UDPID.UNKNOWN_ID.isUnknown()).isTrue();
    }

    @Test
    void should_ReturnFalse_When_NonUnknownIDChecked() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id.isUnknown()).isFalse();
    }

    // =================================================================
    // toString Tests
    // =================================================================

    @Test
    void should_ReturnOriginalString_When_ToStringCalled() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id.toString()).isEqualTo(VALID_ID);
    }

    @Test
    void should_PreserveFormat_When_ToStringCalled() {
        String idWithDashes = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        UDPID id = new UDPID(idWithDashes);

        assertThat(id.toString()).isEqualTo(idWithDashes);
    }

    // =================================================================
    // toBytes Tests
    // =================================================================

    @Test
    void should_ConvertToBytes_When_ToBytesCalledFirstTime() {
        UDPID id = new UDPID(VALID_ID);

        byte[] bytes = id.toBytes();

        assertThat(bytes).hasSize(36);
        assertThat(new String(bytes)).isEqualTo(VALID_ID);
    }

    @Test
    void should_ReturnCachedBytes_When_ToBytesCalledMultipleTimes() {
        UDPID id = new UDPID(VALID_ID);

        byte[] bytes1 = id.toBytes();
        byte[] bytes2 = id.toBytes();

        assertThat(bytes1).isSameAs(bytes2); // Same object reference (cached)
    }

    @Test
    void should_ConvertCorrectly_When_BytesToStringRoundTrip() {
        UDPID id = new UDPID(VALID_ID);
        byte[] bytes = id.toBytes();
        String reconstructed = new String(bytes);

        assertThat(reconstructed).isEqualTo(VALID_ID);
    }

    // =================================================================
    // equals Tests
    // =================================================================

    @Test
    void should_BeEqual_When_SameInstance() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id).isEqualTo(id);
    }

    @Test
    void should_BeEqual_When_SameIDString() {
        UDPID id1 = new UDPID(VALID_ID);
        UDPID id2 = new UDPID(VALID_ID);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    void should_NotBeEqual_When_DifferentIDStrings() {
        UDPID id1 = new UDPID(VALID_ID);
        UDPID id2 = new UDPID(ANOTHER_ID);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void should_NotBeEqual_When_ComparedWithNull() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id).isNotEqualTo(null);
    }

    @Test
    void should_NotBeEqual_When_ComparedWithDifferentType() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id).isNotEqualTo(VALID_ID); // String, not UDPID
    }

    @Test
    void should_BeSymmetric_When_Equals() {
        UDPID id1 = new UDPID(VALID_ID);
        UDPID id2 = new UDPID(VALID_ID);

        assertThat(id1.equals(id2)).isTrue();
        assertThat(id2.equals(id1)).isTrue();
    }

    // =================================================================
    // hashCode Tests
    // =================================================================

    @Test
    void should_HaveSameHashCode_When_EqualIDs() {
        UDPID id1 = new UDPID(VALID_ID);
        UDPID id2 = new UDPID(VALID_ID);

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void should_HaveDifferentHashCode_When_DifferentIDs() {
        UDPID id1 = new UDPID(VALID_ID);
        UDPID id2 = new UDPID(ANOTHER_ID);

        // Note: Different IDs should typically have different hash codes,
        // but this is not guaranteed by the hash contract
        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode());
    }

    @Test
    void should_HaveConsistentHashCode_When_CalledMultipleTimes() {
        UDPID id = new UDPID(VALID_ID);

        int hash1 = id.hashCode();
        int hash2 = id.hashCode();

        assertThat(hash1).isEqualTo(hash2);
    }

    // =================================================================
    // ByteBuffer Constructor Tests
    // =================================================================

    @Test
    void should_CreateID_When_ByteBufferHasValidData() {
        ByteBuffer buffer = ByteBuffer.wrap(VALID_ID.getBytes());
        UDPID id = new UDPID(buffer);

        assertThat(id.toString()).isEqualTo(VALID_ID);
    }

    @Test
    void should_AdvanceBufferPosition_When_ReadFromByteBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(VALID_ID.getBytes());
        int initialPosition = buffer.position();

        new UDPID(buffer);

        assertThat(buffer.position()).isEqualTo(initialPosition + 36);
    }

    @Test
    void should_HandleASCIICharacters_When_ReadFromByteBuffer() {
        String asciiId = "ABCDEFGH-1234-5678-90AB-CDEFGHIJKLMN";
        ByteBuffer buffer = ByteBuffer.wrap(asciiId.getBytes());
        UDPID id = new UDPID(buffer);

        assertThat(id.toString()).isEqualTo(asciiId);
    }

    // =================================================================
    // Constant Tests
    // =================================================================

    @Test
    void should_HaveCorrectLength_When_LengthConstantChecked() {
        assertThat(UDPID.LENGTH).isEqualTo(36);
    }

    @Test
    void should_MatchConstant_When_ValidIDLength() {
        UDPID id = new UDPID(VALID_ID);

        assertThat(id.toString().length()).isEqualTo(UDPID.LENGTH);
    }

    // =================================================================
    // Edge Case Tests
    // =================================================================

    @Test
    void should_HandleAllZeros_When_ValidFormat() {
        String zeroId = "00000000-0000-0000-0000-000000000000";
        UDPID id = new UDPID(zeroId);

        assertThat(id.toString()).isEqualTo(zeroId);
        assertThat(id.isUnknown()).isFalse();
    }

    @Test
    void should_HandleAllLetters_When_ValidFormat() {
        String letterId = "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE";
        UDPID id = new UDPID(letterId);

        assertThat(id.toString()).isEqualTo(letterId);
    }

    @Test
    void should_HandleMixedCase_When_ValidFormat() {
        String mixedId = "AbCdEfGh-1234-5678-90AB-cDeF12345678";
        UDPID id = new UDPID(mixedId);

        assertThat(id.toString()).isEqualTo(mixedId);
    }

    @Test
    void should_HandleSpecialCharacters_When_ValidLength() {
        String specialId = "!@#$%^&*-()[]{}?<>:;'`~+=_.,01234567"; // Exactly 36 chars
        assertThat(specialId).hasSize(36);
        UDPID id = new UDPID(specialId);

        assertThat(id.toString()).isEqualTo(specialId);
    }
}
