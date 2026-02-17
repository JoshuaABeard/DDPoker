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
package com.donohoedigital.games.poker.gameserver.websocket.message;

import java.time.Instant;

/**
 * Server-to-client WebSocket message envelope.
 *
 * All messages sent from the server to clients are wrapped in this envelope
 * with a type, game ID, timestamp, and type-specific data payload.
 *
 * @param type
 *            Message type
 * @param gameId
 *            Game ID this message belongs to
 * @param timestamp
 *            When the message was created
 * @param data
 *            Type-specific message payload
 */
public record ServerMessage(ServerMessageType type, String gameId, Instant timestamp, Object data) {
    /**
     * Factory method to create a server message with current timestamp.
     *
     * @param type
     *            Message type
     * @param gameId
     *            Game ID
     * @param data
     *            Message payload
     * @return New server message
     */
    public static ServerMessage of(ServerMessageType type, String gameId, Object data) {
        return new ServerMessage(type, gameId, Instant.now(), data);
    }
}
