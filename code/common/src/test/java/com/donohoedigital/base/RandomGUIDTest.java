/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 Joshua Beard and contributors
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.base;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for RandomGUID - cryptographically random GUID generation using MD5
 * hashing
 */
class RandomGUIDTest {

    // =================================================================
    // Constructor Tests
    // =================================================================

    @Test
    void should_CreateGUID_When_DefaultConstructorUsed() {
        RandomGUID guid = new RandomGUID("192.168.1.1");

        assertThat(guid.valueBeforeMD5).isNotEmpty();
        assertThat(guid.valueAfterMD5).isNotEmpty();
    }

    @Test
    void should_CreateGUID_When_SecureModeEnabled() {
        RandomGUID guid = new RandomGUID("192.168.1.1", true);

        assertThat(guid.valueBeforeMD5).isNotEmpty();
        assertThat(guid.valueAfterMD5).isNotEmpty();
    }

    @Test
    void should_CreateGUID_When_SecureModeDisabled() {
        RandomGUID guid = new RandomGUID("192.168.1.1", false);

        assertThat(guid.valueBeforeMD5).isNotEmpty();
        assertThat(guid.valueAfterMD5).isNotEmpty();
    }

    @Test
    void should_CreateGUID_When_NullHostProvided() {
        RandomGUID guid = new RandomGUID(null);

        // Should default to "0.0.0.0" when null host provided
        assertThat(guid.valueBeforeMD5).contains("0.0.0.0");
        assertThat(guid.valueAfterMD5).isNotEmpty();
    }

    // =================================================================
    // Value Before MD5 Tests (Seed Value)
    // =================================================================

    @Test
    void should_IncludeHostInSeed_When_HostProvided() {
        String host = "192.168.1.1";
        RandomGUID guid = new RandomGUID(host);

        assertThat(guid.valueBeforeMD5).startsWith(host + ":");
    }

    @Test
    void should_IncludeTimestampInSeed_When_GUIDGenerated() {
        RandomGUID guid = new RandomGUID("test");

        // Seed should have format: host:timestamp:random
        String[] parts = guid.valueBeforeMD5.split(":");
        assertThat(parts).hasSize(3);
        assertThat(parts[0]).isEqualTo("test");
        assertThat(parts[1]).matches("\\d+"); // timestamp
        assertThat(parts[2]).matches("-?\\d+"); // random number (can be negative)
    }

    @Test
    void should_UseDefaultHostInSeed_When_NullHostProvided() {
        RandomGUID guid = new RandomGUID(null);

        assertThat(guid.valueBeforeMD5).startsWith("0.0.0.0:");
    }

    // =================================================================
    // Value After MD5 Tests (MD5 Hash)
    // =================================================================

    @Test
    void should_GenerateMD5Hash_When_GUIDCreated() {
        RandomGUID guid = new RandomGUID("test");

        // MD5 hash should be 32 hex characters (128 bits)
        assertThat(guid.valueAfterMD5).hasSize(32);
        assertThat(guid.valueAfterMD5).matches("[0-9a-f]{32}");
    }

    @Test
    void should_GenerateLowercaseHex_When_MD5Computed() {
        RandomGUID guid = new RandomGUID("test");

        // MD5 hash stored in valueAfterMD5 should be lowercase hex
        assertThat(guid.valueAfterMD5).matches("[0-9a-f]+");
    }

    // =================================================================
    // toString Format Tests (Standard GUID Format)
    // =================================================================

    @Test
    void should_FormatAsStandardGUID_When_ToStringCalled() {
        RandomGUID guid = new RandomGUID("test");

        String formatted = guid.toString();

        // Standard GUID format: 8-4-4-4-12 (36 characters with hyphens)
        assertThat(formatted).hasSize(36);
        assertThat(formatted).matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
    }

    @Test
    void should_UseUppercaseHex_When_ToStringCalled() {
        RandomGUID guid = new RandomGUID("test");

        String formatted = guid.toString();

        // toString should convert to uppercase
        assertThat(formatted).matches("[0-9A-F-]+");
        assertThat(formatted).doesNotMatch(".*[a-z].*");
    }

    @Test
    void should_Have5Hyphens_When_FormattedAsGUID() {
        RandomGUID guid = new RandomGUID("test");

        String formatted = guid.toString();

        long hyphenCount = formatted.chars().filter(ch -> ch == '-').count();
        assertThat(hyphenCount).isEqualTo(4);
    }

    @Test
    void should_Have8Characters_When_FirstSegment() {
        RandomGUID guid = new RandomGUID("test");

        String formatted = guid.toString();
        String firstSegment = formatted.substring(0, 8);

        assertThat(firstSegment).hasSize(8);
        assertThat(firstSegment).matches("[0-9A-F]{8}");
    }

