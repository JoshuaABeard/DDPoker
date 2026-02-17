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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Represents a single player's WebSocket connection.
 *
 * Wraps a WebSocketSession with player identity and tracking for rate limiting
 * and anti-replay protection.
 */
public class PlayerConnection {

    private final WebSocketSession session;
    private final long profileId;
    private final String username;
    private final String gameId;
    private final ObjectMapper objectMapper;

    private volatile long lastActionTimestamp = 0;
    private volatile long lastSequenceNumber = 0;

    /**
     * Creates a new player connection.
     *
     * @param session
     *            WebSocket session
     * @param profileId
     *            Player's profile ID
     * @param username
     *            Player's username
     * @param gameId
     *            Game ID
     * @param objectMapper
     *            JSON object mapper
     */
    public PlayerConnection(WebSocketSession session, long profileId, String username, String gameId,
            ObjectMapper objectMapper) {
        this.session = session;
        this.profileId = profileId;
        this.username = username;
        this.gameId = gameId;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a message to the client.
     *
     * @param message
     *            Server message to send
     */
    public void sendMessage(ServerMessage message) {
        if (!session.isOpen()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message: " + message, e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send message to session " + session.getId(), e);
        }
    }

    /**
     * Checks if the WebSocket session is open.
     *
     * @return true if session is open, false otherwise
     */
    public boolean isOpen() {
        return session.isOpen();
    }

    /**
     * Closes the WebSocket session.
     */
    public void close() {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close session " + session.getId(), e);
        }
    }

    /**
     * Gets the player's profile ID.
     *
     * @return Profile ID
     */
    public long getProfileId() {
        return profileId;
    }

    /**
     * Gets the player's username.
     *
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the game ID.
     *
     * @return Game ID
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Gets the timestamp of the last action.
     *
     * @return Last action timestamp in milliseconds
     */
    public long getLastActionTimestamp() {
        return lastActionTimestamp;
    }

    /**
     * Sets the timestamp of the last action.
     *
     * @param timestamp
     *            Timestamp in milliseconds
     */
    public void setLastActionTimestamp(long timestamp) {
        this.lastActionTimestamp = timestamp;
    }

    /**
     * Gets the last sequence number received.
     *
     * @return Last sequence number
     */
    public long getLastSequenceNumber() {
        return lastSequenceNumber;
    }

    /**
     * Sets the last sequence number received.
     *
     * @param sequenceNumber
     *            Sequence number
     */
    public void setLastSequenceNumber(long sequenceNumber) {
        this.lastSequenceNumber = sequenceNumber;
    }

    /**
     * Gets the underlying WebSocket session.
     *
     * @return WebSocket session
     */
    public WebSocketSession getSession() {
        return session;
    }
}
