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
package com.donohoedigital.games.poker.online;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.model.OnlineGame;
import com.donohoedigital.games.poker.model.util.OnlineGameList;

/**
 * Unit tests for {@link GameSummaryConverter}.
 */
class GameSummaryConverterTest {

    private static final String SERVER_HOST = "localhost:54321";
    private GameSummaryConverter converter;

    @BeforeEach
    void setUp() {
        converter = new GameSummaryConverter(SERVER_HOST);
    }

    // =========================================================================
    // SERVER game URL construction
    // =========================================================================

    @Test
    void serverGame_constructsWsUrl() {
        GameSummary summary = buildSummary("game-abc", "Test Game", "SERVER", "WAITING_FOR_PLAYERS",
                "ws://localhost:54321/ws/games/game-abc");

        OnlineGame game = converter.convert(summary);

        assertThat(game.getUrl()).isEqualTo("ws://" + SERVER_HOST + "/ws/games/game-abc");
    }

    @Test
    void serverGame_usesServerHostForUrlEvenIfWsUrlProvided() {
        // SERVER game wsUrl should be constructed from serverHost, not from the summary
        // wsUrl
        GameSummary summary = buildSummary("game-xyz", "My Game", "SERVER", "WAITING_FOR_PLAYERS",
                "ws://some-other-host/ws/games/game-xyz");

        OnlineGame game = converter.convert(summary);

        assertThat(game.getUrl()).startsWith("ws://" + SERVER_HOST + "/ws/games/");
    }

    // =========================================================================
    // COMMUNITY game URL pass-through
    // =========================================================================

    @Test
    void communityGame_usesRegisteredWsUrl() {
        GameSummary summary = buildSummary("community-id", "Community Game", "COMMUNITY", "WAITING_FOR_PLAYERS",
                "ws://203.0.113.42:8765/ws/games/local-uuid");

        OnlineGame game = converter.convert(summary);

        assertThat(game.getUrl()).isEqualTo("ws://203.0.113.42:8765/ws/games/local-uuid");
    }

    @Test
    void communityGame_nullWsUrl_fallsBackToServerHostConstruction() {
        GameSummary summary = buildSummary("community-id", "Community Game", "COMMUNITY", "WAITING_FOR_PLAYERS", null);

        OnlineGame game = converter.convert(summary);

        // When wsUrl is null, falls back to server-hosted URL construction
        assertThat(game.getUrl()).startsWith("ws://" + SERVER_HOST + "/ws/games/");
    }

    // =========================================================================
    // Mode mapping
    // =========================================================================

    @Test
    void waitingForPlayers_mapToModeReg() {
        GameSummary summary = buildSummary("g1", "Game", "SERVER", "WAITING_FOR_PLAYERS", null);

        OnlineGame game = converter.convert(summary);

        assertThat(game.getMode()).isEqualTo(OnlineGame.MODE_REG);
    }

    @Test
    void inProgress_mapToModePlay() {
        GameSummary summary = buildSummary("g1", "Game", "SERVER", "IN_PROGRESS", null);

        OnlineGame game = converter.convert(summary);

        assertThat(game.getMode()).isEqualTo(OnlineGame.MODE_PLAY);
    }

    @Test
    void cancelled_mapToModeStop() {
        GameSummary summary = buildSummary("g1", "Game", "SERVER", "CANCELLED", null);

        OnlineGame game = converter.convert(summary);

        assertThat(game.getMode()).isEqualTo(OnlineGame.MODE_STOP);
    }

    // =========================================================================
    // TournamentProfile name
    // =========================================================================

    @Test
    void gameName_setOnTournamentProfile() {
        GameSummary summary = buildSummary("g1", "Friday Night Poker", "SERVER", "WAITING_FOR_PLAYERS", null);

        OnlineGame game = converter.convert(summary);

        assertThat(game.getTournament()).isNotNull();
        assertThat(game.getTournament().getName()).isEqualTo("Friday Night Poker");
    }

    // =========================================================================
    // Host player
    // =========================================================================

    @Test
    void ownerName_setAsHostPlayer() {
        GameSummary summary = buildSummary("g1", "Game", "SERVER", "WAITING_FOR_PLAYERS", null);

        OnlineGame game = converter.convert(summary);

        assertThat(game.getHostPlayer()).isEqualTo("alice");
    }

    // =========================================================================
    // Hosting type (transient field)
    // =========================================================================

    @Test
    void hostingType_setOnOnlineGame() {
        GameSummary serverSummary = buildSummary("g1", "Game", "SERVER", "WAITING_FOR_PLAYERS", null);
        GameSummary communitySummary = buildSummary("g2", "Game", "COMMUNITY", "WAITING_FOR_PLAYERS",
                "ws://1.2.3.4/ws");

        OnlineGame serverGame = converter.convert(serverSummary);
        OnlineGame communityGame = converter.convert(communitySummary);

        assertThat(serverGame.getHostingType()).isEqualTo("SERVER");
        assertThat(communityGame.getHostingType()).isEqualTo("COMMUNITY");
    }

    // =========================================================================
    // convertAll
    // =========================================================================

    @Test
    void convertAll_returnsCorrectListSize() {
        List<GameSummary> summaries = List.of(buildSummary("g1", "Game 1", "SERVER", "WAITING_FOR_PLAYERS", null),
                buildSummary("g2", "Game 2", "COMMUNITY", "IN_PROGRESS", "ws://1.2.3.4:8765/ws/games/g2"));

        OnlineGameList list = converter.convertAll(summaries);

        assertThat(list).hasSize(2);
        assertThat(list.getTotalSize()).isEqualTo(2);
        assertThat(list.get(0).getTournament().getName()).isEqualTo("Game 1");
        assertThat(list.get(1).getHostingType()).isEqualTo("COMMUNITY");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private GameSummary buildSummary(String gameId, String name, String hostingType, String status, String wsUrl) {
        return new GameSummary(gameId, name, hostingType, status, "alice", 1, 9, false, wsUrl,
                new GameSummary.BlindsSummary(10, 20, 0), Instant.now(), null);
    }
}
