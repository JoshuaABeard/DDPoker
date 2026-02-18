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
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.gameserver.dto.GameSummary;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageData;
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;

/**
 * Spring-managed broadcaster for lobby-phase WebSocket messages.
 *
 * <p>
 * Wraps {@link GameConnectionManager} to send lobby state changes (player
 * joined/left/kicked, settings changed, game starting, game cancelled) to all
 * connections in a game.
 * </p>
 */
public class LobbyBroadcaster {

    private final GameConnectionManager connectionManager;

    public LobbyBroadcaster(GameConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void broadcastLobbyPlayerJoined(String gameId, ServerMessageData.LobbyPlayerData player) {
        ServerMessage msg = ServerMessage.of(ServerMessageType.LOBBY_PLAYER_JOINED, gameId,
                new ServerMessageData.LobbyPlayerJoinedData(player));
        connectionManager.broadcastToGame(gameId, msg);
    }

    public void broadcastLobbyPlayerLeft(String gameId, ServerMessageData.LobbyPlayerData player) {
        ServerMessage msg = ServerMessage.of(ServerMessageType.LOBBY_PLAYER_LEFT, gameId,
                new ServerMessageData.LobbyPlayerLeftData(player));
        connectionManager.broadcastToGame(gameId, msg);
    }

    public void broadcastLobbyPlayerKicked(String gameId, ServerMessageData.LobbyPlayerData player) {
        ServerMessage msg = ServerMessage.of(ServerMessageType.LOBBY_PLAYER_KICKED, gameId,
                new ServerMessageData.LobbyPlayerKickedData(player));
        connectionManager.broadcastToGame(gameId, msg);
    }

    public void broadcastLobbySettingsChanged(String gameId, GameSummary updatedSettings) {
        ServerMessage msg = ServerMessage.of(ServerMessageType.LOBBY_SETTINGS_CHANGED, gameId,
                new ServerMessageData.LobbySettingsChangedData(updatedSettings));
        connectionManager.broadcastToGame(gameId, msg);
    }

    public void broadcastLobbyGameStarting(String gameId, int startingInSeconds) {
        ServerMessage msg = ServerMessage.of(ServerMessageType.LOBBY_GAME_STARTING, gameId,
                new ServerMessageData.LobbyGameStartingData(startingInSeconds));
        connectionManager.broadcastToGame(gameId, msg);
    }

    public void broadcastGameCancelled(String gameId, String reason) {
        ServerMessage msg = ServerMessage.of(ServerMessageType.GAME_CANCELLED, gameId,
                new ServerMessageData.GameCancelledData(reason));
        connectionManager.broadcastToGame(gameId, msg);
    }
}
