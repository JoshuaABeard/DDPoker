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
package com.donohoedigital.comms;

import com.donohoedigital.base.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DDMessage - the primary message container used for client-server
 * communication, supporting params and data chunks.
 */
class DDMessageTest {

    private Version savedVersion;

    @BeforeEach
    void setUp() {
        savedVersion = DDMessage.getDefaultVersion();
        DDMessage.setDefaultVersion(null);
    }

    @AfterEach
    void tearDown() {
        DDMessage.setDefaultVersion(savedVersion);
    }

    // ---- Constructors ----

    @Test
    void should_CreateEmptyMessage_When_DefaultConstructed() {
        DDMessage msg = new DDMessage();

        assertThat(msg.getCategory()).isEqualTo(DDMessage.CAT_NONE);
        assertThat(msg.getNumData()).isZero();
        assertThat(msg.getCreateTimeStamp()).isGreaterThan(0);
    }

    @Test
    void should_SetCategory_When_ConstructedWithCategory() {
        DDMessage msg = new DDMessage(DDMessage.CAT_TESTING);

        assertThat(msg.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
    }

    @Test
    void should_SetCategoryAndData_When_ConstructedWithStringData() {
        DDMessage msg = new DDMessage(DDMessage.CAT_TESTING, "hello");

        assertThat(msg.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
        assertThat(msg.getNumData()).isEqualTo(1);
        assertThat(msg.getDataAsString()).isEqualTo("hello");
    }

    @Test
    void should_SetCategoryAndData_When_ConstructedWithByteData() {
        byte[] data = Utils.encode("bytes");
        DDMessage msg = new DDMessage(DDMessage.CAT_TESTING, data);

        assertThat(msg.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
        assertThat(msg.getNumData()).isEqualTo(1);
        assertThat(msg.getData()).isEqualTo(data);
    }

    // ---- Category ----

    @Test
    void should_GetAndSetCategory_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setCategory(100);

        assertThat(msg.getCategory()).isEqualTo(100);
    }

    // ---- Version ----

    @Test
    void should_SetAndGetVersion_When_Called() {
        DDMessage msg = new DDMessage();
        Version v = new Version("3.0");
        msg.setVersion(v);

        assertThat(msg.getVersion()).isSameAs(v);
    }

    @Test
    void should_UseDefaultVersion_When_Constructed() {
        Version v = new Version("2.0");
        DDMessage.setDefaultVersion(v);

        DDMessage msg = new DDMessage();

        assertThat(msg.getVersion()).isSameAs(v);
    }

    // ---- Status ----

    @Test
    void should_DefaultToStatusNone_When_Created() {
        DDMessage msg = new DDMessage();

        assertThat(msg.getStatus()).isEqualTo(DDMessageListener.STATUS_NONE);
    }

    @Test
    void should_SetAndGetStatus_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setStatus(DDMessageListener.STATUS_OK);

        assertThat(msg.getStatus()).isEqualTo(DDMessageListener.STATUS_OK);
    }

    // ---- FromIP ----

    @Test
    void should_SetAndGetFromIP_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setFromIP("192.168.1.1");

        assertThat(msg.getFromIP()).isEqualTo("192.168.1.1");
    }

    @Test
    void should_ReturnNull_When_NoFromIPSet() {
        DDMessage msg = new DDMessage();

        assertThat(msg.getFromIP()).isNull();
    }

    // ---- Exception fields ----

    @Test
    void should_SetAndGetException_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setException("NullPointerException");

