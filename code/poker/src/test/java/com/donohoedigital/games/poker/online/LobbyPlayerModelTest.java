/*
 * Copyright (c) 2026 DD Poker Community
 * Licensed under GPL-3.0. See LICENSE.txt.
 */
package com.donohoedigital.games.poker.online;

import com.donohoedigital.games.poker.protocol.dto.GameSummary;
import com.donohoedigital.games.poker.protocol.dto.LobbyPlayerInfo;
import com.donohoedigital.games.poker.protocol.message.ServerMessageData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Lobby.PlayerModel}.
 */
class LobbyPlayerModelTest {

    private Lobby.PlayerModel model;

    @BeforeEach
    void setUp() {
        model = new Lobby.PlayerModel();
    }

    @Test
    void initialState_isEmpty() {
        assertThat(model.getRowCount()).isZero();
        assertThat(model.getColumnCount()).isEqualTo(3);
    }

    @Test
    void updateFromLobbyPlayerData_replacesAllPlayers() {
        ServerMessageData.LobbyPlayerData host = new ServerMessageData.LobbyPlayerData(1L, "Alice", true, false, null);
        ServerMessageData.LobbyPlayerData player = new ServerMessageData.LobbyPlayerData(2L, "Bob", false, false, null);

        model.updateFromLobbyPlayerData(List.of(host, player));

        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.getValueAt(0, 1)).isEqualTo("Alice");
        assertThat(model.getValueAt(0, 2)).isEqualTo("Host");
        assertThat(model.getValueAt(1, 1)).isEqualTo("Bob");
        assertThat(model.getValueAt(1, 2)).isEqualTo("Player");
    }

    @Test
    void updateFromLobbyPlayerData_aiPlayerShowsAIRole() {
        ServerMessageData.LobbyPlayerData ai = new ServerMessageData.LobbyPlayerData(3L, "Bot", false, true, "expert");

        model.updateFromLobbyPlayerData(List.of(ai));

        assertThat(model.getValueAt(0, 2)).isEqualTo("AI");
    }

    @Test
    void getValueAt_column0_returnsOneBasedRowNumber() {
        ServerMessageData.LobbyPlayerData p = new ServerMessageData.LobbyPlayerData(1L, "X", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p));

        assertThat(model.getValueAt(0, 0)).isEqualTo(1);
    }

    @Test
    void getValueAt_unknownColumn_returnsEmpty() {
        ServerMessageData.LobbyPlayerData p = new ServerMessageData.LobbyPlayerData(1L, "X", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p));

        assertThat(model.getValueAt(0, 99)).isEqualTo("");
    }

    @Test
    void addLobbyPlayer_appendsToList() {
        ServerMessageData.LobbyPlayerData host = new ServerMessageData.LobbyPlayerData(1L, "Alice", true, false, null);
        model.updateFromLobbyPlayerData(List.of(host));

        ServerMessageData.LobbyPlayerData newPlayer = new ServerMessageData.LobbyPlayerData(2L, "Bob", false, false,
                null);
        model.addLobbyPlayer(newPlayer);

        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.getValueAt(1, 1)).isEqualTo("Bob");
    }

    @Test
    void removePlayer_removesByName() {
        ServerMessageData.LobbyPlayerData p1 = new ServerMessageData.LobbyPlayerData(1L, "Alice", true, false, null);
        ServerMessageData.LobbyPlayerData p2 = new ServerMessageData.LobbyPlayerData(2L, "Bob", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p1, p2));

        model.removePlayer("Alice");

        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 1)).isEqualTo("Bob");
    }

    @Test
    void removePlayer_noMatchDoesNothing() {
        ServerMessageData.LobbyPlayerData p = new ServerMessageData.LobbyPlayerData(1L, "Alice", true, false, null);
        model.updateFromLobbyPlayerData(List.of(p));

        model.removePlayer("Nobody");

        assertThat(model.getRowCount()).isEqualTo(1);
    }

    @Test
    void getPlayerAt_validRow_returnsData() {
        ServerMessageData.LobbyPlayerData host = new ServerMessageData.LobbyPlayerData(42L, "Alice", true, false, null);
        model.updateFromLobbyPlayerData(List.of(host));

        ServerMessageData.LobbyPlayerData result = model.getPlayerAt(0);
        assertThat(result).isNotNull();
        assertThat(result.profileId()).isEqualTo(42L);
        assertThat(result.name()).isEqualTo("Alice");
        assertThat(result.isOwner()).isTrue();
    }

    @Test
    void getPlayerAt_negativeRow_returnsNull() {
        assertThat(model.getPlayerAt(-1)).isNull();
    }

    @Test
    void getPlayerAt_outOfBoundsRow_returnsNull() {
        assertThat(model.getPlayerAt(0)).isNull();
    }

    @Test
    void getProfileIdAtRow_validRow_returnsProfileId() {
        ServerMessageData.LobbyPlayerData p = new ServerMessageData.LobbyPlayerData(99L, "Bob", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p));

        assertThat(model.getProfileIdAtRow(0)).isEqualTo(99L);
    }

    @Test
    void getProfileIdAtRow_invalidRow_returnsNegativeOne() {
        assertThat(model.getProfileIdAtRow(0)).isEqualTo(-1);
        assertThat(model.getProfileIdAtRow(-1)).isEqualTo(-1);
    }

    @Test
    void updateFromGameSummary_convertsLobbyPlayerInfo() {
        List<LobbyPlayerInfo> infos = List.of(new LobbyPlayerInfo("Alice", "Host"), new LobbyPlayerInfo("Bot1", "AI"),
                new LobbyPlayerInfo("Bob", "Player"));
        GameSummary summary = new GameSummary("g1", "Test", "SERVER", "WAITING", "Alice", 3, 6, false, null, null, null,
                null, infos);

        model.updateFromGameSummary(summary);

        assertThat(model.getRowCount()).isEqualTo(3);
        assertThat(model.getValueAt(0, 1)).isEqualTo("Alice");
        assertThat(model.getValueAt(0, 2)).isEqualTo("Host");
        assertThat(model.getValueAt(1, 1)).isEqualTo("Bot1");
        assertThat(model.getValueAt(1, 2)).isEqualTo("AI");
        assertThat(model.getValueAt(2, 1)).isEqualTo("Bob");
        assertThat(model.getValueAt(2, 2)).isEqualTo("Player");
    }

    @Test
    void updateFromGameSummary_nullPlayers_clearsModel() {
        ServerMessageData.LobbyPlayerData p = new ServerMessageData.LobbyPlayerData(1L, "X", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p));

        GameSummary summary = new GameSummary("g1", "Test", "SERVER", "WAITING", "Alice", 0, 6, false, null, null, null,
                null, null);
        model.updateFromGameSummary(summary);

        assertThat(model.getRowCount()).isZero();
    }

    @Test
    void updateFromGameSummary_profileIdIsZero() {
        // REST polling doesn't provide profileId, so it defaults to 0
        List<LobbyPlayerInfo> infos = List.of(new LobbyPlayerInfo("Alice", "Host"));
        GameSummary summary = new GameSummary("g1", "Test", "SERVER", "WAITING", "Alice", 1, 6, false, null, null, null,
                null, infos);

        model.updateFromGameSummary(summary);

        assertThat(model.getProfileIdAtRow(0)).isZero();
    }

    @Test
    void isCellEditable_alwaysFalse() {
        ServerMessageData.LobbyPlayerData p = new ServerMessageData.LobbyPlayerData(1L, "X", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p));

        assertThat(model.isCellEditable(0, 0)).isFalse();
        assertThat(model.isCellEditable(0, 1)).isFalse();
        assertThat(model.isCellEditable(0, 2)).isFalse();
    }

    @Test
    void updateFromLobbyPlayerData_replacesExistingData() {
        ServerMessageData.LobbyPlayerData p1 = new ServerMessageData.LobbyPlayerData(1L, "Alice", true, false, null);
        model.updateFromLobbyPlayerData(List.of(p1));

        ServerMessageData.LobbyPlayerData p2 = new ServerMessageData.LobbyPlayerData(2L, "Bob", false, false, null);
        model.updateFromLobbyPlayerData(List.of(p2));

        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 1)).isEqualTo("Bob");
    }
}
