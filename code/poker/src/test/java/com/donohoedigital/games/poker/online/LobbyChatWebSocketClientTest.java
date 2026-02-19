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

import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LobbyChatWebSocketClient}.
 *
 * Tests use the package-private {@code createListenerForTesting()} hook to
 * invoke message callbacks directly, bypassing the need for a real WebSocket
 * server. State-management and message-parsing are fully exercised.
 */
class LobbyChatWebSocketClientTest {

    private TestLobbyMessageListener listener;
    private LobbyChatWebSocketClient client;
    private WebSocket stubWebSocket;

    @BeforeEach
    void setUp() {
        listener = new TestLobbyMessageListener();
        // null HttpClient / scheduler — no real network needed for these tests
        client = new LobbyChatWebSocketClient(listener, null, null);
        stubWebSocket = new LobbyChatWebSocketClient.NoOpWebSocket();
    }

    // -------------------------------------------------------------------------
    // onConnected / onDisconnected lifecycle
    // -------------------------------------------------------------------------

    @Test
    void onOpen_setsConnectedAndNotifiesListener() {
        WebSocket.Listener ws = client.createListenerForTesting();

        ws.onOpen(stubWebSocket);

        assertThat(client.isConnected()).isTrue();
        assertThat(listener.connected).isTrue();
    }

    @Test
    void onClose_clearsConnectedAndNotifiesListener() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onClose(stubWebSocket, WebSocket.NORMAL_CLOSURE, "done");

