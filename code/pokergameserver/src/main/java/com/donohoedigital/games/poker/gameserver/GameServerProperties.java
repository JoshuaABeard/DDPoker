/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Source Code
 * Copyright (c) 2003-2026  Doug Donohoe, DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * For the full License text, please see the LICENSE.txt file in the root directory
 * of this project.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the poker game server.
 *
 * @param maxConcurrentGames
 *            Maximum number of games that can run simultaneously (default 50)
 * @param actionTimeoutSeconds
 *            Timeout in seconds for player actions (default 30, 0 = no timeout)
 * @param reconnectTimeoutSeconds
 *            Timeout in seconds before disconnected player is auto-removed
 *            (default 120)
 * @param threadPoolSize
 *            Size of thread pool for running game instances (default 10)
 * @param rateLimitMillis
 *            Minimum milliseconds between player actions to prevent spam
 *            (default 1000)
 * @param consecutiveTimeoutLimit
 *            Number of consecutive timeouts before a player is auto-folded
 *            without waiting (default 3)
 * @param disconnectGraceTurns
 *            Number of turns to wait with normal timeout before auto-folding
 *            disconnected players (default 2)
 * @param maxGamesPerUser
 *            Maximum active games a single user can own simultaneously (default
 *            5)
 * @param communityHeartbeatTimeoutMinutes
 *            Minutes before a COMMUNITY game with no heartbeat is cancelled
 *            (default 5)
 * @param lobbyTimeoutHours
 *            Hours before a SERVER lobby stuck in WAITING_FOR_PLAYERS is
 *            cancelled (default 24)
 * @param completedGameRetentionDays
 *            Days to retain COMPLETED/CANCELLED game records before deletion
 *            (default 7)
 * @param serverBaseUrl
 *            Base URL used to build ws_url for SERVER-hosted games. For
 *            embedded server this is overridden at runtime with the actual
 *            port. (default "ws://localhost")
 * @param aiActionDelayMs
 *            Milliseconds to pause after each AI player action so humans can
 *            observe the game progressing (default 0 = no delay, embedded mode
 *            sets this to ~400 ms)
 */
@ConfigurationProperties(prefix = "game.server")
public record GameServerProperties(int maxConcurrentGames, int actionTimeoutSeconds, int reconnectTimeoutSeconds,
        int threadPoolSize, int rateLimitMillis, int consecutiveTimeoutLimit, int disconnectGraceTurns,
        int maxGamesPerUser, int communityHeartbeatTimeoutMinutes, int lobbyTimeoutHours,
        int completedGameRetentionDays, String serverBaseUrl, int aiActionDelayMs) {
    /**
     * Canonical constructor with validation and defaults.
     */
    public GameServerProperties {
        if (maxConcurrentGames <= 0)
            maxConcurrentGames = 50;
        if (actionTimeoutSeconds < 0)
            actionTimeoutSeconds = 30;
        if (reconnectTimeoutSeconds < 0)
            reconnectTimeoutSeconds = 120;
        if (threadPoolSize <= 0)
            threadPoolSize = 10;
        if (rateLimitMillis <= 0)
            rateLimitMillis = 1000;
        if (consecutiveTimeoutLimit <= 0)
            consecutiveTimeoutLimit = 3;
        if (disconnectGraceTurns < 0)
            disconnectGraceTurns = 2;
        if (maxGamesPerUser <= 0)
            maxGamesPerUser = 5;
        if (communityHeartbeatTimeoutMinutes <= 0)
            communityHeartbeatTimeoutMinutes = 5;
        if (lobbyTimeoutHours <= 0)
            lobbyTimeoutHours = 24;
        if (completedGameRetentionDays <= 0)
            completedGameRetentionDays = 7;
        if (serverBaseUrl == null || serverBaseUrl.isBlank())
            serverBaseUrl = "ws://localhost";
        if (aiActionDelayMs < 0)
            aiActionDelayMs = 0;
    }

    /**
     * Default constructor for Spring.
     */
    public GameServerProperties() {
        this(50, 30, 120, 10, 1000, 3, 2, 5, 5, 24, 7, "ws://localhost", 0);
    }
}
