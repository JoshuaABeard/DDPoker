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
package com.donohoedigital.games.poker.protocol.dto;

import java.time.Instant;
import java.util.List;

/**
 * Public game summary returned by the lobby listing API.
 *
 * <p>
 * wsUrl is null for private games in list view; returned in GameJoinResponse
 * after successful join.
 * </p>
 */
public record GameSummary(String gameId, String name, String hostingType, // "SERVER" | "COMMUNITY"
        String status, // GameInstanceState name
        String ownerName, int playerCount, int maxPlayers, boolean isPrivate, // true if passwordHash is non-null
        String wsUrl, // null for private games in list view
        BlindsSummary blinds, Instant createdAt, Instant startedAt, List<LobbyPlayerInfo> players) {
    /** Blind level amounts extracted from the game profile. */
    public record BlindsSummary(int smallBlind, int bigBlind, int ante) {
    }
}
