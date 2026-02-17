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
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameConnectionManager.
 */
class GameConnectionManagerTest {

    private GameConnectionManager manager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        manager = new GameConnectionManager();
    }

    @Test
    void addConnection_addsConnectionToGame() {
        WebSocketSession session = mock(WebSocketSession.class);
        PlayerConnection connection = new PlayerConnection(session, 1L, "player1", "game-1", objectMapper);

        manager.addConnection("game-1", 1L, connection);

        Collection<PlayerConnection> connections = manager.getConnections("game-1");
        assertEquals(1, connections.size());
        assertTrue(connections.contains(connection));
    }

    @Test
    void addConnection_replacesExistingConnection() {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        PlayerConnection connection1 = new PlayerConnection(session1, 1L, "player1", "game-1", objectMapper);
        PlayerConnection connection2 = new PlayerConnection(session2, 1L, "player1", "game-1", objectMapper);

        manager.addConnection("game-1", 1L, connection1);
        manager.addConnection("game-1", 1L, connection2);

        Collection<PlayerConnection> connections = manager.getConnections("game-1");
        assertEquals(1, connections.size());
        assertTrue(connections.contains(connection2));
        assertFalse(connections.contains(connection1));
    }

    @Test
    void removeConnection_removesConnectionFromGame() {
        WebSocketSession session = mock(WebSocketSession.class);
        PlayerConnection connection = new PlayerConnection(session, 1L, "player1", "game-1", objectMapper);

        manager.addConnection("game-1", 1L, connection);
        manager.removeConnection("game-1", 1L);

        Collection<PlayerConnection> connections = manager.getConnections("game-1");
        assertTrue(connections.isEmpty());
    }

    @Test
    void sendToPlayer_sendsMessageToSpecificPlayer() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        PlayerConnection connection = new PlayerConnection(session, 1L, "player1", "game-1", objectMapper);
        manager.addConnection("game-1", 1L, connection);

        ServerMessage message = ServerMessage.of(ServerMessageType.CONNECTED, "game-1", null);
        manager.sendToPlayer("game-1", 1L, message);

        verify(session).sendMessage(any());
    }

    @Test
    void sendToPlayer_doesNothingIfPlayerNotConnected() {
        ServerMessage message = ServerMessage.of(ServerMessageType.CONNECTED, "game-1", null);

        // Should not throw exception
        assertDoesNotThrow(() -> manager.sendToPlayer("game-1", 999L, message));
    }

    @Test
    void broadcastToGame_sendsToAllPlayers() throws Exception {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        PlayerConnection connection1 = new PlayerConnection(session1, 1L, "player1", "game-1", objectMapper);
        PlayerConnection connection2 = new PlayerConnection(session2, 2L, "player2", "game-1", objectMapper);

        manager.addConnection("game-1", 1L, connection1);
        manager.addConnection("game-1", 2L, connection2);

        ServerMessage message = ServerMessage.of(ServerMessageType.HAND_STARTED, "game-1", null);
        manager.broadcastToGame("game-1", message);

        verify(session1).sendMessage(any());
        verify(session2).sendMessage(any());
    }

    @Test
    void broadcastToGame_excludesSpecifiedPlayer() throws Exception {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        PlayerConnection connection1 = new PlayerConnection(session1, 1L, "player1", "game-1", objectMapper);
        PlayerConnection connection2 = new PlayerConnection(session2, 2L, "player2", "game-1", objectMapper);

        manager.addConnection("game-1", 1L, connection1);
        manager.addConnection("game-1", 2L, connection2);

        ServerMessage message = ServerMessage.of(ServerMessageType.HAND_STARTED, "game-1", null);
        manager.broadcastToGame("game-1", message, 1L); // Exclude player 1

        verify(session1, never()).sendMessage(any());
        verify(session2).sendMessage(any());
    }

    @Test
    void getConnections_returnsEmptyForNonexistentGame() {
        Collection<PlayerConnection> connections = manager.getConnections("nonexistent");
        assertNotNull(connections);
        assertTrue(connections.isEmpty());
    }

    @Test
    void getConnections_isolatesGamesByGameId() {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        PlayerConnection connection1 = new PlayerConnection(session1, 1L, "player1", "game-1", objectMapper);
        PlayerConnection connection2 = new PlayerConnection(session2, 2L, "player2", "game-2", objectMapper);

        manager.addConnection("game-1", 1L, connection1);
        manager.addConnection("game-2", 2L, connection2);

        assertEquals(1, manager.getConnections("game-1").size());
        assertEquals(1, manager.getConnections("game-2").size());
    }

    @Test
    void threadSafety_concurrentAddsAndRemoves() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        WebSocketSession session = mock(WebSocketSession.class);
                        long profileId = (threadId * operationsPerThread) + j;
                        PlayerConnection connection = new PlayerConnection(session, profileId, "player" + profileId,
                                "game-1", objectMapper);

                        manager.addConnection("game-1", profileId, connection);
                        if (j % 2 == 0) {
                            manager.removeConnection("game-1", profileId);
                        } else {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Should have half the connections remaining (those not removed)
        Collection<PlayerConnection> connections = manager.getConnections("game-1");
        assertEquals(successCount.get(), connections.size());
    }
}
