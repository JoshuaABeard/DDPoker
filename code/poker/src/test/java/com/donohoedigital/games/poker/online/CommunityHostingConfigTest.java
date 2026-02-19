/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Community Edition
 * Copyright (c) 2026 DD Poker Community
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
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.donohoedigital.games.poker.online;

import static org.assertj.core.api.Assertions.*;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CommunityHostingConfig}.
 *
 * Tests cover connection URL construction, port preference persistence, and
 * public IP detection delegation. LAN IP detection is verified to return a
 * well-formed address (exact value is environment-dependent).
 */
class CommunityHostingConfigTest {

    /** Isolated preferences node — removed after each test to avoid state leak. */
    private Preferences testPrefs;

    @BeforeEach
    void setUp() throws BackingStoreException {
        testPrefs = Preferences.userRoot().node("/test/ddpoker/communityhosting");
        testPrefs.clear();
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        testPrefs.removeNode();
    }

    // -------------------------------------------------------------------------
    // Connection URL construction
    // -------------------------------------------------------------------------

    @Test
    void buildGameUrl_constructsWebSocketUrl() {
        // 203.0.113.0/24 is TEST-NET-3 (RFC 5737), safe for documentation/tests
        String url = CommunityHostingConfig.buildGameUrl("203.0.113.42", 11885, "abc-123");

        assertThat(url).isEqualTo("ws://203.0.113.42:11885/ws/games/abc-123");
    }

    @Test
    void buildGameUrl_handlesCustomPort() {
        String url = CommunityHostingConfig.buildGameUrl("203.0.113.1", 9000, "game-42");

        assertThat(url).isEqualTo("ws://203.0.113.1:9000/ws/games/game-42");
    }

    @Test
    void buildGameUrl_handlesLanIp() {
        String url = CommunityHostingConfig.buildGameUrl("192.168.1.100", 11885, "lan-game");

        assertThat(url).startsWith("ws://192.168.1.100:11885/ws/games/");
        assertThat(url).endsWith("/lan-game");
    }

    // -------------------------------------------------------------------------
    // Port preference persistence
    // -------------------------------------------------------------------------

    @Test
    void loadPort_returnsDefaultPort_whenNoPrefsSaved() {
        assertThat(CommunityHostingConfig.loadPort(testPrefs)).isEqualTo(CommunityHostingConfig.DEFAULT_COMMUNITY_PORT);
    }

    @Test
    void defaultCommunityPort_is11885() {
        assertThat(CommunityHostingConfig.DEFAULT_COMMUNITY_PORT).isEqualTo(11885);
    }

    @Test
    void savePort_andLoadPort_roundTrip() {
        CommunityHostingConfig.savePort(testPrefs, 9000);

        assertThat(CommunityHostingConfig.loadPort(testPrefs)).isEqualTo(9000);
    }

    @Test
    void savePort_overridesExistingValue() {
        CommunityHostingConfig.savePort(testPrefs, 9000);
        CommunityHostingConfig.savePort(testPrefs, 12000);

        assertThat(CommunityHostingConfig.loadPort(testPrefs)).isEqualTo(12000);
    }

    @Test
    void savePort_storesPortInCorrectPrefsKey() {
        CommunityHostingConfig.savePort(testPrefs, 7777);

        // Verify the value is stored under the expected key
        int stored = testPrefs.getInt(CommunityHostingConfig.PREF_COMMUNITY_PORT, -1);
        assertThat(stored).isEqualTo(7777);
    }

    // -------------------------------------------------------------------------
    // Public IP detection via PublicIPDetector
    // -------------------------------------------------------------------------

    @Test
    void detectPublicIp_delegatesToPublicIPDetector_success() {
        // Stub: first call returns a valid public IP (203.0.113.x is TEST-NET-3)
        PublicIPDetector stubDetector = new PublicIPDetector(url -> "203.0.113.42");
        CommunityHostingConfig config = new CommunityHostingConfig(stubDetector);

        assertThat(config.detectPublicIp()).isEqualTo("203.0.113.42");
    }

    @Test
    void detectPublicIp_returnsNull_whenDetectorFails() {
        // Stub: all services fail (return null)
        PublicIPDetector stubDetector = new PublicIPDetector(url -> null);
        CommunityHostingConfig config = new CommunityHostingConfig(stubDetector);

        assertThat(config.detectPublicIp()).isNull();
    }

    // -------------------------------------------------------------------------
    // LAN IP detection
    // -------------------------------------------------------------------------

    @Test
    void detectLanIp_returnsNonNullNonEmptyAddress() {
        String ip = CommunityHostingConfig.detectLanIp();

        assertThat(ip).isNotNull().isNotEmpty();
    }

    @Test
    void detectLanIp_returnsValidIpv4Format() {
        String ip = CommunityHostingConfig.detectLanIp();

        // Must have 4 octets
        String[] parts = ip.split("\\.");
        assertThat(parts).hasSize(4);
        // Each part must be numeric
        for (String part : parts) {
            assertThatNoException().isThrownBy(() -> {
                int octet = Integer.parseInt(part);
                assertThat(octet).isBetween(0, 255);
            });
        }
    }
}
