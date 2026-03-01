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
package com.donohoedigital.games.poker.gameserver;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GameServerProperties} canonical constructor validation.
 *
 * <p>
 * The compact constructor normalizes out-of-range values to sensible defaults.
 * </p>
 */
class GameServerPropertiesTest {

    /** Create a valid default properties instance. */
    private GameServerProperties defaults() {
        return new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0);
    }

    @Test
    void should_UseSuppliedValues_When_AllValid() {
        GameServerProperties props = defaults();

        assertThat(props.maxConcurrentGames()).isEqualTo(50);
        assertThat(props.actionTimeoutSeconds()).isEqualTo(30);
        assertThat(props.reconnectTimeoutSeconds()).isEqualTo(120);
        assertThat(props.threadPoolSize()).isEqualTo(10);
        assertThat(props.rateLimitMillis()).isEqualTo(1000);
        assertThat(props.consecutiveTimeoutLimit()).isEqualTo(3);
        assertThat(props.disconnectGraceTurns()).isEqualTo(2);
        assertThat(props.maxGamesPerUser()).isEqualTo(5);
        assertThat(props.communityHeartbeatTimeoutMinutes()).isEqualTo(5);
        assertThat(props.lobbyTimeoutHours()).isEqualTo(24);
        assertThat(props.completedGameRetentionDays()).isEqualTo(7);
        assertThat(props.serverBaseUrl()).isEqualTo("ws://localhost");
        assertThat(props.aiActionDelayMs()).isEqualTo(0);
    }

    @Test
    void should_DefaultMaxConcurrentGames_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(0, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost",
                0);
        assertThat(props.maxConcurrentGames()).isEqualTo(50);

        GameServerProperties props2 = new GameServerProperties(-5, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7,
                "ws://localhost", 0);
        assertThat(props2.maxConcurrentGames()).isEqualTo(50);
    }

    @Test
    void should_DefaultActionTimeout_When_Negative() {
        GameServerProperties props = new GameServerProperties(50, -1, 120, 10, 1000, 3, 2, 5, 5, 24, 7,
                "ws://localhost", 0);
        assertThat(props.actionTimeoutSeconds()).isEqualTo(30);
    }

    @Test
    void should_AllowZeroActionTimeout_When_Zero() {
        // 0 means "no timeout" — must not be replaced by default
        GameServerProperties props = new GameServerProperties(50, 0, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost",
                0);
        assertThat(props.actionTimeoutSeconds()).isEqualTo(0);
    }

    @Test
    void should_DefaultReconnectTimeout_When_Negative() {
        GameServerProperties props = new GameServerProperties(50, 30, -1, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost",
                0);
        assertThat(props.reconnectTimeoutSeconds()).isEqualTo(120);
    }

    @Test
    void should_DefaultThreadPoolSize_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 0, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost",
                0);
        assertThat(props.threadPoolSize()).isEqualTo(10);
    }

    @Test
    void should_DefaultRateLimitMillis_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 0, 3, 2, 5, 5, 24, 7, "ws://localhost",
                0);
        assertThat(props.rateLimitMillis()).isEqualTo(1000);
    }

    @Test
    void should_DefaultConsecutiveTimeoutLimit_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 0, 2, 5, 5, 24, 7,
                "ws://localhost", 0);
        assertThat(props.consecutiveTimeoutLimit()).isEqualTo(3);
    }

    @Test
    void should_DefaultDisconnectGraceTurns_When_Negative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, -1, 5, 5, 24, 7,
                "ws://localhost", 0);
        assertThat(props.disconnectGraceTurns()).isEqualTo(2);
    }

    @Test
    void should_AllowZeroDisconnectGraceTurns_When_Zero() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 0, 5, 5, 24, 7,
                "ws://localhost", 0);
        assertThat(props.disconnectGraceTurns()).isEqualTo(0);
    }

    @Test
    void should_DefaultMaxGamesPerUser_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 0, 5, 24, 7,
                "ws://localhost", 0);
        assertThat(props.maxGamesPerUser()).isEqualTo(5);
    }

    @Test
    void should_DefaultCommunityHeartbeatTimeout_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 0, 24, 7,
                "ws://localhost", 0);
        assertThat(props.communityHeartbeatTimeoutMinutes()).isEqualTo(5);
    }

    @Test
    void should_DefaultLobbyTimeout_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 0, 7, "ws://localhost",
                0);
        assertThat(props.lobbyTimeoutHours()).isEqualTo(24);
    }

    @Test
    void should_DefaultCompletedGameRetention_When_ZeroOrNegative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 0,
                "ws://localhost", 0);
        assertThat(props.completedGameRetentionDays()).isEqualTo(7);
    }

    @Test
    void should_DefaultServerBaseUrl_When_Null() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, null, 0);
        assertThat(props.serverBaseUrl()).isEqualTo("ws://localhost");
    }

    @Test
    void should_DefaultServerBaseUrl_When_Blank() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "  ", 0);
        assertThat(props.serverBaseUrl()).isEqualTo("ws://localhost");
    }

    @Test
    void should_DefaultAiActionDelayMs_When_Negative() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7,
                "ws://localhost", -100);
        assertThat(props.aiActionDelayMs()).isEqualTo(0);
    }

    @Test
    void should_AllowPositiveAiActionDelayMs() {
        GameServerProperties props = new GameServerProperties(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7,
                "ws://localhost", 400);
        assertThat(props.aiActionDelayMs()).isEqualTo(400);
    }
}
