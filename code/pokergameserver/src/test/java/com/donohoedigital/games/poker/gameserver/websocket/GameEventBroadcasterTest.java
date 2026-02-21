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
import com.donohoedigital.games.poker.gameserver.GameInstance;
import com.donohoedigital.games.poker.gameserver.ServerGameTable;
import com.donohoedigital.games.poker.gameserver.ServerPlayer;
import com.donohoedigital.games.poker.gameserver.ServerTournamentContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

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
    void playerActed_withGameReference_populatesPlayerName() throws Exception {
        // Create a real player and table so name lookup works
        ServerPlayer player = new ServerPlayer(42, "TestPlayer", true, 0, 5000);
        player.setSeat(0);
        ServerGameTable sgt = new ServerGameTable(0, 2, null, 50, 100, 0);
        sgt.addPlayer(player, 0);

        ServerTournamentContext tournament = mock(ServerTournamentContext.class);
        when(tournament.getNumTables()).thenReturn(1);
        when(tournament.getTable(0)).thenReturn(sgt);

        GameInstance game = mock(GameInstance.class);
        when(game.getTournament()).thenReturn(tournament);

        GameEventBroadcaster broadcasterWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                game);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 99L,
                new PlayerConnection(session, 99L, "observer", "game-1", objectMapper));

        broadcasterWithGame
                .accept(new GameEvent.PlayerActed(0, 42, com.donohoedigital.games.poker.core.state.ActionType.FOLD, 0));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("TestPlayer");
        }));
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

    // ====================================
    // Fix 3: null-game broadcaster still broadcasts (reconnect path)
    // ====================================

    @Test
    void nullGameBroadcaster_playerRemoved_sendsPlayerLeft() throws Exception {
        // Broadcaster without game (null-game constructor) always sends PLAYER_LEFT.
        // This verifies the reconnect-path broadcaster (fix 3) doesn't break
        // PlayerRemoved.
        PlayerConnection p1 = makeConnectedPlayer(1L);

        broadcaster.accept(new GameEvent.PlayerRemoved(0, 42, 3));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    // ====================================
    // Fix 6 server: PlayerRemoved suppression during consolidation
    // ====================================

    @Test
    void playerRemoved_activePlayer_suppressesPlayerLeft() throws Exception {
        // Active player (finishPosition=0) being consolidated: PLAYER_LEFT suppressed.
        PlayerConnection p1 = makeConnectedPlayer(1L);

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerPlayer activePlayer = mock(ServerPlayer.class);
        when(activePlayer.getID()).thenReturn(42);
        when(activePlayer.getFinishPosition()).thenReturn(0);
        when(mockCtx.getAllPlayers()).thenReturn(List.of(activePlayer));
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.PlayerRemoved(0, 42, 3));

        verify(p1.getSession(), never()).sendMessage(any());
    }

    @Test
    void playerRemoved_eliminatedPlayer_sendsPlayerLeft() throws Exception {
        // Eliminated player (finishPosition>0): PLAYER_LEFT sent to clear seat.
        PlayerConnection p1 = makeConnectedPlayer(1L);

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerPlayer eliminatedPlayer = mock(ServerPlayer.class);
        when(eliminatedPlayer.getID()).thenReturn(42);
        when(eliminatedPlayer.getFinishPosition()).thenReturn(5);
        when(mockCtx.getAllPlayers()).thenReturn(List.of(eliminatedPlayer));
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.PlayerRemoved(0, 42, 3));

        verify(p1.getSession()).sendMessage(any(TextMessage.class));
    }

    // ====================================
    // Fix 6 server: PlayerAdded includes player name
    // ====================================

    @Test
    void playerAdded_withGame_includesPlayerName() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        GameInstance mockGame = mock(GameInstance.class);
        ServerTournamentContext mockCtx = mock(ServerTournamentContext.class);
        ServerGameTable mockTable = mock(ServerGameTable.class);
        ServerPlayer namedPlayer = mock(ServerPlayer.class);
        when(namedPlayer.getName()).thenReturn("Alice");
        when(mockTable.getPlayer(3)).thenReturn(namedPlayer);
        when(mockCtx.getTable(0)).thenReturn(mockTable);
        when(mockGame.getTournament()).thenReturn(mockCtx);

        GameEventBroadcaster broadcastWithGame = new GameEventBroadcaster("game-1", connectionManager, converter,
                mockGame);
        broadcastWithGame.accept(new GameEvent.PlayerAdded(0, 42, 3));

        verify(session).sendMessage(argThat(msg -> {
            String json = ((TextMessage) msg).getPayload();
            return json.contains("Alice");
        }));
    }

    @Test
    void playerAdded_withoutGame_sendsEmptyName() throws Exception {
        // Broadcaster without game falls back to empty player name.
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        connectionManager.addConnection("game-1", 1L, new PlayerConnection(session, 1L, "p1", "game-1", objectMapper));

        broadcaster.accept(new GameEvent.PlayerAdded(0, 42, 3));

        verify(session).sendMessage(any(TextMessage.class));
    }
}