        assertThat(msg.getException()).isEqualTo("NullPointerException");
    }

    @Test
    void should_SetAndGetDDException_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setDDException("Custom DD error");

        assertThat(msg.getDDException()).isEqualTo("Custom DD error");
    }

    @Test
    void should_SetAndGetApplicationErrorMessage_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setApplicationErrorMessage("Something went wrong");

        assertThat(msg.getApplicationErrorMessage()).isEqualTo("Something went wrong");
    }

    @Test
    void should_SetAndGetApplicationStatusMessage_When_Called() {
        DDMessage msg = new DDMessage();
        msg.setApplicationStatusMessage("Processing complete");

        assertThat(msg.getApplicationStatusMessage()).isEqualTo("Processing complete");
    }

    // ---- Timestamp ----

    @Test
    void should_SetTimestamp_When_Constructed() {
        DDMessage msg = new DDMessage();

        assertThat(msg.getCreateTimeStamp()).isGreaterThan(0);
        assertThat(msg.getCreateTimeStampLong()).isNotNull();
    }

    @Test
    void should_UpdateTimestamp_When_SetCreateTimeStampCalled() {
        DDMessage msg = new DDMessage();
        long original = msg.getCreateTimeStamp();

        // Small delay to ensure different timestamp
        msg.setCreateTimeStamp();
        long updated = msg.getCreateTimeStamp();

        assertThat(updated).isGreaterThanOrEqualTo(original);
    }

    // ---- Data management ----

    @Test
    void should_AddStringData_When_AddDataCalledWithString() {
        DDMessage msg = new DDMessage();
        msg.addData("chunk1");
        msg.addData("chunk2");

        assertThat(msg.getNumData()).isEqualTo(2);
        assertThat(msg.getDataAtAsString(0)).isEqualTo("chunk1");
        assertThat(msg.getDataAtAsString(1)).isEqualTo("chunk2");
    }

    @Test
    void should_AddByteData_When_AddDataCalledWithBytes() {
        DDMessage msg = new DDMessage();
        byte[] data = new byte[]{1, 2, 3};
        msg.addData(data);

        assertThat(msg.getNumData()).isEqualTo(1);
        assertThat(msg.getDataAt(0)).isEqualTo(data);
    }

    @Test
    void should_NotAddData_When_NullStringPassed() {
        DDMessage msg = new DDMessage();
        msg.addData((String) null);

        assertThat(msg.getNumData()).isZero();
    }

    @Test
    void should_NotAddData_When_NullBytesPassed() {
        DDMessage msg = new DDMessage();
        msg.addData((byte[]) null);

        assertThat(msg.getNumData()).isZero();
    }

    @Test
    void should_ReturnNull_When_GetDataAtCalledOnEmptyMessage() {
        DDMessage msg = new DDMessage();

        assertThat(msg.getDataAt(0)).isNull();
        assertThat(msg.getDataAtAsString(0)).isNull();
    }

    @Test
    void should_ClearAllData_When_ClearDataCalled() {
        DDMessage msg = new DDMessage();
        msg.addData("data1");
        msg.addData("data2");
        assertThat(msg.getNumData()).isEqualTo(2);

        msg.clearData();

        assertThat(msg.getNumData()).isZero();
    }

    @Test
    void should_ReturnFirstChunk_When_GetDataCalled() {
        DDMessage msg = new DDMessage();
        msg.addData("first");
        msg.addData("second");

        assertThat(msg.getDataAsString()).isEqualTo("first");
    }

    // ---- copyTo ----

    @Test
    void should_CopyAllFields_When_CopyToCalled() {
        DDMessage source = new DDMessage(DDMessage.CAT_TESTING);
        source.setStatus(DDMessageListener.STATUS_OK);
        source.setFromIP("10.0.0.1");
        source.setException("test error");
        source.addData("some data");

        DDMessage target = new DDMessage();
        source.copyTo(target);

        assertThat(target.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
        assertThat(target.getStatus()).isEqualTo(DDMessageListener.STATUS_OK);
        assertThat(target.getFromIP()).isEqualTo("10.0.0.1");
        assertThat(target.getException()).isEqualTo("test error");
        assertThat(target.getNumData()).isEqualTo(1);
        assertThat(target.getDataAsString()).isEqualTo("some data");
    }

    // ---- toString variants ----

    @Test
    void should_IncludeParams_When_ToStringCalled() {
        DDMessage msg = new DDMessage(DDMessage.CAT_TESTING);

        String s = msg.toString();

        assertThat(s).contains("PARAMS:");
    }

    @Test
    void should_IncludeDataNone_When_NoDataPresent() {
        DDMessage msg = new DDMessage();

        assertThat(msg.toString()).contains("DATA: (none)");
    }

    @Test
    void should_IncludeDataContent_When_DataPresent() {
        DDMessage msg = new DDMessage();
        msg.addData("payload");

        String full = msg.toString(true);
        assertThat(full).contains("payload");

        String noData = msg.toString(false);
        assertThat(noData).contains("bytes)");
    }

    @Test
    void should_IncludeStatus_When_StatusSetAndToStringCalled() {
        DDMessage msg = new DDMessage();
        msg.setStatus(DDMessageListener.STATUS_OK);

        assertThat(msg.toString()).contains("STATUS:");
    }

    @Test
    void should_ReturnParamsString_When_ToStringParamsCalled() {
        DDMessage msg = new DDMessage(DDMessage.CAT_TESTING);

        String params = msg.toStringParams();

        assertThat(params).contains("cat=");
    }

    @Test
    void should_ReturnSizeInfo_When_ToStringSizeCalled() {
        DDMessage msg = new DDMessage();
        msg.addData("hello");

        String sizeStr = msg.toStringSize();

        assertThat(sizeStr).contains("DATA[0]:");
        assertThat(sizeStr).contains("bytes)");
    }

    @Test
    void should_ReturnNoData_When_ToStringSizeCalledOnEmptyMessage() {
        DDMessage msg = new DDMessage();

        assertThat(msg.toStringSize()).isEqualTo("[NO DATA]");
    }

    // ---- Write/read round-trip via streams ----

    @Test
    void should_RoundTripParamsOnly_When_WrittenAndRead() throws IOException {
        DDMessage original = new DDMessage(DDMessage.CAT_TESTING);
        original.setFromIP("127.0.0.1");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        original.write(baos);

        DDMessage restored = new DDMessage();
        restored.read(new ByteArrayInputStream(baos.toByteArray()), baos.size());

        assertThat(restored.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
        assertThat(restored.getFromIP()).isEqualTo("127.0.0.1");
    }

    @Test
    void should_RoundTripWithData_When_WrittenAndRead() throws IOException {
        DDMessage original = new DDMessage(DDMessage.CAT_TESTING, "hello world");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        original.write(baos);

        DDMessage restored = new DDMessage();
        restored.read(new ByteArrayInputStream(baos.toByteArray()), baos.size());

        assertThat(restored.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
        assertThat(restored.getDataAsString()).isEqualTo("hello world");
    }

    @Test
    void should_RoundTripMultipleDataChunks_When_WrittenAndRead() throws IOException {
        DDMessage original = new DDMessage(DDMessage.CAT_TESTING);
        original.addData("chunk1");
        original.addData("chunk2");
        original.addData("chunk3");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        original.write(baos);

        DDMessage restored = new DDMessage();
        restored.read(new ByteArrayInputStream(baos.toByteArray()), baos.size());

        assertThat(restored.getNumData()).isEqualTo(3);
        assertThat(restored.getDataAtAsString(0)).isEqualTo("chunk1");
        assertThat(restored.getDataAtAsString(1)).isEqualTo("chunk2");
        assertThat(restored.getDataAtAsString(2)).isEqualTo("chunk3");
    }

    // ---- Marshal/demarshal via DataMarshal interface ----

    @Test
    void should_RoundTripViaMarshal_When_MarshalAndDemarshalCalled() {
        MsgState state = new MsgState();
        DDMessage original = new DDMessage(DDMessage.CAT_TESTING, "test data");
        original.setFromIP("1.2.3.4");

        String marshalled = original.marshal(state);

        DDMessage restored = new DDMessage();
        restored.demarshal(state, marshalled);

        assertThat(restored.getCategory()).isEqualTo(DDMessage.CAT_TESTING);
        assertThat(restored.getDataAsString()).isEqualTo("test data");
        assertThat(restored.getFromIP()).isEqualTo("1.2.3.4");
    }

    // ---- Static default version ----

    @Test
    void should_SetAndGetDefaultVersion_When_Called() {
        Version v = new Version("1.0");
        DDMessage.setDefaultVersion(v);

        assertThat(DDMessage.getDefaultVersion()).isSameAs(v);
    }

    // ---- Static MsgState ----

    @Test
    void should_SetAndGetMsgState_When_Called() {
        MsgState original = DDMessage.getMsgState();
        try {
            MsgState state = new MsgState();
            DDMessage.setMsgState(state);

            assertThat(DDMessage.getMsgState()).isSameAs(state);
        } finally {
            DDMessage.setMsgState(original);
        }
    }

    // ---- Empty stream handling ----

    @Test
    void should_HandleEmptyStream_When_ReadCalled() throws IOException {
        DDMessage msg = new DDMessage();
        // Empty stream should not throw (EOFException is caught for empty messages)
        msg.read(new ByteArrayInputStream(new byte[0]), 0);
    }
}
