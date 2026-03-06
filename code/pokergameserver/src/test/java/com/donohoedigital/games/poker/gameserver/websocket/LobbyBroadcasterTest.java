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
package com.donohoedigital.games.poker.gameserver.websocket;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.protocol.message.ServerMessageData;
import com.donohoedigital.games.poker.protocol.message.ServerMessageType;

/**
 * Unit tests for {@link LobbyBroadcaster}.
 *
 * <p>
 * Verifies that each broadcast method constructs the correct message type and
 * delegates to {@link GameConnectionManager#broadcastToGame}.
 * </p>
 */
class LobbyBroadcasterTest {

    private GameConnectionManager connectionManager;
    private LobbyBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        connectionManager = mock(GameConnectionManager.class);
        broadcaster = new LobbyBroadcaster(connectionManager);
    }

    private ServerMessageData.LobbyPlayerData buildPlayer(String name) {
        return new ServerMessageData.LobbyPlayerData(99L, name, false, false, null);
    }

    @Test
    void should_broadcastLobbyPlayerJoined_with_correctMessageType() {
        ServerMessageData.LobbyPlayerData player = buildPlayer("Alice");
        broadcaster.broadcastLobbyPlayerJoined("game-1", player);

        verify(connectionManager).broadcastToGame(eq("game-1"),
                argThat(msg -> msg.type() == ServerMessageType.LOBBY_PLAYER_JOINED));
    }

    @Test
    void should_broadcastLobbyPlayerLeft_with_correctMessageType() {
        ServerMessageData.LobbyPlayerData player = buildPlayer("Bob");
        broadcaster.broadcastLobbyPlayerLeft("game-1", player);

        verify(connectionManager).broadcastToGame(eq("game-1"),
                argThat(msg -> msg.type() == ServerMessageType.LOBBY_PLAYER_LEFT));
    }

    @Test
    void should_broadcastLobbyPlayerKicked_with_correctMessageType() {
        ServerMessageData.LobbyPlayerData player = buildPlayer("Charlie");
        broadcaster.broadcastLobbyPlayerKicked("game-1", player);

        verify(connectionManager).broadcastToGame(eq("game-1"),
                argThat(msg -> msg.type() == ServerMessageType.LOBBY_PLAYER_KICKED));
    }

    @Test
    void should_broadcastLobbySettingsChanged_with_correctMessageType() {
        GameSummary summary = mock(GameSummary.class);
        broadcaster.broadcastLobbySettingsChanged("game-2", summary);

        verify(connectionManager).broadcastToGame(eq("game-2"),
                argThat(msg -> msg.type() == ServerMessageType.LOBBY_SETTINGS_CHANGED));
    }

    @Test
    void should_broadcastLobbyGameStarting_with_correctMessageType() {
        broadcaster.broadcastLobbyGameStarting("game-3", 30);

        verify(connectionManager).broadcastToGame(eq("game-3"),
                argThat(msg -> msg.type() == ServerMessageType.LOBBY_GAME_STARTING));
    }

    @Test
    void should_broadcastGameCancelled_with_correctMessageType() {
        broadcaster.broadcastGameCancelled("game-4", "host disconnected");

        verify(connectionManager).broadcastToGame(eq("game-4"),
                argThat(msg -> msg.type() == ServerMessageType.GAME_CANCELLED));
    }

    @Test
    void should_propagateGameId_in_broadcastLobbyPlayerJoined() {
        ServerMessageData.LobbyPlayerData player = buildPlayer("Dave");
        broadcaster.broadcastLobbyPlayerJoined("my-game-id", player);

        verify(connectionManager).broadcastToGame(eq("my-game-id"), argThat(msg -> "my-game-id".equals(msg.gameId())));
    }
}
