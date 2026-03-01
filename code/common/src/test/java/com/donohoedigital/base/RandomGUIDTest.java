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

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for RandomGUID - cryptographically seeded GUID generator using MD5
 * hashing.
 */
class RandomGUIDTest {

    @Test
    void should_HaveNonNullNonEmptyRawValue_When_GUIDGenerated() {
        RandomGUID guid = new RandomGUID("127.0.0.1");

        assertThat(guid.valueAfterMD5).isNotNull().isNotEmpty();
    }

    @Test
    void should_Have32CharLowercaseHexRawValue_When_GUIDGenerated() {
        RandomGUID guid = new RandomGUID("127.0.0.1");

        // MD5 produces 16 bytes = 32 hex characters
        assertThat(guid.valueAfterMD5).hasSize(32);
        assertThat(guid.valueAfterMD5).matches("[0-9a-f]{32}");
    }

    @Test
    void should_ReturnFormattedGUIDString_When_ToStringCalled() {
        RandomGUID guid = new RandomGUID("127.0.0.1");

        String result = guid.toString();

        // Standard GUID format: 8-4-4-4-12 uppercase hex chars with dashes = 36 chars
        // total
        assertThat(result).hasSize(36);
        assertThat(result).matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
    }

    @Test
    void should_ProduceUniqueGUIDs_When_GeneratedMultipleTimes() {
        Set<String> guids = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            guids.add(new RandomGUID("127.0.0.1").toString());
        }

        // All 20 generated GUIDs should be distinct
        assertThat(guids).hasSize(20);
    }

    @Test
    void should_GenerateValidGUID_When_SecureOptionEnabled() {
        RandomGUID guid = new RandomGUID("127.0.0.1", true);

        assertThat(guid.toString()).matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
    }

    @Test
    void should_PopulateValueBeforeMD5_When_GUIDGenerated() {
        RandomGUID guid = new RandomGUID("10.0.0.1");

        // valueBeforeMD5 should contain the host string used as input
        assertThat(guid.valueBeforeMD5).isNotNull().isNotEmpty();
        assertThat(guid.valueBeforeMD5).startsWith("10.0.0.1:");
    }
}
