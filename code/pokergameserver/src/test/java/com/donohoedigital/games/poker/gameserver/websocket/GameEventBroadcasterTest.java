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
 * if not, see <http://www.gnu.org/licenses/>.
 * ============================================================================================
 */
package com.donohoedigital.games.poker.gameserver.websocket;

import com.donohoedigital.games.poker.core.event.GameEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GameEventBroadcaster — verifies correct event routing.
 */
class GameEventBroadcasterTest {

    private GameConnectionManager connectionManager;
    private OutboundMessageConverter converter;
    private GameEventBroadcaster broadcaster;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        connectionManager = new GameConnectionManager();
        converter = new OutboundMessageConverter();
        broadcaster = new GameEventBroadcaster("game-1", connectionManager, converter);
    }

    private PlayerConnection makeConnectedPlayer(long profileId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        PlayerConnection conn = new PlayerConnection(session, profileId, "player" + profileId, "game-1", objectMapper);
        connectionManager.addConnection("game-1", profileId, conn);
        return conn;
    }

    // ====================================
    // Event type dispatch tests
    // ====================================

    @Test
    void handStarted_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.HandStarted(0, 5));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void playerActed_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster
                .accept(new GameEvent.PlayerActed(0, 1, com.donohoedigital.games.poker.core.state.ActionType.FOLD, 0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void communityCardsDealt_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(
                new GameEvent.CommunityCardsDealt(0, com.donohoedigital.games.poker.core.state.BettingRound.FLOP));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void levelChanged_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.LevelChanged(0, 3));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void tournamentCompleted_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.TournamentCompleted(1));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handCompleted_broadcastsHandCompleteMessage() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);

        broadcaster.accept(new GameEvent.HandCompleted(0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void potAwarded_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.PotAwarded(0, 0, new int[]{1}, 500));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void showdownStarted_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.ShowdownStarted(0));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void cleaningDone_silentlyIgnored() throws Exception {
        // Internal housekeeping events should not be broadcast
        PlayerConnection p1 = makeConnectedPlayer(1L);

        broadcaster.accept(new GameEvent.CleaningDone(0));

        verify(p1.getSession(), never()).sendMessage(any());
    }

    @Test
    void noConnectedPlayers_doesNotThrow() {
        // No exception when nobody is connected
        assertDoesNotThrow(() -> broadcaster.accept(new GameEvent.HandStarted(0, 1)));
    }

    @Test
    void broadcastsCorrectMessageType_handStarted() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.HandStarted(0, 5));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"HAND_STARTED\"") || json.contains("\"type\" : \"HAND_STARTED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_playerActed() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster
                .accept(new GameEvent.PlayerActed(0, 1, com.donohoedigital.games.poker.core.state.ActionType.CHECK, 0));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"PLAYER_ACTED\"") || json.contains("\"type\" : \"PLAYER_ACTED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_levelChanged() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.LevelChanged(0, 3));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"LEVEL_CHANGED\"") || json.contains("\"type\" : \"LEVEL_CHANGED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_potAwarded() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.PotAwarded(0, 0, new int[]{1}, 500));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"POT_AWARDED\"") || json.contains("\"type\" : \"POT_AWARDED\"");
        }));
    }

    @Test
    void broadcastsCorrectMessageType_showdownStarted() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.ShowdownStarted(0));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"SHOWDOWN_STARTED\"") || json.contains("\"type\" : \"SHOWDOWN_STARTED\"");
        }));
    }

    @Test
    void playerEliminated_broadcastsToAllPlayers() throws Exception {
        PlayerConnection p1 = makeConnectedPlayer(1L);
        PlayerConnection p2 = makeConnectedPlayer(2L);

        broadcaster.accept(new GameEvent.PlayerEliminated(0, 1, 4));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
        verify(p2.getSession()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastsCorrectMessageType_playerEliminated() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.PlayerEliminated(0, 1, 4));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("\"type\":\"PLAYER_ELIMINATED\"") || json.contains("\"type\" : \"PLAYER_ELIMINATED\"");
        }));
    }
}
