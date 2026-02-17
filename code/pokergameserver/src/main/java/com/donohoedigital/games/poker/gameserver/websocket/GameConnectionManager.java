/*
 * ============================================================================================
 * DD Poker - Source Code
 * Copyright (c) 2026  DD Poker Community
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket connections for all active games.
 *
 * Thread-safe connection tracking with support for per-game connection
 * management, message routing, and reconnection handling.
 */
public class GameConnectionManager {

    /** Map of gameId -> (profileId -> PlayerConnection) */
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, PlayerConnection>> connections = new ConcurrentHashMap<>();

    /**
     * Adds a player connection to a game.
     *
     * If the player is already connected, the old connection is replaced
     * (reconnection).
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     * @param connection
     *            Player connection
     */
    public void addConnection(String gameId, long profileId, PlayerConnection connection) {
        connections.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>()).put(profileId, connection);
    }

    /**
     * Removes a player connection from a game.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     */
    public void removeConnection(String gameId, long profileId) {
        ConcurrentHashMap<Long, PlayerConnection> gameConnections = connections.get(gameId);
        if (gameConnections != null) {
            gameConnections.remove(profileId);
            if (gameConnections.isEmpty()) {
                connections.remove(gameId);
            }
        }
    }

    /**
     * Sends a message to a specific player in a game.
     *
     * If the player is not connected, this method does nothing.
     *
     * @param gameId
     *            Game ID
     * @param profileId
     *            Player's profile ID
     * @param message
     *            Message to send
     */
    public void sendToPlayer(String gameId, long profileId, ServerMessage message) {
        ConcurrentHashMap<Long, PlayerConnection> gameConnections = connections.get(gameId);
        if (gameConnections != null) {
            PlayerConnection connection = gameConnections.get(profileId);
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    /**
     * Broadcasts a message to all players in a game.
     *
     * @param gameId
     *            Game ID
     * @param message
     *            Message to broadcast
     */
    public void broadcastToGame(String gameId, ServerMessage message) {
        broadcastToGame(gameId, message, null);
    }

    /**
     * Broadcasts a message to all players in a game, optionally excluding one
     * player.
     *
     * @param gameId
     *            Game ID
     * @param message
     *            Message to broadcast
     * @param excludeProfileId
     *            Profile ID to exclude (null to include all players)
     */
    public void broadcastToGame(String gameId, ServerMessage message, Long excludeProfileId) {
        ConcurrentHashMap<Long, PlayerConnection> gameConnections = connections.get(gameId);
        if (gameConnections != null) {
            gameConnections.values().stream()
                    .filter(conn -> excludeProfileId == null || conn.getProfileId() != excludeProfileId)
                    .forEach(conn -> conn.sendMessage(message));
        }
    }

    /**
     * Gets all connections for a game.
     *
     * @param gameId
     *            Game ID
     * @return Unmodifiable collection of player connections (empty if game not
     *         found)
     */
    public Collection<PlayerConnection> getConnections(String gameId) {
        ConcurrentHashMap<Long, PlayerConnection> gameConnections = connections.get(gameId);
        if (gameConnections == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(gameConnections.values());
    }
}