    @Test
    void should_Have4Characters_When_MiddleSegments() {
        RandomGUID guid = new RandomGUID("test");

        String formatted = guid.toString();
        String[] segments = formatted.split("-");

        assertThat(segments[1]).hasSize(4);
        assertThat(segments[2]).hasSize(4);
        assertThat(segments[3]).hasSize(4);
    }

    @Test
    void should_Have12Characters_When_LastSegment() {
        RandomGUID guid = new RandomGUID("test");

        String formatted = guid.toString();
        String[] segments = formatted.split("-");

        assertThat(segments[4]).hasSize(12);
    }

    // =================================================================
    // Uniqueness Tests
    // =================================================================

    @Test
    void should_GenerateUniqueGUIDs_When_MultipleCreated() {
        Set<String> guids = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            RandomGUID guid = new RandomGUID("test");
            guids.add(guid.toString());
        }

        // All 100 GUIDs should be unique
        assertThat(guids).hasSize(100);
    }

    @Test
    void should_GenerateUniqueMD5Hashes_When_MultipleCreated() {
        Set<String> hashes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            RandomGUID guid = new RandomGUID("test");
            hashes.add(guid.valueAfterMD5);
        }

        // All 100 MD5 hashes should be unique
        assertThat(hashes).hasSize(100);
    }

    @Test
    void should_GenerateUniqueSeeds_When_MultipleCreated() {
        Set<String> seeds = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            RandomGUID guid = new RandomGUID("test");
            seeds.add(guid.valueBeforeMD5);
        }

        // All 100 seeds should be unique (due to time and random components)
        assertThat(seeds).hasSize(100);
    }

    // =================================================================
    // Secure Mode Tests
    // =================================================================

    @Test
    void should_GenerateValidGUID_When_SecureModeTrue() {
        RandomGUID guid = new RandomGUID("test", true);

        assertThat(guid.valueAfterMD5).hasSize(32);
        assertThat(guid.toString()).hasSize(36);
        assertThat(guid.toString()).matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
    }

    @Test
    void should_GenerateValidGUID_When_SecureModeFalse() {
        RandomGUID guid = new RandomGUID("test", false);

        assertThat(guid.valueAfterMD5).hasSize(32);
        assertThat(guid.toString()).hasSize(36);
        assertThat(guid.toString()).matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
    }

    @Test
    void should_GenerateUniqueGUIDs_When_SecureModeUsed() {
        Set<String> guids = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            RandomGUID guid = new RandomGUID("test", true);
            guids.add(guid.toString());
        }

        assertThat(guids).hasSize(50);
    }

    @Test
    void should_GenerateUniqueGUIDs_When_NonSecureModeUsed() {
        Set<String> guids = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            RandomGUID guid = new RandomGUID("test", false);
            guids.add(guid.toString());
        }

        assertThat(guids).hasSize(50);
    }

    // =================================================================
    // Host Parameter Tests
    // =================================================================

    @Test
    void should_IncludeHostInSeed_When_DifferentHostsUsed() {
        RandomGUID guid1 = new RandomGUID("192.168.1.1");
        RandomGUID guid2 = new RandomGUID("192.168.1.2");

        assertThat(guid1.valueBeforeMD5).startsWith("192.168.1.1:");
        assertThat(guid2.valueBeforeMD5).startsWith("192.168.1.2:");
    }

    @Test
    void should_AcceptHostnameString_When_HostProvided() {
        String hostname = "server.example.com";
        RandomGUID guid = new RandomGUID(hostname);

        assertThat(guid.valueBeforeMD5).startsWith(hostname + ":");
    }

    @Test
    void should_AcceptEmptyHost_When_EmptyStringProvided() {
        RandomGUID guid = new RandomGUID("");

        assertThat(guid.valueBeforeMD5).startsWith(":");
        assertThat(guid.valueAfterMD5).hasSize(32);
    }

    // =================================================================
    // Edge Cases
    // =================================================================

    @Test
    void should_GenerateConsistentLength_When_MultipleGUIDs() {
        for (int i = 0; i < 20; i++) {
            RandomGUID guid = new RandomGUID("test");

            assertThat(guid.valueAfterMD5).hasSize(32);
            assertThat(guid.toString()).hasSize(36);
        }
    }

    @Test
    void should_GenerateDifferentGUIDs_When_SameHostUsed() {
        RandomGUID guid1 = new RandomGUID("test");
        RandomGUID guid2 = new RandomGUID("test");

        // Even with same host, GUIDs should differ due to time and random components
        assertThat(guid1.toString()).isNotEqualTo(guid2.toString());
        assertThat(guid1.valueAfterMD5).isNotEqualTo(guid2.valueAfterMD5);
    }

    @Test
    void should_HaveValidFormat_When_SpecialCharactersInHost() {
        RandomGUID guid = new RandomGUID("host@#$%");

        assertThat(guid.valueBeforeMD5).startsWith("host@#$%:");
        assertThat(guid.valueAfterMD5).hasSize(32);
        assertThat(guid.toString()).hasSize(36);
    }
}
