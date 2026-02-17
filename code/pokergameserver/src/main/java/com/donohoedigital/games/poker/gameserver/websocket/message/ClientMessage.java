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

/**
 * Client-to-server WebSocket message envelope.
 *
 * All messages sent from clients to the server are wrapped in this envelope
 * with a type, sequence number (for anti-replay), and type-specific data
 * payload.
 *
 * @param type
 *            Message type
 * @param sequenceNumber
 *            Monotonically increasing sequence number (anti-replay protection)
 * @param data
 *            Type-specific message payload
 */
public record ClientMessage(ClientMessageType type, long sequenceNumber, Object data) {
}