        assertThat(client.isConnected()).isFalse();
        assertThat(listener.disconnected).isTrue();
    }

    @Test
    void onError_clearsConnectedAndNotifiesListener() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onError(stubWebSocket, new RuntimeException("test error"));

        assertThat(client.isConnected()).isFalse();
        assertThat(listener.disconnected).isTrue();
    }

    @Test
    void disconnect_clearsConnectedState() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);
        assertThat(client.isConnected()).isTrue();

        client.disconnect();

        assertThat(client.isConnected()).isFalse();
    }

    @Test
    void isConnected_returnsFalse_beforeAnyConnection() {
        assertThat(client.isConnected()).isFalse();
    }

    // -------------------------------------------------------------------------
    // Message parsing — LOBBY_PLAYER_LIST
    // -------------------------------------------------------------------------

    @Test
    void onText_playerList_dispatchesOnPlayerList() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onText(
                stubWebSocket, "{\"type\":\"LOBBY_PLAYER_LIST\",\"players\":["
                        + "{\"playerId\":42,\"playerName\":\"Alice\"}," + "{\"playerId\":99,\"playerName\":\"Bob\"}]}",
                true);

        assertThat(listener.playerList).hasSize(2);
        assertThat(listener.playerList.get(0).playerId()).isEqualTo(42L);
        assertThat(listener.playerList.get(0).playerName()).isEqualTo("Alice");
        assertThat(listener.playerList.get(1).playerId()).isEqualTo(99L);
        assertThat(listener.playerList.get(1).playerName()).isEqualTo("Bob");
    }

    @Test
    void onText_emptyPlayerList_dispatchesEmptyList() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onText(stubWebSocket, "{\"type\":\"LOBBY_PLAYER_LIST\",\"players\":[]}", true);

        assertThat(listener.playerList).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Message parsing — LOBBY_JOIN / LOBBY_LEAVE
    // -------------------------------------------------------------------------

    @Test
    void onText_lobbyJoin_dispatchesOnPlayerJoined() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onText(stubWebSocket, "{\"type\":\"LOBBY_JOIN\",\"playerId\":42,\"playerName\":\"Alice\"}", true);

        assertThat(listener.lastJoinedPlayerId).isEqualTo(42L);
        assertThat(listener.lastJoinedPlayerName).isEqualTo("Alice");
    }

    @Test
    void onText_lobbyLeave_dispatchesOnPlayerLeft() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onText(stubWebSocket, "{\"type\":\"LOBBY_LEAVE\",\"playerId\":99,\"playerName\":\"Bob\"}", true);

        assertThat(listener.lastLeftPlayerId).isEqualTo(99L);
        assertThat(listener.lastLeftPlayerName).isEqualTo("Bob");
    }

    // -------------------------------------------------------------------------
    // Message parsing — LOBBY_CHAT
    // -------------------------------------------------------------------------

    @Test
    void onText_lobbyChat_dispatchesOnChatReceived() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        ws.onText(stubWebSocket,
                "{\"type\":\"LOBBY_CHAT\",\"playerId\":42,\"playerName\":\"Alice\",\"message\":\"Hello everyone!\"}",
                true);

        assertThat(listener.lastChatPlayerId).isEqualTo(42L);
        assertThat(listener.lastChatPlayerName).isEqualTo("Alice");
        assertThat(listener.lastChatMessage).isEqualTo("Hello everyone!");
    }

    // -------------------------------------------------------------------------
    // Multi-fragment messages
    // -------------------------------------------------------------------------

    @Test
    void onText_multiFragment_buffersUntilLast() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        // Simulate a message split across three frames
        ws.onText(stubWebSocket, "{\"type\":\"LOBBY_JOIN\",", false);
        ws.onText(stubWebSocket, "\"playerId\":7,", false);
        ws.onText(stubWebSocket, "\"playerName\":\"Charlie\"}", true);

        assertThat(listener.lastJoinedPlayerId).isEqualTo(7L);
        assertThat(listener.lastJoinedPlayerName).isEqualTo("Charlie");
    }

    @Test
    void onText_multiFragment_resetBufferAfterComplete() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        // First message
        ws.onText(stubWebSocket, "{\"type\":\"LOBBY_JOIN\",\"playerId\":1,\"playerName\":\"A\"}", true);
        // Second message (should not bleed into first)
        ws.onText(stubWebSocket, "{\"type\":\"LOBBY_JOIN\",\"playerId\":2,\"playerName\":\"B\"}", true);

        assertThat(listener.lastJoinedPlayerId).isEqualTo(2L);
        assertThat(listener.lastJoinedPlayerName).isEqualTo("B");
    }

    // -------------------------------------------------------------------------
    // Error handling
    // -------------------------------------------------------------------------

    @Test
    void onText_unknownType_silentlyIgnored() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        assertThatNoException()
                .isThrownBy(() -> ws.onText(stubWebSocket, "{\"type\":\"UNKNOWN_TYPE\",\"data\":\"x\"}", true));
    }

    @Test
    void onText_malformedJson_silentlyIgnored() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        assertThatNoException().isThrownBy(() -> ws.onText(stubWebSocket, "not-json-at-all", true));
    }

    @Test
    void onText_emptyMessage_silentlyIgnored() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);

        assertThatNoException().isThrownBy(() -> ws.onText(stubWebSocket, "", true));
    }

    // -------------------------------------------------------------------------
    // sendChat
    // -------------------------------------------------------------------------

    @Test
    void sendChat_doesNotThrow_whenNotConnected() {
        assertThatNoException().isThrownBy(() -> client.sendChat("Hello"));
    }

    @Test
    void sendChat_doesNotThrow_afterDisconnect() {
        WebSocket.Listener ws = client.createListenerForTesting();
        ws.onOpen(stubWebSocket);
        client.disconnect();

        assertThatNoException().isThrownBy(() -> client.sendChat("Hello"));
    }

    // -------------------------------------------------------------------------
    // Helper: test listener implementation
    // -------------------------------------------------------------------------

    private static class TestLobbyMessageListener implements LobbyChatWebSocketClient.LobbyMessageListener {
        boolean connected;
        boolean disconnected;
        List<LobbyChatWebSocketClient.LobbyPlayer> playerList;
        long lastJoinedPlayerId;
        String lastJoinedPlayerName;
        long lastLeftPlayerId;
        String lastLeftPlayerName;
        long lastChatPlayerId;
        String lastChatPlayerName;
        String lastChatMessage;

        @Override
        public void onConnected() {
            connected = true;
        }

        @Override
        public void onDisconnected() {
            disconnected = true;
        }

        @Override
        public void onPlayerList(List<LobbyChatWebSocketClient.LobbyPlayer> players) {
            playerList = new ArrayList<>(players);
        }

        @Override
        public void onPlayerJoined(long playerId, String playerName) {
            lastJoinedPlayerId = playerId;
            lastJoinedPlayerName = playerName;
        }

        @Override
        public void onPlayerLeft(long playerId, String playerName) {
            lastLeftPlayerId = playerId;
            lastLeftPlayerName = playerName;
        }

        @Override
        public void onChatReceived(long playerId, String playerName, String message) {
            lastChatPlayerId = playerId;
            lastChatPlayerName = playerName;
            lastChatMessage = message;
        }
    }
}
