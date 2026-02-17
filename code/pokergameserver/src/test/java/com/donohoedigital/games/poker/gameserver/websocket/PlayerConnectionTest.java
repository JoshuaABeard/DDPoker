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
import com.donohoedigital.games.poker.gameserver.websocket.message.ServerMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for PlayerConnection.
 */
class PlayerConnectionTest {

    private WebSocketSession mockSession;
    private ObjectMapper objectMapper;
    private PlayerConnection connection;

    @BeforeEach
    void setUp() {
        mockSession = mock(WebSocketSession.class);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Java 8 date/time support
        connection = new PlayerConnection(mockSession, 123L, "testuser", "game-1", objectMapper);
    }

    @Test
    void sendMessage_serializesAndSendsToWebSocket() throws IOException {
        when(mockSession.isOpen()).thenReturn(true);

        ServerMessage message = ServerMessage.of(ServerMessageType.CONNECTED, "game-1", null);
        connection.sendMessage(message);

        verify(mockSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendMessage_doesNotSendIfSessionClosed() throws IOException {
        when(mockSession.isOpen()).thenReturn(false);

        ServerMessage message = ServerMessage.of(ServerMessageType.CONNECTED, "game-1", null);
        connection.sendMessage(message);

        verify(mockSession, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void isOpen_delegatesToWebSocketSession() {
        when(mockSession.isOpen()).thenReturn(true);
        assertTrue(connection.isOpen());

        when(mockSession.isOpen()).thenReturn(false);
        assertFalse(connection.isOpen());
    }

    @Test
    void close_closesWebSocketSession() throws IOException {
        connection.close();
        verify(mockSession).close();
    }

    @Test
    void getters_returnCorrectValues() {
        assertEquals(123L, connection.getProfileId());
        assertEquals("testuser", connection.getUsername());
        assertEquals("game-1", connection.getGameId());
    }

    @Test
    void sequenceTracking_tracksLastSequenceNumber() {
        assertEquals(0, connection.getLastSequenceNumber());

        connection.setLastSequenceNumber(5);
        assertEquals(5, connection.getLastSequenceNumber());
    }

    @Test
    void actionTimestamp_tracksLastActionTimestamp() {
        long initialTimestamp = connection.getLastActionTimestamp();
        assertEquals(0, initialTimestamp);

        long now = System.currentTimeMillis();
        connection.setLastActionTimestamp(now);
        assertEquals(now, connection.getLastActionTimestamp());
    }
}
